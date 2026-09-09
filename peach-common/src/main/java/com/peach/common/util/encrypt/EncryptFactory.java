package com.peach.common.util.encrypt;

/**
 * 加解密服务工厂，通过 SPI 加载 {@link EncryptProvider}。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/30 16:10
 */
public final class EncryptFactory {

    private EncryptFactory() {
        throw new IllegalStateException("Utility class");
    }
    /**
     * 按算法类型获取加解密服务。
     *
     * @param type 算法类型，见 {@link EncryptConst}
     * @return 对应实现
     * @throws IllegalArgumentException 未注册该类型 Provider 时抛出
     */
    public static EncryptService getEncrypt(String type) {
        return EncryptRegister.getProvider(type).getEncrypt();
    }

    /**
     * 本地加解密演示入口，保留用于手动验证 SPI 注册与 default key 兜底策略。
     *
     * @param args 启动参数
     * @throws Exception 加解密失败时抛出
     */
    public static void main(String[] args) throws Exception {

        String str = "123456";
        System.out.println("============ AES EXAMPLE ============");
        EncryptService encrypt = getEncrypt(EncryptConst.AES);
        String aesEncrypt = encrypt.encrypt(str);
        System.out.println(aesEncrypt);
        String aseDescrypt = encrypt.decrypt(aesEncrypt);
        System.out.println(aseDescrypt);

        System.out.println("============ DES EXAMPLE ============");

        EncryptService des = getEncrypt(EncryptConst.DES);
        String desEncrypt = des.encrypt(str);
        System.out.println(desEncrypt);
        String desDecrypt = des.decrypt(desEncrypt);
        System.out.println(desDecrypt);

        System.out.println("============ SM4 EXAMPLE ============");
        EncryptService sm4 = getEncrypt(EncryptConst.SM4);
        String sm4Encrypt = sm4.encrypt(str);
        System.out.println(sm4Encrypt);
        String sm4Decrypt = sm4.decrypt(sm4Encrypt);
        System.out.println(sm4Decrypt);
    }
}
