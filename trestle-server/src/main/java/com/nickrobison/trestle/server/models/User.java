package com.nickrobison.trestle.server.models;

import javax.persistence.*;

/**
 * Created by nrobison on 1/18/17.
 */
@Entity
@Table(name = "users")
@NamedQueries({
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.findAll", query = "select u from User u"),
        @NamedQuery(name = "com.nickrobison.trestle.server.queries.User.findByName", query = "select u from User u where u.firstName like :name or u.lastName like :name")
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private String username;
    private String email;

    public User() {}

    public User(String firstName, String lastName, String username, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.email = email;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        if (getId() != user.getId()) return false;
        if (!getFirstName().equals(user.getFirstName())) return false;
        if (!getLastName().equals(user.getLastName())) return false;
        if (!getUsername().equals(user.getUsername())) return false;
        return getEmail().equals(user.getEmail());
    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + getFirstName().hashCode();
        result = 31 * result + getLastName().hashCode();
        result = 31 * result + getUsername().hashCode();
        result = 31 * result + getEmail().hashCode();
        return result;
    }
}
