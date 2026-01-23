package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.mapper.UserStatusMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.util.annotation.Nullable;

@RestController
@RequestMapping("/api/userStatus")
public class UserStatusController {

    private final UserStatusService service;
    private final UserStatusMapper userStatusMapper;

    public UserStatusController(UserStatusService service, UserStatusMapper userStatusMapper) {
        this.service = service;
        this.userStatusMapper = userStatusMapper;
    }

    @GetMapping()
    public ResponseEntity<?> getAllUserStatus(@RequestParam("seasonYear") Integer seasonYear, @Nullable @RequestParam("teamId") Long teamId) {
        return ResponseEntity.status(200).body(service.fetchByQuery(seasonYear, teamId).stream().map(userStatusMapper::entityToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserStatus(@PathVariable Long id) {
        var userStatus = service.findById(id);
        if (userStatus.isEmpty()) {
            return ResponseEntity.status(404).body("No userStatus with id: " + id);
        }
        return ResponseEntity.status(200).body(userStatusMapper.entityToDto(userStatus.get()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserStatusByUser(@PathVariable Long userId, @RequestParam("seasonYear") Integer seasonYear) {
        var userStatus = service.findByUserId(userId, seasonYear);
        if (userStatus.isEmpty()) {
            return ResponseEntity.status(404).body("No userStatus with userId: " + userId);
        }

        return ResponseEntity.status(200).body(userStatus.map(userStatusMapper::entityToDto));
    }

    @PostMapping("/setUserStatus")
    public ResponseEntity<?> setUserStatusForYear(@RequestParam("seasonYear") Integer year) {
        service.createUserStatusForAllUsers(year);
        return ResponseEntity.status(200).body("All User Status created");
    }
}
