package com.peach.request;

/**
 * 查询对象元信息请求。
 *
 * <p>该请求仅表达“读取对象元信息”，不包含下载语义，适合作为统一的 `head/stat` 查询模型。</p>
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/18 14:15
 */
public class HeadObjectRequest extends StorageObjectRequest {

    private HeadObjectRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public HeadObjectRequest build() {
            validate();
            return new HeadObjectRequest(this);
        }
    }
}