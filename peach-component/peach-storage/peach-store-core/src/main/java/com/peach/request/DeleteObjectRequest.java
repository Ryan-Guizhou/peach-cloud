package com.peach.request;


/**
 * 删除对象请求。
 *
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class DeleteObjectRequest extends StorageObjectRequest {

    private DeleteObjectRequest(Builder builder) {
        super(builder);
    }

    /**
     * 创建删除对象请求构造器。
     *
     * @return 构造器
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends StorageObjectRequest.Builder<Builder> {

        @Override
        protected Builder self() {
            return this;
        }

        public DeleteObjectRequest build() {
            validate();
            return new DeleteObjectRequest(this);
        }
    }
}