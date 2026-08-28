package com.peach.request;

/**
 * MoveObjectRequest相关类。
 * <p>通过 `recursive` 区分文件移动和目录移动。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/17 16:20
 */
public class MoveObjectRequest extends StorageTransferRequest {

    private MoveObjectRequest(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends StorageTransferRequest.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        @Override
        public MoveObjectRequest build() {
            validate();
            return new MoveObjectRequest(this);
        }
    }
}
