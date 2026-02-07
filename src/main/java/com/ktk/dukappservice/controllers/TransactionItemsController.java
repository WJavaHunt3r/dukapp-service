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
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.enums.TransactionType;
import com.ktk.dukappservice.mapper.TransactionItemMapper;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
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
    public ResponseEntity<?> addTransaction(@Valid @RequestBody TransactionItemDto transactionItem) {
        Optional<Transaction> transaction = transactionService.findById(transactionItem.getTransactionId());
        if (transaction.isEmpty()) {
            return ResponseEntity.status(400).body("No transaction found with id: " + transactionItem.getTransactionId());
        }

        Optional<User> user = userService.findById(transactionItem.getUser().getId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user found with id: " + transactionItem.getUser().getId());
        }

        Optional<Round> round = roundService.findById(transactionItem.getRound().getId());
        if (round.isEmpty()) {
            return ResponseEntity.status(400).body("No round found with id: " + transactionItem.getRound().getId());
        }

        Optional<User> createUser = userService.findById(transactionItem.getCreateUserId());
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("CreateUser not found by id: " + transactionItem.getCreateUserId());
        } else if (createUser.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("User is not allowed to create transactions");
        }

        TransactionItem entity = new TransactionItem();
        entity.setCreateUser(createUser.get());
        entity.setUser(user.get());
        transactionItemService.save(modelMapper.dtoToEntity(transactionItem, entity));
        transactionServiceUtils.updateUserStatus(transactionItem.getRound(), user.get());
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addTransactions(@Valid @RequestBody List<TransactionItemDto> transactionItems) {
        transactionItems.forEach(this::addTransaction);
        transactionServiceUtils.calculateAllTeamStatus();
        return ResponseEntity.ok().body("Successfully added");

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied!");
        }
        Optional<TransactionItem> item = transactionItemService.findById(id);
        if (item.isPresent()) {
            transactionItemService.deleteById(id);
            transactionServiceUtils.updateUserStatus(item.get().getRound(), user.get());
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
                                                 @Nullable @RequestParam("endDate") LocalDate endDate) {
//        if (userId != null) {
//            Optional<User> user = userService.findById(userId);
//            if (user.isEmpty()) {
//                return ResponseEntity.status(404).body("No user found with id: " + userId);
//            }
//            if (roundId != null) {
//                Optional<Round> round = roundService.findById(roundId);
//                if (round.isEmpty()) {
//                    return ResponseEntity.status(404).body("No round with id:" + roundId);
//                }
//                return ResponseEntity.ok(StreamSupport.stream(transactionItemService.findAllByUserAndRound(user.get(), round.get()).spliterator(), false).map(modelMapper::entityToDto));
//            }
//            return ResponseEntity.ok(StreamSupport.stream(transactionItemService.findAllByUser(user.get()).spliterator(), false).map(modelMapper::entityToDto));
//        }
//        if (transactionId != null) {
//            return ResponseEntity.ok(StreamSupport.stream(transactionItemService.findAllByTransactionId(transactionId).spliterator(), false).map(modelMapper::entityToDto));
//        }
        return ResponseEntity.ok(transactionItemService.fetchByQuery(transactionType, startDate, endDate, transactionId, roundId, userId, seasonYear).stream().map(modelMapper::entityToDto));

//        return ResponseEntity.status(400).body("Empty parameters");

    }
}
