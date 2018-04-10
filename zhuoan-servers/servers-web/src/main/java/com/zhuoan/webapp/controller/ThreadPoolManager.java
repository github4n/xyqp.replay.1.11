package com.zhuoan.webapp.controller;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ThreadPoolManager {
    private final static Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    private static final ExecutorService executorService = new ThreadPoolExecutor(8, 24,
        60L, TimeUnit.SECONDS,
        new LinkedBlockingQueue<Runnable>(200), new ThreadFactoryBuilder().setNameFormat("call-back-handle-pool-%d").build(), new ThreadPoolExecutor.AbortPolicy());

    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override
            public void run() {

            }
        }).start();

        executorService.submit(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

}
