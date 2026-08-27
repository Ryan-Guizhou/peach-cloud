package com.peach.request;

/**
 * 拷贝对象请求。
 *
 * <p>通过 `recursive` 区分文件拷贝和目录拷贝。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/17 16:20
 */
public class CopyObjectRequest extends StorageTransferRequest {

    private CopyObjectRequest(Builder builder) {
        super(builder);
    }

    /**
     * 创建拷贝对象请求构造器。
     *
     * @return 构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends StorageTransferRequest.Builder<Builder> {
        @Override
        protected Builder self() { return this; }
        @Override
        public CopyObjectRequest build() { validate(); return new CopyObjectRequest(this); }
    }
}