package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.TransactionDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Optional;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/transaction")
public class TransactionsController {

    private final TransactionService transactionService;
    private final ModelMapper modelMapper;
    private final TransactionItemService transactionItemService;
    private final UserService userService;
    private final RoundService roundService;
    private final TransactionServiceUtils transactionServiceUtils;

    public TransactionsController(TransactionService transactionService, ModelMapper modelMapper, TransactionItemService transactionItemService, UserService userService, RoundService roundService, TransactionServiceUtils transactionServiceUtils) {
        this.transactionService = transactionService;
        this.modelMapper = modelMapper;
        this.transactionItemService = transactionItemService;
        this.userService = userService;
        this.roundService = roundService;
        this.transactionServiceUtils = transactionServiceUtils;
    }

    @PostMapping
    public ResponseEntity<?> addTransaction(@Valid @RequestBody TransactionDto transaction, @RequestParam("userId") Long userId) {
        Optional<User> createUser = findById(userId);
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (createUser.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied:");
        }
        return ResponseEntity.status(200).body(convertToDto(transactionService.save(convertToEntity(transaction, createUser.get()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied!");
        }
        if (transactionService.existsById(id)) {
            Iterable<TransactionItem> items = transactionItemService.fetchByQuery(null, null, null, id, null, null, null);
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
    public ResponseEntity<?> getTransactions(@Nullable @RequestParam("createUserId") Long createUserId, @Nullable @RequestParam("roundId") Long roundId) {
        if (createUserId != null) {
            Optional<User> createUser = findById(createUserId);
            if (createUser.isPresent()) {
                return ResponseEntity.status(200).body(StreamSupport.stream(transactionService.findAllByCreateUser(createUser.get()).spliterator(), false).map(this::convertToDto));
            }

        }
        if (roundId != null) {
            Optional<Round> round = roundService.findById(roundId);
            if (round.isEmpty()) {
                return ResponseEntity.status(400).body("No Round with given ID: " + roundId);
            }
            return ResponseEntity.status(200).body(transactionService.findAllByRound(round.get()).stream().map(this::convertToDto));
        }
        return ResponseEntity.status(200).body(StreamSupport.stream(transactionService.findAll().spliterator(), false).map(this::convertToDto));
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
