package com.demo.common;

import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：所有 Controller 未捕获的异常都会到这里，统一返回 ApiResult。
 * <p>
 * HTTP 状态故意用 200，业务成败看 body.code（Demo 简化前端处理）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务主动抛出的错误 */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleBiz(BizException e) {
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /** {@code @Valid} / {@code @Validated} 失败时的参数错误 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleValid(Exception e) {
        String msg = "参数校验失败";
        if (e instanceof MethodArgumentNotValidException manv && manv.getBindingResult().getFieldError() != null) {
            msg = manv.getBindingResult().getFieldError().getDefaultMessage();
        } else if (e instanceof BindException be && be.getBindingResult().getFieldError() != null) {
            msg = be.getBindingResult().getFieldError().getDefaultMessage();
        }
        return ApiResult.fail(400, msg);
    }

    /** 兜底：未预料异常，避免把堆栈直接甩给前端 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResult<Void> handleOther(Exception e) {
        e.printStackTrace();
        return ApiResult.fail(500, "服务器内部错误");
    }
}
