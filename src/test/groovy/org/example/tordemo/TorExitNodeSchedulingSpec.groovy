package org.example.tordemo

import com.github.tomakehurst.wiremock.WireMockServer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
        'tor.cache.refresh-enabled=true',
        'tor.cache.refresh-interval=1h',
        'tor.cache.ttl=30m'
])
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TorExitNodeSchedulingSpec extends Specification {

    private static final String EXIT_ADDRESS = '171.25.193.39'
    private static final WireMockServer TOR_API = new WireMockServer(wireMockConfig().dynamicPort())

    @DynamicPropertySource
    static void torClientProperties(DynamicPropertyRegistry registry) {
        // The first scheduled refresh can run during context startup, before setup().
        if (!TOR_API.isRunning()) {
            TOR_API.start()
            TOR_API.stubFor(get(urlEqualTo('/exit-addresses'))
                    .willReturn(ok("ExitAddress ${EXIT_ADDRESS} 2026-11-15 12:00:00\n")
                            .withHeader('Content-Type', 'text/plain')))
        }
        registry.add('tor.client.base-url', TOR_API::baseUrl)
    }

    @LocalServerPort
    private int port

    private HttpClient httpClient

    def setup() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build()
    }

    def cleanup() {
        httpClient?.close()
    }

    def cleanupSpec() {
        TOR_API.stop()
    }

    def "loads the cache automatically on startup without an explicit refresh"() {
        expect:
        new PollingConditions(timeout: 10, delay: 0.1).eventually {
            HttpResponse<String> response = checkExitAddress()
            assert response.statusCode() == 200
            assert response.body().isEmpty()
        }
        TOR_API.verify(1, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    private HttpResponse<String> checkExitAddress() {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:${port}/${EXIT_ADDRESS}"))
                .timeout(Duration.ofSeconds(2))
                .method('HEAD', HttpRequest.BodyPublishers.noBody())
                .build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
