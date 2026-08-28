package com.peach.captcha.util;


import com.peach.captcha.model.PointVO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Json工具类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 18:10
 */
@Slf4j
public final class JsonUtil {

    private JsonUtil() {
        throw new IllegalStateException("Utility class");
    }
    /**
     * 解析json数组字符串为PointVO列表.
     * @param text json数组字符串
     * @param clazz 目标类型
     * @return PointVO列表
     */
    public static List<PointVO> parseArray(String text) {
        if (text == null) {
            return List.of();
        } else {
            String[] arr = text.replaceFirst("\\[","")
                    .replaceFirst("\\]","").split("\\}");
            List<PointVO> ret = new ArrayList<>(arr.length);
            for (String s : arr) {
                ret.add(parseObject(s, PointVO.class));
            }
            return ret;
        }
    }

    /**
     * 解析json字符串为PointVO.
     * @param text json字符串
     * @param clazz 目标类型
     * @return PointVO
     */
    public static PointVO parseObject(String text, @SuppressWarnings("unused") Class<PointVO> clazz) {
        if (text == null) {
            return null;
        }
        try {
            return PointVO.parseJson(text);
        } catch (Exception ex) {
            log.error("JSON parsing failed, text={}", text, ex);
            return null;
        }
    }

    public static String toJsonString(Object object) {
        if(object == null) {
            return "{}";
        }
        if (object instanceof PointVO pointVO) {
            return pointVO.toJsonString();
        }
        if (object instanceof List<?> list) {
            StringBuilder buf = new StringBuilder("[");
            list.forEach(item -> {
                if (item instanceof PointVO pointVO) {
                    buf.append(pointVO.toJsonString()).append(",");
                }
            });
            return buf.deleteCharAt(buf.lastIndexOf(",")).append("]").toString();
        }
        if (object instanceof Map<?, ?> map) {
            return map.entrySet().toString();
        }
        throw new UnsupportedOperationException("不支持的输入类型:" +object.getClass().getSimpleName());
    }
}
