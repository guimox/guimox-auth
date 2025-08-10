package com.guimox.auth.controller;

import com.guimox.auth.dto.response.ClientResponseDto;
import com.guimox.auth.models.AuthClient;
import com.guimox.auth.service.ClientService;
import com.guimox.auth.utils.ClientIdGenerator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/client")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/generate")
    public String generateRandomId() {
        return ClientIdGenerator.generateClientId();
    }

    @GetMapping("/apps")
    public ResponseEntity<List<AuthClient>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @GetMapping("")
    public ResponseEntity<ClientResponseDto> getAppName(@RequestParam String code) {
        return ResponseEntity.ok(clientService.getAppNameFromCode(code));
    }

}
