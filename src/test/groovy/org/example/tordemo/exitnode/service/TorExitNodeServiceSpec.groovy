package org.example.tordemo.exitnode.service

import org.example.tordemo.exitnode.client.TorExitNodeClient
import org.example.tordemo.exitnode.config.TorCacheProperties
import org.example.tordemo.exitnode.exception.ExitNodesUnavailableException
import org.example.tordemo.exitnode.helpers.MutableClock
import spock.lang.Specification

import java.time.Duration

class TorExitNodeServiceSpec extends Specification {

    private static final String EXIT_ADDRESS = '171.25.193.39'
    private static final String OTHER_ADDRESS = '171.25.193.40'
    private static final Duration TTL = Duration.ofMinutes(30)

    private final TorExitNodeClient client = Mock()
    private final MutableClock clock = new MutableClock()
    private final TorExitNodeService service = new TorExitNodeService(client, new TorCacheProperties(TTL), clock)

    def "reports unavailable before the first successful refresh"() {
        when:
        service.isExitNode(EXIT_ADDRESS)

        then:
        thrown(ExitNodesUnavailableException)
        0 * client._
    }

    def "checks cached addresses without downloading again"() {
        when:
        service.refresh()
        boolean knownAddress = service.isExitNode(EXIT_ADDRESS)
        boolean unknownAddress = service.isExitNode(OTHER_ADDRESS)
        boolean repeatedLookup = service.isExitNode(EXIT_ADDRESS)

        then:
        1 * client.fetchExitNodes() >> Set.of(EXIT_ADDRESS)
        0 * client._
        knownAddress
        !unknownAddress
        repeatedLookup
    }

    def "replaces the previous snapshot after a successful refresh"() {
        given:
        client.fetchExitNodes() >>> [Set.of(EXIT_ADDRESS), Set.of(OTHER_ADDRESS)]
        service.refresh()

        when:
        service.refresh()

        then:
        !service.isExitNode(EXIT_ADDRESS)
        service.isExitNode(OTHER_ADDRESS)
    }

    def "keeps a snapshot independent of the mutable client result"() {
        given:
        Set<String> addresses = new HashSet<>(Set.of(EXIT_ADDRESS))
        client.fetchExitNodes() >> addresses
        service.refresh()

        when:
        addresses.clear()
        addresses.add(OTHER_ADDRESS)

        then:
        service.isExitNode(EXIT_ADDRESS)
        !service.isExitNode(OTHER_ADDRESS)
    }

    def "preserves the previous snapshot when fetching fails"() {
        given:
        IllegalStateException failure = new IllegalStateException('Tor is unavailable')
        client.fetchExitNodes() >> Set.of(EXIT_ADDRESS) >> { throw failure }
        service.refresh()

        when:
        service.refresh()

        then:
        IllegalStateException exception = thrown()
        exception.is(failure)
        service.isExitNode(EXIT_ADDRESS)
    }

    def "rejects an empty update and preserves the previous snapshot"() {
        given:
        client.fetchExitNodes() >>> [Set.of(EXIT_ADDRESS), Set.of()]
        service.refresh()

        when:
        service.refresh()

        then:
        thrown(IllegalStateException)
        service.isExitNode(EXIT_ADDRESS)
    }

    def "recovers after the initial download fails"() {
        given:
        IllegalStateException failure = new IllegalStateException('Tor is unavailable')
        client.fetchExitNodes() >> { throw failure } >> Set.of(EXIT_ADDRESS)

        when:
        service.refresh()

        then:
        IllegalStateException exception = thrown()
        exception.is(failure)

        when:
        service.isExitNode(EXIT_ADDRESS)

        then:
        thrown(ExitNodesUnavailableException)

        when:
        service.refresh()

        then:
        service.isExitNode(EXIT_ADDRESS)
    }

    def "uses the snapshot until just before expiry"() {
        given:
        client.fetchExitNodes() >> Set.of(EXIT_ADDRESS)
        service.refresh()

        when:
        clock.advance(TTL.minusNanos(1))

        then:
        service.isExitNode(EXIT_ADDRESS)
    }

    def "rejects lookups at expiry regardless of membership: #address"() {
        given:
        client.fetchExitNodes() >> Set.of(EXIT_ADDRESS)
        service.refresh()
        clock.advance(TTL)

        when:
        service.isExitNode(address)

        then:
        thrown(ExitNodesUnavailableException)

        where:
        address << [EXIT_ADDRESS, OTHER_ADDRESS]
    }

    def "does not extend TTL when fetching fails"() {
        given:
        client.fetchExitNodes() >> Set.of(EXIT_ADDRESS) >> { throw new IllegalStateException('Tor is unavailable') }
        service.refresh()
        clock.advance(Duration.ofMinutes(20))

        when:
        service.refresh()

        then:
        thrown(IllegalStateException)
        service.isExitNode(EXIT_ADDRESS)

        when:
        clock.advance(Duration.ofMinutes(10))
        service.isExitNode(EXIT_ADDRESS)

        then:
        thrown(ExitNodesUnavailableException)
    }

    def "does not extend TTL when a refresh returns no addresses"() {
        given:
        client.fetchExitNodes() >>> [Set.of(EXIT_ADDRESS), Set.of()]
        service.refresh()
        clock.advance(Duration.ofMinutes(20))

        when:
        service.refresh()

        then:
        thrown(IllegalStateException)
        service.isExitNode(EXIT_ADDRESS)

        when:
        clock.advance(Duration.ofMinutes(10))
        service.isExitNode(EXIT_ADDRESS)

        then:
        thrown(ExitNodesUnavailableException)
    }

    def "renews TTL after successful refresh even when addresses are unchanged"() {
        given:
        client.fetchExitNodes() >> Set.of(EXIT_ADDRESS)
        service.refresh()
        clock.advance(Duration.ofMinutes(35))

        when:
        service.refresh()
        clock.advance(TTL.minusNanos(1))

        then:
        service.isExitNode(EXIT_ADDRESS)

        when:
        clock.advance(Duration.ofNanos(1))
        service.isExitNode(EXIT_ADDRESS)

        then:
        thrown(ExitNodesUnavailableException)
    }
}
