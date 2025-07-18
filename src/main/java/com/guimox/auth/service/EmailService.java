package com.guimox.auth.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final Resend resend;

    public EmailService(@Value("${app.resend.api-key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    public void sendVerificationEmail(String to, String subject, String htmlBody) {
        CreateEmailOptions request = CreateEmailOptions.builder()
                .from("Your App <auth@auth.guimox.dev>") // Must be a verified domain in Resend
                .to(to)
                .subject(subject)
                .html(htmlBody)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(request);
            System.out.println("Email sent. ID: " + response.getId());
        } catch (Exception e) {
            e.printStackTrace(); // Or log properly
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}
