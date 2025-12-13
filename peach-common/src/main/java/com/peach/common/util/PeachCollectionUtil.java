package com.peach.common.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @Description //借用spring的集合工具类的便利，补全一下反向判断
 * @CreateTime 2024/10/14 15:51
 */
public final class PeachCollectionUtil extends CollectionUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PeachCollectionUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    public static Map map(Object object) {
        return map(object, false);
    }

    public static Map map(Object object,boolean isRemoveEmpty) {
        try {
            Map map;
            if (object instanceof LinkedHashMap){
                String json = JSONObject.toJSONString(object);
                map = (Map) JSONObject.parse(json);
            }else{
                map = PropertyUtils.describe(object);
            }
            map.remove("class");
            if (isRemoveEmpty) removeEmpty(map);
            return map;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * 移除map中为空的值
     * @param map
     */
    private static void removeEmpty( Map map ) {
        Iterator<Map.Entry> itera = map.entrySet().iterator();
        Map.Entry entry;
        while (itera.hasNext()) {
            entry = itera.next();
            if (Objects.isNull(entry.getValue()) || StringUtil.isEmpty(entry.getValue().toString())) {
                itera.remove();
            }
        }
    }

    /**
     * 汇总所有传入集合，生成一个汇总集合
     * @param lists
     * @return
     */
    public static List collectAll(List ... lists){
        List res = Lists.newArrayList();
        for (List list : lists) {
            if (isNotEmpty(list)){
                res.addAll(list);
            }
        }
        return res;
    }

    /**
     * 汇总所有传入集合，生成一个汇总集合
     * @param lists
     * @return
     */
    public static List collectAll(List<List<Object>> lists){
        List res = Lists.newArrayList();
        for (List list : lists) {
            if (isNotEmpty(list)) res.addAll(list);
        }
        return res;
    }

    /**
     * List<Object> 转 List<Map>
     * @param objects
     * @return
     */
    public static List<Map> map( List<Object> objects){
        List<Map> res = Lists.newArrayList();
        for (Object o : objects) {
            Map obj = map(o);
            res.add(obj);
        }
        return res;
    }

    public static Map mapKeyToUpper(Map obj){
        return mapKeyToUpper(obj, false);
    }
    public static Map mapKeyToUpper(Map obj,boolean isToLine){
        Map res = Maps.newHashMap();
        obj.forEach(( key, value ) -> {
            String upperKey = key.toString();
            if (isToLine) upperKey = StringUtil.humpToLine(upperKey);
            upperKey = upperKey.toUpperCase();
            res.put(upperKey, value);
        });
        return res;
    }

    public static List<Map> mapKeyToUpper( List<Object> objects){
        return mapKeyToUpper(objects, false);
    }

    public static List<Map> mapKeyToUpper( List<Object> objects,boolean isToLine){
        List<Map> res = Lists.newArrayList();
        objects.forEach(o -> {
            Map obj = mapKeyToUpper(map(o),isToLine);
            res.add(obj);
        });
        return res;
    }

    public static Map mapKeyToLower( Object obj,boolean isToHump ) {
        return mapKeyToLower(map(obj),isToHump);
    }

    public static Map mapKeyToLower(Map obj,boolean isToHump ){
        Map res = Maps.newHashMap();
        obj.forEach(( key, value ) -> {
            String finalKey = key.toString().toLowerCase();
            if (isToHump) {
                finalKey = StringUtil.lineToHump(finalKey);
            }
            res.put(finalKey, value);
        });
        return res;
    }

    public static Map mapKeyToLowerContainsLine(Map obj,boolean isToHump ){
        Map res = Maps.newHashMap();
        obj.forEach(( key, value ) -> {
            String finalKey = key.toString();
            if (finalKey.contains("_")) {
                finalKey=finalKey.toLowerCase();
                if (isToHump) {
                    finalKey = StringUtil.lineToHump(finalKey);
                }
            }
            res.put(finalKey, value);
        });
        return res;
    }

    public static List<Map> mapKeyToLower( List<Object> objects,boolean isToHump ){
        List<Map> res = Lists.newArrayList();
        objects.forEach(o -> {
            Map obj = mapKeyToLower(map(o),isToHump);
            res.add(obj);
        });
        return res;
    }

    /**
     * 将JSON字符传转为Map
     * @param printPrjContent
     * @return
     */
    public static Map<String, Object> transJsonToMap( String printPrjContent ) {
        Map tempPrj = JSON.parseObject(printPrjContent);
        Map<String, Object> res = Maps.newHashMap();
        tempPrj.forEach(( o, o2 ) -> {
            res.put(o.toString(), o2);
        });
        return res;
    }
    /**
     * orgObjs 批量创建指定类型的新对象
     * @param orgObjs
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> List<T>  convertToObject(List orgObjs,Class<T> tClass){
        List<T> targets = Lists.newArrayList();
        orgObjs.forEach(o -> {
            T tarObj = MAPPER.convertValue(o, tClass);
            targets.add(tarObj);
        });
        return targets;
    }

    /**
     * 利用对象object 创建指定类型的新对象
     * @param object
     * @param tClass
     * @param <T>
     * @return
     */
    public static <T> T  convertToObject(Object object,Class<T> tClass){
        T tarObj = MAPPER.convertValue(object, tClass);
        return tarObj;
    }

    /**
     * 字符串排序（适用于代码按级次有规律的：参考会计科目代码）
     * @param codes
     * @return
     */
    public static List<String> sortCodes(List<String> codes) {
        codes.sort(( o1, o2 ) -> {
            return Integer.compare(o1.compareTo(o2), 0);
        });
        return codes;
    }

    /**
     * 递归寻找第一个末级节点
     * @param srcCode 原始传入的节点代码
     * @param workCode 当前已经匹配到的节点代码（逐步向后移动，直到找到第一个末级节点）
     * @param sortedCodes 已经排序后的代码集合
     * @return
     */
    private static String getFirstLeafCode( String srcCode,String workCode, List<String> sortedCodes ) {
        String resCode = workCode;
        //过滤当前工作节点代码的下级
        List<String> resCodes = sortedCodes.stream().filter(s -> s.startsWith(workCode) && !s.equals(srcCode) && !s.equals(workCode)).collect(Collectors.toList());
        //如果存在下级就继续匹配
        if (PeachCollectionUtil.isNotEmpty(resCodes)) {
            //暂时将第一个节点作为结果
            resCode = resCodes.stream().findFirst().get();
            //如果存在多个节点递归继续匹配
            if (resCodes.size() > 1) {
                resCode = getFirstLeafCode(srcCode,resCode, sortedCodes);
            }
        }
        return resCode;
    }

    public static String getFirstLeafCode( String srcCode, List<String> codes ) {
        List<String> sortCodes = sortCodes(codes);
        return getFirstLeafCode(srcCode,srcCode, sortCodes);
    }

    /**
     * 判断标准几个{@parameter srcData} 是否包含 目标集合{@parameter targetData}中的任何一个
     * @param srcData 标准集合
     * @param targetData 待比较集合
     * @return
     */
    public static boolean containsAny(List<String> srcData,List<String> targetData) {
        boolean isMatch = false;
        for (String s : targetData) {
            if (srcData.contains(s)) {
                isMatch = true;
                break;
            }
        }
        return isMatch;
    }

    /**
     * 对list 进行分片
     * @param <T>
     * @param list
     * @param partitionCount
     * @return
     */
    public static <T> List<List<T>> partition(List<T> list, int partitionCount) {
        if (CollectionUtils.isEmpty(list)) {
            return com.google.common.collect.Lists.newArrayList();
        }
        return com.google.common.collect.Lists.partition(list, partitionCount);
    }


}
