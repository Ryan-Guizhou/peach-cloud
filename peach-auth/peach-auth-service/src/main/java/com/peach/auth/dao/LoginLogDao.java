package com.peach.auth.dao;

import com.peach.common.annoation.MybatisDao;
import com.peach.auth.entity.LoginLogDO;
import com.peach.auth.qo.LoginLogQO;
import com.peach.satoken.context.UserContext;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录审计持久化；不提供单条更新和用户删除操作。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 20:00
 */
@MybatisDao
public interface LoginLogDao {
    /** 插入白名单记录。 */
    void insert(LoginLogDO row);
    /** 按服务端已验证范围稳定分页查询。 */
    List<LoginLogDO> selectByQO(LoginLogQO query);
    /** 调用方负责验证当前租户或未归属事件的平台审计权限。 */
    LoginLogDO selectById(String id);
    /** 校验当前身份的活动角色在当前租户和机构下的显式资源授权。 */
    int hasPermission(@Param("actor") UserContext actor, @Param("permission") String permission);
    /** 每批最多清理1000条早于截止时间的日志，供后台保留期任务使用。 */
    int deleteExpired(@Param("cutoff") LocalDateTime cutoff);
}
