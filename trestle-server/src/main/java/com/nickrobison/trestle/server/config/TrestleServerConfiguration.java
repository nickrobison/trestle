package com.nickrobison.trestle.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by nrobison on 11/28/16.
 */
@SuppressWarnings({"initialization.fields.uninitialized"})
public class TrestleServerConfiguration extends Configuration {

    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();
    @Valid
    @NotNull
    @JsonProperty
    private JWTConfig jwt;
    @Valid
    @NotNull
    @JsonProperty
    private TrestleReasonerConfiguration reasoner;
    @JsonProperty
    private SwaggerBundleConfiguration swaggerBundleConfiguration;

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return this.database;
    }

    @JsonProperty("jwt")
    public JWTConfig getJwtConfig() {
        return this.jwt;
    }

    @JsonProperty("reasoner")
    public TrestleReasonerConfiguration getReasonerConfig() {
        return this.reasoner;
    }

    @JsonProperty("swagger")
    public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
        return this.swaggerBundleConfiguration;
    }
}
