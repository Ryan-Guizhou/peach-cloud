package com.peach.common.util.encrypt.impl;

import com.peach.common.util.encrypt.AbstractCbcEncrypt;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptKeyProfile;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * AES/CBC/PKCS5Padding 实现。
 *
 * <p>密钥加载顺序见 {@link com.peach.common.util.encrypt.EncryptKeyResolver}，
 * 对应配置项为 {@link EncryptConst#AES_KEY_PROPERTY}、{@link EncryptConst#AES_KEY_ENV}
 * 与 {@link EncryptConst#AES_DEFAULT_KEY_FILE}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public class AesEncryptService extends AbstractCbcEncrypt {

    private static final String PAYLOAD_ALGORITHM = "AES-GCM";

    private static final String GCM_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final String CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding";

    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final byte[] LEGACY_IV = "SHA1PRNG20250313".getBytes(StandardCharsets.UTF_8);

    public AesEncryptService(String algorithm) {
        super(EncryptKeyProfile.AES, algorithm);
    }

    @Override
    protected String payloadAlgorithm() {
        return PAYLOAD_ALGORITHM;
    }

    @Override
    protected int ivLengthBytes() {
        return GCM_IV_LENGTH_BYTES;
    }

    @Override
    protected Cipher createCipher(int mode, byte[] iv) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(resolveKeyBytes(), keyAlgorithm());
        Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION);
        cipher.init(mode, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher;
    }

    @Override
    protected Cipher createLegacyCipher(int mode) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(resolveKeyBytes(), keyAlgorithm());
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
        cipher.init(mode, secretKey, new IvParameterSpec(LEGACY_IV));
        return cipher;
    }
}
