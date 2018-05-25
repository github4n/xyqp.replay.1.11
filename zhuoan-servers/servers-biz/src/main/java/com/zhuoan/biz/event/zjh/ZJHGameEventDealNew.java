package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.*;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 10:14 2018/4/25
 * @Modified By:
 **/
@Component
public class ZJHGameEventDealNew {

    public static int GAME_ZJH = 1;

    private final static Logger logger = LoggerFactory.getLogger(ZJHGameEventDealNew.class);

    @Resource
    private GameTimerZJH gameTimerZJH;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;
    /**
     * 创建房间通知自己
     *
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
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_ZJH");
        }
    }

    /**
     * 加入房间通知
     *
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
            ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
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
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush_ZJH");
        }
    }

    public void gameReady(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_INIT, client) &&
            !CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_READY, client) &&
            !CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (ZJHGameEventDealNew.GAME_ZJH==0) {
            postData.put("notSend",CommonConstant.GLOBAL_YES);
            exitRoom(client,postData);
            JSONObject result = new JSONObject();
            result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
            result.put(CommonConstant.RESULT_KEY_MSG,"即将停服进行更新");
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getLeaveScore()) {
                // 清出房间
                postData.put("notSend",CommonConstant.GLOBAL_YES);
                postData.put("notSendToMe",CommonConstant.GLOBAL_NO);
                exitRoom(client,postData);
                JSONObject result = new JSONObject();
                result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                    result.put(CommonConstant.RESULT_KEY_MSG,"元宝不足");
                }
                if(room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                    result.put(CommonConstant.RESULT_KEY_MSG,"金币不足");
                }
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
                return;
            }
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != ZJHConstant.ZJH_GAME_STATUS_READY) {
            room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_READY);
        }
        // 当前准备人数大于最低开始人数开启定时器
        if (room.getNowReadyCount() == ZJHConstant.ZJH_MIN_START_COUNT) {
            room.setTimeLeft(ZJHConstant.ZJH_TIMER_READY);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerZJH.readyOverTime(roomNo,ZJHConstant.ZJH_GAME_STATUS_READY);
                }
            });
        }
        // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
        if (room.isAllReady() && room.getPlayerMap().size() >= ZJHConstant.ZJH_MIN_START_COUNT) {
            startGame(room);
        } else {
            JSONObject result = new JSONObject();
            result.put("index", room.getPlayerMap().get(account).getMyIndex());
            result.put("showTimer", CommonConstant.GLOBAL_NO);
            if (room.getNowReadyCount() >= ZJHConstant.ZJH_MIN_START_COUNT) {
                result.put("showTimer", CommonConstant.GLOBAL_YES);
            }
            result.put("timer", room.getTimeLeft());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "playerReadyPush_ZJH");
        }
    }

    public void startGame(final ZJHGameRoomNew room) {
        // 非准备或初始阶段无法开始开始游戏
        if (room.getGameStatus() != ZJHConstant.ZJH_GAME_STATUS_READY && room.getGameStatus() != ZJHConstant.ZJH_GAME_STATUS_INIT) {
            return;
        }
        // 初始化房间信息
        room.initGame();
        // 洗牌
        room.xiPai();
        // 发牌
        room.faPai();
        if (room.getFee() > 0) {
            JSONArray array = new JSONArray();
            for (String account : room.getPlayerMap().keySet()) {
                // 中途加入不抽水
                if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                    // 更新实体类数据
                    Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account);
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                    // 负数清零
                    if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                    }
                    array.add(playerinfo.getId());
                }
            }
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
        JSONArray gameProcessFP = new JSONArray();
        // 玩家下注（底注）
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                int myIndex = room.getPlayerIndex(account);
                double score = room.getCurrentScore();
                // 添加下注记录
                room.addXiazhuList(myIndex, score);
                // 更新下注记录
                room.addScoreChange(account, score);
                // 存放游戏记录
                JSONObject userPai = new JSONObject();
                userPai.put("account", account);
                userPai.put("name", room.getPlayerMap().get(account).getName());
                userPai.put("pai", room.getUserPacketMap().get(account).getPai());
                gameProcessFP.add(userPai);
            }
        }
        room.getGameProcess().put("faPai",gameProcessFP);
        room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_GAME);
        final String player = room.getNextOperationPlayer(room.getBanker());
        // 检查下家跟注状态
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerZJH.gameOverTime(room.getRoomNo(),ZJHConstant.ZJH_GAME_STATUS_GAME,player);
            }
        });
        JSONObject result = obtainStartData(room,player);
        // 通知玩家
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"gameStartPush_ZJH");
    }

    public void gameEvent(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, ZJHConstant.ZJH_GAME_STATUS_GAME, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (postData.containsKey("type")) {
            int type = postData.getInt("type");
            // 验证是否轮到该玩家操作(看牌和跟到底不需要验证)
            if (type!=ZJHConstant.GAME_ACTION_TYPE_GDD&&type!=ZJHConstant.GAME_ACTION_TYPE_LOOK) {
                if (!room.getFocus().equals(account)) {
                    JSONObject result = new JSONObject();
                    result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                    result.put(CommonConstant.RESULT_KEY_MSG,"当前无法操作");
                    CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameActionPush_ZJH");
                    return;
                }
            }
            switch (type) {
                case ZJHConstant.GAME_ACTION_TYPE_GDD:
                    int value = postData.getInt("value");
                    genDaoDi(room,account,value,client);
                    break;
                case ZJHConstant.GAME_ACTION_TYPE_GIVE_UP:
                    giveUp(room,account);
                    break;
                case ZJHConstant.GAME_ACTION_TYPE_COMPARE:
                    int index = postData.getInt("index");
                    compare(room,account,index);
                    break;
                case ZJHConstant.GAME_ACTION_TYPE_LOOK:
                    look(room,account);
                    break;
                case ZJHConstant.GAME_ACTION_TYPE_GZ:
                    xiaZhu(room,account,room.getCurrentScore(),ZJHConstant.GAME_ACTION_TYPE_GZ);
                    break;
                case ZJHConstant.GAME_ACTION_TYPE_JZ:
                    double score = postData.getDouble("score");
                    xiaZhu(room,account,score,ZJHConstant.GAME_ACTION_TYPE_JZ);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 跟到底
     * @param room
     * @param account
     * @param value
     */
    public void genDaoDi(ZJHGameRoomNew room,String account,int value,SocketIOClient client){
        JSONObject result = new JSONObject();
        if (value==1) {
            room.getUserPacketMap().get(account).isGenDaoDi = true;
        } else {
            room.getUserPacketMap().get(account).isGenDaoDi = false;
        }
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        result.put("value",value);
        result.put("index",room.getPlayerIndex(account));
        result.put("type",ZJHConstant.GAME_ACTION_TYPE_GDD);
        CommonConstant.sendMsgEventToSingle(client, result.toString(), "gameActionPush_ZJH");
        if (room.getFocus().equals(account)&&room.getUserPacketMap().get(account).isGenDaoDi) {
            xiaZhu(room,account,room.getCurrentScore(),ZJHConstant.GAME_ACTION_TYPE_GZ);
        }
    }

