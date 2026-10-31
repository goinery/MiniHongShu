package com.hongshu.common.enums;

import java.io.Serial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回对象
 *
 * @author hongshu
 */
@Data
public class ResponseInfo<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 返回码
     */
    private Integer code;

    /**
     * 返回提示
     */
    private String msg;

    /**
     * 返回数据
     */
    private T data;

    public ResponseInfo() {
    }

    public ResponseInfo(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResponseInfo(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 判断是否成功
     */
    @JsonIgnore
    public Boolean isSuccess() {
        return ResponseEnum.SUCCESS.getCode().equals(this.code);
    }

    /**
     * 成功
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> success() {
        return new ResponseInfo<>(ResponseEnum.SUCCESS.getCode(), ResponseEnum.SUCCESS.getMsg());
    }

    /**
     * 成功
     *
     * @param msg 返回提示
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> successMsg(String msg) {
        return new ResponseInfo<>(ResponseEnum.SUCCESS.getCode(), msg);
    }

    /**
     * 成功
     *
     * @param data 返回结果
     * @param <T>  返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> success(T data) {
        return new ResponseInfo<>(ResponseEnum.SUCCESS.getCode(), ResponseEnum.SUCCESS.getMsg(), data);
    }

    /**
     * 参数验证失败返回
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> validateFail() {
        return new ResponseInfo<>(ResponseEnum.BAD_REQUEST.getCode(), ResponseEnum.BAD_REQUEST.getMsg());
    }

    /**
     * 参数验证失败返回结果
     *
     * @param msg 提示信息
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> validateFail(String msg) {
        return new ResponseInfo<>(ResponseEnum.BAD_REQUEST.getCode(), msg);
    }

    /**
     * 访问禁止返回（token失效）
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> unauthorized() {
        return new ResponseInfo<>(ResponseEnum.PROHIBIT_VISIT.getCode(), ResponseEnum.PROHIBIT_VISIT.getMsg());
    }

    /**
     * 访问禁止返回
     *
     * @param msg 返回提示
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> unauthorized(String msg) {
        return new ResponseInfo<>(ResponseEnum.PROHIBIT_VISIT.getCode(), msg);
    }

    /**
     * 权限不足返回
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> permissionDenied() {
        return new ResponseInfo<>(ResponseEnum.PERMISSION_DENIED.getCode(), ResponseEnum.PERMISSION_DENIED.getMsg());
    }

    /**
     * 权限不足返回
     *
     * @param msg 返回提示
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> permissionDenied(String msg) {
        return new ResponseInfo<>(ResponseEnum.PERMISSION_DENIED.getCode(), msg);
    }

    /**
     * 访问资源丢失返回
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> resourcesError() {
        return new ResponseInfo<>(ResponseEnum.RESOURCES_ERROR.getCode(), ResponseEnum.RESOURCES_ERROR.getMsg());
    }

    /**
     * 请求方式错误
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> requestMethodError() {
        return new ResponseInfo<>(ResponseEnum.REQUEST_METHOD_ERROR.getCode(), ResponseEnum.REQUEST_METHOD_ERROR.getMsg());
    }

    /**
     * 连接超时
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> connectTimeOut() {
        return new ResponseInfo<>(ResponseEnum.CONNECT_TIME_OUT.getCode(), ResponseEnum.CONNECT_TIME_OUT.getMsg());
    }

    /**
     * 请求频繁
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> requestTooMany() {
        return new ResponseInfo<>(ResponseEnum.TOO_MANY_REQUESTS.getCode(), ResponseEnum.TOO_MANY_REQUESTS.getMsg());
    }

    /**
     * 账号不存在
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> accountNotExist() {
        return new ResponseInfo<>(ResponseEnum.ACCOUNT_NOT_EXIST.getCode(), ResponseEnum.ACCOUNT_NOT_EXIST.getMsg());
    }

    /**
     * 密码错误
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> passwordError() {
        return new ResponseInfo<>(ResponseEnum.PASSWORD_ERROR.getCode(), ResponseEnum.PASSWORD_ERROR.getMsg());
    }

    /**
     * 账号已禁用
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> accountDisabled() {
        return new ResponseInfo<>(ResponseEnum.ACCOUNT_IS_DISABLED.getCode(), ResponseEnum.ACCOUNT_IS_DISABLED.getMsg());
    }

    /**
     * 手机号已存在
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> telError() {
        return new ResponseInfo<>(ResponseEnum.TEL_IS_EXIST.getCode(), ResponseEnum.TEL_IS_EXIST.getMsg());
    }

    /**
     * 用户名存在
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> nameError() {
        return new ResponseInfo<>(ResponseEnum.NAME_IS_EXIST.getCode(), ResponseEnum.NAME_IS_EXIST.getMsg());
    }

    /**
     * 系统异常
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> error() {
        return new ResponseInfo<>(ResponseEnum.ERROR.getCode(), ResponseEnum.ERROR.getMsg());
    }

    /**
     * 系统异常
     *
     * @param msg 返回提示
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> error(String msg) {
        return new ResponseInfo<>(ResponseEnum.ERROR.getCode(), msg);
    }

    /**
     * 业务异常
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> businessFail() {
        return new ResponseInfo<>(ResponseEnum.BUSINESS_ERROR.getCode(), ResponseEnum.BUSINESS_ERROR.getMsg());
    }

    /**
     * 业务异常
     *
     * @param message 返回提示
     * @param <T>     返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> businessFail(String message) {
        return new ResponseInfo<>(ResponseEnum.BUSINESS_ERROR.getCode(), message);
    }

    /**
     * 业务异常
     *
     * @param message 返回提示
     * @param data    返回结果
     * @param <T>     返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> businessFail(String message, T data) {
        return new ResponseInfo<>(ResponseEnum.BUSINESS_ERROR.getCode(), message, data);
    }

    /**
     * 系统预警
     *
     * @param <T> 返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> systemWarning() {
        return new ResponseInfo<>(ResponseEnum.SYSTEM_WARNING.getCode(), ResponseEnum.SYSTEM_WARNING.getMsg());
    }

    /**
     * 系统预警
     *
     * @param message 返回提示
     * @param <T>     返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> systemWarning(String message) {
        return new ResponseInfo<>(ResponseEnum.SYSTEM_WARNING.getCode(), message);
    }

    /**
     * 自定义异常
     *
     * @param response 响应状态枚举
     * @param <T>      返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> customizeError(ResponseEnum response) {
        return new ResponseInfo<>(response.getCode(), response.getMsg());
    }

    /**
     * 自定义异常
     *
     * @param code 返回码
     * @param msg  返回提示
     * @param <T>  返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> customizeError(Integer code, String msg) {
        return new ResponseInfo<>(code, msg);
    }

    /**
     * 自定义异常
     *
     * @param code 返回码
     * @param msg  返回提示
     * @param data 返回结果
     * @param <T>  返回数据类型
     * @return 通用响应对象
     */
    public static <T> ResponseInfo<T> customizeError(Integer code, String msg, T data) {
        return new ResponseInfo<>(code, msg, data);
    }

}
