package com.nickrobison.trestle.server.models;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

/**
 * Created by nrobison on 1/18/17.
 */
// TODO: TRESTLE-748: Move to criteria queries, rather than just strings
@Singleton
public class UserDAO extends AbstractDAO<User> {

    @Inject
    public UserDAO(SessionFactory factory) {
        super(factory);
    }

    public Optional<User> findById(long id) {
        return Optional.ofNullable(get(id));
    }

    public Long create(User user) {
        return persist(user).getId();
    }

    public void deleteUser(long id) {
        namedQuery("com.nickrobison.trestle.server.queries.User.deleteByID")
                .setParameter("id", id).executeUpdate();
    }

    public List<User> findAll() {
        return list((Query<User>) namedQuery("com.nickrobison.trestle.server.queries.User.findAll"));
    }

    public List<User> findByName(String name) {
        return list((Query<User>) namedQuery("com.nickrobison.trestle.server.queries.User.findByName")
        .setParameter("name", "%" + name + "%")
        );
    }

    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(uniqueResult((Query<User>) namedQuery("com.nickrobison.trestle.server.queries.User.findByUsername")
                .setParameter("username", username)));
    }
}
