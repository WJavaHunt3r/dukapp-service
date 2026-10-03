package com.ktk.dukappservice.service.notifications;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class FirebaseFcmClient implements FcmClient {
    private static final String APP_NAME = "dukapp";

    private final FirebaseMessaging messaging;

    public FirebaseFcmClient(String credentialsFile) throws IOException {
        try (InputStream credentials = new FileInputStream(credentialsFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(a -> a.getName().equals(APP_NAME))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
            this.messaging = FirebaseMessaging.getInstance(app);
        }
    }

    @Override
    public List<Outcome> send(List<String> tokens, String title, String body, Map<String, String> data) throws FirebaseMessagingException {
        MulticastMessage message = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data)
                .build();
        BatchResponse response = messaging.sendEachForMulticast(message);
        return response.getResponses().stream().map(r -> {
            if (r.isSuccessful()) {
                return new Outcome(true, false);
            }
            MessagingErrorCode code = r.getException() == null ? null : r.getException().getMessagingErrorCode();
            return new Outcome(false, code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT
                    || code == MessagingErrorCode.SENDER_ID_MISMATCH);
        }).toList();
    }
}
