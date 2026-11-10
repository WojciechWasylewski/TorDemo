package org.example.tordemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "tor.cache.refresh-enabled=false")
class TorDemoApplicationTests {

    @Test
    void contextLoads() {
    }

}
