package com.ktk.dukappservice.data.activityitems;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityItemRepository extends JpaRepository<ActivityItem, Long> {

    @Query(value = "SELECT ai from ActivityItem ai join fetch ai.activity join fetch ai.round join fetch  ai.user where " +
            " ( ai.activity.id = ?1 or ?1 is NULL  ) " +
            " and ( ai.user.id = ?2 or ?2 is NULL  ) " +
            " and ( ai.activity.registeredInApp = ?3 or ?3 is null ) " +
            " and ( ai.round = ?4 or ?4 is NULL ) " +
            " and ( lower(ai.description) like lower(concat('%', concat(?5, '%'))) or ?5 is null )",
            countQuery = "SELECT count(*) from ActivityItem ai where " +
                    " ( ai.activity.id = ?1 or ?1 is NULL  ) " +
                    " and ( ai.user.id = ?2 or ?2 is NULL  ) " +
                    " and ( ai.activity.registeredInApp = ?3 or ?3 is null ) " +
                    " and ( ai.round = ?4 or ?4 is NULL ) " +
                    " and ( lower(ai.description) like lower(concat('%', concat(?5, '%'))) or ?5 is null )")
    Page<ActivityItem> fetchByQuery(Long activityId, Long userId, Boolean registeredInApp, Long roundId, String searchText, Pageable pageable);

    @Query("select sum(ai.hours) from ActivityItem ai where ai.activity.id = ?1 ")
    double sumHoursByActivity(Long activityId);
}
