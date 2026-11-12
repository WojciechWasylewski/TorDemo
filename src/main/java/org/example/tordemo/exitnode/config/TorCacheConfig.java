package org.example.tordemo.exitnode.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TorCacheProperties.class)
class TorCacheConfig {

    @Bean
    Clock torCacheClock() {
        return Clock.systemUTC();
    }

}
