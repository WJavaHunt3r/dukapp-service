package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.camps.Camp;
import com.ktk.dukappservice.data.camps.CampService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/api/camp")
public class CampController {

    private final CampService campService;
    private final UserService userService;

    public CampController(CampService campService, UserService userService) {
        this.campService = campService;
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity getCamps(@RequestParam("seasonYear") Integer seasonYear, Pageable pageable) {
        return ResponseEntity.status(200).body(campService.fetchByQuery(seasonYear, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity getCamp(@PathVariable Long id) {
        var camp = campService.findById(id);
        if (camp.isEmpty()) {
            return ResponseEntity.status(404).body("No camp with id: " + id);
        }
        return ResponseEntity.status(200).body(camp.get());
    }

    @PostMapping()
    public ResponseEntity postCamp(@Valid @RequestBody Camp camp, @RequestParam Long userId) {
        Optional<User> user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user found with id: " + userId);
        }
        if (!user.get().isAdmin()) {
            return ResponseEntity.status(404).body("Unauthorized request");
        }
        return ResponseEntity.status(200).body(campService.save(camp));
    }

    @PutMapping("/{campId}")
    public ResponseEntity putCamp(@Valid @RequestBody Camp camp, @PathVariable Long campId) {
        if (campService.findById(campId).isEmpty() || !camp.getId().equals(campId)) {
            return ResponseEntity.status(400).body("Invalid campId");
        }
        return ResponseEntity.status(200).body(campService.save(camp));
    }

    @DeleteMapping("/{campId}")
    public ResponseEntity deleteCamp(@PathVariable Long campId) {
        if (campService.findById(campId).isPresent()) {
            return ResponseEntity.status(400).body("Invalid campId");
        }
        campService.deleteById(campId);
        return ResponseEntity.status(200).body("Delete Successful");
    }
}
