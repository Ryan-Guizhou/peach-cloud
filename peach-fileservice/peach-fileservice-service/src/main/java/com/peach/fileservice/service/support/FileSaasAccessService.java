package com.peach.fileservice.service.support;

import cn.dev33.satoken.stp.StpUtil;
import com.peach.auth.dto.SaasIamPermissionCheckDTO;
import com.peach.auth.dto.SaasIamFileObjectCheckDTO;
import com.peach.auth.dto.SaasIamFileFieldReadDTO;
import com.peach.auth.dto.SaasIamFileFieldWriteDTO;
import com.peach.auth.vo.SaasIamFileQueryScopeVO;
import java.util.LinkedHashMap;
import java.util.Map;
import com.peach.common.unique.UniqueIdFacade;
import com.peach.fileservice.vo.FileRecordVO;
import com.peach.fileservice.vo.FileUploadSessionVO;
import com.peach.auth.openfeign.SaasIamFeignClient;
import com.peach.auth.vo.SaasIamContextVO;
import com.peach.auth.vo.SaasIamPermissionDecisionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * 文件操作的受信 IAM 在线授权入口，网络异常一律默认拒绝。
 *
 * <p>只允许以当前 Servlet 请求关联的 Sa-Token 为最终用户凭证；
 * Feign 中间件应独立附加 Same-Token 服务凭证。绝不信任客户端自报
 * tenantRefId、appRefId、memberRefId、角色或业务租户号。
 * 动作允许不等于完整对象授权；对象 SQL 还必须按物理范围及所有者收敛。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/19
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSaasAccessService {

    private static final Map<String, Integer> EDITABLE_METADATA_LENGTHS =
            Map.of("displayName", 255, "bizTag", 64, "remark", 500);

    private static final Pattern ACTION_CODE =
            Pattern.compile("^(GET|POST|PUT|PATCH|DELETE):/api/file/[a-zA-Z0-9/_{}.-]+$");

    private final SaasIamFeignClient iamFeignClient;


    /**
     * 受控文件动作，缺失最终用户 Token、远程 IAM 拒绝或数据库范围丢失均拒绝。
     *
     * @param action 业务端内建固定 API 动作权限码，不能来源于用户入参
     * @return 有效且绑定当前租户应用的物理身份快照
     */
    public FileSaasScope requireCurrent(String action) {
        if (!StringUtils.hasText(action) || action.length() > 128
                || !ACTION_CODE.matcher(action).matches()) {
            throw new SecurityException("Invalid trusted file API action");
        }
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Valid end-user token is required");
        }
        try {
            SaasIamPermissionCheckDTO request = new SaasIamPermissionCheckDTO();
            request.setPermissionCode(action);
            SaasIamPermissionDecisionVO decision = iamFeignClient.checkApiPermission(token, request);
            if (decision == null || !decision.isAllowed() || decision.getContext() == null) {
                throw new SecurityException("File API action was not authorized");
            }
            SaasIamContextVO subject = decision.getContext();
            return new FileSaasScope(subject.getTenantRefId(), subject.getAppRefId(),
                    subject.getUserRefId(), subject.getMemberRefId(), subject.getTenantBusinessId());
        } catch (RuntimeException exception) {
            // 不泄漏最终用户 Token、远端权限目录及具体认证错误。
            log.warn("Trusted file authorization rejected");
            throw new SecurityException("Trusted file authorization required");
        }
    }

    /**
     * 文件元数据写入必须同时通过本地物理归属校验与 Auth 同绑定行/字段写权限。
     * 未注册列、缺失值和超长字段均在调用数据库或 Auth 前拒绝。
     *
     * @param scope 当前身份的物理租户应用范围
     * @param record 通过限定租户/应用的 SQL 查询得到的真实文件记录
     * @param changes 客户端请求的全部待修改字段
     */
    public void requireWritableFileFields(FileSaasScope scope, FileRecordVO record,
                                          Map<String, String> changes) {
        if (scope == null || record == null || !StringUtils.hasText(record.getId())
                || !scope.tenantRefId().equals(record.getTenantRefId())
                || !scope.appRefId().equals(record.getAppRefId())
                || !StringUtils.hasText(record.getOwnerUserRefId())
                || changes == null || changes.isEmpty() || changes.size() > EDITABLE_METADATA_LENGTHS.size()
                || !EDITABLE_METADATA_LENGTHS.keySet().containsAll(changes.keySet())) {
            throw new SecurityException("Protected file metadata changes are invalid");
        }
        for (Map.Entry<String, String> entry : changes.entrySet()) {
            if (entry.getValue() == null
                    || entry.getValue().length() > EDITABLE_METADATA_LENGTHS.get(entry.getKey())) {
                throw new SecurityException("Protected file metadata value is invalid");
            }
        }
        SaasIamFileObjectCheckDTO object = new SaasIamFileObjectCheckDTO();
        object.setPermissionCode("FILE.EDIT");
        object.setTenantRefId(record.getTenantRefId());
        object.setAppRefId(record.getAppRefId());
        object.setObjectRefId(record.getId());
        object.setOwnerUserRefId(record.getOwnerUserRefId());
        object.setOrgRefId(record.getOrgRefId());
        object.setDeleted(Integer.valueOf(1).equals(record.getIsDelete()));
        SaasIamFileFieldWriteDTO request = new SaasIamFileFieldWriteDTO();
        request.setObject(object);
        request.setChanges(Map.copyOf(changes));
        String token = requireSubjectToken();
        try {
            SaasIamPermissionDecisionVO decision = iamFeignClient.checkFileFieldWrites(token, request);
            if (decision == null || !decision.isAllowed()) {
                throw new SecurityException("File metadata write policy denied");
            }
        } catch (RuntimeException exception) {
            log.warn("Trusted file metadata field write denied");
            throw new SecurityException("File metadata write authorization required");
        }
    }

    /**
     * 接收 Auth 的同绑定角色行范围，必须与当前在线用户物理身份一致。
     * 仅在固定 MyBatis WHERE 使用参数化组织 ID，不拼接 SQL 字符串。
     *
     * @param scope 当前动作实时核验的物理用户
     * @return 可授权文件列表的物理行范围
     */
    public SaasIamFileQueryScopeVO requireListQueryScope(FileSaasScope scope) {
        if (scope == null) {
            throw new SecurityException("Trusted file list identity required");
        }
        String token = requireSubjectToken();
        try {
            SaasIamFileQueryScopeVO plan = iamFeignClient.fileQueryScope(token);
            if (plan == null || !scope.tenantRefId().equals(plan.getTenantRefId())
                    || !scope.appRefId().equals(plan.getAppRefId())
                    || !scope.userRefId().equals(plan.getUserRefId())
                    || (!plan.isTenantAll() && !plan.isSelf()
                    && (plan.getOrganizationRefIds() == null || plan.getOrganizationRefIds().isEmpty())
                    && (plan.getSelfOrganizationRefIds() == null || plan.getSelfOrganizationRefIds().isEmpty()))) {
                throw new SecurityException("Trusted file SQL scope missing or mismatched");
            }
            validateOrganizationScope(plan.getOrganizationRefIds());
            validateOrganizationScope(plan.getSelfOrganizationRefIds());
            return plan;
        } catch (RuntimeException exception) {
            log.warn("Trusted file query scope rejected");
            throw new SecurityException("File query scope authorization required");
        }
    }

    private static void validateOrganizationScope(List<String> organizations) {
        if (organizations == null || organizations.size() > 512
                || organizations.stream().anyMatch(id -> !StringUtils.hasText(id) || id.length() > 32)) {
            throw new SecurityException("Invalid bounded file organization scope");
        }
    }

    /**
     * 对真实文件记录再次执行 Auth 数据范围校验，动作与范围必须来自同一有效绑定。
     *
     * @param action FILE.READ / FILE.DOWNLOAD / FILE.DELETE / FILE.RESTORE
     * @param scope 本次动作授权得到的物理身份
     * @param record 通过 scoped SQL 返回的真实文件行
     */
    public void requireFileData(String action, FileSaasScope scope, FileRecordVO record) {
        if (scope == null || record == null || !StringUtils.hasText(record.getId())
                || !scope.tenantRefId().equals(record.getTenantRefId())
                || !scope.appRefId().equals(record.getAppRefId())
                || !StringUtils.hasText(record.getOwnerUserRefId())) {
            throw new SecurityException("File ownership is unavailable or mismatched");
        }
        // Auth 必须收到数据库真实所有者，绝不能将当前登录者冒充为对象 owner；
        // 否则 SELF 可能错误放行不属于本人的文件。
        requireDataDecision(action, scope, record.getId(), record.getOwnerUserRefId(),
                record.getOrgRefId(), Integer.valueOf(1).equals(record.getIsDelete()));
    }

    /**
     * 上传分片会话的所有者/成员/应用归属及同绑定 FILE.UPLOAD 数据策略。
     */
    public void requireUploadSessionData(FileSaasScope scope, FileUploadSessionVO session) {
        if (session == null || !StringUtils.hasText(session.getId())
                || !scope.tenantRefId().equals(session.getTenantRefId())
                || !scope.appRefId().equals(session.getAppRefId())
                || !scope.memberRefId().equals(session.getMemberRefId())
                || !scope.userRefId().equals(session.getOwnerUserRefId())) {
            throw new SecurityException("Trusted upload session scope mismatch");
        }
        requireDataDecision("FILE.UPLOAD", scope, session.getId(),
                session.getOwnerUserRefId(), session.getOrgRefId(), false);
    }

    /**
     * 创建新文件或分页列出本人文件前用服务端构造的虚拟对象校验 FILE 数据策略。
     *
     * <p>未指定机构时只接受独立绑定的 TENANT_ALL 或 SELF 范围授权，
     * ORG/ORG_TREE 不能借此冒充全租户数据范围。</p>
     */
    public void requireOwnDataAction(String action, FileSaasScope scope) {
        if (!List.of("FILE.UPLOAD", "FILE.LIST").contains(action)) {
            throw new SecurityException("Unsupported file data action");
        }
        requireDataDecision(action, scope, UniqueIdFacade.nextId(),
                scope.userRefId(), null, false);
    }

    private void requireDataDecision(String action, FileSaasScope scope, String objectRefId,
                                     String ownerUserRefId, String orgRefId, boolean deleted) {
        if (scope == null || !StringUtils.hasText(objectRefId) || objectRefId.length() > 32
                || !StringUtils.hasText(ownerUserRefId) || ownerUserRefId.length() > 32) {
            throw new SecurityException("Trusted file object is required");
        }
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Authenticated file subject is required");
        }
        SaasIamFileObjectCheckDTO request = new SaasIamFileObjectCheckDTO();
        request.setPermissionCode(action);
        request.setTenantRefId(scope.tenantRefId());
        request.setAppRefId(scope.appRefId());
        request.setObjectRefId(objectRefId);
        request.setOwnerUserRefId(ownerUserRefId);
        request.setOrgRefId(orgRefId);
        request.setDeleted(deleted);
        try {
            SaasIamPermissionDecisionVO decision = iamFeignClient.checkFileObjectPermission(token, request);
            if (decision == null || !decision.isAllowed()) {
                throw new SecurityException("File data policy rejected");
            }
        } catch (RuntimeException exception) {
            log.warn("Trusted file data policy rejected, action={}", action);
            throw new SecurityException("Trusted file data authorization is required");
        }
    }

    /**
     * 按同一授权绑定的行/字段策略投影一个真实文件记录。
     *
     * <p>原始记录绝不直接返回客户端；只有字段定义明确 ALLOW/MASK
     * 并具有与对象行范围一致的角色绑定才可出现在返回对象中。
     * FILE_ID 本身必须明确 ALLOW，否则记录不具备可安全展示的身份。</p>
     *
     * @param action FILE.READ 或 FILE.LIST
     * @param scope 本次动作已验证的物理用户归属
     * @param record 从带物理租户/应用/所有者谓词的 SQL 得到的记录
     * @return 不包含内部存储定位、租户业务标识和未授权字段的视图
     */
    public FileRecordVO projectVisibleFields(String action, FileSaasScope scope, FileRecordVO record) {
        SaasIamFileFieldReadDTO request = buildFieldReadRequest(action, scope, record);
        String token = requireSubjectToken();
        try {
            Map<String, Object> permitted = iamFeignClient.projectFileFields(token, request);
            return toVisibleRecord(record, request.getFields(), permitted);
        } catch (RuntimeException exception) {
            log.warn("Trusted file field projection denied, action={}", action);
            throw new SecurityException("File field authorization required");
        }
    }

    /**
     * 一个页面只向 Auth 发出一次远程请求，在 Auth 端共享当前用户角色与字段策略查询。
     * 每条记录依然独立校验物理归属和行级绑定，任意失败使整页默认拒绝。
     *
     * @param scope 已实时验证的物理用户身份
     * @param records SQL 按 tenant/app/owner 分页后得到的最多 50 条文件记录
     * @return 明确获准的公共字段视图
     */
    public List<FileRecordVO> projectVisiblePage(FileSaasScope scope, List<FileRecordVO> records) {
        if (scope == null || records == null || records.size() > 50) {
            throw new SecurityException("Protected file page is invalid");
        }
        if (records.isEmpty()) {
            return List.of();
        }
        List<SaasIamFileFieldReadDTO> requests = new ArrayList<>(records.size());
        for (FileRecordVO row : records) {
            requests.add(buildFieldReadRequest("FILE.LIST", scope, row));
        }
        String token = requireSubjectToken();
        try {
            List<Map<String, Object>> results = iamFeignClient.projectFileFieldsBatch(token, requests);
            if (results == null || results.size() != requests.size()) {
                throw new SecurityException("Trusted file page projection is incomplete");
            }
            List<FileRecordVO> visible = new ArrayList<>(results.size());
            for (int index = 0; index < results.size(); index++) {
                visible.add(toVisibleRecord(records.get(index),
                        requests.get(index).getFields(), results.get(index)));
            }
            return List.copyOf(visible);
        } catch (RuntimeException exception) {
            log.warn("Trusted file page field projection denied");
            throw new SecurityException("Protected file page field authorization required");
        }
    }

    private static SaasIamFileFieldReadDTO buildFieldReadRequest(
            String action, FileSaasScope scope, FileRecordVO record) {
        if (!List.of("FILE.READ", "FILE.LIST").contains(action) || scope == null
                || record == null || !StringUtils.hasText(record.getId())
                || record.getId().length() > 32 || !StringUtils.hasText(record.getFileId())
                || !scope.tenantRefId().equals(record.getTenantRefId())
                || !scope.appRefId().equals(record.getAppRefId())
                || !StringUtils.hasText(record.getOwnerUserRefId())) {
            throw new SecurityException("File field scope is invalid");
        }
        SaasIamFileObjectCheckDTO object = new SaasIamFileObjectCheckDTO();
        object.setPermissionCode(action);
        object.setTenantRefId(record.getTenantRefId());
        object.setAppRefId(record.getAppRefId());
        object.setObjectRefId(record.getId());
        object.setOwnerUserRefId(record.getOwnerUserRefId());
        object.setOrgRefId(record.getOrgRefId());
        object.setDeleted(Integer.valueOf(1).equals(record.getIsDelete()));

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("fileId", record.getFileId());
        source.put("fileName", record.getFileName());
        source.put("displayName", record.getDisplayName());
        source.put("contentType", record.getContentType());
        source.put("fileSize", record.getFileSize());
        source.put("fileExt", record.getFileExt());
        source.put("fileStatus", record.getFileStatus());
        source.put("bizType", record.getBizType());
        source.put("bizId", record.getBizId());
        source.put("bizTag", record.getBizTag());
        source.put("remark", record.getRemark());
        source.put("createdTime", record.getCreatedTime());

        SaasIamFileFieldReadDTO request = new SaasIamFileFieldReadDTO();
        request.setObject(object);
        request.setFields(source);
        return request;
    }

    private static FileRecordVO toVisibleRecord(FileRecordVO original,
                                                Map<String, Object> submitted,
                                                Map<String, Object> permitted) {
        if (permitted == null || permitted.isEmpty()
                || !submitted.keySet().containsAll(permitted.keySet())
                || !original.getFileId().equals(permitted.get("fileId"))) {
            throw new SecurityException("Readable file identity field is required");
        }
        FileRecordVO visible = new FileRecordVO();
        visible.setFileId(original.getFileId());
        visible.setFileName(asText(permitted.get("fileName")));
        visible.setDisplayName(asText(permitted.get("displayName")));
        visible.setContentType(asText(permitted.get("contentType")));
        Object size = permitted.get("fileSize");
        if (size instanceof Number number) {
            visible.setFileSize(number.longValue());
        }
        visible.setFileExt(asText(permitted.get("fileExt")));
        visible.setFileStatus(asText(permitted.get("fileStatus")));
        visible.setBizType(asText(permitted.get("bizType")));
        visible.setBizId(asText(permitted.get("bizId")));
        visible.setBizTag(asText(permitted.get("bizTag")));
        visible.setRemark(asText(permitted.get("remark")));
        visible.setCreatedTime(asText(permitted.get("createdTime")));
        return visible;
    }

    private static String requireSubjectToken() {
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Authenticated file subject is required");
        }
        return token;
    }

    private static String asText(Object value) {
        return value instanceof String text ? text : null;
    }

    /**
     * 用已验证的本次请求最终用户 Token 取得审计成员。
     */
    public String currentOperator() {
        return requireContext().memberRefId();
    }

    /**
     * 只查询当前用户 Token 的在线物理身份，不能单独用于授予读写动作。
     *
     * @return IAM 在线核验的物理归属及当前用户
     */
    public FileSaasScope requireContext() {
        String token = StpUtil.getTokenValue();
        if (!StringUtils.hasText(token) || token.length() > 1024) {
            throw new SecurityException("Valid end-user token is required");
        }
        try {
            SaasIamContextVO context = iamFeignClient.requireContext(token);
            if (context == null) {
                throw new SecurityException("Trusted IAM context unavailable");
            }
            return new FileSaasScope(context.getTenantRefId(), context.getAppRefId(),
                    context.getUserRefId(), context.getMemberRefId(), context.getTenantBusinessId());
        } catch (RuntimeException exception) {
            log.warn("Trusted file context rejected");
            throw new SecurityException("Trusted file identity required");
        }
    }

    /**
     * 文件永久业务 ID 只能是一个安全路径段，拒绝请求路径语法注入。
     */
    public FileSaasScope requireForFile(String method, String fileId, String suffix) {
        if (fileId == null || !fileId.matches("[A-Za-z0-9-]{1,64}")
                || suffix == null || !List.of("", "/url", "/restore", "/metadata").contains(suffix)
                || !List.of("GET", "POST", "PATCH", "DELETE").contains(method)) {
            throw new SecurityException("Invalid file action");
        }
        return requireCurrent(method + ":/api/file/internal/" + fileId + suffix);
    }
}
