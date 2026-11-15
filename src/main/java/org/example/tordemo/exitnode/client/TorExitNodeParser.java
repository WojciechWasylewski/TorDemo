package org.example.tordemo.exitnode.client;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.example.tordemo.exitnode.exception.InvalidTorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
public class TorExitNodeParser {

    private static final Logger log = LoggerFactory.getLogger(TorExitNodeParser.class);
    private static final InetAddressValidator ADDRESS_VALIDATOR = InetAddressValidator.getInstance();

    public Set<String> parse(String torResponse) {
        if (torResponse == null || torResponse.isBlank()) {
            throw new InvalidTorResponseException("Tor response is empty");
        }

        Set<String> validAddresses = new HashSet<>();
        int rejectedRecords = 0;
        Iterator<String> iterator = torResponse.lines().iterator();

        while (iterator.hasNext()) {
            String[] fields = iterator.next().strip().split("\\s+", 3);
            if (!"ExitAddress".equals(fields[0])) {
                continue;
            }
            if (!hasValidAddress(fields)) {
                rejectedRecords++;
                continue;
            }
            validAddresses.add(fields[1]);
        }

        if (rejectedRecords > 0) {
            log.warn("Skipped {} malformed ExitAddress records; parsed {} unique valid IPv4 addresses",
                    rejectedRecords, validAddresses.size());
        }

        if (validAddresses.isEmpty()) {
            throw new InvalidTorResponseException("Tor response contains no valid exit-node addresses");
        }

        return Set.copyOf(validAddresses);
    }

    private boolean hasValidAddress(String[] fields) {
        return fields.length >= 2 && ADDRESS_VALIDATOR.isValidInet4Address(fields[1]);
    }
}
