package com.guimox.auth.controller;

import com.guimox.auth.dto.request.LoginUserRequestDto;
import com.guimox.auth.dto.request.RefreshTokenRequestDto;
import com.guimox.auth.dto.request.RegisterUserRequestDto;
import com.guimox.auth.model.User;
import com.guimox.auth.dto.response.LoginResponse;
import com.guimox.auth.dto.response.TokenRefreshResponse;
import com.guimox.auth.service.AuthenticationService;
import com.guimox.auth.service.JwtService;
import org.springframework.http.MediaType;
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

    @GetMapping("/verify")
    public ResponseEntity<String> verifyUser(
            @RequestParam String token,
            @RequestParam String email) {
        try {
            authenticationService.verifyUser(token, email);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><h2>Account verified successfully!</h2>" +
                            "<p>You can now close this window and log in.</p></body></html>");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body("<html><body><h2>Verification failed</h2>" +
                            "<p>" + e.getMessage() + "</p></body></html>");
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