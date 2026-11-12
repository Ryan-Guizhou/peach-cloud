package com.peach.redis.tool.quickstart.enums;

import com.peach.common.key.KeyDefinition;

/**
 * 工具示例的完整 Key 定义；运行标识隔离并行示例，业务参数不得包含敏感数据。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/12 14:09
 */
public enum RedisToolKey implements KeyDefinition {

    SESSION("peach:quickstart:tool:{0}:session:{1}", "会话", "示例会话内容"),
    PROFILE("peach:quickstart:tool:{0}:profile:{1}", "用户资料", "资料字段与值"),
    TAGS("peach:quickstart:tool:{0}:tags:{1}", "用户标签", "标签集合"),
    VALUE("peach:quickstart:tool:{0}:value:{1}", "字符串操作", "字符串或整数"),
    HASH("peach:quickstart:tool:{0}:hash:{1}", "哈希操作", "字段与数字"),
    LIST("peach:quickstart:tool:{0}:list:{1}", "列表操作", "有序元素"),
    SET("peach:quickstart:tool:{0}:set:{1}", "集合操作", "不重复成员"),
    ZSET("peach:quickstart:tool:{0}:zset:{1}", "排行榜操作", "成员及分值"),
    DELETE("peach:quickstart:tool:{0}:delete:{1}", "删除操作", "待删除示例值"),
    DELETE_PATTERN("peach:quickstart:tool:{0}:delete:*", "模式删除范围", "仅匹配当前运行删除示例"),
    CHANNEL("peach:quickstart:tool:{0}:channel:{1}", "发布订阅通道", "临时演示消息，不持久化");

    private final String pattern;

    private final String moduleCode;

    private final String keyIntroduce;

    private final String valueIntroduce;

    private final String author;

    RedisToolKey(String pattern, String keyIntroduce, String valueIntroduce) {
        this.pattern = pattern;
        this.moduleCode = "QUICKSTART-TOOL";
        this.keyIntroduce = keyIntroduce;
        this.valueIntroduce = valueIntroduce;
        this.author = "Mr Shu";
    }

    @Override
    public String moduleCode() {
        return moduleCode;
    }

    @Override
    public String keyIntroduce() {
        return keyIntroduce;
    }

    @Override
    public String valueIntroduce() {
        return valueIntroduce;
    }

    @Override
    public String author() {
        return author;
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
