package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.Packer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.event.FundEventDeal;
import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.PackerCompare;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:26 2018/4/17
 * @Modified By:
 **/
@Component
public class NNGameEventDealNew {

    public static int GAME_NN = 1;

    private final static Logger logger = LoggerFactory.getLogger(NNGameEventDealNew.class);

    @Resource
    private GameTimerNiuNiu gameTimerNiuNiu;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RedisService redisService;

    @Resource
    private RobotEventDeal robotEventDeal;

    @Resource
    private UserBiz userBiz;

    @Resource
    private ClubBiz clubBiz;

    @Resource
    private FundEventDeal fundEventDeal;



    /**
     * 创建房间通知自己
     *
     * @param client
     */
    public void createRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject roomData = obtainRoomData(account, roomNo);
        // 数据不为空
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("data", roomData);
            // 通知自己
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
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
            NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
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
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush_NN");
        }
    }

    /**
     * 获取当前房间状态数据
     *
     * @param account
     * @param roomNo
     * @return
     */
    public JSONObject obtainRoomData(String account, String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String room_no = room.getRoomNo();
        JSONObject obj = new JSONObject();
        JSONObject fangjian = new JSONObject();
        obj.put("gameStatus", room.getGameStatus());
        obj.put("room_no", room.getRoomNo());
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("di", room.getScore());
        obj.put("banker_type", room.getBankerType());

        if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            obj.put("clubCode", room.getClubCode());
        }
        // 元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append("底注:");
            roomInfo.append((int) room.getScore());
            roomInfo.append(" 进:");
            roomInfo.append((int) room.getEnterScore());
            roomInfo.append(" 出:");
            roomInfo.append((int) room.getLeaveScore());
            if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
                roomInfo.append("\nJ以下庄家赢");
            }

            obj.put("roominfo", roomInfo.toString());
            obj.put("roominfo2", room.getWfType());
            fangjian.put("roominfo", roomInfo.toString());
            fangjian.put("roominfo2", room.getWfType());
            fangjian.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        }
        // 房卡场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
            || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append(room.getWfType());
            obj.put("roominfo", String.valueOf(roomInfo));
            fangjian.put("roominfo", String.valueOf(roomInfo));
            fangjian.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        }
        if (Dto.stringIsNULL(room.getBanker()) || (room.getGameStatus() <= NNConstant.NN_GAME_STATUS_DZ && room.getBankerType() != NNConstant.NN_BANKER_TYPE_ZZ) ||
            room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB || room.getUserPacketMap().get(room.getBanker()) == null) {
            obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
            obj.put("qzScore", 0);
        } else {
            obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
            obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
            obj.put("qzType", NNConstant.NN_QZ_TYPE);
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            obj.put("wanfaType", NNConstant.NN_GAME_TYPE);
            fangjian.put("wanfaType", NNConstant.NN_GAME_TYPE);
        }
        room.setRoomInfoData(fangjian);

        obj.put("game_index", room.getGameIndex());
        if (room.getGameIndex() == 0) {
            obj.put("game_index", 1);
        }
        obj.put("showTimer", CommonConstant.GLOBAL_NO);
        if (room.getTimeLeft() > NNConstant.NN_TIMER_INIT) {
            obj.put("showTimer", CommonConstant.GLOBAL_YES);
        }
        obj.put("timer", room.getTimeLeft());
        obj.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
        obj.put("qzTimes", room.getQzTimes(room.getPlayerMap().get(account).getScore()));
        obj.put("baseNum", room.getBaseNumTimes(room.getPlayerMap().get(account).getScore()));
        obj.put("users", room.getAllPlayer());
        obj.put("qiangzhuang", room.getQZResult());
        obj.put("xiazhu", room.getXiaZhuResult());
        obj.put("gameData", room.getGameData(account, room_no));
        if (room.getJieSanTime() > 0) {
            obj.put("jiesan", CommonConstant.GLOBAL_YES);
            obj.put("jiesanData", room.getJieSanData());
        }
        if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {
            obj.put("jiesuanData", room.getFinalSummary());
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ && room.getGameStatus() == NNConstant.NN_GAME_STATUS_TO_BE_BANKER) {
            obj.put("bankerMinScore", room.getMinBankerScore());
            obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
            if (room.getPlayerMap().get(account).getScore() >= room.getMinBankerScore()) {
                obj.put("bankerIsUse", CommonConstant.GLOBAL_YES);
            }
        }
        obj.put("startIndex", getStartIndex(roomNo));
