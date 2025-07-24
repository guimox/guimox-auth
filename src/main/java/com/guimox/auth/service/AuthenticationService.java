package com.guimox.auth.service;

import com.guimox.auth.dto.oauth2.GoogleUser;
import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.model.App;
import com.guimox.auth.model.User;
import com.guimox.auth.model.Verification;
import com.guimox.auth.repository.AppRepository;
import com.guimox.auth.repository.UserRepository;
import com.guimox.auth.repository.VerificationRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final AppRepository appRepository;
    private final EmailService emailService;
    private final OAuth2Service oAuth2Service;
    private final VerificationRepository verificationRepository;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder, AppRepository appRepository,
            EmailService emailService, OAuth2Service oAuth2Service, VerificationRepository verificationRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.appRepository = appRepository;
        this.emailService = emailService;
        this.oAuth2Service = oAuth2Service;
        this.verificationRepository = verificationRepository;
    }

    public String signup(RegisterUserRequestDto input) {
        App app = appRepository.findByName(input.getApp())
                .orElseThrow(() -> new RuntimeException("App does not exist"));

        String generatedRegisterCode = generateVerificationCode();

        User user = new User.Builder()
                .email(input.getEmail())
                .password(passwordEncoder.encode(input.getPassword()))
                .enabled(false)
                .verificationCode(generatedRegisterCode)
                .verificationCodeExpiresAt((LocalDateTime.now().plusHours(24))) // Set expiration
                .addApp(app)
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

        if (user.getVerificationCode() == null) {
            throw new RuntimeException("No pending verification for this account");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(LocalDateTime.now())) {
            user.setVerificationCode(null);
            user.setVerificationCodeExpiresAt(null);
            userRepository.save(user);
            throw new RuntimeException("Verification link has expired. Please request a new one");
        }

        if (!user.getVerificationCode().equals(token)) {
            throw new RuntimeException("Invalid verification link");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);
    }

    public String processGrantCode(String code, String appCodeString) {
        App appCode = appRepository.findByName(appCodeString)
                .orElseThrow(() -> new RuntimeException("App does not exist"));

        String accessToken = oAuth2Service.getOauthAccessTokenGoogle(code);

        GoogleUser googleUser = oAuth2Service.getProfileDetailsGoogle(accessToken);

        User user = new User.Builder()
                .email(googleUser.getEmail())
                .password(null)
                .addApp(appCode)
                .build();

        String generatedCode = generateVerificationCode();
        User savedUser = userRepository.save(user);
        Verification verification = new Verification(generatedCode, user.getEmail(), LocalDateTime.now().plusHours(1));
        verificationRepository.save(verification);
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
            Verification verification = new Verification(generateVerificationCode(), user.getEmail(), LocalDateTime.now().plusHours(1));
            sendVerificationEmail(email, verification.getCode(), appCodeString);
            verificationRepository.save(verification);
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

        emailService.sendVerificationEmail(userEmail, subject, htmlMessage);
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
