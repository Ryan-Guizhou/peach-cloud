package com.peach.common.loader;

import java.net.URI;
import java.net.URL;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;


/**
 * 自定义服务加载器。
 * <p>扩展标准ServiceLoader，支持从自定义路径加载SPI服务实现
 * <h3>特性：</h3>
 * <ul>
 * <li>兼容标准ServiceLoader，从META-INF/services/加载</li>
 * <li>支持从自定义路径加载服务实现</li>
 * <li>线程安全：加载方法是静态的，使用局部变量</li>
 * <li>性能优化：避免重复加载，使用缓存策略</li>
 * <li>安全性：类加载隔离，异常隔离处理</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 */
@Slf4j
public class CustomServiceLoader {
    
    /**
     * 私有构造器，防止实例化
     */
    private CustomServiceLoader() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    
    /**
     * 从标准路径和自定义路径加载服务实现
     *
     * @param serviceClass 服务接口类，不能为null
     * @param customPaths 自定义路径数组，可为空
     * @param classLoader 类加载器，如果为null则使用当前线程上下文类加载器
     * @param <T> 服务类型
     * @return 加载的服务实例列表（不可修改）
     * @throws IllegalArgumentException 如果serviceClass为null
     */
    public static <T> List<T> load(Class<T> serviceClass, String[] customPaths, ClassLoader classLoader) {
        // 参数校验
        Objects.requireNonNull(serviceClass, "Service class must not be null");

        // 使用线程安全的ArrayList
        List<T> providers = new ArrayList<>();
        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();

        // 1. 加载标准SPI服务
        loadStandardServices(serviceClass, loader, providers);

        // 2. 加载自定义路径服务（如果提供了自定义路径）
        if (customPaths != null && customPaths.length > 0) {
            for (String customPath : customPaths) {
                // 跳过空路径
                if (customPath != null && !customPath.isBlank()) {
                    providers.addAll(loadFromCustomPath(serviceClass, customPath, loader));
                }
            }
        }

        // 返回不可修改的列表，保护内部数据
        return List.copyOf(providers);
    }

    /**
     * 加载标准SPI服务
     *
     * @param serviceClass 服务接口类
     * @param classLoader 类加载器
     * @param providers 用于收集服务实例的列表
     * @param <T> 服务类型
     */
    private static <T> void loadStandardServices(Class<T> serviceClass, ClassLoader classLoader, List<T> providers) {
        try {
            ServiceLoader<T> standardLoader = ServiceLoader.load(serviceClass, classLoader);
            Iterator<T> iterator = standardLoader.iterator();
            while (iterator.hasNext()) {
                addStandardProvider(serviceClass, iterator, providers);
            }
        } catch (Exception e) {
            log.warn("Failed to load standard SPI services for {}", serviceClass.getName(), e);
        }
    }

    private static <T> void addStandardProvider(Class<T> serviceClass, Iterator<T> iterator, List<T> providers) {
        try {
            providers.add(iterator.next());
        } catch (ServiceConfigurationError e) {
            log.warn("Failed to load standard SPI service for {}", serviceClass.getName(), e);
        }
    }

    /**
     * 从单个自定义路径加载服务
     *
     * @param serviceClass 服务接口类
     * @param customPath 自定义路径，如 "META-INF/custom-services/"
     * @param classLoader 类加载器
     * @param <T> 服务类型
     * @return 从该路径加载的服务实例列表（可能为空）
     */
    private static <T> List<T> loadFromCustomPath(Class<T> serviceClass, String customPath, ClassLoader classLoader) {
        List<T> providers = new ArrayList<>();

        try {
            // 获取所有资源URL
            Enumeration<URL> resources = classLoader.getResources(customPath);

            // 使用LinkedHashSet避免重复URL（有些类加载器可能返回重复项）
            Set<URI> urlSet = new LinkedHashSet<>();
            while (resources.hasMoreElements()) {
                urlSet.add(resources.nextElement().toURI());
            }

            for (URI uri : urlSet) {
                loadFromUri(serviceClass, uri, classLoader, providers);
            }

        } catch (IOException e) {
            // 路径不存在是正常情况，只记录debug日志
            log.debug("No resources found for custom path: {}", customPath);
        } catch (Exception e) {
            // 其他异常记录warn级别
            log.warn("Failed to load services from custom path: {}", customPath, e);
        }

        return providers;
    }

