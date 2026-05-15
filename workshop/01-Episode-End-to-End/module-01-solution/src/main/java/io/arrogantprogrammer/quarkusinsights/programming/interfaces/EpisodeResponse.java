package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Flat JSON response shape for an Episode aggregate.
 *
 * <p>Designed for direct Jackson serialization — all components are
 * primitives or basic JDK types, no domain types leak across the
 * wire. {@link EpisodeResponseMapper} produces these from a loaded
 * {@code Episode} aggregate.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 *
 * @param id          the Episode's id
 * @param number      sequential episode number
 * @param title       display title
 * @param airDate     air date
 * @param theAbstract submitted abstract, or null if not yet submitted
 * @param presenters  presenter PersonIds (UUID strings)
 * @param speakers    speaker PersonIds (UUID strings)
 * @param status      lifecycle status (SCHEDULED / LIVE / PUBLISHED / CANCELED)
 * @param createdAt   timestamp of creation
 * @param updatedAt   timestamp of last state-mutating behavior
 */
public record EpisodeResponse(
    UUID id,
    int number,
    String title,
    LocalDate airDate,
    AbstractView theAbstract,
    List<UUID> presenters,
    List<UUID> speakers,
    EpisodeStatus status,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Inline view of the submitted abstract.
     *
     * @param id          the abstract's id
     * @param text        the body
     * @param submittedAt when the abstract was first submitted
     */
    public record AbstractView(UUID id, String text, Instant submittedAt) {
    }
}
