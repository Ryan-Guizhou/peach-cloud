package com.peach.captcha.quickstart;

import com.peach.captcha.model.CaptchaVO;
import com.peach.captcha.quickstart.example.CaptchaCheckExample;
import com.peach.captcha.quickstart.example.CaptchaGenerateExample;
import com.peach.captcha.quickstart.example.CaptchaVerifyExample;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 CaptchaService 生成、成功校验、二次校验以及错误码/过期失败路径。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:10
 */
@SpringBootTest(properties = "quickstart.captcha.demo.enabled=false")
class CaptchaCapabilityTest {

    @Autowired
    private CaptchaGenerateExample generateExample;

    @Autowired
    private CaptchaCheckExample checkExample;

    @Autowired
    private CaptchaVerifyExample verifyExample;

    @Test
    void shouldGenerateThenCheckAndVerify() {
        CaptchaVO generated = generateExample.generate("it-pass");
        assertThat(generated.getToken()).isNotBlank();
        assertThat(generated.getSlidingOriginalImageBase64()).isNotBlank();
        assertThat(generated.getSecretKey()).isNotBlank();

        Response checkResp = checkExample.checkSuccess(generated);
        assertThat(checkResp.isSuccess()).isTrue();
        assertThat(checkResp.getData()).isInstanceOf(CaptchaVO.class);
        CaptchaVO checked = (CaptchaVO) checkResp.getData();
        assertThat(checked.getResult()).isTrue();
        assertThat(checked.getCaptchaVerification()).isNotBlank();

        Response verifyResp = verifyExample.verify(checked.getCaptchaVerification());
        assertThat(verifyResp.isSuccess()).isTrue();

        Response reused = verifyExample.verify(checked.getCaptchaVerification());
        assertThat(reused.isSuccess()).isFalse();
        assertThat(reused.getCode()).isEqualTo(StatusEnum.API_CAPTCHA_INVALID.getCode());
    }

    @Test
    void shouldRejectWrongAnswer() {
        CaptchaVO generated = generateExample.generate("it-wrong");
        Response response = checkExample.checkWrongAnswer(generated);
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(StatusEnum.FAIL.getCode());
    }

    @Test
    void shouldRejectExpiredAndMissingToken() {
        CaptchaVO generated = generateExample.generate("it-expired");
        Response expired = checkExample.checkExpired(generated);
        assertThat(expired.isSuccess()).isFalse();
        assertThat(expired.getCode()).isEqualTo(StatusEnum.API_CAPTCHA_INVALID.getCode());

        Response missing = checkExample.checkMissingToken();
        assertThat(missing.isSuccess()).isFalse();
        assertThat(missing.getCode()).isEqualTo(StatusEnum.API_CAPTCHA_INVALID.getCode());
    }
}
