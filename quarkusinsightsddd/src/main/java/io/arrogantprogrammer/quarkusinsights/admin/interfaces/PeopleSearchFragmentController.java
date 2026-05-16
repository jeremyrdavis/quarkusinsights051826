package io.arrogantprogrammer.quarkusinsights.admin.interfaces;

import io.arrogantprogrammer.quarkusinsights.people.application.PersonQueries;
import io.arrogantprogrammer.quarkusinsights.people.application.PersonSummary;
import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Optional;

/**
 * Returns an HTML fragment of People matching a search query, for HTMX
 * live-search consumers. Shared between the {@code /admin/people} list
 * page and the Episode composition page's presenter/speaker pickers.
 *
 * <p>The {@code pickerMode} flag controls whether each result row
 * carries an "Add" button that adds the person to a target hidden-input
 * group on the calling form. When {@code pickerMode} is false, the
 * results are simple links into each Person's detail page.
 *
 * <p>Part of the Admin presentation layer.
 */
@Path("/admin/fragments/people-search")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class PeopleSearchFragmentController {

    @Inject PersonQueries personQueries;

    @Inject @Location("admin/people/_search-results") Template results;

    /**
     * GET /admin/fragments/people-search?search=&size=&picker=&target=
     *
     * @param search optional substring filter; empty returns the first page of all People
     * @param size   maximum results to return (default 8 — picker-friendly)
     * @param picker whether to render result rows with "Add" buttons
     * @param target if picker is true, the prefix for hidden-input names
     *               (e.g., "presenter" or "speaker") that the Add button
     *               attaches to
     * @return rendered _search-results fragment
     */
    @GET
    @Transactional
    public TemplateInstance fragment(
        @QueryParam("search") String search,
        @QueryParam("size") @DefaultValue("8") int size,
        @QueryParam("picker") @DefaultValue("false") boolean picker,
        @QueryParam("target") @DefaultValue("") String target
    ) {
        Optional<String> filter = Optional.ofNullable(search).filter(s -> !s.isBlank());
        List<PersonSummary> matches = personQueries.search(filter, 0, size);
        long total = personQueries.countAll(filter);
        return results
            .data("results", matches)
            .data("total", total)
            .data("size", size)
            .data("pickerMode", picker)
            .data("pickerTarget", target);
    }
}
