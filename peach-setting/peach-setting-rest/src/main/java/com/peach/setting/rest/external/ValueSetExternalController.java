package com.peach.setting.rest.external;

import com.peach.common.constant.ServicePathConstant;
import com.peach.common.response.Response;
import com.peach.setting.service.IValueSetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Indexed;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 值集外部接口，供其他业务服务通过 Feign 调用。
 */
@Validated
@Indexed
@RestController
@RequestMapping(ServicePathConstant.SETTING_PATH_SERVICE + "/valueSet")
@Tag(name = "ValueSetExternalController", description = "值集外部接口")
@RequiredArgsConstructor
public class ValueSetExternalController {

    private final IValueSetService valueSetService;

    @GetMapping("/item/list/{valueSetCode}")
    @Operation(summary = "根据值集编码查询值集项列表")
    public Response itemList(@NotBlank @PathVariable String valueSetCode) {
        return Response.success(valueSetService.itemListByValueSetCode(valueSetCode));
    }
}
