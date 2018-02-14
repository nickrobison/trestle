package com.nickrobison.trestle.reasoner.engines.object;

import com.nickrobison.trestle.reasoner.parser.IClassBuilder;
import com.nickrobison.trestle.reasoner.parser.IClassParser;
import com.nickrobison.trestle.reasoner.parser.IClassRegister;
import com.nickrobison.trestle.reasoner.threading.TrestleExecutorService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

/**
 * Created by nickrobison on 2/13/18.
 */
public class ObjectEngine {

    private static final Logger logger = LoggerFactory.getLogger(ObjectEngine.class);

    private final IClassParser parser;
    private final IClassRegister classRegistry;
    private final IClassBuilder builder;
//    private final TrestleExecutorService objectThreadPool;

    @Inject
    public ObjectEngine(IClassRegister classRegistry, IClassBuilder builder, IClassParser parser) {
        logger.debug("Initializing Object engine");
        this.parser = parser;
        this.builder = builder;
        this.classRegistry = classRegistry;

        final Config config = ConfigFactory.load().getConfig("trestle");

//        this.objectThreadPool = TrestleExecutorService.executorFactory("object-pool", config.getInt("threading.object-pool.size"), this.metrician);
    }


}
