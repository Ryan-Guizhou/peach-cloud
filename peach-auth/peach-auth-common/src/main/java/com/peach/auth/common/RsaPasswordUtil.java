package com.peach.auth.common;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.HexUtil;
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
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RSA 非对称加解密工具类（专用于登录密码传输加密与摘要校验）。
 *
 * <p><b>设计架构与业务目标：</b>
 * <ul>
 *   <li><b>传输安全：</b>前端使用 RSA 公钥对明文密码进行传输加密，后端通过私钥解密，保障数据链路安全。</li>
 *   <li><b>存储安全：</b>解密获取明文后计算 MD5 摘要，与数据库中的摘要存储值进行对比校验。</li>
 *   <li><b>高可用与性能平衡：</b>基于 Redis 实现分布式共享密钥，并辅以 Guava Local Cache 降低网络开销。</li>
 * </ul>
 *
 * <p><b>前端加解密对齐规范：</b>
 * <ul>
 *   <li><b>算法类型：</b>RSA（密钥长度 1024 位）</li>
 *   <li><b>填充模式：</b>RSA/ECB/OAEPWithSHA-256AndMGF1Padding（与 Web Crypto API 严格兼容）</li>
 *   <li><b>密文编码：</b>十六进制字符串（Hex）</li>
 * </ul>
 *
 * <p><b>分布式密钥管理机制：</b>
 * <ul>
 *   <li><b>持久化存储：</b>密钥对存储于 Redis ({@link RMapCache}) 中，实现跨实例共享。</li>
 *   <li><b>本地二级缓存：</b>使用 Guava Cache 缓存密钥快照（写后 3 小时过期），显著降低 Redis 访问频率。</li>
 *   <li><b>并发并发控制：</b>基于 Redisson 分布式锁，防止集群多节点同时初始化导致密钥覆盖。</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/3/13 12:42
 */
@Slf4j
public final class RsaPasswordUtil {

    /**
     * Redis 中存储 RSA 密钥 Map 的键名
     */
    private static final String RSA_KEYS = "AUTH:RSA:KEYS";

    /**
     * Redis 分布式锁键名，确保密钥生成过程的互斥性
     */
    private static final String RSA_LOCK = "AUTH:RSA:LOCK";

    /**
     * 密钥 Map 中公钥的标识 Key
     */
    private static final String PUBLIC_KEY = "publicKey";

    /**
     * 密钥 Map 中私钥的标识 Key
     */
    private static final String PRIVATE_KEY = "privateKey";

    /**
     * 密钥算法标准名称
     */
    private static final String ALGORITHM_TYPE = "RSA";

