package com.guimox.auth.api.service;

import com.guimox.auth.dto.response.ClientResponseDto;
import com.guimox.auth.models.AuthClient;
import com.guimox.auth.api.repository.AuthClientRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClientService {

    private final AuthClientRepository authClientRepository;

    public ClientService(AuthClientRepository authClientRepository) {
        this.authClientRepository = authClientRepository;
    }

    public ClientResponseDto getAppNameFromCode(String appCode) {
        AuthClient authClient = authClientRepository.findById(appCode).orElseThrow(() -> new RuntimeException("Faulty app code"));
        return new ClientResponseDto(authClient.getAppName());
    }

    public List<AuthClient> getAllClients() {
        return authClientRepository.findAll();
    }

}
