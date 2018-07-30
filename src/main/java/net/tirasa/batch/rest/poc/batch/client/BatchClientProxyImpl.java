package net.tirasa.batch.rest.poc.batch.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.tirasa.batch.rest.poc.batch.BatchRequestItem;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.ClientProxyImpl;
import org.apache.cxf.jaxrs.client.ClientState;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.message.Message;

public class BatchClientProxyImpl extends ClientProxyImpl {

    private final BatchClientFactoryBean factory;

    public BatchClientProxyImpl(
            final BatchClientFactoryBean factory,
            final URI baseURI,
            final ClassLoader loader,
            final ClassResourceInfo cri,
            final boolean isRoot,
            final boolean inheritHeaders,
            final Object... varValues) {

        super(baseURI, loader, cri, isRoot, inheritHeaders, varValues);
        this.factory = factory;
    }

    public BatchClientProxyImpl(
            final BatchClientFactoryBean factory,
            final ClientState initialState,
            final ClassLoader loader,
            final ClassResourceInfo cri,
            final boolean isRoot,
            final boolean inheritHeaders,
            final Object... varValues) {

        super(initialState, loader, cri, isRoot, inheritHeaders, varValues);
        this.factory = factory;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Object[] preProcessResult(final Message message) throws Exception {

        BatchRequestItem bri = new BatchRequestItem();
        bri.setMethod((String) message.get(Message.HTTP_REQUEST_METHOD));
        bri.setRequestURI(StringUtils.substringAfter(
                (String) message.getContextualProperty(Message.REQUEST_URI),
                getState().getBaseURI().toASCIIString()));
        bri.setHeaders((Map<String, List<Object>>) message.get(Message.PROTOCOL_HEADERS));

        BatchOfflineHTTPConduit conduit = (BatchOfflineHTTPConduit) message.getExchange().getConduit(message);
        bri.setContent(conduit.getOutputStream().toString(StandardCharsets.UTF_8.name()));

        factory.add(bri);
        return null;
    }

    @Override
    protected Object handleResponse(final Message outMessage, final Class<?> serviceCls) throws Throwable {
        return null;
    }
}
