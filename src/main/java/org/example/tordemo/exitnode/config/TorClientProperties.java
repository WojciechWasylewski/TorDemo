package org.example.tordemo.exitnode.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;
import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "tor.client")
public record TorClientProperties(
        @NotNull URI baseUrl,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout
) {
    public TorClientProperties {
        if (baseUrl != null && (!baseUrl.isAbsolute()
                || baseUrl.getHost() == null
                || !("https".equalsIgnoreCase(baseUrl.getScheme())
                || "http".equalsIgnoreCase(baseUrl.getScheme())))) {
            throw new IllegalArgumentException("tor.client.base-url must be an absolute HTTP(S) URL");
        }
        requirePositive(connectTimeout, "tor.client.connect-timeout");
        requirePositive(readTimeout, "tor.client.read-timeout");
    }

    private static void requirePositive(Duration duration, String property) {
        if (duration != null && (duration.isZero() || duration.isNegative())) {
            throw new IllegalArgumentException(property + " must be positive");
        }
    }
}
