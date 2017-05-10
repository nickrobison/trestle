package com.nickrobison.trestle.server;

import com.github.dirkraft.dropwizard.fileassets.FileAssetsBundle;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.nickrobison.trestle.server.auth.AuthDynamicFeature;
import com.nickrobison.trestle.server.auth.AuthValueFactoryProvider;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import com.nickrobison.trestle.server.modules.HibernateModule;
import com.nickrobison.trestle.server.modules.JWTModule;
import com.nickrobison.trestle.server.modules.TrestleServerModule;
import io.dropwizard.Application;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import io.dropwizard.db.PooledDataSourceFactory;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.stream.Stream;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServer extends Application<TrestleServerConfiguration> {
    private static final Logger logger = LoggerFactory.getLogger(TrestleServer.class);

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
                .addModule(new JWTModule())
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
        final JerseyEnvironment jersey = environment.jersey();
        Stream.of(
                new AuthDynamicFeature(),
                new AuthValueFactoryProvider.Binder()).forEach(jersey::register);

        //    database migration?
//        final ManagedDataSource migrationDataSource = createMigrationDataSource(trestleServerConfiguration, environment);
//        try (Connection connection = migrationDataSource.getConnection()) {
//            final JdbcConnection conn = new JdbcConnection(connection);
//            final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(conn);
//            final Liquibase liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database);
//            liquibase.update("");
//            logger.info("Migration complete");
//        } catch(Exception ex) {
//            throw new IllegalStateException("Unable to migrate database", ex);
//        } finally {
//            try {
//                migrationDataSource.stop();
//            } catch (Exception ex) {
//                logger.error("Unable to stop migration data source", ex);
//            }
//        }
    }

    private ManagedDataSource createMigrationDataSource(TrestleServerConfiguration trestleServerConfiguration, Environment environment) {
        final DataSourceFactory dataSourceFactory = trestleServerConfiguration.getDataSourceFactory();
            return dataSourceFactory.build(environment.metrics(), "migration-ds");
    }
}
