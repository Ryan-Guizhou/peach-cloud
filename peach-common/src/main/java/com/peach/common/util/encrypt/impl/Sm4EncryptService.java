package com.peach.common.util.encrypt.impl;

import com.peach.common.util.encrypt.AbstractCbcEncrypt;
import com.peach.common.util.encrypt.EncryptConst;
import com.peach.common.util.encrypt.EncryptKeyProfile;
import com.peach.common.util.encrypt.support.BouncyCastleProviderSupport;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * SM4/CBC/PKCS5Padding 实现。
 *
 * <p>依赖 BouncyCastle Provider 提供 SM4 算法支持。密钥加载顺序见
 * {@link com.peach.common.util.encrypt.EncryptKeyResolver}，对应配置项为
 * {@link EncryptConst#SM4_KEY_PROPERTY}、{@link EncryptConst#SM4_KEY_ENV}
 * 与 {@link EncryptConst#SM4_DEFAULT_KEY_FILE}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:13
 */
public class Sm4EncryptService extends AbstractCbcEncrypt {

    private static final String PAYLOAD_ALGORITHM = "SM4-GCM";

    private static final String GCM_TRANSFORMATION = "SM4/GCM/NoPadding";

    private static final String CIPHER_TRANSFORMATION = "SM4/CBC/PKCS5Padding";

    private static final int GCM_IV_LENGTH_BYTES = 12;

    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final byte[] LEGACY_IV = "PEACHSM4IV202503".getBytes(StandardCharsets.UTF_8);

    static {
        BouncyCastleProviderSupport.providerName();
    }

    public Sm4EncryptService(String algorithm) {
        super(EncryptKeyProfile.SM4, algorithm);
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
        Cipher cipher = Cipher.getInstance(GCM_TRANSFORMATION, BouncyCastleProviderSupport.providerName());
        cipher.init(mode, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher;
    }

    @Override
    protected Cipher createLegacyCipher(int mode) throws GeneralSecurityException {
        SecretKeySpec secretKey = new SecretKeySpec(resolveKeyBytes(), keyAlgorithm());
        Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION, BouncyCastleProviderSupport.providerName());
        cipher.init(mode, secretKey, new IvParameterSpec(LEGACY_IV));
        return cipher;
    }
}
