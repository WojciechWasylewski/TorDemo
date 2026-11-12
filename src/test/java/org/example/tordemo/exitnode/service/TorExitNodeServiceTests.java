package org.example.tordemo.exitnode.service;

import java.util.HashSet;
import java.util.Set;

import org.example.tordemo.exitnode.client.TorExitNodeClient;
import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TorExitNodeServiceTests {

    @Mock
    private TorExitNodeClient client;

    @InjectMocks
    private TorExitNodeService service;

    @Test
    void shouldReportUnavailableBeforeTheFirstSuccessfulRefresh() {
        assertThatThrownBy(() -> service.isExitNode("171.25.193.39"))
                .isInstanceOf(ExitNodesUnavailableException.class);

        verifyNoInteractions(client);
    }

    @Test
    void shouldCheckCachedAddressesWithoutFetchingAgain() {
        when(client.fetchExitNodes()).thenReturn(Set.of("171.25.193.39"));
        service.refresh();

        assertThat(service.isExitNode("171.25.193.39")).isTrue();
        assertThat(service.isExitNode("192.0.2.1")).isFalse();
        assertThat(service.isExitNode("171.25.193.39")).isTrue();

        verify(client).fetchExitNodes();
        verifyNoMoreInteractions(client);
    }

    @Test
    void shouldReplaceThePreviousSnapshotAfterSuccessfulRefresh() {
        when(client.fetchExitNodes()).thenReturn(Set.of("171.25.193.39"))
                .thenReturn(Set.of("171.25.193.40"));
        service.refresh();

        service.refresh();

        assertThat(service.isExitNode("171.25.193.39")).isFalse();
        assertThat(service.isExitNode("171.25.193.40")).isTrue();
    }

    @Test
    void shouldKeepASnapshotIndependentOfTheClientResult() {
        var addresses = new HashSet<>(Set.of("171.25.193.39"));
        when(client.fetchExitNodes()).thenReturn(addresses);
        service.refresh();

        addresses.clear();
        addresses.add("171.25.193.40");

        assertThat(service.isExitNode("171.25.193.39")).isTrue();
        assertThat(service.isExitNode("171.25.193.40")).isFalse();
    }

    @Test
    void shouldPreserveThePreviousSnapshotWhenFetchingFails() {
        var failure = new IllegalStateException("Tor is unavailable");
        when(client.fetchExitNodes()).thenReturn(Set.of("171.25.193.39"))
                .thenThrow(failure);
        service.refresh();

        assertThatThrownBy(service::refresh).isSameAs(failure);

        assertThat(service.isExitNode("171.25.193.39")).isTrue();
    }

    @Test
    void shouldRejectAnEmptyRefreshAndPreserveThePreviousSnapshot() {
        when(client.fetchExitNodes()).thenReturn(Set.of("171.25.193.39"))
                .thenReturn(Set.of());
        service.refresh();

        assertThatThrownBy(service::refresh).isInstanceOf(IllegalStateException.class);

        assertThat(service.isExitNode("171.25.193.39")).isTrue();
    }

    @Test
    void shouldBecomeAvailableAfterAnInitialFetchFailure() {
        var failure = new IllegalStateException("Tor is unavailable");
        when(client.fetchExitNodes()).thenThrow(failure)
                .thenReturn(Set.of("171.25.193.39"));

        assertThatThrownBy(service::refresh).isSameAs(failure);
        assertThatThrownBy(() -> service.isExitNode("171.25.193.39"))
                .isInstanceOf(ExitNodesUnavailableException.class);

        service.refresh();

        assertThat(service.isExitNode("171.25.193.39")).isTrue();
    }
}
