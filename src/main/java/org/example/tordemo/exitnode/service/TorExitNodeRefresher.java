package org.example.tordemo.exitnode.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TorExitNodeRefresher {

    private static final Logger log = LoggerFactory.getLogger(TorExitNodeRefresher.class);

    private final TorExitNodeService service;

    public TorExitNodeRefresher(TorExitNodeService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${tor.cache.refresh-interval}", initialDelay = 0)
    public void refresh() {
        try {
            service.refresh();
            log.info("Successfully refreshed the Tor exit-node cache");
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh Tor exit-node data; keeping the previous snapshot", exception);
        }
    }
}
