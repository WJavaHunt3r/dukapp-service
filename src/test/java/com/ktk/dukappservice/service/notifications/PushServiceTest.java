package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.notifications.DeviceToken;
import com.ktk.dukappservice.data.notifications.DeviceTokenRepository;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.NotificationType;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PushServiceTest {

    private final DeviceTokenRepository repository = mock(DeviceTokenRepository.class);

    @Test
    void sendsInChunksOf500AndCountsOutcomes() throws Exception {
        List<List<String>> chunks = new ArrayList<>();
        FcmClient client = (tokens, title, body, data) -> {
            chunks.add(List.copyOf(tokens));
            return tokens.stream().map(t -> new FcmClient.Outcome(true, false)).toList();
        };
        List<DeviceToken> devices = IntStream.range(0, 1201).mapToObj(i -> device(i % 600, "t" + i)).toList();

        PushService.Result result = new PushService(repository, client).send(devices, NotificationType.GENERAL, "T", "B", Map.of());

        assertThat(chunks).extracting(List::size).containsExactly(500, 500, 201);
        assertThat(result).isEqualTo(new PushService.Result(600, 1201, 1201, 0));
        verify(repository, never()).deleteByTokenIn(any());
    }

    @Test
    void deletesInvalidTokensAndSetsTypeInData() throws Exception {
        Map<String, String> sentData = new HashMap<>();
        FcmClient client = (tokens, title, body, data) -> {
            sentData.putAll(data);
            return List.of(new FcmClient.Outcome(true, false), new FcmClient.Outcome(false, true), new FcmClient.Outcome(false, false));
        };

        PushService.Result result = new PushService(repository, client)
                .send(List.of(device(1, "ok"), device(1, "gone"), device(2, "flaky")), NotificationType.JOB_NEW, "T", "B", Map.of("jobId", "5"));

        assertThat(result).isEqualTo(new PushService.Result(2, 3, 1, 2));
        assertThat(sentData).containsEntry("type", "JOB_NEW").containsEntry("jobId", "5");
        verify(repository).deleteByTokenIn(List.of("gone"));
    }

    @Test
    void failedChunkCountsAsFailedAndDoesNotThrow() {
        FcmClient client = (tokens, title, body, data) -> {
            throw new RuntimeException("network");
        };

        PushService.Result result = new PushService(repository, client).send(List.of(device(1, "a")), NotificationType.WEEKLY, "T", "B", Map.of());

        assertThat(result).isEqualTo(new PushService.Result(1, 1, 0, 1));
    }

    @Test
    void withoutFirebaseOnlyLogs() {
        PushService.Result result = new PushService(repository, (FcmClient) null)
                .send(List.of(device(1, "a"), device(1, "b")), NotificationType.GENERAL, "T", "B", Map.of());

        assertThat(result).isEqualTo(new PushService.Result(1, 2, 0, 0));
        verifyNoInteractions(repository);
    }

    @Test
    void nothingToSend() {
        assertThat(new PushService(repository, (FcmClient) null).send(List.of(), NotificationType.GENERAL, "T", "B", Map.of()))
                .isEqualTo(new PushService.Result(0, 0, 0, 0));
    }

    static DeviceToken device(long userId, String token) {
        User user = new User();
        user.setId(userId);
        DeviceToken device = new DeviceToken();
        device.setUser(user);
        device.setToken(token);
        return device;
    }
}
