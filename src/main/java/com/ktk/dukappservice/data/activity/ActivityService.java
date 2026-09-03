package com.ktk.dukappservice.data.activity;

import com.ktk.dukappservice.data.activityitems.ActivityItem;
import com.ktk.dukappservice.data.activityitems.ActivityItemService;
import com.ktk.dukappservice.data.transactionitems.TransactionItem;
import com.ktk.dukappservice.data.transactionitems.TransactionItemService;
import com.ktk.dukappservice.data.transactions.Transaction;
import com.ktk.dukappservice.data.transactions.TransactionService;
import com.ktk.dukappservice.data.users.User;
import com.ktk.dukappservice.service.BaseService;
import com.ktk.dukappservice.service.TransactionServiceUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.ktk.dukappservice.data.ServiceUtils.getDateFrom;
import static com.ktk.dukappservice.data.ServiceUtils.getDateTo;

@Service
public class ActivityService extends BaseService<Activity, Long> {

    private final ActivityRepository repository;
    private final TransactionItemService transactionItemService;
    private final TransactionService transactionService;
    private final ActivityItemService activityItemService;
    private final TransactionServiceUtils transactionServiceUtils;

    public ActivityService(ActivityRepository repository, TransactionItemService transactionItemService, TransactionService transactionService, ActivityItemService activityItemService, TransactionServiceUtils transactionServiceUtils) {
        this.repository = repository;
        this.transactionItemService = transactionItemService;
        this.transactionService = transactionService;
        this.activityItemService = activityItemService;
        this.transactionServiceUtils = transactionServiceUtils;
    }

    public Page<Activity> fetchByQuery(Long responsible, Long employer, Boolean registeredInApp, Boolean registeredInMyShare, Long createUser, String searchText, Pageable pageable) {
        return fetchByQuery(responsible, employer, registeredInApp, registeredInMyShare, createUser, null, searchText, pageable);
    }

    public Page<Activity> fetchByQuery(Long responsible, Long employer, Boolean registeredInApp, Boolean registeredInMyShare, Long createUser, String referenceDate, String searchText, Pageable pageable) {
        return repository.fetchByQuery(responsible, employer, registeredInApp, registeredInMyShare, createUser, getDateFrom(referenceDate), getDateTo(referenceDate), searchText, pageable);
    }

    public Optional<Activity> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    protected JpaRepository<Activity, Long> getRepository() {
        return repository;
    }

    @Override
    public Class<Activity> getEntityClass() {
        return Activity.class;
    }

    @Override
    public Activity createEntity() {
        return new Activity();
    }

    public void rollbackTransactions(Long id) {
        transactionItemService.deleteByTransactionId(id);
        transactionService.deleteById(id);
    }

    public Long registerActivity(Activity activity, User createUser) {
        Transaction transaction = createTransaction(activity, createUser);

        try {
            for (var item : activityItemService.findByActivity(activity.getId())) {
                createTransactionItem(transaction, createUser, item);
            }
//            transactionServiceUtils.calculateAllTeamStatus();
            activity.setRegisteredInApp(true);
        } catch (Exception ignored){

        }
        return transaction.getId();
    }

    private void createTransactionItem(Transaction transaction, User createUser, ActivityItem item) {
        TransactionItem transactionItem = new TransactionItem();
        transactionItem.setHours(item.getHours());
        transactionItem.setTransactionDate(item.getActivity().getActivityDateTime().toLocalDate());
        transactionItem.setRound(item.getRound());
        transactionItem.setTransactionId(transaction.getId());
        transactionItem.setCreateDateTime(LocalDateTime.now());
        transactionItem.setCreateUser(createUser);
        transactionItem.setUser(item.getUser());
        transactionItem.setDescription(item.getDescription());
        transactionItem.setAccount(item.getAccount());
        transactionItem.setTransactionType(item.getTransactionType());

        transactionItemService.save(transactionItem);
        transactionServiceUtils.updateUserStatus(item.getRound(), transactionItem.getUser());
    }

    private Transaction createTransaction(Activity activity, User createUser) {
        Transaction transaction = new Transaction();
        transaction.setCreateUser(createUser);
        transaction.setAccount(activity.getAccount());
        transaction.setTransactionType(activity.getTransactionType());
        transaction.setName(activity.getDescription());

        return transactionService.save(transaction);
    }
}
