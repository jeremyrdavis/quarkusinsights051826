package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.application.AssignPresenterCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignSpeakerCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.CancelEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.ComposeEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeListRow;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeService;
import io.arrogantprogrammer.quarkusinsights.programming.application.GoLiveCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.PublishEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.ScheduleEpisodeCommand;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
 * REST adapter for the Programming bounded context.
 *
 * <p>Wires HTTP requests to {@link EpisodeService} (the application-
 * layer service for the Episode aggregate) and translates the
 * post-mutation Episode state into a flat {@link EpisodeResponse}
 * for clients. Each method is {@code @Transactional} so the service
 * call and the subsequent read-after-write {@code findById} share a
 * single Hibernate session.
 *
 * <p>Endpoint table:
 * <ul>
 *   <li>POST /api/episodes — schedule</li>
 *   <li>GET /api/episodes/{id} — load</li>
 *   <li>POST /api/episodes/{id}/abstract — submit abstract</li>
 *   <li>POST /api/episodes/{id}/presenters — assign presenter</li>
 *   <li>POST /api/episodes/{id}/speakers — assign speaker</li>
 *   <li>POST /api/episodes/{id}/go-live — go live</li>
 *   <li>POST /api/episodes/{id}/publish — publish</li>
 *   <li>POST /api/episodes/{id}/cancel — cancel</li>
 * </ul>
 *
 * <p>Domain exceptions thrown by the service or the aggregate
 * propagate out of these methods and are translated to HTTP statuses
 * by the {@code ExceptionMapper}s in this package (400, 404, 409).
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Path("/api/episodes")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EpisodeResource {

    @Inject EpisodeService episodeService;
    @Inject EpisodeRepository repository;
    @Inject EpisodeQueries episodeQueries;
    @Inject EpisodeResponseMapper responseMapper;

    /**
     * Schedules a new Episode.
     *
     * @param request the schedule request
     * @return 201 Created with Location header
     */
    @POST
    @Transactional
    public Response schedule(ScheduleEpisodeRequest request) {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(request.number()),
            new EpisodeTitle(request.title()),
            new AirDate(request.airDate())
        );
        EpisodeId id = episodeService.schedule(cmd);
        Episode created = loadOrThrow(id);
        return Response.created(URI.create("/api/episodes/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Composes a complete Episode (schedule + abstract + presenters +
     * speakers) in a single transactional step.
     *
     * @param request the compose request
     * @return 201 Created with Location header
     */
    @POST
    @Path("/compose")
    @Transactional
    public Response compose(ComposeEpisodeRequest request) {
        ComposeEpisodeCommand cmd = new ComposeEpisodeCommand(
            new EpisodeNumber(request.number()),
            new EpisodeTitle(request.title()),
            new AirDate(request.airDate()),
            new AbstractText(request.abstractText()),
            request.presenterIds().stream().map(PersonId::new).collect(java.util.stream.Collectors.toSet()),
            request.speakerIds().stream().map(PersonId::new).collect(java.util.stream.Collectors.toSet())
        );
        EpisodeId id = episodeService.compose(cmd);
        Episode created = loadOrThrow(id);
        return Response.created(URI.create("/api/episodes/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Loads an Episode.
     */
    @GET
    @Path("/{id}")
    @Transactional
    public EpisodeResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new EpisodeId(id)));
    }

    /**
     * Returns a paginated list of Episode list rows for admin use.
     *
     * @param page   zero-based page index (default 0)
     * @param size   maximum page size (default 20)
     * @param status optional status filter; null returns all statuses
     * @return 200 with the list of rows and a total-count header
     */
    @GET
    @Transactional
    public Response list(
        @QueryParam("page") @DefaultValue("0") int page,
        @QueryParam("size") @DefaultValue("20") int size,
        @QueryParam("status") EpisodeStatus status
    ) {
        Optional<EpisodeStatus> filter = Optional.ofNullable(status);
        List<EpisodeListRow> rows = episodeQueries.listAll(page, size, filter);
        long total = episodeQueries.countAll(filter);
        return Response.ok(rows).header("X-Total-Count", total).build();
    }

    /**
     * Submits the Episode's abstract.
     */
    @POST
    @Path("/{id}/abstract")
    @Transactional
    public EpisodeResponse submitAbstract(@PathParam("id") UUID id, SubmitAbstractRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.submitAbstract(new SubmitAbstractCommand(episodeId, new AbstractText(request.text())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Assigns a presenter.
     */
    @POST
    @Path("/{id}/presenters")
    @Transactional
    public EpisodeResponse assignPresenter(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.assignPresenter(new AssignPresenterCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Assigns a speaker.
     */
    @POST
    @Path("/{id}/speakers")
    @Transactional
    public EpisodeResponse assignSpeaker(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.assignSpeaker(new AssignSpeakerCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Transitions the Episode to LIVE.
     */
    @POST
    @Path("/{id}/go-live")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse goLive(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.goLive(new GoLiveCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Publishes the Episode.
     */
    @POST
    @Path("/{id}/publish")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse publish(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.publish(new PublishEpisodeCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Cancels the Episode.
     */
    @POST
    @Path("/{id}/cancel")
    @Transactional
    public EpisodeResponse cancel(@PathParam("id") UUID id, CancelRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        episodeService.cancel(new CancelEpisodeCommand(episodeId, request.reason()));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    private Episode loadOrThrow(EpisodeId id) {
        return repository.findById(id).orElseThrow(() -> new EpisodeNotFound(id));
    }
}
