package net.tirasa.batch.rest.poc.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.tirasa.batch.rest.poc.api.RootService;
import net.tirasa.batch.rest.poc.batch.BatchConstants;
import net.tirasa.batch.rest.poc.batch.BatchPayloadGenerator;
import net.tirasa.batch.rest.poc.batch.BatchPayloadParser;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import net.tirasa.batch.rest.poc.batch.BatchResponseItem;
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

    private static final Map<String, List<BatchResponseItem>> RESULTS = Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private Bus bus;

    @Context
    private MessageContext mc;

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    private DestinationRegistry getDestinationRegistryFromBusOrDefault(final String transportId) {
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
        // parse Content-Type, expect appropriate boundary
        MediaType mediaType = MediaType.valueOf(mc.getHttpServletRequest().getContentType());
        String boundary = mediaType.getParameters().get(BatchConstants.BOUNDARY_PARAMETER);

        // parse batch request
        List<BatchRequestItem> batchRequestItems;
        try {
            batchRequestItems = BatchPayloadParser.parse(input, mediaType, new BatchRequestItem());
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }

        // prepare for batch processing
        BatchProcess batchProcess = new BatchProcess(
                batchRequestItems,
                boundary,
                getDestinationRegistryFromBusOrDefault(
                        mc.getServletConfig().getInitParameter(CXFNonSpringServlet.TRANSPORT_ID)),
                mc.getServletConfig(),
                mc.getHttpServletRequest());

        RESULTS.put(boundary, Collections.emptyList());

        // manage synchronous Vs asynchronous batch processing
        String prefer = mc.getHttpHeaders().getHeaderString(BatchConstants.PREFER_HEADER);
        if (BatchConstants.PREFERENCE_RESPOND_ASYNC.equals(prefer)) {
            // wait a bit, just for easier tests
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }

            pool.submit(batchProcess);
            return Response.accepted().
                    header(BatchConstants.PREFERENCE_APPLIED_HEADER, BatchConstants.PREFERENCE_RESPOND_ASYNC).
                    type(BatchConstants.multipartMixedWith(boundary)).
                    build();
        } else {
            batchProcess.run();
            return batch();
        }
    }

    @Override
    public Response batch() {
        MediaType mediaType = MediaType.valueOf(mc.getHttpServletRequest().getContentType());
        String boundary = mediaType.getParameters().get(BatchConstants.BOUNDARY_PARAMETER);

        if (RESULTS.containsKey(boundary)) {
            List<BatchResponseItem> batchResponseItems = RESULTS.get(boundary);
            if (batchResponseItems.isEmpty()) {
                return Response.accepted().
                        type(BatchConstants.multipartMixedWith(boundary)).
                        build();
            }

            RESULTS.remove(boundary);
            return Response.ok(
                    BatchPayloadGenerator.generate(batchResponseItems, BatchConstants.DOUBLE_DASH + boundary)).
                    type(BatchConstants.multipartMixedWith(boundary)).
                    build();
        }

        throw new NotFoundException(boundary);
    }

    private static class BatchProcess implements Runnable {

        private final List<BatchRequestItem> batchRequestItems;

        private final String boundary;

        private final DestinationRegistry destinationRegistry;

        private final ServletConfig servletConfig;

        private final HttpServletRequest servletRequest;

        public BatchProcess(
                final List<BatchRequestItem> batchRequestItems,
                final String boundary,
                final DestinationRegistry destinationRegistry,
                final ServletConfig servletConfig,
                final HttpServletRequest servletRequest) {

            this.batchRequestItems = batchRequestItems;
            this.boundary = boundary;
            this.destinationRegistry = destinationRegistry;
            this.servletConfig = servletConfig;
            this.servletRequest = servletRequest;
        }

        @Override
        public void run() {
            List<BatchResponseItem> batchResponseItems = new ArrayList<>(batchRequestItems.size());

            batchRequestItems.forEach((BatchRequestItem reqItem) -> {
                LOG.debug("Batch item:\n{}", reqItem);

                AbstractHTTPDestination dest =
                        destinationRegistry.getDestinationForPath(reqItem.getRequestURI(), true);
                if (dest == null) {
                    dest = destinationRegistry.checkRestfulRequest(reqItem.getRequestURI());
                }
                LOG.debug("Destination found for {}: {}", reqItem.getRequestURI(), dest);

                if (dest == null) {
                    BatchResponseItem resItem = new BatchResponseItem();
                    resItem.setStatus(404);
                    batchResponseItems.add(resItem);
                } else {
                    BatchItemRequest request = new BatchItemRequest(servletRequest, reqItem);
                    BatchItemResponse response = new BatchItemResponse();
                    try {
                        dest.invoke(
                                servletConfig,
                                servletConfig.getServletContext(),
                                request,
                                response);

                        LOG.debug("Returned:\nstatus: {}\nheaders: {}\nbody:\n{}",
                                response.getStatus(),
                                response.getHeaders(),
                                new String(response.getUnderlyingOutputStream().toByteArray()));

                        BatchResponseItem resItem = new BatchResponseItem();
                        resItem.setStatus(response.getStatus());
                        resItem.setHeaders(response.getHeaders());

                        String output = new String(response.getUnderlyingOutputStream().toByteArray());
                        if (output.length() > 0) {
                            resItem.setContent(output);
                        }
                        batchResponseItems.add(resItem);
                    } catch (IOException e) {
                        LOG.error("Invocation of {} failed", dest.getPath(), e);

                        BatchResponseItem resItem = new BatchResponseItem();
                        resItem.setStatus(404);
                        batchResponseItems.add(resItem);
                    }
                }
            });

            RESULTS.put(boundary, batchResponseItems);
        }
    }
}
