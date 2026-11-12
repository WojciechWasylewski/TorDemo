package org.example.tordemo.exitnode.config;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "tor.cache")
public record TorCacheProperties(@NotNull Duration ttl) {

    public TorCacheProperties {
        if (ttl != null && (ttl.isZero() || ttl.isNegative())) {
            throw new IllegalArgumentException("tor.cache.ttl must be positive");
        }
    }
}
