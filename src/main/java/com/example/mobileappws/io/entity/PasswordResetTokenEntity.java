package com.example.mobileappws.io.entity;

import jakarta.persistence.*;

import java.io.Serial;
import java.io.Serializable;

@Entity(name = "password_reset_tokens")
public class PasswordResetTokenEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = -6693173158800716194L;

    @Id
    @GeneratedValue
    private long id;

    private String token;

    @OneToOne
    @JoinColumn(name = "users_id")
    private UserEntity userDetails;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserEntity getUserDetails() {
        return userDetails;
    }

    public void setUserDetails(UserEntity userDetails) {
        this.userDetails = userDetails;
    }
}
