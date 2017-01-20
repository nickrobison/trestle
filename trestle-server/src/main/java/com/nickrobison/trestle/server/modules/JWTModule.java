package com.nickrobison.trestle.server.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.nickrobison.trestle.server.config.JWTConfig;
import com.nickrobison.trestle.server.config.TrestleServerConfiguration;
import com.nickrobison.trestle.server.models.User;
import jwt4j.JWTHandler;
import jwt4j.JWTHandlerBuilder;

import javax.inject.Singleton;

/**
 * Created by nrobison on 1/20/17.
 */
@Singleton
public class JWTModule extends AbstractModule {

    private JWTHandler<User> handler;

    @Override
    protected void configure() {
    }

    @Provides
    public JWTHandler<User> providesJWTHandler(TrestleServerConfiguration configuration) {
        if (handler == null) {
            this.handler = getJWTHandler(configuration.getJwtConfig());
        }
        return handler;
    }

    @Provides
    public JWTConfig providesJWTConfig(TrestleServerConfiguration configuration) {
        return configuration.getJwtConfig();
    }

    private JWTHandler<User> getJWTHandler(JWTConfig jwtConfig) {
        return new JWTHandlerBuilder<User>()
                .withSecret(jwtConfig.getAuthSalt().getBytes())
                .withDataClass(User.class)
                .build();
    }
}