//        room.setRoomInfoData(obj);
        return obj;
    }

    /**
     * 上庄
     *
     * @param client
     * @param data
     */
    public void gameBeBanker(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_TO_BE_BANKER, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 庄家已经确定
        if (!Dto.stringIsNULL(room.getBanker())) {
            return;
        }
        // 元宝不足无法上庄
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
                return;
            }
        }
        // 设置庄家
        room.setBanker(account);
        // 设置游戏状态
        room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
        changeGameStatus(room);
    }

    /**
     * 准备
     *
     * @param client
     * @param data
     */
    public void gameReady(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_INIT, client) &&
            !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_READY, client) &&
            !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_JS, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (NNGameEventDealNew.GAME_NN == 0) {
            postData.put("notSend", CommonConstant.GLOBAL_YES);
            exitRoom(client, postData);
            JSONObject result = new JSONObject();
            result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
            result.put(CommonConstant.RESULT_KEY_MSG, "即将停服进行更新!");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getLeaveScore()) {
                // 清出房间
                postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
                postData.put("notSend", CommonConstant.GLOBAL_YES);
                exitRoom(client, postData);
                JSONObject result = new JSONObject();
                result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                    result.put(CommonConstant.RESULT_KEY_MSG, "元宝不足");
                }
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                    result.put(CommonConstant.RESULT_KEY_MSG, "金币不足");
                }
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
                return;
            }
        }
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
            if (room.getPlayerMap().get(account).getRoomCardNum() < room.getLeaveScore()) {
                postData.put("notSend", CommonConstant.GLOBAL_YES);
                postData.put("notSendToMe", CommonConstant.GLOBAL_YES);
                exitRoom(client, postData);
                JSONObject result = new JSONObject();
                result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                result.put(CommonConstant.RESULT_KEY_MSG, "钻石不足,无法参赛");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "tipMsgPush");
                return;
            }
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY) {
            room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
        }
        int minStartCount = NNConstant.NN_MIN_START_COUNT;
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("mustFull")) {
                minStartCount = room.getPlayerCount();
            }
        }
        // 当前准备人数大于最低开始人数开始游戏
        if (room.getNowReadyCount() == minStartCount) {
            room.setTimeLeft(NNConstant.NN_TIMER_READY);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerNiuNiu.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_READY, 0);
                }
            });
        }
        // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
        if (room.isAllReady() && room.getPlayerMap().size() >= minStartCount) {
            startGame(room);
        } else {
            JSONObject result = new JSONObject();
            result.put("index", room.getPlayerMap().get(account).getMyIndex());
            result.put("showTimer", CommonConstant.GLOBAL_NO);
            if (room.getNowReadyCount() >= NNConstant.NN_MIN_START_COUNT) {
                result.put("showTimer", CommonConstant.GLOBAL_YES);
            }
            result.put("timer", room.getTimeLeft());
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "playerReadyPush_NN");
        }
    }


    /**
     * 房主提前开始
     *
     * @param client
     * @param data
     */
    public void gameStart(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_INIT, client) &&
            !CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_READY, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int type = postData.getInt("type");
        String eventName = "gameStartPush_NN";
        // 只有房卡场和俱乐部有提前开始的功能
        if (room.getRoomType() != CommonConstant.ROOM_TYPE_FK && room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB) {
            sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前房间不支持");
            return;
        }
        // 不是房主无法提前开始
        if (!account.equals(room.getOwner())) {
            sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前没有开始权限");
            return;
        }
        // 已经玩过无法提前开始
        if (room.getGameIndex() != 0) {
            sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "游戏已开局，无法提前开始");
            return;
        }
        int readyCount = room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_READY ?
            room.getNowReadyCount() : room.getNowReadyCount() + 1;
        // 实时准备人数不足
        if (readyCount < NNConstant.NN_MIN_START_COUNT) {
            sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_NO, "当前准备人数不足");
            return;
        }
        // 需要提前退出的人
        List<String> outList = new ArrayList<>();
        for (String player : room.getUserPacketMap().keySet()) {
            // 不是房主且未准备
            if (!account.equals(player) && room.getUserPacketMap().get(player).getStatus() != NNConstant.NN_USER_STATUS_READY) {
                outList.add(player);
            }
        }
        // 第一次点开始游戏有人需要退出
        if (type == NNConstant.START_GAME_TYPE_UNSURE && outList.size() > 0) {
            sendStartResultToSingle(client, eventName, CommonConstant.GLOBAL_YES, "是否开始");
            return;
        }
        if (!Dto.isObjNull(room.getSetting())) {
            room.getSetting().remove("mustFull");
        }
        // 房主未准备直接准备
        if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
            gameReady(client, data);
        }
        if (outList.size() > 0) {
            // 退出房间
            for (String player : outList) {
                if (room.getUserPacketMap().get(player).getStatus() != NNConstant.NN_USER_STATUS_READY) {
                    SocketIOClient playerClient = GameMain.server.getClient(room.getPlayerMap().get(player).getUuid());
                    JSONObject exitData = new JSONObject();
                    exitData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
                    exitData.put(CommonConstant.DATA_KEY_ACCOUNT, player);
                    exitData.put("notSend", CommonConstant.GLOBAL_YES);
                    exitData.put("notSendToMe", CommonConstant.GLOBAL_YES);
                    exitRoom(playerClient, exitData);
                    // 通知玩家
                    JSONObject result = new JSONObject();
                    result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                    result.put(CommonConstant.RESULT_KEY_MSG, "已被房主踢出房间");
                    CommonConstant.sendMsgEventToSingle(playerClient, result.toString(), "tipMsgPush");
                }
            }
        } else {
            startGame(room);
        }

    }

    /**
     * 通知玩家
     *
     * @param client
     * @param eventName
     * @param code
     * @param msg
     */
    private void sendStartResultToSingle(SocketIOClient client, String eventName, int code, String msg) {
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, code);
        result.put(CommonConstant.RESULT_KEY_MSG, msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

    /**
     * 明牌抢庄开始游戏
     *
     * @param room
     */
    public void startGameMp(final NNGameRoomNew room) {
        ((NNGameEventDealNew) AopContext.currentProxy()).shuffleAndFp(room);

        JSONArray gameProcessFP = new JSONArray();
        // 设置玩家手牌
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                room.getUserPacketMap().get(uuid).saveMingPai();
                // 存放游戏记录
                JSONObject userPai = new JSONObject();
                userPai.put("account", uuid);
                userPai.put("name", room.getPlayerMap().get(uuid).getName());
                userPai.put("pai", room.getUserPacketMap().get(uuid).getMyPai());
                gameProcessFP.add(userPai);
            }
        }
        room.getGameProcess().put("faPai", gameProcessFP);
        // 设置房间状态(抢庄)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_QZ);
        // 设置房间倒计时
        room.setTimeLeft(NNConstant.NN_TIMER_QZ);
        // 开启抢庄定时器
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_QZ, 0);
            }
        });
    }

    public void shuffleAndFp(NNGameRoomNew room) {
        // 洗牌
        NiuNiuServer.xiPai(room.getRoomNo());
        // 发牌
        NiuNiuServer.faPai(room.getRoomNo());
    }

    /**
     * 开始游戏
     *
     * @param room
     */
    public void startGame(final NNGameRoomNew room) {
        // 非准备或初始阶段无法开始开始游戏
        if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY && room.getGameStatus() != NNConstant.NN_GAME_STATUS_INIT) {
            return;
        }
        redisService.insertKey("summaryTimes_nn" + room.getRoomNo(), "0", null);
        // 初始化房间信息
        room.initGame();
        // 更新游戏局数
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("update_index")) {
                roomBiz.increaseRoomIndexByRoomNo(room.getRoomNo());
            }
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
            // 明牌抢庄开始游戏
            startGameMp(room);
        } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_QZ) {
            // 设置房间状态(抢庄)
            room.setGameStatus(NNConstant.NN_GAME_STATUS_QZ);
        } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            // 通比模式开始游戏
            startGameTb(room);
        } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_FZ || room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
            // 设置房间状态(下注)
            room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
            // 开启亮牌定时器
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_XZ, 0);
                }
            });
        }
        // 是否抽水
        if (room.getFee() > 0) {
            JSONArray array = new JSONArray();
            Map<String, Double> map = new HashMap<String, Double>();
            for (String account : room.getPlayerMap().keySet()) {
                if (room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                    // 中途加入不抽水
                    if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                        // 更新实体类数据
                        Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account);
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                        // 负数清零
                        if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                            RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                        }
                        map.put(room.getPlayerMap().get(account).getOpenId(), -room.getFee());
                        array.add(playerinfo.getId());
                    }
                }
            }
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
            if (room.isFund()) {
                fundEventDeal.addBalChangeRecord(map, "牛牛游戏抽水");
            }

        }
        // 通知前端状态改变
        changeGameStatus(room);
    }

    /**
     * 通比模式开始游戏
     *
     * @param room
     */
    public void startGameTb(final NNGameRoomNew room) {
        ((NNGameEventDealNew) AopContext.currentProxy()).shuffleAndFp(room);
        // 设置房间状态(下注)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
        changeGameStatus(room);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                    room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
                    // 通比模式默认下一个底注
                    room.getUserPacketMap().get(account).setXzTimes((int) room.getScore());
                    // 通知玩家
                    JSONObject result = new JSONObject();
                    result.put("index", room.getPlayerMap().get(account).getMyIndex());
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("value", room.getUserPacketMap().get(account).getXzTimes());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
                }
            }
        }
        // 设置游戏状态(亮牌)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
        // 设置倒计时
        room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
        // 开启亮牌定时器
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_LP, 0);
            }
        });
    }

    /**
     * 抢庄
     *
     * @param client
     * @param data
     */
    public void gameQiangZhuang(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 非抢庄阶段收到抢庄消息不作处理
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_QZ, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey(NNConstant.DATA_KEY_VALUE)) {
            // 设置玩家抢庄状态及抢庄倍数
            if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                /**
                 * 增加游戏模式需要对消息进行判断
                 * 游戏模式                玩家状态
                 * 通比模式/霸王庄/坐庄     不处理
                 * 明牌抢庄                准备
                 */
                // 非抢庄、明牌抢庄抢庄消息不做处理
                if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_MP && room.getBankerType() != NNConstant.NN_BANKER_TYPE_QZ) {
                    return;
                }
                // 不是准备状态的玩家抢庄消息不作处理(包括中途加入和已经抢过庄的)
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
                    return;
                }
                int maxTimes = getMaxTimes(room.getQzTimes(room.getPlayerMap().get(account).getScore()));
                if (postData.getInt(NNConstant.DATA_KEY_VALUE) < 0 || postData.getInt(NNConstant.DATA_KEY_VALUE) > maxTimes) {
                    return;
                }
                // 设置为玩家抢庄状态，抢庄倍数
                room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_QZ);
                room.getUserPacketMap().get(account).setQzTimes(postData.getInt(NNConstant.DATA_KEY_VALUE));
                // 所有人都完成抢庄
                if (room.isAllQZ()) {
                    gameDingZhuang(room, account);
                    JSONArray gameProcessQZ = new JSONArray();
                    for (String uuid : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                            if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                                JSONObject userQZ = new JSONObject();
                                userQZ.put("account", uuid);
                                userQZ.put("name", room.getPlayerMap().get(uuid).getName());
                                userQZ.put("qzTimes", room.getUserPacketMap().get(uuid).getQzTimes());
                                userQZ.put("banker", room.getPlayerMap().get(room.getBanker()).getName());
                                gameProcessQZ.add(userQZ);
                            }
                        }
                    }
                    room.getGameProcess().put("qiangzhuang", gameProcessQZ);
                } else {
                    JSONObject result = new JSONObject();
                    result.put("index", room.getPlayerMap().get(account).getMyIndex());
                    result.put("value", room.getUserPacketMap().get(account).getQzTimes());
                    result.put("type", 0);
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "qiangZhuangPush_NN");
                }
            }
        }
    }

    /**
     * 定庄
     *
     * @param room
     * @param lastAccount(最后一个抢庄玩家账号)
     */
    public void gameDingZhuang(final NNGameRoomNew room, String lastAccount) {
        // 非抢庄阶段不作处理
        if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_QZ) {
            return;
        }
        // 所有抢庄玩家
        List<String> qzList = new ArrayList<String>();
        // 所有参与玩家
        List<String> allList = new ArrayList<String>();
        // 最大抢庄倍数
        int maxBs = 1;
        // 随机庄家
        if (room.getSjBanker() == 1) {
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    // 中途加入除外
                    if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
                        // 所有有参与抢庄的玩家
                        if (room.getUserPacketMap().get(account).getQzTimes() > 0) {
                            qzList.add(account);
                        }
                        allList.add(account);
                    }
                }
            }
        } else {// 最高倍数为庄家
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    // 中途加入除外
                    if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
                        // 抢庄倍数大于最大倍数
                        if (room.getUserPacketMap().get(account).getQzTimes() >= maxBs) {
                            if (room.getUserPacketMap().get(account).getQzTimes() > maxBs) {
                                qzList.clear();
                            }
                            qzList.add(account);
                            maxBs = room.getUserPacketMap().get(account).getQzTimes();
                        }
                        allList.add(account);
                    }
                }
            }
        }
        // 庄家下标
        int bankerIndex = 0;
        // 只有一个玩家抢庄
        if (qzList.size() == 1) {
            room.setBanker(qzList.get(bankerIndex));
            room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
        } else if (qzList.size() > 1) {
            // 多个玩家抢庄
            bankerIndex = RandomUtils.nextInt(qzList.size());
            room.setBanker(qzList.get(bankerIndex));
            room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
        } else {// 无人抢庄
            if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_SJ) {
                // 随机庄家
                bankerIndex = RandomUtils.nextInt(allList.size());
                room.setBanker(allList.get(bankerIndex));
                room.setGameStatus(NNConstant.NN_GAME_STATUS_DZ);
            } else if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_JS) {
                // 解散房间
                // TODO: 2018/4/18 解散房间
                // 解散房间不需要后续通知玩家庄家已经确定
                return;
            } else if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_CK) {
                // 重新开局
                // 重置游戏状态
                room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
                // 初始化倒计时
                room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                // 重置玩家状态
                for (String account : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                        room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_INIT);
                    }
                }
                JSONObject result = new JSONObject();
                result.put("type", CommonConstant.SHOW_MSG_TYPE_NORMAL);
                result.put(CommonConstant.RESULT_KEY_MSG, "无人抢庄重新开局");
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "tipMsgPush");

                // 通知玩家
                changeGameStatus(room);
                // 重新开局不需要后续通知玩家庄家已经确定
                return;
            }
        }
        // 通知玩家
        JSONObject result = new JSONObject();
        result.put("index", room.getPlayerMap().get(lastAccount).getMyIndex());
        result.put("value", room.getUserPacketMap().get(lastAccount).getQzTimes());
        result.put("type", 1);
        result.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
        result.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
        result.put("gameStatus", room.getGameStatus());
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "qiangZhuangPush_NN");
        int sjCount = qzList.size();
        if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_SJ && sjCount == 0) {
            sjCount = allList.size();
        }
        // 多人抢庄才进行休眠
        final int sleepTime;
        if (sjCount > 1) {
            sleepTime = sjCount * 800;
        } else {
            sleepTime = 0;
        }
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_DZ, sleepTime);
            }
        });
    }

    /**
     * 下注
     *
     * @param client
     * @param data
     */
    public void gameXiaZhu(SocketIOClient client, Object data) {
        // 非下注阶段收到下注消息不作处理
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_XZ, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        final NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (postData.containsKey(NNConstant.DATA_KEY_MONEY)) {
            // 设置玩家下注状态及下注倍数
            if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                /**
                 * 增加游戏模式需要对消息进行判断
                 * 游戏模式      玩家状态
                 * 通比模式      不处理
                 * 明牌抢庄      闲家抢庄，庄家不作处理
                 * 霸王庄/坐庄   闲家准备，庄家不作处理
                 */
                if (account.equals(room.getBanker())) {
                    return;
                }
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
                    if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_QZ) {
                        return;
                    }
                }
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ || room.getBankerType() == NNConstant.NN_BANKER_TYPE_FZ) {
                    if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
                        return;
                    }
                }
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
                    return;
                }
                int maxTimes = getMaxTimes(room.getBaseNumTimes(room.getPlayerMap().get(account).getScore()));
                if (postData.getInt(NNConstant.DATA_KEY_MONEY) < 0 || postData.getInt(NNConstant.DATA_KEY_MONEY) > maxTimes) {
                    return;
                }
                room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
                room.getUserPacketMap().get(account).setXzTimes(postData.getInt(NNConstant.DATA_KEY_MONEY));
                // 通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put("value", room.getUserPacketMap().get(account).getXzTimes());
                room.getGameProcess().put("xiazhu", result);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
                // 所有人都完成下注
                if (room.isAllXiaZhu()) {
                    // 明牌抢庄已发牌
                    if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_MP) {
                        ((NNGameEventDealNew) AopContext.currentProxy()).shuffleAndFp(room);
                    }
                    // 设置游戏状态
                    room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
                    room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
                    ThreadPoolHelper.executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_LP, 0);
                        }
                    });

                    // 存放游戏记录
                    JSONArray gameProcessXZ = new JSONArray();
                    for (String uuid : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                            if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT && !room.getBanker().equals(uuid)) {
                                JSONObject userXZ = new JSONObject();
                                userXZ.put("account", uuid);
                                userXZ.put("name", room.getPlayerMap().get(uuid).getName());
                                userXZ.put("xzTimes", room.getUserPacketMap().get(uuid).getXzTimes());
                                gameProcessXZ.add(userXZ);
                            }
                        }
                    }
