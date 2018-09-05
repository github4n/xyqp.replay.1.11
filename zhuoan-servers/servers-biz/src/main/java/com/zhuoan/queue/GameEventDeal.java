package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.club.ClubEventDeal;
import com.zhuoan.biz.event.ddz.DdzGameEventDeal;
import com.zhuoan.biz.event.gppj.GPPJGameEventDeal;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.sw.SwGameEventDeal;
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
    private QZMJGameEventDeal qzmjGameEventDeal;

    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;

    @Resource
    private SwGameEventDeal swGameEventDeal;

    @Resource
    private DdzGameEventDeal ddzGameEventDeal;

    @Resource
    private MatchEventDeal matchEventDeal;

    @Resource
    private ClubEventDeal clubEventDeal;

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
                baseEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_NN:
                // 牛牛
                NNEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_SSS:
                // 十三水
                SSSEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_ZJH:
                // 炸金花
                ZJHEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_BDX:
                // 比大小
                BDZEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_QZMJ:
                // 泉州麻将
                QzMjEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                // 骨牌牌九
                GpPjEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_SW:
                // 水蛙
                swEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_ID_DDZ:
                // 斗地主
                ddzEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_MATCH:
                // 比赛场
                matchEvents(data, sorts, client);
                break;
            case CommonConstant.GAME_CLUB:
                clubEvents(data, sorts, client);
                break;
            default:
                break;
        }
    }

    private void clubEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case ClubConstant.CLUB_EVENT_GET_MY_CLUB_LIST:
                clubEventDeal.getMyClubList(client, data);
                break;
            case ClubConstant.CLUB_EVENT_GET_CLUB_MEMBERS:
                clubEventDeal.getClubMembers(client, data);
                break;
            case ClubConstant.CLUB_EVENT_GET_CLUB_SETTING:
                clubEventDeal.getClubSetting(client, data);
                break;
            case ClubConstant.CLUB_EVENT_CHANGE_CLUB_SETTING:
                clubEventDeal.changeClubSetting(client, data);
                break;
            case ClubConstant.CLUB_EVENT_EXIT_CLUB:
                clubEventDeal.exitClub(client, data);
                break;
            case ClubConstant.CLUB_EVENT_TO_TOP:
                clubEventDeal.toTop(client, data);
                break;
            case ClubConstant.CLUB_EVENT_REFRESH_CLUB_INFO:
                clubEventDeal.refreshClubInfo(client, data);
                break;
            case ClubConstant.CLUB_EVENT_QUICK_JOIN_CLUB_ROOM:
                clubEventDeal.quickJoinClubRoom(client, data);
                break;
            default:
                break;
        }
    }

    private void matchEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case MatchConstant.MATCH_EVENT_GET_MATCH_SETTING:
                matchEventDeal.obtainMatchInfo(client,data);
                break;
            case MatchConstant.MATCH_EVENT_SIGN_UP:
                matchEventDeal.matchSignUp(client,data);
                break;
            case MatchConstant.MATCH_EVENT_UPDATE_MATCH_COUNT:
                matchEventDeal.updateMatchCount(client,data);
                break;
            case MatchConstant.MATCH_EVENT_CANCEL_SIGN:
                matchEventDeal.matchCancelSign(client,data);
                break;
            case MatchConstant.MATCH_EVENT_GET_WINNING_RECORD:
                matchEventDeal.getWinningRecord(client,data);
                break;
            case MatchConstant.MATCH_EVENT_GET_SIGN_UP_INFO:
                matchEventDeal.getSignUpInfo(client,data);
                break;
            default:
                break;
        }
    }

    private void ddzEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case DdzConstant.DDZ_GAME_EVENT_READY:
                ddzGameEventDeal.gameReady(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_CALL_AND_ROB:
                ddzGameEventDeal.gameBeLandlord(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_GAME_IN:
                ddzGameEventDeal.gameEvent(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_RECONNECT:
                ddzGameEventDeal.reconnectGame(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_PROMPT:
                ddzGameEventDeal.gamePrompt(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_CONTINUE:
                ddzGameEventDeal.gameContinue(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_EXIT_ROOM:
                ddzGameEventDeal.exitRoom(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_CLOSE_ROOM:
                ddzGameEventDeal.closeRoom(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_TRUSTEE:
                ddzGameEventDeal.gameTrustee(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_AUTO_PLAY:
                ddzGameEventDeal.gameAutoPlay(client,data);
                break;
            case DdzConstant.DDZ_GAME_EVENT_GET_OUT_INFO:
                ddzGameEventDeal.getOutInfo(client,data);
                break;
            default:
                break;
        }
    }

    private void swEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case SwConstant.SW_GAME_EVENT_START_GAME:
                swGameEventDeal.gameStart(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_BET:
                swGameEventDeal.gameBet(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_BE_BANKER:
                swGameEventDeal.gameBeBanker(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_UNDO:
                swGameEventDeal.gameUndo(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_EXIT_ROOM:
                swGameEventDeal.exitRoom(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_CHANGE_SEAT:
                swGameEventDeal.gameChangeSeat(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_RECONNECT:
                swGameEventDeal.reconnectGame(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_GET_HISTORY:
                swGameEventDeal.getHistory(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_GET_ALL_USER:
                swGameEventDeal.getAllUser(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_HIDE_TREASURE:
                swGameEventDeal.gameHide(client,data);
                break;
            case SwConstant.SW_GAME_EVENT_GET_UNDO_INFO:
                swGameEventDeal.getUndoInfo(client,data);
                break;
            default:
                break;
        }
    }

    private void GpPjEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case GPPJConstant.GP_PJ_GAME_EVENT_READY:
                gppjGameEventDeal.gameReady(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_START:
                gppjGameEventDeal.gameStart(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_CUT:
                gppjGameEventDeal.gameCut(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_QZ:
                gppjGameEventDeal.gameQz(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_XZ:
                gppjGameEventDeal.gameXz(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_SHOW:
                gppjGameEventDeal.gameShow(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_RECONNECT:
                gppjGameEventDeal.reconnectGame(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_EXIT:
                gppjGameEventDeal.exitRoom(client,data);
                break;
            case GPPJConstant.GP_PJ_GAME_EVENT_CLOSE_ROOM:
                gppjGameEventDeal.closeRoom(client,data);
                break;
            default:
                break;
        }
    }

    private void QzMjEvents(Object data, Integer sorts, SocketIOClient client) {
        switch (sorts) {
            case QZMJConstant.QZMJ_GAME_EVENT_READY:
                qzmjGameEventDeal.loadFinish(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_CP:
                qzmjGameEventDeal.gameChuPai(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_IN:
                qzmjGameEventDeal.gameEvent(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_GANG_CP:
                qzmjGameEventDeal.gangChupaiEvent(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_CLOSE_ROOM:
                qzmjGameEventDeal.closeRoom(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_EXIT_ROOM:
                qzmjGameEventDeal.exitRoom(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_RECONNECT:
                qzmjGameEventDeal.reconnectGame(client,data);
                break;
            case QZMJConstant.QZMJ_GAME_EVENT_TRUSTEE:
                qzmjGameEventDeal.gameTrustee(client,data);
                break;
            default:
                break;
        }
    }

    private void BDZEvents(Object data, Integer sorts, SocketIOClient client) {
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
    }

    private void ZJHEvents(Object data, Integer sorts, SocketIOClient client) {
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
            case ZJHConstant.ZJH_GAME_EVENT_CLOSE_ROOM:
                zjhGameEventDealNew.closeRoom(client, data);
                break;
            default:
                break;
        }
    }

    private void SSSEvents(Object data, Integer sorts, SocketIOClient client) {
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
            case SSSConstant.SSS_GAME_EVENT_CLOSE_ROOM:
                // 断线重连
                sssGameEventDealNew.closeRoom(client, data);
                break;
            case SSSConstant.SSS_GAME_EVENT_BE_BANKER:
                // 上庄
                sssGameEventDealNew.gameBeBanker(client, data);
                break;
            case SSSConstant.SSS_GAME_EVENT_XZ:
                // 下注
                sssGameEventDealNew.gameXiaZhu(client, data);
                break;
            default:
                break;
        }
    }

    private void NNEvents(Object data, Integer sorts, SocketIOClient client) {
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
            case NNConstant.NN_GAME_EVENT_BE_BANKER:
                nnGameEventDealNew.gameBeBanker(client, data);
                break;
            default:
                break;
        }
    }

    private void baseEvents(Object data, Integer sorts, SocketIOClient client) {
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
            case CommonConstant.BASE_GAME_EVENT_SON_GAME:
                baseEventDeal.getRoomGid(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_JOIN_COIN_ROOM:
                baseEventDeal.joinCoinRoom(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_ROOM_CARD_PAY_INFO:
                baseEventDeal.getRoomCardPayInfo(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_COIN_SETTING:
                baseEventDeal.getCoinSetting(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_USER_SIGN_INFO:
                baseEventDeal.checkSignIn(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_DO_USER_SIGN:
                baseEventDeal.doUserSignIn(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_COMPETITIVE_INFO:
                baseEventDeal.getCompetitiveInfo(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_JOIN_COMPETITIVE_ROOM:
                baseEventDeal.joinCompetitiveRoom(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_CHECK_IP:
                baseEventDeal.gameCheckIp(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_PROXY_ROOM_LIST:
                baseEventDeal.getProxyRoomList(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_DISSOLVE_PROXY_ROOM:
                baseEventDeal.dissolveProxyRoom(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_USER_ACHIEVEMENT_INFO:
                baseEventDeal.getUserAchievementInfo(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_PROPS_INFO:
                baseEventDeal.getPropsInfo(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_USER_PURCHASE:
                baseEventDeal.userPurchase(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_ACHIEVEMENT_RANK:
                baseEventDeal.getAchievementRank(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_DRAW_INFO:
                baseEventDeal.getDrawInfo(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GAME_DRAW:
                baseEventDeal.gameDraw(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_ACHIEVEMENT_DETAIL:
                baseEventDeal.getAchievementDetail(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_DRAW_ACHIEVEMENT_REWARD:
                baseEventDeal.drawAchievementReward(client, data);
                break;
            case CommonConstant.BASE_GAME_EVENT_CHANGE_ROOM:
                baseEventDeal.changeRoomBase(client,data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_ROOM_CARD_GAME_LOG:
                baseEventDeal.getRoomCardGameLogList(client,data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_ROOM_CARD_GAME_LOG_DETAIL:
                baseEventDeal.getRoomCardGameLogDetail(client,data);
                break;
            case CommonConstant.BASE_GAME_EVENT_GET_CLUB_GAME_LOG:
                baseEventDeal.getClubGameLogList(client,data);
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
