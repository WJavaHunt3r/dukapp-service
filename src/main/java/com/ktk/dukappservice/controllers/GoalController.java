package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.goals.Goal;
import com.ktk.dukappservice.data.goals.GoalService;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.dto.GoalDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.mapper.GoalMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/goal")
public class GoalController {

    private final GoalService goalService;
    private final UserService userService;
    private final SeasonService seasonService;
    private final GoalMapper goalMapper;
    private final PaceUserRoundService userRoundService;
    private final UserStatusService userStatusService;

    public GoalController(GoalService goalService, UserService userService, SeasonService seasonService, GoalMapper goalMapper, PaceUserRoundService userRoundService, UserStatusService userStatusService, PaceUserRoundService paceUserRoundService) {
        this.goalService = goalService;
        this.userService = userService;
        this.seasonService = seasonService;
        this.goalMapper = goalMapper;
        this.userRoundService = userRoundService;
        this.userStatusService = userStatusService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGoalById(@PathVariable Long id) {
        Optional<Goal> goal = goalService.findById(id);
        if (goal.isEmpty()) {
            return ResponseEntity.status(404).body("No goal found with id: " + goal);
        }
        return ResponseEntity.status(200).body(goalMapper.entityToDto(goal.get()));
    }

    @GetMapping()
    public ResponseEntity<?> getAllGoals(@RequestParam(value = "seasonYear", required = false) Integer seasonYear,
                                         @RequestParam(value = "userId", required = false) Long userId,
                                         Pageable pageable) {
        Page<Goal> goals = goalService.fetchByQuery(seasonYear, userId, pageable);
        return ResponseEntity.status(200).body(goals.map(goalMapper::entityToDto));
    }

    @PostMapping()
    public ResponseEntity<?> saveGoal(@Valid @RequestBody GoalDto goalDto, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = userService.findById(goalDto.getUserId());
        if (user.isEmpty() || !goalService.fetchByQuery(goalDto.getSeasonYear(), goalDto.getUserId(), null).isEmpty()) {
            return ResponseEntity.status(404).body("No user found with id: " + goalDto.getUserId() + ". Or User already has a goal.");
        }
        Optional<Season> season = seasonService.findBySeasonYear(goalDto.getSeasonYear());
        if (season.isEmpty()) {
            return ResponseEntity.status(404).body("No Season found with the year: " + goalDto.getSeasonYear());
        }
        Goal goal = new Goal();
        goal.setUser(user.get());
        goal.setSeason(season.get());
        Goal goalEntity  = goalService.save(goalMapper.dtoToEntity(goalDto, goal));
        userStatusService.createUserStatus(user.get(), goalDto.getGoal(), season.get());
        userRoundService.createPaceUserRound(user.get());


        return ResponseEntity.status(200).body(goalMapper.entityToDto(goalEntity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editGoal(@Valid @RequestBody GoalDto goalDto, @RequestParam("userId") Long userId, @PathVariable Long id) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user found with id: " + userId);
        }
//        if (user.get().getRole() == Role.ADMIN) {
            Optional<Goal> goal = goalService.findById(id);
            if (goal.isEmpty() || !goalDto.getId().equals(id)) {
                return ResponseEntity.status(404).body("Goal not found with id: " + id);
            }

            Goal entity = goalService.save(goalMapper.dtoToEntity(goalDto, goal.get()));
            userRoundService.calculateUserRoundStatus(entity.getUser());
            userStatusService.calculateUserStatus(goal.get().getUser(), goal.get().getGoal());
            return ResponseEntity.status(200).body(goalMapper.entityToDto(entity));
//        }
//        return ResponseEntity.status(404).body("User not allowed to change this goal");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteGoal(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied!");
        }
        if (goalService.existsById(id)) {
            Optional<Goal> g = goalService.findById(id);
            Optional<UserStatus> us = userStatusService.findByUserIdAndSeason(g.get().getUser().getId(), g.get().getSeason().getSeasonYear());
            us.ifPresent(u -> userStatusService.deleteById(u.getId()));
//            userRoundService.deleteByUserAndSeason(g.get().getUser(), g.get().getSeason());
            goalService.deleteById(id);
            return ResponseEntity.status(200).body("Delete successful");
        }
        return ResponseEntity.status(403).body("No goal found with id:" + id);

    }

}
