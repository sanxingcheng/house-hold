package com.household.wealth.service;

import com.household.common.exception.ForbiddenException;
import com.household.common.exception.NotFoundException;
import com.household.common.util.SnowflakeIdGenerator;
import com.household.wealth.cache.SummaryCacheService;
import com.household.wealth.client.AuthUserClient;
import com.household.wealth.dto.request.FamilyAssetCreateRequest;
import com.household.wealth.dto.request.FamilyAssetUpdateRequest;
import com.household.wealth.dto.response.FamilyAssetResponse;
import com.household.wealth.entity.FamilyAsset;
import com.household.wealth.repository.FamilyAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyAssetService {

    private final FamilyAssetRepository familyAssetRepository;
    private final AuthUserClient authUserClient;

    @Autowired(required = false)
    private SummaryCacheService summaryCacheService;

    private static final SnowflakeIdGenerator ID_GEN = new SnowflakeIdGenerator(2, 3);

    public List<FamilyAssetResponse> listByFamily(Long familyId) {
        return familyAssetRepository.findByFamilyIdOrderByCreatedAtDesc(familyId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyAssetResponse create(Long userId, Long familyId, FamilyAssetCreateRequest req) {
        requireAdmin(userId, familyId);
        FamilyAsset asset = new FamilyAsset();
        asset.setId(ID_GEN.nextId());
        asset.setFamilyId(familyId);
        asset.setAssetName(req.getAssetName());
        asset.setAssetType(req.getAssetType());
        asset.setAmount(req.getAmount());
        asset.setCurrency(req.getCurrency() != null ? req.getCurrency() : "CNY");
        asset.setRemark(req.getRemark());
        if (req.getLoanTotal() != null) {
            asset.setLoanTotal(req.getLoanTotal());
        }
        if (req.getLoanRemaining() != null) {
            asset.setLoanRemaining(req.getLoanRemaining());
        }
        if (req.getLoanOnly() != null) {
            asset.setLoanOnly(req.getLoanOnly());
        }
        asset.setCreatedBy(userId);
        familyAssetRepository.save(asset);
        invalidateFamilySummaryCache(familyId);
        return toResponse(asset);
    }

    @Transactional(rollbackFor = Exception.class)
    public FamilyAssetResponse update(Long userId, Long familyId, Long assetId, FamilyAssetUpdateRequest req) {
        requireAdmin(userId, familyId);
        FamilyAsset asset = familyAssetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("资产不存在"));
        if (!asset.getFamilyId().equals(familyId)) {
            throw new ForbiddenException("资产不属于该家庭");
        }
        if (req.getAssetName() != null) asset.setAssetName(req.getAssetName());
        if (req.getAssetType() != null) asset.setAssetType(req.getAssetType());
        if (req.getAmount() != null) asset.setAmount(req.getAmount());
        if (req.getCurrency() != null) asset.setCurrency(req.getCurrency());
        if (req.getRemark() != null) asset.setRemark(req.getRemark());
        if (req.getLoanTotal() != null) asset.setLoanTotal(req.getLoanTotal());
        if (req.getLoanRemaining() != null) asset.setLoanRemaining(req.getLoanRemaining());
        if (req.getLoanOnly() != null) asset.setLoanOnly(req.getLoanOnly());
        familyAssetRepository.save(asset);
        invalidateFamilySummaryCache(familyId);
        return toResponse(asset);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long familyId, Long assetId) {
        requireAdmin(userId, familyId);
        FamilyAsset asset = familyAssetRepository.findById(assetId)
                .orElseThrow(() -> new NotFoundException("资产不存在"));
        if (!asset.getFamilyId().equals(familyId)) {
            throw new ForbiddenException("资产不属于该家庭");
        }
        familyAssetRepository.delete(asset);
        invalidateFamilySummaryCache(familyId);
    }

    private void invalidateFamilySummaryCache(Long familyId) {
        if (summaryCacheService != null) {
            summaryCacheService.invalidateFamily(familyId);
        }
    }

    private void requireAdmin(Long userId, Long familyId) {
        try {
            Boolean admin = authUserClient.checkFamilyAdmin(familyId, userId).get("admin");
            if (!Boolean.TRUE.equals(admin)) {
                throw new ForbiddenException("需要家庭管理员权限");
            }
        } catch (Exception e) {
            if (e instanceof ForbiddenException) throw e;
            throw new ForbiddenException("无法验证管理员权限");
        }
    }

    private FamilyAssetResponse toResponse(FamilyAsset a) {
        return new FamilyAssetResponse(
                String.valueOf(a.getId()),
                String.valueOf(a.getFamilyId()),
                a.getAssetName(),
                a.getAssetType(),
                a.getAmount(),
                a.getCurrency(),
                a.getRemark(),
                String.valueOf(a.getCreatedBy()),
                a.getCreatedAt() != null ? a.getCreatedAt().toString() : null,
                a.getLoanTotal(),
                a.getLoanRemaining(),
                a.getLoanOnly()
        );
    }
}
