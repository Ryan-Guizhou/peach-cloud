package com.peach.message.service;

import com.peach.common.response.Response;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageReadDTO;
import com.peach.message.dto.MessageRevokeDTO;
import com.peach.message.qo.SiteMessageQO;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务接口
 */
public interface IMessageService {

    /**
     * 发布消息
     *
     * @param data 消息发布DTO
     * @return 响应结果
     */
    Response publish(MessagePublishDTO data);

    /**
     * 发布普通消息
     *
     * @param data 消息发布DTO
     * @return 响应结果
     */
    Response publishMessage(MessagePublishDTO data);

    /**
     * 发布公告
     *
     * @param data 消息发布DTO
     * @return 响应结果
     */
    Response publishAnnouncement(MessagePublishDTO data);

    /**
     * 发布待办
     *
     * @param data 消息发布DTO
     * @return 响应结果
     */
    Response publishTodo(MessagePublishDTO data);

    /**
     * 分页查询站内信
     *
     * @param qo 查询对象
     * @return 响应结果
     */
    Response pageList(SiteMessageQO qo);

    /**
     * 查询未读数
     *
     * @param receiverId 接收人ID
     * @return 响应结果
     */
    Response unreadCount(String receiverId);

    /**
     * 按分类查询未读数
     *
     * @param receiverId 接收人ID
     * @param messageCategory 消息大类
     * @param messageType 消息小类
     * @return 响应结果
     */
    Response unreadCount(String receiverId, String messageCategory, String messageType);

    /**
     * 标记消息已读
     *
     * @param data 已读DTO
     * @return 响应结果
     */
    Response read(MessageReadDTO data);

    /**
     * 全部标记已读
     *
     * @param receiverId 接收人ID
     * @return 响应结果
     */
    Response readAll(String receiverId);

    /**
     * 按分类全部标记已读
     *
     * @param receiverId 接收人ID
     * @param messageCategory 消息大类
     * @param messageType 消息小类
     * @return 响应结果
     */
    Response readAll(String receiverId, String messageCategory, String messageType);

    /**
     * 按来源撤销消息
     *
     * @param data 撤销DTO
     * @return 响应结果
     */
    Response revoke(MessageRevokeDTO data);

    /**
     * 撤销普通消息
     *
     * @param data 撤销DTO
     * @return 响应结果
     */
    Response revokeMessage(MessageRevokeDTO data);

    /**
     * 撤销公告
     *
     * @param data 撤销DTO
     * @return 响应结果
     */
    Response revokeAnnouncement(MessageRevokeDTO data);

    /**
     * 撤销待办
     *
     * @param data 撤销DTO
     * @return 响应结果
     */
    Response revokeTodo(MessageRevokeDTO data);
}
