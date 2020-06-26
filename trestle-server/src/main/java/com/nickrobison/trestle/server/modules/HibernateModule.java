package com.nickrobison.trestle.server.modules;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.hibernate.SessionFactoryFactory;
import org.hibernate.SessionFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;
import java.security.ProviderException;

/**
 * Created by nrobison on 1/18/17.
 */
public class HibernateModule extends AbstractModule {
    private static final Logger logger = LoggerFactory.getLogger(HibernateModule.class);

    private final TrestleHibernateBundle hibernate;

  public HibernateModule(TrestleHibernateBundle hibernate) {
    this.hibernate = hibernate;
  }

  @Override
    protected void configure() {
        bind(TrestleHibernateBundle.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public SessionFactory sessionFactory(TrestleHibernateBundle h2) {
        final SessionFactory sessionFactory = this.hibernate.getSessionFactory();
        if (sessionFactory == null) {
            throw new ProviderException("Unable to get SessionFactory");
        }
        return sessionFactory;
    }


    @Singleton
    public static class TrestleHibernateBundle extends HibernateBundle<TrestleServerConfiguration> implements ConfiguredBundle<TrestleServerConfiguration> {
        private static final Logger logger = LoggerFactory.getLogger(HibernateBundle.class);
        public static final String ENTITY_PREFIX = "com.nickrobison.trestle.server.models";

        @Inject
        public TrestleHibernateBundle() {
            super(applicationEntities(), new SessionFactoryFactory());
        }

        private static ImmutableList<Class<?>> applicationEntities() {
            logger.info("Scanning prefix {} for Hibernate entities", ENTITY_PREFIX);
            final Reflections reflections = new Reflections(ENTITY_PREFIX);
            final ImmutableList<Class<?>> entities = ImmutableList.copyOf(reflections.getTypesAnnotatedWith(Entity.class));
            logger.info("Founded {} entities in prefix", entities.size());
            return entities;
        }

        @Override
        public PooledDataSourceFactory getDataSourceFactory(TrestleServerConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    }
}

