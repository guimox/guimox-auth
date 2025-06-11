package com.guimox.auth.controller;

import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RefreshTokenRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.dto.request.VerifyUserRequestDto;
import com.guimox.auth.model.User;
import com.guimox.auth.responses.LoginResponse;
import com.guimox.auth.responses.TokenRefreshResponse;
import com.guimox.auth.service.AuthenticationService;
import com.guimox.auth.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping("/auth")
@RestController
public class AuthenticationController {
    private final JwtService jwtService;
    private final AuthenticationService authenticationService;

    public AuthenticationController(JwtService jwtService, AuthenticationService authenticationService) {
        this.jwtService = jwtService;
        this.authenticationService = authenticationService;
    }

    @GetMapping("/grantcode")
    public String grantCode(@RequestParam("code") String code, @RequestParam("scope") String scope, @RequestParam("authuser") String authUser, @RequestParam("prompt") String prompt, @RequestParam String state) {
        String appCode = state.split(":")[1]; //
        return authenticationService.processGrantCode(code, appCode);
    }

    @PostMapping("/signup")
    public ResponseEntity<String> register(@RequestBody RegisterUserRequestDto registerUserRequestDto) {
        String registeredUser = authenticationService.signup(registerUserRequestDto);
        return ResponseEntity.ok(registeredUser);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> authenticate(@RequestBody LoginUserRequestDto loginUserRequestDto) {
        User authenticatedUser = authenticationService.authenticate(loginUserRequestDto);

        String accessToken = jwtService.generateToken(authenticatedUser);
        String refreshToken = jwtService.generateRefreshToken(authenticatedUser);

        LoginResponse loginResponse = new LoginResponse(accessToken, jwtService.getAccessTokenExpirationTime(), refreshToken);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyUser(@RequestBody VerifyUserRequestDto verifyUserRequestDto) {
        try {
            authenticationService.verifyUser(verifyUserRequestDto);
            return ResponseEntity.ok("Account verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/resend")
    public ResponseEntity<String> resendVerificationCode(@RequestParam String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code sent");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequestDto request) {
        String requestRefreshToken = request.getRefreshToken();

        try {
            if (jwtService.isTokenValid(requestRefreshToken)) {
                String username = jwtService.extractUsername(requestRefreshToken);
                User user = authenticationService.findUserByEmail(username); // Or however you retrieve the user
                if (user == null) {
                    return ResponseEntity.badRequest().body("Invalid refresh token: User not found.");
                }

                String newAccessToken = jwtService.generateToken(user);

                return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, requestRefreshToken));
            } else {
                return ResponseEntity.badRequest().body("Refresh token is invalid or expired!");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing refresh token: " + e.getMessage());
        }
    }
}