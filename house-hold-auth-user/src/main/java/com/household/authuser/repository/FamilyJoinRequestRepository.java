package com.household.authuser.repository;

import com.household.authuser.entity.FamilyJoinRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyJoinRequestRepository extends JpaRepository<FamilyJoinRequest, Long> {

    List<FamilyJoinRequest> findByFamilyIdAndStatusOrderByCreatedAtDesc(Long familyId, String status);

    List<FamilyJoinRequest> findByUserIdAndRequestTypeAndStatusOrderByCreatedAtDesc(
            Long userId, String requestType, String status);

    Optional<FamilyJoinRequest> findByFamilyIdAndUserIdAndStatus(Long familyId, Long userId, String status);

    List<FamilyJoinRequest> findByUserIdAndRequestTypeOrderByCreatedAtDesc(Long userId, String requestType);
}
