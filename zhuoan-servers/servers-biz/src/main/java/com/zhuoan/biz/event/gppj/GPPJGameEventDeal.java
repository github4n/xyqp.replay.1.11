package com.zhuoan.biz.event.gppj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.gppj.GPPJCore;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gppj.UserPacketGPPJ;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.GPPJConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:36 2018/6/9
 * @Modified By:
 **/
@Component
public class GPPJGameEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(GPPJGameEventDeal.class);

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private UserBiz userBiz;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RedisService redisService;

    @Resource
    private GameTimerGPPJ gameTimerGPPJ;

    /**
     * 创建房间通知自己
     * @param client
     */
    public void createRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject roomData = obtainRoomData(roomNo, account);
        // 数据不为空
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("data", roomData);
            // 通知自己
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "enterRoomPush_GPPJ");
        }
    }

    /**
     * 加入房间通知
     * @param client
     * @param data
     */
    public void joinRoom(SocketIOClient client, Object data) {
        // 进入房间通知自己
        createRoom(client, data);
        JSONObject joinData = JSONObject.fromObject(data);
        // 非重连通知其他玩家
        if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
            String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
            GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
            Playerinfo player = room.getPlayerMap().get(account);
            JSONObject obj = new JSONObject();
            obj.put("account", player.getAccount());
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", player.getScore());
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            obj.put("userStatus", room.getUserPacketMap().get(account).getStatus());
            // 通知玩家
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(obj), "playerEnterPush_GPPJ");
        }
    }

    /**
     * 准备
     * @param client
     * @param data
     */
    public void gameReady(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_INIT, client) &&
            !CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_READY, client) &&
            !CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_READY) {
            return;
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != GPPJConstant.GP_PJ_GAME_STATUS_READY) {
            room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_READY);
        }
        // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
        if (isAllReady(roomNo) && room.getPlayerMap().size() >= GPPJConstant.GP_PJ_MIN_START_COUNT) {
            // 初始化房间信息
            initRoom(roomNo);
            // 设置房间状态
            room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_CUT);
            // 开启切牌定时器
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_CUT,GPPJConstant.GP_PJ_USER_STATUS_CUT,
                        GPPJConstant.GP_PJ_TIME_CUT,GPPJConstant.SLEEP_TYPE_NONE);
                }
            });
            // 通知玩家
            changeGameStatus(roomNo);
        } else {
            JSONObject result = new JSONObject();
            result.put("index", room.getPlayerMap().get(account).getMyIndex());
            result.put("startBtnIndex", obtainOwnerIndex(roomNo));
            result.put("showStartBtn", obtainStartBtnStatus(roomNo));
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "playerReadyPush_GPPJ");
        }
    }

    /**
     * 开始游戏
     * @param client
     * @param data
     */
    public void gameStart(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_READY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_READY) {
            return;
        }
        if (!account.equals(room.getOwner())) {
            return;
        }
        if (!postData.containsKey(GPPJConstant.DATA_KEY_TYPE)) {
            return;
        }
        int type = postData.getInt(GPPJConstant.DATA_KEY_TYPE);
        // 第一次点开始游戏人数未满(需要扣除房主本人)
        if (type==GPPJConstant.START_GAME_TYPE_UNSURE&&obtainNowReadyCount(roomNo)<room.getPlayerCount()-1) {
            JSONObject result = new JSONObject();
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameStartPush_GPPJ");
            return;
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_READY);
        // 初始化房间信息
        initRoom(roomNo);
        // 设置房间状态
        room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_CUT);
        // 开启切牌定时器
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_CUT,GPPJConstant.GP_PJ_USER_STATUS_CUT,
                    GPPJConstant.GP_PJ_TIME_CUT,GPPJConstant.SLEEP_TYPE_NONE);
            }
        });
        // 通知玩家
        changeGameStatus(roomNo);
    }

    /**
     * 切牌
     * @param client
     * @param data
     */
    public void gameCut(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 非抢庄阶段收到抢庄消息不作处理
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_CUT, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 非准备状态无法切牌
        if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY) {
            return;
        }
        if (postData.containsKey(GPPJConstant.DATA_KEY_CUT_PLACE)) {
            // 设置切牌位置
            room.setCutPlace(postData.getInt(GPPJConstant.DATA_KEY_CUT_PLACE));
            // 设置切牌玩家
            room.setCutIndex(room.getPlayerMap().get(account).getMyIndex());
            // 开始游戏发牌
            startGame(roomNo);
            // 通知玩家切牌完场
            for (String uuid : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                    JSONObject result = new JSONObject();
                    result.put("dice", room.getDice());
                    result.put("paiIndex", obtainPaiIndex(roomNo));
                    result.put("paiArray", obtainPlayerIndex(roomNo));
                    int[] myPai = new int[]{0,0};
                    // 看牌抢庄模式有参与玩家传第一张牌
                    if (room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK&&room.getUserPacketMap().get(uuid).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                        myPai[0] = GPPJCore.getPaiValue(room.getUserPacketMap().get(uuid).getPai()[1]);
                    }
                    result.put("myPai", myPai);
                    CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(result),"gameCutPush_GPPJ");
                }
            }
            // 设置房间状态
            if (room.getBankerType()==GPPJConstant.BANKER_TYPE_OWNER) {
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_XZ);
                // 开启下注定时器
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_XZ,GPPJConstant.GP_PJ_USER_STATUS_XZ,
                            GPPJConstant.GP_PJ_TIME_XZ,GPPJConstant.SLEEP_TYPE_START_GAME);
                    }
                });
            }
            if (room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK) {
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_QZ);
                // 开启抢庄定时器
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_QZ,GPPJConstant.GP_PJ_USER_STATUS_QZ,
                            GPPJConstant.GP_PJ_TIME_QZ,GPPJConstant.SLEEP_TYPE_START_GAME);
                    }
                });
            }
            if (room.getBankerType()==GPPJConstant.BANKER_TYPE_COMPARE) {
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_SHOW);
                // 开启咪牌定时器
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_SHOW,GPPJConstant.GP_PJ_USER_STATUS_SHOW,
                            GPPJConstant.GP_PJ_TIME_SHOW,GPPJConstant.SLEEP_TYPE_START_GAME);
                    }
                });
            }
        }
    }

    /**
     * 抢庄
     * @param client
     * @param data
     */
    public void gameQz(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 非抢庄阶段收到抢庄消息不作处理
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_QZ, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey(GPPJConstant.DATA_KEY_QZ_TIMES)) {
            // 非看牌抢庄抢庄消息不做处理
            if (room.getBankerType() != GPPJConstant.BANKER_TYPE_LOOK) {
                return;
            }
            // 不是准备、切牌状态的玩家抢庄消息不作处理(包括中途加入和已经抢过庄的)
            if (room.getUserPacketMap().get(account).getStatus() != GPPJConstant.GP_PJ_USER_STATUS_READY&&
                room.getUserPacketMap().get(account).getStatus() != GPPJConstant.GP_PJ_USER_STATUS_CUT) {
                return;
            }
            int maxTimes = obtainMaxTimes(obtainQzBtn(roomNo,account));
            if (postData.getInt(GPPJConstant.DATA_KEY_QZ_TIMES)<0||postData.getInt(GPPJConstant.DATA_KEY_QZ_TIMES)>maxTimes) {
                return;
            }
            // 设置为玩家抢庄状态，抢庄倍数
            room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_QZ);
            room.getUserPacketMap().get(account).setBankerTimes(postData.getInt(GPPJConstant.DATA_KEY_QZ_TIMES));
            // 所有人都完成抢庄
            if (isAllQz(roomNo)) {
                // 确定庄家
                chooseBanker(roomNo);
                // 设置房间状态
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_XZ);
                // 开启下注定时器
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_XZ,GPPJConstant.GP_PJ_USER_STATUS_XZ,
                            GPPJConstant.GP_PJ_TIME_XZ,GPPJConstant.SLEEP_TYPE_NONE);
                    }
                });
                // 改变状态通知玩家
                changeGameStatus(roomNo);
            } else {
                JSONObject result = new JSONObject();
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put("qzTimes", room.getUserPacketMap().get(account).getBankerTimes());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameQzPush_GPPJ");
            }
        }
    }

    /**
     * 下注
     * @param client
     * @param data
     */
    public void gameXz(SocketIOClient client, Object data) {
        // 非下注阶段收到下注消息不作处理
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_XZ, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (postData.containsKey(GPPJConstant.DATA_KEY_XZ_TIMES)) {
            // 庄家
            if (account.equals(room.getBanker())) {
                return;
            }
            // 房主坐庄模式
            if (room.getBankerType()==GPPJConstant.BANKER_TYPE_OWNER) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY&&
                    room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_CUT) {
                    return;
                }
            }
            // 看牌抢庄模式
            if (room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_QZ) {
                    return;
                }
            }
            // 互比模式
            if (room.getBankerType() == GPPJConstant.BANKER_TYPE_COMPARE) {
                return;
            }
            // 最大下注倍数
            int maxTimes = obtainMaxTimes(obtainXzBtn(roomNo,account));
            if (postData.getInt(GPPJConstant.DATA_KEY_XZ_TIMES)<=0||postData.getInt(GPPJConstant.DATA_KEY_XZ_TIMES)>maxTimes) {
                return;
            }
            room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_XZ);
            room.getUserPacketMap().get(account).setXzTimes(postData.getInt(GPPJConstant.DATA_KEY_XZ_TIMES));
            // 通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("index", room.getPlayerMap().get(account).getMyIndex());
            result.put("xzTimes", room.getUserPacketMap().get(account).getXzTimes());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXzPush_GPPJ");
            // 所有人都完成下注
            if (isAllXz(roomNo)) {
                // 设置游戏状态
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_SHOW);
                // 房主坐庄模式或看牌抢庄模式所有人都下注完将庄家设为已下注状态
                if (room.getBankerType()==GPPJConstant.BANKER_TYPE_OWNER||room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK) {
                    if (!Dto.stringIsNULL(room.getBanker())&&room.getUserPacketMap().get(room.getBanker())!=null) {
                        room.getUserPacketMap().get(room.getBanker()).setStatus(GPPJConstant.GP_PJ_USER_STATUS_XZ);
                    }
                }
                // 开启咪牌定时器
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.gameOverTime(roomNo,GPPJConstant.GP_PJ_GAME_STATUS_SHOW,GPPJConstant.GP_PJ_USER_STATUS_SHOW,
                            GPPJConstant.GP_PJ_TIME_SHOW,GPPJConstant.SLEEP_TYPE_NONE);
                    }
                });
                // 通知玩家
                changeGameStatus(roomNo);
            }
        }
    }

    /**
     * 咪牌
     * @param client
     * @param data
     */
    public void gameShow(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 非亮牌阶段收到亮牌消息不作处理
        if (!CommonConstant.checkEvent(postData, GPPJConstant.GP_PJ_GAME_STATUS_SHOW, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 设置玩家亮牌状态
        if (room.getBankerType()==GPPJConstant.BANKER_TYPE_COMPARE) {
            if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY&&
                room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_CUT) {
                return;
            }
        }
        if (room.getBankerType()==GPPJConstant.BANKER_TYPE_OWNER||room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK) {
            if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_XZ) {
                return;
            }
        }
        // 设置玩家状态
        room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_SHOW);
        // 所有人都完成亮牌
        if (isAllShow(roomNo)) {
            showFinish(roomNo);
        } else {
            // 通知玩家
            JSONObject result = new JSONObject();
            result.put("index", room.getPlayerMap().get(account).getMyIndex());
            result.put("pai", GPPJCore.getPaiValue(room.getUserPacketMap().get(account).getPai()));
            result.put("paiType", room.getUserPacketMap().get(account).getPaiType());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameShowPush_GPPJ");
        }
    }

    /**
     * 重连
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)||
            !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)||
            !postData.containsKey("uuid")) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        // uuid不匹配
        if (!userInfo.containsKey("uuid")||Dto.stringIsNULL(userInfo.getString("uuid"))||
            !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            return;
        }
        JSONObject result = new JSONObject();
        if (client == null) {
            return;
        }
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_GPPJ");
            return;
        }
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_GPPJ");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_GPPJ");
    }

    /**
     * 退出
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        boolean canExit = false;
        // 金币场、元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            // 未参与游戏可以自由退出
            if (room.getUserPacketMap().get(account).getStatus() == GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                canExit = true;
            } else if (room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_INIT ||
                room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_READY ||
                room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
                canExit = true;
            }
        }else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
            // 总结算之后可以退出房间
            if (room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
                canExit = true;
            }
            if (!room.getOwner().equals(account)) {
                if (room.getUserPacketMap().get(account).getPlayTimes()==0) {
                    canExit = true;
                }
            }
        }
        Playerinfo player = room.getPlayerMap().get(account);
        if (canExit) {
            List<UUID> allUUIDList = room.getAllUUIDList();
            // 更新数据库
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", room.getRoomNo());
            if (room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                roomInfo.put("user_id" + room.getPlayerMap().get(account).getMyIndex(), 0);
            }
            // 移除数据
            for (int i = 0; i < room.getUserIdList().size(); i++) {
                if (room.getUserIdList().get(i) == room.getPlayerMap().get(account).getId()) {
                    room.getUserIdList().set(i, 0L);
                    break;
                }
            }
            room.getPlayerMap().remove(account);
            room.getUserPacketMap().remove(account);
            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("type", 1);
            result.put("index", player.getMyIndex());
            result.put("startBtnIndex", obtainOwnerIndex(roomNo));
            result.put("showStartBtn", obtainStartBtnStatus(roomNo));
            if (!postData.containsKey("notSend")) {
                CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_GPPJ");
            }
            if (postData.containsKey("notSendToMe")) {
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_GPPJ");
            }
            // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
            if (isAllReady(roomNo) && room.getPlayerMap().size() >= GPPJConstant.GP_PJ_MIN_START_COUNT) {
                startGame(roomNo);
            }
            // 所有人都退出清除房间数据
            if (room.getPlayerMap().size() == 0) {
                redisService.deleteByKey("summaryTimes_gp_pj"+room.getRoomNo());
                roomInfo.put("status",room.getIsClose());
                roomInfo.put("game_index",room.getGameIndex());
                RoomManage.gameRoomMap.remove(room.getRoomNo());
            }
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
        } else {
            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "当前无法退出,请发起解散");
            result.put("type", 1);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush_GPPJ");
        }
    }

    /**
     * 解散房间
     * @param client
     * @param data
     */
    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey("type")) {
            JSONObject result = new JSONObject();
            int type = postData.getInt("type");
            // 有人发起解散设置解散时间
            if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
                // 有人发起解散设置解散时间
                final int closeTime = 60;
                room.setJieSanTime(closeTime);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerGPPJ.closeRoomOverTime(roomNo,closeTime);
                    }
                });
            }
            // 设置解散状态
            room.getUserPacketMap().get(account).setIsCloseRoom(type);
            // 有人拒绝解散
            if (type == CommonConstant.CLOSE_ROOM_DISAGREE) {
                // 重置解散
                room.setJieSanTime(0);
                // 设置玩家为未确认状态
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        room.getUserPacketMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
                    }
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_GPPJ");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (isAllAgreeClose(roomNo)) {
                    // 未玩完一局不需要强制结算
                    if (!room.isNeedFinalSummary()) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 更新数据库
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("room_no",room.getRoomNo());
                        roomInfo.put("status",room.getIsClose());
                        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"解散成功");
                        CommonConstant.sendMsgEventToAll(uuidList,result.toString(),"tipMsgPush");
                        return;
                    }
                    room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY);
                    changeGameStatus(roomNo);
                } else {// 刷新数据
                    room.getUserPacketMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", obtainCloseData(roomNo));
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_GPPJ");
                }
            }
        }
    }

    /**
     * 所有人亮牌完成
     * @param roomNo
     */
    public void showFinish(String roomNo) {
        String summaryTimesKey = "summaryTimes_gp_pj"+roomNo;
        long summaryTimes = redisService.incr(summaryTimesKey,1);
        if (summaryTimes>1) {
            return;
        }
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 设置房间状态
        room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY);
        // 结算
        switch (room.getBankerType()) {
            case GPPJConstant.BANKER_TYPE_OWNER:
                summaryBanker(roomNo);
                break;
            case GPPJConstant.BANKER_TYPE_LOOK:
                summaryBanker(roomNo);
                break;
            case GPPJConstant.BANKER_TYPE_COMPARE:
                summaryCompare(roomNo);
                break;
            default:
                break;
        }
        // 通知玩家
        changeGameStatus(roomNo);
        // 房卡场触发总结算
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
            // 玩完一局之后解散需要触发总结算
            room.setNeedFinalSummary(true);
            // 局数到了之后触发总结算
            if (room.getGameStatus()== GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY&&room.getGameIndex()==room.getGameCount()) {
                room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);
                room.setGameStatus(GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY);
            }
            // 更新房卡数
            updateRoomCard(roomNo);
            // 保存战绩
            saveGameLog(roomNo);
        }
    }

    /**
     * 更新房卡数
     * @param roomNo
     */
    public void updateRoomCard(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    // 房主支付
                    if (room.getPayType()==CommonConstant.PAY_TYPE_OWNER) {
                        if (account.equals(room.getOwner())) {
                            // 参与第一局需要扣房卡
                            if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                                roomCardCount = room.getPlayerCount()*room.getSinglePayNum();
                                array.add(room.getPlayerMap().get(room.getOwner()).getId());
                            }
                        }
                    }
                    // 房费AA
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA) {
                        // 参与第一局需要扣房卡
                        if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                            array.add(room.getPlayerMap().get(account).getId());
                            roomCardCount = room.getSinglePayNum();
                        }
                    }
                }
            }
        }
        if (array.size()>0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array,roomCardCount)));
        }
    }

    /**
     * 保存战绩
     * @param roomNo
     */
    public void saveGameLog(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        JSONArray array = new JSONArray();
        // 存放游戏记录
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 有参与的玩家
                if (room.getUserPacketMap().get(account).getStatus() == GPPJConstant.GP_PJ_USER_STATUS_SHOW) {
                    JSONObject obj = new JSONObject();
                    obj.put("id", room.getPlayerMap().get(account).getId());
                    obj.put("total", room.getPlayerMap().get(account).getScore());
                    obj.put("fen", room.getUserPacketMap().get(account).getScore());
                    array.add(obj);
                    // 战绩记录
                    JSONObject gameLogResult = new JSONObject();
                    gameLogResult.put("account", account);
                    gameLogResult.put("name", room.getPlayerMap().get(account).getName());
                    gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
                    if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().containsKey(room.getBanker())&&room.getPlayerMap().get(room.getBanker())!=null) {
                        gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                    }else {
                        gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                    }
                    gameLogResult.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
                    gameLogResult.put("myPai", room.getUserPacketMap().get(account).getPai());
                    gameLogResult.put("score", room.getUserPacketMap().get(account).getScore());
                    gameLogResult.put("totalScore", room.getPlayerMap().get(account).getScore());
                    gameLogResult.put("win", CommonConstant.GLOBAL_YES);
                    if (room.getUserPacketMap().get(account).getScore() < 0) {
                        gameLogResult.put("win", CommonConstant.GLOBAL_NO);
                    }
                    gameLogResults.add(gameLogResult);
                    // 用户战绩
                    JSONObject userResult = new JSONObject();
                    userResult.put("zhuang", room.getBanker());
                    userResult.put("isWinner", CommonConstant.GLOBAL_NO);
                    if (room.getUserPacketMap().get(account).getScore() > 0) {
                        userResult.put("isWinner", CommonConstant.GLOBAL_YES);
                    }
                    userResult.put("score", room.getUserPacketMap().get(account).getScore());
                    userResult.put("totalScore", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                    userResult.put("player", room.getPlayerMap().get(account).getName());
                    gameResult.add(userResult);
                }
            }
        }
        if (room.getId()==0) {
            JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
            if (!Dto.isObjNull(roomInfo)) {
                room.setId(roomInfo.getLong("id"));
            }
        }
        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), String.valueOf(room.getGameProcess()));
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, String.valueOf(gameResult));
        for (int i = 0; i < userGameLogs.size(); i++) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
        }
    }

    /**
     * 改变状态通知玩家
     * @param roomNo
     */
    public void changeGameStatus(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                JSONObject obj = new JSONObject();
                obj.put("gameStatus", room.getGameStatus());
                obj.put("banker",obtainBankerIndex(roomNo));
                obj.put("game_index", room.getGameIndex());
                if (room.getGameIndex() == 0) {
                    obj.put("game_index", 1);
                }
                obj.put("dice", room.getDice());
                obj.put("cutIndex", room.getCutIndex());
                obj.put("cutPlace", room.getCutPlace());
                obj.put("leftPai", obtainLeftPai(roomNo));
                obj.put("paiIndex", obtainPaiIndex(roomNo));
                obj.put("paiArray", obtainPlayerIndex(roomNo));
                obj.put("bankerTimes", obtainBankerTimes(roomNo));
                obj.put("readyBtnType", obtainBtnType(roomNo,account));
                obj.put("qzTimes", obtainQzBtn(roomNo,account));
                obj.put("xzTimes", obtainXzBtn(roomNo,account));
                obj.put("users", obtainAllPlayer(roomNo));
                obj.put("gameData", obtainGameData(roomNo, account));
                obj.put("showStartBtn", obtainStartBtnStatus(roomNo));
                // 总结算
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                    if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
                        obj.put("summaryData", obtainFinalSummaryData(roomNo));
                    }
                    if (room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY&&room.getGameIndex()==room.getGameCount()) {
                        obj.put("summaryData", obtainFinalSummaryData(roomNo));
                    }
                }
                UUID uuid = room.getPlayerMap().get(account).getUuid();
                // 通知玩家
                if (uuid != null) {
                    CommonConstant.sendMsgEventToSingle(uuid, String.valueOf(obj), "changeGameStatusPush_GPPJ");
                }
            }
        }
    }

    /**
     * 初始化房间
     * @param roomNo
     */
    public void initRoom(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 游戏局数+1
        room.setGameIndex(room.getGameIndex()+1);
        double minScore = 0;
        String minAccount = "";
        // 获取上局输家
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 非准备状态的玩家设为未参与
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY) {
                    room.getUserPacketMap().get(account).setStatus(GPPJConstant.GP_PJ_USER_STATUS_INIT);
                }else if (room.getUserPacketMap().get(account).getScore()<minScore) {
                    minScore = room.getUserPacketMap().get(account).getScore();
                    minAccount = account;
                }
            }
        }
        // 房主在房间内取房主，否则随机取
        if (Dto.stringIsNULL(minAccount)&&minScore==0) {
            if (!Dto.stringIsNULL(room.getOwner())&&room.getPlayerMap().containsKey(room.getOwner())&&
                room.getPlayerMap().get(room.getOwner())!=null) {
                minAccount = room.getOwner();
            }else {
                for (String account : room.getPlayerMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                        minAccount = account;
                        break;
                    }
                }
            }
        }
        // 设置切牌玩家下标
        room.setCutIndex(room.getPlayerMap().get(minAccount).getMyIndex());
        // 初始化玩家牌局信息
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                room.getUserPacketMap().get(account).setIsCloseRoom(0);
                room.getUserPacketMap().get(account).setScore(0);
                room.getUserPacketMap().get(account).setXzTimes(0);
                room.getUserPacketMap().get(account).setBankerTimes(0);
                // 没参与的玩家不增加游戏局数
                if (room.getUserPacketMap().get(account).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_READY) {
                    room.getUserPacketMap().get(account).setPlayTimes(room.getUserPacketMap().get(account).getPlayTimes()+1);
                }
            }
        }

    }

    /**
     * 开始游戏
     * @param roomNo
     */
    public void startGame(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 洗牌
        String[] pai = GPPJCore.ShufflePai();
        // 获取筛子
        JSONArray dice = dice();
        room.setDice(dice);
        // 发牌
        faPai(roomNo,pai);
        // 重置结算次数
        redisService.insertKey("summaryTimes_gp_pj"+room.getRoomNo(),"0",null);
    }

    /**
     * 确定庄家
     * @param roomNo
     */
    public void chooseBanker(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 抢庄玩家
        List<String> qzList = new ArrayList<String>();
        // 所有玩家
        List<String> allList = new ArrayList<String>();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    allList.add(account);
                    if (room.getUserPacketMap().get(account).getBankerTimes()>0) {
                        qzList.add(account);
                    }
                }
            }
        }
        // 无人抢庄随机庄家
        if (qzList.size()==0) {
            int bankerIndex = RandomUtils.nextInt(allList.size());
            room.setBanker(allList.get(bankerIndex));
        }else {
            int bankerIndex = RandomUtils.nextInt(qzList.size());
            room.setBanker(qzList.get(bankerIndex));
        }
    }

    /**
     * 摇骰子
     * @return
     */
    public JSONArray dice() {
        JSONArray dice = new JSONArray();
        for (int i = 0; i < 3; i++) {
            dice.add(RandomUtils.nextInt(6)+1);
        }
        return dice;
    }

    /**
     * 发牌
     * @param roomNo
     * @param pai
     */
    public void faPai(String roomNo,String[] pai) {
        // 牌下标
        int paiIndex = 0;
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 设置玩家手牌
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    String[] userPai = new String[2];
                    for (int i = 0; i < userPai.length; i++) {
                        userPai[i] = pai[paiIndex];
                        paiIndex++;
                    }
                    // 设置玩家手牌
                    room.getUserPacketMap().get(account).setPai(userPai);
                    // 设置玩家牌型
                    room.getUserPacketMap().get(account).setPaiType(GPPJCore.getPaiType(userPai));
                }
            }
        }
        // 设置剩余牌
        int[] left = new int[pai.length-paiIndex];
        for (int i = 0; i < left.length; i++) {
            left[i] = GPPJCore.getPaiValue(pai[i+paiIndex]);
        }
        // 设置剩余牌
        room.setLeftPai(left);
    }

    /**
     * 庄闲模式(房主坐庄，看牌抢庄)结算
     * @param roomNo
     */
    public void summaryBanker(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (Dto.stringIsNULL(room.getBanker())||!room.getUserPacketMap().containsKey(room.getBanker())||
            room.getUserPacketMap().get(room.getBanker())==null) {
            return;
        }
        UserPacketGPPJ banker = room.getUserPacketMap().get(room.getBanker());
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 所有参与的非庄家玩家
                if (room.getUserPacketMap().get(account).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT&&!account.equals(room.getBanker())) {
                    UserPacketGPPJ other = room.getUserPacketMap().get(account);
                    double sum;
                    // 牌型相同庄家赢
                    if (GPPJCore.comparePai(other.getPai(),banker.getPai())==GPPJConstant.COMPARE_RESULT_WIN) {
                        sum = room.getScore() * room.getUserPacketMap().get(account).getXzTimes();
                    }else {
                        sum = -room.getScore() * room.getUserPacketMap().get(account).getXzTimes();
                    }
                    if (room.getMultiple()==GPPJConstant.MULTIPLE_TYPE_ZZ_DOUBLE) {
                        if (banker.getPaiType()==38||other.getPaiType()==38) {
                            // 至尊翻4倍
                            sum *= 4;
                        } else if (banker.getPaiType()>=27||other.getPaiType()>=27) {
                            // 对子翻2倍
                            sum *= 2;
                        }
                    }
                    // 设置玩家当局输赢分数
                    banker.setScore(Dto.sub(banker.getScore(),sum));
                    other.setScore(Dto.add(other.getScore(),sum));
                    // 设置玩家总计输赢分数
                    double bankerScoreOld = room.getPlayerMap().get(room.getBanker()).getScore();
                    room.getPlayerMap().get(room.getBanker()).setScore(Dto.sub(bankerScoreOld,sum));
                    double otherScoreOld = room.getPlayerMap().get(account).getScore();
                    room.getPlayerMap().get(account).setScore(Dto.add(otherScoreOld,sum));
                }
            }
        }
    }

    /**
     * 互比结算
     * @param roomNo
     */
    public void summaryCompare(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            room.setScore(10);
        }
        List<String> gameList = new ArrayList<String>();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 所有参与的玩家
                if (room.getUserPacketMap().get(account).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    gameList.add(account);
                }
            }
        }
        for (String account : gameList) {
            UserPacketGPPJ me = room.getUserPacketMap().get(account);
            // 当局总计输赢
            double sum = 0;
            for (String uuid : gameList) {
                UserPacketGPPJ other = room.getUserPacketMap().get(uuid);
                if (GPPJCore.comparePai(me.getPai(),other.getPai())==GPPJConstant.COMPARE_RESULT_WIN) {
                    sum += room.getScore();
                }else if (GPPJCore.comparePai(me.getPai(),other.getPai())==GPPJConstant.COMPARE_RESULT_LOSE) {
                    sum -= room.getScore();
                }
            }
            // 设置玩家当局输赢分数
            me.setScore(Dto.add(me.getScore(),sum));
            // 设置玩家总计输赢分数
            double otherScoreOld = room.getPlayerMap().get(account).getScore();
            room.getPlayerMap().get(account).setScore(Dto.add(otherScoreOld,sum));
        }
    }

    /**
     * 获取实时准备人数
     * @param roomNo
     * @return
     */
    public int obtainNowReadyCount(String roomNo) {
        int readyCount = 0;
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_READY) {
                    readyCount ++;
                }
            }
        }
        return readyCount;
    }

    /**
     * 是否全部准备
     * @param roomNo
     * @return
     */
    public boolean isAllReady(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 是否全部准备
     * @param roomNo
     * @return
     */
    public boolean isAllQz(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_INIT&&
                    room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_QZ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 是否全部准备
     * @param roomNo
     * @return
     */
    public boolean isAllXz(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (!account.equals(room.getBanker())) {
                    if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_INIT&&
                        room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_XZ) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 是否全部准备
     * @param roomNo
     * @return
     */
    public boolean isAllShow(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_INIT&&
                    room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_SHOW) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 是否全部同意解散
     * @param roomNo
     * @return
     */
    public boolean isAllAgreeClose(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()){
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getIsCloseRoom()!= CommonConstant.CLOSE_ROOM_AGREE) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 获取可选最大倍数
     * @param array
     * @return
     */
    public int obtainMaxTimes(JSONArray array) {
        int maxTimes = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject baseNum = array.getJSONObject(i);
            if (baseNum.getInt("isUse")==CommonConstant.GLOBAL_YES&&baseNum.getInt("val")>maxTimes) {
                maxTimes = baseNum.getInt("val");
            }
        }
        return  maxTimes;
    }


    /**
     * 获取实时房间数据
     * @param account
     * @param roomNo
     * @return
     */
    public JSONObject obtainRoomData(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("gameStatus", room.getGameStatus());
        obj.put("room_no", room.getRoomNo());
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("player_count", room.getPlayerCount());
        obj.put("di", room.getScore());
        // 房卡场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append(room.getWfType());
            roomInfo.append("   ");
            roomInfo.append(room.getPlayerCount());
            roomInfo.append("人");
            obj.put("roomInfo",String.valueOf(roomInfo));
        }
        obj.put("banker",obtainBankerIndex(roomNo));
        obj.put("ownerIndex",obtainOwnerIndex(roomNo));
        obj.put("game_index", room.getGameIndex());
        if (room.getGameIndex() == 0) {
            obj.put("game_index", 1);
        }
        obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        obj.put("dice", room.getDice());
        obj.put("cutIndex", room.getCutIndex());
        obj.put("cutPlace", room.getCutPlace());
        obj.put("paiIndex", obtainPaiIndex(roomNo));
        obj.put("paiArray", obtainPlayerIndex(roomNo));
        obj.put("leftPai", obtainLeftPai(roomNo));
        obj.put("bankerTimes", obtainBankerTimes(roomNo));
        obj.put("readyBtnType", obtainBtnType(roomNo,account));
        obj.put("qzTimes", obtainQzBtn(roomNo,account));
        obj.put("xzTimes", obtainXzBtn(roomNo,account));
        obj.put("users", obtainAllPlayer(roomNo));
        obj.put("qzJoin", obtainQzResult(roomNo));
        obj.put("xzJoin", obtainXzResult(roomNo));
        obj.put("gameData", obtainGameData(roomNo, account));
        obj.put("showStartBtn", obtainStartBtnStatus(roomNo));
        obj.put("isClose", CommonConstant.GLOBAL_NO);
        if (room.getJieSanTime()>0&&room.getGameStatus()!=GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
            obj.put("isClose", CommonConstant.GLOBAL_YES);
            obj.put("closeData", obtainCloseData(roomNo));

        }
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            if (room.getGameStatus() == GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
                obj.put("summaryData", obtainFinalSummaryData(roomNo));
            }
        }
        int[] myPai = new int[]{0,0};
        // 看牌抢庄模式有参与玩家传第一张牌
        if (room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK&&room.getUserPacketMap().get(account).getPai()!=null) {
            myPai[0] = GPPJCore.getPaiValue(room.getUserPacketMap().get(account).getPai()[1]);
        }
        obj.put("myPai", myPai);
        return obj;
    }

    /**
     * 获取解散数据
     * @param roomNo
     * @return
     */
    public JSONArray obtainCloseData(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray closeData = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                JSONObject obj = new JSONObject();
                obj.put("index",room.getPlayerMap().get(account).getMyIndex());
                obj.put("name",room.getPlayerMap().get(account).getName());
                obj.put("jiesanTimer",room.getJieSanTime());
                obj.put("result",room.getUserPacketMap().get(account).getIsCloseRoom());
                closeData.add(obj);
            }
        }
        return closeData;
    }

    /**
     * 获取游戏数据
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainGameData(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject gameData = new JSONObject();
        for (String uuid : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                // 所有有参与的玩家
                if (room.getUserPacketMap().get(uuid).getStatus()>GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    JSONObject obj = new JSONObject();
                    // 座位号
                    obj.put("index",room.getPlayerMap().get(uuid).getMyIndex());
                    // 抢庄、下注阶段
                    if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_QZ||room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_XZ) {
                        // 看牌抢庄展示自己的一张手牌，其余模式两张暗牌
                        if (room.getBankerType()==GPPJConstant.BANKER_TYPE_LOOK&&uuid.equals(account)) {
                            String[] myPai = room.getUserPacketMap().get(uuid).getPai();
                            if (myPai!=null&&myPai.length==2) {
                                obj.put("pai",new int[]{GPPJCore.getPaiValue(myPai[1]),0});
                            }else {
                                obj.put("pai",new int[]{});
                            }
                        }else {
                            obj.put("pai",new int[]{0,0});
                        }
                    }
                    // 亮牌阶段
                    if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SHOW) {
                        // 玩家自己或已亮牌展示牌型和牌
                        if (uuid.equals(account)||room.getUserPacketMap().get(uuid).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_SHOW) {
                            obj.put("pai",GPPJCore.getPaiValue(room.getUserPacketMap().get(uuid).getPai()));
                            obj.put("paiType",room.getUserPacketMap().get(uuid).getPaiType());
                        }else {
                            obj.put("pai",new int[]{0,0});
                        }
                    }
                    // 结算阶段
                    if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY) {
                        obj.put("pai",GPPJCore.getPaiValue(room.getUserPacketMap().get(uuid).getPai()));
                        obj.put("paiType",room.getUserPacketMap().get(uuid).getPaiType());
                        obj.put("sum",room.getUserPacketMap().get(uuid).getScore());
                        obj.put("scoreLeft",room.getPlayerMap().get(uuid).getScore());
                    }
                    gameData.put(room.getPlayerMap().get(uuid).getMyIndex(),obj);
                }
            }
        }

        return gameData;
    }

    /**
     * 获取抢庄结果
     * @param roomNo
     * @return
     */
    public JSONArray obtainQzResult(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray qzResult = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                UserPacketGPPJ up = room.getUserPacketMap().get(account);
                if (up.getStatus()==GPPJConstant.GP_PJ_USER_STATUS_QZ) {
                    JSONObject obj = new JSONObject();
                    obj.put("index", room.getPlayerMap().get(account).getMyIndex());
                    obj.put("qzTimes", up.getBankerTimes());
                    qzResult.add(obj);
                }
            }
        }
        return qzResult;
    }

    /**
     * 获取下注结果
     * @param roomNo
     * @return
     */
    public JSONArray obtainXzResult(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray xzResult = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                UserPacketGPPJ up = room.getUserPacketMap().get(account);
                JSONObject obj = new JSONObject();
                obj.put("index", room.getPlayerMap().get(account).getMyIndex());
                obj.put("xzTimes", up.getXzTimes());
                xzResult.add(obj);
            }
        }
        return xzResult;
    }

    /**
     * 获取当前房间内的所有玩家
     * @param roomNo
     * @return
     */
    public JSONArray obtainAllPlayer(String roomNo){
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        for(String account : room.getPlayerMap().keySet()){
            Playerinfo player = room.getPlayerMap().get(account);
            if(player!=null){
                UserPacketGPPJ up = room.getUserPacketMap().get(account);
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                obj.put("score", player.getScore());
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", up.getStatus());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 获取可选抢庄倍数
     * @param roomNo
     * @param account
     * @return
     */
    public JSONArray obtainQzBtn(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        JSONArray qzTimes = room.getQzTimes();
        for (int i = 0; i < qzTimes.size(); i++) {
            JSONObject obj = new JSONObject();
            int value = qzTimes.getInt(i);
            obj.put("name",String.valueOf(new StringBuffer().append(value).append("倍")));
            obj.put("val",value);
            obj.put("isUse",CommonConstant.GLOBAL_NO);
            // 房卡场不做分数判断
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                obj.put("isUse",CommonConstant.GLOBAL_YES);
            }
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取可选下注倍数
     * @param roomNo
     * @param account
     * @return
     */
    public JSONArray obtainXzBtn(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        JSONArray xzTimes = room.getBaseNum();
        for (int i = 0; i < xzTimes.size(); i++) {
            JSONObject obj = xzTimes.getJSONObject(i);
            obj.put("isUse",CommonConstant.GLOBAL_NO);
            // 房卡场不做分数判断
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                obj.put("isUse",CommonConstant.GLOBAL_YES);
            }
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取展示按钮类别
     * @param roomNo
     * @param account
     * @return
     */
    public int obtainBtnType(String roomNo, String account) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 游戏局数已到或房间总结算状态展示查看总成绩按钮
        if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
            return GPPJConstant.GP_PJ_BTN_TYPE_SHOW;
        }
        // 初始、准备、结算阶段
        if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_INIT||room.getGameStatus()==GPPJConstant.GP_PJ_BTN_TYPE_READY||
            room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY) {
            // 玩家在当前房间内
            if (!Dto.stringIsNULL(account)&&room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_READY) {
                    // 庄家展示开始游戏按钮，其余玩家展示准备按钮
                    if (account.equals(room.getOwner())) {
                        return GPPJConstant.GP_PJ_BTN_TYPE_START;
                    }else {
                        return GPPJConstant.GP_PJ_USER_STATUS_READY;
                    }
                }
            }
        }
        return GPPJConstant.GP_PJ_BTN_TYPE_NONE;
    }

    /**
     * 获取开始游戏按钮状态
     * @param roomNo
     * @return
     */
    public int obtainStartBtnStatus(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 初始、准备、结算阶段
        if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_INIT||room.getGameStatus()==GPPJConstant.GP_PJ_BTN_TYPE_READY||
            room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY) {
            // 有人准备返回1
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                    if (room.getUserPacketMap().get(account).getStatus()==GPPJConstant.GP_PJ_USER_STATUS_READY) {
                        return CommonConstant.GLOBAL_YES;
                    }
                }
            }
        }
        return CommonConstant.GLOBAL_NO;
    }

    /**
     * 获取剩余牌组
     * @param roomNo
     * @return
     */
    public int[] obtainLeftPai(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getLeftPai()==null) {
            return new int[]{};
        }
        if (room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SHOW||room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_SUMMARY||
            room.getGameStatus()==GPPJConstant.GP_PJ_GAME_STATUS_FINAL_SUMMARY) {
            return room.getLeftPai();
        }else {
            int[] leftPai = new int[room.getLeftPai().length];
            for (int i = 0; i < leftPai.length; i++) {
                leftPai[i] = 0;
            }
            return leftPai;
        }
    }

    /**
     * 获取总结算数据
     * @param roomNo
     */
    public JSONArray obtainFinalSummaryData(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 有结算数据直接返回结算数据，防止退出之后少人
        if (room.getFinalSummaryData().size()>0) {
            return room.getFinalSummaryData();
        }
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getPlayTimes()>0) {
                    JSONObject obj = new JSONObject();
                    obj.put("name",room.getPlayerMap().get(account).getName());
                    obj.put("account",account);
                    obj.put("headimg",room.getPlayerMap().get(account).getRealHeadimg());
                    obj.put("score",room.getPlayerMap().get(account).getScore());
                    obj.put("isOwner",CommonConstant.GLOBAL_NO);
                    if (account.equals(room.getOwner())) {
                        obj.put("isOwner",CommonConstant.GLOBAL_YES);
                    }
                    obj.put("isWinner",CommonConstant.GLOBAL_NO);
                    if (room.getPlayerMap().get(account).getScore()>0) {
                        obj.put("isWinner",CommonConstant.GLOBAL_YES);
                    }
                    array.add(obj);
                }
            }
        }
        room.setFinalSummaryData(array);
        return array;
    }

    /**
     * 获取发牌下标
     * @param roomNo
     */
    public JSONArray obtainPaiIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 切牌下标
        int index = room.getCutPlace();
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus() > GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    array.add(index);
                    index++;
                    // 超出牌的下标重置
                    if (index>16) {
                        index=1;
                    }
                }
            }
        }
        return array;
    }

    /**
     * 获取发牌玩家座位号
     * @param roomNo
     */
    public JSONArray obtainPlayerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                if (room.getUserPacketMap().get(account).getStatus() > GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                    array.add(room.getPlayerMap().get(account).getMyIndex());
                }
            }
        }
        return array;
    }

    /**
     * 获取庄家倍数
     * @param roomNo
     * @return
     */
    public int obtainBankerTimes(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 庄家存在返回庄家下标，否则返回-1
        if (!Dto.stringIsNULL(room.getBanker())&&room.getUserPacketMap().containsKey(room.getBanker())&&room.getUserPacketMap().get(room.getBanker())!=null) {
            return room.getUserPacketMap().get(room.getBanker()).getBankerTimes();
        }
        return -1;
    }

    /**
     * 获取庄家下标
     * @param roomNo
     * @return
     */
    public int obtainBankerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 房主坐庄返回房主下标
        if (room.getBankerType() == GPPJConstant.BANKER_TYPE_OWNER) {
            if (!Dto.stringIsNULL(room.getOwner())&&room.getPlayerMap().containsKey(room.getOwner())&&room.getPlayerMap().get(room.getOwner())!=null) {
                return room.getPlayerMap().get(room.getOwner()).getMyIndex();
            }
        }
        // 看牌抢庄返回庄家下标
        if (room.getBankerType() == GPPJConstant.BANKER_TYPE_LOOK) {
            if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().containsKey(room.getBanker())&&room.getPlayerMap().get(room.getBanker())!=null) {
                return room.getPlayerMap().get(room.getBanker()).getMyIndex();
            }
        }
        return CommonConstant.NO_BANKER_INDEX;
    }

    /**
     * 获取房主下标
     * @param roomNo
     * @return
     */
    public int obtainOwnerIndex(String roomNo) {
        GPPJGameRoom room = (GPPJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 返回房主下标
        if (!Dto.stringIsNULL(room.getOwner())&&room.getPlayerMap().containsKey(room.getOwner())&&room.getPlayerMap().get(room.getOwner())!=null) {
            return room.getPlayerMap().get(room.getOwner()).getMyIndex();
        }
        return CommonConstant.NO_BANKER_INDEX;
    }
}
