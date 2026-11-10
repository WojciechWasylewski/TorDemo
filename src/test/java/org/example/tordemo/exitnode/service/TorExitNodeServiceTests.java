package org.example.tordemo.exitnode.service;

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;

import java.util.HashSet;
import java.util.Set;

import org.example.tordemo.exitnode.client.TorExitNodeClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TorExitNodeServiceTests {

    private final TorExitNodeClient client = mock(TorExitNodeClient.class);
    private final TorExitNodeService service = new TorExitNodeService(client);

    @Test
    void lookupIsUnavailableUntilTheFirstSuccessfulRefresh() {
        assertThatThrownBy(() -> service.isExitNode("1.2.3.4"))
                .isInstanceOf(ExitNodesUnavailableException.class);
        verify(client, times(0)).fetchExitNodes();
    }

    @Test
    void lookupsReuseAnImmutableSnapshotAndRefreshReplacesIt() {
        var addresses = new HashSet<>(Set.of("1.2.3.4"));
        when(client.fetchExitNodes()).thenReturn(addresses, Set.of("5.6.7.8"));
        service.refresh();
        addresses.clear();

        assertThat(service.isExitNode("1.2.3.4")).isTrue();
        assertThat(service.isExitNode("5.6.7.8")).isFalse();
        verify(client, times(1)).fetchExitNodes();

        service.refresh();
        assertThat(service.isExitNode("1.2.3.4")).isFalse();
        assertThat(service.isExitNode("5.6.7.8")).isTrue();
    }

    @Test
    void failedRefreshRetainsThePreviousSnapshot() {
        when(client.fetchExitNodes()).thenReturn(Set.of("1.2.3.4"))
                .thenThrow(new IllegalStateException("Upstream unavailable"));
        service.refresh();

        assertThatIllegalStateException().isThrownBy(service::refresh);
        assertThat(service.isExitNode("1.2.3.4")).isTrue();
    }

    @Test
    void emptyResponseCannotReplaceThePreviousSnapshot() {
        when(client.fetchExitNodes()).thenReturn(Set.of("1.2.3.4"), Set.of());
        service.refresh();

        assertThatIllegalStateException().isThrownBy(service::refresh);
        assertThat(service.isExitNode("1.2.3.4")).isTrue();
    }
}
