package com.nickrobison.trestle.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by nrobison on 11/28/16.
 */
public class TrestleServerConfiguration extends Configuration {

    @NotEmpty
    private String connectionString;
    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

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
}
