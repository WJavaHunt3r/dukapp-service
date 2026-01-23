package com.ktk.dukappservice.data.transactionitems;

import com.ktk.dukappservice.data.paceteam.PaceTeam;
import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.enums.Account;
import com.ktk.dukappservice.enums.TransactionType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionItemService {

    private final TransactionItemRepository transactionItemRepository;

    public TransactionItemService(TransactionItemRepository transactionItemRepository) {
        this.transactionItemRepository = transactionItemRepository;
    }

    public Optional<TransactionItem> findById(Long id) {
        return transactionItemRepository.findById(id);
    }

    public List<TransactionItem> fetchByQuery(TransactionType transactionType, LocalDate startDate, LocalDate endDate, Long transactionId, Long roundId, Long userId, Integer seasonYear) {
        return transactionItemRepository.fetchByQuery(transactionType, startDate, endDate, transactionId, roundId, userId, seasonYear);
    }

    public Double sumPointsByUserAndRound(User user, Round s) {
        return transactionItemRepository.sumPointsByUserAndRound(user, s);
    }

    public Double sumPointsByUserAndSeasonYear(User user, Integer seasonYear) {
        return transactionItemRepository.sumPointsByUserAndSeasonYear(user, seasonYear);
    }

    public Integer sumCreditsByUserAndRound(User user, Round r) {
        return transactionItemRepository.sumCreditsByUserAndRound(user, r);
    }

    public Integer sumCreditByUserAndSeasonYear(User user, Integer year, Account account) {
        return transactionItemRepository.sumCreditByUserAndSeasonYear(user, year, account);
    }

    public Integer countByTransactionId(Long id) {
        return transactionItemRepository.countByTransactionId(id);
    }

    public void deleteByTransactionId(Long transactionId) {
        fetchByQuery(null, null, null, transactionId, null, null, null).forEach(t -> transactionItemRepository.deleteById(t.getId()));
    }

    public void deleteById(Long transactionItemId) {
        transactionItemRepository.deleteById(transactionItemId);
    }

    public TransactionItem save(TransactionItem t) {
        if (t.getPoints() != 0.0 || t.getHours() != 0.0 || t.getCredit() != 0) {
            if (t.getAccount().equals(Account.MYSHARE)) {
                if (t.getTransactionType().equals(TransactionType.CREDIT) && t.getCredit() != 0) {
                    double creditPoints = (double) t.getCredit() / 1000.0;
                    t.setPoints(creditPoints);
                } else if (TransactionType.DUKA_MUNKA_2000.equals(t.getTransactionType()) && t.getHours() != 0) {
                    t.setPoints(t.getHours() * 4.0);
                    t.setCredit((int) (t.getHours() * 2000));
                } else if (TransactionType.HOURS.equals(t.getTransactionType()) && t.getHours() != 0) {
                    t.setPoints(t.getHours() * 4.0);
                    t.setCredit((int) (t.getHours() * 3000));
                } else if (t.getTransactionType().equals(TransactionType.DUKA_MUNKA) && t.getHours() != 0) {
                    t.setPoints(t.getHours() * 4.0);
                    t.setCredit((int) (t.getHours() * 1000));
                }

            } else if (t.getAccount().equals(Account.SAMVIRK)) {
//                double creditPoints = (double) t.getCredit() / 1000.0;
//                double samvirkpoints = StreamSupport.stream(transactionItemRepository.findAllByUserAndRoundAndAccount(t.getUser(), t.getRound(), Account.SAMVIRK).spliterator(), false)
//                        .mapToDouble(TransactionItem::getPoints).sum();
//                double maxPoints = t.getRound().getSamvirkMaxPoints();
//                double onTrackPoints = t.getRound().getSamvirkOnTrackPoints();
//                if (maxPoints != 0 && creditPoints + samvirkpoints > maxPoints - onTrackPoints) {
//                    t.setPoints(maxPoints - onTrackPoints - samvirkpoints < 0 ? 0 : maxPoints - onTrackPoints - samvirkpoints);
//                } else {
//                    t.setPoints(creditPoints);
//                }

            } else if (t.getAccount().equals(Account.OTHER)) {
                if (t.getTransactionType().equals(TransactionType.POINT)) {
                    if(t.getHours() != 0) {
                        t.setPoints(t.getHours() * 6.0);
                    }
                }

                t.setCredit(0);
            }

            t.setCreateDateTime(LocalDateTime.now());
            if (t.getTransactionDate() == null) {
                t.setTransactionDate(t.getCreateDateTime().toLocalDate());
            }
            return transactionItemRepository.save(t);
        }
        return t;
    }

    public void saveAll(Iterable<TransactionItem> transactions) {
        transactions.iterator().forEachRemaining(this::save);
    }

    public Double sumHoursByTeamAndRound(PaceTeam team, Round round) {
        return transactionItemRepository.sumHoursByTeamAndRound(team, round);
    }

    public Integer sumCreditsByTeamAndRound(PaceTeam team, Round round) {
        return transactionItemRepository.sumCreditsByTeamAndRound(team, round);
    }
}
