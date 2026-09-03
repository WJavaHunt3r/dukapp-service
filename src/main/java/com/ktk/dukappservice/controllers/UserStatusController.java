package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.mapper.UserStatusMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/userStatus")
public class UserStatusController {

    private final UserStatusService service;
    private final UserStatusMapper userStatusMapper;
    private final RoundService roundService;

    public UserStatusController(UserStatusService service, UserStatusMapper userStatusMapper, RoundService roundService) {
        this.service = service;
        this.userStatusMapper = userStatusMapper;

        this.roundService = roundService;
    }

    @GetMapping()
    public ResponseEntity<?> getAllUserStatus(@RequestParam(value = "seasonYear") Integer seasonYear,
                                              @RequestParam(value = "teamId", required = false) Long teamId,
                                              @RequestParam(value = "keyword", required = false) String keyword,
                                              Pageable pageable) {
        var round = roundService.getCurrentRound();
        return ResponseEntity.status(200).body(service.fetchByQuery(seasonYear, teamId, keyword, pageable).map((UserStatus entity) -> userStatusMapper.entityToDto(entity, round)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserStatus(@PathVariable Long id) {
        var userStatus = service.findById(id);
        if (userStatus.isEmpty()) {
            return ResponseEntity.status(404).body("No userStatus with id: " + id);
        }
        return ResponseEntity.status(200).body(userStatusMapper.entityToDto(userStatus.get(), roundService.getCurrentRound()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserStatusByUser(@PathVariable Long userId, @RequestParam("seasonYear") Integer seasonYear) {
        var userStatus = service.findByUserIdAndSeason(userId, seasonYear);
        if (userStatus.isEmpty()) {
            return ResponseEntity.status(404).body("No userStatus with userId: " + userId);
        }

        return ResponseEntity.status(200).body(userStatus.map((UserStatus entity) -> userStatusMapper.entityToDto(entity, roundService.getCurrentRound())));
    }

    @PostMapping("/setUserStatus")
    public ResponseEntity<?> setUserStatusForYear(@RequestParam("seasonYear") Integer year) {
        service.createUserStatusForAllUsers(year);
        return ResponseEntity.status(200).body("All User Status created");
    }
}
