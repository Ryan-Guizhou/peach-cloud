package com.peach.auth.vo;

import lombok.Data;
import java.util.List;

/**
 * 只读授权校准差异；只有后续授权保存才修改数据库。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/22 21:00
 */
@Data
public class RoleCalibrationVO {
    private List<String> removedFuncCodes;
    private List<String> removedResourceKeys;
}
