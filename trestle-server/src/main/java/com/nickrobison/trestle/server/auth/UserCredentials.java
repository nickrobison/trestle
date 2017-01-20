package com.nickrobison.trestle.server.auth;

import com.nickrobison.trestle.server.models.User;

import javax.validation.constraints.NotNull;

/**
 * Created by nrobison on 1/20/17.
 */
public class UserCredentials {

    @NotNull
    private String username;
    @NotNull
    private String password;

    public UserCredentials() {}

    public UserCredentials(String user, String password) {
        this.username = username;
        this.password = password;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserCredentials that = (UserCredentials) o;

        if (!getUsername().equals(that.getUsername())) return false;
        return getPassword().equals(that.getPassword());
    }

    @Override
    public int hashCode() {
        int result = getUsername().hashCode();
        result = 31 * result + getPassword().hashCode();
        return result;
    }
}
