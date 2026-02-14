package com.omnibrain.backend.auth;

public class User {
    public String id;
    public String email;
    public String role;

    public User(String id, String email, String role) {
        this.id = id;
        this.email = email;
        this.role = role;
    }
}
