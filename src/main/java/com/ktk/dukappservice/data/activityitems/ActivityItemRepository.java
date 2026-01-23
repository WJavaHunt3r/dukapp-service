package com.ktk.dukappservice.data.activityitems;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityItemRepository extends JpaRepository<ActivityItem, Long> {

    List<ActivityItem> findByActivity_Id(Long activityId);

    @Query("SELECT ai from ActivityItem ai where " +
            " ( ai.user.id = ?1 or ?1 is NULL  ) " +
            " and ( ai.activity.registeredInApp = ?2 or ?2 is null ) " +
            " and ( ai.round = ?3 or ?3 is NULL ) " +
            " and ( lower(ai.description) like lower(concat('%', concat(?4, '%'))) or ?4 is null )")
    List<ActivityItem> fetchByQuery(Long userId, Boolean registeredInApp, Long roundId, String searchText);

    @Query("select sum(ai.hours) from ActivityItem ai where ai.activity.id = ?1 ")
    double sumHoursByActivity(Long activityId);
}
