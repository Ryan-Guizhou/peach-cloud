package com.peach.message.service.impl;

import lombok.RequiredArgsConstructor;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.PageResult;
import com.peach.common.constant.PubCommonConst;
import com.peach.common.response.Response;
import com.peach.common.util.DateUtil;
import com.peach.message.common.MessageCategoryConfig;
import com.peach.message.common.enums.MessageEnum;
import com.peach.message.dao.SiteMessageDao;
import com.peach.message.dto.MessagePublishDTO;
import com.peach.message.dto.MessageReadDTO;
import com.peach.message.dto.MessageRevokeDTO;
import com.peach.message.entity.SiteMessageDO;
import com.peach.message.qo.SiteMessageQO;
import com.peach.message.service.IMessageService;
import com.peach.message.service.IWebSocketPushService;
import com.peach.message.vo.SiteMessageVO;
import com.peach.satoken.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 14:45
 * @Description 消息服务实现
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements IMessageService {

        private final SiteMessageDao siteMessageDao;

        private final IWebSocketPushService webSocketPushService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response publish(MessagePublishDTO data) {
        MessageEnum.ReceiverType receiverType = data.getReceiverType() == null
                ? MessageEnum.ReceiverType.USER : data.getReceiverType();
        if (!MessageEnum.ReceiverType.USER.equals(receiverType)) {
            return Response.businessResponse("当前仅支持 USER 接收人类型");
        }
        if (data.getReceiverIds() == null || data.getReceiverIds().isEmpty()) {
            return Response.paramError("接收人ID列表不能为空");
        }
        boolean persistent = data.getPersistent() == null || data.getPersistent();
        boolean realtime = data.getRealtime() == null || data.getRealtime();
        List<SiteMessageDO> messages = new ArrayList<SiteMessageDO>();
        for (String receiverId : data.getReceiverIds()) {
            if (StringUtils.isNotBlank(receiverId) && persistent) {
                messages.add(buildSiteMessage(data, receiverId));
            }
        }
        if (!messages.isEmpty()) {
            siteMessageDao.batchInsert(messages);
        }
        if (realtime) {
            if (!messages.isEmpty()) {
                for (SiteMessageDO message : messages) {
                    pushMessageCreated(message.getReceiverId(), data, message);
                    pushUnreadCountChanged(message.getReceiverId(), null, message.getMessageType());
                }
            } else {
                for (String receiverId : data.getReceiverIds()) {
                    if (StringUtils.isNotBlank(receiverId)) {
                        pushMessageCreated(receiverId, data, null);
                    }
                }
            }
        }
        return Response.success(messages);
    }

    @Override
    public Response publishMessage(MessagePublishDTO data) {
        fillMessageCategory(data, MessageEnum.MessageCategory.MESSAGE, MessageEnum.MessageSourceType.MESSAGE);
        return publish(data);
    }

    @Override
    public Response publishAnnouncement(MessagePublishDTO data) {
        fillMessageCategory(data, MessageEnum.MessageCategory.ANNOUNCEMENT, MessageEnum.MessageSourceType.ANNOUNCEMENT);
        return publish(data);
    }

    @Override
    public Response publishTodo(MessagePublishDTO data) {
        fillMessageCategory(data, MessageEnum.MessageCategory.TODO, MessageEnum.MessageSourceType.TODO);
        return publish(data);
    }

    @Override
    public Response pageList(SiteMessageQO qo) {
        fillCurrentTenantOrg(qo);
        PageInfo<SiteMessageVO> pageInfo = PageHelper.startPage(qo.getPageNum(), qo.getPageSize())
                .doSelectPageInfo(() -> siteMessageDao.selectByQO(qo));
        return Response.success(new PageResult<SiteMessageVO>(pageInfo.getList(), pageInfo.getTotal()));
    }

    @Override
    public Response unreadCount(String receiverId) {
        return unreadCount(receiverId, null, null);
    }

    @Override
    public Response unreadCount(String receiverId, String messageCategory, String messageType) {
        if (StringUtils.isBlank(receiverId)) {
            return Response.paramError("接收人ID不能为空");
        }
        return Response.success(siteMessageDao.countUnread(receiverId, resolveMessageTypes(messageCategory), messageType));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response read(MessageReadDTO data) {
        SiteMessageVO db = siteMessageDao.selectById(data.getMessageId());
        if (db == null) {
            return Response.businessResponse("站内信不存在");
        }
        if (StringUtils.isNotBlank(data.getReceiverId()) && !data.getReceiverId().equals(db.getReceiverId())) {
            return Response.businessResponse("无权操作该站内信");
        }
        if (!PubCommonConst.LOGIC_TRUE.equals(db.getReadFlag())) {
            SiteMessageDO update = new SiteMessageDO();
            update.setId(data.getMessageId());
            update.setReadFlag(PubCommonConst.LOGIC_TRUE);
            update.setModifyTime(DateUtil.nowTime());
            siteMessageDao.updateById(update);
            pushUnreadCountChanged(db.getReceiverId(), null, db.getMessageType());
        }
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response readAll(String receiverId) {
        return readAll(receiverId, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response readAll(String receiverId, String messageCategory, String messageType) {
        if (StringUtils.isBlank(receiverId)) {
            return Response.paramError("接收人ID不能为空");
        }
        siteMessageDao.readAllByReceiver(receiverId, resolveMessageTypes(messageCategory), messageType, DateUtil.nowTime());
        pushUnreadCountChanged(receiverId, messageCategory, messageType);
        return Response.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response revoke(MessageRevokeDTO data) {
        siteMessageDao.revokeBySource(data.getSourceType(), data.getSourceCode());
        return Response.success();
    }

    @Override
    public Response revokeMessage(MessageRevokeDTO data) {
        data.setSourceType(MessageEnum.MessageSourceType.MESSAGE.getCode());
        return revoke(data);
    }

    @Override
    public Response revokeAnnouncement(MessageRevokeDTO data) {
        data.setSourceType(MessageEnum.MessageSourceType.ANNOUNCEMENT.getCode());
        return revoke(data);
    }

    @Override
    public Response revokeTodo(MessageRevokeDTO data) {
        data.setSourceType(MessageEnum.MessageSourceType.TODO.getCode());
        return revoke(data);
    }

    private void fillMessageCategory(MessagePublishDTO data,
                                     MessageEnum.MessageCategory messageCategory,
                                     MessageEnum.MessageSourceType sourceType) {
        if (StringUtils.isBlank(data.getSourceType())) {
            data.setSourceType(sourceType.getCode());
        }
        if (StringUtils.isBlank(data.getMessageType())) {
            List<String> types = MessageCategoryConfig.getTypes(messageCategory);
            if (types != null && !types.isEmpty()) {
                data.setMessageType(types.get(0));
            }
        }
    }

    private SiteMessageDO buildSiteMessage(MessagePublishDTO data, String receiverId) {
        SiteMessageDO message = new SiteMessageDO();
        message.setId(IDGeneratorUtil.UUID());
        message.setMessageCode(IDGeneratorUtil.UUID());
        message.setReceiverId(receiverId);
        message.setTitleMessageKey(StringUtils.defaultIfBlank(data.getTitleMessageKey(), data.getTitle()));
        message.setContentMessageKey(StringUtils.defaultIfBlank(data.getContentMessageKey(), data.getContent()));
        message.setMessageType(data.getMessageType());
        message.setSourceType(StringUtils.defaultIfBlank(data.getSourceType(), data.getBizType()));
        message.setSourceCode(StringUtils.defaultIfBlank(data.getSourceCode(), data.getBizId()));
        message.setReadFlag(PubCommonConst.LOGIC_FLASE);
        message.setSendStatus(MessageEnum.SendStatus.SENT.getCode());
        message.fillCreateTime();
        return message;
    }

    private void pushMessageCreated(String receiverId, MessagePublishDTO data, SiteMessageDO message) {
        Map<String, Object> payload = new HashMap<String, Object>();
        if (message != null) {
            payload.put("messageId", message.getId());
            payload.put("messageCode", message.getMessageCode());
        }
        payload.put("title", StringUtils.defaultIfBlank(data.getTitle(), data.getTitleMessageKey()));
        payload.put("content", StringUtils.defaultIfBlank(data.getContent(), data.getContentMessageKey()));
        payload.put("messageType", data.getMessageType());
        payload.put("bizType", data.getBizType());
        payload.put("bizId", data.getBizId());
        payload.put("url", data.getUrl());
        payload.put("extra", data.getExtra());
        webSocketPushService.pushToUser(receiverId, MessageEnum.WebSocketEventType.MESSAGE_CREATED.getCode(), payload);
    }

    private void pushUnreadCountChanged(String receiverId) {
        pushUnreadCountChanged(receiverId, null, null);
    }

    private void pushUnreadCountChanged(String receiverId, String messageCategory, String messageType) {
        Map<String, Object> payload = new HashMap<String, Object>();
        payload.put("messageCategory", messageCategory);
        payload.put("messageType", messageType);
        payload.put("count", siteMessageDao.countUnread(receiverId, resolveMessageTypes(messageCategory), messageType));
        webSocketPushService.pushToUser(receiverId, MessageEnum.WebSocketEventType.UNREAD_COUNT_CHANGED.getCode(), payload);
    }

    private void fillCurrentTenantOrg(SiteMessageQO qo) {
        qo.setTenantId(requireTenantId());
        qo.setOrgId(requireOrgId());
    }

    private String requireTenantId() {
        String tenantId = SecurityContextHolder.currentTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new IllegalStateException("Current tenant context is missing");
        }
        return tenantId;
    }

    private String requireOrgId() {
        String orgId = SecurityContextHolder.currentOrgId();
        if (StringUtils.isBlank(orgId)) {
            throw new IllegalStateException("Current organization context is missing");
        }
        return orgId;
    }

    private List<String> resolveMessageTypes(String messageCategory) {
        if (StringUtils.isBlank(messageCategory)) {
            return null;
        }
        try {
            return MessageCategoryConfig.getTypes(MessageEnum.MessageCategory.valueOf(messageCategory));
        } catch (Exception e) {
            return null;
        }
    }
}
