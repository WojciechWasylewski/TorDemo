package org.example.tordemo.exitnode.client;

import org.example.tordemo.exitnode.exception.InvalidTorResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TorExitNodeParserTests {

    private final TorExitNodeParser parser = new TorExitNodeParser();

    @Test
    void shouldExtractAddressesAndIgnoreRelayMetadata() {
        String response = """
                ExitNode 4786BEB8D9152472EE32CFB31458D68A06528ABA
                Published 2026-11-10 06:01:46
                LastStatus 2026-11-10 15:00:00
                ExitAddress 171.25.193.39 2026-11-10 15:29:00
                ExitAddress 171.25.193.40 2026-11-10 15:29:00

                ExitNode 5538F4859D663156DF421B45B5E451DE2457BB7E
                Published 2026-11-10 06:01:46
                LastStatus 2026-11-10 15:00:00
                ExitAddress 171.25.193.38 2026-11-10 14:26:19
                """;

        var addresses = parser.parse(response);

        assertThat(addresses).containsExactlyInAnyOrder(
                "171.25.193.39", "171.25.193.40", "171.25.193.38");
    }

    @Test
    void shouldReturnEachAddressOnlyOnce() {
        String response = """
                ExitNode first-relay
                ExitAddress 171.25.193.39 2026-11-10 15:29:00
                ExitNode second-relay
                ExitAddress 171.25.193.39 2026-11-10 15:00:57
                """;

        assertThat(parser.parse(response)).containsExactly("171.25.193.39");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "\n\n", " ", "ExitNode relay\nPublished 2026-11-10 06:01:46\nLastStatus 2026-11-10 15:00:00",
            "<html>Upstream unavailable</html>"
    })
    void shouldRejectResponsesWithoutAddresses(String response) {
        assertThatThrownBy(() -> parser.parse(response)).isInstanceOf(InvalidTorResponseException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void shouldHandleUnixAndWindowsLineEndings(String lineEnding) {
        String response = "ExitNode relay" + lineEnding
                + "ExitAddress 171.25.193.39 2026-11-10 15:29:00" + lineEnding
                + "ExitAddress 171.25.193.40 2026-11-10 15:00:57";

        assertThat(parser.parse(response))
                .containsExactlyInAnyOrder("171.25.193.39", "171.25.193.40");
    }

    @Test
    void shouldHandleMultipleSpacesBetweenFields() {
        String response = "ExitAddress   171.25.193.39   2026-11-10   15:29:00";

        assertThat(parser.parse(response)).containsExactly("171.25.193.39");
    }

    @Test
    void shouldHandleTabsAndSurroundingWhitespace() {
        String response = "  ExitAddress\t171.25.193.39\t2026-11-10 15:29:00  \n";

        assertThat(parser.parse(response)).containsExactly("171.25.193.39");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ExitAddress", "ExitAddress   ", "ExitAddress\t",
            "ExitAddress invalid-address", "ExitAddress 999.1.2.3",
            "ExitAddress ::1", "ExitAddress 01.2.3.4"
    })
    void shouldSkipMalformedRecordsAndKeepTheValidAddresses(String invalidRecord) {
        String response = "ExitAddress 171.25.193.39 2026-11-10 15:29:00\n"
                + invalidRecord + "\n"
                + "ExitAddress 171.25.193.40 2026-11-10 15:29:00\n";

        assertThat(parser.parse(response))
                .containsExactlyInAnyOrder("171.25.193.39", "171.25.193.40");
    }

    @Test
    void shouldRejectAResponseWhenEveryAddressRecordIsMalformed() {
        String response = """
                ExitNode relay
                ExitAddress
                ExitAddress invalid-address
                ExitAddress 999.1.2.3
                """;

        assertThatThrownBy(() -> parser.parse(response))
                .isInstanceOf(InvalidTorResponseException.class)
                .hasMessage("Tor response contains no valid exit-node addresses");
    }

    @Test
    void shouldDeduplicateValidAddressesWhenMalformedRecordsAreSkipped() {
        String response = """
                ExitAddress 171.25.193.39 2026-11-10 15:29:00
                ExitAddress invalid-address
                ExitAddress 171.25.193.39 2026-11-10 15:30:00
                """;

        assertThat(parser.parse(response)).containsExactly("171.25.193.39");
    }
}
