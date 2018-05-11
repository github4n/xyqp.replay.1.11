package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.nn.BaseEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.constant.*;
import com.zhuoan.exception.EventException;
import com.zhuoan.service.socketio.SocketIoManagerService;
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
public class GameEventDeal {


    private final static Logger logger = LoggerFactory.getLogger(GameEventDeal.class);

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
    @Resource
    private SocketIoManagerService socketIoManagerService;

    public void eventsMQ(Message message) {
        if (GameMain.server == null) {
            // 当MQ宕机重启时，若socket服务未启动，则先启动socket服务
            socketIoManagerService.startServer();
        }
        JSONObject jsonObject = JSONObject.fromObject(obtainMessageStr(message));
        Object data = jsonObject.get("dataObject");
        Integer gid = (Integer) jsonObject.get("gid");
        Integer sorts = (Integer) jsonObject.get("sorts");
        SocketIOClient client = GameMain.server.getClient(UUID.fromString((String) jsonObject.get("sessionId")));


        // 处理队列中的信息。。。。。
        switch (gid) {
            case CommonConstant.GAME_BASE:
                switch (sorts) {
                    case CommonConstant.BASE_GAME_GET_USER_INFO:
                        baseEventDeal.getUserInfo(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_CHECK_USER:
                        baseEventDeal.checkUser(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_GAME_SETTING:
                        baseEventDeal.getGameSetting(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_ALL_ROOM_LIST:
                        baseEventDeal.getAllRoomList(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_CREATE_ROOM:
                        baseEventDeal.createRoomBase(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_JOIN_ROOM:
                        baseEventDeal.joinRoomBase(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_SHUFFLE_INFO:
                        baseEventDeal.getShuffleInfo(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_DO_SHUFFLE:
                        baseEventDeal.doShuffle(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_SEND_MESSAGE:
                        baseEventDeal.sendMessage(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_SEND_VOICE:
                        baseEventDeal.sendVoice(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_USER_GAME_LOGS:
                        baseEventDeal.getUserGameLogs(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_DISSOLVE_ROOM:
                        baseEventDeal.dissolveRoom(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_ON_OR_OFF_GAME:
                        baseEventDeal.onOrOffGame(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_SEND_NOTICE:
                        baseEventDeal.sendNotice(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_NOTICE:
                        baseEventDeal.getNotice(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_GET_ROOM_AND_PLAYER_COUNT:
                        baseEventDeal.getRoomAndPlayerCount(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_TEST:
                        baseEventDeal.test(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_SON_GAME:
                        baseEventDeal.getRoomGid(client, data);
                        break;
                    case CommonConstant.BASE_GAME_EVENT_JOIN_COIN_ROOM:
                        baseEventDeal.joinCoinRoom(client, data);
                        break;
                    default:
                        break;
                }
                break;
            case CommonConstant.GAME_ID_NN:
                // 牛牛
                switch (sorts) {
                    case NNConstant.NN_GAME_EVENT_READY:
                        nnGameEventDealNew.gameReady(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_QZ:
                        nnGameEventDealNew.gameQiangZhuang(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_XZ:
                        nnGameEventDealNew.gameXiaZhu(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_LP:
                        nnGameEventDealNew.showPai(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_EXIT:
                        nnGameEventDealNew.exitRoom(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_RECONNECT:
                        nnGameEventDealNew.reconnectGame(client, data);
                        break;
                    case NNConstant.NN_GAME_EVENT_CLOSE_ROOM:
                        nnGameEventDealNew.closeRoom(client, data);
                        break;
                    default:
                        break;
                }
                break;
            case CommonConstant.GAME_ID_SSS:
                // 十三水
                switch (sorts) {
                    case SSSConstant.SSS_GAME_EVENT_READY:
                        // 玩家准备
                        sssGameEventDealNew.gameReady(client, data);
                        break;
                    case SSSConstant.SSS_GAME_EVENT_EVENT:
                        // 游戏中
                        sssGameEventDealNew.gameEvent(client, data);
                        break;
                    case SSSConstant.SSS_GAME_EVENT_EXIT:
                        // 离开房间
                        sssGameEventDealNew.exitRoom(client, data);
                        break;
                    case SSSConstant.SSS_GAME_EVENT_RECONNECT:
                        // 断线重连
                        sssGameEventDealNew.reconnectGame(client, data);
                        break;
                    default:
                        break;
                }
                break;
            case CommonConstant.GAME_ID_ZJH:
                // 炸金花
                switch (sorts) {
                    case ZJHConstant.ZJH_GAME_EVENT_READY:
                        zjhGameEventDealNew.gameReady(client, data);
                        break;
                    case ZJHConstant.ZJH_GAME_EVENT_GAME:
                        zjhGameEventDealNew.gameEvent(client, data);
                        break;
                    case ZJHConstant.ZJH_GAME_EVENT_EXIT:
                        zjhGameEventDealNew.exitRoom(client, data);
                        break;
                    case ZJHConstant.ZJH_GAME_EVENT_RECONNECT:
                        zjhGameEventDealNew.reconnectGame(client, data);
                        break;
                    default:
                        break;
                }
                break;
            case CommonConstant.GAME_ID_BDX:
                // 比大小
                switch (sorts) {
                    case BDXConstant.BDX_GAME_EVENT_XZ:
                        bdxGameEventDealNew.xiaZhu(client, data);
                        break;
                    case BDXConstant.BDX_GAME_EVENT_GIVE_UP:
                        bdxGameEventDealNew.gameEvent(client, data);
                        break;
                    case BDXConstant.BDX_GAME_EVENT_EXIT:
                        bdxGameEventDealNew.exitRoom(client, data);
                        break;
                    case BDXConstant.BDX_GAME_EVENT_RECONNECT:
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
                logger.info("[" + this.getClass().getName() + "] 接收 = [" + messageStr + "]");
            } catch (JMSException e) {
                throw new EventException("[" + this.getClass().getName() + "] 信息接收出现异常");
            }
        }
        return messageStr;
    }
}
