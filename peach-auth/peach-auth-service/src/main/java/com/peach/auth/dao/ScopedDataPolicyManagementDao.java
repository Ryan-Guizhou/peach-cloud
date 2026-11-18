package com.peach.auth.dao;

import com.peach.auth.vo.RoleDataPolicyVO;
import com.peach.auth.vo.RoleFieldPolicyVO;
import com.peach.common.annoation.MybatisDao;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 角色动作数据/字段策略的受控事务持久层。
 *
 * <p>仅角色行被锁定且操作者身份重新验证后允许写入；物理 ID 在所有 JOIN 中稳定使用。
 * 禁止 HTTP 参数参与 SQL 标识符或拼接可执行 SQL。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/18 10:45
 */
@Indexed
@MybatisDao
public interface ScopedDataPolicyManagementDao {

    List<RoleDataPolicyVO> selectDataPolicies(@Param("tenantRefId") String tenantRefId,
                                               @Param("appRefId") String appRefId,
                                               @Param("roleRefId") String roleRefId,
                                               @Param("permissionRefId") String permissionRefId);

    int retireDataPolicies(@Param("tenantRefId") String tenantRefId,
                           @Param("appRefId") String appRefId,
                           @Param("roleRefId") String roleRefId,
                           @Param("permissionRefId") String permissionRefId);

    int insertDataPolicy(@Param("id") String id,
                         @Param("tenantRefId") String tenantRefId,
                         @Param("appRefId") String appRefId,
                         @Param("roleRefId") String roleRefId,
                         @Param("permissionRefId") String permissionRefId,
                         @Param("dataScope") String dataScope,
                         @Param("orgRefId") String orgRefId,
                         @Param("operatorRefId") String operatorRefId);

    List<RoleFieldPolicyVO> selectFieldPolicies(@Param("tenantRefId") String tenantRefId,
                                                 @Param("appRefId") String appRefId,
                                                 @Param("roleRefId") String roleRefId,
                                                 @Param("permissionRefId") String permissionRefId);

    List<String> selectDelegableFieldRefs(@Param("tenantRefId") String tenantRefId,
                                          @Param("appRefId") String appRefId,
                                          @Param("resourceCode") String resourceCode,
                                          @Param("fieldRefIds") List<String> fieldRefIds);

    int retireFieldPolicies(@Param("tenantRefId") String tenantRefId,
                            @Param("appRefId") String appRefId,
                            @Param("roleRefId") String roleRefId,
                            @Param("permissionRefId") String permissionRefId);

    int insertFieldPolicy(@Param("id") String id,
                          @Param("tenantRefId") String tenantRefId,
                          @Param("appRefId") String appRefId,
                          @Param("roleRefId") String roleRefId,
                          @Param("permissionRefId") String permissionRefId,
                          @Param("fieldRefId") String fieldRefId,
                          @Param("readEffect") String readEffect,
                          @Param("writeEffect") String writeEffect,
                          @Param("operatorRefId") String operatorRefId);
}
