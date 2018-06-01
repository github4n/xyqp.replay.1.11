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
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.SSSConstant;
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
import java.util.Arrays;
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

    @Resource
    private RedisService redisService;

    @Resource
    private RobotEventDeal robotEventDeal;

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

    /**
     * 准备
     * @param client
     * @param data
     */
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
            result.put(CommonConstant.RESULT_KEY_MSG,"即将停服更新");
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB){
            if (room.getPlayerMap().get(account).getScore()<room.getLeaveScore()) {
                postData.put("notSend",CommonConstant.GLOBAL_YES);
                postData.put("notSendToMe",CommonConstant.GLOBAL_YES);
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
        room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus()!=SSSConstant.SSS_GAME_STATUS_READY) {
            room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
        }
        // 当前准备人数大于最低开始人数开始游戏
        if (room.getNowReadyCount()==room.getMinPlayer()) {
            final int readyTime;
            if (!Dto.isObjNull(room.getSetting())&&room.getSetting().containsKey("goldready")) {
                readyTime = room.getSetting().getInt("goldready");
            }else {
                readyTime = SSSConstant.SSS_TIMER_READY;
            }
            room.setTimeLeft(readyTime);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerSSS.gameOverTime(roomNo,SSSConstant.SSS_GAME_STATUS_READY,readyTime);
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
        redisService.insertKey("summaryTimes_sss"+room.getRoomNo(),"0",null);
        // 初始化房间信息
        room.initGame();
        if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_BWZ||room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_HB) {
            startGameCommon(room.getRoomNo());
        }else if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_ZZ) {
            // 设置房间状态
            room.setGameStatus(SSSConstant.SSS_GAME_STATUS_XZ);
            // 听通知玩家
            changeGameStatus(room);
            // 设置倒计时时间
            final int gameEventTime;
            if (!Dto.isObjNull(room.getSetting())&&room.getSetting().containsKey("XZTime")) {
                gameEventTime = room.getSetting().getInt("XZTime");
            }else {
                gameEventTime = SSSConstant.SSS_TIMER_GAME_XZ;
            }
            room.setTimeLeft(gameEventTime);
            // 改变状态，通知玩家
            changeGameStatus(room);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerSSS.gameOverTime(room.getRoomNo(),SSSConstant.SSS_GAME_STATUS_XZ,gameEventTime);
                }
            });
        }
        if (room.getFee()>0) {
            JSONArray array = new JSONArray();
            for (String account : room.getPlayerMap().keySet()) {
                // 中途加入不抽水
                if (room.getUserPacketMap().get(account).getStatus()>SSSConstant.SSS_USER_STATUS_INIT) {
                    // 更新实体类数据
                    Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account);
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(),room.getFee()));
                    // 负数清零
                    if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()<0) {
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                    }
                    array.add(playerinfo.getId());
                }
            }
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
        }
    }

    public void startGameCommon(String roomNo) {
        final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
        // 设置倒计时时间
        final int gameEventTime;
        if (!Dto.isObjNull(room.getSetting())&&room.getSetting().containsKey("goldpeipai")) {
            gameEventTime = room.getSetting().getInt("goldpeipai");
        }else {
            gameEventTime = SSSConstant.SSS_TIMER_GAME_EVENT;
        }
        room.setTimeLeft(gameEventTime);
        // 改变状态，通知玩家
        changeGameStatus(room);
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerSSS.gameOverTime(room.getRoomNo(),SSSConstant.SSS_GAME_STATUS_GAME_EVENT,gameEventTime);
            }
        });

    }


    /**
     * 下注
     * @param client
     * @param data
     */
    public void gameXiaZhu(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_XZ, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_READY) {
            return;
        }
        // 庄家不能下注
        if (account.equals(room.getBanker())) {
            return;
        }
        // 设置玩家下注状态
        room.getUserPacketMap().get(account).setStatus(SSSConstant.SSS_USER_STATUS_XZ);
        // 下注分数
        room.getUserPacketMap().get(account).setXzTimes(postData.getInt("money"));
        JSONObject result = new JSONObject();
        result.put("index",room.getPlayerMap().get(account).getMyIndex());
        result.put("value",room.getUserPacketMap().get(account).getXzTimes());
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(result),"gameXiaZhuPush_SSS");
        if (room.isAllXiaZhu()) {
            startGameCommon(roomNo);
        }
    }

    /**
     * 上庄
     * @param client
     * @param data
     */
    public void gameBeBanker(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
        // 设置游戏状态
        room.setGameStatus(SSSConstant.SSS_GAME_STATUS_READY);
        changeGameStatus(room);
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
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        Player player = room.getUserPacketMap().get(account);
        // 牌为空或已经亮过牌不作处理
        if (player.getPai()==null||player.getStatus()==SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
            return;
        }
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
                        auto = checkBestPai(auto);
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
                        best = checkBestPai(best);
                        player.setPai(best);
                        room.changePlayerPai(best,account);
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
                    room.getUserPacketMap().get(account).setPaiType(specialType);
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
                allFinishDeal(roomNo);
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
     * 所有人完成操作进入比牌阶段
     * @param roomNo
     */
    public void allFinishDeal(final String roomNo) {
        String summaryTimesKey = "summaryTimes_sss"+roomNo;
        long summaryTimes = redisService.incr(summaryTimesKey,1);
        if (summaryTimes>1) {
            return;
        }
        final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 设置为比牌状态
        room.setGameStatus(SSSConstant.SSS_GAME_STATUS_COMPARE);
        // 根据庄家类型进行结算
        switch (room.getBankerType()) {
            case SSSConstant.SSS_BANKER_TYPE_HB:
                gameSummaryHb(roomNo);
                break;
            case SSSConstant.SSS_BANKER_TYPE_BWZ:
                gameSummaryBwzOrZZ(roomNo);
                break;
            case SSSConstant.SSS_BANKER_TYPE_ZZ:
                gameSummaryBwzOrZZ(roomNo);
                break;
            default:
                break;
        }
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            room.setNeedFinalSummary(true);
            roomCardSummary(roomNo);
        }
        // 改变状态通知玩家
        changeGameStatus(room);
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                int  compareTime = SSSConstant.SSS_COMPARE_TIME_BASE;
                compareTime += room.obtainNotSpecialCount()*3*SSSConstant.SSS_COMPARE_TIME_SHOW;
                compareTime += room.getDqArray().size()*SSSConstant.SSS_COMPARE_TIME_DQ;
                compareTime += room.getSwat()*SSSConstant.SSS_COMPARE_TIME_SWAT;
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
                                    double oldScore = RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).getScore();
                                    double newScore = Dto.add(sum,oldScore);
                                    RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).setScore(newScore);
                                }
                            }
                            if (room.getGameStatus()!=SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
                                room.setGameStatus(SSSConstant.SSS_GAME_STATUS_SUMMARY);
                            }
                            // 初始化倒计时
                            room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                            // 更新数据库
                            updateUserScore(room);
                            if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB) {
                                saveUserDeduction(room);
                            }
                            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                                updateRoomCard(room.getRoomNo());
                            }
                            if (room.getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
                                saveGameLog(room);
                            }
                            // 改变状态，通知玩家
                            changeGameStatus(room);
                            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_SUMMARY&&room.getGameIndex()==room.getGameCount()) {
                                    room.setGameStatus(SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY);
                                }
                            }
                            // 坐庄模式元宝不足
                            if (room.getBankerType()== SSSConstant.SSS_BANKER_TYPE_ZZ) {
                                if (room.getPlayerMap().get(room.getBanker()).getScore()<room.getMinBankerScore()) {
                                    // 庄家设为空
                                    room.setBanker(null);
                                    // 设置游戏状态
                                    room.setGameStatus(SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER);
                                    // 初始化倒计时
                                    room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                                    // 重置玩家状态
                                    for (String uuid : room.getUserPacketMap().keySet()) {
                                        room.getUserPacketMap().get(uuid).setStatus(SSSConstant.SSS_TIMER_INIT);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("",e);
                    }
                }
            }
        });
    }


    /**
     * 房卡场结算
     * @param roomNo
     */
    public void roomCardSummary(String roomNo) {
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() == SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
                Player up = room.getUserPacketMap().get(account);
                // 胜利次数+1
                if (up.getScore()>0) {
                    up.setWinTimes(up.getWinTimes()+1);
                }
                // 全垒打次数+1
                if (up.getSwat()==CommonConstant.GLOBAL_YES) {
                    up.setSwatTimes(up.getSwatTimes()+1);
                }
                // 特殊牌次数+1
                if (up.getPaiType()>0) {
                    up.setSpecialTimes(up.getSpecialTimes()+1);
                }
                // 普通牌次数+1
                if (up.getPaiType()==0) {
                    up.setOrdinaryTimes(up.getOrdinaryTimes()+1);
                }
                for (int i = 0; i < room.getDqArray().size(); i++) {
                    JSONArray dq = room.getDqArray().getJSONArray(i);
                    // 打枪次数+1
                    if (dq.getInt(0)==room.getPlayerMap().get(account).getMyIndex()) {
                        up.setDqTimes(up.getDqTimes()+1);
                    }
                    // 被打枪次数+1
                    if (dq.getInt(1)==room.getPlayerMap().get(account).getMyIndex()) {
                        up.setBdqTimes(up.getBdqTimes()+1);
                    }
                }
            }
        }
    }


    /**
     * 最优牌同花
     * @param myPai
     * @return
     */
    public String[] checkBestPai(String[] myPai){
        JSONArray midPai = new JSONArray();
        for (int i = 3; i < 8; i++) {
            midPai.add(myPai[i]);
        }
        JSONArray footPai = new JSONArray();
        for (int i = 8; i < 13; i++) {
            footPai.add(myPai[i]);
        }
        int mid = SSSComputeCards.isSameFlower(midPai);
        int foot = SSSComputeCards.isSameFlower(footPai);
        if (mid==5&&foot==5) {
            int result = SSSComputeCards.compareSameFlower(midPai,footPai);
            if (result==1) {
                String[] pai = new String[myPai.length];
                for (int i = 0; i < 3; i++) {
                    pai[i] = myPai[i];
                }
                for (int i = 3; i < 8; i++) {
                    pai[i] = footPai.getString(i-3);
                }
                for (int i = 8; i < 13; i++) {
                    pai[i] = midPai.getString(i-8);
                }
                return pai;
            }
        }
        return myPai;
    }

    /**
     * 更新数据库
     * @param room
     */
    public void updateUserScore(SSSGameRoomNew room){
        JSONArray array = new JSONArray();
        // 存放游戏记录
        for (String uuid : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(uuid).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
                // 元宝输赢情况
                JSONObject obj = new JSONObject();
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                    obj.put("total", room.getPlayerMap().get(uuid).getScore());
                    obj.put("fen", room.getUserPacketMap().get(uuid).getScore());
                    obj.put("id", room.getPlayerMap().get(uuid).getId());
                    array.add(obj);
                }
            }
        }
        // 更新玩家分数
        if (array.size()>0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
        }
    }

    public void updateRoomCard(String roomNo) {
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        for (String account : room.getUserPacketMap().keySet()) {
            // 房主支付
            if (room.getPayType()==CommonConstant.PAY_TYPE_OWNER) {
                if (account.equals(room.getOwner())) {
                    // 参与第一局需要扣房卡
                    if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                        array.add(room.getPlayerMap().get(account).getId());
                        roomCardCount = room.getPlayerCount()*room.getSinglePayNum();
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
        if (array.size()>0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array,roomCardCount)));
        }
    }


    public void saveUserDeduction(SSSGameRoomNew room) {
        JSONArray userDeductionData = new JSONArray();
        for (String uuid : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(uuid).getStatus() > SSSConstant.SSS_USER_STATUS_INIT) {
                // 用户游戏记录
                JSONObject object = new JSONObject();
                object.put("id", room.getPlayerMap().get(uuid).getId());
                object.put("gid", room.getGid());
                object.put("roomNo", room.getRoomNo());
                object.put("type", room.getRoomType());
                object.put("fen", room.getUserPacketMap().get(uuid).getScore());
                object.put("old", Dto.sub(room.getPlayerMap().get(uuid).getScore(),room.getUserPacketMap().get(uuid).getScore()));
                if (room.getPlayerMap().get(uuid).getScore()<0) {
                    object.put("new", 0);
                }else {
                    object.put("new", room.getPlayerMap().get(uuid).getScore());
                }
                userDeductionData.add(object);
            }
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
    }

    public void saveGameLog(SSSGameRoomNew room) {
        JSONArray array = new JSONArray();
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
                userJS.put("old", Dto.sub(room.getPlayerMap().get(uuid).getScore(),room.getUserPacketMap().get(uuid).getScore()));
                if (room.getPlayerMap().get(uuid).getScore()<0) {
                    userJS.put("new", 0);
                }else {
                    userJS.put("new", room.getPlayerMap().get(uuid).getScore());
                }
                gameProcessJS.add(userJS);

                JSONObject obj = new JSONObject();
                obj.put("fen",room.getUserPacketMap().get(uuid).getScore());
                obj.put("id",room.getPlayerMap().get(uuid).getId());
                array.add(obj);
                // 战绩记录
                JSONObject gameLogResult = new JSONObject();
                gameLogResult.put("account", uuid);
                gameLogResult.put("name", room.getPlayerMap().get(uuid).getName());
                gameLogResult.put("headimg", room.getPlayerMap().get(uuid).getHeadimg());
                if (room.getPlayerMap().get(room.getBanker())==null) {
                    gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                }else {
                    gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                }
                gameLogResult.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
                gameLogResult.put("myPai", room.getUserPacketMap().get(uuid).getMyPai());
                gameLogResult.put("score", room.getUserPacketMap().get(uuid).getScore());
                gameLogResult.put("totalScore", room.getPlayerMap().get(uuid).getScore());
                gameLogResult.put("win", CommonConstant.GLOBAL_YES);
                if (room.getUserPacketMap().get(uuid).getScore() < 0) {
                    gameLogResult.put("win", CommonConstant.GLOBAL_NO);
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
        logger.info(room.getRoomNo()+"---"+String.valueOf(room.getGameProcess()));
        JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
        for (int j = 0; j < userGameLogs.size(); j++) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(j)));
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
        if (!Dto.stringIsNULL(account)&&room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
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
            }else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                // 房卡场没玩过可以退出
                if (room.getUserPacketMap().get(account).getPlayTimes()==0) {
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA||!room.getOwner().equals(account)) {
                        canExit = true;
                    }
                }
                if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
                    canExit = true;
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no",room.getRoomNo());
                if (room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                     roomInfo.put("user_id"+room.getPlayerMap().get(account).getMyIndex(),0);
                }
                // 移除数据
                for (int i = 0; i < room.getUserIdList().size(); i++) {
                    if (room.getUserIdList().get(i)==room.getPlayerMap().get(account).getId()) {
                        room.getUserIdList().set(i, 0L);
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
                if (room.getTimeLeft()>SSSConstant.SSS_TIMER_INIT&&room.getGameStatus()!=SSSConstant.SSS_GAME_STATUS_COMPARE) {
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
                // 坐庄模式
                if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ) {
                    // 房主退出且房间内有其他玩家
                    if (account.equals(room.getBanker())&&room.getUserPacketMap().size()>0) {
                        // 庄家设为空
                        room.setBanker(null);
                        // 设置游戏状态
                        room.setGameStatus(SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER);
                        // 初始化倒计时
                        room.setTimeLeft(SSSConstant.SSS_TIMER_INIT);
                        // 重置玩家状态
                        for (String uuid : room.getUserPacketMap().keySet()) {
                            room.getUserPacketMap().get(uuid).setStatus(SSSConstant.SSS_USER_STATUS_INIT);
                        }
                        changeGameStatus(room);
                        return;
                    }
                }
                // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏
                if (room.isAllReady()&&room.getPlayerMap().size()>=room.getMinPlayer()) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size()==0) {
                    redisService.deleteByKey("summaryTimes_sss"+room.getRoomNo());
                    roomInfo.put("status",-1);
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
                // 机器人退出
                if (room.isRobot()&&room.getRobotList().contains(account)) {
                    robotEventDeal.robotExit(account);
                }
            }else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG,"游戏中无法退出");
                result.put("showTimer",CommonConstant.GLOBAL_YES);
                if (room.getTimeLeft()==0) {
                    result.put("showTimer",CommonConstant.GLOBAL_NO);
                }
                result.put("timer",room.getTimeLeft());
                result.put("type",1);
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"exitRoomPush_SSS");
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
        final SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
                        gameTimerSSS.closeRoomOverTime(roomNo,room.getJieSanTime());
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
                    room.getUserPacketMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_SSS");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (room.isAgreeClose()) {
                    // 未玩完一局不需要强制结算
                    if (!room.isNeedFinalSummary()) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"解散房间成功");
                        CommonConstant.sendMsgEventToAll(uuidList,result.toString(),"tipMsgPush");
                        return;
                    }
                    room.setGameStatus(SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY);
                    changeGameStatus(room);
                } else {// 刷新数据
                    room.getUserPacketMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", room.getJieSanData());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_SSS");
                }
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
        if (Dto.stringIsNULL(account)||!room.getPlayerMap().containsKey(account)||room.getPlayerMap().get(account)==null) {
            result.put("type",CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_SSS");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type",1);
        result.put("data",obtainRoomData(roomNo,account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"reconnectGamePush_SSS");
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
            if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_BWZ||room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_ZZ) {
                if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().get(room.getBanker())!=null) {
                    obj.put("zhuang",room.getPlayerMap().get(room.getBanker()).getMyIndex());
                }else {
                    obj.put("zhuang",CommonConstant.NO_BANKER_INDEX);
                }
            }else if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_HB) {
                obj.put("zhuang",CommonConstant.NO_BANKER_INDEX);
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
            if (room.getMaPaiType()>0 && !Dto.stringIsNULL(room.getMaPai())) {
                String[] ma = room.getMaPai().split("-");
                obj.put("mapai",Integer.valueOf(ma[1])+20*(Integer.valueOf(ma[0])-1));
            }else {
                obj.put("mapai",0);
            }
            obj.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            obj.put("myPaiType",room.getUserPacketMap().get(account).getPaiType());
            obj.put("gameData",room.obtainGameData());
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                if (room.getGameStatus()== SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
                    obj.put("jiesuanData", room.obtainFinalSummaryData());
                }
                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_SUMMARY&&room.getGameIndex()==room.getGameCount()) {
                    obj.put("jiesuanData", room.obtainFinalSummaryData());
                }
            }
            // 坐庄模式
            obj.put("isBanker",CommonConstant.GLOBAL_NO);
            if (room.getBankerType()== SSSConstant.SSS_BANKER_TYPE_ZZ) {
                // 上庄阶段或结算阶段庄家分数不足
                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER||
                    (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_SUMMARY&&room.getPlayerMap().get(room.getBanker()).getScore()<room.getMinBankerScore())) {
                    obj.put("isBanker",CommonConstant.GLOBAL_YES);
                    obj.put("bankerMinScore",room.getMinBankerScore());
                    obj.put("bankerIsUse",CommonConstant.GLOBAL_YES);
                    if (room.getPlayerMap().get(account).getScore()<room.getMinBankerScore()) {
                        obj.put("bankerIsUse",CommonConstant.GLOBAL_NO);
                    }
                }
                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_XZ) {
                    obj.put("baseNum",room.getBaseNumTimes(room.getPlayerMap().get(account).getScore()));
                }
            }
            UUID uuid = room.getPlayerMap().get(account).getUuid();
            if (uuid!=null) {
                CommonConstant.sendMsgEventToSingle(uuid,obj.toString(),"changeGameStatusPush_SSS");
            }
        }
        if (room.isRobot()) {
            for (String robotAccount : room.getRobotList()) {
                int delayTime = RandomUtils.nextInt(3)+2;
                if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_SUMMARY) {
                    robotEventDeal.changeRobotActionDetail(robotAccount,SSSConstant.SSS_GAME_EVENT_READY,delayTime);
                }
                if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_GAME_EVENT) {
                    robotEventDeal.changeRobotActionDetail(robotAccount,SSSConstant.SSS_GAME_EVENT_EVENT,delayTime);
                }
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
            if (room.getMaPaiType()>0 && !Dto.stringIsNULL(room.getMaPai())) {
                String[] ma = room.getMaPai().split("-");
                roomData.put("mapai",Integer.valueOf(ma[1])+20*(Integer.valueOf(ma[0])-1));
            }else {
                roomData.put("mapai",0);
            }
            if(room.getRoomType()== CommonConstant.ROOM_TYPE_YB){
                StringBuffer roomInfo = new StringBuffer();
                roomInfo.append("房号:");
                roomInfo.append(room.getRoomNo());
                roomInfo.append(" 最低");
                roomInfo.append(room.getMinPlayer());
                roomInfo.append("人 ");
                roomInfo.append(room.getWfType());
                roomData.put("roominfo", roomInfo.toString());
                StringBuffer roomInfo2 = new StringBuffer();
                roomInfo2.append("底注:");
                roomInfo2.append((int) room.getScore());
                roomInfo2.append(" 进:");
                roomInfo2.append((int) room.getEnterScore());
                roomInfo2.append(" 出:");
                roomInfo2.append((int) room.getLeaveScore());
                roomData.put("roominfo2", roomInfo2.toString());
            }
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                StringBuffer roomInfo = new StringBuffer();
                roomInfo.append(room.getWfType());
                roomInfo.append(" ");
                roomInfo.append(room.getPlayerCount());
                roomInfo.append("人 ");
                roomInfo.append(room.getGameCount());
                roomInfo.append("局 ");
                if (room.getColor()==0) {
                    roomInfo.append("不加色");
                }
                if (room.getColor()==1) {
                    roomInfo.append("加一色");
                }
                if (room.getColor()==2) {
                    roomInfo.append("加两色");
                }
                roomData.put("roominfo", roomInfo.toString());
            }
            roomData.put("zhuang",CommonConstant.NO_BANKER_INDEX);
            if (!Dto.stringIsNULL(room.getBanker())&&room.getBankerType()!=SSSConstant.SSS_BANKER_TYPE_HB&&room.getPlayerMap().get(room.getBanker())!=null) {
                roomData.put("zhuang",room.getPlayerMap().get(room.getBanker()).getMyIndex());
            }
            roomData.put("game_index",room.getGameIndex());
            roomData.put("showTimer",CommonConstant.GLOBAL_NO);
            if (room.getTimeLeft()>SSSConstant.SSS_TIMER_INIT) {
                roomData.put("showTimer",CommonConstant.GLOBAL_YES);
            }
            if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_COMPARE) {
                roomData.put("showTimer",CommonConstant.GLOBAL_NO);
                roomData.put("bipaiTimer",room.getCompareTimer()*100);
            }
            roomData.put("timer",room.getTimeLeft());
            roomData.put("myIndex",room.getPlayerMap().get(account).getMyIndex());
            roomData.put("users",room.getAllPlayer());
            roomData.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            roomData.put("myPaiType",room.getUserPacketMap().get(account).getPaiType());
            roomData.put("gameData",room.obtainGameData());
            if (room.getJieSanTime() > 0) {
                roomData.put("jiesan", CommonConstant.GLOBAL_YES);
                roomData.put("jiesanData", room.getJieSanData());
            }
            if (room.getGameStatus() == SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
                roomData.put("jiesuanData", room.obtainFinalSummaryData());
            }
            if (room.getBankerType()== SSSConstant.SSS_BANKER_TYPE_ZZ) {
                if (room.getGameStatus()==SSSConstant.SSS_GAME_STATUS_TO_BE_BANKER) {
                    roomData.put("bankerMinScore",room.getMinBankerScore());
                    roomData.put("bankerIsUse",CommonConstant.GLOBAL_NO);
                    if (room.getPlayerMap().get(account).getScore()>=room.getMinBankerScore()) {
                        roomData.put("bankerIsUse",CommonConstant.GLOBAL_YES);
                    }
                }
                roomData.put("baseNum",room.getBaseNumTimes(room.getPlayerMap().get(account).getScore()));
                roomData.put("xiazhu",room.obtainXzResult());
            }
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
                // 赢几个人
                int winPlayer = 0;
                int special = player.getPaiType();
                if (special>0) {
                    break;
                }
                for (String other : gameList) {
                    if (!other.equals(account)) {
                        Player otherPlayer = room.getUserPacketMap().get(other);
                        int otherSpecial = otherPlayer.getPaiType();
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
                int minSwatCount = SSSConstant.SSS_MIN_SWAT_COUNT;
                if (!Dto.isObjNull(room.getSetting())&&room.getSetting().containsKey("qld")) {
                    minSwatCount = room.getSetting().getInt("qld");
                }
                if (gameList.size()>=minSwatCount&&winPlayer==gameList.size()-1) {
                    room.setSwat(CommonConstant.GLOBAL_YES);
                    isSwat = true;
                    room.getUserPacketMap().get(account).setSwat(CommonConstant.GLOBAL_YES);
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
                        if (special>0&&otherSpecial>0) {
                            if (player.getPaiScore()>otherPlayer.getPaiScore()) {
                                sumScoreSingle += player.getPaiScore();
                            } else if (player.getPaiScore()<otherPlayer.getPaiScore()) {
                                sumScoreSingle -= otherPlayer.getPaiScore();
                            }
                        } else if (special>0&&otherSpecial==0) {
                            sumScoreSingle += player.getPaiScore();
                        } else if (special==0&&otherSpecial>0) {
                            sumScoreSingle -= otherPlayer.getPaiScore();
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
                            if (room.getMaPaiType()>0) {
                                if (Arrays.asList(player.getPai()).contains(room.getMaPai())) {
                                    sumScoreSingle *= 2;
                                }
                                if (Arrays.asList(otherPlayer.getPai()).contains(room.getMaPai())) {
                                    sumScoreSingle *= 2;
                                }
                            }
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
                            }
                            // 全垒打只对全垒打的人翻倍
                            if (isSwat) {
                                // 自己或者他人全垒打
                                if (room.getUserPacketMap().get(account).getSwat()==CommonConstant.GLOBAL_YES||
                                    room.getUserPacketMap().get(other).getSwat()==CommonConstant.GLOBAL_YES) {
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

    /**
     * 霸王庄、坐庄结算
     * 区别：坐庄模式没有全垒打
     * @param roomNo
     */
    public void gameSummaryBwzOrZZ(String roomNo){
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
            boolean isSwat = true;
            int minSwatCount = SSSConstant.SSS_MIN_SWAT_COUNT;
            if (!Dto.isObjNull(room.getSetting())&&room.getSetting().containsKey("qld")) {
                minSwatCount = room.getSetting().getInt("qld");
            }
            if (gameList.size()<minSwatCount||room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_ZZ) {
                isSwat = false;
            }
            // 庄家
            Player player = room.getUserPacketMap().get(room.getBanker());
            // 庄家输赢
            double sumScoreBanker = 0;
            // 庄家是否是特殊牌
            int special = player.getPaiType();
            // 单局结果
            JSONArray bankerResult = new JSONArray();
            // 头道
            bankerResult.add(new JSONObject().element("pai",player.getHeadPai()).element("score",0).element("type",0));
            // 中道
            bankerResult.add(new JSONObject().element("pai",player.getMidPai()).element("score",0).element("type",0));
            // 尾道
            bankerResult.add(new JSONObject().element("pai",player.getFootPai()).element("score",0).element("type",0));
            for (String account : gameList) {
                if (!account.equals(room.getBanker())) {
                    // 闲家
                    Player otherPlayer = room.getUserPacketMap().get(account);
                    // 闲家输赢
                    double sumScoreOther = 0;
                    // 闲家是否是特殊牌
                    int otherSpecial = otherPlayer.getPaiType();
                    // 单局结果
                    JSONArray otherResult = new JSONArray();
                    // 头道
                    otherResult.add(new JSONObject().element("pai",otherPlayer.getHeadPai()).element("score",0).element("type",0));
                    // 中道
                    otherResult.add(new JSONObject().element("pai",otherPlayer.getMidPai()).element("score",0).element("type",0));
                    // 尾道
                    otherResult.add(new JSONObject().element("pai",otherPlayer.getFootPai()).element("score",0).element("type",0));
                    if (special>0&&otherSpecial>0) {
                        if (player.getPaiScore()>otherPlayer.getPaiScore()) {
                            sumScoreBanker += player.getPaiScore();
                            sumScoreOther -= player.getPaiScore();
                        }else if (player.getPaiScore()<otherPlayer.getPaiScore()) {
                            sumScoreBanker -= otherPlayer.getPaiScore();
                            sumScoreOther += otherPlayer.getPaiScore();
                        }
                        isSwat = false;
                    } else if (special>0&&otherSpecial==0) {
                        sumScoreBanker += player.getPaiScore();
                        sumScoreOther -= player.getPaiScore();
                        isSwat = false;
                    } else if (special==0&&otherSpecial>0) {
                        sumScoreBanker -= otherPlayer.getPaiScore();
                        sumScoreOther += otherPlayer.getPaiScore();
                        isSwat = false;
                    } else {
                        // 比牌结果
                        JSONObject compareResult = SSSComputeCards.compare(player.getPai(), otherPlayer.getPai());
                        // 自己的牌型及分数
                        JSONArray bankerSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(0));
                        // 其他玩家的牌型及分数
                        JSONArray otherSingleResult = JSONArray.fromObject(compareResult.getJSONArray("result").get(1));
                        int winTime = 0;
                        for (int i = 0; i < otherResult.size(); i++) {
                            // 增加相应的分数
                            bankerResult.getJSONObject(i).put("score",bankerResult.getJSONObject(i).getInt("score")+bankerSingleResult.getJSONObject(i).getInt("score"));
                            otherResult.getJSONObject(i).put("score",otherResult.getJSONObject(i).getInt("score")+otherSingleResult.getJSONObject(i).getInt("score"));
                            // 设置没道对应的牌型
                            otherResult.getJSONObject(i).put("type",otherSingleResult.getJSONObject(i).getInt("type"));
                            bankerResult.getJSONObject(i).put("type",bankerSingleResult.getJSONObject(i).getInt("type"));
                            if (bankerSingleResult.getJSONObject(i).getInt("score")>otherSingleResult.getJSONObject(i).getInt("score")) {
                                winTime ++;
                            } else if (bankerSingleResult.getJSONObject(i).getInt("score")<otherSingleResult.getJSONObject(i).getInt("score")){
                                winTime --;
                            }
                        }
                        sumScoreOther = compareResult.getInt("B");
                        if (room.getBankerType()==SSSConstant.SSS_BANKER_TYPE_ZZ) {
                            sumScoreOther = compareResult.getInt("B")*room.getUserPacketMap().get(account).getXzTimes();
                        }
                        if (room.getMaPaiType()>0 && Arrays.asList(player.getPai()).contains(room.getMaPai())) {
                            sumScoreOther *= 2;
                        }
                        if (room.getMaPaiType()>0 && Arrays.asList(otherPlayer.getPai()).contains(room.getMaPai())) {
                            sumScoreOther *= 2;
                        }
                        // 三道全赢打枪
                        if (winTime==3) {
                            sumScoreOther *= 2;
                            JSONArray dq = new JSONArray();
                            dq.add(room.getPlayerMap().get(room.getBanker()).getMyIndex());
                            dq.add(room.getPlayerMap().get(account).getMyIndex());
                            if (!room.getDqArray().contains(dq)) {
                                room.getDqArray().add(dq);
                            }
                        }else {
                            isSwat = false;
                        }
                        // 三道全输被打枪
                        if (winTime==-3) {
                            sumScoreOther *= 2;
                            JSONArray dq = new JSONArray();
                            dq.add(room.getPlayerMap().get(account).getMyIndex());
                            dq.add(room.getPlayerMap().get(room.getBanker()).getMyIndex());
                            if (!room.getDqArray().contains(dq)) {
                                room.getDqArray().add(dq);
                            }
                        }
                        sumScoreBanker -= sumScoreOther;
                    }
                    // 设置闲家家当局输赢
                    room.getUserPacketMap().get(account).setScore(sumScoreOther*room.getScore());
                    // 设置头道输赢情况
                    room.getUserPacketMap().get(account).setHeadResult(otherResult.getJSONObject(0));
                    // 设置中道输赢情况
                    room.getUserPacketMap().get(account).setMidResult(otherResult.getJSONObject(1));
                    // 设置尾道输赢情况
                    room.getUserPacketMap().get(account).setFootResult(otherResult.getJSONObject(2));
                }
            }
            // 设置闲家家当局输赢
            room.getUserPacketMap().get(room.getBanker()).setScore(sumScoreBanker*room.getScore());
            // 设置头道输赢情况
            room.getUserPacketMap().get(room.getBanker()).setHeadResult(bankerResult.getJSONObject(0));
            // 设置中道输赢情况
            room.getUserPacketMap().get(room.getBanker()).setMidResult(bankerResult.getJSONObject(1));
            // 设置尾道输赢情况
            room.getUserPacketMap().get(room.getBanker()).setFootResult(bankerResult.getJSONObject(2));
            // 全垒打翻倍
            if (isSwat) {
                room.setSwat(CommonConstant.GLOBAL_YES);
                for (String account : room.getUserPacketMap().keySet()) {
                    double nowScore = room.getUserPacketMap().get(account).getScore();
                    room.getUserPacketMap().get(account).setScore(nowScore*2);
                    if (account.equals(room.getBanker())) {
                        room.getUserPacketMap().get(account).setSwat(CommonConstant.GLOBAL_YES);
                    }
                }
            }
        }
    }
}
