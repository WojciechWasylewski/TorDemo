package org.example.tordemo.exitnode.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Component
public class TorExitNodeClient {

    private static final Logger log = LoggerFactory.getLogger(TorExitNodeClient.class);

    private final RestClient torRestClient;
    private final TorExitNodeParser torExitNodeParser;

    public TorExitNodeClient(RestClient torRestClient, TorExitNodeParser torExitNodeParser) {
        this.torRestClient = torRestClient;
        this.torExitNodeParser = torExitNodeParser;
    }

    public Set<String> fetchExitNodes() {
        String response = torRestClient.get()
                .uri("/exit-addresses")
                .accept(MediaType.TEXT_PLAIN)
                .retrieve()
                .body(String.class);

        Set<String> torIPAddresses = torExitNodeParser.parse(response);
        log.info("Successfully fetched {} unique exit-node addresses from Tor API", torIPAddresses.size());

        return torIPAddresses;
    }
}
