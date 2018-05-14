package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.zjh.ZJHGameEventDealNew;
import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.bdx.BDXGameRoomNew;
import com.zhuoan.biz.model.bdx.UserPackerBDX;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.*;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MathDelUtil;
import com.zhuoan.util.SensitivewordFilter;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.math.BigDecimal;
import java.util.*;

import static net.sf.json.JSONObject.fromObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:03 2018/4/17
 * @Modified By:
 **/
@Component
public class BaseEventDeal {

    public static String noticeContentGame = "";

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
    private BDXGameEventDealNew bdxGameEventDealNew;

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
        // 检查是否能加入房间
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString("account");
        // 房间信息
        JSONObject baseInfo = postData.getJSONObject("base_info");
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
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")
            && userInfo.getDouble("coins") < baseInfo.getDouble("goldcoins")) {
            // 元宝不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "金币不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 创建房间
        createRoomBase(client, JSONObject.fromObject(data), userInfo);
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
            case CommonConstant.GAME_ID_BDX:
                gameRoom = new BDXGameRoomNew();
                ((BDXGameRoomNew)gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPackerBDX());
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
        for (int i = 0; i < playerNum; i++) {
            if (i == 0) {
                idList.add(userInfo.getLong("id"));
            } else {
                idList.add((long) 0);
            }
        }
        gameRoom.setUserIdList(idList);
        // 支付类型
        if (baseInfo.containsKey("paytype")) {
            gameRoom.setPayType(baseInfo.getInt("paytype"));
        }
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_YB || gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_JB) {
            gameRoom.setGameCount(9999);
        }else if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
            if (baseInfo.containsKey("turn")) {
                JSONObject turn = baseInfo.getJSONObject("turn");
                if (turn.containsKey("turn")) {
                    gameRoom.setGameCount(turn.getInt("turn"));
                }else {
                    gameRoom.setGameCount(999);
                }
                // 单个玩家需要扣除的房卡
                if (turn.containsKey("AANum")) {
                    gameRoom.setSinglePayNum(turn.getInt("AANum"));
                }
            }else {
                gameRoom.setGameCount(999);
            }
        }
        // 底分
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
            gameRoom.setLeaveScore(baseInfo.getInt("goldcoins"));
        }
        if (baseInfo.containsKey("open")&&baseInfo.getInt("open") == 1) {
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
        } else if (baseInfo.getInt("roomType") == CommonConstant.READY_OVERTIME_NOTHING){
            gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
        }
        // 玩家人数
        gameRoom.setPlayerCount(playerNum);
        // 金币、元宝扣服务费
        if ((baseInfo.containsKey("fee") && baseInfo.getInt("fee") == 1) || baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB) {

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
            case CommonConstant.GAME_ID_BDX:
                bdxGameEventDealNew.createRoom(client, object);
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

    public static String randomRoomNo(){
        String roomNo = MathDelUtil.getRandomStr(6);
        if (RoomManage.gameRoomMap.containsKey(roomNo)) {
            return randomRoomNo();
        }
        return roomNo;
    }

    private JSONObject getGameSetting(GameRoom gameRoom) {
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
            logger.error("请启动REmote DIctionary Server");
            gameSetting = roomBiz.getGameSetting();
        }
        return gameSetting;
    }

    /**
     * 加入房间判断是否满足条件
     *
     * @param client
     * @param data
     */
    public void joinRoomBase(SocketIOClient client, Object data) {
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);

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
        } else if (RoomManage.gameRoomMap.get(roomNo).getRoomType() == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")
            && userInfo.getDouble("coins") < RoomManage.gameRoomMap.get(roomNo).getEnterScore()) {
            // 元宝不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "金币不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
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
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
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
            joinData.put("isReconnect", CommonConstant.GLOBAL_YES);
        } else {
            // 更新数据库
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no", gameRoom.getRoomNo());
            roomInfo.put("user_id" + myIndex, playerinfo.getId());
            roomInfo.put("user_icon" + myIndex, playerinfo.getHeadimg());
            roomInfo.put("user_name" + myIndex, playerinfo.getName());
            // 更新房间信息
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            joinData.put("isReconnect", CommonConstant.GLOBAL_NO);
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
            case CommonConstant.GAME_ID_BDX:
                // 重连不需要重新设置用户牌局信息
                if (!((BDXGameRoomNew) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((BDXGameRoomNew) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPackerBDX());
                }
                bdxGameEventDealNew.joinRoom(client, joinData);
                break;
            default:
                break;
        }
    }

    /**
     * 获取玩家信息
     *
     * @param data
     * @return
     */
    public Playerinfo obtainPlayerInfo(JSONObject data) {
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
        if (userInfo.containsKey("Identification")) {
            playerinfo.setGhName(userInfo.getString("Identification"));
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
        }else {
            room.qzTimes.add(1);
            room.qzTimes.add(2);
            room.qzTimes.add(3);
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
        }else if (room.getBankerType()==NNConstant.NN_BANKER_TYPE_TB){
            JSONArray array = new JSONArray();
            JSONObject obj = new JSONObject();
            obj.put("val",baseInfo.getDouble("yuanbao"));
            array.add(obj);
            room.setBaseNum(array.toString());
        }
        room.getUserPacketMap().put(account, new UserPacket());
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
            logger.error("请启动REmote DIctionary Server");
            gameInfoById = roomBiz.getGameInfoByID(CommonConstant.GAME_ID_SSS).getJSONObject("setting");
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
    }

    private JSONArray getGameSetting(int gid, String platform) {
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
                logger.error("请启动REmote DIctionary Server");
                gameSetting = publicBiz.getRoomSetting(gid, platform);
            }
        }
        return gameSetting;
    }

    /**
     * 检查用户是否在房间内
     * @param client
     * @param data
     */
    public void checkUser(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("account")) {
            String account = postData.getString("account");
            // 遍历房间列表
            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                if (gameRoom.getPlayerMap().containsKey(account) && gameRoom.getPlayerMap().get(account) != null) {
                    postData.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
                    postData.put("myIndex", gameRoom.getPlayerMap().get(account).getMyIndex());
                    joinRoomBase(client, postData);
                    return;
                }
            }
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "checkUserPush");
        }
    }

    /**
     * 获取玩家信息
     * @param client
     * @param data
     */
    public void getUserInfo(SocketIOClient client, Object data){
        JSONObject postdata = JSONObject.fromObject(data);
        if (!postdata.containsKey("account")) {
            return;
        }
        JSONObject result = new JSONObject();
        String account = postdata.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("user", userInfo);
        }else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result),"getUserInfoPush");
    }

    /**
     * 获取房间列表
     * @param client
     * @param data
     */
    public void getAllRoomList(SocketIOClient client, Object data){
        JSONObject fromObject = JSONObject.fromObject(data);
        int gameId = fromObject.getInt("gid");
        JSONObject result = new JSONObject();
        int type = 0;
        if (fromObject.containsKey("type")) {
            type = fromObject.getInt("type");
        }
        JSONArray allRoom = new JSONArray();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            if (RoomManage.gameRoomMap.get(roomNo).getGid()==gameId&&RoomManage.gameRoomMap.get(roomNo).isOpen()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                JSONObject obj = new JSONObject();
                obj.put("room_no", gameRoom.getRoomNo());
                obj.put("gid", gameId);
                obj.put("base_info", gameRoom.getRoomInfo());
                obj.put("fytype", gameRoom.getWfType());
                obj.put("iszs", 0);
                obj.put("player", gameRoom.getPlayerCount());
                obj.put("renshu", gameRoom.getPlayerMap().size());
                for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
                    obj.put("user_id"+i, gameRoom.getUserIdList().get(i));
                }
                if (type==0||(type==1&&gameRoom.getPlayerMap().size()<gameRoom.getPlayerCount())) {
                    allRoom.add(obj);
                }
            }
        }
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        result.element("gid", gameId);
        result.element("array", allRoom);
        result.element("sType", fromObject.get("sType"));
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"getAllRoomListPush");
    }

    /**
     * 获取战绩记录
     * @param client
     * @param data
     */
    public void getUserGameLogs(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("id")&&postData.containsKey("gid")) {
            long userId = postData.getLong("id");
            int gameId = postData.getInt("gid");
            JSONArray userGameLogs = gameLogBiz.getUserGameLogsByUserId(userId,gameId);
            JSONObject back = new JSONObject();
            JSONArray result = new JSONArray();
            if (userGameLogs.size()>0) {
                for (int i = 0; i < userGameLogs.size(); i++) {
                    JSONObject userGameLog = userGameLogs.getJSONObject(i);
                    JSONObject obj = new JSONObject();
                    obj.put("room_no",userGameLog.getString("room_no"));
                    obj.put("createTime",userGameLog.getString("createtime"));
                    JSONArray userResult = new JSONArray();
                    for (int j = 0; j < userGameLog.getJSONArray("result").size(); j++) {
                        JSONObject object = userGameLog.getJSONArray("result").getJSONObject(j);
                        userResult.add(new JSONObject().element("player",object.getString("player")).element("score",object.getString("score")));
                    }
                    obj.put("playermap",userResult);
                    result.add(obj);
                }
            }
            if (result.size()==0) {
                back.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            }else {
                back.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                back.put("data",result);
                back.put("gid",gameId);
            }
            CommonConstant.sendMsgEventToSingle(client,back.toString(),"getGameLogsListPush");
        }
    }

    /**
     * 解散房间
     * @param client
     * @param data
     */
    public void dissolveRoom(SocketIOClient client,Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("adminCode")&&postData.containsKey("adminPass")&&postData.containsKey("memo")) {
            String adminCode = postData.getString("adminCode");
            String adminPass = postData.getString("adminPass");
            String memo = postData.getString("memo");
            if (postData.containsKey("room_no")) {
                String roomNo = postData.getString("room_no");
                if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                    JSONObject sysUser = userBiz.getSysUser(adminCode,adminPass,memo);
                    if (!Dto.isObjNull(sysUser)) {
                        JSONObject result = new JSONObject();
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"房间已解散");
                        CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),result.toString(),"tipMsgPush");
                        RoomManage.gameRoomMap.remove(roomNo);
                    }
                }
            }
        }
    }

    /**
     * 开关游戏
     * @param client
     * @param data
     */
    public void onOrOffGame(SocketIOClient client,Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("adminCode")&&postData.containsKey("adminPass")&&postData.containsKey("memo")) {
            String adminCode = postData.getString("adminCode");
            String adminPass = postData.getString("adminPass");
            String memo = postData.getString("memo");
            if (postData.containsKey("game_id")&&postData.containsKey("value")) {
                JSONObject sysUser = userBiz.getSysUser(adminCode,adminPass,memo);
                if (!Dto.isObjNull(sysUser)) {
                    int gameId = postData.getInt("game_id");
                    int value = postData.getInt("value");
                    switch (gameId) {
                        case CommonConstant.GAME_ID_NN:
                            NNGameEventDealNew.GAME_NN = value;
                            break;
                        case CommonConstant.GAME_ID_SSS:
                            SSSGameEventDealNew.GAME_SSS = value;
                            break;
                        case CommonConstant.GAME_ID_ZJH:
                            ZJHGameEventDealNew.GAME_ZJH = value;
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * 发送滚动公告
     * @param client
     * @param data
     */
    public void sendNotice(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("adminCode")&&postData.containsKey("adminPass")&&postData.containsKey("memo")) {
            String adminCode = postData.getString("adminCode");
            String adminPass = postData.getString("adminPass");
            String memo = postData.getString("memo");
            if (postData.containsKey("content")&&postData.containsKey("type")) {
                String content = postData.getString("content");
                int type = postData.getInt("type");
                // 通知所有
                JSONObject sysUser = userBiz.getSysUser(adminCode,adminPass,memo);
                if (!Dto.isObjNull(sysUser)) {
                    switch (type) {
                        case CommonConstant.NOTICE_TYPE_MALL:
                            // TODO: 2018/5/10 大厅滚动公告设置
                            break;
                        case CommonConstant.NOTICE_TYPE_GAME:
                            BaseEventDeal.noticeContentGame = content;
                            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                                sendNoticeToPlayerByRoomNo(roomNo, content, type);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * 获取滚动公告
     * @param client
     * @param data
     */
    public void getNotice(SocketIOClient client,Object data){
        JSONObject postData = JSONObject.fromObject(data);
        int type = postData.getInt("type");
        JSONObject result = new JSONObject();
        result.put("type",type);
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        switch (type) {
            case CommonConstant.NOTICE_TYPE_MALL:
                if (!postData.containsKey("platform")) {
                    return;
                }else {
                    String platform = postData.getString("platform");
                    JSONObject noticeInfo = getNoticeInfoByPlatform(platform);
                    if (!Dto.isObjNull(noticeInfo)) {
                        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                        result.put("content",noticeInfo.getString("con"));
                    }else {
                        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                    }
                }
                break;
            case CommonConstant.NOTICE_TYPE_GAME:
                if (Dto.stringIsNULL(BaseEventDeal.noticeContentGame)) {
                    result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
                }else {
                    result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                    result.put("content",BaseEventDeal.noticeContentGame);
                }
                break;
            default:
                break;
        }
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"getMessagePush");
    }

    private JSONObject getNoticeInfoByPlatform(String platform) {
        JSONObject noticeInfo;
        StringBuffer sb = new StringBuffer();
        sb.append("notice_");
        sb.append(platform);
        try {
            Object object = redisService.queryValueByKey(String.valueOf(sb));
            if (object != null) {
                noticeInfo = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(sb)));
            }else {
                noticeInfo = publicBiz.getNoticeByPlatform(platform);
                redisService.insertKey(String.valueOf(sb), String.valueOf(noticeInfo), null);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
            noticeInfo = publicBiz.getNoticeByPlatform(platform);
        }
        return noticeInfo;
    }

    /**
     * 发送滚动公告通知玩家
     * @param roomNo
     * @param content
     * @param type
     */
    public void sendNoticeToPlayerByRoomNo(String roomNo, String content, int type){
        if (!Dto.stringIsNULL(content)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("content",content);
            result.put("type",type);
            CommonConstant.sendMsgEventToAll(RoomManage.gameRoomMap.get(roomNo).getAllUUIDList(),result.toString(),"getMessagePush");
        }
    }

    /**
     * 获取洗牌信息
     * @param client
     * @param data
     */
    public void getShuffleInfo(SocketIOClient client,Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO,client)) {
            return;
        }
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String gameId = postData.getString("gid");
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        JSONObject result=new JSONObject();
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        JSONObject sysSet=publicBiz.getSysBaseSet();
        JSONObject set=publicBiz.getAPPGameSetting();
        if (!Dto.isObjNull(set)&&set.getString("isXipai").equals("1")&&!set.getString("xipaiLayer").equals("0")&&!Dto.stringIsNULL(set.getString("xipaiCount"))) {
            //总洗牌次数
            result.element("allCount", set.getInt("xipaiLayer"));
            if (set.getString("xipaiObj").equals("1")) {
                result.element("obj",sysSet.getString("cardname"));
            }else if (set.getString("xipaiObj").equals("2")) {
                result.element("obj",sysSet.getString("coinsname"));
            }else if (set.getString("xipaiObj").equals("3")) {
                result.element("obj",sysSet.getString("yuanbaoname"));
            }
            JSONObject userInfo = userBiz.getUserByAccount(account);
            int size=userInfo.getInt("pumpVal");
            if (size<set.getInt("xipaiLayer")) {
                String[] count=set.getString("xipaiCount").substring(1, set.getString("xipaiCount").length()-1).split("\\$");
                if (count.length>size) {
                    result.element("cur", size+1);
                    //扣除数额
                    result.element("sum", count[size]);
                    result.element(CommonConstant.RESULT_KEY_MSG, "可以洗牌");
                    result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                }else {
                    result.element("cur", size+1);
                    result.element(CommonConstant.RESULT_KEY_MSG, "扣除次数已达上限");
                }
            }else {
                result.element("cur", size+1);
                result.element(CommonConstant.RESULT_KEY_MSG, "扣除次数已达上限");
            }
        }else {
            result.element(CommonConstant.RESULT_KEY_MSG, "无此功能");
        }
        CommonConstant.sendMsgEventToSingle(client, result.toString(),"xipaiMessaPush");
    }

    /**
     * 洗牌
     * @param client
     * @param data
     */
    public void doShuffle(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO,client)) {
            return;
        }
        JSONObject result=new JSONObject();
        result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        result.element(CommonConstant.RESULT_KEY_MSG, "操作失败，稍后重试");
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String gameId = postData.getString("gid");
        // 当前洗牌次数
        int currentTime = postData.getInt("cur");
        // 总次数
        int totalTime = postData.getInt("allCount");
        double sum = postData.getDouble("sum");
        // 当前洗牌消耗元宝
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        // 洗牌次数大于总次数
        if (currentTime<=totalTime) {
            if (sum<=room.getPlayerMap().get(account).getScore()) {
                double oldScore = room.getPlayerMap().get(account).getScore();
                room.getPlayerMap().get(account).setScore(Dto.sub(oldScore,sum));
                // 更新玩家分数
                JSONObject obj = new JSONObject();
                obj.put("account", account);
                obj.put("updateType", room.getUpdateType());
                obj.put("sum", -sum);
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_USER_INFO, obj));
                // 插入洗牌数据
                if (room.getId()==0) {
                    JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
                    if (!Dto.isObjNull(roomInfo)) {
                        room.setId(roomInfo.getLong("id"));
                    }
                }
                JSONObject object = new JSONObject();
                object.put("userId",room.getPlayerMap().get(account).getId());
                object.put("gameId",gameId);
                object.put("room_id",room.getId());
                object.put("room_no",roomNo);
                object.put("new",room.getPlayerMap().get(account).getScore());
                object.put("old",oldScore);
                object.put("change",-sum);
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_APP_OBJ_REC, object));
                // 通知玩家
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.element("index",room.getPlayerMap().get(account).getMyIndex());
                result.element("score",room.getPlayerMap().get(account).getScore());
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"xipaiFunPush");
                return;
            }else {
                result.element(CommonConstant.RESULT_KEY_MSG, "余额不足");
            }
        }else {
            result.element(CommonConstant.RESULT_KEY_MSG, "洗牌次数已达上限");
        }
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"xipaiFunPush");
    }

    /**
     * 发送消息
     * @param client
     * @param data
     */
    public void sendMessage(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO,client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int type = postData.getInt("type");
        String content = postData.getString("data");
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        //敏感词替代
        String backData = SensitivewordFilter.replaceSensitiveWord(content, 1, "*");
        JSONObject result = new JSONObject();
        result.put("user", room.getPlayerMap().get(account).getMyIndex());
        result.put("type", type);
        result.put("data", backData);
        CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),result.toString(),"sendMsgEventPush");
    }

    /**
     * 发送语音
     * @param client
     * @param data
     */
    public void sendVoice(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO,client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        // 游戏语音通知
        int gameId=postData.getInt("gid");
        switch (gameId) {
            case CommonConstant.GAME_ID_NN:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),postData.toString(),"voiceCallGamePush_NN");
                break;
            case CommonConstant.GAME_ID_SSS:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),postData.toString(),"voiceCallGamePush_SSS");
                break;
            case CommonConstant.GAME_ID_ZJH:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),postData.toString(),"voiceCallGamePush_ZJH");
                break;
            default:
                break;
        }
    }

    /**
     * 子游戏接口
     * @param client
     * @param data
     */
    public void getRoomGid(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject result = new JSONObject();
        if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"房间不存在");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"getRoomGidPush");
            return;
        }
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        if (gameRoom.getPlayerMap().size()>=gameRoom.getPlayerCount()) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG,"房间已满");
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"getRoomGidPush");
            return;
        }
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"getRoomGidPush");
        result.put("gid",gameRoom.getGid());
    }

    /**
     * 金币场加入房间
     * @param client
     * @param data
     */
    public void joinCoinRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        int account = postData.getInt(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject obj = new JSONObject();
        obj.put("gameId",gameId);
        // TODO: 2018/5/10 平台号写死 
        obj.put("platform","SDTQP");
        JSONObject goldSetting = publicBiz.getGoldSetting(obj);
        postData.put("base_info",JSONObject.fromObject(goldSetting.getString("option")));
        List<String> roomNoList = new ArrayList<String>();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_JB&&room.getGid()==gameId&&
                !room.getPlayerMap().containsKey(account)&&room.getPlayerMap().size()<room.getPlayerCount()) {
                roomNoList.add(roomNo);
            }
        }
        if (roomNoList.size()==0) {
            createRoomBase(client,postData);
        }else {
            Collections.sort(roomNoList);
            postData.put("room_no",roomNoList.get(0));
            joinRoomBase(client,postData);
        }
    }

    /**
     * 测试-机器人加入创建房间
     * @param client
     * @param data
     */
    public void test(SocketIOClient client,Object data){
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String account = postData.getString("account");
        List<String> roomNoList = new ArrayList<String>();
        int limit = postData.getInt("limit");
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getGid()==gameId&&!room.getPlayerMap().containsKey(account)&&room.getPlayerMap().size()<=limit) {
                roomNoList.add(roomNo);
                break;
            }
        }
        if (roomNoList.size()>0) {
            postData.put("room_no",roomNoList.get(0));
            joinRoomBase(client,postData);
        }else {
            createRoomBase(client,data);
        }
    }

    /**
     * 测试-获取当前房间数和游戏中玩家数量
     * @param client
     * @param data
     */
    public void getRoomAndPlayerCount(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("game_id");
        int roomCount = 0;
        int playerCount = 0;
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getGid()==gameId) {
                roomCount ++;
                playerCount += room.getPlayerMap().size();
            }
        }
        JSONObject result = new JSONObject();
        result.put("roomCount",roomCount);
        result.put("playerCount",playerCount);
        CommonConstant.sendMsgEventToSingle(client,result.toString(),"getRoomAndPlayerCountPush");
    }
 }
