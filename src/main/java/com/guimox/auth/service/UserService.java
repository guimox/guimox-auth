package com.guimox.auth.service;

import com.guimox.auth.email.ResendEmailClient;
import com.guimox.auth.models.User;
import com.guimox.auth.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository, ResendEmailClient resendEmailClient) {
        this.userRepository = userRepository;
    }

    public List<User> allUsers() {
        return userRepository.findAll();
    }
}