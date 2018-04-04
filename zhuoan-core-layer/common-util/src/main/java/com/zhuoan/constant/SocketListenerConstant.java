package com.zhuoan.constant;

/**
 * SocketListenerConstant
 *
 * @author weixiang.wu
 * @date 2018-04-02 15:39
 **/
public class SocketListenerConstant {
    /**
     * 执行周期，时间单位为毫秒
     */
    public static final Long CACHE_TIME = Long.MAX_VALUE;

    /**
     * 执行任务前的延迟时间，单位是毫秒
     */
    public static final Integer DELAY = 3000;
    public static final int WORKER_THREADS = 2;
    public static final int MAX_FRAME_PAYLOAD_LENGTH = 1024 * 1024;
    public static final int MAX_HTTP_CONTENT_LENGTH = 1024 * 1024;
}
