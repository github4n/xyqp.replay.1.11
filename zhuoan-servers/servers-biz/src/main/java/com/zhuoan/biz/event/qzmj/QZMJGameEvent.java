package com.zhuoan.biz.event.qzmj;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:44 2018/5/21
 * @Modified By:
 **/
@Service
public class QZMJGameEvent {
    @Resource
    private Destination qzmjQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerQZMJGameEvent(SocketIOServer server) {
        /**
         * 准备方法
         */
        server.addEventListener("gameReady", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_READY));
            }
        });

        /**
         * 出牌方法
         */
        server.addEventListener("gameChupai", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_CP));
            }
        });

        /**
         * 游戏事件
         */
        server.addEventListener("gameEvent", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_IN));
            }
        });

        /**
         * 杠完出牌事件
         */
        server.addEventListener("gangChupaiEvent", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_GANG_CP));
            }
        });

        /**
         * 解散房间事件
         */
        server.addEventListener("closeRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_CLOSE_ROOM));
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_EXIT_ROOM));
            }
        });

        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_RECONNECT));
            }
        });
    }
}
