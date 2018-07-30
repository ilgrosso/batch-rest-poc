package net.tirasa.batch.rest.poc.batch.client;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import org.apache.cxf.endpoint.ConduitSelector;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.UpfrontConduitSelector;
import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;

public class BatchClientFactoryBean extends JAXRSClientFactoryBean {

    private final List<BatchRequestItem> batchRequestItems = new ArrayList<>();

    private ClassLoader proxyLoader;

    private boolean inheritHeaders;

    @Override
    public void setClassLoader(final ClassLoader loader) {
        this.proxyLoader = loader;
    }

    @Override
    public void setInheritHeaders(final boolean inheritHeaders) {
        this.inheritHeaders = inheritHeaders;
    }

    @Override
    protected ConduitSelector getConduitSelector(final Endpoint ep) {
        ConduitSelector cs = getConduitSelector();
        if (cs == null) {
            try {
                cs = new UpfrontConduitSelector(new BatchOfflineHTTPConduit(bus, ep.getEndpointInfo()));
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not create " + BatchOfflineHTTPConduit.class.getName(), e);
            }
        }
        cs.setEndpoint(ep);
        return cs;
    }

    @Override
    protected ClientProxyImpl createClientProxy(
            final ClassResourceInfo cri,
            final boolean isRoot,
            final ClientState actualState,
            final Object[] varValues) {

        if (actualState == null) {
            return new BatchClientProxyImpl(
                    this, URI.create(getAddress()), proxyLoader, cri, isRoot, inheritHeaders, varValues);
        } else {
            return new BatchClientProxyImpl(
                    this, actualState, proxyLoader, cri, isRoot, inheritHeaders, varValues);
        }
    }

    public boolean add(final BatchRequestItem item) {
        return this.batchRequestItems.add(item);
    }

    public List<BatchRequestItem> getBatchRequestItems() {
        return batchRequestItems;
    }
}
