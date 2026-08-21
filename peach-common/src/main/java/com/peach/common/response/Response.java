package com.peach.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.peach.common.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 通用响应模型。
 *
 * <p>该类当前属于公共兼容契约。本次仅进行实现层现代化，不改变现有继承结构、
 * 工厂方法、JSON 字段和返回语义。泛型化或 record 化需要在独立契约迁移中完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:45
 */
public class Response implements Serializable {

    private static final long serialVersionUID = 2402460635136759519L;

    @Schema(description = "状态码")
    private String code;

    @Schema(description = "操作信息")
    private String msg;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "返回数据")
    private Object data;

    public Response() {
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public Response setCode(String code) {
        this.code = code;
        return this;
    }

    public Response setMsg(String msg) {
        this.msg = msg;
        return this;
    }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isSuccess() {
        return code.startsWith("2");
    }

    public static Response success() {
        return new SuccessResponse();
    }

    public static Response success(Object data) {
        return new SuccessResponse(data);
    }

    public static Response fail() {
        return new FailResponse();
    }

    public static Response fail(String msg) {
        return new FailResponse(msg);
    }

    public static Response fail(StatusEnum statusEnum) {
        return new FailResponse(statusEnum);
    }

    public static Response paramError() {
        return new FailResponse(StatusEnum.PARAM_ERROR);
    }

    public static Response paramError(String msg) {
        return new FailResponse(StatusEnum.PARAM_ERROR.getCode(), msg);
    }

    public static Response businessResponse() {
        return new BusinessFailResponse();
    }

    public static Response businessResponse(String msg) {
        return new BusinessFailResponse(StatusEnum.BUSINESS_FAIL_CODE.getCode(), msg);
    }

    public static Response businessResponse(String code, String msg) {
        return new BusinessFailResponse(code, msg);
    }

    public static Response commonResponse(boolean status) {
        return commonResponse(status, null, null);
    }

    public static Response commonResponse(boolean status, String msg) {
        return commonResponse(status, msg, null);
    }

    public static Response commonResponse(boolean status, String msg, Object data) {
        var response = status ? Response.success() : Response.fail();
        if (StringUtil.isNotEmpty(msg)) {
            response.setMsg(msg);
        }
        if (data != null) {
            response.setData(data);
        }
        return response;
    }

    public static class SuccessResponse extends Response implements Serializable {

        private static final long serialVersionUID = 9040035077231522334L;

        public SuccessResponse() {
            setCode(StatusEnum.SUCCESS.getCode());
            setMsg(StatusEnum.SUCCESS.getMessage());
        }

        public SuccessResponse(Object data) {
            setCode(StatusEnum.SUCCESS.getCode());
            setMsg(StatusEnum.SUCCESS.getMessage());
            setData(data);
        }
    }

    public static class BusinessFailResponse extends Response implements Serializable {

        private static final long serialVersionUID = -348678046434125007L;

        public BusinessFailResponse() {
            setCode(StatusEnum.BUSINESS_FAIL_CODE.getCode());
            setMsg(StatusEnum.BUSINESS_FAIL_CODE.getMessage());
        }

        public BusinessFailResponse(String msg) {
            setCode(StatusEnum.FAIL.getCode());
            setMsg(msg);
        }

        public BusinessFailResponse(String code, String msg) {
            setCode(code);
            setMsg(msg);
        }
    }

    public static class FailResponse extends Response implements Serializable {

        private static final long serialVersionUID = -3506879010527215679L;

        public FailResponse() {
            setCode(StatusEnum.FAIL.getCode());
            setMsg(StatusEnum.FAIL.getMessage());
        }

        public FailResponse(String msg) {
            setCode(StatusEnum.FAIL.getCode());
            setMsg(msg);
        }

        public FailResponse(StatusEnum statusEnum) {
            setCode(statusEnum.getCode());
            setMsg(statusEnum.getMessage());
        }

        public FailResponse(String code, String msg) {
            setCode(code);
            setMsg(msg);
        }
    }

    public static class CommonResponse extends Response implements Serializable {

        private static final long serialVersionUID = -5544154186538478127L;

        public CommonResponse(StatusEnum statusEnum) {
            setCode(statusEnum.getCode());
            setMsg(statusEnum.getMessage());
            setData(null);
        }

        public CommonResponse(StatusEnum statusEnum, String msg) {
            setCode(statusEnum.getCode());
            setMsg(statusEnum.getMessage() + ":" + msg);
            setData(null);
        }

        public CommonResponse(String code, String msg) {
            setCode(code);
            setMsg(msg);
            setData(null);
        }
    }
}
