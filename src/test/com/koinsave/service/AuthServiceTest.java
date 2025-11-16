package com.koinsave.service;

import com.koinsave.dto.request.LoginRequest;
import com.koinsave.dto.request.RegisterRequest;
import com.koinsave.dto.response.AuthResponse;
import com.koinsave.exception.AuthException;
import com.koinsave.model.User;
import com.koinsave.repository.UserRepository;
import com.koinsave.util.JwtUtil;
import com.koinsave.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "test@example.com",
                "password123",
                "Test User",
                BigDecimal.valueOf(1000)
        );

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setBalance(BigDecimal.valueOf(1000));
        testUser.setActive(true);
    }

    @Test
    void register_WithValidData_ShouldCreateUserAndReturnAuthResponse() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordUtil.encode(validRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtUtil.generateToken(testUser.getEmail(), testUser.getId())).thenReturn("test-jwt-token");

        AuthResponse response = authService.register(validRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());
        assertEquals(BigDecimal.valueOf(1000), response.getBalance());
        assertEquals(1L, response.getUserId());

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(passwordUtil).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).generateToken(testUser.getEmail(), testUser.getId());
    }

    @Test
    void register_WithExistingEmail_ShouldThrowAuthException() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.register(validRequest));

        assertEquals("Email already registered", exception.getMessage());

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordUtil, never()).encode(anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getEmail(), testUser.getId())).thenReturn("test-jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("test-jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("Test User", response.getFullName());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordUtil).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtil).generateToken(testUser.getEmail(), testUser.getId());
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowAuthException() {
        LoginRequest loginRequest = new com.koinsave.dto.request.LoginRequest("wrong@example.com", "password123");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordUtil, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowAuthException() {
        LoginRequest loginRequest = new com.koinsave.dto.request.LoginRequest("test@example.com", "wrongpassword");

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid email or password", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordUtil).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }

    @Test
    void login_WithInactiveUser_ShouldThrowAuthException() {
        LoginRequest loginRequest = new com.koinsave.dto.request.LoginRequest("test@example.com", "password123");
        testUser.setActive(false);

        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordUtil.matches(loginRequest.getPassword(), testUser.getPassword())).thenReturn(true);

        AuthException exception = assertThrows(AuthException.class,
                () -> authService.login(loginRequest));

        assertEquals("Account is inactive", exception.getMessage());

        verify(userRepository).findByEmail(loginRequest.getEmail());
        verify(passwordUtil).matches(loginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtil, never()).generateToken(anyString(), anyLong());
    }
}