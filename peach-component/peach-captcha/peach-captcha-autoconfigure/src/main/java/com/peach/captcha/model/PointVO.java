package com.peach.captcha.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Point视图对象。
 *
 * @param secretKey 验证码密钥
 * @param x 横坐标
 * @param y 纵坐标
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 11:06
 */
@Schema(description = "验证码坐标视图对象")
public record PointVO(
        @Schema(description = "验证码密钥") String secretKey,
        @Schema(description = "横坐标") int x,
        @Schema(description = "纵坐标") int y) implements Serializable {

    @Serial
    private static final long serialVersionUID = -2118960679939189710L;

    public PointVO(int x, int y) {
        this(null, x, y);
    }

    public String toJsonString() {
        return String.format("{\"secretKey\":\"%s\",\"x\":%d,\"y\":%d}", secretKey, x, y);
    }

    public static PointVO parseJson(String jsonStr) {
        Map<String, Object> m = HashMap.newHashMap(64);
        Arrays.stream(jsonStr
                .replaceFirst(",\\{", "\\{")
                .replaceFirst("\\{", "")
                .replaceFirst("\\}", "")
                .replaceAll("\"", "")
                .split(",")).forEach(item -> {
            String[] parts = item.split(":");
            m.put(parts[0], parts.length > 1 ? parts[1] : "");
        });
        int parsedX = Double.valueOf(String.valueOf(m.getOrDefault("x", "0"))).intValue();
        int parsedY = Double.valueOf(String.valueOf(m.getOrDefault("y", "0"))).intValue();
        String parsedSecretKey = String.valueOf(m.getOrDefault("secretKey", ""));
        return new PointVO(parsedSecretKey, parsedX, parsedY);
    }
}
