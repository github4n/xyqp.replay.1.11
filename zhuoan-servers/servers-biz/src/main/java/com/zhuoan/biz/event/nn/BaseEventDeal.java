package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.*;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static net.sf.json.JSONObject.fromObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:03 2018/4/17
 * @Modified By:
 **/
@Component
public class BaseEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(BaseEventDeal.class);

    @Resource
    private UserBiz userBiz;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private PublicBiz publicBiz;

    @Resource
    private GameLogBiz gameLogBiz;

    @Resource
    private NNGameEventDealNew nnGameEventDealNew;

    @Resource
    private SSSGameEventDealNew sssGameEventDealNew;

    @Resource
    private ZJHGameEventDealNew zjhGameEventDealNew;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RedisService redisService;

    /**
     * 创建房间判断是否满足条件
     *
     * @param client
     * @param data
     */
    public void createRoomBase(SocketIOClient client, Object data) {
        long start = System.currentTimeMillis();
        // 检查是否能加入房间
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString("account");
        // 房间信息
        JSONObject baseInfo = postData.getJSONObject("base_info");
        baseInfo.put("roomType", 3);
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)) {
            // 用户不存在
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "用户不存在");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB && userInfo.containsKey("yuanbao")
            && userInfo.getDouble("yuanbao") < baseInfo.getDouble("enterYB")) {
            // 元宝不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "元宝不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 设置客户端标识
        client.set(CommonConstant.CLIENT_TAG_ACCOUNT, account);
        // 创建房间
        createRoomBase(client, JSONObject.fromObject(data), userInfo);
        long end = System.currentTimeMillis();
        logger.info("公共---createRoomBase(SocketIOClient client, Object data)"+(end-start));
    }

    /**
     * 创建房间创建实体对象
     *
     * @param client
     * @param postData
     * @param userInfo
     */
    public void createRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        long start = System.currentTimeMillis();
        JSONObject baseInfo = postData.getJSONObject("base_info");
        // 添加房间信息
        String roomNo = RoomManage.randomRoomNo();
        client.set(CommonConstant.CLIENT_TAG_ROOM_NO, roomNo);
        GameRoom gameRoom;
        switch (postData.getInt("gid")) {
            case CommonConstant.GAME_ID_NN:
                gameRoom = new NNGameRoomNew();
                createRoomNN((NNGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
                break;
            case CommonConstant.GAME_ID_SSS:
                gameRoom = new SSSGameRoomNew();
                createRoomSSS((SSSGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
                break;
            case CommonConstant.GAME_ID_ZJH:
                gameRoom = new ZJHGameRoomNew();
                createRoomZJH((ZJHGameRoomNew) gameRoom, baseInfo, userInfo.getString("account"));
                break;
            default:
                gameRoom = new GameRoom();
                break;
        }
        // 设置房间属性
        gameRoom.setRoomType(baseInfo.getInt("roomType"));
        gameRoom.setGid(postData.getInt("gid"));
        gameRoom.setPort(postData.getInt("port"));
        gameRoom.setIp(postData.getString("ip"));
        gameRoom.setRoomNo(roomNo);
        gameRoom.setRoomInfo(baseInfo);
        gameRoom.setCreateTime(new Date().toString());
        int playerNum = baseInfo.getInt("player");
        if (postData.getInt("gid") == CommonConstant.GAME_ID_SSS) {
            playerNum = baseInfo.getInt("maxPlayer");
        }
        List<Long> idList = new ArrayList<Long>();
        List<String> iconList = new ArrayList<String>();
        List<String> nameList = new ArrayList<String>();
        List<Integer> scoreList = new ArrayList<Integer>();
        for (int i = 0; i < playerNum; i++) {
            if (i == 0) {
                idList.add(userInfo.getLong("id"));
                iconList.add(userInfo.getString("headimg"));
                nameList.add(userInfo.getString("name"));
                scoreList.add(userInfo.getInt("score"));
            } else {
                idList.add((long) 0);
                iconList.add("");
                nameList.add("");
                scoreList.add(0);
            }
        }
        gameRoom.setUserIdList(idList);
        gameRoom.setUserIconList(iconList);
        gameRoom.setUserNameList(nameList);
        gameRoom.setUserScoreList(scoreList);
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_JB || gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            gameRoom.setGameCount(9999);
        }
        if (baseInfo.containsKey("di")) {
            gameRoom.setScore(baseInfo.getDouble("di"));
        } else {
            gameRoom.setScore(1);
        }
        // 元宝模式
        if (baseInfo.containsKey("yuanbao") && baseInfo.getDouble("yuanbao") > 0) {
            //底分
            gameRoom.setScore(baseInfo.getDouble("yuanbao"));
        }
        // 元宝模式
        if (baseInfo.containsKey("enterYB") && baseInfo.getDouble("enterYB") > 0) {
            gameRoom.setEnterScore(baseInfo.getDouble("enterYB"));
        }
        // 元宝模式
        if (baseInfo.containsKey("leaveYB") && baseInfo.getDouble("leaveYB") > 0) {
            gameRoom.setLeaveScore(baseInfo.getDouble("leaveYB"));
        }
        //设置金币场准入金币
        if (baseInfo.containsKey("goldcoins")) {
            gameRoom.setEnterScore(baseInfo.getInt("goldcoins"));
        }
        if (baseInfo.getInt("open") == 1) {
            gameRoom.setOpen(true);
        } else {
            gameRoom.setOpen(false);
        }
        // 是否允许玩家中途加入
        if (baseInfo.containsKey("halfwayin") && baseInfo.getInt("halfwayin") == 1) {
            gameRoom.setHalfwayIn(true);
        }
        //准备超时（0：不处理 1：自动准备 2：踢出房间）
        if (baseInfo.containsKey("readyovertime")) {
            if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_NOTHING) {
                gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
            } else if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_AUTO) {
                gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_AUTO);
            } else if (baseInfo.getInt("readyovertime") == CommonConstant.READY_OVERTIME_OUT) {
                gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
            }
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB) {
            gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
        } else {
            gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
        }
        // 玩家人数
        gameRoom.setPlayerCount(playerNum);
        // 金币、元宝扣服务费
        if ((baseInfo.containsKey("fee") && baseInfo.getInt("fee") == 1) || baseInfo.getInt("roomType") == 3) {

            /* 获取房间设置，插入缓存 */
            JSONObject gameSetting = getGameSetting(gameRoom);

            JSONObject roomFee = gameSetting.getJSONObject("pumpData");
            double fee;
            // 服务费：费率x底注
            if (baseInfo.containsKey("custFee")) {
                // 自定义费率
                fee = baseInfo.getDouble("custFee") * gameRoom.getScore();
            } else {
                // 统一费率
                fee = roomFee.getDouble("proportion") * gameRoom.getScore();
            }
            double maxFee = roomFee.getDouble("max");
            double minFee = roomFee.getDouble("min");
            if (fee > maxFee) {
                fee = maxFee;
            } else if (fee < minFee) {
                fee = minFee;
            }
            fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            gameRoom.setFee(fee);
        }
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo", userInfo);
        obtainPlayerInfoData.put("myIndex", 0);
        obtainPlayerInfoData.put("uuid", client.getSessionId().toString());
        obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location", postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        RoomManage.gameRoomMap.put(roomNo, gameRoom);
        // 通知玩家
        JSONObject object = new JSONObject();
        object.put(CommonConstant.DATA_KEY_ACCOUNT, playerinfo.getAccount());
        object.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
        switch (gameRoom.getGid()) {
            case CommonConstant.GAME_ID_NN:
                nnGameEventDealNew.createRoom(client, object);
                break;
            case CommonConstant.GAME_ID_SSS:
                sssGameEventDealNew.createRoom(client, object);
                break;
            case CommonConstant.GAME_ID_ZJH:
                zjhGameEventDealNew.createRoom(client, object);
                break;
            default:
                break;
        }
        // 组织数据，插入数据库
        JSONObject obj = new JSONObject();
        obj.put("game_id", gameRoom.getGid());
        obj.put("room_no", gameRoom.getRoomNo());
        obj.put("roomtype", gameRoom.getRoomType());
        obj.put("base_info", gameRoom.getRoomInfo());
        obj.put("createtime", new SimpleDateFormat("yyyy:MM:dd hh:mm:ss").format(new Date()));
        obj.put("game_count", gameRoom.getGameCount());
        obj.put("user_id0", playerinfo.getId());
        obj.put("user_icon0", playerinfo.getHeadimg());
        obj.put("user_name0", playerinfo.getName());
        obj.put("ip", gameRoom.getIp());
        obj.put("port", gameRoom.getPort());
        obj.put("status", 0);
        if (gameRoom.isOpen()) {
            obj.put("open", 1);
        } else {
            obj.put("open", 0);
        }

        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_ROOM, obj));

        long end = System.currentTimeMillis();
        logger.info("公共---createRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo)"+(end-start));
    }

    private JSONObject getGameSetting(GameRoom gameRoom) {
        long start = System.currentTimeMillis();
        JSONObject gameSetting;
        try {
            Object object = redisService.queryValueByKey(CacheKeyConstant.GAME_SETTING);
            if (object != null) {
                gameSetting = JSONObject.fromObject(redisService.queryValueByKey(CacheKeyConstant.GAME_SETTING));
            }else {
                gameSetting = roomBiz.getGameSetting();
                redisService.insertKey(CacheKeyConstant.GAME_SETTING, String.valueOf(gameSetting), null);
            }
        } catch (Exception e) {
            gameSetting = roomBiz.getGameSetting();
            redisService.insertKey(CacheKeyConstant.GAME_SETTING, String.valueOf(gameSetting), null);
        }
        long end = System.currentTimeMillis();
        logger.info("公共---getGameSetting(GameRoom gameRoom)"+(end-start));
        return gameSetting;
    }

    /**
     * 加入房间判断是否满足条件
     *
     * @param client
     * @param data
     */
    public void joinRoomBase(SocketIOClient client, Object data) {
        long start = System.currentTimeMillis();
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString("account");
        // 房间号
        String roomNo = postData.getString("roomNo");

        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "房间不存在");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)) {
            // 用户不存在
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "用户不存在");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        } else if (RoomManage.gameRoomMap.get(roomNo).getRoomType() == CommonConstant.ROOM_TYPE_YB && userInfo.containsKey("yuanbao")
            && userInfo.getDouble("yuanbao") < RoomManage.gameRoomMap.get(roomNo).getEnterScore()) {
            // 元宝不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "元宝不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 设置客户端标识
        client.set(CommonConstant.CLIENT_TAG_ACCOUNT, account);
        client.set(CommonConstant.CLIENT_TAG_ROOM_NO, roomNo);

        joinRoomBase(client, postData, userInfo);
        long end = System.currentTimeMillis();
        logger.info("公共---joinRoomBase(SocketIOClient client, Object data)"+(end-start));
    }

    /**
     * 加入房间创建实体对象
     *
     * @param client
     * @param postData
     * @param userInfo
     */
    public void joinRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        long start = System.currentTimeMillis();
        String roomNo = postData.getString("roomNo");
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        JSONObject result = new JSONObject();
        int myIndex = -1;
        // 获取当前房间的第一个空位
        for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
            if (gameRoom.getUserIdList().get(i) == 0) {
                myIndex = i;
                break;
            }
        }
        // 重连不需要重新获取座位号
        if (postData.containsKey("myIndex")) {
            myIndex = postData.getInt("myIndex");
        }
        if (myIndex < 0) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "房间已满");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 设置房间属性
        gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
        RoomManage.gameRoomMap.put(roomNo, gameRoom);
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo", userInfo);
        obtainPlayerInfoData.put("myIndex", myIndex);
        obtainPlayerInfoData.put("uuid", client.getSessionId().toString());
        obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location", postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        JSONObject joinData = new JSONObject();
        // 是否重连
        if (gameRoom.getPlayerMap().containsKey(playerinfo.getAccount())) {
            joinData.put("isReconnect", 1);
        } else {
            // 更新数据库
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", gameRoom.getRoomNo());
            // TODO: 2018/4/20 字符串拼接
            roomInfo.put("user_id" + myIndex, playerinfo.getId());
            roomInfo.put("user_icon" + myIndex, playerinfo.getHeadimg());
            roomInfo.put("user_name" + myIndex, playerinfo.getName());
            // 更新房间信息
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            joinData.put("isReconnect", 0);
        }
        joinData.put(CommonConstant.DATA_KEY_ACCOUNT, userInfo.getString("account"));
        joinData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        // 通知玩家
        switch (gameRoom.getGid()) {
            case CommonConstant.GAME_ID_NN:
                // 重连不需要重新设置用户牌局信息
                if (!((NNGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((NNGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacket());
                }
                nnGameEventDealNew.joinRoom(client, joinData);
                break;
            case CommonConstant.GAME_ID_SSS:
                // 重连不需要重新设置用户牌局信息
                if (!((SSSGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((SSSGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new Player());
                }
                sssGameEventDealNew.joinRoom(client, joinData);
                break;
            case CommonConstant.GAME_ID_ZJH:
                // 重连不需要重新设置用户牌局信息
                if (!((ZJHGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((ZJHGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new com.zhuoan.biz.model.zjh.UserPacket());
                }
                zjhGameEventDealNew.joinRoom(client, joinData);
                break;
            default:
                break;
        }
        long end = System.currentTimeMillis();
        logger.info("公共---joinRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo)"+(end-start));
    }

    /**
     * 获取玩家信息
     *
     * @param data
     * @return
     */
    public Playerinfo obtainPlayerInfo(JSONObject data) {
        long start = System.currentTimeMillis();
        JSONObject userInfo = data.getJSONObject("userInfo");
        int myIndex = data.getInt("myIndex");
        UUID uuid = UUID.fromString(data.getString("uuid"));
        int roomType = data.getInt("room_type");
        Playerinfo playerinfo = new Playerinfo();
        playerinfo.setId(userInfo.getLong("id"));
        playerinfo.setAccount(userInfo.getString("account"));
        playerinfo.setName(userInfo.getString("name"));
        playerinfo.setUuid(uuid);
        playerinfo.setMyIndex(myIndex);
        if (roomType == CommonConstant.ROOM_TYPE_JB) {
            // 金币模式
            playerinfo.setScore(userInfo.getDouble("coins"));
        } else if (roomType == CommonConstant.ROOM_TYPE_YB) {
            // 元宝模式
            playerinfo.setScore(userInfo.getDouble("yuanbao"));
        } else {
            // 房卡模式
            playerinfo.setScore(0);
        }
        playerinfo.setHeadimg(userInfo.getString("headimg"));
        playerinfo.setSex(userInfo.getString("sex"));
        playerinfo.setIp(userInfo.getString("ip"));
        if (userInfo.containsKey("sign")) {
            playerinfo.setSignature(userInfo.getString("sign"));
        } else {
            playerinfo.setSignature("");
        }
        if (userInfo.containsKey("ghName")) {
            playerinfo.setGhName(userInfo.getString("ghName"));
        }
        if (userInfo.containsKey("area")) {
            playerinfo.setArea(userInfo.getString("area"));
        } else {
            playerinfo.setArea("");
        }
        int vip = userInfo.getInt("lv");
        if (vip > 1) {
            playerinfo.setVip(vip - 1);
        } else {
            playerinfo.setVip(0);
        }
        playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
        // 保存用户坐标
        if (data.containsKey("location")) {
            playerinfo.setLocation(data.getString("location"));
        }
        // 设置幸运值
        if (userInfo.containsKey("luck")) {
            playerinfo.setLuck(userInfo.getInt("luck"));
        }
        long end = System.currentTimeMillis();
        logger.info("公共---obtainPlayerInfo(JSONObject data)"+(end-start));
        return playerinfo;
    }

    /**
     * 设置牛牛房间特殊参数
     *
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomNN(NNGameRoomNew room, JSONObject baseInfo, String account) {
        long start = System.currentTimeMillis();
        room.setBankerType(baseInfo.getInt("type"));
        // 玩法
        String wanFa = "";
        switch (baseInfo.getInt("type")) {
            case NNConstant.NN_BANKER_TYPE_FZ:
                wanFa = "房主坐庄";
                break;
            case NNConstant.NN_BANKER_TYPE_LZ:
                wanFa = "轮庄";
                break;
            case NNConstant.NN_BANKER_TYPE_QZ:
                wanFa = "抢庄";
                break;
            case NNConstant.NN_BANKER_TYPE_MP:
                wanFa = "明牌抢庄";
                break;
            case NNConstant.NN_BANKER_TYPE_NN:
                wanFa = "牛牛坐庄";
                break;
            case NNConstant.NN_BANKER_TYPE_TB:
                wanFa = "通比牛牛";
                break;
            default:
                break;
        }
        room.setWfType(wanFa);
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        // 设置基本牌型倍数
        if (baseInfo.containsKey("niuniuNum")) {
            JSONArray nnNums = baseInfo.getJSONArray("niuniuNum");
            for (int i = 0; i <= 10; i++) {
                int value = nnNums.getInt(0);
                if (i == 7) {
                    value = nnNums.getInt(1);
                } else if (i == 8) {
                    value = nnNums.getInt(2);
                } else if (i == 9) {
                    value = nnNums.getInt(3);
                } else if (i == 10) {
                    value = nnNums.getInt(4);
                }
                // 设置倍率
                room.ratio.put(i, value);
            }
        }
        if (baseInfo.containsKey("special")) {
            List<Integer> specialType = new ArrayList<Integer>();
            JSONArray types = baseInfo.getJSONArray("special");
            for (int i = 0; i < types.size(); i++) {
                int type = types.getJSONObject(i).getInt("type");
                int value = types.getJSONObject(i).getInt("value");
                specialType.add(type);
                // 设置特殊牌型的倍率
                room.ratio.put(type, value);
            }
            room.setSpecialType(specialType);
        }
        // 抢庄是否要加倍
        if (baseInfo.containsKey("qzTimes")) {
            room.qzTimes = baseInfo.getJSONArray("qzTimes");
        }
        // 抢庄是否是随机庄（随机、最高倍数为庄）
        if (baseInfo.containsKey("qzsjzhuang")) {
            room.setSjBanker(baseInfo.getInt("qzsjzhuang"));
        } else {
            room.setSjBanker(0);
        }
        // 没人抢庄
        if (baseInfo.containsKey("qznozhuang") && baseInfo.getInt("qznozhuang") == NNConstant.NN_QZ_NO_BANKER_CK) {
            // 无人抢庄，重新发牌
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_CK);
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB) {
            // 无人抢庄，房间自动解散
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_JS);
        } else {
            // 无人抢庄，随机庄
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_SJ);
        }
        if (baseInfo.containsKey("baseNum")) {
            // 设置基础倍率
            room.setBaseNum(baseInfo.getJSONArray("baseNum").toString());
        }
        room.getUserPacketMap().put(account, new UserPacket());
        long end = System.currentTimeMillis();
        logger.info("公共---createRoomNN(NNGameRoomNew room, JSONObject baseInfo, String account)"+(end-start));
    }

    /**
     * 设置十三水房间特殊参数
     *
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomSSS(SSSGameRoomNew room, JSONObject baseInfo, String account) {
        room.setBankerType(baseInfo.getInt("type"));
        // 玩法
        String wanFa = "";
        switch (baseInfo.getInt("type")) {
            case SSSConstant.SSS_BANKER_TYPE_BWZ:
                wanFa = "霸王庄";
                break;
            case SSSConstant.SSS_BANKER_TYPE_HB:
                wanFa = "互比";
                break;
            default:
                break;
        }
        room.setWfType(wanFa);
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        // 最大人数
        if (baseInfo.containsKey("maxPlayer")) {
            // 最低开始人数
            room.setMinPlayer(baseInfo.getInt("player"));
            room.setPlayerCount(baseInfo.getInt("maxPlayer"));
        }
        /* 获取游戏信息设置,插入缓存 */
        room.setSetting(getGameInfoById());
        room.getUserPacketMap().put(account, new Player());
    }

    /**
     * 设置十三水房间特殊参数
     *
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomZJH(ZJHGameRoomNew room, JSONObject baseInfo, String account) {
        // 玩法
        String wanFa = "";
        switch (baseInfo.getInt("type")) {
            case ZJHConstant.ZJH_GAME_TYPE_CLASSIC:
                wanFa = "经典模式";
                break;
            case ZJHConstant.ZJH_GAME_TYPE_MEN:
                wanFa = "必闷三圈";
                break;
            case ZJHConstant.ZJH_GAME_TYPE_HIGH:
                wanFa = "激情模式";
                break;
            default:
                break;
        }

        room.setWfType(wanFa);
        // 玩法类型 经典模式、必闷三圈、激情模式
        room.setGameType(baseInfo.getInt("type"));
        // 设置下注时间
        room.setXzTimer(ZJHConstant.ZJH_TIMER_XZ);
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        if (baseInfo.containsKey("di")) {
            room.setScore(baseInfo.getDouble("di"));
        } else {
            room.setScore(1);
        }
        // 元宝模式
        if (baseInfo.containsKey("yuanbao") && baseInfo.getDouble("yuanbao") > 0) {
            //底分
            room.setScore(baseInfo.getDouble("yuanbao"));
        }
        // 倍数
        if(baseInfo.containsKey("baseNum")){
            room.setBaseNum(baseInfo.getJSONArray("baseNum"));
        }else{
            JSONArray baseNum = new JSONArray();
            for (int i = 1; i <= 5; i++) {
                if (room.getScore()%1==0) {
                    baseNum.add(String.valueOf((int)room.getScore()*i));
                }else {
                    baseNum.add(String.valueOf(room.getScore()*i));
                }
            }
            room.setBaseNum(baseNum);
        }
        // 下注上限
        if(baseInfo.containsKey("maxcoins")){
            room.setMaxScore(baseInfo.getDouble("maxcoins"));
        }else{
            room.setMaxScore(100000000);
        }
        // 下注轮数上限
        if(baseInfo.containsKey("gameNum")){
            room.setTotalGameNum(baseInfo.getInt("gameNum"));
        }else{
            room.setTotalGameNum(15);
        }
        room.setCurrentScore(room.getScore());
        room.getUserPacketMap().put(account, new com.zhuoan.biz.model.zjh.UserPacket());
    }

    private JSONObject getGameInfoById() {
        long start = System.currentTimeMillis();
        JSONObject gameInfoById;
        try {
            Object object = redisService.queryValueByKey(CacheKeyConstant.GAME_INFO_BY_ID);
            if (object!=null) {
                gameInfoById = JSONObject.fromObject(redisService.queryValueByKey(CacheKeyConstant.GAME_INFO_BY_ID));

            }else {
                gameInfoById = roomBiz.getGameInfoByID(CommonConstant.GAME_ID_SSS).getJSONObject("setting");
                redisService.insertKey(CacheKeyConstant.GAME_INFO_BY_ID, String.valueOf(gameInfoById), null);
            }
        } catch (Exception e) {
            gameInfoById = roomBiz.getGameInfoByID(CommonConstant.GAME_ID_SSS).getJSONObject("setting");
            redisService.insertKey(CacheKeyConstant.GAME_INFO_BY_ID, String.valueOf(gameInfoById), null);
        }
        long end = System.currentTimeMillis();
        logger.info("公共---getGameInfoById()"+(end-start));
        return gameInfoById;
    }

    /**
     * 获取房间设置
     *
     * @param client
     * @param data
     */
    public void getGameSetting(SocketIOClient client, Object data) {
        long start = System.currentTimeMillis();
        JSONObject fromObject = JSONObject.fromObject(data);
        int gid = fromObject.getInt("gid");
        String platform = fromObject.getString("platform");

        /* 查询房间设置,插入缓存*/
        JSONArray gameSetting = getGameSetting(gid, platform);

        if (!Dto.isNull(gameSetting)) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < gameSetting.size(); i++) {
                array.add(gameSetting.getJSONObject(i));
            }
            JSONObject result = new JSONObject();
            result.put("data", array);
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "getGameSettingPush");
        }
        long end = System.currentTimeMillis();
        logger.info("公共---getGameSetting(SocketIOClient client, Object data)"+(end-start));
    }

    private JSONArray getGameSetting(int gid, String platform) {
        long start = System.currentTimeMillis();
        String key = "";
        switch (gid) {
            case CommonConstant.GAME_ID_NN:
                key = CacheKeyConstant.GAME_SETTING_NN;
                break;
            case CommonConstant.GAME_ID_SSS:
                key = CacheKeyConstant.GAME_SETTING_SSS;
                break;
            case CommonConstant.GAME_ID_ZJH:
                key = CacheKeyConstant.GAME_SETTING_ZJH;
                break;
            default:
                break;
        }
        JSONArray gameSetting = new JSONArray();
        if (!key.equals("")) {
            try {
                Object object = redisService.queryValueByKey(key);
                if (object!=null) {
                    gameSetting = JSONArray.fromObject(object);
                }else {
                    gameSetting = publicBiz.getRoomSetting(gid, platform);
                    redisService.insertKey(key, String.valueOf(gameSetting), null);
                }
            } catch (Exception e) {
                gameSetting = publicBiz.getRoomSetting(gid, platform);
                redisService.insertKey(key, String.valueOf(gameSetting), null);
            }
        }
        long end = System.currentTimeMillis();
        logger.info("公共---getGameSetting(int gid, String platform)"+(end-start));
        return gameSetting;
    }

    /**
     * 检查用户是否在房间内
     *
     * @param client
     * @param data
     */
    public void checkUser(SocketIOClient client, Object data) {
        long start = System.currentTimeMillis();
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("account")) {
            String account = postData.getString("account");
            // 遍历房间列表
            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                if (gameRoom.getPlayerMap().containsKey(account) && gameRoom.getPlayerMap().get(account) != null) {
                    postData.put("roomNo", gameRoom.getRoomNo());
                    postData.put("myIndex", gameRoom.getPlayerMap().get(account).getMyIndex());
                    joinRoomBase(client, postData);
                    return;
                }
            }
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "checkUserPush");
        }
        long end = System.currentTimeMillis();
        logger.info("公共---checkUser(SocketIOClient client, Object data)"+(end-start));
    }

    /**
     * 获取战绩记录
     * @param client
     * @param data
     */
    public void getUserGameLogs(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("user_id")&&postData.containsKey("game_id")) {
            long userId = postData.getLong("user_id");
            int gameId = postData.getInt("game_id");
            JSONArray userGameLogs = gameLogBiz.getUserGameLogsByUserId(userId,gameId);
            JSONArray result = new JSONArray();
            if (userGameLogs.size()>0) {
                for (int i = 0; i < userGameLogs.size(); i++) {
                    JSONObject userGameLog = userGameLogs.getJSONObject(i);
                    JSONObject obj = new JSONObject();
                    obj.put("room_no",userGameLog.getString("room_no"));
                    obj.put("createtime",userGameLog.getString("createtime"));
                    JSONArray userResult = new JSONArray();
                    for (int j = 0; j < userGameLog.getJSONArray("result").size(); j++) {
                        JSONObject object = userGameLog.getJSONArray("result").getJSONObject(j);
                        userResult.add(new JSONObject().element("name",object.getString("player")).element("score",object.getString("score")));
                    }
                    obj.put("result",userResult);
                    result.add(obj);
                }
            }
        }
    }
}
