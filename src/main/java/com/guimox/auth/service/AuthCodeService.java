package com.guimox.auth.service;

import com.guimox.auth.models.User;
import com.guimox.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AuthCodeService {

    private static final String AUTH_CODE_PREFIX = "auth_code:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserRepository userRepository;

    public void storeAuthCode(String authCode, Long userId, int expirationSeconds) {
        String key = AUTH_CODE_PREFIX + authCode;
        String value = userId.toString();

        redisTemplate.opsForValue().set(key, value, expirationSeconds, TimeUnit.SECONDS);
    }

    public User validateAndConsumeAuthCode(String authCode) {
        if (authCode == null || authCode.trim().isEmpty()) {
            return null;
        }

        String key = AUTH_CODE_PREFIX + authCode;

        String userIdStr = redisTemplate.opsForValue().getAndDelete(key);

        if (userIdStr == null) {
            return null;
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            return userRepository.findById(userId).orElse(null);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isAuthCodeValid(String authCode) {
        if (authCode == null || authCode.trim().isEmpty()) {
            return false;
        }

        String key = AUTH_CODE_PREFIX + authCode;
        return redisTemplate.hasKey(key);
    }

    public void invalidateAuthCode(String authCode) {
        if (authCode != null && !authCode.trim().isEmpty()) {
            String key = AUTH_CODE_PREFIX + authCode;
            redisTemplate.delete(key);
        }
    }

    public long getAuthCodeTTL(String authCode) {
        if (authCode == null || authCode.trim().isEmpty()) {
            return -1;
        }

        String key = AUTH_CODE_PREFIX + authCode;
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
}
