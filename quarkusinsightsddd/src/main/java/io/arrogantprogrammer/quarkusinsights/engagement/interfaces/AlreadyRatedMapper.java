package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.AlreadyRated;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link AlreadyRated} to HTTP 409 Conflict.
 *
 * <p>The response body includes the {@code existingRatingId} so clients can
 * offer "view your previous rating" UX, directing the user to retrieve and
 * display the already-submitted rating rather than submitting a duplicate.
 *
 * <p>This mapper handles {@link AlreadyRated} regardless of whether it was
 * thrown from the §3.6 precheck path in {@code RatingService.submit} or from
 * the constraint-translation path in {@code RatingRepositoryImpl.save} —
 * both produce the same exception type.
 *
 * <p>Part of the Engagement bounded context, interfaces layer. See SPEC.md §3.6.
 */
@Provider
public class AlreadyRatedMapper implements ExceptionMapper<AlreadyRated> {

    /**
     * Converts an {@link AlreadyRated} exception to a 409 response.
     *
     * @param exception the exception to convert; carries the existing rating's id
     * @return a 409 JSON response including existingRatingId for client navigation
     */
    @Override
    public Response toResponse(AlreadyRated exception) {
        return Response.status(Response.Status.CONFLICT)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "AlreadyRated",
                "message", exception.getMessage(),
                "existingRatingId", exception.existingRatingId().value().toString()
            ))
            .build();
    }
}
