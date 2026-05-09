package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record RatingId(UUID value) {

    public RatingId {
        if (value == null) {
            throw new IllegalArgumentException("RatingId value must not be null");
        }
    }

    public static RatingId random() {
        return new RatingId(UUID.randomUUID());
    }

    public static RatingId fromString(String s) {
        return new RatingId(UUID.fromString(s));
    }
}
