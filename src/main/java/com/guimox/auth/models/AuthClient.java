package com.guimox.auth.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class AuthClient {

    @Id
    private String clientId;

    @Column(nullable = false)
    private String redirectUri;

    public String getRedirectUri() {
        return redirectUri;
    }
}
