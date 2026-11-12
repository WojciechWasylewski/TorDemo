package org.example.tordemo.exitnode.client;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TorExitNodeClientTests {

    private MockRestServiceServer server;
    private TorExitNodeClient client;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("https://tor.example");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new TorExitNodeClient(builder.build(), new TorExitNodeParser());
    }

    @Test
    void shouldFetchTheTextListAndReturnParsedAddresses() {
        server.expect(requestTo("https://tor.example/exit-addresses"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Accept", "text/plain"))
                .andRespond(withSuccess("""
                        ExitNode 4786BEB8D9152472EE32CFB31458D68A06528ABA
                        Published 2026-11-10 06:01:46
                        ExitAddress 171.25.193.39 2026-11-10 15:29:00
                        ExitAddress 171.25.193.40 2026-11-10 15:00:57
                        """, MediaType.TEXT_PLAIN));

        var addresses = client.fetchExitNodes();

        assertThat(addresses).containsExactlyInAnyOrder("171.25.193.39", "171.25.193.40");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 500, 503})
    void shouldPropagateUpstreamHttpErrors(int status) {
        server.expect(requestTo("https://tor.example/exit-addresses"))
                .andRespond(withStatus(HttpStatusCode.valueOf(status)));

        assertThatThrownBy(client::fetchExitNodes)
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
        server.verify();
    }

    @Test
    void shouldPropagateNetworkTimeouts() {
        server.expect(requestTo("https://tor.example/exit-addresses"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(client::fetchExitNodes)
                .isInstanceOf(ResourceAccessException.class)
                .hasCauseInstanceOf(SocketTimeoutException.class);
        server.verify();
    }
}
