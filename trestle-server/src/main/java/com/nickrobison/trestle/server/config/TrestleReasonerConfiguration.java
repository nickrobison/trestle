package com.nickrobison.trestle.server.config;

import javax.validation.constraints.NotNull;

/**
 * Created by nrobison on 4/22/17.
 */
public class TrestleReasonerConfiguration {
    @NotNull
    private String connectionString;
    @NotNull
    private String username;
    @NotNull
    private String password;
    @NotNull
    private String ontology;
    @NotNull
    private String prefix;
    @NotNull
    private String location;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getOntology() {
        return ontology;
    }

    public void setOntology(String ontology) {
        this.ontology = ontology;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
