package com.nickrobison.trestle.server.models;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by nickrobison on 1/29/18.
 */
@Singleton
public class UserExperimentResultDAO extends AbstractDAO<UserExperimentResult> {

    @Inject
    public UserExperimentResultDAO(SessionFactory factory) {
        super(factory);
    }

    public Long create(UserExperimentResult result) {
        return persist(result).getUserId();
    }
}
