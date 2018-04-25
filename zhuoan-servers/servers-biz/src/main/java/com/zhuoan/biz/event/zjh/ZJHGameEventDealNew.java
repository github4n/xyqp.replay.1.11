package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.ZJHConstant;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

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
        // 元宝不足无法准备
        if (room.getPlayerMap().get(account).getScore() < room.getLeaveScore()) {
            // 清出房间
            exitRoom(client,data);
            return;
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
                    // TODO: 2018/4/25 定时器
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

    public void startGame(ZJHGameRoomNew room) {
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
                    Playerinfo playerinfo = room.getPlayerMap().get(account);
                    room.getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                    // 负数清零
                    if (room.getPlayerMap().get(account).getScore() < 0) {
                        room.getPlayerMap().get(account).setScore(0);
                    }
                    array.add(playerinfo.getId());
                }
            }
            // TODO: 2018/4/25 抽水
            //producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
        // 玩家下注（底注）
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().get(account).getStatus() > ZJHConstant.ZJH_USER_STATUS_INIT) {
                int myIndex = room.getPlayerIndex(account);
                double score = room.getCurrentScore();
                // 添加下注记录
                room.addXiazhuList(myIndex, score);
                // 更新下注记录
                room.addScoreChange(account, score);
            }
        }
        JSONObject result = obtainStartData(room);
        room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_GAME);
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
    public boolean xiaZhu(ZJHGameRoomNew room,String account,double score,int type){
        JSONObject result = new JSONObject();
        // 看牌翻倍
        if (type==ZJHConstant.GAME_ACTION_TYPE_JZ) {
            room.setCurrentScore(score);
        }
        if (room.getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_KP) {
            score *= 2;
        }
        // 金币不足
        if (room.getPlayerMap().get(account).getScore()<score) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"元宝不足");
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(),result.toString(),"gameActionPush_ZJH");
            return false;
        }
        int myIndex = room.getPlayerIndex(account);
        // 添加下注记录
        room.addXiazhuList(myIndex, score);
        // 更新下注记录
        room.addScoreChange(account, score);
        // 添加下注用户
        room.addXzPlayer(myIndex);

        // 到达下注论述强制比牌
        if (room.getGameNum()==room.getTotalGameNum()||room.getTotalScore()==room.getMaxScore()) {
            // TODO: 2018/4/25 强制结算
            
        }

        if (type!=ZJHConstant.GAME_ACTION_TYPE_COMPARE) {
            // 通知玩家
            for (String uuid  : room.getUserPacketMap().keySet()) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put("nextNum", room.getPlayerIndex(room.getNextOperationPlayer(account)));
                result.put("gameNum", room.getGameNum());
                result.put("currentScore", room.getCurrentScore());
                result.put("totalScore", room.getTotalScore());
                result.put("myScore", room.getUserPacketMap().get(account).getScore());
                result.put("score", score);
                result.put("realScore", room.getPlayerMap().get(account).getScore());
                result.put("type", type);
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),result.toString(),"gameActionPush_ZJH");
            }
        }
        return true;
    }

    /**
     * 比牌
     * @param room
     * @param account
     * @param index
     */
    public void compare(ZJHGameRoomNew room,String account,int index){
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
                    String nextPlayer = room.getNextOperationPlayer(account);
                    int nextNum = room.getPlayerIndex(nextPlayer);
                    int gameNum = room.getGameNum();
                    if (isGameOver==1) {
                        summary(room);
                    }
                    // 通知玩家
                    for (String uid : room.getUserPacketMap().keySet()) {
                        JSONObject result = new JSONObject();
                        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                        result.put("index", room.getPlayerIndex(account));
                        result.put("nextNum", nextNum);
                        result.put("gameNum", gameNum);
                        result.put("currentScore", room.getCurrentScore());
                        result.put("totalScore", room.getTotalScore());
                        result.put("myScore", room.getUserPacketMap().get(uid).getScore());
                        result.put("score", room.getCurrentScore());
                        result.put("type", ZJHConstant.GAME_ACTION_TYPE_COMPARE);
                        result.put("result", compareResult);
                        result.put("isGameover", isGameOver);
                        if(isGameOver==1){
                            result.put("jiesuan",obtainSummaryData(room));
                            result.put("showPai",room.getUserPacketMap().get(uid).getBipaiList());
                        }
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uid).getUuid(),result.toString(),"gameActionPush_ZJH");
                    }
                }
            }
        }
    }

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

    public void giveUp(ZJHGameRoomNew room,String account) {
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
        String nextPlayer = room.getNextOperationPlayer(account);
        int nextNum = room.getPlayerIndex(nextPlayer);
        int gameNum = room.getGameNum();
        if(isGameOver==0 && room.getYiXiaZhu().contains(nextNum)){
            gameNum = gameNum+1;
        }

        if (isGameOver==1) {
            summary(room);
        }
        // 通知玩家
        for (String uid : room.getUserPacketMap().keySet()) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("index", room.getPlayerIndex(account));
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
    }

    public void summary(ZJHGameRoomNew room){
        room.setGameStatus(ZJHConstant.ZJH_GAME_STATUS_SUMMARY);
        for (String uuid : room.getUserPacketMap().keySet()) {

            Playerinfo player = room.getPlayerMap().get(uuid);
            // 参与游戏的玩家才能参与结算
            if(room.getUserPacketMap().get(uuid).getStatus()>ZJHConstant.ZJH_USER_STATUS_INIT) {
                if (room.getUserPacketMap().get(uuid).getStatus()==ZJHConstant.ZJH_USER_STATUS_WIN) {
                    double d = room.getTotalScore();
                    double oldScore = player.getScore();
                    room.getPlayerMap().get(uuid).setScore(Dto.add(oldScore,room.getTotalScore()));
                    room.setBanker(uuid);
                }
            }
        }
        // TODO: 2018/4/25 更新数据库
    }


    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        ZJHGameRoomNew room = (ZJHGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
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
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no", room.getRoomNo());
                roomInfo.put("user_id" + room.getPlayerMap().get(account).getMyIndex(), 0);
                // 移除数据
                for (int i = 0; i < room.getUserIdList().size(); i++) {
                    if (room.getUserIdList().get(i) == room.getPlayerMap().get(account).getId()) {
                        room.getUserIdList().set(i, (long) 0);
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
                CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_ZJH");
                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (room.isAllReady() && room.getPlayerMap().size() >= ZJHConstant.ZJH_MIN_START_COUNT) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size() == 0) {
                    roomInfo.put("status", -1);
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                //producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
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
        if (!room.getPlayerMap().containsKey(account)||room.getPlayerMap().get(account)==null) {
            result.put("type",CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_ZJH");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        client.set(CommonConstant.CLIENT_TAG_ACCOUNT,account);
        client.set(CommonConstant.CLIENT_TAG_ROOM_NO,roomNo);
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
    public JSONObject obtainStartData(ZJHGameRoomNew room){
        JSONObject object = new JSONObject();
        object.put("gameStatus",room.getGameStatus());
        object.put("zhuang",room.getBanker());
        object.put("game_index",room.getGameIndex());
        object.put("nextNum",room.getPlayerIndex(room.getNextOperationPlayer(room.getBanker())));
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
        obj.put("xzTimer", room.getXzTimer());
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            StringBuffer roominfo = new StringBuffer();
            roominfo.append("底注:");
            roominfo.append(room.getScore());
            roominfo.append("进:");
            roominfo.append(room.getEnterScore());
            roominfo.append(" 出:");
            roominfo.append(room.getEnterScore());
            StringBuffer diInfo = new StringBuffer();
            diInfo.append("底注:");
            diInfo.append(room.getScore());
            StringBuffer roominfo3 = new StringBuffer();
            roominfo3.append("进:");
            roominfo3.append(room.getEnterScore());
            roominfo3.append(" 出:");
            roominfo3.append(room.getEnterScore());
            obj.put("diInfo", diInfo.toString());
            obj.put("roominfo3", roominfo3.toString());
            obj.put("roominfo", roominfo.toString());
            obj.put("roominfo2", room.getWfType());
        }
        obj.put("baseNum", room.getBaseNum());
        obj.put("totalGameNum", room.getTotalGameNum());
        obj.put("dizhu", room.getScore());
        obj.put("wanfa", room.getWfType());
        obj.put("gameStatus", room.getGameStatus());
        if (room.getUserPacketMap().containsKey(room.getBanker()) && room.getUserPacketMap().get(room.getBanker()) != null) {
            obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
        } else {
            obj.put("zhuang", -1);
        }
        obj.put("game_index", room.getGameIndex());
        obj.put("showTimer", CommonConstant.GLOBAL_NO);
        if (room.getTimeLeft() > ZJHConstant.ZJH_TIMER_INIT) {
            obj.put("showTimer", CommonConstant.GLOBAL_YES);
        }
        obj.put("timer", room.getTimeLeft());
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
        return obj;
    }
}
