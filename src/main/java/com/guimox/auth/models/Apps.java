package com.guimox.auth.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "apps")
public class Apps {

    @Id
    private String clientId;

    @Column(name = "app_name")
    private String appName;

    @Column(nullable = false)
    private String redirectUri;

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAppName() {
        return appName;
    }
}
