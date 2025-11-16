package com.koinsave.service;

import com.koinsave.dto.request.LoginRequest;
import com.koinsave.dto.request.RegisterRequest;
import com.koinsave.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest registerRequest);
    AuthResponse login(LoginRequest loginRequest);

}
