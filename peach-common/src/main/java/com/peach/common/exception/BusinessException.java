package com.peach.common.exception;

import com.peach.common.response.StatusEnum;
import com.peach.common.util.StringUtil;
import com.peach.common.util.language.MultiLanguage;
import lombok.Data;

import java.util.Optional;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:39
 */
@Data
public class BusinessException extends RuntimeException {

    private final String code;

    private final String msg;

    private final String language;

    public BusinessException(String message,String language) {
        super(message);
        this.code = StatusEnum.BUSINESS_FAIL_CODE.getCode();
        this.msg = message;
        this.language = language;
    }

    public BusinessException(StatusEnum statusEnum,String language) {
        super(statusEnum.getMessage());
        this.code = statusEnum.getCode();
        this.msg = statusEnum.getMessage();
        this.language = language;
    }

    public BusinessException(String code, String message,String language) {
        super(message);
        this.code = code;
        this.msg = message;
        this.language = language;
    }

    public String toString(){
        return Optional.ofNullable(language)
                .filter(StringUtil::isNotBlank)
                .map(lang -> MultiLanguage.getExceptionMultiLanguage(msg, lang))
                .orElse(msg);
    }


}
