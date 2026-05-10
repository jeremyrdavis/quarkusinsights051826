package io.arrogantprogrammer.quarkusinsights.people.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.domain.PersonNotFound;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Maps {@link PersonNotFound} to HTTP 404 Not Found.
 *
 * <p>Part of the People bounded context, interfaces layer.
 */
@Provider
public class PersonNotFoundMapper implements ExceptionMapper<PersonNotFound> {

    @Override
    public Response toResponse(PersonNotFound exception) {
        return Response.status(Response.Status.NOT_FOUND)
            .type(MediaType.APPLICATION_JSON)
            .entity(Map.of(
                "error", "PersonNotFound",
                "message", exception.getMessage()
            ))
            .build();
    }
}
