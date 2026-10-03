package com.ktk.dukappservice.service;

import com.ktk.dukappservice.data.notifications.NotificationPreferenceService;
import com.ktk.dukappservice.data.paceuserround.PaceUserRoundRepository;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.rounds.RoundService;
import com.ktk.dukappservice.data.userstatus.StatusTotals;
import com.ktk.dukappservice.data.userstatus.UserStatus;
import com.ktk.dukappservice.data.userstatus.UserStatusService;
import com.ktk.dukappservice.enums.NotificationType;
import com.ktk.dukappservice.service.microsoft.MicrosoftService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class NotificationService {

    private final RoundService roundService;
    private final UserStatusService userStatusService;
    private final PaceUserRoundRepository paceUserRoundRepository;
    private final MicrosoftService microsoftService;
    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationService(RoundService roundService,
                               UserStatusService userStatusService,
                               PaceUserRoundRepository paceUserRoundRepository,
                               MicrosoftService microsoftService,
                               NotificationPreferenceService notificationPreferenceService) {
        this.notificationPreferenceService = notificationPreferenceService;
        this.roundService = roundService;
        this.userStatusService = userStatusService;
        this.paceUserRoundRepository = paceUserRoundRepository;
        this.microsoftService = microsoftService;
    }

    /**
     * Run by {@code NotificationScheduler} at the time set in the ON_TRACK_EMAIL notification schedule.
     * Users who switched the e-mail off in their notification preferences are skipped.
     */
    public void sendOnTrackEmails() {
        Round currentRound = roundService.getCurrentRound();
        int currentYear = LocalDate.now().getYear();

        log.info("Starting scheduled on-track email notification for year: {}", currentYear);

        Page<UserStatus> statuses = userStatusService.fetchByQuery(currentYear, null);
        Set<Long> optedOut = notificationPreferenceService.findDisabledUserIds(NotificationType.ON_TRACK_EMAIL);

        List<CompletableFuture<Void>> emailTasks = statuses.stream()
                .filter(status -> !optedOut.contains(status.getUser().getId()))
                .map(status -> CompletableFuture.runAsync(() -> processEmailForUser(status, currentRound)))
                .toList();

        CompletableFuture.allOf(emailTasks.toArray(new CompletableFuture[0]))
                .thenRun(() -> log.info("Finished sending all scheduled emails."));
    }

    private void processEmailForUser(UserStatus u, Round currentRound) {
        paceUserRoundRepository.findByUserAndRound(u.getUser(), currentRound)
                .ifPresent(ur -> {
                    if (!ur.isLocalOnTrack() && u.getUser().getEmail() != null && !u.getUser().getEmail().isEmpty()) {
                        StatusTotals totals = userStatusService.getStatusTotals(u);
                        try {
                            microsoftService.sendStatusUpdate(
                                    totals.transactions(),
                                    totals.status() * 100,
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