package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingNotFound;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link RatingNotFound} to HTTP 404 Not Found.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@Provider
public class RatingNotFoundMapper implements ExceptionMapper<RatingNotFound> {

    /**
     * Converts a {@link RatingNotFound} exception to a 404 response.
     *
     * @param exception the exception to convert
     * @return a 404 JSON response with error and message fields
     */
    @Override
    public Response toResponse(RatingNotFound exception) {
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "RatingNotFound",
                "message", exception.getMessage()
            ))
            .build();
    }
}
