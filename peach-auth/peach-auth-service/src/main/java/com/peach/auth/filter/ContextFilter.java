package com.peach.auth.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.peach.common.CurrentContext;
import com.peach.common.CurrentContextEntity;
import com.peach.common.CurrentUserDO;
import com.peach.common.util.StringUtil;
import com.peach.auth.service.IUserService;
import com.peach.userservice.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/29 11:14
 */
@Slf4j
public class ContextFilter extends OncePerRequestFilter implements Ordered {

    private static final List<String> EFFECTIVE_LANGUAGE = Arrays.asList("zh","en","ko");

    public static Cache<String, CurrentUserDO> USER_CACHE = CacheBuilder.newBuilder().maximumSize(10000).weakValues().weakKeys().expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String language = getLanguage(request);
        String requestPath = request.getRequestURI();
        String ticket = StringUtil.EMPTY;
        try {
            ticket = StpUtil.getTokenValue();
        }catch (Exception e){
            log.error("");
        }
        if (StringUtil.isBlank(ticket)){
            ticket = getAuthrorization(request);
        }
        CurrentContextEntity currentContextEntity = new CurrentContextEntity();

        if (!FilterPathUtil.CHECK_HEALTH_PATH.equals(requestPath) && !FilterPathUtil.EXCLUDE_PATH.contains(requestPath)){

            Object objectId = null;
            try {
                if (StringUtil.isNotBlank( ticket)){
                    objectId = StpUtil.getLoginId(ticket);
                }else {
                    objectId = StpUtil.getLoginId();
                }
                if (StpUtil.isLogin(objectId)){
                    String userId = StringUtil.getStringValue(objectId);
                    CurrentUserDO currentUserDO = USER_CACHE.get(userId,()->{
                        IUserService userService = SpringUtil.getBean(IUserService.class);
                        UserVO userVO = userService.selectUserById(userId);
                        CurrentUserDO userInfo = new CurrentUserDO();
                        if (userVO != null){
                            userInfo.setUserId(userVO.getUserId());
                        }
                        return userInfo;
                    });
                    currentUserDO.setLanguage(language);
                    currentContextEntity.setCurrentUserDO(currentUserDO);
                }else {
                    log.error("");
                }

            }catch (Exception e){
                log.error("");
            }
            currentContextEntity.setLanguage(language);
            CurrentContext.setCurrentContext(currentContextEntity);
        }
        try {
            filterChain.doFilter(request, response);
        }catch (Exception e){
            log.error("");
        }finally {
            CurrentContext.clear();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        log.info("ContextFilter has been destroyed");
    }

    /**
     * 从请求头中获取token / Get token from request header
     * @param request HttpServletRequest
     * @return
     */
    private String getAuthrorization(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        return authorization;
    }

    /**
     * 从请求头中解析语言 / Parse the language from the request header
     * @param request HttpServletRequest
     * @return
     */
    private String getLanguage(HttpServletRequest request){
        String language = request.getHeader("language");
        if (StringUtil.isBlank(language)){
            language = (String) request.getAttribute("language");
        }
        if (StringUtil.isBlank(language)){
            language = request.getHeader("Language");
        }
        if (StringUtil.isBlank(language)){
            language = request.getHeader("Accept-Language");
        }
        String lang = StringUtil.EMPTY;
        if (StringUtil.isBlank(language)){
            lang = "zh";
        }else {
            try {
                lang = language.split(";")[0].trim().toLowerCase();
                lang = lang.split(",")[1];
                if (!EFFECTIVE_LANGUAGE.contains(lang)) {
                    return "zh";
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return lang;
    }
}
