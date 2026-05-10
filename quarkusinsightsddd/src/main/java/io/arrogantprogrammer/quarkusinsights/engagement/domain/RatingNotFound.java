package io.arrogantprogrammer.quarkusinsights.engagement.domain;

import io.arrogantprogrammer.quarkusinsights.shared.RatingId;

/**
 * Thrown when a use case attempts to load a Rating aggregate by id
 * and the repository returns empty.
 *
 * <p>This exception is unchecked because not-found from a repository
 * is typically a programmer error or an invalid client request — the
 * interface layer (REST adapter, added in Bundle 2) translates it to a
 * 404 Not Found response.
 *
 * <p>Part of the Engagement bounded context, domain layer.
 */
public class RatingNotFound extends RuntimeException {

    private final RatingId ratingId;

    /**
     * Creates a RatingNotFound exception.
     *
     * @param ratingId the id that was not found
     */
    public RatingNotFound(RatingId ratingId) {
        super("Rating " + ratingId.value() + " not found");
        this.ratingId = ratingId;
    }

    /**
     * @return the id that was not found
     */
    public RatingId ratingId() {
        return ratingId;
    }
}
