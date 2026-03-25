package com.household.wealth.service;

import com.household.common.exception.ForbiddenException;
import com.household.common.util.SnowflakeIdGenerator;
import com.household.wealth.client.AuthUserClient;
import com.household.wealth.dto.response.OperationLogResponse;
import com.household.wealth.entity.OperationLog;
import com.household.wealth.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogService {

    private static final SnowflakeIdGenerator ID_GEN = new SnowflakeIdGenerator(2, 4);

    private final OperationLogRepository operationLogRepository;
    private final AuthUserClient authUserClient;

    public void createLog(Long userId, Long familyId, String action, String resourceType, String resourceId, String detail) {
        OperationLog log = new OperationLog();
        log.setId(ID_GEN.nextId());
        log.setUserId(userId);
        log.setFamilyId(familyId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId != null ? resourceId : "");
        log.setDetail(detail != null && detail.length() > 512 ? detail.substring(0, 512) : detail);
        operationLogRepository.save(log);
    }

    public Page<OperationLogResponse> listByFamily(Long userId, Long familyId, int page, int size) {
        try {
            Boolean admin = authUserClient.checkFamilyAdmin(familyId, userId).get("admin");
            if (!Boolean.TRUE.equals(admin)) {
                throw new ForbiddenException("需要家庭管理员权限查看操作日志");
            }
        } catch (ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            throw new ForbiddenException("无法验证家庭权限");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return operationLogRepository.findByFamilyIdOrderByCreatedAtDesc(familyId, pageable)
                .map(this::toResponse);
    }

    private OperationLogResponse toResponse(OperationLog log) {
        return new OperationLogResponse(
                String.valueOf(log.getId()),
                String.valueOf(log.getUserId()),
                log.getFamilyId() != null ? String.valueOf(log.getFamilyId()) : null,
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetail(),
                log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
    }
}
