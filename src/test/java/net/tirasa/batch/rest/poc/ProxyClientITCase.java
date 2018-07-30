package net.tirasa.batch.rest.poc;

import net.tirasa.batch.rest.poc.batch.client.BatchClientFactoryBean;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import javax.ws.rs.core.MediaType;
import net.tirasa.batch.rest.poc.api.UserService;
import net.tirasa.batch.rest.poc.batch.BatchPayloadGenerator;
import net.tirasa.batch.rest.poc.data.User;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyClientITCase extends AbstractITCase {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyClientITCase.class);

    @Override
    protected String requestBody(final String boundary) {
        BatchClientFactoryBean bcfb = new BatchClientFactoryBean();
        bcfb.setAddress(BASE_ADDRESS);
        bcfb.setProvider(new JacksonJaxbJsonProvider());

        bcfb.setServiceClass(UserService.class);
        UserService service = bcfb.create(UserService.class);

        Client client = WebClient.client(service);
        client.type(MediaType.APPLICATION_XML).accept(MediaType.APPLICATION_XML);

        User toCreate = toCreate();
        service.create(toCreate);

        client.reset();
        client.type(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        User toUpdate = toUpdate();
        service.replace(toCreate.getId(), toUpdate);

        service.delete(toCreate.getId());

        service.delete(RandomStringUtils.random(10));

        service.delete(toUpdate.getId());

        String body = BatchPayloadGenerator.generate(bcfb.getBatchRequestItems(), boundary);
        LOG.debug("Batch request body:\n{}", body);

        return body;
    }
}
