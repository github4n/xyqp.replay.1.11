package com.zhuoan.biz.event.sw;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SwConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:51 2018/6/19
 * @Modified By:
 **/
@Service
public class SwGameEvent {

    @Resource
    private Destination swQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerSwGameEvent(SocketIOServer server) {
        /**
         * 开始游戏事件
         */
        server.addEventListener("gameStart_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_START_GAME));
            }
        });

        /**
         * 下注事件
         */
        server.addEventListener("gameBet_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_BET));
            }
        });

        /**
         * 上庄事件
         */
        server.addEventListener("gameBeBanker_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_BE_BANKER));
            }
        });

        /**
         * 撤销事件
         */
        server.addEventListener("gameUndo_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_UNDO));
            }
        });

        /**
         * 退出事件
         */
        server.addEventListener("exitRoom_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_EXIT_ROOM));
            }
        });

        /**
         * 换桌事件
         */
        server.addEventListener("gameChangeSeat_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_CHANGE_SEAT));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_RECONNECT));
            }
        });

        /**
         * 走势图事件
         */
        server.addEventListener("getHistory_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_GET_HISTORY));
            }
        });

        /**
         * 玩家列表事件
         */
        server.addEventListener("getAllUsers_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_GET_ALL_USER));
            }
        });

        /**
         * 玩家列表事件
         */
        server.addEventListener("gameHide_SW", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(swQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SW, SwConstant.SW_GAME_EVENT_HIDE_TREASURE));
            }
        });

    }
}
