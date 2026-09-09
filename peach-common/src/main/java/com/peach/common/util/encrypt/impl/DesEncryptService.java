package com.peach.common.util.encrypt.impl;

import com.peach.common.util.encrypt.AbstractCbcEncrypt;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptKeyProfile;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * DES/CBC/PKCS5Padding 实现。
 *
 * <p>密钥加载顺序见 {@link com.peach.common.util.encrypt.EncryptKeyResolver}，
 * 对应配置项为 {@link EncryptConst#DES_KEY_PROPERTY}、{@link EncryptConst#DES_KEY_ENV}
 * 与 {@link EncryptConst#DES_DEFAULT_KEY_FILE}。{@link DESKeySpec} 仅使用前 8 字节。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public class DesEncryptService extends AbstractCbcEncrypt {

    private static final String PAYLOAD_ALGORITHM = "DES-CBC";

    private static final String CIPHER_TRANSFORMATION = "DES/CBC/PKCS5Padding";

    private static final int IV_LENGTH_BYTES = 8;

    private static final byte[] LEGACY_IV = "SHA1PRNG".getBytes(StandardCharsets.UTF_8);

    public DesEncryptService(String algorithm) {
        super(EncryptKeyProfile.DES, algorithm);
    }

    @Override
    protected String payloadAlgorithm() {
        return PAYLOAD_ALGORITHM;
    }

    @Override
    protected int ivLengthBytes() {
        return IV_LENGTH_BYTES;
    }

    @Override
    protected Cipher createCipher(int mode, byte[] iv) throws GeneralSecurityException {
        return createCbcCipher(mode, iv);
    }

    @Override
    protected Cipher createLegacyCipher(int mode) throws GeneralSecurityException {
        return createCbcCipher(mode, LEGACY_IV);
    }

    private Cipher createCbcCipher(int mode, byte[] iv) throws GeneralSecurityException {
        DESKeySpec desKeySpec = new DESKeySpec(resolveKeyBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(keyAlgorithm());
        SecretKey secretKey = keyFactory.generateSecret(desKeySpec);
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, secretKey, new IvParameterSpec(iv));
        return cipher;
    }
}
