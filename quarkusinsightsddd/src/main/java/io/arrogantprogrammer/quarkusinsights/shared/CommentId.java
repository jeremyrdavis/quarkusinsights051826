package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record CommentId(UUID value) {

    public CommentId {
        if (value == null) {
            throw new IllegalArgumentException("CommentId value must not be null");
        }
    }

    public static CommentId random() {
        return new CommentId(UUID.randomUUID());
    }

    public static CommentId fromString(String s) {
        return new CommentId(UUID.fromString(s));
    }
}
