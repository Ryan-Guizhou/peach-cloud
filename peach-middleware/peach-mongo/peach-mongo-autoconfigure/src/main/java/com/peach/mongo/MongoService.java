package com.peach.mongo;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageInfo;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
/**
 * Mongo服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/13 17:01
 */
@Slf4j
public class MongoService<T> implements IMongoService<T>{

    private static final int DEFAULT_MAX_MATERIALIZED_DOCUMENTS = 500;

    private final MongoTemplate mongoTemplate;
    private final int maxListSize;
    private final int maxPageSize;

    public MongoService(MongoTemplate mongoTemplate) {
        this(mongoTemplate, DEFAULT_MAX_MATERIALIZED_DOCUMENTS, DEFAULT_MAX_MATERIALIZED_DOCUMENTS);
    }

    /**
     * Spring 配置的查询物化上限，拒绝危险的大列表而不静默截断。
     */
    public MongoService(MongoTemplate mongoTemplate, PeachMongoProperties properties) {
        this(mongoTemplate, properties.getMaxListSize(), properties.getMaxPageSize());
    }

    private MongoService(MongoTemplate mongoTemplate, int maxListSize, int maxPageSize) {
        if (maxListSize < 1 || maxListSize > 100_000
                || maxPageSize < 1 || maxPageSize > 100_000) {
            throw new IllegalArgumentException("Mongo query materialization limits must be 1..100000");
        }
        this.mongoTemplate = Objects.requireNonNull(mongoTemplate, "mongoTemplate");
        this.maxListSize = maxListSize;
        this.maxPageSize = maxPageSize;
    }


    @Override
    public MongoCollection<Document> getCollection(String collectionName) {
        return mongoTemplate.getCollection(collectionName);
    }

    @Override
    public Set<String> collectionNames() {
        Set<String> collectionNames = mongoTemplate.getCollectionNames();
        return CollectionUtils.isEmpty(collectionNames) ? Set.of() : collectionNames;
    }

    @Override
    public boolean collectionExists(String collectionName) {
        return mongoTemplate.collectionExists(collectionName);
    }

    @Override
    public MongoCollection<Document> createCollection(String collectionName) {
        return mongoTemplate.createCollection(collectionName);
    }

    @Override
    public void dropCollection(String collectionName) {
        mongoTemplate.dropCollection(collectionName);
    }

    @Override
    public void createIndex(String collectionName, Document document) {
        getCollection(collectionName).createIndex(document);
    }

    @Override
    public List<Document> findList(String collectionName, Document query, Document sort, Document projection, Integer skip, Integer limit) {
        if (skip != null && skip < 0) {
            throw new IllegalArgumentException("Mongo list skip must be nonnegative");
        }
        if (limit != null && (limit < 1 || limit > maxListSize)) {
            throw new IllegalArgumentException("Mongo list limit must be between 1 and " + maxListSize);
        }
        List<Document> documents;
        try {
            Document finalQuery = query == null ? new Document() : query;
            FindIterable<Document> findIterable = getCollection(collectionName).find(finalQuery);
            if (sort != null && !sort.isEmpty()){
                findIterable = findIterable.sort(sort);
            }
            if (projection != null && !projection.isEmpty()){
                findIterable = findIterable.projection(projection);
            }
            if (skip != null && skip > 0){
                findIterable = findIterable.skip(skip);
            }
            findIterable = findIterable.limit(limit == null ? maxListSize + 1 : limit);
            documents = findIterable.into(new ArrayList<>());
        } catch (Exception ex) {
            throw new IllegalStateException("Mongo list query failed for collection: " + collectionName, ex);
        }
        if (limit == null && documents.size() > maxListSize) {
            throw new IllegalStateException(
                    "Mongo list exceeds max-list-size; use findPage or cursor-based iteration");
        }
        return documents;
    }

    @Override
    public List<Document> findList(String collectionName, Document query) {
        return findList(collectionName, query, null, null, null, null);
    }

    @Override
    public List<Document> findList(String collectionName, Document query, Document sort) {
        return findList(collectionName, query, sort, null, null, null);
    }

    @Override
    public List<Document> findList(String collectionName, Document query, Document sort, Document projection) {
        return findList(collectionName, query, sort, projection, null, null);
    }

    @Override
    public List<Document> findList(String collectionName) {
        return findList(collectionName, null, null, null, null, null);
    }

    @Override
    public long count(String collectionName) {
        return count(collectionName, null);
    }

    @Override
    public long count(String collectionName, Document query) {
        Document finalQuery = query == null ? new Document() : query;
        return getCollection(collectionName).countDocuments(finalQuery);
    }

