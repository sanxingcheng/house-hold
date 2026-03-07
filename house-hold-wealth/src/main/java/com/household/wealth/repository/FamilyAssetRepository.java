package com.household.wealth.repository;

import com.household.wealth.entity.FamilyAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyAssetRepository extends JpaRepository<FamilyAsset, Long> {

    List<FamilyAsset> findByFamilyIdOrderByCreatedAtDesc(Long familyId);
}
