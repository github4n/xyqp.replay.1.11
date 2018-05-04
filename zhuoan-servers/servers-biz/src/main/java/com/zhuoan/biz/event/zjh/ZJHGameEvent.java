package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.ZJHConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * 监听事件
 */
@Service
public class ZJHGameEvent {

    @Resource
    private Destination zjhQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerZJHGameEvent(SocketIOServer server) {

        /**
         * 准备方法
         */
        server.addEventListener("gameReady_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_READY));
            }
        });


        /**
         * 游戏事件
         */
        server.addEventListener("gameEvent_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_GAME));
            }
        });

        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH,ZJHConstant.ZJH_GAME_EVENT_EXIT));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_RECONNECT));
            }
        });
    }
}
