package org.example.tordemo.exitnode.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

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
    @ValueSource(strings = {
            "", "\n\n", "ExitNode relay\nPublished 2026-11-10 06:01:46\nLastStatus 2026-11-10 15:00:00"
    })
    void shouldReturnAnEmptySetWhenThereAreNoAddressRecords(String response) {
        assertThat(parser.parse(response)).isEmpty();
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
}
