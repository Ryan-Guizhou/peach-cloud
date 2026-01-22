package com.peach.captcha.service.impl;


import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.model.PointVO;
import com.peach.captcha.util.AesUtil;
import com.peach.captcha.util.CaptchaImageUtil;
import com.peach.captcha.util.JsonUtil;
import com.peach.captcha.util.RandomUtils;
import com.peach.common.keymanager.RedisKeyBuild;
import com.peach.common.keymanager.RedisKeyManage;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/21 17:10
 */
@Slf4j
public class BlockPuzzleCaptchaServiceImpl extends AbstractCacheService{


    @Override
    public void init(Properties config) {
        super.init(config);
    }

    @Override
    public Response get(CaptchaVO captchaVO) {
        Response response = super.get(captchaVO);
        if (!validatedReq(response)){
            return response;
        }
        BufferedImage originalSlidingImage = CaptchaImageUtil.getSlidingImage();
        if (originalSlidingImage == null){
            log.error("init original sliding image error");
            return Response.fail("init original sliding image error");
        }

        //设置水印
        Graphics graphics = originalSlidingImage.getGraphics();
        int height = originalSlidingImage.getHeight();
        int width = originalSlidingImage.getWidth();
        graphics.setColor(Color.white);
        graphics.setFont(WARK_MARK_FRONT);
        graphics.drawString(WATER_MARK, width - getEnOrChLength(WATER_MARK), height - (HAN_ZI_SIZE / 2) + 7);

        String slidingBlockString = CaptchaImageUtil.getSlidingBlockString();
        BufferedImage slidingBlockImage = CaptchaImageUtil.getBase64StrToImage(slidingBlockString);
        if (slidingBlockImage == null){
            log.error("init sliding block image error");
            return Response.fail("init sliding block image error");
        }

        // 切图
        CaptchaVO captcha = pictureTemplatesCut(originalSlidingImage, slidingBlockImage, slidingBlockString);
        if (captcha == null
                || StringUtils.isBlank(captcha.getSlidingOriginalImageBase64())
                || StringUtils.isBlank(captcha.getNewSlidingBlockingImageBase64())) {
            return Response.fail("pictureTemplatesCut error");
        }

        return Response.success(captcha);
    }


    @Override
    public Response check(CaptchaVO captchaVO) {
        Response check = super.check(captchaVO);
        if (!validatedReq(check)){
            return check;
        }
        String codeKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, captchaVO.getToken())
                .getRealKey();
        if (!existCaptchaKey(codeKey)){
            log.error("captcha check not found, key: {}", codeKey);
            return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
        }
        // 获取point
        String pointJson = getCaptchaByKey(codeKey);
        deleteCaptchKey(codeKey);
        PointVO cachePoint = null;
        PointVO frontPoint = null;
        try {
            cachePoint = JsonUtil.parseObject(pointJson, PointVO.class);
            //aes解密
            pointJson = decrypt(captchaVO.getPointJson(), cachePoint.getSecretKey());
            frontPoint = JsonUtil.parseObject(pointJson, PointVO.class);
        } catch (Exception e) {
            log.error("captcha point parse error, pointJson: {}", pointJson, e);
            afterValidateFail(captchaVO);
            return Response.fail(e.getMessage());
        }

