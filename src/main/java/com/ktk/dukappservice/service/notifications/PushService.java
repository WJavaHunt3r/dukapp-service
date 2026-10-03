package com.ktk.dukappservice.service.notifications;

import com.ktk.dukappservice.data.notifications.DeviceToken;
import com.ktk.dukappservice.data.notifications.DeviceTokenRepository;
import com.ktk.dukappservice.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Sends push notifications through Firebase Cloud Messaging. Without {@code app.firebase.credentialsFile} the app
 * still runs: notifications are only logged. Tokens Firebase reports as invalid are deleted.
 */
@Service
public class PushService {
    private static final Logger LOG = LoggerFactory.getLogger(PushService.class);

    private final DeviceTokenRepository deviceTokenRepository;
    private final FcmClient fcmClient;

    @Autowired
    public PushService(DeviceTokenRepository deviceTokenRepository,
                       @Value("${app.firebase.credentialsFile:}") String credentialsFile) {
        this(deviceTokenRepository, createClient(credentialsFile));
    }

    PushService(DeviceTokenRepository deviceTokenRepository, FcmClient fcmClient) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.fcmClient = fcmClient;
    }

    public record Result(int users, int devices, int delivered, int failed) {
        static final Result EMPTY = new Result(0, 0, 0, 0);
    }

    /**
     * Sends to the given devices (already filtered by the users' preferences). {@code data} is delivered to the app
     * alongside the notification; "type" is always set.
     */
    public Result send(Collection<DeviceToken> devices, NotificationType type, String title, String body, Map<String, String> data) {
        if (devices.isEmpty()) {
            return Result.EMPTY;
        }
        int users = (int) devices.stream().map(d -> d.getUser().getId()).distinct().count();
        Map<String, String> payload = new HashMap<>(data);
        payload.put("type", type.name());

        if (fcmClient == null) {
            LOG.info("Push disabled (no app.firebase.credentialsFile), not sending {} to {} devices of {} users: {} - {}",
                    type, devices.size(), users, title, body);
            return new Result(users, devices.size(), 0, 0);
        }

        List<String> tokens = devices.stream().map(DeviceToken::getToken).distinct().toList();
        int delivered = 0;
        int failed = 0;
        List<String> invalid = new ArrayList<>();
        for (int start = 0; start < tokens.size(); start += FcmClient.MAX_TOKENS) {
            List<String> chunk = tokens.subList(start, Math.min(start + FcmClient.MAX_TOKENS, tokens.size()));
            try {
                List<FcmClient.Outcome> outcomes = fcmClient.send(chunk, title, body, payload);
                for (int i = 0; i < outcomes.size(); i++) {
                    if (outcomes.get(i).success()) {
                        delivered++;
                    } else {
                        failed++;
                        if (outcomes.get(i).tokenInvalid()) {
                            invalid.add(chunk.get(i));
                        }
                    }
                }
            } catch (Exception e) {
                failed += chunk.size();
                LOG.error("Failed to send {} push notification to {} devices", type, chunk.size(), e);
            }
        }
        if (!invalid.isEmpty()) {
            deviceTokenRepository.deleteByTokenIn(invalid);
            LOG.info("Deleted {} invalid device tokens", invalid.size());
        }
        LOG.info("Sent {} push to {} users: {} delivered, {} failed", type, users, delivered, failed);
        return new Result(users, tokens.size(), delivered, failed);
    }

    private static FcmClient createClient(String credentialsFile) {
        if (credentialsFile == null || credentialsFile.isBlank()) {
            LOG.warn("app.firebase.credentialsFile is not set: push notifications are disabled (only logged)");
            return null;
        }
        try {
            return new FirebaseFcmClient(credentialsFile);
        } catch (Exception e) {
            LOG.error("Could not initialize Firebase from {}: push notifications are disabled", credentialsFile, e);
            return null;
        }
    }
}
