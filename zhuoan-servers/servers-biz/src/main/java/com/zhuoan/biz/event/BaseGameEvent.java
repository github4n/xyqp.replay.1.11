package com.zhuoan.biz.event;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.event.AddingEventConstant;
import com.zhuoan.constant.event.SendingEventConstant;
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
         *  刷新用户信息
         */
        server.addEventListener("getUserInfo", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_GET_USER_INFO));
            }
        });

        /**
         *  检查用户是否在房间内
         */
        server.addEventListener(AddingEventConstant.CHECK_USER, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_CHECK_USER));
            }
        });

        /**
         *  获取游戏设置
         */
        server.addEventListener(AddingEventConstant.GET_GAME_SETTING, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_GAME_SETTING));
            }
        });

        /**
         * 玩家获取房间列表
         */
        server.addEventListener(AddingEventConstant.GET_ALL_ROOM_LIST, Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_ALL_ROOM_LIST));
            }
        });

        /**
         *   创建房间
         */
        server.addEventListener("createRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_CREATE_ROOM));
            }
        });

        /**
         * 加入房间
         */
        server.addEventListener("joinRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, data, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
            }
        });

        /**
         *  获取洗牌信息
         */
        server.addEventListener("xipaiMessa", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_SHUFFLE_INFO));
            }
        });

        /**
         *   洗牌
         */
        server.addEventListener("xipaiFun", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE,CommonConstant.BASE_GAME_EVENT_DO_SHUFFLE));
            }
        });

        /**
         *   发送消息
         */
        server.addEventListener("sendMsgEvent", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_SEND_MESSAGE));
            }
        });
        /**
         *   发送语音
         */
        server.addEventListener("voiceCallGame", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_SEND_VOICE));
            }
        });

        /**
         * 获取玩家战绩
         */
        server.addEventListener("getGameLogsList", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_USER_GAME_LOGS));
            }
        });

        /**
         *   解散房间
         */
        server.addEventListener("dissolveRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_DISSOLVE_ROOM));
            }
        });

        /**
         *   开关游戏
         */
        server.addEventListener("onOrOffGame", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_ON_OR_OFF_GAME));
            }
        });
        /**
         *   发送滚动公告
         */
        server.addEventListener("sendNotice", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_SEND_NOTICE));
            }
        });

        /**
         *   获取滚动公告
         */
        server.addEventListener("getMessage", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_NOTICE));
            }
        });

        /**
         *   获取当前房间数玩家数
         */
        server.addEventListener("getRoomAndPlayerCount", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_GET_ROOM_AND_PLAYER_COUNT));
            }
        });

        /**
         *   测试接口
         */
        server.addEventListener("testInterface", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_TEST));
            }
        });

        /**
         *  子游戏接口
         */
        server.addEventListener("getRoomGid", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_SON_GAME));
            }
        });
        /**
         *  金币场加入房间
         */
        server.addEventListener("createRoomCoins", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender) {
                producerService.sendMessage(baseQueueDestination, new Messages(client, object, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_COIN_ROOM));
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
