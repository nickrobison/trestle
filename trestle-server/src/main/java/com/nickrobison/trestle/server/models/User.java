package com.nickrobison.trestle.server.models;

import com.nickrobison.trestle.server.auth.Privilege;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Set;

/**
 * Created by nrobison on 1/18/17.
 */
@Entity
@Table(name = "users")
@NamedQueries({
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.findAll", query = "select u from User u"),
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.findByName", query = "select u from User u where u.firstName like :name or u.lastName like :name"),
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.findByUsername", query = "select u from User u where u.username = :username"),
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.deleteByID", query = "delete from User u where u.id = :id")
})
// I think we can suppress this for Beans
@SuppressWarnings({"initialization.fields.uninitialized"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @NotNull
    private String username;
    @NotNull
    private String email;
    @NotNull
    private String password;
    private int privileges;

    public User() {}

    public User(String firstName, String lastName, String username, String email, String password, Set<Privilege> privileges) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.privileges = Privilege.buildPrivilageMask(privileges);
    }

    public User(String firstName, String lastName, String username, String email, String password, int privileges) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
        this.password = password;
        this.privileges = privileges;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() { return this.password; }

    public void setPassword(String password) { this.password = password; }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<Privilege> getPrivilegeSet() {
        return Privilege.parsePrivileges(this.privileges);
    }

    public void setPrivilegeSet(Set<Privilege> privileges) {
        this.privileges = Privilege.buildPrivilageMask(privileges);
    }

    public int getPrivileges() { return this.privileges; }

    public void setPrivileges(int privileges) { this.privileges = privileges; }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (getId() != user.getId()) return false;
        if (getFirstName() != null ? !getFirstName().equals(user.getFirstName()) : user.getFirstName() != null)
            return false;
        if (getLastName() != null ? !getLastName().equals(user.getLastName()) : user.getLastName() != null)
            return false;
        if (!getUsername().equals(user.getUsername())) return false;
        if (!getEmail().equals(user.getEmail())) return false;
        return getPrivilegeSet().equals(user.getPrivilegeSet());
    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + (getFirstName() != null ? getFirstName().hashCode() : 0);
        result = 31 * result + (getLastName() != null ? getLastName().hashCode() : 0);
        result = 31 * result + getUsername().hashCode();
        result = 31 * result + getEmail().hashCode();
        result = 31 * result + getPrivilegeSet().hashCode();
        return result;
    }
}
