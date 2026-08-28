package com.peach.mongo;

import com.github.pagehelper.PageInfo;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.List;
import java.util.Set;

/**
 * IMongo服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/13 16:59
 */
public interface IMongoService<T> {

    /**
     * 获取集合
     * @param collectionName 集合名称
     * @return 集合
     */
    MongoCollection<Document> getCollection(String collectionName);

    /**
     * 获取所有集合名称
     * @return 所有集合名称
     */
    Set<String> collectionNames();

    /**
     * 集合是否存在
     * @param collectionName 集合名称
     * @return 集合是否存在
     */
    boolean collectionExists(String collectionName);

    /**
     * 创建集合
     * @param collectionName 集合名称
     * @return 创建集合
     */
    MongoCollection<Document> createCollection(String collectionName);

    /**
     * 删除集合
     * @param collectionName 集合名称
     */
    void dropCollection(String collectionName);

    /**
     * 创建索引
     * @param collectionName 集合名称
     * @param document 索引文档
     */
    void createIndex(String collectionName, Document document);


    /**
     * 查询集合文档列表
     * @param collectionName 集合名称
     * @param query 查询文档
     * @param sort 排序文档
     * @param projection 投影文档
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 文档列表
     */
    List<Document> findList(String collectionName, Document query,Document sort,
                            Document  projection,Integer skip, Integer limit);

    /**
     * 查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @return 文档列表
     */
    List<Document> findList(String collectionName, Document query);

    /**
     * 筛选条件查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @return 文档列表
     */
    List<Document> findList(String collectionName, Document query,Document sort);

    /**
     * 筛选条件查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @param projection 投影文档
     * @return 文档列表
     */
    List<Document> findList(String collectionName, Document query,Document sort, Document  projection);

    /**
     * 查询集合所有文档列表
     * @param collectionName 集合名称
     * @return 文档列表
     */
    List<Document> findList(String collectionName);

    /**
     * 查询集合文档数量
     * @param collectionName 集合名称
     * @return 文档数量
     */
    long count(String collectionName);

    /**
     * 查询集合文档数量
     * @param collectionName 集合名称
     * @param query 查询文档
     * @return 文档数量
     */
    long count(String collectionName, Document query);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @param projection 投影文档
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 文档列表
     */
    PageInfo<Document> findPage(String collectionName, Document query, Document sort, Document projection, Integer pageNum, Integer pageSize);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 文档列表
     */
    PageInfo<Document> findPage(String collectionName, Document query, Document sort, Integer pageNum, Integer pageSize);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 文档列表
     */
    PageInfo<Document> findPage(String collectionName, Document query, Integer pageNum, Integer pageSize);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 文档列表
     */
    PageInfo<Document> findPage(String collectionName, Integer pageNum, Integer pageSize);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @param projection 投影文档
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param clazz 实体类
     * @return 文档列表
     */
    PageInfo<T> findPage(String collectionName, Document query, Document sort, Document projection, Integer pageNum, Integer pageSize, Class<T> clazz);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param sort 排序文档
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param clazz 实体类
     * @return 文档列表
     */
    PageInfo<T> findPage(String collectionName, Document query, Document sort, Integer pageNum, Integer pageSize, Class<T> clazz);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param clazz 实体类
     * @return 文档列表
     */
    PageInfo<T> findPage(String collectionName, Document query, Integer pageNum, Integer pageSize, Class<T> clazz);

    /**
     * 分页查询集合文档列表
     * @param collectionName 集合名称
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param clazz 实体类
     * @return 文档列表
     */
    PageInfo<T> findPage(String collectionName, Integer pageNum, Integer pageSize, Class<T> clazz);

    /**
     * 插入文档
     * @param collectionName 集合名称
     * @param document 文档
     */
    boolean insertOne(String collectionName, Document document);

    /**
     * 批量插入文档
     * @param collectionName 集合名称
     * @param documents 文档列表
     */
    boolean insertMany(String collectionName, List<Document> documents);

    /**
     * 删除文档
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @return 删除数量
     */
    boolean deleteOne(String collectionName, Document query);

    /**
     * 批量删除文档
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @return 删除数量
     */
    boolean deleteMany(String collectionName, Document query);

    /**
     * 更新文档
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param update 更新文档
     * @return 更新数量
     */
    boolean updateOne(String collectionName, Document query, Document update);

    /**
     * 批量更新文档
     * @param collectionName 集合名称
     * @param query 筛选条件
     * @param update 批量更新文档
     * @return 更新数量
     */
    boolean updateMany(String collectionName, Document query, Document update);
}
