package com.peach.setting.service;

import com.peach.common.PageResult;
import com.peach.setting.dto.NoticeDTO;
import com.peach.setting.dto.NoticePublishDTO;
import com.peach.setting.qo.NoticeQO;
import com.peach.setting.qo.SiteMessageQO;
import com.peach.setting.vo.NoticeVO;
import com.peach.setting.vo.SiteMessageVO;

import java.util.List;

/**
 * 公告服务接口。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:50
 * @Description 公告服务接口
 */
public interface INoticeService {

    /**
     * 分页查询公告列表
     *
     * @param qo 公告查询参数对象
     * @return 公告分页结果对象
     */
    PageResult<NoticeVO> noticePageList(NoticeQO qo);

    /**
     * 根据ID查询公告详情
     *
     * @param id 公告ID
     * @return 公告视图对象，不存在时返回null
     */
    NoticeVO noticeSelectById(String id);

    /**
     * 新增公告
     *
     * @param data 公告数据对象
     */
    void saveNotice(NoticeDTO data);

    /**
     * 修改公告
     *
     * @param data 公告数据对象
     */
    void updateNotice(NoticeDTO data);

    /**
     * 批量删除公告
     *
     * @param ids 公告ID列表
     */
    void deleteNotice(List<String> ids);

    /**
     * 发布公告
     *
     * @param data 公告发布数据对象
     */
    void publishNotice(NoticePublishDTO data);

    /**
     * 撤销公告
     *
     * @param id 公告ID
     */
    void revokeNotice(String id);

    /**
     * 标记公告为已读
     *
     * @param noticeCode 公告编码
     * @param userId 用户ID
     */
    void markNoticeRead(String noticeCode, String userId);

    /**
     * 分页查询站内信列表
     *
     * @param qo 站内信查询参数对象
     * @return 站内信分页结果对象
     */
    PageResult<SiteMessageVO> siteMessagePageList(SiteMessageQO qo);

    /**
     * 标记站内信为已读
     *
     * @param id 站内信ID
     */
    void markSiteMessageRead(String id);
}
