package com.nickrobison.trestle.server.models;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 1/18/17.
 */
@Singleton
public class UserDAO extends AbstractDAO<User> {

    @Inject
    public UserDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<User> findById(long id) {
        return Optional.ofNullable(get(id));
    }

    public long create(User user) {
        return persist(user).getId();
    }

    public List<User> findAll() {
        return list(namedQuery("com.nickrobison.trestle.server.queries.User.findAll"));
    }

    public List<User> findByname(String name) {
        StringBuilder builder = new StringBuilder("%");
        builder.append(name).append("%");
        return list(namedQuery("com.nickrobison.trestle.server.queries.User.findByName")
        .setParameter("name", builder.toString())
        );
    }
}