    @Override
    public PageInfo<Document> findPage(String collectionName, Document query, Document sort, Document projection,
                                       Integer pageNum, Integer pageSize) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1
                || pageSize > maxPageSize) {
            throw new IllegalArgumentException(
                    "Mongo pagination requires positive pageNum and pageSize <= " + maxPageSize);
        }
        long offset = ((long) pageNum - 1L) * pageSize;
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Mongo pagination offset exceeds supported range");
        }
        try {
            MongoCollection<Document> collection = getCollection(collectionName);
            Document filter = query == null ? new Document() : query;
            long total = collection.countDocuments(filter);
            long pageCount = total / pageSize + (total % pageSize == 0L ? 0L : 1L);
            if (pageCount > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Mongo page count exceeds supported range");
            }

            PageInfo<Document> page = new PageInfo<>();
            page.setPageNum(pageNum);
            page.setPageSize(pageSize);
            page.setTotal(total);
            page.setPages((int) pageCount);
            if (total == 0L || offset >= total) {
                page.setList(List.of());
                return page;
            }
            FindIterable<Document> iterable = collection.find(filter);
            if (sort != null && !sort.isEmpty()) {
                iterable = iterable.sort(sort);
            }
            if (projection != null && !projection.isEmpty()) {
                iterable = iterable.projection(projection);
            }
            page.setList(iterable.skip((int) offset).limit(pageSize).into(new ArrayList<>()));
            return page;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Mongo page query failed for collection: " + collectionName, ex);
        }
    }

    @Override
    public PageInfo<Document> findPage(String collectionName, Document query, Document sort, Integer pageNum, Integer pageSize) {
        return findPage(collectionName, query, sort, null, pageNum, pageSize);
    }

    @Override
    public PageInfo<Document> findPage(String collectionName, Document query, Integer pageNum, Integer pageSize) {
        return findPage(collectionName, query, null, null, pageNum, pageSize);
    }

    @Override
    public PageInfo<Document> findPage(String collectionName, Integer pageNum, Integer pageSize) {
        return findPage(collectionName, null, null, null, pageNum, pageSize);
    }

    @Override
    public PageInfo<T> findPage(String collectionName, Document query, Document sort, Document projection, Integer pageNum, Integer pageSize, Class<T> clazz) {
        PageInfo<Document> pageInfo = findPage(collectionName, query, sort, projection, pageNum, pageSize);
        PageInfo<T> mappedPage = new PageInfo<>();
        mappedPage.setPageNum(pageInfo.getPageNum());
        mappedPage.setPageSize(pageInfo.getPageSize());
        mappedPage.setTotal(pageInfo.getTotal());
        mappedPage.setPages(pageInfo.getPages());
        mappedPage.setList(pageInfo.getList().stream()
                .map(Document::toJson)
                .map(json -> JSON.parseObject(json, clazz))
                .toList());
        return mappedPage;
    }

    @Override
    public PageInfo<T> findPage(String collectionName, Document query, Document sort, Integer pageNum, Integer pageSize, Class<T> clazz) {
        return findPage(collectionName, query, sort, null, pageNum, pageSize, clazz);
    }

    @Override
    public PageInfo<T> findPage(String collectionName, Document query, Integer pageNum, Integer pageSize, Class<T> clazz) {
        return findPage(collectionName, query, null, null, pageNum, pageSize, clazz);
    }

    @Override
    public PageInfo<T> findPage(String collectionName, Integer pageNum, Integer pageSize, Class<T> clazz) {
        return findPage(collectionName, null, null, null, pageNum, pageSize, clazz);
    }

    @Override
    public boolean insertOne(String collectionName, Document document) {
        try {
            getCollection(collectionName).insertOne(document);
            return true;
        }catch (Exception e){
            log.error("MongoService.insert() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean insertMany(String collectionName, List<Document> documents) {
        try {
            getCollection(collectionName).insertMany(documents);
            return true;
        }catch (Exception e){
            log.error("MongoService.insertMany() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean deleteOne(String collectionName, Document query) {
        try {
            getCollection(collectionName).deleteOne(query);
            return true;
        }catch (Exception e){
            log.error("MongoService.deleteOne() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean deleteMany(String collectionName, Document query) {
        try {
            getCollection(collectionName).deleteMany(query);
            return true;
        }catch (Exception e){
            log.error("MongoService.deleteMany() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean updateOne(String collectionName, Document query, Document update) {
        try {
            getCollection(collectionName).updateOne(query, update);
            return true;
        }catch (Exception e){
            log.error("MongoService.updateOne() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

    @Override
    public boolean updateMany(String collectionName, Document query, Document update) {
        try {
            getCollection(collectionName).updateMany(query, update);
            return true;
        }catch (Exception e){
            log.error("MongoService.updateMany() failed, collection={}, cause={}", collectionName, e.getClass().getSimpleName());
            return false;
        }
    }

}
