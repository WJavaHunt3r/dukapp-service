package com.ktk.dukappservice.controllers;

import com.ktk.dukappservice.data.donation.Donation;
import com.ktk.dukappservice.data.donation.DonationService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.dto.DonationDto;
import com.ktk.dukappservice.mapper.DonationMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@RequestMapping("/api/donations")
public class DonationsController {

    private final DonationService donationService;
    private final UserService userService;

    private final DonationMapper mapper;

    public DonationsController(DonationService donationService, UserService userService, DonationMapper mapper) {
        this.donationService = donationService;
        this.userService = userService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<?> getDonations(@RequestParam("dateTime") String dateTime) {
        return ResponseEntity.status(200).body(donationService.fetchByQuery(dateTime).stream().map(mapper::entityToDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDonation(@PathVariable Long id) {
        var donation = donationService.findById(id);
        if (donation.isEmpty()) {
            return ResponseEntity.status(404).body("No donation with id: " + id);
        }
        return ResponseEntity.status(200).body(mapper.entityToDto(donation.get()));
    }

    @PostMapping()
    @PreAuthorize("hasAuthority('DONATION_MANAGE')")
    public ResponseEntity<?> postDonation(@Valid @RequestBody DonationDto donation) {
        return ResponseEntity.status(200).body(mapper.entityToDto(donationService.save(mapper.dtoToEntity(donation, new Donation()))));
    }

    @PutMapping("/{donationId}")
    @PreAuthorize("hasAuthority('DONATION_MANAGE')")
    public ResponseEntity<?> putDonation(@Valid @RequestBody DonationDto donation, @PathVariable Long donationId) {
        if (donationService.findById(donationId).isEmpty() || !donation.getId().equals(donationId)) {
            return ResponseEntity.status(400).body("Invalid donationId");
        }
        return ResponseEntity.status(200).body(mapper.entityToDto(donationService.save(mapper.dtoToEntity(donation, new Donation()))));
    }

    @DeleteMapping("/{donationId}")
    @PreAuthorize("hasAuthority('DONATION_MANAGE')")
    public ResponseEntity<?> deleteDonation(@PathVariable Long donationId) {
        if (donationService.findById(donationId).isEmpty()) {
            return ResponseEntity.status(400).body("Invalid donationId");
        }
        donationService.deleteById(donationId);
        return ResponseEntity.status(200).body("Delete Successful");
    }
}
