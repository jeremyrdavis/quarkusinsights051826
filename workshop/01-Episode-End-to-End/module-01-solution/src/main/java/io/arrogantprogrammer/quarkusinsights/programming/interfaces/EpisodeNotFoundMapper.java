package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link EpisodeNotFound} to HTTP 404 Not Found.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Provider
public class EpisodeNotFoundMapper implements ExceptionMapper<EpisodeNotFound> {

    @Override
    public Response toResponse(EpisodeNotFound exception) {
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "EpisodeNotFound",
                "message", exception.getMessage()
            ))
            .build();
    }
}
