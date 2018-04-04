package com.zhuoan.webapp.controller;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * BaseController
 *
 * @author weixiang.wu
 * @date 2018 -04-01 13:16
 */
public class BaseController {

    private static final Logger logger = LoggerFactory.getLogger(BaseController.class);

    /**
     * The com.zhuoan.constant PAGE_DEFAULT.
     */
    protected static final Integer PAGE_DEFAULT = 1;// 分页查询默认第一页
    /**
     * The com.zhuoan.constant LIMIT_DEFAULT.
     */
    protected static final Integer LIMIT_DEFAULT = 10;// 分页查询默认数量

    /** The com.zhuoan.constant IPV4_LOCAL. */
    private static final String IPV4_LOCAL = "127.0.0.1";
    /** The com.zhuoan.constant IPV6_LOCAL. */
    private static final String IPV6_LOCAL = "0:0:0:0:0:0:0:1";
    /** The com.zhuoan.constant IP_UNKNOWN. */
    private static final String IP_UNKNOWN = "unknown";


    /**
     * Gets page limit.
     *
     * @return the page limit
     */
    public int getPageLimit() {
        return LIMIT_DEFAULT;
    }


    /**
     * Gets the ip addr. 获取访问者真实IP地址
     *
     * @param request the request
     * @return the ip addr
     */
    public static String getIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || IP_UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip.equals(IPV4_LOCAL) || ip.equals(IPV6_LOCAL)) {
                // 根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    logger.error("未知主机异常:", e);
                }
                if (inet != null) {
                    ip = inet.getHostAddress();
                }
            }
        }
        // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) { // "***.***.***.***".length()
            ip = ip.substring(0, ip.indexOf(','));
        }
        return ip;
    }


    /**
     * Object to json string.
     *
     * @param <T> the type parameter
     * @param t   the t
     * @return the string
     */
    protected <T> String objectToJson(T t) {
        try {
            String response = JSONObject.toJSONString(t);
            logger.info("接口响应的数据response=[" + response + "]");
            return response;
        } catch (Exception e) {
            logger.info("接口返回数据转为json格式错误:", e);
            return null;
        }
    }
}
