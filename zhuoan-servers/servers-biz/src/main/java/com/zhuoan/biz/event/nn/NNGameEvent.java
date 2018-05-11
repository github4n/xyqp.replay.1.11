package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * 监听事件
 */
@Service
public class NNGameEvent {

    @Resource
    private Destination nnQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerNNGameEvent(SocketIOServer server) {
        /**
         * 准备方法
         */
        server.addEventListener("gameReady_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_READY));
            }
        });

        /**
         * 下注方法
         */
        server.addEventListener("gameXiaZhu_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_XZ));
            }
        });

        /**
         * 游戏事件
         */
        server.addEventListener("gameEvent_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_LP));
            }
        });

        /**
         * 抢庄事件
         */
        server.addEventListener("qiangZhuang_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_QZ));
            }
        });

        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_EXIT));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_RECONNECT));
            }
        });

        /**
         * 断线重连事件
         */
        server.addEventListener("closeRoom_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_NN, NNConstant.NN_GAME_EVENT_CLOSE_ROOM));
            }
        });
    }
}
