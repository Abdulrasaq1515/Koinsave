//package com.koinsave.service;
//
//import com.koinsave.dto.request.LoginRequest;
//import com.koinsave.dto.request.RegisterRequest;
//import com.koinsave.dto.response.AuthResponse;
//import com.koinsave.exception.AuthException;
//import com.koinsave.model.User;
//import com.koinsave.repository.UserRepository;
//import com.koinsave.util.JwtUtil;
//import com.koinsave.util.PasswordUtil;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//
//@Service
//@RequiredArgsConstructor
//public class AuthServiceImpl implements AuthService {
//
//    private final UserRepository userRepository;
//    private final PasswordUtil passwordUtil;
//    private final JwtUtil jwtUtil;
//
//    @Transactional
//    public AuthResponse register(RegisterRequest request) {
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new AuthException("Email already registered");
//        }
//
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordUtil.encode(request.getPassword()));
//        user.setFullName(request.getFullName());
//        user.setBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO);
//        user.setActive(true);
//
//        User savedUser = userRepository.save(user);
//
//        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId());
//
//        return new AuthResponse(
//                token,
//                savedUser.getEmail(),
//                savedUser.getFullName(),
//                savedUser.getBalance(),
//                savedUser.getId()
//        );
//    }
//
//
//    @Transactional(readOnly = true)
//    public AuthResponse login(LoginRequest request) {
//        User user = userRepository.findByEmail(request.getEmail())
//                .orElseThrow(() -> new AuthException("Invalid email or password"));
//
//        if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
//            throw new AuthException("Invalid email or password");
//        }
//
//        if (!user.getActive()) {
//            throw new AuthException("Account is inactive");
//        }
//
//        String token = jwtUtil.generateToken(user.getEmail(), user.getId());
//
//        return new AuthResponse(
//                token,
//                user.getEmail(),
//                user.getFullName(),
//                user.getBalance(),
//                user.getId()
//        );
//    }
//}

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration process for email: {}", request.getEmail());

        try {
            // Check if user exists
            log.debug("Checking if email already exists: {}", request.getEmail());
            if (userRepository.existsByEmail(request.getEmail())) {
                log.warn("Registration failed - email already registered: {}", request.getEmail());
                throw new AuthException("Email already registered");
            }

            // Create user
            log.debug("Creating new user entity");
            User user = new User();
            user.setEmail(request.getEmail().toLowerCase().trim());
            user.setFullName(request.getFullName().trim());

            log.debug("Encoding password");
            String encodedPassword = passwordUtil.encode(request.getPassword());
            user.setPassword(encodedPassword);

            user.setBalance(request.getInitialBalance() != null ?
                    request.getInitialBalance() : BigDecimal.ZERO);
            user.setActive(true);

            log.debug("Saving user to database");
            User savedUser = userRepository.save(user);
            log.info("User saved successfully with ID: {}", savedUser.getId());

            log.debug("Generating JWT token");
            String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId());
            log.info("JWT token generated successfully");

            // Create response
            AuthResponse response = new AuthResponse(
                    token,
                    savedUser.getEmail(),
                    savedUser.getFullName(),
                    savedUser.getBalance(),
                    savedUser.getId()
            );

            log.info("Registration completed successfully for user: {}", savedUser.getEmail());
            return response;

        } catch (AuthException e) {
            log.error("AuthException during registration: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during registration for email: {}", request.getEmail(), e);
            throw new RuntimeException("Registration failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Login failed - user not found: {}", request.getEmail());
                        return new AuthException("Invalid email or password");
                    });

            log.debug("Checking password for user: {}", user.getEmail());
            if (!passwordUtil.matches(request.getPassword(), user.getPassword())) {
                log.warn("Login failed - invalid password for user: {}", user.getEmail());
                throw new AuthException("Invalid email or password");
            }

            if (!user.getActive()) {
                log.warn("Login failed - account inactive for user: {}", user.getEmail());
                throw new AuthException("Account is inactive");
            }

            log.debug("Generating JWT token for login");
            String token = jwtUtil.generateToken(user.getEmail(), user.getId());
            log.info("Login successful for user: {}", user.getEmail());

            return new AuthResponse(
                    token,
                    user.getEmail(),
                    user.getFullName(),
                    user.getBalance(),
                    user.getId()
            );

        } catch (AuthException e) {
            log.error("AuthException during login: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for email: {}", request.getEmail(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }
}