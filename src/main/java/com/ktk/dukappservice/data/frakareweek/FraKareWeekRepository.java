package com.ktk.dukappservice.data.frakareweek;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraKareWeekRepository extends JpaRepository<FraKareWeek, Long> {
    @Query(" SELECT fks from FraKareWeek fks where ( fks.year = ?1 or ?1 is null) " +
            " and (fks.weekNumber = ?2 or  ?2 is null) order by fks.weekNumber desc ")
    List<FraKareWeek> fetchByQuery(Integer year, Integer weekNumber);

}