//                    room.getGameProcess().put("xiaZhu", gameProcessXZ);
                    // 通知玩家
                    changeGameStatus(room);
                }
            }
        }
    }

    public int getMaxTimes(JSONArray array) {
        int maxTimes = 0;
        for (int i = 0; i < array.size(); i++) {
            JSONObject baseNum = array.getJSONObject(i);
            if (baseNum.getInt("isuse") == CommonConstant.GLOBAL_YES && baseNum.getInt("val") > maxTimes) {
                maxTimes = baseNum.getInt("val");
            }
        }
        return maxTimes;
    }

    /**
     * 亮牌
     *
     * @param client
     * @param data
     */
    public void showPai(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 非亮牌阶段收到亮牌消息不作处理
        if (!CommonConstant.checkEvent(postData, NNConstant.NN_GAME_STATUS_LP, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 设置玩家亮牌状态
        if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            /**
             * 增加游戏模式需要对消息进行判断
             * 游戏模式      玩家状态
             * 通比模式      下注
             * 明牌抢庄      闲家下注，庄家抢庄
             * 霸王庄/坐庄   闲家下注，庄家准备
             */
            if (!account.equals(room.getBanker()) || room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_XZ) {
                    return;
                }
            } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_QZ) {
                    return;
                }
            } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ || room.getBankerType() == NNConstant.NN_BANKER_TYPE_FZ) {
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
                    return;
                }
            }
            // 配牛
            if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_TB) {
                peiNiu(roomNo, account);
            } else {
                UserPacket winner = new UserPacket(room.getUserPacketMap().get(account).getPs(), false, room.getSpecialType());
                // 设置牌型
                room.getUserPacketMap().get(account).setType(winner.getType());
            }
            // 设置玩家状态
            room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_LP);
            // 所有人都完成亮牌
            if (!room.isAllShowPai()) {
                // 通知玩家
                JSONObject result = new JSONObject();
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put("pai", room.getUserPacketMap().get(account).getSortPai());
                result.put("paiType", room.getUserPacketMap().get(account).getType());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "showPaiPush_NN");
            } else {
                showFinish(roomNo);
            }
        }
    }

    public void showFinish(String roomNo) {
        String summaryTimesKey = "summaryTimes_nn" + roomNo;
        long summaryTimes = redisService.incr(summaryTimesKey, 1);
        if (summaryTimes > 1) {
            return;
        }
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 存放游戏记录
        JSONArray gameProcessLP = new JSONArray();
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                    JSONObject userLP = new JSONObject();
                    userLP.put("account", uuid);
                    userLP.put("name", room.getPlayerMap().get(uuid).getName());
                    userLP.put("pai", room.getUserPacketMap().get(uuid).getMingPai());
                    gameProcessLP.add(userLP);
                }
            }
        }
        // 结算玩家输赢数据
        gameJieSuan(room);
        // 设置房间状态
        room.setGameStatus(NNConstant.NN_GAME_STATUS_JS);
        // 初始化倒计时
        room.setTimeLeft(NNConstant.NN_TIMER_INIT);
