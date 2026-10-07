package com.ktk.dukappservice.data.jobchat;

import com.ktk.dukappservice.data.jobregistrations.JobRegistration;
import com.ktk.dukappservice.data.jobs.Job;
import com.ktk.dukappservice.data.jobs.JobService;
import com.ktk.dukappservice.data.roles.AppRole;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.JobRegistrationStatus;
import com.ktk.dukappservice.enums.JobStatus;
import com.ktk.dukappservice.enums.Permission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobChatServiceTest {

    private final JobChatMessageRepository messages = mock(JobChatMessageRepository.class);
    private final JobChatMuteRepository mutes = mock(JobChatMuteRepository.class);
    private final JobChatMemberRepository memberRepository = mock(JobChatMemberRepository.class);
    private final JobService jobService = mock(JobService.class);
    private JobChatService service;
    private Job job;
    private User creator, responsible, registered, waitlisted, stranger, admin;
    private final List<JobChatMute> muteStore = new ArrayList<>();
    private final List<JobChatMember> memberStore = new ArrayList<>();

    private static User user(long id, Permission... permissions) {
        User u = new User();
        u.setId(id);
        u.setFirstname("User");
        u.setLastname("" + id);
        AppRole role = AppRole.of("R" + id, "", permissions.length == 0 ? EnumSet.noneOf(Permission.class) : EnumSet.copyOf(Arrays.asList(permissions)));
        u.setRoles(new HashSet<>(Set.of(role)));
        return u;
    }

    private JobRegistration registration(User user, JobRegistrationStatus status) {
        JobRegistration r = new JobRegistration();
        r.setJob(job);
        r.setUser(user);
        r.setStatus(status);
        return r;
    }

    @BeforeEach
    void setUp() {
        service = new JobChatService(messages, mutes, memberRepository, jobService);
        creator = user(1);
        responsible = user(2);
        registered = user(3);
        waitlisted = user(4);
        stranger = user(5);
        admin = user(6, Permission.JOB_MANAGE_ALL);
        job = new Job();
        job.setId(1L);
        job.setCreateUser(creator);
        job.setResponsible(responsible);
        job.setStatus(JobStatus.OPEN);
        when(jobService.findActiveRegistrations(1L)).thenReturn(List.of(
                registration(registered, JobRegistrationStatus.REGISTERED), registration(waitlisted, JobRegistrationStatus.WAITLISTED)));
        when(memberRepository.save(any())).thenAnswer(i -> {
            JobChatMember m = i.getArgument(0);
            memberStore.add(m);
            return m;
        });
        when(memberRepository.findByJobId(anyLong())).thenAnswer(i -> new ArrayList<>(memberStore));
        when(memberRepository.findByJobIdAndUserId(anyLong(), anyLong())).thenAnswer(i -> memberStore.stream()
                .filter(m -> m.getUser().getId().equals(i.getArgument(1))).findFirst());
        doAnswer(i -> memberStore.remove((JobChatMember) i.getArgument(0))).when(memberRepository).delete(any(JobChatMember.class));
        when(messages.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mutes.save(any())).thenAnswer(i -> {
            muteStore.add(i.getArgument(0));
            return i.getArgument(0);
        });
        doAnswer(i -> muteStore.remove((JobChatMute) i.getArgument(0))).when(mutes).delete(any(JobChatMute.class));
        when(mutes.findByJobIdAndUserId(anyLong(), anyLong())).thenAnswer(i -> muteStore.stream()
                .filter(m -> m.getUser().getId().equals(i.getArgument(1))).findFirst());
        when(mutes.findByJobIdAndUserIdIn(anyLong(), anyCollection())).thenAnswer(i -> muteStore.stream()
                .filter(m -> ((Collection<?>) i.getArgument(1)).contains(m.getUser().getId())).toList());
    }

    @Test
    void registeredUsersCreatorAndResponsibleTakePartButNotTheWaitlistOrStrangers() {
        assertThat(service.participantIds(job)).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(service.canAccess(registered, job)).isTrue();
        assertThat(service.canAccess(waitlisted, job)).isFalse();
        assertThat(service.canAccess(stranger, job)).isFalse();
        assertThat(service.canAccess(admin, job)).isTrue();
    }

    @Test
    void notificationsGoToEveryoneExceptTheSenderAndWhoMuted() {
        service.setMuted(job, responsible, true);

        assertThat(service.recipientIds(job, 3L)).containsExactly(1L);
        assertThat(service.isMuted(1L, 2L)).isTrue();

        service.setMuted(job, responsible, false);
        assertThat(service.recipientIds(job, 3L)).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void messagesAreTrimmedAndStrangersAndEmptyTextsAreRejected() {
        assertThat(service.post(job, registered, "  hello  ").getText()).isEqualTo("hello");
        assertThatThrownBy(() -> service.post(job, stranger, "hi")).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThatThrownBy(() -> service.post(job, registered, "   ")).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void theChatIsArchivedOnceTheJobIsCompletedOrCancelled() {
        job.setStatus(JobStatus.COMPLETED);
        assertThat(JobChatService.isArchived(job)).isTrue();
        assertThatThrownBy(() -> service.post(job, registered, "late")).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        // reading stays possible
        assertThat(service.canAccess(registered, job)).isTrue();

        job.setStatus(JobStatus.CANCELLED);
        assertThat(JobChatService.isArchived(job)).isTrue();
    }

    @Test
    void organisersCanAddAndRemovePeopleWhoAreNotRegistered() {
        service.addMember(job, stranger, responsible);

        assertThat(service.canAccess(stranger, job)).isTrue();
        assertThat(service.recipientIds(job, 3L)).contains(5L);
        assertThat(service.participants(job)).filteredOn(p -> p.user().getId() == 5L)
                .singleElement().satisfies(p -> {
                    assertThat(p.role()).isEqualTo("MEMBER");
                    assertThat(p.removable()).isTrue();
                });

        service.removeMember(job, 5L, responsible);
        assertThat(service.canAccess(stranger, job)).isFalse();
    }

    @Test
    void onlyOrganisersManageMembersAndRegisteredPeopleCannotBeRemoved() {
        assertThatThrownBy(() -> service.addMember(job, stranger, registered)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
        assertThat(service.canManageMembers(admin, job)).isTrue();
        assertThat(service.canManageMembers(creator, job)).isTrue();
        assertThat(service.canManageMembers(registered, job)).isFalse();

        assertThatThrownBy(() -> service.removeMember(job, 3L, responsible)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        // somebody already in the chat can't be added again
        assertThatThrownBy(() -> service.addMember(job, registered, responsible)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void nobodyIsAddedToAnArchivedChat() {
        job.setStatus(JobStatus.COMPLETED);
        assertThatThrownBy(() -> service.addMember(job, stranger, responsible)).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }
}
