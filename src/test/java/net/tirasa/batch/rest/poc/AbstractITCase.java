package net.tirasa.batch.rest.poc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import net.tirasa.batch.rest.poc.batch.BatchConstants;
import net.tirasa.batch.rest.poc.batch.BatchPayloadParser;
import net.tirasa.batch.rest.poc.batch.BatchResponseItem;
import net.tirasa.batch.rest.poc.data.User;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.Test;

public abstract class AbstractITCase {

    protected static final String BASE_ADDRESS = "http://localhost:8080";

    protected static final String BATCH_ADDRESS = BASE_ADDRESS + "/batch";

    protected User toCreate() {
        User toCreate = new User();
        toCreate.setId("xxxyyy");
        toCreate.getAttributes().put("firstname", "John");
        toCreate.getAttributes().put("surname", "Doe");

        return toCreate;
    }

    protected User toUpdate() {
        User toUpdate = new User();
        toUpdate.setId("yuyueeee");
        toUpdate.getAttributes().put("firstname", "Johnny");
        toUpdate.getAttributes().put("surname", "Doe");

        return toUpdate;
    }

    protected void check(final Response response) throws IOException {
        String body = IOUtils.toString((InputStream) response.getEntity());

        List<BatchResponseItem> resItems = BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem());
        assertEquals(5, resItems.size());
        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.get(0).getStatus());
        assertNotNull(resItems.get(0).getHeaders().get(HttpHeaders.LOCATION));
        assertEquals(MediaType.APPLICATION_XML, resItems.get(0).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(Response.Status.OK.getStatusCode(), resItems.get(1).getStatus());
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(1).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(2).getStatus());
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(3).getStatus());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resItems.get(4).getStatus());
    }

    protected abstract String requestBody(String boundary) throws JAXBException, JsonProcessingException;

    @Test
    public void sync() throws IOException, JAXBException {
        String boundary = "--batch_" + UUID.randomUUID().toString();

        Response response = WebClient.create(BATCH_ADDRESS).
                type(BatchConstants.multipartMixedWith(boundary.substring(2))).
                post(requestBody(boundary));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(BatchConstants.multipartMixedWith(boundary.substring(2)), response.getMediaType().toString());

        check(response);
    }

    @Test
    public void async() throws IOException, JAXBException {
        String boundary = "--batch_" + UUID.randomUUID().toString();

        // request async processing
        Response response = WebClient.create(BATCH_ADDRESS).
                type(BatchConstants.multipartMixedWith(boundary.substring(2))).
                header(BatchConstants.PREFER_HEADER, BatchConstants.PREFERENCE_RESPOND_ASYNC).
                post(requestBody(boundary));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(BatchConstants.multipartMixedWith(boundary.substring(2)), response.getMediaType().toString());
        assertEquals(
                BatchConstants.PREFERENCE_RESPOND_ASYNC,
                response.getHeaderString(BatchConstants.PREFERENCE_APPLIED_HEADER));
        assertNotNull(response.getLocation());

        // check results: still work in progress...
        WebClient statusClient = WebClient.create(response.getLocation()).
                type(BatchConstants.multipartMixedWith(boundary.substring(2)));
        response = statusClient.get();
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
        assertEquals(BatchConstants.multipartMixedWith(boundary.substring(2)), response.getMediaType().toString());
        assertNotNull(response.getLocation());
        assertEquals("5", response.getHeaderString(HttpHeaders.RETRY_AFTER));

        // wait a bit...
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
        }

        // check results: now available
        response = statusClient.get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(BatchConstants.multipartMixedWith(boundary.substring(2)), response.getMediaType().toString());

        check(response);

        // check again results: removed since they were returned above
        response = statusClient.get();
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }
}
