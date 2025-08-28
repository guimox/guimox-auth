package com.guimox.auth.api.repository;

import com.guimox.auth.models.Apps;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthClientRepository extends JpaRepository<Apps, String> {
    boolean existsByAppName(String appName);

    Optional<Apps> findByAppName(String appName);
}