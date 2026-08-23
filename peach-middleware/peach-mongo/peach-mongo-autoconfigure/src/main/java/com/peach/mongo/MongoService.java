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
        log.info("MongoService.findList collectionName: {}, query: {}, sort: {}, projection: {}, skip: {}, limit: {}", collectionName, query, sort, projection, skip, limit);
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
            log.error("MongoService.findList() error: {}", e.getMessage(), e);
            return documentList;
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
        log.info("MongoService.count() collectionName: {}, query: {}", collectionName, query);
        Document finalQuery = query == null ? new Document() : query;
        return getCollection(collectionName).countDocuments(finalQuery);
    }

    @Override
    public PageInfo<Document> findPage(String collectionName, Document query, Document sort, Document projection, Integer pageNum, Integer pageSize) {
        log.info("MongoService.findPage() collectionName: {}, query: {}, sort: {}, projection: {}, pageNum: {}, pageSize: {}", collectionName, query, sort, projection, pageNum, pageSize);
        PageInfo<Document> pageInfo = new PageInfo<>();
        try {
            MongoCollection<Document> mongoCollection = getCollection(collectionName);
            pageInfo.setPageNum(pageNum);
            pageInfo.setPageSize(pageSize);
            int total = (query == null || query.isEmpty()) ? (int)mongoCollection.estimatedDocumentCount() :(int)mongoCollection.countDocuments(query);
            pageInfo.setTotal(total);

            if (total == 0){
                pageInfo.setList(List.of());
                return pageInfo;
            }
            int pages = total / pageSize;
            if (total % pageSize > 0) {
                pages++;
            }
            pageInfo.setPages(pages);
            FindIterable<Document> findIterable = mongoCollection.find(query);
            if (sort != null && !sort.isEmpty()){
                findIterable = findIterable.sort(sort);
            }
            if (projection != null && !projection.isEmpty()){
                findIterable = findIterable.projection(projection);
            }
            findIterable = findIterable.skip((pageNum - 1) * pageSize).limit(pageSize);
            List<Document> list = findIterable.into(new ArrayList<>());
            pageInfo.setList(list);
            return pageInfo;
        }catch (Exception e){
            log.error("MongoService.findPage() error: {}", e.getMessage(), e);
            return pageInfo;
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
        return PageInfo.of(pageInfo.getList().stream().map(document -> document.toJson()).map(json -> JSON.parseObject(json, clazz)).toList(), pageInfo.getNavigatePages());
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
            log.error("MongoService.insert() collectionName: {}, document: {}", collectionName, document, e);
            return false;
        }
    }

    @Override
    public boolean insertMany(String collectionName, List<Document> documents) {
        try {
            getCollection(collectionName).insertMany(documents);
            return true;
        }catch (Exception e){
            log.error("MongoService.insertMany() collectionName: {}, documents: {}", collectionName, documents, e);
            return false;
        }
    }

    @Override
    public boolean deleteOne(String collectionName, Document query) {
        try {
            getCollection(collectionName).deleteOne(query);
            return true;
        }catch (Exception e){
            log.error("MongoService.deleteOne() collectionName: {}, query: {}", collectionName, query, e);
            return false;
        }
    }

    @Override
    public boolean deleteMany(String collectionName, Document query) {
        try {
            getCollection(collectionName).deleteMany(query);
            return true;
        }catch (Exception e){
            log.error("MongoService.deleteMany() collectionName: {}, query: {}", collectionName, query, e);
            return false;
        }
    }

    @Override
    public boolean updateOne(String collectionName, Document query, Document update) {
        try {
            getCollection(collectionName).updateOne(query, update);
            return true;
        }catch (Exception e){
            log.error("MongoService.updateOne() collectionName: {}, query: {}, update: {}", collectionName, query, update, e);
            return false;
        }
    }

    @Override
    public boolean updateMany(String collectionName, Document query, Document update) {
        try {
            getCollection(collectionName).updateMany(query, update);
            return true;
        }catch (Exception e){
            log.error("MongoService.updateMany() collectionName: {}, query: {}, update: {}", collectionName, query, update, e);
            return false;
        }
    }

}
