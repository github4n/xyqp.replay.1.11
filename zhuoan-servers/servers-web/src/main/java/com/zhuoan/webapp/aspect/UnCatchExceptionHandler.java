package com.zhuoan.webapp.aspect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * UnCatchExceptionHandler
 *
 * @author weixiang.wu
 * @date 2018 -04-02 14:20
 */
public class UnCatchExceptionHandler implements HandlerExceptionResolver {

    private static Logger logger = LoggerFactory.getLogger(UnCatchExceptionHandler.class);

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse httpServletResponse, Object o, Exception e) {
        logger.error("ZHUO_AN:异常URL【" + request.getRequestURL() + "】未成功捕捉异常：", e);
        // 跳转至404错误页面
        return new ModelAndView("/error");
    }
}
