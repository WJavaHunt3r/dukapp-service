package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activityitems.ActivityItem;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.dto.ActivityItemDto;
import com.ktk.dukappservice.dto.UserDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.activity.ActivityService;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.users.UserService;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller()
@RequestMapping("/api/activityItem")
public class ActivityItemController {
    private ActivityService activityService;
    private ActivityItemService activityItemService;
    private UserService userService;
    private RoundService roundService;
    private ModelMapper modelMapper;

    public ActivityItemController(ActivityService activityService, ActivityItemService activityItemService, UserService userService, RoundService roundService, ModelMapper modelMapper) {
        this.activityService = activityService;
        this.activityItemService = activityItemService;
        this.userService = userService;
        this.roundService = roundService;
        this.modelMapper = modelMapper;
    }

    @PostMapping()
    public ResponseEntity<?> addActivityItem(@Valid @RequestBody ActivityItemDto activityItem) {
        Optional<Activity> activity = activityService.findById(activityItem.getActivity().getId());
        if (activity.isEmpty()) {
            return ResponseEntity.status(400).body("No activity found with id: " + activityItem.getActivity().getId());
        }

        Optional<User> user = userService.findById(activityItem.getUser().getId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user found with id: " + activityItem.getUser().getId());
        }

        Optional<Round> round = roundService.findById(activityItem.getRound().getId());
        if (round.isEmpty()) {
            return ResponseEntity.status(400).body("No round found with id: " + activityItem.getRound().getId());
        }

        Optional<User> createUser = userService.findById(activityItem.getCreateUser().getId());
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("CreateUser not found by id: " + activityItem.getCreateUser());
        }

        activityItemService.save(convertToEntity(activityItem, user.get(), createUser.get()));
        return ResponseEntity.status(200).build();
    }

    @PostMapping("/items")
    public ResponseEntity<?> addActivityItems(@Valid @RequestBody List<ActivityItemDto> activityItems) {
        activityItems.forEach(this::addActivityItem);
        return ResponseEntity.ok().body("Successfully added");

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivityItem(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied!");
        }
        Optional<ActivityItem> item = activityItemService.findById(id);
        if (item.isPresent()) {
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
                                              @Nullable @RequestParam("searchText") String searchText) {
        if (activityId != null) {
            return ResponseEntity.ok(activityItemService.findByActivity(activityId).stream().map(this::convertToDto));
        }

        return ResponseEntity.ok(activityItemService.fetchByQuery(userId, registeredInApp, roundId, searchText).stream().map(this::convertToDto));

    }

    private ActivityItem convertToEntity(ActivityItemDto dto, User user, User createUser) {
        ActivityItem activity = modelMapper.map(dto, ActivityItem.class);
        activity.setUser(user);
        activity.setCreateUser(createUser);
        activity.setCreateDateTime(LocalDateTime.now());
        return activity;
    }

    private ActivityItemDto convertToDto(ActivityItem activity) {
        ActivityItemDto dto = modelMapper.map(activity, ActivityItemDto.class);
        dto.setUser(modelMapper.map(activity.getUser(), UserDto.class));
        dto.setCreateUser(modelMapper.map(activity.getCreateUser(), UserDto.class));
        return dto;
    }
}
