package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.paceteam.PaceTeamService;
import com.ktk.dukappservice.data.paceteamround.PaceTeamRoundService;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.UserDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.mapper.UserMapper;
import com.ktk.dukappservice.service.UserFamilyImportService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final PaceTeamService paceTeamService;
    private final UserMapper userMapper;
    private final SeasonService seasonService;
    private final PaceTeamRoundService paceTeamRoundService;

    public UserController(UserService userService, PaceTeamService paceTeamService, UserMapper modelMapper, SeasonService seasonService, PaceTeamRoundService paceTeamRoundService, UserFamilyImportService userFamilyImportService) {
        this.userService = userService;
        this.paceTeamService = paceTeamService;
        this.userMapper = modelMapper;
        this.seasonService = seasonService;
        this.paceTeamRoundService = paceTeamRoundService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> userById = userService.findById(id);
        if (userById.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userById.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userById = userService.findByUsername(userDetails.getUsername());
        if (userById.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userById.get()));
        }
        return ResponseEntity.status(404).body("User not found");

    }

    @GetMapping("/myShare/{myShareId}")
    public ResponseEntity<?> getUserByMYShare(@PathVariable Long myShareId) {
        Optional<User> userByMyShareId = userService.findByMyShareId(myShareId);
        if (userByMyShareId.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userByMyShareId.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        Optional<User> userByMyShareId = userService.findByUsername(username);
        if (userByMyShareId.isPresent()) {
            return ResponseEntity.status(200).body(userMapper.entityToDto(userByMyShareId.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping("/me/family")
    public ResponseEntity<?> getFamily(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> user = userService.findByUsername(userDetails.getUsername());
        if (user.isPresent()) {
            if (user.get().getAge() <= 18) {
                return ResponseEntity.status(404).body("No kids");
            }
            return ResponseEntity.status(200).body(userService.findFamily(user.get().getFamilyId(), user.get().getId()).stream().map(userMapper::entityToDto));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam(value = "teamId", required = false) Long teamId,
                                      @RequestParam(value = "familyId", required = false) Long familyId,
                                      @RequestParam(value = "churchId", required = false) Long churchId,
                                      @RequestParam(value = "spouseId", required = false) Long spouseId,
                                      @RequestParam(value = "keyword", required = false) String keyword, Pageable pageable) {
        return ResponseEntity.status(200).body(userService.fetchByQuery(familyId, spouseId, teamId, churchId, keyword, pageable).map(userMapper::entityToDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putUser(@Valid @RequestBody UserDto userDto, @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> createUser = userService.findByUsername(userDetails.getUsername());
        if (createUser.isPresent() && createUser.get().getRole().equals(Role.USER) && !userDto.getId().equals(id)) {
            return ResponseEntity.status(403).body("Permission denied:");
        }
        Optional<User> user = userService.findById(userDto.getId());
        if (user.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + userDto.getId());
        }
        return ResponseEntity.status(200).body(userMapper.entityToDto(userService.save(userMapper.dtoToEntity(userDto, user.get()))));
    }

    @GetMapping("/setPaceTeams")
    public ResponseEntity<?> setPaceTeams() {
        paceTeamRoundService.createTeamRounds();
        return ResponseEntity.status(200).body("Pace Teams set");
    }

}
