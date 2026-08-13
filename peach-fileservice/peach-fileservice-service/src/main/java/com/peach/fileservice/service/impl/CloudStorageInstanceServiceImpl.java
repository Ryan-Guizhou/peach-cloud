package com.peach.fileservice.service.impl;

import com.peach.common.IDGeneratorUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.dao.CloudStorageInstanceDao;
import com.peach.fileservice.dto.CloudStorageInstanceSaveDTO;
import com.peach.fileservice.entity.CloudStorageInstanceDO;
import com.peach.fileservice.qo.CloudStorageInstanceQO;
import com.peach.fileservice.service.ICloudStorageInstanceService;
import com.peach.fileservice.vo.CloudStorageInstanceVO;
import com.peach.manager.CloudStorageManagerService;
import com.peach.satoken.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * Default cloud storage instance management service.
 */
@Slf4j
@Indexed
@Service
public class CloudStorageInstanceServiceImpl implements ICloudStorageInstanceService {

    private static final Integer ENABLED = 1;

    private static final Integer DISABLED = 0;

    @Resource
    private CloudStorageInstanceDao cloudStorageInstanceDao;

    @Resource
    private CloudStorageInstanceSupport cloudStorageInstanceSupport;

    @Resource
    private CloudStorageManagerService cloudStorageManagerService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CloudStorageInstanceVO add(CloudStorageInstanceSaveDTO data) {
        validateSaveRequest(data, false);
        CloudStorageInstanceDO instanceDO = buildForSave(data, null);
        instanceDO.setInstanceId(IDGeneratorUtil.UUID());
        instanceDO.fillCreateTime();
        cloudStorageInstanceDao.insert(instanceDO);
        return selectById(instanceDO.getInstanceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CloudStorageInstanceVO update(CloudStorageInstanceSaveDTO data) {
        validateSaveRequest(data, true);
        CloudStorageInstanceVO stored = cloudStorageInstanceDao.selectById(data.getInstanceId());
        if (stored == null) {
            throw new RuntimeException("cloud storage instance not found");
        }
        CloudStorageInstanceDO instanceDO = buildForSave(data, stored);
        instanceDO.setInstanceId(data.getInstanceId());
        instanceDO.fillModifyTime(cloudStorageInstanceSupport.currentOperator());
        cloudStorageInstanceDao.updateById(instanceDO);
        return selectById(data.getInstanceId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String instanceId) {
        requireById(instanceId);
        cloudStorageInstanceDao.delById(instanceId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(String instanceId) {
        updateStatus(instanceId, ENABLED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(String instanceId) {
        updateStatus(instanceId, DISABLED);
    }

    @Override
    public boolean testConnection(CloudStorageInstanceSaveDTO data) {
        validateSaveRequest(data, false);
        CloudStorageInstanceDO instanceDO = buildForSave(data, null);
        return cloudStorageManagerService.testConnection(cloudStorageInstanceSupport.toProviderConfig(instanceDO));
    }

    @Override
    public CloudStorageInstanceVO selectById(String instanceId) {
        return requireById(instanceId);
    }


    @Override
    public List<CloudStorageInstanceVO> list(CloudStorageInstanceQO data) {
        if (data == null) {
            data = new CloudStorageInstanceQO();
        }
        fillCurrentTenantOrg(data);
        CloudStorageInstanceDO query = new CloudStorageInstanceDO();
        query.setInstanceName(data.getInstanceName());
        query.setStoreType(data.getStoreType());
        query.setEnabled(data.getEnabled());
        query.setTenantId(data.getTenantId());
        query.setOrgId(data.getOrgId());
        List<CloudStorageInstanceVO> records = cloudStorageInstanceDao.select(query);
        return sanitize(records);
    }

    @Override
    public List<CloudStorageInstanceVO> listEnabled() {
        List<CloudStorageInstanceVO> records = cloudStorageInstanceDao.selectAllEnabled();
        return sanitize(records);
    }

    private void updateStatus(String instanceId, Integer status) {
        requireById(instanceId);
        CloudStorageInstanceDO update = new CloudStorageInstanceDO();
        update.setInstanceId(instanceId);
        update.setEnabled(status);
        update.fillModifyTime(cloudStorageInstanceSupport.currentOperator());
        cloudStorageInstanceDao.updateById(update);
    }

    private CloudStorageInstanceDO buildForSave(CloudStorageInstanceSaveDTO data, CloudStorageInstanceVO stored) {
        CloudStorageInstanceDO instanceDO = new CloudStorageInstanceDO();
        instanceDO.setInstanceName(data.getInstanceName());
        instanceDO.setStoreType(data.getStoreType());
        instanceDO.setEndpoint(data.getEndpoint());
        instanceDO.setRegion(data.getRegion());
        instanceDO.setBucketName(data.getBucketName());
        instanceDO.setPrefix(data.getPrefix());
        instanceDO.setAccessKey(data.getAccessKey());
        if (StringUtil.isNotBlank(data.getSecretKey())) {
            instanceDO.setSecretKey(cloudStorageInstanceSupport.encryptSecret(data.getSecretKey()));
        } else if (stored != null) {
            instanceDO.setSecretKey(stored.getSecretKey());
        }
        instanceDO.setRootPath(data.getRootPath());
        instanceDO.setDomain(data.getDomain());
        instanceDO.setPathStyleAccess(data.getPathStyleAccess() == null ? 0 : data.getPathStyleAccess());
        instanceDO.setPublicRead(data.getPublicRead() == null ? 0 : data.getPublicRead());
        instanceDO.setExtraJson(data.getExtraJson());
        instanceDO.setEnabled(data.getEnabled() == null ? ENABLED : data.getEnabled());
        instanceDO.setRemark(data.getRemark());
        return instanceDO;
    }

    private CloudStorageInstanceVO requireById(String instanceId) {
        CloudStorageInstanceVO instanceVO = cloudStorageInstanceDao.selectById(instanceId);
        if (instanceVO == null) {
            throw new RuntimeException("cloud storage instance not found");
        }
        instanceVO.setSecretKeyMasked(maskSecret(instanceVO.getSecretKey()));
        instanceVO.setSecretKey(null);
        return instanceVO;
    }

    private void validateSaveRequest(CloudStorageInstanceSaveDTO data, boolean update) {
        if (data == null) {
            throw new RuntimeException("request data is empty");
        }
        if (update && StringUtil.isBlank(data.getInstanceId())) {
            throw new RuntimeException("instanceId is empty");
        }
    }

    private List<CloudStorageInstanceVO> sanitize(List<CloudStorageInstanceVO> records) {
        List<CloudStorageInstanceVO> result = new ArrayList<CloudStorageInstanceVO>();
        for (CloudStorageInstanceVO record : records) {
            if (record == null) {
                continue;
            }
            record.setSecretKeyMasked(maskSecret(record.getSecretKey()));
            record.setSecretKey(null);
            result.add(record);
        }
        return result;
    }

    private String maskSecret(String secretKey) {
        if (StringUtil.isBlank(secretKey)) {
            return null;
        }
        int visible = Math.min(4, secretKey.length());
        return "****" + secretKey.substring(secretKey.length() - visible);
    }

    private void fillCurrentTenantOrg(CloudStorageInstanceQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private String requireTenantId() {
        String tenantId = SecurityContextHolder.currentTenantId();
        if (StringUtil.isBlank(tenantId)) {
            throw new IllegalStateException("Current tenant context is missing");
        }
        return tenantId;
    }

    private String requireOrgId() {
        String orgId = SecurityContextHolder.currentOrgId();
        if (StringUtil.isBlank(orgId)) {
            throw new IllegalStateException("Current organization context is missing");
        }
        return orgId;
    }
}
