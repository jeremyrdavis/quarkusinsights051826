package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.application.RatingService;
import io.arrogantprogrammer.quarkusinsights.engagement.application.SubmitRatingCommand;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Rating;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingNotFound;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.RatingRepository;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import io.arrogantprogrammer.quarkusinsights.shared.RatingId;
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
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Rating aggregate in the Engagement bounded context.
 *
 * <p>Wires HTTP requests to {@link RatingService} (the application-layer
 * service for the Rating aggregate) and translates the post-mutation Rating
 * state into a flat {@link RatingResponse} for clients. Each method is
 * {@code @Transactional} so the service call and the subsequent
 * read-after-write {@code findById} share a single Hibernate session.
 *
 * <p>Endpoint table:
 * <ul>
 *   <li>POST /api/ratings — submit a new rating; returns 201 + Location, or 409
 *       if the author has already rated this episode (§3.6 rule)</li>
 *   <li>GET /api/ratings/{id} — load a rating by id; returns 200 or 404</li>
 *   <li>GET /api/ratings/by-episode/{episodeId} — list all ratings for an episode
 *       in chronological order</li>
 * </ul>
 *
 * <p>Domain exceptions thrown by the service propagate out of these methods
 * and are translated to HTTP statuses by the {@code ExceptionMapper}s in this
 * package (404, 409).
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@Path("/api/ratings")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RatingResource {

    @Inject RatingService ratingService;
    @Inject RatingRepository ratingRepository;
    @Inject RatingResponseMapper responseMapper;

    /**
     * Submits a new Rating on a published episode.
     *
     * @param request the submit request containing episodeId, authorHandle, and stars
     * @return 201 Created with Location header and Rating body, or 409 Conflict
     *         if the author has already rated this episode
     */
    @POST
    @Transactional
    public Response submit(SubmitRatingRequest request) {
        SubmitRatingCommand cmd = new SubmitRatingCommand(
            new EpisodeId(request.episodeId()),
            new AuthorHandle(request.authorHandle()),
            new Stars(request.stars())
        );
        RatingId id = ratingService.submit(cmd);
        Rating created = loadOrThrow(id);
        return Response.created(URI.create("/api/ratings/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Loads a Rating by id.
     *
     * @param id the rating id
     * @return 200 with Rating body, or 404 if not found
     */
    @GET
    @Path("/{id}")
    @Transactional
    public RatingResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new RatingId(id)));
    }

    /**
     * Lists all Ratings for a given episode in chronological order.
     *
     * @param episodeId the episode id to filter by
     * @return 200 with list of Ratings (may be empty)
     */
    @GET
    @Path("/by-episode/{episodeId}")
    @Transactional
    public List<RatingResponse> listByEpisode(@PathParam("episodeId") UUID episodeId) {
        return ratingRepository.findByEpisode(new EpisodeId(episodeId))
            .stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    private Rating loadOrThrow(RatingId id) {
        return ratingRepository.findById(id)
            .orElseThrow(() -> new RatingNotFound(id));
    }
}
