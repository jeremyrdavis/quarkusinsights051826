package io.arrogantprogrammer.quarkusinsights.engagement.interfaces;

import io.arrogantprogrammer.quarkusinsights.engagement.application.CommentService;
import io.arrogantprogrammer.quarkusinsights.engagement.application.EditCommentCommand;
import io.arrogantprogrammer.quarkusinsights.engagement.application.SubmitCommentCommand;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentNotFound;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.shared.CommentId;
import io.arrogantprogrammer.quarkusinsights.shared.EpisodeId;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST adapter for the Comment aggregate in the Engagement bounded context.
 *
 * <p>Wires HTTP requests to {@link CommentService} (the application-layer
 * service for the Comment aggregate) and translates the post-mutation Comment
 * state into a flat {@link CommentResponse} for clients. Each method is
 * {@code @Transactional} so the service call and the subsequent
 * read-after-write {@code findById} share a single Hibernate session.
 *
 * <p>Endpoint table:
 * <ul>
 *   <li>POST /api/comments — submit a new comment; returns 201 + Location</li>
 *   <li>GET /api/comments/{id} — load a comment by id; returns 200 or 404</li>
 *   <li>PUT /api/comments/{id} — edit a comment within the 5-minute window;
 *       returns 200 or 409 if window expired</li>
 *   <li>GET /api/comments/by-episode/{episodeId} — list all comments for an
 *       episode in chronological order</li>
 * </ul>
 *
 * <p>Domain exceptions thrown by the service or the aggregate propagate out
 * of these methods and are translated to HTTP statuses by the
 * {@code ExceptionMapper}s in this package (404, 409).
 *
 * <p>Part of the Engagement bounded context, interfaces layer.
 */
@Path("/api/comments")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentResource {

    @Inject CommentService commentService;
    @Inject CommentRepository commentRepository;
    @Inject CommentResponseMapper responseMapper;

    /**
     * Submits a new Comment on a published episode.
     *
     * @param request the submit request containing episodeId, authorHandle, and body
     * @return 201 Created with Location header and Comment body
     */
    @POST
    @Transactional
    public Response submit(SubmitCommentRequest request) {
        SubmitCommentCommand cmd = new SubmitCommentCommand(
            new EpisodeId(request.episodeId()),
            new AuthorHandle(request.authorHandle()),
            new CommentBody(request.body())
        );
        CommentId id = commentService.submit(cmd);
        Comment created = loadOrThrow(id);
        return Response.created(URI.create("/api/comments/" + id.value()))
            .entity(responseMapper.toResponse(created))
            .build();
    }

    /**
     * Loads a Comment by id.
     *
     * @param id the comment id
     * @return 200 with Comment body, or 404 if not found
     */
    @GET
    @Path("/{id}")
    @Transactional
    public CommentResponse get(@PathParam("id") UUID id) {
        return responseMapper.toResponse(loadOrThrow(new CommentId(id)));
    }

    /**
     * Edits a Comment's body within the 5-minute edit window.
     *
     * @param id      the comment id
     * @param request the edit request containing the new body
     * @return 200 with updated Comment body, or 404/409 on error
     */
    @PUT
    @Path("/{id}")
    @Transactional
    public CommentResponse edit(@PathParam("id") UUID id, EditCommentRequest request) {
        CommentId commentId = new CommentId(id);
        commentService.edit(new EditCommentCommand(commentId, new CommentBody(request.body())));
        return responseMapper.toResponse(loadOrThrow(commentId));
    }

    /**
     * Lists all Comments for a given episode in chronological order.
     *
     * @param episodeId the episode id to filter by
     * @return 200 with list of Comments (may be empty)
     */
    @GET
    @Path("/by-episode/{episodeId}")
    @Transactional
    public List<CommentResponse> listByEpisode(@PathParam("episodeId") UUID episodeId) {
        return commentRepository.findByEpisode(new EpisodeId(episodeId))
            .stream()
            .map(responseMapper::toResponse)
            .toList();
    }

    private Comment loadOrThrow(CommentId id) {
        return commentRepository.findById(id)
            .orElseThrow(() -> new CommentNotFound(id));
    }
}
