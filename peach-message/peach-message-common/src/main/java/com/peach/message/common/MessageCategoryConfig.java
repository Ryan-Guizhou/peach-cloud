package com.peach.message.common;

import com.peach.message.common.enums.MessageEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/23 15:26
 * @Description 消息大类与小类映射配置
 */
public class MessageCategoryConfig {

    private static final List<String> MESSAGE_TYPES = Collections.unmodifiableList(Arrays.asList(
            MessageEnum.MessageType.SYSTEM.getCode(),
            MessageEnum.MessageType.NOTICE.getCode(),
            MessageEnum.MessageType.CUSTOM.getCode()
    ));

    private static final List<String> ANNOUNCEMENT_TYPES = Collections.unmodifiableList(Arrays.asList(
            "INFO",
            "WARNING",
            "MAINTENANCE",
            "PROMOTION"
    ));

    private static final List<String> TODO_TYPES = Collections.unmodifiableList(Arrays.asList(
            MessageEnum.MessageType.APPROVAL.getCode(),
            MessageEnum.MessageType.TASK.getCode()
    ));

    private MessageCategoryConfig() {
    }

    public static List<String> getTypes(MessageEnum.MessageCategory category) {
        if (MessageEnum.MessageCategory.MESSAGE.equals(category)) {
            return MESSAGE_TYPES;
        }
        if (MessageEnum.MessageCategory.ANNOUNCEMENT.equals(category)) {
            return ANNOUNCEMENT_TYPES;
        }
        if (MessageEnum.MessageCategory.TODO.equals(category)) {
            return TODO_TYPES;
        }
        return List.of();
    }
}
