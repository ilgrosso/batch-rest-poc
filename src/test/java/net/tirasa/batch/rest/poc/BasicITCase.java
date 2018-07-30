package net.tirasa.batch.rest.poc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.tirasa.batch.rest.poc.batch.BatchPayloadGenerator;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import net.tirasa.batch.rest.poc.data.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicITCase extends AbstractITCase {

    private static final Logger LOG = LoggerFactory.getLogger(BasicITCase.class);

    @Override
    protected String requestBody(final String boundary) throws JAXBException, JsonProcessingException {
        JAXBContext context = JAXBContext.newInstance(User.class);
        Marshaller marshaller = context.createMarshaller();

        List<BatchRequestItem> reqItems = new ArrayList<>();

        StringWriter writer = new StringWriter();
        marshaller.marshal(toCreate(), writer);
        String createPayload = writer.toString();

        BatchRequestItem create = new BatchRequestItem();
        create.setMethod(HttpMethod.POST);
        create.setRequestURI("/users");
        create.setHeaders(new HashMap<>());
        create.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_XML));
        create.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_XML));
        create.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList(createPayload.length()));
        create.setContent(createPayload);
        reqItems.add(create);

        String updatePayload = new ObjectMapper().writeValueAsString(toUpdate());

        BatchRequestItem update = new BatchRequestItem();
        update.setMethod(HttpMethod.PUT);
        update.setRequestURI("/users/xxxyyy");
        update.setHeaders(new HashMap<>());
        update.getHeaders().put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_JSON));
        update.getHeaders().put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_JSON));
        update.getHeaders().put(HttpHeaders.CONTENT_LENGTH, Arrays.asList(updatePayload.length()));
        update.setContent(updatePayload);
        reqItems.add(update);

        BatchRequestItem delete1 = new BatchRequestItem();
        delete1.setMethod(HttpMethod.DELETE);
        delete1.setRequestURI("/roles/xxxyyy");
        reqItems.add(delete1);

        BatchRequestItem delete2 = new BatchRequestItem();
        delete2.setMethod(HttpMethod.DELETE);
        delete2.setRequestURI("/users/xxxyyy");
        reqItems.add(delete2);

        BatchRequestItem delete3 = new BatchRequestItem();
        delete3.setMethod(HttpMethod.DELETE);
        delete3.setRequestURI("/users/yuyueeee");
        reqItems.add(delete3);

        String body = BatchPayloadGenerator.generate(reqItems, boundary);
        LOG.debug("Batch request body:\n{}", body);

        return body;
    }
}
