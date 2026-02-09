package com.peach.setting.service;

import com.peach.redis.manager.MultiCacheManager;
import com.peach.setting.service.impl.IFrontMutiLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;


/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/9 18:33
 */
@Slf4j
@Indexed
@Service
public class FrontMutiLanguageImpl implements IFrontMutiLanguage {

    @Autowired
    private MultiCacheManager cacheManager;

    @Override
    public void init() {

    }
}
