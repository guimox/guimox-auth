package com.guimox.auth.api.controller;

import com.guimox.auth.api.service.AuthCodeService;
import com.guimox.auth.dto.request.AuthCodeExchangeRequest;
import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RefreshTokenRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.dto.response.SignupResponseDto;
import com.guimox.auth.models.User;
import com.guimox.auth.dto.response.LoginResponse;
import com.guimox.auth.dto.response.TokenRefreshResponse;
import com.guimox.auth.api.service.AuthenticationService;
import com.guimox.auth.jwt.JwtUtils;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {
    private final JwtUtils jwtUtils;
    private final AuthenticationService authenticationService;
    private final AuthCodeService authCodeService;

    public AuthenticationController(JwtUtils jwtUtils, AuthenticationService authenticationService, AuthCodeService authCodeService) {
        this.jwtUtils = jwtUtils;
        this.authenticationService = authenticationService;
        this.authCodeService = authCodeService;
    }

    @GetMapping("/grantcode")
    public String grantCode(@RequestParam("code") String code, @RequestParam("scope") String scope, @RequestParam("authuser") String authUser, @RequestParam("prompt") String prompt, @RequestParam String state) {
        String appCode = state.split(":")[1]; //
        return authenticationService.processGrantCode(code, appCode);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> register(@RequestBody RegisterUserRequestDto registerUserRequestDto) {
        SignupResponseDto registeredUser = authenticationService.signup(registerUserRequestDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(@RequestBody LoginUserRequestDto loginUserRequestDto,
                                          HttpServletResponse response) {
        try {
            User authenticatedUser = authenticationService.authenticate(loginUserRequestDto);
            URI redirectUri = authenticationService.getRedirectLogin(loginUserRequestDto.getApp());

            String authCode = UUID.randomUUID().toString();
            authCodeService.storeAuthCode(authCode, authenticatedUser.getId(), 600);

            String redirectUrl = UriComponentsBuilder.fromUri(redirectUri)
                    .queryParam("status", "success")
                    .queryParam("code", authCode)
                    .build()
                    .toString();

            Map<String, Object> jsonResponse = new HashMap<>();
            jsonResponse.put("success", true);
            jsonResponse.put("redirectUrl", redirectUrl);
            jsonResponse.put("message", "Authentication successful");

            return ResponseEntity.ok(jsonResponse);

        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid credentials");

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    @PostMapping("/token")
    public ResponseEntity<LoginResponse> exchangeAuthCode(@RequestBody AuthCodeExchangeRequest request) {
        User user = authCodeService.validateAndConsumeAuthCode(request.getCode());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String accessToken = jwtUtils.generateToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        LoginResponse response = new LoginResponse(accessToken, jwtUtils.getAccessTokenExpirationTime(), refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/verify")
    public ResponseEntity<Void> verifyUser(
            @RequestParam("token") String token) {

        try {
            authenticationService.verifyUser(token);
            URI successRedirect = authenticationService.getRedirectUriByToken(token, true, null);
            return ResponseEntity.status(HttpStatus.FOUND).location(successRedirect).build();

        } catch (RuntimeException e) {
            URI failureRedirect = authenticationService.getRedirectUriByToken(token, false, e.getMessage());
            return ResponseEntity.status(HttpStatus.FOUND).location(failureRedirect).build();
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email, "testing");
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();

        try {
            if (jwtUtils.isTokenValid(requestRefreshToken)) {
                String username = jwtUtils.extractUsername(requestRefreshToken);
                User user = authenticationService.findUserByEmail(username); // Or however you retrieve the user
                if (user == null) {
                    return ResponseEntity.badRequest().body("Invalid refresh token: User not found.");
                }

                String newAccessToken = jwtUtils.generateToken(user);

                return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, requestRefreshToken));
            } else {
                return ResponseEntity.badRequest().body("Refresh token is invalid or expired!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing refresh token: " + e.getMessage());
        }
    }
}