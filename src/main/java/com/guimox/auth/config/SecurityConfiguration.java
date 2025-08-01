    package com.guimox.auth.config;

    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.authentication.AuthenticationProvider;
    import org.springframework.security.config.annotation.web.builders.HttpSecurity;
    import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
    import org.springframework.security.config.http.SessionCreationPolicy;
    import org.springframework.security.web.SecurityFilterChain;
    import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
    import org.springframework.web.cors.CorsConfiguration;
    import org.springframework.web.cors.CorsConfigurationSource;
    import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

    import java.util.List;

    @Configuration
    @EnableWebSecurity
    public class SecurityConfiguration {
        private final AuthenticationProvider authenticationProvider;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfiguration(
                JwtAuthenticationFilter jwtAuthenticationFilter,
                AuthenticationProvider authenticationProvider
        ) {
            this.authenticationProvider = authenticationProvider;
            this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .cors(cors -> cors.configurationSource(corsConfigurationSource())) // Apply CORS configuration
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/grantcode/**").permitAll()
                            .requestMatchers("/error").permitAll() // Allow access to error endpoint
                            .requestMatchers("/auth/**").permitAll()
                            .requestMatchers("/api/auth/**").permitAll() // Changed from /auth/** to /api/auth/**
                            .requestMatchers("/auth/verify").permitAll() // Explicitly allow GET verification
                            .anyRequest().authenticated()
                    )
                    .sessionManagement(session -> session
                            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    )
                    .authenticationProvider(authenticationProvider)
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

            return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
            configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
            configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/**", configuration);
            return source;
        }
    }