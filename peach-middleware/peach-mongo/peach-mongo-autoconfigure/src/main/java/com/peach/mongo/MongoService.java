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

    private final MongoTemplate mongoTemplate;

    public MongoService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
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
        List<Document> documentList = new ArrayList<>();
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
            if (limit != null && limit > 0){
                findIterable = findIterable.limit(limit);
            }
            documentList = findIterable.into(new ArrayList<>());
            return documentList;
        }catch (Exception e){
            throw new IllegalStateException("Mongo list query failed for collection: " + collectionName, e);
        }
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
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1) {
            throw new IllegalArgumentException("Mongo pagination requires positive pageNum and pageSize");
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
