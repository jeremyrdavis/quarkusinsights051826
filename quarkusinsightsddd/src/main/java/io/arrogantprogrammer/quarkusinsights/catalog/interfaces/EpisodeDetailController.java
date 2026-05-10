package io.arrogantprogrammer.quarkusinsights.catalog.interfaces;

import io.arrogantprogrammer.quarkusinsights.catalog.application.PublicCatalogQueries;
import io.arrogantprogrammer.quarkusinsights.catalog.domain.PublicEpisodeView;
import io.arrogantprogrammer.quarkusinsights.engagement.application.CommentService;
import io.arrogantprogrammer.quarkusinsights.engagement.application.RatingService;
import io.arrogantprogrammer.quarkusinsights.engagement.application.SubmitCommentCommand;
import io.arrogantprogrammer.quarkusinsights.engagement.application.SubmitRatingCommand;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.AuthorHandle;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Comment;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentBody;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.CommentRepository;
import io.arrogantprogrammer.quarkusinsights.engagement.domain.Stars;
import io.arrogantprogrammer.quarkusinsights.programming.domain.EpisodeNumber;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Public episode-detail page + HTMX endpoints for posting comments and
 * ratings.
 *
 * <p>The HTMX endpoints are the documented exception to the
 * "no cross-context service calls" rule (design D8): the catalog OWNS
 * the public UI surface and delegates writes to the engagement context's
 * services. The catalog imports
 * {@link io.arrogantprogrammer.quarkusinsights.engagement.application.CommentService}
 * and
 * {@link io.arrogantprogrammer.quarkusinsights.engagement.application.RatingService}.
 *
 * <p>Part of the Public Catalog bounded context, interfaces layer.
 */
@Path("/episodes/{number}")
@ApplicationScoped
public class EpisodeDetailController {

    @Inject PublicCatalogQueries queries;
    @Inject CommentService commentService;
    @Inject RatingService ratingService;
    @Inject CommentRepository commentRepository;

    @Location("episode-detail.html")
    @Inject Template episodeDetail;

    @Location("comment-fragment.html")
    @Inject Template commentFragment;

    @Location("rating-summary-fragment.html")
    @Inject Template ratingSummaryFragment;

    /**
     * GET /episodes/{number} — render the episode detail page.
     *
     * @param number the episode number from the path
     * @return the rendered episode detail page
     * @throws NotFoundException if no episode with the given number exists
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance get(@PathParam("number") int number) {
        PublicEpisodeView view = queries.findByNumber(new EpisodeNumber(number))
            .orElseThrow(() -> new NotFoundException("Episode " + number + " not found"));
        var comments = commentRepository.findByEpisode(view.id());
        return episodeDetail.data("episode", view, "comments", comments);
    }

    /**
     * POST /episodes/{number}/comments — submit a comment, return HTML
     * fragment for HTMX swap.
     *
     * @param number      the episode number from the path
     * @param authorHandle the commenter's handle
     * @param body        the comment text
     * @return a rendered comment fragment for HTMX beforeend insertion
     * @throws NotFoundException if no episode with the given number exists
     */
    @POST
    @Path("/comments")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance submitComment(@PathParam("number") int number,
                                          @FormParam("authorHandle") String authorHandle,
                                          @FormParam("body") String body) {
        PublicEpisodeView view = queries.findByNumber(new EpisodeNumber(number))
            .orElseThrow(() -> new NotFoundException("Episode " + number + " not found"));
        var commentId = commentService.submit(new SubmitCommentCommand(
            view.id(), new AuthorHandle(authorHandle), new CommentBody(body)));
        Comment posted = commentRepository.findById(commentId).orElseThrow();
        return commentFragment.data("c", posted);
    }

    /**
     * POST /episodes/{number}/ratings — submit a rating, return HTML
     * fragment with updated count and average.
     *
     * @param number      the episode number from the path
     * @param authorHandle the rater's handle
     * @param stars       the star rating (1–5)
     * @return a rendered rating-summary fragment for HTMX outerHTML swap
     * @throws NotFoundException if no episode with the given number exists
     */
    @POST
    @Path("/ratings")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    @Transactional
    public TemplateInstance submitRating(@PathParam("number") int number,
                                         @FormParam("authorHandle") String authorHandle,
                                         @FormParam("stars") int stars) {
        PublicEpisodeView view = queries.findByNumber(new EpisodeNumber(number))
            .orElseThrow(() -> new NotFoundException("Episode " + number + " not found"));
        ratingService.submit(new SubmitRatingCommand(
            view.id(), new AuthorHandle(authorHandle), new Stars(stars)));
        // Re-query for updated counts (the projector has fired by now since same TX or near-same)
        PublicEpisodeView updated = queries.findByNumber(new EpisodeNumber(number)).orElseThrow();
        return ratingSummaryFragment.data("episode", updated);
    }
}
