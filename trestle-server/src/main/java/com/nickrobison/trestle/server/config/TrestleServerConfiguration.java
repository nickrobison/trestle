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

    @NotEmpty
    private String connectionString;
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotEmpty
    private String ontology;
    @NotEmpty
    private String prefix;
    @Valid
    @NotNull
    @JsonProperty
    private DataSourceFactory database = new DataSourceFactory();
    @Valid
    @NotNull
    @JsonProperty
    private JWTConfig jwt;

    @JsonProperty
    public String getConnectionString() {
        return connectionString;
    }

    @JsonProperty
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    @JsonProperty
    public String getUsername() {
        return username;
    }

    @JsonProperty
    public void setUsername(String username) {
        this.username = username;
    }

    @JsonProperty
    public String getPassword() {
        return password;
    }

    @JsonProperty
    public void setPassword(String password) {
        this.password = password;
    }

    @JsonProperty
    public String getOntology() {
        return this.ontology;
    }

    @JsonProperty
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @JsonProperty
    public String getPrefix() {
        return this.prefix;
    }

    @JsonProperty
    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return this.database;
    }

    @JsonProperty("jwt")
    public JWTConfig getJwtConfig() {
        return this.jwt;
    }
}
