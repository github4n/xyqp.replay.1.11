package com.zhuoan.webapp.listener.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.model.GameLogsCache;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import com.zhuoan.constant.GamesConstant;
import com.zhuoan.queue.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;


/**
 * QueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:01
 */
@Component
public class SSSQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(SSSQueueMessageListener.class);

     public final RoomManage roomManage = new RoomManage();

     public final GameLogsCache gameLogsCache = new GameLogsCache();

    public final SSSGameEventDeal sssService = new SSSGameEventDeal();

    /**
     * 当收到消息时，自动调用该方法。
     *
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        logger.info("SSSQueueMessageListener监听开始");



        if (message instanceof TextMessage) {
            TextMessage tm = (TextMessage) message;
            try {
                logger.info("QueueMessageListener 监听到" +
                    "队列[" + String.valueOf(message.getJMSDestination()) + "] 消息：" + tm.getText());
            } catch (JMSException e) {
                logger.error("队列监听发生异常", e.getMessage());
            }
        }
        if (message instanceof Messages) {
            logger.info("当前队列监听到对象,待有相关处理..." + message);
//            TODO something ...
            Messages messages = (Messages) message;
            SocketIOClient client = messages.getClient();
            Object data = messages.getDataObject();

            // 处理队列中的信息。。。。。
            switch (messages.getGid()) {
                case GamesConstant.COMMON:
                    switch (messages.getSorts()) {
                        case 1:
                            roomManage.getGameSetting(client, data);
                            break;
                        case 2:
                            roomManage.getAllRoomList(client, data);
                            break;
                        case 3:
                            roomManage.checkUser(client, data);
                            break;
                        case 4:
                            gameLogsCache.getGameLogsList(client, data);
                            break;
                        default:
                            break;
                    }
                    break;
                case GamesConstant.NN:
                    switch (messages.getSorts()) {

                        case 15:
                            //new com.za.gameservers.sss.test.SSSGameEventDeal().createRoom(client, data);
                            roomManage.createRoomNN(client, data);
                            break;
                        case 16:
                            //new com.za.gameservers.sss.test.SSSGameEventDeal().joinRoom(client, data);
                            roomManage.joinRoomNN(client, data);
                            break;
                    }
                case GamesConstant.SSS:
                    // 十三水
                    switch (messages.getSorts()) {
                        case 1:
                            // 创建房间或加入房间
                            sssService.enterRoom(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 2:
                            // 玩家准备
                            sssService.gameReady(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 3:
                            // 游戏中
                            sssService.gameEvent(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 4:
                            // 解散房间
//								sssService.closeRoom(messages.getClient(),
//										messages.getDataObject());
                            break;
                        case 5:
                            // 离开房间
                            sssService.exitRoom(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 6:
                            // 断线重连
                            sssService.reconnectGame(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 7:
                            // 判断玩家是否需要断线重连
                            sssService.gameConnReset(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 8:
                            // 总结算
                            sssService.gameSummary(messages.getClient(),
                                messages.getDataObject());
                            break;
                        case 9:
                            // 查询更新用户实时信息
//								sssService.playerInfo(messages.getClient(),
//										messages.getDataObject());
                            break;
                        case 10:
                            // 游戏开始-发牌（没有监听事件，只有推送事件）
//								sssService.gameStart(messages.getDataObject());
                            break;
                        case 11:
                            // 游戏结算-结算（没有监听事件，只有推送事件）
//								sssService.gameEnd(messages.getDataObject());
                            break;
                        case 12:
                            // 超时准备
                            new SSSServiceImpl().readyOvertime(messages.getDataObject());
                            break;
                        case 13:
                            // 超时配牌
                            new SSSServiceImpl().peipaiOvertime(messages.getDataObject());
                            break;
                        default:
                            break;
                    }
                default:
                    break;
            }


        }


    }


}
