package com.dsw.readeverywhere.model;

import org.springframework.stereotype.Component;


public class User{
    private String email;
    private String name;
    private String usedSpace;
    private String totalSpace;
    private String password;

    public String getEmail() {
        return email;
    }
    public String getName() {
        return name;
    }

    public String getUsedSpace() {
        return usedSpace;
    }

    public String getTotalSpace() {
        return totalSpace;
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

    public User setUsedSpace(String usedSpace) {
        this.usedSpace = usedSpace;
        return this;
    }

    public User setTotalSpace(String totalSpace) {
        this.totalSpace = totalSpace;
        return this;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }
}
