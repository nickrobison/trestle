package com.nickrobison.trestle.server.config;

import javax.validation.constraints.NotNull;

/**
 * Created by nrobison on 1/19/17.
 */
@SuppressWarnings({"initialization.fields.uninitialized"})
public class JWTConfig {

    @NotNull
    private String authHeader;
    @NotNull
    private String authSalt;
    @NotNull
    private Integer expirationTime;

    public String getAuthHeader() {
        return authHeader;
    }

    public void setAuthHeader(String authHeader) {
        this.authHeader = authHeader;
    }

    public String getAuthSalt() {
        return authSalt;
    }

    public void setAuthSalt(String authSalt) {
        this.authSalt = authSalt;
    }

    public Integer getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Integer expirationTime) {
        this.expirationTime = expirationTime;
    }
}
