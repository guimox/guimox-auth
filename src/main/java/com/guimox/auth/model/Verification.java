package com.guimox.auth.model;

import java.time.LocalDateTime;

public class Verification {

    private String code;
    private String email;
    private LocalDateTime expiresIn;

    public Verification(String code, String email, LocalDateTime expiresIn) {
        this.code = code;
        this.email = email;
        this.expiresIn = expiresIn;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(LocalDateTime expiresIn) {
        this.expiresIn = expiresIn;
    }
}
