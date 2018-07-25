package net.tirasa.batch.rest.poc.api;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import net.tirasa.batch.rest.poc.batch.BatchConstants;

@Path("")
public interface RootService {

    @POST
    @Path("/batch")
    @Consumes(BatchConstants.MULTIPART_MIXED)
    @Produces(BatchConstants.MULTIPART_MIXED)
    Response batch(InputStream input);

    @GET
    @Path("/batch")
    @Produces(BatchConstants.MULTIPART_MIXED)
    Response batch();
}
