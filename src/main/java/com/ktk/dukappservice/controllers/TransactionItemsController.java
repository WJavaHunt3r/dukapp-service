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
import com.ktk.dukappservice.service.notifications.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/transactionItem")
public class TransactionItemsController {
    private final TransactionItemService transactionItemService;
    private final UserService userService;
    private final RoundService roundService;
    private final TransactionItemMapper modelMapper;
    private final TransactionService transactionService;
    private final TransactionServiceUtils transactionServiceUtils;
    private final PushNotificationService pushNotificationService;

    public TransactionItemsController(TransactionItemService transactionItemService, UserService userService, RoundService roundService, TransactionItemMapper modelMapper, TransactionService transactionService, TransactionServiceUtils transactionServiceUtils, PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
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
        User createUser = userService.getCurrentUser(userDetails);
        Map<Long, List<String>> created = new HashMap<>();
        ResponseEntity<?> response = createItem(transactionItem, createUser, created);
        pushNotificationService.transactionsCreated(created, createUser.getId());
        return response;
    }

    @PostMapping("/items")
    @PreAuthorize("hasAuthority('TRANSACTION_MANAGE')")
    public ResponseEntity<?> addTransactions(@Valid @RequestBody List<TransactionItemDto> transactionItems, @AuthenticationPrincipal UserDetails userDetails) {
        User createUser = userService.getCurrentUser(userDetails);
        Map<Long, List<String>> created = new HashMap<>();
        transactionItems.forEach(item -> createItem(item, createUser, created));
        // One push per user for the whole batch
        pushNotificationService.transactionsCreated(created, createUser.getId());
//        transactionServiceUtils.calculateAllTeamStatus();
        return ResponseEntity.ok().body("Successfully added");

    }

    /** Creates one item; on success adds its description to {@code created} under the item's user. */
    private ResponseEntity<?> createItem(TransactionItemDto transactionItem, User createUser, Map<Long, List<String>> created) {
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

        TransactionItem entity = new TransactionItem();
        entity.setCreateUser(createUser);
        entity.setUser(user.get());
        entity.setRound(round.get());
        TransactionItem saved = transactionItemService.save(modelMapper.dtoToEntity(transactionItem, entity));
        transactionServiceUtils.updateUserStatus(round.get(), user.get());
        created.computeIfAbsent(user.get().getId(), id -> new ArrayList<>()).add(saved.getDescription());
        return ResponseEntity.status(200).build();
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
