package com.peach.auth.service.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peach.auth.common.AuthLoginValueSetConst;
import com.peach.auth.vo.LoginInitVO;
import com.peach.captcha.constant.CaptchaEnum;
import com.peach.common.response.Response;
import com.peach.common.util.StringUtil;
import com.peach.setting.openfeign.SettingFeignClient;
import com.peach.setting.vo.ValueSetItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录初始化配置解析，从 setting 值集加载并合并本地默认值。
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
public class LoginInitConfigSupport {

    private static final String DEFAULT_APP_ID = "f73b300578a5436d82ec7fca2c07c284";

    private static final String DEFAULT_SYSTEM_NAME = "Peach Cloud DataOS";

    private static final String DEFAULT_SYSTEM_DESCRIPTION = "面向租户与机构的数据治理、权限和业务协同平台";

    private static final String DEFAULT_ENCRYPTION_ALGORITHM = "RSA-OAEP";

    private final SettingFeignClient settingFeignClient;

    private final ObjectMapper objectMapper;

    public LoginInitVO buildLoginInit(String publicKey) {
        Map<String, String> config = loadLoginConfigMap();
        LoginInitVO initVO = new LoginInitVO();
        initVO.setSystemName(config.getOrDefault(AuthLoginValueSetConst.SYSTEM_NAME, DEFAULT_SYSTEM_NAME));
        initVO.setSystemDescription(config.getOrDefault(AuthLoginValueSetConst.SYSTEM_DESCRIPTION, DEFAULT_SYSTEM_DESCRIPTION));
        initVO.setAppId(config.getOrDefault(AuthLoginValueSetConst.APP_ID, DEFAULT_APP_ID));
        initVO.setFiscal(LocalDate.now(ZoneId.systemDefault()).getYear());
        initVO.setPublicKey(publicKey);
        initVO.setEncryptionAlgorithm(config.getOrDefault(AuthLoginValueSetConst.ENCRYPTION_ALGORITHM, DEFAULT_ENCRYPTION_ALGORITHM));
        initVO.setCaptchaType(config.getOrDefault(AuthLoginValueSetConst.CAPTCHA_TYPE,
                CaptchaEnum.CaptchaServiceType.BLOCKPUZZLE.getCode()));
        initVO.setCaptchaRequired(parseBoolean(config.get(AuthLoginValueSetConst.CAPTCHA_ENABLED), true));
        initVO.setLoginConfig(config);
        return initVO;
    }

    public boolean isCaptchaRequired() {
        return parseBoolean(loadLoginConfigMap().get(AuthLoginValueSetConst.CAPTCHA_ENABLED), true);
    }

    public String resolveCaptchaType() {
        String captchaType = loadLoginConfigMap().get(AuthLoginValueSetConst.CAPTCHA_TYPE);
        if (StringUtil.isBlank(captchaType)) {
            return CaptchaEnum.CaptchaServiceType.BLOCKPUZZLE.getCode();
        }
        return captchaType.trim();
    }

    private Map<String, String> loadLoginConfigMap() {
        Map<String, String> defaults = defaultConfig();
        try {
            Response response = settingFeignClient.listValueSetItems(AuthLoginValueSetConst.VALUE_SET_CODE);
            if (response == null || !response.isSuccess() || response.getData() == null) {
                log.warn("Failed to load login value set from setting service, using defaults");
                return defaults;
            }
            List<ValueSetItemVO> items = objectMapper.convertValue(response.getData(), new TypeReference<>() {
            });
            if (CollectionUtils.isEmpty(items)) {
                return defaults;
            }
            Map<String, String> merged = new LinkedHashMap<>(defaults);
            for (ValueSetItemVO item : items) {
                if (item == null || StringUtil.isBlank(item.getItemCode()) || StringUtil.isBlank(item.getItemValue())) {
                    continue;
                }
                if (item.getStatus() != null && item.getStatus() == 0) {
                    continue;
                }
                merged.put(item.getItemCode().trim(), item.getItemValue().trim());
            }
            return merged;
        } catch (RuntimeException exception) {
            log.warn("Load login value set failed, using defaults", exception);
            return defaults;
        }
    }

    private Map<String, String> defaultConfig() {
        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put(AuthLoginValueSetConst.SYSTEM_NAME, DEFAULT_SYSTEM_NAME);
        defaults.put(AuthLoginValueSetConst.SYSTEM_DESCRIPTION, DEFAULT_SYSTEM_DESCRIPTION);
        defaults.put(AuthLoginValueSetConst.APP_ID, DEFAULT_APP_ID);
        defaults.put(AuthLoginValueSetConst.ENCRYPTION_ALGORITHM, DEFAULT_ENCRYPTION_ALGORITHM);
        defaults.put(AuthLoginValueSetConst.CAPTCHA_ENABLED, "true");
        defaults.put(AuthLoginValueSetConst.CAPTCHA_TYPE, CaptchaEnum.CaptchaServiceType.BLOCKPUZZLE.getCode());
        return defaults;
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if (StringUtil.isBlank(value)) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value.trim()) || "Y".equalsIgnoreCase(value.trim());
    }
}
