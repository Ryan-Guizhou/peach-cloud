package com.peach.fileservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 云存储对象列表浏览请求参数
 * 用于前端向服务端发起文件/目录列表查询的请求数据传输对象
 *
 * @author your-name
 * @date 2026-07-09
 */
@Data
@Schema(description = "云存储浏览器列表请求参数")
public class CloudStorageListDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 相对路径
     * 表示当前要浏览的目录路径，相对于存储桶根目录
     * 为空或null时表示浏览根目录
     * 格式示例：""（根目录）、"images/"、"images/2026/"
     * 注意：路径应以"/"结尾表示目录，否则可能被视为文件前缀
     */
    @Schema(description = "相对路径（相对于存储桶根目录）")
    private String path;

    /**
     * 是否递归查询
     * true-递归列出该路径下所有子目录和文件（深度遍历）
     * false-仅列出当前目录下的直接子项（平铺展示）
     * 递归查询可能导致大量数据返回，建议配合分页参数使用
     */
    @Schema(description = "是否递归查询（true：递归所有子目录；false：仅查询当前目录）")
    private Boolean recursive;

    /**
     * 单次返回的最大对象数量
     * 用于控制分页大小，限制单次查询返回的记录数
     * 默认值通常由服务端配置（如：100、500、1000）
     * 取值范围建议：1-1000，超过限制时服务端可能进行截断
     * 对应云存储SDK中的MaxKeys参数
     */
    @Schema(description = "单次返回最大对象数量（分页大小，建议1-1000）")
    private Integer maxKeys;

    /**
     * 分页续传令牌
     * 用于获取下一页数据，当上一页返回结果中存在truncated=true时，
     * 使用返回的nextContinuationToken作为此参数进行下一页查询
     * 首次请求时不传或传null
     * 对应云存储SDK中的ContinuationToken或Marker参数
     */
    @Schema(description = "分页续传令牌（首次请求不传，后续使用响应中的nextContinuationToken）")
    private String continuationToken;
}