//        room.getGameProcess().put("showPai", gameProcessLP);
        // 设置玩家状态
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                if (room.getUserPacketMap().get(uuid).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                    room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_JS);
                }
            }
        }
        // 通知玩家
        changeGameStatus(room);
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
            || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS) {
                saveGameLog(room.getRoomNo());
            }
            if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS && room.getGameIndex() == room.getGameCount()) {

                room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                    room.setOpen(false);
                }
                room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
            }
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
            if (room.getPlayerMap().get(room.getBanker()).getScore() < room.getMinBankerScore()) {
                // 庄家设为空
                room.setBanker(null);
                // 设置游戏状态
                room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
                // 初始化倒计时
                room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                // 重置玩家状态
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                        room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
                    }
                }
            }
        }
    }

    /**
     * 结算
     *
     * @param room
     */
    public void gameJieSuan(NNGameRoomNew room) {
        if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_LP) {
            return;
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            niuNiuTongBi(room);
        } else {
            // 通杀
            boolean tongSha = true;
            // 通赔
            boolean tongPei = true;
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    UserPacket bankerUp = room.getUserPacketMap().get(room.getBanker());
                    // 有参与的玩家
                    if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                        // 不是庄家
                        if (!account.equals(room.getBanker())) {
                            // 计算输赢
                            UserPacket banker = new UserPacket(room.getUserPacketMap().get(room.getBanker()).getPs(), true, room.getSpecialType());
                            UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(account).getPs(), room.getSpecialType());
                            UserPacket winner = PackerCompare.getWin(userpacket, banker);
                            // 庄家抢庄倍数
                            int qzTimes = room.getUserPacketMap().get(room.getBanker()).getQzTimes();
                            if (qzTimes <= 0) {
                                qzTimes = 1;
                            }
                            // 坐庄模式闲家没牛十点以下直接输
                            if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
                                if (room.getUserPacketMap().get(account).getType() == 0) {
                                    Packer[] ps = room.getUserPacketMap().get(account).getPs();
                                    boolean allMin = true;
                                    for (Packer p : ps) {
                                        if (p.getNum().getNum() > 10) {
                                            allMin = false;
                                            break;
                                        }
                                    }
                                    if (allMin) {
                                        userpacket.setWin(false);
                                    }
                                }
                            }
                            // 输赢分数 下注倍数*倍率*底注*抢庄倍数
                            double totalScore = room.getUserPacketMap().get(account).getXzTimes() * room.getRatio().get(winner.getType()) * room.getScore() * qzTimes;
                            // 闲家赢
                            if (userpacket.isWin()) {
                                // 设置闲家当局输赢
                                room.getUserPacketMap().get(account).setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), totalScore));
                                // 设置庄家当局输赢
                                room.getUserPacketMap().get(room.getBanker()).setScore(Dto.sub(bankerUp.getScore(), totalScore));
                                // 闲家当前分数
                                double oldScoreXJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.add(oldScoreXJ, totalScore));
                                // 庄家家当前分数
                                double oldScoreZJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker()).getScore();
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker()).setScore(Dto.sub(oldScoreZJ, totalScore));
                                tongSha = false;
                            } else { // 庄家赢
                                // 设置闲家当局输赢
                                room.getUserPacketMap().get(account).setScore(Dto.sub(room.getUserPacketMap().get(account).getScore(), totalScore));
                                // 设置庄家当局输赢
                                room.getUserPacketMap().get(room.getBanker()).setScore(Dto.add(bankerUp.getScore(), totalScore));
                                // 闲家当前分数
                                double oldScoreXJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(oldScoreXJ, totalScore));
                                // 庄家家当前分数
                                double oldScoreZJ = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker()).getScore();
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(room.getBanker()).setScore(Dto.add(oldScoreZJ, totalScore));
                                tongPei = false;
                            }
                        }
                        // 负数清零
                        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                            if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                            }
                        }
                    }
                }
            }
            // 通杀
            if (tongSha) {
                room.setTongSha(1);
            }
            // 通赔
            if (tongPei) {
                room.setTongSha(-1);
            }
        }
        // 房卡场结算
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
            || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            room.setNeedFinalSummary(true);
            roomCardSummary(room.getRoomNo());
            // 更新房卡
            updateRoomCard(room.getRoomNo());
        }
        // 更新数据库
        updateUserScore(room.getRoomNo());
        // 竞技场
        updateCompetitiveUserScore(room.getRoomNo());
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            saveUserDeduction(room.getRoomNo());
        }
        // 金币场不插入战绩
        if (room.getRoomType() != CommonConstant.ROOM_TYPE_JB && room.getRoomType() != CommonConstant.ROOM_TYPE_COMPETITIVE) {
//            saveGameLog(room.getRoomNo());
        }
    }

    /**
     * 房卡场结算
     *
     * @param roomNo
     */
    public void roomCardSummary(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 有参与的玩家
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                    UserPacket up = room.getUserPacketMap().get(account);
                    // 无牛次数+1
                    if (up.getType() == 0) {
                        up.setWuNiuTimes(up.getWuNiuTimes() + 1);
                    }
                    // 牛牛次数+1
                    if (up.getType() == 10) {
                        up.setNiuNiuTimes(up.getNiuNiuTimes() + 1);
                    }
                    // 胜利次数+1
                    if (up.getScore() > 0) {
                        up.setWinTimes(up.getWinTimes() + 1);
                    }
                    if (account.equals(room.getBanker())) {
                        // 通杀次数+1
                        if (room.getTongSha() == 1) {
                            up.setTongShaTimes(up.getTongShaTimes() + 1);
                        }
                        // 通赔次数+1
                        if (room.getTongSha() == -1) {
                            up.setTongPeiTimes(up.getTongPeiTimes() + 1);
                        }
                    }
                }
            }
        }
    }

    /**
     * 竞技场结算
     *
     * @param roomNo
     */
    public void updateCompetitiveUserScore(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
            JSONArray array = new JSONArray();
            JSONArray userIds = new JSONArray();
            for (String uuid : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                    if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                        room.getPlayerMap().get(uuid).setRoomCardNum(room.getPlayerMap().get(uuid).getRoomCardNum() - room.getSinglePayNum());
                        if (room.getPlayerMap().get(uuid).getRoomCardNum() < 0) {
                            room.getPlayerMap().get(uuid).setRoomCardNum(0);
                        }
                        JSONObject obj = new JSONObject();
                        obj.put("total", 2);
                        obj.put("fen", room.getUserPacketMap().get(uuid).getScore());
                        obj.put("id", room.getPlayerMap().get(uuid).getId());
                        array.add(obj);
                        userIds.add(room.getPlayerMap().get(uuid).getId());
                        JSONObject object = new JSONObject();
                        object.put("userId", room.getPlayerMap().get(uuid).getId());
                        object.put("score", room.getUserPacketMap().get(uuid).getScore());
                        object.put("type", 2);
                        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.ADD_OR_UPDATE_USER_COINS_REC, object));
                    }
                }
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("array", array);
            jsonObject.put("updateType", "score");
            // 更新玩家分数
            if (array.size() > 0 && userIds.size() > 0) {
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(userIds, room.getSinglePayNum())));
            }
        }
    }

    /**
     * 更新数据库
     *
     * @param roomNo
     */
    public void updateUserScore(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return;
        }
        JSONArray array = new JSONArray();
        Map<String, Double> map = new HashMap<String, Double>();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 有参与的玩家
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                    // 元宝输赢情况
                    JSONObject obj = new JSONObject();
                    if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB || room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                        double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                        long userId = room.getPlayerMap().get(account).getId();
                        double sum = room.getUserPacketMap().get(account).getScore();
                        array.add(obtainUserScoreData(total, sum, userId));
                        map.put(room.getPlayerMap().get(account).getOpenId(), sum);
                    }
                }
            }
        }
        if (room.getId() == 0) {
            JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
            if (!Dto.isObjNull(roomInfo)) {
                room.setId(roomInfo.getLong("id"));
            }
        }
        if (array.size() > 0) {
            // 更新玩家分数
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
            if (room.isFund()) {
                fundEventDeal.addBalChangeRecord(map, "牛牛游戏输赢");
            }
        }
    }

    public void updateRoomCard(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
            for (String account : room.getUserPacketMap().keySet()) {
                if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                    // 房主支付
                    if (room.getPayType() == CommonConstant.PAY_TYPE_OWNER) {
                        if (account.equals(room.getOwner())) {
                            // 参与第一局需要扣房卡
                            if (room.getUserPacketMap().get(account).getPlayTimes() == 1) {
                                roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                                array.add(room.getPlayerMap().get(account).getId());
                            }
                        }
                    }
                    // 房费AA
                    if (room.getPayType() == CommonConstant.PAY_TYPE_AA) {
                        // 参与第一局需要扣房卡
                        if (room.getUserPacketMap().get(account).getPlayTimes() == 1) {
                            array.add(room.getPlayerMap().get(account).getId());
                            roomCardCount = room.getSinglePayNum();
                        }
                    }
                    // 房主（会长）支付，非一次性扣清
                    if (room.getPayType() == CommonConstant.PAY_TYPE_OWNER2) {
                        if (account.equals(room.getOwner())) {
                            for (String player : room.getUserPacketMap().keySet()) {
                                if (room.getUserPacketMap().get(player) != null && room.getUserPacketMap().get(player).getPlayTimes() == 1) {
                                    roomCardCount += room.getSinglePayNum();
                                }
                            }
                            if (roomCardCount > 0) {
                                if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                                    array.add(room.getPlayerMap().get(account).getId());
                                } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                                    clubBiz.clubPump(room.getClubCode(), roomCardCount, room.getId(), roomNo, room.getGid());
                                }
                            }
                        }
                    }
                    // 俱乐部会长支付（一次性扣清房间人数）
                    if (room.getPayType() == CommonConstant.PAY_TYPE_LORD) {
                        if (room.getGameIndex() == 1) {
                            if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                                if (!room.isCost()) {
                                    roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                                    boolean pump = clubBiz.clubPump(room.getClubCode(), roomCardCount, room.getId(), roomNo, room.getGid());
                                    room.setCost(pump);
                                }
                            }
                        }
                    }
                }
            }
        } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_DK && room.getGameIndex() == 1) {
            JSONObject userInfo = userBiz.getUserByAccount(room.getOwner());
            if (!Dto.isObjNull(userInfo)) {
                roomCardCount = room.getPlayerCount() * room.getSinglePayNum();
                array.add(userInfo.getLong("id"));
            }
        }
        if (array.size() > 0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array, roomCardCount)));
        }
    }

    public void saveUserDeduction(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return;
        }
        JSONArray userDeductionData = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                    // 用户游戏记录
                    JSONObject object = new JSONObject();
                    object.put("id", room.getPlayerMap().get(account).getId());
                    object.put("gid", room.getGid());
                    object.put("roomNo", room.getRoomNo());
                    object.put("type", room.getRoomType());
                    object.put("fen", room.getUserPacketMap().get(account).getScore());
                    object.put("old", Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(), room.getUserPacketMap().get(account).getScore()));
                    if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                        object.put("new", 0);
                    } else {
                        object.put("new", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                    }
                    userDeductionData.add(object);
                }
            }
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
    }

    /**
     * 获取需要更新的数据
     *
     * @param total
     * @param sum
     * @param id
     * @return
     */
    public JSONObject obtainUserScoreData(double total, double sum, long id) {
        JSONObject obj = new JSONObject();
        obj.put("id", id);
        obj.put("total", total);
        obj.put("fen", sum);
        return obj;
    }

    public void saveGameLog(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room == null) {
            return;
        }
        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        JSONArray array = new JSONArray();
        // 存放游戏记录
        JSONArray gameProcessJS = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 有参与的玩家
