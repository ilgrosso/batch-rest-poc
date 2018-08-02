package net.tirasa.batch.rest.poc.impl;

import net.tirasa.batch.rest.poc.batch.server.BatchProcess;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RootServiceImpl implements RootService {

    private static final String DEFAULT_TRANSPORT_ID = "http://cxf.apache.org/transports/http/configuration";

    private static final ExecutorService POOL = Executors.newFixedThreadPool(10);

    private static final Map<String, Future<List<BatchResponseItem>>> FUTURES =
            Collections.synchronizedMap(new HashMap<>());

    @Autowired
    private Bus bus;

    @Context
    private UriInfo uriInfo;

    @Context
    private MessageContext mc;

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
        if (FUTURES.containsKey(boundary)) {
            throw new IllegalArgumentException("Boundary " + boundary + " already processing");
        }

        // parse batch request
        List<BatchRequestItem> batchRequestItems;
        try {
            batchRequestItems = BatchPayloadParser.parse(input, mediaType, new BatchRequestItem());
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }

        // prepare for batch processing
        BatchProcess batchProcess = new BatchProcess(
                uriInfo.getBaseUri().toASCIIString(),
                batchRequestItems,
                getDestinationRegistryFromBusOrDefault(
                        mc.getServletConfig().getInitParameter(CXFNonSpringServlet.TRANSPORT_ID)),
                mc.getServletConfig(),
                mc.getHttpServletRequest());

        // manage synchronous Vs asynchronous batch processing
        String prefer = mc.getHttpHeaders().getHeaderString(BatchConstants.PREFER_HEADER);
        if (BatchConstants.PREFERENCE_RESPOND_ASYNC.equals(prefer)) {
            // wait a bit, just for easier tests
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
            }

            FUTURES.put(boundary, POOL.submit(batchProcess));

            return Response.accepted().
                    header(BatchConstants.PREFERENCE_APPLIED_HEADER, BatchConstants.PREFERENCE_RESPOND_ASYNC).
                    header(HttpHeaders.LOCATION, uriInfo.getAbsolutePathBuilder().build()).
                    type(BatchConstants.multipartMixedWith(boundary)).
                    build();
        } else {
            FUTURES.put(boundary, CompletableFuture.completedFuture(batchProcess.call()));
            return batch();
        }
    }

    @Override
    public Response batch() {
        MediaType mediaType = MediaType.valueOf(mc.getHttpServletRequest().getContentType());
        String boundary = mediaType.getParameters().get(BatchConstants.BOUNDARY_PARAMETER);

        if (FUTURES.containsKey(boundary)) {
            Future<List<BatchResponseItem>> future = FUTURES.get(boundary);
            if (future.isDone()) {
                try {
                    return Response.ok(
                            BatchPayloadGenerator.generate(future.get(), BatchConstants.DOUBLE_DASH + boundary)).
                            type(BatchConstants.multipartMixedWith(boundary)).
                            build();
                } catch (Exception e) {
                    throw new InternalServerErrorException(e);
                } finally {
                    FUTURES.remove(boundary);
                }
            } else {
                return Response.accepted().
                        type(BatchConstants.multipartMixedWith(boundary)).
                        header(HttpHeaders.RETRY_AFTER, 5).
                        header(HttpHeaders.LOCATION, uriInfo.getAbsolutePathBuilder().build()).
                        build();
            }
        }

        throw new NotFoundException(boundary);
    }
}
