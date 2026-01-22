package com.ktk.workhuservice.data.transactionitems;

import com.ktk.workhuservice.data.paceteam.PaceTeam;
import com.ktk.workhuservice.data.rounds.Round;
import com.ktk.workhuservice.data.users.User;
import com.ktk.workhuservice.enums.Account;
import com.ktk.workhuservice.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {

    @Query("SELECT SUM(ti.points) FROM TransactionItem ti where ti.round = ?2 and ti.user = ?1 ")
    Double sumPointsByUserAndRound(User user, Round round);

    @Query("SELECT SUM(ti.points) FROM TransactionItem ti where ti.round.season.seasonYear = ?2 and ti.user = ?1 ")
    Double sumPointsByUserAndSeasonYear(User user,Integer seasonYear);

    @Query("SELECT SUM(ti.credit) FROM TransactionItem ti where ti.round = ?2 and ti.user = ?1 ")
    Integer sumCreditsByUserAndRound(User user, Round r);

    @Query("SELECT SUM(ti.credit) FROM TransactionItem ti where ti.round.season.seasonYear = ?2 and ti.user = ?1 and ti.account = ?3")
    Integer sumCreditByUserAndSeasonYear(User user, Integer seasonYear, Account account);

    Integer countByTransactionId(Long id);

    @Query("SELECT ti FROM TransactionItem ti where ( ti.transactionType = ?1 or ?1 is null) " +
            " and (ti.transactionDate > ?2 and ti.transactionDate < ?3 or ?2 is null or ?3 is null) " +
            " and (ti.transactionId = ?4 or ?4 is null) " +
            " and (ti.round.id = ?5 or ?5 is null) " +
            " and (ti.user.id = ?6 or ?6 is null) " +
            " and (ti.round.season.seasonYear = ?7 or ?7 is null) ")
    List<TransactionItem> fetchByQuery(TransactionType transactionType, LocalDate startDate, LocalDate endDate, Long transactionId, Long roundId, Long userId, Integer seasonYear);

    @Query("SELECT SUM(ti.hours) FROM TransactionItem ti where ti.round = ?2 and ti.user.paceTeam = ?1")
    Double sumHoursByTeamAndRound(PaceTeam team, Round round);

    @Query("SELECT SUM(ti.credit) FROM TransactionItem ti where ti.round = ?2 and ti.user.paceTeam = ?1 and ti.transactionType = 'CREDIT' ")
    Integer sumCreditsByTeamAndRound(PaceTeam team, Round round);
}
