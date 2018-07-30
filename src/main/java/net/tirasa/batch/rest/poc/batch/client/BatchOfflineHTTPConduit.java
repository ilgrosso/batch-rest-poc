package net.tirasa.batch.rest.poc.batch.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.Address;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DefaultBasicAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class BatchOfflineHTTPConduit extends HTTPConduit {

    private ByteArrayOutputStream baos;

    public BatchOfflineHTTPConduit(final Bus bus, final EndpointInfo ei) throws IOException {
        this(bus, ei, null);
    }

    public BatchOfflineHTTPConduit(
            final Bus bus,
            final EndpointInfo ei,
            final EndpointReferenceType t) throws IOException {

        super(bus, ei, t);
        this.proxyAuthSupplier = new DefaultBasicAuthSupplier();
        this.proxyAuthorizationPolicy = new ProxyAuthorizationPolicy();
    }

    @Override
    protected void setupConnection(
            final Message message, Address address,
            final HTTPClientPolicy csPolicy) throws IOException {
    }

    @Override
    public HTTPClientPolicy getClient(final Message message) {
        return new HTTPClientPolicy();
    }

    @Override
    protected OutputStream createOutputStream(
            final Message message,
            final boolean needToCacheRequest,
            final boolean isChunking,
            final int chunkThreshold) throws IOException {

        baos = new ByteArrayOutputStream();
        return baos;
    }

    public ByteArrayOutputStream getOutputStream() {
        return baos;
    }
}
