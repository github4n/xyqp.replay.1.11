package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.bdx.BDXGameEventDeal;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.nn.BaseEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDeal;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.model.GameLogsCache;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.event.GidConstant;
import com.zhuoan.exception.EventException;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
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
public class GameEventDeal {


    private final static Logger logger = LoggerFactory.getLogger(GameEventDeal.class);

    @Resource
    private RoomManage roomManage;
    @Resource
    private GameLogsCache gameLogsCache;
    @Resource
    private SSSGameEventDeal sssGameEventDeal;
    @Resource
    private NNGameEventDeal nnGameEventDeal;
    @Resource
    private ZJHGameEventDeal zjhGameEventDeal;
    @Resource
    private BDXGameEventDeal bdxGameEventDeal;
    @Resource
    private BaseEventDeal baseEventDeal;
    @Resource
    private NNGameEventDealNew nnGameEventDealNew;
    @Resource
    private SSSGameEventDealNew sssGameEventDealNew;
    @Resource
    private ZJHGameEventDealNew zjhGameEventDealNew;
    @Resource
    private BDXGameEventDealNew bdxGameEventDealNew;

    public void eventsMQ(Message message) {
        JSONObject jsonObject = JSONObject.fromObject(obtainMessageStr(message));
        Object data = jsonObject.get("dataObject");
        Integer gid = (Integer) jsonObject.get("gid");
        Integer sorts = (Integer) jsonObject.get("sorts");
        SocketIOClient client = obtainSocketIOClient(jsonObject);
        /* todo Messages可以重新封装 */


        // 处理队列中的信息。。。。。
        switch (gid) {
            case GidConstant.COMMON:
                switch (sorts) {
                    case 1:
                        baseEventDeal.getGameSetting(client, data);
                        break;
                    case 2:
                        baseEventDeal.getAllRoomList(client, data);
                        break;
                    case 3:
                        baseEventDeal.checkUser(client, data);
                        break;
                    case 4:
                        baseEventDeal.getUserGameLogs(client,data);
                        break;
                    case 5:
                        roomManage.getRoomCardPayInfo(client, data);
                        break;
                    case 6:
                        baseEventDeal.dissolveRoom(client, data);
                        break;
                    case 7:
                        baseEventDeal.onOrOffGame(client, data);
                        break;
                    case 8:
                        baseEventDeal.sendNotice(client, data);
                        break;
                    case 9:
                        baseEventDeal.getShuffleInfo(client, data);
                        break;
                    case 10:
                        baseEventDeal.doShuffle(client, data);
                        break;
                    case 11:
                        baseEventDeal.sendMessage(client, data);
                        break;
                    case 12:
                        baseEventDeal.sendVoice(client, data);
                        break;
                    case 13:
                        baseEventDeal.getNotice(client, data);
                        break;
                    case 14:
                        baseEventDeal.test(client, data);
                        break;
                    case 15:
                        baseEventDeal.getRoomAndPlayerCount(client, data);
                        break;
                    default:
                        break;
                }
                break;
            case GidConstant.NN:
                // 牛牛
                switch (sorts) {
                    case 1:
                        nnGameEventDeal.enterRoom(client, data);
                        break;
                    case 2:
                        nnGameEventDealNew.gameReady(client, data);
                        break;
                    case 3:
                        nnGameEventDealNew.gameXiaZhu(client, data);
                        break;
                    case 4:
                        nnGameEventDealNew.showPai(client, data);
                        break;
                    case 5:
                        nnGameEventDealNew.gameQiangZhuang(client, data);
                        break;
                    case 6:
                        nnGameEventDeal.closeRoom(client, data);
                        break;
                    case 7:
                        nnGameEventDealNew.exitRoom(client, data);
                        break;
                    case 8:
                        nnGameEventDealNew.reconnectGame(client, data);
                        break;
                    case 9:
                        nnGameEventDeal.gameConnReset(client, data);
                        break;
                    case 10:
                        nnGameEventDeal.revokeXiazhu(client, data);
                        break;
                    case 11:
                        nnGameEventDeal.sureXiazhu(client, data);
                        break;
                    case 12:
                        nnGameEventDeal.gameRuzuo(client, data);
                        break;
                    case 13:
                        nnGameEventDeal.gameZhanQi(client, data);
                        break;
                    case 14:
                        nnGameEventDeal.changeTable(client, data);
                        break;
                    case 15:
                        baseEventDeal.createRoomBase(client, data);
                        break;
                    case 16:
                        baseEventDeal.joinRoomBase(client, data);
                        break;
                    case 17:
                        // 准备定时器
                        nnGameEventDeal.autoReady(client, data);
                        break;
                    case 18:
                        // 抢庄定时器
                        nnGameEventDeal.qiangzhuang(client, data);
                        break;
                    case 19:
                        // 下注定时器
                        nnGameEventDeal.xiazhu(client, data);
                        break;
                    case 20:
                        // 亮牌定时器
                        nnGameEventDeal.showPai(client, data);
                        break;
                    case 21:
                        // 开始游戏（抢庄）定时器
                        nnGameEventDeal.startgameqz(client, data);
                        break;
                    case 22:
                        // 解散定时器
                        nnGameEventDeal.jiesan(client, data);
                        break;
                    case 23:
                        // 获取最新用户信息
                        roomManage.getUserInfo(client, data);
                        break;
                    case 24:
                        // 获取最新用户信息
                        nnGameEventDealNew.gameOvertime(data);
                        break;

                    default:
                        break;
                }
                break;
            case GidConstant.SSS:
                // 十三水
                switch (sorts) {
                    case 1:
                        // 创建房间或加入房间
                        sssGameEventDeal.enterRoom(client, data);
                        break;
                    case 2:
                        // 玩家准备
                        sssGameEventDealNew.gameReady(client, data);
                        break;
                    case 3:
                        // 游戏中
                        sssGameEventDealNew.gameEvent(client, data);
                        break;
                    case 4:
                        // 解散房间
                        sssGameEventDeal.closeRoom(client, data);
                        break;
                    case 5:
                        // 离开房间
                        sssGameEventDealNew.exitRoom(client, data);
                        break;
                    case 6:
                        // 断线重连
                        sssGameEventDealNew.reconnectGame(client, data);
                        break;
                    case 7:
                        // 判断玩家是否需要断线重连
                        sssGameEventDeal.gameConnReset(client, data);
                        break;
                    case 8:
                        // 总结算
                        sssGameEventDeal.gameSummary(client, data);
                        break;
                    case 9:
                        // 查询更新用户实时信息
                        sssGameEventDeal.playerInfo(client, data);
                        break;
                    case 10:
                        // 游戏开始-发牌（没有监听事件，只有推送事件）
                        sssGameEventDeal.gameStart(data);
                        break;
                    case 11:
                        // 游戏结算-结算（没有监听事件，只有推送事件）
                        sssGameEventDeal.gameEnd(data);
                        break;
                    default:
                        break;
                }
                break;
            case GidConstant.ZJH:
                // 炸金花
                switch (sorts) {
                    case 1:
                        zjhGameEventDeal.enterRoom(client, data);
                        break;
                    case 2:
                        zjhGameEventDealNew.gameReady(client, data);
                        break;
                    case 3:
                        zjhGameEventDealNew.gameEvent(client, data);
                        break;
                    case 4:
                        zjhGameEventDeal.closeRoom(client, data);
                        break;
                    case 5:
                        zjhGameEventDealNew.exitRoom(client, data);
                        break;
                    case 6:
                        zjhGameEventDealNew.reconnectGame(client, data);
                        break;
                    case 7:
                        zjhGameEventDeal.gameConnReset(client, data);
                        break;
                    case 8:
                        zjhGameEventDeal.changeTable(client, data);
                        break;
                    case 9:
                        zjhGameEventDeal.ready(client, data);
                        break;
                    case 10:
                        zjhGameEventDeal.xiazhu(client, data);
                        break;
                    case 11:
                        zjhGameEventDeal.gendaodi(client, data);
                        break;

                    default:
                        break;
                }
                break;
            case GidConstant.BDX:
                // 比大小
                switch (sorts) {
                    case 1:
                        bdxGameEventDeal.enterRoom(client, data);
                        break;
                    case 2:
                        bdxGameEventDealNew.xiaZhu(client, data);
                        break;
                    case 3:
                        bdxGameEventDealNew.gameEvent(client, data);
                        break;
                    case 4:
                        bdxGameEventDealNew.exitRoom(client, data);
                        break;
                    case 5:
                        bdxGameEventDealNew.reconnectGame(client, data);
                        break;

                    default:
                        break;
                }
                break;
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
        if (Dto.isObjNull(clientObject)) {
            return null;
        }
        String sessionId = String.valueOf(clientObject.get("sessionId"));
        return GameMain.server.getClient(UUID.fromString(sessionId));
    }
}
