package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.domain.MissingPresenter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link MissingPresenter} to HTTP 409 Conflict.
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Provider
public class MissingPresenterMapper implements ExceptionMapper<MissingPresenter> {

    @Override
    public Response toResponse(MissingPresenter exception) {
        return Response.status(Response.Status.CONFLICT)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "MissingPresenter",
                "message", exception.getMessage()
            ))
            .build();
    }
}
