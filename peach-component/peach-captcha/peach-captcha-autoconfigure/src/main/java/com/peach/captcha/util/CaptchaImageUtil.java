package com.peach.captcha.util;


import com.alibaba.fastjson.JSON;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.common.util.Base64Util;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 17:39
 */
@Slf4j
public final class CaptchaImageUtil {

    private static final Integer DEFAULT_IMAGE_COUNT = 6;

    private static final String IMAGES_SUFFIX = ".png";

    private static final String PATH_SEPARATOR = "/";

    private CaptchaImageUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 旋转底图
     */
    private static final Map<String, String> ROTATE_ORIGINAL_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 旋转块
     */
    private static final Map<String, String> ROTATE_BLOCK_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 滑块底图
     */
    private static final Map<String, String> SLIDING_ORIGINAL_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 滑块
     */
    private static final Map<String, String> SLIDING_BLOCK_CACHE_MAP = new ConcurrentHashMap<>();

    /**
     * 点选文字
     */
    private static final Map<String, String> PIC_CLICK_CACHE_MAP = new ConcurrentHashMap<>();


    /**
     * 图片文件名缓存
     */
    private static final Map<String, String[]> FILE_NAME_MAP = new ConcurrentHashMap<>();


    /**
     * 初始化图片缓存 / Initialize image cache
     * @param captchaOriginalPathJigsaw 滑块底图和滑块路径 / Sliding block original and sliding block path
     * @param captchaOriginalPathClick 点选文字路径 / Click-to-select text path
     * @param captchaOriginalPathRotate 旋转底图和旋转块路径 / Rotate original and rotate block path
     * @throws IOException
     */
    public static void initCaptchaImage(String captchaOriginalPathJigsaw, String captchaOriginalPathClick, String captchaOriginalPathRotate) {

        // 滑块底图和滑块
        if (StringUtil.isNotBlank(captchaOriginalPathJigsaw)){
            SLIDING_ORIGINAL_CACHE_MAP.putAll(getCustomImagesFile(captchaOriginalPathJigsaw + "/original"));
            SLIDING_BLOCK_CACHE_MAP.putAll(getCustomImagesFile(captchaOriginalPathJigsaw + "/slidingBlock"));
        }else {
            SLIDING_ORIGINAL_CACHE_MAP.putAll(getResourcesImagesFile("defaultImages/sliding/original"));
            SLIDING_BLOCK_CACHE_MAP.putAll(getResourcesImagesFile("defaultImages/sliding/slidingBlock"));
        }

        // 旋转底图和旋转块
        if (StringUtil.isNotBlank(captchaOriginalPathRotate)){
            ROTATE_ORIGINAL_CACHE_MAP.putAll(getCustomImagesFile(captchaOriginalPathRotate + "/original"));
            ROTATE_BLOCK_CACHE_MAP.putAll(getCustomImagesFile(captchaOriginalPathRotate + "/slidingBlock"));
        }else {
            ROTATE_ORIGINAL_CACHE_MAP.putAll(getResourcesImagesFile("defaultImages/rotate/original"));
            ROTATE_BLOCK_CACHE_MAP.putAll(getResourcesImagesFile("defaultImages/rotate/slidingBlock"));
        }

        if (StringUtil.isNotBlank(captchaOriginalPathClick)){
            PIC_CLICK_CACHE_MAP.putAll(getCustomImagesFile(captchaOriginalPathClick));
        }else {
            PIC_CLICK_CACHE_MAP.putAll(getResourcesImagesFile("defaultImages/pic-click"));
        }

        FILE_NAME_MAP.put(CaptchaEnum.CaptchCacheMapEnum.SLIDING_ORIGINAL.getCode(), SLIDING_ORIGINAL_CACHE_MAP.keySet().toArray(new String[0]));
        FILE_NAME_MAP.put(CaptchaEnum.CaptchCacheMapEnum.SLIDING_BLOCK.getCode(), SLIDING_BLOCK_CACHE_MAP.keySet().toArray(new String[0]));
        FILE_NAME_MAP.put(CaptchaEnum.CaptchCacheMapEnum.ROTATE_ORIGINAL.getCode(), ROTATE_ORIGINAL_CACHE_MAP.keySet().toArray(new String[0]));
        FILE_NAME_MAP.put(CaptchaEnum.CaptchCacheMapEnum.ROTATE_BLOCK.getCode(), ROTATE_BLOCK_CACHE_MAP.keySet().toArray(new String[0]));
        FILE_NAME_MAP.put(CaptchaEnum.CaptchCacheMapEnum.PIC_CLICK.getCode(), PIC_CLICK_CACHE_MAP.keySet().toArray(new String[0]));

        log.info("CaptchaImageUtil initCaptcha success, FILE_NAME_MAP: {}", JSON.toJSONString(FILE_NAME_MAP));
    }

    /**
     * 获取resource图片资源文件 / Retrieve image resource files
     * @param path 图片资源路径 / Image resource path
     * @return Map<String,String> 图片资源文件映射 / Image resource file map
     */
    private static Map<String,String> getResourcesImagesFile(String path){
        Map<String,String> imagesMap = HashMap.newHashMap(64);
        if (StringUtil.isBlank(path)){
            return imagesMap;
        }
        ClassLoader classLoader = CaptchaImageUtil.class.getClassLoader();
        for (Integer defaultImageCount = DEFAULT_IMAGE_COUNT; defaultImageCount > 0; defaultImageCount--) {
            String completePath = path.concat(PATH_SEPARATOR).concat(defaultImageCount.toString()).concat(IMAGES_SUFFIX);
            InputStream resourceAsStream = classLoader.getResourceAsStream(completePath);
            try {
                assert path != null;
                byte[] bytes = FileCopyUtil.copyToByteArray(resourceAsStream);
                String keyName = defaultImageCount.toString().concat(IMAGES_SUFFIX);
                String encodeValue = Base64Util.encodeToString(bytes);
                imagesMap.put(keyName, encodeValue);
            } catch (IOException e) {
                log.error("Error copying resource to byte array: {}", completePath, e);
            }
        }
        return imagesMap;
    }

