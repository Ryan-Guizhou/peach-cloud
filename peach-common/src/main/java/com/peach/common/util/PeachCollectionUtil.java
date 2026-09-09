package com.peach.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.peach.common.constant.PubCommonConst;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 集合与对象转换工具类（基于 JDK 21 重构优化）
 * <p>
 * 提供集合空值校验、对象与 Map 相互转换、键名大小写/驼峰转换、代码级次树查找等常用工具方法。
 * </p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/10/14 15:51
 */
public final class PeachCollectionUtil {

    /**
     * 统一使用 Jackson ObjectMapper 处理对象与 Map/JSON 的转换，配置忽略未知属性
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private PeachCollectionUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 判断集合是否为空（null 或无元素）
     *
     * @param collection 待检查的集合，支持 null
     * @return 若集合为 null 或不包含任何元素返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * 判断集合是否非空
     *
     * @param collection 待检查的集合，支持 null
     * @return 若集合不为 null 且包含元素返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * 判断 Map 是否为空（null 或无键值对）
     *
     * @param map 待检查的 Map，支持 null
     * @return 若 Map 为 null 或不包含任何 entry 返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否非空
     *
     * @param map 待检查的 Map，支持 null
     * @return 若 Map 不为 null 且包含键值对返回 {@code true}，否则返回 {@code false}
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    /**
     * 将 JavaBean 或对象转换为 Map，默认保留空值键值对
     *
     * @param object 待转换的对象，支持 null
     * @return 转换后的 Map（包含 Key-Value 映射），若对象为 null 则返回空 Map
     */
    public static Map<String, Object> map(Object object) {
        return map(object, false);
    }

