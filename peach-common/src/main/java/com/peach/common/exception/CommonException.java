package com.peach.common.exception;

import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import com.peach.common.util.language.MultiLanguage;

import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 10:28
 */
public class CommonException extends RuntimeException {

    private final String msg;

    private final String language;


    public CommonException(String message,String language) {
        super(message);
        this.msg = message;
        this.language = language;
    }

    public CommonException(StatusEnum statusEnum, String language) {
        super(statusEnum.getMessage());
        this.msg = statusEnum.getMessage();
        this.language = language;
    }


    public String toString(){
        return Optional.ofNullable(language)
                .filter(StringUtil::isNotBlank)
                .map(lang -> MultiLanguage.getExceptionMultiLanguage(msg, lang))
                .orElse(msg);
    }
}
