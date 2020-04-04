package com.nickrobison.trestle.ontology;

import java.io.Serializable;

/**
 * Created by nickrobison on 3/29/20.
 */
public class ConnectionProperties implements Serializable {
    public static final long serialVersionUID = 42L;

    private final String connectionString;
    private final String username;
    private final String password;

    public ConnectionProperties(String connectionString, String username, String password) {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
    }

    public String getConnectionString() {
        return connectionString;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
