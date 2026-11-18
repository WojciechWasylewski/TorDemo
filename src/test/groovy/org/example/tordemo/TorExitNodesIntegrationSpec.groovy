package org.example.tordemo

import com.github.tomakehurst.wiremock.WireMockServer
import org.example.tordemo.exitnode.helpers.MutableClock
import org.example.tordemo.exitnode.helpers.TestClockConfiguration
import org.example.tordemo.exitnode.service.TorExitNodeRefresher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = [
        'tor.cache.refresh-enabled=false',
        'tor.cache.ttl=30m'
])
@Import(TestClockConfiguration)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TorExitNodesIntegrationSpec extends Specification {

    private static final String EXIT_ADDRESS = '171.25.193.39'
    private static final String OTHER_ADDRESS = '192.0.2.1'
    private static final WireMockServer TOR_API = new WireMockServer(wireMockConfig().dynamicPort())

    @DynamicPropertySource
    static void torClientProperties(DynamicPropertyRegistry registry) {
        // Spring needs the stub URL before injecting the application's HTTP client.
        if (!TOR_API.isRunning()) {
            TOR_API.start()
        }
        registry.add('tor.client.base-url', TOR_API::baseUrl)
    }

    @LocalServerPort
    private int port

    @Autowired
    private TorExitNodeRefresher refresher

    @Autowired
    private MutableClock clock

    @Autowired
    private ObjectMapper objectMapper

    private HttpClient httpClient

    def setup() {
        TOR_API.resetAll()
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    def cleanup() {
        httpClient?.close()
    }

    def cleanupSpec() {
        TOR_API.stop()
    }

    def "serves GET and HEAD from the same cached feed"() {
        given:
        stubFeed(EXIT_ADDRESS)

        when:
        refresher.refresh()
        HttpResponse<String> response = send('GET', EXIT_ADDRESS)

        then:
        response.statusCode() == 200
        response.headers().firstValue('Content-Type').orElseThrow().startsWith('application/json')
        objectMapper.readTree(response.body()) == objectMapper.readTree('''
            {"requestedIp":"171.25.193.39","isExitNode":true}
            ''')
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 200)
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 404)
        assertEmptyResponse('GET', OTHER_ADDRESS, 404)
        TOR_API.verify(1, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    def "rejects invalid input without fetching the feed"() {
        when:
        HttpResponse<String> response = send('GET', '256.1.2.3')

        then:
        response.statusCode() == 400
        response.headers().firstValue('Content-Type').orElseThrow().startsWith('application/json')
        JsonNode error = objectMapper.readTree(response.body())
        error.path('status').asInt() == 400
        error.path('message').asText() == 'The IP address must be a valid IPv4 address.'
        Instant.parse(error.path('timestamp').asText()) != null
        assertEmptyResponse('HEAD', '256.1.2.3', 400)
        TOR_API.verify(0, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    def "remains unavailable after initial failure and recovers after a successful refresh"() {
        given:
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 503)
        stubUpstream(503, '')

        when:
        refresher.refresh()

        then:
        assertEmptyResponse('GET', EXIT_ADDRESS, 503)
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 503)

        when:
        stubFeed(EXIT_ADDRESS)
        refresher.refresh()

        then:
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 200)
        TOR_API.verify(2, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    def "preserves the snapshot and original expiry after #description"() {
        given:
        stubFeed(EXIT_ADDRESS)
        refresher.refresh()
        clock.advance(Duration.ofMinutes(20))
        stubUpstream(upstreamStatus, body)

        when:
        refresher.refresh()

        then:
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 200)
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 404)

        when:
        clock.advance(Duration.ofMinutes(10))

        then:
        assertEmptyResponse('GET', EXIT_ADDRESS, 503)
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 503)
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 503)
        TOR_API.verify(2, getRequestedFor(urlEqualTo('/exit-addresses')))

        where:
        description              | upstreamStatus | body
        'an HTTP error'          | 503            | ''
        'an entirely bad feed'   | 200            | 'ExitAddress\nExitAddress invalid-address'
        'an empty response body' | 200            | ''
        'a no-content response'  | 204            | ''
    }

    def "replaces addresses and renews expiry after a successful refresh"() {
        given:
        stubFeed(EXIT_ADDRESS)
        refresher.refresh()
        clock.advance(Duration.ofMinutes(20))
        stubFeed(OTHER_ADDRESS)

        when:
        refresher.refresh()
        clock.advance(Duration.ofMinutes(10))

        then:
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 404)
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 200)

        when:
        clock.advance(Duration.ofMinutes(20))

        then:
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 503)
        TOR_API.verify(2, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    def "publishes valid addresses and renews TTL when some records are malformed"() {
        given:
        stubFeed(EXIT_ADDRESS)
        refresher.refresh()
        clock.advance(Duration.ofMinutes(20))
        stubUpstream(200, "ExitAddress ${OTHER_ADDRESS}\nExitAddress invalid-address\nExitAddress")

        when:
        refresher.refresh()
        clock.advance(Duration.ofMinutes(10))
        HttpResponse<String> response = send('GET', OTHER_ADDRESS)

        then:
        assertEmptyResponse('HEAD', EXIT_ADDRESS, 404)
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 200)
        response.statusCode() == 200
        objectMapper.readTree(response.body()).path('requestedIp').asText() == OTHER_ADDRESS

        when:
        clock.advance(Duration.ofMinutes(20))

        then:
        assertEmptyResponse('HEAD', OTHER_ADDRESS, 503)
        TOR_API.verify(2, getRequestedFor(urlEqualTo('/exit-addresses')))
    }

    private void stubFeed(String address) {
        stubUpstream(200, "ExitAddress ${address} 2026-11-13 12:00:00\n")
    }

    private void stubUpstream(int status, String body) {
        TOR_API.stubFor(get(urlEqualTo('/exit-addresses'))
                .willReturn(aResponse().withStatus(status)
                        .withHeader('Content-Type', 'text/plain').withBody(body)))
    }

    private HttpResponse<String> send(String method, String address) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:${port}/${address}"))
                .timeout(Duration.ofSeconds(5))
                .method(method, HttpRequest.BodyPublishers.noBody()).build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private void assertEmptyResponse(String method, String address, int status) {
        HttpResponse<String> response = send(method, address)
        assert response.statusCode() == status: "${method} /${address} status"
        assert response.body().isEmpty(): "${method} /${address} body"
    }
}
