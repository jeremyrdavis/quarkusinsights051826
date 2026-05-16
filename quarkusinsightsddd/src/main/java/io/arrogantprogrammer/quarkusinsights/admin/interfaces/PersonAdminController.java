package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonService;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary;
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
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin pages for the Person aggregate.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET /admin/people} — paginated list, optionally filtered by search query</li>
 *   <li>{@code GET /admin/people/new} — register form</li>
 *   <li>{@code POST /admin/people/new} — handle registration</li>
 *   <li>{@code GET /admin/people/{id}} — detail with inline edit forms</li>
 *   <li>{@code POST /admin/people/{id}/name} — rename</li>
 *   <li>{@code POST /admin/people/{id}/bio} — update biography</li>
 *   <li>{@code POST /admin/people/{id}/socials} — update social links</li>
 * </ul>
 *
 * <p>Edit form submissions accept {@code application/x-www-form-urlencoded}
 * and redirect (303 See Other) back to the detail page on success. On
 * validation failure (any domain VO constructor or
 * {@code IllegalArgumentException}) the form is re-rendered with the
 * user's inputs preserved and an inline error message.
 *
 * <p>Part of the Admin presentation layer.
 */
@Path("/admin/people")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class PersonAdminController {

    @Inject PersonService personService;
    @Inject PersonQueries personQueries;
    @Inject PersonRepository personRepository;

    @Inject @Location("admin/people/list") Template list;
    @Inject @Location("admin/people/new") Template newForm;
    @Inject @Location("admin/people/detail") Template detail;

    @GET
    @Transactional
    public TemplateInstance list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @QueryParam("search") String search
    ) {
        Optional<String> filter = Optional.ofNullable(search).filter(s -> !s.isBlank());
        List<PersonSummary> results = personQueries.search(filter, page, size);
        long total = personQueries.countAll(filter);
        long pageCount = Math.max(1, (total + size - 1) / size);
        return list
            .data("active", "people")
            .data("results", results)
            .data("total", total)
            .data("page", page)
            .data("size", size)
            .data("pageCount", pageCount)
            .data("search", filter.orElse(""))
            .data("pickerMode", false)
            .data("pickerTarget", "");
    }

    @GET
    @Path("/new")
    @Transactional
    public TemplateInstance newForm() {
        return newForm.data("active", "people");
    }

    @POST
    @Path("/new")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response register(
        @FormParam("firstName") String firstName,
        @FormParam("lastName") String lastName,
        @FormParam("email") String email,
        @FormParam("bio") String bio
    ) {
        try {
            PersonId id = personService.register(new RegisterPersonCommand(
                new PersonName(firstName, lastName),
                new Email(email),
                new Bio(bio)
            ));
            return Response.seeOther(URI.create("/admin/people/" + id.value())).build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(newForm
                    .data("active", "people")
                    .data("error", e.getMessage())
                    .data("firstName", firstName)
                    .data("lastName", lastName)
                    .data("email", email)
                    .data("bio", bio))
                .build();
        }
    }

    @GET
    @Path("/{id}")
    @Transactional
    public TemplateInstance get(@PathParam("id") UUID id) {
        Person person = loadOrThrow(new PersonId(id));
        return detail.data("active", "people").data("person", person);
    }

    @POST
    @Path("/{id}/name")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response rename(
        @PathParam("id") UUID id,
        @FormParam("firstName") String firstName,
        @FormParam("lastName") String lastName
    ) {
        PersonId personId = new PersonId(id);
        try {
            personService.rename(new RenamePersonCommand(personId, new PersonName(firstName, lastName)));
            return Response.seeOther(URI.create("/admin/people/" + id)).build();
        } catch (IllegalArgumentException e) {
            return renderDetailWithError(personId, e.getMessage());
        }
    }

    @POST
    @Path("/{id}/bio")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateBio(
        @PathParam("id") UUID id,
        @FormParam("bio") String bio
    ) {
        PersonId personId = new PersonId(id);
        try {
            personService.updateBio(new UpdateBioCommand(personId, new Bio(bio)));
            return Response.seeOther(URI.create("/admin/people/" + id)).build();
        } catch (IllegalArgumentException e) {
            return renderDetailWithError(personId, e.getMessage());
        }
    }

    @POST
    @Path("/{id}/socials")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateSocials(
        @PathParam("id") UUID id,
        @FormParam("twitter") String twitter,
        @FormParam("linkedin") String linkedin,
        @FormParam("website") String website
    ) {
        PersonId personId = new PersonId(id);
        try {
            SocialLinks socials = new SocialLinks(
                toUri(twitter),
                toUri(linkedin),
                toUri(website)
            );
            personService.updateSocials(new UpdateSocialsCommand(personId, socials));
            return Response.seeOther(URI.create("/admin/people/" + id)).build();
        } catch (IllegalArgumentException | URISyntaxException e) {
            return renderDetailWithError(personId, e.getMessage());
        }
    }

    private Optional<URI> toUri(String value) throws URISyntaxException {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new URI(value.trim()));
    }

    private Response renderDetailWithError(PersonId id, String message) {
        Person person = loadOrThrow(id);
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(detail
                .data("active", "people")
                .data("person", person)
                .data("error", message))
            .build();
    }

    private Person loadOrThrow(PersonId id) {
        return personRepository.findById(id).orElseThrow(() -> new PersonNotFound(id));
    }
}
