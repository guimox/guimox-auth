package com.guimox.auth.controller;

import com.guimox.auth.utils.ClientIdGenerator;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/client")
public class AuthClientController {

    @GetMapping("/generate")
    public String generateRandomId() {
        return ClientIdGenerator.generateClientId();
    }
}
