package org.example.tordemo.exitnode.client

import org.example.tordemo.exitnode.Ipv4Address
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import spock.lang.Specification

import static org.springframework.test.web.client.match.MockRestRequestMatchers.*
import static org.springframework.test.web.client.response.MockRestResponseCreators.*

class TorExitNodeClientSpec extends Specification {

    private MockRestServiceServer server
    private TorExitNodeClient client

    def setup() {
        RestClient.Builder builder = RestClient.builder().baseUrl('https://tor.example')
        server = MockRestServiceServer.bindTo(builder).build()
        client = new TorExitNodeClient(builder.build(), new TorExitNodeParser())
    }

    def "fetches the text feed and returns parsed addresses"() {
        given:
        server.expect(requestTo('https://tor.example/exit-addresses'))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header('Accept', 'text/plain'))
                .andRespond(withSuccess('''
                    ExitNode 4786BEB8D9152472EE32CFB31458D68A06528ABA
                    Published 2026-11-10 06:01:46
                    ExitAddress 171.25.193.39 2026-11-10 15:29:00
                    ExitAddress 171.25.193.40 2026-11-10 15:00:57
                    ''', MediaType.TEXT_PLAIN))

        when:
        Set<Ipv4Address> addresses = client.fetchExitNodes()

        then:
        addresses == Set.of(new Ipv4Address('171.25.193.39'), new Ipv4Address('171.25.193.40'))
        server.verify()
    }

    def "propagates upstream HTTP #statusCode errors"() {
        given:
        server.expect(requestTo('https://tor.example/exit-addresses'))
                .andRespond(withStatus(HttpStatusCode.valueOf(statusCode)))

        when:
        client.fetchExitNodes()

        then:
        RestClientResponseException exception = thrown()
        exception.statusCode.value() == statusCode
        server.verify()

        where:
        statusCode << [404, 500, 503]
    }

    def "propagates network timeouts"() {
        given:
        server.expect(requestTo('https://tor.example/exit-addresses'))
                .andRespond(withException(new SocketTimeoutException('Read timed out')))

        when:
        client.fetchExitNodes()

        then:
        ResourceAccessException exception = thrown()
        exception.cause instanceof SocketTimeoutException
        server.verify()
    }
}
