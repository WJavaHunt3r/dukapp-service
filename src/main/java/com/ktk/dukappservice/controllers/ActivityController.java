package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.activity.Activity;
import com.ktk.dukappservice.data.activity.ActivityService;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.ActivityDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.mapper.ActivityMapper;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import com.microsoft.graph.models.odataerrors.ODataError;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/activity")
public class ActivityController {

    private final ActivityService activityService;
    private final UserService userService;
    private final ActivityMapper activityMapper;
    private final ActivityItemService activityItemService;
    private final MicrosoftService microsoftService;

    public ActivityController(ActivityService activityService, UserService userService, ActivityMapper activityMapper, ActivityItemService activityItemService, MicrosoftService microsoftService) {
        this.activityService = activityService;
        this.userService = userService;
        this.activityMapper = activityMapper;
        this.activityItemService = activityItemService;
        this.microsoftService = microsoftService;
    }

    @GetMapping()
    public ResponseEntity<?> getActivities(@Nullable @RequestParam("responsibleId") Long responsibleId,
                                           @Nullable @RequestParam("employerId") Long employerId,
                                           @Nullable @RequestParam("registeredInApp") Boolean registeredInApp,
                                           @Nullable @RequestParam("registeredInMyShare") Boolean registeredInMyShare,
                                           @Nullable @RequestParam("createUserId") Long createUserId,
                                           @Nullable @RequestParam("referenceDate") String referenceMonth,
                                           @Nullable @RequestParam("searchText") String searchText) {
        return ResponseEntity.status(200).body(activityService.fetchByQuery(responsibleId, employerId, registeredInApp, registeredInMyShare, createUserId, referenceMonth, searchText).stream().map((activity -> activityMapper.entityToDto(activity))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getActivity(@PathVariable Long id) {
        var activity = activityService.findById(id);
        if (activity.isEmpty()) {
            return ResponseEntity.status(404).body("No activity with id: " + id);
        }
        return ResponseEntity.status(200).body(activity.get());
    }

    @PostMapping()
    public ResponseEntity<?> postActivity(@Valid @RequestBody ActivityDto activity) {
        Optional<User> createUser = userService.findById(activity.getCreateUser().getId());
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + activity.getCreateUser().getId());
        }
        Optional<User> employer = userService.findById(activity.getEmployer().getId());
        if (employer.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + activity.getEmployer().getId());
        }
        Optional<User> responsibleUser = userService.findById(activity.getResponsible().getId());
        if (responsibleUser.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + activity.getResponsible().getId());
        }
        Activity entity = new Activity();
        entity.setCreateUser(createUser.get());
        entity.setResponsible(responsibleUser.get());
        entity.setEmployer(employer.get());
        return ResponseEntity.status(200).body(activityMapper.entityToDto(activityService.save(activityMapper.dtoToEntity(activity, entity))));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putActivity(@Valid @RequestBody ActivityDto activityDto, @PathVariable Long id) {
        Optional<Activity> activity = activityService.findById(id);
        if (activity.isEmpty() || !activityDto.getId().equals(id)) {
            return ResponseEntity.status(400).body("Invalid activityId");
        }
        return ResponseEntity.status(200).body(activityMapper.entityToDto(activityService.save(activityMapper.dtoToEntity(activityDto, activity.get()))));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteActivity(@PathVariable Long id, @RequestParam("userId") Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        Optional<Activity> item = activityService.findById(id);
        if (item.isPresent()) {
            if (!item.get().getCreateUser().getId().equals(user.get().getId()) && !user.get().getRole().equals(Role.ADMIN)) {
                return ResponseEntity.status(403).body("Permission denied!");
            }
            if (item.get().isRegisteredInApp() || item.get().isRegisteredInMyShare()) {
                return ResponseEntity.status(400).body("Activity already registered. Can't modify.");
            }
            activityItemService.deleteByActivityId(item.get().getId());
            activityService.deleteById(id);
            return ResponseEntity.status(200).body("Delete successful");
        }

        return ResponseEntity.status(403).body("No activity item found with id:" + id);

    }

    @PostMapping("/{id}/register")
    public ResponseEntity<?> registerActivity(@PathVariable Long id, @RequestParam Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied:");
        }
        Optional<Activity> activity = activityService.findById(id);
        if (activity.isEmpty()) {
            return ResponseEntity.status(400).body("No activity with id:" + id);
        }
        if (activity.get().isRegisteredInApp()) {
            return ResponseEntity.status(400).body("Activity already registered!");
        }

        Long transactionId = null;
        try {
            transactionId = activityService.registerActivity(activity.get(), user.get());
            microsoftService.sendActivityToSharePointListItem(activity.get());
            activity.get().setRegisteredInTeams(true);
        } catch (Exception e) {
            if (e instanceof ODataError) {
                ((ODataError) e).getError().getCode();
                ((ODataError) e).getError().getMessage();
            }
            activityService.rollbackTransactions(transactionId);
            return ResponseEntity.status(500).body(e.toString());
        }
        activityService.save(activity.get());

        return ResponseEntity.status(200).body("Registration successful");
    }

    @PostMapping("/{id}/registerInTeams")
    public ResponseEntity<?> registerActivityInTeams(@PathVariable Long id, @RequestParam Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userId);
        }
        if (user.get().getRole().equals(Role.USER)) {
            return ResponseEntity.status(403).body("Permission denied:");
        }
        Optional<Activity> activity = activityService.findById(id);
        if (activity.isEmpty()) {
            return ResponseEntity.status(400).body("No activity with id:" + id);
        }
        if (activity.get().isRegisteredInTeams()) {
            return ResponseEntity.status(400).body("Activity already registered!");
        }

        try {
            microsoftService.sendActivityToSharePointListItem(activity.get());
            activity.get().setRegisteredInTeams(true);
            activityService.save(activity.get());

            return ResponseEntity.status(200).body("Successfully registered in Teams");
        } catch (Exception e) {
            if (e instanceof ODataError) {
                Objects.requireNonNull(((ODataError) e).getError()).getCode();
                ((ODataError) e).getError().getMessage();
            }
            return ResponseEntity.status(500).body(e.toString());
        }
    }

}
