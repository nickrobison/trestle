package com.nickrobison.trestle.server;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.nickrobison.trestle.server.auth.AuthModule;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.modules.HibernateModule;
import com.nickrobison.trestle.server.modules.JWTModule;
import com.nickrobison.trestle.server.modules.TrestleServerModule;
import com.nickrobison.trestle.server.serializers.GeometrySerializer;
import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedPooledDataSource;
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
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.GuiceBundle;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.sql.Connection;
import java.util.EnumSet;

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
    final SimpleModule m = new SimpleModule();
    m.addSerializer(Geometry.class, new GeometrySerializer());
    bootstrap.getObjectMapper().registerModule(m);
    bootstrap.addBundle(migrations);

    final HibernateModule.TrestleHibernateBundle hibernateBundle = new HibernateModule.TrestleHibernateBundle();
    bootstrap.addBundle(hibernateBundle);


    final GuiceBundle guiceBundle = GuiceBundle.builder()
      .modules(new JWTModule(), new AuthModule(), new HibernateModule(hibernateBundle), new TrestleServerModule())
      .enableAutoConfig(getClass().getPackage().getName())
      .build();

    bootstrap.addBundle(guiceBundle);

//        Add Swagger
//        bootstrap.addBundle(new SwaggerBundle<TrestleServerConfiguration>() {
//            @Override
//            protected SwaggerBundleConfiguration getSwaggerBundleConfiguration(TrestleServerConfiguration trestleServerConfiguration) {
//                return trestleServerConfiguration.getSwaggerBundleConfiguration();
//            }
//        });
  }

  @Override
  public void run(TrestleServerConfiguration trestleServerConfiguration, Environment environment) throws Exception {
    configureCors(environment);
    final JerseyEnvironment jersey = environment.jersey();
    jersey.register(new AuthValueFactoryProvider.Binder<>(User.class));

    //    database migration?
    final ManagedPooledDataSource migrationDataSource = createMigrationDataSource(trestleServerConfiguration, environment);
    try {
      if (migrationDataSource.getUrl().contains(".//////")) {
        logger.warn("Using local H2 file database, cannot perform migration");
      } else {
        logger.info("Performing Database migration");
        try (Connection connection = migrationDataSource.getConnection()) {
          final JdbcConnection conn = new JdbcConnection(connection);
          final Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(conn);
          try (Liquibase liquibase = new Liquibase("migrations.xml", new ClassLoaderResourceAccessor(), database)) {
            liquibase.update("");
            logger.info("Migration complete");
          }
        } catch (Exception ex) {
          throw new IllegalStateException("Unable to migrate database", ex);
        }
      }
    } finally {
      migrationDataSource.stop();
    }

  }

  private ManagedPooledDataSource createMigrationDataSource(TrestleServerConfiguration trestleServerConfiguration, Environment environment) {
    final DataSourceFactory dataSourceFactory = trestleServerConfiguration.getDataSourceFactory();
    return (ManagedPooledDataSource) dataSourceFactory.build(environment.metrics(), "migration-ds");
  }

  private void configureCors(Environment environment) {
    final FilterRegistration.Dynamic cors =
      environment.servlets().addFilter("CORS", CrossOriginFilter.class);

    // Configure CORS parameters
    cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin,Authorization");
    cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "OPTIONS,GET,PUT,POST,DELETE,HEAD");
    cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, "true");

    // Add URL mapping
    cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

  }
}
