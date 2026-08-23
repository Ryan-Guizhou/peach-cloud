package com.peach.auth.service.impl;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.ResourceDTO;
import com.peach.auth.dao.ResourceDao;
import com.peach.auth.entity.ResourceDO;
import com.peach.auth.qo.ResourceQO;
import com.peach.auth.service.IResouceService;
import com.peach.auth.vo.ResourceVO;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 18:35
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ResouceServceImpl implements IResouceService {

        private final ResourceDao resourceDao;

    @Override
    public PageInfo<ResourceVO> pageList(ResourceQO resourceQO) {
        PageInfo<ResourceVO> pageInfo = PageHelper.startPage(resourceQO.getPageNum(), resourceQO.getPageSize())
                .doSelectPageInfo(() -> resourceDao.selectByQO(resourceQO));
        return pageInfo;
    }

    @Override
    public List<ResourceVO> list(ResourceQO resourceQO) {
        return resourceDao.select(new ResourceDO());
    }

    @Override
    public ResourceVO selectById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new ResourceVO();
        }
        return resourceDao.selectById(id);
    }

    @Override
    public void add(ResourceDTO resourceDTO) {
        ResourceDO resourceDO = new ResourceDO();
        BeanUtils.copyProperties(resourceDTO, resourceDO);
        resourceDO.fillCreateTime(null);
        if (resourceDO.getIsDelete() == null) {
            resourceDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        resourceDao.insert(resourceDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        resourceDao.delById(id);
    }

    @Override
    public void update(ResourceDTO resourceDTO) {
        ResourceDO resourceDO = new ResourceDO();
        BeanUtils.copyProperties(resourceDTO, resourceDO);
        resourceDO.fillModifyTime(null);
        resourceDao.updateById(resourceDO);
    }
}
