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
 * @date 2018 -04-09 21:40
 */
public class ThreadPoolHelper {

    private static final int CORE_POOL_SIZE = 8;

    private static final int MAXIMUM_POOL_SIZE = 24;

    private static final long KEEP_ALIVE_TIME = 60L;
    private static final TimeUnit UNIT = TimeUnit.SECONDS;

    private static final int CAPACITY = 200;

    private static final String NAME_FORMAT = "call-back-handle-pool-%d";

    /**
     * The constant executorService.
     */
    public static ExecutorService executorService =
        /**
         * Creates a new {@code ThreadPoolExecutor} with the given initial
         * parameters.
         *
         * @param corePoolSize the number of threads to keep in the pool, even
         *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
         * @param maximumPoolSize the maximum number of threads to allow in the
         *        pool
         * @param keepAliveTime when the number of threads is greater than
         *        the core, this is the maximum time that excess idle threads
         *        will wait for new tasks before terminating.
         * @param unit the time unit for the {@code keepAliveTime} argument
         * @param workQueue the queue to use for holding tasks before they are
         *        executed.  This queue will hold only the {@code Runnable}
         *        tasks submitted by the {@code execute} method.
         * @param threadFactory the factory to use when the executor
         *        creates a new thread
         * @param handler the handler to use when execution is blocked
         *        because the thread bounds and queue capacities are reached
         * @throws IllegalArgumentException if one of the following holds:<br>
         *         {@code corePoolSize < 0}<br>
         *         {@code keepAliveTime < 0}<br>
         *         {@code maximumPoolSize <= 0}<br>
         *         {@code maximumPoolSize < corePoolSize}
         * @throws NullPointerException if {@code workQueue}
         *         or {@code threadFactory} or {@code handler} is null
         */
        new ThreadPoolExecutor(
            CORE_POOL_SIZE,
            MAXIMUM_POOL_SIZE,
            KEEP_ALIVE_TIME,
            UNIT,
            //使用一个基于FIFO排序的阻塞队列，在所有corePoolSize线程都忙时新任务将在队列中等待
            new LinkedBlockingQueue<Runnable>(CAPACITY),
            new ThreadFactoryBuilder().setNameFormat(NAME_FORMAT).build(),
            // RejectedExecutionHandler的四种策略：CallerRunsPolicy、AbortPolicy、DiscardPolicy、DiscardOldestPolicy
            new ThreadPoolExecutor.AbortPolicy()
        );


}
