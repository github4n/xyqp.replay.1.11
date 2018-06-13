package com.zhuoan.biz.event.gppj;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.GPPJConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:36 2018/6/9
 * @Modified By:
 **/
@Component
public class GPPJGameEvent {

    @Resource
    private Destination gppjQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerGPPJGameEvent(SocketIOServer server) {
        /**
         * 准备事件
         */
        server.addEventListener("gameReady_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_READY));
            }
        });

        /**
         * 准备事件
         */
        server.addEventListener("gameStart_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_START));
            }
        });

        /**
         * 切牌事件
         */
        server.addEventListener("gameCut_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_CUT));
            }
        });

        /**
         * 抢庄事件
         */
        server.addEventListener("gameQz_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_QZ));
            }
        });

        /**
         * 下注事件
         */
        server.addEventListener("gameXz_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_XZ));
            }
        });

        /**
         * 咪牌事件
         */
        server.addEventListener("gameShow_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_SHOW));
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_RECONNECT));
            }
        });

        /**
         * 退出事件
         */
        server.addEventListener("exitRoom_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_EXIT));
            }
        });

        /**
         * 解散房间事件
         */
        server.addEventListener("closeRoom_GPPJ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_CLOSE_ROOM));
            }
        });

    }



}
