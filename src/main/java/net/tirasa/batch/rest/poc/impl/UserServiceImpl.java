package net.tirasa.batch.rest.poc.impl;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import net.tirasa.batch.rest.poc.api.UserService;
import net.tirasa.batch.rest.poc.data.User;

public class UserServiceImpl implements UserService {

    private static final Map<String, User> USERS = Collections.synchronizedMap(new HashMap<>());

    @Context
    private UriInfo uriInfo;

    @Override
    public List<User> list() {
        return USERS.values().stream().collect(Collectors.toList());
    }

    @Override
    public User read(final String id) {
        User user = USERS.get(id);
        if (user == null) {
            throw new NotFoundException(id);
        }

        return user;
    }

    @Override
    public Response create(final User user) {
        if (user.getId() == null) {
            throw new BadRequestException("Missing user id");
        }

        USERS.put(user.getId(), user);

        URI location = uriInfo.getAbsolutePathBuilder().path(user.getId()).build();
        return Response.created(location).entity(user).build();
    }

    @Override
    public Response replace(final String id, final User replacement) {
        User user = USERS.remove(id);
        if (user == null) {
            throw new NotFoundException(id);
        }

        USERS.put(replacement.getId(), replacement);
        return Response.ok(replacement).build();
    }

    @Override
    public void delete(final String id) {
        User user = USERS.remove(id);
        if (user == null) {
            throw new NotFoundException(id);
        }
    }
}
