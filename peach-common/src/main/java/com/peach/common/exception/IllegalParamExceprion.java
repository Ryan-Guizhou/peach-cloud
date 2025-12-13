package com.peach.common.exception;

import com.peach.common.util.StringUtil;
import com.peach.common.util.language.MultiLanguage;

import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 10:26
 * @Description 非法参数异常
 */
public class IllegalParamExceprion extends RuntimeException {

    private final String msg;

    private final String language;


    public IllegalParamExceprion(String message,String language) {
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
