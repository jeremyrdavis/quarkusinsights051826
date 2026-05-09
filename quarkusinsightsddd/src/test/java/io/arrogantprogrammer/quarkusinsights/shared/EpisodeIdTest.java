package io.arrogantprogrammer.quarkusinsights.shared;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EpisodeIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(IllegalArgumentException.class, () -> new EpisodeId(null));
    }

    @Test
    void equalsAndHashCodeByValue() {
        UUID first = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID second = UUID.fromString("11111111-1111-1111-1111-111111111111");
        // Distinct UUID objects, same value — UUID.fromString does not intern.
        EpisodeId a = new EpisodeId(first);
        EpisodeId b = new EpisodeId(second);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void randomYieldsDistinctIds() {
        EpisodeId a = EpisodeId.random();
        EpisodeId b = EpisodeId.random();
        assertNotEquals(a, b);
    }

    @Test
    void fromStringRoundTrips() {
        String s = "22222222-2222-2222-2222-222222222222";
        EpisodeId id = EpisodeId.fromString(s);
        assertEquals(s, id.value().toString());
    }

    @Test
    void fromStringRejectsInvalidUuid() {
        assertThrows(IllegalArgumentException.class, () -> EpisodeId.fromString("not-a-uuid"));
    }

    @Test
    void fromStringRejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> EpisodeId.fromString(null));
    }
}
