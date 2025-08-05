package com.guimox.auth.service;

import com.guimox.auth.config.OAuth2Config;
import com.guimox.auth.dto.oauth2.GoogleUser;
import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.email.ResendEmailClient;
import com.guimox.auth.models.User;
import com.guimox.auth.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ResendEmailClient resendEmailClient;
    private final OAuth2Config oAuth2Config;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder,
            ResendEmailClient resendEmailClient,
            OAuth2Config oAuth2Config
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resendEmailClient = resendEmailClient;
        this.oAuth2Config = oAuth2Config;
    }

    public String signup(RegisterUserRequestDto input) {
        String generatedRegisterCode = generateVerificationCode();

        User user = new User.Builder()
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .enabled(false)
                .build();

        User savedUser = userRepository.save(user);
        sendVerificationEmail(user.getEmail(), generatedRegisterCode, input.getApp());

        return "Email sent for verification";
    }

    public User authenticate(LoginUserRequestDto input) {
        User user = userRepository.findByEmail(input.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your account.");
        }
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return user;
    }

    public void verifyUser(String token, String email) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Verification token is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email is required");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid verification link");
        }

        User user = optionalUser.get();

        if (user.isEnabled()) {
            throw new RuntimeException("Account is already verified");
        }

        userRepository.save(user);
    }

    public String processGrantCode(String code, String appCodeString) {
        String accessToken = oAuth2Config.getOauthAccessTokenGoogle(code);

        GoogleUser googleUser = oAuth2Config.getProfileDetailsGoogle(accessToken);

        User user = new User.Builder()
                .email(googleUser.getEmail())
                .password(null)
                .build();

        String generatedCode = generateVerificationCode();
        User savedUser = userRepository.save(user);
        sendVerificationEmail(savedUser.getEmail(), generatedCode, appCodeString);

        return "worked";
    }

    public void resendVerificationCode(String email, String appCodeString) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (user.isEnabled()) {
                throw new RuntimeException("Account is already verified");
            }
        } else {
            throw new RuntimeException("User not found");
        }
    }

    private void sendVerificationEmail(String userEmail, String token, String appCode) {
        String subject = "Account Verification";

        String verificationLink = buildVerificationLink(userEmail, token);

        String htmlMessage = "<html>"
                + "<body style=\"font-family: Arial, sans-serif; max-width: 450px;\">"
                + "<div style=\"background-color: #f5f5f5; padding: 20px;\">"
                + "<h2 style=\"color: #333;\">Welcome to our app!</h2>"
                + "<p style=\"font-size: 16px;\">Please click the link below to verify your account:</p>"
                + "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1);\">"
                + "<a href=\"" + verificationLink + "\" "
                + "style=\"display: inline-block; padding: 12px 24px; background-color: #007bff; color: white; "
                + "text-decoration: none; border-radius: 5px; font-weight: bold;\">Verify Account</a>"
                + "</div>"
                + "<p style=\"font-size: 14px; color: #666; margin-top: 20px;\">"
                + "If the button doesn't work, copy and paste this link into your browser:</p>"
                + "<p style=\"font-size: 12px; color: #007bff; word-break: break-all;\">" + verificationLink + "</p>"
                + "<p style=\"font-size: 12px; color: #999; margin-top: 20px;\">"
                + "This link will expire in 24 hours. If you didn't request this verification, please ignore this email.</p>"
                + "</div>"
                + "</body>"
                + "</html>";

        resendEmailClient.sendVerificationEmail(userEmail, subject, htmlMessage);
    }

    private String buildVerificationLink(String email, String token) {
        try {
            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);

            String baseUrl = getBaseUrl();

            return String.format("%s/auth/verify?email=%s&token=%s",
                    baseUrl, encodedEmail, encodedToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build verification link", e);
        }
    }

    private String getBaseUrl() {
        return "http://localhost:8080";
    }
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElse(null); // Return null if not found, controller will handle this
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
