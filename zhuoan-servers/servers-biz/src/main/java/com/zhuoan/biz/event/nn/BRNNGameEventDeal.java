package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.model.nn.NiuNiu;
import com.zhuoan.biz.model.nn.UserPacket;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
import com.zhuoan.biz.service.nn.impl.NiuNiuServiceImpl;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;
import java.util.Set;

/**
 * 百人牛牛
 * @author lhp
 *
 */
public class BRNNGameEventDeal {

	NiuNiuService brnnService = new NiuNiuServiceImpl();
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
			
			// uuid
			String uuid=postdata.getString("uuid");
			//根据uuid获取用户信息
			userinfo=mjBiz.getUserInfoByAccount(account);
			//验证
			//uuid不合法 返回提示信息
			if(userinfo==null){
				result.put("code", 0);
				result.put("msg", "用户不存在");
				client.sendEvent("enterRoomPush_BRNN", result);
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
				client.sendEvent("enterRoomPush_BRNN", result);
				return;
			}
		}else{
			
			userinfo = client.get("userinfo");
		}
		
		result.put("code", 1);
		result.put("msg", "");
		
		// 获取房间信息
		JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
		if(room!=null&&room.containsKey("id")){
			
			long userId = userinfo.getLong("id");
			
			JSONObject obj=new JSONObject();
			obj.put("room_no", roomNo);
			obj.put("roomType", room.getInt("roomtype"));
			
			if(!Constant.niuNiuGameMap.containsKey(roomNo)){
				
				Playerinfo player = new Playerinfo();
				
				player.setId(userId);
				player.setAccount(account);
				player.setName(userinfo.getString("name"));
				player.setUuid(client.getSessionId());
				player.setMyIndex((int)userId);
				player.setScore(userinfo.getInt("coins"));
				player.setHeadimg(userinfo.getString("headimg"));
				player.setSex(userinfo.getString("sex"));
				player.setIp(userinfo.getString("ip"));
				player.setStatus(Constant.ONLINE_STATUS_YES);
				// 保存用户坐标
				if(postdata.containsKey("location")){
					player.setLocation(postdata.getString("location"));
				}
				
				//创建房间
				NNGameRoom nnGame = brnnService.createGameRoom(room, clientTag, player);
				
				client.set(Constant.ROOM_KEY_NN, roomNo);
				
				obj.put("users",nnGame.getBRNNAllPlayer(player.getAccount()));//告诉他原先加入的玩家
				obj.put("myIndex",player.getMyIndex());
				obj.put("isReady",nnGame.getReadyIndex());
				obj.put("baseNum", nnGame.getBaseNum());
				obj.put("globalTimer", MutliThreadBRNN.GLOBALTIMER);
				obj.put("gametype", nnGame.getGameType());
				
				LogUtil.print("创建房间："+obj);
				result.put("data", obj);
				client.sendEvent("enterRoomPush_BRNN", result);
				
				// 游戏状态
				Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_QIANGZHUANG);
				// 开启换庄（准备）定时器
				new MutliThreadBRNN(null, roomNo, 0).start();
				
			}else{//加入房间
				
				// 是否进入下一局
				boolean isNext = false;
				
				if(postdata.containsKey("isNext")){
					isNext = true;
				}
				
				Playerinfo player = new Playerinfo();
				
				player.setId(userId);
				player.setAccount(account);
				player.setName(userinfo.getString("name"));
				player.setUuid(client.getSessionId());
				player.setScore(userinfo.getInt("coins"));
				player.setHeadimg(userinfo.getString("headimg"));
				player.setSex(userinfo.getString("sex"));
				player.setIp(userinfo.getString("ip"));
				player.setStatus(Constant.ONLINE_STATUS_YES);
				player.setMyIndex((int)userId);
				// 保存用户坐标
				if(postdata.containsKey("location")){
					player.setLocation(postdata.getString("location"));
				}
				
				//加入房间
				brnnService.joinGameRoom(roomNo, clientTag, player, isNext);
				client.set(Constant.ROOM_KEY_NN, roomNo);
				
				NNGameRoom nnGame = Constant.niuNiuGameMap.get(roomNo);
				
				obj.put("users",nnGame.getBRNNAllPlayer(account));//告诉他原先加入的玩家
				obj.put("myIndex", (int)userId);
				obj.put("isReady",nnGame.getReadyIndex());
				obj.put("baseNum", nnGame.getBaseNum());
				obj.put("globalTimer", MutliThreadBRNN.GLOBALTIMER);
				obj.put("gametype", nnGame.getGameType());
				if(postdata.containsKey("isNext")){
					obj.put("isNext", postdata.get("isNext"));
				}
				obj.put("isRecon", 1);
				
				LogUtil.print("加入房间："+obj);
				result.put("data", obj);
				client.sendEvent("enterRoomPush_BRNN", result);
				
			}
		}
	}

	/**
	 * 庄家开始游戏（开始下注）
	 * @param client
	 * @param data
	 */
	public void gameReady(SocketIOClient client, Object data) {
		
		JSONObject postdata = JSONObject.fromObject(data);
		
		// 房间号
		String roomNo = postdata.getString("room_no");
		
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			String clientTag = Constant.getClientTag(client);
			NNGameRoom room=Constant.niuNiuGameMap.get(roomNo);//获取房间

			// 是否开始游戏
			boolean startGame = false;
			
			if(room.getRoomType()==1&&room.getZhuang().equals(clientTag)){ // 金币场庄家点击开始游戏，直接开始
				
				startGame = true;
				
				// 下一局开始时，清除掉线或金币不足的玩家
				brnnService.cleanPlayer(room);
				
			}
			
			if(startGame&&room.getGameStatus()!=NiuNiu.GAMESTATUS_XIAZHU
					&&room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){

				// 游戏状态
				Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_XIAZHU);
				
				// 重置准备
				room.setReadyCount(0);
				
				// 初始化房间信息
				room.initGame();
				
				Set<String> uuidList = room.getPlayerMap().keySet();
				
				if(uuidList.contains(clientTag)){
					
					// 通知玩家
					for (String uuid : uuidList) {
						
						JSONObject result = new JSONObject();
						SocketIOClient clientother= GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							clientother.sendEvent("gameStartPush_BRNN", result);
						}
					}
				}
				
				// 开启下注定时器，开始计时
				new MutliThreadBRNN(brnnService, roomNo, 1).start();
			}
		}
	}

	
	/**
	 * 游戏事件
	 * @param client
	 * @param data
	 */
	public void gameEvent(SocketIOClient client, Object data) {
		
		String roomNo=client.get(Constant.ROOM_KEY_NN);
		JSONObject postdata = JSONObject.fromObject(data);
		int type = postdata.getInt("type");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			if(type==0){ // 上庄
				
				shangZhuang(client, postdata);
				
			}if(type==1){ // 上庄列表
				
				getShangZhuangList(client, postdata);
				
			}else if(type==2){ // 走势列表
				
				getQuShiList(client, postdata);
				
			}else if(type==3){ // 在线玩家列表
				
				getPlayerList(client, postdata);
				
			}else if(type==4){ // 抢座
				
				qiangZuo(client, postdata);
				
			}else if(type==5){ // 牌型
				
				getPaiInfo(client, postdata);
			}
		}
	}

	
	/**
	 * 获取牌型数据
	 * @param client
	 * @param postdata
	 */
	private void getPaiInfo(SocketIOClient client, JSONObject postdata) {

		String roomNo = postdata.getString("room_no");
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		JSONArray array = new JSONArray();
		for(Integer type:room.ratio.keySet()){
			JSONObject obj = new JSONObject();
			obj.put("type", type);
			obj.put("value", room.ratio.get(type));
			array.add(obj);
		}
		
		for (String uuid : room.getPlayerMap().keySet()) {
			
			JSONObject result = new JSONObject();
			result.put("code", 1);
			result.put("type", postdata.getInt("type"));
			result.put("data", array);
			SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
			if(clientother!=null){
				clientother.sendEvent("gameEventPush_BRNN", result);
			}
		}
	}

	/**
	 * 抢座
	 * @param client
	 * @param postdata
	 */
	private void qiangZuo(SocketIOClient client, JSONObject postdata) {
		
		String clientTag = Constant.getClientTag(client);
		String roomNo = postdata.getString("room_no");
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		int index = postdata.getInt("index");
		room.getPlayerMap().get(clientTag).setMyIndex(index);
		
		// 通知玩家
		JSONArray users = room.getBRNNAllPlayer("");
		for (String uuid : room.getPlayerMap().keySet()) {
			
			JSONObject result = new JSONObject();
			result.put("code", 1);
			result.put("type", postdata.getInt("type"));
			result.put("users", users);
			result.put("myIndex", room.getPlayerMap().get(uuid).getMyIndex());
			SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
			if(clientother!=null){
				clientother.sendEvent("gameEventPush_BRNN", result);
			}
		}
	}

	/**
	 * 玩家上庄
	 * @param client
	 * @param postdata
	 */
	private void shangZhuang(SocketIOClient client, JSONObject postdata) {
		
		String clientTag = Constant.getClientTag(client);
		String roomNo = postdata.getString("room_no");
		
		// TODO 判断是否满足上庄条件
		Playerinfo playerinfo = Constant.niuNiuGameMap.get(roomNo).getPlayerMap().get(clientTag);
		if(playerinfo.getScore()>=50000){
			
			Constant.niuNiuGameMap.get(roomNo).getShangzhuangList().add(playerinfo);
			
			// 获取上庄列表
			getShangZhuangList(client, postdata);
		}else{
			JSONObject result = new JSONObject();
			result.put("code", 0);
			result.put("type", postdata.getInt("type"));
			result.put("msg", "您的金币不足，请先充值！");
			
			client.sendEvent("gameEventPush_BRNN", result);
		}
	}

	/**
	 * 上庄列表
	 * @param client
	 * @param postdata
	 */
	private void getShangZhuangList(SocketIOClient client, JSONObject postdata) {
		
		String roomNo = postdata.getString("room_no");
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		
		JSONArray users = new JSONArray();
		List<Playerinfo> playerinfoList = room.getShangzhuangList();
		for (Playerinfo player : playerinfoList) {
			JSONObject user = new JSONObject();
			user.put("account", player.getAccount());
			user.put("name", player.getName());
			user.put("headimg", player.getRealHeadimg());
			user.put("score", player.getScore());
			users.add(user);
		}
		
		JSONObject result = new JSONObject();
		result.put("code", 1);
		result.put("type", postdata.getInt("type"));
		result.put("data", users);
		result.put("szScore", 50000);
		
		client.sendEvent("gameEventPush_BRNN", result);
	}

	/**
	 * 走势列表
	 * @param client
	 * @param postdata
	 */
	private void getQuShiList(SocketIOClient client, JSONObject postdata) {
		
		String roomNo = postdata.getString("room_no");
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		
		JSONObject result = new JSONObject();
		result.put("code", 1);
		result.put("type", postdata.getInt("type"));
		result.put("data", room.getQuShiList());
//		String data = "[[1,1,0,1,0,0,1],[1,1,0,1,0,0,1],[1,1,0,1,0,0,1],[1,1,0,1,0,0,1],[1,1,0,1,0,0,1]]";
//		result.put("data", data);
		
		client.sendEvent("gameEventPush_BRNN", result);
	}

	/**
	 * 玩家列表
	 * @param client
	 * @param postdata
	 */
	private void getPlayerList(SocketIOClient client, JSONObject postdata) {

		String roomNo = postdata.getString("room_no");
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		
		JSONArray users = new JSONArray();
		for (String uuid : room.getPlayerMap().keySet()) {
			
			Playerinfo player = room.getPlayerMap().get(uuid);
			JSONObject user = new JSONObject();
			user.put("name", player.getName());
			user.put("headimg", player.getRealHeadimg());
			user.put("score", player.getScore());
			users.add(user);
		}
		
		JSONObject result = new JSONObject();
		result.put("code", 1);
		result.put("type", postdata.getInt("type"));
		result.put("data", users);
		
		client.sendEvent("gameEventPush_BRNN", result);
	}

	/**
	 * 退出房间
	 * @param client
	 * @param data
	 */
	public void exitRoom(SocketIOClient client, Object data) {
		
		// 房间号
		String roomNo=client.get(Constant.ROOM_KEY_NN);
		JSONObject userinfo = client.get("userinfo");
		if(!Dto.isObjNull(userinfo)){
			exitRoom(Constant.getClientTag(client), roomNo, userinfo.getLong("id"));
		}
	}
	
	
	/**
	 * 玩家退出玩家房间
	 * @param uuid
	 * @param roomNo
	 * @param userId
	 */
	public void exitRoom(String uuid, String roomNo, long userId) {
		
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			NNGameRoom game = Constant.niuNiuGameMap.get(roomNo);
			
			boolean isRealExit = true;
			
			// 金币场庄家退出游戏
			if(game.getRoomType()==1&&game.getZhuang().equals(uuid)){
				
				// 准备或者牌局结束阶段庄家退出
//				if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY || game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
//					
//				}else{
//					
//					isRealExit = false;
//					
//					LogUtil.print("###############牌局进行中，庄家不能退出房间！！！！！！！！！");
//					
//					JSONObject result=new JSONObject();
//					result.put("code", 0);
//					result.put("msg", "游戏进行中，不能离开房间");
//					SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
//					if(askclient!=null){
//						askclient.sendEvent("exitRoomPush_BRNN", result);
//					}
//				}
				
			}

			if(isRealExit){
				
				if(game.getPlayerMap().get(uuid)!=null){
					
					// 移出上庄列表
					Constant.niuNiuGameMap.get(roomNo).getShangzhuangList().remove(game.getPlayerMap().get(uuid));
					
					JSONObject result = new JSONObject();
					result.put("type", 2); //退出房间
					result.put("index", game.getPlayerIndex(uuid));
					for(String uuid1:game.getPlayerMap().keySet()){
						
						if(uuid.equals(uuid1) || game.getPlayerMap().get(uuid1).getMyIndex()<=6){
							
							SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid1));
							if(askclient!=null){
								askclient.sendEvent("exitRoomPush_BRNN", result);
							}
						}
					}
				}
				
				// 更新数据库房间信息
				
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
					
					if(userIndex!=null){
						
						String sql = "update za_gamerooms set "+userIndex+"=? where status>=0 and id=?";
						DBUtil.executeUpdateBySQL(sql, new Object[]{0, room.getLong("id")});
					}
					
					Constant.niuNiuGameMap.get(roomNo).getUserIDSet().remove(userId);
				}
				// 清除房间用户数据
				Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().remove(uuid);
				Constant.niuNiuGameMap.get(roomNo).getPlayerMap().remove(uuid);
				
				// 金币场没人的房间直接清除
				if(game.getRoomType()==1&&Constant.niuNiuGameMap.get(roomNo).getPlayerMap().size()==0){
					Constant.niuNiuGameMap.remove(roomNo);
					LogUtil.print("金币场没人的房间直接清除："+roomNo);
				}
			}
		}
	}

	public void reconnectGame(SocketIOClient client, Object data) {
		

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
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			NNGameRoom game = Constant.niuNiuGameMap.get(roomNo);
			Playerinfo player = null;
			for(String uuid:game.getPlayerMap().keySet()){
				if(game.getPlayerMap().get(uuid)!=null&&game.getPlayerMap().get(uuid).getAccount().equals(account)){
					player = game.getPlayerMap().get(uuid);
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
				if(game.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){ // 上庄阶段
					
					result.put("type", -1); 
					
				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_READY){ // 准备（空闲）阶段
					
					result.put("type", 0); 
					// 倒计时
					result.put("timer", game.getXiazhuTime());
					
				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){ // 玩家下注阶段
					
					result.put("type", 1);
					// 倒计时
					result.put("timer", game.getXiazhuTime());
					result.put("myXiazhu", game.getplaceArrayNums(Integer.valueOf(clientTag)));
					// 所有玩家下注记录
					result.put("placeArray", game.getPlaceArray());
					
				}else if(game.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN){ // 结算阶段
					
					result.put("type", 2);
					// 倒计时
					result.put("timer", game.getXiazhuTime());
					result.put("myXiazhu", game.getplaceArrayNums(Integer.valueOf(clientTag)));
					// 所有玩家下注记录
					result.put("placeArray", game.getPlaceArray());

					// 结算数据返回
					JSONObject jsData = new JSONObject();
					
					jsData.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
					jsData.put("zhuangScore", game.getUserPacketList().get(0).getScore());
					
					// 庄家区域
					JSONObject zhuangResult = new JSONObject();
					zhuangResult.put("pai", game.getUserPacketList().get(0).getMyPai());
					zhuangResult.put("result", game.getUserPacketList().get(0).type);
					jsData.put("zhuangResult", zhuangResult);
					
					// 闲家下注区域信息
					JSONArray xianResult = new JSONArray();
					for (int j = 1; j <= 4; j++) {
						JSONObject xres = new JSONObject();
						UserPacket up = game.getUserPacketList().get(j);
						xres.put("pai", up.getMyPai());
						xres.put("result", up.type);
						xianResult.add(xres);
					}
					jsData.put("xianResult", xianResult);
					
					// 获取座位上的玩家数据
					jsData.put("users", game.getBRNNAllPlayer(clientTag));
					// 4个区域的下注
					JSONObject myXiazhu = game.getplaceArrayNums(Integer.valueOf(clientTag));
					int xiazhuScore = 0;
					if(!Dto.isObjNull(myXiazhu)){
						if(myXiazhu.containsKey("0")){
							xiazhuScore+=myXiazhu.getInt("0");
						}
						if(myXiazhu.containsKey("1")){
							xiazhuScore+=myXiazhu.getInt("1");
						}
						if(myXiazhu.containsKey("2")){
							xiazhuScore+=myXiazhu.getInt("2");
						}
						if(myXiazhu.containsKey("3")){
							xiazhuScore+=myXiazhu.getInt("3");
						}
					}
					jsData.put("myXiazhu", myXiazhu);
					double score = game.getUserPacketMap().get(clientTag).getScore();
					// 自己的总分
					jsData.put("myScore", score);
					// -1/0/1 自己的输赢(总的,0是未下注)
					if(xiazhuScore>0){
						if(score>=0){
							jsData.put("myResult", 1);
						}else{
							jsData.put("myResult", -1);
						}
					}else{
						jsData.put("myResult", 0);
					}
					
					result.put("jiesuanData", jsData);
				}	

				result.put("baseNum", game.getBaseNum());
				result.put("users",game.getBRNNAllPlayer(player.getAccount()));
				result.put("myIndex",player.getMyIndex());
				
				if(obj.containsKey("isRecon")){
					result.put("isRecon", 1);
				}
				
				LogUtil.print("断线重连返回="+result.toString());
				client.sendEvent("reconnectGamePush_BRNN", result);
			}

			//通知其他人用户重连
			for(String uuid:game.getPlayerMap().keySet()){
				
				SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
				if(askclient!=null){
					JSONObject cl = new JSONObject();
					cl.put("index", Constant.niuNiuGameMap.get(roomNo).getPlayerIndex(clientTag));
					askclient.sendEvent("userReconnectPush_BRNN", cl);
				}
			}
			
		}else{
			JSONObject result = new JSONObject();
			result.put("type", 999); //玩家还未创建房间就已经掉线
			client.sendEvent("reconnectGamePush_BRNN", result);
			LogUtil.print("创建房间（999）："+ JSONObject.fromObject(result));
		}
	}
	

	public void gameConnReset(SocketIOClient client, Object data) {
		
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			// 重连恢复游戏
			JSONObject userinfo = client.get("userinfo");
			JSONObject dataJson = new JSONObject();
			dataJson.put("room_no", roomNo);
			dataJson.put("account", userinfo.getString("account"));
			dataJson.put("isRecon", 1);
			reconnectGame(client, dataJson);
		}
	}

	/**
	 * 下注
	 * @param client
	 * @param data
	 */
	public void gameXiaZhu(SocketIOClient client, Object data) {

		String clientTag = Constant.getClientTag(client);
		JSONObject postdata = JSONObject.fromObject(data);
		String roomNo=postdata.getString("room_no");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			
			int num = postdata.getInt("num");
			int place = postdata.getInt("place");
			int money = postdata.getInt("money");
			int xiazhutime = room.getXiazhuTime();
			if(xiazhutime>0){
				
				String uuid = Constant.getClientTag(client);
			    boolean isCanXiaZhu = true;
			    int xiazhuScore = 0;
				// 金币场时，判断玩家金币是否足够且庄家金币足够赔付
			    if(room.getRoomType()==1){
			    	
			    	JSONObject myScore = room.getplaceArrayNums(Integer.valueOf(room.getPlayerAccount(uuid)));
			    	
			    	if(!Dto.isObjNull(myScore)){
			    		if(myScore.containsKey("0")){
							xiazhuScore+=myScore.getInt("0");
						}
			    		if(myScore.containsKey("1")){
							xiazhuScore+=myScore.getInt("1");
						}
						if(myScore.containsKey("2")){
							xiazhuScore+=myScore.getInt("2");
						}
						if(myScore.containsKey("3")){
							xiazhuScore+=myScore.getInt("3");
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
			    		double playertotalMoney = room.getPlayerTotalMoneyByBR(room.getPlayerMap());
			    		// 庄家剩余筹码
			    		double zhuangtotalMoney = room.getPlayerMap().get(room.getZhuang()).getScore();
			    		
			    		if((playertotalMoney + money)*4*room.getScore() > zhuangtotalMoney){
			    			
			    			isCanXiaZhu = false;
			    			postdata.element("code", 0);
			    			postdata.element("msg", "您的下注已超出限制，暂时无法下注");
			    		}
			    	}
			    }
			    
			    // 判断玩家是否可以下注
				if(isCanXiaZhu){
					
					//添加下注记录
					room.addPlayerMoney(num, place, money);
					
					postdata.element("code", 1);
					postdata.element("msg", "下注成功");
					
					postdata.element("playerMoney", room.getMoneyPlaceByBR(room.getPlayerMap()));
					
					//记录下下注信息
					room.getPlaceArray().add(postdata);
					
					// 房卡场下注完成
					if(room.getRoomType()==0 || room.getRoomType()==2){
						room.getUserPacketMap().get(clientTag).setIsReady(2);
					}
				}
				
			}else{
				postdata.element("code", 0);
				postdata.element("msg", "下注时间到无法下注");
			}
			
			for(String uuid:room.getPlayerMap().keySet()){
				
				JSONObject result = JSONObject.fromObject(postdata);
				if(uuid.equals(clientTag) && result.getInt("code")==1){
					double myXiazhu = room.getPlayerMoneyNum(num, place);
					result.element("myXiazhu", myXiazhu);
				}
				
				SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
				if(clientother!=null){
					clientother.sendEvent("gameXiaZhuPush_BRNN", result);
				}
			}
		}
	}

	
	/**
	 * 闲家撤销下注
	 * @param client
	 * @param data
	 */
	public void revokeXiazhu(SocketIOClient client, Object data) {

		JSONObject obj= JSONObject.fromObject(data);
		String roomNo=obj.getString("room_no");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			JSONObject result = new JSONObject();
			String uuid = Constant.getClientTag(client);
			int num = Integer.valueOf(room.getPlayerAccount(uuid));
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
			
			result.element("playerMoney", room.getMoneyPlaceByBR(room.getPlayerMap()));
			result.element("code", 1);
			result.element("num", num);

			for(String uid : room.getPlayerMap().keySet()){
				SocketIOClient askclient=GameMain.server.getClient(room.getUUIDByClientTag(uid));
				if(askclient!=null){
					askclient.sendEvent("revokeXiazhuPush_BRNN", result);
				}

			}
			
			// 玩家已撤销下注
			room.getUserPacketMap().get(uuid).setIsReady(0);
			
		}
	}
	
	
	/**
	 * 闲家确认下注
	 * @param client
	 * @param data
	 */
	public void sureXiazhu(SocketIOClient client, Object data) {

		String clientTag = Constant.getClientTag(client);
		JSONObject obj= JSONObject.fromObject(data);
		
		String roomNo=obj.getString("room_no");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			String uuid = Constant.getClientTag(client);
			int num = Integer.valueOf(room.getPlayerAccount(uuid));

			JSONObject result = new JSONObject();
			result.element("code", 1);
			result.element("num", num);
			
			for(String uid : room.getPlayerMap().keySet()){
				SocketIOClient askclient=GameMain.server.getClient(room.getUUIDByClientTag(uid));
				if(askclient!=null){
					askclient.sendEvent("sureXiazhuPush_BRNN", result);
				}
			}
			
			// 玩家已确认下注
			room.getUserPacketMap().get(clientTag).setIsReady(2);
		}
	}
	
}
