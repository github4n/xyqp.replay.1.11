package com.zhuoan.webapp.listener.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.util.thread.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import javax.annotation.Resource;

/**
 * 在一些业务场景中，当容器初始化完成之后，需要处理一些操作，比如一些数据的加载、初始化缓存、特定任务的注册等等。
 * 这个时候我们就可以使用Spring提供的ApplicationListener来进行操作
 *
 * @author weixiang.wu
 * @date 2018-04-02 10:53
 **/
public class PostConstructListener implements ApplicationListener<ContextRefreshedEvent> {

    private final static Logger logger = LoggerFactory.getLogger(PostConstructListener.class);

    @Resource
    private SocketIoManagerService socketIoManagerService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        logger.info("默认启动socket 服务");
        if (socketIoManagerService.getServer() == null) {
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    socketIoManagerService.startServer();
                }
            });
        }
    }


}
