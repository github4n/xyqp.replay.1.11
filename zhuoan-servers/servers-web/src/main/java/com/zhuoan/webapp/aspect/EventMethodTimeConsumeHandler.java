package com.zhuoan.webapp.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 求事件处理方法的耗时
 *
 * @author weixiang.wu
 * @date 2018-05-02 14:43
 **/
@Component("eventMethodTimeConsumeHandler")
@Aspect
public class EventMethodTimeConsumeHandler {

    private final static Logger logger = LoggerFactory.getLogger(EventMethodTimeConsumeHandler.class);

    /**
     * 设置慢方法阀值
     */
    private final static Long slowMethodMillis = 100L;

    @Pointcut("execution(* com.zhuoan.biz.event..*.*(..)))")
    private void gamesEventDealPointcut() {
    }

    @Around("gamesEventDealPointcut())")
    public void doAroundOnMessage(ProceedingJoinPoint call) throws Throwable {
        Signature callSignature = call.getSignature();
        Long beginTime = System.currentTimeMillis();
        call.proceed();
        Long timeConsume = System.currentTimeMillis() - beginTime;
        if (timeConsume > slowMethodMillis) {
            logger.warn("慢方法 = [" + callSignature.getDeclaringTypeName() + "." + callSignature.getName() + "] 耗时 = [" + timeConsume + "] ms");
            // TODO: 2018/5/2/002 慢方法处理
        }
    }

}
