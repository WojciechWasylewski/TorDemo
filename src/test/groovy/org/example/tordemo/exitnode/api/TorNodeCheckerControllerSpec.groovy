package org.example.tordemo.exitnode.api

import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException
import org.example.tordemo.exitnode.service.TorExitNodeService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.json.JsonCompareMode
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(TorNodeCheckerController)
class TorNodeCheckerControllerSpec extends Specification {

    private static final String IP_ADDRESS = '171.25.193.39'

    @Autowired
    private MockMvc mockMvc

    @SpringBean
    TorExitNodeService service = Mock()

    def "routes HEAD to the lookup and returns 200 when the address is an exit node"() {
        when:
        mockMvc.perform(head('/{ip}', IP_ADDRESS))
                .andExpect(status().isOk())

        then:
        1 * service.isExitNode(IP_ADDRESS) >> true
    }

    def "returns address details for GET when the address is an exit node"() {
        when:
        mockMvc.perform(get('/{ip}', IP_ADDRESS))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json('''
                    {"requestedIp": "171.25.193.39", "isExitNode": true}
                    ''', JsonCompareMode.STRICT))

        then:
        1 * service.isExitNode(IP_ADDRESS) >> true
    }

    def "returns an empty 404 response for #method when the address is not an exit node"() {
        when:
        mockMvc.perform(request(method, '/{ip}', IP_ADDRESS))
                .andExpect(status().isNotFound())
                .andExpect(content().string(''))

        then:
        1 * service.isExitNode(IP_ADDRESS) >> false

        where:
        method << [HttpMethod.GET, HttpMethod.HEAD]
    }

    def "returns an empty 503 response for #method when the cache is unavailable"() {
        when:
        mockMvc.perform(request(method, '/{ip}', IP_ADDRESS))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(''))

        then:
        1 * service.isExitNode(IP_ADDRESS) >> { throw new ExitNodesUnavailableException() }

        where:
        method << [HttpMethod.GET, HttpMethod.HEAD]
    }

    def "rejects invalid HEAD input without calling the service: #invalidAddress"() {
        when:
        mockMvc.perform(head('/{ip}', invalidAddress))
                .andExpect(status().isBadRequest())

        then:
        0 * service._

        where:
        invalidAddress << ['256.1.2.3', '1.2.3', '01.2.3.4', 'example.com', '::1']
    }

    def "returns validation details for invalid GET input without calling the service: #invalidAddress"() {
        when:
        mockMvc.perform(get('/{ip}', invalidAddress))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath('$.status').value(400))
                .andExpect(jsonPath('$.message').value('The IP address must be a valid IPv4 address.'))
                .andExpect(jsonPath('$.timestamp').isNotEmpty())
                .andExpect(jsonPath('$.path').doesNotExist())

        then:
        0 * service._

        where:
        invalidAddress << ['256.1.2.3', '1.2.3', '01.2.3.4', 'example.com', '::1']
    }
}
