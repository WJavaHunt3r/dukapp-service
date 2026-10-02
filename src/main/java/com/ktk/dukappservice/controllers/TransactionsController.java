package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.TransactionDto;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/transaction")
public class TransactionsController {

    private final TransactionService transactionService;
    private final ModelMapper modelMapper;
    private final TransactionItemService transactionItemService;
    private final UserService userService;
    private final TransactionServiceUtils transactionServiceUtils;

    public TransactionsController(TransactionService transactionService, ModelMapper modelMapper, TransactionItemService transactionItemService, UserService userService, RoundService roundService, TransactionServiceUtils transactionServiceUtils) {
        this.transactionService = transactionService;
        this.modelMapper = modelMapper;
        this.transactionItemService = transactionItemService;
        this.userService = userService;
        this.transactionServiceUtils = transactionServiceUtils;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> addTransaction(@Valid @RequestBody TransactionDto transaction, @AuthenticationPrincipal UserDetails userDetails) {
        User createUser = userService.getCurrentUser(userDetails);
        return ResponseEntity.status(200).body(convertToDto(transactionService.save(convertToEntity(transaction, createUser))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        if (transactionService.existsById(id)) {
            Iterable<TransactionItem> items = transactionItemService.fetchByQuery(null, null, null, id, null, null, null, null);
            transactionService.deleteById(id);
            transactionItemService.deleteByTransactionId(id);
            for (var item : items) {
                transactionServiceUtils.updateUserStatus(item.getRound(), item.getUser());
            }

            return ResponseEntity.status(200).body("Delete successful");
        }
        return ResponseEntity.status(403).body("No transaction found with id:" + id);

    }

    @GetMapping
    public ResponseEntity<?> getTransactions(@RequestParam(value = "createUserId", required = false) Long createUserId,
                                             @RequestParam(value = "dateFrom") String dateFrom,
                                             @RequestParam(value = "dateTo") String dateTo,
                                             @RequestParam(value = "keyword", required = false) String keyword,
                                             Pageable pageable) {
        return ResponseEntity.status(200).body(transactionService.fetchByQuery(dateFrom, dateTo, createUserId, pageable).map(this::convertToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id) {
        Optional<Transaction> transaction = transactionService.findById(id);
        if (transaction.isEmpty()) {
            return ResponseEntity.status(400).body("No transaction found with Id: " + id);
        }
        return ResponseEntity.status(200).body(convertToDto(transaction.get()));
    }

    private Transaction convertToEntity(TransactionDto dto, User user) {
        Transaction transaction = modelMapper.map(dto, Transaction.class);
        transaction.setCreateUser(user);
        return transaction;
    }

    private TransactionDto convertToDto(Transaction transaction) {
        TransactionDto dto = modelMapper.map(transaction, TransactionDto.class);
        dto.setTransactionCount(transactionItemService.countByTransactionId(transaction.getId()));
        return dto;
    }

    private Optional<User> findById(Long id) {
        return userService.findById(id);
    }
}
