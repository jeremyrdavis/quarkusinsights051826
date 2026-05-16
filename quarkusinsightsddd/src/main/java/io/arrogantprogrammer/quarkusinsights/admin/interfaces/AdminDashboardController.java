package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.programming.application.EpisodeQueries;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Optional;

/**
 * Admin dashboard at {@code GET /admin}. Renders a minimal summary of
 * episodes and people counts plus quick-action links into the rest of
 * the admin UI.
 *
 * <p>No authentication is enforced — the admin UI is a maintainer
 * convenience for development and demos, not a production surface.
 *
 * <p>Part of the Admin presentation layer.
 */
@Path("/admin")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class AdminDashboardController {

    @Inject EpisodeQueries episodeQueries;
    @Inject PersonQueries personQueries;

    @Inject
    @Location("admin/dashboard")
    Template dashboard;

    /**
     * GET /admin — render the dashboard.
     *
     * @return the rendered dashboard
     */
    @GET
    @Transactional
    public TemplateInstance get() {
        return dashboard
            .data("active", "dashboard")
            .data("episodeCount", episodeQueries.countAll(Optional.empty()))
            .data("personCount", personQueries.countAll(Optional.empty()));
    }
}
