package com.nickrobison.trestle.server.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * Created by nrobison on 11/28/16.
 */
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
}
