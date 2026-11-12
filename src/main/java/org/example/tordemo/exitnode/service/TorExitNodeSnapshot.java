package org.example.tordemo.exitnode.service;

import java.time.Instant;
import java.util.Set;

record TorExitNodeSnapshot(Set<String> addresses, Instant expiresAt) {
}