//                if (room.getUserPacketMap().get(account).getStatus()==NNConstant.NN_USER_STATUS_LP) {
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS) {

                    JSONObject userJS = new JSONObject();
                    userJS.put("account", account);
                    userJS.put("name", room.getPlayerMap().get(account).getName());
                    userJS.put("sum", room.getUserPacketMap().get(account).getScore());
                    userJS.put("pai", room.getUserPacketMap().get(account).getSortPai());
                    userJS.put("paiType", room.getUserPacketMap().get(account).getType());
                    userJS.put("old", Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(), room.getUserPacketMap().get(account).getScore()));
                    if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                        userJS.put("new", 0);
                    } else {
                        userJS.put("new", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                    }
                    gameProcessJS.add(userJS);
                    double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                    double sum = room.getUserPacketMap().get(account).getScore();
                    long userId = room.getPlayerMap().get(account).getId();
                    array.add(obtainUserScoreData(total, sum, userId));
                    // 战绩记录
                    JSONObject gameLogResult = new JSONObject();
                    gameLogResult.put("account", account);
                    gameLogResult.put("name", room.getPlayerMap().get(account).getName());
                    gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
                    if (!Dto.stringIsNULL(room.getBanker()) && room.getPlayerMap().containsKey(room.getBanker()) && room.getPlayerMap().get(room.getBanker()) != null) {
                        gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                    } else {
                        gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                    }
                    gameLogResult.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
                    gameLogResult.put("myPai", room.getUserPacketMap().get(account).getMyPai());
                    gameLogResult.put("mingPai", room.getUserPacketMap().get(account).getSortPai());
                    gameLogResult.put("score", room.getUserPacketMap().get(account).getScore());
                    gameLogResult.put("totalScore", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                    gameLogResult.put("win", CommonConstant.GLOBAL_YES);
                    if (room.getUserPacketMap().get(account).getScore() < 0) {
                        gameLogResult.put("win", CommonConstant.GLOBAL_NO);
                    }
                    gameLogResult.put("zhuangTongsha", room.getTongSha());
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
                    userResult.put("account", account);
                    gameResult.add(userResult);
                }
            }
        }
