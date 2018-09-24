package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DdzConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:01 2018/7/3
 * @Modified By:
 **/
@Component
public class GameTimerDdz {



    private final static Logger logger = LoggerFactory.getLogger(GameTimerDdz.class);

    public static final int TIMER_TYPE_ROB = 2;
    public static final int TIMER_TYPE_DOUBLE = 3;
    public static final int TIMER_TYPE_EVENT = 4;

    @Resource
    private Destination ddzQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private Destination matchDealQueueDestination;

    @Resource
    private RedisService redisService;

    /**
     * 准备倒计时
     * @param roomNo
     * @param outAccount
     * @param timeLeft
     */
    public void gameReadyOverTime(String roomNo, String outAccount, int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 房间非准备状态
                if (room.getGameStatus() != DdzConstant.DDZ_GAME_STATUS_READY) {
                    sendReadyTimerToAll(outAccount, 0, room);
                    break;
                }
                // 玩家不在当前房间内
                if (!room.getPlayerMap().containsKey(outAccount) || room.getPlayerMap().get(outAccount) == null) {
                    sendReadyTimerToAll(outAccount, 0, room);
                    break;
                }
                // 房间人数未满
                if (room.getPlayerMap().size() != DdzConstant.DDZ_PLAYER_NUMBER) {
                    sendReadyTimerToAll(outAccount, 0, room);
                    break;
                }
                // 该玩家已准备
                if (room.getUserPacketMap().get(outAccount).getStatus() == DdzConstant.DDZ_USER_STATUS_READY) {
                    sendReadyTimerToAll(outAccount, 0, room);
                    break;
                }
                sendReadyTimerToAll(outAccount, i, room);
                if (i == 0) {
                    JSONObject data = new JSONObject();
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,outAccount);
                    producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_EXIT_ROOM));
                }

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else {
                break;
            }
        }
    }

    private void sendReadyTimerToAll(String outAccount, int time, DdzGameRoom room) {
        JSONObject result = new JSONObject();
        result.put("index", room.getPlayerMap().get(outAccount).getMyIndex());
        result.put("time", time);
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameTimer_DDZ");
    }

    /**
     * 出牌超时
     * @param roomNo
     * @param nextAccount
     * @param timeLeft
     */
    public void gameEventOverTime(String roomNo,String nextAccount,int timeLeft){
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 非操作
                if (room.getFocusIndex()!=room.getPlayerMap().get(nextAccount).getMyIndex()) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                if (i == timeLeft && room.getSetting().containsKey("auto_last") && room.getSetting().getInt("auto_last") == CommonConstant.GLOBAL_YES) {
                    List<String> lastCard = room.getLastCard();
                    if (room.getLastCard().size() == 0 || nextAccount.equals(room.getLastOperateAccount())) {
                        lastCard.clear();
                    }
                    if (DdzCore.checkCard(lastCard, room.getUserPacketMap().get(nextAccount).getMyPai())) {
                        int cardType = DdzCore.obtainCardType(room.getUserPacketMap().get(nextAccount).getMyPai());
                        if (cardType != DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE && cardType != DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS) {
                            JSONObject data = new JSONObject();
                            data.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
                            data.put(CommonConstant.DATA_KEY_ACCOUNT, nextAccount);
                            data.put(DdzConstant.DDZ_DATA_KEY_PAI_LIST, room.getUserPacketMap().get(nextAccount).getMyPai());
                            data.put(DdzConstant.DDZ_DATA_KEY_TYPE, DdzConstant.DDZ_GAME_EVENT_TYPE_YES);
                            producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_GAME_IN));
                            break;
                        }
                    }
                }
                // 托管状态自动出牌
                if (i==timeLeft&&room.getUserPacketMap().get(nextAccount).getIsTrustee()==CommonConstant.GLOBAL_YES) {
                    JSONObject data = new JSONObject();
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,nextAccount);
                    producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_AUTO_PLAY));
                    break;
                }
                // 倒计时到了之后执行事件
                if (i==0&&room.getRoomType()!=CommonConstant.ROOM_TYPE_FK&&room.getRoomType()!=CommonConstant.ROOM_TYPE_DK) {
                    JSONObject data = new JSONObject();
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,nextAccount);
                    data.put(DdzConstant.DDZ_DATA_KEY_TYPE,CommonConstant.GLOBAL_YES);
                    producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_TRUSTEE));
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("",e);
                }
            }else {
                break;
            }
        }
    }

    /**
     * 抢地主超时
     * @param roomNo
     * @param focus
     * @param type
     * @param timeLeft
     */
    public void gameRobOverTime(String roomNo,int focus,int type,int timeLeft){
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 非操作
                if (room.getFocusIndex()!=focus) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                // 倒计时到了之后执行事件
                if (i==0) {
                    for (String account : room.getPlayerMap().keySet()) {
                        if (room.getPlayerMap().get(account).getMyIndex()==focus) {
                            JSONObject data = new JSONObject();
                            data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                            data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                            data.put(DdzConstant.DDZ_DATA_KEY_TYPE,type);
                            data.put(DdzConstant.DDZ_DATA_KEY_IS_CHOICE,CommonConstant.GLOBAL_NO);
                            producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CALL_AND_ROB));
                            break;
                        }
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("",e);
                }
            }else {
                break;
            }
        }
    }

    /**
     * 解散超时
     * @param roomNo
     * @param timeLeft
     */
    public void closeRoomOverTime(String roomNo,int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                DdzGameRoom room = (DdzGameRoom)RoomManage.gameRoomMap.get(roomNo);
                if (room.getJieSanTime()==0) {
                    break;
                }
                // 设置倒计时
                room.setJieSanTime(i);
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                            if (room.getUserPacketMap().get(account).getIsCloseRoom() == CommonConstant.CLOSE_ROOM_UNSURE) {
                                autoAccountList.add(account);
                            }
                        }
                    }
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        // 同意解散
                        data.put("type",CommonConstant.CLOSE_ROOM_AGREE);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CLOSE_ROOM));
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("",e);
                }
            }else {
                break;
            }
        }
    }

    /**
     * 加倍超时
     * @param roomNo
     * @param timeLeft
     */
    public void doubleOverTime(String roomNo, int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                if (room.getGameStatus() != DdzConstant.DDZ_GAME_STATUS_DOUBLE) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                if (i == 0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                            if (room.getUserPacketMap().get(account).getDoubleTime() < 1) {
                                autoAccountList.add(account);
                            }
                        }
                    }
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO, room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT, account);
                        // 不加倍
                        data.put(DdzConstant.DDZ_DATA_KEY_TYPE, DdzConstant.DDZ_DOUBLE_TYPE_NO);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_GAME_DOUBLE));
                    }
                    break;
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else {
                break;
            }
        }
    }

    @Scheduled(cron = "0/1 * * * * ?")
    public void checkTimer() {
        String key = "room_map";
        Map<Object, Object> roomMap = redisService.hmget(key);
        if (roomMap != null && roomMap.size() > 0) {
            for (Object roomNo : roomMap.keySet()) {
                Object roomObj = redisService.hget("room_map", String.valueOf(roomNo));
                if (roomObj != null) {
                    JSONObject roomInfo = JSONObject.fromObject(roomObj);
                    int timeLeft = roomInfo.getInt("timeLeft") - 1;
                    if (timeLeft <= 8) {
//                        JSONObject obj = new JSONObject();
//                        obj.put("deal_type", MatchDealConstant.MATCH_DEAL_TYPE_TIME);
//                        obj.put("roomNo", String.valueOf(roomNo));
//                        obj.put("roomInfo", roomInfo);
//                        producerService.sendMessage(matchDealQueueDestination, obj);
                        doOverTimeDeal(roomNo, roomInfo);
                    } else {
                        if (RoomManage.gameRoomMap.containsKey(String.valueOf(roomNo)) && RoomManage.gameRoomMap.get(String.valueOf(roomNo)) != null) {
                            RoomManage.gameRoomMap.get(String.valueOf(roomNo)).setTimeLeft(timeLeft);
                        }
                        redisService.hset(key, String.valueOf(roomNo), String.valueOf(roomInfo.element("timeLeft", timeLeft)));
                    }
                }
            }
        }
    }

    public void doOverTimeDeal(Object roomNo, JSONObject roomInfo) {
        switch (roomInfo.getInt("timerType")) {
            case TIMER_TYPE_ROB:
                gameRobOverTime(String.valueOf(roomNo), roomInfo.getInt("focus"), roomInfo.getInt("type"), 0);
                break;
            case TIMER_TYPE_DOUBLE:
                doubleOverTime(String.valueOf(roomNo), 0);
                break;
            case TIMER_TYPE_EVENT:
                gameEventOverTime(String.valueOf(roomNo), roomInfo.getString("nextPlayerAccount"), 0);
                break;
            default:
                break;
        }
    }
}
