package com.koinsave.service;

import com.koinsave.dto.request.LoginRequest;
import com.koinsave.dto.request.RegisterRequest;
import com.koinsave.dto.response.AuthResponse;
import com.koinsave.exception.AuthException;
import com.koinsave.model.User;
import com.koinsave.repository.UserRepository;
import com.koinsave.util.JwtUtil;
import com.koinsave.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordUtil.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setBalance(request.getInitialBalance());

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId());

        return new AuthResponse(
                token,
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getBalance(),
                savedUser.getId()
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Invalid email or password"));

        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
            throw new AuthException("Invalid email or password");
        }

        if (!user.getActive()) {
            throw new AuthException("Account is inactive");
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getId());

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFullName(),
                user.getBalance(),
                user.getId()
        );
    }
}