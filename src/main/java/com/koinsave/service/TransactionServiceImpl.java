package com.koinsave.service;

import com.koinsave.dto.request.TransferRequest;
import com.koinsave.dto.response.BalanceResponse;
import com.koinsave.dto.response.TransactionResponse;
import com.koinsave.exception.TransactionException;
import com.koinsave.model.Transaction;
import com.koinsave.model.User;
import com.koinsave.repository.TransactionRepository;
import com.koinsave.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransactionResponse transfer(Long senderId, TransferRequest request) {
        validateTransferRequest(senderId, request);

        User sender = userRepository.findByIdForUpdate(senderId)
                .orElseThrow(() -> new TransactionException("Sender not found"));
        User receiver = userRepository.findByIdForUpdate(request.getReceiverId())
                .orElseThrow(() -> new TransactionException("Receiver not found"));

        validateTransferAmount(sender, request.getAmount());

        updateBalances(sender, receiver, request.getAmount());

        Transaction transaction = createTransaction(sender, receiver, request);
        Transaction savedTransaction = transactionRepository.save(transaction);

        return mapToResponse(savedTransaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getUserTransactions(Long userId) {
        return transactionRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new TransactionException("User not found"));

        return new BalanceResponse(
                user.getBalance(),
                user.getEmail(),
                user.getFullName()
        );

    }


    private void validateTransferRequest(Long senderId, TransferRequest request) {
        if (senderId.equals(request.getReceiverId())) {
            throw new TransactionException("Cannot transfer to yourself");
        }

        Optional.of(request.getAmount())
                .filter(amount -> amount.compareTo(BigDecimal.ZERO) > 0)
                .orElseThrow(() -> new TransactionException("Amount must be greater than zero"));
    }

    private void validateTransferAmount(User sender, BigDecimal amount) {
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new TransactionException("Insufficient balance");
        }
    }

    private void updateBalances(User sender, User receiver, BigDecimal amount) {
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        userRepository.saveAll(List.of(sender, receiver));
    }

    private Transaction createTransaction(User sender, User receiver, TransferRequest request) {
        Transaction transaction = new Transaction();
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setAmount(request.getAmount());
        transaction.setDescription(request.getDescription());
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
        return transaction;
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSender().getId(),
                transaction.getSender().getFullName(),
                transaction.getReceiver().getId(),
                transaction.getReceiver().getFullName(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getStatus().toString(),
                transaction.getCreatedAt()
        );
    }
}