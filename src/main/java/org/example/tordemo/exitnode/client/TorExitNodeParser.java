package org.example.tordemo.exitnode.client;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.example.tordemo.exitnode.exception.InvalidTorResponseException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TorExitNodeParser {

    private static final InetAddressValidator ADDRESS_VALIDATOR = InetAddressValidator.getInstance();

    public Set<String> parse(String torResponse) {
        if (torResponse == null || torResponse.isBlank()) {
            throw new InvalidTorResponseException("Tor response is empty");
        }

        Set<String> addresses = torResponse.lines()
                .map(line -> line.strip().split("\\s+", 3))
                .filter(fields -> "ExitAddress".equals(fields[0]))
                .map(this::extractValidAddress)
                .collect(Collectors.toUnmodifiableSet());

        if (addresses.isEmpty()) {
            throw new InvalidTorResponseException("Tor response contains no exit-node addresses");
        }

        return addresses;
    }

    private String extractValidAddress(String[] fields) {
        if (fields.length < 2 || !ADDRESS_VALIDATOR.isValidInet4Address(fields[1])) {
            throw new InvalidTorResponseException("Tor response contains a missing or invalid IPv4 address");
        }
        return fields[1];
    }
}
