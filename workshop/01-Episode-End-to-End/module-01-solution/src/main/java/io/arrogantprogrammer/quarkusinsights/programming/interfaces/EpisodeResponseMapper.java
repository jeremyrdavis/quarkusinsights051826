package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Translates {@link Episode} aggregates into wire-friendly
 * {@link EpisodeResponse}s for HTTP responses. Unwraps domain VOs
 * so no domain types leak across the JSON boundary.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@ApplicationScoped
public class EpisodeResponseMapper {

    /**
     * Builds a flat {@link EpisodeResponse} from an Episode.
     *
     * @param episode the source aggregate
     * @return the wire-shaped response
     */
    public EpisodeResponse toResponse(Episode episode) {
        EpisodeResponse.AbstractView abstractView = null;
        if (episode.theAbstract() != null) {
            abstractView = new EpisodeResponse.AbstractView(
                episode.theAbstract().id().value(),
                episode.theAbstract().text().value(),
                episode.theAbstract().submittedAt()
            );
        }
        List<java.util.UUID> presenters = episode.presenters().stream()
            .map(PersonId::value).sorted().toList();
        List<java.util.UUID> speakers = episode.speakers().stream()
            .map(PersonId::value).sorted().toList();
        return new EpisodeResponse(
            episode.id().value(),
            episode.number().value(),
            episode.title().value(),
            episode.airDate().value(),
            abstractView,
            presenters,
            speakers,
            episode.status(),
            episode.createdAt(),
            episode.updatedAt()
        );
    }
}
