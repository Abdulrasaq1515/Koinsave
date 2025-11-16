package com.koinsave.controller;

import com.koinsave.dto.request.TransferRequest;
import com.koinsave.dto.response.BalanceResponse;
import com.koinsave.dto.response.TransactionResponse;
import com.koinsave.repository.UserRepository;
import com.koinsave.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @Valid @RequestBody TransferRequest request,
            HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        TransactionResponse response = transactionService.transfer(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getHistory(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        List<TransactionResponse> transactions = transactionService.getUserTransactions(userId);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        BalanceResponse response = transactionService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
}