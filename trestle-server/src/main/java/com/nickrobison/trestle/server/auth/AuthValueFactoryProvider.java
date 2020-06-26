//package com.nickrobison.trestle.server.auth;
//
//import com.nickrobison.trestle.server.annotations.AuthRequired;
//import com.nickrobison.trestle.server.models.User;
//import org.glassfish.hk2.api.Factory;
//import org.glassfish.hk2.api.InjectionResolver;
//import org.glassfish.hk2.api.ServiceLocator;
//import org.glassfish.hk2.api.TypeLiteral;
//import org.glassfish.hk2.utilities.binding.AbstractBinder;
//import org.glassfish.jersey.server.internal.inject.AbstractContainerRequestValueFactory;
//import org.glassfish.jersey.server.internal.inject.AbstractValueFactoryProvider;
//import org.glassfish.jersey.server.internal.inject.MultivaluedParameterExtractorProvider;
//import org.glassfish.jersey.server.internal.inject.ParamInjectionResolver;
//import org.glassfish.jersey.server.model.Parameter;
//import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
///**
// * Created by nrobison on 1/20/17.
// */
//@Singleton
//public class AuthValueFactoryProvider extends AbstractValueFactoryProvider {
//
//    @Inject
//    public AuthValueFactoryProvider(MultivaluedParameterExtractorProvider multivaluedParameterExtractorProvider, ServiceLocator serviceLocator) {
//        super(multivaluedParameterExtractorProvider, serviceLocator, Parameter.Source.UNKNOWN);
//    }
//
//    @Override
//    protected Factory<?> createValueFactory(Parameter parameter) {
//        if (parameter.isAnnotationPresent(AuthRequired.class)) {
//            return new AbstractContainerRequestValueFactory<User>() {
//                @Override
//                public User provide() {
//                    final Object user = getContainerRequest().getProperty("user");
//                    if (user != null && user instanceof User) {
//                        return User.class.cast(user);
//                    } else {
//                        return null;
//                    }
//                }
//            };
//        } else {
//            return null;
//        }
//    }
//
//    private static class AuthRequiredInjectionResolver extends ParamInjectionResolver<AuthRequired> {
//
//        public AuthRequiredInjectionResolver() {
//            super(AuthValueFactoryProvider.class);
//        }
//    }
//
//    public static class Binder extends AbstractBinder {
//
//        @Override
//        protected void configure() {
//            bind(AuthValueFactoryProvider.class).to(ValueFactoryProvider.class).in(Singleton.class);
//            bind(AuthRequiredInjectionResolver.class).to(new TypeLiteral<InjectionResolver<AuthRequired>>() {
//            }).in(Singleton.class);
//        }
//    }
//}
