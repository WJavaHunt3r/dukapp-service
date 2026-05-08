package com.ktk.dukappservice.data.activityitems;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityItemService extends BaseService<ActivityItem, Long> {
    private ActivityItemRepository activityItemRepository;

    public ActivityItemService(ActivityItemRepository activityItemRepository) {
        this.activityItemRepository = activityItemRepository;
    }

    public Page<ActivityItem> findByActivity(Long activityId) {
        return fetchByQuery(activityId, null, null, null, null, null);
    }

    public Page<ActivityItem> fetchByQuery(Long activityId, Long userId, Boolean registeredInApp, Long roundId, String searchText, Pageable pageable) {
        return activityItemRepository.fetchByQuery(activityId, userId, registeredInApp, roundId, searchText, pageable);
    }

    public void deleteByActivityId(Long activityId) {
        findByActivity(activityId).forEach(a -> deleteById(a.getId()));
    }

    public double sumHoursByActivity(Long activityId) {
        return activityItemRepository.sumHoursByActivity(activityId);
    }

    @Override
    protected JpaRepository<ActivityItem, Long> getRepository() {
        return activityItemRepository;
    }

    @Override
    public Class<ActivityItem> getEntityClass() {
        return ActivityItem.class;
    }

    @Override
    public ActivityItem createEntity() {
        return new ActivityItem();
    }
}
