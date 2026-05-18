package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activity.ActivityService;
import com.ktk.dukappservice.data.activityitems.ActivityItem;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.ActivityItemDto;
import com.ktk.dukappservice.enums.Role;
import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller()
@RequestMapping("/api/activityItem")
public class ActivityItemController {
    private final ActivityService activityService;
    private final ActivityItemService activityItemService;
    private final UserService userService;
    private final RoundService roundService;
    private final ModelMapper modelMapper;

    public ActivityItemController(ActivityService activityService, ActivityItemService activityItemService, UserService userService, RoundService roundService, ModelMapper modelMapper) {
        this.activityService = activityService;
        this.activityItemService = activityItemService;
        this.userService = userService;
        this.roundService = roundService;
        this.modelMapper = modelMapper;
    }

    @PostMapping()
    public ResponseEntity<?> addActivityItem(@Valid @RequestBody ActivityItemDto activityItem, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<Activity> activity = activityService.findById(activityItem.getActivityId());
        if (activity.isEmpty()) {
            return ResponseEntity.status(400).body("No activity found with id: " + activityItem.getActivityId());
        }

        Optional<User> user = userService.findById(activityItem.getUserId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user found with id: " + activityItem.getUserId());
        }

        Optional<Round> round = roundService.findById(activityItem.getRoundId());
        if (round.isEmpty()) {
            return ResponseEntity.status(400).body("No round found with id: " + activityItem.getRoundId());
        }

        Optional<User> createUser = userService.findByUsername(userDetails.getUsername());
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("CreateUser not found by id: " + activityItem.getCreateUserId());
        }

        activityItemService.save(convertToEntity(activityItem, user.get(), createUser.get(), activity.get(), round.get()));
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addActivityItems(@Valid @RequestBody List<ActivityItemDto> activityItems, @AuthenticationPrincipal UserDetails userDetails) {
        activityItems.forEach((e) -> addActivityItem(e, userDetails));
        return ResponseEntity.ok().body("Successfully added");

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivityItem(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with username:" + userDetails.getUsername());
        }

        Optional<ActivityItem> item = activityItemService.findById(id);
        if (item.isPresent()) {
            if (!user.get().getRole().equals(Role.ADMIN) && !Objects.equals(user.get().getId(), item.get().getCreateUser().getId())) {
                return ResponseEntity.status(403).body("Permission denied!");
            }
            if (item.get().getActivity().isRegisteredInApp()) {
                return ResponseEntity.status(400).body("Activity already registered. Can't modify.");
            }
            activityItemService.deleteById(id);
            return ResponseEntity.status(200).body("Delete successful");
        }

        return ResponseEntity.status(403).body("No activity item found with id:" + id);

    }

    @GetMapping()
    public ResponseEntity<?> getActivityItems(@Nullable @RequestParam("activityId") Long activityId,
                                              @Nullable @RequestParam("userId") Long userId,
                                              @Nullable @RequestParam("registeredInApp") Boolean registeredInApp,
                                              @Nullable @RequestParam("roundId") Long roundId,
                                              @Nullable @RequestParam("searchText") String searchText, Pageable pageable) {

        return ResponseEntity.ok(activityItemService.fetchByQuery(activityId, userId, registeredInApp, roundId, searchText, pageable).map(this::convertToDto));

    }

    private ActivityItem convertToEntity(ActivityItemDto dto, User user, User createUser, Activity activity, Round round) {
        ActivityItem activityitem = modelMapper.map(dto, ActivityItem.class);
        activityitem.setUser(user);
        activityitem.setCreateUser(createUser);
        activityitem.setCreateDateTime(LocalDateTime.now());
        activityitem.setActivity(activity);
        activityitem.setRound(round);
        return activityitem;
    }

    private ActivityItemDto convertToDto(ActivityItem activity) {
        ActivityItemDto dto = modelMapper.map(activity, ActivityItemDto.class);
        dto.setUserName(activity.getUser().getFullName());
        dto.setUserId(activity.getUser().getId());
        dto.setCreateUserName(activity.getCreateUser().getFullName());
        dto.setCreateUserId(activity.getCreateUser().getId());
        dto.setActivityId(activity.getActivity().getId());
        dto.setRoundId(activity.getRound().getId());
        return dto;
    }
}
