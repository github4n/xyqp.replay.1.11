package com.zhuoan.biz.event.sw;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.SwConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 14:00 2018/6/15
 * @Modified By:
 **/
@Component
public class SwGameEventDeal {

    @Resource
    GameTimerSw gameTimerSw;

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
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "enterRoomPush_SW");
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
            SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
            JSONObject result = new JSONObject();
            JSONObject user = obtainPlayerInfo(room.getRoomNo(),account);
            // 有座位的人进行通知
            if (user.getInt("index")<=SwConstant.SW_MAX_SEAT_NUM) {
                result.put("user",user);
            }
            result.put("playerCount",room.getPlayerMap().size());
            // 通知玩家
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(result), "playerEnterPush_SW");
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
        if (!CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_INIT, client)&&
            !CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_READY, client)&&
            !CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!account.equals(room.getBanker())) {
            return;
        }
        if (!postData.containsKey(SwConstant.SW_DATA_KEY_TREASURE)) {
            return;
        }
        // 押宝
        int treasure = postData.getInt(SwConstant.SW_DATA_KEY_TREASURE);
        if (treasure<SwConstant.TREASURE_BLACK_ROOK||treasure>SwConstant.TREASURE_RED_KING) {
            return;
        }
        // 重置结算次数
        redisService.insertKey("summaryTimes_sw_"+room.getRoomNo(),"0",null);
        // 设置押宝
        room.setTreasure(treasure);
        // 清空下注列表
        room.getBetArray().clear();
        // 清空结算列表
        room.getSummaryArray().clear();
        // 设置房间状态
        room.setGameStatus(SwConstant.SW_GAME_STATUS_BET);
        // 设置倒计时
        room.setTimeLeft(SwConstant.SW_TIME_BET);
        // 改变状态通知玩家
        changeGameStatus(roomNo);
        // 开始下注倒计时
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerSw.gameOverTime(roomNo, SwConstant.SW_TIME_BET);
            }
        });
    }

    /**
     * 下注
     * @param client
     * @param data
     */
    public void gameBet(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_BET, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (account.equals(room.getBanker())) {
            return;
        }
        if (!postData.containsKey(SwConstant.SW_DATA_KEY_PLACE)||!postData.containsKey(SwConstant.SW_DATA_KEY_VALUE)) {
            return;
        }
        // 下注位置
        int place = postData.getInt(SwConstant.SW_DATA_KEY_PLACE);
        // 下注金额
        int value = postData.getInt(SwConstant.SW_DATA_KEY_VALUE);
        if (place<SwConstant.TREASURE_BLACK_ROOK||place>SwConstant.TREASURE_RED_KING) {
            return;
        }
        if (value<=0||value>obtainMaxTimes(room.getBaseNum())) {
            return;
        }
        JSONObject result = new JSONObject();
        // 元宝是否足够
        if (value+room.getFee()>room.getPlayerMap().get(account).getScore()) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"余额不足");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameBetPush_SW");
            return;
        }
        // 单子是否达到上限
        if (room.getSingleMax()>0&&obtainTotalBetByPlace(roomNo,place)+value>room.getSingleMax()) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"已达下注上限");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameBetPush_SW");
            return;
        }
        // 庄家是否够赔
        if ((obtainTotalBetByPlace(roomNo,place)+value)*room.getRatio()*room.getScore()>room.getPlayerMap().get(room.getBanker()).getScore()) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"已达下注上限");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameBetPush_SW");
            return;
        }
        // 添加下注记录
        addBetRecord(roomNo,account,place,value);
        // 减少玩家分数
        changeUserScore(roomNo,account,-value*room.getScore());
        // 通知玩家
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        result.put("index",room.getPlayerMap().get(account).getMyIndex());
        result.put("value",value);
        result.put("place",place);
        result.put("myScore", obtainTotalBetByAccountAndPlace(roomNo,account,place));
        result.put("totalScore", obtainTotalBetByPlace(roomNo,place));
        result.put("scoreLeft", room.getPlayerMap().get(account).getScore());
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(result),"gameBetPush_SW");
    }

    /**
     * 上庄
     * @param client
     * @param data
     */
    public void gameBeBanker(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_CHOICE_BANKER, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 庄家已经确定
        if (!Dto.stringIsNULL(room.getBanker())) {
            return;
        }
        // 元宝不足无法上庄
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
                return;
            }
        }
        // 设置庄家
        room.setBanker(account);
        // 有座位的人上庄清空原先座位
        if (room.getPlayerMap().get(account).getMyIndex()<room.getUserIdList().size()) {
            room.getUserIdList().set(room.getPlayerMap().get(account).getMyIndex(),0L);
        }
        // 庄家坐0号位
        room.getPlayerMap().get(account).setMyIndex(0);
        room.getUserIdList().set(0,room.getPlayerMap().get(account).getId());
        // 设置游戏状态
        room.setGameStatus(SwConstant.SW_GAME_STATUS_READY);
        // 通知玩家
        changeGameStatus(roomNo);
    }

    /**
     * 撤销下注
     * @param client
     * @param data
     */
    public void gameUndo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, SwConstant.SW_GAME_STATUS_BET, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (account.equals(room.getBanker())) {
            return;
        }
        JSONObject result = new JSONObject();
        JSONObject lastBetRecord = obtainLastBetRecord(roomNo,account);
        if (!Dto.isObjNull(lastBetRecord)) {
            // 移除下注记录
            removeBetRecord(roomNo,account);
            // 增加分数
            changeUserScore(roomNo,account,lastBetRecord.getInt("value")*room.getScore());
            // 通知玩家
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.putAll(lastBetRecord);
            result.put("myScore", obtainTotalBetByAccountAndPlace(roomNo,account,lastBetRecord.getInt("place")));
            result.put("totalScore", obtainTotalBetByPlace(roomNo,lastBetRecord.getInt("place")));
            result.put("scoreLeft", room.getPlayerMap().get(account).getScore());
            // 通知玩家
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(result),"gameUndoPush_SW");
        }else {
            // 通知玩家
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"当前未下注");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameUndoPush_SW");
        }
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
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        boolean canExit = false;
        // 金币场、元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            // 未下注可以自由退出
            if (!account.equals(room.getBanker())&&Dto.isObjNull(obtainLastBetRecord(roomNo,account))) {
                canExit = true;
            } else if (room.getGameStatus() == SwConstant.SW_GAME_STATUS_INIT||
                room.getGameStatus() == SwConstant.SW_GAME_STATUS_READY||
                room.getGameStatus() == SwConstant.SW_GAME_STATUS_SUMMARY) {
                // 初始及准备阶段可以退出
                canExit = true;
            }
        }
        Playerinfo player = room.getPlayerMap().get(account);
        if (canExit) {
            List<UUID> allUUIDList = room.getAllUUIDList();
            // 更新数据库
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", room.getRoomNo());
            if (room.getPlayerMap().get(account).getMyIndex()<10) {
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
            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("type", 1);
            result.put("index", player.getMyIndex());
            result.put("playerCount", room.getPlayerMap().size());
            if (!postData.containsKey("notSend")) {
                CommonConstant.sendMsgEventToAll(allUUIDList, String.valueOf(result), "exitRoomPush_SW");
            }
            if (postData.containsKey("notSendToMe")) {
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "exitRoomPush_SW");
            }
            // 所有人都退出清除房间数据
            if (room.getPlayerMap().size() == 0) {
                redisService.deleteByKey("summaryTimes_sw_"+room.getRoomNo());
                roomInfo.put("status",room.getIsClose());
                roomInfo.put("game_index",room.getGameIndex());
                RoomManage.gameRoomMap.remove(room.getRoomNo());
            }
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            // 房主退出且房间内有其他玩家
            if (account.equals(room.getBanker())&&room.getPlayerMap().size()>0) {
                room.setBanker(null);
                // 开始上庄
                choiceBanker(roomNo);
            }
        } else {
            // 组织数据，通知玩家
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "游戏已开始无法退出");
            result.put("type", 1);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "exitRoomPush_SW");
        }
    }

    /**
     * 换座
     * @param client
     * @param data
     */
    public void gameChangeSeat(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        if (!postData.containsKey(SwConstant.SW_DATA_KEY_INDEX)) {
            return;
        }
        int index = postData.getInt(SwConstant.SW_DATA_KEY_INDEX);
        // 目标座位不合法
        if (index<SwConstant.SW_MIN_SEAT_NUM||index>SwConstant.SW_MAX_SEAT_NUM) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (account.equals(room.getBanker())) {
            return;
        }
        JSONObject result = new JSONObject();
        // 庄家无法换坐
        if (account.equals(room.getBanker())) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"庄家无法换座");
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
            return;
        }
        // 已下过注且非结算阶段
        if (room.getGameStatus()==SwConstant.SW_GAME_STATUS_BET) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"当前无法换座");
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
            return;
        }
        // 当前座位有人
        if (!Dto.stringIsNULL(obtainUserAccountByIndex(roomNo,index))) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"该座位已被使用");
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "gameChangeSeatPush_SW");
            return;
        }
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        result.put("lastIndex",room.getPlayerMap().get(account).getMyIndex());
        // 有座位的人换坐清空原先座位
        if (room.getPlayerMap().get(account).getMyIndex()<room.getUserIdList().size()) {
            room.getUserIdList().set(room.getPlayerMap().get(account).getMyIndex(),0L);
        }
        room.getPlayerMap().get(account).setMyIndex(index);
        // 设置座位号
        room.getUserIdList().set(index,room.getPlayerMap().get(account).getId());
        result.put("user",obtainPlayerInfo(roomNo,account));
        for (String uuid : room.getPlayerMap().keySet()) {
            result.put("myIndex",room.getPlayerMap().get(uuid).getMyIndex());
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(), String.valueOf(result), "gameChangeSeatPush_SW");
        }
    }

    /**
     * 断线重连
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
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
            return;
        }
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_SW");
    }

    /**
     * 获取走势图
     * @param client
     * @param data
     */
    public void getHistory(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject result = new JSONObject();
        result.put("array",room.getHistoryResult());
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getHistoryPush_SW");
    }

    /**
     * 获取玩家列表
     * @param client
     * @param data
     */
    public void getAllUser(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int pageIndex = postData.getInt("pageIndex");
        JSONObject result = new JSONObject();
        JSONArray array = new JSONArray();
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("name",room.getPlayerMap().get(account).getName());
            obj.put("score",room.getPlayerMap().get(account).getScore());
            obj.put("headimg",room.getPlayerMap().get(account).getRealHeadimg());
            obj.put("isBanker",CommonConstant.GLOBAL_NO);
            if (account.equals(room.getBanker())) {
                obj.put("isBanker",CommonConstant.GLOBAL_YES);
            }
            array.add(obj);
        }
        JSONArray users = new JSONArray();
        // 开始
        int beginIndex = pageIndex*SwConstant.SW_USER_SIZE_PER_PAGE;
        // 结束
        int endIndex = (pageIndex+1)*SwConstant.SW_USER_SIZE_PER_PAGE;
        if (array.size()>endIndex) {
            for (int i = beginIndex; i < endIndex; i++) {
                users.add(array.getJSONObject(i));
            }
        }else if (array.size()>beginIndex) {
            // 不足一页
            for (int i = beginIndex; i < array.size(); i++) {
                users.add(array.getJSONObject(i));
            }
        }
        result.put("array",users);
        result.put("pageIndex",pageIndex);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAllUsersPush_SW");
    }

    /**
     * 下注完成
     * @param roomNo
     */
    public void betFinish(final String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()!=SwConstant.SW_GAME_STATUS_BET) {
            return;
        }
        room.setGameStatus(SwConstant.SW_GAME_STATUS_SHOW);
        // 是否抽水
        if (room.getFee() > 0) {
            JSONArray array = new JSONArray();
            for (String account : room.getPlayerMap().keySet()) {
                // 中途加入不抽水
                if (!Dto.isObjNull(obtainLastBetRecord(roomNo,account))||account.equals(room.getBanker())) {
                    changeUserScore(roomNo,account,-room.getFee());
                    array.add(room.getPlayerMap().get(account).getId());
                }
            }
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
        // 设置倒计时
        room.setTimeLeft(SwConstant.SW_TIME_SHOW);
        changeGameStatus(roomNo);
        // 开始展示押宝倒计时
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerSw.gameOverTime(roomNo, SwConstant.SW_TIME_SHOW);
            }
        });
    }

    /**
     * 结算
     * @param roomNo
     */
    public void summary(final String roomNo) {
        // 防止重复结算
        String summaryTimesKey = "summaryTimes_sw_"+roomNo;
        long summaryTimes = redisService.incr(summaryTimesKey,1);
        if (summaryTimes>1) {
            return;
        }
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()!=SwConstant.SW_GAME_STATUS_SHOW) {
            return;
        }
        room.setGameStatus(SwConstant.SW_GAME_STATUS_SUMMARY);
        double bankerSum = 0;
        for (String account : room.getPlayerMap().keySet()) {
            // 所有非庄家玩家结算
            if (!account.equals(room.getBanker())) {
                // 押宝下注金额
                int winBetNum = obtainTotalBetByAccountAndPlace(roomNo,account,room.getTreasure());
                // 总下注金额
                int totalBetNum = obtainTotalBetByAccount(roomNo,account);
                // 当局输赢=(赢*赔率-总下注)*底分
                double mySum = (winBetNum * room.getRatio() - totalBetNum) * room.getScore();
                bankerSum -= mySum;
                // 改变玩家积分
                changeUserScore(roomNo,account,winBetNum*room.getRatio()*room.getScore());
                // 添加结算记录
                JSONObject myResult = new JSONObject();
                myResult.put("account", account);
                myResult.put("index", room.getPlayerMap().get(account).getMyIndex());
                myResult.put("score", mySum);
                room.getSummaryArray().add(myResult);
            }
        }
        changeUserScore(roomNo,room.getBanker(),bankerSum);
        // 添加结算记录
        JSONObject bankerResult = new JSONObject();
        bankerResult.put("account",room.getBanker());
        bankerResult.put("index", room.getPlayerMap().get(room.getBanker()).getMyIndex());
        bankerResult.put("score", bankerSum);
        room.getSummaryArray().add(bankerResult);
        // 更新玩家分数
        updateUserScore(roomNo);
        // 添加战绩
        addUserGameLog(roomNo,room.getTreasure());
        // 添加输赢记录
        saveUserDeduction(roomNo);
        // 添加走势图记录
        addHistoryTreasure(roomNo,room.getTreasure());
        // 通知玩家
        changeGameStatus(roomNo);
        // 分数不够重置庄家
        if (room.getPlayerMap().get(room.getBanker()).getScore()<room.getMinBankerScore()) {
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerSw.gameOverTime(roomNo, SwConstant.SW_TIME_SUMMARY_ANIMATION);
                }
            });
        }
    }

    /**
     * 更新玩家分数
     * @param roomNo
     */
    public void updateUserScore(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (int i = 0; i < room.getSummaryArray().size(); i++) {
            String account = room.getSummaryArray().getJSONObject(i).getString("account");
            JSONObject obj = new JSONObject();
            obj.put("id", room.getPlayerMap().get(account).getId());
            obj.put("total", room.getPlayerMap().get(account).getScore());
            obj.put("fen", room.getSummaryArray().getJSONObject(i).getDouble("score"));
            array.add(obj);
        }
        if (array.size()>0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
        }
    }

    /**
     * 玩家输赢记录
     * @param roomNo
     */
    public void saveUserDeduction(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray userDeductionData = new JSONArray();
        for (int i = 0; i < room.getSummaryArray().size(); i++) {
            String account = room.getSummaryArray().getJSONObject(i).getString("account");
            JSONObject object = new JSONObject();
            object.put("id", room.getPlayerMap().get(account).getId());
            object.put("gid", room.getGid());
            object.put("roomNo", room.getRoomNo());
            object.put("type", room.getRoomType());
            object.put("fen", room.getSummaryArray().getJSONObject(i).getDouble("score"));
            object.put("old", Dto.sub(room.getPlayerMap().get(account).getScore(),object.getDouble("fen")));
            if (room.getPlayerMap().get(account).getScore()<0) {
                object.put("new", 0);
            }else {
                object.put("new", room.getPlayerMap().get(account).getScore());
            }
            userDeductionData.add(object);
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
    }

    /**
     * 保存玩家战绩
     * @param roomNo
     * @param treasure
     */
    public void addUserGameLog(String roomNo,int treasure) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        for (String account : room.getPlayerMap().keySet()) {
            // 有参与的玩家
            if (!Dto.isObjNull(obtainLastBetRecord(roomNo,account))||account.equals(room.getBanker())) {
                JSONObject obj = new JSONObject();
                obj.put("treasure",treasure);
                if (account.equals(room.getBanker())) {
                    obj.put("totalBet",obtainTotalBet(roomNo)*room.getScore());
                    obj.put("winBet",obtainTotalBetByPlace(roomNo,treasure)*room.getScore());
                }else {
                    obj.put("totalBet",obtainTotalBetByAccount(roomNo,account)*room.getScore());
                    obj.put("winBet",obtainTotalBetByAccountAndPlace(roomNo,account,treasure)*room.getScore());
                }
                obj.put("sum",obtainPlayerScoreByIndex(roomNo,account));
                obj.put("id",room.getPlayerMap().get(account).getId());
                array.add(obj);
            }
        }
        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(String.valueOf(array), String.valueOf(room.getBetArray()));
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        for (int i = 0; i < array.size(); i++) {
            JSONArray result = new JSONArray();
            result.add(new JSONObject().element("player","藏宝人").element("score",room.getPlayerMap().get(room.getBanker()).getName()).element("banker",room.getBanker()));
            result.add(new JSONObject().element("player","藏宝").element("score",obtainTreasureName(array.getJSONObject(i).getInt("treasure"))));
            result.add(new JSONObject().element("player","赔率").element("score",room.getRatio()));
            result.add(new JSONObject().element("player","总注").element("score",array.getJSONObject(i).getDouble("totalBet")));
            result.add(new JSONObject().element("player","押中").element("score",array.getJSONObject(i).getDouble("winBet")));
            result.add(new JSONObject().element("player","输赢").element("score",array.getJSONObject(i).getDouble("sum")));
            JSONObject userGameLog = new JSONObject();
            userGameLog.put("gid", room.getGid());
            userGameLog.put("room_id", room.getId());
            userGameLog.put("room_no", roomNo);
            userGameLog.put("game_index", room.getGameIndex());
            userGameLog.put("user_id", array.getJSONObject(i).getLong("id"));
            userGameLog.put("gamelog_id", gameLogObj.getLong("id"));
            userGameLog.put("result", result);
            userGameLog.put("createtime", TimeUtil.getNowDate());
            userGameLog.put("account", array.getJSONObject(i).getDouble("sum"));
            userGameLog.put("fee", room.getFee());
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLog));
        }
    }

    /**
     * 重置庄家
     * @param roomNo
     */
    public void choiceBanker(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 更换庄家位置
        if (!Dto.stringIsNULL(room.getBanker())) {
            room.getPlayerMap().get(room.getBanker()).setMyIndex(room.getLastIndex());
            room.setLastIndex(room.getLastIndex()+1);
            // 清空庄家
            room.setBanker(null);
        }
        room.getBetArray().clear();
        room.getSummaryArray().clear();
        room.setGameStatus(SwConstant.SW_GAME_STATUS_CHOICE_BANKER);
        changeGameStatus(roomNo);
    }

    /**
     * 添加下注记录
     * @param roomNo
     * @param account
     * @param place
     * @param value
     */
    public void addBetRecord(String roomNo, String account, int place, int value) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject betRecord = new JSONObject();
        betRecord.put("account",account);
        betRecord.put("index",room.getPlayerMap().get(account).getMyIndex());
        betRecord.put("place",place);
        betRecord.put("value",value);
        room.getBetArray().add(betRecord);
    }

    /**
     * 移除下注记录
     * @param roomNo
     * @param account
     */
    public void removeBetRecord(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();
        for (int i = betArray.size() - 1; i >= 0; i--) {
            if (betArray.getJSONObject(i).getString("account").equals(account)) {
                betArray.remove(i);
                break;
            }
        }
    }

    /**
     * 添加走势图记录
     * @param roomNo
     * @param treasure
     */
    public void addHistoryTreasure(String roomNo, int treasure) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        room.getHistoryResult().add(0,treasure);
        // 超出指定长度后移除超出部分
        if (room.getHistoryResult().size()>SwConstant.SW_HISTORY_TREASURE_SIZE) {
            for (int i = 0; i < room.getHistoryResult().size(); i++) {
                if (i>=SwConstant.SW_HISTORY_TREASURE_SIZE) {
                    room.getHistoryResult().remove(i);
                }
            }
        }
    }

    /**
     * 改变玩家分数
     * @param roomNo
     * @param account
     * @param score
     */
    public void changeUserScore(String roomNo, String account, double score) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(account)&&room.getPlayerMap().containsKey(account)&&
            room.getPlayerMap().get(account)!=null) {
            double oldScore = room.getPlayerMap().get(account).getScore();
            double newScore = Dto.add(oldScore,score);
            room.getPlayerMap().get(account).setScore(newScore);
        }
    }

    /**
     * 游戏状态改变通知玩家
     * @param roomNo
     */
    public void changeGameStatus(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("gameStatus", room.getGameStatus());
            obj.put("game_index", room.getGameIndex());
            obj.put("treasure", obtainTreasure(roomNo));
            obj.put("showTimer", CommonConstant.GLOBAL_NO);
            if (room.getTimeLeft()>0) {
                obj.put("showTimer", CommonConstant.GLOBAL_YES);
                obj.put("time", room.getTimeLeft());
            }
            obj.put("users", obtainAllPlayer(roomNo,account));
            obj.put("myScore", obtainMyScore(roomNo,account));
            obj.put("totalScore", obtainTotalScore(roomNo));
            obj.put("baseNum", room.getBaseNum());
            obj.put("bankerIndex", obtainBankerIndex(roomNo));
            obj.put("bankerBtn", obtainBankerBtnStatus(roomNo,account));
            obj.put("bankerScore", room.getMinBankerScore());
            obj.put("summaryData", obtainSummaryData(roomNo,account));
            obj.put("winArray", obtainWinIndex(roomNo));
            obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
            UUID uuid = room.getPlayerMap().get(account).getUuid();
            // 通知玩家
            if (uuid != null) {
                CommonConstant.sendMsgEventToSingle(uuid, String.valueOf(obj), "changeGameStatusPush_SW");
            }
        }
    }

    /**
     * 获取单个玩家所有子下注情况
     * @param roomNo
     * @param account
     * @return
     */
    public JSONArray obtainMyScore(String roomNo, String account) {
        JSONArray array = new JSONArray();
        for (int i = 1; i <= 12; i++) {
            JSONObject obj = new JSONObject();
            obj.put("place",i);
            obj.put("score",obtainTotalBetByAccountAndPlace(roomNo,account,i));
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取各个位置下注情况
     * @param roomNo
     * @return
     */
    public JSONArray obtainTotalScore(String roomNo) {
        JSONArray array = new JSONArray();
        for (int i = 1; i <= 12; i++) {
            JSONObject obj = new JSONObject();
            obj.put("place",i);
            obj.put("score",obtainTotalBetByPlace(roomNo,i));
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取玩家信息
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainPlayerInfo(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
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
        return obj;
    }

    /**
     * 获取房间信息
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainRoomData(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("gameStatus",room.getGameStatus());
        obj.put("room_no",roomNo);
        obj.put("roomType",room.getRoomType());
        obj.put("game_count",room.getGameCount());
        obj.put("myIndex",room.getPlayerMap().get(account).getMyIndex());
        obj.put("playerCount",room.getPlayerMap().size());
        obj.put("di",room.getScore());
        StringBuffer roomInfo = new StringBuffer();
        roomInfo.append(room.getWfType());
        roomInfo.append(" 1赔");
        roomInfo.append(room.getRatio());
        roomInfo.append(" ");
        roomInfo.append(room.getRoomNo());
        roomInfo.append("\n单子下注上限:");
        if (room.getSingleMax()==0) {
            roomInfo.append("不限");
        }else {
            roomInfo.append(room.getSingleMax());
        }
        roomInfo.append("\n入场:");
        roomInfo.append((int) room.getEnterScore());
        roomInfo.append(" 离场:");
        roomInfo.append((int) room.getLeaveScore());
        roomInfo.append("\n底注:");
        roomInfo.append((int) room.getScore());
        obj.put("roominfo",String.valueOf(roomInfo));
        obj.put("bankerIndex",obtainBankerIndex(roomNo));
        obj.put("bankerBtn", obtainBankerBtnStatus(roomNo,account));
        obj.put("myScore", obtainMyScore(roomNo,account));
        obj.put("totalScore", obtainTotalScore(roomNo));
        obj.put("bankerScore", room.getMinBankerScore());
        obj.put("game_index",room.getPlayerMap().get(account).getMyIndex());
        obj.put("treasure",obtainTreasure(roomNo));
        obj.put("showTimer", CommonConstant.GLOBAL_NO);
        if (room.getTimeLeft()>0) {
            obj.put("showTimer", CommonConstant.GLOBAL_YES);
            obj.put("time", room.getTimeLeft());
        }
        obj.put("users", obtainAllPlayer(roomNo,account));
        obj.put("baseNum", room.getBaseNum());
        obj.put("betResult", room.getBetArray());
        obj.put("summaryData", obtainSummaryData(roomNo,account));
        obj.put("playerCount",room.getPlayerMap().size());
        return obj;
    }

    /**
     * 获取结算数据
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainSummaryData(String roomNo, String account) {
        JSONObject summaryData = new JSONObject();
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()==SwConstant.SW_GAME_STATUS_SUMMARY) {
            summaryData.put("index",room.getPlayerMap().get(account).getMyIndex());
            // 没有下注未参与，有下注根据分数显示输赢
            if (!account.equals(room.getBanker())&&obtainTotalBetByAccount(roomNo,account)<=0) {
                summaryData.put("isWinner",SwConstant.SUMMARY_RESULT_NO_IN);
                summaryData.put("sum",0);
            }else {
                double score = obtainPlayerScoreByIndex(roomNo,account);
                summaryData.put("isWinner",SwConstant.SUMMARY_RESULT_LOSE);
                if (score>0) {
                    summaryData.put("isWinner",SwConstant.SUMMARY_RESULT_WIN);
                }
                summaryData.put("sum",score);
            }
        }
        return summaryData;
    }

    /**
     * 获取玩家输赢分数
     * @param roomNo
     * @param account
     * @return
     */
    public double obtainPlayerScoreByIndex(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray summaryArray = room.getSummaryArray();
        for (int i = 0; i < summaryArray.size(); i++) {
            // 下标相同返回对应的分数
            if (summaryArray.getJSONObject(i).getString("account").equals(account)) {
                return summaryArray.getJSONObject(i).getDouble("score");
            }
        }
        return 0;
    }

    /**
     * 获取上庄按钮是否可用
     * @param roomNo
     * @return
     */
    public int obtainBankerBtnStatus(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 庄家已确定且分数足够
        if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().containsKey(room.getBanker())&&
            room.getPlayerMap().get(room.getBanker())!=null) {
            if (room.getPlayerMap().get(room.getBanker()).getScore()>room.getMinBankerScore()) {
                return CommonConstant.GLOBAL_NO;
            }
        }
        if (!Dto.stringIsNULL(account)&&room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
            if (room.getPlayerMap().get(account).getScore()>room.getMinBankerScore()) {
                return CommonConstant.GLOBAL_YES;
            }
        }
        return CommonConstant.GLOBAL_NO;
    }

    /**
     * 获取庄家下标
     * @param roomNo
     * @return
     */
    public int obtainBankerIndex(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().containsKey(room.getBanker())&&
            room.getPlayerMap().get(room.getBanker())!=null&&room.getPlayerMap().get(room.getBanker()).getScore()>room.getMinBankerScore()) {
            return room.getPlayerMap().get(room.getBanker()).getMyIndex();
        }
        return CommonConstant.NO_BANKER_INDEX;
    }

    /**
     * 获取当局押宝
     * @param roomNo
     * @return
     */
    public int obtainTreasure(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()>=SwConstant.SW_GAME_STATUS_SHOW) {
            return room.getTreasure();
        }
        return 0;
    }


    /**
     * 获取当前房间内的所有玩家
     * @param roomNo
     * @param me
     * @return
     */
    public JSONArray obtainAllPlayer(String roomNo,String me){
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        for(String account : room.getPlayerMap().keySet()){
            Playerinfo player = room.getPlayerMap().get(account);
            if(player!=null){
                if (account.equals(me)||player.getMyIndex()<=SwConstant.SW_MAX_SEAT_NUM) {
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
                    array.add(obj);
                }
            }
        }
        return array;
    }

    /**
     * 获取单个玩家单子的下注金额
     * @param roomNo
     * @param account
     * @param place
     * @return
     */
    public int obtainTotalBetByAccountAndPlace(String roomNo, String account, int place) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();
        for (int i = 0; i < betArray.size(); i++) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getString("account").equals(account)&&betRecord.getInt("place")==place) {
                totalBet += betRecord.getInt("value");
            }
        }
        return totalBet;
    }

    /**
     * 获取单子的下注金额
     * @param roomNo
     * @param place
     * @return
     */
    public int obtainTotalBetByPlace(String roomNo, int place) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();
        for (int i = 0; i < betArray.size(); i++) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getInt("place")==place) {
                totalBet += betRecord.getInt("value");
            }
        }
        return totalBet;
    }

    /**
     * 获取总下注金额
     * @param roomNo
     * @return
     */
    public int obtainTotalBet(String roomNo) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();
        for (int i = 0; i < betArray.size(); i++) {
            totalBet += betArray.getJSONObject(i).getInt("value");
        }
        return totalBet;
    }

    /**
     * 获取单个玩家总计的下注金额
     * @param roomNo
     * @param account
     * @return
     */
    public int obtainTotalBetByAccount(String roomNo, String account) {
        int totalBet = 0;
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int index = room.getPlayerMap().get(account).getMyIndex();
        JSONArray betArray = room.getBetArray();
        for (int i = 0; i < betArray.size(); i++) {
            JSONObject betRecord = betArray.getJSONObject(i);
            if (betRecord.getString("account").equals(account)) {
                totalBet += betRecord.getInt("value");
            }
        }
        return totalBet;
    }

    /**
     * 获取最后一条下注记录
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainLastBetRecord(String roomNo, String account) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray betArray = room.getBetArray();
        int myIndex = room.getPlayerMap().get(account).getMyIndex();
        for (int i = betArray.size() - 1; i >= 0; i--) {
            if (betArray.getJSONObject(i).getString("account").equals(account)) {
                return betArray.getJSONObject(i);
            }
        }
        return null;
    }

    /**
     * 根据座位号获取相应的玩家
     * @param roomNo
     * @param index
     * @return
     */
    public String obtainUserAccountByIndex(String roomNo, int index) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getPlayerMap().get(account).getMyIndex()==index) {
                return account;
            }
        }
        return null;
    }

    /**
     * 获取最大倍数
     * @param array
     * @return
     */
    public int obtainMaxTimes(JSONArray array) {
        int maxTimes = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject baseNum = array.getJSONObject(i);
            if (baseNum.getInt("val")>maxTimes) {
                maxTimes = baseNum.getInt("val");
            }
        }
        return  maxTimes;
    }

    /**
     * 获取所有获胜玩家
     * @param roomNo
     * @return
     */
    public JSONArray obtainWinIndex(String roomNo) {
        SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray winArray = new JSONArray();
        if (room.getGameStatus()==SwConstant.SW_GAME_STATUS_SUMMARY) {
            JSONArray betArray = room.getBetArray();
            for (int i = betArray.size() - 1; i >= 0; i--) {
                if (betArray.getJSONObject(i).getInt("place")==room.getTreasure()&&!winArray.contains(betArray.getJSONObject(i).getInt("index"))) {
                    winArray.add(betArray.getJSONObject(i).getInt("index"));
                }
            }
        }
        return winArray;
    }

    /**
     * 获取藏宝名称
     * @param treasure
     * @return
     */
    public String obtainTreasureName(int treasure) {
        switch (treasure) {
            case SwConstant.TREASURE_BLACK_ROOK:
                return "車";
            case SwConstant.TREASURE_BLACK_KNIGHT:
                return "馬";
            case SwConstant.TREASURE_BLACK_CANNON:
                return "包";
            case SwConstant.TREASURE_BLACK_ELEPHANT:
                return "象";
            case SwConstant.TREASURE_BLACK_MANDARIN:
                return "士";
            case SwConstant.TREASURE_BLACK_KING:
                return "將";
            case SwConstant.TREASURE_RED_ROOK:
                return "俥";
            case SwConstant.TREASURE_RED_KNIGHT:
                return "傌";
            case SwConstant.TREASURE_RED_CANNON:
                return "炮";
            case SwConstant.TREASURE_RED_ELEPHANT:
                return "相";
            case SwConstant.TREASURE_RED_MANDARIN:
                return "仕";
            case SwConstant.TREASURE_RED_KING:
                return "帥";
            default:
                return null;
        }
    }
}
