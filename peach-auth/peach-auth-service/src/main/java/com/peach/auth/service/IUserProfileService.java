package com.peach.auth.service;

import com.peach.auth.vo.AvatarHistoryVO;
import com.peach.auth.dto.UserProfileUpdateDTO;
import com.peach.auth.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * IUser资料服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */

public interface IUserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO updateCurrentProfile(UserProfileUpdateDTO updateDTO);

    AvatarHistoryVO uploadAvatar(MultipartFile file);

    AvatarHistoryVO selectAvatar(String avatarHistoryId);
}
