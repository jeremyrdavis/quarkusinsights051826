package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.domain.EditWindowExpired;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link EditWindowExpired} to HTTP 409 Conflict.
 *
 * <p>The response body includes the {@code commentId} and {@code submittedAt}
 * timestamp so clients can display meaningful information to the user about
 * when the 5-minute window closed.
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@Provider
public class EditWindowExpiredMapper implements ExceptionMapper<EditWindowExpired> {

    /**
     * Converts an {@link EditWindowExpired} exception to a 409 response.
     *
     * @param exception the exception to convert
     * @return a 409 JSON response including commentId and submittedAt
     */
    @Override
    public Response toResponse(EditWindowExpired exception) {
        return Response.status(Response.Status.CONFLICT)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "EditWindowExpired",
                "message", exception.getMessage(),
                "commentId", exception.commentId().value().toString(),
                "submittedAt", exception.submittedAt().toString()
            ))
            .build();
    }
}
