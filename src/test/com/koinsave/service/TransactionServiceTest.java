package com.koinsave.service;

import com.koinsave.dto.request.TransferRequest;
import com.koinsave.dto.response.TransactionResponse;
import com.koinsave.exception.TransactionException;
import com.koinsave.model.Transaction;
import com.koinsave.model.User;
import com.koinsave.repository.TransactionRepository;
import com.koinsave.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private User sender;
    private User receiver;
    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        sender = new User();
        sender.setId(1L);
        sender.setEmail("sender@example.com");
        sender.setFullName("Sender User");
        sender.setBalance(BigDecimal.valueOf(1000));
        sender.setActive(true);

        receiver = new User();
        receiver.setId(2L);
        receiver.setEmail("receiver@example.com");
        receiver.setFullName("Receiver User");
        receiver.setBalance(BigDecimal.valueOf(500));
        receiver.setActive(true);

        transferRequest = new TransferRequest(2L, BigDecimal.valueOf(100), "Test transfer");
    }

    @Test
    void transfer_WithValidData_ShouldCompleteTransaction() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            transaction.setCreatedAt(LocalDateTime.now());
            return transaction;
        });

        TransactionResponse response = transactionService.transfer(1L, transferRequest);

        assertNotNull(response);
        assertEquals(1L, response.getSenderId());
        assertEquals("Sender User", response.getSenderName());
        assertEquals(2L, response.getReceiverId());
        assertEquals("Receiver User", response.getReceiverName());
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
        assertEquals("Test transfer", response.getDescription());
        assertEquals("COMPLETED", response.getStatus());

        verify(userRepository).findByIdForUpdate(1L);
        verify(userRepository).findByIdForUpdate(2L);
        verify(userRepository).saveAll(anyList());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transfer_WithInsufficientBalance_ShouldThrowException() {
        sender.setBalance(BigDecimal.valueOf(50)); // Only 50 available
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(receiver));

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Insufficient balance", exception.getMessage());

        verify(userRepository).findByIdForUpdate(1L);
        verify(userRepository).findByIdForUpdate(2L);
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_ToSelf_ShouldThrowException() {
        transferRequest.setReceiverId(1L); // Same as sender

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Cannot transfer to yourself", exception.getMessage());

        verify(userRepository, never()).findByIdForUpdate(anyLong());
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_WithZeroAmount_ShouldThrowException() {
        transferRequest.setAmount(BigDecimal.ZERO);

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Amount must be greater than zero", exception.getMessage());

        verify(userRepository, never()).findByIdForUpdate(anyLong());
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_WithNegativeAmount_ShouldThrowException() {
        transferRequest.setAmount(BigDecimal.valueOf(-50));

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Amount must be greater than zero", exception.getMessage());

        verify(userRepository, never()).findByIdForUpdate(anyLong());
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_SenderNotFound_ShouldThrowException() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Sender not found", exception.getMessage());

        verify(userRepository).findByIdForUpdate(1L);
        verify(userRepository, never()).findByIdForUpdate(2L);
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    void transfer_ReceiverNotFound_ShouldThrowException() {
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sender));
        when(userRepository.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        TransactionException exception = assertThrows(TransactionException.class,
                () -> transactionService.transfer(1L, transferRequest));

        assertEquals("Receiver not found", exception.getMessage());

        verify(userRepository).findByIdForUpdate(1L);
        verify(userRepository).findByIdForUpdate(2L);
        verify(userRepository, never()).saveAll(anyList());
        verify(transactionRepository, never()).save(any(Transaction.class));
    }
}