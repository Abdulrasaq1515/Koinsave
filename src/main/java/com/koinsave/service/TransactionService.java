package com.koinsave.service;

import com.koinsave.dto.request.TransferRequest;
import com.koinsave.dto.response.BalanceResponse;
import com.koinsave.dto.response.TransactionResponse;

import java.util.List;

public interface TransactionService {
    TransactionResponse transfer(Long SenderId,TransferRequest transferRequest);
    List<TransactionResponse> getUserTransactions(Long userId);
    BalanceResponse getBalance(Long userId);
}
