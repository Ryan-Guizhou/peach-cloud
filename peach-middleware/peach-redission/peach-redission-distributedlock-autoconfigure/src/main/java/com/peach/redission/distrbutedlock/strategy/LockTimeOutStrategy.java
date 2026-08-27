package com.peach.redission.distrbutedlock.strategy;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/26 9:47
 */
public enum LockTimeOutStrategy implements LockTimeOutHandler{

    FAIL(){
        @Override
        public void handler(String lockName) {
            String msg = String.format("%s请求频繁",lockName);
            throw new IllegalStateException(msg);
        }
    },

}
