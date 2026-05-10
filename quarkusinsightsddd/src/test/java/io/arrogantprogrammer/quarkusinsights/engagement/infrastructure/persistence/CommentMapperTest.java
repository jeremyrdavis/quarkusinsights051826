package io.arrogantprogrammer.quarkusinsights.engagement.infrastructure.persistence;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies {@link CommentMapper} round-trips a Comment aggregate through
 * {@link CommentEntity} preserving all state. Also verifies {@code applyTo}
 * copies only the mutable body field. Pure JUnit; no Quarkus startup since the
 * mapper has no CDI dependencies of its own.
 */
class CommentMapperTest {

    private CommentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CommentMapper();
    }

    @Test
    void roundTripPreservesAllFields() {
        Comment original = Comment.rehydrate(
            CommentId.random(),
            EpisodeId.random(),
            new AuthorHandle("testuser"),
            new CommentBody("This is a great episode!"),
            Instant.now()
        );

        CommentEntity entity = mapper.toEntity(original);
        Comment roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.episodeId(), roundTripped.episodeId());
        assertEquals(original.author(), roundTripped.author());
        assertEquals(original.body(), roundTripped.body());
        assertEquals(original.submittedAt(), roundTripped.submittedAt());
    }

    @Test
    void toEntityMapsAllColumns() {
        CommentId id = CommentId.random();
        EpisodeId episodeId = EpisodeId.random();
        AuthorHandle author = new AuthorHandle("myhandle");
        CommentBody body = new CommentBody("Great content!");
        Instant submittedAt = Instant.parse("2026-01-15T10:00:00Z");

        Comment comment = Comment.rehydrate(id, episodeId, author, body, submittedAt);
        CommentEntity entity = mapper.toEntity(comment);

        assertEquals(id.value(), entity.id);
        assertEquals(episodeId.value(), entity.episodeId);
        assertEquals(author.value(), entity.authorHandle);
        assertEquals(body.value(), entity.body);
        assertEquals(submittedAt, entity.submittedAt);
    }

    @Test
    void applyToUpdatesMutableBodyOnly() {
        Instant submittedAt = Instant.now();
        Comment original = Comment.rehydrate(
            CommentId.random(),
            EpisodeId.random(),
            new AuthorHandle("author1"),
            new CommentBody("Original body"),
            submittedAt
        );

        CommentEntity entity = mapper.toEntity(original);
        java.util.UUID originalId = entity.id;
        java.util.UUID originalEpisodeId = entity.episodeId;
        String originalAuthor = entity.authorHandle;
        Instant originalSubmittedAt = entity.submittedAt;

        // Simulate an edit by rehydrating with new body (within 5-minute window)
        Comment edited = Comment.rehydrate(
            original.id(),
            original.episodeId(),
            original.author(),
            new CommentBody("Updated body text"),
            submittedAt
        );

        mapper.applyTo(edited, entity);

        // Body updated
        assertEquals("Updated body text", entity.body);
        // Identity and immutable fields preserved
        assertEquals(originalId, entity.id);
        assertEquals(originalEpisodeId, entity.episodeId);
        assertEquals(originalAuthor, entity.authorHandle);
        assertEquals(originalSubmittedAt, entity.submittedAt);
    }
}
