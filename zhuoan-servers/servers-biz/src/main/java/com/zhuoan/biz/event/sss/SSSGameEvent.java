package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;


/**
 * The type Sss game event.
 */
@Service
public class SSSGameEvent {

    private final static Logger logger = LoggerFactory.getLogger(SSSGameEvent.class);

    @Resource
    private Destination sssQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * Listener sss game event.监听十三水游戏事件
     */
    public void listenerSSSGameEvent(SocketIOServer server) {

        /**
         * 准备方法
         */
        server.addEventListener("gameReady_SSS", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_READY));
            }
        });

        /**
         * 游戏事件
         */
        server.addEventListener("gameAction_SSS", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_EVENT));
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_SSS", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_EXIT));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_SSS", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_RECONNECT));
            }
        });
    }
}
