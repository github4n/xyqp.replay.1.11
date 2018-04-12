package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.biz.model.zjh.ZJHGame;
import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.zjh.ZhaJinHuaService;
import com.zhuoan.biz.service.zjh.impl.ZhaJinHuaServiceImpl;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.Messages;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.times.TimerMsgData;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;


@Service
public class ZJHGameEventDeal {


    public static int GENDAODI = 1; // 跟到底
    public static int QIPAI = 2; // 弃牌
    public static int BIPAI = 3; // 比牌
    public static int KANPAI = 4; // 看牌
    public static int GENZHU = 5; // 跟注
    public static int JIAZHU = 6; // 加注
    public static int JIESUAN = 7; // 结算


    ZhaJinHuaService zjhService = new ZhaJinHuaServiceImpl();
    MaJiangBiz mjBiz=new MajiangBizImpl();

    /**
     * 创建、加入房间
     * @param client
     * @param data
     */
    public void enterRoom(SocketIOClient client, Object data) {


        JSONObject postdata = JSONObject.fromObject(data);

        // 房间号
        String roomNo = postdata.getString("room_no");
        // 用户账号
        String account = postdata.getString("account");

        // 设置客户端标识
        client.set(Constant.CLIENTTAG, account);
        String clientTag = account;

        JSONObject result=new JSONObject();

        JSONObject userinfo = new JSONObject();

        if(!client.has("userinfo")){

            // 返回的json
            //根据uuid获取用户信息
			/*if (UserInfoCache.userInfoMap.containsKey(account)) {
				userinfo = UserInfoCache.userInfoMap.get(account);
			} else {
				userinfo=mjBiz.getUserInfoByAccount(account);
				// 存入缓存
				UserInfoCache.userInfoMap.put(account, userinfo);
			}*/
            userinfo=mjBiz.getUserInfoByAccount(account);
            if (UserInfoCache.userInfoMap.containsKey(account)) {
                userinfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
            }
            if (userinfo.containsKey("gulidId")&&userinfo.getLong("gulidId")!=0) {
                JSONObject ghName = mjBiz.getGHName(userinfo.getLong("gulidId"));
                if (!Dto.isObjNull(ghName)) {
                    userinfo.element("ghName", ghName.getString("name"));
                }
            }
            UserInfoCache.userInfoMap.put(account, userinfo);
            //验证
            //uuid不合法 返回提示信息
            if(Dto.isObjNull(userinfo)){
                result.put("code", 0);
                result.put("msg", "用户不存在");
                client.sendEvent("enterRoomPush_ZJH", result);
                return;
            }else{
                client.set("userinfo", userinfo);
            }
//			String uuid=postdata.getString("uuid");
//			if(!userinfo.getString("uuid").equals(uuid)) {
//				result.put("code", 0);
//				result.put("msg", "该帐号已在其他地方登录");
//				client.sendEvent("enterRoomPush_ZJH", result);
//				return;
//			}
        }else{

            userinfo = client.get("userinfo");
        }

        result.put("code", 1);
        result.put("msg", "");

        // 获取房间信息
        //JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
        try {
            RoomManage.lock(roomNo);

            int myIndex = -1;
            GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
            if(gameRoom!=null){

                // 房间类型（0：房卡  1：金币）
                int roomType = 0;
                //if(room.containsKey("roomtype")&&room.get("roomtype")!=null){
                roomType = gameRoom.getRoomType();
                //}

                if(gameRoom.getGameCount()>gameRoom.getGameIndex() || roomType == 1 || roomType == 3){ //房间局数还未用完

                    long userId = userinfo.getLong("id");
                    for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
                        if (userId==gameRoom.getUserIdList().get(i)) {
                            myIndex = i;
                            break;
                        }
                    }
				/*if(userId==room.getLong("user_id0")){
					myIndex = 0;
				}
				if(userId==room.getLong("user_id1")){
					myIndex = 1;
				}
				if(userId==room.getLong("user_id2")){
					myIndex = 2;
				}
				if(userId==room.getLong("user_id3")){
					myIndex = 3;
				}
				if(userId==room.getLong("user_id4")){
					myIndex = 4;
				}
				if(userId==room.getLong("user_id5")){
					myIndex = 5;
				}
				if(userId==room.getLong("user_id6")){
					myIndex = 6;
				}
				if(userId==room.getLong("user_id7")){
					myIndex = 7;
				}
				if(userId==room.getLong("user_id8")){
					myIndex = 8;
				}
				if(userId==room.getLong("user_id9")){
					myIndex = 9;
				}*/
                    if (myIndex<0) {
                        result.put("code", 0);
                        result.put("msg", "加入房间失败");
                        client.sendEvent("enterRoomPush_ZJH", result);
                        RoomManage.unLock(roomNo);
                        return;
                    }

                    JSONObject obj=new JSONObject();
                    obj.put("room_no", roomNo);
                    obj.put("roomType", gameRoom.getRoomType());
                    obj.put("xzTimer", MutliThreadZJH.xzTimer);

                    if(gameRoom.getRoomType()==0 || gameRoom.getRoomType()==2){ // 房卡模式

                        obj.put("game_count", gameRoom.getGameCount());
                        obj.put("game_index", gameRoom.getGameIndex()+1);
                        // 房间属性信息
                        String base_info = gameRoom.getRoomInfo().toString();
                        JSONObject objInfo = JSONObject.fromObject(base_info);
                        if(objInfo.containsKey("player")&&objInfo.containsKey("turn")){

                            // 房间信息
                            String roominfo = objInfo.get("player")+"人/"
                                + objInfo.getJSONObject("turn").getInt("turn") + "局";
                            obj.put("roominfo", roominfo);
                        }
                        // 规则
                        JSONArray guizes = getGameGuiZe(objInfo);
                        obj.put("guize", guizes);
                    }

                    if(gameRoom.getRoomType()==3){ // 元宝模式

                        // 房间属性信息
                        String base_info = gameRoom.getRoomInfo().toString();
                        JSONObject objInfo = JSONObject.fromObject(base_info);
                        if(objInfo.containsKey("yuanbao")){

                            // 房间信息
                            StringBuffer roominfo = new StringBuffer();
                            roominfo.append("底注:");
                            roominfo.append(objInfo.get("yuanbao"));
                            roominfo.append("进:");
                            roominfo.append(objInfo.get("enterYB"));
                            roominfo.append(" 出:");
                            roominfo.append(objInfo.get("leaveYB"));
                            StringBuffer diInfo = new StringBuffer();
                            diInfo.append("底注:");
                            diInfo.append(objInfo.get("yuanbao"));
                            StringBuffer roominfo3 = new StringBuffer();
                            roominfo3.append("进:");
                            roominfo3.append(objInfo.get("enterYB"));
                            roominfo3.append(" 出:");
                            roominfo3.append(objInfo.get("leaveYB"));
                            obj.put("diInfo", diInfo.toString());
                            obj.put("roominfo3", roominfo3.toString());
                            obj.put("roominfo", roominfo.toString());
                            int type = objInfo.getInt("type");
                            String wanfa = "";
                            if(type==0){
                                wanfa = "经典模式";
                            }else if(type==1){
                                wanfa = "必闷三圈";
                            }else if(type==2){
                                wanfa = "激情模式";
                            }
                            RoomManage.gameRoomMap.get(roomNo).setFytype(wanfa);
                            obj.put("roominfo2", wanfa);
                        }
                        // 判断玩家元宝是否足够
                        if(userinfo.getDouble("yuanbao")<objInfo.getDouble("enterYB")) {

                            // 删除房间内玩家
                            //mjBiz.delGameRoomUserByUid(room, userId);

                            result.put("code", 0);
                            result.put("msg", "您的元宝不足，请先充值");
                            client.sendEvent("enterRoomPush_ZJH", result);
                            RoomManage.unLock(roomNo);
                            return;
                        }
                    }

                    if(gameRoom.getFirstTime()==0){

                        Playerinfo player = new Playerinfo();

                        player.setId(userinfo.getLong("id"));
                        player.setAccount(account);
                        player.setName(userinfo.getString("name"));
                        player.setUuid(client.getSessionId());
                        player.setMyIndex(myIndex);
                        if(roomType == 1){ // 金币模式
                            //player.setScore(userinfo.getInt("coins"));
                            /**
                             * 金皇冠筹码小数     wqm  2018/02/27
                             */
                            player.setScore(userinfo.getDouble("coins"));
                        }else if(roomType == 3){ // 元宝模式
                            player.setScore(userinfo.getDouble("yuanbao"));
                        }else{ // 房卡模式
                            player.setScore(0);
                        }
                        player.setHeadimg(userinfo.getString("headimg"));
                        player.setSex(userinfo.getString("sex"));
                        player.setIp(userinfo.getString("ip"));
                        if(userinfo.containsKey("sign")){
                            player.setSignature(userinfo.getString("sign"));
                        }else{
                            player.setSignature("");
                        }
                        if(userinfo.containsKey("area")){
                            player.setArea(userinfo.getString("area"));
                        }else{
                            player.setArea("");
                        }
                        // 工会名称
                        if (userinfo.containsKey("ghName")) {
                            player.setGhName(userinfo.getString("ghName"));
                        }
                        int vip = userinfo.getInt("lv");
                        if(vip>1){
                            player.setVip(vip-1);
                        }else{
                            player.setVip(0);
                        }
                        player.setStatus(Constant.ONLINE_STATUS_YES);
                        // 保存用户坐标
                        if(postdata.containsKey("location")){
                            player.setLocation(postdata.getString("location"));
                        }
                        // 设置幸运值
                        if(userinfo.containsKey("luck")){
                            player.setLuck(userinfo.getInt("luck"));
                        }
                        // 工会名称
                        if (userinfo.containsKey("ghName")) {
                            player.setGhName(userinfo.getString("ghName"));
                        }
                        JSONObject room1 = new JSONObject();
                        room1.element("base_info", gameRoom.getRoomInfo());
                        room1.element("room_no", gameRoom.getRoomNo());
                        room1.element("roomtype", gameRoom.getRoomType());
                        room1.element("game_count", gameRoom.getGameCount());
                        room1.element("game_id", gameRoom.getGid());
                        //创建房间
                        ZJHGame zjhGame = zjhService.createGameRoom(room1, clientTag, player);

                        client.set(Constant.ROOM_KEY_ZJH, roomNo);

                        obj.put("users",zjhGame.getAllPlayer());//告诉他原先加入的玩家
                        obj.put("myIndex",player.getMyIndex());
                        obj.put("isGameIng",zjhGame.getGameIngIndex());
                        obj.put("isReady", zjhGame.getPlayerIsReady());
                        obj.put("baseNum", zjhGame.getBaseNum());
                        obj.put("totalGameNum", zjhGame.getTotalGameNum());
                        obj.put("gametype", zjhGame.getGameType());
                        obj.put("wanfa", zjhGame.getWanfa());
                        obj.put("dizhu", zjhGame.getScore());

                        LogUtil.print("创建房间："+obj);
                        result.put("data", obj);
                        client.sendEvent("enterRoomPush_ZJH", result);

                    }else{//加入房间

                        // 是否进入下一局
                        boolean isNext = false;

                        if(postdata.containsKey("isNext")){
                            isNext = true;
                        }

                        Playerinfo player = new Playerinfo();

                        player.setId(userinfo.getLong("id"));
                        player.setAccount(account);
                        player.setName(userinfo.getString("name"));
                        player.setUuid(client.getSessionId());
                        player.setMyIndex(myIndex);
                        if(roomType == 1){ // 金币模式
                            //player.setScore(userinfo.getInt("coins"));
                            /**
                             * 金皇冠筹码小数     wqm  2018/02/27
                             */
                            player.setScore(userinfo.getDouble("coins"));
                        }else if(roomType == 3){ // 元宝模式
                            player.setScore(userinfo.getDouble("yuanbao"));
                        }else{ // 房卡模式
                            player.setScore(0);
                        }
                        player.setHeadimg(userinfo.getString("headimg"));
                        player.setSex(userinfo.getString("sex"));
                        player.setIp(userinfo.getString("ip"));
                        if(userinfo.containsKey("sign")){
                            player.setSignature(userinfo.getString("sign"));
                        }else{
                            player.setSignature("");
                        }
                        // 工会名称
                        if (userinfo.containsKey("ghName")) {
                            player.setGhName(userinfo.getString("ghName"));
                        }
                        if(userinfo.containsKey("area")){
                            player.setArea(userinfo.getString("area"));
                        }else{
                            player.setArea("");
                        }
                        int vip = userinfo.getInt("lv");
                        if(vip>1){
                            player.setVip(vip-1);
                        }else{
                            player.setVip(0);
                        }
                        player.setStatus(Constant.ONLINE_STATUS_YES);
                        // 保存用户坐标
                        if(postdata.containsKey("location")){
                            player.setLocation(postdata.getString("location"));
                        }
                        // 设置幸运值
                        if(userinfo.containsKey("luck")){
                            player.setLuck(userinfo.getInt("luck"));
                        }

                        //加入房间
                        boolean joinResult = zjhService.joinGameRoom(roomNo, clientTag, player, isNext);
                        client.set(Constant.ROOM_KEY_ZJH, roomNo);

                        ZJHGame zjhGame = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

                        Set<String> uuids = zjhGame.getUserPacketMap().keySet();

                        obj.put("users",zjhGame.getAllPlayer());//告诉他原先加入的玩家
                        obj.put("myIndex",player.getMyIndex());
                        obj.put("isGameIng",zjhGame.getGameIngIndex());
                        obj.put("isReady", zjhGame.getPlayerIsReady());
                        obj.put("baseNum", zjhGame.getBaseNum());
                        obj.put("totalGameNum", zjhGame.getTotalGameNum());
                        obj.put("gametype", zjhGame.getGameType());
                        obj.put("wanfa", zjhGame.getWanfa());
                        obj.put("dizhu", zjhGame.getScore());

                        if(postdata.containsKey("isNext")){
                            obj.put("isNext", postdata.get("isNext"));
                        }

                        LogUtil.print("加入房间："+obj);
                        result.put("data", obj);
                        client.sendEvent("enterRoomPush_ZJH", result);

                        if(uuids.size()>1 && joinResult){// 退出重进时不通知其他玩家

                            JSONObject obj1=new JSONObject();
                            JSONObject playerObj=new JSONObject();
                            JSONObject result1=new JSONObject();
                            playerObj.put("account", player.getAccount());
                            playerObj.put("name", player.getName());
                            playerObj.put("headimg", player.getRealHeadimg());
                            playerObj.put("sex", player.getSex());
                            playerObj.put("ip", player.getIp());
                            playerObj.put("introduction",player.getSignature());
                            playerObj.put("vip", player.getVip());
                            playerObj.put("location", player.getLocation());
                            playerObj.put("area", player.getArea());
                            playerObj.put("score", player.getScore());
                            playerObj.put("index", player.getMyIndex());
                            playerObj.put("ghName", player.getGhName());
                            obj1.put("user", playerObj);
                            result1.put("code", 1);
                            result1.put("msg", "");
                            result1.put("data", obj1);

                            for(String other:uuids){
                                if(!other.equals(clientTag)){
                                    SocketIOClient clientother=GameMain.server.getClient(zjhGame.getUUIDByClientTag(other));
                                    if(clientother!=null){
                                        clientother.sendEvent("playerEnterPush_ZJH", result1);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }

    /**
     * 获取规则数组
     * @param objInfo
     * @return
     */
    private JSONArray getGameGuiZe(JSONObject objInfo) {

        JSONArray guiZes = new JSONArray();
        if(objInfo.containsKey("baseNum")){
            JSONObject obj = new JSONObject();
            obj.put("name", "筹码：");
            obj.put("value", objInfo.get("baseNum").toString());
            guiZes.add(obj);
        }
        if(objInfo.containsKey("di")){
            JSONObject obj = new JSONObject();
            obj.put("name", "底注：");
            obj.put("value", objInfo.getInt("di"));
            guiZes.add(obj);
        }
        if(objInfo.containsKey("maxcoins")){
            JSONObject obj = new JSONObject();
            obj.put("name", "上限：");
            obj.put("value", objInfo.getInt("maxcoins"));
            guiZes.add(obj);
        }
        if(objInfo.containsKey("turn")){
            JSONObject obj = new JSONObject();
            obj.put("name", "局数：");
            String txt = objInfo.getJSONObject("turn").getInt("turn") + "局";
            obj.put("value", txt);
            guiZes.add(obj);
        }
        if(objInfo.containsKey("player")){
            JSONObject obj = new JSONObject();
            obj.put("name", "人数：");
            obj.put("value", objInfo.getInt("player"));
            guiZes.add(obj);
        }
        if(objInfo.containsKey("paytype")){
            JSONObject obj = new JSONObject();
            obj.put("name", "房费：");
            String txt = "房主支付";
            if(objInfo.getInt("paytype")==1){
                txt = "AA支付";
            }
            obj.put("value", txt);
            guiZes.add(obj);
        }
        return guiZes;
    }

    /**
     * 准备
     * @param client
     * @param data
     */
    public void gameReady(SocketIOClient client, Object data) {

        JSONObject postdata = JSONObject.fromObject(data);

        // 房间号
        String roomNo = postdata.getString("room_no");

        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){
                String clientTag = Constant.getClientTag(client);
                ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));//获取房间

                //不够金币没法准备
                if(room.getRoomType()==3 && room.getPlayerMap().get(clientTag).getScore()<room.getLeaveScore()){
                    if(room.getPlayerMap().get(clientTag)!=null){
                        exitRoom(clientTag, room.getRoomNo(), room.getPlayerMap().get(clientTag).getId());
                    }
                    //两人以上准备开始游戏
                    if(room.getReadyCount()>1 && room.getUserPacketMap().size()>1 && room.isAllReady()){

                        startGame(room);
                    }
                }else {
                    // 准备
                    zjhService.isReady(roomNo, clientTag);

                    if(room.getUserPacketMap().containsKey(clientTag)){

                        JSONObject obj = new JSONObject();
                        obj.put("myIndex", room.getPlayerIndex(clientTag));
                        obj.put("isReady", room.getPlayerIsReady());

                        // 通知玩家
                        for (String uuid  : room.getUserPacketMap().keySet()) {
                            SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
                            if(clientother!=null){
                                clientother.sendEvent("playerReadyPush_ZJH", obj);
                            }
                        }
                    }

                    // 两人准备后开始准备倒计时
                    if(room.getReadyCount()==2&&room.getPlayerMap().keySet().size()>2
                        &&(room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_READY
                        || room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN)){

                        //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.READY).start();
                        JSONObject result1 = new JSONObject();
                        result1.put("type", 0);
                        result1.put("timer", 15);
                        for (String uuid  : room.getUserPacketMap().keySet()) {
                            SocketIOClient askclient=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
                            if(askclient!=null){
                                askclient.sendEvent("zjhTimerPush_ZJH", result1);
                            }
                        }

                        TimerMsgData tmd=new TimerMsgData();
                        tmd.nTimeLimit=15;
                        tmd.nType=9;
                        tmd.roomid=roomNo;
                        tmd.client=null;
                        tmd.data=new JSONObject().element("room_no", roomNo);
                        tmd.gid=6;
                        tmd.gmd= new Messages(null, new JSONObject().element("room_no", roomNo), 6, 9);
                        GameMain.singleTime.createTimer(tmd);
                    }

                    if(room.getReadyCount()>1 && room.getUserPacketMap().size()>1 && room.isAllReady()){

                        startGame(room);
                    }
                }

            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }

    }


    /**
     * 开始游戏
     * @param room
     */
    public void startGame(ZJHGame room) {

        boolean startGame = false;

        if(room.getRoomType()==1 || room.getRoomType()==3){

            // 下一局开始时，清除掉线玩家
            zjhService.cleanDisconnectPlayer(room);
        }

        if(((ZJHGame)RoomManage.gameRoomMap.get(room.getRoomNo())).getPlayerMap().size()>=2){
            startGame = true;
        }

        if(startGame){

            // 重置准备
            room.setReadyCount(0);

            // 初始化房间信息
            room.initGame();

            String roomNo = room.getRoomNo();

            Set<String> uuidList = room.getUserPacketMap().keySet();

            // 洗牌
            zjhService.xiPai(roomNo);

            // 发牌
            zjhService.faPai(roomNo);

            // 幸运大转盘
            luckyTurning(room);

            // 玩家下注（底注）
            for (String uuid : uuidList) {

                int myIndex = room.getPlayerIndex(uuid);
                double score = room.getCurrentScore();
                // 添加下注记录
                room.addXiazhuList(myIndex, score);
                // 更新总下注记录
                room.addTotalScore(score);
            }

            // 必闷三圈
//			if(room.getWanfa()==1){
//				for (int i = 0; i < 3; i++) {
//					for (String uuid : uuidList) {
//						zjhService.genzhu(uuid, roomNo);
//					}
//				}
//			}

            // 当局操作玩家
            room.setFocus(room.getNextPlayer(room.getZhuang()));

            JSONArray userIds = new JSONArray();
            // 通知玩家
            for (String uuid : uuidList) {

                Playerinfo player = room.getPlayerMap().get(uuid);
                SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));

                if(room.getRoomType()==3){ // 元宝模式

                    // 是否扣服务费
                    boolean isFee = true;

                    // 观战玩家不能扣服务费
                    if(room.getUserPacketMap().get(uuid)!=null){

                        if(room.getUserPacketMap().get(uuid).getStatus()!=ZhaJinHuaCore.USERPACKER_STATUS_ANPAI){

                            isFee = false;
                            System.out.println("观战玩家不能扣服务费");
                        }
                    }

                    if(isFee){

                        userIds.add(player.getId());

                        // 第一局开始需要扣费提示
//						if(player.isTipMsg()){
//							if(clientother!=null){
//								JSONObject obj = new JSONObject();
//								// type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
//								obj.put("type", 0);
//								obj.put("msg", "本局扣服务费："+room.getFee());
//								clientother.sendEvent("tipMsgPush_ZJH", obj);
//							}
//							player.setTipMsg(false);
//						}

                        // 玩家扣服务费
                        if(room.getFee()>0){
                            player.setScore(player.getScore() - room.getFee());
                        }
                    }
                }

                int myIndex = room.getPlayerIndex(uuid);
                double score = room.getCurrentScore();

                JSONObject result = new JSONObject();
                result.put("baseNum", room.getBaseNum());
                result.put("isGameIng",room.getGameIngIndex());
                result.put("myIndex", myIndex);
                result.put("zhuang", room.getPlayerIndex(room.getZhuang()));
                result.put("nextNum", room.getPlayerIndex(room.getFocus()));
                result.put("gameNum", room.getGameNum());
                result.put("game_index", room.getCurrentGameIndex());
                result.put("totalScore", room.getTotalScore());
                result.put("di", score);
                result.put("currentScore", score);

                if(clientother!=null){
                    clientother.sendEvent("gameStartPush_ZJH", result);
                }
            }
            // 玩家扣服务费
            if(room.getFee()>0){

                if(room.getRoomType()==1){ // 金币模式
                    //mjBiz.dealGoldRoomFee(userIds, roomNo, 6, room.getFee(), "2");
                    mjBiz.pump(userIds, roomNo, 1, room.getFee(), "coins");
                }else if(room.getRoomType()==3){ // 元宝模式
                    //mjBiz.dealGoldRoomFee(userIds, roomNo, 6, room.getFee(), "3");
                    for (String uuid : room.getPlayerMap().keySet()) {
                        UserInfoCache.updateUserScore(uuid, -room.getFee(), 3);
                    }
                    RoomManage.unLock(roomNo);
                    mjBiz.pump(userIds, roomNo, 1, room.getFee(), "yuanbao");
                    RoomManage.lock(roomNo);
                }

                // 刷新玩家余额
                for (String uuid : room.getPlayerMap().keySet()) {

                    JSONObject result = new JSONObject();
                    result.put("users",room.getAllPlayer());

                    SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
                    if(clientother!=null){
                        clientother.sendEvent("userInfoPush_ZJH", result);
                    }
                }
            }

            // 下注倒计时
            //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
            TimerMsgData tmd=new TimerMsgData();
            tmd.nTimeLimit=MutliThreadZJH.xzTimer;
            tmd.nType=10;
            tmd.roomid=roomNo;
            tmd.client=null;
            tmd.data=new JSONObject().element("room_no", roomNo).element("focus", room.getFocus());
            tmd.gid=6;
            tmd.gmd= new Messages(null, new JSONObject().element("room_no", roomNo).element("focus", room.getFocus()), 6, 10);
            GameMain.singleTime.createTimer(tmd);

            // 游戏状态
            ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(ZhaJinHuaCore.GAMESTATUS_XIAZHU);
        }

    }


    private void luckyTurning(ZJHGame room) {
        int luckyNum = RandomUtils.nextInt(100);
        for (String uuid  : room.getUserPacketMap().keySet()) {
            UserPacket up = room.getUserPacketMap().get(uuid);
            if(up.getLuck()!=-1 && up.getLuck()>=luckyNum){

                Integer[] maxPai = up.getPai();
                String maxUUID = uuid;
                for (String uid  : room.getUserPacketMap().keySet()) {
                    if(!uuid.equals(uid)){
                        Integer[] curPai = room.getUserPacketMap().get(uid).getPai();
                        if(ZhaJinHuaCore.compare(Arrays.asList(maxPai), Arrays.asList(curPai)) < 0){
                            maxPai = curPai;
                            maxUUID = uid;
                        }
                    }
                }
                if(!maxUUID.equals(uuid)){
                    Integer[] curPai = room.getUserPacketMap().get(uuid).getPai();
                    int curType = room.getUserPacketMap().get(uuid).getType();
                    int maxType = room.getUserPacketMap().get(maxUUID).getType();
                    room.getUserPacketMap().get(uuid).setPai(maxPai);
                    room.getUserPacketMap().get(uuid).setType(maxType);
                    room.getUserPacketMap().get(maxUUID).setPai(curPai);
                    room.getUserPacketMap().get(maxUUID).setType(curType);
                }
            }
        }
    }

    /**
     * 游戏事件
     * @param client
     * @param data
     */
    public void gameEvent(SocketIOClient client, Object data) {

        JSONObject obj=JSONObject.fromObject(data);

        String roomNo = "";
        String uuid = "";
        if(client!=null){
            roomNo = client.get(Constant.ROOM_KEY_ZJH);
            uuid = Constant.getClientTag(client);
        }else{
            roomNo = obj.getString("room_no");
            uuid = obj.getString("uuid");
        }
        try {
            RoomManage.lock(roomNo);

            if(RoomManage.gameRoomMap.containsKey(roomNo)){

                int type = obj.getInt("type");

                ZJHGame room = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

                UserPacket userPacket = room.getUserPacketMap().get(uuid);

                // 还没开牌时才能操作
                if(userPacket!=null&&(userPacket.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
                    ||userPacket.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI)){

                    if(type==JIAZHU){// 加注

                        boolean xiazhu = zjhService.xiazhu(uuid, roomNo, obj.getInt("score"), ZJHGameEventDeal.JIAZHU);
                        // 加注成功开启线程
                        if (xiazhu) {
                            // 下一个操作的玩家
                            String nextUUID = room.getNextOperationPlayer(room, uuid);
                            // 下注定时器
                            //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                            // 跟到底定时器
                            //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                            TimerMsgData tmd=new TimerMsgData();
                            tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                            tmd.nType=10;
                            tmd.roomid=roomNo;
                            tmd.client=client;
                            tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                            tmd.gid=6;
                            tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                            GameMain.singleTime.createTimer(tmd);
                        }

                    }else if(type==GENZHU){// 跟注

                        // 判断是否开启线程
                        UserPacket user = room.getUserPacketMap().get(uuid);
                        Playerinfo player = room.getPlayerMap().get(uuid);
                        double score = room.getCurrentScore();

                        if(type==ZJHGameEventDeal.JIAZHU){
                            room.setCurrentScore(score);
                        }

                        // 判断玩家是否需要加倍
                        if(user.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
                            score = score*2;
                        }

                        // 获取玩家下注总分数
                        double totalScore = room.getXiazhuScore(player.getMyIndex()) + score;

                        boolean canXiazhu = false;
                        if (room.getRoomType()==0||room.getRoomType()==2) {
                            canXiazhu = true;
                        }else if (player.getScore()>=totalScore) {
                            canXiazhu = true;
                        }

                        zjhService.genzhu(uuid, roomNo);

                        if (canXiazhu) {

                            // 下一个操作的玩家
                            String nextUUID = room.getNextOperationPlayer(room, uuid);
                            // 下注定时器
                            //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                            // 跟到底定时器
                            //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                            TimerMsgData tmd=new TimerMsgData();
                            tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                            tmd.nType=10;
                            tmd.roomid=roomNo;
                            tmd.client=client;
                            tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                            tmd.gid=6;
                            tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                            GameMain.singleTime.createTimer(tmd);
                        }

                    }else if(type==GENDAODI){// 跟到底

                        zjhService.gendaodi(uuid, roomNo, obj);

                        // 如果轮到自己操作点击跟到底则默认跟注
                        if (room.getFocus().equals(uuid)&&room.getUserPacketMap().get(uuid).isGenDaoDi) {
                            // 判断是否开启线程
                            UserPacket user = room.getUserPacketMap().get(uuid);
                            Playerinfo player = room.getPlayerMap().get(uuid);
                            double score = room.getCurrentScore();

                            if(type==ZJHGameEventDeal.JIAZHU){
                                room.setCurrentScore(score);
                            }

                            // 判断玩家是否需要加倍
                            if(user.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
                                score = score*2;
                            }

                            // 获取玩家下注总分数
                            double totalScore = room.getXiazhuScore(player.getMyIndex()) + score;

                            boolean canXiazhu = false;
                            if (room.getRoomType()==0||room.getRoomType()==2) {
                                canXiazhu = true;
                            }else if (player.getScore()>=totalScore) {
                                canXiazhu = true;
                            }

                            zjhService.genzhu(uuid, roomNo);

                            if (canXiazhu) {

                                // 下一个操作的玩家
                                String nextUUID = room.getNextOperationPlayer(room, uuid);
                                // 下注定时器
                                //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                                // 跟到底定时器
                                //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                                TimerMsgData tmd=new TimerMsgData();
                                tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                                tmd.nType=10;
                                tmd.roomid=roomNo;
                                tmd.client=client;
                                tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                                tmd.gid=6;
                                tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                                GameMain.singleTime.createTimer(tmd);
                            }
                        }

                    }else if(type==QIPAI){// 弃牌

                        zjhService.qipai(uuid, roomNo);

                        // 下一个操作的玩家
                        String nextUUID = room.getNextOperationPlayer(room, uuid);
                        // 下注定时器
                        //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                        // 跟到底定时器
                        //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                        TimerMsgData tmd=new TimerMsgData();
                        tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                        tmd.nType=10;
                        tmd.roomid=roomNo;
                        tmd.client=client;
                        tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                        tmd.gid=6;
                        tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                        GameMain.singleTime.createTimer(tmd);

                    }else if(type==KANPAI){// 看牌

                        zjhService.kanpai(uuid, roomNo);

                    }else if(type==BIPAI){// 比牌

                        // 判断是否开启线程
                        UserPacket user = room.getUserPacketMap().get(uuid);
                        Playerinfo player = room.getPlayerMap().get(uuid);
                        double score = room.getCurrentScore();

                        if(type==ZJHGameEventDeal.JIAZHU){
                            room.setCurrentScore(score);
                        }

                        // 判断玩家是否需要加倍
                        if(user.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
                            score = score*2;
                        }
                        // 获取玩家下注总分数
                        double totalScore = room.getXiazhuScore(player.getMyIndex()) + score;

                        boolean canXiazhu = false;
                        if (room.getRoomType()==0||room.getRoomType()==2) {
                            canXiazhu = true;
                        }else if (player.getScore()>=totalScore) {
                            canXiazhu = true;
                        }

                        if(canXiazhu){

                            zjhService.bipai(uuid, roomNo, obj);

                            // 下一个操作的玩家
                            String nextUUID = room.getNextOperationPlayer(room, uuid);

                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                            // 下注定时器
                            //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                            // 跟到底定时器
                            //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                            TimerMsgData tmd=new TimerMsgData();
                            tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                            tmd.nType=10;
                            tmd.roomid=roomNo;
                            tmd.client=client;
                            tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                            tmd.gid=6;
                            tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                            GameMain.singleTime.createTimer(tmd);
                        }else {
                            SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
                            if(clientother!=null){
                                JSONObject result = new JSONObject();
                                result.put("code", 0);
                                result.put("type", type);
                                result.put("index", room.getPlayerMap().get(uuid).getMyIndex());
                                result.put("msg", "您的金币不足，无法比牌");
                                clientother.sendEvent("gameActionPush_ZJH", result);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }


    /**
     * 解散房间
     * @param client
     * @param data
     */
    public void closeRoom(SocketIOClient client, Object data) {

        // 房间号
        String roomNo=client.get(Constant.ROOM_KEY_ZJH);
        String clientTag = Constant.getClientTag(client);
        if(RoomManage.gameRoomMap.containsKey(roomNo)&&((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(clientTag)!=null){

            JSONObject obj=JSONObject.fromObject(data);
            ZJHGame game = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
            boolean isClose = false;
            if(obj.getInt("type")==1){//同意解散房间

                game.getUserPacketMap().get(clientTag).isCloseRoom=1;

            }else if(obj.getInt("type")==0){//拒绝解散房间

                game.getUserPacketMap().get(clientTag).isCloseRoom=-1;

            }else if(obj.getInt("type")==-1){ //倒计时后强制解散房间

                isClose=true;
            }

            JSONArray array = new JSONArray();
            int agree = 0; // 同意人数
            int refuse = 0;//拒绝人数
            List<String> names = new ArrayList<String>();
            for (String uuid  : game.getUserPacketMap().keySet()) {

                JSONObject result = new JSONObject();
                result.put("name", game.getPlayerMap().get(uuid).getName());
                result.put("index", game.getPlayerMap().get(uuid).getMyIndex());
                if(game.getUserPacketMap().get(uuid).isCloseRoom==1){
                    result.put("result", 1);
                    agree++;
                }else if(game.getUserPacketMap().get(uuid).isCloseRoom==-1){
                    result.put("result", -1);
                    refuse++;
                    names.add(game.getPlayerMap().get(uuid).getName());
                }else{
                    result.put("result", 0);
                }
                array.add(result);
            }

            if(agree+refuse==game.getUserPacketMap().size()){

                JSONObject result = new JSONObject();
                if(game.getPlayerCount()!=game.getReadyCount()&&game.getUserPacketMap().get(game.getFangzhu()).isCloseRoom==1
                    &&game.getFangzhu().equals(clientTag)){ //游戏未开始阶段，房主可以直接解散房间

                    result.put("type", 1); //解散房间
                    result.put("result", 2);//房主解散
                    for (String uuid  : game.getUserPacketMap().keySet()) {

                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }

                    //更新房间信息
                    JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
                    if(room!=null&&room.containsKey("id")){
                        String sql = "update za_gamerooms set status=? where id=?";
                        DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
                        if(room.getInt("roomtype") == 2){
                            //代开房间房间解散，新建房间
                            GlobalService.insertGameRoom(roomNo);
                        }
                    }

                    // 清除房间缓存数据
                    RoomManage.gameRoomMap.remove(roomNo);

                }else if(refuse==0){ //所有人都同意退出房间
                    result.put("type", 1); //解散房间
                    result.put("result", 1);
                    for (String uuid  : game.getUserPacketMap().keySet()) {

                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }

//					更新房间信息
                    JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
                    if(room!=null&&room.containsKey("id")){
                        String sql = "update za_gamerooms set status=? where id=?";
                        DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
                        if(room.getInt("roomtype") == 2){
                            //代开房间房间解散，新建房间
                            GlobalService.insertGameRoom(roomNo);
                        }
                    }

                    // 清除房间缓存数据
                    RoomManage.gameRoomMap.remove(roomNo);

                }else{ // 有人拒绝退出
                    result.put("type", 1); //解散房间
                    result.put("result", 0);
                    result.put("user", names.toArray());
                    for (String uuid  : game.getUserPacketMap().keySet()) {
                        // 重置准备状态
                        game.getUserPacketMap().get(uuid).isCloseRoom=0;
                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }
                }

            }else{

                if(isClose&&refuse==0){

                    JSONObject result = new JSONObject();
                    result.put("type", 1); //解散房间
                    result.put("result", 1);
                    for (String uuid  : game.getUserPacketMap().keySet()) {

                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }

                    //更新房间信息
                    JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
                    if(room!=null&&room.containsKey("id")){
                        String sql = "update za_gamerooms set status=? where id=?";
                        DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
                        if(room.getInt("roomtype") == 2){
                            //代开房间房间解散，新建房间
                            GlobalService.insertGameRoom(roomNo);
                        }
                    }

                    // 清除房间缓存数据
                    RoomManage.gameRoomMap.remove(roomNo);

                }else if(isClose&&refuse>0){
                    JSONObject result = new JSONObject();
                    result.put("type", 1); //解散房间
                    result.put("result", 0);
                    result.put("user", names.toArray());
                    for (String uuid  : game.getUserPacketMap().keySet()) {
                        // 重置准备状态
                        game.getUserPacketMap().get(uuid).isCloseRoom=0;
                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }
                }else{ //通知其他人退出申请

                    for (String uuid : game.getUserPacketMap().keySet()) {

                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                        if(askclient!=null){
                            askclient.sendEvent("closeRoomPush_ZJH", array);
                        }
                    }
                }
            }
        }else{

            // 房间不存在直接退回大厅
            JSONObject result = new JSONObject();
            result.put("type", 1); //解散房间
            result.put("result", 1);
            SocketIOClient askclient=GameMain.server.getClient(client.getSessionId());
            if(askclient!=null){
                askclient.sendEvent("exitRoomPush_ZJH", result);
            }
        }

    }


    /**
     * 退出房间
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data) {

        // 房间号
        String roomNo=client.get(Constant.ROOM_KEY_ZJH);
        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){

                JSONObject userinfo = client.get("userinfo");
                if(!Dto.isObjNull(userinfo)){
                    exitRoom(Constant.getClientTag(client), roomNo, userinfo.getLong("id"));
                }

                // 其他玩家已经准备则开始游戏
                if(RoomManage.gameRoomMap.containsKey(roomNo) && ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).isAllReady()){
                    JSONObject obj = new JSONObject();
                    obj.put("room_no", roomNo);
                    startGame(((ZJHGame) RoomManage.gameRoomMap.get(roomNo)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }


    /**
     * 玩家退出玩家房间
     * @param uuid
     * @param roomNo
     * @param userId
     */
    public void exitRoom(String uuid, String roomNo, long userId) {

        if(RoomManage.gameRoomMap.containsKey(roomNo)){

            ZJHGame game = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

            boolean isRealExit = true;

            // 金币场庄家退出游戏
            if((game.getRoomType()==1||game.getRoomType()==3)&&game.getPlayerMap().size()>1){

                // 准备或者牌局结束阶段庄家退出
                if(game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_READY || game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN){

                    // 金币场庄家退出游戏
                    if(game.getZhuang().equals(uuid)){

                        String oldZhuang = game.getZhuang();
                        String zhuang = game.getNextPlayer(oldZhuang);
                        if(!oldZhuang.equals(zhuang)){

                            ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
                            ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setFangzhu(zhuang);
                            JSONObject result = new JSONObject();
                            result.put("zhuang", game.getPlayerIndex(zhuang));
                            for (String uid  : game.getUserPacketMap().keySet()) {
                                // 重置庄家信息
                                SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
                                if(clientother!=null){
                                    clientother.sendEvent("huanZhuangPush_ZJH", result);
                                }
                            }
                        }
                    }
                }else if((game.getUserPacketMap().get(uuid)!=null
                    &&game.getUserPacketMap().get(uuid).getStatus()==-1)
                    ||game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_QIPAI){ // 玩家处于观战中，随时可以退出

                    // 弃牌退出扣元宝
                    if (game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_QIPAI) {
                        Playerinfo playerinfo = game.getPlayerMap().get(uuid);
                        double xiazhuScore = game.getXiazhuScore(playerinfo.getMyIndex());
                        String sql = "update za_users set yuanbao=yuanbao-? where id=?";
                        UserInfoCache.updateUserScore(playerinfo.getAccount(), xiazhuScore, 3);
                        GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{xiazhuScore, playerinfo.getId()}, SqlModel.EXECUTEUPDATEBYSQL));
                        //DBUtil.executeUpdateBySQL(sql, new Object[]{xiazhuScore, playerinfo.getId()});
                    }
                    System.out.println("出门左转，请便！");

                }else{
                    isRealExit = false;

                    JSONObject result = new JSONObject();
                    result.put("type", 2); //退出房间
                    result.put("code", 0);
                    result.put("msg", "正在游戏中暂时无法退出游戏");
                    SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                    if(askclient!=null){
                        askclient.sendEvent("exitRoomPush_ZJH", result);
                    }
                }
            }

            if(isRealExit){

                // 玩家下标
                int index = game.getPlayerIndex(uuid);

                if(game.getPlayerMap().get(uuid)!=null){

                    JSONObject result = new JSONObject();
                    result.put("type", 2); //退出房间
                    result.put("index", index);
                    for (String uuid1  : game.getUserPacketMap().keySet()) {

                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
                        if(askclient!=null){
                            askclient.sendEvent("exitRoomPush_ZJH", result);
                        }
                    }
                }

                // 获取房间信息
				/*JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
				if(room!=null&&room.containsKey("id")){

					// 删除房间内玩家
					mjBiz.delGameRoomUserByUid(room, userId);
					((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserIDSet().remove(userId);
				}*/
                RoomManage roomManage = new RoomManage();
                roomManage.playerExit(uuid, userId, roomNo);
                // 清除玩家下注信息
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).clearXiaZhuList(index);
                // 清除房间用户数据
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().remove(uuid);
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().remove(uuid);

                // 房间准备人数少于2时取消定时器，不踢人
                int count = 0;
                for (String uid:((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().keySet()) {
                    int ready = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uid).getIsReady();
                    if(ready!=0){
                        count++;
                    }
                }
                if (count<2) {
                    JSONObject result = new JSONObject();
                    result.put("type", MutliThreadZJH.READY);
                    result.put("timer", 0);
                    for (String uuid1  : game.getUserPacketMap().keySet()) {
                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
                        if(askclient!=null){
                            askclient.sendEvent("zjhTimerPush_ZJH", result);
                        }
                    }
                }

                // 金币场没人的房间直接清除
                if((game.getRoomType()==1||game.getRoomType()==3)&&((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size()==0){
                    RoomManage.gameRoomMap.remove(roomNo);
                    //if(room!=null&&room.containsKey("id")){
                    String sql = "update za_gamerooms set status=? where room_no=?";
                    //DBUtil.executeUpdateBySQL(sql, new Object[]{-1, roomNo});
                    GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{-1, roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
                    LogUtil.print("金币场没人的房间直接清除："+roomNo);
                    //}
                }

                // 所有人都已准备则开始游戏
//				int count = game.getUserPacketMap().size();
//				if(count>1 && game.getReadyIndex().length==count){
//					if(game.getTimer()>=3){
//
//						// 触发开始游戏
//    					startGame(game);
//					}
//				}
            }

        }
    }


    public void reconnectGame(SocketIOClient client, Object data) {

        JSONObject obj=JSONObject.fromObject(data);
        // 房间号
        String roomNo = obj.getString("room_no");
        // 用户账号
        String account = obj.getString("account");
        String clientTag = Constant.getClientTag(client);
        if(clientTag.equals("")){
            clientTag = account;
            // 设置客户端标识
            client.set(Constant.CLIENTTAG, clientTag);
        }
        //根据uuid获取用户信息
        JSONObject userinfo=new JSONObject();
		/*if (UserInfoCache.userInfoMap.containsKey(account)) {
			userinfo = UserInfoCache.userInfoMap.get(account);
		} else {
			userinfo=mjBiz.getUserInfoByAccount(account);
			// 存入缓存
			UserInfoCache.userInfoMap.put(account, userinfo);
		}*/
        userinfo=mjBiz.getUserInfoByAccount(account);
        if (UserInfoCache.userInfoMap.containsKey(account)) {
            userinfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
            if (userinfo.containsKey("ghName")) {
                userinfo.element("ghName",UserInfoCache.userInfoMap.get(account).getString("ghName"));
            }
        }else if (userinfo.containsKey("gulidId")&&userinfo.getLong("gulidId")!=0) {
            JSONObject ghName = mjBiz.getGHName(userinfo.getLong("gulidId"));
            if (!Dto.isObjNull(ghName)) {
                userinfo.element("ghName", ghName.getString("name"));
            }
        }
        UserInfoCache.userInfoMap.put(account, userinfo);
        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){

                ZJHGame game = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
                Playerinfo player = null;
                for (String uuid  : game.getUserPacketMap().keySet()) {
                    if(game.getPlayerMap().get(uuid)!=null&&game.getPlayerMap().get(uuid).getAccount().equals(account)){
                        player = game.getPlayerMap().get(uuid);
                        player.setStatus(Constant.ONLINE_STATUS_YES);
                        player.setUuid(client.getSessionId());
                    }
                }

                if(player!=null){

                    // 设置会话信息
                    client.set(Constant.ROOM_KEY_ZJH, roomNo);
                    client.set("userinfo", userinfo);
                    LogUtil.print(account+":重连成功！");

                    // 返回给玩家当前牌局信息（基础信息）
                    if(game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_READY){ // 准备阶段

                        JSONObject result=new JSONObject();
                        result.put("type", 0);
                        result.put("game_index", game.getCurrentGameIndex());
                        result.put("baseNum", game.getBaseNum());
                        result.put("gametype", game.getGameType());
                        result.put("isGameIng",game.getGameIngIndex());
                        result.put("isXiaZhuIng",game.getProgressIndex());
                        result.put("room_no", roomNo);
                        result.put("users",game.getAllPlayer());//告诉他原先加入的玩家
                        result.put("myIndex",player.getMyIndex());
                        result.put("isReady",game.getPlayerIsReady());
                        result.put("timer",game.getTimeLeft());

                        LogUtil.print("断线重连返回(准备阶段)="+result.toString());
                        client.sendEvent("reconnectGamePush_ZJH", result);

                    }else if(game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_XIAZHU){ // 下注阶段

                        // 确定下次操作的玩家
                        int nextNum = game.getPlayerIndex(game.getFocus());
                        int gameNum = game.getGameNum();
                        if(game.getYixiazhu().contains(nextNum)){
                            gameNum = gameNum+1;
                        }

                        JSONArray array1 = new JSONArray();
                        for(String uuid : game.getPlayerMap().keySet()){
                            Playerinfo player1 = game.getPlayerMap().get(uuid);
                            if(player!=null){
                                JSONObject obj1 = new JSONObject();
                                obj1.put("account", player1.getAccount());
                                obj1.put("name", player1.getName());
                                obj1.put("headimg", player1.getRealHeadimg());
                                obj1.put("sex", player1.getSex());
                                obj1.put("ip", player1.getIp());
                                obj1.put("vip", player1.getVip());
                                obj1.put("location", player1.getLocation());
                                obj1.put("area", player1.getArea());
                                BigDecimal b1 = new BigDecimal(Double.toString(player1.getScore()));
                                BigDecimal b2 = new BigDecimal(Double.toString(game.getXiazhuScore(player1.getMyIndex())));
                                obj1.put("score", b1.subtract(b2).doubleValue());
                                obj1.put("index", player1.getMyIndex());
                                obj1.put("status", player1.getStatus());
                                obj1.put("introduction", player1.getSignature());
                                array1.add(obj1);
                            }
                        }

                        JSONObject result=new JSONObject();
                        result.put("type", 1);
                        result.put("game_index", game.getCurrentGameIndex());
                        result.put("di", game.getScore());
                        result.put("baseNum", game.getBaseNum());
                        result.put("gametype", game.getGameType());
                        result.put("isGameIng",game.getGameIngIndex());
                        result.put("isXiaZhuIng",game.getProgressIndex());
                        result.put("room_no", roomNo);
                        result.put("users",array1);//告诉他原先加入的玩家
                        result.put("index",player.getMyIndex());
                        result.put("status",game.getPlayerStatus());
                        result.put("gameNum", gameNum);
                        result.put("nextNum", nextNum);
                        result.put("currentScore", game.getCurrentScore());
                        result.put("totalScore", game.getTotalScore());
                        result.put("isGameover", 0);


                        if(game.getUserPacketMap().get(clientTag).isGenDaoDi){
                            result.put("isGendaodi", 1);
                        }else{
                            result.put("isGendaodi", 0);
                        }
                        result.put("xiazhuList", game.getXiazhuList());
                        JSONArray array = new JSONArray();
                        for (String uuid  : game.getUserPacketMap().keySet()) {

                            int index = game.getPlayerMap().get(uuid).getMyIndex();
                            double myScore = game.getXiazhuScore(index);
                            JSONObject score = new JSONObject();
                            score.put("index", index);
                            score.put("myScore", myScore);
                            array.add(score);
                        }
                        result.put("myScore", array);
                        result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
                        UserPacket userPacket = game.getUserPacketMap().get(clientTag);
                        if(userPacket.isShow){
                            result.put("isShow", 1);
                            result.put("mypai", userPacket.getPai());
                            result.put("paiType", userPacket.getType());
                        }else{
                            result.put("isShow", 0);
                            result.put("mypai", new int[]{0,0,0});
                            result.put("paiType", 0);
                        }
                        result.put("timer",game.getTimeLeft());

                        LogUtil.print("断线重连返回(下注阶段)="+result.toString());
                        client.sendEvent("reconnectGamePush_ZJH", result);

                    }else if(game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN){ // 结算阶段

                        JSONArray jiesuanArray = new JSONArray();
                        for (String uuid : game.getUserPacketMap().keySet()) {
                            JSONObject result = new JSONObject();
                            int index = game.getPlayerIndex(uuid);
                            result.put("index", index);
                            double myScore = - game.getXiazhuScore(index);
                            if(game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_VICTORY){
                                result.put("win", 1);
                                BigDecimal b1 = new BigDecimal(Double.toString(game.getTotalScore()));
                                BigDecimal b2 = new BigDecimal(Double.toString(myScore));
                                myScore = b1.add(b2).doubleValue();
                            }else if (game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_READY
                                &&game.getZhuang().equals(uuid)) {// 上局赢家为庄、已准备
                                result.put("win", 1);
                                BigDecimal b1 = new BigDecimal(Double.toString(game.getTotalScore()));
                                BigDecimal b2 = new BigDecimal(Double.toString(myScore));
                                myScore = b1.add(b2).doubleValue();
                            }else{
                                result.put("win", 0);
                            }
                            result.put("score", myScore);
                            result.put("totalScore", game.getPlayerMap().get(uuid).getScore());
                            jiesuanArray.add(result);
                        }

                        JSONObject result=new JSONObject();
                        result.put("type", 2);
                        result.put("game_index", game.getCurrentGameIndex());
                        result.put("di", game.getScore());
                        result.put("baseNum", game.getBaseNum());
                        result.put("gametype", game.getGameType());
                        result.put("isGameIng",game.getGameIngIndex());
                        result.put("isXiaZhuIng",game.getProgressIndex());
                        result.put("room_no", roomNo);
                        result.put("users",game.getAllPlayer());//告诉他原先加入的玩家
                        result.put("index",player.getMyIndex());
                        result.put("status",game.getPlayerStatus());
                        result.put("gameNum", game.getGameNum());
                        result.put("currentScore", game.getCurrentScore());
                        result.put("totalScore", game.getTotalScore());
                        result.put("jiesuan", jiesuanArray);
                        result.put("isReady", game.getPlayerIsReady());
                        result.put("isGameover", 1);
                        result.put("xiazhuList", game.getXiazhuList());
                        JSONArray array = new JSONArray();
                        for (String uuid : game.getPlayerMap().keySet()) {

                            int index = game.getPlayerMap().get(uuid).getMyIndex();
                            double myScore = game.getXiazhuScore(index);
                            JSONObject score = new JSONObject();
                            score.put("index", index);
                            score.put("myScore", myScore);
                            array.add(score);
                        }
                        result.put("myScore", array);
                        result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
                        UserPacket userPacket = game.getUserPacketMap().get(clientTag);
                        if(userPacket.isShow){
                            result.put("isShow", 1);
                            result.put("mypai", userPacket.getPai());
                            result.put("paiType", userPacket.getType());
                        }else{
                            result.put("isShow", 0);
                            result.put("mypai", new int[]{0,0,0});
                            result.put("paiType", 0);
                        }
                        result.put("showPai", userPacket.getBipaiList());

                        int readyCount = 0;
                        for (String uuid : game.getUserPacketMap().keySet()) {
                            int ready = game.getUserPacketMap().get(uuid).getStatus();
                            if(ready==ZhaJinHuaCore.USERPACKER_STATUS_READY){
                                readyCount++;
                            }
                        }
                        if (readyCount>=2) {
                            result.put("timer",game.getTimeLeft());
                        }

                        JSONArray jiesuanData = new JSONArray();
                        // 总结算数据
                        if(game.getGameCount()==game.getGameIndex()){

                            Set<String> uuids = game.getUserPacketMap().keySet();
                            // 大赢家分数
                            double dayinjia = 0;
                            for(String uid:uuids){
                                if(game.getPlayerMap().get(uid).getScore()>=dayinjia){
                                    dayinjia = game.getPlayerMap().get(uid).getScore();
                                }
                            }

                            // 设置玩家信息
                            for(String uid:uuids){
                                Playerinfo playerinfo = game.getPlayerMap().get(uid);
                                JSONObject user = new JSONObject();
                                user.put("name", playerinfo.getName());
                                user.put("account", playerinfo.getAccount());
                                user.put("headimg", playerinfo.getRealHeadimg());
                                user.put("score", playerinfo.getScore());

                                if(game.getFangzhu().equals(uid)){

                                    user.put("fangzhu", 1);
                                }else{
                                    user.put("fangzhu", 0);
                                }

                                if(playerinfo.getScore() == dayinjia){

                                    user.put("win", 1);
                                }else{
                                    user.put("win", 0);
                                }

                                jiesuanData.add(user);
                            }
                        }

                        if(jiesuanData.size()>0){
                            result.put("jiesuanData", jiesuanData);
                            result.put("jiesuanTime", game.getJiesuanTime());
                        }

                        LogUtil.print("断线重连返回(结算阶段)="+result.toString());
                        client.sendEvent("reconnectGamePush_ZJH", result);
                    }
                }else {
                    JSONObject result = new JSONObject();
                    result.put("type", 999); //玩家还未创建房间就已经掉线

                    client.sendEvent("reconnectGamePush_ZJH", result);
                }

                //通知其他人用户重连
                for(String uuid:game.getPlayerMap().keySet()){

                    SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                    if(askclient!=null){
                        JSONObject cl = new JSONObject();
                        cl.put("index", ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getPlayerIndex(clientTag));
                        askclient.sendEvent("userReconnectPush_ZJH", cl);
                    }
                }

            }else{

                JSONObject result = new JSONObject();
                result.put("type", 999); //玩家还未创建房间就已经掉线
                client.sendEvent("reconnectGamePush_ZJH", result);
                LogUtil.print("创建房间（999）："+JSONObject.fromObject(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }

    }


    public void gameConnReset(SocketIOClient client, Object data) {

        JSONObject postdata = JSONObject.fromObject(data);
        // 房间号
        String roomNo = postdata.getString("room_no");
        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){

//			ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));//获取房间
//			if(!room.getUuidList().contains(client.getSessionId())){ //判断玩家是否是重新进入游戏
//				// 重连恢复游戏
//				JSONObject userinfo = client.get("userinfo");
//				JSONObject dataJson = new JSONObject();
//				dataJson.put("room_no", roomNo);
//				dataJson.put("account", userinfo.getString("account"));
//				reconnectGame(client, dataJson);
//			}else{
//				JSONObject result = new JSONObject();
//				result.put("type", -1); //开启准备按钮
//				client.sendEvent("reconnectGamePush_ZJH", result);
//			}
                // 重连恢复游戏
                JSONObject userinfo = client.get("userinfo");
                JSONObject dataJson = new JSONObject();
                dataJson.put("room_no", roomNo);
                dataJson.put("account", userinfo.getString("account"));
                reconnectGame(client, dataJson);
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }

    public void changeTable(SocketIOClient client,Object data){
        JSONObject postdata = JSONObject.fromObject(data);
        String roomNo = postdata.getString("room_no");
        double di = postdata.getDouble("di");
        if (RoomManage.gameRoomMap.containsKey(roomNo)) {
            ZJHGame room = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
            if (room!=null) {
                // 查询剩余的房间
                String sql = "select $ from za_gamerooms where game_id=? and roomtype=? and status=? and room_no!=?";
                String temp = "room_no,base_info";
                for (int i = 0; i < room.getPlayerCount(); i++) {
                    temp += (",user_id"+i) ;
                }
                JSONArray roomArray = DBUtil.getObjectListBySQL(sql.replace("$", temp), new Object[]{6,room.getRoomType(),0,roomNo});

                // 是否可以换桌
                boolean canHuanZhuo = false;
                // 当前所有可换桌房间
                List<JSONObject> roomList = new ArrayList<JSONObject>();

                // 当前只有一个房间不能换桌
                if (roomArray.size()>0) {


                    for (int i = 0; i < roomArray.size(); i++) {
                        JSONObject roomInfo = roomArray.getJSONObject(i);
                        // 判断该房间是否有空座位
                        for (int j = 0; j < room.getPlayerCount(); j++) {
                            if (roomInfo.getJSONObject("base_info").containsKey("di")) {
                                if (roomInfo.getJSONObject("base_info").getDouble("di")==di) {
                                    if (roomInfo.getLong("user_id"+j)==0) {
                                        roomList.add(roomInfo);
                                        break;
                                    }
                                }
                            }
                        }
                    }

                    // 房间内无空座不能换桌
                    if (roomList.size()>0) {
                        canHuanZhuo = true;
                    }
                }

                if (canHuanZhuo) {
                    long id = room.getPlayerMap().get(Constant.getClientTag(client)).getId();
                    changeTableExitRoom(client,new Object());
                    String string = roomList.get(0).getString("room_no");
                    int index = 0;
                    for (int j = 0; j < room.getPlayerCount(); j++) {
                        if (roomList.get(0).getLong("user_id"+j)==0) {
                            index = j;
                            break;
                        }
                    }

                    sql = "update za_gamerooms set user_id"+index+" =? where room_no=?";
                    Object[] params = new Object[] { id, string };
                    DBUtil.executeUpdateBySQL(sql, params);
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("roomType", 1);
                    jsonObject.put("room_no", string);
                    jsonObject.put("account", Constant.getClientTag(client));
                    enterRoom(client, jsonObject);

                }else {
                    JSONObject obj = new JSONObject();
                    // type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
                    obj.put("type", 0);
                    obj.put("msg", "当前无空闲房间，请稍后再试");
                    client.sendEvent("tipMsgPush_ZJH", obj);
                }
            }
        }
    }

    /**
     * 退出房间
     * @param client
     * @param data
     */
    public void changeTableExitRoom(SocketIOClient client, Object data) {

        // 房间号
        String roomNo=client.get(Constant.ROOM_KEY_ZJH);
        if(RoomManage.gameRoomMap.containsKey(roomNo)){

            JSONObject userinfo = client.get("userinfo");
            if(!Dto.isObjNull(userinfo)){
                changeTableExitRoom(Constant.getClientTag(client), roomNo, userinfo.getLong("id"));
            }

            // 其他玩家已经准备则开始游戏
            if(RoomManage.gameRoomMap.containsKey(roomNo) && ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).isAllReady()){
                JSONObject obj = new JSONObject();
                obj.put("room_no", roomNo);
                startGame(((ZJHGame) RoomManage.gameRoomMap.get(roomNo)));
            }
        }
    }


    /**
     * 玩家退出玩家房间
     * @param uuid
     * @param roomNo
     * @param userId
     */
    public void changeTableExitRoom(String uuid, String roomNo, long userId) {

        if(RoomManage.gameRoomMap.containsKey(roomNo)){

            ZJHGame game = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

            boolean isRealExit = true;

            // 金币场庄家退出游戏
            if((game.getRoomType()==1||game.getRoomType()==3)&&game.getPlayerMap().size()>1){

                // 准备或者牌局结束阶段庄家退出
                if(game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_READY || game.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN){

                    // 金币场庄家退出游戏
                    if(game.getZhuang().equals(uuid)){

                        String oldZhuang = game.getZhuang();
                        String zhuang = game.getNextPlayer(oldZhuang);
                        if(!oldZhuang.equals(zhuang)){

                            ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
                            ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setFangzhu(zhuang);
                            JSONObject result = new JSONObject();
                            result.put("zhuang", game.getPlayerIndex(zhuang));
                            for (String uid  : game.getUserPacketMap().keySet()) {
                                // 重置庄家信息
                                SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
                                if(clientother!=null){
                                    clientother.sendEvent("huanZhuangPush_ZJH", result);
                                }
                            }
                        }
                    }
                }else if((game.getUserPacketMap().get(uuid)!=null
                    &&game.getUserPacketMap().get(uuid).getStatus()==-1)
                    ||game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_QIPAI){ // 玩家处于观战中，随时可以退出

                    // 弃牌退出扣元宝
                    if (game.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_QIPAI) {
                        Playerinfo playerinfo = game.getPlayerMap().get(uuid);
                        double xiazhuScore = game.getXiazhuScore(playerinfo.getMyIndex());
                        String sql = "update za_users set yuanbao=yuanbao-? where id=?";
                        DBUtil.executeUpdateBySQL(sql, new Object[]{xiazhuScore, playerinfo.getId()});
                    }
                    System.out.println("出门左转，请便！");

                }else{
                    isRealExit = false;

                    JSONObject result = new JSONObject();
                    result.put("type", 2); //退出房间
                    result.put("code", 0);
                    result.put("msg", "正在游戏中暂时无法换桌");
                    SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
                    if(askclient!=null){
                        askclient.sendEvent("exitRoomPush_ZJH", result);
                    }
                }
            }

            if(isRealExit){

                // 玩家下标
                int index = game.getPlayerIndex(uuid);

                if(game.getPlayerMap().get(uuid)!=null){

                    JSONObject result = new JSONObject();
                    result.put("type", 2); //退出房间
                    result.put("index", index);
                    for (String uuid1  : game.getUserPacketMap().keySet()) {
                        if (!uuid1.equals(uuid)) {
                            SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
                            if(askclient!=null){
                                askclient.sendEvent("exitRoomPush_ZJH", result);
                            }
                        }
                    }
                }

                // 获取房间信息
                JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
                if(room!=null&&room.containsKey("id")){

                    // 删除房间内玩家
                    mjBiz.delGameRoomUserByUid(room, userId);
                    ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserIDSet().remove(userId);
                }

                // 清除玩家下注信息
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).clearXiaZhuList(index);
                // 清除房间用户数据
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().remove(uuid);
                ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().remove(uuid);

                // 房间准备人数少于2时取消定时器，不踢人
                int count = 0;
                for (String uid:((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().keySet()) {
                    int ready = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uid).getIsReady();
                    if(ready!=0){
                        count++;
                    }
                }
                if (count<2) {
                    JSONObject result = new JSONObject();
                    result.put("type", MutliThreadZJH.READY);
                    result.put("timer", 0);
                    for (String uuid1  : game.getUserPacketMap().keySet()) {
                        SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
                        if(askclient!=null){
                            askclient.sendEvent("zjhTimerPush_ZJH", result);
                        }
                    }
                }

                // 金币场没人的房间直接清除
                if((game.getRoomType()==1||game.getRoomType()==3)&&((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size()==0){
                    RoomManage.gameRoomMap.remove(roomNo);
                    if(room!=null&&room.containsKey("id")){
                        String sql = "update za_gamerooms set status=? where id=?";
                        DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
                        LogUtil.print("金币场没人的房间直接清除："+roomNo);
                    }
                }
            }
        }
    }

    // 准备定时器
    public void ready(SocketIOClient client,Object data){
        // 如果房间存在
        JSONObject fromObject = JSONObject.fromObject(data);
        String roomNo = fromObject.getString("room_no");
        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){
                if(RoomManage.gameRoomMap.get(roomNo)!=null){

                    ZJHGame room = (ZJHGame) RoomManage.gameRoomMap.get(roomNo);

                    int count = 0;
                    for (String uid:room.getUserPacketMap().keySet()) {
                        int ready = room.getUserPacketMap().get(uid).getIsReady();
                        if(ready!=0){
                            count++;
                        }
                    }

                    if((room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_READY || room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN)&&count>1){


                        // 超过时间未准备，清出房间
                        try {
                            Set<String> uuidList = room.getUserPacketMap().keySet();
                            List<String> lxList = new ArrayList<String>();
                            for (String uuid : uuidList) {

                                if(room.getUserPacketMap().get(uuid).getStatus()!=ZhaJinHuaCore.USERPACKER_STATUS_READY){
                                    lxList.add(uuid);
                                }
                            }
                            for (String uuid : lxList) {

                                if(room.getPlayerMap().get(uuid)!=null){
                                    exitRoom(uuid, room.getRoomNo(), room.getPlayerMap().get(uuid).getId());
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if(room.getReadyCount()>1 && room.getUserPacketMap().size()>1 && room.isAllReady()){
                            // 触发开始游戏
                            startGame(room);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }

    // 下注定时器
    public void xiazhu(SocketIOClient client,Object data){
        JSONObject fromObject = JSONObject.fromObject(data);
        String roomNo = fromObject.getString("room_no");
        String focus = fromObject.getString("focus");
        try {
            RoomManage.lock(roomNo);
            if(RoomManage.gameRoomMap.containsKey(roomNo)){

                if(RoomManage.gameRoomMap.get(roomNo)!=null){
                    ZJHGame room = (ZJHGame) RoomManage.gameRoomMap.get(roomNo);
                    if(room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_XIAZHU && room.getFocus().equals(focus)){
                        // 超过时间未下注，自动弃牌

                        zjhService.qipai(focus, roomNo);

                        // 下一个操作的玩家
                        String nextUUID = room.getNextOperationPlayer(room, focus);
                        // 跟到底定时器
                        //new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
                        // 下注倒计时
                        //new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
                        TimerMsgData tmd=new TimerMsgData();
                        tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                        tmd.nType=10;
                        tmd.roomid=roomNo;
                        tmd.client=client;
                        tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID);
                        tmd.gid=6;
                        tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID), 6, 10);
                        GameMain.singleTime.createTimer(tmd);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }

    // 跟到底定时器
    public void gendaodi(SocketIOClient client, Object data){
        JSONObject fromObject = JSONObject.fromObject(data);
        String roomNo = fromObject.getString("room_no");
        String string = fromObject.getString("focus");
        try {
            RoomManage.lock(roomNo);
            if (RoomManage.gameRoomMap.containsKey(roomNo)) {
                if (RoomManage.gameRoomMap.get(roomNo)!=null) {
                    ZJHGame room = (ZJHGame) RoomManage.gameRoomMap.get(roomNo);
                    if (checkXiazhu(room)) {
                        if (room.getFocus().equals(string)&&room.getUserPacketMap().get(string).isGenDaoDi) {
                            if(room.getUserPacketMap().get(string).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
                                || room.getUserPacketMap().get(string).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
                                double score = room.getCurrentScore();

                                // 跟到底是否成功
                                boolean xiazhu2 = zjhService.xiazhu(string, roomNo, score, ZJHGameEventDeal.GENZHU);
                                if (xiazhu2) {
                                    // 下一个操作的玩家
                                    String nextUUID1 = room.getNextOperationPlayer(room, string);

                                    TimerMsgData tmd=new TimerMsgData();
                                    tmd.nTimeLimit=MutliThreadZJH.xzTimer;
                                    tmd.nType=10;
                                    tmd.roomid=roomNo;
                                    tmd.client=client;
                                    tmd.data=new JSONObject().element("room_no", roomNo).element("focus", nextUUID1);
                                    tmd.gid=6;
                                    tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo).element("focus", nextUUID1), 6, 10);
                                    GameMain.singleTime.createTimer(tmd);
                                }
                            }
                        }
                    }else {
                        System.out.println("超过单局下注上限或已达到下注轮数上限");
                        //强制结算
                        zjhService.compelBiPai(roomNo, string);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            RoomManage.unLock(roomNo);
        }
    }

    public boolean checkXiazhu(ZJHGame room) {
        // 超过单局下注上限，强制结算
        if(room.getSingleMaxScore()>0 && room.getTotalScore()>=room.getSingleMaxScore()){
            return false;
        }

        // 达到下注轮数上限（最后一轮所有人都已下注），强制结算
        if(room.getTotalGameNum()>0
            && room.getGameNum()>=room.getTotalGameNum()
            && room.getProgressIndex().length==room.getYixiazhu().size()){
            return false;
        }
        return true;
    }

    public void savelogs(String roomNo, JSONArray jiesuanData,
                         JSONArray jiesuanArray){
        ZJHGame room = (ZJHGame) RoomManage.gameRoomMap.get(roomNo);
        //房间信息
        JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
        if(roomInfo!=null){
            int game_index = roomInfo.getInt("game_index");
            // 保存游戏记录
            JSONObject gamelog = new JSONObject();
            gamelog.put("gid", 6);
            gamelog.put("room_id", roomInfo.getLong("id"));
            gamelog.put("room_no", roomNo);
            gamelog.put("game_index", game_index);
            gamelog.put("base_info", roomInfo.getString("base_info"));
            gamelog.put("result", jiesuanArray.toString());
            String nowTime = TimeUtil.getNowDate();
            gamelog.put("finishtime", nowTime);
            gamelog.put("createtime", nowTime);
            gamelog.put("status", 1);
            gamelog.put("roomtype", 0);
            if(jiesuanData.size()>0){
                gamelog.put("jiesuan", jiesuanData.toString());
            }

            long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);

            // 保存玩家战绩
            String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
                + "gamelog_id,result,createtime,account,fee) VALUES";
            Object[] params = new Object[room.getUserIDSet().size()];
            int temp = 0;
            for(Long uid:room.getUserIDSet()){
                double score = 0;
                for (Object jsonObject : jiesuanArray) {
                    JSONObject fromObject = JSONObject.fromObject(jsonObject);
                    if (fromObject.getLong("id")==uid) {
                        score = fromObject.getDouble("score");
                    }
                }
                gamelogSql += "("+6+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+","+
                    "?"+",'"+nowTime+"',"+score+","+room.getFee()+")";
                if (temp<room.getUserIDSet().size()-1) {
                    gamelogSql += ",";
                }
                params[temp] = jiesuanArray.toString();
                temp ++;
            }
            DBUtil.executeUpdateBySQL(gamelogSql, params);

            // 更新房间局数序号
            String sql = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
            DBUtil.executeUpdateBySQL(sql, new Object[]{roomNo});
        }
    }
}
