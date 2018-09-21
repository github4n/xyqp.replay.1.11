package com.zhuoan.util.thread;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ThreadPoolManager
 *
 * @author weixiang.wu
 * @date 2018-04-09 21:40
 **/
public class ThreadPoolHelper {

    public static  ExecutorService executorService = new ThreadPoolExecutor(8, Integer.MAX_VALUE,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(2), new ThreadFactoryBuilder().setNameFormat("call-back-handle-pool-%d").build(), new ThreadPoolExecutor.AbortPolicy());

}
