package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.mentormentee.MentorMentee;
import com.ktk.dukappservice.dto.MentorMenteeDto;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.mapper.MentorMenteeMapper;
import com.ktk.dukappservice.data.mentormentee.MentorMenteeService;
import com.ktk.dukappservice.data.users.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/mentorMentee")
public class MentorMenteeController {

    private MentorMenteeService mentorMenteeService;
    private MentorMenteeMapper mapper;
    private UserService userService;

    public MentorMenteeController(MentorMenteeService mentorMenteeService, MentorMenteeMapper mapper, UserService userService) {
        this.mentorMenteeService = mentorMenteeService;
        this.mapper = mapper;
        this.userService = userService;
    }

    @GetMapping()
    public ResponseEntity getMentees(@Nullable @RequestParam("userId") Long userId) {
        if (userId != null) {
            var user = userService.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(404).body("No user with given id: " + userId);
            }
            return ResponseEntity.ok(mentorMenteeService.findByMentor(user.get()).stream().map(mapper::entityToDto));
        }
        return ResponseEntity.ok(StreamSupport.stream(mentorMenteeService.findAll().spliterator(), false).map(mapper::entityToDto));
    }

    @PostMapping()
    public ResponseEntity postMentorMentee(@Valid @RequestBody MentorMenteeDto mentorMentee, @RequestParam("userId") Long userId) {
        var user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user with given id: " + userId);
        }
        if (user.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Permission denied!");
        }

        var mentor = userService.findById(mentorMentee.getMentor().getId());
        if (mentor.isEmpty()) {
            return ResponseEntity.status(404).body("No user with given id: " + mentorMentee.getMentor().getId());
        }

        var mentee = userService.findById(mentorMentee.getMentee().getId());
        if (mentee.isEmpty()) {
            return ResponseEntity.status(404).body("No user with given id: " + mentorMentee.getMentee().getId());
        }

        MentorMentee entity = new MentorMentee();
        entity.setMentee(mentee.get());
        entity.setMentor(mentor.get());

        return ResponseEntity.ok(mapper.entityToDto(mentorMenteeService.save(entity)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity postMentorMentee(@PathVariable Long id, @RequestParam("userId") Long userId) {
        var user = userService.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(404).body("No user with given id: " + userId);
        }
        if (user.get().getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body("Permission denied!");
        }

        if (!mentorMenteeService.existsById(id)) {
            return ResponseEntity.status(404).body("No Mentor Mentee with given id: " + id);
        }

        mentorMenteeService.deleteById(id);

        return ResponseEntity.ok("Successfully deleted");
    }

}
