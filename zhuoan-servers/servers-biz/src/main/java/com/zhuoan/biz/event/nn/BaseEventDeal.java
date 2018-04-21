package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.constant.Constant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    private UserBiz userBiz;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private PublicBiz publicBiz;

    @Resource
    private NNGameEventDealNew nnGameEventDealNew;

    /**
     * 创建房间
     * @param client
     * @param data
     */
    public void createRoomBase(SocketIOClient client, Object data){
        // 检查是否能加入房间
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString("account");
        // 房间信息
        JSONObject baseInfo = postData.getJSONObject("base_info");
        baseInfo.put("roomType",3);
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)){
            // 用户不存在
            result.put(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.put(NNConstant.RESULT_KEY_MSG,"用户不存在");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        } else if (baseInfo.getInt("roomType")==NNConstant.ROOM_TYPE_YB&&userInfo.containsKey("yuanbao")
            &&userInfo.getDouble("yuanbao")<baseInfo.getDouble("enterYB")) {
            // 元宝不足
            result.element(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.element(NNConstant.RESULT_KEY_MSG,"元宝不足");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        }
        // 设置客户端标识
        client.set(NNConstant.CLIENT_TAG_ACCOUNT,account);
        client.set(NNConstant.CLIENT_TAG_USER_INFO,userInfo);
        // 创建房间
        createRoomBase(client,JSONObject.fromObject(data),JSONObject.fromObject(client.get(NNConstant.CLIENT_TAG_USER_INFO)));
    }

    public void joinRoomBase(SocketIOClient client, Object data){
        JSONObject postData = fromObject(data);
        JSONObject result = new JSONObject();
        // 玩家账号
        String account = postData.getString("account");
        // 房间号
        String roomNo = postData.getString("roomNo");

        if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null){
            result.element(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.element(NNConstant.RESULT_KEY_MSG,"房间不存在");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        }
        // 获取用户信息
        JSONObject userInfo = userBiz.getUserByAccount(account);
        if (Dto.isObjNull(userInfo)){
            // 用户不存在
            result.element(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.element(NNConstant.RESULT_KEY_MSG,"用户不存在");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        } else if (RoomManage.gameRoomMap.get(roomNo).getRoomType()==NNConstant.ROOM_TYPE_YB&&userInfo.containsKey("yuanbao")
            &&userInfo.getDouble("yuanbao")<RoomManage.gameRoomMap.get(roomNo).getEnterScore()) {
            // 元宝不足
            result.element(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.element(NNConstant.RESULT_KEY_MSG,"元宝不足");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        }
        // 设置客户端标识
        client.set(NNConstant.CLIENT_TAG_ACCOUNT,account);
        client.set(NNConstant.CLIENT_TAG_USER_INFO,userInfo);
        client.set(NNConstant.CLIENT_TAG_ROOM_NO,roomNo);

        joinRoomBase(client,postData,userInfo);
    }

    public void joinRoomBase(SocketIOClient client,JSONObject postData, JSONObject userInfo){
        String roomNo = postData.getString("roomNo");
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        JSONObject result=new JSONObject();
        int myIndex = -1;
        // 获取当前房间的第一个空位
        for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
            if (gameRoom.getUserIdList().get(i)==0) {
                myIndex = i;
                break;
            }
        }
        // 重连不需要重新获取座位号
        if (postData.containsKey("myIndex")) {
            myIndex = postData.getInt("myIndex");
        }
        if (myIndex<0) {
            result.put("code", 0);
            result.put("msg", "房间已满");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        }
        // 设置房间属性
        gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
        RoomManage.gameRoomMap.put(roomNo, gameRoom);
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo",userInfo);
        obtainPlayerInfoData.put("myIndex",myIndex);
        obtainPlayerInfoData.put("uuid", client.getSessionId().toString());
        obtainPlayerInfoData.put("room_type",gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location",postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        JSONObject joinData = new JSONObject();
        // 是否重连
        if (gameRoom.getPlayerMap().containsKey(playerinfo.getAccount())) {
            joinData.put("isReconnect",1);
        }else {
            // 更新数据库
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room_no",gameRoom.getRoomNo());
            // TODO: 2018/4/20 字符串拼接
            roomInfo.put("user_id"+myIndex,playerinfo.getId());
            roomInfo.put("user_icon"+myIndex,playerinfo.getHeadimg());
            roomInfo.put("user_name"+myIndex,playerinfo.getName());
            roomBiz.updateGameRoom(roomInfo);
            joinData.put("isReconnect",0);
        }
        joinData.put("account",userInfo.getString("account"));
        joinData.put("room_no",roomNo);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        // 通知玩家
        switch (gameRoom.getGid()){
            case NNConstant.GAME_ID_NN:
                // 重连不需要重新设置用户牌局信息
                if (!((NNGameRoomNew)gameRoom).getUserPacketMap().containsKey(userInfo.getString("account"))) {
                    ((NNGameRoomNew)gameRoom).getUserPacketMap().put(userInfo.getString("account"),new UserPacket());
                }
                nnGameEventDealNew.joinRoom(client,joinData);
                break;
            default:
                break;
        }
    }

    public void createRoomBase(SocketIOClient client,JSONObject postData, JSONObject userInfo){
        JSONObject baseInfo = postData.getJSONObject("base_info");
        // 添加房间信息
        String roomNo = RoomManage.randomRoomNo();
        client.set(NNConstant.CLIENT_TAG_ROOM_NO,roomNo);
        GameRoom gameRoom;
        switch (postData.getInt("gid")) {
            case NNConstant.GAME_ID_NN:
                gameRoom = new NNGameRoomNew();
                createRoomNN((NNGameRoomNew)gameRoom,baseInfo,userInfo.getString("account"));
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
        List<Long> idList = new ArrayList<Long>();
        List<String> iconList = new ArrayList<String>();
        List<String> nameList = new ArrayList<String>();
        List<Integer> scoreList = new ArrayList<Integer>();
        for (int i = 0; i < playerNum; i++) {
            if (i==0) {
                idList.add(userInfo.getLong("id"));
                iconList.add(userInfo.getString("headimg"));
                nameList.add(userInfo.getString("name"));
                scoreList.add(userInfo.getInt("score"));
            }else {
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
        if (gameRoom.getRoomType()==NNConstant.ROOM_TYPE_JB||gameRoom.getRoomType()==NNConstant.ROOM_TYPE_JB) {
            gameRoom.setGameCount(9999);
        }
        if(baseInfo.containsKey("di")){
            gameRoom.setScore(baseInfo.getDouble("di"));
        }else{
            gameRoom.setScore(1);
        }
        // 元宝模式
        if(baseInfo.containsKey("yuanbao")&&baseInfo.getDouble("yuanbao")>0){
            //底分
            gameRoom.setScore(baseInfo.getDouble("yuanbao"));
        }
        // 元宝模式
        if(baseInfo.containsKey("enterYB")&&baseInfo.getDouble("enterYB")>0){
            gameRoom.setEnterScore(baseInfo.getDouble("enterYB"));
        }
        // 元宝模式
        if(baseInfo.containsKey("leaveYB")&&baseInfo.getDouble("leaveYB")>0){
            gameRoom.setLeaveScore(baseInfo.getDouble("leaveYB"));
        }
        //设置金币场准入金币
        if(baseInfo.containsKey("goldcoins")){
            gameRoom.setEnterScore(baseInfo.getInt("goldcoins"));
        }
        if (baseInfo.getInt("open")==1) {
            gameRoom.setOpen(true);
        }else {
            gameRoom.setOpen(false);
        }
        // 玩家人数
        gameRoom.setPlayerCount(playerNum);
        // 金币、元宝扣服务费
        if((baseInfo.containsKey("fee")&&baseInfo.getInt("fee")==1)||baseInfo.getInt("roomType")==3){
            // TODO: 2018/4/20 需要缓存
            JSONObject gameSetting = roomBiz.getGameSetting();
            JSONObject roomFee = gameSetting.getJSONObject("pumpData");
            double fee;
            // 服务费：费率x底注
            if(baseInfo.containsKey("custFee")){
                // 自定义费率
                fee = baseInfo.getDouble("custFee")*gameRoom.getScore();
            }else{
                // 统一费率
                fee = roomFee.getDouble("proportion")*gameRoom.getScore();
            }
            double maxFee = roomFee.getDouble("max");
            double minFee = roomFee.getDouble("min");
            if(fee>maxFee){
                fee = maxFee;
            }else if(fee<minFee){
                fee = minFee;
            }
            fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            gameRoom.setFee(fee);
        }
        // 获取用户信息
        JSONObject obtainPlayerInfoData = new JSONObject();
        obtainPlayerInfoData.put("userInfo",userInfo);
        obtainPlayerInfoData.put("myIndex",0);
        obtainPlayerInfoData.put("uuid", client.getSessionId().toString());
        obtainPlayerInfoData.put("room_type",gameRoom.getRoomType());
        if (postData.containsKey("location")) {
            obtainPlayerInfoData.put("location",postData.getString("location"));
        }
        Playerinfo playerinfo = obtainPlayerInfo(obtainPlayerInfoData);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        RoomManage.gameRoomMap.put(roomNo,gameRoom);
        // 通知玩家
        switch (gameRoom.getGid()){
            case NNConstant.GAME_ID_NN:
                nnGameEventDealNew.createRoom(client);
                break;
            default:
                break;
        }
        JSONObject obj = new JSONObject();
        obj.put("game_id",gameRoom.getGid());
        obj.put("room_no",gameRoom.getRoomNo());
        obj.put("roomtype",gameRoom.getRoomType());
        obj.put("base_info",gameRoom.getRoomInfo());
        obj.put("createtime",new SimpleDateFormat("yyyy:MM:dd hh:mm:ss").format(new Date()));
        obj.put("game_count",gameRoom.getGameCount());
        obj.put("user_id0",playerinfo.getId());
        obj.put("user_icon0",playerinfo.getHeadimg());
        obj.put("user_name0",playerinfo.getName());
        obj.put("ip",gameRoom.getIp());
        obj.put("port",gameRoom.getPort());
        obj.put("status",0);
        if (gameRoom.isOpen()) {
            obj.put("open",0);
        }else {
            obj.put("open",0);
        }
        roomBiz.insertGameRoom(obj);
    }

    /**
     * 获取玩家信息
     * @param data
     * @return
     */
    public Playerinfo obtainPlayerInfo(JSONObject data){
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
        if(roomType == NNConstant.ROOM_TYPE_JB){
            // 金币模式
            playerinfo.setScore(userInfo.getDouble("coins"));
        }else if(roomType == NNConstant.ROOM_TYPE_YB){
            // 元宝模式
            playerinfo.setScore(userInfo.getDouble("yuanbao"));
        }else{
            // 房卡模式
            playerinfo.setScore(0);
        }
        playerinfo.setHeadimg(userInfo.getString("headimg"));
        playerinfo.setSex(userInfo.getString("sex"));
        playerinfo.setIp(userInfo.getString("ip"));
        if(userInfo.containsKey("sign")){
            playerinfo.setSignature(userInfo.getString("sign"));
        }else{
            playerinfo.setSignature("");
        }
        if (userInfo.containsKey("ghName")) {
            playerinfo.setGhName(userInfo.getString("ghName"));
        }
        if(userInfo.containsKey("area")){
            playerinfo.setArea(userInfo.getString("area"));
        }else{
            playerinfo.setArea("");
        }
        int vip = userInfo.getInt("lv");
        if(vip>1){
            playerinfo.setVip(vip-1);
        }else{
            playerinfo.setVip(0);
        }
        playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
        // 保存用户坐标
        if(data.containsKey("location")){
            playerinfo.setLocation(data.getString("location"));
        }
        // 设置幸运值
        if (userInfo.containsKey("luck")) {
            playerinfo.setLuck(userInfo.getInt("luck"));
        }
        return  playerinfo;
    }

    /**
     * 设置牛牛房间特殊参数
     * @param room
     * @param baseInfo
     * @param account
     */
    public void createRoomNN(NNGameRoomNew room,JSONObject baseInfo,String account){
        room.setRoomType(baseInfo.getInt("roomType"));
        room.setBankerType(baseInfo.getInt("type"));
        // 玩法
        String wanFa = "";
        switch (baseInfo.getInt("type")){
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
        if(baseInfo.containsKey("niuniuNum")){
            JSONArray nnNums = baseInfo.getJSONArray("niuniuNum");
            for (int i = 0; i <= 10; i++) {
                int value = nnNums.getInt(0);
                if(i==7){
                    value = nnNums.getInt(1);
                }else if(i==8){
                    value = nnNums.getInt(2);
                }else if(i==9){
                    value = nnNums.getInt(3);
                }else if(i==10){
                    value = nnNums.getInt(4);
                }
                // 设置倍率
                room.ratio.put(i, value);
            }
        }
        if(baseInfo.containsKey("special")){
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
        if(baseInfo.containsKey("qzTimes")){
            room.qzTimes = baseInfo.getJSONArray("qzTimes");
        }
        // 抢庄是否是随机庄（随机、最高倍数为庄）
        if(baseInfo.containsKey("qzsjzhuang")){
            room.setSjBanker(baseInfo.getInt("qzsjzhuang"));
        }else {
            room.setSjBanker(0);
        }
        // 没人抢庄
        if (baseInfo.containsKey("qznozhuang")&&baseInfo.getInt("qznozhuang")==NNConstant.NN_QZ_NO_BANKER_CK) {
            // 无人抢庄，重新发牌
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_CK);
        }else if(baseInfo.getInt("roomType")==3){
            // 无人抢庄，房间自动解散
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_JS);
        }else{
            // 无人抢庄，随机庄
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_SJ);
        }
        // 是否允许玩家中途加入
        if(baseInfo.containsKey("halfwayin")&&baseInfo.getInt("halfwayin")==1){
            room.setHalfwayIn(true);
        }
        //准备超时（0：不处理 1：自动准备 2：踢出房间）
        if(baseInfo.containsKey("readyovertime")){
            if(baseInfo.getInt("readyovertime")==NNConstant.NN_READY_OVERTIME_NOTHING){
                room.setReadyOvertime(NNConstant.NN_READY_OVERTIME_NOTHING);
            }else if(baseInfo.getInt("readyovertime")==NNConstant.NN_READY_OVERTIME_AUTO){
                room.setReadyOvertime(NNConstant.NN_READY_OVERTIME_AUTO);
            }else if(baseInfo.getInt("readyovertime")==NNConstant.NN_READY_OVERTIME_OUT){
                room.setReadyOvertime(NNConstant.NN_READY_OVERTIME_OUT);
            }
        }else if(baseInfo.getInt("roomType")==NNConstant.ROOM_TYPE_YB){
            room.setReadyOvertime(NNConstant.NN_READY_OVERTIME_OUT);
        }else{
            room.setReadyOvertime(NNConstant.NN_READY_OVERTIME_NOTHING);
        }
        if(baseInfo.containsKey("baseNum")){
            // 设置基础倍率
            room.setBaseNum(baseInfo.getJSONArray("baseNum").toString());
        }
        room.getUserPacketMap().put(account,new UserPacket());
    }

    /**
     * 获取房间设置
     * @param client
     * @param data
     */
    public void getGameSetting(SocketIOClient client, Object data){
        JSONObject fromObject = JSONObject.fromObject(data);
        int gid = fromObject.getInt("gid");
        String platform = fromObject.getString("platform");
        // TODO: 2018/4/19  查询房间设置,需要缓存
        JSONArray gameSetting = publicBiz.getRoomSetting(gid,platform);
        if (!Dto.isNull(gameSetting)) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < gameSetting.size(); i++) {
                array.add(gameSetting.getJSONObject(i));
            }
            JSONObject result = new JSONObject();
            result.put("data", array);
            result.put("code", 1);
            NNConstant.sendMsgEventToSingle(client,result.toString(),"getGameSettingPush");
        }
    }

    /**
     * 检查用户是否在房间内
     * @param client
     * @param data
     */
    public void checkUser(SocketIOClient client, Object data){
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("account")) {
            String account = postData.getString("account");
            // 遍历房间列表
            for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                if (gameRoom.getPlayerMap().containsKey(account)&&gameRoom.getPlayerMap().get(account)!=null) {
                    postData.put("roomNo",gameRoom.getRoomNo());
                    postData.put("myIndex",gameRoom.getPlayerMap().get(account).getMyIndex());
                    joinRoomBase(client,postData);
                    return;
                }
            }
            JSONObject result = new JSONObject();
            result.put(NNConstant.RESULT_KEY_CODE,1);
            NNConstant.sendMsgEventToSingle(client,result.toString(),"checkUserPush");
        }
    }
}
