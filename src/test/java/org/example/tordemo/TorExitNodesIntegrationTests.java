package org.example.tordemo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.example.tordemo.exitnode.helpers.MutableClock;
import org.example.tordemo.exitnode.helpers.TestClockConfiguration;
import org.example.tordemo.exitnode.service.TorExitNodeRefresher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "tor.cache.refresh-enabled=false",
        "tor.cache.ttl=30m"
})
@Import(TestClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TorExitNodesIntegrationTests {

    private static final String EXIT_ADDRESS = "171.25.193.39";
    private static final String OTHER_ADDRESS = "192.0.2.1";

    @RegisterExtension
    static WireMockExtension torApi = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void torClientProperties(DynamicPropertyRegistry registry) {
        registry.add("tor.client.base-url", torApi::baseUrl);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TorExitNodeRefresher refresher;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ObjectMapper objectMapper;

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterEach
    void closeHttpClient() {
        httpClient.close();
    }

    @Test
    void shouldServeGetAndHeadFromTheSameCachedFeed() throws Exception {
        stubFeed(EXIT_ADDRESS);
        refresher.refresh();

        assertEmptyResponse("HEAD", EXIT_ADDRESS, 200);
        var response = send("GET", EXIT_ADDRESS);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type").orElseThrow())
                .startsWith("application/json");
        assertThat(objectMapper.readTree(response.body()))
                .isEqualTo(objectMapper.readTree("""
                        {"requestedIp":"171.25.193.39","isExitNode":true}
                        """));
        assertEmptyResponse("HEAD", OTHER_ADDRESS, 404);
        assertEmptyResponse("GET", OTHER_ADDRESS, 404);

        torApi.verify(1, getRequestedFor(urlEqualTo("/exit-addresses")));
    }

    @Test
    void shouldRejectInvalidInputWithoutFetchingTheFeed() throws Exception {
        assertEmptyResponse("HEAD", "256.1.2.3", 400);
        var response = send("GET", "256.1.2.3");

        assertThat(response.statusCode()).isEqualTo(400);
        var error = objectMapper.readTree(response.body());
        assertThat(error.path("status").asInt()).isEqualTo(400);
        assertThat(error.path("message").asText())
                .isEqualTo("The IP address must be a valid IPv4 address.");
        assertThat(Instant.parse(error.path("timestamp").asText())).isNotNull();
        torApi.verify(0, getRequestedFor(urlEqualTo("/exit-addresses")));
    }

    @Test
    void shouldRemainUnavailableAfterInitialFailureAndRecoverOnSuccessfulRefresh() throws Exception {
        assertEmptyResponse("HEAD", EXIT_ADDRESS, 503);
        stubUpstream(503, "");
        refresher.refresh();

        assertEmptyResponse("GET", EXIT_ADDRESS, 503);
        assertEmptyResponse("HEAD", EXIT_ADDRESS, 503);

        stubFeed(EXIT_ADDRESS);
        refresher.refresh();

        assertEmptyResponse("HEAD", EXIT_ADDRESS, 200);
        torApi.verify(2, getRequestedFor(urlEqualTo("/exit-addresses")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http-error", "malformed-feed", "empty-body", "no-content"})
    void shouldPreserveSnapshotAndOriginalExpiryAfterFailedRefresh(String failure) throws Exception {
        stubFeed(EXIT_ADDRESS);
        refresher.refresh();
        clock.advance(Duration.ofMinutes(20));
        switch (failure) {
            case "http-error" -> stubUpstream(503, "");
            case "malformed-feed" -> stubUpstream(200,
                    "ExitAddress " + OTHER_ADDRESS + "\nExitAddress invalid-address");
            case "empty-body" -> stubUpstream(200, "");
            case "no-content" -> stubUpstream(204, "");
            default -> throw new IllegalArgumentException("Unknown failure: " + failure);
        }

        refresher.refresh();

        assertEmptyResponse("HEAD", EXIT_ADDRESS, 200);
        assertEmptyResponse("HEAD", OTHER_ADDRESS, 404);
        clock.advance(Duration.ofMinutes(10));
        assertEmptyResponse("GET", EXIT_ADDRESS, 503);
        assertEmptyResponse("HEAD", EXIT_ADDRESS, 503);
        assertEmptyResponse("HEAD", OTHER_ADDRESS, 503);
        torApi.verify(2, getRequestedFor(urlEqualTo("/exit-addresses")));
    }

    @Test
    void shouldReplaceAddressesAndRenewExpiryAfterSuccessfulRefresh() throws Exception {
        stubFeed(EXIT_ADDRESS);
        refresher.refresh();
        clock.advance(Duration.ofMinutes(20));
        stubFeed(OTHER_ADDRESS);

        refresher.refresh();
        clock.advance(Duration.ofMinutes(10));

        assertEmptyResponse("HEAD", EXIT_ADDRESS, 404);
        assertEmptyResponse("HEAD", OTHER_ADDRESS, 200);
        clock.advance(Duration.ofMinutes(20));
        assertEmptyResponse("HEAD", OTHER_ADDRESS, 503);
        torApi.verify(2, getRequestedFor(urlEqualTo("/exit-addresses")));
    }

    private void stubFeed(String address) {
        stubUpstream(200, "ExitAddress " + address + " 2026-11-13 12:00:00\n");
    }

    private void stubUpstream(int status, String body) {
        torApi.stubFor(get(urlEqualTo("/exit-addresses"))
                .willReturn(aResponse().withStatus(status)
                        .withHeader("Content-Type", "text/plain").withBody(body)));
    }

    private HttpResponse<String> send(String method, String address) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/" + address))
                .timeout(Duration.ofSeconds(5))
                .method(method, HttpRequest.BodyPublishers.noBody()).build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void assertEmptyResponse(String method, String address, int status) throws Exception {
        var response = send(method, address);
        assertThat(response.statusCode()).as("%s /%s status", method, address).isEqualTo(status);
        assertThat(response.body()).as("%s /%s body", method, address).isEmpty();
    }
}
