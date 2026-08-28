package com.peach.setting.service;

import com.peach.common.PageResult;
import com.peach.setting.dto.DictItemDTO;
import com.peach.setting.dto.DictTypeDTO;
import com.peach.setting.qo.DictItemQO;
import com.peach.setting.qo.DictTypeQO;
import com.peach.setting.vo.DictItemVO;
import com.peach.setting.vo.DictTypeVO;

import java.util.List;

/**
 * 字典服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 字典服务接口
 */
public interface IDictService {

    /**
     * 分页查询字典类型列表
     *
     * @param qo 字典类型查询参数对象
     * @return 字典类型分页结果对象
     */
    PageResult<DictTypeVO> typePageList(DictTypeQO qo);

    /**
     * 根据ID查询字典类型详情
     *
     * @param id 字典类型ID
     * @return 字典类型视图对象，不存在时返回null
     */
    DictTypeVO typeSelectById(String id);

    /**
     * 新增字典类型
     *
     * @param data 字典类型数据对象
     */
    void saveType(DictTypeDTO data);

    /**
     * 修改字典类型
     *
     * @param data 字典类型数据对象
     */
    void updateType(DictTypeDTO data);

    /**
     * 批量删除字典类型
     *
     * @param ids 字典类型ID列表
     */
    void deleteType(List<String> ids);

    /**
     * 分页查询字典项列表
     *
     * @param qo 字典项查询参数对象
     * @return 字典项分页结果对象
     */
    PageResult<DictItemVO> itemPageList(DictItemQO qo);

    /**
     * 根据字典编码查询字典项列表
     *
     * @param dictCode 字典编码
     * @return 字典项视图对象列表
     */
    List<DictItemVO> itemListByDictCode(String dictCode);

    /**
     * 根据ID查询字典项详情
     *
     * @param id 字典项ID
     * @return 字典项视图对象，不存在时返回null
     */
    DictItemVO itemSelectById(String id);

    /**
     * 新增字典项
     *
     * @param data 字典项数据对象
     */
    void saveItem(DictItemDTO data);

    /**
     * 修改字典项
     *
     * @param data 字典项数据对象
     */
    void updateItem(DictItemDTO data);

    /**
     * 批量删除字典项
     *
     * @param ids 字典项ID列表
     */
    void deleteItem(List<String> ids);
}
