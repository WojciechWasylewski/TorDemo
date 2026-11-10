package org.example.tordemo.exitnode.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "tor.cache.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class TorCacheConfig {
}
