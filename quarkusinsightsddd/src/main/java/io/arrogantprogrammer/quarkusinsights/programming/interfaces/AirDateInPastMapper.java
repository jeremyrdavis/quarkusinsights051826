package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link AirDateInPast} to HTTP 400 Bad Request.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Provider
public class AirDateInPastMapper implements ExceptionMapper<AirDateInPast> {

    @Override
    public Response toResponse(AirDateInPast exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "AirDateInPast",
                "message", exception.getMessage()
            ))
            .build();
    }
}
