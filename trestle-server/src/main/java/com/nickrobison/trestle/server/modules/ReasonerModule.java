package com.nickrobison.trestle.server.modules;

import com.nickrobison.trestle.TrestleBuilder;
import com.nickrobison.trestle.TrestleReasoner;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by nrobison on 11/28/16.
 */
@Singleton
public class ReasonerModule implements Managed {

    private static final Logger logger = LoggerFactory.getLogger(ReasonerModule.class);

    private TrestleReasoner reasoner;
    private final TrestleServerConfiguration configuration;

    @Inject
    public ReasonerModule(TrestleServerConfiguration configuration) {
        this.configuration = configuration;
    }

    @Inject
    public TrestleReasoner getReasoner() {
        return reasoner;
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting Trestle Reasoner");
        this.reasoner = new TrestleBuilder()
                .withDBConnection(configuration.getConnectionString(),
                        configuration.getUsername(),
                        configuration.getPassword())
                .withName(configuration.getOntology())
                .withPrefix(configuration.getPrefix())
                .withoutCaching()
                .build();

        logger.info("Reasoner started");
    }

    @Override
    public void stop() throws Exception {
        logger.info("Shutting down reasoner");
        this.reasoner.shutdown(false);
    }
}
