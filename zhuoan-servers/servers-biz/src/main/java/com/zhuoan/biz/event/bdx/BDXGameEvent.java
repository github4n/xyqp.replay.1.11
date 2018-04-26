package com.zhuoan.biz.event.bdx;

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
         * 加入房间，或创建房间事件
         */
        server.addEventListener("enterRoom_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, 10, 1));
//				try {
//					queue.addQueue(new Messages(client, data, 10, 1));
//					//bdxService.enterRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(BDXGameEvent.class).error(e.getMessage(), e);
//					logger.error("",e);
//				}


                //========================================准备定时器倒计时================================================
				/*try {
					JSONObject json = new JSONObject();
					json.element("type", 1);
					MutliThreadSSS m = new MutliThreadSSS(client, data , sssService,json);
					m.start();
				} catch (InterruptedException e) {
					logger.error("",e);
				} */

            }
        });


        /**
         * 游戏事件
         */
        server.addEventListener("gameXiazhu_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, 10, 2));
//				try {
//					queue.addQueue(new Messages(client, data, 10, 2));
//					//bdxService.gameEvent(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(BDXGameEvent.class).error(e.getMessage(), e);
//					logger.error("",e);
//				}
            }
        });

        /**
         * 游戏结算事件
         */
        server.addEventListener("gameEvent_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, 10, 3));
//				try {
//					queue.addQueue(new Messages(client, data, 10, 3));
//					//bdxService.gameSummary(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(BDXGameEvent.class).error(e.getMessage(), e);
//					logger.error("",e);
//				}
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, 10, 4));
//				try {
//					queue.addQueue(new Messages(client, data, 10, 4));
//					//bdxService.exitRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(BDXGameEvent.class).error(e.getMessage(), e);
//					logger.error("",e);
//				}
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_BDX", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(bdxQueueDestination, new Messages(client, data, 10, 5));
//				try {
//					queue.addQueue(new Messages(client, data, 10, 5));
//					//bdxService.reconnectGame(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(BDXGameEvent.class).error(e.getMessage(), e);
//					logger.error("",e);
//				}
            }
        });


    }
}
