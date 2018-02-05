package com.nickrobison.trestle.server.models;

import io.dropwizard.hibernate.AbstractDAO;
import org.hibernate.SessionFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

/**
 * Created by nickrobison on 2/5/18.
 */
@Singleton
public class UIErrorDAO extends AbstractDAO<UIError> {

    @Inject
    public UIErrorDAO(SessionFactory factory) {
        super(factory);
    }

    public UUID create(UIError error) {
        return this.persist(error).getId();
    }
}
