package org.example.tordemo.exitnode.service;

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;
import org.example.tordemo.exitnode.config.TorCacheProperties;

import org.example.tordemo.exitnode.client.TorExitNodeClient;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.time.Clock;
import java.time.Duration;

@Service
public class TorExitNodeService {
    private final TorExitNodeClient torExitNodeClient;
    private final Clock clock;
    private final Duration ttl;
    private volatile TorExitNodeSnapshot cachedSnapshot;

    public TorExitNodeService(TorExitNodeClient torExitNodeClient, TorCacheProperties properties, Clock clock) {
        this.torExitNodeClient = torExitNodeClient;
        this.ttl = properties.ttl();
        this.clock = clock;
    }

    public boolean isExitNode(String ipAddress) {
        TorExitNodeSnapshot snapshot = cachedSnapshot;
        if (snapshot == null || !clock.instant().isBefore(snapshot.expiresAt())) {
            throw new ExitNodesUnavailableException();
        }
        return snapshot.addresses().contains(ipAddress);
    }

    synchronized void refresh() {
        Set<String> updated = Set.copyOf(torExitNodeClient.fetchExitNodes());
        if (updated.isEmpty()) {
            throw new IllegalStateException("Tor returned no exit-node addresses");
        }
        cachedSnapshot = new TorExitNodeSnapshot(updated, clock.instant().plus(ttl));
    }

}
