package com.peach.initialize.quickstart;

import com.peach.initialize.base.InitializeHandler;
import com.peach.initialize.constant.InitializeHandlerType;
import com.peach.initialize.execute.AppStartEventExecute;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证启动初始化不会因事件重复投递或其他上下文启动而重复执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
class AppStartedEventOnceTest {

    @Test
    void shouldExecuteOnlyOnceForOwnedContext() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        InitializeHandler handler = mock(InitializeHandler.class);
        when(handler.type()).thenReturn(InitializeHandlerType.APP_EVENT_LISTENER);
        when(handler.executeOrder()).thenReturn(1);
        when(context.getBeansOfType(InitializeHandler.class)).thenReturn(Map.of("handler", handler));

        ApplicationStartedEvent event = mock(ApplicationStartedEvent.class);
        when(event.getApplicationContext()).thenReturn(context);
        AppStartEventExecute executor = new AppStartEventExecute(context);

        executor.onApplicationEvent(event);
        executor.onApplicationEvent(event);

        verify(handler, times(1)).executeInitialize(context);
    }

    @Test
    void shouldIgnoreEventsFromOtherContexts() {
        ConfigurableApplicationContext owner = mock(ConfigurableApplicationContext.class);
        ConfigurableApplicationContext other = mock(ConfigurableApplicationContext.class);
        ApplicationStartedEvent foreignEvent = mock(ApplicationStartedEvent.class);
        when(foreignEvent.getApplicationContext()).thenReturn(other);

        new AppStartEventExecute(owner).onApplicationEvent(foreignEvent);

        verify(owner, times(0)).getBeansOfType(InitializeHandler.class);
    }
}