//        room.getGameProcess().put("JieSuan", gameProcessJS);
        logger.info(room.getRoomNo() + "---" + String.valueOf(room.getGameProcess()));
        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
        for (int i = 0; i < userGameLogs.size(); i++) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
        }
    }

    /**
     * 退出房间
     *
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            boolean canExit = false;
            // 金币场、元宝场
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB ||
                room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
                // 未参与游戏可以自由退出
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_INIT) {
                    canExit = true;
                } else if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_INIT ||
                    room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY ||
                    room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS) {// 初始及准备阶段可以退出
                    canExit = true;
                }
            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
                || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                if (room.getUserPacketMap().get(account).getPlayTimes() == 0) {
                    if (room.getPayType() == CommonConstant.PAY_TYPE_AA || !room.getOwner().equals(account) || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                        canExit = true;
                    }
                }
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {
                    canExit = true;
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no", room.getRoomNo());
                if (room.getRoomType() != CommonConstant.ROOM_TYPE_FK && room.getRoomType() != CommonConstant.ROOM_TYPE_DK
                    && room.getRoomType() != CommonConstant.ROOM_TYPE_CLUB) {
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
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY && room.getNowReadyCount() < NNConstant.NN_MIN_START_COUNT) {
                    // 重置房间倒计时
                    room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                }
                if (room.getTimeLeft() > 0) {
                    result.put("showTimer", CommonConstant.GLOBAL_YES);
                } else {
                    result.put("showTimer", CommonConstant.GLOBAL_NO);
                }
                result.put("timer", room.getTimeLeft());
                result.put("startIndex", getStartIndex(roomNo));
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_NN");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_NN");
                }
                // 坐庄模式
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
                    // 房主退出且房间内有其他玩家
                    if (account.equals(room.getBanker()) && room.getUserPacketMap().size() > 0) {
                        // 庄家设为空
                        room.setBanker(null);
                        // 设置游戏状态
                        room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
                        // 初始化倒计时
                        room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                        // 重置玩家状态
                        for (String uuid : room.getUserPacketMap().keySet()) {
                            if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                                room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
                            }
                        }
                        changeGameStatus(room);
                        return;
                    }
                }
                int minStartCount = NNConstant.NN_MIN_START_COUNT;
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                    if (!Dto.isObjNull(room.getSetting()) && room.getSetting().containsKey("mustFull")) {
                        minStartCount = room.getPlayerCount();
                    }
                }
                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (room.isAllReady() && room.getPlayerMap().size() >= minStartCount) {
                    startGame(room);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size() == 0 && room.getRoomType() != CommonConstant.ROOM_TYPE_DK) {
                    redisService.deleteByKey("summaryTimes_nn" + room.getRoomNo());
                    roomInfo.put("status", room.getIsClose());
                    roomInfo.put("game_index", room.getGameIndex());
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
                // 机器人退出
                if (room.isRobot() && room.getRobotList().contains(account)) {
                    robotEventDeal.robotExit(account);
                }
            } else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
                result.put("showTimer", CommonConstant.GLOBAL_YES);
                result.put("timer", room.getTimeLeft());
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush_NN");
            }
        }
    }


    /**
     * 状态改变通知前端
     *
     * @param room
     */
    public void changeGameStatus(NNGameRoomNew room) {
        String room_no = room.getRoomNo();
        for (String account : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                JSONObject obj = new JSONObject();
                obj.put("gameStatus", room.getGameStatus());
                if (Dto.stringIsNULL(room.getBanker()) || (room.getGameStatus() <= NNConstant.NN_GAME_STATUS_DZ && room.getBankerType() != NNConstant.NN_BANKER_TYPE_ZZ) ||
                    room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB || room.getPlayerMap().get(room.getBanker()) == null) {
                    obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                    obj.put("qzScore", 0);
                } else {
                    obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                    obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
                }
                obj.put("game_index", room.getGameIndex());
                obj.put("showTimer", CommonConstant.GLOBAL_YES);
                if (room.getTimeLeft() == NNConstant.NN_TIMER_INIT) {
                    obj.put("showTimer", CommonConstant.GLOBAL_NO);
                }
                obj.put("timer", room.getTimeLeft());
                obj.put("qzTimes", room.getQzTimes(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()));
                obj.put("baseNum", room.getBaseNumTimes(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()));
                obj.put("users", room.getAllPlayer());
                obj.put("gameData", room.getGameData(account, room_no));
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_XZ) {
                    room.setUserInfoData(obj);
                }
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_DK
                    || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                    if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_ZJS) {
                        obj.put("jiesuanData", room.getFinalSummary());
                    }
                    if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS && room.getGameIndex() == room.getGameCount()) {
                        obj.put("jiesuanData", room.getFinalSummary());
                    }
                }
                /**
                 * 20180518 房主坐庄模式 wqm
                 */
                obj.put("isBanker", CommonConstant.GLOBAL_NO);
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
                    if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_TO_BE_BANKER ||
                        (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS && room.getPlayerMap().get(room.getBanker()).getScore() < room.getMinBankerScore())) {
                        obj.put("isBanker", CommonConstant.GLOBAL_YES);
                        obj.put("bankerMinScore", room.getMinBankerScore());
                        obj.put("bankerIsUse", CommonConstant.GLOBAL_YES);
                        if (room.getPlayerMap().get(account).getScore() < room.getMinBankerScore()) {
                            obj.put("bankerIsUse", CommonConstant.GLOBAL_NO);
                        }
                    }
                }

                UUID uuid = room.getPlayerMap().get(account).getUuid();
                if (uuid != null) {
                    CommonConstant.sendMsgEventToSingle(uuid, obj.toString(), "changeGameStatusPush_NN");

                }
            }
        }
        if (room.isRobot()) {
            for (String robotAccount : room.getRobotList()) {
                int delayTime = RandomUtils.nextInt(3) + 2;
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS || room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY) {
                    robotEventDeal.changeRobotActionDetail(robotAccount, NNConstant.NN_GAME_EVENT_READY, delayTime);
                }
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_QZ) {
                    robotEventDeal.changeRobotActionDetail(robotAccount, NNConstant.NN_GAME_EVENT_QZ, delayTime);
                }
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_XZ) {
                    robotEventDeal.changeRobotActionDetail(robotAccount, NNConstant.NN_GAME_EVENT_XZ, delayTime);
                }
                if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_LP) {
                    robotEventDeal.changeRobotActionDetail(robotAccount, NNConstant.NN_GAME_EVENT_LP, delayTime);
                }
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
        final NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
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
                        gameTimerNiuNiu.closeRoomOverTime(roomNo, room.getJieSanTime());
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
                    if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                        room.getUserPacketMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
                    }
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_NN");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (room.isAgreeClose()) {
                    // 未玩完一局不需要强制结算
                    if (!room.isNeedFinalSummary()) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 更新数据库
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("room_no", room.getRoomNo());
                        roomInfo.put("status", room.getIsClose());
                        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type", CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG, "房间已被解散");
                        CommonConstant.sendMsgEventToAll(uuidList, result.toString(), "tipMsgPush");
                        return;
                    }
                    if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                        room.setOpen(false);
                    }
                    room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
                    changeGameStatus(room);
