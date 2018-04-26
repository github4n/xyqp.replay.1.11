package com.zhuoan.webapp.listener.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

/**
 * PreDealSocketListener
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
     * 关闭socketio服务端口
     */
    @PreDestroy
    public void closeSocketPort() {
        if (service.getServer() != null) {
            service.stopServer();
        }
    }
}
