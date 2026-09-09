package com.peach.common.loader;

import com.peach.common.constant.PubCommonConst;
import com.peach.common.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义服务加载器。
 * <p>扩展标准 Java {@link ServiceLoader}，支持从标准路径（{@code META-INF/services/}）
 * 以及自定义类路径、外部配置文件中加载 SPI 服务实现。</p>
 *
 * <h3>特性：</h3>
 * <ul>
 *   <li><b>标准兼容：</b>优先从标准 {@code META-INF/services/} 加载。</li>
 *   <li><b>多路径支持：</b>支持指定多个自定义类路径（目录前缀或具体文件全路径）。</li>
 *   <li><b>重复类去重：</b>同一具体实现类在一次加载中仅实例化一次并保留加载顺序。</li>
 *   <li><b>性能优化：</b>采用基于服务类与类加载器的线程安全缓存策略，避免频繁扫描与重复反射实例化。</li>
 *   <li><b>异常隔离：</b>单个 SPI 配置文件语法错误或类实例化失败不中断其他可用服务的加载。</li>
 *   <li><b>安全反射：</b>规范处理构造器访问权限并自动校验接口/抽象类合法性。</li>
 * </ul>
 *
 * @Author Mr Shu
 * @Version 1.1.0
 * @CreateTime 2025/12/9 15:44
 */
@Slf4j
public class CustomServiceLoader {

    private record CacheKey(Class<?> serviceClass, ClassLoader classLoader, List<String> customPaths) {}

    private static final Map<CacheKey, List<?>> SERVICE_CACHE = new ConcurrentHashMap<>();

    private CustomServiceLoader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 从标准路径加载服务实现（默认使用当前线程上下文类加载器，启用缓存）。
     *
     * @param serviceClass 服务接口类，不能为 null
     * @param <T>          服务类型
     * @return 加载的服务实例不可变列表
     */
    public static <T> List<T> load(Class<T> serviceClass) {
        return load(serviceClass, (String[]) null, null);
    }

    /**
     * 从标准路径加载服务实现，指定类加载器（启用缓存）。
     *
     * @param serviceClass 服务接口类，不能为 null
     * @param classLoader  类加载器，若为 null 则使用线程上下文类加载器
     * @param <T>          服务类型
     * @return 加载的服务实例不可变列表
     */
    public static <T> List<T> load(Class<T> serviceClass, ClassLoader classLoader) {
        return load(serviceClass, (String[]) null, classLoader);
    }

    /**
     * 从标准路径和自定义路径加载服务实现（使用默认上下文类加载器，启用缓存）。
     *
     * @param serviceClass 服务接口类，不能为 null
     * @param customPaths  自定义路径数组，可为空
     * @param <T>          服务类型
     * @return 加载的服务实例不可变列表
     */
    public static <T> List<T> load(Class<T> serviceClass, String... customPaths) {
        return load(serviceClass, customPaths, null);
    }

