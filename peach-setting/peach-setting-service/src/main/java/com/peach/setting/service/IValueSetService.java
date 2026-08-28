package com.peach.setting.service;

import com.peach.common.PageResult;
import com.peach.setting.dto.ValueSetDTO;
import com.peach.setting.dto.ValueSetItemDTO;
import com.peach.setting.qo.ValueSetItemQO;
import com.peach.setting.qo.ValueSetQO;
import com.peach.setting.vo.ValueSetItemVO;
import com.peach.setting.vo.ValueSetVO;

import java.util.List;

/**
 * 值集服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 值集服务接口
 */
public interface IValueSetService {

    /**
     * 分页查询值集列表
     *
     * @param qo 值集查询参数对象
     * @return 值集分页结果对象
     */
    PageResult<ValueSetVO> pageList(ValueSetQO qo);

    /**
     * 根据ID查询值集详情
     *
     * @param id 值集ID
     * @return 值集视图对象，不存在时返回null
     */
    ValueSetVO selectById(String id);

    /**
     * 新增值集
     *
     * @param data 值集数据对象
     */
    void save(ValueSetDTO data);

    /**
     * 修改值集
     *
     * @param data 值集数据对象
     */
    void update(ValueSetDTO data);

    /**
     * 批量删除值集
     *
     * @param ids 值集ID列表
     */
    void delete(List<String> ids);

    /**
     * 分页查询值集项列表
     *
     * @param qo 值集项查询参数对象
     * @return 值集项分页结果对象
     */
    PageResult<ValueSetItemVO> itemPageList(ValueSetItemQO qo);

    /**
     * 根据值集编码查询值集项列表
     *
     * @param valueSetCode 值集编码
     * @return 值集项视图对象列表
     */
    List<ValueSetItemVO> itemListByValueSetCode(String valueSetCode);

    /**
     * 根据ID查询值集项详情
     *
     * @param id 值集项ID
     * @return 值集项视图对象，不存在时返回null
     */
    ValueSetItemVO itemSelectById(String id);

    /**
     * 新增值集项
     *
     * @param data 值集项数据对象
     */
    void saveItem(ValueSetItemDTO data);

    /**
     * 修改值集项
     *
     * @param data 值集项数据对象
     */
    void updateItem(ValueSetItemDTO data);

    /**
     * 批量删除值集项
     *
     * @param ids 值集项ID列表
     */
    void deleteItem(List<String> ids);
}
