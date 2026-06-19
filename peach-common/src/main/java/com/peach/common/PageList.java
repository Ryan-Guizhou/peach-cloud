package com.peach.common;

import java.io.Serializable;
import java.util.List;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/4/5 10:52
 */
public class PageList<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer pageIndex = 1;

    private Integer pageSize = 20;

    private Integer total = 0;

    public PageList<T> setPageIndex(Integer pageIndex) {
        this.pageIndex = pageIndex;
        return this;
    }

    public PageList<T> setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public PageList<T> setTotal(Integer total) {
        this.total = total;
        return this;
    }

    public List<T> getPageList(List<T> list){
        if (list == null){
            return list;
        }
        List<T> newList= null;
        total = list.size();
        newList = list.subList(pageSize * (pageIndex-1), ((pageSize * pageIndex) > total ? total : (pageSize * pageIndex)));
        return newList;
    }
}
