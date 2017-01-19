package com.nickrobison.trestle.server;

import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.nickrobison.trestle.server.modules.HibernateModule;
import com.nickrobison.trestle.server.modules.TrestleServerModule;
import io.dropwizard.Application;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServer extends Application<TrestleServerConfiguration> {

//    //        Hibernate
//    private final HibernateBundle<TrestleServerConfiguration> hibernate = new HibernateBundle<TrestleServerConfiguration>(User.class) {
//
//        @Override
//        public PooledDataSourceFactory getDataSourceFactory(TrestleServerConfiguration configuration) {
//            return configuration.getDataSourceFactory();
//        }
//    };
    private final MigrationsBundle<TrestleServerConfiguration> migrations = new MigrationsBundle<TrestleServerConfiguration>() {
        @Override
        public PooledDataSourceFactory getDataSourceFactory(TrestleServerConfiguration configuration) {
            return configuration.getDataSourceFactory();
        }
    };

    public static void main(String[] args) throws Exception {
        new TrestleServer().run(args);
    }

    @Override
    public String getName() {
        return "trestle-server";
    }

    @Override
    public void initialize(Bootstrap<TrestleServerConfiguration> bootstrap) {
        bootstrap.addBundle(new FileAssetsBundle("src/main/resources/build/", "/static", "index.html"));
//        bootstrap.addBundle(hibernate);
        bootstrap.addBundle(migrations);

        final GuiceBundle<TrestleServerConfiguration> guiceBundle = GuiceBundle.<TrestleServerConfiguration>newBuilder()
                .addModule(new TrestleServerModule())
                .addModule(new HibernateModule())
                .setConfigClass(TrestleServerConfiguration.class)
                .enableAutoConfig(getClass().getPackage().getName())
//                .setInjectorFactory((stage, modules) -> LifecycleInjector.builder()
//                        .inStage(stage)
//                        .withModules(modules)
//                        .build()
//                        .createInjector())
                .build();

        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public void run(TrestleServerConfiguration trestleServerConfiguration, Environment environment) throws Exception {

//        final UserDAO userDAO = new UserDAO(hibernate.getSessionFactory());
//        environment.jersey().register(new User);
    }
}
