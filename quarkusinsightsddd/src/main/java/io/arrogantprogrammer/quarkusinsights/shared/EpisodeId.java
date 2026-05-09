package io.arrogantprogrammer.quarkusinsights.shared;

import java.util.UUID;

public record EpisodeId(UUID value) {

    public EpisodeId {
        if (value == null) {
            throw new IllegalArgumentException("EpisodeId value must not be null");
        }
    }

    public static EpisodeId random() {
        return new EpisodeId(UUID.randomUUID());
    }

    public static EpisodeId fromString(String s) {
        return new EpisodeId(UUID.fromString(s));
    }
}
