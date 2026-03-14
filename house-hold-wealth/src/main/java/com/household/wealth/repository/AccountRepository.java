package com.household.wealth.repository;

import com.household.wealth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    List<Account> findByFamilyId(Long familyId);

    @Query("SELECT DISTINCT a.userId FROM Account a")
    List<Long> findDistinctUserIds();

    @Modifying
    @Query("UPDATE Account a SET a.familyId = :familyId WHERE a.userId = :userId")
    int updateFamilyIdByUserId(@Param("userId") Long userId, @Param("familyId") Long familyId);

    @Modifying
    @Query("UPDATE Account a SET a.familyId = null WHERE a.userId = :userId")
    int clearFamilyIdByUserId(@Param("userId") Long userId);
}
