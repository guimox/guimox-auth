package com.guimox.auth.repository;

import com.guimox.auth.model.User;
import com.guimox.auth.model.Verification;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationRepository extends CrudRepository<Verification, Long> {
}