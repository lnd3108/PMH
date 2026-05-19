package com.example.demo.security.jwt;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();

    public void blacklist(String token) {
        blacklistedTokens.add(token);
    }

    /// kiểm tra token có nằm trong blacklist không
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }
}