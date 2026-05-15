package io.arrogantprogrammer.quarkusinsights.shared.interfaces;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link IllegalArgumentException} to HTTP 400 Bad Request.
 *
 * <p>Covers validation failures thrown by domain VO constructors
 * (e.g., blank title, negative episode number, null fields, malformed
 * email address). The more-specific domain exceptions
 * (e.g., {@link io.arrogantprogrammer.quarkusinsights.programming.domain.AirDateInPast})
 * have their own mappers which take priority; this mapper acts
 * as a broad catch-all for the remaining VO validation errors across
 * all bounded contexts.
 *
 * <p>Part of the shared kernel's interfaces support — applies automatically
 * to every JAX-RS resource in the application.
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "IllegalArgumentException",
                "message", exception.getMessage()
            ))
            .build();
    }
}
