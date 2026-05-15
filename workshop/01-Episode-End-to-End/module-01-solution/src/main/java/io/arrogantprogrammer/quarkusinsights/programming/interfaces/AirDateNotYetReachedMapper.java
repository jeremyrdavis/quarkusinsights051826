package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateNotYetReached;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link AirDateNotYetReached} to HTTP 400 Bad Request.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Provider
public class AirDateNotYetReachedMapper implements ExceptionMapper<AirDateNotYetReached> {

    @Override
    public Response toResponse(AirDateNotYetReached exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "AirDateNotYetReached",
                "message", exception.getMessage()
            ))
            .build();
    }
}
