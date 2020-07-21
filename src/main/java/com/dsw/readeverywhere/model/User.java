package com.dsw.readeverywhere.model;

import org.springframework.stereotype.Component;


public class User{
    private String email;
    private String name;
    private String password;

    public String getEmail() {
        return email;
    }
    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public User setName(String name) {
        this.name = name;
        return this;
    }
    public User setEmail(String email) {
        this.email = email;
        return this;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }
}
