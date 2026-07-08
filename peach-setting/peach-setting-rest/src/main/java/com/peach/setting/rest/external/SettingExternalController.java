package com.peach.setting.rest.external;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/7/8 13:44
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/setting/multiMessage")
@Tag(name = "SettingExternalController", description = "Setting服务外部接口暴露接口管理")
public class SettingExternalController {
}
