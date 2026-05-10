package io.arrogantprogrammer.quarkusinsights.catalog.interfaces;

import io.arrogantprogrammer.quarkusinsights.catalog.application.PublicCatalogQueries;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

/**
 * Public history list of published episodes.
 *
 * <p>Renders the {@code history.html} template with a paginated list
 * of PUBLISHED episodes, reverse chronological by air date.
 *
 * <p>Part of the Public Catalog bounded context, interfaces layer.
 */
@Path("/episodes")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class HistoryController {

    @Inject PublicCatalogQueries queries;
    @Inject Template history;

    /**
     * GET /episodes?page=&size= — render the history page.
     *
     * @param page zero-based page number (default 0)
     * @param size page size (default 20)
     * @return the rendered history page
     */
    @GET
    public TemplateInstance get(@QueryParam("page") @DefaultValue("0") int page,
                                @QueryParam("size") @DefaultValue("20") int size) {
        return history.data("episodes", queries.findHistory(page, size));
    }
}
