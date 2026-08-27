package com.peach.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.peach.common.util.StringUtil;
import io.swagger.v3.oas.annotations.media.Schema;


import java.io.Serializable;

/**
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
    private transient Object data;

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

    public Response() {
        super();
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
        return this.code.startsWith("2");
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
        Response res = Response.success();
        if (!status) {
            res = Response.fail();
        }
        if (StringUtil.isNotEmpty(msg))
            res.setMsg(msg);
        if (null != data)
            res.setData(data);
        return res;
    }


    public static class SuccessResponse extends Response implements Serializable{

        private static final long serialVersionUID = 9040035077231522334L;

        public SuccessResponse() {
            super();
            this.setCode(StatusEnum.SUCCESS.getCode());
            this.setMsg(StatusEnum.SUCCESS.getMessage());
        }

        public SuccessResponse(Object data) {
            super();
            this.setCode(StatusEnum.SUCCESS.getCode());
            this.setMsg(StatusEnum.SUCCESS.getMessage());
            this.setData(data);
        }
    }

    public static class BusinessFailResponse extends Response implements Serializable{


        private static final long serialVersionUID = -348678046434125007L;

        public BusinessFailResponse() {
            super();
            this.setCode(StatusEnum.BUSINESS_FAIL_CODE.getCode());
            this.setMsg(StatusEnum.BUSINESS_FAIL_CODE.getMessage());
        }

        public BusinessFailResponse(String msg) {
            super();
            this.setCode(StatusEnum.FAIL.getCode());
            this.setMsg(msg);
        }

        public BusinessFailResponse(String code, String msg) {
            super();
            this.setCode(code);
            this.setMsg(msg);
        }
    }

    public static class FailResponse extends Response implements Serializable{

        private static final long serialVersionUID = -3506879010527215679L;

        public FailResponse() {
            super();
            this.setCode(StatusEnum.FAIL.getCode());
            this.setMsg(StatusEnum.FAIL.getMessage());
        }

        public FailResponse(String msg) {
            super();
            this.setCode(StatusEnum.FAIL.getCode());
            this.setMsg(msg);
        }

        public FailResponse(StatusEnum statusEnum) {
            super();
            this.setCode(statusEnum.getCode());
            this.setMsg(statusEnum.getMessage());
        }

        public FailResponse(String code, String msg) {
            super();
            this.setCode(code);
            this.setMsg(msg);
        }

    }

    public static class CommonResponse extends Response implements Serializable{

        private static final long serialVersionUID = -5544154186538478127L;

        public CommonResponse(StatusEnum statusEnum) {
            super();
            this.setCode(statusEnum.getCode());
            this.setMsg(statusEnum.getMessage());
            this.setData(null);
        }

        public CommonResponse(StatusEnum statusEnum,String msg) {
            super();
            this.setCode(statusEnum.getCode());
            this.setMsg(statusEnum.getMessage()+":"+msg);
            this.setData(null);
        }

        public CommonResponse(String code,String msg) {
            super();
            this.setCode(code);
            this.setMsg(msg);
            this.setData(null);
        }

    }



}
