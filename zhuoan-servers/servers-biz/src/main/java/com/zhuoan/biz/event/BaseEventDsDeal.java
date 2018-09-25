package com.zhuoan.biz.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.ddz.DdzGameEventDsDeal;
import com.zhuoan.biz.game.biz.*;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.*;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MathDelUtil;
import com.zhuoan.util.SensitivewordFilter;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

import static net.sf.json.JSONObject.fromObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:03 2018/4/17
 * @Modified By:
 **/
@Component
public class BaseEventDsDeal extends SuperEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(BaseEventDsDeal.class);

    @Resource
    private UserBiz userBiz;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private PublicBiz publicBiz;

    @Resource
    private AchievementBiz achievementBiz;

    @Resource
    private PropsBiz propsBiz;

    @Resource
    private DdzGameEventDsDeal ddzGameEventDsDeal;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RedisService redisService;

    @Resource
    private RobotEventDeal robotEventDeal;

    /**
     * 创建房间判断是否满足条件
     *
     * @param client
     * @param data
     */
    public void createRoomBase(SocketIOClient client, Object data) {
        // 检查是否能加入房间
        JSONObject postData = fromObject(data);
        // 玩家账号
        String account = postData.getString("account");
        // 房间信息
        JSONObject baseInfo = postData.getJSONObject("base_info");
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)) {
            // 用户不存在
            sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "用户不存在", "enterRoomPush_NN");
            return;
        } else if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid")) || !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "账号已在其他地方登陆", "enterRoomPush_NN");
            return;
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")) {
            if (userInfo.getDouble("coins") < baseInfo.getDouble("goldCoinEnter")) {
                // 金币不足
                sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "金币不足", "enterRoomPush_NN");
                return;
            } else if (baseInfo.getDouble("goldCoinEnter") != baseInfo.getDouble("goldCoinLeave") &&
                userInfo.getDouble("coins") > baseInfo.getDouble("goldCoinLeave")) {
                // 金币过多
                sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "金币超该场次限制", "enterRoomPush_NN");
                return;
            }
        }
        // 创建房间
        createRoomBase(client, postData, userInfo);
    }

    /**
     * 创建房间创建实体对象
     *
     * @param client
     * @param postData
     * @param userInfo
     */
    public void createRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        JSONObject baseInfo = postData.getJSONObject("base_info");
        // 添加房间信息
        String roomNo = randomRoomNo();
        GameRoom gameRoom = createRoomByGameId(postData.getInt("gid"), baseInfo, userInfo);
        // 设置房间类型
        gameRoom.setRoomType(baseInfo.getInt("roomType"));
        // 游戏id
        gameRoom.setGid(postData.getInt("gid"));
        // 端口
        if (postData.containsKey("port")) {
            gameRoom.setPort(postData.getInt("port"));
        }
        // IP
        if (postData.containsKey("ip")) {
            gameRoom.setIp(postData.getString("ip"));
        }
        // 比赛场编号设置
        if (postData.containsKey("match_num") && gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
            gameRoom.setMatchNum(postData.getString("match_num"));
        }
        // 房间号
        gameRoom.setRoomNo(roomNo);

        gameRoom.setRoomInfo(baseInfo);
        gameRoom.setCreateTime(TimeUtil.getNowDate());
        int playerNum = baseInfo.getInt("player");
        List<Long> idList = new ArrayList<Long>();
        for (int i = 0; i < playerNum; i++) {
            if (i == 0) {
                idList.add(userInfo.getLong("id"));
            } else {
                idList.add(0L);
            }
        }
        gameRoom.setUserIdList(idList);
        // 房间资金类型
        gameRoom.setCurrencyType(gameRoom.getUpdateType());
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            gameRoom.setGameCount(9999);
        }
        // 底分
        if (baseInfo.containsKey("di")) {
            gameRoom.setScore(baseInfo.getDouble("di"));
        } else if (gameRoom.getGid() == CommonConstant.GAME_ID_DDZ) {
            if (baseInfo.containsKey("baseNum")) {
                gameRoom.setScore(baseInfo.getInt("baseNum"));
            }
        } else {
            gameRoom.setScore(1);
        }
        // 机器人
        if (baseInfo.containsKey("robot") && baseInfo.getInt("robot") == CommonConstant.GLOBAL_YES) {
            // 机器人
            gameRoom.setRobot(true);
        } else {
            gameRoom.setRobot(false);
        }
        //设置金币场准入金币
        if (baseInfo.containsKey("goldCoinEnter")) {
            gameRoom.setEnterScore(baseInfo.getInt("goldCoinEnter"));
        }
        //设置金币场准入金币
        if (baseInfo.containsKey("goldCoinLeave")) {
            gameRoom.setLeaveScore(baseInfo.getInt("goldCoinLeave"));
        }
        // 是否陌生人可见
        if (baseInfo.containsKey("open") && baseInfo.getInt("open") == 1) {
            gameRoom.setOpen(true);
        } else {
            gameRoom.setOpen(false);
        }
        // 房卡场不同平台无法加入  20180828 wqm
        if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FK || baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_CLUB) {
            if (userInfo.containsKey("platform")) {
                gameRoom.setPlatform(userInfo.getString("platform"));
            }
        }
        // 玩家人数
        gameRoom.setPlayerCount(playerNum);
        // 金币、元宝扣服务费
        if (baseInfo.containsKey("fee")) {
            gameRoom.setFee(baseInfo.getDouble("fee"));
        }
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo", userInfo);
        obtainPlayerInfoData.put("myIndex", 0);
        if (client != null) {
            obtainPlayerInfoData.put("uuid", String.valueOf(client.getSessionId()));
        } else {
            obtainPlayerInfoData.put("uuid", String.valueOf(UUID.randomUUID()));
        }
        obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location", postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        roomSerializable(gameRoom);
        // 通知玩家
        informUser(gameRoom, playerinfo, client);
        // 组织数据，插入数据库
        addGameRoom(gameRoom, playerinfo);
        // 开启机器人
        if (gameRoom.isRobot() && gameRoom.getRoomType() != CommonConstant.ROOM_TYPE_MATCH) {
            robotEventDeal.robotJoin(roomNo);
        }
    }

    /**
     * 获取房间实体对象
     *
     * @param gameId
     * @param baseInfo
     * @param userInfo
     * @return
     */
    public GameRoom createRoomByGameId(int gameId, JSONObject baseInfo, JSONObject userInfo) {
        GameRoom gameRoom;
        switch (gameId) {
            case CommonConstant.GAME_ID_DDZ:
                gameRoom = new DdzGameRoom();
                createRoomDdz((DdzGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
                break;
            default:
                gameRoom = new GameRoom();
                break;
        }
        return gameRoom;
    }

    /**
     * 通知玩家
     */
    public void informUser(GameRoom gameRoom, Playerinfo playerinfo, SocketIOClient client) {
        JSONObject object = new JSONObject();
        object.put(CommonConstant.DATA_KEY_ACCOUNT, playerinfo.getAccount());
        object.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
        switch (gameRoom.getGid()) {
            case CommonConstant.GAME_ID_DDZ:
                ddzGameEventDsDeal.createRoom(client, object);
                break;
            default:
                break;
        }
    }

    /**
     * 插入房间数据
     *
     * @param gameRoom
     * @param playerinfo
     */
    public void addGameRoom(GameRoom gameRoom, Playerinfo playerinfo) {
        JSONObject obj = new JSONObject();
        obj.put("game_id", gameRoom.getGid());
        obj.put("room_no", gameRoom.getRoomNo());
        obj.put("roomtype", gameRoom.getRoomType());
        obj.put("base_info", gameRoom.getRoomInfo());
        obj.put("createtime", TimeUtil.getNowDate());
        obj.put("game_count", gameRoom.getGameCount());
        obj.put("user_id0", playerinfo.getId());
        obj.put("user_icon0", playerinfo.getHeadimg());
        obj.put("user_name0", playerinfo.getName());
        obj.put("ip", gameRoom.getIp());
        obj.put("port", gameRoom.getPort());
        obj.put("status", 0);
        if (gameRoom.isOpen()) {
            obj.put("open", CommonConstant.GLOBAL_YES);
        } else {
            obj.put("open", CommonConstant.GLOBAL_NO);
        }
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_ROOM, obj));
    }

    /**
     * 生成随机房间号
     *
     * @return
     */
    public String randomRoomNo() {
        String roomNo = MathDelUtil.getRandomStr(6);
        GameRoom gameRoom = roomDeserializable(roomNo);
        if (gameRoom != null) {
            return randomRoomNo();
        }
        return roomNo;
    }

    /**
     * 加入房间判断是否满足条件
     *
     * @param client
     * @param data
     */
    public void joinRoomBase(SocketIOClient client, Object data) {
        JSONObject postData = fromObject(data);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GameRoom room = roomDeserializable(roomNo);
        // 房间不存在
        if (room == null) {
            sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "房间不存在", "enterRoomPush_NN");
            return;
        }
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)) {
            // 用户不存在
            sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "用户不存在", "enterRoomPush_NN");
            return;
        }
        if (client != null && room.getRoomType() != CommonConstant.ROOM_TYPE_MATCH) {
            if (!userInfo.containsKey("uuid") || Dto.stringIsNULL(userInfo.getString("uuid")) ||
                !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
                return;
            }
        }
        // 重连不需要再次检查玩家积分
        if (!room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")) {
                if (userInfo.getDouble("coins") < room.getEnterScore()) {
                    // 金币不足
                    sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "金币不足", "enterRoomPush_NN");
                    return;
                } else if (room.getEnterScore() != room.getLeaveScore() && userInfo.getDouble("coins") > room.getLeaveScore()) {
                    // 金币过多
                    sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "金币超出该场次限定", "enterRoomPush_NN");
                    return;
                }
            }
        }
        joinRoomBase(client, postData, userInfo);
    }

    /**
     * 加入房间创建实体对象
     *
     * @param client
     * @param postData
     * @param userInfo
     */
    public void joinRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 所有房间对象集合
        Map<Object, Object> allRoomMap = redisService.hmget(ROOM_KEY);
        for (Object roomNum : allRoomMap.keySet()) {
            if (!allRoomMap.get(roomNum).equals(roomNo)) {
                GameRoom room = roomDeserializable(String.valueOf(roomNum));
                if (room != null && room.getPlayerMap().containsKey(userInfo.getString("account"))) {
                    return;
                }
            }
        }
        GameRoom gameRoom = roomDeserializable(String.valueOf(roomNo));
        int myIndex = -1;
        // 获取当前房间的第一个空位
        int begin = 0;
        for (int i = begin; i < gameRoom.getUserIdList().size(); i++) {
            if (gameRoom.getUserIdList().get(i) == 0 || gameRoom.getUserIdList().get(i) == userInfo.getLong("id")) {
                myIndex = i;
                break;
            }
        }
        // 重连不需要重新获取座位号
        if (postData.containsKey("myIndex")) {
            myIndex = postData.getInt("myIndex");
        }
        if (myIndex < 0) {
            sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "房间已满", "enterRoomPush_NN");
            return;
        }
        // 设置房间属性
        if (myIndex < gameRoom.getUserIdList().size()) {
            gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
        }
        // 添加机器人
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && userInfo.containsKey("openid") && "0".equals(userInfo.getString("openid"))) {
            gameRoom.getRobotList().add(userInfo.getString("account"));
            robotEventDeal.addRobotInfo(userInfo.getString("account"), roomNo, gameRoom.getGid());
        }
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo", userInfo);
        obtainPlayerInfoData.put("myIndex", myIndex);
        if (client != null) {
            obtainPlayerInfoData.put("uuid", String.valueOf(client.getSessionId()));
        } else {
            obtainPlayerInfoData.put("uuid", String.valueOf(UUID.randomUUID()));
        }
        obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location", postData.getString("location"));
        }
        // 设置排名
        if (postData.containsKey("my_rank")) {
            obtainPlayerInfoData.put("my_rank", postData.getInt("my_rank"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        // 组织数据，加入房间
        JSONObject joinData = new JSONObject();
        joinData.put(CommonConstant.DATA_KEY_ACCOUNT, userInfo.getString("account"));
        joinData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
        // 是否重连
        if (gameRoom.getPlayerMap().containsKey(playerinfo.getAccount())) {
            if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_MATCH) {
                playerinfo.setMyRank(gameRoom.getPlayerMap().get(playerinfo.getAccount()).getMyRank());
            }
            joinData.put("isReconnect", CommonConstant.GLOBAL_YES);
        } else {
            // 更新数据库
            if (myIndex < 10 && gameRoom.getRoomType() != CommonConstant.ROOM_TYPE_MATCH) {
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no", gameRoom.getRoomNo());
                roomInfo.put("user_id" + myIndex, playerinfo.getId());
                roomInfo.put("user_icon" + myIndex, playerinfo.getHeadimg());
                roomInfo.put("user_name" + myIndex, playerinfo.getName());
                // 更新房间信息
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            }
            joinData.put("isReconnect", CommonConstant.GLOBAL_NO);
        }
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        // 添加用户牌局信息
        addUserPacket(userInfo.getString("account"), gameRoom);
        // 更新缓存
        roomSerializable(gameRoom);
        // 通知玩家
        switch (gameRoom.getGid()) {
            case CommonConstant.GAME_ID_DDZ:
                ddzGameEventDsDeal.joinRoom(client, joinData);
                break;
            default:
                break;
        }
    }

    /**
     * 添加用户牌局信息
     * @param account
     * @param gameRoom
     */
    private void addUserPacket(String account, GameRoom gameRoom) {
        if (gameRoom instanceof DdzGameRoom) {
            // 重连不需要添加
            if (!((DdzGameRoom) gameRoom).getUserPacketMap().containsKey(account)) {
                UserPacketDdz up = new UserPacketDdz();
                // 设置连胜局数
                Object winTimeInfo = redisService.hget("win_time_info_" + gameRoom.getScore(), account);
                if (!Dto.isNull(winTimeInfo)) {
                    up.setWinStreakTime(Integer.parseInt(String.valueOf(winTimeInfo)));
                }
                ((DdzGameRoom) gameRoom).getUserPacketMap().put(account, up);
            }
        }
    }

    /**
     * 获取玩家信息
     *
     * @param data
     * @return
     */
    public Playerinfo obtainPlayerInfo(JSONObject data) {
        // 用户信息
        JSONObject userInfo = data.getJSONObject("userInfo");
        // 座位号
        int myIndex = data.getInt("myIndex");
        // uuid
        UUID uuid = UUID.fromString(data.getString("uuid"));
        // 房间类型
        int roomType = data.getInt("room_type");
        Playerinfo playerinfo = new Playerinfo();
        playerinfo.setUuid(uuid);
        playerinfo.setMyIndex(myIndex);
        if (roomType == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")) {
            // 金币模式
            playerinfo.setScore(userInfo.getDouble("coins"));
        }
        // id
        playerinfo.setId(userInfo.getLong("id"));
        // account
        playerinfo.setAccount(userInfo.getString("account"));
        // 昵称
        if (userInfo.containsKey("name")) {
            playerinfo.setName(userInfo.getString("name"));
        } else {
            playerinfo.setName("");
        }
        // 头像
        if (userInfo.containsKey("headimg")) {
            playerinfo.setHeadimg(userInfo.getString("headimg"));
        } else {
            playerinfo.setHeadimg("");
        }
        // 性别
        if (userInfo.containsKey("sex")) {
            playerinfo.setSex(userInfo.getString("sex"));
        } else {
            playerinfo.setSex("");
        }
        // ip
        if (userInfo.containsKey("ip")) {
            playerinfo.setIp(userInfo.getString("ip"));
        } else {
            playerinfo.setIp("");
        }
        // 个性签名
        if (userInfo.containsKey("sign")) {
            playerinfo.setSignature(userInfo.getString("sign"));
        } else {
            playerinfo.setSignature("");
        }
        // 地区
        if (userInfo.containsKey("area")) {
            playerinfo.setArea(userInfo.getString("area"));
        } else {
            playerinfo.setArea("");
        }
        // 平台标识
        if (userInfo.containsKey("platform")) {
            playerinfo.setPlatform(userInfo.getString("platform"));
        } else {
            playerinfo.setArea("");
        }
        // 在线状态
        playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
        // 坐标
        if (data.containsKey("location")) {
            playerinfo.setLocation(data.getString("location"));
        }
        // 排名
        if (data.containsKey("my_rank")) {
            playerinfo.setMyRank(data.getInt("my_rank"));
        }
        // 幸运值
        if (userInfo.containsKey("luck")) {
            playerinfo.setLuck(userInfo.getInt("luck"));
        }
        // openID
        if (userInfo.containsKey("openid")) {
            playerinfo.setOpenId(userInfo.getString("openid"));
        }
        return playerinfo;
    }

    /**
     * 斗地主
     *
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomDdz(DdzGameRoom room, JSONObject baseInfo, String account) {
        room.setWfType("斗地主");
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        // 添加牌局信息
        if (baseInfo.getInt("roomType") != CommonConstant.ROOM_TYPE_DK) {
            addUserPacket(account,room);
        }
        JSONObject setting = getGameInfoById(CommonConstant.GAME_ID_DDZ);
        if (baseInfo.containsKey("win_streak")) {
            JSONArray winStreakArray = setting.getJSONArray("win_streak_array");
            for (Object winStreak : winStreakArray) {
                JSONObject winStreakObj = JSONObject.fromObject(winStreak);
                if (baseInfo.getInt("di") == winStreakObj.getInt("di")) {
                    room.setWinStreakObj(winStreakObj);
                    break;
                }
            }
        }
        // 设置房间信息
        room.setSetting(setting);
        // 最大倍数
        if (baseInfo.containsKey("multiple")) {
            room.setMaxMultiple(baseInfo.getInt("multiple"));
        }
    }


    /**
     * 根据游戏id获取游戏配置
     *
     * @param gameId
     * @return
     */
    private JSONObject getGameInfoById(int gameId) {
        JSONObject gameInfoById;
        try {
            StringBuffer key = new StringBuffer();
            key.append(CacheKeyConstant.GAME_INFO_BY_ID);
            key.append("_");
            key.append(gameId);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                gameInfoById = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(key)));

            } else {
                gameInfoById = roomBiz.getGameInfoByID(gameId).getJSONObject("setting");
                redisService.insertKey(String.valueOf(key), String.valueOf(gameInfoById), null);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
            gameInfoById = roomBiz.getGameInfoByID(gameId).getJSONObject("setting");
        }
        return gameInfoById;
    }

    /**
     * 获取房间设置
     *
     * @param client
     * @param data
     */
    public void getGameSetting(SocketIOClient client, Object data) {
    }

    /**
     * 获取游戏状态信息
     *
     * @param gameId
     * @return
     */
    private JSONObject getGameStatusInfo(int gameId) {
        JSONObject gameInfoById;
        try {
            StringBuffer key = new StringBuffer();
            key.append("game_on_or_off_");
            key.append(gameId);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                gameInfoById = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(key)));

            } else {
                gameInfoById = roomBiz.getGameInfoByID(gameId);
                redisService.insertKey(String.valueOf(key), String.valueOf(gameInfoById), null);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
            gameInfoById = roomBiz.getGameInfoByID(gameId);
        }
        return gameInfoById;
    }

    /**
     * 检查用户是否在房间内
     *
     * @param client
     * @param data
     */
    public void checkUser(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("gid") && postData.getInt("gid") > 0) {
            int gameId = postData.getInt("gid");
            JSONObject gameStatusInfo = getGameStatusInfo(gameId);
            if (Dto.isObjNull(gameStatusInfo) || !gameStatusInfo.containsKey("status") ||
                gameStatusInfo.getInt("status") != 1) {
                sendPromptToSinglePlayer(client, CommonConstant.GLOBAL_NO, "敬请期待", "enterRoomPush_NN");
                return;
            }
        }
        if (postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
            String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
            // 遍历房间列表
            Map<Object, Object> allRoomMap = redisService.hmget(ROOM_KEY);
            for (Object roomNum : allRoomMap.keySet()) {
                GameRoom room = roomDeserializable(String.valueOf(roomNum));
                if (room != null && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                    postData.put(CommonConstant.DATA_KEY_ROOM_NO, String.valueOf(roomNum));
                    postData.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
                    joinRoomBase(client, postData);
                    return;
                }
            }
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "checkUserPush");
        }
    }

    /**
     * 获取玩家信息
     *
     * @param client
     * @param data
     */
    public void getUserInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
            return;
        }
        JSONObject result = new JSONObject();
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            userInfo.remove("uuid");
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("user", userInfo);
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUserInfoPush");
    }

    /**
     * 获取房间列表
     *
     * @param client
     * @param data
     */
    public void getAllRoomList(SocketIOClient client, Object data) {}

    /**
     * 获取战绩记录
     *
     * @param client
     * @param data
     */
    public void getUserGameLogs(SocketIOClient client, Object data) {}

    /**
     * 解散房间
     *
     * @param client
     * @param data
     */
    public void dissolveRoom(SocketIOClient client, Object data) {}

    /**
     * 开关游戏
     *
     * @param client
     * @param data
     */
    public void onOrOffGame(SocketIOClient client, Object data) {}

    /**
     * 发送滚动公告
     *
     * @param client
     * @param data
     */
    public void sendNotice(SocketIOClient client, Object data) {}

    /**
     * 获取滚动公告
     *
     * @param client
     * @param data
     */
    public void getNotice(SocketIOClient client, Object data) {}

    /**
     * 获取洗牌信息
     *
     * @param client
     * @param data
     */
    public void getShuffleInfo(SocketIOClient client, Object data) {}

    /**
     * 洗牌
     *
     * @param client
     * @param data
     */
    public void doShuffle(SocketIOClient client, Object data) {}

    /**
     * 发送消息
     *
     * @param client
     * @param data
     */
    public void sendMessage(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int type = postData.getInt("type");
        String content = postData.getString("data");
        GameRoom room = roomDeserializable(roomNo);
        if (room != null) {
            //敏感词替代
            String backData = SensitivewordFilter.replaceSensitiveWord(content, 1, "*");
            JSONObject result = new JSONObject();
            result.put("user", room.getPlayerMap().get(account).getMyIndex());
            result.put("type", type);
            result.put("data", backData);
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "sendMsgEventPush");
        }
    }

    /**
     * 发送语音
     *
     * @param client
     * @param data
     */
    public void sendVoice(SocketIOClient client, Object data) {}

    /**
     * 子游戏接口
     *
     * @param client
     * @param data
     */
    public void getRoomGid(SocketIOClient client, Object data) {}

    /**
     * 获取房卡场支付信息
     *
     * @param client
     * @param data
     */
    public void getRoomCardPayInfo(SocketIOClient client, Object data) {}

    /**
     * 金币场加入房间
     *
     * @param client
     * @param data
     */
    public void joinCoinRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (Dto.stringIsNULL(account)) {
            return;
        }
        JSONObject option = new JSONObject();
        if (postData.containsKey("option")) {
            option = postData.getJSONObject("option");
        } else {
            JSONObject object = new JSONObject();
            object.put("gameId", gameId);
            object.put("platform", postData.getString("platform"));
            JSONArray array = getGoldSettingByGameIdAndPlatform(object);
            JSONObject userInfo = userBiz.getUserByAccount(account);
            if (!Dto.isObjNull(userInfo) && userInfo.containsKey("coins")) {
                int userCoins = userInfo.getInt("coins");
                // 取符合场次要求的场次信息
                for (int i = array.size() - 1; i >= 0; i--) {
                    JSONObject obj = array.getJSONObject(i).getJSONObject("option");
                    if (obj.getInt("goldCoinEnter") < userCoins && obj.getInt("goldCoinLeave") >= userCoins) {
                        option = obj;
                        break;
                    } else if (obj.getInt("goldCoinEnter") == obj.getInt("goldCoinLeave") && obj.getInt("goldCoinEnter") < userCoins) {
                        option = obj;
                        break;
                    }
                }
                if (Dto.isObjNull(option) && array.size() > 0) {
                    option = array.getJSONObject(0).getJSONObject("option");
                }
            }
        }
        postData.put("base_info", option);
        if (Dto.isObjNull(option)) {
            return;
        }
        // 比赛场判断
        Object playerSignUpInfo = redisService.hget("player_sign_up_info" + MatchConstant.MATCH_TYPE_COUNT, account);
        if (playerSignUpInfo != null) {
            sendPromptToSinglePlayer(client,CommonConstant.GLOBAL_NO,"已经报名比赛场","enterRoomPush_NN");
            return;
        }
        List<String> roomNoList = new ArrayList<>();
        // 所有房间对象集合
        Map<Object, Object> allRoomMap = redisService.hmget(ROOM_KEY);
        for (Object roomNo : allRoomMap.keySet()) {
            GameRoom room = roomDeserializable(String.valueOf(roomNo));
            if (room != null && room.getRoomType() == CommonConstant.ROOM_TYPE_JB && room.getGid() == gameId &&
                room.getScore() == option.getDouble("di") && !room.getPlayerMap().containsKey(account) &&
                room.getPlayerMap().size() < room.getPlayerCount()) {
                roomNoList.add(String.valueOf(roomNo));
            }
        }

        if (roomNoList.size() <= 0) {
            createRoomBase(client, postData);
        } else {
            // 随机加入
            Collections.shuffle(roomNoList);
            postData.put("room_no", roomNoList.get(0));
            joinRoomBase(client, postData);
        }
    }

    /**
     * 金币场房间设置缓存
     *
     * @param obj
     * @return
     */
    private JSONArray getGoldSettingByGameIdAndPlatform(JSONObject obj) {
        JSONArray goldSettings;
        StringBuffer sb = new StringBuffer();
        sb.append("gold_setting_");
        sb.append(obj.getString("platform"));
        sb.append("_");
        sb.append(obj.getInt("gameId"));
        try {
            Object object = redisService.queryValueByKey(String.valueOf(sb));
            if (object != null) {
                goldSettings = JSONArray.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
            } else {
                goldSettings = publicBiz.getGoldSetting(obj);
                redisService.insertKey(String.valueOf(sb), String.valueOf(goldSettings), null);
            }
        } catch (Exception e) {
            goldSettings = publicBiz.getGoldSetting(obj);
            logger.error("请启动REmote DIctionary Server");
        }
        return goldSettings;
    }

    /**
     * 获取金币场房间设置
     *
     * @param client
     * @param data
     */
    public void getCoinSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String platform = postData.getString("platform");
        JSONObject obj = new JSONObject();
        obj.put("gameId", gameId);
        obj.put("platform", platform);
        JSONArray goldSettings = getGoldSettingByGameIdAndPlatform(obj);
        for (int i = 0; i < goldSettings.size(); i++) {
            JSONObject goldSetting = goldSettings.getJSONObject(i);
            goldSetting.put("online", goldSetting.getInt("online") + RandomUtils.nextInt(goldSetting.getInt("online")));
            goldSetting.put("enter", goldSetting.getJSONObject("option").getInt("goldCoinEnter"));
            goldSetting.put("leave", goldSetting.getJSONObject("option").getInt("goldCoinLeave"));
        }
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        result.put("data", goldSettings);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getGameGoldSettingPush");
    }

    /**
     * 用户签到信息
     *
     * @param client
     * @param data
     */
    public void checkSignIn(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        long userId = postData.getLong("userId");
        String platform = postData.getString("platform");
        int type = CommonConstant.SIGN_INFO_EVENT_TYPE_HALL;
        if (postData.containsKey("type")) {
            type = postData.getInt("type");
        }
        // 当前日期
        String nowTime = TimeUtil.getNowDateymd() + " 00:00:00";
        JSONObject signInfo = publicBiz.getUserSignInfo(platform, userId);
        JSONObject result = new JSONObject();
        int minReward = getCoinsSignMinReward(platform);
        int maxReward = getCoinsSignMaxReward(platform);
        int baseReward = getCoinsSignBaseReward(platform);
        if (Dto.isObjNull(signInfo)) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("reward", baseReward + minReward);
            result.put("days", 0);
            result.put("isSign", CommonConstant.GLOBAL_NO);
        } else {
            if (!TimeUtil.isLatter(signInfo.getString("createtime"), nowTime)) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("isSign", CommonConstant.GLOBAL_NO);
                String yesterday = TimeUtil.addDaysBaseOnNowTime(nowTime, -1, "yyyy-MM-dd HH:mm:ss");
                if (TimeUtil.isLatter(signInfo.getString("createtime"), yesterday)) {
                    int signDay = signInfo.getInt("singnum") + 1;
                    // 一周签到模式
                    if (CommonConstant.weekSignPlatformList.contains(platform) && signDay >= 8) {
                        signDay = 1;
                    }
                    int reward = baseReward + signDay * minReward;
                    if (reward > maxReward) {
                        reward = maxReward;
                    }
                    result.put("reward", reward);
                    result.put("days", signDay - 1);
                } else {
                    result.put("reward", baseReward + minReward);
                    result.put("days", 0);
                }
            } else {
                // 已经签到过了判断是否是用户主动请求
                if (type == CommonConstant.SIGN_INFO_EVENT_TYPE_CLICK) {
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("days", signInfo.getInt("singnum") - 1);
                    result.put("isSign", CommonConstant.GLOBAL_YES);
                } else {
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                }
            }
        }
        // 签到奖励数组
        JSONArray array = new JSONArray();
        for (int i = 1; i <= 7; i++) {
            int reward = baseReward + minReward * i;
            if (reward > maxReward) {
                reward = maxReward;
            }
            array.add(reward);
        }
        result.put("array", array);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "checkSignInPush");
    }

    /**
     * 最少签到金币
     *
     * @return
     */
    public int getCoinsSignMinReward(String platform) {
        int minReward = CommonConstant.COINS_SIGN_MIN;
        JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
        if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_min")) {
            return signRewardInfo.getInt("signin_min");
        }
        return minReward;
    }

    /**
     * 最多签到金币
     *
     * @return
     */
    public int getCoinsSignMaxReward(String platform) {
        int maxReward = CommonConstant.COINS_SIGN_MAX;
        JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
        if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_max")) {
            return signRewardInfo.getInt("signin_max");
        }
        return maxReward;
    }

    /**
     * 签到金币基数
     *
     * @return
     */
    private int getCoinsSignBaseReward(String platform) {
        int maxReward = CommonConstant.COINS_SIGN_BASE;
        JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
        if (!Dto.isObjNull(signRewardInfo) && signRewardInfo.containsKey("signin_base")) {
            return signRewardInfo.getInt("signin_base");
        }
        return maxReward;
    }

    /**
     * 获取签到奖励缓存
     *
     * @param platform
     * @return
     */
    private JSONObject getCoinsSignRewardInfo(String platform) {
        JSONObject signRewardInfo;
        StringBuffer sb = new StringBuffer();
        sb.append("sign_reward_info_");
        sb.append(platform);
        try {
            Object object = redisService.queryValueByKey(String.valueOf(sb));
            if (object != null) {
                signRewardInfo = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
            } else {
                signRewardInfo = publicBiz.getSignRewardInfoByPlatform(platform);
                redisService.insertKey(String.valueOf(sb), String.valueOf(signRewardInfo), null);
            }
        } catch (Exception e) {
            signRewardInfo = publicBiz.getSignRewardInfoByPlatform(platform);
            logger.error("请启动REmote DIctionary Server");
        }
        return signRewardInfo;
    }

    /**
     * 用户签到
     *
     * @param client
     * @param data
     */
    public void doUserSignIn(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        long userId = postData.getLong("userId");
        String platform = postData.getString("platform");
        String account = postData.getString("account");
        // 当前日期
        String nowTime = TimeUtil.getNowDate();
        // 签到信息
        JSONObject signInfo = publicBiz.getUserSignInfo(platform, userId);
        JSONObject object = new JSONObject();
        JSONObject result = new JSONObject();
        int reward = getCoinsSignBaseReward(platform) + getCoinsSignMinReward(platform);
        if (Dto.isObjNull(signInfo)) {
            object.put("singnum", 1);
            object.put("createtime", nowTime);
            object.put("userID", userId);
            object.put("platform", platform);
        } else {
            object.put("id", signInfo.getLong("id"));
            String today = TimeUtil.getNowDateymd() + " 00:00:00";
            String yesterday = TimeUtil.addDaysBaseOnNowTime(today, -1, "yyyy-MM-dd HH:mm:ss");
            // 今日已签到
            if (TimeUtil.isLatter(signInfo.getString("createtime"), today)) {
                result.put(CommonConstant.RESULT_KEY_CODE, -1);
                result.put(CommonConstant.RESULT_KEY_MSG, "今日已签到");
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userSignInPush");
                return;
            }
            if (TimeUtil.isLatter(signInfo.getString("createtime"), yesterday)) {
                int signDay = signInfo.getInt("singnum") + 1;
                // 一周签到模式
                if (CommonConstant.weekSignPlatformList.contains(platform) && signDay >= 8) {
                    signDay = 1;
                }
                object.put("singnum", signDay);
                reward = getCoinsSignBaseReward(platform) + (signDay) * getCoinsSignMinReward(platform);
                int maxReward = getCoinsSignMaxReward(platform);
                if (reward > maxReward) {
                    reward = maxReward;
                }
            } else {
                object.put("singnum", 1);
            }
            object.put("createtime", nowTime);
        }
        if (!Dto.isObjNull(object)) {
            int back = publicBiz.addOrUpdateUserSign(object);
            JSONObject userInfo = userBiz.getUserByAccount(account);
            if (back > 0 && !Dto.isObjNull(userInfo)) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("newScore", userInfo.getInt("coins") + reward);
                result.put("days", object.getInt("singnum"));
                JSONObject obj = new JSONObject();
                obj.put("account", account);
                obj.put("updateType", "coins");
                obj.put("sum", reward);
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_USER_INFO, obj));
            } else {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userSignInPush");
    }

    /**
     * 获取当前房间数和游戏中玩家数量
     *
     * @param client
     * @param data
     */
    public void getRoomAndPlayerCount(SocketIOClient client, Object data) {}

    /**
     * 获取竞技场信息
     *
     * @param client
     * @param data
     */
    public void getCompetitiveInfo(SocketIOClient client, Object data) {}

    /**
     * 竞技场加入房间
     *
     * @param client
     * @param data
     */
    public void joinCompetitiveRoom(SocketIOClient client, Object data) {}

    /**
     * ip检测
     * @param client
     * @param data
     */
    public void gameCheckIp(SocketIOClient client, Object data) {}

    /**
     * 获取代开房间列表
     *
     * @param client
     * @param data
     */
    public void getProxyRoomList(SocketIOClient client, Object data) {}

    /**
     * 代开房间解散
     *
     * @param client
     * @param data
     */
    public void dissolveProxyRoom(SocketIOClient client, Object data) {}

    /**
     * 获取用户成就信息
     *
     * @param client
     * @param data
     */
    public void getUserAchievementInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 所要获取的游戏id
        JSONArray idList = postData.getJSONArray("id_list");
        String platform = postData.getString("platform");
        // 用户成就信息
        JSONArray userAchievements = achievementBiz.getUserAchievementByAccount(account);
        // 组织数据，通知前端
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        // 成就详情
        JSONArray achievementArray = new JSONArray();
        for (int i = 0; i < idList.size(); i++) {
            int gameId = idList.getInt(i);
            JSONObject userAchievement = getAchievementByGameId(userAchievements, gameId);
            JSONObject achievement = new JSONObject();
            achievement.put("game_id", gameId);
            // 有存在取数据库，不存在取默认
            if (!Dto.isObjNull(userAchievement)) {
                achievement.put("score", userAchievement.getInt("achievement_score"));
                achievement.put("name", userAchievement.getString("achievement_name"));
            } else {
                achievement.put("score", 0);
                JSONArray achievementInfo = achievementBiz.getAchievementInfoByGameId(gameId, platform);
                if (achievementInfo.size() > 0) {
                    achievement.put("name", achievementInfo.getJSONObject(0).getString("achievement_name"));
                } else {
                    achievement.put("name", "未设置");
                }
            }
            achievementArray.add(achievement);
        }
        result.put("data", achievementArray);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getUserAchievementInfoPush");
    }

    /**
     * 判断用户成就信息是否存在
     *
     * @param userAchievements
     * @param gameId
     * @return
     */
    private JSONObject getAchievementByGameId(JSONArray userAchievements, int gameId) {
        for (Object obj : userAchievements) {
            JSONObject userAchievement = JSONObject.fromObject(obj);
            if (userAchievement.containsKey("game_id") && userAchievement.getInt("game_id") == gameId) {
                return userAchievement;
            }
        }
        return null;
    }

    /**
     * 获取道具商城详情
     *
     * @param client
     * @param data
     */
    public void getPropsInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int type = postData.getInt("type");
        String platform = postData.getString("platform");
        JSONArray props = propsBiz.getPropsInfoByPlatform(platform);
        JSONObject result = new JSONObject();
        result.put("data", props);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getPropsInfoPush");
    }

    /**
     * 用户购买道具
     *
     * @param client
     * @param data
     */
    public void userPurchase(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString("account");
        String uuid = postData.getString("uuid");
        long propsId = postData.getLong("props_id");
        // 当前时间
        String nowTime = TimeUtil.getNowDate();
        // 道具信息
        JSONObject propsInfo = propsBiz.getPropsInfoById(propsId);
        // 通知前端
        JSONObject result = new JSONObject();
        if (!Dto.isObjNull(propsInfo)) {
            JSONObject userInfo = userBiz.getUserByAccount(account);
            // 支付类型
            String costType = propsInfo.getString("cost_type");
            // 价格
            int propsPrice = propsInfo.getInt("props_price");
            // 持续时间(小时)
            int duration = propsInfo.getInt("duration");
            // 道具类型
            int propsType = propsInfo.getInt("props_type");
            if (!Dto.isObjNull(userInfo) && userInfo.containsKey(costType) && userInfo.getInt(costType) > propsPrice) {
                if (!Dto.stringIsNULL(uuid) && uuid.equals(userInfo.getString("uuid"))) {
                    JSONObject userProps = propsBiz.getUserPropsByType(account, propsType);
                    JSONObject props = new JSONObject();
                    String endTime;
                    if (Dto.isObjNull(userProps)) {
                        endTime = TimeUtil.addHoursBaseOnNowTime(nowTime, duration, "yyyy-MM-dd HH:mm:ss");
                        props.put("user_account", account);
                        props.put("game_id", propsInfo.getInt("game_id"));
                        props.put("props_type", propsInfo.getInt("props_type"));

                        props.put("props_name", propsInfo.getString("type_name"));
                        props.put("end_time", endTime);
                        // type为偶数需要更新数量
                        if (propsInfo.getInt("props_type") % 2 == 0) {
                            props.put("props_count", duration);
                        }
                    } else {
                        // 当前是否过期
                        if (TimeUtil.isLatter(nowTime, userProps.getString("end_time"))) {
                            endTime = TimeUtil.addHoursBaseOnNowTime(nowTime, duration, "yyyy-MM-dd HH:mm:ss");
                        } else {
                            endTime = TimeUtil.addHoursBaseOnNowTime(userProps.getString("end_time"), duration, "yyyy-MM-dd HH:mm:ss");
                        }
                        props.put("id", userProps.getString("id"));
                        props.put("end_time", endTime);
                        // type为偶数需要更新数量
                        if (propsInfo.getInt("props_type") % 2 == 0) {
                            props.put("props_count", userProps.getInt("props_count") + duration);
                        }
                    }
                    propsBiz.addOrUpdateUserProps(props);
                    // 扣除玩家金币房卡
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("id", userInfo.getLong("id"));
                    obj.put("total", userInfo.getInt(costType));
                    obj.put("fen", -propsPrice);
                    array.add(obj);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("array", array);
                    jsonObject.put("updateType", costType);
                    producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put(CommonConstant.RESULT_KEY_MSG, "购买成功");
                    if (costType.equals("roomcard")) {
                        // 添加记录
                        publicBiz.addUserWelfareRec(account, -propsPrice, CommonConstant.CURRENCY_TYPE_ROOM_CARD - 1, propsInfo.getInt("game_id"));
                        result.put("roomcard", userInfo.getInt("roomcard") - propsPrice);
                    } else {
                        result.put("roomcard", userInfo.getInt("roomcard"));
                    }
                    if (costType.equals("coins")) {
                        result.put("coins", userInfo.getInt("coins") - propsPrice);
                    } else {
                        result.put("coins", userInfo.getInt("coins"));
                    }
                    result.put("end_time", endTime);
                    result.put("account", account);
                    if (propsType == CommonConstant.PROPS_TYPE_DOUBLE_CARD) {
                        result.put("props_count", props.getInt("props_count"));
                    }
                    if (postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)) {
                        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
                            GameRoom room = roomDeserializable(roomNo);
                            if (room != null && room.getPlayerMap().containsKey(account) && room.getPlayerMap().get(account) != null) {
                                if (room instanceof DdzGameRoom) {
                                    if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                                        room.getPlayerMap().get(account).setScore(result.getInt("coins"));
                                    }
                                    // 更改对应的道具类型
                                    switch (propsType) {
                                        case CommonConstant.PROPS_TYPE_JPQ:
                                            ((DdzGameRoom) room).getUserPacketMap().get(account).setJpqEndTime(endTime);
                                            break;
                                        case CommonConstant.PROPS_TYPE_DOUBLE_CARD:
                                            ((DdzGameRoom) room).getUserPacketMap().get(account).setDoubleCardNum(props.getInt("props_count"));
                                            break;
                                        default:
                                            break;
                                    }
                                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(result), "userPurchasePush");
                                }
                            }
                    }
                } else {
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                    result.put(CommonConstant.RESULT_KEY_MSG, "信息不正确");
                }
            } else {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "余额不足");
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "道具不存在");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "userPurchasePush");
    }

    /**
     * 获取用户背包详情
     *
     * @param client
     * @param data
     */
    public void getBackpackInfo(SocketIOClient client, Object data) {
        // 页面传递数据
        JSONObject postData = JSONObject.fromObject(data);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 平台号
        String platform = postData.getString("platform");
        // 道具详情
        List<JSONObject> propsList = new ArrayList<>();
        // 当前时间
        String nowDate = TimeUtil.getNowDate();
        // 查询当前用户道具
        JSONArray userProps = propsBiz.getUserPropsByAccount(account);
        if (!Dto.isNull(userProps) && userProps.size() > 0) {
            for (Object obj : userProps) {
                JSONObject userProp = JSONObject.fromObject(obj);
                // 按数量计算且当前数量大于0
                if (userProp.getInt("props_type") % 2 == 0 && userProp.getInt("props_count") > 0) {
                    JSONObject propsInfo = new JSONObject();
                    propsInfo.put("type", userProp.getInt("props_type"));
                    propsInfo.put("name", userProp.getString("props_name"));
                    propsInfo.put("count", userProp.getInt("props_count"));
                    propsList.add(propsInfo);
                } else if (TimeUtil.isLatter(userProp.getString("end_time"), nowDate)) {
                    // 按时间计算且当前时间未过期
                    JSONObject propsInfo = new JSONObject();
                    propsInfo.put("type", userProp.getInt("props_type"));
                    propsInfo.put("name", userProp.getString("props_name"));
                    propsInfo.put("endTime", userProp.getString("end_time"));
                    propsList.add(propsInfo);
                }
            }
        }
        JSONObject result = new JSONObject();
        if (propsList.size() > 0) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("propsList", propsList);
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getBackpackInfoPush");
    }

    /**
     * 获取成就排行榜
     *
     * @param client
     * @param data
     */
    public void getAchievementRank(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gameId");
        int limit = postData.getInt("limit");
        JSONArray achievementRank = achievementBiz.getAchievementRank(limit, gameId);
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        result.put("data", achievementRank);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAchievementRankPush");
    }

    /**
     * 获取抽奖信息
     *
     * @param client
     * @param data
     */
    public void getDrawInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!checkGameEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        GameRoom room = roomDeserializable(roomNo);
        JSONObject result = new JSONObject();
        if (room != null && !Dto.isObjNull(room.getWinStreakObj())) {
            JSONObject winStreakObj = room.getWinStreakObj();
            // 次数
            int time = winStreakObj.getInt("time");
            // 是否需要连胜
            int mustWin = winStreakObj.getInt("mustWin");
            int winStreakTime = 0;
            if (room instanceof DdzGameRoom) {
                winStreakTime = ((DdzGameRoom) room).getUserPacketMap().get(account).getWinStreakTime();
            }
            if (winStreakTime >= time) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("cardNum", 6);
            } else {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                if (mustWin == CommonConstant.GLOBAL_YES) {
                    result.put(CommonConstant.RESULT_KEY_MSG, "再连胜" + (time - winStreakTime) + "场可拆红包");
                } else {
                    result.put(CommonConstant.RESULT_KEY_MSG, "再赢" + (time - winStreakTime) + "场可拆红包");
                }
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "当前房间不可拆红包");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getDrawInfoPush");
    }

    /**
     * 抽奖
     * @param client
     * @param data
     */
    public void gameDraw(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!checkGameEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int num = postData.getInt("num");
        GameRoom room = roomDeserializable(roomNo);
        int winStreakTime = 0;
        if (room != null && !Dto.isObjNull(room.getWinStreakObj())) {
            if (room instanceof DdzGameRoom) {
                winStreakTime = ((DdzGameRoom) room).getUserPacketMap().get(account).getWinStreakTime();
            }
            JSONObject winStreakObj = room.getWinStreakObj();
            // 次数
            int time = winStreakObj.getInt("time");
            if (winStreakTime >= time) {
                List<JSONObject> rewardList = room.getWinStreakObj().getJSONArray("rewardArr");
                if (num >= 0 && num < rewardList.size()) {
                    Collections.shuffle(rewardList);
                    JSONObject rewardObj = rewardList.get(num);
                    double reward = rewardObj.getDouble("val");
                    // 奖励类别
                    String rewardType = null;
                    // 奖励详情
                    String rewardDetail = "谢谢参与";
                    // 初始化抽奖次数
                    if (room instanceof DdzGameRoom) {
                        ((DdzGameRoom) room).getUserPacketMap().get(account).setWinStreakTime(0);
                    }
                    if (reward > 0) {
                        if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_ROOM_CARD) {
                            rewardType = "roomcard";
                            rewardDetail = reward + "钻石";
                        } else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_COINS) {
                            rewardType = "coins";
                            rewardDetail = reward + "金币";
                            // 更新金币
                            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
                                double newScore = Dto.add(room.getPlayerMap().get(account).getScore(), reward);
                                room.getPlayerMap().get(account).setScore(newScore);
                            }
                        } else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_SCORE) {
                            rewardType = "score";
                            rewardDetail = reward + "实物券";
                        } else if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_YB) {
                            rewardType = "yuanbao";
                            rewardDetail = reward + "红包券";
                        }
                    }
                    if (!Dto.stringIsNULL(rewardType)) {
                        // 更新奖励
                        JSONArray array = new JSONArray();
                        JSONObject obj = new JSONObject();
                        obj.put("total", room.getPlayerMap().get(account).getScore());
                        obj.put("fen", reward);
                        obj.put("id", room.getPlayerMap().get(account).getId());
                        array.add(obj);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("array", array);
                        jsonObject.put("updateType", rewardType);
                        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
                        // 红包券和实物券添加记录
                        if (rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_SCORE ||
                            rewardObj.getInt("type") == CommonConstant.CURRENCY_TYPE_YB) {
                            publicBiz.addUserWelfareRec(account, reward, rewardObj.getInt("type") - 1, room.getGid());
                        }
                    }
                    // 通知前端
                    JSONObject result = new JSONObject();
                    result.put("num", num);
                    result.put("reward", rewardDetail);
                    result.put("rewardList", rewardList);
                    result.put("drawInfo", "还剩" + time + "局");
                    result.put("nowScore", room.getPlayerMap().get(account).getScore());
                    result.put("index", room.getPlayerMap().get(account).getMyIndex());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameDrawPush");
                }
            }
        }
    }

    /**
     * 获取成就详情
     * @param client
     * @param data
     */
    public void getAchievementDetail(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String platform = postData.getString("platform");
        int gameId = postData.getInt("game_id");
        JSONObject result = new JSONObject();
        // 所有成就
        JSONArray achievementArray = achievementBiz.getAchievementInfoByGameId(gameId, platform);
        // 玩家当前成就
        JSONObject userAchievement = achievementBiz.getUserAchievementByAccountAndGameId(account, gameId);
        JSONArray array = new JSONArray();
        for (Object obj : achievementArray) {
            JSONObject achievement = JSONObject.fromObject(obj);
            // 可以领取 已经领取 不能领
            if (!Dto.isObjNull(userAchievement) && userAchievement.getJSONArray("reward_array").contains(achievement.getLong("id"))) {
                achievement.put("status", 1);
            } else if (!Dto.isObjNull(userAchievement) && userAchievement.getJSONArray("draw_array").contains(achievement.getLong("id"))) {
                achievement.put("status", 0);
            } else {
                achievement.put("status", 2);
            }
            array.add(achievement);
        }
        result.put("achievement_array", array);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getAchievementDetailPush");
    }

    /**
     * 获取成就奖励
     * @param client
     * @param data
     */
    public void drawAchievementReward(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int userId = postData.getInt("user_id");
        JSONObject result = new JSONObject();
        // 成就详情
        int achievementId = postData.getInt("achievement_id");
        JSONObject achievementInfo = achievementBiz.getAchievementInfoById(achievementId);
        if (!Dto.isObjNull(achievementInfo)) {
            // 用户成就
            JSONObject userAchievement = achievementBiz.getUserAchievementByAccountAndGameId(account, achievementInfo.getInt("game_id"));
            if (!Dto.isObjNull(userAchievement) && userAchievement.getJSONArray("reward_array").contains(achievementId)) {
                double reward = achievementInfo.getDouble("reward");
                String rewardType = null;
                if (achievementInfo.getInt("reward_type") == CommonConstant.CURRENCY_TYPE_COINS) {
                    rewardType = "coins";
                }
                if (!Dto.stringIsNULL(rewardType)) {
                    // 更新成就信息
                    JSONObject newUserAchievement = new JSONObject();
                    newUserAchievement.put("id", userAchievement.getLong("id"));
                    JSONArray rewardArray = new JSONArray();
                    JSONArray drawArray = userAchievement.getJSONArray("draw_array");
                    for (int i = 0; i < userAchievement.getJSONArray("reward_array").size(); i++) {
                        if (userAchievement.getJSONArray("reward_array").getInt(i) == achievementId) {
                            drawArray.add(achievementId);
                        } else {
                            rewardArray.add(userAchievement.getJSONArray("reward_array").getInt(i));
                        }
                    }
                    newUserAchievement.put("reward_array", rewardArray);
                    newUserAchievement.put("draw_array", drawArray);
                    achievementBiz.updateUserAchievement(newUserAchievement);
                    // 更新奖励
                    JSONArray array = new JSONArray();
                    JSONObject obj = new JSONObject();
                    obj.put("total", 1);
                    obj.put("fen", reward);
                    obj.put("id", userId);
                    array.add(obj);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("array", array);
                    jsonObject.put("updateType", rewardType);
                    producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, jsonObject));
                    // 通知前端
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put(CommonConstant.RESULT_KEY_MSG, "领取成功");
                    // 刷新成就
                    JSONArray achievementArray = achievementBiz.getAchievementInfoByGameId(achievementInfo.getInt("game_id"), achievementInfo.getString("platform"));
                    JSONArray arr = new JSONArray();
                    for (Object o : achievementArray) {
                        JSONObject achievement = JSONObject.fromObject(o);
                        // 可以领取 已经领取 不能领
                        if (rewardArray.contains(achievement.getLong("id"))) {
                            achievement.put("status", 1);
                        } else if (drawArray.contains(achievement.getLong("id"))) {
                            achievement.put("status", 0);
                        } else {
                            achievement.put("status", 2);
                        }
                        arr.add(achievement);
                    }
                    result.put("achievement_array", arr);
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "drawAchievementRewardPush");
                    return;
                }
            }
        }
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        result.put(CommonConstant.RESULT_KEY_MSG, "领取失败");
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "drawAchievementRewardPush");
    }

    /**
     * 换房间
     *
     * @param client
     * @param data
     */
    public void changeRoomBase(SocketIOClient client, Object data) {}

    /**
     * 获取房卡场战绩
     *
     * @param client
     * @param data
     */
    public void getRoomCardGameLogList(SocketIOClient client, Object data) {}

    /**
     * 获取房卡场战绩
     *
     * @param client
     * @param data
     */
    public void getClubGameLogList(SocketIOClient client, Object data) {}

    /**
     * 获取房卡场战绩详情
     *
     * @param client
     * @param data
     */
    public void getRoomCardGameLogDetail(SocketIOClient client, Object data) {}

}
