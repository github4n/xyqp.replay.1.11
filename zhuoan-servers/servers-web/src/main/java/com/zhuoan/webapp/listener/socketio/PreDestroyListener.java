package com.zhuoan.webapp.listener.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.util.thread.ThreadPoolHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * PreDestroyListener
 *
 * @author weixiang.wu
 * @date 2018 -04-25 13:42
 */
@Component
public class PreDestroyListener {

    private final static Logger logger = LoggerFactory.getLogger(PreDestroyListener.class);

    @Resource
    private SocketIoManagerService service;

    /**
     * Start socket port.
     */
    @PostConstruct
    public void startSocketPort() {
        if (service.getServer() == null) {
            logger.info("默认启动socket 服务");
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    service.startServer();
                }
            });
        }
    }

    /**
     * Tomcat关闭时，关闭socketio服务端口
     */
    @PreDestroy
    public void closeSocketPort() {
        if (service.getServer() != null) {
            service.stopServer();
        }
    }
}
