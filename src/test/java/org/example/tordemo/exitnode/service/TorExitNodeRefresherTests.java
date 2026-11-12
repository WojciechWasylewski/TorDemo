package org.example.tordemo.exitnode.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TorExitNodeRefresherTests {

    @Mock
    private TorExitNodeService service;

    @InjectMocks
    private TorExitNodeRefresher refresher;

    @Test
    void shouldRefreshTheCache() {
        refresher.refresh();

        verify(service).refresh();
    }

    @Test
    void shouldCatchExceptionWhenServiceRefreshFails() {
        doThrow(new IllegalStateException("Tor is unavailable"))
                .when(service).refresh();

        assertThatCode(refresher::refresh).doesNotThrowAnyException();

        verify(service).refresh();
    }

}
