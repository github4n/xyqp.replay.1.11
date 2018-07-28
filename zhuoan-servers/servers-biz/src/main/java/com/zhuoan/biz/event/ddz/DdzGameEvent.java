package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DdzConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:07 2018/6/27
 * @Modified By:
 **/
@Component
public class DdzGameEvent {
    @Resource
    private Destination ddzQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerDDZGameEvent(SocketIOServer server) {
        /**
         * 准备事件
         */
        server.addEventListener("gameReady_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_READY));
            }
        });

        /**
         * 叫地主、抢地主事件
         */
        server.addEventListener("gameLandlord_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CALL_AND_ROB));
            }
        });

        /**
         * 出牌事件
         */
        server.addEventListener("gameEvent_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_GAME_IN));
            }
        });

        /**
         * 重连事件
         */
        server.addEventListener("reconnectGame_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_RECONNECT));
            }
        });

        /**
         * 提示事件
         */
        server.addEventListener("gamePrompt_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_PROMPT));
            }
        });

        /**
         * 继续游戏事件
         */
        server.addEventListener("gameContinue_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CONTINUE));
            }
        });

        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_EXIT_ROOM));
            }
        });

        /**
         * 解散房间事件
         */
        server.addEventListener("closeRoom_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CLOSE_ROOM));
            }
        });

        /**
         * 托管事件
         */
        server.addEventListener("gameTrustee_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_TRUSTEE));
            }
        });

        /**
         * 退出房间提示事件
         */
        server.addEventListener("getOutInfo_DDZ", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_GET_OUT_INFO));
            }
        });
    }

}
