package com.zhuoan.biz.event.bdx;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.biz.event.GameMain;
import com.zhuoan.biz.model.*;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.biz.service.bdx.BDXService;
import com.zhuoan.biz.service.bdx.impl.BDXServiceImpl;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.util.Dto;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class BDXGameEventDeal {

	BDXService bdxService = new BDXServiceImpl();
	MaJiangBiz mjBiz=new MajiangBizImpl();

	/**
	 * 创建、加入房间
	 * @param client
	 * @param data
	 */
	public void enterRoom(SocketIOClient client, Object data) {


		JSONObject postdata = JSONObject.fromObject(data);
		System.out.println("传入参数："+postdata.toString());
		// 房间号
		String roomNo = postdata.getString("room_no");
		// 用户账号
		String account = postdata.getString("account");

		JSONObject result=new JSONObject();

		JSONObject userinfo = new JSONObject();

		if(!client.has("userinfo")){

			// uuid
			String uuid=postdata.getString("uuid");
			// 返回的json
			//根据uuid获取用户信息
			/*if (UserInfoCache.userInfoMap.containsKey(account)) {
				userinfo = UserInfoCache.userInfoMap.get(account);
			}else {
				userinfo=mjBiz.getUserInfoByAccount(account);
				UserInfoCache.userInfoMap.put(account, userinfo);
			}*/
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
				client.sendEvent("enterRoomPush_BDX", result);
				return;
			}else{
				client.set("userinfo", userinfo);
				client.set("userAccount", account);
			}
			if(!userinfo.getString("uuid").equals(uuid)) {
				result.put("code", 0);
				result.put("msg", "该帐号已在其他地方登录");
				LogUtil.print("比大小该帐号已在其他地方登录导致加入房间失败--------");
				client.sendEvent("enterRoomPush_BDX", result);
				return;
			}
		}else{

			userinfo = client.get("userinfo");
			client.set("userAccount", account);
		}

		result.put("code", 1);
		result.put("msg", "");

		// 获取房间信息
		//JSONObject room = mjBiz.getRoomInfoByRno(roomNo);	
		try {
			RoomManage.lock(roomNo);
			BDXGameRoom gameRoom = (BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if (RoomManage.gameRoomMap.containsKey(roomNo)) {
				if (RoomManage.gameRoomMap.get(roomNo)==null) {
					LogUtil.print("房间为空2导致加入房间失败,房间号为"+roomNo);
				}
			}

			int myIndex = -1;
			if(gameRoom!=null){

				// 房间类型（0：房卡  1：金币 2:代开房卡 3:元宝）
				int roomType = 0;
				//if(room.containsKey("roomtype")&&room.get("roomtype")!=null){
				roomType = gameRoom.getRoomType();
				//}
				if(gameRoom.getGameCount()>gameRoom.getGameIndex()||  roomType == 1||roomType==3){ //房间局数还未用完

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
				}*/

					JSONObject obj=new JSONObject();
					obj.put("room_no", roomNo);

					obj.put("roomType", gameRoom.getRoomType());
					if(gameRoom.getRoomType()==0 || gameRoom.getRoomType()==2){ // 房卡模式
						obj.put("roomType", 0);
						obj.put("game_count", gameRoom.getGameCount());
						obj.put("game_index", gameRoom.getGameIndex()+1);
					}
					// 房间属性信息
					String base_info = gameRoom.getRoomInfo().toString();
					JSONObject objInfo = JSONObject.fromObject(base_info);

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
							player.setScore(userinfo.getInt("coins"));
						}else if(roomType == 3){
							player.setScore(userinfo.getDouble("yuanbao"));
						}else{
							player.setScore(0);
						}
						//player.setScore(userinfo.getInt("score"));
						player.setHeadimg(userinfo.getString("headimg"));
						player.setStatus(0);

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
						BDXGameRoom bdxGame = bdxService.createGameRoom(room1, client.getSessionId(), objInfo, player);

						// 金币、元宝扣服务费

						//sssGame.setGameType(objInfo.getInt("type"));
						client.set(Constant.ROOM_KEY_BDX, roomNo);

						obj.put("type","比大小");//玩法

						obj.put("users",bdxGame.getAllPlayer());//告诉他原先加入的玩家
						obj.put("myIndex",player.getMyIndex());
						LogUtil.print("创建房间："+obj);
						result.put("data", obj);
						client.sendEvent("enterRoomPush_BDX", result);

						//					
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
							player.setScore(userinfo.getInt("coins"));
						}else if(roomType == 3){
							player.setScore(userinfo.getDouble("yuanbao"));
						} else{
							player.setScore(0);
						}
						//player.setScore(userinfo.getInt("score"));
						player.setHeadimg(userinfo.getString("headimg"));
						player.setStatus(0);
						// 保存用户坐标
						if(postdata.containsKey("location")){
							player.setLocation(postdata.getString("location"));
						}

						//加入房间
						boolean is= bdxService.joinGameRoom(roomNo, client.getSessionId(), player,roomType);
						client.set(Constant.ROOM_KEY_BDX, roomNo);
						System.err.println("acc:"+account+is);
						List<UUID> uuids=gameRoom.getUuidList();//获取原来房间里的人

						obj.put("type","");//玩法

						obj.put("users",gameRoom.getAllPlayer());//告诉他原先加入的玩家
						obj.put("myIndex", myIndex);
						LogUtil.print("加入房间："+obj);
						result.put("data", obj);
						client.sendEvent("enterRoomPush_BDX", result);

						//如果是断线重连 就不发送
						if (is) {
							if(uuids.size()>0){

								//JSONObject obj1=new JSONObject();
								JSONObject playerObj=new JSONObject();
								playerObj.put("name",player.getName());
								playerObj.put("headimg",player.getRealHeadimg());
								playerObj.put("id",player.getAccount());
								playerObj.put("sex",player.getSex());
								playerObj.put("ip", player.getIp());
								playerObj.put("location", player.getLocation());
								playerObj.put("score",player.getScore());
								playerObj.put("index",player.getMyIndex());
								obj.put("user", playerObj);
								//obj1.put("readyTime", rt);
								result.put("data", obj);

								for(UUID other:uuids){
									if(!other.equals(client.getSessionId())){
										SocketIOClient clientother= GameMain.server.getClient(other);
										if(clientother!=null){
											clientother.sendEvent("playerEnterPush_BDX", result);
										}
									}
								}
							}
						}

					}	
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			//RoomManage.unLock(roomNo);
		} finally {
			RoomManage.unLock(roomNo);
		}
	}



	/**
	 * 游戏事件
	 * @param client
	 * @param data
	 */
	public void gameEvent(SocketIOClient client, Object data) {

		JSONObject postdata = JSONObject.fromObject(data);

		Object roomNo=client==null?postdata.get("roomNo"):client.get(Constant.ROOM_KEY_BDX);
		//UUID sessionId=client==null?UUID.fromString(objData.getString("uuid")):client.getSessionId();
		

		//String account=client==null?postdata.getString("account"):client.get("userAccount").toString();
		String account;
		if (postdata.containsValue("account")) {
			account=postdata.getString("account");
		}else{
			if (Dto.isNull(client)||Dto.isNull(client.get("userAccount"))) {
				return;
			}
			account=client.get("userAccount").toString();
		}
		if (Dto.isNull(roomNo)) {
			return;
		}

		try {
			RoomManage.lock(roomNo.toString());
			if(RoomManage.gameRoomMap.containsKey(roomNo)){

				BDXGameRoom room=(BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);

				// 完成下注操作
				if (room.getPlayerMap().get(account).getStatus()==0) {
					room.getPlayerMap().get(account).setStatus(1);
					System.out.println("1ac:"+account+",totalscore1:"+room.getPlayerMap().get(account).getScore()+",zhu:"+postdata.getInt("zhu"));
					if (postdata.getInt("zhu")>room.getPlayerMap().get(account).getScore()) {
						room.getPlayerMap().get(account).setLuck(0);
					}else{
						room.getPlayerMap().get(account).setLuck(postdata.getInt("zhu"));
					}
					System.out.println("2ac:"+account+",totalscore1:"+room.getPlayerMap().get(account).getScore()+",zhu:"+room.getPlayerMap().get(account).getLuck());
					room.setGameStatus(1);
				}

				// 通知玩家
				JSONObject result1 = new JSONObject();

				result1.put("isReady", room.getReadyIndex());
				result1.put("index",room.getPlayerMap().get(account).getMyIndex());
				result1.put("zhu", room.getPlayerMap().get(account).getLuck());
				for (String uc : room.getUserAcc()) {
					Playerinfo pi=room.getPlayerMap().get(uc);
					SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
					if(clientother!=null){
						clientother.sendEvent("playerReadyPush_BDX", result1);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			//RoomManage.unLock(roomNo);
		} finally {
			RoomManage.unLock(roomNo.toString());
		}

	}

	/**
	 * 结算
	 * @param client
	 * @param data
	 * @return 
	 */
	public void gameSummary(SocketIOClient client, Object data) {
		JSONObject postdata = JSONObject.fromObject(data);

		Object roomNo=client==null?postdata.get("roomNo"):client.get(Constant.ROOM_KEY_BDX);
		//UUID sessionId=client==null?UUID.fromString(objData.getString("uuid")):client.getSessionId();
		String account=client==null?postdata.getString("account"):client.get("userAccount").toString();
		if (Dto.isNull(roomNo)) {
			return;
		}
		try {
			RoomManage.lock(roomNo.toString());
			if(RoomManage.gameRoomMap.containsKey(roomNo)){

				BDXGameRoom room=(BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);
				//int isReady = 0;
				JSONArray us=new JSONArray();
				for (String uid : room.getPlayerMap().keySet()) {
					us.add(uid);
					/*if(room.getPlayerMap().get(uid).getStatus()==1){ // 获取当前已配好牌的玩家
					isReady++;
				}*/
				}/*isReady==room.getPlayerCount()&&*/

				synchronized (this) {

					if (postdata.getInt("isRS")==1&&room.getGameStatus()==1) {

						LogUtil.print("比大小:结算  ");
						JSONArray sum=new JSONArray();
						String iswin="";

						Playerinfo up=room.getPlayerMap().get(account);
						int rsl=up.getLuck();
						for (int i=0;i<us.size();i++) {
							Playerinfo upi1=room.getPlayerMap().get(us.getString(i));

							//if (upi1.getLuck()<0) {upi1.setLuck(0);}

							if (!account.equals(upi1.getAccount())) {
								iswin=upi1.getAccount();
								LogUtil.print("比大小结算 结果替换前- Aac"+account+",Luck:"+up.getLuck()+";ac"+upi1.getAccount()+",Luck:"+upi1.getLuck());
								int a=up.getLuck();
								//int b=upi1.getLuck();
								up.setLuck(-a);
								upi1.setLuck(a);
								LogUtil.print("比大小结算 结果替换后- Aac"+account+",Luck:"+up.getLuck()+";ac"+upi1.getAccount()+",Luck:"+upi1.getLuck());

								/*if (up.getLuck()>upi1.getLuck()) {
									iswin=upi1.getAccount();
									up.setLuck(-a);
									upi1.setLuck(a);
								}else{
									iswin=up.getAccount();
									up.setLuck(b);
									upi1.setLuck(-b);
								}
								System.err.println("Bac"+account+",Luck:"+up.getLuck()+";ac"+upi1.getAccount()+",Luck:"+upi1.getLuck());*/
							}
						}

						int ying=0;
						int shu=0;
						int randNumber1 =  new Random().nextInt(12)+1;
						int randNumber2 =  new Random().nextInt(12)+1;
						int color =  new Random().nextInt(3)+1;
						int color1 =  new Random().nextInt(3)+1;
						if (randNumber1>randNumber2&&randNumber1!=0&&randNumber2!=0) {
							ying=randNumber1;
							shu=randNumber2;
						}else if (randNumber1<randNumber2&&randNumber1!=0&&randNumber2!=0){
							ying=randNumber2;
							shu=randNumber1;
						}else{
							ying=13;
							shu=5;
						}
						if (color==2){
							ying=ying+20;
						}else if (color==3){
							ying=ying+40;
						}else if (color==4){
							ying=ying+60;
						}
						if (color1==2){
							shu=shu+20;
						}else if (color1==3){
							shu=shu+40;
						}else if (color1==4){
							shu=shu+60;
						}

						for(String uuid :room.getUserAcc()){
							JSONObject obj=new JSONObject();
							Playerinfo upi=room.getPlayerMap().get(uuid);

							LogUtil.print("比大小结算 加分前  -3ac:"+uuid+",totalscore1:"+upi.getScore()+",zhu:"+upi.getLuck());

							double total= upi.getScore()+upi.getLuck();

							LogUtil.print("比大小结算  加分后 -4ac:"+uuid+",totalscore2:"+total+",zhu:"+upi.getLuck());

							upi.setScore(total);

							obj.element("index",room.getPlayerIndex(uuid));
							obj.element("totalscore",upi.getScore());//总分
							obj.element("zhu", upi.getLuck());

							if (upi.getAccount().equals(iswin)) {
								obj.element("iswin", 1);//谁赢
								obj.element("pai", ying);
							}else{
								obj.element("iswin", 0);//谁输
								obj.element("pai", shu);
							}
							obj.element("headimg","http://"+Constant.cfgProperties.getProperty("local_remote_ip")+Constant.DOMAIN+upi.getHeadimg());
							obj.element("name",upi.getName());
							obj.element("account",upi.getAccount());

							sum.add(obj);
						}

						for(String uuid :room.getUserAcc()){
							Playerinfo upi=room.getPlayerMap().get(uuid);

							SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
							if(askclient!=null){
								askclient.sendEvent("UserGameSummary_BDX", sum);
							}

						}
						room.setGameStatus(0);
						RoomManage.gameRoomMap.get(roomNo).setGameIndex(room.getGameIndex()+1);
						//元宝结算
						StringBuffer sqlx=new StringBuffer();
						sqlx.append("insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values $");
						String ve="";
						String te=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
						String sql = "update za_users SET yuanbao = CASE id  $ END WHERE id IN (/)";
						String z="";
						String d="";

						for (String uuuid : room.getPlayerMap().keySet()) {

							Playerinfo uinfo = room.getPlayerMap().get(uuuid);
							ve=ve+"("+uinfo.getId()+","+10+",'"+roomNo+"',"+3+","+uinfo.getLuck()+",'"+te+"'),";

							if (uinfo.getScore()<=0) {
								z=z+" WHEN "+uinfo.getId()+" THEN 0";
							}else{
								if (rsl>0&&uinfo.getLuck()==0) {
									LogUtil.print("比大小结算-有误："+rsl+","+uinfo.getLuck());
									uinfo.setLuck(rsl);
									LogUtil.print("比大小结算-有误修正："+rsl+","+uinfo.getLuck());
								}
								UserInfoCache.updateUserScore(uuuid, uinfo.getLuck(), 3);
								z=z+" WHEN "+uinfo.getId()+" THEN yuanbao+"+uinfo.getLuck();
							}

							d=d+uinfo.getId()+",";
							/*if (uinfo.getScore()<=0) {
							//负数清零
							String sql = "update za_users set yuanbao=yuanbao-yuanbao where id=?";
							int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ uinfo.getId() });
						}else{
							if (rsl>0&&uinfo.getLuck()==0) {
								LogUtil.print("比大小结算-有误："+rsl+","+uinfo.getLuck());
								uinfo.setLuck(rsl);
								LogUtil.print("比大小结算-有误修正："+rsl+","+uinfo.getLuck());
							}
								String sql = "update za_users set yuanbao=yuanbao+? where id=?";
								int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ uinfo.getLuck(),uinfo.getId() });

						}*/
						}
						BDXGameRoom room2 = room;
						//gamelog(room2,sum);
						new SaveGameLogsBDX(room2,sum).start();
						//扣除元宝记录 
						//String sql1 = "insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values(?,?,?,?,?,?)";
						//DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{});
						String replace = sql.replace("$", z).replace("/", d.substring(0, d.length()-1));
						GameMain.sqlQueue.addSqlTask(new SqlModel(replace, new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
						//扣除元宝记录
						//DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{});
						String replace2 = sqlx.toString().replace("$", ve.substring(0, ve.length()-1));
						GameMain.sqlQueue.addSqlTask(new SqlModel(replace2, new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
					}	
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			//RoomManage.unLock(roomNo);
		} finally {
			RoomManage.unLock(roomNo.toString());
		}
	}

	/**
	 * 退出房间
	 * @param client
	 * @param data
	 */
	public void exitRoom(SocketIOClient client, Object data) {
		//System.out.println("当前接收到的离线数据为:"+data.toString());

		JSONObject objData= JSONObject.fromObject(data);

		// 房间号
		Object roomNo=client==null?objData.get("roomNo"):client.get(Constant.ROOM_KEY_BDX);
		//UUID sessionId=client==null?UUID.fromString(objData.getString("uuid")):client.getSessionId();
		/*String account;
		long userId;
		UUID sessionId;
		if(client!=null){
			JSONObject userinfo = client.get("userinfo");
			userId = userinfo.getLong("id");
			account = userinfo.getString("account");
			sessionId=client.getSessionId();
		}else{
			sessionId=UUID.fromString(objData.getString("uuid"));
			account=objData.getString("account");
			userId=objData.getLong("id");
		}*/

		if (!Dto.isNull(roomNo)) {
			try {
				RoomManage.lock(roomNo.toString());
				if(RoomManage.gameRoomMap.containsKey(roomNo)){
					
					BDXGameRoom game = (BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);
					JSONObject result = new JSONObject();
					result.put("type", 2); //退出房间
					//result.put("index", game.getPlayerIndex(account));
					result.put("ISexit", 0);//同意退房
					/*if (game.getGameStatus()==0&&game.getGameStatus()!=5) {
				}else{
					result.put("ISexit", 1);
				}*/
					
					for(String uc:game.getUserAcc()){
						Playerinfo upi=game.getPlayerMap().get(uc);
						result.put("index", game.getPlayerIndex(uc));
						SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_BDX", result);
						}
					}
					RoomManage.gameRoomMap.remove(roomNo);
					
					
					// 更新数据库房间信息
					/*JSONObject room = mjBiz.getRoomInfoByRno((String) roomNo);
				/*if(room!=null&&room.containsKey("id")){


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

						String sql = "update za_gamerooms set "+userIndex+"=0 where id=?";
						int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ room.getLong("id")});
						System.out.println("退出房间，更新条数："+i+";sql="+sql);
					}

				}*/
					String sql1 = "update za_gamerooms set status=-2 where room_no=?";
					GameMain.sqlQueue.addSqlTask(new SqlModel(sql1, new Object[]{roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
					LogUtil.print("没人的房间直接清除："+roomNo);
					
				}
			} catch (Exception e) {
				//RoomManage.unLock(roomNo);
				// TODO: handle exception
			} finally {
				RoomManage.unLock(roomNo.toString());
//			String sql = "update za_gamerooms set user_id0=0,user_id1=0,user_id2=0,user_id3=0 where room_no=?";
//			int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ roomNo});
//			System.out.println("退出房间，更新条数："+i+";sql="+sql);
				// 清除房间用户数据
				//Constant.bdxGameMap.get(roomNo).getPlayerPaiJu().remove(account);
				/*Constant.bdxGameMap.get(roomNo).getPlayerMap().remove(account);
				Constant.bdxGameMap.get(roomNo).getUuidList().remove(sessionId);
				Constant.bdxGameMap.get(roomNo).getUserSet().remove(userId);
				Constant.bdxGameMap.get(roomNo).getUserAcc().remove(account);*/
				
				// 金币场没人的房间直接清除
				//if(Constant.bdxGameMap.get(roomNo).getPlayerMap().size()==0){}
				
				//Constant.bdxGameMap.remove(roomNo);
				//RoomManage.lock(roomNo);
				//RoomManage.unLock(roomNo);
				//DBUtil.executeUpdateBySQL(sql1, new Object[]{roomNo});
			}
		}

	}

	public void reconnectGame(SocketIOClient client, Object data) {

		LogUtil.print("玩家开始重连-->"+client.getSessionId());
		JSONObject obj= JSONObject.fromObject(data);
		// 房间号
		String roomNo = obj.getString("room_no");
		// 用户账号
		String account = obj.getString("account");
		//根据uuid获取用户信息
		JSONObject userinfo = new JSONObject();
		/*if (UserInfoCache.userInfoMap.containsKey(account)) {
			userinfo = UserInfoCache.userInfoMap.get(account);
		}else {
			userinfo = mjBiz.getUserInfoByAccount(account);
			UserInfoCache.userInfoMap.put(account, userinfo);
		}*/
		
		userinfo=mjBiz.getUserInfoByAccount(account);
		if (UserInfoCache.userInfoMap.containsKey(account)) {
			userinfo.put("yuanbao",UserInfoCache.userInfoMap.get(account).getDouble("yuanbao"));
		}
		UserInfoCache.userInfoMap.put(account, userinfo);

		try {

			RoomManage.lock(roomNo);
			if(RoomManage.gameRoomMap.containsKey(roomNo)){

				BDXGameRoom game = (BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);

				System.out.println("当前房间状态："+game.getGameStatus());

				UUID oldUUID = null;

				Playerinfo player = null;
				for (UUID uuid : game.getUuidList()) {
					if(game.getPlayerMap().get(account)!=null&&game.getPlayerMap().get(account).getUuid().equals(uuid)){
						oldUUID = uuid;
						player = game.getPlayerMap().get(account);
						//player.setStatus(Constant.ONLINE_STATUS_YES);
					}
				}


				// 重置玩家sessionid
				if(player!=null){
					// 获取玩家位置
					UUID newUUID = client.getSessionId();
					LogUtil.print("oldUUID="+oldUUID+"; newUUID="+newUUID);
					// 更新sessionId
					//Playerinfo playerPaiJu = game.getPlayerMap().get(account);
					//game.getPlayerMap().get(oldUUID).setUuid(newUUID);
					((BDXGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUuidList().remove(oldUUID);
					((BDXGameRoom)RoomManage.gameRoomMap.get(roomNo)).getUuidList().add(newUUID);
					player.setUuid(newUUID);

					// 设置会话信息
					client.set(Constant.ROOM_KEY_BDX, roomNo);
					client.set("userinfo", userinfo);
					client.set("userAccount", account);
					LogUtil.print(account+":重连成功！");
				}

				if(game.getGameStatus()>=0){ //游戏已经开局

					if(player!=null){

						// 返回给玩家当前牌局信息（基础信息）
						JSONObject result=new JSONObject();


						JSONObject roomData=new JSONObject();
						roomData.put("room_no", roomNo);
						roomData.put("users",game.getAllPlayer());//告诉他原先加入的玩家
						roomData.put("myIndex",player.getMyIndex());
						roomData.put("isReady",game.getReadyIndex());

						LogUtil.print("断线重连返回="+result.toString());
						result.put("roomData", roomData);
						client.sendEvent("reconnectGamePush_BDX", result);
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
					client.sendEvent("reconnectGamePush_BDX", result);
					LogUtil.print("游戏准备（888）："+ JSONObject.fromObject(result));
				}

				//通知其他人用户重连
				for(String uuid : game.getUserAcc()){
					Playerinfo upi=game.getPlayerMap().get(uuid);
					SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
					if(askclient!=null){
						JSONObject cl = new JSONObject();
						cl.put("index", game.getPlayerIndex(account));
						askclient.sendEvent("userReconnectPush_BDX", cl);
						LogUtil.print("断线重连通知其人："+cl);
					}
				}
			}else{
				JSONObject result = new JSONObject();
				result.put("type", 999); //玩家还未创建房间就已经掉线
				client.sendEvent("reconnectGamePush_BDX", result);
				LogUtil.print("创建房间（999）："+ JSONObject.fromObject(result));
			}
		} catch (Exception e) {
			// TODO: handle exception
			//RoomManage.unLock(roomNo);
		} finally {
			RoomManage.unLock(roomNo);
		}
	}


	/**
	 * 战绩记录
	 * @param room
	 * @param us
	 */
	public void gamelog(BDXGameRoom room, JSONArray us) {
		JSONArray sum = new JSONArray();

		JSONArray jiesuan = new JSONArray();

		int index = -1;
		//int index1 = 0;


		JSONArray ugloga = new JSONArray();
		JSONObject uoc=new JSONObject();
		for(String uuid :room.getUserAcc()){
			// for (UUID uid : room.getPlayerPaiJu().keySet()) {
			JSONObject uglog = new JSONObject();
			JSONObject objt = new JSONObject();
			Playerinfo ui = room.getPlayerMap().get(uuid);

			//战绩存缓存
			Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();
			for (String acc :room.getUserAcc()) {
				Playerinfo uii = room.getPlayerMap().get(acc);
				playerMap.put(acc, new JSONObject().element("score",uii.getLuck() ).element("name", uii.getName()));
			}
			GameLogsCache.addGameLogs(uuid, 10, new GameLogs(room.getRoomNo(), playerMap, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));


			uoc.element(String.valueOf(ui.getId()), ui.getLuck());
			objt.element("score", ui.getLuck());// 单局分数
			objt.element("totalscore", ui.getScore());// 总分
			objt.element("myIndex", room.getPlayerIndex(uuid));
			if (ui.getLuck() == 0) {
				objt.put("isdan", -1);// 平分秋色
			} else if (ui.getLuck() > 0) {
				index=room.getPlayerIndex(uuid);
				objt.put("isdan", 1);// 赢
			} else {
				objt.put("isdan", 0);// 输
			}
			objt.element("win", index);// 谁赢
			objt.element(
					"headimg",
					"http://"
							+ Constant.cfgProperties
							.getProperty("local_remote_ip")
							+ Constant.DOMAIN + ui.getHeadimg());
			objt.element("name", ui.getName());
			objt.element("account", ui.getAccount());

			uglog.put("score", ui.getLuck());
			uglog.put("TotalScore", ui.getScore());
			uglog.put("player", room.getPlayerMap().get(uuid)
					.getName());
			uglog.put("zhuang", room.getPlayerIndex(room.getZhuang()));
			uglog.put(
					"headimg",
					"http://"
							+ Constant.cfgProperties
							.getProperty("local_remote_ip")
							+ Constant.DOMAIN + ui.getHeadimg());
			uglog.put("name", ui.getName());
			uglog.put("account", ui.getAccount());

			ugloga.add(uglog);

			sum.add(objt);

			ui.setStatus(0);
			ui.setLuck(0);
		}

		// 房间信息
		JSONObject roomInfo = mjBiz.getRoomInfoByRno(room.getRoomNo());
		if (roomInfo == null) {
			roomInfo = mjBiz.getRoomInfoByRno1(room.getRoomNo());
		}
		// 查询总战绩
		/**
		 * 去掉select *   wqm 2018/02/26
		 */
		String sql3 = "select id from za_gamelogs where room_no=?  and game_index=? and gid=? and room_id=?";
		JSONArray arr1 = DBUtil.getObjectListBySQL(sql3, new Object[] { room.getRoomNo(),
				room.getGameIndex(), 10, roomInfo.getLong("id") });
		long gamelog_id = 0;
		if (arr1.size() == 0) {
			// （已完结）保存游戏记录
			String sql = "insert into za_gamelogs(gid,room_id,room_no,game_index,base_info,result,jiesuan,finishtime,status) values(?,?,?,?,?,?,?,?,?)";
			DBUtil.executeUpdateBySQL(
					sql,
					new Object[] {
							10,
							roomInfo.getLong("id"),
							room.getRoomNo(),
							room.getGameIndex(),
							roomInfo.getString("base_info"),
							sum.toString(),
							jiesuan.toString(),
							new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
							.format(new Date()), 1 });
		}
		// 查询战绩
		JSONObject result = DBUtil
				.getObjectBySQL(
						"select id from za_gamelogs where gid=? and room_id=? and room_no=? and game_index=?",
						new Object[] { 10, roomInfo.getLong("id"), room.getRoomNo(),
								room.getGameIndex() });
		if (!Dto.isObjNull(result)) {
			gamelog_id = result.getLong("id");
		}

		StringBuffer  sqlx =new StringBuffer();
		sqlx.append("insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,gamelog_id,result,fee,account,createtime) values $");
		String ve="";

		String te =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		Object[] params = new Object[room.getUserSet().size()];
		int temp = 0;
		for (Long uid : room.getUserSet()) {
			ve=ve+"("+10+","+roomInfo.getLong("id")+",'"+room.getRoomNo()+"',"+
					room.getGameIndex()+","+
					uid+","+
					gamelog_id+","+
					"?"+","+
					0+","+
					uoc.getInt(String.valueOf(uid))+",'"+te +"'),";
			params[temp] = ugloga.toString();
			temp ++;
		}

		DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), params);



		// 保存玩家战绩
		/*for (Long uid : room.getUserSet()) {
			String sql2 = "select * from za_usergamelogs where room_id=? and room_no=? and user_id=? and game_index=?";
			JSONArray arr = DBUtil.getObjectListBySQL(sql2, new Object[] {
					roomInfo.getLong("id"), room.getRoomNo(), uid, room.getGameIndex() });
			if (arr.size() == 0) {
				String sql1 = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,gamelog_id,result,createtime) values(?,?,?,?,?,?,?,?)";
				DBUtil.executeUpdateBySQL(
						sql1,
						new Object[] {
								10,
								roomInfo.getLong("id"),
								room.getRoomNo(),
								room.getGameIndex(),
								uid,
								gamelog_id,
								ugloga.toString(),
								new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
										.format(new Date()) });
			}
		}*/

	}

}

class SaveGameLogsBDX extends Thread{
	BDXGameEventDeal bdxGameEventDeal = new BDXGameEventDeal();
	BDXGameRoom room;
	JSONArray us;
	
	public SaveGameLogsBDX(BDXGameRoom room, JSONArray us) {
		this.room = room;
		this.us = us;
	}
	
	public void run(){
		bdxGameEventDeal.gamelog(room, us);
	}
}
