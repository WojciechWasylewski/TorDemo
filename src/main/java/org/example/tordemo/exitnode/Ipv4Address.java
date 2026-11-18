package org.example.tordemo.exitnode;

import org.apache.commons.validator.routines.InetAddressValidator;

public record Ipv4Address(String value) {

    public Ipv4Address {
        if (!isValid(value)) {
            throw new IllegalArgumentException("Invalid IPv4 address");
        }
    }

    public static boolean isValid(String value) {
        return value != null && InetAddressValidator.getInstance().isValidInet4Address(value);
    }
}
