package com.peach.common;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/4/5 10:59
 */
public class PageResult<T> {

    private Long total;

    private Long topTotal;

    private List<T> result = new ArrayList<>();

    public PageResult<T> setTotal(Long t) {
        this.total = t;
        return this;
    }

    public PageResult<T> setResult(List<T> page) {
        this.result = page;
        return this;
    }

    public PageResult<T> setTopTotal(Long topTotal) {
        this.topTotal = topTotal;
        return this;
    }

    public PageResult(List<T> page,Long total){
        this.total = total;
        this.result = page;
    }

    public PageResult(){

    }

    public Long getTotal() {
        return this.total;
    }

    public Long getTopTotal() {
        return this.topTotal;
    }

    public List<T> getResult() {
        return this.result;
    }
}
