package org.example.tordemo.exitnode.validation

import spock.lang.Specification

class IPv4ValidatorSpec extends Specification {

    private final IPv4Validator validator = new IPv4Validator()

    def "validates IPv4 input #address as #expected"() {
        expect:
        validator.isValid(address, null) == expected

        where:
        address                | expected
        '0.0.0.0'              | true
        '255.255.255.255'      | true
        '171.25.193.39'        | true
        '127.0.0.1'            | true
        '192.168.1.1'          | true
        null                   | false
        ''                     | false
        ' '                    | false
        '256.1.2.3'            | false
        '1.256.2.3'            | false
        '1.2.256.3'            | false
        '1.2.3.256'            | false
        '1000.220.101.54'      | false
        '1.2.3'                | false
        '1.2.3.4.5'            | false
        '1..3.4'               | false
        '1.2.3.'               | false
        '01.2.3.4'             | false
        '1.2.3.04'             | false
        '-1.2.3.4'             | false
        '+1.2.3.4'             | false
        ' 1.2.3.4'             | false
        '1.2.3.4 '             | false
        'localhost'            | false
        'example.com'          | false
        '::1'                  | false
        '::ffff:192.168.1.1'   | false
        '1.2.3.4/24'           | false
        '1.2.3.4:8080'         | false
    }
}
