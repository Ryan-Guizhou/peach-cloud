package com.peach.mongo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.util.Assert;
import java.util.concurrent.TimeUnit;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/13 17:01
 */
@AutoConfiguration
@EnableConfigurationProperties(PeachMongoProperties.class)
public class MongoAutoConfigure {

    @Bean
    @ConditionalOnMissingBean(MongoDatabaseFactory.class)
    public MongoDatabaseFactory mongoDatabaseFactory(
            MongoClient mongoClient,
            PeachMongoProperties properties) {

        ConnectionString cs = new ConnectionString(properties.getUri());

        String database = cs.getDatabase();
        if (database == null || database.isEmpty()) {
            database = properties.getDatabase();
        }

        Assert.hasText(database, "Mongo database name must not be empty");
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean(MongoClient.class)
    public MongoClient mongoClient(PeachMongoProperties properties) {
        ConnectionString cs = new ConnectionString(properties.getUri());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(cs)
                .applyToConnectionPoolSettings(builder -> {
                    builder.maxSize(properties.getPool().getMaxSize());
                    builder.minSize(properties.getPool().getMinSize());
                    builder.maxConnecting(properties.getPool().getMaxConnecting());
                    builder.maxConnectionIdleTime(properties.getPool().getMaxConnectionIdleTimeMs(), TimeUnit.MILLISECONDS);
                    builder.maintenanceInitialDelay(properties.getPool().getMaintenanceInitialDelayMs(),TimeUnit.MILLISECONDS);
                    builder.maintenanceFrequency(properties.getPool().getMaintenanceFrequencyMs(),TimeUnit.MILLISECONDS);
                })
                .applyToSocketSettings(builder -> {
                    builder.connectTimeout(properties.getSocket().getConnectTimeoutMs(),TimeUnit.MILLISECONDS);
                    builder.readTimeout(properties.getSocket().getReadTimeoutMs(),TimeUnit.MILLISECONDS);
                })
                .applyToClusterSettings(builder ->
                        builder.serverSelectionTimeout(properties.getCluster().getServerSelectionTimeoutMs(),TimeUnit.MILLISECONDS))
                .retryReads(properties.isRetryReads())
                .retryWrites(properties.isRetryWrites())
                .build();

        return MongoClients.create(settings);
    }


    @Bean
    @ConditionalOnMissingBean(MongoTemplate.class)
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory, PeachMongoProperties peachMongoProperties){
        if (peachMongoProperties.getTemplate().isRemoveClassField()){
            DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
            MongoMappingContext mappingContext = new MongoMappingContext();
            MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mappingContext);
            converter.setTypeMapper(new DefaultMongoTypeMapper(null));
            converter.afterPropertiesSet();
            return new MongoTemplate(mongoDatabaseFactory, converter);
        }
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean
    @ConditionalOnMissingBean(IMongoService.class)
    public IMongoService mongoService(MongoTemplate mongoTemplate){
        return new MongoService(mongoTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "peach.mongo.transaction", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(MongoTransactionManager.class)
    public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory mongoDatabaseFactory){
        return new MongoTransactionManager(mongoDatabaseFactory);
    }
}
