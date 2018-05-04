package com.zhuoan.biz.event.bdx;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.BDXConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * 比大小游戏监听事件
 */
@Service
public class BDXGameEvent {

    @Resource
    private Destination bdxQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerBDXGameEvent(SocketIOServer server) {
        /**
         * 游戏事件
         */
        server.addEventListener("gameXiazhu_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_BDX, BDXConstant.BDX_GAME_EVENT_XZ));
            }
        });

        /**
         * 游戏结算事件
         */
        server.addEventListener("gameEvent_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_BDX, BDXConstant.BDX_GAME_EVENT_GIVE_UP));
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_BDX, BDXConstant.BDX_GAME_EVENT_EXIT));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_BDX, BDXConstant.BDX_GAME_EVENT_RECONNECT));
            }
        });


    }
}
