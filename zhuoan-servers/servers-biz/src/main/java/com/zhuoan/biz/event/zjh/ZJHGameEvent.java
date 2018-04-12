package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
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
         * 加入房间，或创建房间事件
         */
        server.addEventListener("enterRoom_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 1));
//					queue.addQueue(new Messages(client, data, 6, 1));
                //zjhService.enterRoom(client, data);
            }
        });


        /**
         * 准备方法
         */
        server.addEventListener("gameReady_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 2));
//					queue.addQueue(new Messages(client, data, 6, 2));
                //zjhService.gameReady(client, data);
            }
        });


        /**
         * 游戏事件
         */
        server.addEventListener("gameEvent_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 3));
//					queue.addQueue(new Messages(client, data, 6, 3));
                //zjhService.gameEvent(client, data);
            }
        });


        /**
         * 申请解散房间事件
         */
        server.addEventListener("closeRoom_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 4));
//                    queue.addQueue(new Messages(client, data, 6, 4));
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 5));
//                    queue.addQueue(new Messages(client, data, 6, 5));
                //zjhService.exitRoom(client, data);
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 6));
//                    queue.addQueue(new Messages(client, data, 6, 6));
            }
        });


        /**
         * 判断玩家是否是重新连接
         */
        server.addEventListener("gameConnReset_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 7));
//                    queue.addQueue(new Messages(client, data, 6, 7));
            }
        });

        server.addEventListener("getChangeTable_ZJH", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
//                    queue.addQueue(new Messages(client, data, 6, 8));
                //zjhService.changeTable(client, data);
                producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 8));
            }
        });

    }
}
