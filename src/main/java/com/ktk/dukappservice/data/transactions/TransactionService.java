package com.ktk.dukappservice.data.transactions;

import com.ktk.dukappservice.data.rounds.Round;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TransactionService extends BaseService<Transaction, Long> {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction save(Transaction t) {
        if (t.getCreateDateTime() == null) {
            t.setCreateDateTime(LocalDateTime.now());
        }
        return transactionRepository.save(t);
    }

    public Iterable<Transaction> findAllByCreateUser(User createUser) {
        return transactionRepository.findAllByCreateUser(createUser);
    }

    public Optional<Transaction> findByName(String name) {
        return transactionRepository.findByName(name);
    }

    @Override
    protected JpaRepository<Transaction, Long> getRepository() {
        return transactionRepository;
    }

    @Override
    public Class<Transaction> getEntityClass() {
        return Transaction.class;
    }

    @Override
    public Transaction createEntity() {
        return new Transaction();
    }

    public List<Transaction> findAllByRound(Round round) {
        return transactionRepository.findAllByRoundNr(round.getStartDateTime(), round.getEndDateTime());

    }
}
