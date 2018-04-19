package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.constant.Constant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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
    private MaJiangBiz maJiangBiz;

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
        JSONObject userInfo = maJiangBiz.getUserInfoByAccount(account);
        if (Dto.isObjNull(userInfo)){// 用户不存在
            result.put(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.put(NNConstant.RESULT_KEY_MSG,"用户不存在");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        } else if (baseInfo.getInt("roomType")==NNConstant.ROOM_TYPE_YB&&userInfo.containsKey("yuanbao")
            &&userInfo.getDouble("yuanbao")<baseInfo.getDouble("enterYB")) {// 元宝不足
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
        JSONObject userInfo = maJiangBiz.getUserInfoByAccount(account);
        if (Dto.isObjNull(userInfo)){// 用户不存在
            result.element(NNConstant.RESULT_KEY_CODE,NNConstant.GLOBAL_NO);
            result.element(NNConstant.RESULT_KEY_MSG,"用户不存在");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        } else if (RoomManage.gameRoomMap.get(roomNo).getRoomType()==NNConstant.ROOM_TYPE_YB&&userInfo.containsKey("yuanbao")
            &&userInfo.getDouble("yuanbao")<RoomManage.gameRoomMap.get(roomNo).getEnterScore()) {// 元宝不足
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
        if (myIndex<0) {
            result.put("code", 0);
            result.put("msg", "房间已满");
            NNConstant.sendMsgEventToSingle(client,result.toString(),"enterRoomPush_NN");
            return;
        }
        // 设置房间属性
        gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
        gameRoom.getUserIconList().set(myIndex, userInfo.getString("headimg"));
        gameRoom.getUserNameList().set(myIndex, userInfo.getString("name"));
        gameRoom.getUserScoreList().set(myIndex, userInfo.getInt("score"));
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
            joinData.put("isReconnect",0);
        }
        joinData.put("account",userInfo.getString("account"));
        joinData.put("room_no",roomNo);
        gameRoom.getPlayerMap().put(playerinfo.getAccount(), playerinfo);
        // 通知玩家
        switch (gameRoom.getGid()){
            case NNConstant.GID_NN:
                ((NNGameRoomNew)gameRoom).getUserPacketMap().put(userInfo.getString("account"),new UserPacket());
                nnGameEventDealNew.joinRoom(client,joinData);
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
            case NNConstant.GID_NN:
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
        if(baseInfo.containsKey("yuanbao")&&baseInfo.getDouble("yuanbao")>0){ // 元宝模式
            gameRoom.setScore(baseInfo.getDouble("yuanbao")); //底分
        }
        if(baseInfo.containsKey("enterYB")&&baseInfo.getDouble("enterYB")>0){ // 元宝模式
            gameRoom.setEnterScore(baseInfo.getDouble("enterYB"));
        }
        if(baseInfo.containsKey("leaveYB")&&baseInfo.getDouble("leaveYB")>0){ // 元宝模式
            gameRoom.setLeaveScore(baseInfo.getDouble("leaveYB"));
        }
        if(baseInfo.containsKey("goldcoins")){
            gameRoom.setEnterScore(baseInfo.getInt("goldcoins"));//设置金币场准入金币
        }
        if (baseInfo.getInt("open")==1) {
            gameRoom.setOpen(true);
        }else {
            gameRoom.setOpen(false);
        }
        gameRoom.setPlayerCount(playerNum);//玩家人数
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
            case NNConstant.GID_NN:
                nnGameEventDealNew.createRoom(client);
                break;
        }
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
        if(roomType == NNConstant.ROOM_TYPE_JB){ // 金币模式
            playerinfo.setScore(userInfo.getDouble("coins"));
        }else if(roomType == NNConstant.ROOM_TYPE_YB){ // 元宝模式
            playerinfo.setScore(userInfo.getDouble("yuanbao"));
        }else{ // 房卡模式
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
        }
        room.setWfType(wanFa);
        room.setBanker(account);// 庄家
        room.setOwner(account);// 房主
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
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_CK);// 无人抢庄，重新发牌
        }else if(baseInfo.getInt("roomType")==3){
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_JS);// 无人抢庄，房间自动解散
        }else{
            room.setQzNoBanker(NNConstant.NN_QZ_NO_BANKER_SJ);// 无人抢庄，随机庄
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
            room.setBaseNum(baseInfo.getJSONArray("baseNum").toString());//设置基础倍率
        }
        room.getUserPacketMap().put(account,new UserPacket());
    }
}
