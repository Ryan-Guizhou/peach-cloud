package com.peach.rocket.security;

import com.peach.rocket.core.MqMessageEnvelope;
import com.peach.rocket.exception.MqException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.TreeMap;

/**
 * 使用长度前缀和稳定字段顺序构造信封 AAD；不依赖 JSON 属性顺序及本机时区。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
public final class MqEnvelopeAuthentication {
    private MqEnvelopeAuthentication() { }

    /** 所有身份、路由和 Header 均绑定至密文；null 与空字符串编码不同。 */
    public static byte[] associatedData(MqMessageEnvelope<?> envelope, String algorithm, String keyId) {
        try {
            var bytes = new ByteArrayOutputStream();
            var output = new DataOutputStream(bytes);
            field(output, "PEACH-MQ-AAD-1");
            field(output, algorithm);
            field(output, keyId);
            field(output, envelope.messageId());
            field(output, envelope.topic());
            field(output, envelope.tag());
            field(output, envelope.key());
            field(output, envelope.producerApp());
            field(output, envelope.payloadType());
            output.writeInt(envelope.version());
            field(output, envelope.createdAt() == null ? null : envelope.createdAt().toString());
            var headers = new TreeMap<>(envelope.headers());
            output.writeInt(headers.size());
            for (var header : headers.entrySet()) {
                field(output, header.getKey());
                field(output, header.getValue());
            }
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new MqException("Failed to authenticate MQ envelope", ex);
        }
    }

    private static void field(DataOutputStream output, String value) throws IOException {
        if (value == null) {
            output.writeInt(-1);
        } else {
            byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
            output.writeInt(encoded.length);
            output.write(encoded);
        }
    }
}
