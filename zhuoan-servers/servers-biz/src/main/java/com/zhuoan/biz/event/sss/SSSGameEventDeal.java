package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.core.sss.SSSSpecialCards;
import com.zhuoan.biz.model.Player;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import com.zhuoan.constant.Constant;
import com.zhuoan.constant.NewConstant;
import com.zhuoan.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

// 游戏事件处理
public class SSSGameEventDeal {

	MaJiangBiz mjBiz=new MajiangBizImpl();
	public SSSServiceImpl serviceImpl = new SSSServiceImpl();
	public JSONObject gameset =null;

	// 创建房间
	public void createRoom(SocketIOClient client, Object data){
		serviceImpl.createRoom(client, data);
	}

	// 加入房间
	public void joinRoom(SocketIOClient client, Object data){
		serviceImpl.joinRoom(client, data);
	}

	// 准备
	public void gameReady(SocketIOClient client, Object data){
		if (NewConstant.checkStatus(client, NewConstant.GAMESTATUS_SSS_READY)) {
			SSSGameRoom sssGameRoom = (SSSGameRoom) RoomManage.gameRoomMap.get(client.get(NewConstant.ROOMNO).toString());
			serviceImpl.playerReady(client.get(NewConstant.CLIENTTAG).toString(), sssGameRoom);
		}
	}

	// 游戏事件
	public void gameEvent(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		if (Dto.isObjNull(fromObject)) {
			return;
		}
		if (NewConstant.checkStatus(client, NewConstant.GAMESTATUS_SSS_FAPAI)) {
			SSSGameRoom sssGameRoom = (SSSGameRoom) RoomManage.gameRoomMap.get(client.get(NewConstant.ROOMNO).toString());
			serviceImpl.peiPai(sssGameRoom, client.get(NewConstant.CLIENTTAG).toString(), fromObject);
		}
	}

	// 退出房间
	public void exitRoom(SocketIOClient client, Object data){
		if (NewConstant.checkStatus(client, NewConstant.CHECK_GAMESTATUS_NO)) {
			SSSGameRoom sssGameRoom = (SSSGameRoom) RoomManage.gameRoomMap.get(client.get(NewConstant.ROOMNO).toString());
			serviceImpl.exitRoom(sssGameRoom, client.get(NewConstant.CLIENTTAG).toString());
		}
	}

