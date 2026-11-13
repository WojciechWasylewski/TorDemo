package org.example.tordemo.exitnode.api;

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException;
import org.example.tordemo.exitnode.service.TorExitNodeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TorNodeCheckerController.class)
class TorNodeCheckerControllerTests {

    private static final String IP_ADDRESS = "171.25.193.39";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TorExitNodeService service;

    @Test
    void shouldReturnAnEmptyOkResponseForHeadWhenAddressIsAnExitNode() throws Exception {
        when(service.isExitNode(IP_ADDRESS)).thenReturn(true);

        mockMvc.perform(head("/{ip}", IP_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(service).isExitNode(IP_ADDRESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD"})
    void shouldReturnAnEmptyNotFoundResponseWhenAddressIsNotAnExitNode(String method) throws Exception {
        when(service.isExitNode(IP_ADDRESS)).thenReturn(false);

        mockMvc.perform(request(HttpMethod.valueOf(method), "/{ip}", IP_ADDRESS))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));

        verify(service).isExitNode(IP_ADDRESS);
    }

    @Test
    void shouldReturnAddressDetailsForGetWhenAddressIsAnExitNode() throws Exception {
        when(service.isExitNode(IP_ADDRESS)).thenReturn(true);

        mockMvc.perform(get("/{ip}", IP_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {"requestedIp": "171.25.193.39", "isExitNode": true}
                        """, JsonCompareMode.STRICT));

        verify(service).isExitNode(IP_ADDRESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.1.2.3", "1.2.3", "01.2.3.4", "example.com", "::1"})
    void shouldRejectInvalidHeadInputBeforeCallingTheService(String invalidAddress) throws Exception {
        mockMvc.perform(head("/{ip}", invalidAddress))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"256.1.2.3", "1.2.3", "01.2.3.4", "example.com", "::1"})
    void shouldRejectInvalidGetInputBeforeCallingTheService(String invalidAddress) throws Exception {
        mockMvc.perform(get("/{ip}", invalidAddress))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("The IP address must be a valid IPv4 address."))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.path").doesNotExist());

        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "HEAD"})
    void shouldReturnAnEmptyServiceUnavailableResponseWhenCacheIsUnavailable(String method) throws Exception {
        when(service.isExitNode(IP_ADDRESS)).thenThrow(new ExitNodesUnavailableException());

        mockMvc.perform(request(HttpMethod.valueOf(method), "/{ip}", IP_ADDRESS))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
        verify(service).isExitNode(IP_ADDRESS);
    }
}
