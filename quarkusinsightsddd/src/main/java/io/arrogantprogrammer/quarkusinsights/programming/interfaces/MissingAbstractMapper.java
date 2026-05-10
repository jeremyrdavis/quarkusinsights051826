package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.MissingAbstract;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link MissingAbstract} to HTTP 409 Conflict.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Provider
public class MissingAbstractMapper implements ExceptionMapper<MissingAbstract> {

    @Override
    public Response toResponse(MissingAbstract exception) {
        return Response.status(Response.Status.CONFLICT)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "MissingAbstract",
                "message", exception.getMessage()
            ))
            .build();
    }
}
