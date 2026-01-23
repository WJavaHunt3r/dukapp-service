package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.frakareweek.FraKareWeek;
import com.ktk.dukappservice.data.frakareweek.FraKareWeekService;
import com.ktk.dukappservice.data.userfrakarestreak.UserFraKareWeekService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Optional;

@RestController
@RequestMapping("/api/fraKareWeek")
public class FraKareWeekController {

    private UserFraKareWeekService userFraKareWeekService;
    private FraKareWeekService fraKareWeekService;

    public FraKareWeekController(UserFraKareWeekService userFraKareWeekService, FraKareWeekService fraKareWeekService) {
        this.userFraKareWeekService = userFraKareWeekService;
        this.fraKareWeekService = fraKareWeekService;
    }

    @GetMapping()
    public ResponseEntity getFraKareWeeks(@Nullable @RequestParam("year") Integer year,
                                          @Nullable @RequestParam("weekNumber") Integer weekNumber) {
        return ResponseEntity.status(200).body(fraKareWeekService.fetchByQuery(year, weekNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity getUserFraKareWeek(@PathVariable Long id) {
        Optional<FraKareWeek> fraKareStreak = fraKareWeekService.findById(id);
        if (fraKareStreak.isEmpty()) {
            return ResponseEntity.status(404).body("No fraKareWeek with id: " + id);
        }
        return ResponseEntity.status(200).body(fraKareStreak.get());
    }

    @PostMapping("/createCurrentFrakareWeek")
    public ResponseEntity createFraKareWeek() {
        if (userFraKareWeekService.fetchByQuery(null, LocalDate.now().get(WeekFields.ISO.weekOfWeekBasedYear()), null, null).isEmpty()) {
            fraKareWeekService.createFraKareWeek();
            return ResponseEntity.status(200).body("Fra Kare week successfully created!");
        }
        return ResponseEntity.status(404).body("A FraKareWeek already exists for this week!");

    }
}
