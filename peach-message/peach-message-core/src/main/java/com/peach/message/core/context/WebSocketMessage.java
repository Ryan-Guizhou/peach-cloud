package com.peach.message.core.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MsgType {
        BROADCAST_ALL,      // 广播给所有人
        BROADCAST_TYPE,     // 广播给指定业务类型
        UNICAST,            // 单播给指定用户
        UNICAST_BATCH,      // 批量单播
        CLOSE               // 关闭连接
    }

    private MsgType msgType;
    private String type;    // 业务类型
    private String sid;     // 用户ID
    private String content; // 消息内容
    private Map<String, String> batchContent; // 批量消息内容

    public static WebSocketMessage unicast(String type, String sid, String content) {
        return new WebSocketMessage(MsgType.UNICAST, type, sid, content, null);
    }

    public static WebSocketMessage broadcastType(String type, String content) {
        return new WebSocketMessage(MsgType.BROADCAST_TYPE, type, null, content, null);
    }
    
    public static WebSocketMessage broadcastAll(String content) {
        return new WebSocketMessage(MsgType.BROADCAST_ALL, null, null, content, null);
    }
    
    public static WebSocketMessage close(String type, String sid) {
        return new WebSocketMessage(MsgType.CLOSE, type, sid, null, null);
    }
}
