package org.example.tordemo.exitnode.service

import spock.lang.Specification

class TorExitNodeRefresherSpec extends Specification {

    private final TorExitNodeService service = Mock()
    private final TorExitNodeRefresher refresher = new TorExitNodeRefresher(service)

    def "refreshes the cache through the service"() {
        when:
        refresher.refresh()

        then:
        1 * service.refresh()
    }

    def "catches exceptions when the service refresh fails"() {
        when:
        refresher.refresh()

        then:
        1 * service.refresh() >> { throw new IllegalStateException('Tor is unavailable') }
        noExceptionThrown()
    }
}
