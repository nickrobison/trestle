package com.nickrobison.trestle.server.modules;

import com.nickrobison.trestle.datasets.CensusCounty;
import com.nickrobison.trestle.datasets.CensusState;
import com.nickrobison.trestle.datasets.GAULObject;
import com.nickrobison.trestle.datasets.TigerCountyObject;
import com.nickrobison.trestle.reasoner.TrestleBuilder;
import com.nickrobison.trestle.reasoner.TrestleReasoner;
import com.nickrobison.trestle.server.config.TrestleReasonerConfiguration;
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
public class ManagedReasoner implements Managed {

  private static final Logger logger = LoggerFactory.getLogger(ManagedReasoner.class);

  private final TrestleReasoner reasoner;

  @Inject
  public ManagedReasoner(TrestleServerConfiguration configuration) {
    this.reasoner = initializeReasoner((configuration.getReasonerConfig()));
  }

  public TrestleReasoner getReasoner() {
    return reasoner;
  }

  @Override
  public void start() {
    // Not used
  }

  @Override
  public void stop() {
    logger.info("Shutting down reasoner");
    this.reasoner.shutdown(false);
  }

  private static TrestleReasoner initializeReasoner(TrestleReasonerConfiguration configuration) {
    logger.info("Starting Trestle Reasoner");
    final TrestleReasoner reasoner = new TrestleBuilder()
      .withDBConnection(configuration.getConnectionString(),
        configuration.getUsername(),
        configuration.getPassword())
      .withName(configuration.getOntology())
      .withPrefix(configuration.getPrefix())
      .withOntology(configuration.getLocation())
      .withInputClasses(GAULObject.class, TigerCountyObject.class, CensusState.class, CensusCounty.class)
//                .withoutMetrics()
      .build();

    logger.info("Reasoner started");

//        Enable GeoSPARQL
//        FIXME(nickrobison): This needs to come out, it's a patch to get the demos working. Needs TRESTLE-694
    reasoner.getUnderlyingOntology().executeUpdateSPARQL("PREFIX : <http://www.ontotext.com/plugins/geosparql#>\n" +
      "\n" +
      "INSERT DATA {\n" +
      "  _:s :enabled \"true\" .\n" +
      "}");

    return reasoner;
  }
}