	/**
	 * 创建、加入房间
	 * @param client
	 * @param data
	 * 旧版
	 */
	public void enterRoom(SocketIOClient client, Object data) {

		JSONObject postdata = JSONObject.fromObject(data);
		System.out.println("传入参数："+postdata.toString());
		// 房间号
		String roomNo = postdata.getString("room_no");
		// 用户账号
		String account = postdata.getString("account");
		client.set(NewConstant.ROOMNO, roomNo);
		client.set(NewConstant.CLIENTTAG, account);

		JSONObject result=new JSONObject();

		JSONObject userinfo = new JSONObject();

		if(!client.has("userinfo")){

			// uuid
			String uuid=postdata.getString("uuid");
			// 返回的json
			//根据uuid获取用户信息
			userinfo=mjBiz.getUserInfoByAccount(account);
			if (UserInfoCache.userInfoMap.containsKey(account)) {
				userinfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
			}
			UserInfoCache.userInfoMap.put(account, userinfo);

			//验证
			//uuid不合法 返回提示信息
			if(userinfo==null){
				result.put("code", 0);
				result.put("msg", "用户不存在");
				client.sendEvent("enterRoomPush_SSS", result);

				return;
			}else{
				client.set("userinfo", userinfo);
				client.set("userAccount", account);
			}
			if(!userinfo.getString("uuid").equals(uuid)) {
				result.put("code", 0);
				result.put("msg", "该帐号已在其他地方登录");
				LogUtil.print("十三水该帐号已在其他地方登录导致加入房间失败--------");
				client.sendEvent("enterRoomPush_SSS", result);
				return;
			}
		}else{
			client.set("userAccount", account);
			userinfo = client.get("userinfo");
		}

		result.put("code", 1);
		result.put("msg", "");

		// 获取房间信息
		if (gameset==null) {
			System.out.println("查询游戏设置");
			gameset = mjBiz.getGameInfoByID(4);	
		}


		int myIndex = -1;
		SSSGameRoom gameRoom=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if (RoomManage.gameRoomMap.containsKey(roomNo)) {
			if (RoomManage.gameRoomMap.get(roomNo)==null) {
				LogUtil.print("房间为空2导致加入房间失败,房间号为"+roomNo);
			}
		}
		if(gameRoom!=null){


			int roomType = gameRoom.getRoomType();

			if(gameRoom.getGameCount()>gameRoom.getGameIndex() || roomType == 1||roomType==3){ //房间局数还未用完

				long userId = userinfo.getLong("id");
				for (int i = 0; i < gameRoom.getUserIdList().size(); i++) {
					if (userId==gameRoom.getUserIdList().get(i)) {
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

				JSONObject obj=new JSONObject();
				obj.put("room_no", roomNo);

				obj.put("roomType", gameRoom.getRoomType());
				if(gameRoom.getRoomType()==0 || gameRoom.getRoomType()==2){ // 房卡模式
					obj.put("roomType", 0);
					obj.put("game_count", gameRoom.getGameCount());
					obj.put("game_index", gameRoom.getGameIndex()+1);
				}
				// 房间属性信息
				JSONObject objInfo = gameRoom.getRoomInfo();
				// 判断玩家元宝是否足够
				if(roomType==3&&userinfo.getDouble("yuanbao")<objInfo.getDouble("enterYB")) {

					// 删除房间内玩家
					//mjBiz.delGameRoomUserByUid(room, userId);

					result.put("code", 0);
					result.put("msg", "您的元宝不足，请先充值");
					client.sendEvent("enterRoomPush_NN", result);
					RoomManage.unLock(roomNo);
					return;
				}
				String roomName="";
				if (objInfo.containsKey("type")) {
					if (objInfo.getInt("type")==0) {
						roomName="互比";
					}else if (objInfo.getInt("type")==1) {
						roomName="霸王庄";
					}
				}
				gameRoom.setFytype(roomName);
				String rs="";
				if (objInfo.containsKey("player")) {
					rs=objInfo.getInt("player")+"人/";
				}
				String tu="";
				if (objInfo.containsKey("turn")) {
					tu=objInfo.getInt("turn")+"局/";
				}
				String ys="";
				if (objInfo.containsKey("color")) {
					if (objInfo.getInt("color")==0) {
						ys="不加色/";
					}else if (objInfo.getInt("color")==1) {
						ys="加一色/";
					}else if (objInfo.getInt("color")==2) {
						ys="加两色/";
					}

				}
				String jm="";
				if (objInfo.containsKey("jiama")) {

					if (objInfo.getInt("jiama")==0) {
						jm="不加/";
					}else if (objInfo.getInt("jiama")==1) {
						jm="随机/";
					}else {
						jm="加码/";
					}
				}
				String pt="";
				if (objInfo.containsKey("paytype")) {
					if (objInfo.getInt("paytype")==0) {
						pt="房主支付/";
					}else if (objInfo.getInt("paytype")==1) {
						pt="房卡AA/";
					}

				}
				String dz="";
				if (objInfo.containsKey("yuanbao")) {
					dz="底:"+objInfo.getString("yuanbao")+"/";
				}
				String rc="";
				if (objInfo.containsKey("enterYB")) {
					rc="入:"+objInfo.getString("enterYB")+"/";
				}
				String lc="";
				if (objInfo.containsKey("leaveYB")) {
					lc="离:"+objInfo.getString("leaveYB")+"/";
				}
				obj.put("type",roomName);//玩法
				obj.put("turn",tu);//房卡型
				obj.put("ys",ys);//加色
				obj.put("ma",jm);//马牌
				obj.put("pt",pt);//房卡付款类型					
				obj.put("dz",dz);//底注				
				obj.put("rc",rc);//入场					
				obj.put("lc",lc);//离场	
				if(gameRoom.getFirstTime()==0){

					Playerinfo player = new Playerinfo();

					player.setId(userinfo.getLong("id"));
					player.setAccount(account);
					player.setName(userinfo.getString("name"));
					player.setUuid(client.getSessionId());
					player.setMyIndex(myIndex);
					player.setSex(userinfo.getString("sex"));
					player.setIp(userinfo.getString("ip"));
					if(roomType == 1){
						if (gameRoom.getRoomInfo().getInt("level")!=-1) {
							//普通金币场
							player.setScore(userinfo.getInt("coins"));
						}else{
							//竞技场
							player.setScore(userinfo.getInt("score"));
						}
					}else if(roomType == 3){
						player.setScore(userinfo.getDouble("yuanbao"));
					}else{
						player.setScore(0);
					}
					player.setHeadimg(userinfo.getString("headimg"));
					player.setStatus(Constant.ONLINE_STATUS_YES);
					// 设置幸运值
					if(userinfo.containsKey("luck")){
						player.setLuck(userinfo.getInt("luck"));
					}
					// 保存用户坐标
					if(postdata.containsKey("location")){
						player.setLocation(postdata.getString("location"));
					}
					JSONObject room1 = new JSONObject();
					room1.element("base_info", gameRoom.getRoomInfo());
					room1.element("room_no", gameRoom.getRoomNo());
					room1.element("roomtype", gameRoom.getRoomType());
					room1.element("game_count", gameRoom.getGameCount());
					room1.element("game_id", gameRoom.getGid());
					//创建房间
					SSSGameRoom sssGame = serviceImpl.createGameRoom(room1, client.getSessionId(), objInfo, player,gameRoom);

					sssGame.setSetting(gameset.getJSONObject("setting"));//塞入游戏设置
					if (roomType==3) {
						sssGame.setMaxplayer(gameset.getJSONObject("setting").getInt("maxplayer"));//最大人数
						System.out.println("配牌时间："+gameset.getJSONObject("setting").getInt("goldpeipai"));
						sssGame.setPeipaiTime(gameset.getJSONObject("setting").getInt("goldpeipai"));

						// 金币、元宝扣服务费
						JSONObject gameSetting = mjBiz.getGameSetting();
						JSONObject roomFee = gameSetting.getJSONObject("pumpData");
						// 服务费：费率x底注
						double fee = roomFee.getDouble("proportion")*sssGame.getScore();
						double maxFee = roomFee.getDouble("max");
						double minFee = roomFee.getDouble("min");
						if(fee>maxFee){
							fee = maxFee;
						}else if(fee<minFee){
							fee = minFee;
						}
						fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
						sssGame.setFee(fee);

						rs="最低"+rs;
					}else{
						sssGame.setMaxplayer(objInfo.getInt("player"));//最大人数
					}

					client.set(Constant.ROOM_KEY_SSS, roomNo);



					obj.put("player",rs);//人数

					obj.put("users",sssGame.getAllPlayer());//告诉他原先加入的玩家
					obj.put("myIndex",player.getMyIndex());
					obj.put("isReady",sssGame.getReadyIndex());
					int rt=gameRoom.getReadyTime();
					if (rt==0) {
						rt=gameRoom.getSetting().getInt("goldready");
					}
					String a=sssGame.getMaPai();
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
					obj.put("mapai",ma);

					result.put("data", obj);
					client.sendEvent("enterRoomPush_SSS", result);

				}else{//加入房间
					Playerinfo player = new Playerinfo();

					player.setId(userinfo.getLong("id"));
					player.setAccount(account);
					player.setName(userinfo.getString("name"));
					player.setUuid(client.getSessionId());
					player.setMyIndex(myIndex);
					player.setSex(userinfo.getString("sex"));
					player.setIp(userinfo.getString("ip"));
					if(roomType == 1){
						if (gameRoom.getRoomInfo().getInt("level")!=-1) {
							//普通金币场
							player.setScore(userinfo.getInt("coins"));
						}else{
							//竞技场
							player.setScore(userinfo.getInt("score"));
						}
					}else if(roomType == 3){
						player.setScore(userinfo.getDouble("yuanbao"));
					} else{
						player.setScore(0);
					}
					player.setHeadimg(userinfo.getString("headimg"));
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
					boolean is= serviceImpl.joinGameRoom(roomNo, client.getSessionId(), player,roomType);
					client.set(Constant.ROOM_KEY_SSS, roomNo);
					System.err.println("acc:"+account+is);
					ConcurrentSkipListSet<UUID> uuids=gameRoom.getUuidList();//获取原来房间里的人
					if(roomType==3){rs="最低"+rs;}
					obj.put("player",rs);//人数
					obj.put("users",gameRoom.getAllPlayer());//告诉他原先加入的玩家
					obj.put("myIndex", myIndex);
					int rt=gameRoom.getReadyTime();
					if (rt==0) {
						rt=gameRoom.getTimeLeft();
					}
					obj.put("isReady",gameRoom.getReadyIndex());
					if (gameRoom.getReadyIndex().length>=gameRoom.getPlayerCount()&&gameRoom.getGameStatus()<3) {
						obj.put("readyTime", rt);						
					}else if(gameRoom.getGameStatus()==3||gameRoom.getGameStatus()==4){
						obj.put("peipaiTime",gameRoom.getTimeLeft());	
					}
					if (gameRoom.getGameStatus()>2) {
						JSONArray chu=new JSONArray();
						for (String uuid : gameRoom.getPlayerPaiJu().keySet()) {
							if(gameRoom.getPlayerPaiJu().get(uuid).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){
								chu.add(gameRoom.getPlayerIndex(uuid));
							}
						}
						obj.put("chupai", chu);
					}
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
					obj.put("mapai",ma);

					////LogUtil.print("加入房间："+obj);
					result.put("data", obj);
					client.sendEvent("enterRoomPush_SSS", result);

					//如果是断线重连 就不发送
					if (is) {
						if(uuids.size()>0){
							JSONObject re=new JSONObject();
							re=result;
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


							re.put("data", obj);

							for (String string : gameRoom.getPlayerMap().keySet()) {
								if (!gameRoom.getRobotList().contains(string)) {
									SocketIOClient clientother= GameMain.server.getClient(gameRoom.getPlayerMap().get(string).getUuid());
									if(clientother!=null){
										clientother.sendEvent("playerEnterPush_SSS", re);
									}
								}
							}
						}
					}
				}	
			}
		}
	}

	public void gameConnReset(SocketIOClient client, Object data) {
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			JSONObject userinfo = client.get("userinfo");
			if(room.getPlayerMap().containsKey(client.get(NewConstant.CLIENTTAG))&&room.getPlayerMap().get(client.get(NewConstant.CLIENTTAG)).getUuid()!=client.getSessionId()){ //判断玩家是否是重新进入游戏
				// 重连恢复游戏
				JSONObject dataJson = new JSONObject();
				dataJson.put("room_no", roomNo);
				dataJson.put("account", userinfo.getString("account"));
				System.err.println(userinfo.toString());
				reconnectGame(client, dataJson);
			}
		}
	}

	public void reconnectGame(SocketIOClient client, Object data) {
		JSONObject obj= JSONObject.fromObject(data);
		// 房间号
		String roomNo = obj.getString("room_no");
		// 用户账号
		String account = obj.getString("account");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			SSSGameRoom game = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);

			UUID oldUUID = null;

			Playerinfo player = null;
			for (UUID uuid : game.getUuidList()) {
				if(game.getPlayerMap().get(account)!=null&&game.getPlayerMap().get(account).getUuid().equals(uuid)){
					oldUUID = uuid;
					player = game.getPlayerMap().get(account);
					player.setStatus(Constant.ONLINE_STATUS_YES);
				}
			}

			// 重置玩家sessionid
			if(player!=null){
				// 获取玩家位置
				UUID newUUID = client.getSessionId();

				game.getUuidList().remove(oldUUID);
				game.getUuidList().add(newUUID);
				player.setUuid(newUUID);

				// 设置会话信息
				client.set(Constant.ROOM_KEY_SSS, roomNo);
				//根据uuid获取用户信息
				JSONObject userinfo=new JSONObject();
				userinfo=mjBiz.getUserInfoByAccount(account);
				if (UserInfoCache.userInfoMap.containsKey(account)) {
					userinfo.put("yuanbao", UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
				}
				UserInfoCache.userInfoMap.put(account, userinfo);
				client.set("userinfo", userinfo);
				client.set("userAccount", account);
			}

			if(game.getGameStatus()>=0){ //游戏已经开局

				if(player!=null){
					if (Dto.stringIsNULL(account)) {
						account=player.getAccount();
					}
					// 返回给玩家当前牌局信息（基础信息）
					JSONObject result=new JSONObject();
					String a=game.getMaPai();
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
					result.put("mapai", ma);

					//判断当前是否是在申请解散房间阶段
					if(game.getCloseTime()>0){
						result.put("jiesanTime", game.getCloseTime());
						boolean isJieSan = true;
						JSONArray jiesans = new JSONArray();
						for(String uuid:game.getPlayerPaiJu().keySet()){
							if(game.getPlayerMap().get(uuid)!=null){
								JSONObject jiesan = new JSONObject();
								jiesan.put("name", game.getPlayerMap().get(uuid).getName());
								jiesan.put("index", game.getPlayerMap().get(uuid).getMyIndex());
								if(game.getPlayerPaiJu().get(uuid).isCloseRoom==1){
									jiesan.put("result", 1);
								}else if(game.getPlayerPaiJu().get(uuid).isCloseRoom==-1){
									isJieSan = false;
									break;
								}else{
									jiesan.put("result", 0);
								}

								jiesans.add(jiesan);
							}
						}
						if(isJieSan){
							result.put("jiesan", 1);
							result.put("jiesanData", jiesans);
						}else{
							result.put("jiesan", 0);
						}

					}

					if(game.getGameStatus()<NewConstant.GAMESTATUS_SSS_FAPAI){ // 准备阶段

						//判断是否准备
						if(game.getPlayerPaiJu().get(account).getIsReady()==1){
							result.put("type", 0);
						}else{

							result.put("type", 0);
							if(((game.getRoomType()==1||game.getRoomType()==3) && game.getPlayerPaiJu().keySet().size()== game.getNowReadyCount() && game.getNowReadyCount()>1)|| game.getNowReadyCount() >= game.getPlayerCount()){
								// 重置准备
								result.put("myIndex", game.getPlayerIndex(account));
								if (game.getGameType()==1) {
									result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
								}
								if (game.getNowReadyCount()>=game.getPlayerCount()&&game.getPlayerPaiJu().keySet().size()>=game.getPlayerCount()) {
									result.put("readyTime", game.getTimeLeft());
								}		
								result.put("game_index", game.getGameIndex()+1);
								result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());
							}else{
								if (game.getRoomType()==0&&game.getGameCount()<game.getGameIndex()+1) {
									result.put("type", 4);
								}
								result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());
								result.put("myIndex", game.getPlayerIndex(account));
								result.put("isReady", game.getReadyIndex());
								if (game.getNowReadyCount()>=game.getPlayerCount()&&game.getPlayerPaiJu().keySet().size()>=game.getPlayerCount()) {
									result.put("readyTime", game.getTimeLeft());
								}
								if (game.getGameType()==1) {
									result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
								}
							}
						}
					}else if(game.getGameStatus()==NewConstant.GAMESTATUS_SSS_FAPAI||game.getGameStatus()==NewConstant.GAMESTATUS_SSS_PEIPAI){ // 发牌、配牌阶段

						result.put("type", 1);
						if (game.getGameType()==1) {
							result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
						}
						if(game.getPlayerPaiJu().get(account).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){
							result.put("status", 1);
						}else{
							result.put("status", 0);
						}
						result.put("peipaiTime", game.getTimeLeft());

						if(game.getPlayerPaiJu().get(account).getPai()!=null){
							result.put("myPaiType", SSSSpecialCards.isSpecialCards(game.getPlayerPaiJu().get(account).getPai(),game.getSetting()));
							result.put("myPai", game.getPlayerPaiJu().get(account).getMyPai());
						}

						result.put("myIndex",  game.getPlayerIndex(account));


						result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());


						JSONArray chu=new JSONArray();
						for (String uuid : game.getPlayerPaiJu().keySet()) {
							if(game.getPlayerPaiJu().get(uuid).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){
								chu.add(game.getPlayerIndex(uuid));
							}
						}
						result.put("chupai", chu);
						result.put("gameIndex", game.getGameIndex()+1);
						result.put("maPai", ma);

					}else if(game.getGameStatus()==NewConstant.GAMESTATUS_SSS_JIESUAN){ // 结算阶段

						if(game.getPlayerPaiJu().get(account).getIsReady()==NewConstant.USERSTATUS_SSS_READY){ // 玩家已经准备
							result.put("type", 0);
						}else{
							result.put("type", 3);
							Player uuu = game.getPlayerPaiJu().get(account);

							JSONArray data1=new JSONArray();
							int index=0;
							JSONArray us=new JSONArray();
							for (String uuid : game.getPlayerPaiJu().keySet()) {
								if(game.getPlayerPaiJu().get(uuid).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){ // 获取当前已配好牌的玩家
									us.add(uuid);
								}
							}
							for (int i = 0; i < us.size(); i++) {
								Player u1 = game.getPlayerPaiJu().get(us.getString(i));
								int d=0;
								for (String uuid : game.getPlayerPaiJu().keySet()) {
									Player u2 = game.getPlayerPaiJu().get(uuid);

									if (!us.getString(i).equals(uuid)&&u1.getTotalScore()>u2.getTotalScore()) {
										d++;
									}
								}
								if (d==game.getPlayerPaiJu().keySet().size()-1) {
									index=game.getPlayerIndex(us.getString(i));
								}
							}

							for (int i = 0; i < us.size(); i++) {
								JSONObject obj1=new JSONObject();
								Player u = game.getPlayerPaiJu().get(us.getString(i));

								obj1.element("gun", u.getGun());//打枪
								obj1.element("swat", u.getSwat());//全垒打
								obj1.element("special", u.getSpecial());//特殊牌
								obj1.element("ordinary", u.getOrdinary());//普通牌

								obj1.element("score", u.getScore());//单局分数
								obj1.element("totalscore", u.getTotalScore());//总分
								obj1.element("myIndex", game.getPlayerIndex(us.getString(i)));
								obj1.element("win", index);//谁赢

								if(u.getPai()!=null){

									obj1.element("myPai", u.getMyPai());
								}

								if (u.getScore()==0) {
									obj1.put("isdan", -1);//平分秋色
								}else if (u.getScore()>0) {
									obj1.put("isdan", 1);//赢
								}else{
									obj1.put("isdan", 0);//输
								}
								obj1.element("jiesuan",2);//结算类型 1 小结  2 大结
								data1.add(obj1);
							}


							JSONObject users=new JSONObject();
							users.element("score",uuu.getScore() );//当前牌局分
							users.element("totalscore",uuu.getTotalScore());//总分
							users.element("myIndex", game.getPlayerIndex(account));

							result.put("data",data1);
							result.put("users",users);
							if (game.getGameType()==1) {
								result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
							}
							result.put("gameIndex", game.getGameIndex());

							// 重置玩家状态信息
							game.getPlayerPaiJu().get(account).setIsReady(0);

						}
					}	

					JSONObject roomData=new JSONObject();
					roomData.put("room_no", roomNo);
					roomData.put("users",game.getAllPlayer());//告诉他原先加入的玩家
					roomData.put("myIndex",player.getMyIndex());
					roomData.put("isReady",game.getReadyIndex());
					roomData.put("gameStatus",game.getGameStatus());

					if (game.getNowReadyCount()>=game.getPlayerCount()&&game.getPlayerPaiJu().keySet().size()>=game.getPlayerCount()&&game.getGameStatus()<3) {
						result.put("readyTime", game.getTimeLeft());
					}	
					result.put("roomData", roomData);
					client.sendEvent("reconnectGamePush_SSS", result);
				}else{
					JSONObject result = new JSONObject();
					result.put("type", 999); //玩家还未创建房间就已经掉线
					client.sendEvent("reconnectGamePush_SSS", result);
					return;
				}
			}else{
				JSONObject result = new JSONObject();
				result.put("type", 888); //游戏准备时断线重连
				obj.put("users",game.getAllPlayer());//告诉他原先加入的玩家
				obj.put("isReady",game.getReadyIndex());
				if(player!=null){
					obj.put("myIndex",player.getMyIndex());
				}
				result.put("data", obj);
				client.sendEvent("reconnectGamePush_SSS", result);
			}

			//通知其他人用户重连
			for(String uuid : game.getPlayerPaiJu().keySet()){
				Playerinfo upi=game.getPlayerMap().get(uuid);
				if (!game.getRobotList().contains(upi.getAccount())) {
					SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
					if(askclient!=null){
						JSONObject cl = new JSONObject();
						cl.put("index", game.getPlayerIndex(account));
						askclient.sendEvent("userReconnectPush_SSS", cl);
					}
				}
			}
		}else{
			JSONObject result = new JSONObject();
			result.put("type", 999); //玩家还未创建房间就已经掉线
			client.sendEvent("reconnectGamePush_SSS", result);
		}
	}

	public void gameSummary(SocketIOClient client, Object data) {
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			if (room.getGameStatus()>NewConstant.GAMESTATUS_SSS_XIPAI||room.getGameStatus()==NewConstant.GAMESTATUS_SSS_READY) {

				JSONArray us=new JSONArray();

				if (postdata.containsKey("jiesan")&&postdata.getInt("jiesan")==1) {
					for (String uuid : room.getPlayerPaiJu().keySet()) {
						us.add(uuid);
					}
				}else{
					for (String uuid : room.getPlayerPaiJu().keySet()) {
						if(room.getPlayerPaiJu().get(uuid).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI){ // 获取当前已配好牌的玩家
							us.add(uuid);
						}
					}
				}
				JSONArray sum=new JSONArray();

				JSONArray jiesuan=new JSONArray();

				int index=-1;
				for (int i = 0; i < us.size(); i++) {
					Player u1 = room.getPlayerPaiJu().get(us.getString(i));
					int d=0;
					for (int ii = 0; ii < us.size(); ii++) {
						Player u2 = room.getPlayerPaiJu().get(us.getString(ii));

						if (us.getString(i)!=us.getString(ii)&&u1.getTotalScore()>u2.getTotalScore()) {
							d++;
						}
					}
					if (d==room.getPlayerPaiJu().keySet().size()-1) {
						index=room.getPlayerIndex(us.getString(i));
					}
				}

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

				JSONArray ugloga = new JSONArray();
				for (int j = 0; j < us.size(); j++) {
					JSONObject uglog = new JSONObject();
					JSONObject obj=new JSONObject();
					Player u = room.getPlayerPaiJu().get(us.getString(j));
					Playerinfo ui= room.getPlayerMap().get(us.getString(j));
					obj.element("gun", u.getGun());//打枪
					obj.element("swat", u.getSwat());//全垒打
					obj.element("special", u.getSpecial());//特殊牌
					obj.element("ordinary", u.getOrdinary());//普通牌
					obj.element("score", u.getScore());//单局分数
					BigDecimal bigDecimal = new BigDecimal(u.getTotalScore());  
					//这里的 2 就是你要保留几位小数。  
					double f1 = bigDecimal.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue(); 
					obj.element("totalscore",f1);//总分
					obj.element("myIndex", room.getPlayerIndex(us.getString(j)));
					obj.element("win", index);//谁赢
					obj.element("myPai", u.getMyPai());
					obj.element("jiesuan",2);//结算类型 1 小结  2 大结
					if (room.getGameType()==1) {
						obj.element("zhuang",room.getPlayerIndex(room.getZhuang()));}
					obj.element("headimg","http://"+Constant.cfgProperties.getProperty("local_remote_ip")+Constant.DOMAIN+ui.getHeadimg());
					obj.element("name",ui.getName());
					obj.element("account",ui.getAccount());
					obj.element("mapai",ma);

					if (room.getRoomType()==3&&u.getTotalScore()<room.getMinscore()) {
						obj.element("isYuanBao",1);
					}else{
						obj.element("isYuanBao",0);
					}

					//	T淘
					if (u.getScore()==0) {
						obj.put("isdan", -1);//平分秋色
					}else if (u.getScore()>0) {
						obj.put("isdan", 1);//赢
					}else{
						obj.put("isdan", 0);//输
					}
					if (room.getPlayerIndex(us.getString(j))==index) {
						uglog.put("isWinner", 1);
					}else{
						uglog.put("isWinner", 0);
					}

					uglog.put("score", u.getScore());
					uglog.put("TotalScore", u.getTotalScore());
					uglog.put("player",room.getPlayerMap().get(us.getString(j)).getName() );
					if (room.getGameType()==1) {
						uglog.put("zhuang",room.getPlayerIndex(room.getZhuang()));
					}
					uglog.put("headimg","http://"+Constant.cfgProperties.getProperty("local_remote_ip")+Constant.DOMAIN+ui.getHeadimg());
					uglog.put("name",ui.getName());
					uglog.put("account",ui.getAccount());

					ugloga.add(uglog);

					sum.add(obj);
					if (room.getGameCount()==room.getGameIndex()) {
						JSONObject ju=new JSONObject();
						ju.element("score", u.getScore());//单局分数
						ju.element("totalscore", u.getTotalScore());//总分
						ju.element("myPai", u.getMyPai());
						ju.element("headimg","http://"+Constant.cfgProperties.getProperty("local_remote_ip")+Constant.DOMAIN+ui.getHeadimg());
						ju.element("name",ui.getName());
						ju.element("account",ui.getAccount());
						if (room.getPlayerIndex(us.getString(j))==index) {
							ju.element("isWinner", 1);
						}else{
							ju.element("isWinner", 0);
						}
						if (room.getGameType()==1) {
							if (room.getPlayerIndex(us.getString(j))==room.getPlayerIndex(room.getZhuang())) {
								ju.element("isFangzhu", 1);
							}else{
								ju.element("isFangzhu", 0);
							}
						}
						JSONArray array=new JSONArray();

						JSONObject gm=new JSONObject();
						gm.element("name","打枪");
						gm.element("val", u.getGun());
						array.add(gm);
						gm.element("name","全垒打");
						gm.element("val", u.getSwat());
						array.add(gm);
						gm.element("name","特殊牌");
						gm.element("val", u.getSpecial());
						array.add(gm);
						gm.element("name","普通牌");
						gm.element("val", u.getOrdinary());
						array.add(gm);

						ju.element("data",array);

						jiesuan.add(ju);
					}
				}

				if (room.getCloseTime()==-1) {
					if(client!=null){
						client.sendEvent("UserGameSummary_SSS", sum);
						if (Dto.stringIsNULL(client.get("userAccount").toString())) {
							room.getPlayerPaiJu().get(postdata.getString("account")).setIsReady(0);
						}else{
							room.getPlayerPaiJu().get(client.get("userAccount").toString()).setIsReady(0);
						}
					}else{
						SocketIOClient askclient= GameMain.server.getClient(room.getPlayerMap().get(postdata.getString("account")).getUuid());
						askclient.sendEvent("UserGameSummary_SSS", sum);
						room.getPlayerPaiJu().get(postdata.getString("account")).setIsReady(0);
					}
				}else{
					//通知其他人用户
					for(String uuid :room.getPlayerPaiJu().keySet()){
						Playerinfo upi=room.getPlayerMap().get(uuid);
						SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
						if(askclient!=null){
							askclient.sendEvent("UserGameSummary_SSS", sum);
						}
						// 重置玩家状态信息
						room.getPlayerPaiJu().get(uuid).setIsReady(0);
						room.getPlayerPaiJu().get(uuid).setStatus(Constant.ONLINE_STATUS_YES);
					}
				}

				room.setGameStatus(NewConstant.GAMESTATUS_SSS_READY);
			}
		}
	}
}
