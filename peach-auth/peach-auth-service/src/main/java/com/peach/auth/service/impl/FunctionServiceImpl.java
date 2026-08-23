package com.peach.auth.service.impl;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.auth.dto.FunctionDTO;
import com.peach.auth.dao.FunctionDao;
import com.peach.auth.entity.FunctionDO;
import com.peach.auth.qo.FunctionQO;
import com.peach.auth.service.IFunctionService;
import com.peach.auth.vo.FunctionVO;
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
 * @CreateTime 2026/1/17 18:30
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class FunctionServiceImpl implements IFunctionService {

        private final FunctionDao functionDao;

    @Override
    public PageInfo<FunctionVO> pageList(FunctionQO functionQO) {
        PageInfo<FunctionVO> pageInfo = PageHelper.startPage(functionQO.getPageNum(), functionQO.getPageSize())
                .doSelectPageInfo(() -> functionDao.select(new FunctionDO()));
        return pageInfo;
    }

    @Override
    public List<FunctionVO> list(FunctionQO functionQO) {
        return functionDao.select(new FunctionDO());
    }

    @Override
    public FunctionVO selectById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return new FunctionVO();
        }
        return functionDao.selectById(id);
    }

    @Override
    public void add(FunctionDTO functionDTO) {
        FunctionDO functionDO = new FunctionDO();
        BeanUtils.copyProperties(functionDTO, functionDO);
        functionDO.fillCreateTime(null);
        if (functionDO.getIsDelete() == null) {
            functionDO.setIsDelete(PubCommonConst.LOGIC_FLASE);
        }
        functionDao.insert(functionDO);
    }

    @Override
    public void delById(String id) {
        if (StringUtil.isBlank(id)) {
            log.info("id is blank");
            return;
        }
        functionDao.delById(id);
    }

    @Override
    public void update(FunctionDTO functionDTO) {
        FunctionDO functionDO = new FunctionDO();
        BeanUtils.copyProperties(functionDTO, functionDO);
        functionDO.fillModifyTime(null);
        functionDao.updateById(functionDO);
    }
}
