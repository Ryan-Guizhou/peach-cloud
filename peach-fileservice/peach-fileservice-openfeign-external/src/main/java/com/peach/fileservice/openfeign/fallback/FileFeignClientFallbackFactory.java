package com.peach.fileservice.openfeign.fallback;

import com.peach.common.constant.ServiceContextConstant;
import com.peach.common.response.Response;
import com.peach.fileservice.openfeign.FileFeignClient;
import com.peach.openfeign.support.PeachFeignFallbackSupport;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/1 15:00
 * @Description 文件服务Feign降级工厂
 */
public class FileFeignClientFallbackFactory implements FallbackFactory<FileFeignClient> {

    private final PeachFeignFallbackSupport fallbackSupport;

    public FileFeignClientFallbackFactory(PeachFeignFallbackSupport fallbackSupport) {
        this.fallbackSupport = fallbackSupport;
    }

    @Override
    public FileFeignClient create(Throwable cause) {
        return new FileFeignClient() {
            @Override
            public Response upload(MultipartFile file, String bizType, String bizId, String bizTag, String displayName,
                                   String contentType, String remark, String storageProvider) {
                return fallbackSupport.fail(ServiceContextConstant.FILE_SERVICE_CONTEXT, "upload", cause);
            }

            @Override
            public Response sha256(MultipartFile file) {
                return fallbackSupport.fail(ServiceContextConstant.FILE_SERVICE_CONTEXT, "sha256", cause);
            }

            @Override
            public Response detail(String fileId) {
                return fallbackSupport.fail(ServiceContextConstant.FILE_SERVICE_CONTEXT, "detail", cause);
            }

            @Override
            public Response getUrl(String fileId) {
                return fallbackSupport.fail(ServiceContextConstant.FILE_SERVICE_CONTEXT, "getUrl", cause);
            }

            @Override
            public Response delete(String fileId) {
                return fallbackSupport.fail(ServiceContextConstant.FILE_SERVICE_CONTEXT, "delete", cause);
            }
        };
    }
}
