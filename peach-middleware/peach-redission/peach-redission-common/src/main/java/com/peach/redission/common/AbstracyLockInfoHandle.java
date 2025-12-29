package com.peach.redission.common;

import org.aspectj.lang.JoinPoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/22 10:18
 */
public abstract class AbstracyLockInfoHandle implements LockInfoHandle{

    private static final String LOCK_PREFIX_NAME = "LOCK_DISTRIBUTE_ID";

    @Override
    public String getLockName(JoinPoint joinPoint, String name, String[] keys) {
        return getLockPrefixName() + ":" + name + ":" +getRealKey(joinPoint, keys);
    }

    @Override
    public String getAssemblyLockName(String name, String[] keys) {
        List<String> keyList = Arrays.stream(keys).collect(Collectors.toList());
        return LOCK_PREFIX_NAME + ":" + name + ":" + String.join("-", keyList);
    }

    private String getRealKey(JoinPoint joinPoint,String[] keys){
        List<String> keyList = new ArrayList<>();
        for (String key : keys) {
            String content = getContent(joinPoint.getArgs(), key);
            keyList.add(content);
        }
        return Optional.ofNullable(keyList)
                .map(list -> list.stream().collect(Collectors.joining("-")))
                .orElseThrow(() -> new RuntimeException("key is null"));
    }

    private String getContent(Object[] objects,String content){
        SpelParse spelParse = SpelParse.create();
        for (int i = 0; i < objects.length; i++) {
            spelParse.setVariable("p" + i, objects[i]);
        }
        return spelParse.parseExpression(content);
    }

    public abstract String getLockPrefixName();
}
