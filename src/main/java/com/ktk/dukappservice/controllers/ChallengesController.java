package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.challenges.Challenges;
import com.ktk.dukappservice.data.challenges.ChallengesService;
import com.ktk.dukappservice.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/challenges")
public class ChallengesController {

    private final ChallengesService challengesService;
    private final FileStorageService fileStorageService;

    public ChallengesController(ChallengesService challengesService, FileStorageService fileStorageService) {
        this.challengesService = challengesService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping()
    public ResponseEntity getChallengess(@Nullable @RequestParam("date") String date) {
        List<Challenges> goals = challengesService.fetchByQuery(date);
        return ResponseEntity.status(200).body(goals);
    }

    @GetMapping("/{id}")
    public ResponseEntity getChallenges(@PathVariable Long id) {
        var challenges = challengesService.findById(id);
        if (challenges.isEmpty()) {
            return ResponseEntity.status(404).body("No challenges with id: " + id);
        }
        return ResponseEntity.status(200).body(challenges.get());
    }

    @PostMapping
    public ResponseEntity<String> createChallenge(@RequestBody Challenges challenge,
                                                  @RequestParam("image") MultipartFile imageFile) {
        String imageName = fileStorageService.store(imageFile); // Save file and get its name

        challenge.setImagePath(imageName); // Save just the name in the DB
        challengesService.save(challenge);

        return ResponseEntity.ok("Challenge created with image: " + imageName);
    }

    @PutMapping("/{challengesId}")
    public ResponseEntity putChallenges(@Valid @RequestBody Challenges challenges, @PathVariable Long challengesId) {
        if (challengesService.findById(challengesId).isEmpty() || !challenges.getId().equals(challengesId)) {
            return ResponseEntity.status(400).body("Invalid challengesId");
        }

        return ResponseEntity.status(200).body(challengesService.save(challenges));
    }

    @DeleteMapping("/{challengesId}")
    public ResponseEntity deleteChallenges(@PathVariable Long challengesId) {
        if (challengesService.findById(challengesId).isPresent()) {
            return ResponseEntity.status(400).body("Invalid challengesId");
        }
        challengesService.deleteById(challengesId);
        return ResponseEntity.status(200).body("Delete Successful");
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        Resource file = fileStorageService.loadAsResource(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}
