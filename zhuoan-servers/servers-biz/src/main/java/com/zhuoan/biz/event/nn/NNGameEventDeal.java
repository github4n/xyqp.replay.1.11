package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.protocol.Packet;
import com.zhuoan.biz.core.nn.NiuNiu;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.Packer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.PackerCompare;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
import com.zhuoan.biz.service.nn.impl.NiuNiuServiceImpl;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.Messages;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.times.TimerMsgData;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class NNGameEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(NNGameEventDeal.class);

	// 申请解散房间倒计时
	private static JSONObject CLOSETIME = JSONObject.fromObject("{\"default\": 180, \"WJY\": 180}");

	/**
	 * 0下注时间、1亮牌时间、2抢庄时间、3准备时间
	 */
	public static int[] GLOBALTIMER = new int[]{25,20,15,15};

	NiuNiuService nnService = new NiuNiuServiceImpl();
	MaJiangBiz mjBiz=new MajiangBizImpl();

	/**
	 * 创建、加入房间
	 * @param client
	 * @param data
	 */
	public void enterRoom(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

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

			// uuid
			String uuid=postdata.getString("uuid");
			// 缓存中没有才取数据库
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
				client.sendEvent("enterRoomPush_NN", result);
				return;
			}else{
				if(!userinfo.containsKey("headimg")){
					userinfo.element("headimg", "null");
				}
				client.set("userinfo", userinfo);
			}
			if(!userinfo.getString("uuid").equals(uuid)) {
				result.put("code", 0);
				result.put("msg", "该帐号已在其他地方登录");
				LogUtil.print("牛牛该帐号已在其他地方登录导致加入房间失败--------");
				client.sendEvent("enterRoomPush_NN", result);
				return;
			}
		}else{

			userinfo = client.get("userinfo");
		}
		result.put("code", 1);
		result.put("msg", "");

		try {
			RoomManage.lock(roomNo);
			if (RoomManage.gameRoomMap.containsKey(roomNo)) {
				if (RoomManage.gameRoomMap.get(roomNo)==null) {
					LogUtil.print("房间为空2导致加入房间失败,房间号为"+roomNo);
				}
			}
			if (RoomManage.gameRoomMap.get(roomNo)!=null) {
				NNGameRoom gameRoom = (NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
				JSONObject objInfo = gameRoom.getRoomInfo();
				
				JSONObject obj=new JSONObject();
				obj.put("room_no", roomNo);
				obj.put("roomType", gameRoom.getRoomType());
				// 游戏倒计时设置
				if(objInfo.containsKey("globalTimer")){
					JSONArray array = objInfo.getJSONArray("globalTimer");
					for (int i = 0; i < array.size(); i++) {
						GLOBALTIMER[i] = array.getInt(i);
					}
				}
				
				obj.put("globalTimer", GLOBALTIMER);
				
				if(gameRoom.getRoomType()==0 || gameRoom.getRoomType()==2){ // 房卡模式
					
					obj.put("game_count", gameRoom.getGameCount());
					
					if(objInfo.containsKey("player")&&objInfo.containsKey("turn")){
						
						int type = objInfo.getInt("type");
						String wanfa = "";
						if(type==0){
							wanfa = "房主坐庄";
						}else if(type==1){
							wanfa = "轮庄";
						}else if(type==2){
							wanfa = "抢庄";
						}else if(type==3){
							wanfa = "明牌抢庄";
						}else if(type==4){
							wanfa = "牛牛坐庄";
						}
						// 房间信息
						String roominfo = wanfa+"/" 
								+ objInfo.get("player")+"人/" 
								+ objInfo.getJSONObject("turn").getInt("turn") + "局";
						// AA支付
						if(gameRoom.getPayType()==1){
							roominfo = roominfo + "/房费AAx" + objInfo.getJSONObject("turn").getInt("AANum");
						}
						obj.put("roominfo", roominfo);
					}
				}
				
				if(gameRoom.getRoomType()==3){ // 元宝模式
					
					if(objInfo.containsKey("yuanbao")){
						
						// 房间信息
						StringBuffer roominfo = new StringBuffer();
						roominfo.append("底注:");
						roominfo.append(objInfo.get("yuanbao"));
						roominfo.append(" 进:");
						roominfo.append(objInfo.get("enterYB"));
						roominfo.append(" 出:");
						roominfo.append(objInfo.get("leaveYB"));
						obj.put("roominfo", roominfo.toString());
						int type = objInfo.getInt("type");
						String wanfa = "";
						if(type==0){
							wanfa = "房主坐庄";
						}else if(type==1){
							wanfa = "轮庄";
						}else if(type==2){
							wanfa = "抢庄";
						}else if(type==3){
							wanfa = "明牌抢庄";
						}else if(type==4){
							wanfa = "牛牛坐庄";
						}else if(type==5){
							wanfa = "通比牛牛";
						}
						RoomManage.gameRoomMap.get(roomNo).setWfType(wanfa);
						obj.put("roominfo2", wanfa);
					}
					// 判断玩家元宝是否足够
					if(userinfo.getDouble("yuanbao")<objInfo.getDouble("enterYB")) {
						
						// 删除房间内玩家
						//mjBiz.delGameRoomUserByUid(room, userId);
						
						result.put("code", 0);
						result.put("msg", "您的元宝不足，请先充值");
						client.sendEvent("enterRoomPush_NN", result);
						RoomManage.unLock(roomNo);
						return;
					}
				}
				
				// 抢庄倒计时
				int qztimer = 15;
				if (RoomManage.gameRoomMap.get(roomNo).getFirstTime()==0) {
					
					if(RoomManage.gameRoomMap.containsKey(roomNo)){
						Playerinfo player = new Playerinfo();
						
						player.setId(userinfo.getLong("id"));
						player.setAccount(account);
						player.setName(userinfo.getString("name"));
						player.setUuid(client.getSessionId());
						int myIndex = -1;
						for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
							if (gameRoom.getUserIdList().get(i)==userinfo.getLong("id")) {
								myIndex = i;
								break;
							}
						}
						player.setMyIndex(myIndex);
						if(gameRoom.getRoomType() == 1){ // 金币模式
							player.setScore(userinfo.getDouble("coins"));
						}else if(gameRoom.getRoomType() == 3){ // 元宝模式
							player.setScore(userinfo.getDouble("yuanbao"));
						}else{ // 房卡模式
							player.setScore(0);
						}
						player.setHeadimg(userinfo.getString("headimg"));
						player.setSex(userinfo.getString("sex"));
						//player.setIp(userinfo.getString("ip"));
						player.setIp("");
						if(userinfo.containsKey("sign")){
							player.setSignature(userinfo.getString("sign"));
						}else{
							player.setSignature("");
						}
						if (userinfo.containsKey("ghName")) {
							player.setGhName(userinfo.getString("ghName"));
						}
						if (userinfo.containsKey("openid")) {
							player.setOpenid(userinfo.getString("openid"));
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
						if (userinfo.containsKey("luck")) {
							player.setLuck(userinfo.getInt("luck"));
						}
						
						JSONObject room1 = new JSONObject();
						room1.element("base_info", gameRoom.getRoomInfo());
						room1.element("room_no", gameRoom.getRoomNo());
						room1.element("roomtype", gameRoom.getRoomType());
						room1.element("game_count", gameRoom.getGameCount());
						room1.element("game_id", gameRoom.getGid());
						
						//创建房间
						NNGameRoom nnGame = nnService.createGameRoom(room1, clientTag, player);
						//金币不足
						if (gameRoom.getRoomType()==1&&nnGame.getGoldcoins()>userinfo.getDouble("coins")) {
							RoomManage.gameRoomMap.remove(nnGame.getRoomNo());
							if (nnGame.isRobot()&&nnGame.getRobotList().size()>0) {
								/*for (String str : nnGame.getRobotList()) {
									String sql = "update za_users set status=0 where account=?";
									DBUtil.executeUpdateBySQL(sql, new Object[]{str});
								}*/
							}
							JSONObject enterResult = new JSONObject();
							enterResult.put("code", 0);
							enterResult.put("msg", "您的金币不足，请先充值");
							client.sendEvent("enterRoomPush_NN", enterResult);
							RoomManage.unLock(roomNo);
							return;
						}
						if (nnGame.isRobot()&&nnGame.getRobotList().size()>0) {
							AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 0);
							a.start();
						}
						
						client.set("ROOM_KEY_NN", roomNo);
						
						obj.put("users",nnGame.getAllPlayer());//告诉他原先加入的玩家
						obj.put("myIndex",player.getMyIndex());
						obj.put("isReady",nnGame.getReadyIndex());
						if(nnGame.getZhuang()!=null){
							// 抢庄模式
							if((nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3)
									&&nnGame.getGameStatus()== NiuNiu.GAMESTATUS_QIANGZHUANG){
								obj.put("zhuang",-1);
								obj.put("qiangzhuang", nnGame.getPlayerIsReady());
								obj.put("qztimer", qztimer);
							}else{
								obj.put("zhuang",nnGame.getPlayerIndex(nnGame.getZhuang()));
							}
						}else{
							obj.put("zhuang",-1);
							obj.put("qiangzhuang", nnGame.getPlayerIsReady());
							obj.put("qztimer", qztimer);
						}
						
						// 抢庄加倍
						if(nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3){
							obj.put("qzTimes", nnGame.qzTimes);
							if(gameRoom.getRoomType()==1 || gameRoom.getRoomType()==3){
								obj.put("qzTimes2", nnGame.getQzTimes(player.getScore()));
							}
							obj.put("qzhuangtimes", nnGame.getPlayerQzResult());
						}
						if(nnGame.getZhuangType()==3){
							obj.put("qzType", 1);
						}
						// 通比模式
						if(nnGame.getZhuangType()==5){
							obj.put("wanfaType", 1);
						}
						obj.put("game_index", nnGame.getGameIndex()+1);
						obj.put("di", nnGame.getScore());
						obj.put("baseNum", nnGame.getBaseNum());
						obj.put("gametype", nnGame.getGameType());
						obj.put("isGameIng",nnGame.getGameIngIndex());
						
						result.put("data", obj);
						LogUtil.print("==========创建房间："+result);
						client.sendEvent("enterRoomPush_NN", result);
					}
				}else {
					int myIndex = -1;
					for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
						if (gameRoom.getUserIdList().get(i)==userinfo.getLong("id")) {
							myIndex = i;
							break;
						}
					}
					if (myIndex<0) {
						result.put("code", 0);
						result.put("msg", "加入房间失败");
						client.sendEvent("enterRoomPush_NN", result);
						RoomManage.unLock(roomNo);
						return;
					}
					
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
					if(gameRoom.getRoomType() == 1){ // 金币模式
						player.setScore(userinfo.getDouble("coins"));
					}else if(gameRoom.getRoomType() == 3){ // 元宝模式
						player.setScore(userinfo.getDouble("yuanbao"));
					}else{ // 房卡模式
						player.setScore(0);
					}
					player.setHeadimg(userinfo.getString("headimg"));
					player.setSex(userinfo.getString("sex"));
					System.err.println(userinfo);
					//player.setIp(userinfo.getString("ip"));
					player.setIp("");
					if(userinfo.containsKey("sign")){
						player.setSignature(userinfo.getString("sign"));
					}else{
						player.setSignature("");
					}
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
					if (userinfo.containsKey("luck")) {
						player.setLuck(userinfo.getInt("luck"));
					}
					if (userinfo.containsKey("openid")) {
						player.setOpenid(userinfo.getString("openid"));
					}
					
					//加入房间
					boolean joinResult = nnService.joinGameRoom(roomNo, clientTag, player, isNext);
					client.set(Constant.ROOM_KEY_NN, roomNo);
					
					NNGameRoom nnGame = (NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
					
					Set<String> uuids=nnGame.getPlayerMap().keySet();//获取原来房间里的人
					
					obj.put("users",nnGame.getAllPlayer());//告诉他原先加入的玩家
					if (nnGame.isGuanzhan()) {
						// 观战玩家myindex为-1
						obj.put("myIndex", NiuNiu.USERPACKER_STATUS_GUANZHAN);
					}else {
						obj.put("myIndex", myIndex);
					}
					obj.put("isReady",nnGame.getReadyIndex());
					
					if(nnGame.getZhuang()!=null){
						// 抢庄模式
						if((nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3)
								&&nnGame.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
							obj.put("zhuang",-1);
							obj.put("qiangzhuang", nnGame.getPlayerIsReady());
							obj.put("qztimer", qztimer);
						}else{
							// 进入一个空的房间，自己当庄
							if(nnGame.getPlayerMap().get(nnGame.getZhuang())!=null){
								obj.put("zhuang",nnGame.getPlayerIndex(nnGame.getZhuang()));
							}else{
								obj.put("zhuang", myIndex);
								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setFangzhu(clientTag);
								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setZhuang(clientTag);
							}
						}
					}else{
						obj.put("zhuang",-1);
						obj.put("qiangzhuang", nnGame.getPlayerIsReady());
						obj.put("qztimer", qztimer);
					}
					
					// 抢庄加倍
					if(nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3){
						obj.put("qzTimes", nnGame.qzTimes);
						if(gameRoom.getRoomType()==1 || gameRoom.getRoomType()==3){
							obj.put("qzTimes2", nnGame.getQzTimes(player.getScore()));
						}
						obj.put("qzhuangtimes", nnGame.getPlayerQzResult());
					}
					if(nnGame.getZhuangType()==3){
						obj.put("qzType", 1);
					}
					// 通比模式
					if(nnGame.getZhuangType()==5){
						obj.put("wanfaType", 1);
					}
					obj.put("game_index", nnGame.getGameIndex()+1);
					obj.put("di", nnGame.getScore());
					obj.put("baseNum", nnGame.getBaseNum());
					obj.put("gametype", nnGame.getGameType());
					obj.put("isGameIng",nnGame.getGameIngIndex());
					//				obj.put("readyStatus", nnGame.getUserPacketMap().get(clientTag).getStatus());
					if(postdata.containsKey("isNext")){
						obj.put("isNext", postdata.get("isNext"));
					}
					
					LogUtil.print("加入房间："+obj);
					result.put("data", obj);
					client.sendEvent("enterRoomPush_NN", result);
					
					if(uuids.size()>1 && joinResult){// 退出重进时不通知其他玩家
						
						JSONObject result1 = new JSONObject();
						JSONObject obj1=new JSONObject();
						JSONObject playerObj=new JSONObject();
						playerObj.put("account", player.getAccount());
						playerObj.put("name",player.getName());
						playerObj.put("headimg",player.getRealHeadimg());
						playerObj.put("sex",player.getSex());
						playerObj.put("ip",player.getIp());
						playerObj.put("ghName",player.getGhName());
						playerObj.put("introduction",player.getSignature());
						playerObj.put("vip", player.getVip());
						playerObj.put("area", player.getArea());
						playerObj.put("location", player.getLocation());
						if(nnGame.getPlayerMap().get(clientTag)!=null){
							playerObj.put("score",nnGame.getPlayerMap().get(clientTag).getScore());
						}else{
							playerObj.put("score",player.getScore());
						}
						playerObj.put("index",player.getMyIndex());
						UserPacket up = nnGame.getUserPacketMap().get(clientTag);
						playerObj.put("readyStatus", up.getStatus());
						obj1.put("user", playerObj);
						result1.put("code", 1);
						result1.put("msg", "");
						result1.put("data", obj1);
						
						for(String other:((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().keySet()){
							if(!other.equals(clientTag)&&!nnGame.getRobotList().contains(other)){
								SocketIOClient clientother= GameMain.server.getClient(nnGame.getUUIDByClientTag(other));
								if(clientother!=null){
									clientother.sendEvent("playerEnterPush_NN", result1);
								}
							}
						}
						
						// 通知观战玩家
						if (nnGame.isGuanzhan()&&nnGame.getGzPlayerMap().size()>0) {
							for (String string : nnGame.getGzPlayerMap().keySet()) {
								SocketIOClient clientother=GameMain.server.getClient(nnGame.getGzPlayerMap().get(string).getUuid());
								if(clientother!=null){
									clientother.sendEvent("playerEnterPush_NN", result1);
								}
							}
						}
						
						//每有一个非机器人加入房间则机器人列表长度减1
						if (nnGame.isRobot()&&!nnGame.getRobotList().contains(account)&&nnGame.getRobotList().size()>0&&!nnGame.getPlayerMap().containsKey(clientTag)) {
							List<String> list = new ArrayList<String>();
							for (int i = 0; i < nnGame.getRobotList().size()-1; i++) {
								list.add(nnGame.getRobotList().get(i));
							}
							nnGame.setRobotList(list);
							//String sql = "update za_users set status=0 where account=?";
							//DBUtil.executeUpdateBySQL(sql, new Object[]{nnGame.getRobotList().get(nnGame.getRobotList().size()-1)});
						}
						
						if (!nnGame.getRobotList().contains(clientTag)&&nnGame.isRobot()&&nnGame.getRobotList().size()>0&&!nnGame.getPlayerMap().containsKey(clientTag)) {
							AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 3);
							a.start();
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

		// 获取房间信息

		/*JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
			int myIndex = -1;
			if(room!=null&&room.containsKey("id")){

			// 房间类型（0：房卡  1：金币）
			int roomType = 0;
			if(room.containsKey("roomtype")&&room.get("roomtype")!=null){
				roomType = room.getInt("roomtype");
			}

			if(room.getInt("game_count")>room.getInt("game_index") || roomType == 1 || roomType == 3){ //房间局数还未用完

				long userId = userinfo.getLong("id");

				if(userId==room.getLong("user_id0")){
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
				}

				if (myIndex<0) {
					result.put("code", 0);
					result.put("msg", "加入房间失败");
					client.sendEvent("enterRoomPush_NN", result);
					return;
				}

				// 房间属性信息
				String base_info = room.getString("base_info");
				JSONObject objInfo = JSONObject.fromObject(base_info);

				JSONObject obj=new JSONObject();
				obj.put("room_no", roomNo);
				obj.put("roomType", room.getInt("roomtype"));
				// 游戏倒计时设置
				if(objInfo.containsKey("globalTimer")){
					JSONArray array = objInfo.getJSONArray("globalTimer");
					for (int i = 0; i < array.size(); i++) {
						GLOBALTIMER[i] = array.getInt(i);
					}
				}

				// 各个阶段倒计时
				JSONObject gameInfo1 = mjBiz.getGameInfoByID(1);
				if (!Dto.isObjNull(gameInfo1)) {
					JSONObject gameInfo = JSONObject.fromObject(gameInfo1.get("setting"));
					// 下注时间
					if (gameInfo!=null) {
						if (gameInfo.containsKey("xiazhuTime")&&gameInfo.getInt("xiazhuTime")!=0) {
							GLOBALTIMER[0] = gameInfo.getInt("xiazhuTime");
						}
						// 亮牌时间
						if (gameInfo.containsKey("liangpaiTime")&&gameInfo.getInt("liangpaiTime")!=0) {
							GLOBALTIMER[1] = gameInfo.getInt("liangpaiTime");
						}
						// 抢庄时间
						if (gameInfo.containsKey("qiangzhuangTime")&&gameInfo.getInt("qiangzhuangTime")!=0) {
							GLOBALTIMER[2] = gameInfo.getInt("qiangzhuangTime");
						}
						// 准备时间
						if (gameInfo.containsKey("zhunbeiTime")&&gameInfo.getInt("zhunbeiTime")!=0) {
							GLOBALTIMER[3] = gameInfo.getInt("zhunbeiTime");
						}
					}
				}
				obj.put("globalTimer", GLOBALTIMER);

				if(room.getInt("roomtype")==0 || room.getInt("roomtype")==2){ // 房卡模式

					obj.put("game_count", room.getInt("game_count"));

					if(objInfo.containsKey("player")&&objInfo.containsKey("turn")){

						int type = objInfo.getInt("type");
						String wanfa = "";
						if(type==0){
							wanfa = "房主坐庄";
						}else if(type==1){
							wanfa = "轮庄";
						}else if(type==2){
							wanfa = "抢庄";
						}else if(type==3){
							wanfa = "明牌抢庄";
						}else if(type==4){
							wanfa = "牛牛坐庄";
						}
						// 房间信息
						String roominfo = wanfa+"/" 
								+ objInfo.get("player")+"人/" 
								+ objInfo.getJSONObject("turn").getInt("turn") + "局";
						// AA支付
						if(room.containsKey("paytype") && room.getInt("paytype")==1){
							roominfo = roominfo + "/房费AAx" + objInfo.getJSONObject("turn").getInt("AANum");
						}
						obj.put("roominfo", roominfo);
					}
				}

				if(room.getInt("roomtype")==3){ // 元宝模式

					if(objInfo.containsKey("yuanbao")){

						// 房间信息
						StringBuffer roominfo = new StringBuffer();
						roominfo.append("底注:");
						roominfo.append(objInfo.get("yuanbao"));
						roominfo.append(" 进:");
						roominfo.append(objInfo.get("enterYB"));
						roominfo.append(" 出:");
						roominfo.append(objInfo.get("leaveYB"));
						obj.put("roominfo", roominfo.toString());
						int type = objInfo.getInt("type");
						String wanfa = "";
						if(type==0){
							wanfa = "房主坐庄";
						}else if(type==1){
							wanfa = "轮庄";
						}else if(type==2){
							wanfa = "抢庄";
						}else if(type==3){
							wanfa = "明牌抢庄";
						}else if(type==4){
							wanfa = "牛牛坐庄";
						}else if(type==5){
							wanfa = "通比牛牛";
						}
						obj.put("roominfo2", wanfa);
					}
					// 判断玩家元宝是否足够
					if(userinfo.getDouble("yuanbao")<objInfo.getDouble("enterYB")) {

						// 删除房间内玩家
						mjBiz.delGameRoomUserByUid(room, userId);

						result.put("code", 0);
						result.put("msg", "您的元宝不足，请先充值");
						client.sendEvent("enterRoomPush_NN", result);
						return;
					}
				}

				// 抢庄倒计时
				int qztimer = 15;

				if(!Constant.niuNiuGameMap.containsKey(roomNo)){

					Playerinfo player = new Playerinfo();

					player.setId(userinfo.getLong("id"));
					player.setAccount(account);
					player.setName(userinfo.getString("name"));
					player.setUuid(client.getSessionId());
					player.setMyIndex(myIndex);
					if(roomType == 1){ // 金币模式
						//player.setScore(userinfo.getInt("coins"));
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
					if (userinfo.containsKey("luck")) {
						player.setLuck(userinfo.getInt("luck"));
					}

					//创建房间
					NNGameRoom nnGame = nnService.createGameRoom(room, clientTag, player);
					//金币不足
					if (roomType==1&&nnGame.getGoldcoins()>userinfo.getDouble("coins")) {
						Constant.niuNiuGameMap.remove(nnGame.getRoomNo());
						if (nnGame.isRobot()&&nnGame.getRobotList().size()>0) {
							for (String str : nnGame.getRobotList()) {
								String sql = "update za_users set status=0 where account=?";
								DBUtil.executeUpdateBySQL(sql, new Object[]{str});
							}
						}
						JSONObject enterResult = new JSONObject();
						enterResult.put("code", 0);
						enterResult.put("msg", "您的金币不足，请先充值");
						client.sendEvent("enterRoomPush_NN", enterResult);
						return;
					}
					if (nnGame.isRobot()&&nnGame.getRobotList().size()>0) {
						AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 0);
						a.start();
					}

					client.set(Constant.ROOM_KEY_NN, roomNo);

					obj.put("users",nnGame.getAllPlayer());//告诉他原先加入的玩家
					obj.put("myIndex",player.getMyIndex());
					obj.put("isReady",nnGame.getReadyIndex());
					if(nnGame.getZhuang()!=null){
						// 抢庄模式
						if((nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3)
								&&nnGame.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
							obj.put("zhuang",-1);
							obj.put("qiangzhuang", nnGame.getPlayerIsReady());
							obj.put("qztimer", qztimer);
						}else{
							obj.put("zhuang",nnGame.getPlayerIndex(nnGame.getZhuang()));
						}
					}else{
						obj.put("zhuang",-1);
						obj.put("qiangzhuang", nnGame.getPlayerIsReady());
						obj.put("qztimer", qztimer);
					}

					// 抢庄加倍
					if(nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3){
						obj.put("qzTimes", nnGame.qzTimes);
						if(roomType==1 || roomType==3){
							obj.put("qzTimes2", nnGame.getQzTimes(player.getScore()));
						}
						obj.put("qzhuangtimes", nnGame.getPlayerQzResult());
					}
					if(nnGame.getZhuangType()==3){
						obj.put("qzType", 1);
					}
					// 通比模式
					if(nnGame.getZhuangType()==5){
						obj.put("wanfaType", 1);
					}
					obj.put("game_index", nnGame.getGameIndex()+1);
					obj.put("di", nnGame.getScore());
					obj.put("baseNum", nnGame.getBaseNum());
					obj.put("gametype", nnGame.getGameType());
					obj.put("isGameIng",nnGame.getGameIngIndex());

					LogUtil.print("创建房间："+obj);
					result.put("data", obj);
					client.sendEvent("enterRoomPush_NN", result);

				}else{//加入房间
					//金币不足
					if (roomType==1&&Constant.niuNiuGameMap.get(roomNo).getGoldcoins()>userinfo.getDouble("coins")) {
						JSONObject enterResult = new JSONObject();
						enterResult.put("code", 0);
						enterResult.put("msg", "您的金币不足，请先充值");
						client.sendEvent("enterRoomPush_NN", enterResult);
						String sqlString = "update za_gamerooms set user_id"+myIndex+"=0 where room_no =?";
						DBUtil.executeUpdateBySQL(sqlString, new Object[]{roomNo});
						Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().remove(userinfo.getString("account"));
						Constant.niuNiuGameMap.get(roomNo).getPlayerMap().remove(userinfo.getString("account"));
						return;
					}

					if (myIndex<0) {
						result.put("code", 0);
						result.put("msg", "加入房间失败");
						client.sendEvent("enterRoomPush_NN", result);
						return;
					}

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
					if (userinfo.containsKey("luck")) {
						player.setLuck(userinfo.getInt("luck"));
					}

					//加入房间
					boolean joinResult = nnService.joinGameRoom(roomNo, clientTag, player, isNext);
					client.set(Constant.ROOM_KEY_NN, roomNo);

					NNGameRoom nnGame = Constant.niuNiuGameMap.get(roomNo);

					Set<String> uuids=nnGame.getPlayerMap().keySet();//获取原来房间里的人

					obj.put("users",nnGame.getAllPlayer());//告诉他原先加入的玩家
					if (nnGame.isGuanzhan()) {
						// 观战玩家myindex为-1
						obj.put("myIndex", NiuNiu.USERPACKER_STATUS_GUANZHAN);
					}else {
						obj.put("myIndex", myIndex);
					}
					obj.put("isReady",nnGame.getReadyIndex());

					if(nnGame.getZhuang()!=null){
						// 抢庄模式
						if((nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3)
								&&nnGame.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
							obj.put("zhuang",-1);
							obj.put("qiangzhuang", nnGame.getPlayerIsReady());
							obj.put("qztimer", qztimer);
						}else{
							// 进入一个空的房间，自己当庄
							if(nnGame.getPlayerMap().get(nnGame.getZhuang())!=null){
								obj.put("zhuang",nnGame.getPlayerIndex(nnGame.getZhuang()));
							}else{
								obj.put("zhuang", myIndex);
								Constant.niuNiuGameMap.get(roomNo).setFangzhu(clientTag);
								Constant.niuNiuGameMap.get(roomNo).setZhuang(clientTag);
							}
						}
					}else{
						obj.put("zhuang",-1);
						obj.put("qiangzhuang", nnGame.getPlayerIsReady());
						obj.put("qztimer", qztimer);
					}

					// 抢庄加倍
					if(nnGame.getZhuangType()==2 || nnGame.getZhuangType()==3){
						obj.put("qzTimes", nnGame.qzTimes);
						if(roomType==1 || roomType==3){
							obj.put("qzTimes2", nnGame.getQzTimes(player.getScore()));
						}
						obj.put("qzhuangtimes", nnGame.getPlayerQzResult());
					}
					if(nnGame.getZhuangType()==3){
						obj.put("qzType", 1);
					}
					// 通比模式
					if(nnGame.getZhuangType()==5){
						obj.put("wanfaType", 1);
					}
					obj.put("game_index", nnGame.getGameIndex()+1);
					obj.put("di", nnGame.getScore());
					obj.put("baseNum", nnGame.getBaseNum());
					obj.put("gametype", nnGame.getGameType());
					obj.put("isGameIng",nnGame.getGameIngIndex());
//					obj.put("readyStatus", nnGame.getUserPacketMap().get(clientTag).getStatus());
					if(postdata.containsKey("isNext")){
						obj.put("isNext", postdata.get("isNext"));
					}

					LogUtil.print("加入房间："+obj);
					result.put("data", obj);
					client.sendEvent("enterRoomPush_NN", result);

					if(uuids.size()>1 && joinResult){// 退出重进时不通知其他玩家

						JSONObject obj1=new JSONObject();
						JSONObject playerObj=new JSONObject();
						playerObj.put("account", player.getAccount());
						playerObj.put("name",player.getName());
						playerObj.put("headimg",player.getRealHeadimg());
						playerObj.put("sex",player.getSex());
						playerObj.put("ip",player.getIp());
						playerObj.put("introduction",player.getSignature());
						playerObj.put("vip", player.getVip());
						playerObj.put("area", player.getArea());
						playerObj.put("location", player.getLocation());
						if(nnGame.getPlayerMap().get(clientTag)!=null){
							playerObj.put("score",nnGame.getPlayerMap().get(clientTag).getScore());
						}else{
							playerObj.put("score",player.getScore());
						}
						playerObj.put("index",player.getMyIndex());
						UserPacket up = nnGame.getUserPacketMap().get(clientTag);
						playerObj.put("readyStatus", up.getStatus());
						obj1.put("user", playerObj);
						result.put("data", obj1);

						for(String other:Constant.niuNiuGameMap.get(roomNo).getPlayerMap().keySet()){
							if(!other.equals(clientTag)&&!nnGame.getRobotList().contains(other)){
								SocketIOClient clientother=GameMain.server.getClient(nnGame.getUUIDByClientTag(other));
								if(clientother!=null){
									clientother.sendEvent("playerEnterPush_NN", result);
								}
							}
						}

						// 通知观战玩家
						if (nnGame.isGuanzhan()&&nnGame.getGzPlayerMap().size()>0) {
							for (String string : nnGame.getGzPlayerMap().keySet()) {
								SocketIOClient clientother=GameMain.server.getClient(nnGame.getGzPlayerMap().get(string).getUuid());
								if(clientother!=null){
									clientother.sendEvent("playerEnterPush_NN", result);
								}
							}
						}

						//每有一个非机器人加入房间则机器人列表长度减1
						if (nnGame.isRobot()&&!nnGame.getRobotList().contains(account)&&nnGame.getRobotList().size()>0&&!nnGame.getPlayerMap().containsKey(clientTag)) {
							List<String> list = new ArrayList<String>();
							for (int i = 0; i < nnGame.getRobotList().size()-1; i++) {
								list.add(nnGame.getRobotList().get(i));
							}
							nnGame.setRobotList(list);
							String sql = "update za_users set status=0 where account=?";
							DBUtil.executeUpdateBySQL(sql, new Object[]{nnGame.getRobotList().get(nnGame.getRobotList().size()-1)});
						}

						if (!nnGame.getRobotList().contains(clientTag)&&nnGame.isRobot()&&nnGame.getRobotList().size()>0&&!nnGame.getPlayerMap().containsKey(clientTag)) {
							AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 3);
							a.start();
						}
					}

					// 金币场超过时间没有开始游戏，需要换庄
					//					if(roomType==1 && Constant.niuNiuGameMap.get(roomNo).getGameStatus()==NiuNiu.GAMESTATUS_READY){
					//						
					//						// 开启准备定时器，开始计时
					//						MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 0);
					//						m.start();
					//					}
				}
			}
		}*/
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛enterRoom方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 庄家开始游戏（开始下注）
	 * @param client
	 * @param data
	 */
	public void gameReady(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");

		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				String clientTag = Constant.getClientTag(client);
				NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
				
				if (room.getGameStatus()!=NiuNiu.GAMESTATUS_READY&&room.getGameStatus()!=NiuNiu.GAMESTATUS_LIANGPAI) {
					RoomManage.unLock(roomNo);
					return;
				}
				
				if(room.getRoomType()==1||room.getRoomType()==3){ // 金币、元宝模式
					
					// 下一局开始时，清除掉线或金币不足的玩家
					List<String> cleanPlayer = nnService.cleanPlayer(room);
					if (cleanPlayer.contains(clientTag)) {
						RoomManage.unLock(roomNo);
						return;
					}
				}
				
				if(!postdata.containsKey("auto")){
					for (String uuid : room.getPlayerMap().keySet()) {
						if (!room.getRobotList().contains(uuid)) {
							JSONObject result = new JSONObject();
							result.put("users",room.getAllPlayer());
							
							SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
							if(clientother!=null){
								clientother.sendEvent("userInfoPush_NN", result);
							}
						}
					}
				}
				
				// 是否开始游戏
				boolean startGame = false;
				
				//			if(room.getRoomType()==1&&room.getZhuang().equals(clientTag)){ // 金币场庄家点击开始游戏，直接开始
				//				
				//				startGame = true;
				//				
				//			}else if(room.getRoomType()==0 || room.getRoomType()==2 || room.getRoomType()==3){ // 房卡场需要所有人都准备好才能开始
				//			}
				
				// 准备
				NiuNiuServer.isReady(roomNo, clientTag);
				
				// 两个人以上准备则开启准备倒计时
				if(!postdata.containsKey("auto") && room.getUserIDSet().size()>2 && room.getReadyCount()==2){
					// 开启准备定时器，开始计时
					/*MutliThreadNN m = new MutliThreadNN(null, roomNo, 6);
				m.start();*/
					NNGameRoom game = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
					JSONObject result = new JSONObject();
					result.put("timer", GLOBALTIMER[3]);
					for (String uuid  : game.getUserPacketMap().keySet()) {
						if (!game.getRobotList().contains(uuid)) {
							
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("nnTimerPush_NN", result);
							}
						}
					}
					//定时器
					
					//GameMain.singleTime.deleteTimer(roomNo);
					TimerMsgData tmd=new TimerMsgData();
					tmd.nTimeLimit=GLOBALTIMER[3];
					tmd.nType=17;
					tmd.roomid=roomNo;
					tmd.client=client;
					tmd.data=new JSONObject().element("room_no", roomNo);
					tmd.gid=1;
					tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo), 1, 17);
					GameMain.singleTime.createTimer(tmd);
				}
				
				boolean temp = false;
				for (String account : room.getPlayerMap().keySet()) {
					if (!Dto.stringIsNULL(room.getPlayerMap().get(account).getOpenid())) {
						temp = true;
					}
				}
				
				// 6人场两人以上就可以开始
				if(room.getReadyCount() == room.getPlayerCount() 
						|| (room.getPlayerCount()>=6 && room.getReadyCount()>=2 && room.getReadyCount()==room.getUserPacketMap().size())){ 
					
					startGame = true;
					
					// 是否允许玩家中途加入
					if(!room.isHalfwayin()){
						
						if(room.getGameIndex()==0){
							// 开始游戏后禁止其他玩家加入房间
							//						mjBiz.stopJoin(roomNo);
						}
					}
					
				}else{
					
					// 抢庄完第二次准备不需要通知
					// 自动准备不需要通知
					if(!postdata.containsKey("auto") && !(postdata.containsKey("qzhuang")&&postdata.getInt("qzhuang")==1)){
						
						JSONObject result = new JSONObject();
						result.put("myIndex", room.getPlayerIndex(clientTag));
						result.put("isReady", room.getReadyIndex());
						result.put("game_index", room.getGameIndex()+1);
						
						// 准备状态
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_READY);
						
						// 通知玩家
						for (String uuid : room.getPlayerMap().keySet()) {
							if (!room.getRobotList().contains(uuid)) {
								SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
								if(clientother!=null){
									clientother.sendEvent("playerReadyPush_NN", result);
								}
							}
						}
						// 通知观战玩家
						if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
							for (String string : room.getGzPlayerMap().keySet()) {
								SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
								if(clientother!=null){
									clientother.sendEvent("playerReadyPush_NN", result);
								}
							}
						}
					}
				}
				
				if(startGame&&room.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU
						&&room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
					
					if(room.getZhuangType()!=2&&room.getZhuangType()!=3||(postdata.containsKey("qzhuang")&&postdata.getInt("qzhuang")==1)){
						
						startGame(room);
						
					}else{// 开始抢庄
						
						if (room.isRobot()&&room.getRobotList().size()>0) {
							AutoThreadNN autoThreadNN = new AutoThreadNN(nnService, roomNo, 5);
							autoThreadNN.start();
						}
						
						if(room.getPlayerMap().containsKey(clientTag)){
							
							if(room.getRoomType()==3){
								
								// 重置抢庄倍数
								for (String uuid : room.getPlayerMap().keySet()) {
									if (!room.getRobotList().contains(uuid)) {
										JSONObject result = new JSONObject();
										result.put("type", 1);
										double score = room.getPlayerMap().get(uuid).getScore();
										result.put("qzTimes", room.qzTimes);
										if(room.getRoomType()==1 || room.getRoomType()==3){
											JSONArray qzTimes = room.getQzTimes(score);
											result.put("qzTimes2", qzTimes);
											System.out.println("score："+score+"，qzTimes："+qzTimes.toString());
										}
										
										SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
										if(clientother!=null){
											clientother.sendEvent("gameUISettingPush_NN", result);
										}
									}
									
								}
							}
							
							// 抢庄阶段
							((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_QIANGZHUANG);
							
							// 抢庄模式
							if(room.getGameCount()>room.getGameIndex()){ 
								
								/*MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 3);
							m.start();*/
								//定时器
								//GameMain.singleTime.deleteTimer(roomNo);
								TimerMsgData tmd=new TimerMsgData();
								tmd.nTimeLimit=GLOBALTIMER[2];
								tmd.nType=18;
								tmd.roomid=roomNo;
								tmd.client=client;
								tmd.data=new JSONObject().element("room_no", roomNo);
								tmd.gid=1;
								tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo), 1, 18);
								GameMain.singleTime.createTimer(tmd);
							}
							
							// 明牌抢庄发牌
							if(room.getZhuangType()==3){ 
								
								// 洗牌
								NiuNiuServer.xiPai(roomNo);
								// 发牌
								NiuNiuServer.faPai(roomNo);
								
								luckyTurning(room);
							}
							
							// 通知玩家
							for (String uuid : room.getPlayerMap().keySet()) {
								UserPacket up = room.getUserPacketMap().get(uuid);
								if(up!=null&&up.getMyPai()[0]>0){
									JSONObject result = new JSONObject();
									if(room.getZhuangType()==3){
										// 设置明牌抢庄的牌组
										((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).saveMingPai();
										int[] mypai = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).getMingPai();
										int[] pais = new int[mypai.length];
										for (int i = 0; i < pais.length-1; i++) {
											pais[i] = mypai[i];
										}
										result.put("myPai",pais);
										result.put("type", 1);
									}else{
										result.put("type", 0);
									}
									result.put("isGameIng",room.getGameIngIndex());
									if (!room.getRobotList().contains(uuid)) {
										
										SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
										if(clientother!=null){
											LogUtil.print(roomNo+" "+room.getPlayerMap().get(uuid).getName()+" 开始抢庄接收参数:"+result);
											clientother.sendEvent("qiangzhuangStartPush_NN", result);
										}
									}
								}
							}
							
							//通知观战玩家
							if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
								for (String string : room.getGzPlayerMap().keySet()) {
									JSONObject resultTogz = new JSONObject();
									if(room.getZhuangType()==3){
										// 明牌抢庄模式观战玩家看不到游戏玩家手牌
										int[] pais = new int[]{0,0,0,0,0};
										resultTogz.put("myPai",pais);
										resultTogz.put("type", 1);
									}else{
										resultTogz.put("type", 0);
									}
									resultTogz.put("isGameIng",room.getGameIngIndex());
									SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
									if(clientother1!=null){
										clientother1.sendEvent("qiangzhuangStartPush_NN", resultTogz);
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
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛gameReady方法消耗时间:"+(end-start));
		}
	}
	
	public void gameReady1(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();
		
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		
		try {
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				String clientTag = Constant.getClientTag(client);
				NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
				
				if(room.getRoomType()==1||room.getRoomType()==3){ // 金币、元宝模式
					
					// 下一局开始时，清除掉线或金币不足的玩家
					List<String> cleanPlayer = nnService.cleanPlayer(room);
					if (cleanPlayer.contains(clientTag)) {
						return;
					}
				}
				
				if(!postdata.containsKey("auto")){
					for (String uuid : room.getPlayerMap().keySet()) {
						if (!room.getRobotList().contains(uuid)) {
							JSONObject result = new JSONObject();
							result.put("users",room.getAllPlayer());
							
							SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
							if(clientother!=null){
								clientother.sendEvent("userInfoPush_NN", result);
							}
						}
					}
				}
				
				// 是否开始游戏
				boolean startGame = false;
				boolean temp = false;
				for (String account : room.getPlayerMap().keySet()) {
					if (!Dto.stringIsNULL(room.getPlayerMap().get(account).getOpenid())) {
						temp = true;
					}
				}
				
				// 准备
				NiuNiuServer.isReady(roomNo, clientTag);
				
				// 两个人以上准备则开启准备倒计时
				if(!postdata.containsKey("auto") && room.getUserIDSet().size()>2 && room.getReadyCount()==2){
					// 开启准备定时器，开始计时
					NNGameRoom game = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
					JSONObject result = new JSONObject();
					result.put("timer", GLOBALTIMER[3]);
					for (String uuid  : game.getUserPacketMap().keySet()) {
						if (!game.getRobotList().contains(uuid)) {
							
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("nnTimerPush_NN", result);
							}
						}
					}
					//定时器
					
					//GameMain.singleTime.deleteTimer(roomNo);
					TimerMsgData tmd=new TimerMsgData();
					tmd.nTimeLimit=GLOBALTIMER[3];
					tmd.nType=17;
					tmd.roomid=roomNo;
					tmd.client=client;
					tmd.data=new JSONObject().element("room_no", roomNo);
					tmd.gid=1;
					tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo), 1, 17);
					GameMain.singleTime.createTimer(tmd);
				}
				
				// 6人场两人以上就可以开始
				if(room.getReadyCount() == room.getPlayerCount() 
						|| (room.getPlayerCount()>=6 && room.getReadyCount()>=2 && room.getReadyCount()==room.getUserPacketMap().size())){ 
					
					startGame = true;
					
					// 是否允许玩家中途加入
					if(!room.isHalfwayin()){
						
						if(room.getGameIndex()==0){
							// 开始游戏后禁止其他玩家加入房间
							//						mjBiz.stopJoin(roomNo);
						}
					}
					
				}else{
					
					// 抢庄完第二次准备不需要通知
					// 自动准备不需要通知
					if(!postdata.containsKey("auto") && !(postdata.containsKey("qzhuang")&&postdata.getInt("qzhuang")==1)){
						
						JSONObject result = new JSONObject();
						result.put("myIndex", room.getPlayerIndex(clientTag));
						result.put("isReady", room.getReadyIndex());
						result.put("game_index", room.getGameIndex()+1);
						
						// 准备状态
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_READY);
						
						// 通知玩家
						for (String uuid : room.getPlayerMap().keySet()) {
							if (!room.getRobotList().contains(uuid)) {
								SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
								if(clientother!=null){
									clientother.sendEvent("playerReadyPush_NN", result);
								}
							}
						}
						// 通知观战玩家
						if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
							for (String string : room.getGzPlayerMap().keySet()) {
								SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
								if(clientother!=null){
									clientother.sendEvent("playerReadyPush_NN", result);
								}
							}
						}
					}
				}
				
				if(startGame&&room.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU
						&&room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
					
					if(room.getZhuangType()!=2&&room.getZhuangType()!=3||(postdata.containsKey("qzhuang")&&postdata.getInt("qzhuang")==1)){
						
						startGame(room);
						
					}else{// 开始抢庄
						
						if (room.isRobot()&&room.getRobotList().size()>0) {
							AutoThreadNN autoThreadNN = new AutoThreadNN(nnService, roomNo, 5);
							autoThreadNN.start();
						}
						
						if(room.getPlayerMap().containsKey(clientTag)){
							
							if(room.getRoomType()==3){
								
								// 重置抢庄倍数
								for (String uuid : room.getPlayerMap().keySet()) {
									if (!room.getRobotList().contains(uuid)) {
										JSONObject result = new JSONObject();
										result.put("type", 1);
										double score = room.getPlayerMap().get(uuid).getScore();
										result.put("qzTimes", room.qzTimes);
										if(room.getRoomType()==1 || room.getRoomType()==3){
											JSONArray qzTimes = room.getQzTimes(score);
											result.put("qzTimes2", qzTimes);
											System.out.println("score："+score+"，qzTimes："+qzTimes.toString());
										}
										
										SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
										if(clientother!=null){
											clientother.sendEvent("gameUISettingPush_NN", result);
										}
									}
									
								}
							}
							
							// 抢庄阶段
							((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_QIANGZHUANG);
							
							// 抢庄模式
							if(room.getGameCount()>room.getGameIndex()){ 
								
								/*MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 3);
							m.start();*/
								//定时器
								//GameMain.singleTime.deleteTimer(roomNo);
								TimerMsgData tmd=new TimerMsgData();
								tmd.nTimeLimit=GLOBALTIMER[2];
								tmd.nType=18;
								tmd.roomid=roomNo;
								tmd.client=client;
								tmd.data=new JSONObject().element("room_no", roomNo);
								tmd.gid=1;
								tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo), 1, 18);
								GameMain.singleTime.createTimer(tmd);
							}
							
							// 明牌抢庄发牌
							if(room.getZhuangType()==3){ 
								
								// 洗牌
								NiuNiuServer.xiPai(roomNo);
								// 发牌
								NiuNiuServer.faPai(roomNo);
								
								luckyTurning(room);
							}
							
							// 通知玩家
							for (String uuid : room.getPlayerMap().keySet()) {
								UserPacket up = room.getUserPacketMap().get(uuid);
								if(up!=null&&up.getMyPai()[0]>0){
									JSONObject result = new JSONObject();
									if(room.getZhuangType()==3){
										// 设置明牌抢庄的牌组
										((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).saveMingPai();
										int[] mypai = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).getMingPai();
										int[] pais = new int[mypai.length];
										for (int i = 0; i < pais.length-1; i++) {
											pais[i] = mypai[i];
										}
										result.put("myPai",pais);
										result.put("type", 1);
									}else{
										result.put("type", 0);
									}
									result.put("isGameIng",room.getGameIngIndex());
									if (!room.getRobotList().contains(uuid)) {
										
										SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
										if(clientother!=null){
											LogUtil.print(roomNo+" "+room.getPlayerMap().get(uuid).getName()+" 开始抢庄接收参数:"+result);
											clientother.sendEvent("qiangzhuangStartPush_NN", result);
										}
									}
								}
							}
							
							//通知观战玩家
							if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
								for (String string : room.getGzPlayerMap().keySet()) {
									JSONObject resultTogz = new JSONObject();
									if(room.getZhuangType()==3){
										// 明牌抢庄模式观战玩家看不到游戏玩家手牌
										int[] pais = new int[]{0,0,0,0,0};
										resultTogz.put("myPai",pais);
										resultTogz.put("type", 1);
									}else{
										resultTogz.put("type", 0);
									}
									resultTogz.put("isGameIng",room.getGameIngIndex());
									SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
									if(clientother1!=null){
										clientother1.sendEvent("qiangzhuangStartPush_NN", resultTogz);
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
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛gameReady1方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 开始游戏
	 * @param room
	 */
	public void startGame(NNGameRoom room) {

		long start = System.currentTimeMillis();
		try {
			//RoomManage.lock(roomNo);
			String roomNo = room.getRoomNo();
			if (!RoomManage.gameRoomMap.containsKey(roomNo)) {
				//RoomManage.unLock(roomNo);
				return;
			}
			// 游戏状态
			((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_XIAZHU);
			
			// 重置准备
			room.setReadyCount(0);
			
			// 重置重新发牌次数
			room.setRestartTime(0);
			
			// 初始化房间信息
			room.initGame();
			
			JSONArray userIds = new JSONArray();
			// 通知玩家
			for (String uuid : room.getPlayerMap().keySet()) {
				
				Playerinfo player = room.getPlayerMap().get(uuid);
				
				if(room.getRoomType()==3||room.getRoomType()==1){ // 元宝模式
					
					// 是否扣服务费
					boolean isFee = true;
					
					// 观战玩家不能扣服务费
					if(room.getUserPacketMap().get(uuid)!=null){
						
						if((room.getZhuangType()==2||room.getZhuangType()==3)&&room.getUserPacketMap().get(uuid).getStatus()==-1){
							
							isFee = false;
							logger.info("观战玩家不能扣服务费");
						}
					}
					
					if(isFee){
						
						userIds.add(player.getId());
						
						// 第一局开始需要扣费提示
						//					if(player.isTipMsg()){
						//						if(clientother!=null){
						//							JSONObject obj = new JSONObject();
						//							// type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
						//							obj.put("type", 0);
						//							obj.put("msg", "本局扣服务费："+room.getFee());
						//							clientother.sendEvent("tipMsgPush_NN", obj);
						//						}
						//						player.setTipMsg(false);
						//					}
						
						// 玩家扣服务费
						if(room.getFee()>0){
							player.setScore(player.getScore() - room.getFee());
						}
					}
				}
				
				JSONObject result = new JSONObject();
				
				result.put("baseNum", room.getBaseNum());
				if(room.getRoomType()==3){
					result.put("baseNum2", room.getBaseNumTimes(player.getScore()));
				}
				result.put("myIndex", room.getPlayerIndex(uuid));
				result.put("zhuang", room.getPlayerIndex(room.getZhuang()));
				result.put("game_index", room.getGameIndex()+1);
				// 获取正在游戏中的玩家下标
				result.put("isGameIng",room.getGameIngIndex());
				// 闲家推注
				if (room.isTuizhu()) {
					// 不是庄家
					if (!uuid.equals(room.getZhuang())) {
						UserPacket userPacket = room.getUserPacketMap().get(uuid);
						userPacket.setBaseNumTuiZhu(room.getBaseNum());
						// 上局没有闲家推注且不是庄家赢了
						if (!userPacket.isTuiZhuLast&&
								userPacket.getIsBankerLast()==NiuNiu.USERPACKER_LAST_NO&&
								userPacket.getWinLast()==NiuNiu.USERPACKER_LAST_YES) {
							int value = 0;
							if (userPacket.getTypeLast()>=0&&userPacket.getTypeLast()<=6) {// 没牛到牛6 闲家推注2倍
								value = (int) (userPacket.getScoreLast()*2);
							}else if (userPacket.getTypeLast()==7) {// 牛7 闲家推注3倍
								value = (int) (userPacket.getScoreLast()*3);
							}else if (userPacket.getTypeLast()==8) {// 牛8 闲家推注4倍
								value = (int) (userPacket.getScoreLast()*4);
							}else if (userPacket.getTypeLast()==9) {// 牛9 闲家推注5倍
								value = (int) (userPacket.getScoreLast()*5);
							}else if (userPacket.getTypeLast()==10) {// 牛牛 闲家推注6倍
								value = (int) (userPacket.getScoreLast()*6);
							}else {// 特殊牌型闲家推注9倍
								value = (int) (userPacket.getScoreLast()*9);
							}
							String baseNum = room.getBaseNum();
							JSONArray fromObject = JSONArray.fromObject(baseNum);
							JSONObject obj = new JSONObject();
							obj.put("name", value);
							obj.put("val", value);
							fromObject.add(obj);
							result.put("baseNum", fromObject.toString());
							userPacket.setBaseNumTuiZhu(fromObject.toString());
							userPacket.setTuiZhuLast(true);
						}else {
							userPacket.setTuiZhuLast(false);
						}
					}
				}
				if (!room.getRobotList().contains(uuid)) {
					
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
					if(clientother!=null){
						clientother.sendEvent("gameStartPush_NN", result);
					}
				}
				
			}
			// 通知观战玩家
			if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
				for (String uuid1 : room.getGzPlayerMap().keySet()) {
					JSONObject result = new JSONObject();
					
					result.put("baseNum", room.getBaseNum());
					result.put("myIndex", NiuNiu.USERPACKER_STATUS_GUANZHAN);
					result.put("zhuang", room.getPlayerIndex(room.getZhuang()));
					result.put("game_index", room.getGameIndex()+1);
					// 获取正在游戏中的玩家下标
					result.put("isGameIng",room.getGameIngIndex());
					SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(uuid1).getUuid());
					if(clientother!=null){
						clientother.sendEvent("gameStartPush_NN", result);
					}
				}
			}
			
			
			// 通比模式（没有玩家下注阶段，默认底注）
			if(room.getZhuangType()==5){
				
				//double money = room.getScore();
				for(String uuid:room.getPlayerMap().keySet()){
					int index = room.getPlayerMap().get(uuid).getMyIndex();
					JSONObject obj = new JSONObject();
					obj.put("room_no", room.getRoomNo());
					obj.put("num", index);
					obj.put("place", index);
					obj.put("money", 1); // 单倍底分
					obj.put("auto", 1);
					SocketIOClient client=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
					if(client!=null){
						new NNGameEventDeal().gameXiaZhu(client, obj);
					}else {
						obj.put("uuid", uuid);
						SocketIOClient client2 = new SocketIOClient() {
							
							@Override
							public void set(String arg0, Object arg1) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public boolean has(String arg0) {
								// TODO Auto-generated method stub
								return false;
							}
							
							@Override
							public <T> T get(String arg0) {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public void del(String arg0) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void sendEvent(String arg0, Object... arg1) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void send(Packet arg0) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void disconnect() {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void sendEvent(String arg0, AckCallback<?> arg1, Object... arg2) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void send(Packet arg0, AckCallback<?> arg1) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void leaveRoom(String arg0) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public void joinRoom(String arg0) {
								// TODO Auto-generated method stub
								
							}
							
							@Override
							public boolean isChannelOpen() {
								// TODO Auto-generated method stub
								return false;
							}
							
							@Override
							public Transport getTransport() {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public UUID getSessionId() {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public SocketAddress getRemoteAddress() {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public SocketIONamespace getNamespace() {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public HandshakeData getHandshakeData() {
								// TODO Auto-generated method stub
								return null;
							}
							
							@Override
							public Set<String> getAllRooms() {
								// TODO Auto-generated method stub
								return null;
							}
						};
						new NNGameEventDeal().gameXiaZhu(client2, obj);
					}
				}
				
				// 进入结算
				if(RoomManage.gameRoomMap.containsKey(roomNo)
						&&((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
					
					LogUtil.print("通比模式，进入结算，当前游戏状态："+room.getGameStatus());
					
					// 结算
					nnService.jieSuan(roomNo);
				}
				
			}else{ // 庄闲模式
				
				if (room.isRobot()&&room.getRobotList().size()>0) {
					
					AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 1);
					a.start();
				}
				
				// 开启下注定时器，开始计时
				/*MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 1);
			m.start();*/
				//定时器
				//GameMain.singleTime.deleteTimer(roomNo);
				TimerMsgData tmd=new TimerMsgData();
				tmd.nTimeLimit=GLOBALTIMER[0];
				tmd.nType=19;
				tmd.roomid=roomNo;
				tmd.client=null;
				tmd.data=new JSONObject().element("room_no", roomNo);
				tmd.gid=1;
				tmd.gmd= new Messages(null, new JSONObject().element("room_no", roomNo), 1, 19);
				GameMain.singleTime.createTimer(tmd);
			}
			
			// 玩家扣服务费
			if(room.getFee()>0){
				
				if(room.getRoomType()==1){ // 金币模式
					mjBiz.pump(userIds, roomNo, 1, room.getFee(), "coins");
				}else if(room.getRoomType()==3){ // 元宝模式
					//mjBiz.pump(userIds, roomNo, 1, room.getFee(), "yuanbao");
					GameMain.sqlQueue.addSqlTask(new SqlModel(4, userIds, roomNo, 1, room.getFee(), "yuanbao"));
				}
				
				// 刷新玩家余额
				for (String uuid : room.getPlayerMap().keySet()) {
					if (!room.getRobotList().contains(uuid)) {
						JSONObject result = new JSONObject();
						result.put("users",room.getAllPlayer());
						
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							clientother.sendEvent("userInfoPush_NN", result);
						}
					}
				}
			}
			
			long end = System.currentTimeMillis();
			if (end-start>10) {
				LogUtil.print(roomNo+"牛牛startGame方法消耗时间:"+(end-start));
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			//RoomManage.unLock(roomNo);
		}

	}


	/**
	 * 游戏事件
	 * @param client
	 * @param data
	 */
	public void gameEvent(SocketIOClient client, Object data) {

		long start = System.currentTimeMillis();
		String roomNo=client.get(Constant.ROOM_KEY_NN);
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				if (((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN) {
					RoomManage.unLock(roomNo);
					return;
				}
				if (((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().containsKey(Constant.getClientTag(client))) {
					// 玩家亮牌
					nnService.showPai(roomNo, Constant.getClientTag(client));
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛gameEvent方法消耗时间:"+(end-start));
		}
	}

	/**
	 * 解散房间
	 * @param client
	 * @param data
	 */
	public void closeRoom(SocketIOClient client, Object data) {

		long start = System.currentTimeMillis();

		// 房间号
		String roomNo=client.get(Constant.ROOM_KEY_NN);
		String clientTag = Constant.getClientTag(client);
		if(RoomManage.gameRoomMap.containsKey(roomNo)&&((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(clientTag)!=null){

			JSONObject obj= JSONObject.fromObject(data);
			NNGameRoom game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
			boolean isClose = false;
			if(obj.getInt("type")==1){//同意解散房间

				if(obj.containsKey("platform")){
					String platform = obj.getString("platform");
					int closeTime = 0;
					if(CLOSETIME.containsKey(platform)){
						closeTime = CLOSETIME.getInt(platform);
					}else{
						closeTime = CLOSETIME.getInt("default");
					}
					if(game.getCloseTime()==0){
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setCloseTime(closeTime);
					}

					// 开启解散定时器，开始计时
					/*MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 4);
					m.start();*/
					//定时器
					//GameMain.singleTime.deleteTimer(roomNo);
					TimerMsgData tmd=new TimerMsgData();
					tmd.nTimeLimit=180;
					tmd.nType=22;
					tmd.roomid=roomNo;
					tmd.client=client;
					tmd.data=new JSONObject().element("room_no", roomNo);
					tmd.gid=1;
					tmd.gmd= new Messages(client, new JSONObject().element("room_no", roomNo), 1, 22);
					GameMain.singleTime.createTimer(tmd);

				}
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
			for(String uuid:game.getPlayerMap().keySet()){

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
				result.put("jiesanTime", game.getCloseTime());
				array.add(result);
			}

			if(agree+refuse==game.getPlayerMap().size()){

				JSONObject result = new JSONObject();
				if(game.getPlayerCount()!=game.getReadyCount()&&game.getUserPacketMap().get(game.getFangzhu()).isCloseRoom==1
						&&game.getFangzhu().equals(clientTag)){ //游戏未开始阶段，房主可以直接解散房间

					result.put("type", 1); //解散房间
					result.put("result", 2);//房主解散
					for(String uuid:game.getPlayerMap().keySet()){

						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_NN", result);
						}
					}

					// 通知观战玩家
					if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
						for (String string : game.getGzPlayerMap().keySet()) {
							SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
							if(askclient!=null){
								askclient.sendEvent("exitRoomPush_NN", result);
							}
						}
					}

					// 获取房间信息
					JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
					if(room!=null&&room.containsKey("id")){
						String sql = "update za_gamerooms set status=? where id=?";
						//DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
						GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{-1, room.getLong("id")}, SqlModel.EXECUTEUPDATEBYSQL));
						if(room.getInt("roomtype") == 2){
							//代开房间房间解散，新建房间
							GlobalService.insertGameRoom(roomNo);
						}
					}

					// 清除房间缓存数据
					RoomManage.gameRoomMap.remove(roomNo);

				}else if(refuse==0){ //所有人都同意退出房间

					// 获取房间信息
					JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
					if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getGameIndex()!=0 || room.getInt("game_index")!=0){

						/**
						 * 20171027 lhp
						 * 申请解散要强制结算
						 */
						JSONArray jiesuanArray = nnService.balance(game);

						result.put("type", 2); 
						result.put("jiesuanData", jiesuanArray);
						for(String uuid:game.getPlayerMap().keySet()){
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("gameActionPush_NN", result);
							}
						}

						// 通知观战玩家
						if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
							for (String string : game.getGzPlayerMap().keySet()) {
								SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
								if(askclient!=null){
									askclient.sendEvent("gameActionPush_NN", result);
								}
							}
						}

						/**
						 * 中途解散添加战绩  2018/02/10
						 */
						//						mjBiz.updateGamelogs(roomNo, 1, jiesuanArray);

					}else{// 解散房间
						result.put("type", 1); 
						result.put("result", 1);
						for(String uuid:game.getPlayerMap().keySet()){
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("exitRoomPush_NN", result);
							}
						}

						// 通知观战玩家
						if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
							for (String string : game.getGzPlayerMap().keySet()) {
								SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_NN", result);
								}
							}
						}
					}

					if(room!=null&&room.containsKey("id")){
						String sql = "update za_gamerooms set status=? where id=?";
						//DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
						GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{-1, room.getLong("id")}, SqlModel.EXECUTEUPDATEBYSQL));
						if(room.getInt("roomtype") == 2){
							//代开房间房间解散，新建房间
							GlobalService.insertGameRoom(roomNo);
						}
					}

					// 清除房间缓存数据
					RoomManage.gameRoomMap.remove(roomNo);

				}else{ // 有人拒绝退出

					// 重置解散倒计时为空
					((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setCloseTime(0);

					result.put("type", 1); //解散房间
					result.put("result", 0);
					result.put("user", names.toArray());
					for(String uuid:game.getPlayerMap().keySet()){
						// 重置准备状态
						game.getUserPacketMap().get(uuid).isCloseRoom=0;
						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_NN", result);
						}
					}
				}

			}else{ 

				if(isClose&&refuse==0){

					// 获取房间信息
					JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
					JSONObject result = new JSONObject();
					if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getGameIndex()!=0 || room.getInt("game_index")!=0){

						/**
						 * 20171027 lhp
						 * 申请解散要强制结算
						 */
						JSONArray jiesuanArray = nnService.balance(game);

						result.put("type", 2); 
						result.put("jiesuanData", jiesuanArray);
						for(String uuid:game.getPlayerMap().keySet()){
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("gameActionPush_NN", result);
							}
						}

						// 通知观战玩家
						if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
							for (String string : game.getGzPlayerMap().keySet()) {
								SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
								if(askclient!=null){
									askclient.sendEvent("gameActionPush_NN", result);
								}
							}
						}

					}else{// 解散房间
						result.put("type", 1); 
						result.put("result", 1);
						for(String uuid:game.getPlayerMap().keySet()){
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
							if(askclient!=null){
								askclient.sendEvent("exitRoomPush_NN", result);
							}
						}

						// 通知观战玩家
						if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
							for (String string : game.getGzPlayerMap().keySet()) {
								SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_NN", result);
								}
							}
						}
					}

					if(room!=null&&room.containsKey("id")){
						String sql = "update za_gamerooms set status=? where id=?";
						//DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
						GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{-1, room.getLong("id")}, SqlModel.EXECUTEUPDATEBYSQL));
						if(room.getInt("roomtype") == 2){
							//代开房间房间解散，新建房间
							GlobalService.insertGameRoom(roomNo);
						}
					}

					// 清除房间缓存数据
					RoomManage.gameRoomMap.remove(roomNo);

				}else if(refuse>0){ //有玩家拒绝，则停止继续询问	

					// 重置解散倒计时为空
					((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setCloseTime(0);

					JSONObject result = new JSONObject();
					result.put("type", 1); //解散房间
					result.put("result", 0);
					result.put("user", names.toArray());
					for(String uuid:game.getPlayerMap().keySet()){
						// 重置准备状态
						game.getUserPacketMap().get(uuid).isCloseRoom=0;
						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_NN", result);
						}
					}
				}else{ //通知其他人退出申请

					for(String uuid:game.getPlayerMap().keySet()){

						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("closeRoomPush_NN", array);
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
				askclient.sendEvent("exitRoomPush_NN", result);
			}
		}

		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛closeRoom方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 退出房间
	 * @param client
	 * @param data
	 */
	public void exitRoom(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		// 房间号
		String roomNo=client.get(Constant.ROOM_KEY_NN);
		JSONObject userinfo = client.get("userinfo");
		try {
			RoomManage.lock(roomNo);
			// 观战玩家退出游戏
			if (((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null) {
				if (((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).isGuanzhan()) {
					if (!Dto.isObjNull(userinfo)) {
						exitRoomGz(client, Constant.getClientTag(client), roomNo);
					}
				}else if (!Dto.isObjNull(userinfo)) {
					exitRoom(Constant.getClientTag(client), roomNo, userinfo.getLong("id"));
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛exitRoom方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 玩家退出玩家房间
	 * @param uuid
	 * @param roomNo
	 * @param userId
	 */
	public void exitRoom(String uuid, String roomNo, long userId) {
		long start = System.currentTimeMillis();

		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			NNGameRoom game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);

			boolean isRealExit = true;

			if (game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN||
					game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU||
					game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG) {
				Integer[] gameIngIndex = game.getGameIngIndex();
				for (Integer integer : gameIngIndex) {
					if (game.getPlayerMap().get(uuid)!=null&&game.getPlayerMap().get(uuid).getMyIndex()==integer) {
						isRealExit = false;
						JSONObject result = new JSONObject();
						result.put("type", 2); //退出房间
						result.put("code", 0);
						result.put("msg", "您正在游戏中无法退出游戏");
						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_NN", result);
						}
						break;
					}
				}
			}

			// 金币场庄家退出游戏
			else if((game.getRoomType()==1||game.getRoomType()==3)&&game.getPlayerMap().size()>1){

				// 准备或者牌局结束阶段庄家退出
				if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY || game.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI){

					if(game.getZhuangType()!=5&&game.getZhuangType()!=2&&game.getZhuangType()!=3){ // 通比模式不进行换庄

						// 换庄
						if(game.getZhuang().equals(uuid)){

							String oldZhuang = game.getZhuang();
							String zhuang = game.getNextPlayer(oldZhuang);
							if(!oldZhuang.equals(zhuang)){

								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setFangzhu(zhuang);
								JSONObject result = new JSONObject();
								result.put("zhuang", game.getPlayerIndex(zhuang));
								for (String uid  : game.getUserPacketMap().keySet()) {
									if (!game.getRobotList().contains(uid)) {
										// 重置庄家信息
										SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
										if(clientother!=null){
											clientother.sendEvent("huanZhuangPush_NN", result);
										}
									}
								}
							}
						}
					}

				}else if(game.getUserPacketMap().get(uuid)!=null && 
						(game.getUserPacketMap().get(uuid).getStatus()<=NiuNiu.USERPACKER_STATUS_CHUSHI || 
						game.getUserPacketMap().get(uuid).getMyPai()[0]==0)){ // 玩家处于观战中，随时可以退出
					System.out.println("出门左转，请便！");

				}else{

					isRealExit = false;

					JSONObject result = new JSONObject();
					result.put("type", 2); //退出房间
					result.put("code", 0);
					result.put("msg", "您正在游戏中无法退出游戏");
					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
					if(askclient!=null){
						askclient.sendEvent("exitRoomPush_NN", result);
					}
				}
			}

			if(isRealExit){

				// 玩家退出房间
				exit(game, uuid, userId);

				// 所有人都已准备则开始游戏
				int count = game.getUserPacketMap().size();
				if(game.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU&&game.getGameStatus()!=NiuNiu.GAMESTATUS_QIANGZHUANG&&
						game.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN&&
						count>1 && game.getReadyIndex().length==count){
					//if(game.getTimeLeft()>=3){
					if(game.getZhuangType()==2||game.getZhuangType()==3){
						// 触发抢庄方法
						game = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
						if (game!=null) {
							for (String uid  : game.getUserPacketMap().keySet()) {
								int ready = game.getUserPacketMap().get(uid).getIsReady();
								if(ready==1){
									
									SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
									if(clientother!=null){
										JSONObject data = new JSONObject();
										data.put("room_no", roomNo);
										data.put("auto", 1);
										gameReady1(clientother, data);
									}
									break;
								}
							}
						}
					}else{
						// 触发开始游戏
						startGame(game);
					}
					//}
				}
			}
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print("玩家"+userId+"退出房间:"+roomNo+"牛牛exitRoom()方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 退出房间
	 * @param game
	 * @param uuid
	 * @param userId
	 */
	public void exit(NNGameRoom game, String uuid, long userId){

		long start = System.currentTimeMillis();
		String roomNo = game.getRoomNo();
		if(game.getPlayerMap().get(uuid)!=null){

			JSONObject result = new JSONObject();
			result.put("type", 2); //退出房间
			result.put("index", game.getPlayerIndex(uuid));
			for(String uuid1:game.getPlayerMap().keySet()){
				if(!game.getRobotList().contains(uuid1)){

					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
					if(askclient!=null){
						askclient.sendEvent("exitRoomPush_NN", result);
					}
				}
			}

			if (game.isRobot()) {
				//每有一个用户退出则新加入一个机器人
				//				String sql = "select account from za_users where openid='0' and status=0 limit ?,1";
				//				JSONObject jsonObject = DBUtil.getObjectBySQL(sql, new Object[]{1});
				//				List<String> list = game.getRobotList();
				//				list.add(jsonObject.getString("account"));
				//				game.setRobotList(list);
				//				sql = "update za_users set status=1,coins=? where account=?";
				//				DBUtil.executeUpdateBySQL(sql, new Object[]{2000+new Random().nextInt(2000),jsonObject.getString("account")});
			}
		}

		// 清除房间用户数据
		((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().remove(uuid);
		((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().remove(uuid);

		// 获取房间信息
		/*JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
		if(room!=null&&room.containsKey("id")){

			String userIndex = null;

			if(userId==room.getLong("user_id0")){
				userIndex = "user_id0";
			}
			if(userId==room.getLong("user_id1")){
				userIndex = "user_id1";
			}
			if(userId==room.getLong("user_id2")){
				userIndex = "user_id2";
			}
			if(userId==room.getLong("user_id3")){
				userIndex = "user_id3";
			}
			if(userId==room.getLong("user_id4")){
				userIndex = "user_id4";
			}
			if(userId==room.getLong("user_id5")){
				userIndex = "user_id5";
			}
			if(userId==room.getLong("user_id6")){
				userIndex = "user_id6";
			}
			if(userId==room.getLong("user_id7")){
				userIndex = "user_id7";
			}
			if(userId==room.getLong("user_id8")){
				userIndex = "user_id8";
			}
			if(userId==room.getLong("user_id9")){
				userIndex = "user_id9";
			}

			if(userIndex!=null){

				String sql = "update za_gamerooms set "+userIndex+"=? where status>=0 and id=?";
				DBUtil.executeUpdateBySQL(sql, new Object[]{0, room.getLong("id")});
			}

		}*/
		RoomManage roomManage = new RoomManage();
		roomManage.playerExit(uuid, userId, roomNo);
		((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserIDSet().remove(userId);

		//房间内全是机器人则清除房间
		boolean clearRoom = true;
		for (String uid : ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().keySet()) {
			if (!((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getRobotList().contains(uid)) {
				clearRoom = false;
				break;
			}
		}

		// 准备人数少于两人取消定时器
		int count1 = 0;
		for (String uid:game.getUserPacketMap().keySet()) {
			int ready = game.getUserPacketMap().get(uid).getIsReady();
			if(ready!=0&&ready!=10){
				count1++;
			}
		}
		if (count1<2) {
			JSONObject result = new JSONObject();
			result.put("timer", 0);
			for (String uuid1  : game.getUserPacketMap().keySet()) {
				if (!game.getRobotList().contains(uuid1)) {
					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
					if(askclient!=null){
						askclient.sendEvent("nnTimerPush_NN", result);
					}
				}
			}
		}

		// 金币场没人的房间直接清除
		if((game.getRoomType()==1||game.getRoomType()==3)&&(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size()==0||clearRoom)){
			//重置状态
			RoomManage.gameRoomMap.remove(roomNo);
			String sql = "update za_gamerooms set status=? where room_no=?";
			//DBUtil.executeUpdateBySQL(sql, new Object[]{-1, roomNo});
			GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{-1, roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
			//			LogUtil.print("金币场没人的房间直接清除："+roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print("玩家"+userId+"退出房间:"+roomNo+"牛牛exit方法消耗时间:"+(end-start));
		}
	}


	public void reconnectGame(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		JSONObject obj= JSONObject.fromObject(data);
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
		//JSONObject userinfo=mjBiz.getUserInfoByAccount(account);
		JSONObject userinfo = new JSONObject();
		/*if (UserInfoCache.userInfoMap.containsKey(account)) {
			userinfo = UserInfoCache.userInfoMap.get(account);
		}else {
			userinfo=mjBiz.getUserInfoByAccount(account);
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
				
				NNGameRoom game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
				Playerinfo player = null;
				for(String uuid:game.getPlayerMap().keySet()){
					if(game.getPlayerMap().get(uuid)!=null&&game.getPlayerMap().get(uuid).getAccount().equals(account)){
						player = game.getPlayerMap().get(uuid);
						player.setStatus(Constant.ONLINE_STATUS_YES);
						player.setUuid(client.getSessionId());
					}
				}
				
				if (player==null) {
					LogUtil.print("重连玩家"+account+"------当前房间内玩家"+game.getPlayerMap().keySet());
				}
				
				if(player!=null){
					
					// 设置会话信息
					client.set(Constant.ROOM_KEY_NN, roomNo);
					client.set("userinfo", userinfo);
					LogUtil.print(account+":重连成功！");
					
					// 返回给玩家当前牌局信息（基础信息）
					JSONObject result=new JSONObject();
					
					if (game.getGameStatus()!=NiuNiu.GAMESTATUS_LIANGPAI&&game.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN) {
						
						result.put("game_index",game.getGameIndex()+1);
					}else {
						result.put("game_index",game.getGameIndex());
						
					}
					// 自动开始游戏倒计时
					if(game.getReadyCount()>=2){
						result.put("timer", game.getTimeLeft());
					}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN 
							//						|| game.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI
							|| game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
						if (game.getTimeLeft()==0&&game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN) {
							result.put("timer", NNGameEventDeal.GLOBALTIMER[1]);
						}else {
							result.put("timer", game.getTimeLeft());
						}
					}else{
						result.put("timer", 0);
					}
					
					if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY || game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){ // 开局准备阶段
						
						result.put("type", 0); //游戏准备时断线重连
						
					}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){ // 玩家下注阶段
						
						result.put("type", 1);
						if (game.getPlayerMap().get(game.getZhuang())!=null) {
							result.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
						}
						result.put("game_index", game.getGameIndex()+1);
						result.put("playerMoney", game.getMoneyPlace());// 获取玩家下注分数
						result.put("myMoney", game.getplaceArrayNums(player.getMyIndex()));
						// 所有玩家下注记录
						result.put("placeArray", game.getPlaceArray());
						
					}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN || game.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI){ // 结算阶段
						
						if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN){
							result.put("type", 2);
							result.put("showPai", game.getChuPaiIndex());
						}else{
							result.put("type", 3);
						}
						if (game.getPlayerMap().get(game.getZhuang())!=null) {
							result.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
						}
						result.put("playerMoney", game.getMoneyPlace());// 获取玩家下注分数
						// 所有玩家下注记录
						result.put("placeArray", game.getPlaceArray());
						
						// 庄家通杀
						boolean tongSha = true;
						// 庄家通赔
						boolean tongPei = true;
						for(String uuid:game.getPlayerMap().keySet()){
							
							UserPacket up = game.getUserPacketMap().get(uuid);
							
							// 根据闲家的输赢判断庄家是否通杀或是通赔
							if(!uuid.equals(game.getZhuang())){
								if(up.isWin()){
									tongSha = false;
								}else{
									tongPei = false;
								}
							}
						}
						
						// 各个玩家信息
						JSONArray array = new JSONArray();
						// 通知玩家
						for(String uuid:game.getPlayerMap().keySet()){
							UserPacket up = game.getUserPacketMap().get(uuid);
							if(up!=null&&up.getMyPai()[0]>0){
								JSONObject user = new JSONObject();
								user.put("account", game.getPlayerMap().get(uuid).getAccount());
								user.put("name", game.getPlayerMap().get(uuid).getName());
								user.put("headimg", game.getPlayerMap().get(uuid).getHeadimg());
								if(uuid.equals(game.getZhuang())){
									user.put("zhuang", 1);
								}else{
									user.put("zhuang", 0);
								}
								user.put("myIndex", game.getPlayerIndex(uuid));
								user.put("myPai", up.getSortPai());
								user.put("mingPai", up.getMingPai());
								user.put("result", up.type);
								user.put("ratio", up.getRatio(game)); // 倍率
								user.put("score", up.getScore());
								user.put("totalScore", game.getPlayerMap().get(uuid).getScore());
								user.put("myMoney", game.getplaceArrayNums(game.getPlayerIndex(uuid)));
								int win = 0;
								if(!uuid.equals(game.getZhuang())){
									
									if(up.isWin()){
										win=1;
									}
								}else{
									
									int totalScore = 0;
									for(String key:game.getPlayerMap().keySet()){
										if(!key.equals(game.getZhuang())){
											totalScore -= game.getUserPacketMap().get(key).getScore();
										}
									}
									if(totalScore>=0){
										win=1;
									}
								}
								user.put("win", win);
								// 庄通杀、通赔
								int zhuangTongsha = 0;
								if(tongSha){
									zhuangTongsha = 1;
								}else if(tongPei){
									zhuangTongsha = -1;
								}
								user.put("zhuangTongsha", zhuangTongsha);
								
								array.add(user);
							}
						}
						result.put("users", array);
					}	
					
					if(game.getZhuangType()==3){
						result.put("qzType", 1);
					}
					
					result.put("baseNum", game.getBaseNum());
					if (game.isTuizhu()) {
						UserPacket userPacket = game.getUserPacketMap().get(player.getAccount());
						if (userPacket.isTuiZhuLast) {
							result.put("baseNum", game.getUserPacketMap().get(player.getAccount()).getBaseNumTuiZhu());
						}
					}
					if(game.getRoomType()==3){
						result.put("baseNum2", game.getBaseNumTimes(player.getScore()));
					}
					result.put("gametype", game.getGameType());
					result.put("jiesan", 0);
					//判断当前是否是在申请解散房间阶段
					if(game.getCloseTime()>0){
						boolean isJieSan = true;
						JSONArray jiesans = new JSONArray();
						for(String uuid:game.getPlayerMap().keySet()){
							if(game.getPlayerMap().get(uuid)!=null){
								JSONObject jiesan = new JSONObject();
								jiesan.put("name", game.getPlayerMap().get(uuid).getName());
								jiesan.put("index", game.getPlayerMap().get(uuid).getMyIndex());
								if(game.getUserPacketMap().get(uuid).isCloseRoom==1){
									jiesan.put("result", 1);
								}else if(game.getUserPacketMap().get(uuid).isCloseRoom==-1){
									isJieSan = false;
									break;
								}else{
									jiesan.put("result", 0);
								}
								jiesan.put("jiesanTime", game.getCloseTime());
								jiesans.add(jiesan);
							}
						}
						if(isJieSan){
							
							result.put("jiesan", 1);
							result.put("jiesanData", jiesans);
						}
					}
					
					/**
					 * 20170921 lhp
					 * 明牌抢庄
					 */
					if(game.getZhuangType()==3&&game.getUserPacketMap().get(clientTag).getStatus()!=NiuNiu.USERPACKER_STATUS_LIANGPAI){
						int[] mypai = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(clientTag).getMingPai();
						int[] pais = new int[5];
						if(mypai!=null){
							for (int i = 0; i < pais.length-1; i++) {
								pais[i] = mypai[i];
							}
						}
						result.put("myPai",pais);
					}
					
					JSONObject roomData=new JSONObject();
					roomData.put("room_no", roomNo);
					if (game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN) {
						JSONArray array = new JSONArray();
						
						for(String uuid : game.getPlayerMap().keySet()){
							
							Playerinfo player1 = game.getPlayerMap().get(uuid);
							if(player1!=null){
								
								UserPacket up = game.getUserPacketMap().get(uuid);
								JSONObject obj1 = new JSONObject();
								obj1.put("account", player1.getAccount());
								obj1.put("name", player1.getName());
								obj1.put("headimg", player1.getRealHeadimg());
								obj1.put("sex", player1.getSex());
								obj1.put("ip", player1.getIp());
								obj1.put("vip", player1.getVip());
								obj1.put("location", player1.getLocation());
								obj1.put("area", player1.getArea());
								BigDecimal b1 = new BigDecimal(Double.toString(up.getScore()));
								BigDecimal b2 = new BigDecimal(Double.toString(player1.getScore()));
								obj1.put("score", b2.subtract(b1).doubleValue());
								obj1.put("index", player1.getMyIndex());
								obj1.put("status", player1.getStatus());
								obj1.put("introduction", player1.getSignature());
								if(up.getStatus()==-1 || (up.getMyPai().length>0&&up.getMyPai()[0]==0)){ // 判断玩家是否是中途加入
									obj1.put("readyStatus", -1);
								}else{
									obj1.put("readyStatus", up.getStatus());
								}
								array.add(obj1);
							}
						}
						roomData.put("users",array);
					}else {
						roomData.put("users",game.getAllPlayer());//告诉他原先加入的玩家
					}
					roomData.put("myIndex",player.getMyIndex());
					roomData.put("isReady",game.getReadyIndex());
					
					// 抢庄模式
					if((game.getZhuangType()==2 || game.getZhuangType()==3) 
							&&game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
						
						roomData.put("zhuang",-1);
						roomData.put("qiangzhuang", game.getPlayerIsReady());
						// 倒计时
						roomData.put("qztimer", game.getTimeLeft());
						//					boolean qzReady = true;
						//					for (String uuid :game.getUserPacketMap().keySet()) {
						//						int ready = game.getUserPacketMap().get(uuid).getIsReady();
						//						if(ready==0){
						//							qzReady = false;
						//							break;
						//						}
						//					}
						//					if(qzReady){
						//					}
						// 判断当前是否是抢庄阶段
						result.put("qzReady", 1);
						result.put("zhuang",-1);
						
					}else{
						roomData.put("zhuang",game.getPlayerIndex(game.getZhuang()));
					}
					
					// 抢庄加倍
					if(game.getZhuangType()==2 || game.getZhuangType()==3){
						
						if(game.getGameStatus()!=NiuNiu.GAMESTATUS_READY){
							
							int qzScore = 0;
							// 将确定下来的倍数回传给玩家
							if (game.getUserPacketMap().get(game.getZhuang())!=null) {
								qzScore = game.getUserPacketMap().get(game.getZhuang()).qzTimes;
							}
							if(qzScore<1){
								qzScore = 1;
							}
							roomData.put("qzScore", qzScore);
						}
						
						// 获取玩家最大下注倍数
						if(game.getRoomType()==1 || game.getRoomType()==3){
							roomData.put("qzTimes2", game.getQzTimes(player.getScore()));
						}
					}
					
					result.put("roomData", roomData);
					// 获取正在游戏中的玩家下标
					result.put("isGameIng",game.getGameIngIndex());
					
					// 获取房间信息
					/*JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
					//				if(!Dto.isObjNull(room)
					//						&&(room.getInt("roomtype")==0||room.getInt("roomtype")==2)
					//						&&room.getInt("game_count")==room.getInt("game_index")){ //房间局数已用完
					//					result.put("isOver", 1);
					//				}else{
					//					result.put("isOver", 0);
					//				}
					if(!Dto.isObjNull(room)){ //房间局数已用完
						int isDaikai = 0;
						if (room.getInt("roomtype")==2) {
							for (String string : game.getPlayerMap().keySet()) {
								Playerinfo playerinfo = game.getPlayerMap().get(string);
								if (playerinfo.getId()==room.getLong("user_id0")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id0")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id1")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id2")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id3")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id4")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id5")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id6")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id7")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id8")) {
									isDaikai = 1;
								}
								if (playerinfo.getId()==room.getLong("user_id9")) {
									isDaikai = 1;
								}
							}
						}
						if (room.getInt("roomtype")==2) {
							if (isDaikai==0) {
								result.put("isOver", 1);
							}else {
								result.put("isOver", 0);
							}
						}else if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
								&&room.getInt("game_count")==room.getInt("game_index")){
							result.put("isOver", 1);
						}else{
							result.put("isOver", 0);
						}
					}else{
						room = mjBiz.getRoomInfoByRno1(roomNo);
						if(!Dto.isObjNull(room)){
							if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
									&&room.getInt("game_count")==room.getInt("game_index")){ //房间局数已用完
								result.put("isOver", 1);
							}
						}
					}*/
					result.put("isOver", 0);
					LogUtil.print("断线重连返回="+result.toString());
					client.sendEvent("reconnectGamePush_NN", result);
				}else {
					JSONObject result = new JSONObject();
					result.put("type", 999); //玩家还未创建房间就已经掉线
					
					client.sendEvent("reconnectGamePush_NN", result);
				}
				
				// 观战玩家重连
				if (game.isGuanzhan()&&player==null) {
					gzReconnectGame(client, data);
					RoomManage.unLock(roomNo);
					return;
				}
				
				//通知其他人用户重连
				for(String uuid:game.getPlayerMap().keySet()){
					if (!game.getRobotList().contains(uuid)) {
						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							JSONObject cl = new JSONObject();
							cl.put("index", ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerIndex(clientTag));
							askclient.sendEvent("userReconnectPush_NN", cl);
						}
					}
				}
				
			}else{
				
				JSONObject result = new JSONObject();
				result.put("type", 999); //玩家还未创建房间就已经掉线
				
				// 获取房间信息
				/*JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
				if(!Dto.isObjNull(room)){ //房间局数已用完
					if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
							&&room.getInt("game_count")==room.getInt("game_count")){
						result.put("isOver", 1);
					}else{
						result.put("isOver", 0);
					}
				}else{
					room = mjBiz.getRoomInfoByRno1(roomNo);
					if(!Dto.isObjNull(room)){
						if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
								&&room.getInt("status")<0){ //房间局数已用完
							result.put("isOver", 1);
						}
					}
				}*/
				result.put("isOver", 0);
				
				client.sendEvent("reconnectGamePush_NN", result);
				LogUtil.print("创建房间（999）："+ JSONObject.fromObject(result));
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛reconnectGame方法消耗时间:"+(end-start));
		}
	}


	public void gameConnReset(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		RoomManage.lock(roomNo);
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			RoomManage.unLock(roomNo);
			// 重连恢复游戏
			JSONObject userinfo = client.get("userinfo");
			JSONObject dataJson = new JSONObject();
			dataJson.put("room_no", roomNo);
			dataJson.put("account", userinfo.getString("account"));
			reconnectGame(client, dataJson);
		}else {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛gameConnReset方法消耗时间:"+(end-start));
		}
	}

	public void gameXiaZhu(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		String uuid = Constant.getClientTag(client);
		JSONObject postdata = JSONObject.fromObject(data);
		if(postdata.containsKey("uuid")){
			uuid = postdata.getString("uuid");
		}
		String roomNo=postdata.getString("room_no");
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
				
				// 是否选择推注筹码
				if (room.isTuizhu()) {
					if (postdata.containsKey("tuizhu")) {
						room.getUserPacketMap().get(uuid).setTzChouma(true);
					}else {
						room.getUserPacketMap().get(uuid).setTzChouma(false);
					}
				}
				if (room.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU) {
					RoomManage.unLock(roomNo);
					return;
				}
				if (room.getUserPacketMap().get(uuid).getIsReady()!=NiuNiu.GAMESTATUS_JIESUAN) {
					
					int num = postdata.getInt("num");
					int place = postdata.getInt("place");
					int money = postdata.getInt("money");
					//int xiazhutime = room.getTimeLeft();
					
					//if(xiazhutime>0 || postdata.containsKey("auto")){
					
					boolean isCanXiaZhu = true;
					// 金币场时，判断玩家金币是否足够且庄家金币足够赔付
					if(room.getRoomType()==1){
						
						JSONObject myScore = room.getplaceArrayNums(room.getPlayerIndex(uuid));
						int xiazhuScore = 0;
						
						if(!Dto.isObjNull(myScore)){
							if(myScore.containsKey("1")){
								xiazhuScore+=myScore.getInt("1");
							}
							if(myScore.containsKey("2")){
								xiazhuScore+=myScore.getInt("2");
							}
							if(myScore.containsKey("3")){
								xiazhuScore+=myScore.getInt("3");
							}
							if(myScore.containsKey("4")){
								xiazhuScore+=myScore.getInt("4");
							}
							if(myScore.containsKey("5")){
								xiazhuScore+=myScore.getInt("5");
							}
							if(myScore.containsKey("0")){
								xiazhuScore+=myScore.getInt("0");
							}
						}
						
						// 玩家总积分
						double totalMoney = room.getPlayerMap().get(uuid).getScore();
						// 玩家可下注最大积分
						double maxScore = (xiazhuScore+money)*4*room.getScore();
						
						if(maxScore > totalMoney){
							
							isCanXiaZhu = false;
							postdata.element("code", 0);
							postdata.element("msg", "您的金币不足，无法下注");
							
						}else{
							
							// 总下注筹码
							double playertotalMoney = room.getPlayerTotalMoney();
							// 庄家剩余筹码
							double zhuangtotalMoney = room.getPlayerMap().get(room.getZhuang()).getScore();
							
							if((playertotalMoney + money)*4*room.getScore() > zhuangtotalMoney){
								
								isCanXiaZhu = false;
								postdata.element("code", 0);
								postdata.element("msg", "您的下注已超出限制，暂时无法下注");
							}
						}
					}
					
					// 倍率
					if(room.getGameType()==1){
						double score = room.getPlayerMoneyNum(room.getPlayerIndex(uuid), room.getPlayerIndex(uuid));
						if(score>0){
							isCanXiaZhu = false;
						}
					}
					
					// 判断玩家是否可以下注
					if(isCanXiaZhu){
						
						//添加下注记录
						room.addPlayerMoney(num, place, money);
						
						postdata.element("code", 1);
						postdata.element("msg", "下注成功");
						
						postdata.element("playerMoney", room.getMoneyPlace());
						//postdata.element("time", DateUtils.getTimestamp());
						
						//记录下下注信息
						room.getPlaceArray().add(postdata);
						
						// 房卡场下注完成
						if(room.getRoomType()==0 || room.getRoomType()==2 || room.getRoomType()==3){
							room.getUserPacketMap().get(uuid).setIsReady(2);
						}
					}
					
					//				}else{
					//					postdata.element("code", 0);
					//					postdata.element("msg", "下注时间到无法下注");
					//				}
					
					for(String uid:room.getPlayerMap().keySet()){
						if (!room.getRobotList().contains(uid)) {
							
							SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
							if(clientother!=null){
								clientother.sendEvent("gameXiaZhuPush_NN", postdata);
							}
						}
					}
					
					if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
						for (String uid : room.getGzPlayerMap().keySet()) {
							SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(uid).getUuid());
							if (clientother!=null) {
								clientother.sendEvent("gameXiaZhuPush_NN", postdata);
							}
						}
					}
					
					/**
					 * 20180317  所有人下完注不发牌
					 */
					boolean isReady = true;
					for(String uid : room.getPlayerMap().keySet()){
						UserPacket up = room.getUserPacketMap().get(uid);
						if(!room.getZhuang().equals(uid)&&up.getIsReady()!=2&&up.getStatus()!=-1){ // 判断是否存在玩家未下注
							if(room.getZhuangType()==3){ // 明牌抢庄
								if(up.getPs()!=null&&up.getPs()[0]!=null){
									isReady = false;
								}
							}else{
								isReady = false;
							}
						}
					}
					if (isReady&&!postdata.containsKey("auto")) {
						nnService.jieSuan(roomNo);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛gameXiaZhu方法消耗时间:"+(end-start));
		}
	}


	public void qiangZhuang(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		String clientTag = Constant.getClientTag(client);
		JSONObject postdata = JSONObject.fromObject(data);
		String roomNo=postdata.getString("room_no");
		String result=postdata.getString("result");

		try {
			RoomManage.lock(roomNo);
			// 设置抢庄加倍倍数
			if(postdata.containsKey("value")){
				int value = postdata.getInt("value");
				((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(clientTag).qzTimes=value;
			}else{
				((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(clientTag).qzTimes=-1;
			}
			
			nnService.qiangZhuang(roomNo, result, Constant.getClientTag(client));
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		} finally {
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛qiangZhuang方法消耗时间:"+(end-start));
		}
	}



	/**
	 * 闲家撤销下注
	 * @param client
	 * @param data
	 */
	public void revokeXiazhu(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		JSONObject obj= JSONObject.fromObject(data);
		String roomNo=obj.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
			JSONObject result = new JSONObject();
			String uuid = Constant.getClientTag(client);
			int num = room.getPlayerIndex(uuid);
			room.delPlayerMoney(num);

			//记录下下注信息
			JSONArray placeArray = room.getPlaceArray();
			for (int i = 0; i < placeArray.size(); i++) {
				JSONObject postdata = placeArray.getJSONObject(i);
				if(postdata.getInt("num")==num){
					postdata.element("msg", "撤回下注，置为0");
					postdata.element("money", 0);
				}
			}
			System.out.println("playerMoney:"+room.getMoneyPlace());

			result.element("playerMoney", room.getMoneyPlace());
			result.element("code", 1);
			result.element("num", num);

			for(String uid : room.getPlayerMap().keySet()){
				SocketIOClient askclient=GameMain.server.getClient(room.getUUIDByClientTag(uid));
				if(askclient!=null){
					askclient.sendEvent("revokeXiazhuPush_NN", result);
				}

			}

			// 玩家已撤销下注
			room.getUserPacketMap().get(uuid).setIsReady(0);

		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛revokeXiazhu方法消耗时间:"+(end-start));
		}
	}


	/**
	 * 闲家确认下注
	 * @param client
	 * @param data
	 */
	public void sureXiazhu(SocketIOClient client, Object data) {
		long start = System.currentTimeMillis();

		String clientTag = Constant.getClientTag(client);
		JSONObject obj= JSONObject.fromObject(data);

		String roomNo=obj.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
			// 当前非结算状态
			if(room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){

				String uuid = Constant.getClientTag(client);
				int num = room.getPlayerIndex(uuid);

				JSONObject result = new JSONObject();
				result.element("code", 1);
				result.element("num", num);

				for(String uid : room.getPlayerMap().keySet()){
					if (!room.getRobotList().contains(uid)) {
						SocketIOClient askclient=GameMain.server.getClient(room.getUUIDByClientTag(uid));
						if(askclient!=null){
							askclient.sendEvent("sureXiazhuPush_NN", result);
						}
					}
				}
				if (room.getUserPacketMap().containsKey(clientTag)) {
					// 玩家已确认下注
					room.getUserPacketMap().get(clientTag).setIsReady(2);
				}

				boolean isReady = true;
				for(String uid : room.getPlayerMap().keySet()){
					UserPacket up = room.getUserPacketMap().get(uid);
					if(!room.getZhuang().equals(uid)&&up.getIsReady()!=2){ // 判断是否存在玩家未下注

						if(room.getZhuangType()==3){
							if(up.getPs()!=null&&up.getPs()[0]!=null){
								isReady = false;
							}
						}else if(up.getStatus()>=NiuNiu.USERPACKER_STATUS_CHUSHI){
							isReady = false;
						}
					}
				}

				// 所有人都确定下注，进入结算
				if(isReady&&room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){

					LogUtil.print("所有人都确定下注，进入结算，当前游戏状态："+room.getGameStatus());

					nnService.jieSuan(roomNo);
				}
			}
		}
		long end = System.currentTimeMillis();
		if (end-start>10) {
			LogUtil.print(roomNo+"牛牛sureXiazhu方法消耗时间:"+(end-start));
		}
	}

	/**
	 * 观战玩家入座
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年1月17日
	 */
	public void gameRuzuo(SocketIOClient client, Object data) {

		String clientTag = Constant.getClientTag(client);
		JSONObject postdata = JSONObject.fromObject(data);

		// 房间号
		String roomNo = postdata.getString("room_no");
		NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
		int index = postdata.getInt("index");

		// 玩家信息
		Playerinfo playerinfo = room.getGzPlayerMap().get(clientTag);
		if (playerinfo==null) {
			if (room.getPlayerMap().containsKey(clientTag)) {
				SocketIOClient clientother1=GameMain.server.getClient(room.getPlayerMap().get(clientTag).getUuid());
				if (clientother1!=null) {
					JSONObject result = new JSONObject();
					result.put("code", 0);
					result.put("msg", "您已在房间内");
					clientother1.sendEvent("gameRuzuoPush_NN", result);
				}
			}
		}else if (room.getPlayerMap().size()==room.getPlayerCount()) {
			SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(clientTag).getUuid());
			if (clientother1!=null) {
				JSONObject result = new JSONObject();
				result.put("code", 0);
				result.put("msg", "当前房间座位已满");
				clientother1.sendEvent("gameRuzuoPush_NN", result);
			}
		}else {
			room.getGzPlayerMap().remove(clientTag);
			playerinfo.setMyIndex(index);

			room.getPlayerMap().put(clientTag, playerinfo);//用户的个人信息
			room.getUserPacketMap().put(clientTag, new UserPacket());
			room.getUserIDSet().add(playerinfo.getId());
			RoomManage.gameRoomMap.put(roomNo, room);

			String sql = "update za_gamerooms set user_id"+index+" =?, user_icon"+index+" =?, user_name"+index+" =?, user_score"+index+" =? where room_no=?";
			Object[] params = new Object[] { playerinfo.getId(), playerinfo.getHeadimg(), playerinfo.getName(), playerinfo.getScore(), roomNo };
			DBUtil.executeUpdateBySQL(sql, params);

			// 通知玩家
			JSONArray users = room.getAllPlayer();
			for (String uuid : room.getPlayerMap().keySet()) {

				JSONObject result = new JSONObject();
				result.put("code", 1);
				result.put("users", users);
				result.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
				SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
				if(clientother!=null){
					clientother.sendEvent("gameRuzuoPush_NN", result);
				}
			}

			//通知观战玩家
			if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
				for (String string : room.getGzPlayerMap().keySet()) {
					JSONObject result = new JSONObject();
					result.put("code", 1);
					result.put("users", users);
					result.put("myIndex", NiuNiu.USERPACKER_STATUS_GUANZHAN);
					SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
					if(clientother1!=null){
						clientother1.sendEvent("gameRuzuoPush_NN", result);
					}
				}
			}
		}
	}

	/**
	 * 观战玩家重连
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年1月18日
	 */
	public void gzReconnectGame(SocketIOClient client, Object data) {

		JSONObject obj= JSONObject.fromObject(data);
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
		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			NNGameRoom game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
			Playerinfo player = null;
			for(String uuid:game.getGzPlayerMap().keySet()){
				if(game.getGzPlayerMap().get(uuid)!=null&&game.getGzPlayerMap().get(uuid).getAccount().equals(account)){
					player = game.getGzPlayerMap().get(uuid);
					player.setStatus(Constant.ONLINE_STATUS_YES);
					player.setUuid(client.getSessionId());
				}
			}

			if(player!=null){

				// 设置会话信息
				client.set(Constant.ROOM_KEY_NN, roomNo);
				//根据uuid获取用户信息
				JSONObject userinfo=mjBiz.getUserInfoByAccount(account);
				client.set("userinfo", userinfo);
				LogUtil.print(account+":重连成功！");

				// 返回给玩家当前牌局信息（基础信息）
				JSONObject result=new JSONObject();

				if (game.getGameStatus()!=NiuNiu.GAMESTATUS_LIANGPAI&&game.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN) {

					result.put("game_index",game.getGameIndex()+1);
				}else {
					result.put("game_index",game.getGameIndex());

				}
				// 自动开始游戏倒计时
				if(game.getReadyCount()>=2){
					result.put("timer", game.getTimeLeft());
				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN 
						|| game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
					if (game.getTimeLeft()==0&&game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN) {
						result.put("timer", NNGameEventDeal.GLOBALTIMER[1]);
					}else {
						result.put("timer", game.getTimeLeft());
					}
				}else{
					result.put("timer", 0);
				}

				if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY || game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){ // 开局准备阶段

					result.put("type", 0); //游戏准备时断线重连

				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){ // 玩家下注阶段

					result.put("type", 1);
					if (game.getPlayerMap().get(game.getZhuang())!=null) {
						result.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
					}
					result.put("game_index", game.getGameIndex()+1);
					result.put("playerMoney", game.getMoneyPlace());// 获取玩家下注分数
					result.put("myMoney", game.getplaceArrayNums(player.getMyIndex()));
					// 所有玩家下注记录
					result.put("placeArray", game.getPlaceArray());

				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN || game.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI){ // 结算阶段

					if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN){
						result.put("type", 2);
						result.put("showPai", game.getChuPaiIndex());
					}else{
						result.put("type", 3);
					}
					if (game.getPlayerMap().get(game.getZhuang())!=null) {
						result.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
					}
					result.put("playerMoney", game.getMoneyPlace());// 获取玩家下注分数
					// 所有玩家下注记录
					result.put("placeArray", game.getPlaceArray());

					// 庄家通杀
					boolean tongSha = true;
					// 庄家通赔
					boolean tongPei = true;
					for(String uuid:game.getPlayerMap().keySet()){

						UserPacket up = game.getUserPacketMap().get(uuid);

						// 根据闲家的输赢判断庄家是否通杀或是通赔
						if(!uuid.equals(game.getZhuang())){
							if(up.isWin()){
								tongSha = false;
							}else{
								tongPei = false;
							}
						}
					}

					// 各个玩家信息
					JSONArray array = new JSONArray();
					// 通知玩家
					for(String uuid:game.getPlayerMap().keySet()){
						UserPacket up = game.getUserPacketMap().get(uuid);
						if(up!=null&&up.getMyPai()[0]>0){
							JSONObject user = new JSONObject();
							user.put("account", game.getPlayerMap().get(uuid).getAccount());
							user.put("name", game.getPlayerMap().get(uuid).getName());
							user.put("headimg", game.getPlayerMap().get(uuid).getHeadimg());
							if(uuid.equals(game.getZhuang())){
								user.put("zhuang", 1);
							}else{
								user.put("zhuang", 0);
							}
							user.put("myIndex", game.getPlayerIndex(uuid));
							user.put("myPai", up.getSortPai());
							user.put("mingPai", up.getMingPai());
							user.put("result", up.type);
							user.put("ratio", up.getRatio(game)); // 倍率
							user.put("score", up.getScore());
							user.put("totalScore", game.getPlayerMap().get(uuid).getScore());
							user.put("myMoney", game.getplaceArrayNums(game.getPlayerIndex(uuid)));
							int win = 0;
							if(!uuid.equals(game.getZhuang())){

								if(up.isWin()){
									win=1;
								}
							}else{

								int totalScore = 0;
								for(String key:game.getPlayerMap().keySet()){
									if(!key.equals(game.getZhuang())){
										totalScore -= game.getUserPacketMap().get(key).getScore();
									}
								}
								if(totalScore>=0){
									win=1;
								}
							}
							user.put("win", win);
							// 庄通杀、通赔
							int zhuangTongsha = 0;
							if(tongSha){
								zhuangTongsha = 1;
							}else if(tongPei){
								zhuangTongsha = -1;
							}
							user.put("zhuangTongsha", zhuangTongsha);

							array.add(user);
						}
					}
					result.put("users", array);
				}	

				if(game.getZhuangType()==3){
					result.put("qzType", 1);
				}

				result.put("baseNum", game.getBaseNum());
				if(game.getRoomType()==3){
					result.put("baseNum2", game.getBaseNumTimes(player.getScore()));
				}
				result.put("gametype", game.getGameType());
				result.put("jiesan", 0);
				//判断当前是否是在申请解散房间阶段
				if(game.getCloseTime()>0){
					boolean isJieSan = true;
					JSONArray jiesans = new JSONArray();
					for(String uuid:game.getPlayerMap().keySet()){
						if(game.getPlayerMap().get(uuid)!=null){
							JSONObject jiesan = new JSONObject();
							jiesan.put("name", game.getPlayerMap().get(uuid).getName());
							jiesan.put("index", game.getPlayerMap().get(uuid).getMyIndex());
							if(game.getUserPacketMap().get(uuid).isCloseRoom==1){
								jiesan.put("result", 1);
							}else if(game.getUserPacketMap().get(uuid).isCloseRoom==-1){
								isJieSan = false;
								break;
							}else{
								jiesan.put("result", 0);
							}
							jiesan.put("jiesanTime", game.getCloseTime());
							jiesans.add(jiesan);
						}
					}
					if(isJieSan){

						result.put("jiesan", 1);
						result.put("jiesanData", jiesans);
					}
				}

				//				if(game.getZhuangType()==3&&game.getUserPacketMap().get(clientTag).getStatus()!=NiuNiu.USERPACKER_STATUS_LIANGPAI){
				//					int[] mypai = Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(clientTag).getMingPai();
				//					int[] pais = new int[5];
				//					if(mypai!=null){
				//						for (int i = 0; i < pais.length-1; i++) {
				//							pais[i] = mypai[i];
				//						}
				//					}
				//					result.put("myPai",pais);
				//				}
				if(game.getZhuangType()==3){
					int[] pais = new int[]{0,0,0,0,0};
					result.put("myPai",pais);
				}

				JSONObject roomData=new JSONObject();
				roomData.put("room_no", roomNo);
				roomData.put("users",game.getAllPlayer());//告诉他原先加入的玩家
				roomData.put("myIndex",NiuNiu.USERPACKER_STATUS_GUANZHAN);
				roomData.put("isReady",game.getReadyIndex());

				// 抢庄模式
				if((game.getZhuangType()==2 || game.getZhuangType()==3) 
						&&game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){

					roomData.put("zhuang",-1);
					roomData.put("qiangzhuang", game.getPlayerIsReady());
					// 倒计时
					roomData.put("qztimer", game.getTimeLeft());
					// 判断当前是否是抢庄阶段
					result.put("qzReady", 1);
					result.put("zhuang",-1);

				}else{
					roomData.put("zhuang",game.getPlayerIndex(game.getZhuang()));
				}

				// 抢庄加倍
				if(game.getZhuangType()==2 || game.getZhuangType()==3){

					if(game.getGameStatus()!=NiuNiu.GAMESTATUS_READY){

						// 将确定下来的倍数回传给玩家
						int qzScore = game.getUserPacketMap().get(game.getZhuang()).qzTimes;
						if(qzScore<1){
							qzScore = 1;
						}
						roomData.put("qzScore", qzScore);
					}

					// 获取玩家最大下注倍数
					if(game.getRoomType()==1 || game.getRoomType()==3){
						roomData.put("qzTimes2", game.getQzTimes(player.getScore()));
					}
				}

				result.put("roomData", roomData);
				// 获取正在游戏中的玩家下标
				result.put("isGameIng",game.getGameIngIndex());

				// 获取房间信息
				JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
				if(!Dto.isObjNull(room)){ //房间局数已用完
					if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
							&&room.getInt("game_count")==room.getInt("game_index")){
						result.put("isOver", 1);
					}else{
						result.put("isOver", 0);
					}
				}else{
					room = mjBiz.getRoomInfoByRno1(roomNo);
					if(!Dto.isObjNull(room)){
						if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
								&&room.getInt("status")<0){ //房间局数已用完
							result.put("isOver", 1);
						}
					}
				}

				LogUtil.print("isover="+result.toString());
				LogUtil.print("断线重连返回="+result.toString());
				client.sendEvent("reconnectGamePush_NN", result);
			}

		}else{

			JSONObject result = new JSONObject();
			result.put("type", 999); //玩家还未创建房间就已经掉线

			// 获取房间信息
			JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
			if(!Dto.isObjNull(room)){ //房间局数已用完
				if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
						&&room.getInt("game_count")==room.getInt("game_index")){
					result.put("isOver", 1);
				}else{
					result.put("isOver", 0);
				}
			}else{
				room = mjBiz.getRoomInfoByRno1(roomNo);
				if(!Dto.isObjNull(room)){
					if((room.getInt("roomtype")==0||room.getInt("roomtype")==2)
							&&room.getInt("status")<0){ //房间局数已用完
						result.put("isOver", 1);
					}
				}
			}

			client.sendEvent("reconnectGamePush_NN", result);
			LogUtil.print("创建房间（999）："+ JSONObject.fromObject(result));
		}
	}

	/**
	 * 观战玩家退出房间
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年1月18日
	 */
	public void exitRoomGz(SocketIOClient client, String uuid, String roomNo) {
		if (RoomManage.gameRoomMap.containsKey(roomNo)) {
			if ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)!=null) {
				NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
				if (room.isGuanzhan()&&room.getFangzhu().equals(uuid)) {
					JSONObject result = new JSONObject();
					result.put("code", 0);
					result.put("type", 2); //退出房间
					result.put("msg", "房主不允许退出游戏");
					client.sendEvent("exitRoomPush_NN", result);
				}else if (room.isGuanzhan()&&room.getGzPlayerMap().containsKey(uuid)&&
						room.getGzPlayerMap().get(uuid)!=null) {

					// 如有坐下过则清空信息
					JSONObject room1 = mjBiz.getRoomInfoByRno(roomNo);
					Playerinfo playerinfo = room.getGzPlayerMap().get(uuid);
					if(room1!=null&&room1.containsKey("id")){

						String userIndex = null;
						long userId = playerinfo.getId();

						if(userId==room1.getLong("user_id0")){
							userIndex = "user_id0";
						}
						if(userId==room1.getLong("user_id1")){
							userIndex = "user_id1";
						}
						if(userId==room1.getLong("user_id2")){
							userIndex = "user_id2";
						}
						if(userId==room1.getLong("user_id3")){
							userIndex = "user_id3";
						}
						if(userId==room1.getLong("user_id4")){
							userIndex = "user_id4";
						}
						if(userId==room1.getLong("user_id5")){
							userIndex = "user_id5";
						}
						if(userId==room1.getLong("user_id6")){
							userIndex = "user_id6";
						}
						if(userId==room1.getLong("user_id7")){
							userIndex = "user_id7";
						}
						if(userId==room1.getLong("user_id8")){
							userIndex = "user_id8";
						}
						if(userId==room1.getLong("user_id9")){
							userIndex = "user_id9";
						}

						if(userIndex!=null){

							String sql = "update za_gamerooms set "+userIndex+"=? where status>=0 and id=?";
							DBUtil.executeUpdateBySQL(sql, new Object[]{0, room1.getLong("id")});
						}
					}

					room.getGzPlayerMap().remove(uuid);
					JSONObject result = new JSONObject();
					result.put("type", 2); //退出房间
					result.put("index", NiuNiu.USERPACKER_STATUS_GUANZHAN);// 观战玩家index为-1
					result.put("code", 1);
					// 观战玩家退出房间只通知自己
					client.sendEvent("exitRoomPush_NN", result);
				}
			}
		}
	}

	/**
	 * 观战玩家站起
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年1月18日
	 */
	public void gameZhanQi(SocketIOClient client, Object data){
		String clientTag = Constant.getClientTag(client);
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		if (RoomManage.gameRoomMap.containsKey(roomNo)) {
			if ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)!=null) {
				NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
				// 玩家信息
				Playerinfo playerinfo = room.getPlayerMap().get(clientTag);
				if (playerinfo==null) {// 玩家不在房间内
					JSONObject result = new JSONObject();
					result.put("code", 0);
					result.put("msg", "您已不在当前房间内");
					client.sendEvent("gameZhanQiPush_NN", result);
				}else if (room.getGameStatus()!=NiuNiu.GAMESTATUS_READY) {// 游戏非初始状态
					JSONObject result = new JSONObject();
					result.put("code", 0);
					result.put("msg", "游戏已经开始");
					client.sendEvent("gameZhanQiPush_NN", result);
				}else if (playerinfo!=null) {

					// 移除用户个人信息
					room.getPlayerMap().remove(clientTag);
					room.getUserPacketMap().remove(clientTag);
					room.getUserIDSet().remove(playerinfo.getId());

					// 将玩家设置为观战状态
					playerinfo.setMyIndex(NiuNiu.USERPACKER_STATUS_GUANZHAN);
					room.getGzPlayerMap().put(clientTag, playerinfo);

					RoomManage.gameRoomMap.put(roomNo, room);

					// 通知玩家
					JSONArray users = room.getAllPlayer();
					for (String uuid : room.getPlayerMap().keySet()) {

						JSONObject result = new JSONObject();
						result.put("code", 1);
						result.put("users", users);
						result.put("isReady",room.getReadyIndex());
						result.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							clientother.sendEvent("gameZhanQiPush_NN", result);
						}
					}

					//通知观战玩家
					if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
						for (String string : room.getGzPlayerMap().keySet()) {
							JSONObject result = new JSONObject();
							result.put("code", 1);
							result.put("users", users);
							result.put("isReady",room.getReadyIndex());
							result.put("myIndex", NiuNiu.USERPACKER_STATUS_GUANZHAN);
							SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
							if(clientother1!=null){
								clientother1.sendEvent("gameZhanQiPush_NN", result);
							}
						}
					}

					// 所有人都已准备则开始游戏
					int count = room.getUserPacketMap().size();
					if(count>1 && room.getReadyIndex().length==count){
						if(room.getTimeLeft()>=3){

							if(room.getZhuangType()==2||room.getZhuangType()==3){
								// 触发抢庄方法
								room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
								if (room!=null) {
									for (String uid  : room.getUserPacketMap().keySet()) {
										int ready = room.getUserPacketMap().get(uid).getIsReady();
										if(ready==1){

											SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
											if(clientother!=null){
												JSONObject data1 = new JSONObject();
												data1.put("room_no", roomNo);
												data1.put("auto", 1);
												gameReady1(clientother, data1);
											}
											break;
										}
									}
								}
							}else{
								// 触发开始游戏
								startGame(room);
							}
						}
					}
				}
			}
		}
	}

	public static void luckyTurning(NNGameRoom room){
		int luckyNum = RandomUtils.nextInt(100);
		// 取幸运值最大玩家uuid
		int maxLuck = 0;
		String maxUuid = null;
		for (String uuid : room.getUserPacketMap().keySet()) {
			if (room.getUserPacketMap().get(uuid).getLuck()>maxLuck) {
				maxLuck = room.getUserPacketMap().get(uuid).getLuck();
				maxUuid = uuid;
			}
		}
		// 幸运值大于随机数
		if (maxLuck!=0&&maxUuid!=null&&room.getUserPacketMap().get(maxUuid).getLuck()>=luckyNum) {
			UserPacket maxUp = room.getUserPacketMap().get(maxUuid);
			for (String uuid : room.getUserPacketMap().keySet()) {
				if (!uuid.equals(maxUuid)) {
					UserPacket zhuang = new UserPacket(maxUp.getPs(), true, room.getSpecialType());
					UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), room.getSpecialType());
					PackerCompare.getWin(userpacket, zhuang);
					if(userpacket.isWin()){
						Packer[] ps = maxUp.getPs();
						maxUp.setPs(room.getUserPacketMap().get(uuid).getPs());
						room.getUserPacketMap().get(uuid).setPs(ps);
					}
				}
			}
		}
	}

	/**
	 * 玩家换桌
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年2月11日
	 */
	public void changeTable(SocketIOClient client, Object data){
		JSONObject postdata = JSONObject.fromObject(data);
		String roomNo = postdata.getString("room_no");
		double di = postdata.getDouble("di");
		if (RoomManage.gameRoomMap.containsKey(roomNo)) {
			NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
			if (room!=null) {
				// 查询剩余的房间
				String sql = "select $ from za_gamerooms where game_id=? and roomtype=? and status=? and room_no!=?";
				String temp = "room_no,base_info";
				for (int i = 0; i < room.getPlayerCount(); i++) {
					temp += (",user_id"+i) ;
				}
				JSONArray roomArray = DBUtil.getObjectListBySQL(sql.replace("$", temp), new Object[]{1,room.getRoomType(),0,roomNo});

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
					JSONObject userinfo = client.get("userinfo");
					changeTableExit(Constant.getClientTag(client), roomNo, userinfo.getLong("id"));
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
					client.sendEvent("tipMsgPush_NN", obj);
				}
			}
		}
	}

	/**
	 * 玩家退出玩家房间
	 * @param uuid
	 * @param roomNo
	 * @param userId
	 */
	public void changeTableExit(String uuid, String roomNo, long userId) {
		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			NNGameRoom game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);

			boolean isRealExit = true;

			if (game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN||
					game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU||
					game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG) {
				Integer[] gameIngIndex = game.getGameIngIndex();
				for (Integer integer : gameIngIndex) {
					// 山弹头报错
					if (game.getPlayerMap().get(uuid)!=null&&game.getPlayerMap().get(uuid).getMyIndex()==integer) {
						isRealExit = false;
						JSONObject result = new JSONObject();
						result.put("type", 2); //退出房间
						result.put("code", 0);
						result.put("msg", "您正在游戏中无法换桌");
						SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_NN", result);
						}
						break;
					}
				}
			}

			// 金币场庄家退出游戏
			else if((game.getRoomType()==1||game.getRoomType()==3)&&game.getPlayerMap().size()>1){

				// 准备或者牌局结束阶段庄家退出
				if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY || game.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI){

					if(game.getZhuangType()!=5&&game.getZhuangType()!=2&&game.getZhuangType()!=3){ // 通比模式不进行换庄

						// 换庄
						if(game.getZhuang().equals(uuid)){

							String oldZhuang = game.getZhuang();
							String zhuang = game.getNextPlayer(oldZhuang);
							if(!oldZhuang.equals(zhuang)){

								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
								((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setFangzhu(zhuang);
								JSONObject result = new JSONObject();
								result.put("zhuang", game.getPlayerIndex(zhuang));
								for (String uid  : game.getUserPacketMap().keySet()) {
									if (!game.getRobotList().contains(uid)) {
										// 重置庄家信息
										SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
										if(clientother!=null){
											clientother.sendEvent("huanZhuangPush_NN", result);
										}
									}
								}
							}
						}
					}

				}else if(game.getUserPacketMap().get(uuid)!=null && 
						(game.getUserPacketMap().get(uuid).getStatus()<=NiuNiu.USERPACKER_STATUS_CHUSHI || 
						game.getUserPacketMap().get(uuid).getMyPai()[0]==0)){ // 玩家处于观战中，随时可以退出
					System.out.println("出门左转，请便！");

				}else{

					isRealExit = false;

					JSONObject result = new JSONObject();
					result.put("type", 2); //退出房间
					result.put("code", 0);
					result.put("msg", "您正在游戏中无法换桌");
					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
					if(askclient!=null){
						askclient.sendEvent("exitRoomPush_NN", result);
					}
				}
			}

			if(isRealExit){

				// 玩家退出房间
				changeTableExit(game, uuid, userId);

				// 所有人都已准备则开始游戏
				int count = game.getUserPacketMap().size();
				if(game.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU&&game.getGameStatus()!=NiuNiu.GAMESTATUS_QIANGZHUANG&&
						game.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN&&
						count>1 && game.getReadyIndex().length==count){
					if(game.getTimeLeft()>=3){

						if(game.getZhuangType()==2||game.getZhuangType()==3){
							// 触发抢庄方法
							game = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
							if (game!=null) {
								for (String uid  : game.getUserPacketMap().keySet()) {
									int ready = game.getUserPacketMap().get(uid).getIsReady();
									if(ready==1){

										SocketIOClient clientother=GameMain.server.getClient(game.getUUIDByClientTag(uid));
										if(clientother!=null){
											JSONObject data = new JSONObject();
											data.put("room_no", roomNo);
											data.put("auto", 1);
											gameReady1(clientother, data);
										}
										break;
									}
								}
							}
						}else{
							// 触发开始游戏
							startGame(game);
						}
					}
				}
			}
		}
	}


	/**
	 * 换桌退出房间
	 * @param game
	 * @param uuid
	 * @param userId
	 */
	public void changeTableExit(NNGameRoom game, String uuid, long userId){
		String roomNo = game.getRoomNo();
		if(game.getPlayerMap().get(uuid)!=null){

			JSONObject result = new JSONObject();
			result.put("type", 2); //退出房间
			result.put("index", game.getPlayerIndex(uuid));
			for(String uuid1:game.getPlayerMap().keySet()){
				if(!game.getRobotList().contains(uuid1)&&!uuid1.equals(uuid)){

					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
					if(askclient!=null){
						askclient.sendEvent("exitRoomPush_NN", result);
					}
				}
			}
		}

		// 清除房间用户数据
		((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().remove(uuid);
		((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().remove(uuid);

		// 获取房间信息
		JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
		if(room!=null&&room.containsKey("id")){

			String userIndex = null;

			if(userId==room.getLong("user_id0")){
				userIndex = "user_id0";
			}
			if(userId==room.getLong("user_id1")){
				userIndex = "user_id1";
			}
			if(userId==room.getLong("user_id2")){
				userIndex = "user_id2";
			}
			if(userId==room.getLong("user_id3")){
				userIndex = "user_id3";
			}
			if(userId==room.getLong("user_id4")){
				userIndex = "user_id4";
			}
			if(userId==room.getLong("user_id5")){
				userIndex = "user_id5";
			}
			if(userId==room.getLong("user_id6")){
				userIndex = "user_id6";
			}
			if(userId==room.getLong("user_id7")){
				userIndex = "user_id7";
			}
			if(userId==room.getLong("user_id8")){
				userIndex = "user_id8";
			}
			if(userId==room.getLong("user_id9")){
				userIndex = "user_id9";
			}

			if(userIndex!=null){

				String sql = "update za_gamerooms set "+userIndex+"=? where status>=0 and id=?";
				DBUtil.executeUpdateBySQL(sql, new Object[]{0, room.getLong("id")});
			}

			((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserIDSet().remove(userId);
		}

		//房间内全是机器人则清除房间
		boolean clearRoom = true;
		for (String uid : ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().keySet()) {
			if (!((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getRobotList().contains(uid)) {
				clearRoom = false;
				break;
			}
		}

		// 准备人数少于两人取消定时器
		int count1 = 0;
		for (String uid:game.getUserPacketMap().keySet()) {
			int ready = game.getUserPacketMap().get(uid).getIsReady();
			if(ready!=0&&ready!=10){
				count1++;
			}
		}
		if (count1<2) {
			JSONObject result = new JSONObject();
			result.put("timer", 0);
			for (String uuid1  : game.getUserPacketMap().keySet()) {
				if (!game.getRobotList().contains(uuid1)) {
					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
					if(askclient!=null){
						askclient.sendEvent("nnTimerPush_NN", result);
					}
				}
			}
		}

		// 金币场没人的房间直接清除
		if((game.getRoomType()==1||game.getRoomType()==3)&&(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getPlayerMap().size()==0||clearRoom)){
			//重置状态
			if (game.isRobot()) {
				for (String uid1 : ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getRobotList()) {
					String sql = "update za_users set status=0 where account=?";
					DBUtil.executeUpdateBySQL(sql, new Object[]{uid1});
				}
			}
			RoomManage.gameRoomMap.remove(roomNo);
			String sql = "update za_gamerooms set status=? where id=?";
			DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
			LogUtil.print("金币场没人的房间直接清除："+roomNo);
		}
	}

	// 准备定时器
	public void autoReady(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		LogUtil.print("准备定时器倒计时到时事件");
		try {
			RoomManage.lock(roomNo);
			if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null){
				
				NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
				
				// 准备人数多于2人才踢人
				int count = 0;
				for (String uid:room.getUserPacketMap().keySet()) {
					int ready = room.getUserPacketMap().get(uid).getIsReady();
					if(ready!=0&&ready!=10){
						count++;
					}
				}
				if((room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI)&&count>1){
					
					
					try {
						Set<String> uuidList = room.getUserPacketMap().keySet();
						List<String> lxList = new ArrayList<String>();
						for (String uuid : uuidList) {
							// 还没准备
							if(room.getUserPacketMap().get(uuid).getIsReady() != 1){
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
					
					if(room.getZhuangType()==2||room.getZhuangType()==3){
						// 触发抢庄方法
						room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
						if (room!=null) {
							for (String uid  : room.getUserPacketMap().keySet()) {
								int ready = room.getUserPacketMap().get(uid).getIsReady();
								if(ready==1){
									
									SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
									if(clientother!=null){
										JSONObject data1 = new JSONObject();
										data1.put("room_no", roomNo);
										data1.put("auto", 1);
										gameReady1(clientother, data1);
										LogUtil.print("准备定时器倒计时到时事件触发开始游戏");
										break;
									}
								}
							}
						}
					}else{
						// 多于一人才开始游戏
						if(room.getUserPacketMap().size()>1){
							// 触发开始游戏
							startGame(room);
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

	// 抢庄定时器
	public void qiangzhuang(SocketIOClient client, Object data){
		int restartTime = 0;

		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		LogUtil.print("抢庄定时器倒计时到时事件");
		// 如果房间存在
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null){
					restartTime = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getRestartTime();
					NNGameRoom room = (NNGameRoom)RoomManage.gameRoomMap.get(roomNo);
					
					// 重新发牌终止上一轮线程
					if (restartTime!=room.getRestartTime()) {
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setXiazhuTime(0);
						RoomManage.unLock(roomNo);
						return;
					}
					
					if(room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI
							|| room.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
						//如果抢庄时间到，则默认不抢
						try {
							for (String uuid : room.getUserPacketMap().keySet()) {
								UserPacket up = room.getUserPacketMap().get(uuid);
								if(!(up.getIsReady()==10||up.getIsReady()==-1)){ // 获取当前未抢庄的玩家
									if(up.getPs()!=null&&up.getPs()[0]!=null){
										((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).qzTimes=-1;
										nnService.qiangZhuang(roomNo, "-1", uuid);
										LogUtil.print("抢庄定时器倒计时到时事件触发开始游戏");
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
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

	// 下注定时器
	public void xiazhu(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		LogUtil.print("下注定时器倒计时到时事件");
		// 如果房间存在
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null){
					NNGameRoom room = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
					
					// 金币场或房卡场且局数还没用完
					if(room.getRoomType()==1 || room.getGameCount()>room.getGameIndex()){ 
						if(room.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
							
							boolean isReady = true;
							for(String uid : room.getPlayerMap().keySet()){
								UserPacket up = room.getUserPacketMap().get(uid);
								if(!room.getZhuang().equals(uid)&&up.getIsReady()!=2&&up.getStatus()!=-1){ // 判断是否存在玩家未下注
									if(room.getZhuangType()==3){ // 明牌抢庄
										if(up.getPs()!=null&&up.getPs()[0]!=null){
											isReady = false;
										}
									}else{
										isReady = false;
									}
								}
							}
							
							//如果下注时间到，则执行比牌方法
							
							try {
								if(!isReady){
									JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
									// 选择最低筹码
									int money = baseNum.getJSONObject(0).getInt("val");
									for(String uuid:room.getPlayerMap().keySet()){
										UserPacket up = room.getUserPacketMap().get(uuid);
										if(!room.getZhuang().equals(uuid)&&up.getIsReady()!=2&&up.getStatus()!=-1){ // 判断是否存在玩家未下注
											// 下注倒计时结束，自动下注（最小筹码）
											if((room.getZhuangType()!=2 && room.getZhuangType()!=3)||(up.getPs()!=null&&up.getPs()[0]!=null)){
												
												int index = room.getPlayerMap().get(uuid).getMyIndex();
												JSONObject obj = new JSONObject();
												obj.put("room_no", room.getRoomNo());
												obj.put("num", index);
												obj.put("place", index);
												obj.put("money", money);
												obj.put("auto", 1);
												obj.put("uuid", uuid);
												SocketIOClient client1=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
												gameXiaZhu(client1, obj);
												LogUtil.print("下注定时器倒计时到时事件触发====下注事件");
											}
										}
									}
								}
								
								// 进入结算
								if(RoomManage.gameRoomMap.containsKey(roomNo)
										&&((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
									LogUtil.print("下注倒计时结束，进入结算，当前游戏状态："+room.getGameStatus());
									
									nnService.jieSuan(roomNo);
								}
								
							} catch (Exception e) {
								e.printStackTrace();
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

	// 亮牌定时器
	public void showPai(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		LogUtil.print("亮牌定时器倒计时到时事件");
		// 如果房间存在
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				//定时器倒计时
				if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null){
					NNGameRoom room = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
					if(room.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN){
						// 游戏状态
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_LIANGPAI);
						
						// 所有玩家都亮牌完毕，展示结算数据
						LogUtil.print("亮牌定时器倒计时到时事件-------+++触发亮牌");
						for (String uid  : room.getUserPacketMap().keySet()) {
							if (!room.getRobotList().contains(uid)) {
								
								SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
								if(clientother!=null){
									JSONObject result = new JSONObject();
									result.put("type", 1);
									clientother.sendEvent("showPaiPush_NN", result);
								}
							}
							// 重置玩家状态信息
							((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uid).setIsReady(0);
							((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
						}
						
						// 金币场
						/*if(room.getRoomType()==1){

        						// 1、游戏结束后，开始下一局游戏时，移除掉线的玩家 
        						//nnService.cleanDisconnectPlayer(room);

        						// 2、超过时间没有开始游戏，需要换庄
        						// 开启准备定时器，开始计时
        						MutliThreadNN m = new MutliThreadNN(null, roomNo, 0);
        						m.start();

        						if (room.isRobot()) {
									AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 3);
									a.start();

									AutoThreadNN a1 = new AutoThreadNN(nnService, roomNo, 4);
									a1.start();
								}

        					}*/
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

	// 开始游戏（抢庄）定时器
	public void startgameqz(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		// 如果房间存在
		try {
			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				
				NNGameRoom room = ((NNGameRoom)RoomManage.gameRoomMap.get(roomNo));
				List<String> uuids = new ArrayList<String>();
				// 抢庄最大倍数
				int qzTimes = 0;
				for (String uuid : room.getPlayerMap().keySet()) {
					UserPacket up = room.getUserPacketMap().get(uuid);
					if(up.getIsReady()==10){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
						if(up.qzTimes>=qzTimes){
							if(up.qzTimes>qzTimes){
								// 清空列表
								uuids.clear();
								qzTimes = up.qzTimes;
							}
							uuids.add(uuid);
						} 
					}
				}
				int time = uuids.size();
				if(time==0){
					time = 1;
				}
				//定时器倒计时
				for (int i = time; i >= 0; i--) {
					if (uuids.size()>=2) {
						try {
							Thread.sleep(800);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
					if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null){
						
						((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).setXiazhuTime(i);//保存倒计时时间
						if(room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI
								|| room.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
							// 开始游戏
							if(i==0){
								try {
									
									// 开始游戏
									startGame(room);
									
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}else{
							break;
						}
					}else{
						System.out.println("房间不存在："+roomNo);
						break;
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

	public void jiesan(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		String roomNo = fromObject.getString("room_no");
		// 如果房间存在
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			//定时器倒计时
			if(((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null&&((NNGameRoom)RoomManage.gameRoomMap.get(roomNo)).getCloseTime()>0){


				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (((NNGameRoom)RoomManage.gameRoomMap.get(roomNo))!=null) {
					MaJiangBiz mjBiz=new MajiangBizImpl();
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
				}
			}
		}
	}
	
	// 保存战绩
	public void savelogs(NNGameRoom room,
                         JSONObject gamelog, JSONArray uglogs, int type, JSONArray array) {
		// 房间号
		String roomNo = room.getRoomNo();
		//更新房间信息
		JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
		if(roomInfo!=null&&roomInfo.containsKey("id")){
			int game_index = roomInfo.getInt("game_index")+1;
			String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			gamelog.put("room_id", roomInfo.getLong("id"));
			long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);
			if (type==1) {// 房卡场
				// 保存玩家战绩
				String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
						+ "gamelog_id,result,createtime) VALUES";
				Object[] params = new Object[]{};
				int temp = 0;
				for(Long uid:room.getUserIDSet()){
					gamelogSql += "("+1+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+",'"+
							uglogs.toString()+"','"+nowTime+"')";
					if (temp < room.getUserIDSet().size()-1) {
						gamelogSql += ",";
					}
					temp++;
				}
				DBUtil.executeUpdateBySQL(gamelogSql, params);
			}else if (type==2) {// 元宝、金币场
				String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
						+ "gamelog_id,result,createtime,account,fee) VALUES";
				Object[] params = new Object[array.size()];
				for (int i = 0; i < array.size(); i++) {
					JSONObject user = array.getJSONObject(i);
					long uid = user.getLong("uid");
					gamelogSql += "("+1+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+","+
							"?"+",'"+nowTime+"',"+user.get("score")+","+room.getFee()+")";
					if (i!=array.size()-1) {
						gamelogSql += ",";
					}
					params[i] = uglogs.toString();
				}
				try {
					DBUtil.executeUpdateBySQL(gamelogSql, params);
				} catch (Exception e) {
					LogUtil.print("++++++++++sql:"+gamelogSql+",params:"+ JSONObject.fromObject(params));
				}
			}
		}
	}
}
