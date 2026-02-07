package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.camps.Camp;
import com.ktk.dukappservice.data.camps.CampService;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.seasons.SeasonService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/camp")
public class CampController {

    private CampService campService;
    private UserService userService;
    private SeasonService seasonService;

    public CampController(CampService campService, UserService userService, SeasonService seasonService) {
        this.campService = campService;
        this.userService = userService;
        this.seasonService = seasonService;
    }

    @GetMapping()
    public ResponseEntity getCamps(@Nullable @RequestParam("seasonId") Long seasonId) {
        if (seasonId == null) {
            return ResponseEntity.status(200).body(campService.findAll());
        }
        Optional<Season> season = seasonService.findById(seasonId);
        if (season.isEmpty()) {
            return ResponseEntity.status(404).body("No season with given id: " + seasonId);
        }
        List<Camp> goals = campService.findAllBySeason(season.get());
        return ResponseEntity.status(200).body(goals);
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
