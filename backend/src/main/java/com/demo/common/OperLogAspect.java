package com.demo.common;

import com.demo.security.LoginUser;
import com.demo.security.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class OperLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperLogAspect.class);

    @Around("@annotation(operLog)")
    public Object around(ProceedingJoinPoint pjp, OperLog operLog) throws Throwable {
        long start = System.currentTimeMillis();
        String who = "anonymous";
        LoginUser user = SecurityUtils.getCurrentUserOrNull();
        if (user != null) {
            who = user.getUsername() + "#" + user.getUserId();
        }
        try {
            Object result = pjp.proceed();
            log.info("[OperLog] op={}, user={}, cost={}ms, args={}",
                    operLog.value(), who, System.currentTimeMillis() - start, pjp.getArgs());
            return result;
        } catch (Throwable ex) {
            log.warn("[OperLog] op={}, user={}, cost={}ms, error={}",
                    operLog.value(), who, System.currentTimeMillis() - start, ex.getMessage());
            throw ex;
        }
    }
}
