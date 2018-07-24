package net.tirasa.batch.rest.poc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.tirasa.batch.rest.poc.batch.BatchPayloadGenerator;
import net.tirasa.batch.rest.poc.batch.BatchPayloadParser;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import net.tirasa.batch.rest.poc.batch.BatchResponseItem;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicITCase {

    private static final Logger LOG = LoggerFactory.getLogger(BasicITCase.class);

    @Test
    public void basic() throws IOException {
        List<BatchRequestItem> reqItems = new ArrayList<>();

        BatchRequestItem create = new BatchRequestItem();
        create.setMethod(HttpMethod.POST);
        create.setRequestURI("/users");
        create.setHeaders(new HashMap<>());
        create.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_XML));
        create.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList("128"));
        create.setContent("<user>\n"
                + "  <username>xxxyyy</username>\n"
                + "  <attributes>\n"
                + "    <firstname>John</firstname>\n"
                + "    <surname>Doe</surname>\n"
                + "  </attributes>\n"
                + "</user>");
        reqItems.add(create);

        BatchRequestItem update = new BatchRequestItem();
        update.setMethod(HttpMethod.PUT);
        update.setRequestURI("/users/xxxyyy");
        update.setHeaders(new HashMap<>());
        update.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_JSON));
        update.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList("94"));
        update.setContent("{\n"
                + "  \"username\": \"yuyueeee\",\n"
                + "  \"attributes\": [\n"
                + "    \"firstname\": \"Johnny\",\n"
                + "    \"lastname\": \"Doe\"\n"
                + "  ]\n"
                + "}");
        reqItems.add(update);

        BatchRequestItem delete1 = new BatchRequestItem();
        delete1.setMethod(HttpMethod.DELETE);
        delete1.setRequestURI("/roles/xxxyyy");
        reqItems.add(delete1);

        BatchRequestItem delete2 = new BatchRequestItem();
        delete2.setMethod(HttpMethod.DELETE);
        delete2.setRequestURI("/users/xxxyyy");
        reqItems.add(delete2);

        String boundary = "--batch_" + UUID.randomUUID().toString();

        Response response = WebClient.create("http://localhost:8080").path("batch").
                type("multipart/mixed;boundary=" + boundary.substring(2)).
                post(BatchPayloadGenerator.generate(reqItems, boundary));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getMediaType().toString().startsWith("multipart/mixed;boundary="));

        String body = IOUtils.toString((InputStream) response.getEntity());
        LOG.debug("Batch response body:\n{}", body);

        List<BatchResponseItem> resItems = BatchPayloadParser.parse(
                new ByteArrayInputStream(body.getBytes()),
                response.getMediaType(),
                new BatchResponseItem());
        assertEquals(4, resItems.size());
        assertEquals(Response.Status.CREATED.getStatusCode(), resItems.get(0).getStatus());
        assertEquals(MediaType.APPLICATION_XML, resItems.get(0).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(Response.Status.OK.getStatusCode(), resItems.get(1).getStatus());
        assertEquals(MediaType.APPLICATION_JSON, resItems.get(1).getHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), resItems.get(2).getStatus());
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), resItems.get(3).getStatus());
    }
}
