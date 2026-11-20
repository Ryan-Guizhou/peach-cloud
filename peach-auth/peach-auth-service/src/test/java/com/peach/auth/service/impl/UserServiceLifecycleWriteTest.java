package com.peach.auth.service.impl;

import com.peach.auth.dao.UserDao;
import com.peach.auth.dto.UserDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 普通用户维护入口不得修改删除、密码状态及登录安全元数据。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/20 15:00
 */
@ExtendWith(MockitoExtension.class)
class UserServiceLifecycleWriteTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRejectManagedLifecycleFieldsInGlobalUserUpdates() {
        List<Consumer<UserDTO>> invalidFields = List.of(
                dto -> dto.setIsDelete(1),
                dto -> dto.setIsModify(1),
                dto -> dto.setPasswdModifyTime("2026-11-20"),
                dto -> dto.setErrorCount("0"),
                dto -> dto.setUnlockTime("2026-11-20"),
                dto -> dto.setLastestLogin("2026-11-20"),
                dto -> dto.setInvlidate("2099-01-01"),
                dto -> dto.setAuthMode("NONE"));

        for (Consumer<UserDTO> invalidField : invalidFields) {
            UserDTO dto = new UserDTO();
            dto.setId("0123456789abcdef0123456789abcdef");
            invalidField.accept(dto);
            assertThatThrownBy(() -> userService.update(dto))
                    .isInstanceOf(SecurityException.class)
                    .hasMessageContaining("dedicated operation");
        }
        verifyNoInteractions(userDao);
    }

    @Test
    void shouldRejectClientControlledDeletionAndPasswordFlagsOnCreate() {
        UserDTO softDeleted = new UserDTO();
        softDeleted.setIsDelete(1);
        assertThatThrownBy(() -> userService.add(softDeleted))
                .isInstanceOf(SecurityException.class);

        UserDTO passwordState = new UserDTO();
        passwordState.setIsModify(1);
        assertThatThrownBy(() -> userService.add(passwordState))
                .isInstanceOf(SecurityException.class);
        verifyNoInteractions(userDao);
    }

    @Test
    void shouldRejectNormalPasswordUpdateAndNullInputsWithoutDaoWrite() {
        UserDTO password = new UserDTO();
        password.setId("0123456789abcdef0123456789abcdef");
        password.setPassword("not-an-encoded-password");
        assertThatThrownBy(() -> userService.update(password))
                .isInstanceOf(SecurityException.class);
        assertThatThrownBy(() -> userService.update(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.add(null))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(userDao);
    }
}
