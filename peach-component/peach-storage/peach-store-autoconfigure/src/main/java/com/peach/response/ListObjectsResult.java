package com.peach.response;

import java.util.ArrayList;
import java.util.List;

/**
 * ListObjects结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public class ListObjectsResult {

    private final String providerName;

    private final String bucketName;

    private final String prefix;

    private final List<ObjectInfo> items;

    private final String nextContinuationToken;

    private final boolean truncated;

    private final List<String> commonPrefixes;

    private ListObjectsResult(Builder builder) {
        this.providerName = builder.providerName;
        this.bucketName = builder.bucketName;
        this.prefix = builder.prefix;
        this.items = List.copyOf(builder.items);
        this.nextContinuationToken = builder.nextContinuationToken;
        this.truncated = builder.truncated;
        this.commonPrefixes = List.copyOf(builder.commonPrefixes);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getProviderName() {
        return providerName;
    }

    public String getBucketName() {
        return bucketName;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<ObjectInfo> getItems() {
        return items;
    }

    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public List<String> getCommonPrefixes() {
        return commonPrefixes;
    }

    /**
     * 构建器。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

    public static class Builder {

        private String providerName;
        private String bucketName;
        private String prefix;
        private List<ObjectInfo> items = new ArrayList<>();
        private String nextContinuationToken;
        private boolean truncated;
        private List<String> commonPrefixes = new ArrayList<>();

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder items(List<ObjectInfo> items) {
            this.items = items == null ? new ArrayList<ObjectInfo>() : new ArrayList<>(items);
            return this;
        }

        public Builder nextContinuationToken(String nextContinuationToken) {
            this.nextContinuationToken = nextContinuationToken;
            return this;
        }

        public Builder truncated(boolean truncated) {
            this.truncated = truncated;
            return this;
        }

        public Builder commonPrefixes(List<String> commonPrefixes) {
            this.commonPrefixes = commonPrefixes == null
                    ? new ArrayList<String>() : new ArrayList<>(commonPrefixes);
            return this;
        }

        public ListObjectsResult build() {
            return new ListObjectsResult(this);
        }
    }
}