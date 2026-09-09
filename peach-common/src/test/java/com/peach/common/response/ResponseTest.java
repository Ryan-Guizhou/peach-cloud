package com.peach.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseTest {

    @Test
    void shouldCreateSuccessResponseWithData() {
        Response response = Response.success("ok-data");

        assertThat(response.getCode()).isEqualTo(StatusEnum.SUCCESS.getCode());
        assertThat(response.getMsg()).isEqualTo(StatusEnum.SUCCESS.getMessage());
        assertThat(response.getData()).isEqualTo("ok-data");
        assertThat(response.isSuccess()).isTrue();
    }

    @Test
    void shouldCreateFailResponseFromStatusEnum() {
        Response response = Response.fail(StatusEnum.TOO_MANY_REQUESTS);

        assertThat(response.getCode()).isEqualTo(StatusEnum.TOO_MANY_REQUESTS.getCode());
        assertThat(response.getMsg()).isEqualTo(StatusEnum.TOO_MANY_REQUESTS.getMessage());
        assertThat(response.isSuccess()).isFalse();
    }

    @Test
    void shouldCreateParamErrorResponse() {
        Response response = Response.paramError("bad input");

        assertThat(response.getCode()).isEqualTo(StatusEnum.PARAM_ERROR.getCode());
        assertThat(response.getMsg()).isEqualTo("bad input");
        assertThat(response.isSuccess()).isFalse();
    }
}