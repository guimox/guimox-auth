package com.guimox.auth.dto.response;

public class SignupResponseDto {
    private final String message;
    private final String email;

    public SignupResponseDto(String message, String email) {
        this.message = message;
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public String getEmail() {
        return email;
    }
}
