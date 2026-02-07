package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.camps.Camp;
import com.ktk.dukappservice.data.camps.CampService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.usercamps.UserCamp;
import com.ktk.dukappservice.data.usercamps.UserCampService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.UserCampDto;
import com.ktk.dukappservice.mapper.UserCampMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/api/userCamp")
public class UserCampController {
    private final UserCampService userCampService;
    private final UserService userService;
    private final UserCampMapper userCampMapper;
    private final CampService campService;
    private final SeasonService seasonService;

    public UserCampController(UserCampService userCampService, UserService userService, UserCampMapper userCampMapper, CampService campService, SeasonService seasonService) {
        this.userCampService = userCampService;
        this.userService = userService;
        this.userCampMapper = userCampMapper;
        this.campService = campService;
        this.seasonService = seasonService;
    }

    @GetMapping()
    public ResponseEntity<?> getUserCamps(@Nullable @RequestParam("userId") Long userId,
                                       @Nullable @RequestParam("campId") Long campId,
                                       @Nullable @RequestParam("seasonYear") Integer seasonYear,
                                       @Nullable @RequestParam("participates") Boolean participates) {
        Optional<User> user = Optional.empty();
        Optional<Camp> camp = Optional.empty();
        Optional<Season> season = Optional.empty();
        if (userId != null) {
            user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(404).body("No user with id: " + userId);
            }
        }

        if (campId != null) {
            camp = campService.findById(campId);
            if (camp.isEmpty()) {
                return ResponseEntity.status(404).body("No camp with id: " + campId);
            }
        }

        if (seasonYear != null) {
            season = seasonService.findBySeasonYear(seasonYear);
            if (season.isEmpty()) {
                return ResponseEntity.status(404).body("No season with year: " + seasonYear);
            }
        }

        return ResponseEntity.status(200).body(userCampService.fetchByQuery(user.orElse(null), camp.orElse(null), season.orElse(null), participates).stream().map(userCampMapper::entityToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserCamp(@PathVariable Long id) {
        var userCamp = userCampService.findById(id);
        if (userCamp.isEmpty()) {
            return ResponseEntity.status(404).body("No userCamp with id: " + id);
        }
        return ResponseEntity.status(200).body(userCampMapper.entityToDto(userCamp.get()));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserCampByUser(@PathVariable Long userId) {
        var user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user with id: " + userId);
        }
        var userCamp = userCampService.findAllByUser(user.get());

        return ResponseEntity.status(200).body(userCamp.stream().map(userCampMapper::entityToDto));
    }

    @PostMapping()
    public ResponseEntity<?> postUserCamp(@Valid @RequestBody UserCampDto userCamp, @RequestParam Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user found with id: " + userId);
        }
        if (!user.get().isAdmin() || !userId.equals(userCamp.getUser().getId())) {
            return ResponseEntity.status(404).body("Unauthorized request");
        }
        UserCamp newUC = new UserCamp();
        newUC.setUser(user.get());
        return ResponseEntity.status(200).body(userCampMapper.entityToDto(userCampService.save(userCampMapper.dtoToEntity(userCamp, newUC))));
    }

    @PutMapping("/{userCampId}")
    public ResponseEntity<?> putUserCamp(@Valid @RequestBody UserCampDto userCampDto, @PathVariable Long userCampId) {
        Optional<UserCamp> userCamp = userCampService.findById(userCampId);
        if (userCamp.isEmpty() || !userCampDto.getId().equals(userCampId)) {
            return ResponseEntity.status(400).body("Invalid userCampId");
        }
        return ResponseEntity.status(200).body(userCampMapper.entityToDto(userCampService.save(userCampMapper.dtoToEntity(userCampDto, userCamp.get()))));
    }
}
