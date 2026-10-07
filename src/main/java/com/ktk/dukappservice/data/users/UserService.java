package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.church.ChurchService;
import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UserService extends BaseService<User, Long> {

    private final UserRepository userRepository;
    private final TransactionItemService transactionItemService;
    private final ChurchService churchService;

    @Value("${app.users.baseChurch}")
    private Long baseChurch;

    /** MyShare ids given to people who register in the app count up from here (6 digits). */
    static final long FIRST_APP_MYSHARE_ID = 100001L;
    static final long LAST_APP_MYSHARE_ID = 999999L;

    /**
     * Saves a user who just registered and gives them the next free 6-digit MyShare id (100001, 100002, ...) unless
     * they already have one. Synchronized so two registrations at the same moment can't get the same number.
     */
    public synchronized User saveNewUser(User user) {
        if (user.getMyShareID() == null) {
            user.setMyShareID(nextMyShareId());
        }
        return save(user);
    }

    private long nextMyShareId() {
        Long highest = userRepository.findMaxMyShareIdBetween(FIRST_APP_MYSHARE_ID, LAST_APP_MYSHARE_ID);
        long next = highest == null ? FIRST_APP_MYSHARE_ID : highest + 1;
        // Skip numbers somebody already holds (e.g. entered by hand above the highest one)
        while (next <= LAST_APP_MYSHARE_ID && userRepository.findByMyShareID(next).isPresent()) {
            next++;
        }
        if (next > LAST_APP_MYSHARE_ID) {
            throw new IllegalStateException("No free 6-digit MyShare id left");
        }
        return next;
    }

    public UserService(UserRepository userRepository, TransactionItemService transactionService, ChurchService churchService) {
        this.userRepository = userRepository;
        this.transactionItemService = transactionService;
        this.churchService = churchService;
    }

    public Page<User> fetchByQuery(Long familyId, Long spouseId, Long teamId, Long churchId, String keyword, Pageable pageable) {
        String kw = keyword == null || keyword.isBlank() ? "" : keyword.trim();
        return userRepository.fetchByQuery(familyId, spouseId, teamId, churchId, kw, pageable);
    }

    public Optional<User> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        user.ifPresent(this::calculateUserPoints);
        return user;
    }

    /**
     * The user behind the authenticated request. Unlike {@link #findByUsername(String)} this doesn't recalculate points.
     */
    public User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No user with username: " + userDetails.getUsername()));
    }

    public Optional<User> findByEmailOrUsername(String email, String username) {
        return userRepository.findByEmailOrUsername(email, username);
    }

    public Iterable<User> getYouth() {
        return getYouth(LocalDate.now().getYear());
    }

    public Iterable<User> getYouth(int year) {
        return userRepository.findAllBUKBySeason(year);
    }

    public void calculateUserPointsForAllUsers() {
        getYouth().forEach(this::calculateUserPoints);
    }

    public Iterable<User> findAllByRole(Role role) {
        return userRepository.findAllByRole(role);
    }

    public List<User> findFamily(Long familyId, Long userId) {
        return userRepository.findFamily(familyId, userId);
    }

    public Iterable<User> findAllByPaceTeam(PaceTeam t, Season s) {
        return userRepository.findAllByPaceTeamAndSeasonAndGoal(t, s);
    }

    public Optional<User> findByMyShareId(Long id) {
        return userRepository.findByMyShareID(id);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Long countAllByTeam(Team t, Integer season) {
        return userRepository.countAllByTeamAndSeasonAndGoal(t, season);
    }

    public void calculateUserPoints(User u) {
        u.setCurrentMyShareCredit(u.getBaseMyShareCredit());
        transactionItemService.fetchByQuery(null, null, null, null, null, u.getId(), LocalDate.now().getYear(), null).forEach(t -> addTransaction(t, u));
        save(u);
    }

    private void addTransaction(TransactionItem t, User u) {
        if (t.getAccount().equals(Account.MYSHARE)) {
            u.setCurrentMyShareCredit(u.getCurrentMyShareCredit() + t.getCredit());
        }
    }

    @Override
    protected JpaRepository<User, Long> getRepository() {
        return userRepository;
    }

    @Override
    public Class<User> getEntityClass() {
        return User.class;
    }

    @Override
    public User createEntity() {
        return new User();
    }

    /**
     * Gives the user the base church ({@code app.users.baseChurch}). Only for users who are known to belong to it (the
     * CSV import). Saving never touches the church: users without one only get the balance and the profile in the app,
     * and used to be moved into the base church again by every save (password change, status recalculation, ...).
     */
    public void assignBaseChurch(User user) {
        churchService.findById(baseChurch).ifPresent(user::setChurch);
    }
}
