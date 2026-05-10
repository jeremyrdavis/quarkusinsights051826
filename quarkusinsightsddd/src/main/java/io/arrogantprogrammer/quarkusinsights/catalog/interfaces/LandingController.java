package io.arrogantprogrammer.quarkusinsights.catalog.interfaces;

import io.arrogantprogrammer.quarkusinsights.catalog.application.PublicCatalogQueries;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Public landing page at the application root.
 *
 * <p>Renders the next upcoming episode (most recent SCHEDULED, sorted by
 * air date) using the Qute {@code landing.html} template. If no
 * upcoming episode exists, the template falls through to a "no
 * upcoming episodes" message.
 *
 * <p>Part of the Public Catalog bounded context, interfaces layer.
 */
@Path("/")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class LandingController {

    @Inject PublicCatalogQueries queries;
    @Inject Template landing;

    /**
     * GET / — render the landing page.
     *
     * @return the rendered landing page
     */
    @GET
    public TemplateInstance get() {
        return landing.data("upcoming", queries.findUpcoming().orElse(null));
    }
}
