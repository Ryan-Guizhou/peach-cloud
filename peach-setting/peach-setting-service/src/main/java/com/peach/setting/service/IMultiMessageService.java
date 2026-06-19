package com.peach.setting.service;

import com.peach.common.PageResult;
import com.peach.setting.dto.LanguageDTO;
import com.peach.setting.dto.MultiMessageDTO;
import com.peach.setting.qo.LanguageQO;
import com.peach.setting.qo.MulitMessageQO;
import com.peach.setting.vo.LanguageVO;
import com.peach.setting.vo.MulitMessageVO;

import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 多语言消息服务接口
 */
public interface IMultiMessageService {

    /**
     * 分页查询语言列表
     *
     * @param qo 语言查询参数对象
     * @return 语言分页结果对象
     */
    PageResult<LanguageVO> languagePageList(LanguageQO qo);

    /**
     * 根据ID查询语言详情
     *
     * @param id 语言ID
     * @return 语言视图对象，不存在时返回null
     */
    LanguageVO languageSelectById(String id);

    /**
     * 新增语言
     *
     * @param data 语言数据对象
     */
    void saveLanguage(LanguageDTO data);

    /**
     * 修改语言
     *
     * @param data 语言数据对象
     */
    void updateLanguage(LanguageDTO data);

    /**
     * 批量删除语言
     *
     * @param ids 语言ID列表
     */
    void deleteLanguage(List<String> ids);

    /**
     * 分页查询多语言消息列表
     *
     * @param qo 多语言消息查询参数对象
     * @return 多语言消息分页结果对象
     */
    PageResult<MulitMessageVO> messagePageList(MulitMessageQO qo);

    /**
     * 根据消息Key查询多语言消息列表
     *
     * @param messageKey 消息Key
     * @return 多语言消息视图对象列表
     */
    List<MulitMessageVO> messageListByKey(String messageKey);

    /**
     * 根据ID查询多语言消息详情
     *
     * @param id 多语言消息ID
     * @return 多语言消息视图对象，不存在时返回null
     */
    MulitMessageVO messageSelectById(String id);

    /**
     * 新增多语言消息
     *
     * @param data 多语言消息数据对象
     */
    void saveMessage(MultiMessageDTO data);

    /**
     * 修改多语言消息
     *
     * @param data 多语言消息数据对象
     */
    void updateMessage(MultiMessageDTO data);

    /**
     * 批量删除多语言消息
     *
     * @param ids 多语言消息ID列表
     */
    void deleteMessage(List<String> ids);
}
