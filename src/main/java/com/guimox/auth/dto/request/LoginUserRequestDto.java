package com.guimox.auth.dto.request;

public class LoginUserRequestDto {
    private String email;
    private String password;
    private String app;

    public String getApp() {
        return app;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}