package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.userrounds.UserRound;
import com.ktk.dukappservice.data.userrounds.UserRoundService;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.UserRoundDto;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api")
public class UserRoundController {

    private UserRoundService userRoundService;
    private RoundService roundService;
    private UserService userService;
    private ModelMapper modelMapper;

    public UserRoundController(UserRoundService userRoundService, RoundService roundService, UserService userService, ModelMapper modelMapper) {
        this.userRoundService = userRoundService;
        this.roundService = roundService;
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    @GetMapping("/userRounds")
    public ResponseEntity<?> getUserRounds(@Nullable @RequestParam("userId") Long userId,
                                        @Nullable @RequestParam("roundId") Long roundId,
                                        @Nullable @RequestParam("seasonYear") Integer seasonYear) {
        if (userId != null) {
            var user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(404).body("No user with given id: " + userId);
            }
            if (roundId != null) {
                Optional<Round> round = roundService.findById(roundId);
                if (round.isEmpty()) {
                    return ResponseEntity.status(404).body("No round with given id: " + roundId);
                }
                return ResponseEntity.status(200).body(entityToDto(userRoundService.findByUserAndRound(user.get(), round.get())));
            }
            if (seasonYear != null) {
                return ResponseEntity.status(200).body(userRoundService.findByUserAndSeasonYear(user.get(), seasonYear).stream().map(this::entityToDto));
            }
            return ResponseEntity.status(200).body(userRoundService.findByUser(user.get()).stream().map(this::entityToDto));

        }
        return ResponseEntity.status(200).body(StreamSupport.stream(userRoundService.findAll().spliterator(), false).map(this::entityToDto));
    }

    @GetMapping("/resetUserRounds")
    public ResponseEntity<?> resetUserRounds(@RequestParam("roundId") Long roundId) {
        Optional<Round> round = roundService.findById(roundId);
        if (round.isEmpty()) {
            return ResponseEntity.status(400).body("No round with id: " + roundId);
        }
        StreamSupport.stream(userRoundService.findByRound(round.get()).spliterator(), false).forEach(ur -> {
            ur.setMyShareOnTrackPoints(false);
            userRoundService.save(ur);
        });
        return ResponseEntity.status(200).body("Reset successfull");
    }

    public UserRoundDto entityToDto(UserRound ur) {
        return modelMapper.map(ur, UserRoundDto.class);
    }
}