    /**
     * 将 JavaBean 或对象转换为 Map，可选择是否移除空值 Entry
     *
     * @param object        待转换的对象
     * @param isRemoveEmpty 是否过滤 null 或空字符串的元素
     * @return 转换后的 Map；若入参为 null 返回空 Map
     */
    public static Map<String, Object> map(Object object, boolean isRemoveEmpty) {
        if (object == null) {
            return new HashMap<>();
        }

        Map<String, Object> map;
        // 使用 JDK 16+ 模式匹配 (Pattern Matching for instanceof)
        if (object instanceof Map<?, ?> m) {
            map = m.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> String.valueOf(e.getKey()),
                            Map.Entry::getValue,
                            (v1, v2) -> v2,
                            LinkedHashMap::new
                    ));
        } else {
            // 使用 Jackson 替代 BeanUtils.describe，提高类型安全性与性能
            map = MAPPER.convertValue(object, new TypeReference<LinkedHashMap<String, Object>>() {});
        }

        map.remove("class");

        if (isRemoveEmpty) {
            removeEmpty(map);
        }
        return map;
    }

    /**
     * 移除 Map 中 Key 对应 Value 为 null 或空白字符串的键值对
     *
     * @param map 待清理的 Map
     */
    private static void removeEmpty(Map<String, Object> map) {
        map.entrySet().removeIf(entry ->
                Objects.isNull(entry.getValue()) ||
                        (entry.getValue() instanceof String str && str.trim().isEmpty())
        );
    }

    /**
     * 汇总所有传入的多个同类型 List，生成一个新的平铺集合
     *
     * @param lists 变长参数 List 列表
     * @param <T>   元素类型
     * @return 汇总后的新 List 集合
     */
    @SafeVarargs
    public static <T> List<T> collectAll(List<T>... lists) {
        if (lists == null || lists.length == 0) {
            return new ArrayList<>();
        }
        List<T> res = new ArrayList<>();
        for (List<T> list : lists) {
            if (isNotEmpty(list)) {
                res.addAll(list);
            }
        }
        return res;
    }

    /**
     * 汇总嵌套列表集合（将 List 的 List 平铺开）
     *
     * @param lists 嵌套的 List 集合
     * @param <T>   元素类型
     * @return 平铺合并后的新 List 集合
     */
    public static <T> List<T> collectAll(Collection<List<T>> lists) {
        if (isEmpty(lists)) {
            return new ArrayList<>();
        }
        return lists.stream()
                .filter(PeachCollectionUtil::isNotEmpty)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    /**
     * 批量将 List 中的对象转换为 List&lt;Map&lt;String, Object&gt;&gt;
     *
     * @param objects 任意对象列表
     * @return 转换后的 Map 列表
     */
    public static List<Map<String, Object>> mapList(List<?> objects) {
        if (isEmpty(objects)) {
            return new ArrayList<>();
        }
        return objects.stream()
                .map(PeachCollectionUtil::map)
                .collect(Collectors.toList());
    }

    /**
     * 将 Map 的 Key 全部转为大写，默认不转换下划线
     *
     * @param obj 原 Map 对象
     * @return Key 转为大写后的新 Map
     */
    public static Map<String, Object> mapKeyToUpper(Map<String, Object> obj) {
        return mapKeyToUpper(obj, false);
    }

    /**
     * 将 Map 的 Key 全部转为大写，支持驼峰转下划线后转大写
     *
     * @param obj      原 Map 对象
     * @param isToLine 是否将驼峰转为下划线格式
     * @return 转换后的新 Map
     */
    public static Map<String, Object> mapKeyToUpper(Map<String, Object> obj, boolean isToLine) {
        if (isEmpty(obj)) {
            return new HashMap<>();
        }
        Map<String, Object> res = HashMap.newHashMap(obj.size());
        obj.forEach((key, value) -> {
            String upperKey = key;
            if (isToLine) {
                upperKey = StringUtil.humpToLine(upperKey);
            }
            res.put(upperKey.toUpperCase(), value);
        });
        return res;
    }

    /**
     * 批量将对象列表转换并转 Key 为大写
     *
     * @param objects 对象列表
     * @return 转换后的 Map 列表
     */
    public static List<Map<String, Object>> mapKeyToUpper(List<?> objects) {
        return mapKeyToUpper(objects, false);
    }

    /**
     * 批量将对象列表转换并转 Key 为大写（可选驼峰转下划线）
     *
     * @param objects  对象列表
     * @param isToLine 是否将驼峰转为下划线格式
     * @return 转换后的 Map 列表
     */
    public static List<Map<String, Object>> mapKeyToUpper(List<?> objects, boolean isToLine) {
        if (isEmpty(objects)) {
            return new ArrayList<>();
        }
        return objects.stream()
                .map(o -> mapKeyToUpper(map(o), isToLine))
                .collect(Collectors.toList());
    }

    /**
     * 将对象转成 Map 并将 Key 转为小写
     *
     * @param obj      待转换的对象
     * @param isToHump 是否将下划线格式转为驼峰格式
     * @return 转换后的新 Map
     */
    public static Map<String, Object> mapKeyToLower(Object obj, boolean isToHump) {
        return mapKeyToLower(map(obj), isToHump);
    }

    /**
     * 将 Map 的 Key 全部转为小写
     *
     * @param obj      原 Map
     * @param isToHump 是否将下划线转为驼峰命名
     * @return 转换 Key 后的新 Map
     */
    public static Map<String, Object> mapKeyToLower(Map<String, Object> obj, boolean isToHump) {
        if (isEmpty(obj)) {
            return new HashMap<>();
        }
        Map<String, Object> res = HashMap.newHashMap(obj.size());
        obj.forEach((key, value) -> {
            String finalKey = key.toLowerCase();
            if (isToHump) {
                finalKey = StringUtil.lineToHump(finalKey);
            }
            res.put(finalKey, value);
        });
        return res;
    }

    /**
     * 仅当 Key 中包含下划线时，将其小写并转换为驼峰命名
     *
     * @param obj      原 Map
     * @param isToHump 是否进行下划线转驼峰格式转换
     * @return 处理后的 Map
     */
    public static Map<String, Object> mapKeyToLowerContainsLine(Map<String, Object> obj, boolean isToHump) {
        if (isEmpty(obj)) {
            return new HashMap<>();
        }
        Map<String, Object> res = HashMap.newHashMap(obj.size());
        obj.forEach((key, value) -> {
            String finalKey = key;
            if (finalKey.contains(PubCommonConst.UNDER_LINE)) {
                finalKey = finalKey.toLowerCase();
                if (isToHump) {
                    finalKey = StringUtil.lineToHump(finalKey);
                }
            }
            res.put(finalKey, value);
        });
        return res;
    }

    /**
     * 批量将对象列表转换并转 Key 为小写
     *
     * @param objects  对象列表
     * @param isToHump 是否将下划线格式转为驼峰格式
     * @return 转换后的 Map 列表
     */
    public static List<Map<String, Object>> mapKeyToLower(List<?> objects, boolean isToHump) {
        if (isEmpty(objects)) {
            return new ArrayList<>();
        }
        return objects.stream()
                .map(o -> mapKeyToLower(map(o), isToHump))
                .collect(Collectors.toList());
    }

    /**
     * 将 JSON 字符串解析转为 Map&lt;String, Object&gt;
     *
     * @param jsonContent JSON 格式字符串
     * @return 解析后的 Map，若输入为空字符串则返回空 Map
     * @throws IllegalArgumentException 当 JSON 文本解析失败时抛出异常
     */
    public static Map<String, Object> transJsonToMap(String jsonContent) {
        if (StringUtil.isEmpty(jsonContent)) {
            return new HashMap<>();
        }
        try {
            return MAPPER.readValue(jsonContent, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON string", e);
        }
    }

    /**
     * 将对象列表批量转换为指定类型的对象列表
     *
     * @param orgObjs 原始对象列表
     * @param tClass  目标对象的 Class 类型
     * @param <T>     目标类型泛型
     * @return 转换后的目标对象 List 集合
     */
    public static <T> List<T> convertToObject(List<?> orgObjs, Class<T> tClass) {
        if (isEmpty(orgObjs)) {
            return new ArrayList<>();
        }
        return orgObjs.stream()
                .map(o -> MAPPER.convertValue(o, tClass))
                .collect(Collectors.toList());
    }

    /**
     * 利用指定对象创建并转换为指定类型的新对象
     *
     * @param object 原始对象
     * @param tClass 目标对象的 Class 类型
     * @param <T>    目标类型泛型
     * @return 转换后的目标对象，若源对象为 null 则返回 null
     */
    public static <T> T convertToObject(Object object, Class<T> tClass) {
        if (object == null) {
            return null;
        }
        return MAPPER.convertValue(object, tClass);
    }

    /**
     * 字符串代码排序（适用于规则级次代码，如会计科目代码 "1001", "100101"）
     *
     * @param codes 待排序的代码 List
     * @return 排序后的代码 List
     */
    public static List<String> sortCodes(List<String> codes) {
        if (isEmpty(codes)) {
            return new ArrayList<>();
        }
        codes.sort(Comparator.naturalOrder());
        return codes;
    }

    /**
     * 递归寻找代码级次中的第一个末级（叶子）节点代码
     *
     * @param srcCode     原始传入的起点代码
     * @param workCode    当前正在匹配推进的工作节点代码
     * @param sortedCodes 已按字典升序排好序的代码列表
     * @return 第一个匹配到的叶子节点代码
     */
    private static String getFirstLeafCode(String srcCode, String workCode, List<String> sortedCodes) {
        // 过滤出当前工作节点代码的直接或间接下级节点
        List<String> resCodes = sortedCodes.stream()
                .filter(s -> s.startsWith(workCode) && !s.equals(srcCode) && !s.equals(workCode))
                .toList();

        if (isNotEmpty(resCodes)) {
            // 利用 JDK 21 SequencedCollection 特性 getFirst() 取首元素
            String resCode = resCodes.getFirst();
            if (resCodes.size() > 1) {
                return getFirstLeafCode(srcCode, resCode, sortedCodes);
            }
            return resCode;
        }
        return workCode;
    }

    /**
     * 寻找传入节点代码在指定集合中的第一个末级（叶子）节点代码
     *
     * @param srcCode 待匹配的根代码
     * @param codes   全量代码列表
     * @return 第一个末级节点代码
     */
    public static String getFirstLeafCode(String srcCode, List<String> codes) {
        List<String> sortedCodes = sortCodes(new ArrayList<>(codes));
        return getFirstLeafCode(srcCode, srcCode, sortedCodes);
    }

    /**
     * 判断标准集合 {@code srcData} 是否包含目标集合 {@code targetData} 中的任意一个元素
     *
     * @param srcData    标准源集合
     * @param targetData 待校验的比对集合
     * @return 若存在任意交集元素返回 {@code true}，否则返回 {@code false}
     */
    public static boolean containsAny(Collection<?> srcData, Collection<?> targetData) {
        if (isEmpty(srcData) || isEmpty(targetData)) {
            return false;
        }
        // 利用 Collections.disjoint 判断是否存在交集 (无交集返回 true，故取反)
        return !Collections.disjoint(srcData, targetData);
    }

    /**
     * 将指定 List 集合按固定的子集合大小进行分片处理
     *
     * @param list          待分片的 List
     * @param partitionSize 每个子 List 的最大容量大小
     * @param <T>           元素类型
     * @return 分片后的 List 嵌套列表
     */
    public static <T> List<List<T>> partition(List<T> list, int partitionSize) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return Lists.partition(list, partitionSize);
    }
}
