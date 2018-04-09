package com.zhuoan.biz.event;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
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
    private Destination sssQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * Listener base game event.公共事件监听
     *
     * @param server the server
     */
    public void listenerBaseGameEvent(SocketIOServer server) {






        server.addEventListener("checkUser", Object.class,new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender){
                try {
                    logger.info("11");
                    producerService.sendMessage(sssQueueDestination, 123);
//
//                    queue.addQueue(new Messages(client, object, 0, 3));
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });











        /**
         * 心跳包
         */
        server.addEventListener("game_ping", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                client.sendEvent("game_pong", obj);
            }
        });

        /**
         * 链接
         */
        server.addEventListener("connection", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                logger.info("链接成功");
                client.sendEvent("connect", request, "成功");
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
