package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.TransactionItemDto;
import com.ktk.dukappservice.enums.TransactionType;
import com.ktk.dukappservice.mapper.TransactionItemMapper;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/transactionItem")
public class TransactionItemsController {
    private final TransactionItemService transactionItemService;
    private final UserService userService;
    private final RoundService roundService;
    private final TransactionItemMapper modelMapper;
    private final TransactionService transactionService;
    private final TransactionServiceUtils transactionServiceUtils;

    public TransactionItemsController(TransactionItemService transactionItemService, UserService userService, RoundService roundService, TransactionItemMapper modelMapper, TransactionService transactionService, TransactionServiceUtils transactionServiceUtils) {
        this.transactionItemService = transactionItemService;
        this.userService = userService;
        this.roundService = roundService;
        this.modelMapper = modelMapper;
        this.transactionService = transactionService;
        this.transactionServiceUtils = transactionServiceUtils;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> addTransaction(@Valid @RequestBody TransactionItemDto transactionItem, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Transaction> transaction = transactionService.findById(transactionItem.getTransactionId());
        if (transaction.isEmpty()) {
            return ResponseEntity.status(400).body("No transaction found with id: " + transactionItem.getTransactionId());
        }

        Optional<User> user = userService.findById(transactionItem.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user found with id: " + transactionItem.getUserId());
        }

        Optional<Round> round = roundService.findById(transactionItem.getRoundId());
        if (round.isEmpty()) {
            return ResponseEntity.status(400).body("No round found with id: " + transactionItem.getRoundId());
        }

        User createUser = userService.getCurrentUser(userDetails);

        TransactionItem entity = new TransactionItem();
        entity.setCreateUser(createUser);
        entity.setUser(user.get());
        entity.setRound(round.get());
        transactionItemService.save(modelMapper.dtoToEntity(transactionItem, entity));
        transactionServiceUtils.updateUserStatus(round.get(), user.get());
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> addTransactions(@Valid @RequestBody List<TransactionItemDto> transactionItems, @AuthenticationPrincipal UserDetails userDetails) {
        transactionItems.forEach(item -> addTransaction(item, userDetails));
//        transactionServiceUtils.calculateAllTeamStatus();
        return ResponseEntity.ok().body("Successfully added");

    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id) {
        Optional<TransactionItem> item = transactionItemService.findById(id);
        if (item.isPresent()) {
            transactionItemService.deleteById(id);
            transactionServiceUtils.updateUserStatus(item.get().getRound(), item.get().getUser());
            transactionServiceUtils.calculateAllTeamStatus(item.get().getRound());
            return ResponseEntity.status(200).body("Delete successful");
        }

        return ResponseEntity.status(403).body("No transaction item found with id:" + id);

    }

    @GetMapping
    public ResponseEntity<?> getTransactionItems(@Nullable @RequestParam("userId") Long userId,
                                                 @Nullable @RequestParam("transactionId") Long transactionId,
                                                 @Nullable @RequestParam("roundId") Long roundId,
                                                 @Nullable @RequestParam("transactionType") TransactionType transactionType,
                                                 @Nullable @RequestParam("seasonYear") Integer seasonYear,
                                                 @Nullable @RequestParam("startDate") LocalDate startDate,
                                                 @Nullable @RequestParam("endDate") LocalDate endDate,
                                                 Pageable pageable) {
        return ResponseEntity.ok(transactionItemService.fetchByQuery(transactionType, startDate, endDate, transactionId, roundId, userId, seasonYear, pageable).map(modelMapper::entityToDto));

    }
}
