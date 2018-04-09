package com.zhuoan.biz.model;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.model.zjh.ZJHGame;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import com.zhuoan.util.MathDelUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class RoomManage {
	/**
	 * 20180315
	 * 房间key
	 */
	public final static String ROOM_KEY_COMMEN="ROOM_KEY_COMMEN";

	/**
	 * 20180315
	 * 房间列表
	 */
	public static Map<String, GameRoom> gameRoomMap = new ConcurrentHashMap<String,GameRoom>();
	
	public static JSONArray result;
	public JSONObject gameset_sss =null;
	MaJiangBiz maJiangBiz = new MajiangBizImpl();

	/**
	 * 检查玩家是否在房间内
	 * 在返回房间号，不在返回空
	 * @param @param account
	 * @param @return   
	 * @return String  
	 * @throws
	 * @date 2018年3月15日
	 */
	public String checkPlayerInRoom(String account){
		for (String roomNo : gameRoomMap.keySet()) {
			GameRoom gameRoom = gameRoomMap.get(roomNo);
			if (gameRoom.getPlayerMap().containsKey(account)&&gameRoom.getPlayerMap().get(account)!=null) {
				return roomNo;
			}
		}
		return "";
	}

	/**
	 * 随机生成不重复的6位房间号
	 * @param @return   
	 * @return String  
	 * @throws
	 * @date 2018年3月15日
	 */
	public static String randomRoomNo(){
		String roomNo = MathDelUtil.getRandomStr(6);
		if (gameRoomMap.containsKey(roomNo)) {
			return randomRoomNo();
		}
		return roomNo;
	}

	/**
	 * gid 游戏id
	 * 获取所有房间列表
	 * @param @param gameId
	 * @param @return   
	 * @return JSONArray  
	 * @throws
	 * @date 2018年3月15日
	 */
	public void getAllRoomList(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		int gameId = fromObject.getInt("gid");
		JSONObject result1 = new JSONObject();
		JSONArray allRoom = new JSONArray();
		for (String roomNo : gameRoomMap.keySet()) {
			if (gameRoomMap.get(roomNo).getGid()==gameId&&gameRoomMap.get(roomNo).isIsopen()) {
				GameRoom gameRoom = gameRoomMap.get(roomNo);
				JSONObject obj = new JSONObject();
				obj.put("room_no", gameRoom.getRoomNo());
				obj.put("gid", gameId);
				for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
					obj.put("user_id"+i, gameRoom.getUserIdList().get(i));
				}
				obj.put("base_info", gameRoom.getRoomInfo());
				obj.put("fytype", gameRoom.getFytype());
				obj.put("iszs", 0);
				obj.put("player", gameRoom.getPlayerCount());
				int renshu = 0;
				for (long id : gameRoom.getUserIdList()) {
					if (id>0) {
						renshu++;
					}
				}
				obj.put("renshu", renshu);
				allRoom.add(obj);
			}
		}
		result1.element("gid", gameId);
		result1.element("code", 1);
		result1.element("array", allRoom);
		result1.element("sType", fromObject.get("sType"));
		client.sendEvent("getAllRoomListPush", result1);
	}


	/**
	 * 玩家准备
	 * @param @param account   
	 * @return void  
	 * @throws
	 * @date 2018年3月15日
	 */
	public void playerReady(String account){
		String roomNo = checkPlayerInRoom(account);
		if (!Dto.stringIsNULL(roomNo)) {
			//			GameRoom gameRoom = gameRoomMap.get(roomNo);
			//			gameRoom.getUserPacketMap().get(account).setReadyStatus(1);
			//			gameRoom.getUserPacketMap().get(account).setHalfStatus(0);
			//			int count = 0;
			//			for (String uid:gameRoom.getUserPacketMap().keySet()) {
			//				if(gameRoom.getUserPacketMap().get(uid).getReadyStatus()==1){
			//					count++;
			//				}
			//			}
			//			gameRoom.setReadyCount(count);
		}
	}

	/**
	 * account 玩家账号
	 * gid 游戏id
	 * base_info 房间信息{roomtype,player,open,type,yuanbao,leaveYB,qzTimes,qznozhuang,halfwayin,readyovertime,
	 * 				custFee,baseNum,roomtype,globalTimer,enterYB}
	 * roomtype 房间类型
	 * game_count 房间局数
	 * ip ip
	 * port 端口
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年3月15日
	 */
	public void createRoomNN(SocketIOClient client, Object data){

		JSONObject objInfo = JSONObject.fromObject(data);
		String uuid = objInfo.getString("account");
		JSONObject base_info = JSONObject.fromObject(objInfo.get("base_info"));
		// 设置客户端标识
		client.set(Constant.CLIENTTAG, uuid);

		JSONObject result=new JSONObject();
		JSONObject userInfo = new JSONObject();

		if(!client.has("userinfo")){

			//根据uuid获取用户信息
//			if (UserInfoCache.userInfoMap.containsKey(uuid)) {
//				userInfo = UserInfoCache.userInfoMap.get(uuid);
//			}else {
			userInfo=maJiangBiz.getUserInfoByAccount(uuid);
			if (UserInfoCache.userInfoMap.containsKey(uuid)) {
				userInfo.put("yuanbao",UserInfoCache.userInfoMap.get(uuid).getDouble("yuanbao"));
			}
			UserInfoCache.userInfoMap.put(uuid, userInfo);
					
			//验证
			//uuid不合法 返回提示信息
			if(Dto.isObjNull(userInfo)){
				result.put("code", 0);
				result.put("msg", "用户不存在");
				client.sendEvent("enterRoomPush_NN", result);
				return;
			}else{
				if(!userInfo.containsKey("headimg")){
					userInfo.element("headimg", "null");
				}
				client.set("userinfo", userInfo);
			}
			String string = objInfo.getString("uuid");
			if(!userInfo.getString("uuid").equals(string)) {
				result.put("code", 0);
				result.put("msg", "该帐号已在其他地方登录");
				LogUtil.print("接收到的uuid为:"+string+",玩家uuid为"+userInfo.getString("uuid"));
				client.sendEvent("enterRoomPush_NN", result);
				return;
			}
		}else{
			userInfo = client.get("userinfo");
		}

		result.put("code", 1);
		result.put("msg", "");

		if (!Dto.isObjNull(userInfo)) {
			// 判断玩家元宝是否足够
			if(userInfo.getDouble("yuanbao")<base_info.getDouble("enterYB")) {
				result.put("code", 0);
				result.put("msg", "您的元宝不足，请先充值");
				client.sendEvent("enterRoomPush_NN", result);
				return;
			}

			// 添加房间信息
			String roomNo = randomRoomNo();
			GameRoom gameRoom;
			switch (objInfo.getInt("gid")) {
			case 1:
				gameRoom = new NNGameRoom();
				break;
			case 4:
				gameRoom = new SSSGameRoom();
				break;
			case 6:
				gameRoom = new ZJHGame();
				break;
			case 10:
				gameRoom = new BDXGameRoom();
				break;
			default:
				gameRoom = new GameRoom();
				break;
			}
			gameRoom.setGid(objInfo.getInt("gid"));
			gameRoom.setPort(objInfo.getInt("port"));
			gameRoom.setIp(objInfo.getString("ip"));
			gameRoom.setRoomType(objInfo.getInt("roomtype"));
			gameRoom.setRoomNo(roomNo);
			
			if (objInfo.getInt("roomtype")==3&&objInfo.getInt("gid")==4) {
				gameRoom.setMaxplayer(8);
				base_info.element("maxplayer", 8);
			}

			gameRoom.setRoomInfo(base_info);
			gameRoom.setCreateTime(new Date().toString());
			int player = base_info.getInt("player");
			if (gameRoom.getGid()==4&&gameRoom.getRoomType()==3) {
				player = 8;
			}
			List<Long> idList = new ArrayList<Long>();
			for (int i = 0; i < player; i++) {
				if (i==0) {
					idList.add(userInfo.getLong("id"));
				}else {
					idList.add((long) 0);
				}
			}
			gameRoom.setUserIdList(idList);
			List<String> iconList = new ArrayList<String>();
			for (int i = 0; i < player; i++) {
				if (i==0) {
					iconList.add(userInfo.getString("headimg"));
				}else {
					iconList.add("");
				}
			}
			gameRoom.setUserIconList(iconList);
			List<String> nameList = new ArrayList<String>();
			for (int i = 0; i < player; i++) {
				if (i==0) {
					nameList.add(userInfo.getString("name"));
				}else {
					nameList.add("");
				}
			}
			gameRoom.setUserNameList(nameList);
			List<Integer> scoreList = new ArrayList<Integer>();
			for (int i = 0; i < player; i++) {
				if (i==0) {
					scoreList.add(userInfo.getInt("score"));
				}else {
					scoreList.add(0);
				}
			}
			gameRoom.setUserScoreList(scoreList);
			gameRoom.setGameCount(9999);
			if (base_info.getInt("open")==1) {
				gameRoom.setIsopen(true);
			}else {
				gameRoom.setIsopen(false);
			}
			gameRoom.setPlayerCount(player);//玩家人数
			//gameRoom.setZhuangType(base_info.getInt("type"));//定庄类型（房主霸庄、轮庄）
			gameRoom.setFangzhu(uuid);
			gameRoom.setZhuang(uuid);
			//底分设置
			if(base_info.containsKey("di")){
				gameRoom.setScore(base_info.getDouble("di"));
			}else{
				gameRoom.setScore(1);
			}
			if(base_info.containsKey("yuanbao")&&base_info.getDouble("yuanbao")>0){ // 元宝模式
				gameRoom.setScore(base_info.getDouble("yuanbao")); //底分
			}
			if(base_info.containsKey("leaveYB")&&base_info.getDouble("leaveYB")>0){ // 元宝模式
				//gameRoom.setGoldcoins(base_info.getDouble("leaveYB"));
			}

			// 金币、元宝扣服务费
			if((base_info.containsKey("fee")&&base_info.getInt("fee")==1)||objInfo.getInt("roomtype")==3){
				JSONObject gameSetting = maJiangBiz.getGameSetting();
				JSONObject roomFee = gameSetting.getJSONObject("pumpData");
				double fee = roomFee.getDouble("proportion")*gameRoom.getScore();
				// 服务费：费率x底注
				if(base_info.containsKey("custFee")){ // 自定义费率
					fee = base_info.getDouble("custFee")*gameRoom.getScore();
				}else{ // 统一费率
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
			gameRoom.setRoomType(objInfo.getInt("roomtype"));
			if(objInfo.containsKey("game_count")&&objInfo.getInt("game_count")>0){
				gameRoom.setGameCount(objInfo.getInt("game_count"));
			}else{
				gameRoom.setGameCount(10000000);
			}
			
			Playerinfo player1 = new Playerinfo();

			player1.setId(userInfo.getLong("id"));
			player1.setAccount(uuid);
			player1.setName(userInfo.getString("name"));
			player1.setUuid(client.getSessionId());
			player1.setMyIndex(0);
			if(objInfo.getInt("roomtype") == 1){ // 金币模式
				player1.setScore(userInfo.getDouble("coins"));
			}else if(objInfo.getInt("roomtype") == 3){ // 元宝模式
				player1.setScore(userInfo.getDouble("yuanbao"));
			}else{ // 房卡模式
				player1.setScore(0);
			}
			player1.setHeadimg(userInfo.getString("headimg"));
			player1.setSex(userInfo.getString("sex"));
			player1.setIp(userInfo.getString("ip"));
			if(userInfo.containsKey("sign")){
				player1.setSignature(userInfo.getString("sign"));
			}else{
				player1.setSignature("");
			}
			if(userInfo.containsKey("area")){
				player1.setArea(userInfo.getString("area"));
			}else{
				player1.setArea("");
			}
			int vip = userInfo.getInt("lv");
			if(vip>1){
				player1.setVip(vip-1);
			}else{
				player1.setVip(0);
			}
			player1.setStatus(Constant.ONLINE_STATUS_YES);
			// 保存用户坐标
			if(objInfo.containsKey("location")){
				player1.setLocation(objInfo.getString("location"));
			}
			// 设置幸运值
			if (userInfo.containsKey("luck")) {
				player1.setLuck(userInfo.getInt("luck"));
			}
			//ConcurrentMap<String,Playerinfo> users=new ConcurrentHashMap<String,Playerinfo>();
			//users.put(uuid, player1);
			//gameRoom.setPlayerMap(users);
			//ConcurrentMap<String, UserPacket> userPacketMap = new ConcurrentHashMap<String, UserPacket>();
			//userPacketMap.put(uuid, new UserPacket());
			//userPacketMap.get(uuid).setLuck(player1.getLuck());
			//gameRoom.setUserPacketMap(userPacketMap);
			CopyOnWriteArraySet<Long> userIDSet = new CopyOnWriteArraySet<Long>();
			userIDSet.add(player1.getId());
			//gameRoom.setUserIDSet(userIDSet);
			String wanfa = "";
			gameRoom.setFytype(wanfa);
			gameRoomMap.put(roomNo, gameRoom);


			JSONObject obj=new JSONObject();
			obj.put("code", 1);
			obj.put("data", new JSONObject().element("game_id", gameRoom.getGid()).element("room_no", gameRoom.getRoomNo()));
			client.sendEvent("createRoomNNPush", obj);
			int isopen = 0;
			if (gameRoom.isIsopen()) {
				isopen = 1;
			}
			String sql = "insert into za_gamerooms(roomtype,server_id,game_id,room_no,base_info,createtime,user_id0,user_icon0,user_name0,"
					+ "user_score0,ip,port,status,game_count,paytype,open) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
			Object[] params = new Object[] { gameRoom.getRoomType(),0, gameRoom.getGid(), roomNo,base_info.toString(), new Date(), userInfo.getLong("id"), userInfo.getString("headimg"),
					userInfo.getString("name"), userInfo.getInt("score"), gameRoom.getIp(), gameRoom.getPort(), 0, gameRoom.getGameCount(),
					gameRoom.getPaytype(),isopen};
			GameMain.sqlQueue.addSqlTask(new SqlModel(sql, params, SqlModel.EXECUTEUPDATEBYSQL));
			DBUtil.executeUpdateBySQL(sql, params);
			
		}
	}
	
	
	/**
	 * roomNo
	 * account
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年3月15日
	 */
	public void joinRoomNN(SocketIOClient client, Object data){
		JSONObject objInfo = JSONObject.fromObject(data);
		System.err.println(objInfo);
		String roomNo = objInfo.getString("roomNo");
		if (gameRoomMap.containsKey(roomNo)&&gameRoomMap.get(roomNo)!=null) {
			GameRoom gameRoom = gameRoomMap.get(roomNo);
			JSONObject obj=new JSONObject();
			obj.put("code", 1);
			obj.put("data", new JSONObject().element("game_id", gameRoom.getGid()).element("room_no", gameRoom.getRoomNo()));
			client.sendEvent("joinRoomNNPush", obj);
			
			String uuid = objInfo.getString("account");
			// 设置客户端标识
			client.set(Constant.CLIENTTAG, uuid);

			JSONObject result=new JSONObject();
			JSONObject userInfo = new JSONObject();

			if(!client.has("userinfo")){

				//根据uuid获取用户信息
				/*if (UserInfoCache.userInfoMap.containsKey(uuid)) {
					userInfo = UserInfoCache.userInfoMap.get(uuid);
				}else {
					userInfo=maJiangBiz.getUserInfoByAccount(uuid);
					UserInfoCache.userInfoMap.put(uuid, userInfo);
				}*/
				userInfo=maJiangBiz.getUserInfoByAccount(uuid);
				if (UserInfoCache.userInfoMap.containsKey(uuid)) {
					userInfo.put("yuanbao",UserInfoCache.userInfoMap.get(uuid).getDouble("yuanbao"));
				}
				UserInfoCache.userInfoMap.put(uuid, userInfo);
				//验证
				//uuid不合法 返回提示信息
				if(Dto.isObjNull(userInfo)){
					result.put("code", 0);
					result.put("msg", "用户不存在");
					client.sendEvent("enterRoomPush_NN", result);
					return;
				}else{
					if(!userInfo.containsKey("headimg")){
						userInfo.element("headimg", "null");
					}
					client.set("userinfo", userInfo);
					client.set("userAccount", uuid);
					client.set("account", uuid);
				}
				String string = objInfo.getString("uuid");
				if(!userInfo.getString("uuid").equals(string)) {
					result.put("code", 0);
					result.put("msg", "该帐号已在其他地方登录");
					LogUtil.print("接收到的uuid为:"+string+",玩家uuid为"+userInfo.getString("uuid"));
					client.sendEvent("enterRoomPush_NN", result);
					return;
				}
			}else{
				userInfo = client.get("userinfo");
			}

			result.put("code", 1);
			result.put("msg", "");

			if (!Dto.isObjNull(userInfo)) {
				int myIndex = -1;
				for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
					if (gameRoom.getUserIdList().get(i)==0) {
						myIndex = i;
						break;
					}
				}
				if (myIndex<0) {
					result.put("code", 0);
					result.put("msg", "加入房间失败");
					//client.sendEvent("enterRoomPush_NN", result);
					return;
				}
				
				JSONObject base_info = gameRoom.getRoomInfo();
				// 判断玩家元宝是否足够
				if(userInfo.getDouble("yuanbao")<base_info.getDouble("enterYB")) {
					result.put("code", 0);
					result.put("msg", "您的元宝不足，请先充值");
					client.sendEvent("enterRoomPush_NN", result);
					return;
				}
				
				gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
				gameRoom.getUserIconList().set(myIndex, userInfo.getString("headimg"));
				gameRoom.getUserNameList().set(myIndex, userInfo.getString("name"));
				gameRoom.getUserScoreList().set(myIndex, userInfo.getInt("score"));
				gameRoomMap.put(roomNo, gameRoom);
				String sql = "update za_gamerooms set user_id"+myIndex+"=?,user_icon"+myIndex+"=?,user_name"+myIndex+"=? where room_no=?";
				Object[] params = new Object[]{userInfo.getLong("id"),userInfo.getString("headimg"), userInfo.getString("name"),gameRoom.getRoomNo()};
				GameMain.sqlQueue.addSqlTask(new SqlModel(sql, params, SqlModel.EXECUTEUPDATEBYSQL));
			}
		}else {
			JSONObject obj=new JSONObject();
			obj.put("code", 0);
			client.sendEvent("joinRoomNNPush", obj);
		}
	}

	
	public void playerExit(String account,long userId,String roomNo){
		//String roomNo = checkPlayerInRoom(account);
		if (!Dto.stringIsNULL(roomNo)) {
			GameRoom gameRoom = gameRoomMap.get(roomNo);
			for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
				if (gameRoom.getUserIdList().get(i)==userId) {
					gameRoom.getUserIdList().set(i, (long)0);
					break;
				}
			}
		}
	}
	
	/**
	 * 测试可用
	 * gid 游戏id
	 * platform 平台号
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年3月16日
	 */
	public void getGameSetting(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		int gid = fromObject.getInt("gid");
		String platform = fromObject.getString("platform");
		if (!Dto.isNull(result)) {
			JSONArray array = new JSONArray();
			for (int i = 0; i < result.size(); i++) {
				JSONObject jsonObject = result.getJSONObject(i);
				if (jsonObject.getInt("is_use")==1) {
					if(jsonObject.containsKey("is_open") && jsonObject.getInt("is_open")==0){
						if (jsonObject.getInt("game_id")==gid&&jsonObject.getString("memo").equals(platform)) {
							array.add(jsonObject);
						}
					}
				}
			}
			JSONObject result1 = new JSONObject();
			result1.put("data", array);
			result1.put("code", 1);
			client.sendEvent("getGameSettingPush", result1);
		}
	}

	/**
	 * gid 游戏id
	 * uid 玩家id
	 * 判断玩家是否在房间内
	 * @param @param client
	 * @param @param data   
	 * @return void  
	 * @throws
	 * @date 2018年3月16日
	 */
	public void checkUser(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		int gid = fromObject.getInt("gid");
		long uid = fromObject.getLong("uid");
		JSONObject roominfo=new JSONObject();
		if (gid == -1) {
			for (String roomNo : gameRoomMap.keySet()) {
				GameRoom gameRoom = gameRoomMap.get(roomNo);
				if (gameRoom.getUserIdList().contains(uid)) {
					roominfo.element("port", gameRoom.getPort());
					roominfo.element("room_no", gameRoom.getRoomNo());
					roominfo.element("game_id", gameRoom.getGid());
					roominfo.element("ip", gameRoom.getIp());
				}
			}
		}else if (fromObject.containsKey("room_no")&&fromObject.getString("room_no")!=null) {
			if (gameRoomMap.get(fromObject.getString("room_no"))!=null) {
				GameRoom gameRoom = gameRoomMap.get(fromObject.getString("room_no"));
				if (gameRoom.getUserIdList().contains(uid)) {
					roominfo.element("port", gameRoom.getPort());
					roominfo.element("room_no", gameRoom.getRoomNo());
					roominfo.element("game_id", gameRoom.getGid());
					roominfo.element("ip", gameRoom.getIp());
				}
			}
		}else {
			for (String roomNo : gameRoomMap.keySet()) {
				GameRoom gameRoom = gameRoomMap.get(roomNo);
				if (gameRoom.getGid()==gid&&gameRoom.getUserIdList().contains(uid)) {
					roominfo.element("port", gameRoom.getPort());
					roominfo.element("room_no", gameRoom.getRoomNo());
					roominfo.element("game_id", gameRoom.getGid());
					roominfo.element("ip", gameRoom.getIp());
				}
			}
		}
		JSONObject result1 = new JSONObject();
		result1.put("code", 1);
		result1.put("msg", "");
		result1.put("data", "");
		
		// 若玩家已加入房间则返回房间信息，否则需要加入或创建新的房间
		if (!Dto.isObjNull(roominfo)) {
			result1.put("data", roominfo);
		}
		client.sendEvent("checkUserPush", result1);
	}
	
	public static void lock(String roomNo){
		if (gameRoomMap.containsKey(roomNo)&&gameRoomMap.get(roomNo)!=null) {
			gameRoomMap.get(roomNo).lock();
		}
	}
	
	public static void unLock(String roomNo){
		//m_locker.unlock();
		try {
			if (gameRoomMap.containsKey(roomNo)&&gameRoomMap.get(roomNo)!=null) {
				gameRoomMap.get(roomNo).unlock();
			}
		} catch (Exception e) {
			LogUtil.print("----------解锁异常----------");
		}
	}
	
	public static void lock1(){
		//m_locker.lock();
	}
	
	public static void unLock1(){
		//m_locker.unlock();
	}
}
