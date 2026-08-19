package com.peach.common.filter;

import com.peach.common.constant.PubCommonConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/2 18:23
 * @Description 请求包装过滤器 / Request wrapper filter
 */
@Slf4j
public abstract class AbstractWrapperFilter extends OncePerRequestFilter {

    /**
     * 抽象过滤器 / Abstract filter
     * @param request 请求 / request
     * @param response 响应 / response
     * @param filterChain 过滤器链 / filter chain
     */
    @Override
    protected final void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        HttpServletRequest wrapped = request;
        if (!isMultipart(request)) {
            wrapped = wrapRequestIfNecessary(request);
        }

        doFilterWrapped(wrapped, response, filterChain);
    }

    private static final String MULTIPART_SUFFIX = "multipart/";

    /**
     * 子类只关心：已经是可重复读的 request / The subclass only cares about requests that are already repeatable read
     * @param request 请求 / request
     * @param response 响应 / response
     * @param filterChain 过滤器链 / filter chain
     */
    protected abstract void doFilterWrapped(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException;


    /**
     * 判断是否是 multipart / Determine whether it is a multipart
     * @param request 请求 / request
     * @return 是否是 multipart /
     */
    protected boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MULTIPART_SUFFIX);
    }

    /**
     * 必要时包装请求 / wrapRequestIfNecessary
     * @param request 请求 / request
     * @return 包装后的请求 /  wrapped request
     */
    protected HttpServletRequest wrapRequestIfNecessary(HttpServletRequest request) {
        if (request instanceof RepeatedlyRequesWrapper) {
            return request;
        }
        return new RepeatedlyRequesWrapper(request);
    }

    /**
     * 重复读取请求体 / Repeatedly read request body
     */
    static class RepeatedlyRequesWrapper extends HttpServletRequestWrapper {

        private final byte[] body;

        private static final int MAX_BODY_SIZE = 1024 * 1024; // 1MB


        public RepeatedlyRequesWrapper(HttpServletRequest request) {
            super(request);
            byte[] tempBody;
            try (InputStream inputStream = request.getInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int len;
                int total = 0;

                while ((len = inputStream.read(buffer)) != -1) {
                    total += len;
                    if (total > MAX_BODY_SIZE) {
                        String message = "Request body too large, max " + MAX_BODY_SIZE + " bytes";
                        log.error(message);
                        throw new ServletException(message);
                    }
                    baos.write(buffer, 0, len);
                }
                tempBody = baos.toByteArray();
            } catch (IOException | ServletException e) {
                log.error("Failed to read request body", e);
                tempBody = new byte[0];
            }
            this.body = tempBody;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() throws IOException {
                    return byteArrayInputStream.read();
                }
            };

        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream(), getCharacterEncodingOrDefault()));
        }

        private String getCharacterEncodingOrDefault() {
            String encoding = getCharacterEncoding();
            return encoding != null ? encoding : PubCommonConst.UTF_8;
        }

        public byte[] getBody() {
            return body.clone();
        }
    }
}
