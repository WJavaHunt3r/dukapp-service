package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.userfrakarestreak.UserFraKareWeek;
import com.ktk.dukappservice.data.userfrakarestreak.UserFraKareWeekService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.mapper.FraKareStreakMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/userFraKareWeek")
public class UserFraKareWeekController {
    private final UserService userService;
    private final UserFraKareWeekService userFraKareWeekService;
    private final FraKareStreakMapper fraKareStreakMapper;

    public UserFraKareWeekController(UserService userService, UserFraKareWeekService userFraKareWeekService, FraKareStreakMapper fraKareStreakMapper) {
        this.userService = userService;
        this.userFraKareWeekService = userFraKareWeekService;
        this.fraKareStreakMapper = fraKareStreakMapper;
    }

    @GetMapping()
    public ResponseEntity getUserFraKareWeeks(@Nullable @RequestParam("userId") Long userId,
                                              @Nullable @RequestParam("weekNumber") Integer weekNumber,
                                              @Nullable @RequestParam("teamId") Long teamId,
                                              @Nullable @RequestParam("listened") Boolean listened,
                                              @Nullable @RequestParam("seasonYear") Integer seasonYear) {
        return ResponseEntity.status(200).body(userFraKareWeekService.fetchByQuery(userId, weekNumber, listened, teamId, seasonYear).stream().map((fraKareStreak -> fraKareStreakMapper.entityToDto(fraKareStreak))));
    }

    @GetMapping("/{id}")
    public ResponseEntity getUserFraKareWeek(@PathVariable Long id) {
        var fraKareStreak = userFraKareWeekService.findById(id);
        if (fraKareStreak.isEmpty()) {
            return ResponseEntity.status(404).body("No fraKareStreak with id: " + id);
        }
        return ResponseEntity.status(200).body(fraKareStreak.get());
    }

    @PutMapping("/{id}/setListened")
    public ResponseEntity setUserFraKareWeekListened(@RequestParam("listened") boolean listened, @PathVariable Long id) {
        Optional<UserFraKareWeek> fraKareStreak = userFraKareWeekService.findById(id);
        if (fraKareStreak.isEmpty()) {
            return ResponseEntity.status(400).body("Invalid fraKareStreakId");
        }

        return ResponseEntity.status(200).body(fraKareStreakMapper.entityToDto(userFraKareWeekService.setListened(fraKareStreak.get(), listened)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUserFraKareWeek(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }

        if (!user.get().isAdmin() && !user.get().isTeamLeader()) {
            return ResponseEntity.status(401).body("Permission denied");
        }

        Optional<UserFraKareWeek> item = userFraKareWeekService.findById(id);
        if (item.isPresent()) {
            userFraKareWeekService.deleteById(id);
            return ResponseEntity.status(200).body("Delete successful");
        }

        return ResponseEntity.status(404).body("No fraKareStreak item found with id:" + id);

    }
}
