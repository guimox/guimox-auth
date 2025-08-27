package com.guimox.auth.api.repository;

import com.guimox.auth.models.AuthClient;
import com.guimox.auth.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthClientRepository extends JpaRepository<AuthClient, String> {
    boolean existsByAppName(String appName);

    Optional<AuthClient> findByAppName(String appName);
}