    /**
     * 获取自定义图片资源文件 / Retrieve custom image resource files
     * @param customPath 自定义图片资源路径 / Custom image resource path
     * @return Map<String,String> 自定义图片资源文件映射 / Custom image resource file map
     */
    private static Map<String,String> getCustomImagesFile(String customPath){
        Map<String,String> imagesMap = HashMap.newHashMap(64);
        if (StringUtil.isBlank(customPath)){
            return imagesMap;
        }
        File file = new File(customPath);
        if (!file.exists() || !file.isDirectory()){
            return imagesMap;
        }
        File[] files = file.listFiles();
        assert files != null;
        for (File item : files) {
            try {
                byte[] bytes = FileCopyUtil.copyToByteArray(new FileInputStream(item));
                String encodeValue =  Base64Util.encodeToString(bytes);
                imagesMap.put(item.getName(), encodeValue);
            } catch (IOException e) {
                log.error("Error copying resource to byte array: {}", item.getName(), e);
            }
        }
        return imagesMap;
    }


    /**
     * 将图片对象转换为base64字符串 / Convert image object to base64 string
     * @param templateImage 图片对象 / Image object
     * @return String base64字符串 / Base64 string
     */
    public static String getImageToBase64Str(BufferedImage templateImage) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()){
            ImageIO.write(templateImage, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64Util.encodeToString(bytes);
        } catch (IOException e) {
            log.error("getImageToBase64Str error:{}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 将base64字符串转换为图片 / Convert base64 string to image
     * @param base64String base64字符串 / Base64 string
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getBase64StrToImage(String base64String) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Util.decodeFromString(base64String))){
            return ImageIO.read(inputStream);
        } catch (IOException e) {
            log.error("getBase64StrToImage error:{}",e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取图片点击验证码图片 / Get image click verification code image
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getPicClickImage() {
        return getBufferedImage(CaptchaEnum.CaptchCacheMapEnum.PIC_CLICK,PIC_CLICK_CACHE_MAP);
    }

    /**
     * 获取滑动验证码图片 / Get sliding verification code image
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getSlidingImage() {
        return getBufferedImage(CaptchaEnum.CaptchCacheMapEnum.SLIDING_ORIGINAL,SLIDING_ORIGINAL_CACHE_MAP);
    }

    /**
     * 获取滑动验证码图片 / Get sliding verification code image
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getSlidingBlockImage() {
        return getBufferedImage(CaptchaEnum.CaptchCacheMapEnum.SLIDING_BLOCK,SLIDING_BLOCK_CACHE_MAP);
    }

    /**
     * 获取旋转验证码图片 / Get rotation verification code image
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getRotateImage() {
        return getBufferedImage(CaptchaEnum.CaptchCacheMapEnum.ROTATE_ORIGINAL,ROTATE_ORIGINAL_CACHE_MAP);
    }

    /**
     * 获取旋转验证码图片 / Get rotation verification code image
     * @return BufferedImage 图片对象 / Image object
     */
    public static BufferedImage getRotateBlockImage() {
        return getBufferedImage(CaptchaEnum.CaptchCacheMapEnum.ROTATE_BLOCK,ROTATE_BLOCK_CACHE_MAP);
    }

    /**
     * 获取图片对象 / Get image object
     * @param captchCacheMapEnum 验证码缓存枚举 / Captcha cache enum
     * @param cacheMap 验证码缓存映射 / Captcha cache map
     * @return BufferedImage 图片对象 / Image object
     */
    private static BufferedImage getBufferedImage(CaptchaEnum.CaptchCacheMapEnum captchCacheMapEnum,Map<String, String> cacheMap) {
        String[] strings = FILE_NAME_MAP.get(captchCacheMapEnum.getCode());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = RandomUtils.getRandomInt(0, strings.length);
        String base64Encode = cacheMap.get(strings[randomInt]);
        return getBase64StrToImage(base64Encode);
    }

    /**
     * 获取滑动验证码图片名称 / Get sliding verification code image name
     * @return String 图片名称 / Image name
     */
    public static String getSlidingBlockString() {
        String[] strings = FILE_NAME_MAP.get(CaptchaEnum.CaptchCacheMapEnum.SLIDING_BLOCK.getCode());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = RandomUtils.getRandomInt(0, strings.length);
        return SLIDING_BLOCK_CACHE_MAP.get(strings[randomInt]);
    }

    /**
     * 获取旋转验证码图片名称 / Get rotation verification code image name
     * @return String 图片名称 / Image name
     */
    public static String getRotateBlockString() {
        String[] strings = FILE_NAME_MAP.get(CaptchaEnum.CaptchCacheMapEnum.ROTATE_BLOCK.getCode());
        if (null == strings || strings.length == 0) {
            return null;
        }
        Integer randomInt = RandomUtils.getRandomInt(0, strings.length);
        return ROTATE_BLOCK_CACHE_MAP.get(strings[randomInt]);
    }

}
