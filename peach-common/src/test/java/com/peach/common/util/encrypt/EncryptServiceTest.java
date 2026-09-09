package com.peach.common.util.encrypt;

import com.peach.common.util.encrypt.impl.AesEncryptService;
import com.peach.common.util.encrypt.impl.Sm4EncryptService;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
class EncryptServiceTest {

    @AfterEach
    void clearKeys() {
        System.clearProperty(EncryptConst.AES_KEY_PROPERTY);
        System.clearProperty(EncryptConst.DES_KEY_PROPERTY);
        System.clearProperty(EncryptConst.SM4_KEY_PROPERTY);
    }

    @Test
    void aesEncryptIncludesRandomIvAndDecryptsPayload() throws Exception {
        System.setProperty(EncryptConst.AES_KEY_PROPERTY, "1234567890123456");
        EncryptService service = new AesEncryptService(EncryptConst.AES);

        String first = service.encrypt("same-plain-text");
        String second = service.encrypt("same-plain-text");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).startsWith("v1:AES-GCM:");
        assertThat(service.decrypt(first)).isEqualTo("same-plain-text");
        assertThat(service.decrypt(second)).isEqualTo("same-plain-text");
    }

    @Test
    void sm4EncryptIncludesRandomIvAndDecryptsPayload() throws Exception {
        System.setProperty(EncryptConst.SM4_KEY_PROPERTY, "1234567890123456");
        EncryptService service = new Sm4EncryptService(EncryptConst.SM4);

        String first = service.encrypt("same-plain-text");
        String second = service.encrypt("same-plain-text");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).startsWith("v1:SM4-GCM:");
        assertThat(service.decrypt(first)).isEqualTo("same-plain-text");
        assertThat(service.decrypt(second)).isEqualTo("same-plain-text");
    }

    @Test
    void aesDecryptKeepsLegacyCbcCipherTextReadable() throws Exception {
        System.setProperty(EncryptConst.AES_KEY_PROPERTY, "1234567890123456");
        String legacyCipherText = legacyAesCbcEncrypt("legacy-secret");

        EncryptService service = new AesEncryptService(EncryptConst.AES);

        assertThat(service.decrypt(legacyCipherText)).isEqualTo("legacy-secret");
    }

    @Test
    void missingConfiguredKeyFallsBackToClasspathDefaultKey() {
        System.clearProperty(EncryptConst.AES_KEY_PROPERTY);

        assertThat(EncryptKeyResolver.resolve(EncryptKeyProfile.AES))
                .isEqualTo("PEACH/COMMON/202503/Ryan_Guizhou".getBytes(StandardCharsets.UTF_8));
    }

    private static String legacyAesCbcEncrypt(String plainText) throws Exception {
        byte[] key = "1234567890123456".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "SHA1PRNG20250313".getBytes(StandardCharsets.UTF_8);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, EncryptConst.AES), new IvParameterSpec(iv));
        return EncryptHexCodec.bytesToHex(cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
