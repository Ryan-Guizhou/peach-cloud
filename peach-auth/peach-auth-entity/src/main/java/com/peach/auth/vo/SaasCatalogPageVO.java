package com.peach.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

/**
 * 不依赖跨租户 COUNT 查询的有界 SaaS 目录分页结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 15:30
 */
@Getter
@Schema(description = "受信目录分页视图")
public class SaasCatalogPageVO {

    private final int page;
    private final int pageSize;
    private final boolean hasMore;
    private final List<SaasCatalogEntryVO> items;

    public SaasCatalogPageVO(int page, int pageSize, boolean hasMore, List<SaasCatalogEntryVO> items) {
        this.page = page;
        this.pageSize = pageSize;
        this.hasMore = hasMore;
        this.items = List.copyOf(items);
    }
}
