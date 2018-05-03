package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSComputeCards;
import com.zhuoan.biz.core.sss.SSSOrdinaryCards;
import com.zhuoan.biz.core.sss.SSSSpecialCardSort;
import com.zhuoan.biz.core.sss.SSSSpecialCards;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.SSSConstant;
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
import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 15:10 2018/4/21
 * @Modified By:
 **/
@Component
public class SSSGameEventDealNew {

    public static int GAME_SSS = 1;

    private final static Logger logger = LoggerFactory.getLogger(SSSGameEventDealNew.class);

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private GameTimerSSS gameTimerSSS;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * 创建房间通知自己
     * @param client
     */
    public void createRoom(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject roomData = obtainRoomData(roomNo,account);
        // 数据不为空
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("data",roomData);
            // 通知自己
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_SSS");
        }
    }

    /**
     * 加入房间通知
     * @param client
     * @param data
     */
    public void joinRoom(SocketIOClient client, Object data){
        // 进入房间通知自己
        createRoom(client, data);
        JSONObject joinData = JSONObject.fromObject(data);
        // 非重连通知其他玩家
        if (joinData.containsKey("isReconnect")&&joinData.getInt("isReconnect")==0) {
            String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
            SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
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
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),obj.toString(),"playerEnterPush_SSS");
        }
    }

    public void gameReady(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData,SSSConstant.SSS_GAME_STATUS_INIT, client)&&
            !CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_READY, client)&&
            !CommonConstant.checkEvent(postData,SSSConstant.SSS_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (SSSGameEventDealNew.GAME_SSS==0) {
            postData.put("notSend",CommonConstant.GLOBAL_YES);
            exitRoom(client,postData);
            JSONObject result = new JSONObject();
            result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
            result.put(CommonConstant.RESULT_KEY_MSG,"即将停服进行更新");
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB){
            if (room.getPlayerMap().get(account).getScore()<room.getLeaveScore()) {
                postData.put("notSendToMe",CommonConstant.GLOBAL_YES);
                exitRoom(client,data);
                JSONObject result = new JSONObject();
                result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                result.put(CommonConstant.RESULT_KEY_MSG,"元宝不足");
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
                return;
            }
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus()!=SSSConstant.SSS_GAME_STATUS_READY) {
            room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
        }
        // 当前准备人数大于最低开始人数开始游戏
        if (room.getNowReadyCount()==room.getMinPlayer()) {
            room.setTimeLeft(SSSConstant.SSS_TIMER_READY);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerSSS.gameOverTime(roomNo,SSSConstant.SSS_GAME_STATUS_READY);
                }
            });
        }
        // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
        if (room.isAllReady()&&room.getUserPacketMap().size()>=room.getMinPlayer()) {
            startGame(room);
        }else {
            JSONObject result = new JSONObject();
            result.put("index",room.getPlayerMap().get(account).getMyIndex());
            result.put("showTimer",CommonConstant.GLOBAL_NO);
            if (room.getNowReadyCount()>=room.getMinPlayer()) {
                result.put("showTimer",CommonConstant.GLOBAL_YES);
            }
            result.put("timer",room.getTimeLeft());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"playerReadyPush_SSS");
        }
    }

    /**
     * 开始游戏
     * @param room
     */
    public void startGame(final SSSGameRoomNew room) {
        // 非准备或初始阶段无法开始开始游戏
        if (room.getGameStatus()!= SSSConstant.SSS_GAME_STATUS_READY) {
            return;
        }
        // 初始化房间信息
        room.initGame();
        // 洗牌
        room.shufflePai(room.getUserPacketMap().size(), room.getColor());
        // 发牌
        room.faPai();
        // 设置房间状态(配牌)
        room.setGameStatus(SSSConstant.SSS_GAME_STATUS_GAME_EVENT);
        // 设置玩家手牌
        JSONArray gameProcessFP = new JSONArray();
        for (String uuid : room.getUserPacketMap().keySet()) {
            // 存放游戏记录
            JSONObject userPai = new JSONObject();
            userPai.put("account", uuid);
            userPai.put("name", room.getPlayerMap().get(uuid).getName());
            userPai.put("pai", room.getUserPacketMap().get(uuid).getMyPai());
            gameProcessFP.add(userPai);
        }
        room.getGameProcess().put("faPai", gameProcessFP);
        if (room.getFee()>0) {
            JSONArray array = new JSONArray();
            for (String account : room.getPlayerMap().keySet()) {
                // 中途加入不抽水
                if (room.getUserPacketMap().get(account).getStatus()>SSSConstant.SSS_USER_STATUS_INIT) {
                    // 更新实体类数据
                    Playerinfo playerinfo = room.getPlayerMap().get(account);
                    room.getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(),room.getFee()));
                    // 负数清零
                    if (room.getPlayerMap().get(account).getScore()<0) {
                        room.getPlayerMap().get(account).setScore(0);
                    }
                    array.add(playerinfo.getId());
                }
            }
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
        // 设置倒计时时间
        room.setTimeLeft(SSSConstant.SSS_TIMER_GAME_EVENT);
        // 改变状态，通知玩家
        changeGameStatus(room);
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerSSS.gameOverTime(room.getRoomNo(),SSSConstant.SSS_GAME_STATUS_GAME_EVENT);
            }
        });
    }

    /**
     * 游戏事件
     * @param client
     * @param data
     */
    public void gameEvent(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,SSSConstant.SSS_GAME_STATUS_GAME_EVENT, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        Player player = room.getUserPacketMap().get(account);
        if (postData.containsKey(SSSConstant.SSS_DATA_KET_TYPE)) {
            // 配牌类型
            int type = postData.getInt(SSSConstant.SSS_DATA_KET_TYPE);
            switch (type) {
                case SSSConstant.SSS_GAME_ACTION_TYPE_AUTO:
                    if (room.getUserPacketMap().get(account).getPaiType()>0) {
                        int specialType = SSSSpecialCards.isSpecialCards(player.getPai(),room.getSetting());
                        String[] specialPai = SSSSpecialCardSort.CardSort(player.getPai(), specialType);
                        room.getUserPacketMap().get(account).setPai(specialPai);
                        // 设置特殊牌型分数
                        room.getUserPacketMap().get(account).setPaiScore(SSSSpecialCards.score(specialType,room.getSetting()));
                        // 设置玩家头中尾手牌
                        room.changePlayerPai(specialPai,account);
                    } else {
                        String[] auto = SSSOrdinaryCards.sort(player.getPai());
                        room.getUserPacketMap().get(account).setPai(auto);
                        room.changePlayerPai(auto,account);
                    }
                    break;
                case SSSConstant.SSS_GAME_ACTION_TYPE_COMMON:
                    JSONArray myPai = postData.getJSONArray(SSSConstant.SSS_DATA_KET_MY_PAI);
                    // 数据不正确直接返回
                    for (int i = 0; i < player.getMyPai().length; i++) {
                        if (!myPai.contains((player.getMyPai())[i])) {
                            return;
                        }
                    }
                    JSONArray actionResult = SSSComputeCards.judge(player.togetMyPai(myPai));
                    if ("倒水".equals(actionResult.get(0))) {
                        String[] best = SSSOrdinaryCards.sort(player.getPai());
                        player.setPai(best);
                    }else{
                        String[] str = new String[13];
                        for (int i = 0; i < myPai.size(); i++) {
                            if (myPai.getInt(i)<20) {
                                String a="1-"+myPai.getString(i);
                                str[i]=a;
                            }else if(myPai.getInt(i)<40){
                                String a="2-"+(myPai.getInt(i)-20);
                                str[i]=a;
                            }else if(myPai.getInt(i)<60){
                                String a="3-"+(myPai.getInt(i)-40);
                                str[i]=a;
                            }else {
                                String a="4-"+(myPai.getInt(i)-60);
                                str[i]=a;
                            }
                        }
                        // 头道排序
                        str= SSSGameRoomNew.AppointSort(str, 0, 2);
                        // 中道排序
                        str=SSSGameRoomNew.AppointSort(str, 3, 7);
                        // 尾道排序
                        str=SSSGameRoomNew.AppointSort(str, 8, 12);
                        // 设置玩家手牌
                        room.getUserPacketMap().get(account).setPai(str);
                        // 设置玩家头中尾手牌
                        room.changePlayerPai(str,account);
                        // 设置玩家牌型
                        room.getUserPacketMap().get(account).setPaiType(0);
                    }
                    break;
                case SSSConstant.SSS_GAME_ACTION_TYPE_SPECIAL:
                    int specialType = SSSSpecialCards.isSpecialCards(player.getPai(),room.getSetting());
                    String[] specialPai = SSSSpecialCardSort.CardSort(player.getPai(), specialType);
                    room.getUserPacketMap().get(account).setPai(specialPai);
                    // 设置特殊牌型分数
                    room.getUserPacketMap().get(account).setPaiScore(SSSSpecialCards.score(specialType,room.getSetting()));
                    // 设置玩家头中尾手牌
                    room.changePlayerPai(specialPai,account);
                    break;
                default:
                    break;
            }
            room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_GAME_EVENT);
            // 所有人都完成操作切换游戏阶段
            if (room.isAllFinish()) {
                // 设置为比牌状态
                room.setGameStatus(SSSConstant.SSS_GAME_STATUS_COMPARE);
                // 根据庄家类型进行结算
                switch (room.getBankerType()) {
                    case SSSConstant.SSS_BANKER_TYPE_HB:
                        gameSummaryHb(roomNo);
                        break;
                    case SSSConstant.SSS_BANKER_TYPE_BWZ:
                        break;
                    default:
                        break;
                }
                // 改变状态通知玩家
                changeGameStatus(room);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        int  compareTime = 20;
                        compareTime += room.obtainNotSpecialCount()*3*7;
                        compareTime += room.getDqArray().size()*11;
                        compareTime += room.getSwat()*38;
                        room.setCompareTimer(compareTime);
                        for (int i = 1; i <= compareTime; i++) {
                            try {
                                room.setCompareTimer(i);
                                Thread.sleep(100);
                                if (i==compareTime) {
                                    // 更新玩家余额
                                    for (String account : room.getUserPacketMap().keySet()) {
                                        if (room.getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                                            double sum = room.getUserPacketMap().get(account).getScore();
                                            double oldScore = room.getPlayerMap().get(account).getScore();
                                            room.getPlayerMap().get(account).setScore(Dto.add(sum,oldScore));
                                        }
                                    }
                                    room.setGameStatus(SSSConstant.SSS_GAME_STATUS_SUMMARY);
                                    // 初始化倒计时
                                    room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                                    // 改变状态，通知玩家
                                    changeGameStatus(room);
                                }
                            } catch (InterruptedException e) {
                                logger.error("",e);
                            }
                        }
                    }
                });
                JSONArray array = new JSONArray();
                JSONArray userDeductionData = new JSONArray();
                JSONArray gameLogResults = new JSONArray();
                JSONArray gameResult = new JSONArray();
                // 存放游戏记录
                JSONArray gameProcessJS = new JSONArray();
                for (String uuid : room.getUserPacketMap().keySet()) {
                    // 有参与的玩家
                    if (room.getUserPacketMap().get(uuid).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
                        JSONObject userJS = new JSONObject();
                        userJS.put("account", uuid);
                        userJS.put("name", room.getPlayerMap().get(uuid).getName());
                        userJS.put("sum", room.getUserPacketMap().get(uuid).getScore());
                        userJS.put("pai", room.getUserPacketMap().get(uuid).getMyPai());
                        userJS.put("paiType", room.getUserPacketMap().get(uuid).getPaiType());
                        gameProcessJS.add(userJS);
                        // 元宝输赢情况
                        JSONObject obj = new JSONObject();
                        obj.put("total", room.getPlayerMap().get(uuid).getScore());
                        obj.put("fen", room.getUserPacketMap().get(uuid).getScore());
                        obj.put("id", room.getPlayerMap().get(uuid).getId());
                        array.add(obj);
                        // 用户游戏记录
                        JSONObject object = new JSONObject();
                        object.put("id", room.getPlayerMap().get(uuid).getId());
                        object.put("gid", room.getGid());
                        object.put("roomNo", room.getRoomNo());
                        object.put("type", 4);
                        object.put("fen", room.getUserPacketMap().get(uuid).getScore());
                        userDeductionData.add(object);
                        // 战绩记录
                        JSONObject gameLogResult = new JSONObject();
                        gameLogResult.put("account", uuid);
                        gameLogResult.put("name", room.getPlayerMap().get(uuid).getName());
                        gameLogResult.put("headimg", room.getPlayerMap().get(uuid).getHeadimg());
                        if (room.getPlayerMap().get(room.getBanker())==null) {
                            gameLogResult.put("zhuang", -1);
                        }else {
                            gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                        }
                        gameLogResult.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
                        gameLogResult.put("myPai", room.getUserPacketMap().get(uuid).getMyPai());
                        gameLogResult.put("score", room.getUserPacketMap().get(uuid).getScore());
                        gameLogResult.put("totalScore", room.getPlayerMap().get(uuid).getScore());
                        gameLogResult.put("win", 1);
                        if (room.getUserPacketMap().get(uuid).getStatus() < 0) {
                            gameLogResult.put("win", 0);
                        }
                        gameLogResults.add(gameLogResult);
                        // 用户战绩
                        JSONObject userResult = new JSONObject();
                        userResult.put("zhuang", room.getBanker());
                        userResult.put("isWinner", CommonConstant.GLOBAL_NO);
                        if (room.getUserPacketMap().get(uuid).getScore() > 0) {
                            userResult.put("isWinner", CommonConstant.GLOBAL_YES);
                        }
                        userResult.put("score", room.getUserPacketMap().get(uuid).getScore());
                        userResult.put("totalScore", room.getPlayerMap().get(uuid).getScore());
                        userResult.put("player", room.getPlayerMap().get(uuid).getName());
                        gameResult.add(userResult);
                    }
                }
                room.getGameProcess().put("JieSuan", gameProcessJS);
                if (room.getId()==0) {
                    JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
                    if (!Dto.isObjNull(roomInfo)) {
                        room.setId(roomInfo.getLong("id"));
                    }
                }
                // 更新玩家分数
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
                // 玩家输赢记录
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
                // 战绩信息
                JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
                JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
                for (int i = 0; i < userGameLogs.size(); i++) {
                    producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
                }
            }else {
                JSONObject result = new JSONObject();
                result.put("index",room.getPlayerMap().get(account).getMyIndex());
                result.put("showTimer",CommonConstant.GLOBAL_NO);
                if (room.getTimeLeft()>SSSConstant.SSS_TIMER_INIT) {
                    result.put("showTimer",CommonConstant.GLOBAL_YES);
                }
                result.put("timer",room.getTimeLeft());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"gameActionPush_SSS");
            }
        }
    }

    /**
     * 退出房间
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
            boolean canExit = false;
            // 金币场、元宝场
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_JB||room.getRoomType()==CommonConstant.ROOM_TYPE_YB) {
                // 未参与游戏可以自由退出
                if (room.getUserPacketMap().get(account).getStatus()== SSSConstant.SSS_USER_STATUS_INIT) {
                    canExit = true;
                }else if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_INIT||
                    room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_READY||
                    room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_SUMMARY) {// 初始及准备阶段可以退出
                    canExit = true;
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no",room.getRoomNo());
                roomInfo.put("user_id"+room.getPlayerMap().get(account).getMyIndex(),0);
                // 移除数据
                for (int i = 0; i < room.getUserIdList().size(); i++) {
                    if (room.getUserIdList().get(i)==room.getPlayerMap().get(account).getId()) {
                        room.getUserIdList().set(i, (long)0);
                        break;
                    }
                }
                room.getPlayerMap().remove(account);
                room.getUserPacketMap().remove(account);
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                result.put("type",1);
                result.put("index",player.getMyIndex());
                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_READY&&room.getNowReadyCount()<room.getMinPlayer()) {
                    // 重置房间倒计时
                    room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                }
                if (room.getTimeLeft()>0&&room.getGameStatus()!=SSSConstant.SSS_GAME_STATUS_COMPARE) {
                    result.put("showTimer",CommonConstant.GLOBAL_YES);
                }else {
                    result.put("showTimer",CommonConstant.GLOBAL_NO);
                }
                result.put("timer",room.getTimeLeft());
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList,result.toString(),"exitRoomPush_SSS");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_SSS");
                }
                // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏
                if (room.isAllReady()&&room.getPlayerMap().size()>=room.getMinPlayer()) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size()==0) {
                    roomInfo.put("status",-1);
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                roomBiz.updateGameRoom(roomInfo);
            }else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG,"游戏中无法退出");
                result.put("showTimer",CommonConstant.GLOBAL_YES);
                result.put("timer",room.getTimeLeft());
                result.put("type",1);
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"exitRoomPush_SSS");
            }
        }
    }

    /**
     * 重连
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client,Object data){
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
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_SSS");
            return;
        }
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (!room.getPlayerMap().containsKey(account)||room.getPlayerMap().get(account)==null) {
            result.put("type",CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_SSS");
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
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_SSS");
    }

    /**
     * 游戏超时事件
     * @param data
     */
    public void gameOvertime(Object data){
        JSONObject postData = JSONObject.fromObject(data);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        int gameStatus = postData.getInt("gameStatus");
        int userStatus = postData.getInt("userStatus");
        // 房间存在
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            SSSGameRoomNew room = (SSSGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
            // 非当前游戏状态停止定时器
            if (room.getGameStatus()!=gameStatus) {
                return;
            }
            // 准备状态需要检查当前准备人数是否大于最低开始人数
            if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY&&room.getNowReadyCount()<room.getMinPlayer()) {
                return;
            }
            // 当前阶段所有未完成操作的玩家
            List<String> autoAccountList = new ArrayList<String>();
            for (String account : room.getUserPacketMap().keySet()) {
                // 除准备阶段以外不需要判断中途加入的玩家
                if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY||room.getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                    if (room.getUserPacketMap().get(account).getStatus()!=userStatus) {
                        autoAccountList.add(account);
                    }
                }
            }
            for (String account : autoAccountList) {
                // 组织数据
                JSONObject obj = new JSONObject();
                // 房间号
                obj.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                // 账号
                obj.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY) {
                    // 准备阶段超时踢出
                    exitRoom(null,obj);
                }
                if (gameStatus==SSSConstant.SSS_GAME_STATUS_COMPARE) {
                    room.setGameStatus(SSSConstant.SSS_GAME_STATUS_SUMMARY);
                    // 初始化倒计时
                    room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                    // 改变状态，通知玩家
                    changeGameStatus(room);
                }
                if (gameStatus==SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
                    // 自动配牌
                    obj.put("type",1);
                    gameEvent(null,obj);
                }
            }
        }
    }

    /**
     * 游戏状态改变通知玩家
     * @param room
     */
    public void changeGameStatus(SSSGameRoomNew room) {
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("gameStatus",room.getGameStatus());
            obj.put("users",room.getAllPlayer());
            if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_BWZ) {
                obj.put("zhuang",room.getPlayerMap().get(room.getBanker()).getMyIndex());
            }else if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_HB) {
                obj.put("zhuang",-1);
            }
            obj.put("game_index",room.getGameIndex());
            obj.put("showTimer",CommonConstant.GLOBAL_NO);
            if (room.getTimeLeft()>SSSConstant.SSS_TIMER_INIT) {
                obj.put("showTimer",CommonConstant.GLOBAL_YES);
            }
            if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_COMPARE) {
                obj.put("bipaiTimer",0);
                obj.put("showTimer",CommonConstant.GLOBAL_NO);
            }
            obj.put("timer",room.getTimeLeft());
            // TODO: 2018/4/22 马牌
            obj.put("mapai",0);
            obj.put("isma",0);
            obj.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            obj.put("myPaiType",room.getUserPacketMap().get(account).getPaiType());
            obj.put("gameData",room.obtainGameData());
            // TODO: 2018/4/18 总结算数据
            obj.put("jiesuanData","");
            UUID uuid = room.getPlayerMap().get(account).getUuid();
            if (uuid!=null) {
                CommonConstant.sendMsgEventToSingle(uuid,obj.toString(),"changeGameStatusPush_SSS");
            }
        }
    }

    /**
     * 获取房间信息
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainRoomData(String roomNo, String account){
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONObject roomData = new JSONObject();
        if (room!=null) {
            roomData.put("gameStatus",room.getGameStatus());
            roomData.put("room_no",room.getRoomNo());
            roomData.put("roomType",room.getRoomType());
            roomData.put("game_count",room.getGameCount());
            roomData.put("di",room.getScore());
            roomData.put("isma",room.getMaPaiType());
            roomData.put("mapai",room.getMaPai());
            if(room.getRoomType()== CommonConstant.ROOM_TYPE_YB){
                StringBuffer roomInfo = new StringBuffer();
                roomInfo.append("房号:");
                roomInfo.append(room.getRoomNo());
                roomInfo.append("/最低");
                roomInfo.append(room.getMinPlayer());
                roomInfo.append("人/");
                roomInfo.append(room.getWfType());
                roomData.put("roominfo", roomInfo.toString());
                StringBuffer roomInfo2 = new StringBuffer();
                roomInfo2.append("底注:");
                roomInfo2.append(room.getScore());
                roomInfo2.append("/入场");
                roomInfo2.append(room.getEnterScore());
                roomInfo2.append("离场/");
                roomInfo2.append(room.getLeaveScore());
                roomData.put("roominfo2", roomInfo2.toString());
            }
            roomData.put("zhuang",-1);
            if (room.getRoomType()!=CommonConstant.ROOM_TYPE_YB&&room.getPlayerMap().get(room.getBanker())!=null) {
                roomData.put("zhuang",room.getPlayerMap().get(room.getBanker()).getMyIndex());
            }
            roomData.put("game_index",room.getGameIndex());
            roomData.put("showTimer",CommonConstant.GLOBAL_NO);
            if (room.getTimeLeft()>SSSConstant.SSS_TIMER_INIT) {
                roomData.put("showTimer",CommonConstant.GLOBAL_YES);
            }
            if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_COMPARE) {
                roomData.put("showTimer",CommonConstant.GLOBAL_NO);
                roomData.put("bipaiTimer",room.getCompareTimer());
            }
            roomData.put("timer",room.getTimeLeft());
            roomData.put("myIndex",room.getPlayerMap().get(account).getMyIndex());
            roomData.put("users",room.getAllPlayer());
            roomData.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            roomData.put("myPaiType",room.getUserPacketMap().get(account).getPaiType());
            roomData.put("gameData",room.obtainGameData());
            roomData.put("jiesan","");
            roomData.put("jiesanData","");

        }
        return roomData;
    }

    /**
     * 互比结算
     * @param roomNo
     */
    public void gameSummaryHb(String roomNo){
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room!=null) {
            // 获取所有参与的玩家
            List<String> gameList = new ArrayList<String>();
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                    gameList.add(account);
                }
            }
            // 是否全垒打
            boolean isSwat = false;
            for (String account : gameList) {
                Player player = room.getUserPacketMap().get(account);
                // 赢几个人人
                int winPlayer = 0;
                int special = player.getPaiType();
                if (special>0) {
                    break;
                }
                for (String other : gameList) {
                    if (!other.equals(account)) {
                        Player otherPlayer = room.getUserPacketMap().get(other);
                        int otherSpecial = player.getPaiType();
                        if (otherSpecial>0) {
                            break;
                        }else {
                            // 比牌结果
                            JSONObject compareResult = SSSComputeCards.compare(player.getPai(), otherPlayer.getPai());
                            // 自己的牌型及分数
                            JSONArray mySingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
                            // 其他玩家的牌型及分数
                            JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
                            int winTime = 0;
                            for (int i = 0; i < mySingleResult.size(); i++) {
                                if (mySingleResult.getJSONObject(i).getInt("score")>otherSingleResult.getJSONObject(i).getInt("score")) {
                                    winTime ++;
                                }
                            }
                            // 三道全赢打枪
                            if (winTime==3) {
                                winPlayer++;
                            }else {
                                break;
                            }
                        }
                    }
                }
                if (gameList.size()>=SSSConstant.SSS_MIN_SWAT_COUNT&&winPlayer==gameList.size()-1) {
                    room.setSwat(1);
                    isSwat = true;
                    room.getUserPacketMap().get(account).setSwat(1);
                    break;
                }
            }
            for (String account : gameList) {
                // 当局总计输赢
                int sumScoreAll = 0;
                // 自己
                Player player = room.getUserPacketMap().get(account);
                // 自己是否是特殊牌
                int special = player.getPaiType();
                // 单局结果
                JSONArray myResult = new JSONArray();
                // 头道
                myResult.add(new JSONObject().element("pai",player.getHeadPai()).element("score",0).element("type",0));
                // 中道
                myResult.add(new JSONObject().element("pai",player.getMidPai()).element("score",0).element("type",0));
                // 尾道
                myResult.add(new JSONObject().element("pai",player.getFootPai()).element("score",0).element("type",0));
                List<Integer> allWinList = new ArrayList<Integer>();
                for (String other : gameList) {
                    // 单个玩家输赢
                    int sumScoreSingle = 0;
                    if (!other.equals(account)) {
                        // 其他玩家
                        Player otherPlayer = room.getUserPacketMap().get(other);
                        int otherSpecial = otherPlayer.getPaiType();
                        if (special>otherSpecial) {
                            sumScoreSingle += player.getPaiScore();
                        } else if (special<otherSpecial) {
                            sumScoreSingle -= otherPlayer.getPaiScore();
                        } else if (special==otherSpecial&&special>0) {
                            // TODO: 2018/4/27 同为特殊牌且牌型相同
                        } else {
                            // 比牌结果
                            JSONObject compareResult = SSSComputeCards.compare(player.getPai(), otherPlayer.getPai());
                            // 自己的牌型及分数
                            JSONArray mySingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
                            // 其他玩家的牌型及分数
                            JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
                            int winTime = 0;
                            for (int i = 0; i < myResult.size(); i++) {
                                // 增加相应的分数
                                myResult.getJSONObject(i).put("score",myResult.getJSONObject(i).getInt("score")+mySingleResult.getJSONObject(i).getInt("score"));
                                // 设置没道对应的牌型
                                myResult.getJSONObject(i).put("type",mySingleResult.getJSONObject(i).getInt("type"));
                                if (mySingleResult.getJSONObject(i).getInt("score")>otherSingleResult.getJSONObject(i).getInt("score")) {
                                    winTime ++;
                                } else if (mySingleResult.getJSONObject(i).getInt("score")<otherSingleResult.getJSONObject(i).getInt("score")){
                                    winTime --;
                                }
                            }
                            sumScoreSingle = compareResult.getInt("A");
                            // 三道全赢打枪
                            if (winTime==3) {
                                sumScoreSingle *= 2;
                                allWinList.add(room.getPlayerMap().get(other).getMyIndex());
                                JSONArray dq = new JSONArray();
                                dq.add(room.getPlayerMap().get(account).getMyIndex());
                                dq.add(room.getPlayerMap().get(other).getMyIndex());
                                if (!room.getDqArray().contains(dq)) {
                                    room.getDqArray().add(dq);
                                }
                            }
                            // 三道全输被打枪
                            if (winTime==-3) {
                                sumScoreSingle *= 2;
                                JSONArray dq = new JSONArray();
                                dq.add(room.getPlayerMap().get(other).getMyIndex());
                                dq.add(room.getPlayerMap().get(account).getMyIndex());
                                if (!room.getDqArray().contains(dq)) {
                                    room.getDqArray().add(dq);
                                }
                            }
                            // 全垒打只对全垒打的人翻倍
                            if (isSwat) {
                                // 自己或者他人全垒打
                                if (room.getUserPacketMap().get(account).getSwat()==1||room.getUserPacketMap().get(other).getSwat()==1) {
                                    sumScoreSingle *= 2;
                                }
                            }
                        }
                        sumScoreAll += sumScoreSingle;
                    }
                }
                // 设置玩家当局输赢
                room.getUserPacketMap().get(account).setScore(sumScoreAll*room.getScore());
                // 设置头道输赢情况
                room.getUserPacketMap().get(account).setHeadResult(myResult.getJSONObject(0));
                // 设置中道输赢情况
                room.getUserPacketMap().get(account).setMidResult(myResult.getJSONObject(1));
                // 设置尾道输赢情况
                room.getUserPacketMap().get(account).setFootResult(myResult.getJSONObject(2));
            }
        }
    }
}
