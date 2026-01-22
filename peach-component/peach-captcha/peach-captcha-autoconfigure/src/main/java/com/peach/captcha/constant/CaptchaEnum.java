package com.peach.captcha.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 15:46
 * @Description 验证码枚举
 */
public interface CaptchaEnum {

    /**
     * 验证缓存类型 枚举
     */
    enum CaptchaCacheType implements CaptchaEnum {

        MEMORY("MEMORY", "本地缓存"),

        REDIS("REDIS", "Redis缓存");

        private String code;

        private String value;

        CaptchaCacheType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 验证码实现类型 枚举
     */
    enum CaptchaServiceType implements CaptchaEnum {

        /**
         * 旋转拼图.
         */
        ROTATEPUZZLE("ROTATEPUZZLE","旋转拼图"),

        /**
         * 滑块拼图
         */
        BLOCKPUZZLE("BLOCKPUZZLE","滑块拼图"),

        /**
         * 文字点选.
         */
        CLICKWORD("CLICKWORD","文字点选"),

        /**
         * 知识验证.
         */
        KNOWLEDGE("KNOWLEDGE","知识验证"),

        /**
         * 文本/运算.
         */
        TEXT("TEXT","文本/运算");


        private String code;

        private String value;

        CaptchaServiceType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 验证码校验操作类型 枚举
     */
    enum CaptchaOpertionType implements CaptchaEnum {

        GET("GET","获取"),

        CHECK("CHECK","验证"),

        VERIFY("VERIFY","二次验证"),

        LOCK("LOCK","锁定"),

        FAIL("FAIL","失败");

        private String code;

        private String value;

        CaptchaOpertionType(String code, String value) {
            this.code = code;
            this.value = value;
        }

        public String getCode() {
            return code;
        }

        public String getValue() {
            return value;
        }
    }

    enum CaptchCacheMapEnum{
        ROTATE_ORIGINAL("ROTATE_ORIGINAL", "旋转拼图底图"),
        ROTATE_BLOCK("ROTATE_BLOCK", "旋转拼图旋转块底图"),
        SLIDING_ORIGINAL("SLIDING_ORIGINAL", "滑动拼图底图"),
        SLIDING_BLOCK("SLIDING_BLOCK", "滑动拼图滑块底图"),
        PIC_CLICK("PIC_CLICK", "文字点选底图");
        private String code;
        private String value;
        CaptchCacheMapEnum(String code, String value) {
            this.code = code;
            this.value = value;
        }
        public String getCode() {
            return code;
        }
        public String getValue() {
            return value;
        }
        public static CaptchCacheMapEnum getByCode(String code){
            for (CaptchCacheMapEnum captchCacheMapEnum : CaptchCacheMapEnum.values()) {
                if (captchCacheMapEnum.getCode().equals(code)){
                    return captchCacheMapEnum;
                }
            }
            return null;
        }
    }
}
