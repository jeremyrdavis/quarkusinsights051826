package io.arrogantprogrammer.quarkusinsights.programming.application;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;

/**
 * Command to submit (or replace) an Episode's abstract.
 *
 * <p>Consumed by {@code SubmitAbstractUseCase}. The use case loads the
 * Episode by id and invokes {@code Episode.submitAbstract(text)}.
 *
 * <p>Part of the Programming bounded context, application layer.
 *
 * @param episodeId the target Episode's id; must not be null
 * @param text      the abstract body; must not be null
 */
public record SubmitAbstractCommand(EpisodeId episodeId, AbstractText text) {

    /**
     * @throws IllegalArgumentException if either component is null
     */
    public SubmitAbstractCommand {
        if (episodeId == null) throw new IllegalArgumentException("episodeId must not be null");
        if (text == null) throw new IllegalArgumentException("text must not be null");
    }
}
