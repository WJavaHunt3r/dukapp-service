package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.paceteam.PaceTeamService;
import com.ktk.dukappservice.data.paceteamround.PaceTeamRoundService;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.UserDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.mapper.UserMapper;
import com.ktk.dukappservice.service.UserFamilyImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final PaceTeamService paceTeamService;
    private final UserMapper userMapper;
    private final SeasonService seasonService;
    private final PaceTeamRoundService paceTeamRoundService;
    private final UserFamilyImportService userFamilyImportService;

    public UserController(UserService userService, PaceTeamService paceTeamService, UserMapper modelMapper, SeasonService seasonService, PaceTeamRoundService paceTeamRoundService, UserFamilyImportService userFamilyImportService) {
        this.userService = userService;
        this.paceTeamService = paceTeamService;
        this.userMapper = modelMapper;
        this.seasonService = seasonService;
        this.paceTeamRoundService = paceTeamRoundService;
        this.userFamilyImportService = userFamilyImportService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> userById = userService.findById(id);
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

    @GetMapping("/{id}/children")
    public ResponseEntity<?> getUserByFamilyId(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        if (user.isPresent()) {
            if (user.get().getAge() <= 18) {
                return ResponseEntity.status(404).body("No kids");
            }
            return ResponseEntity.status(200).body(userService.findChildren(user.get().getFamilyId()).stream().map((e) -> userMapper.entityToDto(e)));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@Nullable @RequestParam("teamId") Long teamId, @Nullable @RequestParam("listO36") Boolean listO36) {
        if (teamId != null) {
            Optional<PaceTeam> team = paceTeamService.findById(teamId);
            if (team.isEmpty()) {
                return ResponseEntity.status(400).body("No team found with id: " + teamId);
            }
            return ResponseEntity.status(200).body(StreamSupport.stream(userService.findAllByPaceTeam(team.get(), seasonService.findBySeasonYear(2024).get()).spliterator(), false).map((Function<User, Object>) userMapper::entityToDto));
        }
        if (listO36 == null || !listO36) {
            return ResponseEntity.status(200).body(StreamSupport.stream(userService.getYouth().spliterator(), false).map((Function<User, Object>) userMapper::entityToDto));
        }
        return ResponseEntity.status(200).body(StreamSupport.stream(userService.findAll().spliterator(), false).map(userMapper::entityToDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> putUser(@Valid @RequestBody UserDto userDto, @PathVariable Long id, @RequestParam("modifyUserId") Long modifyUserId) {
        Optional<User> createUser = userService.findById(modifyUserId);
        if (createUser.isEmpty()) {
            return ResponseEntity.status(400).body("No user with id:" + modifyUserId);
        }
        if (createUser.get().getRole().equals(Role.USER) && !userDto.getId().equals(id)) {
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

    @GetMapping("/importFamilyIds")
    public ResponseEntity<?> impostFamilyIds() {
        boolean succeeded = userFamilyImportService.importUserFamilyIds();
        if (succeeded) {
            return ResponseEntity.status(200).body("User family ids imported");
        } else {
            return ResponseEntity.status(500).body("Failed to import user family ids!");
        }
    }
}
