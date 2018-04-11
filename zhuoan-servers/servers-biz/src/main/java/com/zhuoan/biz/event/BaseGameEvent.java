package com.zhuoan.biz.event;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.zhuoan.constant.event.AddingEventConstant;
import com.zhuoan.constant.event.GidConstant;
import com.zhuoan.constant.event.SendingEventConstant;
import com.zhuoan.constant.event.SortsConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * BaseGameEvent
 *
 * @author weixiang.wu
 * @date 2018-04-09 19:28
 **/
@Service
public class BaseGameEvent {

    private final static Logger logger = LoggerFactory.getLogger(BaseGameEvent.class);

    @Resource
    private Destination baseQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * Listener base game event.公共事件监听
     *
     * @param server the server
     */
    public void listenerBaseGameEvent(SocketIOServer server) {

        server.addEventListener(AddingEventConstant.CHECK_USER, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, GidConstant.COMMON, SortsConstant.IN_GAME));
            }
        });

        /**
         * 心跳包
         */
        server.addEventListener(AddingEventConstant.GAME_PING, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                client.sendEvent(SendingEventConstant.GAME_PONG, obj);
            }
        });

        /**
         * 链接
         */
        server.addEventListener(AddingEventConstant.CONNECTION, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                logger.info("链接成功");
                client.sendEvent(SendingEventConstant.CONNECT, request, "成功");
            }
        });

        /**
         * 当client连接时触发此事件
         */
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                logger.info("用户 IP = [{}] with sessionId = [{}] 上线了！！！", obtainClientIp(client), client.getSessionId());
            }
        });

        /**
         * 当client离线时触发此事件
         */
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                logger.info("用户 IP = [{}] with sessionId = [{}] 离线了！！！", obtainClientIp(client), client.getSessionId());
            }
        });


    }


    /**
     * 获取设备ip
     *
     * @param client
     * @return
     */
    private String obtainClientIp(SocketIOClient client) {
        String sa = String.valueOf(client.getRemoteAddress());
        return sa.substring(1, sa.indexOf(":"));
    }
}