    /**
     * 从标准路径和自定义路径加载服务实现（指定类加载器，启用缓存）。
     *
     * @param serviceClass 服务接口类，不能为 null
     * @param customPaths  自定义路径数组，可为空
     * @param classLoader  类加载器，如果为 null 则使用当前线程上下文类加载器
     * @param <T>          服务类型
     * @return 加载的服务实例不可变列表
     * @throws IllegalArgumentException 如果 serviceClass 为 null
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> load(Class<T> serviceClass, String[] customPaths, ClassLoader classLoader) {
        Objects.requireNonNull(serviceClass, "Service class must not be null");

        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        List<String> pathList = normalizeCustomPaths(customPaths);
        CacheKey cacheKey = new CacheKey(serviceClass, loader, pathList);

        return (List<T>) SERVICE_CACHE.computeIfAbsent(cacheKey, key -> doLoad(serviceClass, pathList, loader));
    }

    /**
     * 获取第一个可用的服务实现。
     *
     * @param serviceClass 服务接口类
     * @param <T>          服务类型
     * @return 首个服务实现的 Optional 包装
     */
    public static <T> Optional<T> findFirst(Class<T> serviceClass) {
        List<T> list = load(serviceClass);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.getFirst());
    }

    /**
     * 获取第一个可用的服务实现，若无实现则返回缺省默认实例。
     *
     * @param serviceClass    服务接口类
     * @param defaultProvider 默认实现实例
     * @param <T>             服务类型
     * @return 首个服务实例或默认实例
     */
    public static <T> T loadFirst(Class<T> serviceClass, T defaultProvider) {
        List<T> list = load(serviceClass);
        return list.isEmpty() ? defaultProvider : list.getFirst();
    }

    /**
     * 清除指定服务接口的缓存并强制重新加载。
     *
     * @param serviceClass 服务接口类
     * @param <T>          服务类型
     * @return 重新加载后的服务实例不可变列表
     */
    public static <T> List<T> reload(Class<T> serviceClass) {
        return reload(serviceClass, (String[]) null, null);
    }

    /**
     * 清除指定参数组合的缓存并强制重新加载。
     *
     * @param serviceClass 服务接口类
     * @param customPaths  自定义路径数组
     * @param classLoader  类加载器
     * @param <T>          服务类型
     * @return 重新加载后的服务实例不可变列表
     */
    public static <T> List<T> reload(Class<T> serviceClass, String[] customPaths, ClassLoader classLoader) {
        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();
        List<String> pathList = normalizeCustomPaths(customPaths);
        CacheKey cacheKey = new CacheKey(serviceClass, loader, pathList);
        SERVICE_CACHE.remove(cacheKey);
        return load(serviceClass, customPaths, classLoader);
    }

    /**
     * 清空所有 SPI 服务缓存。
     */
    public static void clearCache() {
        SERVICE_CACHE.clear();
    }

    /**
     * 执行实际的服务发现与实例化流程。
     */
    private static <T> List<T> doLoad(Class<T> serviceClass, List<String> customPaths, ClassLoader classLoader) {
        Map<Class<?>, T> providerMap = new LinkedHashMap<>();

        // 1. 加载标准 SPI 服务
        loadStandardServices(serviceClass, classLoader, providerMap);

        // 2. 加载自定义路径服务（如果提供了自定义路径）
        if (!customPaths.isEmpty()) {
            for (String customPath : customPaths) {
                if (StringUtil.isNotBlank(customPath)) {
                    loadFromCustomPath(serviceClass, customPath.trim(), classLoader, providerMap);
                }
            }
        }

        return List.copyOf(providerMap.values());
    }

    /**
     * 加载标准 SPI 服务。
     */
    private static <T> void loadStandardServices(Class<T> serviceClass, ClassLoader classLoader,
                                                 Map<Class<?>, T> providerMap) {
        try {
            ServiceLoader<T> standardLoader = ServiceLoader.load(serviceClass, classLoader);
            Iterator<T> iterator = standardLoader.iterator();
            while (iterator.hasNext()) {
                try {
                    T provider = iterator.next();
                    if (provider != null) {
                        providerMap.putIfAbsent(provider.getClass(), provider);
                    }
                } catch (ServiceConfigurationError | Exception e) {
                    log.warn("Failed to load standard SPI service instance for {}", serviceClass.getName(), e);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load standard SPI services for {}", serviceClass.getName(), e);
        }
    }

    /**
     * 从单个自定义路径加载服务。
     */
    private static <T> void loadFromCustomPath(Class<T> serviceClass, String customPath,
                                               ClassLoader classLoader, Map<Class<?>, T> providerMap) {
        String resourcePath = resolveResourcePath(serviceClass, customPath);
        try {
            Enumeration<URL> resources = classLoader.getResources(resourcePath);
            Set<URI> urlSet = new LinkedHashSet<>();
            while (resources.hasMoreElements()) {
                urlSet.add(resources.nextElement().toURI());
            }

            for (URI uri : urlSet) {
                loadFromUri(serviceClass, uri, classLoader, providerMap);
            }
        } catch (IOException e) {
            log.debug("No resources found for custom path: {}", resourcePath);
        } catch (Exception e) {
            log.warn("Failed to load services from custom path: {}", resourcePath, e);
        }
    }

    /**
     * 解析自定义资源路径。
     * <p>若传入的是目录前缀（如 {@code "META-INF/custom-services/"}），自动拼接接口全限定名；
     * 若已包含接口名，则直接使用。</p>
     */
    private static String resolveResourcePath(Class<?> serviceClass, String customPath) {
        String trimmed = customPath.trim();
        if (trimmed.startsWith(PubCommonConst.SLASH)) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith(serviceClass.getName())) {
            return trimmed;
        }
        if (!trimmed.endsWith(PubCommonConst.SLASH)) {
            trimmed = trimmed + PubCommonConst.SLASH;
        }
        return trimmed + serviceClass.getName();
    }

    /**
     * 从单个 URI 加载服务配置。
     */
    private static <T> void loadFromUri(Class<T> serviceClass, URI uri, ClassLoader classLoader,
                                        Map<Class<?>, T> providerMap) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(uri.toURL().openStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                parseAndLoadLine(serviceClass, line, classLoader, providerMap, uri, lineNumber);
            }
        } catch (IOException e) {
            log.warn("Failed to read SPI configuration resource: {}", safeResourceName(uri));
            log.debug("Failed to read SPI configuration URI: {}", uri, e);
        }
    }

    /**
     * 解析单行配置并加载实现类（支持行内 # 注释过滤与前后空白清除）。
     */
    private static <T> void parseAndLoadLine(Class<T> serviceClass, String line, ClassLoader classLoader,
                                             Map<Class<?>, T> providerMap, URI uri, int lineNumber) {
        int commentIndex = line.indexOf(PubCommonConst.SHARP);
        if (commentIndex >= 0) {
            line = line.substring(0, commentIndex);
        }
        String className = line.trim();
        if (className.isEmpty()) {
            return;
        }
        loadServiceClass(serviceClass, className, classLoader, providerMap, uri, lineNumber);
    }

    /**
     * 加载并实例化单个服务类。
     */
    private static <T> void loadServiceClass(Class<T> serviceClass, String className,
                                             ClassLoader classLoader, Map<Class<?>, T> providerMap,
                                             URI uri, int lineNumber) {
        try {
            Class<?> clazz = Class.forName(className, false, classLoader);

            if (!serviceClass.isAssignableFrom(clazz)) {
                log.warn("Class {} does not implement service interface {} (from {} line {})",
                        className, serviceClass.getName(), uri, lineNumber);
                return;
            }

            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                log.warn("Class {} is an interface or abstract class, skipping (from {} line {})",
                        className, uri, lineNumber);
                return;
            }

            if (providerMap.containsKey(clazz)) {
                return;
            }

            Constructor<?> constructor = clazz.getDeclaredConstructor();
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            T provider = serviceClass.cast(constructor.newInstance());
            providerMap.put(clazz, provider);
            log.debug("Successfully loaded service: {} from {}", className, uri);
        } catch (ClassNotFoundException e) {
            log.warn("Service class not found: {} (from {} line {})", className, safeResourceName(uri), lineNumber);
        } catch (NoSuchMethodException e) {
            log.warn("Service class {} does not have a no-arg constructor (from {} line {})",
                    className, safeResourceName(uri), lineNumber);
        } catch (SecurityException e) {
            log.warn("Security exception when loading service class: {} (from {} line {})",
                    className, safeResourceName(uri), lineNumber);
            log.debug("Security exception when loading service class from URI: {}", uri, e);
        } catch (Exception e) {
            log.warn("Failed to instantiate service class: {} (from {} line {})",
                    className, safeResourceName(uri), lineNumber);
            log.debug("Failed to instantiate service class from URI: {}", uri, e);
        }
    }

    /**
     * 从指定外部文件加载服务实现。
     *
     * @param serviceClass 服务接口类
     * @param configFile   配置文件对象（本地文件系统文件）
     * @param classLoader  类加载器
     * @param <T>          服务类型
     * @return 加载的服务实例列表
     * @throws IOException 如果文件不存在或读取失败
     */
    public static <T> List<T> loadFromFile(Class<T> serviceClass, File configFile, ClassLoader classLoader)
            throws IOException {
        Objects.requireNonNull(serviceClass, "Service class must not be null");
        Objects.requireNonNull(configFile, "Config file must not be null");

        if (!configFile.exists()) {
            throw new FileNotFoundException("Config file not found: " + configFile.getAbsolutePath());
        }
        if (!configFile.isFile()) {
            throw new IOException("Path is not a file: " + configFile.getAbsolutePath());
        }
        if (!configFile.canRead()) {
            throw new IOException("Config file is not readable: " + configFile.getAbsolutePath());
        }

        Map<Class<?>, T> providerMap = new LinkedHashMap<>();
        ClassLoader loader = classLoader != null ? classLoader : Thread.currentThread().getContextClassLoader();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                parseAndLoadLine(serviceClass, line, loader, providerMap, configFile.toURI(), lineNumber);
            }
        } catch (Exception e) {
            throw new IOException("Failed to load services from file: " + configFile.getAbsolutePath(), e);
        }

        return List.copyOf(providerMap.values());
    }

    /**
     * 简化版本：使用默认类加载器从外部文件加载服务实现。
     */
    public static <T> List<T> loadFromFile(Class<T> serviceClass, File configFile) throws IOException {
        return loadFromFile(serviceClass, configFile, Thread.currentThread().getContextClassLoader());
    }

    private static List<String> normalizeCustomPaths(String[] customPaths) {
        if (customPaths == null || customPaths.length == 0) {
            return List.of();
        }
        List<String> list = new ArrayList<>();
        for (String path : customPaths) {
            if (path != null && !path.isBlank()) {
                list.add(path.trim());
            }
        }
        return List.copyOf(list);
    }

    private static String safeResourceName(URI uri) {
        if (uri == null) {
            return PubCommonConst.UNKNOWN;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return uri.getScheme() == null ? PubCommonConst.UNKNOWN : uri.getScheme();
        }
        int slashIndex = path.lastIndexOf(PubCommonConst.SLASH);
        return slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
    }
}
