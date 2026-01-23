package com.ktk.dukappservice.data.users;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.seasons.Season;
import com.ktk.dukappservice.data.teams.Team;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class UserService extends BaseService<User, Long> {

    private final UserRepository userRepository;
    private final TransactionItemService transactionItemService;

    public UserService(UserRepository userRepository, TransactionItemService transactionService) {
        this.userRepository = userRepository;
        this.transactionItemService = transactionService;
    }

    public Optional<User> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        user.ifPresent(this::calculateUserPoints);
        return user;
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

    public List<User> findChildren(Long familyId) {
        return userRepository.findChildren(familyId);
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
        transactionItemService.fetchByQuery(null, null, null, null, null, u.getId(), LocalDate.now().getYear()).forEach(t -> addTransaction(t, u));
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
}