//                    saveGameLog(room.getRoomNo());

                } else {// 刷新数据
                    room.getUserPacketMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", room.getJieSanData());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_NN");
                }
            }
        }
    }

    /**
     * 重连
     *
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO) ||
            !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT) ||
            !postData.containsKey("uuid")) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        // uuid不匹配
        if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid")) ||
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
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
            return;
        }
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(account, roomNo));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, result.toString(), "reconnectGamePush_NN");
    }

    /**
     * 配牛
     *
     * @param roomNo
     * @param uuid
     */
    public void peiNiu(String roomNo, String uuid) {
        NNGameRoomNew room = ((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
        UserPacket packet = room.getUserPacketMap().get(uuid);
        if (uuid.equals(room.getBanker())) {
            UserPacket zhuang = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), true, room.getSpecialType());
            packet.setType(zhuang.getType());
            packet.setWin(zhuang.isWin());
        } else {
            Packer[] ups = room.getUserPacketMap().get(uuid).getPs();
            // 有发牌的玩家
            if (ups != null && ups.length > 0 && ups[0] != null) {
                UserPacket zhuang = new UserPacket(room.getUserPacketMap().get(room.getBanker()).getPs(), true, room.getSpecialType());
                UserPacket userpacket = new UserPacket(ups, room.getSpecialType());
                PackerCompare.getWin(userpacket, zhuang);
                packet.setType(userpacket.getType());
                packet.setWin(userpacket.isWin());
            }
        }
    }

    /**
     * 通比牛牛计算
     *
     * @param room
     * @return
     */
    private String niuNiuTongBi(NNGameRoomNew room) {

        String winUUID = null;
        for (String win : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(win) && room.getUserPacketMap().get(win) != null) {
                if (room.getUserPacketMap().get(win).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                    UserPacket winPacket = room.getUserPacketMap().get(win);
                    UserPacket winner = new UserPacket(winPacket.getPs(), true, room.getSpecialType());
                    // 设置牌型
                    winPacket.setType(winner.getType());

                    boolean isWin = true;
                    for (String uuid : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(uuid) && room.getUserPacketMap().get(uuid) != null) {
                            if (!uuid.equals(win) && room.getUserPacketMap().get(uuid).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                                UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), room.getSpecialType());
                                // 计算玩家输赢
                                PackerCompare.getWin(winner, userpacket);
                                // 输给其他玩家
                                if (!winner.isWin()) {
                                    isWin = false;
                                    break;
                                }
                            }
                        }
                    }
                    // 通杀
                    if (isWin) {
                        double totalScore = 0;
                        for (String account : room.getUserPacketMap().keySet()) {
                            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                                if (!account.equals(win) && room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                                    totalScore = Dto.add(totalScore, room.getScore());
                                }
                            }
                        }
                        // 设置当局输赢
                        room.getUserPacketMap().get(win).setScore(totalScore);
                        // 设置当前分数
                        double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).getScore();
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).setScore(Dto.add(oldScore, totalScore));
                        winUUID = win;
                        winPacket.setWin(true);
                    } else {
                        // 设置当局输赢
                        room.getUserPacketMap().get(win).setScore(-room.getScore());
                        // 设置当前分数
                        double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).getScore();
                        RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).setScore(Dto.sub(oldScore, room.getScore()));
                        winPacket.setWin(false);
                    }
                }
            }
        }
        if (!Dto.stringIsNULL(winUUID) && room.getUserPacketMap().containsKey(winUUID) && room.getUserPacketMap().get(winUUID) != null) {
            int winType = room.getUserPacketMap().get(winUUID).getType();
            if (room.getRatio().containsKey(winType) && room.getRatio().get(winType) != null) {
                // 赢家赔率
                int winRatio = room.getRatio().get(winType);
                if (winRatio > 1) {
                    for (String account : room.getUserPacketMap().keySet()) {
                        // 需要额外增加的分数
                        double extraScore = room.getUserPacketMap().get(account).getScore() * (winRatio - 1);
                        // 更改玩家分数
                        room.getUserPacketMap().get(account).setScore(Dto.add(room.getUserPacketMap().get(account).getScore(), extraScore));
                        room.getPlayerMap().get(account).setScore(Dto.add(room.getPlayerMap().get(account).getScore(), extraScore));
                    }
                }
            }
        }
        return winUUID;
    }

    /**
     * 开始游戏按钮下标
     *
     * @param roomNo
     * @return
     */
    private int getStartIndex(String roomNo) {
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
            // 房卡场或俱乐部
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK || room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                // 房主在房间内返回房主的下标
                if (!Dto.stringIsNULL(room.getOwner()) && room.getPlayerMap().containsKey(room.getOwner())
                    && room.getPlayerMap().get(room.getOwner()) != null) {
                    return room.getPlayerMap().get(room.getOwner()).getMyIndex();
                }
            }
        }
        return CommonConstant.NO_BANKER_INDEX;
    }
}
