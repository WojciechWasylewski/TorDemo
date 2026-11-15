package org.example.tordemo.exitnode.client

import org.example.tordemo.exitnode.exception.InvalidTorResponseException
import spock.lang.Specification

class TorExitNodeParserSpec extends Specification {

    private final TorExitNodeParser parser = new TorExitNodeParser()

    def "extracts addresses and ignores relay metadata"() {
        given:
        String response = '''
            ExitNode 4786BEB8D9152472EE32CFB31458D68A06528ABA
            Published 2026-11-10 06:01:46
            LastStatus 2026-11-10 15:00:00
            ExitAddress 171.25.193.39 2026-11-10 15:29:00
            ExitAddress 171.25.193.40 2026-11-10 15:29:00

            ExitNode 5538F4859D663156DF421B45B5E451DE2457BB7E
            Published 2026-11-10 06:01:46
            LastStatus 2026-11-10 15:00:00
            ExitAddress 171.25.193.38 2026-11-10 14:26:19
            '''

        when:
        Set<String> addresses = parser.parse(response)

        then:
        addresses == Set.of('171.25.193.39', '171.25.193.40', '171.25.193.38')
    }

    def "returns each address only once across relays"() {
        given:
        String response = '''
            ExitNode first-relay
            ExitAddress 171.25.193.39 2026-11-10 15:29:00
            ExitNode second-relay
            ExitAddress 171.25.193.39 2026-11-10 15:00:57
            '''

        expect:
        parser.parse(response) == Set.of('171.25.193.39')
    }

    def "rejects responses without addresses: #description"() {
        when:
        parser.parse(response)

        then:
        thrown(InvalidTorResponseException)

        where:
        description     | response
        'null body'     | null
        'empty body'    | ''
        'blank lines'   | '\n\n'
        'whitespace'    | ' '
        'metadata only' | 'ExitNode relay\nPublished 2026-11-10 06:01:46\nLastStatus 2026-11-10 15:00:00'
        'HTML body'     | '<html>Upstream unavailable</html>'
    }

    def "handles #description line endings"() {
        given:
        String response = 'ExitNode relay' + lineEnding +
                'ExitAddress 171.25.193.39 2026-11-10 15:29:00' + lineEnding +
                'ExitAddress 171.25.193.40 2026-11-10 15:00:57'

        expect:
        parser.parse(response) == Set.of('171.25.193.39', '171.25.193.40')

        where:
        description | lineEnding
        'Unix'      | '\n'
        'Windows'   | '\r\n'
    }

    def "handles multiple spaces between fields"() {
        given:
        String response = 'ExitAddress   171.25.193.39   2026-11-10   15:29:00'

        expect:
        parser.parse(response) == Set.of('171.25.193.39')
    }

    def "handles tabs and surrounding whitespace"() {
        given:
        String response = '  ExitAddress\t171.25.193.39\t2026-11-10 15:29:00  \n'

        expect:
        parser.parse(response) == Set.of('171.25.193.39')
    }

    def "skips malformed records and keeps valid addresses: #description"() {
        given:
        String response = 'ExitAddress 171.25.193.39 2026-11-10 15:29:00\n' +
                invalidRecord + '\n' +
                'ExitAddress 171.25.193.40 2026-11-10 15:29:00\n'

        expect:
        parser.parse(response) == Set.of('171.25.193.39', '171.25.193.40')

        where:
        description          | invalidRecord
        'missing address'    | 'ExitAddress'
        'spaces only'        | 'ExitAddress   '
        'tab only'           | 'ExitAddress\t'
        'non-address text'   | 'ExitAddress invalid-address'
        'octet out of range' | 'ExitAddress 999.1.2.3'
        'IPv6'               | 'ExitAddress ::1'
        'leading zero'       | 'ExitAddress 01.2.3.4'
    }

    def "rejects a response when every address record is malformed"() {
        given:
        String response = '''
            ExitNode relay
            ExitAddress
            ExitAddress invalid-address
            ExitAddress 999.1.2.3
            '''

        when:
        parser.parse(response)

        then:
        InvalidTorResponseException exception = thrown()
        exception.message == 'Tor response contains no valid exit-node addresses'
    }

    def "deduplicates valid addresses when malformed records are skipped"() {
        given:
        String response = '''
            ExitAddress 171.25.193.39 2026-11-10 15:29:00
            ExitAddress invalid-address
            ExitAddress 171.25.193.39 2026-11-10 15:30:00
            '''

        expect:
        parser.parse(response) == Set.of('171.25.193.39')
    }
}
