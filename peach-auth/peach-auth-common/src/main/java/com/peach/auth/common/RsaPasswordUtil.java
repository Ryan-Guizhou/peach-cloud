package com.peach.auth.common;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.asymmetric.RSA;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.peach.common.util.InstanceLazyLoader;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Rsa 加解密算法。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/3/13 12:42
 * @Description Rsa 加解密算法
 */
@Slf4j
public class RsaPasswordUtil {

    private RsaPasswordUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 存储RSA密钥的缓存key
     */
    private static final String RSA_KEYS = "AUTH:RSA:KEYS";

    /**
     * 初始化rsa锁
     */
    private static final String RSA_LOCK = "AUTH:RSA:LOCK";

    /**
     * 加密类型 RSA 公钥key
     */
    private static final String PUBLIC_KEY = "publicKey";

    /**
     * 加密类型 RSA 私钥KEY
     */
    private static final String PRIVATE_KEY = "privateKey";


    /**
     * RSA 密钥算法
     */
    private static final String ALGORITHM_TYPE = "RSA";

    /**
     * RSA-OAEP（SHA-256）填充，与前端 Web Crypto {@code RSA-OAEP} 对齐。
     */
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";


    /**
     * 缓存RSA密钥的缓存
     */
    private static final Cache<String, Map<String, String>> RSA_PASS_WORD_INFO = CacheBuilder.newBuilder().expireAfterWrite(3, TimeUnit.HOURS).build();

    /**
     * redisson客户端。
     */
    private static final RedissonClient redissonClient = InstanceLazyLoader.getInstance(RedissonClient.class);


    /**
     * 初始化rsa 密钥
     */
    static {
        initKey();
    }

    /**
     * 将密码通过RSA算法解密 然后加密成md5
     * @param password
     * @return
     */
    public static String realPassword(String password) {
        if (StringUtil.isBlank(password)){
            log.error("The password that needs to be decrypted is empty");
            return StringUtil.EMPTY;
        }

        String decryptPassword;
        try {
            decryptPassword = decrypt(password);
        } catch (Exception e) {
            log.error("Password decryption failed");
            throw new IllegalStateException("RSA initialization failed", e);
        }
        return decryptPassword;
    }

    /**
     * 获取解密之后的md5Hex密码
     * @param password
     * @return
     */
    public static String md5HexPasswd(String password){
        String realPassword = realPassword(password);
        return DigestUtil.md5Hex(realPassword);
    }


    public static String encrypt(String plainText) throws GeneralSecurityException {
        Cipher cipher = initCipher(1, getPublicKey());
        byte[] bytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return byteToHex(bytes);
    }

    /**
     * 获取供客户端加密登录密码使用的 RSA 公钥。
     *
     * @return Base64 编码的 X.509 RSA 公钥
     */
    public static String getPublicKeyBase64() {
        Map<String, String> rsaInfo = getRsaInfo();
        if (MapUtil.isEmpty(rsaInfo) || StringUtil.isBlank(rsaInfo.get(PUBLIC_KEY))) {
            throw new IllegalStateException("RSA public key is unavailable");
        }
        return rsaInfo.get(PUBLIC_KEY);
    }


    public static String decrypt(String cipherText) throws GeneralSecurityException {
        Cipher cipher = initCipher(2, getPrivateKey());
        byte[] bytes = hexToByte(cipherText);
        return new String(cipher.doFinal(bytes), StandardCharsets.UTF_8);
    }

    /**
     * 本地缓存获取rsa 公私钥
     * @return
     */
    private static Map<String, String> getRsaInfo() {
        Map<String, String> rsaInfo = null;
        try {
            rsaInfo = RSA_PASS_WORD_INFO.get(RSA_KEYS, () -> redissonClient.getMapCache(RSA_KEYS));
            if (MapUtil.isEmpty(rsaInfo) || rsaInfo.get(PRIVATE_KEY) == null || rsaInfo.get(PUBLIC_KEY) == null) {
                initKey();
                rsaInfo = RSA_PASS_WORD_INFO.get(RSA_KEYS, () -> redissonClient.getMapCache(RSA_KEYS));
            }
        } catch (ExecutionException e) {
            log.error("Get RSA error:", e);
        }
        return rsaInfo;
    }

