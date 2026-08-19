package com.peach.auth.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dao.OrganizationDao;
import com.peach.auth.dto.OrganizationDTO;
import com.peach.auth.entity.OrganizationDO;
import com.peach.auth.qo.OrganizationQO;
import com.peach.auth.service.IOrganizationService;
import com.peach.auth.vo.OrganizationVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * 机构服务实现。
 *
 * <p>负责机构的分页、查询以及基础增删改，不承载跨机构权限逻辑。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 14:10
 */
@Slf4j
@Indexed
@Service
public class OrganizationServiceImpl implements IOrganizationService {

    @Resource
    private OrganizationDao organizationDao;

    @Override
    public PageInfo<OrganizationVO> pageList(OrganizationQO organizationQO) {
        return PageHelper.startPage(organizationQO.getPageNum(), organizationQO.getPageSize())
                .doSelectPageInfo(() -> organizationDao.selectByQO(organizationQO));
    }

    @Override
    public List<OrganizationVO> list(OrganizationQO organizationQO) {
        return organizationDao.selectByQO(organizationQO);
    }

    @Override
    public OrganizationVO selectById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new OrganizationVO();
        }
        return organizationDao.selectById(id);
    }

    @Override
    public void add(OrganizationDTO organizationDTO) {
        OrganizationDO organizationDO = new OrganizationDO();
        BeanUtils.copyProperties(organizationDTO, organizationDO);
        organizationDO.fillCreateTime(null);
        if (organizationDO.getIsDelete() == null) {
            organizationDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        if (StringUtil.isBlank(organizationDO.getStatus())) {
            organizationDO.setStatus("ENABLE");
        }
        if (organizationDO.getSortNum() == null) {
            organizationDO.setSortNum(0);
        }
        organizationDao.insert(organizationDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        organizationDao.delById(id);
    }

    @Override
    public void update(OrganizationDTO organizationDTO) {
        OrganizationDO organizationDO = new OrganizationDO();
        BeanUtils.copyProperties(organizationDTO, organizationDO);
        organizationDO.fillModifyTime(null);
        organizationDao.updateById(organizationDO);
    }
}
