package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignPresenterCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignSpeakerCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.CancelEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.ComposeEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeListRow;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeService;
import io.arrogantprogrammer.quarkusinsights.programming.application.GoLiveCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.PublishEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.SubmitAbstractCommand;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeStatus;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
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
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Admin pages for the Episode aggregate.
 *
 * <p>Routes:
 * <ul>
 *   <li>{@code GET /admin/episodes} — paginated list, optional status filter</li>
 *   <li>{@code GET /admin/episodes/new} — single-page compose form</li>
 *   <li>{@code POST /admin/episodes/new} — handle composition</li>
 *   <li>{@code GET /admin/episodes/{id}} — admin detail with lifecycle actions</li>
 *   <li>{@code POST /admin/episodes/{id}/go-live} — transition to LIVE</li>
 *   <li>{@code POST /admin/episodes/{id}/publish} — transition to PUBLISHED</li>
 *   <li>{@code POST /admin/episodes/{id}/cancel} — transition to CANCELED</li>
 *   <li>{@code POST /admin/episodes/{id}/abstract} — replace abstract</li>
 *   <li>{@code POST /admin/episodes/{id}/presenters} — add a presenter</li>
 *   <li>{@code POST /admin/episodes/{id}/speakers} — add a speaker</li>
 * </ul>
 *
 * <p>Part of the Admin presentation layer.
 */
