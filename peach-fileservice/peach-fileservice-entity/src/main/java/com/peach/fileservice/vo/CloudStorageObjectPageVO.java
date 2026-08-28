package com.peach.fileservice.vo;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 云存储浏览器分页结果。
 * 用于展示云存储桶内文件和目录的层级浏览视图，支持分页和目录折叠
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Data
@Schema(description = "云存储浏览器分页结果")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudStorageObjectPageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 7203914688378571066L;

    /**
     * 云存储实例ID
     * 标识当前浏览的是哪个云存储实例（如：阿里云OSS、腾讯云COS、AWS S3等）
     */
    @Schema(description = "实例ID")
    private String instanceId;

    /**
     * 存储桶名称
     * 当前浏览的Bucket名称，不同云厂商的容器名称略有不同
     */
    @Schema(description = "存储桶名称")
    private String bucketName;

    /**
     * 逻辑前缀
     * 用于过滤对象的公共前缀，常用于按目录层级筛选
     */
    @Schema(description = "逻辑前缀")
    private String prefix;

    /**
     * 当前浏览路径
     * 用户当前所在的目录路径，不包含Bucket名称
     * 例如："/images/2026/"
     */
    @Schema(description = "当前路径")
    private String path;

    /**
     * 是否还有下一页
     * true-存在更多数据；false-已加载全部数据
     * 对应云存储SDK中的IsTruncated字段
     */
    @Schema(description = "是否有下一页")
    private Boolean truncated;

    /**
     * 下次分页的令牌
     * 用于分页续传，当truncated为true时此值有效
     * 对应云存储SDK中的NextContinuationToken或NextMarker
     */
    @Schema(description = "下次分页续传令牌")
    private String nextContinuationToken;

    /**
     * 公共前缀列表（虚拟目录）
     * 表示当前路径下的子目录名称列表，仅包含目录名称，不包含文件
     * 例如：["docs/", "images/", "videos/"]
     */
    @Schema(description = "目录列表（公共前缀）")
    private List<String> commonPrefixes = new ArrayList<String>();

    /**
     * 对象节点列表（文件和目录详情）
     * 包含当前路径下的文件和子目录的详细信息（如大小、修改时间、ETag等）
     * 每个元素为CloudStorageObjectNodeVO对象，包含完整的对象元数据
     */
    @Schema(description = "对象节点列表（文件及目录详情）")
    private List<CloudStorageObjectNodeVO> items = new ArrayList<CloudStorageObjectNodeVO>();
}
