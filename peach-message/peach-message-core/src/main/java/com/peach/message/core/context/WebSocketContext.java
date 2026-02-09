package com.peach.message.core.context;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/2/4 18:18
 */
public class WebSocketContext {

    private static ThreadLocal<ContextDTO> CONTEXT = new ThreadLocal<>();

    public static void setContext(ContextDTO contextDTO){
        CONTEXT.set(contextDTO);
    }

    public static ContextDTO getContext(){
        return CONTEXT.get();
    }

    public static void removeContext(){
        CONTEXT.remove();
    }
}
