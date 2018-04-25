package com.zhuoan.webapp.aspect;

import com.alibaba.fastjson.JSONObject;
import com.zhuoan.enumtype.ResCodeEnum;
import com.zhuoan.exception.BizException;
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
 * CommonExceptionHandler
 *
 * @author weixiang.wu
 * @date 2018 -04-02 14:05
 */
@Component("commonExceptionHandlerAspect")
@Aspect
public class ControllerExceptionHandler {

    private static Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);


    @Pointcut("execution(* com.zhuoan.webapp.controller..*.*(..)))")
    private void webappControllerPointcut() {
    }//定义一个切入点

    /**
     * Do around response body string.
     *
     * @param call the call
     * @return the string
     * @throws Throwable the throwable
     */
    @Around("webappControllerPointcut()&&@annotation(org.springframework.web.bind.annotation.ResponseBody)")
    public String doAroundResponseBody(ProceedingJoinPoint call) throws Throwable {

        String result = null;
        // map中的内容将存放在放回的json字符串中
        Map<String, String> map = new HashMap<String, String>();

        try {
            //执行Controller
            result = (String) call.proceed();
        } catch (BizException bizE) {
            logger.error("业务异常", bizE);
            //重置result 返回业务异常
            if (bizE.getMessageMap() != null) {
                // 如果bizException包含map则直接使用
                map = bizE.getMessageMap();
            }
            map.put(ResCodeEnum.RES_CODE.getResCode(), bizE.getCode());
            map.put(ResCodeEnum.RES_MSG.getResCode(), bizE.getMessage());
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
