package com.peach.scheduled.qo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 调度任务列表查询对象。
 *
 * <p>分页偏移量在 Java 中计算并通过 MyBatis 参数绑定，禁止使用原始 SQL 字符串替换。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Data
@Schema(description = "调度任务列表查询条件")
public class SchedulerJobQO {

    /** 页码，从 1 开始。 */
    @Schema(description = "页码，从 1 开始")
    private Integer pageNum = 1;

    /** 每页数量。 */
    @Schema(description = "每页数量")
    private Integer pageSize = 20;

    /** 任务编码或任务名称模糊查询关键字。 */
    @Schema(description = "任务编码或任务名称模糊查询关键字")
    private String keyword;

    /** 目标业务应用名称。 */
    @Schema(description = "目标业务应用名称")
    private String applicationName;

    /** 任务生命周期状态。 */
    @Schema(description = "任务生命周期状态")
    private String state;

    /**
     * 获取经过边界修正后的 SQL 分页偏移量。
     *
     * @return 从 0 开始的分页偏移量
     */
    public int getOffset() {
        int safePageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        return (safePageNum - 1) * getSafePageSize();
    }

    /**
     * 获取经过边界修正后的每页数量。
     *
     * @return 1 到 200 之间的每页数量
     */
    public int getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 200);
    }
}
