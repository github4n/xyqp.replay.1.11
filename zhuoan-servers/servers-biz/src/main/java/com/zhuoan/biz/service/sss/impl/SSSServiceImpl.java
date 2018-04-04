package com.zhuoan.biz.service.sss.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.constant.Constant;
import com.zhuoan.biz.constant.NewConstant;
import com.zhuoan.biz.core.sss.*;
import com.zhuoan.biz.event.GameMain;
import com.zhuoan.biz.event.sss.SSSGameRules;
import com.zhuoan.biz.model.Player;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.biz.queue.Messages;
import com.zhuoan.biz.queue.SqlModel;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.times.TimerMsgData;
import com.zhuoan.biz.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

// 业务逻辑处理
public class SSSServiceImpl {
	
	SSSGameRules sssGameRules = new SSSGameRules();
	MaJiangBiz mjBiz = new MajiangBizImpl();
	public JSONObject gameset =null;

	// 创建房间
	public void createRoom(SocketIOClient client, Object data){
		JSONObject objInfo = JSONObject.fromObject(data);
		String account = objInfo.getString("account");
		JSONObject base_info = JSONObject.fromObject(objInfo.get("base_info"));
		// 设置客户端标识
		client.set(NewConstant.CLIENTTAG, account);
		JSONObject result=new JSONObject();
		JSONObject userInfo = new JSONObject();

		// 验证用户信息
		if(!client.has("userinfo")){
			//根据uuid获取用户信息
			userInfo=mjBiz.getUserInfoByAccount(account);
			if (UserInfoCache.userInfoMap.containsKey(account)) {
				userInfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
			}
			// 存入缓存
			UserInfoCache.userInfoMap.put(account, userInfo);
			//uuid不合法 返回提示信息
			if(Dto.isObjNull(userInfo)){
				result.put("code", 0);
				result.put("msg", "用户不存在");
				client.sendEvent("enterRoomPush_SSS", result);
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
				client.sendEvent("enterRoomPush_SSS", result);
				return;
			}
		}else{
			userInfo = client.get("userinfo");
		}
		
		result.put("code", 1);
		result.put("msg", "");
		
		if (Dto.isObjNull(userInfo)) {
			return;
		}
		// 判断玩家元宝是否足够
		if(userInfo.getDouble("yuanbao")<base_info.getDouble("enterYB")) {
			result.put("code", 0);
			result.put("msg", "您的元宝不足，请先充值");
			client.sendEvent("enterRoomPush_NN", result);
			return;
		}
		// 创建房间实体对象
		SSSGameRoom gameRoom = createRoomSSS(objInfo, userInfo, client.getSessionId());
		// 设置客户端标识
		client.set(NewConstant.ROOMNO, gameRoom.getRoomNo());
		// 组织数据，通知玩家
		JSONObject obj = getEnterRoomData(gameRoom,account);
		obj.put("users",gameRoom.getAllPlayer());//告诉他原先加入的玩家
		obj.put("myIndex",gameRoom.getPlayerMap().get(account).getMyIndex());// 座位号
		obj.put("isReady",gameRoom.getReadyIndex());// 已准备玩家
		result.put("data", obj);
		client.sendEvent("enterRoomPush_SSS", result);
	}
	
	public JSONObject getEnterRoomData(SSSGameRoom gameRoom, String account){
		// 房间属性信息
		JSONObject roomInfo = gameRoom.getRoomInfo();
		JSONObject obj=new JSONObject();
		obj.put("room_no", gameRoom.getRoomNo());
		obj.put("roomType", gameRoom.getRoomType());
		if(gameRoom.getRoomType()==0 || gameRoom.getRoomType()==2){ // 房卡模式
			obj.put("roomType", 0);
			obj.put("game_count", gameRoom.getGameCount());
			obj.put("game_index", gameRoom.getGameIndex()+1);
		}
		String tu="";
		if (roomInfo.containsKey("turn")) {
			tu=roomInfo.getInt("turn")+"局/";
		}
		String ys="";
		if (roomInfo.containsKey("color")) {
			if (roomInfo.getInt("color")==0) {
				ys="不加色/";
			}else if (roomInfo.getInt("color")==1) {
				ys="加一色/";
			}else if (roomInfo.getInt("color")==2) {
				ys="加两色/";
			}

		}
		String jm="";
		if (roomInfo.containsKey("jiama")) {

			if (roomInfo.getInt("jiama")==0) {
				jm="不加/";
			}else if (roomInfo.getInt("jiama")==1) {
				jm="随机/";
			}else {
				jm="加码/";
			}
		}
		String pt="";
		if (roomInfo.containsKey("paytype")) {
			if (roomInfo.getInt("paytype")==0) {
				pt="房主支付/";
			}else if (roomInfo.getInt("paytype")==1) {
				pt="房卡AA/";
			}

		}
		String dz="";
		if (roomInfo.containsKey("yuanbao")) {
			dz="底:"+roomInfo.getString("yuanbao")+"/";
		}
		String rc="";
		if (roomInfo.containsKey("enterYB")) {
			rc="入:"+roomInfo.getString("enterYB")+"/";
		}
		String lc="";
		if (roomInfo.containsKey("leaveYB")) {
			lc="离:"+roomInfo.getString("leaveYB")+"/";
		}
		String rs="";
		if (gameRoom.getRoomInfo().containsKey("player")) {
			rs=gameRoom.getRoomInfo().getInt("player")+"人/";
		}
		obj.put("player",rs);// 人数
		obj.put("type",gameRoom.getFytype());// 玩法
		obj.put("turn",tu);// 房卡型
		obj.put("ys",ys);// 加色
		obj.put("ma",jm);// 马牌
		obj.put("pt",pt);// 房卡付款类型					
		obj.put("dz",dz);// 底注				
		obj.put("rc",rc);// 入场					
		obj.put("lc",lc);// 离场
		return obj;
	}
	
