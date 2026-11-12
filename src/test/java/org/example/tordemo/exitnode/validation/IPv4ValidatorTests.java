package org.example.tordemo.exitnode.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IPv4ValidatorTests {

    private final IPv4Validator validator = new IPv4Validator();

    @ParameterizedTest
    @ValueSource(strings = {
            "0.0.0.0", "255.255.255.255", "171.25.193.39", "127.0.0.1", "192.168.1.1"
    })
    void shouldAcceptValidIpv4Addresses(String address) {
        assertThat(validator.isValid(address, null)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            " ", "256.1.2.3", "1.256.2.3", "1.2.256.3", "1.2.3.256",
            "1000.220.101.54", "1.2.3", "1.2.3.4.5", "1..3.4", "1.2.3.",
            "01.2.3.4", "1.2.3.04", "-1.2.3.4", "+1.2.3.4",
            " 1.2.3.4", "1.2.3.4 ", "localhost", "example.com",
            "::1", "::ffff:192.168.1.1", "1.2.3.4/24", "1.2.3.4:8080"
    })
    void shouldRejectInvalidIpv4Addresses(String address) {
        assertThat(validator.isValid(address, null)).isFalse();
    }
}
