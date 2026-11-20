package com.peach.auth.dao;

import com.peach.auth.vo.UserVO;
import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.UserDO;
import com.peach.auth.qo.UserQO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;


import java.util.List;

/**
 * 用户数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/1/9 15:52
 */
@Indexed
@MybatisDao
public interface UserDao extends PeachDao<UserDO, UserVO> {

    /**
     * 将受信 Token 的全局用户物理 ID 映射为永久业务 USER_ID。
     *
     * @param userRefId 已验证的用户物理 ID
     * @return 有效账号的业务 ID，不存在返回 null
     */
    String findBusinessIdByPhysicalId(@Param("userRefId") String userRefId);

    UserVO login(@Param("username") String username);

    /**
     * 锁定当前有效全局账号凭证，避免两次并发改密覆盖或使用过期版本。
     *
     * @param userRefId 由当前 Token 身份确定的用户物理主键
     * @return 用户物理身份、BCrypt 散列及密码版本，不存在返回 null
     */
    UserDO lockCredential(@Param("userRefId") String userRefId);

    /**
     * 核验已登录 SaaS Token 的全局密码版本，任何旧版本立即拒绝。
     */
    int countActivePasswordVersion(@Param("userRefId") String userRefId,
                                   @Param("businessUserId") String businessUserId,
                                   @Param("passwordVersion") long passwordVersion);

    /**
     * CAS 更新 BCrypt 散列并原子递增全局密码版本。
     */
    int changePassword(@Param("userRefId") String userRefId,
                       @Param("passwordHash") String passwordHash,
                       @Param("expectedVersion") long expectedVersion,
                       @Param("operatorRefId") String operatorRefId);


    List<UserVO> selectByQO(UserQO userQO);

    int updateProfileBasic(@Param("userId") String userId, @Param("userName") String userName,
                           @Param("mobilePhone") String mobilePhone, @Param("email") String email,
                           @Param("modifyTime") String modifyTime, @Param("modifierId") String modifierId);
}
