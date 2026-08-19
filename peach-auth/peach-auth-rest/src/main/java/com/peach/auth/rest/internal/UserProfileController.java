package com.peach.auth.rest.internal;

import org.springframework.stereotype.Indexed;
import com.peach.auth.dto.UserProfileUpdateDTO;
import com.peach.auth.service.IUserProfileService;
import com.peach.common.response.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;

@Validated
@Indexed
@RestController
@RequestMapping("/auth/profile")
@Tag(name = "UserProfileController", description = "个人中心")
public class UserProfileController {

    @Resource
    private IUserProfileService userProfileService;

    @GetMapping
    @Operation(summary = "获取当前用户个人资料")
    public Response profile() {
        return Response.success(userProfileService.getCurrentProfile());
    }

    @PostMapping("/basic")
    @Operation(summary = "更新当前用户个人资料")
    public Response updateProfile(@Validated @RequestBody UserProfileUpdateDTO updateDTO) {
        return Response.success(userProfileService.updateCurrentProfile(updateDTO));
    }

    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并使用新头像")
    public Response uploadAvatar(@RequestPart("file") MultipartFile file) {
        return Response.success(userProfileService.uploadAvatar(file));
    }

    @PostMapping("/avatar/{avatarHistoryId}/select")
    @Operation(summary = "选择历史头像并置顶")
    public Response selectAvatar(@NotBlank @PathVariable("avatarHistoryId") String avatarHistoryId) {
        return Response.success(userProfileService.selectAvatar(avatarHistoryId));
    }
}
