package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.model.GameLogsCache;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import com.zhuoan.constant.event.GidConstant;
import com.zhuoan.exception.EventException;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.UUID;

/**
 * MessageQueueDeal
 *
 * @author weixiang.wu
 * @date 2018-04-11 18:01
 **/
@Component
public class GameEventsImpl {


    private final static Logger logger = LoggerFactory.getLogger(GameEventsImpl.class);

    @Resource
    private RoomManage roomManage;
    @Resource
    private SSSGameEventDeal sssService;
    @Resource
    private GameLogsCache gameLogsCache;

    public void EventsMQDeal(Message message) {
        JSONObject jsonObject = JSONObject.fromObject(obtainMessageStr(message));
        Object data = jsonObject.get("dataObject");
        Integer gid = (Integer) jsonObject.get("gid");
        Integer sorts = (Integer) jsonObject.get("sorts");
        SocketIOClient client = obtainSocketIOClient(jsonObject);
        /* todo Messages可以重新封装 */


        switch (gid) {
            case GidConstant.COMMON:
                switch (sorts) {
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
                switch (sorts) {
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
                switch (sorts) {
                    case 1:
                        // 创建房间或加入房间
                        sssService.enterRoom(client, data);
                        break;
                    case 2:
                        // 玩家准备
                        sssService.gameReady(client, data);
                        break;
                    case 3:
                        // 游戏中
                        sssService.gameEvent(client, data);
                        break;
                    case 4:
                        // 解散房间
//								sssService.closeRoom(client,
//										data());
                        break;
                    case 5:
                        // 离开房间
                        sssService.exitRoom(client, data);
                        break;
                    case 6:
                        // 断线重连
                        sssService.reconnectGame(client, data);
                        break;
                    case 7:
                        // 判断玩家是否需要断线重连
                        sssService.gameConnReset(client, data);
                        break;
                    case 8:
                        // 总结算
                        sssService.gameSummary(client, data);
                        break;
                    case 9:
                        // 查询更新用户实时信息
//								sssService.playerInfo(client,data);
                        break;
                    case 10:
                        // 游戏开始-发牌（没有监听事件，只有推送事件）
//								sssService.gameStart(data);
                        break;
                    case 11:
                        // 游戏结算-结算（没有监听事件，只有推送事件）
//								sssService.gameEnd(data);
                        break;
                    case 12:
                        // 超时准备
                        new SSSServiceImpl().readyOvertime(data);
                        break;
                    case 13:
                        // 超时配牌
                        new SSSServiceImpl().peipaiOvertime(data);
                        break;
                    default:
                        break;
                }
            default:
                break;
        }
    }

    private Object obtainMessageStr(Message message) {
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
        return messageStr;
    }

    private SocketIOClient obtainSocketIOClient(JSONObject jsonObject) {
        JSONObject clientObject = (JSONObject) jsonObject.get("client");
        String sessionId = String.valueOf(clientObject.get("sessionId"));
        return
            GameMain.server.getClient(UUID.fromString(sessionId));
    }
}
