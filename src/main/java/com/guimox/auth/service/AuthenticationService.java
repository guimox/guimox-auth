package com.guimox.auth.service;

import com.guimox.auth.config.OAuth2Config;
import com.guimox.auth.dto.oauth2.GoogleUser;
import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.dto.response.SignupResponseDto;
import com.guimox.auth.email.ResendEmailClient;
import com.guimox.auth.jwt.JwtUtils;
import com.guimox.auth.models.AuthClient;
import com.guimox.auth.models.User;
import com.guimox.auth.repository.AuthClientRepository;
import com.guimox.auth.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final ResendEmailClient resendEmailClient;
    private final OAuth2Config oAuth2Config;
    private final JwtUtils jwtUtils;
    private final AuthClientRepository authClientRepository;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    @Value("${server.port}")
    private String serverPort;

    public AuthenticationService(UserRepository userRepository, AuthenticationManager authenticationManager, PasswordEncoder passwordEncoder, ResendEmailClient resendEmailClient, OAuth2Config oAuth2Config, JwtUtils jwtUtils, AuthClientRepository authClientRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.resendEmailClient = resendEmailClient;
        this.oAuth2Config = oAuth2Config;
        this.jwtUtils = jwtUtils;
        this.authClientRepository = authClientRepository;
    }

    public URI getRedirectUriByToken(String token, boolean success, String errorMessage) {
        String appCode = jwtUtils.extractApp(token);
        AuthClient client = authClientRepository.findByAppName(appCode).orElseThrow(() -> new IllegalArgumentException("Unknown client_id: " + appCode));

        String baseUri = client.getRedirectUri();

        if (success) {
            return URI.create(baseUri + "?status=success");
        } else {
            String encodedMessage = URLEncoder.encode(errorMessage, StandardCharsets.UTF_8);
            return URI.create(baseUri + "?status=error&message=" + encodedMessage);
        }
    }

    public URI getRedirectLogin(String appCode) {
        AuthClient client = authClientRepository.findByAppName(appCode).orElseThrow(() -> new IllegalArgumentException("Unknown client_id: " + appCode));

        String baseUri = client.getRedirectUri();

        if (!baseUri.isEmpty()) {
            return URI.create(baseUri + "?status=success");
        } else {
            String encodedMessage = URLEncoder.encode("error", StandardCharsets.UTF_8);
            return URI.create(baseUri + "?status=error&message=" + encodedMessage);
        }
    }

    @Transactional
    public SignupResponseDto signup(RegisterUserRequestDto input) {
        String generatedRegisterCode = generateVerificationCode();
        boolean userExists = false;

        String encodedPassword = passwordEncoder.encode(input.getPassword());
        String verificationToken = null;

        Optional<User> existingUser = userRepository.findByEmail(input.getEmail());

        if (existingUser.isPresent()) {
            userExists = true;
            verificationToken = jwtUtils.generateTokenSignup(input.getApp(), existingUser.get(), generatedRegisterCode);
        } else {
            User user = new User.Builder()
                    .email(input.getEmail())
                    .password(encodedPassword)
                    .enabled(false)
                    .build();

            verificationToken = jwtUtils.generateTokenSignup(input.getApp(), user, generatedRegisterCode);
            userRepository.save(user);
        }

        try {
            if (!userExists) {
                sendVerificationEmail(input.getEmail(), verificationToken, input.getApp());
            } else {
                handleExistingUserSignup(input.getEmail(), existingUser.get(), input.getApp());
            }

            Thread.sleep(50 + new SecureRandom().nextInt(150));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Process interrupted", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to process signup request", e);
        }

        return new SignupResponseDto("Verification email sent", input.getEmail());
    }

    private void handleExistingUserSignup(String email, User existingUser, String app) {
        if (!existingUser.isEnabled()) {
            String newCode = generateVerificationCode();
            String newToken = jwtUtils.generateTokenSignup(app, existingUser, newCode);
            sendVerificationEmail(email, newToken, app);
        }
    }


    public User authenticate(LoginUserRequestDto input) {
        User user = userRepository.findByEmail(input.getEmail()).orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("Account not verified. Please verify your account.");
        }
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(input.getEmail(), input.getPassword()));

        return user;
    }

    public void verifyUser(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new RuntimeException("Verification token is required");
        }

        Claims claims;
        try {
            claims = jwtUtils.extractAllClaims(token);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or expired verification token");
        }

        String tokenEmail = claims.getSubject();
        String verificationCode = claims.get("verificationCode", String.class);

        Optional<User> optionalUser = userRepository.findByEmail(tokenEmail);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid verification link");
        }

        User user = optionalUser.get();

        if (user.isEnabled()) {
            throw new RuntimeException("Account is already verified");
        }

        user.setEnabled(true);
        userRepository.save(user);
    }

    public String processGrantCode(String code, String appCodeString) {
        String accessToken = oAuth2Config.getOauthAccessTokenGoogle(code);

        GoogleUser googleUser = oAuth2Config.getProfileDetailsGoogle(accessToken);

        User user = new User.Builder().email(googleUser.getEmail()).password(null).build();

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
        String subject = appCode + " - Account Verification";

        String verificationLink = buildVerificationLink(token, appCode);

        String htmlMessage =
                "<html>" +
                        "<body style=\"font-family: Arial, sans-serif; max-width: 450px; margin: auto;\">" +
                        "<div style=\"background-color: #f5f5f5; padding: 20px;\">" +
                        "<h2 style=\"color: #333; margin-bottom: 16px;\">Welcome to " + appCode + "</h2>" +
                        "<p style=\"font-size: 15px; color: #555; line-height: 1.5;\">" +
                        "Thank you for registering with <strong>" + appCode + "</strong>. " +
                        "To complete your account setup, please verify your email address by clicking the button below:" +
                        "</p>" +
                        "<div style=\"background-color: #fff; padding: 20px; border-radius: 5px; box-shadow: 0 0 10px rgba(0,0,0,0.1); text-align: center;\">" +
                        "<a href=\"" + verificationLink + "\" " +
                        "style=\"display: inline-block; padding: 12px 24px; background-color: #000000; color: white; " +
                        "text-decoration: none; border-radius: 5px; font-weight: bold;\">" +
                        "Verify Account" +
                        "</a>" +
                        "</div>" +
                        "<p style=\"font-size: 12px; color: #999; margin-top: 20px;\">" +
                        "This link will expire in 24 hours. If you did not request this verification, please disregard this email." +
                        "</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        resendEmailClient.sendVerificationEmail(userEmail, subject, htmlMessage);
    }


    private String buildVerificationLink(String token, String appCode) {
        try {
            String encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8);
            String baseUrl = getBaseUrl(appCode);

            boolean appCodeExists = authClientRepository.existsByAppName(appCode);
            if (!appCodeExists) throw new RuntimeException("Failed to build verification link");

            return String.format("%s/auth/verify?token=%s", baseUrl, encodedToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build verification link", e);
        }
    }

    private String getBaseUrl(String appCode) {
        if ("hlg".equalsIgnoreCase(activeProfile) || "dev".equalsIgnoreCase(activeProfile)) {
            return "http://localhost:" + serverPort;
        } else {
            return "https://auth.guimox.dev";
        }
    }

    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null); // Return null if not found, controller will handle this
    }

    private String generateVerificationCode() {
        Random random = new Random();
        int code = random.nextInt(900000) + 100000;
        return String.valueOf(code);
    }
}
