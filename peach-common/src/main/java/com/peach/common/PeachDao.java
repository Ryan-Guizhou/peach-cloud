package com.peach.common;

import java.util.List;

/**
 * Peach数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/17 16:21
 */
public interface PeachDao<T,E> {

    /**
     * 批量更新
     */
    void batchUpdate(List<T> list);

    /**
     * 插入
     */
    void insert(T t);

    /**
     * 批量插入
     */
    void batchInsert(List<T> list);

    /**
     * 更新
     */
    void update(T t);

    /**
     * 根据ID根据数据
     */
    void updateById(T t);

    /**
     * 删除
     */
    void del(T t);

    /***
     * 根据ID删除
     */
    void delById(String id);

    /**
     * 根据ID批量删除
     * @param ids
     */
    void delByIds(List<String> ids);

    /**
     * 查询数量
     */
    int count(T t);

    /**
     * 根据ID查询
     */
    E selectById(String id);

    /**
     * 根据ID批量查询
     */
    List<E> selectByIds(List<String> ids);

    /**
     * 查询
     */
    List<E> select(T t);
}
