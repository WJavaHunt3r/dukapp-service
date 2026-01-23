
package com.ktk.dukappservice.data.frakareweek;

import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.userfrakarestreak.UserFraKareWeekService;
import com.ktk.dukappservice.data.users.UserService;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.Role;
import com.ktk.dukappservice.enums.TransactionType;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalField;
import java.time.temporal.WeekFields;
import java.util.List;

@Service
public class FraKareWeekService extends BaseService<FraKareWeek, Long> {

    private final FraKareWeekRepository repository;
    private final UserFraKareWeekService userFraKareWeekService;
    private final TransactionService transactionService;
    private final UserService userService;

    public FraKareWeekService(FraKareWeekRepository repository, UserFraKareWeekService userFraKareWeekService, TransactionService transactionService, UserService userService) {
        this.repository = repository;
        this.userFraKareWeekService = userFraKareWeekService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @Override
    protected JpaRepository<FraKareWeek, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<FraKareWeek> getEntityClass() {
        return FraKareWeek.class;
    }

    @Override
    public FraKareWeek createEntity() {
        return new FraKareWeek();
    }

    public List<FraKareWeek> fetchByQuery(Integer year, Integer weekNumber) {
        return repository.fetchByQuery(year, weekNumber);
    }

    @Scheduled(cron = "0 0 7 * * MON")
//    @Scheduled(cron = "0 9 21 * * MON")
    public void createFraKareWeek() {
        LocalDate date = LocalDate.now();
        int weekNumber = date.get(WeekFields.ISO.weekOfWeekBasedYear());

        if (fetchByQuery(date.getYear(), weekNumber).isEmpty()) {
            FraKareWeek week = createEntity();
            week.setWeekNumber(weekNumber);
            week.setYear(date.getYear());
            week.setLocked(false);
            week.setActiveWeek(true);

            TemporalField dayOfWeek = WeekFields.ISO.dayOfWeek();
            LocalDate monday = date.with(dayOfWeek, dayOfWeek.range().getMinimum());
            week.setWeekStartDate(monday);
            week.setWeekEndDate(monday.plusDays(4));

            Transaction transaction = new Transaction();
            transaction.setName(week.getWeekNumber() + ". hét Fra Kare Streak");
            transaction.setTransactionType(TransactionType.BMM_PERFECT_WEEK);
            transaction.setAccount(Account.OTHER);
            transaction.setCreateDateTime(LocalDateTime.now());
            transaction.setCreateUser(userService.findAllByRole(Role.ADMIN).iterator().next());
            Transaction savedTransaction = transactionService.save(transaction);
            week.setTransactionId(savedTransaction.getId());

            userFraKareWeekService.createUserFraKareWeeks(save(week));
        } else {
            userFraKareWeekService.createUserFraKareWeeks(fetchByQuery(date.getYear(), weekNumber).get(0));
        }
    }

//    @Scheduled(cron = "0 33 10 * * FRI")
    public void unlockFraKareWeek() {
        LocalDate date = LocalDate.now();
        int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
        List<FraKareWeek> currentWeeks = fetchByQuery(date.getYear(), week);
        currentWeeks.forEach(curr -> {
            if (curr.isActiveWeek()) {
                curr.setLocked(false);
                save(curr);
            }
        });
    }
}
