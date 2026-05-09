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
        return new PersonId(UUID.fromString(s));
    }
}