        if (cachePoint.x - Integer.parseInt(SLIP_OFFSET) > frontPoint.x
                || frontPoint.x > cachePoint.x + Integer.parseInt(SLIP_OFFSET)
                || cachePoint.y != frontPoint.y) {
            afterValidateFail(captchaVO);
            return Response.fail("coordinate error");
        }
        //校验成功，将信息存入缓存
        String secretKey = cachePoint.getSecretKey();
        String value;
        try {
            value = AesUtil.aesEncrypt(captchaVO.getToken().concat("@").concat(pointJson), secretKey);
            log.info("captcha secretKey:{},  value:{}",secretKey,value);
        } catch (Exception e) {
            log.error("AES encrypt error ", e);
            afterValidateFail(captchaVO);
            return Response.fail(e.getMessage());
        }
        String secondKey = RedisKeyBuild
                .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND,value)
                .getRealKey();
        setCaptchaCahche(secondKey, captchaVO.getToken());
        captchaVO.setResult(true);
        captchaVO.resetClientFlag();
        captchaVO.setCaptchaVerification(value);
        log.info("captcha pointJson:{},token:{}",captchaVO.getPointJson(),captchaVO.getToken());
        return Response.success();
    }

    @Override
    public Response verification(CaptchaVO captchaVO) {
        Response r = super.verification(captchaVO);
        if(!validatedReq(r)){
            return r;
        }

        try {
            String codeKey = RedisKeyBuild
                    .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA_SECOND,captchaVO.getCaptchaVerification())
                    .getRealKey();
            if (!existCaptchaKey(codeKey)) {
                log.error("captcha verification not found, key: {}", codeKey);
                return Response.fail(StatusEnum.API_CAPTCHA_INVALID);
            }
            //二次校验取值后，即刻失效
            deleteCaptchKey(codeKey);
            return Response.success();
        } catch (Exception e) {
            log.error("captcha verification error, key: {}", captchaVO.getCaptchaVerification(), e);
            return Response.fail(e.getMessage());
        }

    }

   /**
     * 切图 / Cut the picture
     *
     * @param slidingOriginalImage
     * @param slidingBlockingImage
     * @param slidingBlockingBase64
     * @return
     */
    public CaptchaVO pictureTemplatesCut(BufferedImage slidingOriginalImage, BufferedImage slidingBlockingImage, String slidingBlockingBase64) {
        try {
            CaptchaVO dataVO = new CaptchaVO();

            int slidingOriginalImageWidth = slidingOriginalImage.getWidth();
            int slidingOriginalImageHeight = slidingOriginalImage.getHeight();
            int slidingBlockingImageWidth = slidingBlockingImage.getWidth();
            int slidingBlockingImageHeight = slidingBlockingImage.getHeight();

            //随机生成拼图坐标
            PointVO point = generateSlidingPoint(slidingOriginalImageWidth, slidingOriginalImageHeight, slidingBlockingImageWidth, slidingBlockingImageHeight);
            int x = point.getX();

            //生成新的拼图图像
            BufferedImage newSlidingImage = new BufferedImage(slidingBlockingImageWidth, slidingBlockingImageHeight, slidingBlockingImage.getType());
            Graphics2D graphics = newSlidingImage.createGraphics();

            int bold = 5;
            //如果需要生成RGB格式，需要做如下配置,Transparency 设置透明
            newSlidingImage = graphics.getDeviceConfiguration().createCompatibleImage(slidingBlockingImageWidth, slidingBlockingImageHeight, Transparency.TRANSLUCENT);
            // 新建的图像根据模板颜色赋值,源图生成遮罩
            cutByTemplate(slidingOriginalImage, slidingBlockingImage, newSlidingImage, x);

            extracted(slidingOriginalImage, slidingBlockingBase64, slidingOriginalImageWidth, slidingBlockingImageWidth, x);

            // 设置“抗锯齿”的属性
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
            graphics.drawImage(newSlidingImage, 0, 0, null);
            graphics.dispose();
            //新建流。
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
            ImageIO.write(newSlidingImage, IMAGE_TYPE_PNG, os);
            byte[] jigsawImages = os.toByteArray();
            //新建流。
            ByteArrayOutputStream oriImagesOs = new ByteArrayOutputStream();
            //利用ImageIO类提供的write方法，将bi以jpg图片的数据模式写入流。
            ImageIO.write(slidingOriginalImage, IMAGE_TYPE_PNG, oriImagesOs);
            byte[] oriCopyImages = oriImagesOs.toByteArray();
            Base64.Encoder encoder = Base64.getEncoder();
            // 抠图之后的图
            dataVO.setSlidingOriginalImageBase64(encoder.encodeToString(oriCopyImages).replaceAll("\r|\n", ""));
            //point信息不传到前端，只做后端check校验
            // 滑块
            dataVO.setNewSlidingBlockingImageBase64(encoder.encodeToString(jigsawImages).replaceAll("\r|\n", ""));
            dataVO.setToken(RandomUtils.getUuid());
            dataVO.setSecretKey(point.getSecretKey());
            //将坐标信息存入redis中
            String codeKey = RedisKeyBuild
                    .createRedisKey(RedisKeyManage.RUNNING_CAPTCHA, dataVO.getToken())
                    .getRealKey();
            setCaptchaCahche(codeKey,point.toJsonString());
            log.debug("token：{},point:{}", dataVO.getToken(), JsonUtil.toJsonString(point));
            return dataVO;
        } catch (Exception e) {
            log.error("pictureTemplatesCut error:{}", e.getMessage(), e);
            return null;
        }
    }

    private void extracted(final BufferedImage slidingOriginalImage, final String slidingBlockingBase64, final int slidingOriginalWidth, final int slidingBlockingWidth, final int x) {
        if (INTERFERENCE_OPTIONS > 0) {
            int position = 0;
            if (slidingOriginalWidth - x - 5 > slidingBlockingWidth * 2) {
                //在原扣图右边插入干扰图
                position = RandomUtils.getRandomInt(x + slidingBlockingWidth + 5, slidingOriginalWidth - slidingBlockingWidth);
            } else {
                //在原扣图左边插入干扰图
                position = RandomUtils.getRandomInt(100, x - slidingBlockingWidth - 5);
            }
            while (true) {
                String s = CaptchaImageUtil.getSlidingBlockString();
                if (!slidingBlockingBase64.equals(s)) {
                    interferenceByTemplate(slidingOriginalImage, Objects.requireNonNull(CaptchaImageUtil.getBase64StrToImage(s)), position);
                    break;
                }
            }
        }
        if (INTERFERENCE_OPTIONS > 1) {
            while (true) {
                String s = CaptchaImageUtil.getSlidingBlockString();
                if (!slidingBlockingBase64.equals(s)) {
                    Integer randomInt = RandomUtils.getRandomInt(slidingBlockingWidth, 100 - slidingBlockingWidth);
                    interferenceByTemplate(slidingOriginalImage, Objects.requireNonNull(CaptchaImageUtil.getBase64StrToImage(s)), randomInt);
                    break;
                }
            }
        }
    }

    /**
     * 干扰抠图处理
     *
     * @param slidingOriginalImage 原图
     * @param templateImage 模板图
     * @param x 随机扣取坐标X
     */
    private static void interferenceByTemplate(BufferedImage slidingOriginalImage, BufferedImage templateImage, int x) {
        //临时数组遍历用于高斯模糊存周边像素值
        int[][] martrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    //抠图区域高斯模糊
                    readPixel(slidingOriginalImage, x + i, j, values);
                    fillMatrix(martrix, values);
                    slidingOriginalImage.setRGB(x + i, j, avgMatrix(martrix));
                }
                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if (case1(rgb,rightRgb)|| case2(rgb,rightRgb) || case3(rgb,downRgb) || case4(rgb,downRgb)) {
                    slidingOriginalImage.setRGB(x + i, j, Color.white.getRGB());
                }
            }
        }

    }

    private void cutByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage newImage, int x) {
        //临时数组遍历用于高斯模糊存周边像素值
        int[][] martrix = new int[3][3];
        int[] values = new int[9];

        int xLength = templateImage.getWidth();
        int yLength = templateImage.getHeight();
        // 模板图像宽度
        for (int i = 0; i < xLength; i++) {
            // 模板图片高度
            for (int j = 0; j < yLength; j++) {
                // 如果模板图像当前像素点不是透明色 copy源文件信息到目标图片中
                int rgb = templateImage.getRGB(i, j);
                if (rgb < 0) {
                    newImage.setRGB(i, j, oriImage.getRGB(x + i, j));

                    //抠图区域高斯模糊
                    readPixel(oriImage, x + i, j, values);
                    fillMatrix(martrix, values);
                    oriImage.setRGB(x + i, j, avgMatrix(martrix));
                }

                //防止数组越界判断
                if (i == (xLength - 1) || j == (yLength - 1)) {
                    continue;
                }
                int rightRgb = templateImage.getRGB(i + 1, j);
                int downRgb = templateImage.getRGB(i, j + 1);
                //描边处理，,取带像素和无像素的界点，判断该点是不是临界轮廓点,如果是设置该坐标像素是白色
                if (case1(rgb,rightRgb)|| case2(rgb,rightRgb) || case3(rgb,downRgb) || case4(rgb,downRgb)) {
                    newImage.setRGB(i, j, Color.white.getRGB());
                    oriImage.setRGB(x + i, j, Color.white.getRGB());
                }
            }
        }

    }

    private static void readPixel(BufferedImage img, int x, int y, int[] pixels) {
        int xStart = x - 1;
        int yStart = y - 1;
        int current = 0;
        for (int i = xStart; i < 3 + xStart; i++) {
            for (int j = yStart; j < 3 + yStart; j++) {
                int tx = i;
                if (tx < 0) {
                    tx = -tx;

                } else if (tx >= img.getWidth()) {
                    tx = x;
                }
                int ty = j;
                if (ty < 0) {
                    ty = -ty;
                } else if (ty >= img.getHeight()) {
                    ty = y;
                }
                pixels[current++] = img.getRGB(tx, ty);

            }
        }
    }

    private static void fillMatrix(int[][] matrix, int[] values) {
        int filled = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                x[j] = values[filled++];
            }
        }
    }

    private static int avgMatrix(int[][] matrix) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int[] x : matrix) {
            for (int j = 0; j < x.length; j++) {
                if (j == 1) {
                    continue;
                }
                Color c = new Color(x[j]);
                r += c.getRed();
                g += c.getGreen();
                b += c.getBlue();
            }
        }
        return new Color(r / 8, g / 8, b / 8).getRGB();
    }

    public static boolean case1(int rgb,int rightRgb){
        return rgb >= 0 && rightRgb < 0;
    }

    public static boolean case2(int rgb,int rightRgb){
        return rgb < 0 && rightRgb >= 0;
    }

    public static boolean case3(int rgb,int downRgb){
        return rgb >= 0 && downRgb < 0;
    }

    public static boolean case4(int rgb,int downRgb){
        return rgb < 0 && downRgb >= 0;
    }

    private static PointVO generateSlidingPoint(int slidingOriginalWidth, int slidingOriginalHeight, int slidingBlockingWidth, int slidingBlockingHeight) {
        Random random = ThreadLocalRandom.current();
        int widthDifference = slidingOriginalWidth - slidingBlockingWidth;
        int heightDifference = slidingOriginalHeight - slidingBlockingHeight;
        int x = widthDifference <= 0 ? 5 : random.nextInt(slidingOriginalWidth - slidingBlockingWidth - 100) + 100;
        int y = heightDifference <= 0 ? 5 : random.nextInt(slidingOriginalHeight - slidingBlockingHeight - 100) + 5;
        String key = null;
        if (CAPTCHA_AES_STATUS) {
            key = AesUtil.getKey();
        }
        return new PointVO(x, y, key);
    }



}
