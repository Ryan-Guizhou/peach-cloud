package com.peach.common.exception;

import com.peach.common.util.StringUtil;
import com.peach.common.util.language.MultiLanguage;

import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 10:25
 * @Description 锁异常
 */
public class LockException extends RuntimeException {

    private final String language;

    private final String msg;


    public LockException(String message,String language) {
        super(message);
        this.language = language;
        this.msg = message;
    }


    public String toString(){
        return Optional.ofNullable(language)
                .filter(StringUtil::isNotBlank)
                .map(lang -> MultiLanguage.getExceptionMultiLanguage(msg, lang))
                .orElse(msg);
    }

}
