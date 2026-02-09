package com.peach.auth.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 13:46
 */
public class FilterPathUtil {

    /**
     * 检测健康接口 / Detect health interface
     */
    public static  final String CHECK_HEALTH_PATH = "/user/health";


    public static List<String> EXCLUDE_PATH = new ArrayList<>();


    static {
        //
        EXCLUDE_PATH.add("/user/login");
        EXCLUDE_PATH.add("/user/register");
        EXCLUDE_PATH.add("/user/logout");
    }
}
