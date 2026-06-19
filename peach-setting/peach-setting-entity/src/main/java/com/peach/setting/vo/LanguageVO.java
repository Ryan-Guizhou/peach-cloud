package com.peach.setting.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.peach.setting.entity.LanguageDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/6 20:35
 * @Description 语言设置视图对象
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "语言设置视图对象")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LanguageVO extends LanguageDO implements Serializable {

    private static final long serialVersionUID = 1L;
}

