package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.Packer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.PackerCompare;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.NNConstant;
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
        JSONObject obj = new JSONObject();
        obj.put("gameStatus", room.getGameStatus());
        obj.put("room_no", room.getRoomNo());
        obj.put("roomType", room.getRoomType());
        obj.put("game_count", room.getGameCount());
        obj.put("di", room.getScore());
        // 元宝场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append("底注:");
            roomInfo.append((int) room.getScore());
            roomInfo.append(" 进:");
            roomInfo.append((int) room.getEnterScore());
            roomInfo.append(" 出:");
            roomInfo.append((int) room.getLeaveScore());
            obj.put("roominfo", roomInfo.toString());
            obj.put("roominfo2", room.getWfType());
        }
        // 房卡场
        if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
            StringBuffer roomInfo = new StringBuffer();
            roomInfo.append(room.getWfType());
            obj.put("roominfo",String.valueOf(roomInfo));
        }
        if (Dto.stringIsNULL(room.getBanker())||(room.getGameStatus()<=NNConstant.NN_GAME_STATUS_DZ&&room.getBankerType()!=NNConstant.NN_BANKER_TYPE_ZZ)||
            room.getBankerType()==NNConstant.NN_BANKER_TYPE_TB||room.getUserPacketMap().get(room.getBanker())==null) {
            obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
            obj.put("qzScore", 0);
        }else {
            obj.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
            obj.put("qzScore", room.getUserPacketMap().get(room.getBanker()).getQzTimes());
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
            obj.put("qzType", NNConstant.NN_QZ_TYPE);
        }
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            obj.put("wanfaType",NNConstant.NN_GAME_TYPE);
        }
        obj.put("game_index", room.getGameIndex());
        if (room.getGameIndex()==0) {
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
        obj.put("gameData", room.getGameData(account));
        if (room.getJieSanTime() > 0) {
            obj.put("jiesan", CommonConstant.GLOBAL_YES);
            obj.put("jiesanData", room.getJieSanData());
        }
        if (room.getGameStatus()==NNConstant.NN_GAME_STATUS_ZJS) {
            obj.put("jiesuanData", room.getFinalSummary());
        }
        if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_ZZ&&room.getGameStatus()==NNConstant.NN_GAME_STATUS_TO_BE_BANKER) {
            obj.put("bankerMinScore",room.getMinBankerScore());
            obj.put("bankerIsUse",CommonConstant.GLOBAL_NO);
            if (room.getPlayerMap().get(account).getScore()>=room.getMinBankerScore()) {
                obj.put("bankerIsUse",CommonConstant.GLOBAL_YES);
            }
        }
        return obj;
    }

    /**
     * 上庄
     * @param client
     * @param data
     */
    public void gameBeBanker(SocketIOClient client, Object data){
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
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
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
        if (NNGameEventDealNew.GAME_NN==0) {
            postData.put("notSend",CommonConstant.GLOBAL_YES);
            exitRoom(client,postData);
            JSONObject result = new JSONObject();
            result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
            result.put(CommonConstant.RESULT_KEY_MSG,"即将停服进行更新!");
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getLeaveScore()) {
                // 清出房间
                postData.put("notSendToMe",CommonConstant.GLOBAL_YES);
                postData.put("notSend",CommonConstant.GLOBAL_YES);
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
        room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != NNConstant.NN_GAME_STATUS_READY) {
            room.setGameStatus(NNConstant.NN_GAME_STATUS_READY);
        }
        // 当前准备人数大于最低开始人数开始游戏
        if (room.getNowReadyCount() == NNConstant.NN_MIN_START_COUNT) {
            room.setTimeLeft(NNConstant.NN_TIMER_READY);
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerNiuNiu.gameOverTime(roomNo, NNConstant.NN_GAME_STATUS_READY,0);
                }
            });
        }
        // 房间内所有玩家都已经完成准备且人数大于最低开始人数通知开始游戏,否则通知玩家准备
        if (room.isAllReady() && room.getPlayerMap().size() >= NNConstant.NN_MIN_START_COUNT) {
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
     * 明牌抢庄开始游戏
     * @param room
     */
    public void startGameMp(final NNGameRoomNew room) {
        // 洗牌
        NiuNiuServer.xiPai(room.getRoomNo());
        // 发牌
        NiuNiuServer.faPai(room.getRoomNo());
        JSONArray gameProcessFP = new JSONArray();
        // 设置玩家手牌
        for (String uuid : room.getUserPacketMap().keySet()) {
            room.getUserPacketMap().get(uuid).saveMingPai();
            // 存放游戏记录
            JSONObject userPai = new JSONObject();
            userPai.put("account", uuid);
            userPai.put("name", room.getPlayerMap().get(uuid).getName());
            userPai.put("pai", room.getUserPacketMap().get(uuid).getMyPai());
            gameProcessFP.add(userPai);
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
                gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_QZ,0);
            }
        });
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
        // 初始化房间信息
        room.initGame();
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_MP) {
            // 明牌抢庄开始游戏
            startGameMp(room);
        } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_QZ) {
            // 设置房间状态(抢庄)
            room.setGameStatus(NNConstant.NN_GAME_STATUS_QZ);
        } else if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            // 通比模式开始游戏
            startGameTb(room);
        } else if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_FZ||room.getBankerType()==NNConstant.NN_BANKER_TYPE_ZZ){
            // 设置房间状态(下注)
            room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
            // 开启亮牌定时器
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_XZ,0);
                }
            });
        }
        // 是否抽水
        if (room.getFee() > 0) {
            JSONArray array = new JSONArray();
            for (String account : room.getPlayerMap().keySet()) {
                // 中途加入不抽水
                if (room.getUserPacketMap().get(account).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
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
            // 抽水
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));

        }
        // 通知前端状态改变
        changeGameStatus(room);
    }

    /**
     * 通比模式开始游戏
     * @param room
     */
    public void startGameTb(final NNGameRoomNew room) {
        // 洗牌
        NiuNiuServer.xiPai(room.getRoomNo());
        // 发牌
        NiuNiuServer.faPai(room.getRoomNo());
        // 设置房间状态(下注)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_XZ);
        changeGameStatus(room);
        for (String account : room.getUserPacketMap().keySet()) {
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
        // 设置游戏状态(亮牌)
        room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
        // 设置倒计时
        room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
        // 开启亮牌定时器
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_LP,0);
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
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 非抢庄、明牌抢庄抢庄消息不做处理
                if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_MP && room.getBankerType() != NNConstant.NN_BANKER_TYPE_QZ) {
                    return;
                }
                // 不是准备状态的玩家下注消息不作处理(包括中途加入和已经抢过庄的)
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_READY) {
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
                        if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                            JSONObject userQZ = new JSONObject();
                            userQZ.put("account", uuid);
                            userQZ.put("name", room.getPlayerMap().get(uuid).getName());
                            userQZ.put("qzTimes", room.getUserPacketMap().get(uuid).getQzTimes());
                            userQZ.put("banker", room.getPlayerMap().get(room.getBanker()).getName());
                            gameProcessQZ.add(userQZ);
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
                // 中途加入除外
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
                    // 所有有参与抢庄的玩家
                    if (room.getUserPacketMap().get(account).getQzTimes() > 0) {
                        qzList.add(account);
                    }
                    allList.add(account);
                }
            }
        } else {// 最高倍数为庄家
            for (String account : room.getUserPacketMap().keySet()) {
                // 中途加入除外
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_QZ) {
                    // 抢庄倍数大于最大倍数
                    if (room.getUserPacketMap().get(account).getQzTimes() >= maxBs) {
                        if (room.getUserPacketMap().get(account).getQzTimes()>maxBs) {
                            qzList.clear();
                        }
                        qzList.add(account);
                        maxBs = room.getUserPacketMap().get(account).getQzTimes();
                    }
                    allList.add(account);
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
                    room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_INIT);
                }
                JSONObject result = new JSONObject();
                result.put("type",CommonConstant.SHOW_MSG_TYPE_NORMAL);
                result.put(CommonConstant.RESULT_KEY_MSG,"无人抢庄重新开局");
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"tipMsgPush");

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
        if (room.getQzNoBanker() == NNConstant.NN_QZ_NO_BANKER_SJ&&sjCount == 0) {
            sjCount = allList.size();
        }
        // 多人抢庄才进行休眠
        final int sleepTime;
        if (sjCount>1) {
            sleepTime = sjCount*800;
        }else {
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
            if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
                // 庄家下注消息不作处理
                if (room.getBanker().equals(account)) {
                    return;
                }
                if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_MP) {
                    if (room.getUserPacketMap().get(account).getStatus()!=NNConstant.NN_USER_STATUS_QZ) {
                        return;
                    }
                }
                // 庄家抢庄倍数
                int qzTimes = 1;
                int baseNum = 4;
                // 抢庄
                if(room.getBankerType()==NNConstant.NN_BANKER_TYPE_QZ || room.getBankerType()==NNConstant.NN_BANKER_TYPE_MP) {
                    if (room.getUserPacketMap().get(room.getBankerType())!=null) {
                        qzTimes = room.getUserPacketMap().get(room.getBanker()).getQzTimes();
                    }
                }
                double myScore = room.getPlayerMap().get(account).getScore();
                if (myScore<baseNum*room.getScore()) {

                }
                room.getUserPacketMap().get(account).setStatus(NNConstant.NN_USER_STATUS_XZ);
                room.getUserPacketMap().get(account).setXzTimes(postData.getInt(NNConstant.DATA_KEY_MONEY));
                // 通知玩家
                JSONObject result = new JSONObject();
                result.put("index", room.getPlayerMap().get(account).getMyIndex());
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("value", room.getUserPacketMap().get(account).getXzTimes());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameXiaZhuPush_NN");
                // 所有人都完成下注
                if (room.isAllXiaZhu()) {
                    // 明牌抢庄已发牌
                    if (room.getBankerType() != NNConstant.NN_BANKER_TYPE_MP) {
                        // 洗牌
                        NiuNiuServer.xiPai(room.getRoomNo());
                        // 发牌
                        NiuNiuServer.faPai(room.getRoomNo());
                    }
                    // 设置游戏状态
                    room.setGameStatus(NNConstant.NN_GAME_STATUS_LP);
                    room.setTimeLeft(NNConstant.NN_TIMER_SHOW);
                    ThreadPoolHelper.executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            gameTimerNiuNiu.gameOverTime(room.getRoomNo(), NNConstant.NN_GAME_STATUS_LP,0);
                        }
                    });

                    // 存放游戏记录
                    JSONArray gameProcessXZ = new JSONArray();
                    for (String uuid : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT && !room.getBanker().equals(uuid)) {
                            JSONObject userXZ = new JSONObject();
                            userXZ.put("account", uuid);
                            userXZ.put("name", room.getPlayerMap().get(uuid).getName());
                            userXZ.put("xzTimes", room.getUserPacketMap().get(uuid).getXzTimes());
                            gameProcessXZ.add(userXZ);
                        }
                    }
                    room.getGameProcess().put("xiaZhu", gameProcessXZ);
                    // 通知玩家
                    changeGameStatus(room);
                }
            }
        }
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
        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            // 是否是庄家(不是庄家且不是下注状态的不做处理)
            if (!account.equals(room.getBanker())) {
                if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_XZ) {
                    return;
                }
            }else {
                if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_MP) {
                    if (room.getUserPacketMap().get(account).getStatus() != NNConstant.NN_USER_STATUS_QZ) {
                        return;
                    }
                }
            }
            // 配牛
            if (room.getBankerType()!=NNConstant.NN_BANKER_TYPE_TB) {
                peiNiu(roomNo, account);
            }else {
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
                // 存放游戏记录
                JSONArray gameProcessLP = new JSONArray();
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().get(uuid).getStatus() > NNConstant.NN_USER_STATUS_INIT) {
                        JSONObject userLP = new JSONObject();
                        userLP.put("account", uuid);
                        userLP.put("name", room.getPlayerMap().get(uuid).getName());
                        userLP.put("pai", room.getUserPacketMap().get(uuid).getMingPai());
                        gameProcessLP.add(userLP);
                    }
                }
                // 结算玩家输赢数据
                gameJieSuan(room);
                // 设置房间状态
                room.setGameStatus(NNConstant.NN_GAME_STATUS_JS);
                // 初始化倒计时
                room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                room.getGameProcess().put("showPai", gameProcessLP);
                // 设置玩家状态
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().get(uuid).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                        room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_JS);
                    }
                }
                // 通知玩家
                changeGameStatus(room);
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                    if (room.getGameStatus()==NNConstant.NN_GAME_STATUS_JS&&room.getGameIndex()==room.getGameCount()) {
                        room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
                    }
                }
                if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_ZZ) {
                    if (room.getPlayerMap().get(room.getBanker()).getScore()<room.getMinBankerScore()) {
                        // 庄家设为空
                        room.setBanker(null);
                        // 设置游戏状态
                        room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
                        // 初始化倒计时
                        room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                        // 重置玩家状态
                        for (String uuid : room.getUserPacketMap().keySet()) {
                            room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
                        }
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
        if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_TB) {
            niuNiuTongBi(room);
        } else {
            // 通杀
            boolean tongSha = true;
            // 通赔
            boolean tongPei = true;
            for (String account : room.getUserPacketMap().keySet()) {
                UserPacket bankerUp = room.getUserPacketMap().get(room.getBanker());
                // 有参与的玩家
                if (room.getUserPacketMap().get(account).getStatus()==NNConstant.NN_USER_STATUS_LP) {
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
                    if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                        if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()<0) {
                            RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
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
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            roomCardSumary(room.getRoomNo());
        }
        // 更新数据库
        updateScore(room.getRoomNo());
    }

    /**
     * 房卡场结算
     * @param roomNo
     */
    public void roomCardSumary(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_LP) {
                UserPacket up = room.getUserPacketMap().get(account);
                // 无牛次数+1
                if (up.getType()==0) {
                    up.setWuNiuTimes(up.getWuNiuTimes()+1);
                }
                // 牛牛次数+1
                if (up.getType()==10) {
                    up.setNiuNiuTimes(up.getNiuNiuTimes()+1);
                }
                // 胜利次数+1
                if (up.getScore()>0) {
                    up.setWinTimes(up.getWinTimes()+1);
                }
                if (account.equals(room.getBanker())) {
                    // 通杀次数+1
                    if (room.getTongSha()==1) {
                        up.setTongShaTimes(up.getTongShaTimes()+1);
                    }
                    // 通赔次数+1
                    if (room.getTongSha()==-1) {
                        up.setTongPeiTimes(up.getTongPeiTimes()+1);
                    }
                }
            }
        }
    }

    /**
     * 更新数据库
     * @param roomNo
     */
    public void updateScore(String roomNo) {
        NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray array = new JSONArray();
        JSONArray userDeductionData = new JSONArray();
        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        // 存放游戏记录
        JSONArray gameProcessJS = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            // 有参与的玩家
            if (room.getUserPacketMap().get(account).getStatus()==NNConstant.NN_USER_STATUS_LP) {
                JSONObject userJS = new JSONObject();
                userJS.put("account", account);
                userJS.put("name", room.getPlayerMap().get(account).getName());
                userJS.put("sum", room.getUserPacketMap().get(account).getScore());
                userJS.put("pai", room.getUserPacketMap().get(account).getSortPai());
                userJS.put("paiType", room.getUserPacketMap().get(account).getType());
                userJS.put("old", Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(),room.getUserPacketMap().get(account).getScore()));
                if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()<0) {
                    userJS.put("new", 0);
                }else {
                    userJS.put("new", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                }
                gameProcessJS.add(userJS);
                // 元宝输赢情况
                JSONObject obj = new JSONObject();
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                    obj.put("total", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                    obj.put("fen", room.getUserPacketMap().get(account).getScore());
                    obj.put("id", room.getPlayerMap().get(account).getId());
                    array.add(obj);
                }else if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK){
                    // 房主支付
                    if (room.getPayType()==CommonConstant.PAY_TYPE_OWNER) {
                        if (account.equals(room.getOwner())) {
                            // 参与第一局需要扣房卡
                            if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                                obj.put("total", room.getPlayerMap().get(account).getRoomCardNum());
                                obj.put("fen", -room.getPlayerCount()*room.getSinglePayNum());
                                obj.put("id", room.getPlayerMap().get(account).getId());
                                array.add(obj);
                            }
                        }
                    }
                    // 房费AA
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA) {
                        // 参与第一局需要扣房卡
                        if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                            obj.put("total", room.getPlayerMap().get(account).getRoomCardNum());
                            obj.put("fen", -room.getSinglePayNum());
                            obj.put("id", room.getPlayerMap().get(account).getId());
                            array.add(obj);
                        }
                    }
                }
                // 用户游戏记录
                JSONObject object = new JSONObject();
                object.put("id", room.getPlayerMap().get(account).getId());
                object.put("gid", room.getGid());
                object.put("roomNo", room.getRoomNo());
                object.put("type", room.getRoomType());
                object.put("fen", room.getUserPacketMap().get(account).getScore());
                object.put("old", Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(),room.getUserPacketMap().get(account).getScore()));
                if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()<0) {
                    object.put("new", 0);
                }else {
                    object.put("new", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                }
                userDeductionData.add(object);
                // 战绩记录
                JSONObject gameLogResult = new JSONObject();
                gameLogResult.put("account", account);
                gameLogResult.put("name", room.getPlayerMap().get(account).getName());
                gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
                if (room.getPlayerMap().containsKey(room.getBanker())&&room.getPlayerMap().get(room.getBanker())!=null) {
                    gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                }else {
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
                gameResult.add(userResult);
            }
        }
        room.getGameProcess().put("JieSuan", gameProcessJS);
        logger.info(room.getRoomNo()+"---"+String.valueOf(room.getGameProcess()));
        if (room.getId()==0) {
            JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
            if (!Dto.isObjNull(roomInfo)) {
                room.setId(roomInfo.getLong("id"));
            }
        }
        if (array.size()>0) {
            // 更新玩家分数
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
        // 金币场不计战绩
        if (room.getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
            // 战绩信息
            JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), room.getGameProcess().toString());
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
            JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
            for (int i = 0; i < userGameLogs.size(); i++) {
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
            }
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
        if (room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            boolean canExit = false;
            // 金币场、元宝场
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                // 未参与游戏可以自由退出
                if (room.getUserPacketMap().get(account).getStatus() == NNConstant.NN_USER_STATUS_INIT) {
                    canExit = true;
                } else if (room.getGameStatus() == NNConstant.NN_GAME_STATUS_INIT ||
                    room.getGameStatus() == NNConstant.NN_GAME_STATUS_READY ||
                    room.getGameStatus() == NNConstant.NN_GAME_STATUS_JS) {// 初始及准备阶段可以退出
                    canExit = true;
                }
            }else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                if (room.getUserPacketMap().get(account).getPlayTimes()==0) {
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA||!room.getOwner().equals(account)) {
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
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush_NN");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush_NN");
                }
                // 坐庄模式
                if (room.getBankerType() == NNConstant.NN_BANKER_TYPE_ZZ) {
                    // 房主退出且房间内有其他玩家
                    if (account.equals(room.getBanker())&&room.getUserPacketMap().size()>0) {
                        // 庄家设为空
                        room.setBanker(null);
                        // 设置游戏状态
                        room.setGameStatus(NNConstant.NN_GAME_STATUS_TO_BE_BANKER);
                        // 初始化倒计时
                        room.setTimeLeft(NNConstant.NN_TIMER_INIT);
                        // 重置玩家状态
                        for (String uuid : room.getUserPacketMap().keySet()) {
                            room.getUserPacketMap().get(uuid).setStatus(NNConstant.NN_USER_STATUS_INIT);
                        }
                        changeGameStatus(room);
                        return;
                    }
                }
                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (room.isAllReady() && room.getPlayerMap().size() >= NNConstant.NN_MIN_START_COUNT) {
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
        for (String account : room.getPlayerMap().keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("gameStatus", room.getGameStatus());
            if (Dto.stringIsNULL(room.getBanker())||(room.getGameStatus()<=NNConstant.NN_GAME_STATUS_DZ&&room.getBankerType()!=NNConstant.NN_BANKER_TYPE_ZZ)||
                room.getBankerType()==NNConstant.NN_BANKER_TYPE_TB||room.getPlayerMap().get(room.getBanker())==null) {
                obj.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                obj.put("qzScore", 0);
            }else {
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
            obj.put("gameData", room.getGameData(account));
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                if (room.getGameStatus()==NNConstant.NN_GAME_STATUS_ZJS) {
                    obj.put("jiesuanData", room.getFinalSummary());
                }
                if (room.getGameStatus()==NNConstant.NN_GAME_STATUS_JS&&room.getGameIndex()==room.getGameCount()) {
                    obj.put("jiesuanData", room.getFinalSummary());
                }
            }
            /**
             * 20180518 房主坐庄模式 wqm
             */
            obj.put("isBanker",CommonConstant.GLOBAL_NO);
            if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_ZZ) {
                if (room.getGameStatus()==NNConstant.NN_GAME_STATUS_TO_BE_BANKER||
                    (room.getGameStatus()==NNConstant.NN_GAME_STATUS_JS&&room.getPlayerMap().get(room.getBanker()).getScore()<room.getMinBankerScore())) {
                    obj.put("isBanker",CommonConstant.GLOBAL_YES);
                    obj.put("bankerMinScore",room.getMinBankerScore());
                    obj.put("bankerIsUse",CommonConstant.GLOBAL_YES);
                    if (room.getPlayerMap().get(account).getScore()<room.getMinBankerScore()) {
                        obj.put("bankerIsUse",CommonConstant.GLOBAL_NO);
                    }
                }
            }
            UUID uuid = room.getPlayerMap().get(account).getUuid();
            if (uuid != null) {
                CommonConstant.sendMsgEventToSingle(uuid, obj.toString(), "changeGameStatusPush_NN");
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
                        gameTimerNiuNiu.closeRoomOverTime(roomNo,room.getJieSanTime());
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
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush_NN");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (room.isAgreeClose()) {
                    // 未玩完一局不需要强制结算
                    if (room.getGameIndex()<=1&&room.getGameStatus()< NNConstant.NN_GAME_STATUS_JS) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"房间已被解散");
                        CommonConstant.sendMsgEventToAll(uuidList,result.toString(),"tipMsgPush");
                        return;
                    }
                    room.setGameStatus(NNConstant.NN_GAME_STATUS_ZJS);
                    changeGameStatus(room);
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
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
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
        if (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
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
     * @param room
     * @return
     */
    private String niuNiuTongBi(NNGameRoomNew room) {

        String winUUID = null;
        for (String win : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().get(win).getStatus()==NNConstant.NN_USER_STATUS_LP) {
                UserPacket winPacket = room.getUserPacketMap().get(win);
                UserPacket winner = new UserPacket(winPacket.getPs(), true, room.getSpecialType());
                // 设置牌型
                winPacket.setType(winner.getType());

                boolean isWin = true;
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if(!uuid.equals(win)&&room.getUserPacketMap().get(uuid).getStatus()==NNConstant.NN_USER_STATUS_LP){
                        UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), room.getSpecialType());
                        // 计算玩家输赢
                        PackerCompare.getWin(winner, userpacket);
                        // 输给其他玩家
                        if(!winner.isWin()){
                            isWin = false;
                            break;
                        }
                    }
                }
                // 通杀
                if(isWin){
                    double totalScore = 0;
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (!account.equals(win)&&room.getUserPacketMap().get(account).getStatus()==NNConstant.NN_USER_STATUS_LP) {
                            totalScore = Dto.add(totalScore,room.getScore());
                        }
                    }
                    // 设置当局输赢
                    room.getUserPacketMap().get(win).setScore(totalScore);
                    // 设置当前分数
                    double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).getScore();
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).setScore(Dto.add(oldScore, totalScore));
                    winUUID = win;
                    winPacket.setWin(true);
                }else{
                    // 设置当局输赢
                    room.getUserPacketMap().get(win).setScore(-room.getScore());
                    // 设置当前分数
                    double oldScore = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).getScore();
                    RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(win).setScore(Dto.sub(oldScore, room.getScore()));
                    winPacket.setWin(false);
                }
            }
        }
        return winUUID;
    }
}
