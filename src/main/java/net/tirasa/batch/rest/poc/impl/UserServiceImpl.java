package net.tirasa.batch.rest.poc.impl;

import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import net.tirasa.batch.rest.poc.api.UserService;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserServiceImpl implements UserService {

    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    @Context
    private MessageContext mc;

    @Override
    public Response list() {
        LOG.debug("============> Invoked list()");
        return Response.ok().build();
    }

    @Override
    public Response read(String id) {
        LOG.debug("============> Invoked read(" + id + ")");
        return Response.ok().build();
    }

    @Override
    public Response create(InputStream in) {
        String user = null;
        try {
            user = IOUtils.toString(in);
            LOG.debug("============> Invoked create() with payload {}", user);
        } catch (IOException e) {
            LOG.error("Unexpected", e);
        }

        return Response.status(Response.Status.CREATED).
                entity(user).
                type(mc.getHttpServletRequest().getContentType()).
                build();
    }

    @Override
    public Response update(String id, InputStream in) {
        try {
            LOG.debug("============> Invoked update({}) with payload {}", id, IOUtils.toString(in));
        } catch (IOException e) {
            LOG.error("Unexpected", e);
        }
        return Response.noContent().build();
    }

    @Override
    public Response replace(String id, InputStream in) {
        String user = null;
        try {
            user = IOUtils.toString(in);
            LOG.debug("============> Invoked replace({}) with payload {}", id, user);
        } catch (IOException e) {
            LOG.error("Unexpected", e);
        }

        return Response.ok().
                entity(user).
                type(mc.getHttpServletRequest().getContentType()).
                build();
    }

    @Override
    public Response delete(String id) {
        LOG.debug("============> Invoked delete(" + id + ")");
        return Response.noContent().build();
    }
}
