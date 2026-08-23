package com.peach.auth.service.impl;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Indexed;
import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.auth.dao.UserAvatarHistoryDao;
import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.UserProfileUpdateDTO;
import com.peach.auth.entity.UserAvatarHistoryDO;
import com.peach.auth.qo.UserQO;
import com.peach.auth.service.IUserProfileService;
import com.peach.auth.vo.AvatarHistoryVO;
import com.peach.auth.vo.UserProfileVO;
import com.peach.auth.vo.UserVO;
import com.peach.common.IDGeneratorUtil;
import com.peach.common.response.Response;
import com.peach.common.util.DateUtil;
import com.peach.common.util.StringUtil;
import com.peach.fileservice.openfeign.FileFeignClient;
import com.peach.fileservice.vo.FileDownloadUrlVO;
import com.peach.fileservice.vo.FileUploadVO;
import com.peach.satoken.constant.SatokenConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements IUserProfileService {

    private static final long MAX_AVATAR_SIZE = 5L * 1024L * 1024L;
    private static final int MAX_AVATAR_HISTORY = 10;
    private static final String AVATAR_BIZ_TYPE = "USER_AVATAR";
    private static final String AVATAR_BIZ_TAG = "PROFILE_AVATAR";

        private final UserDao userDao;

        private final UserAvatarHistoryDao userAvatarHistoryDao;

        private final FileFeignClient fileFeignClient;

        private final ObjectMapper objectMapper;

        private final StringRedisTemplate stringRedisTemplate;

    @Override
    public UserProfileVO getCurrentProfile() {
        String userId = currentUserId();
        UserQO query = new UserQO();
        query.setUserId(userId);
        List<UserVO> users = userDao.selectByQO(query);
        if (CollectionUtils.isEmpty(users)) {
            throw new IllegalStateException("当前用户不存在");
        }
        UserVO user = users.get(0);
        List<AvatarHistoryVO> history = loadAvatarHistory(userId);

        UserProfileVO profile = new UserProfileVO();
        profile.setUserId(user.getUserId());
        profile.setUserCode(user.getUserCode());
        profile.setUserName(user.getUserName());
        profile.setMobilePhone(user.getMobilePhone());
        profile.setEmail(user.getEmail());
        profile.setStatus(user.getStatus());
        profile.setLastestLogin(user.getLastestLogin());
        profile.setDefaultOrgId(user.getDefaultOrgId());
        profile.setAvatarHistory(history);
        profile.setCurrentAvatar(findCurrent(history));
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentProfile(UserProfileUpdateDTO updateDTO) {
        String userId = currentUserId();
        lockUser(userId);
        String userName = normalizeRequired(updateDTO.getUserName(), "用户名称不能为空");
        String mobilePhone = normalizeOptional(updateDTO.getMobilePhone());
        String email = normalizeOptional(updateDTO.getEmail());
        int affected = userDao.updateProfileBasic(userId, userName, mobilePhone, email, DateUtil.nowTime(), userId);
        if (affected != 1) {
            throw new IllegalStateException("个人资料更新失败");
        }
        stringRedisTemplate.opsForHash().put(SatokenConstant.USER_PROFILE_CACHE_PREFIX + userId,
                SatokenConstant.USER_PROFILE_FIELD_USER_NAME, userName);
        return getCurrentProfile();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AvatarHistoryVO uploadAvatar(MultipartFile file) {
        validateAvatar(file);
        String userId = currentUserId();
        FileUploadVO uploaded = requireData(fileFeignClient.upload(file, AVATAR_BIZ_TYPE, userId,
                AVATAR_BIZ_TAG, file.getOriginalFilename(), file.getContentType(), "用户头像", null),
                FileUploadVO.class, "头像上传失败");
        if (StringUtil.isBlank(uploaded.getFileId())) {
            throw new IllegalStateException("头像上传结果缺少文件ID");
        }

        try {
            lockUser(userId);
            List<AvatarHistoryVO> existing = userAvatarHistoryDao.selectActiveByUserId(userId);
            String now = DateUtil.nowTime();
            for (int index = 0; index < existing.size(); index++) {
                AvatarHistoryVO item = existing.get(index);
                userAvatarHistoryDao.updateOrder(item.getAvatarHistoryId(), userId, index + 2, 0, now, userId);
            }

            UserAvatarHistoryDO historyDO = new UserAvatarHistoryDO();
            historyDO.setAvatarHistoryId(IDGeneratorUtil.UUID());
            historyDO.setUserId(userId);
            historyDO.setFileId(uploaded.getFileId());
            historyDO.setSortNo(1);
            historyDO.setIsCurrent(1);
            historyDO.setIsDelete(0);
            historyDO.fillCreateTime(userId);
            userAvatarHistoryDao.insert(historyDO);

            if (existing.size() >= MAX_AVATAR_HISTORY) {
                List<AvatarHistoryVO> overflow = existing.subList(MAX_AVATAR_HISTORY - 1, existing.size());
                markOverflowDeleted(userId, overflow, now);
            }
            return requireCurrent(loadAvatarHistory(userId));
        } catch (RuntimeException exception) {
            safeDeleteFile(uploaded.getFileId());
            throw exception;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AvatarHistoryVO selectAvatar(String avatarHistoryId) {
        if (StringUtil.isBlank(avatarHistoryId)) {
            throw new IllegalArgumentException("头像历史ID不能为空");
        }
        String userId = currentUserId();
        lockUser(userId);
        List<AvatarHistoryVO> history = userAvatarHistoryDao.selectActiveByUserId(userId);
        AvatarHistoryVO selected = null;
        for (AvatarHistoryVO item : history) {
            if (avatarHistoryId.equals(item.getAvatarHistoryId())) {
                selected = item;
                break;
            }
        }
        if (selected == null) {
            throw new IllegalArgumentException("头像历史不存在或不属于当前用户");
        }

        List<AvatarHistoryVO> reordered = new ArrayList<>();
        reordered.add(selected);
        for (AvatarHistoryVO item : history) {
            if (!avatarHistoryId.equals(item.getAvatarHistoryId())) {
                reordered.add(item);
            }
        }
        String now = DateUtil.nowTime();
        for (int index = 0; index < reordered.size(); index++) {
            AvatarHistoryVO item = reordered.get(index);
            userAvatarHistoryDao.updateOrder(item.getAvatarHistoryId(), userId, index + 1,
                    index == 0 ? 1 : 0, now, userId);
        }
        return requireCurrent(loadAvatarHistory(userId));
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择头像图片");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("头像文件不能超过5MB");
        }
        String contentType = file.getContentType();
        boolean jpeg = "image/jpeg".equalsIgnoreCase(contentType);
        boolean png = "image/png".equalsIgnoreCase(contentType);
        if (!jpeg && !png) {
            throw new IllegalArgumentException("头像仅支持 JPG 和 PNG 格式");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] header = new byte[8];
            int length = input.read(header);
            boolean jpegHeader = length >= 3 && (header[0] & 0xff) == 0xff
                    && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
            boolean pngHeader = length == 8 && (header[0] & 0xff) == 0x89
                    && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47
                    && header[4] == 0x0d && header[5] == 0x0a && header[6] == 0x1a && header[7] == 0x0a;
            if ((jpeg && !jpegHeader) || (png && !pngHeader)) {
                throw new IllegalArgumentException("头像文件内容与图片格式不一致");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取头像文件", exception);
        }
    }

    private List<AvatarHistoryVO> loadAvatarHistory(String userId) {
        List<AvatarHistoryVO> history = userAvatarHistoryDao.selectActiveByUserId(userId);
        if (CollectionUtils.isEmpty(history)) {
            return List.of();
        }
        for (AvatarHistoryVO item : history) {
            item.setAvatarUrl(resolveAvatarUrl(item.getFileId()));
        }
        return history;
    }

    private String resolveAvatarUrl(String fileId) {
        try {
            FileDownloadUrlVO result = requireData(fileFeignClient.getUrl(fileId),
                    FileDownloadUrlVO.class, "头像地址获取失败");
            return result.getUrl();
        } catch (RuntimeException exception) {
            log.warn("头像临时地址获取失败，fileId={}", fileId);
            return null;
        }
    }

    private void markOverflowDeleted(String userId, List<AvatarHistoryVO> overflow, String now) {
        List<String> historyIds = new ArrayList<>();
        for (AvatarHistoryVO item : overflow) {
            historyIds.add(item.getAvatarHistoryId());
        }
        userAvatarHistoryDao.markDeleted(historyIds, userId, now, userId);
        for (AvatarHistoryVO item : overflow) {
            safeDeleteFile(item.getFileId());
        }
    }

    private void safeDeleteFile(String fileId) {
        try {
            Response response = fileFeignClient.delete(fileId);
            if (response == null || !response.isSuccess()) {
                log.warn("头像文件清理未成功，fileId={}", fileId);
            }
        } catch (RuntimeException exception) {
            log.warn("头像文件清理失败，fileId={}", fileId);
        }
    }

    private void lockUser(String userId) {
        if (StringUtil.isBlank(userDao.lockById(userId))) {
            throw new IllegalStateException("当前用户不存在");
        }
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (StringUtil.isBlank(normalized)) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        return value == null ? null : value.trim();
    }

    private AvatarHistoryVO findCurrent(List<AvatarHistoryVO> history) {
        for (AvatarHistoryVO item : history) {
            if (Integer.valueOf(1).equals(item.getIsCurrent())) {
                return item;
            }
        }
        return null;
    }

    private AvatarHistoryVO requireCurrent(List<AvatarHistoryVO> history) {
        AvatarHistoryVO current = findCurrent(history);
        if (current == null) {
            throw new IllegalStateException("当前头像状态更新失败");
        }
        return current;
    }

    private <T> T requireData(Response response, Class<T> type, String message) {
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new IllegalStateException(message);
        }
        return objectMapper.convertValue(response.getData(), type);
    }

    private String currentUserId() {
        String userId = StpUtil.getLoginIdAsString();
        if (StringUtil.isBlank(userId)) {
            throw new IllegalStateException("当前用户上下文不存在");
        }
        return userId;
    }
}
