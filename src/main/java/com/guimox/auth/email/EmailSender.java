package com.guimox.auth.email;

public interface EmailSender {
    void sendVerificationEmail(String to, String subject, String htmlBody);
}
