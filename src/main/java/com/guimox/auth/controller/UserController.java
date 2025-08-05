package com.guimox.auth.controller;

import com.guimox.auth.dto.response.UserDTO;
import com.guimox.auth.models.User;
import com.guimox.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/users")
@RestController
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<User> authenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        return ResponseEntity.ok(currentUser);
    }

    @GetMapping("")
    public ResponseEntity<List<UserDTO>> allUsers() {
        List<UserDTO> userDTOs = userService.allUsers()
                .stream()
                .map(UserDTO::new)
                .toList();
        return ResponseEntity.ok(userDTOs);
    }

}