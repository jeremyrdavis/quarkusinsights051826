package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record PersonId(UUID value) {

    public PersonId {
        if (value == null) {
            throw new IllegalArgumentException("PersonId value must not be null");
        }
    }

    public static PersonId random() {
        return new PersonId(UUID.randomUUID());
    }

    public static PersonId fromString(String s) {
        if (s == null) {
            throw new IllegalArgumentException("PersonId string must not be null");
        }
        return new PersonId(UUID.fromString(s));
    }
}
