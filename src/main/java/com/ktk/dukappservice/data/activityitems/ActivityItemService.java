package com.ktk.dukappservice.data.activityitems;

import com.ktk.dukappservice.service.BaseService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityItemService extends BaseService<ActivityItem, Long> {
    private ActivityItemRepository activityItemRepository;

    public ActivityItemService(ActivityItemRepository activityItemRepository) {
        this.activityItemRepository = activityItemRepository;
    }

    public List<ActivityItem> findByActivity(Long activityId) {
        return activityItemRepository.findByActivity_Id(activityId);
    }

    public List<ActivityItem> fetchByQuery(Long userId, Boolean registeredInApp, Long roundId, String searchText) {
        return activityItemRepository.fetchByQuery(userId, registeredInApp, roundId, searchText);
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
