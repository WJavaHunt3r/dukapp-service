package com.ktk.dukappservice.data.activity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    @Query("SELECT ac FROM Activity ac WHERE ((ac.responsible.id = ?1 OR ?1 IS NULL) " +
            " OR (ac.createUser.id = ?5 OR ?5 IS NULL)" +
            " OR (ac.employer.id = ?2 OR ?2 IS NULL)) " +
            " AND (ac.registeredInApp = ?3 OR ?3 IS NULL) " +
            " AND (ac.registeredInMyShare = ?4 OR ?4 IS NULL) " +
            " AND ((cast(?6 as date) is null  OR cast(?7 as date ) is null  ) or (ac.activityDateTime >= ?6 and ac.activityDateTime <= ?7 )) " +
            " AND (lower(ac.description) like lower(concat('%', concat(?8, '%')))  OR ?8 IS NULL)")
List<Activity> fetchByQuery(Long responsible, Long employer, Boolean registeredInApp, Boolean registeredInMyShare, Long createUser, LocalDateTime dateFrom, LocalDateTime dateTo, String searchText);
}
