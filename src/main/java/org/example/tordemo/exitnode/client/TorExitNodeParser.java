package org.example.tordemo.exitnode.client;

import org.example.tordemo.exitnode.Ipv4Address;
import org.example.tordemo.exitnode.exception.InvalidTorResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class TorExitNodeParser {

    private static final Logger log = LoggerFactory.getLogger(TorExitNodeParser.class);

    public Set<Ipv4Address> parse(String torResponse) {
        if (torResponse == null || torResponse.isBlank()) {
            throw new InvalidTorResponseException("Tor response is empty");
        }

        Set<Ipv4Address> validAddresses = new HashSet<>();
        int rejectedRecords = 0;

        for (String line : torResponse.lines().toList()) {
            String[] fields = line.strip().split("\\s+", 3);
            if (!"ExitAddress".equals(fields[0])) {
                continue;
            }
            if (fields.length < 2) {
                rejectedRecords++;
                continue;
            }
            try {
                validAddresses.add(new Ipv4Address(fields[1]));
            } catch (IllegalArgumentException invalidAddress) {
                rejectedRecords++;
            }
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
}
