package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
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
         * 加入房间 or 创建房间
         */
        server.addEventListener("enterRoom_SSS", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {

					/*getAllClients() 返回默认名称空间中的所有客户端实例。
					getBroadcastOperations() 返回默认名称空间的所有实例组成的广播对象。
					getRoomOperations() 返回所有命名空间中指定房间的广播对象，如果命名空间只有一个，该方法到可以大胆使用。
					getClient(uid) 返回默认名称空间的指定客户端。
					getNamespace() 返回指定名称的命名空间。
					getAllClients() 获得本namespace中的所有客户端。
					getClient() 获得指定id客户端对象。
					getRoomClients(room) 获得本空间中指定房间中的客户端。
					getRooms() 获得本空间中的所有房间。
					getRooms(client) 获得指定客户端所在的房间列表。
					leave(room,uuid) 将指定客户端离开指定房间，如果房间中已无客户端，删除该房间。
					getBroadcastOperations 返回针对空间中所有客户端的广播对象。
					getRoomOperations(room) 返回针对指定房间的广播对象。*/

                producerService.sendMessage(sssQueueDestination, 123);
//                    queue.addQueue(new Messages(client, data, 4, 1));


                //queue.execute();
//					sssService.enterRoom(client, data);


                //========================================准备定时器倒计时================================================
				/*try {
					JSONObject json = new JSONObject();
					json.element("type", 1);
					MutliThreadSSS m = new MutliThreadSSS(client, data , sssService,json);
					m.start();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} */

            }
        });

        /**
         * 玩家获取房间列表
         */
        server.addEventListener("getAllRoomList", Object.class,new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object object, AckRequest ackSender){
                try {
                    producerService.sendMessage(sssQueueDestination, 123);
//                    queue.addQueue(new Messages(client, object, 0, 2));
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });


        /**
         * 准备方法
         */
//        server.addEventListener("gameReady_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 2));
//                    //queue.execute();
//                    //sssService.gameReady(client, data);
////					sssGameEventDeal.gameReady(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//
//
//            }
//        });
        server.addEventListener("getGameSetting", Object.class,new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender){

                try {
//                    queue.addQueue(new Messages(client, data, 0, 1));
logger.info("11");
                    //nnService.enterRoom(client, data);
                } catch (Exception e) {
                    //Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });
//
//
//

//
//
//

//
//
        server.addEventListener("getGameLogsList", Object.class,new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender){

                try {
                    logger.info("11");
//                    queue.addQueue(new Messages(client, data, 0, 4));
                    //nnService.enterRoom(client, data);
                } catch (Exception e) {
                    //Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
                    e.printStackTrace();
                }
            }
        });
//        server.addEventListener("joinRoomNN", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 1, 16));
//                    //nnService.enterRoom(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        server.addEventListener("createRoomNN", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 1, 15));
//                    // nnService.enterRoom(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//        /**
//         * 游戏事件
//         */
//        server.addEventListener("gameEvent_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 3));
//                    //queue.execute();
//                    //sssService.gameEvent(client, data);
////					sssGameEventDeal.gameEvent(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//        /**
//         * 申请解散房间事件
//         */
//        server.addEventListener("closeRoom_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 4));
//                    //queue.execute();
//                    //sssService.closeRoom(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//        /**
//         * 退出房间事件
//         */
//        server.addEventListener("exitRoom_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 5));
//                    //queue.execute();
//                    //sssService.exitRoom(client, data);
////					sssGameEventDeal.exitRoom(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//        /**
//         * 断线重连事件
//         */
//        server.addEventListener("reconnectGame_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 6));
//                    //queue.execute();
//                    //sssService.reconnectGame(client, data);
////					sssGameEventDeal.reconnectGame(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//        /**
//         * 判断玩家是否是重新连接
//         */
//        server.addEventListener("gameConnReset_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 7));
//                    //queue.execute();
//                    //sssService.gameConnReset(client, data);
////					sssGameEventDeal.gameConnReset(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        /**
//         * 游戏房间总结算
//         */
//        server.addEventListener("gameSummary_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4, 8));
//                    //queue.execute();
//                    //sssService.gameSummary(client, data);
////					sssGameEventDeal.gameSummary(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//        /**
//         * 更新用户实时信息
//         */
//        server.addEventListener("playerInfo_SSS", Object.class,new DataListener<Object>() {
//
//            @Override
//            public void onData(SocketIOClient client, Object data, AckRequest ackSender){
//
//                try {
//                    queue.addQueue(new Messages(client, data, 4,9));
//                    //queue.execute();
//                    //sssService.playerInfo(client, data);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        });
    }
}