    /**
     * Cipher 加解密转换模式规范
     */
    private static final String CIPHER_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * OAEP 参数规格定义（SHA-256 摘要算法与 MGF1 掩码生成函数）
     */
    private static final OAEPParameterSpec OAEP_SHA256 = new OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
    );

    /**
     * Guava 本地快照缓存，减少高频解密请求对分布式缓存的开销
     */
    private static final Cache<String, Map<String, String>> LOCAL_CACHE =
            CacheBuilder.newBuilder()
                    .expireAfterWrite(3, TimeUnit.HOURS)
                    .build();

    /**
     * Redisson 客户端，采用延迟加载确保 Spring 依赖注入完成
     */
    private static final RedissonClient REDISSON_CLIENT =
            InstanceLazyLoader.getInstance(RedissonClient.class);

    /**
     * 私有构造方法，防止工具类被实例化
     */
    private RsaPasswordUtil() {
        throw new IllegalStateException("Utility class should not be instantiated.");
    }

    /**
     * 解密前端传递的十六进制 RSA 密文，还原原始明文密码。
     *
     * @param encryptedPassword 十六进制格式的密码密文
     * @return 解密后的明文密码字符串；若输入为空则返回空字符串
     * @throws IllegalStateException 当 RSA 密钥尚未就绪或解密执行失败时抛出
     */
    public static String realPassword(String encryptedPassword) {
        if (StringUtil.isBlank(encryptedPassword)) {
            log.error("Decryption failed: encrypted password is empty.");
            return StringUtil.EMPTY;
        }

        try {
            return decrypt(encryptedPassword);
        } catch (GeneralSecurityException e) {
            log.warn("Password decryption failed, reason={}, message={}",
                    e.getClass().getSimpleName(), e.getMessage());
            throw new IllegalStateException("RSA password decryption failed.", e);
        }
    }

    /**
     * 解密前端传递的 RSA 密码密文，并生成 MD5 十六进制摘要。
     *
     * @param encryptedPassword 十六进制格式的密码密文
     * @return 原始明文密码的 MD5 十六进制摘要字符串
     * @throws IllegalStateException 当解密失败时抛出
     */
    public static String md5HexPasswd(String encryptedPassword) {
        String plainText = realPassword(encryptedPassword);
        return DigestUtil.md5Hex(plainText);
    }

    /**
     * 获取当前有效的 Base64 编码 RSA 公钥。
     *
     * @return Base64 格式的公钥字符串，用于前端加密
     * @throws IllegalStateException 当 Redis 连接异常或密钥缺失时抛出
     */
    public static String getPublicKeyBase64() {
        Map<String, String> keyMap = getRsaInfo();
        if (MapUtil.isEmpty(keyMap) || StringUtil.isBlank(keyMap.get(PUBLIC_KEY))) {
            throw new IllegalStateException("RSA public key is unavailable. Please check Redis connection.");
        }
        return keyMap.get(PUBLIC_KEY);
    }

    /**
     * 使用当前公钥加密明文数据。
     *
     * @param plainText 明文内容
     * @return 十六进制格式的加密结果
     * @throws GeneralSecurityException 加密算法配置或执行异常
     */
    public static String encrypt(String plainText) throws GeneralSecurityException {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE, getPublicKey());
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return HexUtil.encodeHexStr(encryptedBytes);
    }

    /**
     * 使用当前私钥解密十六进制密文数据。
     *
     * @param cipherText 十六进制格式的密文
     * @return 解密后的明文字符串
     * @throws GeneralSecurityException 解密算法配置或执行异常
     */
    public static String decrypt(String cipherText) throws GeneralSecurityException {
        byte[] encryptedBytes = HexUtil.decodeHex(cipherText);
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE, getPrivateKey());
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    /**
     * 按照多级缓存策略获取 RSA 密钥对。
     *
     * @return 包含公私钥的映射表；若读取与初始化均失败则返回 null
     */
    private static Map<String, String> getRsaInfo() {
        Map<String, String> localData = LOCAL_CACHE.getIfPresent(RSA_KEYS);
        if (MapUtil.isNotEmpty(localData)) {
            return localData;
        }

        RMapCache<String, String> redisMap = REDISSON_CLIENT.getMapCache(RSA_KEYS);
        Map<String, String> redisData = redisMap.readAllMap();

        if (MapUtil.isNotEmpty(redisData) && redisData.containsKey(PRIVATE_KEY) && redisData.containsKey(PUBLIC_KEY)) {
            LOCAL_CACHE.put(RSA_KEYS, new HashMap<>(redisData));
            return redisData;
        }

        log.warn("RSA keys not found in Redis, triggering initialization...");
        initKey();

        redisData = redisMap.readAllMap();
        if (MapUtil.isNotEmpty(redisData) && redisData.containsKey(PRIVATE_KEY)) {
            LOCAL_CACHE.put(RSA_KEYS, new HashMap<>(redisData));
            return redisData;
        }

        log.error("RSA keys still unavailable after initialization attempt.");
        return null;
    }

    /**
     * 初始化分布式 RSA 密钥对。
     * <p>利用分布式锁保证全局单节点生成，并处理锁竞争与双重检查逻辑。
     */
    private static void initKey() {
        RMapCache<String, String> redisMap = REDISSON_CLIENT.getMapCache(RSA_KEYS);

        Map<String, String> existing = redisMap.readAllMap();
        if (MapUtil.isNotEmpty(existing) && existing.containsKey(PRIVATE_KEY)) {
            LOCAL_CACHE.put(RSA_KEYS, new HashMap<>(existing));
            log.debug("RSA keys already exist in Redis, loaded into local cache.");
            return;
        }

        RLock lock = REDISSON_CLIENT.getLock(RSA_LOCK);
        try {
            boolean locked = lock.tryLock(10, TimeUnit.SECONDS);
            if (!locked) {
                log.info("Could not acquire RSA lock, another instance is likely initializing. Skipping.");
                return;
            }

            try {
                existing = redisMap.readAllMap();
                if (MapUtil.isNotEmpty(existing) && existing.containsKey(PRIVATE_KEY)) {
                    LOCAL_CACHE.put(RSA_KEYS, new HashMap<>(existing));
                    log.debug("RSA keys generated by another instance, loaded into local cache.");
                    return;
                }

                RSA rsa = new RSA();
                String privateKey = rsa.getPrivateKeyBase64();
                String publicKey = rsa.getPublicKeyBase64();

                Map<String, String> newKeyMap = new HashMap<>(2);
                newKeyMap.put(PRIVATE_KEY, privateKey);
                newKeyMap.put(PUBLIC_KEY, publicKey);
                redisMap.putAll(newKeyMap);

                LOCAL_CACHE.put(RSA_KEYS, newKeyMap);

                log.info("System login RSA key pair initialized successfully (1024 bits).");

            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Initialization of RSA keys interrupted.", e);
        } catch (Exception e) {
            log.error("Initialization of RSA keys failed.", e);
        }
    }

    /**
     * 构建并初始化 Cipher 实例。
     *
     * @param mode 加解密模式（如 {@link Cipher#ENCRYPT_MODE} 或 {@link Cipher#DECRYPT_MODE}）
     * @param key  密钥对象（{@link PublicKey} 或 {@link PrivateKey}）
     * @return 配置完毕的 Cipher 实例
     * @throws GeneralSecurityException 算法配置或模式初始化异常
     */
    private static Cipher initCipher(int mode, Key key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, key, OAEP_SHA256);
        return cipher;
    }

    /**
     * 解析 Base64 文本生成标准 PKCS8 规范的 RSA 私钥对象。
     *
     * @return RSA 私钥对象
     * @throws GeneralSecurityException 私钥解码或格式转换失败
     */
    private static PrivateKey getPrivateKey() throws GeneralSecurityException {
        Map<String, String> keyMap = getRsaInfo();
        if (MapUtil.isEmpty(keyMap) || StringUtil.isBlank(keyMap.get(PRIVATE_KEY))) {
            throw new IllegalStateException("RSA private key is missing.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyMap.get(PRIVATE_KEY));
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_TYPE);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 解析 Base64 文本生成标准 X509 规范的 RSA 公钥对象。
     *
     * @return RSA 公钥对象
     * @throws GeneralSecurityException 公钥解码或格式转换失败
     */
    private static PublicKey getPublicKey() throws GeneralSecurityException {
        Map<String, String> keyMap = getRsaInfo();
        if (MapUtil.isEmpty(keyMap) || StringUtil.isBlank(keyMap.get(PUBLIC_KEY))) {
            throw new IllegalStateException("RSA public key is missing.");
        }
        byte[] keyBytes = Base64.getDecoder().decode(keyMap.get(PUBLIC_KEY));
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM_TYPE);
        return keyFactory.generatePublic(spec);
    }
}
