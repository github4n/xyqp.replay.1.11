package com.zhuoan.webapp.listener.socketio;

import com.zhuoan.service.socketio.SocketIoManagerService;
import org.springframework.stereotype.Component;

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

    @Resource
    private SocketIoManagerService service;

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
