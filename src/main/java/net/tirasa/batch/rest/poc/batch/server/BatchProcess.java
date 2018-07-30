/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.tirasa.batch.rest.poc.batch.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import net.tirasa.batch.rest.poc.batch.BatchResponseItem;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchProcess implements Callable<List<BatchResponseItem>> {

    private static final Logger LOG = LoggerFactory.getLogger(BatchProcess.class);

    private final List<BatchRequestItem> batchRequestItems;

    private final DestinationRegistry destinationRegistry;

    private final ServletConfig servletConfig;

    private final HttpServletRequest servletRequest;

    public BatchProcess(
            final List<BatchRequestItem> batchRequestItems,
            final DestinationRegistry destinationRegistry,
            final ServletConfig servletConfig,
            final HttpServletRequest servletRequest) {

        this.batchRequestItems = batchRequestItems;
        this.destinationRegistry = destinationRegistry;
        this.servletConfig = servletConfig;
        this.servletRequest = servletRequest;
    }

    @Override
    public List<BatchResponseItem> call() {
        List<BatchResponseItem> batchResponseItems = new ArrayList<>(batchRequestItems.size());

        batchRequestItems.forEach((BatchRequestItem reqItem) -> {
            LOG.debug("Batch item:\n{}", reqItem);
            AbstractHTTPDestination dest = destinationRegistry.getDestinationForPath(reqItem.getRequestURI(), true);
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
                    dest.invoke(servletConfig, servletConfig.getServletContext(), request, response);
                    LOG.debug("Returned:\nstatus: {}\nheaders: {}\nbody:\n{}", response.getStatus(),
                            response.getHeaders(), new String(response.getUnderlyingOutputStream().toByteArray()));
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

        return batchResponseItems;
    }
}
