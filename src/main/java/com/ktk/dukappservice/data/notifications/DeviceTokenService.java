package com.ktk.dukappservice.data.notifications;

import com.ktk.dukappservice.data.users.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class DeviceTokenService {

    private final DeviceTokenRepository repository;

    public DeviceTokenService(DeviceTokenRepository repository) {
        this.repository = repository;
    }

    /**
     * Registers the token for {@code user}. A token belongs to one device, so if another user registered it before
     * (shared device, logged out without unregistering), it moves to the current user.
     */
    @Transactional
    public void register(User user, String token, String platform) {
        LocalDateTime now = LocalDateTime.now();
        DeviceToken device = repository.findByToken(token).orElseGet(() -> {
            DeviceToken created = new DeviceToken();
            created.setToken(token);
            created.setCreateDateTime(now);
            return created;
        });
        device.setUser(user);
        device.setPlatform(platform);
        device.setLastSeenDateTime(now);
        repository.save(device);
    }

    /** Removes the token if it belongs to {@code user} (call on logout). Returns whether something was removed. */
    @Transactional
    public boolean unregister(User user, String token) {
        Optional<DeviceToken> device = repository.findByToken(token);
        if (device.isPresent() && device.get().getUser().getId().equals(user.getId())) {
            repository.delete(device.get());
            return true;
        }
        return false;
    }
}
