package com.guimox.auth.dto.response;

public class ClientResponseDto {

    private final String appName;

    public String getAppName() {
        return appName;
    }

    public ClientResponseDto(String appName) {
        this.appName = appName;
    }
}
