package com.zhuoan.webapp.aspect;

import com.alibaba.fastjson.JSONObject;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.exception.EventException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * EventExceptionHandler
 *
 * @author weixiang.wu
 * @date 2018-04-11 15:06
 **/
@Component("eventExceptionHandlerAspect")
@Aspect
public class EventExceptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(EventExceptionHandler.class);

    @Pointcut("execution(* com.zhuoan.webapp.listener.event..*.*(..)))")
    private void gamesEventPointcut() {
    }//定义一个切入点


    @Around("gamesEventPointcut())")
    public String doAroundOnMessage(ProceedingJoinPoint call) throws Throwable {

        String result = null;
        // map中的内容将存放在放回的json字符串中
        Map<String, String> map = new HashMap<String, String>();

        try {
            result = (String) call.proceed();
        } catch (EventException ee) {
            logger.error("事件处理异常", ee);
            //重置result 返回业务异常
            if (ee.getMessageMap() != null) {
                // 如果ee包含map则直接使用
                map = ee.getMessageMap();
            }
            map.put(ResCodeEnum.RES_CODE.getResCode(), ee.getCode());
            map.put(ResCodeEnum.RES_MSG.getResCode(), ee.getMessage());
            try {
                result = JSONObject.toJSONString(map);
            } catch (Exception e) {
                logger.info("接口返回数据转为json格式错误:", e);
            }
        } catch (Exception e) {
            logger.error("系统异常", e);
            //重置result 返回系统异常
            map.put(ResCodeEnum.RES_CODE.getResCode(), ResCodeEnum.SYSTEM_EXCEPTION.getResCode());
            map.put(ResCodeEnum.RES_MSG.getResCode(), ResCodeEnum.SYSTEM_EXCEPTION.getResMessage());
            try {
                result = JSONObject.toJSONString(map);
            } catch (Exception ex) {
                logger.info("接口返回数据转为json格式错误:", ex);
            }
        }
        return result;
    }
}
