package com.nickrobison.trestle.server.resources;

import com.nickrobison.trestle.server.auth.*;
import com.nickrobison.trestle.server.models.User;
import com.nickrobison.trestle.server.models.UserDAO;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import jwt4j.JWTHandler;
import jwt4j.JWTHandlerBuilder;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Created by nickrobison on 6/26/20.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class UserResourceTest {

  public UserDAO dao = Mockito.mock(UserDAO.class);

  public JWTHandler<User> handler = new JWTHandlerBuilder<User>()
    .withSecret("test-key".getBytes(Charset.defaultCharset()))
    .withDataClass(User.class)
    .withIssuedAtEnabled(true)
    .withExpirationSeconds(500)
    .build();

  public ResourceExtension resource = ResourceExtension
    .builder()
    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
    .addProvider(PrivsAllowedDynamicFeature.class)
    .addProvider(new TrestleAuthDynamicFeature(new JWTAuthFilterBuilder().setAuthenticator(new TrestleAuthenticator(null, handler)).setAuthorizer(new TrestleAuthorizer()).setPrefix("Bearer").buildAuthFilter()))
    .addProvider(new AuthValueFactoryProvider.Binder<>(User.class))
    .addResource(new UserResource(dao))
    .build();

  @BeforeEach
  void setup() {
    Mockito.reset(dao);
  }

  @Test
  void testNoAuth() {
    final Response response = resource.target("users/1")
      .request(MediaType.APPLICATION_JSON)
      .get();

    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus(), "Should not be authorized");
  }

  @Test
  void testAdminOK() {
    final User user = new User();
    user.setId(1);
    user.setUsername("test-user");
    user.setPrivilegeSet(EnumSet.of(Privilege.ADMIN, Privilege.USER));

    Mockito.when(dao.findById(1)).thenReturn(Optional.of(user));

    final String token = handler.encode(user);
    final Response response = resource.target("users/1")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", token)
      .get();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Admin should validate");
  }

  @Test
  void testDBAOK() {
    final User user = new User();
    user.setId(1);
    user.setUsername("test-user");
    user.setPrivilegeSet(EnumSet.of(Privilege.DBA, Privilege.ADMIN, Privilege.USER));

    Mockito.when(dao.findById(1)).thenReturn(Optional.of(user));

    final String token = handler.encode(user);
    final Response response = resource.target("users/1")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", token)
      .get();

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Admin should validate");
  }

  @Test
  void testUserNotOK() {
    final User user = new User();
    user.setId(1);
    user.setUsername("test-user");
    user.setPrivilegeSet(EnumSet.of(Privilege.USER));

    Mockito.when(dao.findById(1)).thenReturn(Optional.of(user));

    final String token = handler.encode(user);
    final Response response = resource.target("users/1")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", token)
      .get();

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus(), "User should not validate");
  }

  @Test
  void testNoPermissions() {
    final User user = new User();
    user.setId(1);
    user.setUsername("test-user");

    Mockito.when(dao.findById(1)).thenReturn(Optional.of(user));

    final String token = handler.encode(user);
    final Response response = resource.target("users/1")
      .request(MediaType.APPLICATION_JSON)
      .header("Authorization", token)
      .get();

    assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus(), "User should fail");
  }
}
