package io.arrogantprogrammer.quarkusinsights.people.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonService;
import io.arrogantprogrammer.quarkusinsights.people.application.RegisterPersonCommand;
import io.arrogantprogrammer.quarkusinsights.people.application.RenamePersonCommand;
import io.arrogantprogrammer.quarkusinsights.people.application.UpdateBioCommand;
import io.arrogantprogrammer.quarkusinsights.people.application.UpdateSocialsCommand;
import io.arrogantprogrammer.quarkusinsights.people.domain.Bio;
import io.arrogantprogrammer.quarkusinsights.people.domain.Email;
import io.arrogantprogrammer.quarkusinsights.people.domain.Person;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonNotFound;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonName;
import io.arrogantprogrammer.quarkusinsights.people.domain.PersonRepository;
import io.arrogantprogrammer.quarkusinsights.people.domain.SocialLinks;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST adapter for the People bounded context.
 *
 * <p>Wires HTTP requests to {@link PersonService} (the application-layer
 * service for the Person aggregate) and translates the post-mutation
 * Person state into a flat {@link PersonResponse} for clients. Each
 * method is {@code @Transactional} so the service call and the subsequent
 * read-after-write share a single Hibernate session.
 *
 * <p>Endpoint table:
 * <ul>
 *   <li>POST /api/people — register</li>
 *   <li>GET /api/people/{id} — load</li>
 *   <li>PUT /api/people/{id}/name — rename</li>
 *   <li>PUT /api/people/{id}/bio — update biography</li>
 *   <li>PUT /api/people/{id}/socials — update social links</li>
 *   <li>GET /api/people?page=&amp;size= — paginated list</li>
 * </ul>
 *
 * <p>Domain exceptions thrown by the service or the aggregate propagate
 * out of these methods and are translated to HTTP statuses by the
 * {@code ExceptionMapper}s in this package and in
 * {@code shared/interfaces/} (400 for VO validation, 404 for
 * PersonNotFound).
 *
 * <p>Part of the People bounded context, interfaces layer.
 */
@Path("/api/people")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {

    @Inject PersonService personService;
    @Inject PersonRepository repository;
    @Inject PersonResponseMapper responseMapper;

    /**
     * Registers a new Person.
     *
     * @param request the registration request
     * @return 201 Created with Location header and the created Person body
     */
    @POST
    @Transactional
    public Response register(RegisterPersonRequest request) {
        RegisterPersonCommand cmd = new RegisterPersonCommand(
            new PersonName(request.name().first(), request.name().last()),
            new Email(request.email()),
            new Bio(request.bio())
        );
        PersonId id = personService.register(cmd);
        Person created = loadOrThrow(id);
        return Response.created(URI.create("/api/people/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Loads a Person by id.
     *
     * @param id the Person's UUID
     * @return 200 with the Person body, or 404 if not found
     */
    @GET
    @Path("/{id}")
    @Transactional
    public PersonResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new PersonId(id)));
    }

    /**
     * Renames an existing Person.
     *
     * @param id      the Person's UUID
     * @param request the rename request
     * @return 200 with the updated Person body
     */
    @PUT
    @Path("/{id}/name")
    @Transactional
    public PersonResponse rename(@PathParam("id") UUID id, RenamePersonRequest request) {
        PersonId personId = new PersonId(id);
        personService.rename(new RenamePersonCommand(
            personId,
            new PersonName(request.name().first(), request.name().last())
        ));
        return responseMapper.toResponse(loadOrThrow(personId));
    }

    /**
     * Updates an existing Person's biography.
     *
     * @param id      the Person's UUID
     * @param request the bio update request
     * @return 200 with the updated Person body
     */
    @PUT
    @Path("/{id}/bio")
    @Transactional
    public PersonResponse updateBio(@PathParam("id") UUID id, UpdateBioRequest request) {
        PersonId personId = new PersonId(id);
        personService.updateBio(new UpdateBioCommand(personId, new Bio(request.text())));
        return responseMapper.toResponse(loadOrThrow(personId));
    }

    /**
     * Updates an existing Person's social links.
     *
     * <p>Null values in the request clear the corresponding link.
     * PUT with all nulls clears all social links.
     *
     * @param id      the Person's UUID
     * @param request the social links update request
     * @return 200 with the updated Person body
     */
    @PUT
    @Path("/{id}/socials")
    @Transactional
    public PersonResponse updateSocials(@PathParam("id") UUID id, UpdateSocialsRequest request) {
        PersonId personId = new PersonId(id);
        SocialLinks socials = new SocialLinks(
            Optional.ofNullable(request.twitter()).map(URI::create),
            Optional.ofNullable(request.linkedin()).map(URI::create),
            Optional.ofNullable(request.website()).map(URI::create)
        );
        personService.updateSocials(new UpdateSocialsCommand(personId, socials));
        return responseMapper.toResponse(loadOrThrow(personId));
    }

    /**
     * Returns a paginated list of Persons, ordered by registration time.
     *
     * @param page zero-based page index (default 0)
     * @param size maximum page size (default 20)
     * @return 200 with a list of PersonResponse
     */
    @GET
    @Transactional
    public List<PersonResponse> getAll(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size
    ) {
        return repository.findAll(page, size).stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    private Person loadOrThrow(PersonId id) {
        return repository.findById(id).orElseThrow(() -> new PersonNotFound(id));
    }
}
