package com.demo.common;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标在 Controller 方法上，由 {@link OperLogAspect} 拦截打印。
 * <p>
 * {@code value} 是操作名称，例如「分页查询用户」。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    String value() default "";
}
