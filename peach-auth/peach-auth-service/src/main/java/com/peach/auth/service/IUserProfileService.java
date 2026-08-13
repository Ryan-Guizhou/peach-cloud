package com.peach.auth.service;

import com.peach.auth.vo.AvatarHistoryVO;
import com.peach.auth.dto.UserProfileUpdateDTO;
import com.peach.auth.vo.UserProfileVO;
import org.springframework.web.multipart.MultipartFile;

public interface IUserProfileService {

    UserProfileVO getCurrentProfile();

    UserProfileVO updateCurrentProfile(UserProfileUpdateDTO updateDTO);

    AvatarHistoryVO uploadAvatar(MultipartFile file);

    AvatarHistoryVO selectAvatar(String avatarHistoryId);
}
