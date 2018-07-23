package net.tirasa.batch.rest.poc.api;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("")
public interface RootService {

    @POST
    @Path("/batch")
    @Consumes("multipart/mixed")
    @Produces("multipart/mixed")
    Response batch(InputStream input);
}