    /**
     * 初始化rsa 密钥
     */
    private static void initKey() {
        RMapCache<String, String> rsaInfo = redissonClient.getMapCache(RSA_KEYS);
        if (MapUtil.isNotEmpty(rsaInfo) && ObjectUtil.isNotNull(rsaInfo.get(PRIVATE_KEY)) && ObjectUtil.isNotNull(rsaInfo.get(PUBLIC_KEY))) {
            RSA_PASS_WORD_INFO.put(RSA_KEYS, rsaInfo);
        }

        RLock lock = redissonClient.getLock(RSA_LOCK);
        try {
            if (!lock.tryLock(10, TimeUnit.SECONDS)) {
                log.info("Initialization of system login rsa failed, please check the redis connection");
                return;
            }
            rsaInfo = redissonClient.getMapCache(RSA_KEYS);
            if (MapUtil.isEmpty(rsaInfo) || rsaInfo.get(PRIVATE_KEY) == null || rsaInfo.get(PUBLIC_KEY) == null) {
                //生成加密密钥
                RSA rsa = new RSA();
                //私钥加密
                String privateKey = rsa.getPrivateKeyBase64();
                String publicKey = rsa.getPublicKeyBase64();
                rsaInfo.put(PRIVATE_KEY, privateKey);
                rsaInfo.put(PUBLIC_KEY, publicKey);
                log.info("Initialization of system login rsa successful");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialization of system login rsa interrupted", e);
        } catch (Exception e) {
            log.error("Initialization of system login rsa error:", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlockAsync();
            }
        }
    }

    /**
     * 初始化Cipher
     * @param mode
     * @return
     * @throws Exception
     */
    private static Cipher initCipher(int mode, Key key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, key);
        return cipher;
    }

    /**
     * 构建私钥Key
     *
     * @return
     */
    private static PrivateKey getPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_TYPE);
        // 构建密钥规范 进行 Base64 解码
        byte[] decode = Base64.getDecoder().decode(getRsaInfo().get(PRIVATE_KEY));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decode);
        // 生成私钥
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 构建公钥Key
     *
     * @return
     */
    private static PublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 获取密钥工厂
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_TYPE);
        // 构建密钥规范 进行 Base64 解码
        byte[] decode = Base64.getDecoder().decode(getRsaInfo().get(PUBLIC_KEY));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decode);
        return keyFactory.generatePublic(spec);
    }

    /**
     * 16进制转byte
     * @param ciphertext
     * @return
     */
    private static byte[] hexToByte(String ciphertext) {
        byte[] cipherBytes = ciphertext.getBytes(StandardCharsets.UTF_8);
        if ((cipherBytes.length % 2) != 0) {
            log.error("The content:[{}] length is not even",ciphertext);
            throw new IllegalArgumentException(String.format("The content:[%s] length is not even",ciphertext));
        }
        byte[] result = new byte[cipherBytes.length / 2];
        for (int i = 0; i < cipherBytes.length; i += 2) {
            String item = new String(cipherBytes, i, 2);
            result[i / 2] = (byte) Integer.parseInt(item, 16);
        }
        return result;

    }

    /**
     * byte 转16进制
     * @param bytes
     * @return
     */
    private static String byteToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        if (bytes.length == 0){
            return stringBuilder.toString();
        }
        for (int i = 0; i < bytes.length; i++) {
            String s = Integer.toHexString(bytes[i] & 0xFF);
            if (1 == s.length()) {
                stringBuilder.append("0");
            }
            stringBuilder.append(s);
        }
        return stringBuilder.toString();

    }

}