	// 创建房间实体对象
	public SSSGameRoom createRoomSSS(JSONObject objInfo, JSONObject userInfo, UUID uuid){
		String roomNo = RoomManage.randomRoomNo();
		JSONObject base_info = JSONObject.fromObject(objInfo.get("base_info"));
		SSSGameRoom room = new SSSGameRoom();
		room.setRoomNo(roomNo);// 房间号
		room.setGid(objInfo.getInt("gid"));// 游戏id
		room.setPort(objInfo.getInt("port"));// 端口
		room.setIp(objInfo.getString("ip"));// ip
		room.setRoomType(objInfo.getInt("roomtype"));// 房间类型
		int roomSize = base_info.getInt("player");
		if (objInfo.getInt("roomtype")==3) {
			room.setMaxplayer(8);// 元宝场最多8人
			roomSize = 8;
			base_info.element("maxplayer", 8);
		}
		room.setRoomInfo(base_info);// 房间信息
		room.setCreateTime(new Date().toString());// 创建时间
		List<Long> idList = new ArrayList<Long>();
		List<String> iconList = new ArrayList<String>();
		List<String> nameList = new ArrayList<String>();
		List<Integer> scoreList = new ArrayList<Integer>();
		for (int i = 0; i < roomSize; i++) {
			if (i==0) {
				// 将当前玩家放在座位号为0的位置
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
		room.setUserIdList(idList);// 玩家id
		room.setUserIconList(iconList);// 玩家头像
		room.setUserNameList(nameList);// 玩家昵称
		room.setUserScoreList(scoreList);// 玩家积分
		// 是否陌生人可见
		if (base_info.getInt("open")==1) {
			room.setIsopen(true);
		}else {
			room.setIsopen(false);
		}
		room.setPlayerCount(base_info.getInt("player"));//玩家人数
		// 游戏设置为空，从数据库获取
		if (gameset==null) {
			gameset = mjBiz.getGameInfoByID(4);
		}
		room.setSetting(gameset.getJSONObject("setting"));// 游戏设置
		if (room.getRoomType()==3) {
			room.setGameCount(999);// 游戏局数
			room.setMaxplayer(gameset.getJSONObject("setting").getInt("maxplayer"));//最大人数
			room.setPeipaiTime(gameset.getJSONObject("setting").getInt("goldpeipai"));
			// 金币、元宝扣服务费
			JSONObject gameSetting = mjBiz.getGameSetting();
			JSONObject roomFee = gameSetting.getJSONObject("pumpData");
			// 服务费：费率x底注
			double fee = roomFee.getDouble("proportion")*room.getScore();
			double maxFee = roomFee.getDouble("max");
			double minFee = roomFee.getDouble("min");
			if(fee>maxFee){
				fee = maxFee;
			}else if(fee<minFee){
				fee = minFee;
			}
			fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			room.setFee(fee);
		}else{
			room.setGameCount(base_info.getJSONObject("turn").getInt("turn"));
			room.setMaxplayer(objInfo.getInt("player"));//最大人数
		}
		room.setFangzhu(userInfo.getString("account"));// 房主
		room.setZhuang(userInfo.getString("account"));// 庄家
		if (base_info.containsKey("color")) {
			room.setColor(base_info.getInt("color"));//加色
		}else{
			room.setColor(0);//加色
		}
		if (base_info.containsKey("type")) {
			room.setGameType(base_info.getInt("type"));//游戏模式
			if (base_info.getInt("type")==0) {
				room.setFytype("互比");
			}else if (base_info.getInt("type")==1) {
				room.setFytype("霸王庄");
			}
		}
		if (base_info.containsKey("jiama")) {
			room.setMaPaiType(base_info.getInt("jiama"));//马牌模式
		}else{
			room.setMaPaiType(0);//马牌模式
		}

		//马牌 1黑桃，2红桃，3梅花，4方块
		String c="0-0";
		if (room.getRoomType()!=1) {
			if (room.getMaPaiType()==1) {
				int randomNumber = (int)Math.round(Math.random()*12+1);
				c="1-"+randomNumber;
			}else if (room.getMaPaiType()==2){
				c="1-1";
			}else if (room.getMaPaiType()==3){
				c="1-5";
			}else if (room.getMaPaiType()==4){
				c="1-10";
			}
		}
		room.setMaPai(c);
		Map<String,Playerinfo> playerMap=new ConcurrentHashMap<String, Playerinfo>();
		// 用户信息
		Playerinfo playerinfo = new Playerinfo();
		playerinfo.setId(userInfo.getLong("id"));// id
		playerinfo.setAccount(userInfo.getString("account"));// 账号
		playerinfo.setName(userInfo.getString("name"));// 昵称
		playerinfo.setUuid(uuid);// uuid
		playerinfo.setMyIndex(0);// 创建房间座位号为0
		playerinfo.setSex(userInfo.getString("sex"));
		playerinfo.setIp(userInfo.getString("ip"));
		if(room.getRoomType() == 1){
			if (room.getRoomInfo().getInt("level")!=-1) {
				//普通金币场
				playerinfo.setScore(userInfo.getInt("coins"));
			}else{
				//竞技场
				playerinfo.setScore(userInfo.getInt("score"));
			}
		}else if(room.getRoomType() == 3){
			playerinfo.setScore(userInfo.getDouble("yuanbao"));
		}else{
			playerinfo.setScore(0);
		}
		playerinfo.setHeadimg(userInfo.getString("headimg"));
		playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
		// 设置幸运值
		if(userInfo.containsKey("luck")){
			playerinfo.setLuck(userInfo.getInt("luck"));
		}
		// 保存用户坐标
		if(objInfo.containsKey("location")){
			playerinfo.setLocation(objInfo.getString("location"));
		}
		playerMap.put(userInfo.getString("account"), playerinfo);
		room.setPlayerMap(playerMap);
		// 用户牌局信息
		Map<String, Player> playerPaiJu = new ConcurrentHashMap<String, Player>();
		Player player=new Player();
		player.setStatus(NewConstant.USERSTATUS_SSS_HALFWAY);
		if (room.getRoomType()==1) {
			player.setTotalScore(playerinfo.getScore());
			player.setGameNum(0);
			room.setScore(objInfo.getInt("goldcoins")); //底分 金币场
			room.setMinscore(objInfo.getInt("mingoldcoins"));
			room.setLevel(objInfo.getInt("level"));
		}else if(room.getRoomType()==3){
			player.setTotalScore(playerinfo.getScore());
			room.setScore(base_info.getInt("yuanbao")); //底分 元宝场
			room.setMinscore(base_info.getInt("leaveYB"));//离场分数 
		}else{
			room.setScore(1); //底分 房卡场
			room.setMinscore(1);
		}
		playerPaiJu.put(playerinfo.getAccount(), player);
		room.setPlayerPaiJu(playerPaiJu);
		RoomManage.gameRoomMap.put(room.getRoomNo(), room);
		int isopen = 0;
		if (room.isIsopen()) {
			isopen = 1;
		}
		String sql = "insert into za_gamerooms(roomtype,server_id,game_id,room_no,base_info,createtime,user_id0,user_icon0,user_name0,"
				+ "user_score0,ip,port,status,game_count,paytype,open) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Object[] params = new Object[] { room.getRoomType(),0, room.getGid(), roomNo,base_info.toString(), new Date(), userInfo.getLong("id"), userInfo.getString("headimg"),
				userInfo.getString("name"), userInfo.getInt("score"), room.getIp(), room.getPort(), 0, room.getGameCount(),
				room.getPaytype(),isopen};
		GameMain.sqlQueue.addSqlTask(new SqlModel(sql, params, SqlModel.EXECUTEUPDATEBYSQL));
		return room;
	}
	
	public boolean joinRoomSSS(int myIndex, JSONObject objInfo, SSSGameRoom gameRoom, JSONObject userInfo, UUID uuid){
		boolean isReconnect = false;
		if (gameRoom.getUserIdList().contains(userInfo.getLong("id"))) {
			isReconnect = true;
		}
		// 存入相应的座位号
		gameRoom.getUserIdList().set(myIndex, userInfo.getLong("id"));
		gameRoom.getUserIconList().set(myIndex, userInfo.getString("headimg"));
		gameRoom.getUserNameList().set(myIndex, userInfo.getString("name"));
		gameRoom.getUserScoreList().set(myIndex, userInfo.getInt("score"));
		// 玩家信息
		Playerinfo playerinfo = new Playerinfo();
		playerinfo.setId(userInfo.getLong("id"));
		playerinfo.setAccount(userInfo.getString("account"));
		playerinfo.setName(userInfo.getString("name"));
		playerinfo.setUuid(uuid);
		playerinfo.setMyIndex(myIndex);
		playerinfo.setSex(userInfo.getString("sex"));
		playerinfo.setIp(userInfo.getString("ip"));
		if(gameRoom.getRoomType() == 1){
			if (gameRoom.getRoomInfo().getInt("level")!=-1) {
				//普通金币场
				playerinfo.setScore(userInfo.getInt("coins"));
			}else{
				//竞技场
				playerinfo.setScore(userInfo.getInt("score"));
			}
		}else if(gameRoom.getRoomType() == 3){
			playerinfo.setScore(userInfo.getDouble("yuanbao"));
		} else{
			playerinfo.setScore(0);
		}
		playerinfo.setHeadimg(userInfo.getString("headimg"));
		playerinfo.setStatus(Constant.ONLINE_STATUS_YES);
		// 保存用户坐标
		if(objInfo.containsKey("location")){
			playerinfo.setLocation(objInfo.getString("location"));
		}
		// 设置幸运值
		if(userInfo.containsKey("luck")){
			playerinfo.setLuck(userInfo.getInt("luck"));
		}
		gameRoom.getPlayerMap().put(userInfo.getString("account"), playerinfo);
		Player player=new Player();
		player.setIsReady(0);
		player.setIsReady(NewConstant.USERSTATUS_SSS_HALFWAY);
		player.setGameNum(0);
		if (gameRoom.getRoomType()==1||gameRoom.getRoomType()==3) {
			player.setTotalScore(playerinfo.getScore());
		}
		gameRoom.getPlayerPaiJu().put(playerinfo.getAccount(), player);
		RoomManage.gameRoomMap.put(gameRoom.getRoomNo(), gameRoom);
		String sql = "update za_gamerooms set user_id"+myIndex+"=?,user_icon"+myIndex+"=?,user_name"+myIndex+"=? where room_no=?";
		Object[] params = new Object[]{userInfo.getLong("id"),userInfo.getString("headimg"), userInfo.getString("name"),gameRoom.getRoomNo()};
		GameMain.sqlQueue.addSqlTask(new SqlModel(sql, params, SqlModel.EXECUTEUPDATEBYSQL));
		return isReconnect;
	}
	
	// 加入房间
	public void joinRoom(SocketIOClient client, Object data){
		JSONObject objInfo = JSONObject.fromObject(data);
		String account = objInfo.getString("account");
		String roomNo = objInfo.getString("roomNo");
		// 设置客户端标识
		client.set(NewConstant.CLIENTTAG, account);
		JSONObject result=new JSONObject();
		JSONObject userInfo = new JSONObject();

		// 验证用户信息
		if(!client.has("userinfo")){
			//根据uuid获取用户信息
			userInfo=mjBiz.getUserInfoByAccount(account);
			if (UserInfoCache.userInfoMap.containsKey(account)) {
				userInfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
			}
			// 存入缓存
			UserInfoCache.userInfoMap.put(account, userInfo);
			//uuid不合法 返回提示信息
			if(Dto.isObjNull(userInfo)){
				result.put("code", 0);
				result.put("msg", "用户不存在");
				client.sendEvent("enterRoomPush_SSS", result);
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
				client.sendEvent("enterRoomPush_SSS", result);
				return;
			}
		}else{
			userInfo = client.get("userinfo");
		}
		SSSGameRoom gameRoom = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (gameRoom==null) {
			result.put("code", 0);
			result.put("msg", "房间不存在");
			client.sendEvent("enterRoomPush_SSS", result);
			return;
		}
		// 判断玩家元宝是否足够
		if(userInfo.getDouble("yuanbao")<gameRoom.getRoomInfo().getDouble("enterYB")) {
			result.put("code", 0);
			result.put("msg", "您的元宝不足，请先充值");
			client.sendEvent("enterRoomPush_NN", result);
			return;
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
			if(myIndex<0){
				result.put("code", 0);
				result.put("msg", "用户加入错误");
				client.sendEvent("enterRoomPush_SSS", result);
				RoomManage.unLock(roomNo);
				return;
			}
			boolean isReconnect = joinRoomSSS(myIndex, objInfo, gameRoom, userInfo, client.getSessionId());
			// 设置客户端标识
			client.set(NewConstant.ROOMNO, gameRoom.getRoomNo());
			// 组织数据，通知玩家
			JSONObject obj = getEnterRoomData(gameRoom,account);
			obj.put("users",gameRoom.getAllPlayer());//告诉他原先加入的玩家
			obj.put("myIndex", myIndex);
			obj.put("isReady",gameRoom.getReadyIndex());
			obj.put("readyTime", gameRoom.getTimeLeft());
			// 配牌或结算阶段
			if (gameRoom.getGameStatus()>NewConstant.GAMESTATUS_SSS_XIPAI) {
				JSONArray chu=new JSONArray();
				for (String uuid : gameRoom.getPlayerPaiJu().keySet()) {
					// 已配牌玩家
					if(gameRoom.getPlayerPaiJu().get(uuid).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){
						chu.add(gameRoom.getPlayerIndex(uuid));
					}
				}
				obj.put("chupai", chu);
			}
			// 游戏状态
			obj.put("gameStatus",gameRoom.getGameStatus());
			String a=gameRoom.getMaPai();
			String[] val = a.split("-");
			int num = 0;
			if(val[0].equals("2")){
				num = 20;
			}else if(val[0].equals("3")){
				num = 40;
			}else if(val[0].equals("4")){
				num = 60;
			}
			int ma = Integer.valueOf(val[1]) + num;
			obj.put("mapai",ma);// 马牌
			
			result.put("data", obj);
			client.sendEvent("enterRoomPush_SSS", result);
			// 重连不通知其他玩家
			if (!isReconnect) {
				Playerinfo player = gameRoom.getPlayerMap().get(account);
				JSONObject playerObj=new JSONObject();
				playerObj.put("name",player.getName());
				playerObj.put("headimg",player.getRealHeadimg());
				playerObj.put("id",player.getAccount());
				playerObj.put("sex",player.getSex());
				playerObj.put("ip", player.getIp());
				playerObj.put("location", player.getLocation());
				playerObj.put("score",player.getScore());
				playerObj.put("index",player.getMyIndex());
				playerObj.put("sex", player.getSex());
				playerObj.put("status", player.getStatus());
				playerObj.put("id", player.getAccount());
				obj.put("user", playerObj);
				result.put("data", obj);
				for (String string : gameRoom.getPlayerMap().keySet()) {
					SocketIOClient clientother=GameMain.server.getClient(gameRoom.getPlayerMap().get(string).getUuid());
					if(clientother!=null){
						clientother.sendEvent("playerEnterPush_SSS", result);
					}
				}
			}
		}
	}
	
	// 玩家准备
	public void playerReady(String account,SSSGameRoom room){
		room.playerReady(account);
		JSONObject result1 = new JSONObject();
		result1.put("myIndex", room.getPlayerIndex(account));
		result1.put("isReady", room.getReadyIndex());
		result1.put("users",room.getAllPlayer());
		// 准备人数达到最低开始人数开启定时器
		if (room.getPlayerCount()==room.getNowReadyCount()) {
			TimerMsgData tmd=new TimerMsgData();
			tmd.nTimeLimit=room.getSetting().getInt("goldready");
			tmd.nType=12;
			tmd.roomid=room.getRoomNo();
			tmd.client=null;
			tmd.data=new JSONObject().element("room_no", room.getRoomNo());
			tmd.gid=4;
			tmd.gmd= new Messages(null, new JSONObject().element("room_no", room.getRoomNo()), 4, 12);
			GameMain.singleTime.createTimer(tmd);
			// 通知前端展示倒计时
			result1.put("readyTime", room.getSetting().getInt("goldready"));
		}
		for (String uuid : room.getPlayerMap().keySet()) {

			Playerinfo pi=room.getPlayerMap().get(uuid);
			result1.put("isYuanBao", 1);
			SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
			if(clientother!=null){
				clientother.sendEvent("playerReadyPush_SSS", result1);
			}
		}
		
		// 全部准备开始游戏
		if (room.checkIsAllReady()) {
			startGame(room);
		}
	}
	
	// 开始游戏
	public void startGame(SSSGameRoom room){
		// 人数不够无法开始
		if (room.getPlayerPaiJu().size()<room.getPlayerCount()) {
			return;
		}
		
		// 非准备阶段不做处理
		if (room.getGameStatus()!=NewConstant.GAMESTATUS_SSS_READY) {
			return;
		}
		// 定庄
		sssGameRules.dingZhuang(room, NewConstant.ZHUANGTYPE_SSS_FANGZHU);
		// 洗牌
		sssGameRules.xiPai(room);
		// 发牌
		sssGameRules.faPai(room);
		
		// 开启配牌定时器
		TimerMsgData tmd=new TimerMsgData();
		tmd.nTimeLimit=room.getSetting().getInt("goldpeipai");
		tmd.nType=13;
		tmd.roomid=room.getRoomNo();
		tmd.client=null;
		tmd.data=new JSONObject().element("room_no", room.getRoomNo());
		tmd.gid=4;
		tmd.gmd= new Messages(null, new JSONObject().element("room_no", room.getRoomNo()), 4, 13);
		GameMain.singleTime.createTimer(tmd);
		
		String a=room.getMaPai();
		String[] val = a.split("-");
		int num = 0;
		if(val[0].equals("2")){
			num = 20;
		}else if(val[0].equals("3")){
			num = 40;
		}else if(val[0].equals("4")){
			num = 60;
		}
		int ma = Integer.valueOf(val[1]) + num;
		
		
		JSONArray userIds=new JSONArray();
		// 通知玩家
		for (String uc : room.getPlayerMap().keySet()) {
			
			JSONObject result = new JSONObject();
			
			String[] p=SSSGameRoom.daxiao(room.getPlayerPaiJu().get(uc).getPai());
			//重新排序插入数据
			room.getPlayerPaiJu().get(uc).setPai(p);
			 
			int[] pai = room.getPlayerPaiJu().get(uc).getMyPai();
			result.put("myPai", pai);
			result.put("maPai", ma);
			result.put("myIndex", room.getPlayerIndex(uc));
			if (room.getGameStatus()==1) {
				result.put("zhuang", room.getPlayerIndex(room.getZhuang()));
			}
			result.put("game_index", room.getGameIndex()+1);
			result.put("score", room.getFee());//抽水分
			double to=room.getPlayerPaiJu().get(uc).getTotalScore()-room.getFee();
			to = new BigDecimal(to).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			room.getPlayerPaiJu().get(uc).setTotalScore(to);//扣水
			result.put("totalscore", room.getPlayerPaiJu().get(uc).getTotalScore());//总分
			
			if (room.getRoomType()==0||room.getRoomType()==2) {
				result.put("peipaiTime", room.getSetting().getInt("fangkapeipai"));
			}else{
				result.put("peipaiTime", room.getSetting().getInt("goldpeipai"));
			}
			result.put("myPaiType", SSSSpecialCards.isSpecialCards(room.getPlayerPaiJu().get(uc).getPai(),room.getSetting()));
			
			Playerinfo pi=room.getPlayerMap().get(uc);
			
			userIds.add(pi.getId());
			if (!room.getRobotList().contains(uc)) {
				SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
				if(clientother!=null){
					clientother.sendEvent("gameStartPush_SSS", result);
				}
			}
		}
	}	
	
	// 配牌
	public void peiPai(SSSGameRoom room, String account, JSONObject jsonObject){
		int type = jsonObject.getInt("type");
		Player player = room.getPlayerPaiJu().get(account);
		boolean success = false;
		switch (type) {
		case NewConstant.PEIPAITYPE_SSS_AUTO:
			//最优排牌型
			player.setPai(SSSOrdinaryCards.sort(player.getPai()));
			player.setIsAuto(NewConstant.PEIPAITYPE_SSS_AUTO);
			success = true;
			break;
		case NewConstant.PEIPAITYPE_SSS_COMMEN:
			JSONArray myPai = jsonObject.getJSONArray("myPai");
			int[] myPai2 = player.getMyPai();
			// 数据不正确直接返回
			for (int i = 0; i < myPai2.length; i++) {
				if (!myPai.contains(myPai2[2])) {
					return;
				}
			}
			JSONArray t= SSSComputeCards.judge(player.togetMyPai(myPai));
			if ("倒水".equals(t.get(0))) {
				String[] you= SSSOrdinaryCards.sort(player.getPai());
				player.setPai(you);
			}else{
				String[] str=new String[13];
				for (int i = 0; i < myPai.size(); i++) {
					if (myPai.getInt(i)<20) {
						String a="1-"+myPai.getString(i);
						str[i]=a;
					}else if(myPai.getInt(i)>20&&myPai.getInt(i)<40){
						String a="2-"+(myPai.getInt(i)-20);
						str[i]=a;
					}else if(myPai.getInt(i)>40&&myPai.getInt(i)<60){
						String a="3-"+(myPai.getInt(i)-40);
						str[i]=a;
					}else if(myPai.getInt(i)>60){
						String a="4-"+(myPai.getInt(i)-60);
						str[i]=a;
					}
				}
				str=SSSGameRoom.AppointSort(str, 0, 2);
				
				str=SSSGameRoom.AppointSort(str, 3, 7);
				
				str=SSSGameRoom.AppointSort(str, 8, 12);
				player.setPai(str);
				player.setIsAuto(NewConstant.PEIPAITYPE_SSS_COMMEN);
				success = true;
			}
			break;
		case NewConstant.PEIPAITYPE_SSS_SPECIAL:
			int sc= SSSSpecialCards.isSpecialCards(player.getPai(),room.getSetting());
			String[] you= SSSSpecialCardSort.CardSort(player.getPai(),sc);
			player.setPai(you);
			player.setIsAuto(NewConstant.PEIPAITYPE_SSS_SPECIAL);
			success = true;
			break;
		default:
			break;
		}
		if (success) {
			room.playerPeipai(account);
			// 通知玩家
			JSONObject result = new JSONObject();
			result.put("type", 1);
			result.put("myIndex", room.getPlayerIndex(account));
			for (String uid  : room.getPlayerPaiJu().keySet()) {
				Playerinfo pi=room.getPlayerMap().get(uid);
				SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
				if(clientother!=null){
					clientother.sendEvent("gameActionPush_SSS", result);
				}
			}
			if (room.checkIsAllPeipai()) {
				// 设置为配牌状态
				room.setGameStatus(NewConstant.GAMESTATUS_SSS_PEIPAI);
				jiesuan(room);
			}
		}
	}
	
	// 结算
	public void jiesuan(SSSGameRoom room){
		room.jieSuan();
		JSONObject obj = new JSONObject();
		obj.put("type", 2);
		// 马牌
		String[] val = room.getMaPai().split("-");
		int num = 0;
		if(val[0].equals("2")){
			num = 20;
		}else if(val[0].equals("3")){
			num = 40;
		}else if(val[0].equals("4")){
			num = 60;
		}
		int ma = Integer.valueOf(val[1]) + num;
		obj.put("mapai", ma);
		// 结算数据
		JSONArray data = new JSONArray();
		JSONArray showIndex = new JSONArray();
		switch (room.getGameType()) {
		case NewConstant.GAMETYPE_SSS_HUBI:
			data = sssGameRules.jieSuan_HB(room);
			showIndex = sssGameRules.getShowIndex(data);
			break;
		default:
			break;
		}
		obj.put("data",data);
		obj.put("jiesuan",1);//结算类型 1 小结  2 大结
		obj.put("isma",1);//此局是否存在马牌
		if (ma==0) {
			obj.put("isma",0);//此局是否存在马牌
		}
		obj.put("showIndex",showIndex);
		obj.put("gameIndex",room.getGameIndex()+1);
		// 通知玩家
		for (String account : room.getPlayerPaiJu().keySet()) {
			SocketIOClient clientother=GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
			if(clientother!=null){
				clientother.sendEvent("gameActionPush_SSS", obj);
			}
		}
		// 结算用户金币，元宝
		switch (room.getRoomType()) {
		case 3:
			ybJieSuan(room);
			break;

		default:
			break;
		}
	}
	
	// 结算元宝
	public void ybJieSuan(SSSGameRoom room){
		List<String> list = new ArrayList<String>();
		JSONArray uid = new JSONArray();
		// 获取所有参与玩家
		for (String account : room.getPlayerPaiJu().keySet()) {
			if (room.getPlayerPaiJu().get(account).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI) {
				list.add(account);
				uid.add(room.getPlayerMap().get(account).getId());
			}
		}
		//元宝结算
		JSONArray ar=new JSONArray();
		for (String account : list) {
			JSONObject uobj=new JSONObject();
			Player uu = room.getPlayerPaiJu().get(account);
			uobj.element("id", room.getPlayerMap().get(account).getId());
			uobj.element("fen", uu.getScore());
			uobj.element("total", uu.getTotalScore());
			ar.add(uobj);
			// 更新缓存
			UserInfoCache.updateUserScore(account, uu.getScore(), 3);
			UserInfoCache.updateUserScore(account, -room.getFee(), 3);
		}
		mjBiz.updateUser(ar, "yuanbao");

		JSONObject duction=new JSONObject();
		duction.element("user", ar);
		duction.element("gid", 4);
		duction.element("type", 3);
		duction.element("roomNo", room.getRoomNo());
		mjBiz.insertUserdeduction(duction);
		
		// 玩家扣服务费
		if(room.getFee()>0){
			GameMain.sqlQueue.addSqlTask(new SqlModel(4, uid, room.getRoomNo(), 4, room.getFee(), "yuanbao"));
		}
	}
	
	// 退出房间
	public void exitRoom(SSSGameRoom room, String account){
		Player player = room.getPlayerPaiJu().get(account);
		boolean canExit = false;
		if (player.getStatus()==NewConstant.USERSTATUS_SSS_HALFWAY) {
			canExit = true;
		}else if (room.getGameStatus()==NewConstant.GAMESTATUS_SSS_READY) {
			canExit = true;
		}
		JSONObject result = new JSONObject();
		result.put("type", 2); //退出房间
		result.put("index", room.getPlayerIndex(account));
		if (!canExit) {
			SocketIOClient askclient=GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
			result.put("ISexit", 1);
			askclient.sendEvent("exitRoomPush_SSS", result);
			return;
		}
		result.put("ISexit", 0);
		
		// 没有准备的玩家退出不重新检查定时器
		if (player.getIsReady()==NewConstant.USERSTATUS_SSS_READY) {
			if (room.getNowReadyCount()-1<room.getPlayerCount()) {
				GameMain.singleTime.deleteTimer(room.getRoomNo());
				// 通知前端隐藏定时器
				result.put("readyTime", 0);
			}
		}
		
		// 通知玩家
		for(String uc:room.getPlayerPaiJu().keySet()){
			Playerinfo upi=room.getPlayerMap().get(uc);
			SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
			if(askclient!=null){
				askclient.sendEvent("exitRoomPush_SSS", result);
			}
		}
		
		// 清除用户数据
		room.playerExit(account, room.getPlayerMap().get(account).getId(), room.getPlayerMap().get(account).getUuid());
		
		// 全部准备开始游戏
		if (room.checkIsAllReady()) {
			startGame(room);
		}
		
		// 金币场没人的房间直接清除
		if((room.getRoomType()==1||room.getRoomType()==3)&&room.getPlayerMap().size()==0){
			Constant.sssGameMap.remove(room.getRoomNo());
			RoomManage.gameRoomMap.remove(room.getRoomNo());
			String sql = "update za_gamerooms set status=-2 where room_no=?";
			GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{room.getRoomNo()}, SqlModel.EXECUTEUPDATEBYSQL));
		}
	}

	// 准备超时
	public void readyOvertime(Object data){
		JSONObject postData = JSONObject.fromObject(data);
		// 数据格式不正确
		if (Dto.isObjNull(postData)||!postData.containsKey("room_no")) {
			return;
		}
		String roomNo = postData.getString("room_no");
		// 房间为空
		if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
			return;
		}
		SSSGameRoom room = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		// 当前非准备状态不做任何处理 
		if (room.getGameStatus()!=NewConstant.GAMESTATUS_SSS_READY) {
			return;
		}
		// 获取所有非准备状态的玩家
		List<String> outList = new ArrayList<String>();
		for (String uuid : room.getPlayerPaiJu().keySet()) {
			if (room.getPlayerPaiJu().get(uuid).getIsReady()!=NewConstant.USERSTATUS_SSS_READY) {
				outList.add(uuid);
			}
		}
		// 退出房间
		for (String account : outList) {
			exitRoom(room, account);
		}
	}

	// 配牌超时
	public void peipaiOvertime(Object data){
		JSONObject postData = JSONObject.fromObject(data);
		// 数据格式不正确
		if (Dto.isObjNull(postData)||!postData.containsKey("room_no")) {
			return;
		}
		String roomNo = postData.getString("room_no");
		// 房间为空
		if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
			return;
		}
		SSSGameRoom room = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		// 当前非配牌状态不做任何处理 
		if (room.getGameStatus()!=NewConstant.GAMESTATUS_SSS_PEIPAI) {
			return;
		}
		// 获取所有未配牌的玩家
		List<String> outList = new ArrayList<String>();
		for (String uuid : room.getPlayerPaiJu().keySet()) {
			if (room.getPlayerPaiJu().get(uuid).getStatus()!=NewConstant.USERSTATUS_SSS_HALFWAY&&
					room.getPlayerPaiJu().get(uuid).getStatus()!=NewConstant.USERSTATUS_SSS_PEIPAI) {
				outList.add(uuid);
			}
		}
		// 自动配牌
		JSONObject jsonObject = new JSONObject();
		jsonObject.element("type", NewConstant.PEIPAITYPE_SSS_AUTO);
		for (String account : outList) {
			peiPai(room, account, jsonObject);
		}
	}
	
	// 断线重连
	public void reconnectGame(SocketIOClient client, Object data){
		
	}
	
	public void getJiesuanData(JSONArray us, SSSGameRoom room){

	}
	
	public SSSGameRoom createGameRoom(JSONObject roomObj, UUID uuid, JSONObject objInfo, Playerinfo player, SSSGameRoom room) {

		room.setFirstTime(1);
		room.setPlayerCount(objInfo.getInt("player"));//玩家人数
		if (room.getRoomType()==3) {
			room.setGameCount(999);
		}else{
			room.setGameCount(objInfo.getJSONObject("turn").getInt("turn"));
		}
		if (room.getRoomType()==2) {
			room.setRoomType(0);
		}
		//是否加入机器人
		if (objInfo.containsKey("robot")&&objInfo.getInt("robot")==1) {
			room.setRobot(true);
			List<String> list = mjBiz.getRobotList(room.getPlayerCount()-1);
			room.setRobotList(list);
		}else {
			room.setRobot(false);
		}
		ConcurrentSkipListSet<UUID> uuidList=new ConcurrentSkipListSet<UUID>();
		uuidList.add(uuid);//房主加入房间
		room.setUuidList(uuidList);//用户的socketId
		Map<String,Playerinfo> users=new ConcurrentHashMap<String, Playerinfo>();
		users.put(player.getAccount(), player);
		room.setPlayerMap(users);
		if (objInfo.containsKey("color")) {
			room.setColor(objInfo.getInt("color"));//加色
		}else{
			room.setColor(0);//加色
		}
		Map<String, Player> user = new ConcurrentHashMap<String, Player>();
		Player pl=new Player();
		pl.setIsReady(0);
		pl.setStatus(0);
		if (room.getRoomType()==1) {
			pl.setTotalScore(player.getScore());
			pl.setGameNum(0);
			room.setScore(objInfo.getInt("goldcoins")); //底分 金币场
			room.setMinscore(objInfo.getInt("mingoldcoins"));
			room.setLevel(objInfo.getInt("level"));
		}else if(room.getRoomType()==3){
			System.out.println("元宝底分"+objInfo.getInt("yuanbao"));
			pl.setTotalScore(player.getScore());
			room.setScore(objInfo.getInt("yuanbao")); //底分 元宝场
			room.setMinscore(objInfo.getInt("leaveYB"));//离场分数 
		}else{
			room.setScore(1); //底分 房卡场
			room.setMinscore(1);
		}
		user.put(player.getAccount(), pl);
		room.setPlayerPaiJu(user);
		ConcurrentSkipListSet<Long> userSet = new ConcurrentSkipListSet<Long>();
		userSet.add(player.getId());
		room.setUserSet(userSet);
		ConcurrentSkipListSet<String> acc=new ConcurrentSkipListSet<String>();
		acc.add(player.getAccount());
		room.setUserAcc(acc);
		room.setGameType(objInfo.getInt("type"));//游戏模式
		if (objInfo.containsKey("jiama")) {
			room.setMaPaiType(objInfo.getInt("jiama"));//马牌模式
		}else{
			room.setMaPaiType(0);//马牌模式
		}

		//在这里加入马牌 1黑桃，2红桃，3梅花，4方块
		String c="0-0";
		if (roomObj.getInt("roomtype")!=1) {
			if (room.getMaPaiType()==1) {
				int randomNumber = (int)Math.round(Math.random()*12+1);
				c="1-"+randomNumber;
			}else if (room.getMaPaiType()==2){
				c="1-1";
				//room.setMaPai(c);
			}else if (room.getMaPaiType()==3){
				c="1-5";
			}else if (room.getMaPaiType()==4){
				c="1-10";
			}
		}
		room.setMaPai(c);

		//将房间存入缓存
		RoomManage.gameRoomMap.put(room.getRoomNo(), room);
		Constant.sssGameMap.put(roomObj.getString("room_no"), room);
		return room;
	}
	
	public boolean joinGameRoom(String roomNo, UUID uuid, Playerinfo player,int roomType) {

		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if(room!=null){
				System.err.println(1);
				if(room.getUserSet().size()<=room.getMaxplayer()){
					System.err.println(2);
					if(!room.getUserSet().contains(player.getId())){ 
						//新加进来的玩家
						System.err.println(3);
						room.getUuidList().add(uuid);
						room.getPlayerMap().put(player.getAccount(), player);//用户的个人信息
						Player pl=new Player();
						pl.setIsReady(0);
						pl.setGameNum(0);
						if (roomType==1||roomType==3) {
							pl.setTotalScore(player.getScore());
						}
						room.getPlayerPaiJu().put(player.getAccount(), pl);
						room.getUserSet().add(player.getId());
						room.getUserAcc().add(player.getAccount());
						RoomManage.gameRoomMap.put(roomNo, room);
						return true;
					}else if(room.getGameStatus()>0){ // TODO 断线后进来的玩家
						return false;
					}
				}

			}
		}
		return false;
	}
}
