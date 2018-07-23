package net.tirasa.batch.rest.poc.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.tirasa.batch.rest.poc.api.RootService;
import net.tirasa.batch.rest.poc.batch.BatchItem;
import net.tirasa.batch.rest.poc.batch.BatchPayloadGenerator;
import net.tirasa.batch.rest.poc.batch.BatchPayloadParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RootServiceImpl implements RootService {

    private static final Logger LOG = LoggerFactory.getLogger(RootService.class);

    private static final String DEFAULT_TRANSPORT_ID = "http://cxf.apache.org/transports/http/configuration";

    @Autowired
    private Bus bus;

    @Context
    private MessageContext mc;

    protected DestinationRegistry getDestinationRegistryFromBusOrDefault(final String transportId) {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            String peferredTransportId = transportId;

            // Check if the preferred transport is set on a bus level (f.e., from any extension or customization).
            if (StringUtils.isEmpty(peferredTransportId)) {
                peferredTransportId = (String) bus.getProperty(AbstractTransportFactory.PREFERRED_TRANSPORT_ID);
            }

            if (StringUtils.isEmpty(peferredTransportId)) {
                Set<String> candidates = dfm.getRegisteredDestinationFactoryNames();

                // If the default transport is present, fall back to it and don't even consider other candidates
                if (!candidates.contains(DEFAULT_TRANSPORT_ID)) {
                    peferredTransportId = candidates.stream().
                            filter(name -> name.endsWith("/configuration")).
                            findAny().
                            orElse(DEFAULT_TRANSPORT_ID);
                }
            }

            DestinationFactory df = StringUtils.isEmpty(peferredTransportId)
                    ? dfm.getDestinationFactory(DEFAULT_TRANSPORT_ID)
                    : dfm.getDestinationFactory(peferredTransportId);
            if (df instanceof HTTPTransportFactory) {
                return HTTPTransportFactory.class.cast(df).getRegistry();
            }
        } catch (BusException e) {
            // why are we throwing a busexception if the DF isn't found?
        }
        return null;
    }

    @Override
    public Response batch(final InputStream input) {
        DestinationRegistry destinationRegistry = getDestinationRegistryFromBusOrDefault(
                mc.getServletConfig().getInitParameter(CXFNonSpringServlet.TRANSPORT_ID));

        List<BatchItem> batchRequestItems;
        try {
            batchRequestItems = BatchPayloadParser.parse(
                    input, MediaType.valueOf(mc.getHttpServletRequest().getContentType()));
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }

        Response.ResponseBuilder batchResponse = Response.ok();

        String boundary = "--batch_" + UUID.randomUUID().toString();

        batchResponse.type("multipart/mixed;boundary=" + boundary.substring(2));

        List<BatchItem> batchResponseItems = new ArrayList<>(batchRequestItems.size());
        batchRequestItems.forEach((BatchItem reqItem) -> {
            LOG.debug("Batch item:\n{}", reqItem);

            AbstractHTTPDestination dest =
                    destinationRegistry.getDestinationForPath(reqItem.getRequestURI(), true);
            if (dest == null) {
                dest = destinationRegistry.checkRestfulRequest(reqItem.getRequestURI());
            }
            LOG.debug("Destination found for {}: {}", reqItem.getRequestURI(), dest);

            if (dest == null) {
                BatchItem resItem = new BatchItem();
                resItem.setStatus(404);
                batchResponseItems.add(resItem);
            } else {
                BatchItemRequest request = new BatchItemRequest(mc.getHttpServletRequest(), reqItem);
                BatchItemResponse response = new BatchItemResponse();
                try {
                    dest.invoke(
                            mc.getServletConfig(),
                            mc.getServletConfig().getServletContext(),
                            request,
                            response);

                    LOG.debug("Returned:\nstatus: {}\nheaders:{}\nbody:\n{}",
                            response.getStatus(),
                            response.getHeaders(),
                            new String(response.getUnderlyingOutputStream().toByteArray()));

                    BatchItem resItem = new BatchItem();
                    resItem.setStatus(response.getStatus());
                    resItem.setHeaders(response.getHeaders());

                    String output = new String(response.getUnderlyingOutputStream().toByteArray());
                    if (output.length() > 0) {
                        resItem.setContent(output);
                    }
                    batchResponseItems.add(resItem);
                } catch (IOException e) {
                    LOG.error("Invocation failed", e);

                    BatchItem resItem = new BatchItem();
                    resItem.setStatus(404);
                    batchResponseItems.add(resItem);
                }
            }
        });

        batchResponse.entity(BatchPayloadGenerator.generate(batchResponseItems, boundary));
        return batchResponse.build();
    }
}
