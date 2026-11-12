package org.example.tordemo.exitnode.service;

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;

import org.example.tordemo.exitnode.client.TorExitNodeClient;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class TorExitNodeService {
    private final TorExitNodeClient torExitNodeClient;
    private volatile Set<String> exitNodes;

    public TorExitNodeService(TorExitNodeClient torExitNodeClient) {
        this.torExitNodeClient = torExitNodeClient;
    }

    public boolean isExitNode(String ipAddress) {
        Set<String> snapshot = exitNodes;
        if (snapshot == null) {
            throw new ExitNodesUnavailableException();
        }
        return snapshot.contains(ipAddress);
    }

    synchronized void refresh() {
        Set<String> updated = Set.copyOf(torExitNodeClient.fetchExitNodes());
        if (updated.isEmpty()) {
            throw new IllegalStateException("Tor returned no exit-node addresses");
        }
        exitNodes = updated;
    }
}
