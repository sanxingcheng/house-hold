package com.household.wealth.repository;

import com.household.wealth.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    List<Account> findByFamilyId(Long familyId);

    @Query("SELECT DISTINCT a.userId FROM Account a")
    List<Long> findDistinctUserIds();
}
