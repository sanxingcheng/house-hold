package com.household.authuser.repository;

import com.household.authuser.entity.FamilyMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRoleRepository extends JpaRepository<FamilyMemberRole, Long> {

    List<FamilyMemberRole> findByFamilyIdOrderByJoinedAt(Long familyId);

    long countByFamilyId(Long familyId);

    Optional<FamilyMemberRole> findByUserIdAndFamilyId(Long userId, Long familyId);

    List<FamilyMemberRole> findByFamilyIdAndIsAdminTrue(Long familyId);

    @Modifying
    @Query("UPDATE FamilyMemberRole r SET r.role = :role WHERE r.userId = :userId AND r.familyId = :familyId")
    int updateRoleByUserIdAndFamilyId(@Param("userId") Long userId,
                                      @Param("familyId") Long familyId,
                                      @Param("role") String role);

    @Modifying
    @Query("UPDATE FamilyMemberRole r SET r.isAdmin = :isAdmin WHERE r.userId = :userId AND r.familyId = :familyId")
    int updateIsAdminByUserIdAndFamilyId(@Param("userId") Long userId,
                                         @Param("familyId") Long familyId,
                                         @Param("isAdmin") Boolean isAdmin);

    void deleteByUserIdAndFamilyId(Long userId, Long familyId);
}
