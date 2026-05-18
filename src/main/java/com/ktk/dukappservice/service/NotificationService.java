package com.ktk.dukappservice.service;

import com.ktk.dukappservice.data.paceuserround.PaceUserRoundRepository;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class NotificationService {

    private final RoundService roundService;
    private final UserStatusService userStatusService;
    private final PaceUserRoundRepository paceUserRoundRepository;
    private final MicrosoftService microsoftService;

    public NotificationService(RoundService roundService,
                               UserStatusService userStatusService,
                               PaceUserRoundRepository paceUserRoundRepository,
                               MicrosoftService microsoftService) {
        this.roundService = roundService;
        this.userStatusService = userStatusService;
        this.paceUserRoundRepository = paceUserRoundRepository;
        this.microsoftService = microsoftService;
    }

    @Scheduled(cron = "0 0 17 * * TUE")
    public void sendOnTrackEmails() {
        Round currentRound = roundService.getLastRound();
        int currentYear = LocalDate.now().getYear();

        log.info("Starting scheduled on-track email notification for year: {}", currentYear);

        Page<UserStatus> statuses = userStatusService.fetchByQuery(currentYear, null);

        List<CompletableFuture<Void>> emailTasks = statuses.stream()
                .map(status -> CompletableFuture.runAsync(() -> processEmailForUser(status, currentRound)))
                .toList();

        CompletableFuture.allOf(emailTasks.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Finished sending all scheduled emails."));
    }

    private void processEmailForUser(UserStatus u, Round currentRound) {
        paceUserRoundRepository.findByUserAndRound(u.getUser(), currentRound)
                .ifPresent(ur -> {
                    if (!ur.isOnTrack() && u.getUser().getEmail() != null && !u.getUser().getEmail().isEmpty()) {
                        try {
                            microsoftService.sendStatusUpdate(
                                    u.getTransactions(),
                                    u.getStatus() * 100,
                                    ur.getRoundMyShareGoal(),
                                    u.getUser(),
                                    currentRound
                            );
                        } catch (Exception e) {
                            log.error("Failed to send email to user {}: {}", u.getUser().getUsername(), e.getMessage());
                        }
                    }
                });
    }
}