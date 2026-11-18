package org.example.tordemo

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import spock.lang.Specification
import tools.jackson.databind.ObjectMapper

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = 'tor.cache.refresh-enabled=false')
class TorExitNodesApplicationSpec extends Specification {

    @LocalServerPort
    private int port

    @Autowired
    private ObjectMapper objectMapper

    private HttpClient httpClient

    def setup() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()
    }

    def cleanup() {
        httpClient?.close()
    }

    def "serves Swagger UI"() {
        when:
        HttpResponse<String> response = get('/swagger-ui/index.html')

        then:
        response.statusCode() == 200
        response.body().contains('Swagger UI')
    }

    def "reports basic application health as UP"() {
        when:
        HttpResponse<String> response = get('/actuator/health')

        then:
        response.statusCode() == 200
        objectMapper.readTree(response.body()).path('status').asText() == 'UP'
    }

    private HttpResponse<String> get(String path) {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:${port}${path}"))
                .timeout(Duration.ofSeconds(5))
                .GET().build()
        httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
}
