package org.example.tordemo.exitnode.validation;

import org.example.tordemo.exitnode.Ipv4Address;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class IPv4Validator implements ConstraintValidator<IPv4, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return Ipv4Address.isValid(value);
    }
}
