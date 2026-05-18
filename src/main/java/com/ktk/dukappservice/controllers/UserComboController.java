package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.mapper.UserComboMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/userCombo")
public class UserComboController {

    private final UserService userService;

    private final UserComboMapper userComboMapper;

    public UserComboController(UserService userService, UserComboMapper modelMapper) {
        this.userService = userService;
        this.userComboMapper = modelMapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        Optional<User> userById = userService.findById(id);
        if (userById.isPresent()) {
            return ResponseEntity.status(200).body(userComboMapper.entityToDto(userById.get()));
        }

        return ResponseEntity.status(404).body("User not found");
    }

    @GetMapping
    public ResponseEntity<?> getUsers(@RequestParam(value = "teamId", required = false) Long teamId,
                                      @RequestParam(value = "familyId", required = false) Long familyId,
                                      @RequestParam(value = "churchId", required = false) Long churchId,
                                      @RequestParam(value = "spouseId", required = false) Long spouseId,
                                      @RequestParam(value = "keyword", required = false) String keyword, Pageable pageable) {
        return ResponseEntity.status(200).body(userService.fetchByQuery(familyId, spouseId, teamId, churchId, keyword, pageable).map(userComboMapper::entityToDto));
    }

}
