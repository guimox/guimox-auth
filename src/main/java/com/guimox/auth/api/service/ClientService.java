package com.guimox.auth.api.service;

import com.guimox.auth.dto.response.ClientResponseDto;
import com.guimox.auth.models.Apps;
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
        Apps apps = authClientRepository.findById(appCode).orElseThrow(() -> new RuntimeException("Faulty app code"));
        return new ClientResponseDto(apps.getAppName());
    }

    public List<Apps> getAllClients() {
        return authClientRepository.findAll();
    }

}
