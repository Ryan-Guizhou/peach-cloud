package com.peach.code.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 业务编码示例启动事件。
 *
 * <p>应用启动完成后为指定租户生成菜单和通知编码，并记录格式校验结果。该事件用于人工验证
 * Redis/MySQL 双通道发号是否符合预期，不承担业务数据创建职责。</p>
 */
@Component
public class PeachCodeEvent {

    private static final Logger LOG = LoggerFactory.getLogger(PeachCodeEvent.class);

    private final ExampleCodeService exampleCodeService;

    @Value("${peach.code.example.verify-on-startup:true}")
    private boolean verifyOnStartup;

    @Value("${peach.code.example.tenant-id:T001}")
    private String tenantId;

    /**
     * 创建启动验证事件。
     *
     * @param exampleCodeService 示例业务服务
     */
    public PeachCodeEvent(ExampleCodeService exampleCodeService) {
        this.exampleCodeService = exampleCodeService;
    }

    /**
     * 在应用完全就绪后生成示例编码并输出校验结果。
     *
     * @param event Spring Boot 应用就绪事件
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verifyGeneratedCodes(ApplicationReadyEvent event) {
        if (!verifyOnStartup) {
            LOG.info("Code example startup verification is disabled. tenantId={}", tenantId);
            return;
        }
        try {
            String menuCode = exampleCodeService.createMenuCode(tenantId);
            String noticeCode = exampleCodeService.createNoticeCode(tenantId);
            boolean menuValid = matchesPrefixAndNumber(menuCode, "MENU");
            boolean noticeValid = matchesPrefixAndNumber(noticeCode, "NOTICE");
            LOG.info("Code example verification completed. tenantId={}, menuCode={}, menuValid={}, "
                            + "noticeCode={}, noticeValid={}",
                    tenantId, menuCode, menuValid, noticeCode, noticeValid);
        } catch (Exception ex) {
            LOG.error("Code example verification failed. tenantId={}", tenantId, ex);
        }
    }

    /**
     * 校验编码是否由指定前缀和至少一位数字组成。
     *
     * @param code 待校验编码
     * @param prefix 预期编码前缀
     * @return 编码格式合法时返回 {@code true}
     */
    private boolean matchesPrefixAndNumber(String code, String prefix) {
        if (code == null || !code.startsWith(prefix + "_")) {
            return false;
        }
        String number = code.substring(prefix.length() + 1);
        for (int i = 0; i < number.length(); i++) {
            if (!Character.isDigit(number.charAt(i))) {
                return false;
            }
        }
        return !number.isEmpty();
    }
}
