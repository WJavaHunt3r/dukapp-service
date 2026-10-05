package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.notifications.*;
import com.ktk.dukappservice.data.roles.AppRoleService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.DeviceTokenDto;
import com.ktk.dukappservice.dto.GeneralNotificationDto;
import com.ktk.dukappservice.dto.NotificationPreferenceDto;
import com.ktk.dukappservice.dto.NotificationScheduleDto;
import com.ktk.dukappservice.dto.OverdueJobsDto;
import com.ktk.dukappservice.dto.OverdueRemindDto;
import com.ktk.dukappservice.enums.NotificationType;
import com.ktk.dukappservice.service.notifications.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final UserService userService;
    private final AppRoleService appRoleService;
    private final DeviceTokenService deviceTokenService;
    private final NotificationPreferenceService preferenceService;
    private final NotificationScheduleService scheduleService;
    private final GeneralNotificationRepository generalNotificationRepository;
    private final PushNotificationService pushNotificationService;

    public NotificationController(UserService userService, AppRoleService appRoleService, DeviceTokenService deviceTokenService,
                                  NotificationPreferenceService preferenceService, NotificationScheduleService scheduleService,
                                  GeneralNotificationRepository generalNotificationRepository,
                                  PushNotificationService pushNotificationService) {
        this.userService = userService;
        this.appRoleService = appRoleService;
        this.deviceTokenService = deviceTokenService;
        this.preferenceService = preferenceService;
        this.scheduleService = scheduleService;
        this.generalNotificationRepository = generalNotificationRepository;
        this.pushNotificationService = pushNotificationService;
    }

    // ---------------------------------------------------------------- devices (any logged-in user, for themselves)

    /** Call after login / whenever the Firebase SDK hands out a (new) token. */
    @PostMapping("/devices")
    public ResponseEntity<?> registerDevice(@Valid @RequestBody DeviceTokenDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        deviceTokenService.register(userService.getCurrentUser(userDetails), dto.getToken(), dto.getPlatform());
        return ResponseEntity.ok("Device registered");
    }

    /** Call on logout, so the device stops receiving this user's notifications. */
    @DeleteMapping("/devices")
    public ResponseEntity<?> unregisterDevice(@RequestParam("token") String token, @AuthenticationPrincipal UserDetails userDetails) {
        boolean removed = deviceTokenService.unregister(userService.getCurrentUser(userDetails), token);
        return removed ? ResponseEntity.ok("Device unregistered") : ResponseEntity.status(404).body("Device not found");
    }

    // ---------------------------------------------------------------- preferences (current user)

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(toDtos(preferenceService.getPreferences(userService.getCurrentUser(userDetails))));
    }

    /** Body: {"JOB_NEW": false, "ON_TRACK_EMAIL": true, ...}; types left out keep their setting. */
    @PutMapping("/preferences")
    public ResponseEntity<?> putPreferences(@RequestBody Map<NotificationType, Boolean> changes, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(toDtos(preferenceService.updatePreferences(userService.getCurrentUser(userDetails), changes)));
    }

    // ---------------------------------------------------------------- one-off notifications

    @GetMapping("/general")
    @PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
    public ResponseEntity<?> getGeneralNotifications(
            @PageableDefault(size = 20, sort = GeneralNotification.Fields.sentDateTime, direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(generalNotificationRepository.findAll(pageable).map(this::toDto));
    }

    /** Sends immediately to all users (empty roleIds) or the users of the given roles. */
    @PostMapping("/general")
    @PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
    public ResponseEntity<?> sendGeneralNotification(@Valid @RequestBody GeneralNotificationDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        Set<Long> roleIds = dto.getRoleIds() == null ? Set.of() : dto.getRoleIds();
        ResponseEntity<?> invalidRoles = checkRoles(roleIds);
        if (invalidRoles != null) {
            return invalidRoles;
        }
        User sender = userService.getCurrentUser(userDetails);
        return ResponseEntity.ok(toDto(pushNotificationService.sendGeneral(dto.getTitle().trim(), dto.getBody().trim(), roleIds, sender)));
    }

    // ---------------------------------------------------------------- jobs that were not closed

    /** Users who are responsible for jobs that are over but still wait for their hours. */
    @GetMapping("/overdue-jobs")
    @PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
    public ResponseEntity<?> getOverdueJobs() {
        List<OverdueJobsDto> result = new ArrayList<>();
        pushNotificationService.overdueJobsByResponsible().forEach((user, jobs) -> result.add(new OverdueJobsDto(
                user.getId(), user.getFullName(),
                jobs.stream().map(j -> new OverdueJobsDto.Job(j.getId(), j.getDescription(), j.getJobDateTime(), j.getJobEndDateTime())).toList())));
        return ResponseEntity.ok(result);
    }

    /** Sends the "close your job" notification to the given users (empty = all who have unclosed jobs). */
    @PostMapping("/overdue-jobs/remind")
    @PreAuthorize("hasAuthority('NOTIFICATION_SEND')")
    public ResponseEntity<?> remindOverdue(@RequestBody OverdueRemindDto body) {
        return ResponseEntity.ok(pushNotificationService.remindOverdue(body.getUserIds() == null ? Set.of() : body.getUserIds()));
    }

    // ---------------------------------------------------------------- weekly schedules (push + on-track e-mail)

    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE_MANAGE')")
    public ResponseEntity<?> getSchedules() {
        List<NotificationScheduleDto> result = new ArrayList<>();
        scheduleService.findAll().forEach(s -> result.add(toDto(s)));
        result.sort(Comparator.comparing(NotificationScheduleDto::getType).thenComparing(NotificationScheduleDto::getId));
        return ResponseEntity.ok(result);
    }

    @PostMapping("/schedules")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE_MANAGE')")
    public ResponseEntity<?> postSchedule(@Valid @RequestBody NotificationScheduleDto dto) {
        if (dto.getType() != null && dto.getType() != NotificationType.WEEKLY) {
            return ResponseEntity.status(400).body("Only WEEKLY schedules can be created");
        }
        NotificationSchedule schedule = new NotificationSchedule();
        schedule.setType(NotificationType.WEEKLY);
        return saveSchedule(dto, schedule);
    }

    /** Also used for the ON_TRACK_EMAIL schedule (day, time and active only). */
    @PutMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE_MANAGE')")
    public ResponseEntity<?> putSchedule(@Valid @RequestBody NotificationScheduleDto dto, @PathVariable Long id) {
        Optional<NotificationSchedule> schedule = scheduleService.findById(id);
        if (schedule.isEmpty() || !id.equals(dto.getId())) {
            return ResponseEntity.status(400).body("Invalid scheduleId");
        }
        if (dto.getType() != null && dto.getType() != schedule.get().getType()) {
            return ResponseEntity.status(400).body("The type of a schedule can't be changed");
        }
        return saveSchedule(dto, schedule.get());
    }

    @DeleteMapping("/schedules/{id}")
    @PreAuthorize("hasAuthority('NOTIFICATION_SCHEDULE_MANAGE')")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        Optional<NotificationSchedule> schedule = scheduleService.findById(id);
        if (schedule.isEmpty()) {
            return ResponseEntity.status(404).body("No schedule with id: " + id);
        }
        if (schedule.get().getType() == NotificationType.ON_TRACK_EMAIL) {
            return ResponseEntity.status(409).body("The on-track e-mail schedule can't be deleted, set it inactive instead");
        }
        scheduleService.deleteById(id);
        return ResponseEntity.ok("Delete successful");
    }

    private ResponseEntity<?> saveSchedule(NotificationScheduleDto dto, NotificationSchedule schedule) {
        if (schedule.getType() == NotificationType.WEEKLY) {
            if (isBlank(dto.getTitle()) || isBlank(dto.getBody())) {
                return ResponseEntity.status(400).body("Title and body are required");
            }
            Set<Long> roleIds = dto.getRoleIds() == null ? Set.of() : dto.getRoleIds();
            ResponseEntity<?> invalidRoles = checkRoles(roleIds);
            if (invalidRoles != null) {
                return invalidRoles;
            }
            schedule.setTitle(dto.getTitle().trim());
            schedule.setBody(dto.getBody().trim());
            schedule.getRoleIds().clear();
            schedule.getRoleIds().addAll(roleIds);
        }
        schedule.setDayOfWeek(dto.getDayOfWeek());
        schedule.setTime(dto.getTime());
        schedule.setActive(dto.isActive());
        return ResponseEntity.ok(toDto(scheduleService.save(schedule)));
    }

    private ResponseEntity<?> checkRoles(Set<Long> roleIds) {
        for (Long roleId : roleIds) {
            if (roleId == null || !appRoleService.existsById(roleId)) {
                return ResponseEntity.status(400).body("No role with id: " + roleId);
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static List<NotificationPreferenceDto> toDtos(Map<NotificationType, Boolean> preferences) {
        return preferences.entrySet().stream()
                .map(e -> new NotificationPreferenceDto(e.getKey(), e.getKey().getChannel(), e.getValue()))
                .toList();
    }

    private GeneralNotificationDto toDto(GeneralNotification notification) {
        GeneralNotificationDto dto = new GeneralNotificationDto();
        dto.setId(notification.getId());
        dto.setTitle(notification.getTitle());
        dto.setBody(notification.getBody());
        dto.setRoleIds(new HashSet<>(notification.getRoleIds()));
        if (notification.getSentBy() != null) {
            dto.setSentById(notification.getSentBy().getId());
            dto.setSentByName(notification.getSentBy().getFullName());
        }
        dto.setSentDateTime(notification.getSentDateTime());
        dto.setRecipientUsers(notification.getRecipientUsers());
        dto.setDelivered(notification.getDelivered());
        dto.setFailed(notification.getFailed());
        return dto;
    }

    private NotificationScheduleDto toDto(NotificationSchedule schedule) {
        NotificationScheduleDto dto = new NotificationScheduleDto();
        dto.setId(schedule.getId());
        dto.setType(schedule.getType());
        dto.setTitle(schedule.getTitle());
        dto.setBody(schedule.getBody());
        dto.setDayOfWeek(schedule.getDayOfWeek());
        dto.setTime(schedule.getTime());
        dto.setActive(schedule.isActive());
        dto.setRoleIds(new HashSet<>(schedule.getRoleIds()));
        dto.setLastSentDateTime(schedule.getLastSentDateTime());
        return dto;
    }
}
