package org.example.tordemo.exitnode.service;

import org.example.tordemo.exitnode.Ipv4Address;
import java.time.Instant;
import java.util.Set;

record TorExitNodeSnapshot(Set<Ipv4Address> addresses, Instant expiresAt) {
}
