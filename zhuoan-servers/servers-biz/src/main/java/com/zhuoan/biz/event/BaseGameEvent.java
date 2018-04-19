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
         * CHECK_USER 03
         */
        server.addEventListener(AddingEventConstant.CHECK_USER, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, GidConstant.COMMON, SortsConstant.IN_GAME));
            }
        });

        /**
         * 玩家获取房间列表 02
         */
        server.addEventListener(AddingEventConstant.GET_ALL_ROOM_LIST, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, GidConstant.COMMON, 2));
            }
        });


        /**
         * 大厅中选中一款游戏，点击‘创建房间’  01              or 加入房间
         */
        server.addEventListener("getGameSetting", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, GidConstant.COMMON, SortsConstant.ENTER_ROOM));
            }
        });

        server.addEventListener("getGameLogsList", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, GidConstant.COMMON, 4));
            }
        });


        /**
         *    点 " 确认开房 "
         */
        server.addEventListener("createRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                //todo   公共的统一   eventName   gid  sorts 常量类统一
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, 1, 15));
            }
        });

        /**
         *   刷新用户信息
         */
        server.addEventListener("getUserInfo", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, 1, 23));
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
