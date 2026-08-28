package com.peach.redission.distrbutedlock.aspect;

import com.peach.redission.common.LockInfoHandle;
import com.peach.redission.common.LockInfoHandleFactory;
import com.peach.redission.common.LockInfoType;
import com.peach.redission.distrbutedlock.manage.DistrbutedLockerFactory;
import com.peach.redission.distrbutedlock.locker.DistributedLocker;
import com.peach.redission.distrbutedlock.locker.LockType;
import com.peach.redission.distrbutedlock.annoation.DistrbutedLock;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Distrbuted锁切面。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/25 18:59
 */
@Slf4j
@Aspect
public class DistrbutedLockAspect implements ApplicationContextAware {

    private final LockInfoHandleFactory lockInfoHandleFactory;

    private final DistrbutedLockerFactory distrbutedLockerFactory;

    private ApplicationContext applicationContext;

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new ConcurrentHashMap<>();

    static {
        PRIMITIVE_WRAPPER_MAP.put(int.class, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(long.class, Long.class);
        PRIMITIVE_WRAPPER_MAP.put(double.class, Double.class);
        PRIMITIVE_WRAPPER_MAP.put(float.class, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(boolean.class, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(char.class, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(byte.class, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(short.class, Short.class);
    }



    public DistrbutedLockAspect(LockInfoHandleFactory lockInfoHandleFactory, DistrbutedLockerFactory distrbutedLockerFactory) {
        this.lockInfoHandleFactory = lockInfoHandleFactory;
        this.distrbutedLockerFactory = distrbutedLockerFactory;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(distrbutedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistrbutedLock distrbutedLock) throws Throwable {
        LockInfoHandle lockHandle = lockInfoHandleFactory.getLockHandle(LockInfoType.DISTRIBUTE);
        String lockName = lockHandle.getLockName(joinPoint, distrbutedLock.name(), distrbutedLock.keys());
        LockType lockType = distrbutedLock.lockType();
        long waitTime = distrbutedLock.waitTime();
        TimeUnit timeUnit = distrbutedLock.timeUnit();
        DistributedLocker distrbutedLocker = distrbutedLockerFactory.getDistrbutedLocker(lockType);
        boolean result = distrbutedLocker.tryLock(lockName, timeUnit, waitTime);
        log.info("DistrbutedLockAspect Method: {}, lockName: {}, lockType: {}, waitTime: {}, timeUnit: {}, result: {}",
                joinPoint.getSignature().getName(), lockName, lockType, waitTime, timeUnit, result);
        if (result){
            try {
                return joinPoint.proceed();
            }finally {
                log.info("DistrbutedLockAspect unlock lockName: {}", lockName);
                distrbutedLocker.unlock(lockName);
            }
        }

        // 如果加锁失败
        String customLockTimeoutStrategy = distrbutedLock.customLockTimeoutStrategy();
        if (StringUtils.isNotBlank(customLockTimeoutStrategy)) {
            return handleCustomLockTimeoutStrategy(customLockTimeoutStrategy, joinPoint);
        }

        distrbutedLock.lockTimeoutStrategy().handler(lockName);
        return joinPoint.proceed();

    }

    /**
     * 处理自定义的锁超时处理策略
     * 支持格式：
     * 1. "methodName" (当前类方法)
     * 2. "beanName.methodName" (指定Bean的方法)
     * 3. "全限定类名.methodName" (静态方法)
     * @param customLockTimeoutStrategy
     * @param joinPoint
     * @return
     */
    public Object handleCustomLockTimeoutStrategy(String customLockTimeoutStrategy, JoinPoint joinPoint) {
        log.debug("Handling custom lock timeout strategy: {}", customLockTimeoutStrategy);

        // 判断是否包含点号，决定调用方式
        if (customLockTimeoutStrategy.contains(".")) {
            String[] parts = customLockTimeoutStrategy.split("\\.");
            if (parts.length != 2) {
                // 格式不正确，抛异常
                throw new IllegalArgumentException(
                        "Invalid customLockTimeoutStrategy format. Expected: 'methodName' or " +
                                "'beanName.methodName' or 'className.methodName', but got: " +
                                customLockTimeoutStrategy
                );
            }
            String beanNameOrClassName = parts[0];
            String methodName = parts[1];

            // 尝试从Spring容器获取Bean
            if (applicationContext != null) {
                try {
                    Object targetBean = applicationContext.getBean(beanNameOrClassName);
                    if (targetBean != null) {
                        log.debug("Found bean {} in Spring context, invoking method: {}",
                                beanNameOrClassName, methodName);
                        return invokeBeanMethod(targetBean, methodName, joinPoint);
                    }
                } catch (BeansException e) {
                    log.debug("Bean {} not found in Spring context, trying static method",
                            beanNameOrClassName);
                }
            }

            // 尝试调用静态方法
            log.debug("Trying to invoke static method: {}", customLockTimeoutStrategy);
            return invokeStaticMethod(beanNameOrClassName, methodName, joinPoint);
        }

        // 调用当前类的方法（原有逻辑）
        log.debug("Invoking method in current class: {}", customLockTimeoutStrategy);
        return invokeCurrentClassMethod(customLockTimeoutStrategy, joinPoint);
    }


    /**
     * 调用当前类方法，参数类型与切点方法一致。
     *
     * <p>自定义锁超时回调可能为非 public 方法；使用 Spring {@link ReflectionUtils} 调用，
     * 避免在本类中直接 {@code Method.setAccessible}。</p>
     *
     * @param methodName 目标方法名
     * @param joinPoint    切点
     */
    private Object invokeCurrentClassMethod(String methodName, JoinPoint joinPoint) {
        Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Class<?>[] parameterTypes = currentMethod.getParameterTypes();

        Object target = joinPoint.getTarget();
        Method handleMethod = ReflectionUtils.findMethod(target.getClass(), methodName, parameterTypes);
        if (handleMethod == null) {
            throw new IllegalStateException(
                    "Method not found: " + methodName + " in class " + target.getClass().getName()
                            + " with parameters: " + java.util.Arrays.toString(currentMethod.getParameterTypes()));
        }
        return ReflectionUtils.invokeMethod(handleMethod, target, joinPoint.getArgs());
    }

    /**
     * 调用指定 Bean 的方法，参数类型与切点方法一致。
     *
     * <p>外部 Bean 上的自定义回调可能为非 public 方法；使用 Spring {@link ReflectionUtils} 调用。</p>
     */
    private Object invokeBeanMethod(Object targetBean, String methodName, JoinPoint joinPoint) {
        Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Method matchingMethod = findMatchingMethod(targetBean.getClass(), methodName, currentMethod.getParameterTypes());
        if (matchingMethod == null) {
            throw new IllegalStateException(
                    "Method not found in bean: " + methodName + " in class " + targetBean.getClass().getName());
        }
        return ReflectionUtils.invokeMethod(matchingMethod, targetBean, joinPoint.getArgs());
    }

    /**
     * 查找匹配的方法
     * 首先尝试精确匹配,从目标类中查询目标方法,如果没有在匹配相似方法
     * @param clazz
     * @param methodName
     * @param parameterTypes
     * @return
     */
    private Method findMatchingMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        try {
           return clazz.getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            return findCompatibleMethod(clazz, methodName, parameterTypes);
        }
    }

    private Method findCompatibleMethod(Class<?> clazz, String methodName, Class<?>[] parameterTypes) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] methodParameterTypes = method.getParameterTypes();
            if (Arrays.equals(methodParameterTypes, parameterTypes)) {
                return method;
            }
            if (methodParameterTypes.length == parameterTypes.length
                    && isParameterCompatible(methodParameterTypes, parameterTypes)) {
                return method;
            }
        }
        return null;
    }

    private boolean isParameterCompatible(Class<?>[] methodParameterTypes, Class<?>[] parameterTypes) {
        for (int i = 0; i < methodParameterTypes.length; i++) {
            if (!methodParameterTypes[i].isAssignableFrom(parameterTypes[i])
                    && !isPrimitiveWrapperCompatible(methodParameterTypes[i], parameterTypes[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查基本类型和包装类型的兼容性
     * @param paramType
     * @param argType
     * @return
     */
    private boolean isPrimitiveWrapperCompatible(Class<?> paramType, Class<?> argType) {

        return Optional.ofNullable(paramType)
                .filter(Class::isPrimitive)
                .map(PRIMITIVE_WRAPPER_MAP::get)
                .map(wrapper -> wrapper.equals(argType))
                .orElseGet(() ->
                        Optional.ofNullable(argType)
                                .filter(Class::isPrimitive)
                                .map(t -> isPrimitiveWrapperCompatible(t, paramType))
                                .orElse(false)
                );
    }


    /**
     * 调用静态方法
     * @param className
     * @param methodName
     * @param joinPoint
     * @return
     */
    private Object invokeStaticMethod(String className, String methodName, JoinPoint joinPoint) {
        try {
            // 加载类
            Class<?> clazz = ClassUtils.forName(className,
                    Thread.currentThread().getContextClassLoader());

            Method currentMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();

            // 查找匹配的静态方法
            Method handleMethod = findMatchingMethod(clazz, methodName,
                    currentMethod.getParameterTypes());

            if (handleMethod == null) {
                throw new NoSuchMethodException("Static method " + methodName +
                        " not found in class " + className);
            }

            // 确保是静态方法
            if (!java.lang.reflect.Modifier.isStatic(handleMethod.getModifiers())) {
                throw new IllegalArgumentException(
                        "Method " + methodName + " in class " + className + " is not static");
            }

            Object[] args = joinPoint.getArgs();
            return ReflectionUtils.invokeMethod(handleMethod, null, args);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(
                    "Static method not found: " + methodName + " in class " + className, e);
        }
    }


}
