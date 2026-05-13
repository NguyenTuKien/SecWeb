package com.messenger.mini_messenger.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Base64;

public class Base64ValueValidator implements ConstraintValidator<Base64Value, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            try {
                Base64.getUrlDecoder().decode(value);
                return true;
            } catch (IllegalArgumentException ignoredAgain) {
                return false;
            }
        }
    }
}