    /**
     * 看牌
     * @param room
     * @param account
     */
    public void look(ZJHGameRoomNew room,String account) {
        room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_KP);
        for (String uuid : room.getPlayerMap().keySet()) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("type",ZJHConstant.GAME_ACTION_TYPE_LOOK);
            result.put("index",room.getPlayerIndex(account));
            if (uuid.equals(account)) {
                result.put("mypai",room.getUserPacketMap().get(account).getPai());
                result.put("paiType",room.getUserPacketMap().get(account).getType());
            }
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),result.toString(),"gameActionPush_ZJH");
        }
    }

    /**
     * 下注
     * @param room
     * @param account
     * @param score
     * @param type
     */
    public boolean xiaZhu(final ZJHGameRoomNew room,String account,double score,int type){
        JSONObject result = new JSONObject();
        // 加注
        if (type==ZJHConstant.GAME_ACTION_TYPE_JZ) {
            room.setCurrentScore(score);
        }
        // 看牌翻倍
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_KP) {
            score *= 2;
        }
        // 金币不足
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore()<score) {
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG,"元宝不足");
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(),result.toString(),"gameActionPush_ZJH");
                return false;
            }
        }
        int myIndex = room.getPlayerIndex(account);
        final String nextPlayer = room.getNextOperationPlayer(account);
        // 添加下注记录
        room.addXiazhuList(myIndex, score);
        // 更新下注记录
        room.addScoreChange(account, score);
        // 添加下注用户
        room.addXzPlayer(myIndex,room.getPlayerIndex(nextPlayer));
        if (type!=ZJHConstant.GAME_ACTION_TYPE_COMPARE) {
            // 检查下家跟注状态
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerZJH.gameOverTime(room.getRoomNo(),ZJHConstant.ZJH_GAME_STATUS_GAME,nextPlayer);
                }
            });
        }
        int isGameOver = 0;
        // 到达下注论述强制比牌
        if (room.getGameNum()==room.getTotalGameNum()||room.getTotalScore()>=room.getMaxScore()) {
            compelCompare(room,account);
            isGameOver = 1;
        }
        // 比牌下注不需要通知玩家
        if (type!=ZJHConstant.GAME_ACTION_TYPE_COMPARE||isGameOver==1) {
            // 通知玩家
            for (String uuid  : room.getUserPacketMap().keySet()) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("gameStatus", room.getGameStatus());
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put("nextNum", room.getPlayerIndex(nextPlayer));
                result.put("gameNum", room.getGameNum());
                result.put("currentScore", room.getCurrentScore());
                result.put("totalScore", room.getTotalScore());
                result.put("myScore", room.getUserPacketMap().get(account).getScore());
                result.put("score", score);
                result.put("realScore", room.getPlayerMap().get(account).getScore());
                result.put("type", type);
                result.put("isGameover", isGameOver);
                if (isGameOver==1){
                    result.put("jiesuan",obtainSummaryData(room));
                    result.put("showPai",room.getUserPacketMap().get(uuid).getBipaiList());
                }
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),result.toString(),"gameActionPush_ZJH");
            }
            sendFianlSummaryToPlayer(isGameOver,room);
        }
        // 游戏结束
        if (type==ZJHConstant.GAME_ACTION_TYPE_COMPARE&&isGameOver==1) {
            return false;
        }
        return true;
    }

    /**
     * 比牌
     * @param room
     * @param account
     * @param index
     */
    public void compare(final ZJHGameRoomNew room,String account,int index){
        if (xiaZhu(room,account,room.getCurrentScore(),ZJHConstant.GAME_ACTION_TYPE_COMPARE)) {
            double score = room.getCurrentScore();
            if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_KP) {
                score *= 2;
            }
            for (String other : room.getPlayerMap().keySet()) {
                if (room.getPlayerMap().get(other).getMyIndex()==index) {
                    Playerinfo player = room.getPlayerMap().get(other);
                    Integer[] myPai = room.getUserPacketMap().get(account).getPai();
                    Integer[] otherPai = room.getUserPacketMap().get(other).getPai();
                    int backResult = ZhaJinHuaCore.compare(Arrays.asList(myPai), Arrays.asList(otherPai));
                    // 添加比牌记录
                    room.getUserPacketMap().get(account).addBiPaiList(index, otherPai);
                    room.getUserPacketMap().get(account).addBiPaiList(room.getPlayerIndex(account), myPai);
                    room.getUserPacketMap().get(other).addBiPaiList(index, otherPai);
                    room.getUserPacketMap().get(other).addBiPaiList(room.getPlayerIndex(account), myPai);
                    // 比牌结果
                    JSONArray compareResult = new JSONArray();
                    JSONObject me = new JSONObject();
                    me.put("index", room.getPlayerIndex(account));
                    JSONObject obj = new JSONObject();
                    obj.put("index", player.getMyIndex());
                    if(backResult>0){
                        me.put("result", 1);
                        obj.put("result", 0);
                        compareResult.add(me);
                        compareResult.add(obj);
                        // 失败
                        room.getUserPacketMap().get(other).setStatus(ZJHConstant.ZJH_USER_STATUS_LOSE);
                    }else{ // 相等比牌的人输
                        me.put("result", 0);
                        obj.put("result", 1);
                        compareResult.add(me);
                        compareResult.add(obj);
                        // 失败
                        room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_LOSE);
                    }
                    // 游戏结束
                    int isGameOver = 0;
                    // 当剩下一个玩家还未开牌时，游戏结束
                    if(room.getProgressIndex().length<=1){
                        isGameOver = 1;
                        // 最后一次对决，确定胜利玩家
                        if(backResult>0){
                            room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_WIN);
                        }else{
                            room.getUserPacketMap().get(other).setStatus(ZJHConstant.ZJH_USER_STATUS_WIN);
                        }
                    }
                    // 确定下次操作的玩家
                    final String nextPlayer = room.getNextOperationPlayer(account);
                    // 轮数+1
                    if (room.getYiXiaZhu().contains(room.getPlayerIndex(nextPlayer))) {
                        room.getYiXiaZhu().clear();
                        room.setGameNum(room.getGameNum()+1);
                    }
                    int gameNum = room.getGameNum();
                    // 结算
                    if (isGameOver==1) {
                        summary(room);
                    }
                    // 通知玩家
                    for (String uid : room.getUserPacketMap().keySet()) {
                        JSONObject result = new JSONObject();
                        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                        result.put("gameStatus", room.getGameStatus());
                        result.put("index", room.getPlayerIndex(account));
                        result.put("nextNum", -1);
                        result.put("gameNum", gameNum);
                        result.put("currentScore", room.getCurrentScore());
                        result.put("totalScore", room.getTotalScore());
                        result.put("myScore", room.getUserPacketMap().get(account).getScore());
                        result.put("score", score);
                        result.put("realScore", room.getPlayerMap().get(account).getScore());
                        result.put("type", ZJHConstant.GAME_ACTION_TYPE_COMPARE);
                        result.put("result", compareResult);
                        result.put("isGameover", isGameOver);
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uid).getUuid(),result.toString(),"gameActionPush_ZJH");
                    }
                    // 休眠4.5秒前端播放动画
                    try {
                        Thread.sleep(4500);
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                    int nextNum = room.getPlayerIndex(nextPlayer);
                    // 通知玩家比牌结束
                    for (String uid : room.getUserPacketMap().keySet()) {
                        JSONObject result = new JSONObject();
                        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                        result.put("type", ZJHConstant.GAME_ACTION_TYPE_COMPARE_FINISH);
                        result.put("gameStatus", room.getGameStatus());
                        result.put("nextNum", nextNum);
                        result.put("index", room.getPlayerIndex(account));
                        result.put("gameNum", gameNum);
                        result.put("currentScore", room.getCurrentScore());
                        result.put("totalScore", room.getTotalScore());
                        result.put("myScore", room.getUserPacketMap().get(account).getScore());
                        result.put("score", score);
                        result.put("realScore", room.getPlayerMap().get(account).getScore());
                        result.put("result", compareResult);
                        result.put("isGameover", isGameOver);
                        if(isGameOver==1){
                            result.put("jiesuan",obtainSummaryData(room));
                            result.put("showPai",room.getUserPacketMap().get(uid).getBipaiList());
                        }
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uid).getUuid(),result.toString(),"gameActionPush_ZJH");
                    }
                    if(isGameOver!=1){
                        // 检查下家跟注状态
                        ThreadPoolHelper.executorService.submit(new Runnable() {
                            @Override
                            public void run() {
                                gameTimerZJH.gameOverTime(room.getRoomNo(),ZJHConstant.ZJH_GAME_STATUS_GAME,nextPlayer);
                            }
                        });
                    }
                    sendFianlSummaryToPlayer(isGameOver,room);
                }
            }
        }
    }

    /**
     * 总结算通知玩家
     * @param isGameOver
     * @param room
     */
    public void sendFianlSummaryToPlayer(int isGameOver,ZJHGameRoomNew room) {
        if (isGameOver==1&&room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            // 局数到了之后触发总结算
            if (room.getGameIndex()==room.getGameCount()) {
                room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_FINAL_SUMMARY);
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                result.put("type",ZJHConstant.GAME_ACTION_TYPE_FINAL_SUMMARY);
                result.put("jiesuanData",room.obtainFinalSummaryData());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"gameActionPush_ZJH");
            }
        }
    }

    /***
     * 获取结算数据
     * @param room
     * @return
     */
    public JSONArray obtainSummaryData(ZJHGameRoomNew room) {
        JSONArray summaryData = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().get(account).getStatus()>ZJHConstant.ZJH_USER_STATUS_INIT) {
                JSONObject playerData = new JSONObject();
                playerData.put("index",room.getPlayerIndex(account));
                playerData.put("headimg",room.getPlayerMap().get(account).getHeadimg());
                playerData.put("account",account);
                playerData.put("name",room.getPlayerMap().get(account).getName());
                if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                    playerData.put("win",CommonConstant.GLOBAL_YES);
                    playerData.put("score",Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
                }else {
                    playerData.put("win",CommonConstant.GLOBAL_NO);
                    playerData.put("score",-room.getUserPacketMap().get(account).getScore());
                }
                playerData.put("totalScore",room.getPlayerMap().get(account).getScore());
                summaryData.add(playerData);
            }
        }
        return summaryData;
    }

    /**
     * 弃牌
     * @param room
     * @param account
     */
    public void giveUp(final ZJHGameRoomNew room, String account) {
        room.getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_QP);
        // 游戏是否结束
        int isGameOver = 0;
        // 当剩下一个玩家还未开牌时，游戏结束
        if(room.getProgressIndex().length<=1){
            isGameOver = 1;
            // 剩下的玩家直接赢
            for (String winner :room.getUserPacketMap().keySet()) {
                int status = room.getUserPacketMap().get(winner).getStatus();
                // 暗牌或是明牌
                if(status==ZJHConstant.ZJH_USER_STATUS_AP || status==ZJHConstant.ZJH_USER_STATUS_KP){
                    room.getUserPacketMap().get(winner).setStatus(ZJHConstant.ZJH_USER_STATUS_WIN);
                    break;
                }
            }
        }
        if (room.getProgressIndex().length==room.getYiXiaZhu().size()) {
            room.setGameNum(room.getGameNum()+1);
        }
        // 确定下次操作的玩家
        final String nextPlayer = room.getNextOperationPlayer(account);
        // 检查下家跟注状态
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerZJH.gameOverTime(room.getRoomNo(),ZJHConstant.ZJH_GAME_STATUS_GAME,nextPlayer);
            }
        });
        int nextNum = room.getPlayerIndex(nextPlayer);
        int gameNum = room.getGameNum();
        if(isGameOver==0 && room.getYiXiaZhu().contains(nextNum)){
            room.getYiXiaZhu().clear();
        }
        if (isGameOver==1) {
            summary(room);
        }
        // 通知玩家
        for (String uid : room.getUserPacketMap().keySet()) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("index", room.getPlayerIndex(account));
            result.put("gameStatus", room.getGameStatus());
            result.put("nextNum", nextNum);
            result.put("gameNum", gameNum);
            result.put("currentScore", room.getCurrentScore());
            result.put("totalScore", room.getTotalScore());
            result.put("myScore", room.getUserPacketMap().get(account).getScore());
            result.put("type", ZJHConstant.GAME_ACTION_TYPE_GIVE_UP);
            result.put("isGameover", isGameOver);
            if(isGameOver==1){
                result.put("jiesuan",obtainSummaryData(room));
                result.put("showPai",room.getUserPacketMap().get(uid).getBipaiList());
            }
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uid).getUuid(),result.toString(),"gameActionPush_ZJH");
        }
        sendFianlSummaryToPlayer(isGameOver,room);
    }


    /**
     * 强制比牌
     * @param room
     * @param opuuid
     */
    public void compelCompare(ZJHGameRoomNew room, String opuuid) {
        // 获取还在游戏中的玩家
        List<String> uuidList = new ArrayList<String>();
        for (String uuid : room.getUserPacketMap().keySet()) {
            if(room.getUserPacketMap().get(uuid).getStatus()==ZJHConstant.ZJH_USER_STATUS_AP
                || room.getUserPacketMap().get(uuid).getStatus()==ZJHConstant.ZJH_USER_STATUS_KP){

                uuidList.add(uuid);
            }
        }
        // 比较牌的大小，牌最大者获胜
        if(uuidList.size()>1){
            // 添加比牌记录
            for (String uuid : uuidList) {
                for (String uuid1 : uuidList) {
                    room.getUserPacketMap().get(uuid).addBiPaiList(room.getPlayerMap().get(uuid1).getMyIndex(),
                        room.getUserPacketMap().get(uuid1).getPai());
                }
            }
            // 弃牌玩家可以看到强制结算玩家的牌
            for (String string : room.getUserPacketMap().keySet()) {
                if (!uuidList.contains(string)) {
                    for (String uuid : uuidList) {
                        room.getUserPacketMap().get(string).addBiPaiList(room.getPlayerMap().get(uuid).getMyIndex(),
                            room.getUserPacketMap().get(uuid).getPai());
                    }
                }
            }
            // 最大牌
            String maxUUID = uuidList.get(0);
            for (int i = 1; i < uuidList.size(); i++) {
                String uuid = uuidList.get(i);
                Integer[] paiA = room.getUserPacketMap().get(maxUUID).getPai();
                Integer[] paiB = room.getUserPacketMap().get(uuid).getPai();
                int biPaiResult = ZhaJinHuaCore.compare(Arrays.asList(paiA), Arrays.asList(paiB));
                if(biPaiResult>0){
                    // 失败
                    room.getUserPacketMap().get(uuid).setStatus(ZJHConstant.ZJH_USER_STATUS_LOSE);

                }else{ // 相等比牌的人输
                    // 失败
                    room.getUserPacketMap().get(maxUUID).setStatus(ZJHConstant.ZJH_USER_STATUS_LOSE);
                    // 重置最大牌
                    maxUUID = uuid;
                }
            }
            room.getUserPacketMap().get(maxUUID).setStatus(ZJHConstant.ZJH_USER_STATUS_WIN);
            // 结算
            summary(room);
        }
    }

    /**
     * 结算
     * @param room
     */
    public void summary(ZJHGameRoomNew room){
        room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_SUMMARY);
        // 重置房间倒计时
        room.setTimeLeft(ZJHConstant.ZJH_TIMER_INIT);
        for (String uuid : room.getUserPacketMap().keySet()) {

            Playerinfo player = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(uuid);
            // 参与游戏的玩家才能参与结算
            if(room.getUserPacketMap().get(uuid).getStatus()>ZJHConstant.ZJH_USER_STATUS_INIT) {
                if (room.getUserPacketMap().get(uuid).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                    double oldScore = player.getScore();
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(uuid).setScore(Dto.add(oldScore,room.getTotalScore()));
                    room.setBanker(uuid);
                }
                // 负数清零
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                    if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(uuid).getScore()<0) {
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(uuid).setScore(0);
                    }
                }
            }
        }
        updateUserScore(room.getRoomNo());
        saveUserDeductionData(room.getRoomNo());
        if (room.getRoomType()!= CommonConstant.ROOM_TYPE_JB) {
            saveGameLogs(room.getRoomNo());
        }
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            roomCardSummary(room.getRoomNo());
        }
    }

    /**
     * 房卡场结算
     * @param roomNo
     */
    public void roomCardSummary(String roomNo) {
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                UserPacket up = room.getUserPacketMap().get(account);
                if (up.getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                    up.setWinTimes(up.getWinTimes()+1);
                }
            }
        }
    }

    /**
     * 更新数据库
     * @param roomNo
     */
    public void updateUserScore(String roomNo) {
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                // 元宝输赢情况
                JSONObject obj = new JSONObject();
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                    obj.put("total", room.getPlayerMap().get(account).getScore());
                    if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                        obj.put("fen", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
                    }else {
                        obj.put("fen", -room.getUserPacketMap().get(account).getScore());
                    }
                    obj.put("id", room.getPlayerMap().get(account).getId());
                    array.add(obj);
                }else if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                    // 房主支付
                    if (room.getPayType()==CommonConstant.PAY_TYPE_OWNER) {
                        // 房主参与第一局需要扣房卡
                        if (account.equals(room.getOwner())&&room.getUserPacketMap().get(account).getPlayTimes()==1) {
                            obj.put("total", room.getPlayerMap().get(account).getRoomCardNum());
                            obj.put("id", room.getPlayerMap().get(account).getId());
                            obj.put("fen", -room.getPlayerCount()*room.getSinglePayNum());
                            array.add(obj);
                        }
                    }
                    // 房费AA
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA) {
                        // 参与第一局需要扣房卡
                        if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                            obj.put("total", room.getPlayerMap().get(account).getRoomCardNum());
                            obj.put("id", room.getPlayerMap().get(account).getId());
                            obj.put("fen", -room.getSinglePayNum());
                            array.add(obj);
                        }
                    }
                }
            }
        }
        // 更新玩家分数
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
    }

    public void saveUserDeductionData(String roomNo) {
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONArray userDeductionData = new JSONArray();
        if (room.getOutUserData().size()>0) {
            for (JSONObject outUserData : room.getOutUserData()) {
                userDeductionData.add(outUserData.getJSONObject("userDeduction"));
            }
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                // 用户游戏记录
                JSONObject object = obtainUserDeductionData(account,room);
                userDeductionData.add(object);
            }
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
    }

    public void saveGameLogs(String roomNo) {
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        // 存放游戏记录
        JSONArray gameProcessJS = new JSONArray();
        if (room.getOutUserData().size()>0) {
            for (JSONObject outUserData : room.getOutUserData()) {
                gameProcessJS.add(outUserData.getJSONObject("userSummary"));
                gameLogResults.add(outUserData.getJSONObject("gameLog"));
                gameResult.add(outUserData.getJSONObject("userGameLog"));
                array.add(outUserData.getJSONObject("obj"));
            }
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                // 游戏记录
                JSONObject userJS = obtainUserSummaryData(account,room);
                gameProcessJS.add(userJS);
                // 参与玩家
                JSONObject obj = new JSONObject();
                obj.put("total", room.getPlayerMap().get(account).getScore());
                if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                    obj.put("fen", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
                }else {
                    obj.put("fen", -room.getUserPacketMap().get(account).getScore());
                }
                obj.put("id", room.getPlayerMap().get(account).getId());
                array.add(obj);
                // 战绩记录
                JSONObject gameLogResult = obtainGameLogData(account,room);
                gameLogResults.add(gameLogResult);
                // 用户战绩
                JSONObject userResult = obtainUserResult(account,room);
                gameResult.add(userResult);
            }
        }
        room.getGameProcess().put("xiazhu", room.getXiaZhuList());
        room.getGameProcess().put("JieSuan", gameProcessJS);
        if (room.getId()==0) {
            JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
            if (!Dto.isObjNull(roomInfo)) {
                room.setId(roomInfo.getLong("id"));
            }
        }
        logger.info(room.getRoomNo()+"---"+String.valueOf(room.getGameProcess()));
        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
        for (int i = 0; i < userGameLogs.size(); i++) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
        }
    }

    /**
     * 获取玩家战绩数据
     * @param account
     * @param room
     * @return
     */
    public JSONObject obtainUserResult(String account,ZJHGameRoomNew room) {
        JSONObject userResult = new JSONObject();
        userResult.put("zhuang", room.getBanker());
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
            userResult.put("isWinner", CommonConstant.GLOBAL_YES);
            userResult.put("score", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
        }else {
            userResult.put("isWinner", CommonConstant.GLOBAL_NO);
            userResult.put("score", -room.getUserPacketMap().get(account).getScore());
        }
        userResult.put("totalScore", room.getPlayerMap().get(account).getScore());
        userResult.put("player", room.getPlayerMap().get(account).getName());
        return userResult;
    }

    /**
     * 获取战绩数据
     * @param account
     * @param room
     * @return
     */
    public JSONObject obtainGameLogData(String account,ZJHGameRoomNew room) {
        JSONObject gameLogResult = new JSONObject();
        gameLogResult.put("account", account);
        gameLogResult.put("name", room.getPlayerMap().get(account).getName());
        gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
        gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
        gameLogResult.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        gameLogResult.put("myPai", room.getUserPacketMap().get(account).getPai());
        gameLogResult.put("totalScore", room.getPlayerMap().get(account).getScore());
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
            gameLogResult.put("win", CommonConstant.GLOBAL_YES);
            gameLogResult.put("score", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
        }else {
            gameLogResult.put("score", -room.getUserPacketMap().get(account).getScore());
            gameLogResult.put("win", CommonConstant.GLOBAL_NO);
        }
        return gameLogResult;
    }

    /**
     * 获取游戏记录数据
     * @param account
     * @param room
     * @return
     */
    public JSONObject obtainUserDeductionData(String account,ZJHGameRoomNew room) {
        JSONObject object = new JSONObject();
        object.put("id", room.getPlayerMap().get(account).getId());
        object.put("gid", room.getGid());
        object.put("roomNo", room.getRoomNo());
        object.put("type", room.getRoomType());
        object.put("new", room.getPlayerMap().get(account).getScore());
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
            object.put("fen", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
            object.put("old", Dto.sub(room.getPlayerMap().get(account).getScore(),object.getDouble("fen")));
        }else {
            object.put("fen", -room.getUserPacketMap().get(account).getScore());
            object.put("old", Dto.sub(room.getPlayerMap().get(account).getScore(),object.getDouble("fen")));
        }
        return object;
    }

    /**
     * 获取结算数据
     * @param account
     * @param room
     * @return
     */
    public JSONObject obtainUserSummaryData(String account,ZJHGameRoomNew room) {
        JSONObject userJS = new JSONObject();
        userJS.put("account", account);
        userJS.put("name", room.getPlayerMap().get(account).getName());
        userJS.put("pai", room.getUserPacketMap().get(account).getPai());
        userJS.put("paiType", room.getUserPacketMap().get(account).getType());
        userJS.put("new", room.getPlayerMap().get(account).getScore());
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
            userJS.put("sum", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
            userJS.put("old", Dto.sub(room.getPlayerMap().get(account).getScore(),userJS.getDouble("sum")));
        }else {
            userJS.put("sum", -room.getUserPacketMap().get(account).getScore());
            userJS.put("old", Dto.sub(room.getPlayerMap().get(account).getScore(),userJS.getDouble("sum")));
        }
        return userJS;
    }

    /**
     * 退出房间
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            boolean canExit = false;
            // 金币场、元宝场
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                // 未参与游戏可以自由退出
                if (room.getUserPacketMap().get(account).getStatus() == ZJHConstant.ZJH_USER_STATUS_INIT) {
                    canExit = true;
                } else if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_INIT ||
                    room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_READY ||
                    room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
                    canExit = true;
                } else if (room.getUserPacketMap().get(account).getStatus() == ZJHConstant.ZJH_USER_STATUS_QP) {
                    canExit = true;
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("total", room.getPlayerMap().get(account).getScore());
                    if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                        obj.put("fen", Dto.sub(room.getTotalScore(),room.getUserPacketMap().get(account).getScore()));
                    }else {
                        obj.put("fen", -room.getUserPacketMap().get(account).getScore());
                    }
                    obj.put("id", room.getPlayerMap().get(account).getId());
                    array.add(obj);
                    // 更新玩家分数
                    producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
                    JSONObject outUserData = new JSONObject();
                    outUserData.put("userSummary",obtainUserSummaryData(account,room));
                    outUserData.put("gameLog",obtainGameLogData(account,room));
                    outUserData.put("userDeduction",obtainUserDeductionData(account,room));
                    outUserData.put("userGameLog",obtainUserResult(account,room));
                    outUserData.put("obj",obj);
                    room.getOutUserData().add(outUserData);
                }
            }else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                // 房卡场没玩过可以退出
                if (room.getUserPacketMap().get(account).getPlayTimes()==0) {
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA||!room.getOwner().equals(account)) {
                        canExit = true;
                    }
                }
                if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_FINAL_SUMMARY) {
                    canExit = true;
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                // 换庄
                if (room.getBanker().equals(account)) {
                    String newBanker = room.getNextPlayer(account);
                    room.setBanker(newBanker);
                }

                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no", room.getRoomNo());
                roomInfo.put("user_id" + room.getPlayerMap().get(account).getMyIndex(), 0);
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
                if (room.getGameStatus() == ZJHConstant.ZJH_GAME_STATUS_READY && room.getNowReadyCount() < ZJHConstant.ZJH_MIN_START_COUNT) {
                    // 重置房间倒计时
                    room.setTimeLeft(ZJHConstant.ZJH_TIMER_INIT);
                }
                if (room.getTimeLeft() > 0) {
                    result.put("showTimer", CommonConstant.GLOBAL_YES);
                } else {
                    result.put("showTimer", CommonConstant.GLOBAL_NO);
                }
                result.put("timer", room.getTimeLeft());
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_ZJH");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_ZJH");
                }
                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (room.isAllReady() && room.getPlayerMap().size() >= ZJHConstant.ZJH_MIN_START_COUNT) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size() == 0) {
                    roomInfo.put("status", -1);
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            } else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
                result.put("showTimer", CommonConstant.GLOBAL_YES);
                result.put("timer", room.getTimeLeft());
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush_ZJH");
            }
        }
    }

    /**
     * 解散房间
     *
     * @param client
     * @param data
     */
    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        final ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey("type")) {
            JSONObject result = new JSONObject();
            int type = postData.getInt("type");
            // 有人发起解散设置解散时间
            if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
                room.setJieSanTime(60);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerZJH.closeRoomOverTime(roomNo,room.getJieSanTime());
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
                    room.getUserPacketMap().get(uuid).isCloseRoom = CommonConstant.CLOSE_ROOM_UNSURE;
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_ZJH");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (room.isAgreeClose()) {
                    // 未玩完一局不需要强制结算
                    if (room.getGameIndex()<=1&&room.getGameStatus()<ZJHConstant.ZJH_GAME_STATUS_SUMMARY) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"房间已解散");
                        CommonConstant.sendMsgEventToAll(uuidList,result.toString(),"tipMsgPush");
                        return;
                    }
                    // 强制结算
                    room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_FINAL_SUMMARY);
                    result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                    result.put("type",ZJHConstant.GAME_ACTION_TYPE_FINAL_SUMMARY);
                    result.put("jiesuanData",room.obtainFinalSummaryData());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"gameActionPush_ZJH");
                } else {// 刷新数据
                    room.getUserPacketMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", room.getJieSanData());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_ZJH");
                }
            }
        }
    }

    /**
     * 重连
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject result = new JSONObject();
        if (client == null) {
            return;
        }
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
            result.put("type",CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_ZJH");
            return;
        }
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account)==null) {
            result.put("type",CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_ZJH");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type",1);
        result.put("data",obtainRoomData(roomNo,account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_ZJH");
    }

    /**
     * 获取开始游戏数据
     * @param room
     * @return
     */
    public JSONObject obtainStartData(ZJHGameRoomNew room,String player){
        JSONObject object = new JSONObject();
        object.put("gameStatus",room.getGameStatus());
        object.put("zhuang",room.getBanker());
        object.put("game_index",room.getGameIndex());
        object.put("nextNum",room.getPlayerIndex(player));
        object.put("gameNum",room.getGameNum());
        object.put("currentScore",room.getCurrentScore());
        object.put("totalScore",room.getTotalScore());
        object.put("users",room.getAllPlayer());
        return object;
    }

    /**
     * 获取房间数据
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainRoomData(String roomNo, String account) {
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("room_no", room.getRoomNo());
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("xzTimer", ZJHConstant.ZJH_TIMER_XZ);
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            StringBuffer roominfo = new StringBuffer();
            roominfo.append("底注:");
            roominfo.append((int) room.getScore());
            roominfo.append("进:");
            roominfo.append((int) room.getEnterScore());
            roominfo.append(" 出:");
            roominfo.append((int) room.getEnterScore());
            StringBuffer diInfo = new StringBuffer();
            diInfo.append("底注:");
            diInfo.append((int)room.getScore());
            StringBuffer roominfo3 = new StringBuffer();
            roominfo3.append("进:");
            roominfo3.append((int) room.getEnterScore());
            roominfo3.append(" 出:");
            roominfo3.append((int) room.getEnterScore());
            obj.put("diInfo", diInfo.toString());
            obj.put("roominfo3", roominfo3.toString());
            obj.put("roominfo", roominfo.toString());
            obj.put("roominfo2", room.getWfType());
        }
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            StringBuffer roominfo = new StringBuffer();
            roominfo.append(room.getWfType());
            roominfo.append(" ");
            roominfo.append(room.getPlayerCount());
            roominfo.append("人 ");
            roominfo.append(room.getGameCount());
            roominfo.append("局");
            obj.put("roominfo", roominfo.toString());
        }
        obj.put("baseNum", room.getBaseNum());
        obj.put("totalGameNum", room.getTotalGameNum());
        obj.put("dizhu", room.getScore());
        obj.put("wanfa", room.getGameType());
        obj.put("gameStatus", room.getGameStatus());
        if (!Dto.stringIsNULL(room.getBanker()) && room.getUserPacketMap().containsKey(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null) {
            obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
        } else {
            obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
        }
        obj.put("game_index", room.getGameIndex());
        obj.put("showTimer", CommonConstant.GLOBAL_NO);
        if (room.getTimeLeft() > ZJHConstant.ZJH_TIMER_INIT) {
            obj.put("showTimer", CommonConstant.GLOBAL_YES);
        }
        obj.put("timer", room.getTimeLeft());
        if (room.getGameStatus()==ZJHConstant.ZJH_GAME_STATUS_GAME) {
            obj.put("timer", room.getXzTimer());
        }
        obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        obj.put("mypai", room.getUserPacketMap().get(account).getPai());
        obj.put("paiType", room.getUserPacketMap().get(account).getType());
        obj.put("users", room.getAllPlayer());
        obj.put("isGendaodi", room.getUserPacketMap().get(account).isGenDaoDi);
        obj.put("nextNum", room.getPlayerIndex(room.getFocus()));
        obj.put("gameNum", room.getGameNum());
        obj.put("currentScore", room.getCurrentScore());
        obj.put("totalScore", room.getTotalScore());
        obj.put("xiazhuList", room.getXiaZhuList());
        obj.put("myScore", room.getPlayerScore());
        obj.put("isGameover", CommonConstant.GLOBAL_YES);
        if (room.getProgressIndex().length>1) {
            obj.put("isGameover", CommonConstant.GLOBAL_NO);
        }
        // 结算数据
        if (room.getGameStatus()==ZJHConstant.ZJH_GAME_STATUS_SUMMARY) {
            obj.put("jiesuan", obtainSummaryData(room));
            obj.put("showPai", room.getUserPacketMap().get(account).getBipaiList());
        }else {
            obj.put("jiesuan", new JSONArray());
            obj.put("showPai", new JSONArray());
        }
        if (room.getGameStatus()==ZJHConstant.ZJH_GAME_STATUS_FINAL_SUMMARY) {
            obj.put("jiesuanData", room.getFinalSummaryData());
        }
        return obj;
    }
}
