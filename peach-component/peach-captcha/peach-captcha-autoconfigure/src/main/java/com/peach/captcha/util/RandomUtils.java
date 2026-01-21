package com.peach.captcha.util;

import com.peach.common.constant.PubCommonConst;
import lombok.extern.slf4j.Slf4j;

import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 13:50
 * @Description 随机工具类 / Random tool class
 */
@Slf4j
public final class RandomUtils {

    /**
     * 随机数字符串 / Random number string
     */
    private static final String NUMBER_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";


    private RandomUtils(){
        throw new IllegalStateException("Utility class");
    }

    /**
     * 生成UUID
     *
     */
    public static String getUuid() {
        String uuid = UUID.randomUUID().toString();
        uuid = uuid.replace("-", "");
        return uuid;
    }

    /**
     * 获取一个随机数 / Get a random number
     * @param startNum 开始数字 / Start number
     * @param endNum 结束数字 / End number
     * @return  int 随机数 / Random number
     */
    public static int getRandomInt(int startNum, int endNum) {
        if (startNum >= endNum) {
            throw new IllegalArgumentException("startNum must be less than endNum");
        }
        return ThreadLocalRandom.current().nextInt(startNum, endNum);
    }

    /**
     * 获取一个随机数 / Get a random number
     * @param num 随机数范围 / Random number range
     * @return  int 随机数 / Random number
     */
    public static int getRandomInt(int num) {
        if (num <= 0) {
            throw new IllegalArgumentException("num must be greater than 0");
        }
        return ThreadLocalRandom.current().nextInt(num);
    }


    /**
     * 获取一个随机字符串(a-zA-z0-9) / Get a random string
     * @param length 字符串长度 / String length
     * @return  String 随机字符串 / Random string
     */
    public static String getRandomString(int length){
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i<length; i++){
            sb.append(NUMBER_STRING.charAt(r.nextInt(NUMBER_STRING.length())));
        }
        return sb.toString();
    }

    /**
     * 获取一个随机汉字 / Get a random Chinese character
     * @return  String 随机汉字 / Random Chinese character
     */
    public static String getRandomHan() {

        int highCode;
        int lowCode;

        ThreadLocalRandom random = ThreadLocalRandom.current();

        //B0 + 0~39(16~55) 一级汉字所占区
        highCode = (176 + Math.abs(random.nextInt(39)));
        //A1 + 0~93 每区有94个汉字
        lowCode = (161 + Math.abs(random.nextInt(93)));

        byte[] b = new byte[2];
        b[0] = (Integer.valueOf(highCode)).byteValue();
        b[1] = (Integer.valueOf(lowCode)).byteValue();

        try {
             return new String(b, PubCommonConst.GBK);
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException:{}",e.getMessage());
            return null;
        }
    }

    /**
     * 获取一个随机汉字 / Get a random Chinese character
     * @param character 字符串 / String
     * @return  String 随机汉字 / Random Chinese character
     */
    public static String getRandomHan(String character) {
        if (character == null || character.isEmpty()) {
            throw new IllegalArgumentException("character must be not null or empty");
        }
        return String.valueOf(character.charAt(ThreadLocalRandom.current().nextInt(character.length())));
    }


}
