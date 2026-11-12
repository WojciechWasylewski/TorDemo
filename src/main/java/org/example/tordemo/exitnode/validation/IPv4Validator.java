package org.example.tordemo.exitnode.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.InetAddressValidator;

public class IPv4Validator implements ConstraintValidator<IPv4, String> {
    private static final InetAddressValidator IPV4_ADDRESS_VALIDATOR = InetAddressValidator.getInstance();

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && IPV4_ADDRESS_VALIDATOR.isValidInet4Address(value);
    }
}
