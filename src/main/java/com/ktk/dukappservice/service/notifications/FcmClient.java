package com.ktk.dukappservice.service.notifications;

import java.util.List;
import java.util.Map;

/** Sends one notification to up to {@link #MAX_TOKENS} device tokens. Abstracted from Firebase for testing. */
public interface FcmClient {
    int MAX_TOKENS = 500;

    /** One outcome per token, in the same order. */
    List<Outcome> send(List<String> tokens, String title, String body, Map<String, String> data) throws Exception;

    /** {@code tokenInvalid}: the token is expired/unregistered and should be deleted. */
    record Outcome(boolean success, boolean tokenInvalid) {
    }
}
