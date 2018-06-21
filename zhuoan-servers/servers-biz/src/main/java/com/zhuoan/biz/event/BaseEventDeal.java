package com.zhuoan.biz.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.event.bdx.BDXGameEventDealNew;
import com.zhuoan.biz.event.gppj.GPPJGameEventDeal;
import com.zhuoan.biz.event.nn.NNGameEventDealNew;
import com.zhuoan.biz.event.qzmj.QZMJGameEventDeal;
import com.zhuoan.biz.event.sss.SSSGameEventDealNew;
import com.zhuoan.biz.event.sw.SwGameEventDeal;
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
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.biz.model.gppj.UserPacketGPPJ;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.UserPacketQZMJ;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
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
    private QZMJGameEventDeal qzmjGameEventDeal;

    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;

    @Resource
    private SwGameEventDeal swGameEventDeal;

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
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB && userInfo.containsKey("yuanbao")) {
            double minScore = obtainBankMinScore(postData);
            if (minScore!=-1&&userInfo.getDouble("yuanbao") < minScore) {
                // 元宝不足
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.element(CommonConstant.RESULT_KEY_MSG, "元宝不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                return;
            }
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")
            && userInfo.getDouble("coins") < baseInfo.getDouble("goldCoinEnter")) {
            // 金币不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "金币不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FK && userInfo.containsKey("roomcard")) {
            int roomCard = getRoomCardPayInfo(baseInfo);
            if (userInfo.getInt("roomcard")<roomCard) {
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.element(CommonConstant.RESULT_KEY_MSG, "房卡不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                return;
            }
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_COMPETITIVE && userInfo.containsKey("roomcard")
            && userInfo.getDouble("roomcard") < baseInfo.getDouble("goldCoinEnter")) {
            // 金币不足
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "钻石不足");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        if (!userInfo.containsKey("uuid")||Dto.stringIsNULL(userInfo.getString("uuid"))||
            !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            return;
        }
        // 添加工会信息
        if (!Dto.isObjNull(userInfo)&&userInfo.containsKey("gulidId")&&userInfo.getInt("gulidId")>0&&
            userInfo.containsKey("unionid")&&userInfo.containsKey("platform")) {
            JSONObject ghInfo = userBiz.getGongHui(userInfo);
            if (!Dto.isObjNull(ghInfo)&&ghInfo.containsKey("name")) {
                userInfo.put("ghName",ghInfo.getString("name"));
            }
        }
        if (!checkBaseInfo(postData.getJSONObject("base_info"),postData.getInt("gid"))) {
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "参数不正确");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        // 创建房间
        createRoomBase(client, postData, userInfo);
    }

    private boolean checkBaseInfo(JSONObject baseInfo, int gameId) {
        JSONObject gameInfo = getGameInfoById(gameId);
        // 房间类型是否开放
        if (gameInfo.containsKey("openRoomType")&&gameInfo.getJSONArray("openRoomType").contains(baseInfo.getInt("roomType"))) {
            if (gameId==CommonConstant.GAME_ID_BDX) {
                return true;
            }
            // 游戏模式是否开放
            if (gameInfo.containsKey("openType")&&gameInfo.getJSONArray("openType").contains(baseInfo.getInt("type"))) {
                // 入场、离场是否满足条件
                if (checkScoreRatio(baseInfo,gameInfo)) {
                    // 判断最大倍数是否超出
                    if (checkMaxTimes(baseInfo,gameInfo)) {
                        // 判断人数是否超出
                        if (checkPlayerNum(baseInfo,gameInfo)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean checkScoreRatio(JSONObject baseInfo,JSONObject gameInfo) {
        int type = baseInfo.getInt("type");
        int ratio = 0;
        JSONArray array = gameInfo.getJSONArray("scoreRatio");
        if (baseInfo.getInt("roomType")==CommonConstant.ROOM_TYPE_YB) {
            if (baseInfo.containsKey("yuanbao")&&baseInfo.containsKey("leaveYB")&&baseInfo.containsKey("enterYB")) {
                for (int i = 0; i < array.size(); i++) {
                    if (array.getJSONObject(i).containsKey("type")&&array.getJSONObject(i).getInt("type")==type) {
                        ratio = array.getJSONObject(i).getInt("ybRatio");
                        break;
                    }
                }
                if (ratio==0||baseInfo.getDouble("yuanbao")<=0||
                    baseInfo.getDouble("yuanbao")*ratio>baseInfo.getDouble("leaveYB")||
                    baseInfo.getDouble("yuanbao")*ratio>baseInfo.getDouble("enterYB")) {
                    return false;
                }
            }else {
                return false;
            }
        }else if (baseInfo.getInt("roomType")==CommonConstant.ROOM_TYPE_JB) {
            if (baseInfo.containsKey("di")&&baseInfo.containsKey("goldCoinEnter")&&baseInfo.containsKey("goldCoinLeave")) {
                for (int i = 0; i < array.size(); i++) {
                    if (array.getJSONObject(i).containsKey("type")&&array.getJSONObject(i).getInt("type")==type) {
                        ratio = array.getJSONObject(i).getInt("jbRatio");
                        break;
                    }
                }
                if (ratio==0||baseInfo.getDouble("di")<=0||
                    baseInfo.getDouble("di")*ratio>baseInfo.getDouble("goldCoinLeave")||
                    baseInfo.getDouble("di")*ratio>baseInfo.getDouble("goldCoinEnter")) {
                    return false;
                }
            }else {
                return false;
            }
        }
        return true;
    }

    private boolean checkMaxTimes(JSONObject baseInfo,JSONObject gameInfo) {
        if (baseInfo.containsKey("baseNum")&&gameInfo.containsKey("maxXzTimes")) {
            JSONArray array = baseInfo.getJSONArray("baseNum");
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (obj.getInt("val")<0||obj.getInt("val")>gameInfo.getInt("maxXzTimes")) {
                    return false;
                }
            }
        }
        if (baseInfo.containsKey("qzTimes")&&gameInfo.containsKey("maxQzTimes")) {
            JSONArray array = baseInfo.getJSONArray("qzTimes");
            for (int i = 0; i < array.size(); i++) {
                if (array.getInt(i)<0 || array.getInt(i)>gameInfo.getInt("maxQzTimes")) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkPlayerNum(JSONObject baseInfo,JSONObject gameInfo) {
        int playerNum = baseInfo.getInt("player");
        if (baseInfo.containsKey("maxPlayer")) {
            playerNum = baseInfo.getInt("maxPlayer");
        }
        if (playerNum > gameInfo.getInt("maxplayer")) {
            return false;
        }
        return true;
    }


    /**
     * 创建房间创建实体对象
     * @param client
     * @param postData
     * @param userInfo
     */
    public void createRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        for (String roomNum : RoomManage.gameRoomMap.keySet()) {
            if (RoomManage.gameRoomMap.get(roomNum).getPlayerMap().containsKey(userInfo.getString("account"))) {
                return;
            }
        }
        JSONObject baseInfo = postData.getJSONObject("base_info");
        // 添加房间信息
        String roomNo = randomRoomNo();
        GameRoom gameRoom = createRoomByGameId(postData.getInt("gid"),baseInfo,userInfo);
        // 设置房间属性
        gameRoom.setRoomType(baseInfo.getInt("roomType"));
        gameRoom.setGid(postData.getInt("gid"));
        gameRoom.setPort(postData.getInt("port"));
        gameRoom.setIp(postData.getString("ip"));
        gameRoom.setRoomNo(roomNo);
        gameRoom.setRoomInfo(baseInfo);
        gameRoom.setCreateTime(new Date().toString());
        // 坐庄最小分数
        gameRoom.setMinBankerScore(obtainBankMinScore(postData));
        int playerNum = baseInfo.getInt("player");
        if (postData.getInt("gid") == CommonConstant.GAME_ID_SSS&&baseInfo.containsKey("maxPlayer")) {
            playerNum = baseInfo.getInt("maxPlayer");
        }
        List<Long> idList = new ArrayList<Long>();
        for (int i = 0; i < playerNum; i++) {
            if (i == 0) {
                idList.add(userInfo.getLong("id"));
            }else if ((gameRoom.getGid()==CommonConstant.GAME_ID_QZMJ||gameRoom.getGid()==CommonConstant.GAME_ID_NAMJ)&&playerNum==2&&i==1){
                // 麻将差异化，两人场坐对面
                idList.add(-1L);
                idList.add(0L);
            }else {
                idList.add(0L);
            }
        }
        gameRoom.setUserIdList(idList);
        // 支付类型
        if (baseInfo.containsKey("paytype")) {
            gameRoom.setPayType(baseInfo.getInt("paytype"));
        }
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_YB || gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_JB || gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE) {
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
                    if (gameRoom.getPayType() == CommonConstant.PAY_TYPE_AA) {
                        gameRoom.setEnterScore(turn.getInt("AANum"));
                    }
                }
            }else {
                gameRoom.setGameCount(999);
            }
        }
        if (baseInfo.containsKey("singleRoomCard")) {
            gameRoom.setSinglePayNum(baseInfo.getInt("singleRoomCard"));
        }
        // 底分
        if (baseInfo.containsKey("di")) {
            gameRoom.setScore(baseInfo.getDouble("di"));
        } else if (gameRoom.getGid()==CommonConstant.GAME_ID_QZMJ||gameRoom.getGid()==CommonConstant.GAME_ID_NAMJ){
            gameRoom.setScore(5);
        } else {
            gameRoom.setScore(1);
        }
        // 机器人
        if (baseInfo.containsKey("robot")&&baseInfo.getInt("robot")==CommonConstant.GLOBAL_YES) {
            // 机器人
            gameRoom.setRobot(true);
        }else {
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
        if (baseInfo.containsKey("open") && baseInfo.getInt("open") == 1 && postData.getInt("gid") != CommonConstant.GAME_ID_BDX) {
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
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB||baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_JB||
            baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_COMPETITIVE) {
            gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_OUT);
        } else if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_FK){
            gameRoom.setReadyOvertime(CommonConstant.READY_OVERTIME_NOTHING);
        }
        // 玩家人数
        gameRoom.setPlayerCount(playerNum);
        gameRoom.setLastIndex(playerNum);
        // 金币、元宝扣服务费
        if (baseInfo.getInt("roomType") == CommonConstant.ROOM_TYPE_YB) {

            /* 获取房间设置，插入缓存 */
            JSONObject gameSetting = getGameSetting(gameRoom);

            JSONObject gameInfo = getGameInfoById(postData.getInt("gid"));

            JSONObject roomFee = gameSetting.getJSONObject("pumpData");
            double fee;
            // 服务费：费率x底注
            if (baseInfo.containsKey("custFee")) {
                // 自定义费率
                if (gameInfo.containsKey("custFee")) {
                    fee = gameInfo.getDouble("custFee") * gameRoom.getScore();
                }else {
                    fee = baseInfo.getDouble("custFee") * gameRoom.getScore();
                }
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
        }else if (baseInfo.containsKey("fee")) {
            gameRoom.setFee(baseInfo.getDouble("fee"));
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
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FK&&gameRoom.getGameCount()==999) {
            playerinfo.setScore(100);
            if (gameRoom.getGid()==CommonConstant.GAME_ID_NAMJ) {
                playerinfo.setScore(15);
            }
        }
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        RoomManage.gameRoomMap.put(roomNo, gameRoom);
        // 通知玩家
        informUser(gameRoom,playerinfo,client);
        // 组织数据，插入数据库
        addGameRoom(gameRoom,playerinfo);
        // 开启机器人
        if (gameRoom.isRobot()) {
            robotEventDeal.robotJoin(roomNo);
        }
    }

    /**
     * 获取房间实体对象
     * @param gameId
     * @param baseInfo
     * @param userInfo
     * @return
     */
    public GameRoom createRoomByGameId(int gameId,JSONObject baseInfo,JSONObject userInfo) {
        GameRoom gameRoom;
        switch (gameId) {
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
            case CommonConstant.GAME_ID_QZMJ:
                gameRoom = new QZMJGameRoom();
                ((QZMJGameRoom)gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketQZMJ());
                createRoomQZMJ((QZMJGameRoom)gameRoom, baseInfo, userInfo.getString("account"));
                break;
            case CommonConstant.GAME_ID_NAMJ:
                gameRoom = new QZMJGameRoom();
                ((QZMJGameRoom)gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketQZMJ());
                createRoomNAMJ((QZMJGameRoom)gameRoom, baseInfo, userInfo.getString("account"));
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                gameRoom = new GPPJGameRoom();
                ((GPPJGameRoom)gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
                createRoomGPPJ((GPPJGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
                break;
            case CommonConstant.GAME_ID_SW:
                gameRoom = new SwGameRoom();
                createRoomSw((SwGameRoom) gameRoom, baseInfo, userInfo.getString("account"));
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
    public void informUser(GameRoom gameRoom,Playerinfo playerinfo,SocketIOClient client) {
        JSONObject object = new JSONObject();
        object.put(CommonConstant.DATA_KEY_ACCOUNT, playerinfo.getAccount());
        object.put(CommonConstant.DATA_KEY_ROOM_NO, gameRoom.getRoomNo());
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
            case CommonConstant.GAME_ID_QZMJ:
                qzmjGameEventDeal.createRoom(client, object);
                break;
            case CommonConstant.GAME_ID_NAMJ:
                qzmjGameEventDeal.createRoom(client, object);
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                gppjGameEventDeal.createRoom(client, object);
                break;
            case CommonConstant.GAME_ID_SW:
                swGameEventDeal.createRoom(client, object);
                break;
            default:
                break;
        }
    }

    /**
     * 插入房间数据
     * @param gameRoom
     * @param playerinfo
     */
    public void addGameRoom(GameRoom gameRoom,Playerinfo playerinfo) {
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
     * @return
     */
    public static String randomRoomNo(){
        String roomNo = MathDelUtil.getRandomStr(6);
        if (RoomManage.gameRoomMap.containsKey(roomNo)) {
            return randomRoomNo();
        }
        return roomNo;
    }

    /**
     * 获取游戏设置
     * @param gameRoom
     * @return
     */
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
        GameRoom room = RoomManage.gameRoomMap.get(roomNo);
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)) {
            // 用户不存在
            result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.element(CommonConstant.RESULT_KEY_MSG, "用户不存在");
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
            return;
        }
        if (!userInfo.containsKey("uuid")||Dto.stringIsNULL(userInfo.getString("uuid"))||
            !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            return;
        }
        // 添加工会信息
        if (!Dto.isObjNull(userInfo)&&userInfo.containsKey("gulidId")&&userInfo.getInt("gulidId")>0&&
            userInfo.containsKey("unionid")&&userInfo.containsKey("platform")) {
            JSONObject ghInfo = userBiz.getGongHui(userInfo);
            if (!Dto.isObjNull(ghInfo)&&ghInfo.containsKey("name")) {
                userInfo.put("ghName",ghInfo.getString("name"));
            }
        }
        // 重连不需要再次检查玩家积分
        if (!room.getPlayerMap().containsKey(account)||room.getPlayerMap().get(account)==null) {
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_YB && userInfo.containsKey("yuanbao")
                && userInfo.getDouble("yuanbao") < room.getEnterScore()) {
                // 元宝不足
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.element(CommonConstant.RESULT_KEY_MSG, "元宝不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                return;
            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB && userInfo.containsKey("coins")
                && userInfo.getDouble("coins") < room.getEnterScore()) {
                // 元宝不足
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.element(CommonConstant.RESULT_KEY_MSG, "金币不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                return;
            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK && userInfo.containsKey("roomcard")) {
                if (userInfo.getInt("roomcard") < room.getEnterScore()) {
                    result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                    result.element(CommonConstant.RESULT_KEY_MSG, "房卡不足");
                    CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                    return;
                }
            } else if (room.getRoomType() == CommonConstant.ROOM_TYPE_COMPETITIVE && userInfo.containsKey("roomcard")
                && userInfo.getDouble("roomcard") < room.getEnterScore()) {
                // 金币不足
                result.element(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.element(CommonConstant.RESULT_KEY_MSG, "钻石不足");
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush_NN");
                return;
            }
        }
        joinRoomBase(client, postData, userInfo);
    }

    /**
     * 加入房间创建实体对象
     * @param client
     * @param postData
     * @param userInfo
     */
    public void joinRoomBase(SocketIOClient client, JSONObject postData, JSONObject userInfo) {
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        for (String roomNum : RoomManage.gameRoomMap.keySet()) {
            if (!roomNum.equals(roomNo)&&RoomManage.gameRoomMap.get(roomNum).getPlayerMap().containsKey(userInfo.getString("account"))) {
                return;
            }
        }
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        JSONObject result = new JSONObject();
        int myIndex = -1;
        // 获取当前房间的第一个空位
        int begin = 0;
        // 水蛙从1号位开始
        if (gameRoom.getGid()==CommonConstant.GAME_ID_SW) {
            begin = 1;
        }
        for (int i = begin;i < gameRoom.getUserIdList().size(); i++) {
            if (gameRoom.getUserIdList().get(i) == 0||gameRoom.getUserIdList().get(i)==userInfo.getLong("id")) {
                myIndex = i;
                break;
            }
        }
        // 水蛙观战
        if (gameRoom.getGid()==CommonConstant.GAME_ID_SW&&myIndex==-1) {
            myIndex = gameRoom.getLastIndex();
            gameRoom.setLastIndex(myIndex+1);
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
        if (myIndex<gameRoom.getUserIdList().size()) {
            gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
        }
        RoomManage.gameRoomMap.put(roomNo, gameRoom);
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo", userInfo);
        obtainPlayerInfoData.put("myIndex", myIndex);
        if (client!=null) {
            obtainPlayerInfoData.put("uuid", String.valueOf(client.getSessionId()));
        }else {
            obtainPlayerInfoData.put("uuid", String.valueOf(UUID.randomUUID()));
        }
        obtainPlayerInfoData.put("room_type", gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location", postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        // 麻将一刻设置底分
        if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FK&&gameRoom.getGameCount()==999) {
            playerinfo.setScore(100);
            if (gameRoom.getGid()==CommonConstant.GAME_ID_NAMJ) {
                playerinfo.setScore(15);
            }
        }
        JSONObject joinData = new JSONObject();
        // 是否重连
        if (gameRoom.getPlayerMap().containsKey(playerinfo.getAccount())) {
            // 房卡场不需要重新刷新分数
            if (gameRoom.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                playerinfo.setScore(gameRoom.getPlayerMap().get(playerinfo.getAccount()).getScore());
            }
            joinData.put("isReconnect", CommonConstant.GLOBAL_YES);
        } else {
            // 更新数据库
            if (myIndex<10) {
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
        joinData.put(CommonConstant.DATA_KEY_ACCOUNT, userInfo.getString("account"));
        joinData.put(CommonConstant.DATA_KEY_ROOM_NO, roomNo);
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
            case CommonConstant.GAME_ID_QZMJ:
                // 重连不需要重新设置用户牌局信息
                if (!((QZMJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketQZMJ());
                }
                qzmjGameEventDeal.joinRoom(client, joinData);
                break;
            case CommonConstant.GAME_ID_NAMJ:
                // 重连不需要重新设置用户牌局信息
                if (!((QZMJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((QZMJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketQZMJ());
                }
                qzmjGameEventDeal.joinRoom(client, joinData);
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                // 重连不需要重新设置用户牌局信息
                if (!((GPPJGameRoom) gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((GPPJGameRoom) gameRoom).getUserPacketMap().put(userInfo.getString("account"), new UserPacketGPPJ());
                }
                gppjGameEventDeal.joinRoom(client, joinData);
                break;
            case CommonConstant.GAME_ID_SW:
                swGameEventDeal.joinRoom(client, joinData);
                break;
            default:
                break;
        }
    }

    /**
     * 获取玩家信息
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
        } else if (roomType == CommonConstant.ROOM_TYPE_COMPETITIVE) {
            // 竞技场
            playerinfo.setScore(userInfo.getDouble("score"));
        } else{
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
        if (userInfo.containsKey("ghName")&&!Dto.stringIsNULL(userInfo.getString("ghName"))) {
            playerinfo.setGhName(userInfo.getString("ghName"));
        }
        if (userInfo.containsKey("roomcard")) {
            playerinfo.setRoomCardNum(userInfo.getInt("roomcard"));
        }
        if (userInfo.containsKey("area")) {
            playerinfo.setArea(userInfo.getString("area"));
        } else {
            playerinfo.setArea("");
        }
        if (userInfo.containsKey("lv")) {
            int vip = userInfo.getInt("lv");
            if (vip > 1) {
                playerinfo.setVip(vip - 1);
            } else {
                playerinfo.setVip(0);
            }
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
            case NNConstant.NN_BANKER_TYPE_ZZ:
                wanFa = "坐庄模式";
                break;
            default:
                break;
        }
        room.setWfType(wanFa);
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        JSONObject setting = getGameInfoById(CommonConstant.GAME_ID_NN);
        // 设置基本牌型倍数
        if (baseInfo.containsKey("special")) {
            List<Integer> specialType = new ArrayList<Integer>();
            JSONArray types = setting.getJSONArray("special");
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
            obj.put("name",baseInfo.getDouble("yuanbao"));
            array.add(obj);
            room.setBaseNum(array.toString());
        }
        room.getUserPacketMap().put(account, new UserPacket());
    }

    /**
     * 设置十三水房间特殊参数
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
            case SSSConstant.SSS_BANKER_TYPE_ZZ:
                wanFa = "坐庄模式";
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
        // 加色
        if (baseInfo.containsKey("color")) {
            room.setColor(baseInfo.getInt("color"));
        }
        // 马牌
        if (baseInfo.containsKey("jiama")) {
            room.setMaPaiType(baseInfo.getInt("jiama"));
            switch (room.getMaPaiType()) {
                case SSSConstant.SSS_MP_TYPE_ALL_HT:
                    room.setMaPai("1-"+(RandomUtils.nextInt(13)+1));
                    break;
                case SSSConstant.SSS_MP_TYPE_HT_A:
                    room.setMaPai("1-1");
                    break;
                case SSSConstant.SSS_MP_TYPE_HT_3:
                    room.setMaPai("1-3");
                    break;
                case SSSConstant.SSS_MP_TYPE_HT_5:
                    room.setMaPai("1-5");
                    break;
                case SSSConstant.SSS_MP_TYPE_ALL:
                    int num = RandomUtils.nextInt(13)+1;
                    int color = RandomUtils.nextInt(4)+1;
                    room.setMaPai(color+"-"+num);
                    break;
                default:
                    break;
            }
        }
        if (baseInfo.containsKey("baseNum")) {
            // 设置基础倍率
            room.setBaseNum(baseInfo.getJSONArray("baseNum").toString());
        }
        /* 获取游戏信息设置,插入缓存 */
        JSONObject setting = getGameInfoById(CommonConstant.GAME_ID_SSS);
        if (baseInfo.containsKey("peiPaiTime")) {
            setting.put("goldpeipai",baseInfo.getInt("peiPaiTime"));
        }
        room.setSetting(setting);
        room.getUserPacketMap().put(account, new Player());
    }

    /**
     * 创建骨牌牌九房间
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomGPPJ(GPPJGameRoom room, JSONObject baseInfo, String account) {
        room.setBankerType(baseInfo.getInt("type"));
        // 玩法
        String wanFa = "";
        switch (baseInfo.getInt("type")) {
            case GPPJConstant.BANKER_TYPE_OWNER:
                wanFa = "房主坐庄";
                break;
            case GPPJConstant.BANKER_TYPE_LOOK:
                wanFa = "看牌抢庄";
                break;
            case GPPJConstant.BANKER_TYPE_COMPARE:
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
        JSONObject setting = getGameInfoById(CommonConstant.GAME_ID_GP_PJ);
        // 下注倍数
        if (baseInfo.containsKey("baseNum")) {
            room.setBaseNum(baseInfo.getJSONArray("baseNum"));
        }else {
            room.setBaseNum(setting.getJSONArray("xzTimes"));
        }
        // 抢庄倍数
        room.setQzTimes(setting.getJSONArray("qzTimes"));
        // 倍数
        if (baseInfo.containsKey("multiple")) {
            room.setMultiple(baseInfo.getInt("multiple"));
        }
    }

    /**
     * 设置泉州麻将房间参数
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomQZMJ(QZMJGameRoom room, JSONObject baseInfo, String account) {
        room.setWfType("泉州麻将");
        if (baseInfo.containsKey("type")) {
            room.setYouJinScore(baseInfo.getInt("type"));
        }
        room.setPaiCount(QZMJConstant.HAND_PAI_COUNT);
        // 光游
        if(baseInfo.getJSONObject("turn").containsKey("guangyou")){
            room.isGuangYou = true;
        }else{
            room.isGuangYou = false;
        }
        // 有金不平胡
        if(baseInfo.containsKey("hasjinnopinghu")&&baseInfo.getInt("hasjinnopinghu")==1){
            room.hasJinNoPingHu = true;
        }else{
            room.hasJinNoPingHu = false;
        }
        // 是否没有吃、胡
        if(baseInfo.containsKey("isNotChiHu")&&baseInfo.getInt("isNotChiHu")==1){
            room.isNotChiHu = true;
        }else{
            room.isNotChiHu = false;
        }
        // 一课牌局积分是否可以超出（负数）
        if(baseInfo.getJSONObject("turn").containsKey("isOver")&&baseInfo.getJSONObject("turn").getInt("isOver")==1){
            room.isCanOver = true;
        }else{
            room.isCanOver = false;
        }
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        room.getUserPacketMap().put(account,new UserPacketQZMJ());
    }

    /**
     * 设置南安麻将房间参数
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomNAMJ(QZMJGameRoom room, JSONObject baseInfo, String account) {
        if (baseInfo.containsKey("type")) {
            room.setYouJinScore(baseInfo.getInt("type"));
        }
        room.setPaiCount(QZMJConstant.HAND_PAI_COUNT);
        // 光游
        room.isGuangYou = false;
        // 有金不平胡
        room.hasJinNoPingHu = false;
        // 没有吃，平胡
        room.isNotChiHu = true;
        // 一课牌局积分是否可以超出（负数）
        if(baseInfo.getJSONObject("turn").containsKey("isOver")&&baseInfo.getJSONObject("turn").getInt("isOver")==1){
            room.isCanOver = true;
        }else{
            room.isCanOver = false;
        }
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        room.getUserPacketMap().put(account,new UserPacketQZMJ());
    }

    /**
     * 设置水蛙房间参数
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomSw(SwGameRoom room,JSONObject baseInfo, String account) {
        room.setWfType("水蛙");
        // 庄家
        room.setBanker(account);
        // 房主
        room.setOwner(account);
        // 设置赔率
        switch (baseInfo.getInt("type")) {
            case SwConstant.SW_TYPE_TEN:
                room.setRatio(10);
                break;
            case SwConstant.SW_TYPE_TEN_POINT_FIVE:
                room.setRatio(10.5);
                break;
            default:
                room.setRatio(10);
                break;
        }
        // 下注倍数
        JSONObject setting = getGameInfoById(CommonConstant.GAME_ID_SW);
        if (baseInfo.containsKey("singleMax")) {
            room.setSingleMax(baseInfo.getInt("singleMax"));
        }
        if (baseInfo.containsKey("baseNum")) {
            room.setBaseNum(baseInfo.getJSONArray("baseNum"));
        }else {
            room.setBaseNum(setting.getJSONArray("xzTimes"));
        }
    }

    /**
     * 设置十三水房间特殊参数
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
            JSONArray baseNum = new JSONArray();
            for (int i = 0; i < baseInfo.getJSONArray("baseNum").size(); i++) {
                baseNum.add(baseInfo.getJSONArray("baseNum").getJSONObject(i).getInt("val"));
            }
            room.setBaseNum(baseNum);
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

    /**
     * 根据游戏id获取游戏配置
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
            if (object!=null) {
                gameInfoById = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(key)));

            }else {
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
     * @param client
     * @param data
     */
    public void getGameSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gid = postData.getInt("gid");
        String platform = postData.getString("platform");
        int flag = 1;
        if (postData.containsKey("flag")) {
            flag = postData.getInt("flag");
        }
        /* 查询房间设置,插入缓存*/
        JSONArray gameSetting = getGameSetting(gid, platform, flag);

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

    /**
     * 根据平台号获取游戏设置缓存
     * @param gid
     * @param platform
     * @return
     */
    private JSONArray getGameSetting(int gid, String platform, int flag) {
        String key = "game_setting_"+platform+"_"+gid;
        JSONArray gameSetting = new JSONArray();
        if (!key.equals("")) {
            try {
                StringBuffer sb = new StringBuffer();
                sb.append(key);
                sb.append("_");
                sb.append(platform);
                Object object = redisService.queryValueByKey(String.valueOf(sb));
                if (object!=null) {
                    gameSetting = JSONArray.fromObject(object);
                }else {
                    gameSetting = publicBiz.getRoomSetting(gid, platform, flag);
                    redisService.insertKey(String.valueOf(sb), String.valueOf(gameSetting), null);
                }
            } catch (Exception e) {
                logger.error("请启动REmote DIctionary Server");
                gameSetting = publicBiz.getRoomSetting(gid, platform, flag);
            }
        }
        return gameSetting;
    }

    private JSONObject getGameStatusInfo(int gameId) {
        JSONObject gameInfoById;
        try {
            StringBuffer key = new StringBuffer();
            key.append("game_on_or_off_");
            key.append(gameId);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object!=null) {
                gameInfoById = JSONObject.fromObject(redisService.queryValueByKey(String.valueOf(key)));

            }else {
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
     * @param client
     * @param data
     */
    public void checkUser(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("gid")&&postData.getInt("gid")>0) {
            int gameId = postData.getInt("gid");
            JSONObject gameStatusInfo = getGameStatusInfo(gameId);
            if (Dto.isObjNull(gameStatusInfo)||!gameStatusInfo.containsKey("status")||
                gameStatusInfo.getInt("status")!=1) {
                JSONObject result = new JSONObject();
                result.put("type",CommonConstant.SHOW_MSG_TYPE_NORMAL);
                result.put(CommonConstant.RESULT_KEY_MSG,"该游戏正在维护中");
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
                return;
            }
        }
        if (postData.containsKey("account")) {
            String account = postData.getString("account");
            // 遍历房间列表
            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                if (!Dto.stringIsNULL(account) && gameRoom.getPlayerMap().containsKey(account) && gameRoom.getPlayerMap().get(account) != null) {
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
            userInfo.remove("uuid");
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
            if (RoomManage.gameRoomMap.get(roomNo).getGid()!=CommonConstant.GAME_ID_BDX&&
                RoomManage.gameRoomMap.get(roomNo).getGid()==gameId&&RoomManage.gameRoomMap.get(roomNo).isOpen()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                JSONObject obj = new JSONObject();
                obj.put("room_no", gameRoom.getRoomNo());
                obj.put("gid", gameId);
                obj.put("base_info", gameRoom.getRoomInfo());
                obj.put("fytype", gameRoom.getWfType());
                obj.put("iszs", 0);
                obj.put("player", gameRoom.getPlayerCount());
                obj.put("renshu", gameRoom.getPlayerMap().size());
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
                    obj.put("glog_id",userGameLog.getString("gamelog_id"));
                    obj.put("id",userGameLog.getString("id"));
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

    /**
     * 根据平台号获取滚动公告缓存
     * @param platform
     * @return
     */
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
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush_NN");
                break;
            case CommonConstant.GAME_ID_SSS:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush_SSS");
                break;
            case CommonConstant.GAME_ID_ZJH:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush_ZJH");
                break;
            case CommonConstant.GAME_ID_QZMJ:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush");
                break;
            case CommonConstant.GAME_ID_NAMJ:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush");
                break;
            case CommonConstant.GAME_ID_GP_PJ:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush_GPPJ");
                break;
            default:
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account),postData.toString(),"voiceCallGamePush");
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
     * 获取房卡场支付信息
     * @param client
     * @param data
     */
    public void getRoomCardPayInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("base_info")) {
            JSONObject baseInfo = postData.getJSONObject("base_info");
            if (!baseInfo.containsKey("player")||!baseInfo.containsKey("paytype")||!baseInfo.containsKey("turn")) {
                return;
            }
            int roomCard = getRoomCardPayInfo(baseInfo);
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("roomcard",roomCard);
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"getRoomCardPayInfoPush");
        }
    }

    /**
     * 获取房卡场支付信息
     * @param baseInfo
     * @return
     */
    public int getRoomCardPayInfo(JSONObject baseInfo) {
        int roomCard = 0;
        if (baseInfo.containsKey("player")&&baseInfo.containsKey("paytype")) {
            int player = baseInfo.getInt("player");
            int payType = baseInfo.getInt("paytype");
            JSONObject turn = baseInfo.getJSONObject("turn");
            if (turn.containsKey("AANum")) {
                int single = turn.getInt("AANum");
                if (payType==CommonConstant.PAY_TYPE_AA) {
                    roomCard = single;
                }
                if (payType==CommonConstant.PAY_TYPE_OWNER) {
                    roomCard = single*player;
                }
            }
        }
        return roomCard;
    }

    /**
     * 金币场加入房间
     * @param client
     * @param data
     */
    public void joinCoinRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        JSONObject option = postData.getJSONObject("option");
        postData.put("base_info",option);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (Dto.stringIsNULL(account)) {
            return;
        }
        List<String> roomNoList = new ArrayList<String>();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_JB&&room.getGid()==gameId&&room.getScore()==option.getDouble("di")&&
                !room.getPlayerMap().containsKey(account)&&room.getPlayerMap().size()<room.getPlayerCount()) {
                roomNoList.add(roomNo);
            }
        }
        if (roomNoList.size()==0) {
            createRoomBase(client,postData);
        }else {
            // 随机加入
            Collections.shuffle(roomNoList);
            postData.put("room_no",roomNoList.get(0));
            joinRoomBase(client,postData);
        }
    }

    /**
     *
     * 金币场房间设置缓存
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
            }else {
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
     * @param client
     * @param data
     */
    public void getCoinSetting(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String platform = postData.getString("platform");
        JSONObject obj = new JSONObject();
        obj.put("gameId",gameId);
        obj.put("platform",platform);
        JSONArray goldSettings = getGoldSettingByGameIdAndPlatform(obj);
        for (int i = 0; i < goldSettings.size(); i++) {
            JSONObject goldSetting = goldSettings.getJSONObject(i);
            goldSetting.put("online",goldSetting.getInt("online")+1);
            goldSetting.put("enter",goldSetting.getJSONObject("option").getInt("goldCoinEnter"));
            goldSetting.put("leave",goldSetting.getJSONObject("option").getInt("goldCoinLeave"));
        }
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
        result.put("data",goldSettings);
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"getGameGoldSettingPush");
    }

    /**
     * 用户签到信息
     * @param client
     * @param data
     */
    public void checkSignIn(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        long userId = postData.getLong("userId");
        String platform = postData.getString("platform");
        // 当前日期
        String nowTime = TimeUtil.getNowDateymd()+" 00:00:00";
        JSONObject signInfo = publicBiz.getUserSignInfo(platform,userId);
        JSONObject result = new JSONObject();
        int minReward = getCoinsSignMinReward(platform);
        int maxReward = getCoinsSignMaxReward(platform);
        if (Dto.isObjNull(signInfo)) {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("reward",minReward);
            result.put("days",0);
        }else {
            if (!TimeUtil.isLatter(signInfo.getString("createtime"),nowTime)) {
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                String yesterday = TimeUtil.addDaysBaseOnNowTime(nowTime,-1,"yyyy-MM-dd HH:mm:ss");
                if (TimeUtil.isLatter(signInfo.getString("createtime"),yesterday)) {
                    int reward = (signInfo.getInt("singnum")+1)*minReward;
                    if (reward>maxReward) {
                        reward = maxReward;
                    }
                    result.put("reward",reward);
                    result.put("days",signInfo.getInt("singnum"));
                }else {
                    result.put("reward",minReward);
                    result.put("days",0);
                }
            }else {
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            }
        }
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"checkSignInPush");
    }

    /**
     * 最少签到金币
     * @return
     */
    public int getCoinsSignMinReward(String platform) {
        int minReward = CommonConstant.COINS_SIGN_MIN;
        JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
        if (!Dto.isObjNull(signRewardInfo)&&signRewardInfo.containsKey("meitmoney")) {
            return signRewardInfo.getInt("meitmoney");
        }
        return minReward;
    }

    /**
     * 获取签到奖励缓存
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
            }else {
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
     * 最多签到金币
     * @return
     */
    public int getCoinsSignMaxReward(String platform) {
        int maxReward = CommonConstant.COINS_SIGN_MAX;
        JSONObject signRewardInfo = getCoinsSignRewardInfo(platform);
        if (!Dto.isObjNull(signRewardInfo)&&signRewardInfo.containsKey("monthmoney")) {
            return signRewardInfo.getInt("monthmoney");
        }
        return maxReward;
    }

    /**
     * 用户签到
     * @param client
     * @param data
     */
    public void doUserSignIn(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        long userId = postData.getLong("userId");
        String platform = postData.getString("platform");
        String account = postData.getString("account");
        // 当前日期
        String nowTime = TimeUtil.getNowDate();
        // 签到信息
        JSONObject signInfo = publicBiz.getUserSignInfo(platform,userId);
        JSONObject object = new JSONObject();
        JSONObject result = new JSONObject();
        int reward = getCoinsSignMinReward(platform);
        if (Dto.isObjNull(signInfo)) {
            object.put("singnum",1);
            object.put("createtime",nowTime);
            object.put("userID",userId);
            object.put("platform",platform);
        }else {
            object.put("id",signInfo.getLong("id"));
            String today = TimeUtil.getNowDateymd()+" 00:00:00";
            String yesterday = TimeUtil.addDaysBaseOnNowTime(today,-1,"yyyy-MM-dd HH:mm:ss");
            // 今日已签到
            if (TimeUtil.isLatter(signInfo.getString("createtime"),today)) {
                result.put(CommonConstant.RESULT_KEY_CODE,-1);
                result.put(CommonConstant.RESULT_KEY_MSG,"今日已签到");
                CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"userSignInPush");
                return;
            }
            if (TimeUtil.isLatter(signInfo.getString("createtime"),yesterday)) {
                object.put("singnum",signInfo.getInt("singnum")+1);
                reward = (signInfo.getInt("singnum")+1)*getCoinsSignMinReward(platform);
                int maxReward = getCoinsSignMaxReward(platform);
                if (reward>maxReward) {
                    reward = maxReward;
                }
            }else {
                object.put("singnum",1);
            }
            object.put("createtime",nowTime);
        }
        if (!Dto.isObjNull(object)) {
            int back = publicBiz.addOrUpdateUserSign(object);
            JSONObject userInfo = userBiz.getUserByAccount(account);
            if (back>0&&!Dto.isObjNull(userInfo)) {
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
                result.put("newScore",userInfo.getInt("coins")+reward);
                result.put("days",object.getInt("singnum"));
                JSONObject obj = new JSONObject();
                obj.put("account", account);
                obj.put("updateType", "coins");
                obj.put("sum", reward);
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_USER_INFO, obj));
            }else {
                result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            }
        }else {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
        }
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"userSignInPush");
    }


    public double obtainBankMinScore(JSONObject postData) {
        if (!postData.containsKey("gid")||!postData.containsKey("base_info")) {
            return -1;
        }
        JSONObject baseInfo = postData.getJSONObject("base_info");
        if (!baseInfo.containsKey("enterYB")||!baseInfo.containsKey("player")||!baseInfo.containsKey("yuanbao")) {
            return -1;
        }
        int gameId = postData.getInt("gid");
        double minScore = baseInfo.getDouble("enterYB");
        int noBankerNum = baseInfo.getInt("player")-1;
        int maxNum = 1;
        if (baseInfo.containsKey("baseNum")) {
            JSONArray baseNum = baseInfo.getJSONArray("baseNum");
            for (int i = 0; i < baseNum.size(); i++) {
                if (baseNum.getJSONObject(i).getInt("val")>maxNum) {
                    maxNum = baseNum.getJSONObject(i).getInt("val");
                }
            }
        }
        if (gameId==CommonConstant.GAME_ID_NN&&baseInfo.getInt("type")==NNConstant.NN_BANKER_TYPE_ZZ) {
            minScore = noBankerNum*maxNum*baseInfo.getDouble("yuanbao")*5;
        }
        if (gameId==CommonConstant.GAME_ID_SSS&&baseInfo.getInt("type")==SSSConstant.SSS_BANKER_TYPE_ZZ) {
            if (baseInfo.containsKey("maxPlayer")) {
                noBankerNum = baseInfo.getInt("maxPlayer")-1;
            }
            minScore = noBankerNum*maxNum*baseInfo.getDouble("yuanbao")*SSSConstant.SSS_XZ_BASE_NUM;
        }
        if (gameId==CommonConstant.GAME_ID_SW) {
            double ratio = 1;
            if (baseInfo.getInt("type")==SwConstant.SW_TYPE_TEN) {
                ratio = 10;
            }else if (baseInfo.getInt("type")==SwConstant.SW_TYPE_TEN_POINT_FIVE) {
                ratio = 10.5;
            }
            minScore = noBankerNum*3*baseInfo.getDouble("yuanbao")*ratio;
        }
        return minScore;
    }

    /**
     * 获取当前房间数和游戏中玩家数量
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

    /**
     * 获取竞技场信息
     * @param client
     * @param data
     */
    public void getCompetitiveInfo(SocketIOClient client, Object data) {
        JSONObject result = new JSONObject();
        JSONArray arenaArray = publicBiz.getArenaInfo();
        if (arenaArray.size()>0) {
            for (int i = 0; i < arenaArray.size(); i++) {
                JSONObject arena = arenaArray.getJSONObject(i);
                if (arena.containsKey("endTime")&&arena.containsKey("startTime")) {
                    TimeUtil.transTimeStamp(arena, "yyyy-MM-dd HH:mm:ss", "endTime");
                    TimeUtil.transTimeStamp(arena, "yyyy-MM-dd HH:mm:ss", "startTime");
                }
            }
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_YES);
            result.put("data",arenaArray);
        }else {
            result.put(CommonConstant.RESULT_KEY_CODE,CommonConstant.GLOBAL_NO);
            result.put("msg","暂无场次信息");
        }
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"getArenaPush");
    }

    /**
     * 竞技场加入房间
     * @param client
     * @param data
     */
    public void joinCompetitiveRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String platform = postData.getString("platform");
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (Dto.stringIsNULL(account)) {
            return;
        }
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getRoomType()==CommonConstant.ROOM_TYPE_COMPETITIVE&&room.getGid()==gameId&&
                !room.getPlayerMap().containsKey(account)&&room.getPlayerMap().size()<room.getPlayerCount()) {
                postData.put("room_no",roomNo);
                joinRoomBase(client,postData);
                return;
            }
        }
        JSONObject object = new JSONObject();
        object.put("gameId",gameId);
        object.put("platform",platform);
        JSONArray array = getGoldSettingByGameIdAndPlatform(object);
        if (array.size()>0) {
            postData.put("base_info",array.getJSONObject(0).getJSONObject("option"));
            createRoomBase(client,postData);
        }
    }

    public void gameCheckIp(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData,CommonConstant.CHECK_GAME_STATUS_NO,client)) {
            return;
        }
        JSONObject result=new JSONObject();
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        result.put("index",gameRoom.getPlayerMap().get(account).getMyIndex());
        result.put("ipStatus",CommonConstant.GLOBAL_YES);
        for (String uuid : gameRoom.getPlayerMap().keySet()) {
            if (!uuid.equals(account)) {
                if (gameRoom.getPlayerMap().get(account).getIp().equals(gameRoom.getPlayerMap().get(uuid).getIp())) {
                    result.put("ipStatus",CommonConstant.GLOBAL_NO);
                    break;
                }
            }
        }
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameCheckIpPush");
    }
 }
