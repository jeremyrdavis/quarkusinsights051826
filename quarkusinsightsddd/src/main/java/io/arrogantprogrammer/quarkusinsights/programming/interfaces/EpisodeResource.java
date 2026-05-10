package io.arrogantprogrammer.quarkusinsights.programming.interfaces;

import io.arrogantprogrammer.quarkusinsights.programming.application.AssignPresenterCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignPresenterUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignSpeakerCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.AssignSpeakerUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.CancelEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.CancelEpisodeUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.GoLiveCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.GoLiveUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.PublishEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.PublishEpisodeUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.ScheduleEpisodeCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.ScheduleEpisodeUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.application.SubmitAbstractCommand;
import io.arrogantprogrammer.quarkusinsights.programming.application.SubmitAbstractUseCase;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AbstractText;
import io.arrogantprogrammer.quarkusinsights.programming.domain.AirDate;
import io.arrogantprogrammer.quarkusinsights.programming.domain.Episode;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNotFound;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeRepository;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeTitle;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.PersonId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.UUID;

/**
 * REST adapter for the Programming bounded context.
 *
 * <p>Wires HTTP requests to the seven application-layer use cases and
 * translates the post-mutation Episode state into a flat
 * {@link EpisodeResponse} for clients. Each method is
 * {@code @Transactional} so the use case call and the subsequent
 * read-after-write {@code findById} share a single Hibernate session.
 *
 * <p>Endpoint table:
 * <ul>
 *   <li>POST /episodes — schedule</li>
 *   <li>GET /episodes/{id} — load</li>
 *   <li>POST /episodes/{id}/abstract — submit abstract</li>
 *   <li>POST /episodes/{id}/presenters — assign presenter</li>
 *   <li>POST /episodes/{id}/speakers — assign speaker</li>
 *   <li>POST /episodes/{id}/go-live — go live</li>
 *   <li>POST /episodes/{id}/publish — publish</li>
 *   <li>POST /episodes/{id}/cancel — cancel</li>
 * </ul>
 *
 * <p>Domain exceptions thrown by the use cases or the aggregate
 * propagate out of these methods and are translated to HTTP statuses
 * by the {@code ExceptionMapper}s in this package (400, 404, 409).
 *
 * <p>Part of the Programming bounded context, interfaces layer.
 */
@Path("/episodes")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EpisodeResource {

    @Inject ScheduleEpisodeUseCase scheduleUseCase;
    @Inject SubmitAbstractUseCase submitAbstractUseCase;
    @Inject AssignPresenterUseCase assignPresenterUseCase;
    @Inject AssignSpeakerUseCase assignSpeakerUseCase;
    @Inject GoLiveUseCase goLiveUseCase;
    @Inject PublishEpisodeUseCase publishUseCase;
    @Inject CancelEpisodeUseCase cancelUseCase;
    @Inject EpisodeRepository repository;
    @Inject EpisodeResponseMapper responseMapper;

    /**
     * Schedules a new Episode.
     *
     * @param request the schedule request
     * @return 201 Created with Location header pointing at GET /episodes/{id}
     */
    @POST
    @Transactional
    public Response schedule(ScheduleEpisodeRequest request) {
        ScheduleEpisodeCommand cmd = new ScheduleEpisodeCommand(
            new EpisodeNumber(request.number()),
            new EpisodeTitle(request.title()),
            new AirDate(request.airDate())
        );
        EpisodeId id = scheduleUseCase.handle(cmd);
        Episode created = loadOrThrow(id);
        return Response.created(URI.create("/episodes/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Loads an Episode.
     *
     * @param id the Episode id
     * @return 200 OK with the Episode payload (404 if not found via the ExceptionMapper)
     */
    @GET
    @Path("/{id}")
    @Transactional
    public EpisodeResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new EpisodeId(id)));
    }

    /**
     * Submits the Episode's abstract.
     *
     * @param id      the Episode id
     * @param request the abstract request
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/abstract")
    @Transactional
    public EpisodeResponse submitAbstract(@PathParam("id") UUID id, SubmitAbstractRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        submitAbstractUseCase.handle(new SubmitAbstractCommand(episodeId, new AbstractText(request.text())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Assigns a presenter.
     *
     * @param id      the Episode id
     * @param request the assignment request carrying the PersonId
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/presenters")
    @Transactional
    public EpisodeResponse assignPresenter(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        assignPresenterUseCase.handle(new AssignPresenterCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Assigns a speaker.
     *
     * @param id      the Episode id
     * @param request the assignment request carrying the PersonId
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/speakers")
    @Transactional
    public EpisodeResponse assignSpeaker(@PathParam("id") UUID id, AssignPersonRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        assignSpeakerUseCase.handle(new AssignSpeakerCommand(episodeId, new PersonId(request.personId())));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Transitions the Episode to LIVE.
     *
     * @param id the Episode id
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/go-live")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse goLive(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        goLiveUseCase.handle(new GoLiveCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Publishes the Episode.
     *
     * @param id the Episode id
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/publish")
    @Consumes(MediaType.WILDCARD)
    @Transactional
    public EpisodeResponse publish(@PathParam("id") UUID id) {
        EpisodeId episodeId = new EpisodeId(id);
        publishUseCase.handle(new PublishEpisodeCommand(episodeId));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    /**
     * Cancels the Episode.
     *
     * @param id      the Episode id
     * @param request the cancel request carrying the reason
     * @return 200 OK with the updated Episode payload
     */
    @POST
    @Path("/{id}/cancel")
    @Transactional
    public EpisodeResponse cancel(@PathParam("id") UUID id, CancelRequest request) {
        EpisodeId episodeId = new EpisodeId(id);
        cancelUseCase.handle(new CancelEpisodeCommand(episodeId, request.reason()));
        return responseMapper.toResponse(loadOrThrow(episodeId));
    }

    private Episode loadOrThrow(EpisodeId id) {
        return repository.findById(id).orElseThrow(() -> new EpisodeNotFound(id));
    }
}