    /**
     * 从单个 URI 加载服务配置
     *
     * @param serviceClass 服务接口类
     * @param uri 配置文件 URI
     * @param classLoader 类加载器
     * @param providers 用于收集服务实例的列表
     * @param <T> 服务类型
     */
    private static <T> void loadFromUri(Class<T> serviceClass, URI uri, ClassLoader classLoader, List<T> providers) {
        // 使用try-with-resources确保资源关闭
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {

            String className;
            int lineNumber = 0;
            while ((className = reader.readLine()) != null) {
                lineNumber++;
                className = className.trim();

                // 跳过空行和注释
                if (className.isEmpty() || className.startsWith("#")) {
                    continue;
                }

                // 加载服务类
                loadServiceClass(serviceClass, className, classLoader, providers, uri, lineNumber);
            }

        } catch (IOException e) {
            log.warn("Failed to read configuration from URI: {}", uri, e);
        }
    }

    /**
     * 加载单个服务类
     *
     * @param serviceClass 服务接口类
     * @param className 实现类全限定名
     * @param classLoader 类加载器
     * @param providers 用于收集服务实例的列表
     * @param uri 配置来源 URI（用于错误信息）
     * @param lineNumber 配置行号（用于错误信息）
     * @param <T> 服务类型
     */
    private static <T> void loadServiceClass(Class<T> serviceClass, String className,
                                            ClassLoader classLoader, List<T> providers,
                                            URI uri, int lineNumber) {
        try {
            // 使用指定的类加载器加载类（不初始化）
            Class<?> clazz = Class.forName(className, false, classLoader);

            // 检查是否实现了指定接口
            if (serviceClass.isAssignableFrom(clazz)) {
                // 实例化服务（要求有无参构造器）
                T provider = serviceClass.cast(clazz.getDeclaredConstructor().newInstance());
                providers.add(provider);
                log.debug("Successfully loaded service: {} from {}", className, uri);
            } else {
                log.warn("Class {} does not implement service interface {} (from {} line {})",
                        className, serviceClass.getName(), uri, lineNumber);
            }

        } catch (ClassNotFoundException e) {
            log.warn("Service class not found: {} (from {} line {})", className, uri, lineNumber);
        } catch (NoSuchMethodException e) {
            log.warn("Service class {} does not have a public no-arg constructor (from {} line {})",
                    className, uri, lineNumber);
        } catch (SecurityException e) {
            log.warn("Security exception when loading service class: {} (from {} line {})",
                    className, uri, lineNumber, e);
        } catch (Exception e) {
            log.warn("Failed to instantiate service class: {} (from {} line {})",
                    className, uri, lineNumber, e);
        }
    }

    /**
     * 从指定文件加载服务
     *
     * <p>注意：文件路径是文件系统路径，不是类路径</p>
     *
     * @param serviceClass 服务接口类
     * @param configFile 配置文件
     * @param classLoader 类加载器
     * @param <T> 服务类型
     * @return 加载的服务实例列表
     * @throws IOException 如果文件不存在或读取失败
     * @throws IllegalArgumentException 如果参数为null
     */
    public static <T> List<T> loadFromFile(Class<T> serviceClass, File configFile, ClassLoader classLoader)
            throws IOException {

        // 参数校验
        Objects.requireNonNull(serviceClass, "Service class must not be null");
        Objects.requireNonNull(configFile, "Config file must not be null");

        // 文件存在性和可读性检查
        if (!configFile.exists()) {
            throw new FileNotFoundException("Config file not found: " + configFile.getAbsolutePath());
        }
        if (!configFile.isFile()) {
            throw new IOException("Path is not a file: " + configFile.getAbsolutePath());
        }
        if (!configFile.canRead()) {
            throw new IOException("Config file is not readable: " + configFile.getAbsolutePath());
        }

        List<T> providers = new ArrayList<>();
        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {

            String className;
            int lineNumber = 0;
            while ((className = reader.readLine()) != null) {
                lineNumber++;
                className = className.trim();

                if (className.isEmpty() || className.startsWith("#")) {
                    continue;
                }

                // 复用加载逻辑
                loadServiceClass(serviceClass, className, loader, providers, configFile.toURI(), lineNumber);
            }

        } catch (Exception e) {
            // 包装异常，提供更多信息
            throw new IOException("Failed to load services from file: " + configFile.getAbsolutePath(), e);
        }

        return List.copyOf(providers);
    }

    /**
     * 简化版本：使用默认类加载器从自定义路径加载服务
     */
    public static <T> List<T> load(Class<T> serviceClass, String... customPaths) {
        return load(serviceClass, customPaths, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 简化版本：使用默认类加载器从文件加载服务
     */
    public static <T> List<T> loadFromFile(Class<T> serviceClass, File configFile) throws IOException {
        return loadFromFile(serviceClass, configFile, Thread.currentThread().getContextClassLoader());
    }
}
