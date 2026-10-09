package com.ktk.dukappservice.service.notifications;

import com.google.common.util.concurrent.MoreExecutors;
import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.notifications.DeviceToken;
import com.ktk.dukappservice.data.notifications.DeviceTokenRepository;
import com.ktk.dukappservice.data.notifications.GeneralNotification;
import com.ktk.dukappservice.data.notifications.GeneralNotificationRepository;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.*;

import static com.ktk.dukappservice.service.notifications.PushServiceTest.device;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PushNotificationServiceTest {

    private final PushService pushService = mock(PushService.class);
    private final DeviceTokenRepository devices = mock(DeviceTokenRepository.class);
    private final GeneralNotificationRepository history = mock(GeneralNotificationRepository.class);
    private final JobService jobService = mock(JobService.class);
    private final com.ktk.dukappservice.data.jobchat.JobChatService jobChatService = mock(com.ktk.dukappservice.data.jobchat.JobChatService.class);
    private PushNotificationService service;
    private Job job;

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(pushService, devices, history, jobService, jobChatService, MoreExecutors.newDirectExecutorService());
        ReflectionTestUtils.setField(service, "baseChurchId", 1L);
        job = new Job();
        job.setId(5L);
        job.setDescription("Takarítás");
        job.setJobDateTime(LocalDateTime.of(2026, 10, 10, 9, 0));
        when(jobService.findById(5L)).thenReturn(Optional.of(job));
        when(jobService.markCloseReminderSent(anyLong(), anyBoolean())).thenReturn(true);
        when(pushService.send(anyCollection(), any(), any(), any(), anyMap())).thenReturn(new PushService.Result(1, 1, 1, 0));
        when(history.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void newJobGoesToEligibleBaseChurchUsersExceptCreator() {
        DeviceToken creator = device(1, "creator");
        DeviceToken eligible = device(2, "eligible");
        DeviceToken tooYoung = device(3, "young");
        when(devices.findForChurch(1L, NotificationType.JOB_NEW)).thenReturn(List.of(creator, eligible, tooYoung));
        when(jobService.isEligible(eq(job), any())).thenAnswer(i -> ((User) i.getArgument(1)).getId() != 3L);

        service.jobCreated(5L, 1L);

        assertThat(sentDevices(NotificationType.JOB_NEW)).containsExactly(eligible);
    }

    @Test
    void cancelledJobGoesToRegisteredAndWaitlistedExceptCanceller() {
        when(jobService.findActiveRegistrations(5L)).thenReturn(List.of(registration(1), registration(2), registration(3)));
        when(devices.findForUsers(anyCollection(), eq(NotificationType.JOB_CANCELLED))).thenReturn(List.of(device(2, "a")));

        service.jobCancelled(5L, 1L);

        verify(devices).findForUsers(Set.of(2L, 3L), NotificationType.JOB_CANCELLED);
        verify(pushService).send(anyCollection(), eq(NotificationType.JOB_CANCELLED), eq("Munka lemondva"), contains("Takarítás"), eq(Map.of("jobId", "5")));
    }

    @Test
    void registrationByOtherNotifiesTargetButSelfRegistrationDoesNot() {
        User parent = user(1, "Kis", "Anna");
        User child = user(2, "Kis", "Bence");
        when(devices.findForUsers(List.of(2L), NotificationType.JOB_REGISTERED_BY_OTHER)).thenReturn(List.of(device(2, "c")));

        service.registeredByOther(5L, parent, parent, JobRegistrationStatus.REGISTERED);
        verifyNoInteractions(pushService);

        service.registeredByOther(5L, child, parent, JobRegistrationStatus.WAITLISTED);
        verify(pushService).send(anyCollection(), eq(NotificationType.JOB_REGISTERED_BY_OTHER), eq("Jelentkeztettek"),
                eq("Kis Anna várólistára tett: Takarítás (2026.10.10 09:00)"), anyMap());
    }

    @Test
    void transactionsAreGroupedPerUserAndCreatorIsSkipped() {
        when(devices.findForUsers(anyCollection(), eq(NotificationType.TRANSACTION_CREATED)))
                .thenReturn(List.of(device(2, "a"), device(2, "b"), device(3, "c")));

        service.transactionsCreated(Map.of(1L, List.of("own"), 2L, List.of("Bónusz", "Munka"), 3L, List.of("Adomány")), 1L);

        verify(devices).findForUsers(Set.of(2L, 3L), NotificationType.TRANSACTION_CREATED);
        verify(pushService).send(argThat(c -> c.size() == 2), eq(NotificationType.TRANSACTION_CREATED), eq("Új tranzakciók"), eq("2 új tranzakció került rögzítésre."), anyMap());
        verify(pushService).send(argThat(c -> c.size() == 1), eq(NotificationType.TRANSACTION_CREATED), eq("Új tranzakció"), eq("Adomány"), anyMap());
    }

    @Test
    void generalNotificationTargetsAllOrRolesAndIsStored() {
        User admin = user(1, "Admin", "Ádám");

        service.sendGeneral("Hello", "Text", Set.of(), admin);
        verify(devices).findForBroadcast(NotificationType.GENERAL, true, List.of(-1L));

        GeneralNotification stored = service.sendGeneral("Hello", "Text", Set.of(7L), admin);
        verify(devices).findForBroadcast(NotificationType.GENERAL, false, Set.of(7L));
        assertThat(stored.getSentBy()).isEqualTo(admin);
        assertThat(stored.getRoleIds()).containsExactly(7L);
        assertThat(stored.getDelivered()).isEqualTo(1);
        assertThat(stored.getSentDateTime()).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Collection<DeviceToken> sentDevices(NotificationType type) {
        ArgumentCaptor<Collection<DeviceToken>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(pushService).send(captor.capture(), eq(type), any(), any(), anyMap());
        return captor.getValue();
    }

    private static JobRegistration registration(long userId) {
        JobRegistration registration = new JobRegistration();
        registration.setUser(user(userId, "L", "F"));
        return registration;
    }

    private static User user(long id, String lastname, String firstname) {
        User user = new User();
        user.setId(id);
        user.setLastname(lastname);
        user.setFirstname(firstname);
        return user;
    }

    // ---------------------------------------------------------------- unclosed jobs

    private Job overdueJob(LocalDateTime end) {
        Job j = new Job();
        j.setId(7L);
        j.setDescription("Kerti munka");
        j.setJobDateTime(end.minusHours(2));
        j.setJobEndDateTime(end);
        com.ktk.dukappservice.data.users.User responsible = new com.ktk.dukappservice.data.users.User();
        responsible.setId(2L);
        j.setResponsible(responsible);
        return j;
    }

    @Test
    void theFirstReminderGoesOutAnHourAfterTheEndAndOnlyOnce() {
        Job j = overdueJob(LocalDateTime.now().minusMinutes(90));
        when(jobService.findOverdueOpenJobs(any())).thenReturn(List.of(j));
        when(devices.findForUsers(eq(List.of(2L)), eq(NotificationType.JOB_NOT_CLOSED))).thenReturn(List.of());

        service.sendCloseReminders();

        verify(jobService).markCloseReminderSent(7L, false);
        verify(pushService).send(anyCollection(), eq(NotificationType.JOB_NOT_CLOSED), any(), any(), anyMap());

        // already sent, and the next day hasn't come: nothing more
        j.setCloseReminderSentDateTime(LocalDateTime.now());
        clearInvocations(pushService, jobService);
        when(jobService.findOverdueOpenJobs(any())).thenReturn(List.of(j));
        service.sendCloseReminders();
        verify(pushService, never()).send(anyCollection(), any(), any(), any(), anyMap());
    }

    @Test
    void theSecondReminderGoesOutOnTheNextDayAndForgottenJobsAreIgnored() {
        Job j = overdueJob(LocalDateTime.now().minusDays(2));
        j.setCloseReminderSentDateTime(LocalDateTime.now().minusDays(2));
        when(jobService.findOverdueOpenJobs(any())).thenReturn(List.of(j));
        when(devices.findForUsers(anyCollection(), any())).thenReturn(List.of());

        service.sendCloseReminders();
        verify(jobService).markCloseReminderSent(7L, true);

        clearInvocations(jobService);
        Job old = overdueJob(LocalDateTime.now().minusDays(30));
        when(jobService.findOverdueOpenJobs(any())).thenReturn(List.of(old));
        service.sendCloseReminders();
        verify(jobService, never()).markCloseReminderSent(anyLong(), anyBoolean());
    }

    @Test
    void theTestNotificationGoesToEveryDeviceOfTheUser() {
        com.ktk.dukappservice.data.users.User user = new com.ktk.dukappservice.data.users.User();
        user.setId(9L);
        List<DeviceToken> mine = List.of(device(9L, "a"), device(9L, "b"));
        when(devices.findByUserId(9L)).thenReturn(mine);

        service.sendTest(user);

        verify(pushService).send(eq(mine), eq(NotificationType.GENERAL), eq("Teszt értesítés"), any(), anyMap());
    }

    @Test
    void noReminderIsSentWhenTheJobWasClosedMeanwhile() {
        Job j = overdueJob(LocalDateTime.now().minusMinutes(90));
        when(jobService.findOverdueOpenJobs(any())).thenReturn(List.of(j));
        // closed after the list was loaded: marking says no
        when(jobService.markCloseReminderSent(7L, false)).thenReturn(false);

        service.sendCloseReminders();

        verify(pushService, never()).send(anyCollection(), any(), any(), any(), anyMap());
    }
}
