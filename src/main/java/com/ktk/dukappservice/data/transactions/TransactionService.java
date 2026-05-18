package com.ktk.dukappservice.data.transactions;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ktk.dukappservice.data.ServiceUtils.getDateFrom;
import static com.ktk.dukappservice.data.ServiceUtils.getDateTo;

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

    public Page<Transaction> fetchByQuery(String startDateTime, String endDateTime, Long createUserId, Pageable pageable) {
        return transactionRepository.fetchByQuery(getDateFrom(startDateTime), getDateTo(endDateTime), createUserId, pageable);

    }
}