@Path("/admin/episodes")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class EpisodeAdminController {

    @Inject EpisodeService episodeService;
    @Inject EpisodeRepository episodeRepository;
    @Inject EpisodeQueries episodeQueries;
    @Inject PersonQueries personQueries;

    @Inject @Location("admin/episodes/list") Template list;
    @Inject @Location("admin/episodes/compose") Template compose;
    @Inject @Location("admin/episodes/detail") Template detail;

    /** GET /admin/episodes — paginated list with optional status filter. */
    @GET
    @Transactional
    public TemplateInstance list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @QueryParam("status") EpisodeStatus status
    ) {
        Optional<EpisodeStatus> filter = Optional.ofNullable(status);
        List<EpisodeListRow> rows = episodeQueries.listAll(page, size, filter);
        long total = episodeQueries.countAll(filter);
        long pageCount = Math.max(1, (total + size - 1) / size);
        return list
            .data("active", "episodes")
            .data("rows", rows)
            .data("total", total)
            .data("page", page)
            .data("size", size)
            .data("pageCount", pageCount)
            .data("statusFilter", filter.map(Enum::name).orElse(""));
    }

    /** GET /admin/episodes/new — compose form. */
    @GET
    @Path("/new")
    @Transactional
    public TemplateInstance newForm() {
        return compose
            .data("active", "episodes")
            .data("today", LocalDate.now().toString())
            .data("presenterChips", List.<PersonSummary>of())
            .data("speakerChips", List.<PersonSummary>of());
    }

    /**
     * POST /admin/episodes/new — handle composition. Form encodes
     * presenter and speaker IDs as repeated {@code presenterIds} and
     * {@code speakerIds} parameters.
     */
    @POST
    @Path("/new")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response composeEpisode(
        @FormParam("number") Integer number,
        @FormParam("title") String title,
        @FormParam("airDate") String airDateStr,
        @FormParam("abstractText") String abstractText,
        @FormParam("presenterIds") List<String> presenterIdStrings,
        @FormParam("speakerIds") List<String> speakerIdStrings
    ) {
        try {
            Set<PersonId> presenters = parsePersonIds(presenterIdStrings);
            Set<PersonId> speakers = parsePersonIds(speakerIdStrings);
            ComposeEpisodeCommand cmd = new ComposeEpisodeCommand(
                new EpisodeNumber(number == null ? 0 : number),
                new EpisodeTitle(title == null ? "" : title),
                new AirDate(LocalDate.parse(airDateStr)),
                new AbstractText(abstractText == null ? "" : abstractText),
                presenters,
                speakers
            );
            EpisodeId id = episodeService.compose(cmd);
            return Response.seeOther(URI.create("/admin/episodes/" + id.value())).build();
        } catch (RuntimeException e) {
            Set<PersonSummary> presenterChips = resolvePeople(presenterIdStrings);
            Set<PersonSummary> speakerChips = resolvePeople(speakerIdStrings);
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(compose
                    .data("active", "episodes")
                    .data("today", LocalDate.now().toString())
                    .data("error", e.getMessage())
                    .data("number", number)
                    .data("title", title)
                    .data("airDate", airDateStr)
                    .data("abstractText", abstractText)
                    .data("presenterChips", presenterChips)
                    .data("speakerChips", speakerChips))
                .build();
        }
    }

    /** GET /admin/episodes/{id} — detail with lifecycle actions. */
    @GET
    @Path("/{id}")
    @Transactional
    public TemplateInstance get(@PathParam("id") UUID id) {
        Episode episode = loadOrThrow(new EpisodeId(id));
        Map<UUID, String> personNames = resolveNames(episode.presenters(), episode.speakers());
        return detail
            .data("active", "episodes")
            .data("episode", episode)
            .data("personNames", personNames);
    }

    /** POST /admin/episodes/{id}/go-live */
    @POST
    @Path("/{id}/go-live")
    @Transactional
    public Response goLive(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.goLive(new GoLiveCommand(episodeId));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    /** POST /admin/episodes/{id}/publish */
    @POST
    @Path("/{id}/publish")
    @Transactional
    public Response publish(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.publish(new PublishEpisodeCommand(episodeId));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    /** POST /admin/episodes/{id}/cancel */
    @POST
    @Path("/{id}/cancel")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response cancel(
        @PathParam("id") UUID id,
        @FormParam("reason") String reason
    ) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.cancel(new CancelEpisodeCommand(episodeId, reason == null ? "" : reason));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    /** POST /admin/episodes/{id}/abstract — replace the abstract. */
    @POST
    @Path("/{id}/abstract")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response submitAbstract(
        @PathParam("id") UUID id,
        @FormParam("abstractText") String text
    ) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.submitAbstract(new SubmitAbstractCommand(episodeId, new AbstractText(text == null ? "" : text)));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    /** POST /admin/episodes/{id}/presenters — add a presenter. */
    @POST
    @Path("/{id}/presenters")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addPresenter(
        @PathParam("id") UUID id,
        @FormParam("personId") String personIdStr
    ) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.assignPresenter(new AssignPresenterCommand(
                episodeId, new PersonId(UUID.fromString(personIdStr))));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    /** POST /admin/episodes/{id}/speakers — add a speaker. */
    @POST
    @Path("/{id}/speakers")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response addSpeaker(
        @PathParam("id") UUID id,
        @FormParam("personId") String personIdStr
    ) {
        EpisodeId episodeId = new EpisodeId(id);
        try {
            episodeService.assignSpeaker(new AssignSpeakerCommand(
                episodeId, new PersonId(UUID.fromString(personIdStr))));
            return Response.seeOther(URI.create("/admin/episodes/" + id)).build();
        } catch (RuntimeException e) {
            return renderDetailWithError(episodeId, e.getMessage());
        }
    }

    private Episode loadOrThrow(EpisodeId id) {
        return episodeRepository.findById(id).orElseThrow(() -> new EpisodeNotFound(id));
    }

    private Response renderDetailWithError(EpisodeId id, String message) {
        Episode episode = loadOrThrow(id);
        Map<UUID, String> personNames = resolveNames(episode.presenters(), episode.speakers());
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(detail
                .data("active", "episodes")
                .data("episode", episode)
                .data("personNames", personNames)
                .data("error", message))
            .build();
    }

    private Set<PersonId> parsePersonIds(List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
            .filter(s -> s != null && !s.isBlank())
            .map(s -> new PersonId(UUID.fromString(s.trim())))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<PersonSummary> resolvePeople(List<String> ids) {
        if (ids == null) {
            return Set.of();
        }
        Set<PersonSummary> resolved = new LinkedHashSet<>();
        for (String idStr : ids) {
            if (idStr == null || idStr.isBlank()) continue;
            try {
                PersonId pid = new PersonId(UUID.fromString(idStr.trim()));
                personQueries.findById(pid).ifPresent(resolved::add);
            } catch (IllegalArgumentException ignored) {
                // skip unparseable ids in the chip rehydration path
            }
        }
        return resolved;
    }

    private Map<UUID, String> resolveNames(Set<PersonId> presenters, Set<PersonId> speakers) {
        Map<UUID, String> names = new HashMap<>();
        for (PersonId pid : presenters) {
            personQueries.findById(pid).ifPresent(p ->
                names.put(pid.value(), p.displayName()));
        }
        for (PersonId pid : speakers) {
            personQueries.findById(pid).ifPresent(p ->
                names.put(pid.value(), p.displayName()));
        }
        return names;
    }
}
