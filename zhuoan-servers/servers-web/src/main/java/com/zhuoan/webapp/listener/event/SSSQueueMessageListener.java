package com.zhuoan.webapp.listener.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.model.GameLogsCache;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import com.zhuoan.constant.event.GidConstant;
import com.zhuoan.exception.EventException;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import java.util.UUID;


/**
 * QueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:01
 */
@Component
public class SSSQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(SSSQueueMessageListener.class);

    @Resource
    private RoomManage roomManage;
    @Resource
    private SSSGameEventDeal sssService;
    @Resource
    private GameLogsCache gameLogsCache;

    @Override
    public void onMessage(Message message) {
        String messageStr = null;
        if (message != null && message instanceof TextMessage) {
            try {
                TextMessage tm = (TextMessage) message;
                messageStr = tm.getText();
                logger.info("[" + this.getClass().getName() + "] 接收了消息 = [" + messageStr + "]");
            } catch (JMSException e) {
                throw new EventException("[" + this.getClass().getName() + "] 信息接收出现异常");
            }
        }

        JSONObject jsonObject = JSONObject.fromObject(messageStr);
        SocketIOClient client = obtainSocketIOClient(jsonObject);
        Object data = jsonObject.get("dataObject");
        Integer gid = (Integer) jsonObject.get("gid");
        Integer sorts = (Integer) jsonObject.get("sorts");
        Messages messages = new Messages(client, data, gid, sorts);
        /* todo 以上处理只是保证代码执行的准确性，需要再做优化：Messages重新封装 */


        switch (gid) {
            case GidConstant.COMMON:
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
            case GidConstant.NN:
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
            case GidConstant.SSS:
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

    private SocketIOClient obtainSocketIOClient(JSONObject jsonObject) {
        JSONObject clientObject = (JSONObject) jsonObject.get("client");
        String sessionId = String.valueOf(clientObject.get("sessionId"));
        return
            GameMain.server.getClient(UUID.fromString(sessionId));
    }


}
