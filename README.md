## Auth System for My Apps

This project is a centralized authentication service built with Spring Boot, designed to provide secure authentication and authorization for all my applications. It is deployed on my VPS and exposes a set of APIs for user registration, login, email verification, token refresh, and OAuth2 login (Google). The system is intended to be the single point of authentication for any app I develop that requires user management.

## Features

- **User Registration & Login:**  
  Secure endpoints for user signup and login with email and password.

- **JWT Authentication:**  
  Uses JSON Web Tokens (JWT) for stateless authentication and session management.

- **Email Verification:**  
  Sends verification codes to users' emails using the Resend API to confirm their accounts.

- **OAuth2 Integration:**  
  Supports Google OAuth2 login for seamless third-party authentication.

- **Token Refresh:**  
  Provides refresh tokens to maintain user sessions securely.

- **Role & App Management:**  
  Users can be associated with different apps, allowing for multi-app authentication from a single service.

- **Secure Password Storage:**  
  Passwords are hashed using BCrypt for enhanced security.

- **Environment-based Configuration:**  
  Sensitive data (database credentials, API keys, secrets) are externalized using environment variables.

## Tech Stack

- **Java 17**
- **Spring Boot 3**
- **Spring Security**
- **Spring Data JPA**
- **PostgreSQL**
- **JWT (io.jsonwebtoken)**
- **OAuth2 Client**
- **Resend (Email API)**
- **Docker**

## API Endpoints

- `POST /auth/signup` — Register a new user
- `POST /auth/login` — Authenticate and receive JWT tokens
- `POST /auth/verify` — Verify user email with code
- `POST /auth/resend` — Resend verification code
- `POST /auth/refresh-token` — Refresh JWT access token
- `GET /auth/grantcode` — Google OAuth2 callback
- `GET /users/me` — Get authenticated user info
- `GET /users` — List all users (admin only)

## Security

- All sensitive configuration (database URL, JWT secret, OAuth2 credentials, email API keys) is managed via environment variables and never committed to source control.
- The `.gitignore` is configured to prevent accidental commits of secrets or environment-specific files.

## Deployment

- The application is containerized with Docker and can be deployed to any VPS or cloud provider.
- A GitHub Actions workflow is provided for CI/CD, building and deploying the Docker image to your server.
