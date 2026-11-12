package org.example.tordemo.exitnode.api.response;

import java.time.Instant;

public record InvalidIpv4Response(Instant timestamp, int status, String message) {
}
