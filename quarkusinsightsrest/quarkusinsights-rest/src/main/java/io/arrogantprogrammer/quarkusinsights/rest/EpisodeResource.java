package io.arrogantprogrammer.quarkusinsights.rest;

import io.quarkus.logging.Log;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/episodes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EpisodeResource {

    @POST
    @Transactional
    public Response createEpisode(Episode episode, @Context HttpHeaders headers) {
        Log.debugf("Creating episode: %s", episode.getTitle());
        episode.persist();
        if (headers.getHeaderString("HX-Request") != null) {
            return Response.ok("<div>Created episode: " + episode.getTitle() + "</div>").type(MediaType.TEXT_HTML).build();
        }
        return Response.ok(episode).build();
    }

    @GET
    public Response getAllEpisodes() {
        Log.debugf("Retrieved %s episodes", Episode.listAll().size());
        return Response.ok(Episode.listAll()).build();
    }

}
