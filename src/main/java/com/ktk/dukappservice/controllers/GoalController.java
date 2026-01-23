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
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

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

    @GetMapping("/userSeasonGoal")
    public ResponseEntity<?> getUserSeasonGoal(@RequestParam("userId") Long userId, @RequestParam("seasonYear") Integer seasonYear) {
        var user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user with given id: " + userId);
        }
        Optional<Season> season = seasonService.findBySeasonYear(seasonYear);
        if (season.isEmpty()) {
            return ResponseEntity.status(404).body("No season with given id: " + seasonYear);
        }
        Optional<Goal> goal = goalService.findByUserAndSeasonYear(user.get(), seasonYear);
        if (goal.isEmpty()) {
            return ResponseEntity.status(404).body("No goal found with userId: " + userId + " in season: " + seasonYear);
        }
        return ResponseEntity.status(200).body(goalMapper.entityToDto(goal.get()));
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
    public ResponseEntity<?> getAllGoals(@Nullable @RequestParam("seasonYear") Integer seasonYear) {
        if (seasonYear == null) {
            return ResponseEntity.status(200).body(StreamSupport.stream(goalService.findAll().spliterator(), false).map(goalMapper::entityToDto));
        }
        Optional<Season> season = seasonService.findBySeasonYear(seasonYear);
        if (season.isEmpty()) {
            return ResponseEntity.status(404).body("No season with year: " + seasonYear);
        }
        List<Goal> goals = goalService.findBySeason(season.get());
        return ResponseEntity.status(200).body(goals.stream().map(goalMapper::entityToDto));
    }

    @PostMapping()
    public ResponseEntity<?> saveGoal(@Valid @RequestBody GoalDto goalDto) {
        Optional<User> user = userService.findById(goalDto.getUser().getId());
        if (user.isEmpty() || goalService.findByUserAndSeasonYear(user.get(), goalDto.getSeason().getSeasonYear()).isPresent()) {
            return ResponseEntity.status(404).body("No user found with id: " + goalDto.getUser().getId() + ". Or User already has a goal.");
        }
        Goal goal = new Goal();
        goal.setUser(user.get());
        userRoundService.createPaceUserRound(user.get());
        userStatusService.createUserStatus(user.get(), goalDto.getGoal(), goalDto.getSeason());
        return ResponseEntity.status(200).body(goalMapper.entityToDto(goalService.save(goalMapper.dtoToEntity(goalDto, goal))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editGoal(@Valid @RequestBody GoalDto goalDto, @RequestParam("userId") Long userId, @PathVariable Long id) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user found with id: " + userId);
        }
        if (user.get().getRole() == Role.ADMIN) {
            Optional<Goal> goal = goalService.findById(id);
            if (goal.isEmpty() || !goalDto.getId().equals(id)) {
                return ResponseEntity.status(404).body("Goal not found with id: " + id);
            }

            Goal entity = goalService.save(goalMapper.dtoToEntity(goalDto, goal.get()));
            userRoundService.calculateUserRoundStatus(entity.getUser());
            userStatusService.calculateUserStatus(goal.get().getUser(), goal.get().getGoal());
            return ResponseEntity.status(200).body(goalMapper.entityToDto(entity));
        }
        return ResponseEntity.status(404).body("User not allowed to change this goal");
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
            Optional<UserStatus> us = userStatusService.findByUserId(g.get().getUser().getId(), g.get().getSeason().getSeasonYear());
            us.ifPresent(u -> userStatusService.deleteById(u.getId()));
//            userRoundService.deleteByUserAndSeason(g.get().getUser(), g.get().getSeason());
            goalService.deleteById(id);
            return ResponseEntity.status(200).body("Delete successful");
        }
        return ResponseEntity.status(403).body("No goal found with id:" + id);

    }

}
