package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.core.sss.SSSSpecialCards;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.biz.model.sss.AutoExitThread;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * @author Administrator
 *
 */
@Component
public class SSSGameEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(SSSGameEventDeal.class);

	SSSService sssService = new SSSServiceImpl();
	MaJiangBiz mjBiz=new MajiangBizImpl();
//	private SSSGameEventDeal SSSGameEventDeal;
	public JSONObject gameset =null;

	/**
	 * 更新用户信息
	 * @param client
	 * @param data
	 */
	public void playerInfo(SocketIOClient client, Object data){
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
		// 用户账号
		String account = postdata.getString("account");
		// 房间号
		String roomNo = postdata.getString("room_no");
		
		JSONObject result=new JSONObject();
		//JSONObject userinfo = new JSONObject();
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
				
			//userinfo=mjBiz.getUserInfoByAccount(account);
			// 通知玩家
			for (String uc : room.getUserAcc()) {
				//JSONObject userinfo=mjBiz.getUserInfoByAccount(uc);
				JSONObject userinfo = new JSONObject();
				/*if (UserInfoCache.userInfoMap.containsKey(uc)) {
					userinfo = UserInfoCache.userInfoMap.get(uc);
				}else {
					userinfo=mjBiz.getUserInfoByAccount(uc);
					UserInfoCache.userInfoMap.put(uc, userinfo);
				}*/
				if (UserInfoCache.userInfoMap.containsKey(account)) {
					userinfo = UserInfoCache.userInfoMap.get(account);
				}else {
					userinfo=mjBiz.getUserInfoByAccount(account);
					UserInfoCache.userInfoMap.put(account, userinfo);
				}
					Playerinfo pi=room.getPlayerMap().get(uc);
					Player u=room.getPlayerPaiJu().get(uc);
					
					if (room.getRoomType()==1) {
						pi.setScore(userinfo.getDouble("coins"));
						u.setTotalScore(userinfo.getDouble("coins"));
					}else if(room.getRoomType()==3){
						pi.setScore(userinfo.getDouble("yuanbao"));
						u.setTotalScore(userinfo.getDouble("yuanbao"));
					}
					result.put("users",room.getAllPlayer());
					
					if (room.getRoomType()==3) {
						if (room.getMinscore()>u.getTotalScore()) {
								result.put("isYuanBao", 1);
							}else{
								result.put("isYuanBao", 0);
							}					
						}
						SocketIOClient clientother= GameMain.server.getClient(pi.getUuid());
						if(clientother!=null){
							clientother.sendEvent("playerGameInfo_SSS", result);
						}
					}
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：playerInfo_SSS，返回：playerGameInfo_SSS，时间："+(end-sta));
		}
	}
	

	/**
	 * 创建、加入房间
	 * @param client
	 * @param data
	 * 旧版
	 */
	public void enterRoom(SocketIOClient client, Object data) {

		long sta = System.currentTimeMillis();
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
		//JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
		if (gameset==null) {
			System.out.println("查询游戏设置");
			gameset = mjBiz.getGameInfoByID(4);	
		}
		
		try {
			RoomManage.lock(roomNo);
		
			
			
			int myIndex = -1;
			//GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
			SSSGameRoom gameRoom=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if (RoomManage.gameRoomMap.containsKey(roomNo)) {
				if (RoomManage.gameRoomMap.get(roomNo)==null) {
					LogUtil.print("房间为空2导致加入房间失败,房间号为"+roomNo);
				}
			}
			if(gameRoom!=null){
				
				
				int roomType = gameRoom.getRoomType();
				//if(room.containsKey("roomtype")&&room.get("roomtype")!=null){
					
				//}
				
				
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
					//String base_info = gameRoom.getRoomInfo().toString();
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
						tu=objInfo.getJSONObject("turn").getInt("turn")+"局/";
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
							//player.setScore(userinfo.getInt("coins"));
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
						//player.setScore(userinfo.getInt("score"));
						player.setHeadimg(userinfo.getString("headimg"));
						player.setStatus(Constant.ONLINE_STATUS_YES);
						// 设置幸运值
						if(userinfo.containsKey("luck")){
							player.setLuck(userinfo.getInt("luck"));
						}
						// 工会名称
						if (userinfo.containsKey("ghName")) {
							player.setGhName(userinfo.getString("ghName"));
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
						SSSGameRoom sssGame = sssService.createGameRoom(room1, client.getSessionId(), objInfo, player,gameRoom);
						
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
						
						//sssGame.setGameType(objInfo.getInt("type"));
						client.set(Constant.ROOM_KEY_SSS, roomNo);
						
						
						
						obj.put("player",rs);//人数
										
						obj.put("users",sssGame.getAllPlayer());//告诉他原先加入的玩家
						obj.put("myIndex",player.getMyIndex());
						obj.put("isReady",sssGame.getReadyIndex());
						int rt=gameRoom.getReadyTime();
						if (rt==0) {
							rt=gameRoom.getSetting().getInt("goldready");
						}
						//obj.put("readyTime", rt);
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
						
						////LogUtil.print("创建房间："+obj);
						result.put("data", obj);
						client.sendEvent("enterRoomPush_SSS", result);
						
						//if (Constant.sssGameMap.get(roomNo).getRoomType()==1&&Constant.sssGameMap.get(roomNo).getThread()==null) 
						/*if (Constant.sssGameMap.get(roomNo).getThread()==null)
						{
							try {
								JSONObject json = new JSONObject();
								json.element("type", 1);
								MutliThreadSSS1 m = new MutliThreadSSS1(roomNo);
								m.start();
								Constant.sssGameMap.get(roomNo).setThread(m);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}*/
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
							//player.setScore(userinfo.getInt("coins"));
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
						//player.setScore(userinfo.getInt("score"));
						player.setHeadimg(userinfo.getString("headimg"));
						player.setStatus(Constant.ONLINE_STATUS_YES);
						// 保存用户坐标
						if(postdata.containsKey("location")){
							player.setLocation(postdata.getString("location"));
						}
						// 工会名称
						if (userinfo.containsKey("ghName")) {
							player.setGhName(userinfo.getString("ghName"));
						}
						// 设置幸运值
						if(userinfo.containsKey("luck")){
							player.setLuck(userinfo.getInt("luck"));
						}
						//加入房间
						boolean is= sssService.joinGameRoom(roomNo, client.getSessionId(), player,roomType);
						client.set(Constant.ROOM_KEY_SSS, roomNo);
						System.err.println("acc:"+account+is);
						ConcurrentSkipListSet<UUID> uuids=gameRoom.getUuidList();//获取原来房间里的人
						//Constant.sssGameMap.get(roomNo).setSetting(gameset.getJSONObject("setting"));
						if(roomType==3){rs="最低"+rs;}
						obj.put("player",rs);//人数
						/*obj.put("type",roomName);//玩法
						obj.put("turn",tu);//房卡型
						obj.put("ys",ys);//加色
						obj.put("ma",jm);//马牌
						obj.put("pt",pt);//房卡付款类型	
						obj.put("dz",dz);//底注				
						obj.put("rc",rc);//入场					
						obj.put("lc",lc);//离场	
	*/					obj.put("users",gameRoom.getAllPlayer());//告诉他原先加入的玩家
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
								if(gameRoom.getPlayerPaiJu().get(uuid).getStatus()==2){
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
								playerObj.put("sex", player.getSex());
								playerObj.put("status", player.getStatus());
								playerObj.put("id", player.getAccount());
								playerObj.put("ghName", player.getGhName());
								obj.put("user", playerObj);
								
								
								//obj1.put("readyTime", rt);
								re.put("data", obj);
								
	//							for(UUID other:uuids){
	//								if(!other.equals(client.getSessionId())){
	//									SocketIOClient clientother=GameMain.server.getClient(other);
	//									if(clientother!=null){
	//										clientother.sendEvent("playerEnterPush_SSS", result);
	//									}
	//								}
	//							}
								for (String string : gameRoom.getPlayerMap().keySet()) {
									if (!gameRoom.getRobotList().contains(string)) {
										SocketIOClient clientother=GameMain.server.getClient(gameRoom.getPlayerMap().get(string).getUuid());
										if(clientother!=null){
											clientother.sendEvent("playerEnterPush_SSS", re);
										}
									}
								}
							}
						}
						//SSSGameRoom gameRoom = Constant.sssGameMap.get(roomNo);
						//每有一个非机器人且不是断线重连的用户加入房间则机器人列表长度减1
						/*if (gameRoom.isRobot()&&!gameRoom.getRobotList().contains(account)&&gameRoom.getRobotList().size()>0&&is) {
							List<String> list = new ArrayList<String>();
							for (int i = 0; i < gameRoom.getRobotList().size()-1; i++) {
								list.add(gameRoom.getRobotList().get(i));
							}
							gameRoom.setRobotList(list);
							String sql = "update za_users set status=0 where account=?";
							DBUtil.executeUpdateBySQL(sql, new Object[]{gameRoom.getRobotList().get(gameRoom.getRobotList().size()-1)});
						}*/
	//					if (Constant.sssGameMap.get(roomNo).getRoomType()==1&& Constant.sssGameMap.get(roomNo).getThread()==null) 
					/*	if (Constant.sssGameMap.get(roomNo).getThread()==null)
						{
							try {
								JSONObject json = new JSONObject();
								json.element("type", 1);
								MutliThreadSSS1 m = new MutliThreadSSS1(roomNo);
								m.start();
								Constant.sssGameMap.get(roomNo).setThread(m);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}*/
						 
						
					  }	
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：enterRoom_SSS，返回：enterRoomPush_SSS，时间："+(end-sta));
		}

	}

	

	/**
	 * 玩家准备事件
	 * @param client
	 * @param data
	 * @return 
	 */
	public  void gameReady(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
	
		// 房间号
		String roomNo = postdata.getString("room_no");
		try {
			RoomManage.lock(roomNo);
		
		/*RoomManage.unLock(roomNo);
		RoomManage.gameRoomMap.get(roomNo);
		*/
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
			/*if(Constant.sssGameMap.containsKey(roomNo)){*/
				 
				SSSGameRoom room= (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
				//SSSGameRoom room=Constant.sssGameMap.get(roomNo);//获取房间
				/*
				if (room.getGameStatus()>1) {
					return;
				}*/
				String ac ;
				if (room.isRobot()&&postdata.containsKey("robotAccount")) {
					ac = postdata.getString("robotAccount");
				}else {
					ac = client.get("userAccount").toString();
				}
				
				// 准备
				sssService.isReady(roomNo, ac);
				
				//线程 
				/*if (Constant.sssGameMap.get(roomNo).getThread()==null)
				{
					try {
						JSONObject json = new JSONObject();
						json.element("type", 1);
						MutliThreadSSS1 m = new MutliThreadSSS1(roomNo);
						m.start();
						Constant.sssGameMap.get(roomNo).setThread(m);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*/
				
				if (room.getReadyCount()<room.getPlayerCount()) {
					GameMain.singleTime.deleteTimer(roomNo);
				}else{
					/*JSONObject postdata=new JSONObject();
					postdata.element("room_no", roomNo);
					postdata.element("thred", true);*/
					//定时器
					
					if (!GameMain.singleTime.getM_map().containsKey(roomNo)) {
						//GameMain.singleTime.deleteTimer(roomNo);
						System.out.println("进入");
						TimerMsgData tmd=new TimerMsgData();
						tmd.nTimeLimit=room.getSetting().getInt("goldready");
						tmd.nType=10;
						tmd.roomid=roomNo;
						tmd.client=client;
						tmd.data=postdata;
						tmd.gid=4;
						tmd.gmd= new Messages(client, postdata, 4, 10);
						GameMain.singleTime.createTimer(tmd);
					}
				}
				
				JSONObject result1 = new JSONObject();
				result1.put("myIndex", room.getPlayerIndex(ac));
				result1.put("isReady", room.getReadyIndex());
				result1.put("users",room.getAllPlayer());//告诉他原先加入的玩家
				if (room.getRoomType()==3) {
					if (room.getReadyCount()>=room.getPlayerCount()&&room.getUserAcc().size()>=room.getPlayerCount()) {
						System.err.println("传出readyTime"+room.getReadyTime());
						if (room.getReadyTime()<=1) {
							
							result1.put("readyTime", room.getSetting().getInt("goldready"));
							//result1.put("readyTime", room.getTimeLeft());
						}else{
							result1.put("readyTime", room.getReadyTime());
						}
					}
					
				}else if(room.getRoomType()==1){
					if (room.getReadyCount()>=2) {
						System.err.println("传出readyTime"+room.getReadyTime());
						if (room.getReadyTime()==0) {
							
							result1.put("readyTime", room.getSetting().getInt("goldready"));
						}else{
							result1.put("readyTime", room.getReadyTime());
						}
					}
				}
				
				if (!postdata.containsKey("thred")) {
				
					// 通知玩家
					for (String uc : room.getUserAcc()) {
						Playerinfo pi=room.getPlayerMap().get(uc);
						Player u=room.getPlayerPaiJu().get(uc);
						if (room.getRoomType()==3) {
							if (room.getMinscore()>u.getTotalScore()) {
								result1.put("isYuanBao", 1);
							}else{
								result1.put("isYuanBao", 0);
							}					
						}
						if (!room.getRobotList().contains(uc)) {
		
							SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
							if(clientother!=null){
								clientother.sendEvent("playerReadyPush_SSS", result1);
							}
						}
					}
					
				}
				
				int count = 0;
				for (String uid:room.getPlayerPaiJu().keySet()) {
					int ready = room.getPlayerPaiJu().get(uid).getIsReady();
					if(ready==1){
						count++;
					}
				}	
				
				if((room.getRoomType()==1&& room.isAllReady() && count>1)
						|| 
						(count >= room.getPlayerCount()
						&&room.isAllReady())
					){
					GameMain.singleTime.deleteTimer(roomNo);
					//游戏开始 、发牌
					gameStart(data);
				
					
					
					/*System.out.println("进入发牌");
					if (room.isRobot()) {
						RobotThreadSSS robotThreadSSS = new RobotThreadSSS(1, roomNo, sssService);
						robotThreadSSS.start();
						System.err.println("机器人线程");
					}
					
					if (room.getRoomType()==3||room.getRoomType()==1) {
						playerExit( new JSONObject().element("room_no", room.getRoomNo()));
					}
					
					// 重置准备
					//room.setReadyCount(0);
					synchronized (this) {
						// 定庄
						sssService.dingZhuang(roomNo, 0);
						// 洗牌
						sssService.xiPai(roomNo);
						// 发牌
						sssService.faPai(roomNo);
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
					
					
					JSONArray userIds=new JSONArray();
					// 通知玩家
					for (String uc : room.getUserAcc()) {
						
						JSONObject result = new JSONObject();
						
						 String[] p=SSSGameRoom.daxiao(room.getPlayerPaiJu().get(uc).getPai());
						// //LogUtil.print("【发牌阶段】原牌："+Arrays.toString(room.getPlayerPaiJu().get(uc).getPai())+"，排序："+Arrays.toString(p));
						//重新排序插入数据
						 room.getPlayerPaiJu().get(uc).setPai(p);
						 
						int[] pai = room.getPlayerPaiJu().get(uc).getMyPai();
						////LogUtil.print("【发牌阶段】转化之后"+Arrays.toString(pai));
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
						
						if (room.getPeipaiTime()==0) {
							if (room.getRoomType()==0||room.getRoomType()==2) {
								//room.setPeipaiTime(room.getSetting().getInt("fangkapeipai"));//重置配牌时间
								result.put("peipaiTime", room.getSetting().getInt("fangkapeipai"));
							}else{
								//room.setPeipaiTime(room.getSetting().getInt("goldpeipai"));
								result.put("peipaiTime", room.getSetting().getInt("goldpeipai"));
							}
							
						}else{
							result.put("peipaiTime", room.getPeipaiTime());
						}
						
						//result.put("peipaiTime", room.getPeipaiTime());
						result.put("myPaiType", SSSSpecialCards.isSpecialCards(room.getPlayerPaiJu().get(uc).getPai(),room.getSetting()));
						//String msg="游戏ID：4,房间号："+roomNo+",第"+room.getGameIndex()+"局,用户："+uc+",发牌："+Arrays.toString(room.getPlayerPaiJu().get(uc).getPai());
						////LogUtil.print(msg);
						
						System.err.println(result.toString());
						Playerinfo pi=room.getPlayerMap().get(uc);
						
						userIds.add(pi.getId());
						if (!room.getRobotList().contains(uc)) {
							SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
							if(clientother!=null){
								clientother.sendEvent("gameStartPush_SSS", result);
							}
						}
					}
					
					
					// 玩家扣服务费
					if(room.getFee()>0){
						System.err.println("抽水后台记录");
						if(room.getRoomType()==1){ // 金币模式
							
								mjBiz.dealGoldRoomFee(userIds, roomNo, 4, room.getFee(), "2");
							
						}else if(room.getRoomType()==3){ // 元宝模式
							//+mjBiz.dealGoldRoomFee(userIds, roomNo, 4, room.getFee(), "3");
							mjBiz.pump(userIds, roomNo, 4, room.getFee(), "yuanbao");
						}
						
						// 刷新玩家余额
						for (String uuid : room.getPlayerMap().keySet()) {
							
							JSONObject result = new JSONObject();
							result.put("users",room.getAllPlayer());
							Playerinfo pi=room.getPlayerMap().get(uuid);
							SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
							if(clientother!=null){
								clientother.sendEvent("userInfoPush_NN", result);
							}
						}
					}
					
				*/}
				/*else{
					
					JSONObject result = new JSONObject();
					result.put("myIndex", room.getPlayerIndex(client.getSessionId()));
					result.put("isReady", room.getReadyIndex());
					
					// 通知玩家
					for (UUID uuid : room.getUuidList()) {
						SocketIOClient clientother=GameMain.server.getClient(uuid);
						if(clientother!=null){
							clientother.sendEvent("playerReadyPush_SSS", result);
						}
					}
				}*/
				
			}
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			RoomManage.unLock(roomNo);
		}
		
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：gameReady_SSS，返回：playerReadyPush_SSS，时间："+(end-sta));
		}

	}
	
	/**
	 * 游戏开始
	 * @param data
	 */
	public void gameStart(Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
	
		// 房间号
		String roomNo = postdata.getString("room_no");
		System.out.println("游戏开始，进入发牌");
		try {
			//RoomManage.lock(roomNo);
		
		
			if(RoomManage.gameRoomMap.containsKey(roomNo)){
				
				SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
				int count = 0;
				for (String uid:room.getPlayerPaiJu().keySet()) {
					int ready = room.getPlayerPaiJu().get(uid).getIsReady();
					if(ready==1){
						count++;
					}
				}
				if(count<room.getPlayerCount()){
					RoomManage.unLock(roomNo);
					return;
				}
				if (room.getRoomType()==3||room.getRoomType()==1) {
					System.out.println("游戏开始,踢人");
					playerExit( new JSONObject().element("room_no", room.getRoomNo()));
				}
				
				// 重置准备
				//room.setReadyCount(0);
				//synchronized (this) {}
					room.setReadyTime(0);
					
					// 定庄
					sssService.dingZhuang(roomNo, 0);
					// 洗牌
					sssService.xiPai(roomNo);
					// 发牌
					sssService.faPai(roomNo);
				
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
				for (String uc : room.getUserAcc()) {
					
					JSONObject result = new JSONObject();
					
					 String[] p=SSSGameRoom.daxiao(room.getPlayerPaiJu().get(uc).getPai());
					// //LogUtil.print("【发牌阶段】原牌："+Arrays.toString(room.getPlayerPaiJu().get(uc).getPai())+"，排序："+Arrays.toString(p));
					//重新排序插入数据
					 room.getPlayerPaiJu().get(uc).setPai(p);
					 
					int[] pai = room.getPlayerPaiJu().get(uc).getMyPai();
					////LogUtil.print("【发牌阶段】转化之后"+Arrays.toString(pai));
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
							//room.setPeipaiTime(room.getSetting().getInt("fangkapeipai"));//重置配牌时间
							result.put("peipaiTime", room.getSetting().getInt("fangkapeipai"));
						}else{
							//room.setPeipaiTime(room.getSetting().getInt("goldpeipai"));
							result.put("peipaiTime", room.getSetting().getInt("goldpeipai"));
						}
						
					/*	if (room.getPeipaiTime()==0) {
					}else{
						result.put("peipaiTime", room.getPeipaiTime());
					}*/
					
					//result.put("peipaiTime", room.getPeipaiTime());
					result.put("myPaiType", SSSSpecialCards.isSpecialCards(room.getPlayerPaiJu().get(uc).getPai(),room.getSetting()));
					//String msg="游戏ID：4,房间号："+roomNo+",第"+room.getGameIndex()+"局,用户："+uc+",发牌："+Arrays.toString(room.getPlayerPaiJu().get(uc).getPai());
					////LogUtil.print(msg);
					
					System.err.println(result.toString());
					Playerinfo pi=room.getPlayerMap().get(uc);
					
					userIds.add(pi.getId());
					if (!room.getRobotList().contains(uc)) {
						SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
						if(clientother!=null){
							clientother.sendEvent("gameStartPush_SSS", result);
						}
					}
				}
				
				
				if (room.getGameStatus()>=3) {
					GameMain.singleTime.deleteTimer(roomNo);
					postdata.element("type", 1);
					postdata.element("times", 1);
					//定时器
					System.out.println("配牌倒记时进入");
					TimerMsgData tmd=new TimerMsgData();
					tmd.nTimeLimit=room.getSetting().getInt("goldpeipai");
					tmd.nType=11;
					tmd.roomid=roomNo;
					tmd.client=null;
					tmd.data=postdata;
					tmd.gid=4;
					tmd.gmd= new Messages(null, postdata, 4, 11);
					GameMain.singleTime.createTimer(tmd);
				}else{
					System.out.println("配牌倒记时没有进入");
				}
				
				
				// 玩家扣服务费
				/*if(room.getFee()>0){
					System.err.println("抽水后台记录");
					if(room.getRoomType()==1){ // 金币模式
						
							mjBiz.dealGoldRoomFee(userIds, roomNo, 4, room.getFee(), "2");
						
					}else if(room.getRoomType()==3){ // 元宝模式
						//+mjBiz.dealGoldRoomFee(userIds, roomNo, 4, room.getFee(), "3");
						mjBiz.pump(userIds, roomNo, 4, room.getFee(), "yuanbao");
					}
				}*/
				
			}
			
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			//RoomManage.unLock(roomNo);
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			
			LogUtil.print("游戏：十三水，结束方法：gameStart_SSS，返回：gameStartPush_SSS，时间："+(end-sta));
		}

	}

	
	/**
	 * 游戏事件
	 * @param client
	 * @param data
	 */
	public void gameEvent(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
		String roomNo;
		if (postdata.containsKey("room_no")) {
			roomNo=postdata.getString("room_no");
		}else{
			roomNo=client.get(Constant.ROOM_KEY_SSS);
		}
		String account;
		if (postdata.containsKey("account")) {
			account=postdata.getString("account");
		}else{
			account=client.get("userAccount").toString();
		}
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			System.out.println("请求ID"+client.getSessionId());

			int type = postdata.getInt("type");
			System.out.println("牌组合选择："+type);
			
			sssService.peiPai(roomNo,account, type, postdata);
			
			synchronized (this) {
				System.out.println("进入游戏结算");
				postdata.element("roomNo", roomNo);
				gameEnd(postdata);
			}
			
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			
			LogUtil.print("游戏：十三水，结束方法：gameEvent_SSS，返回：gameActionPush_SSS，时间："+(end-sta));
		}

	}
	 
	/**
	 * 游戏结算
	 * @param data
	 */
	public void gameEnd(Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
	
		
		// 房间号
		String roomNo;
		int gametype1 = 0;
		if (postdata.containsKey("room_no")) {
			roomNo = postdata.getString("room_no");
		}else{
			roomNo = postdata.getString("roomNo");
		}
		
		try {
			RoomManage.lock(roomNo);
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			
			if (postdata.containsKey("times")) {
				System.out.println("定时器自动摆牌");
				for (String uuid : room.getPlayerPaiJu().keySet()) {
					if(room.getPlayerPaiJu().get(uuid).getStatus()==1)
						sssService.peiPai(room.getRoomNo(), uuid, 1, postdata);
				}
			}
			
			gametype1 = room.getGameType();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			RoomManage.unLock(roomNo);
		}

		if (gametype1==1) {
			//霸王庄
			System.out.println("霸王庄");
			sssService.jieSuan(roomNo);
			
		}else{
			//普通玩法
			System.out.println("普通玩法");
			sssService.jieSuan1(roomNo);
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			
			LogUtil.print("游戏：十三水，结束方法：gameEnd_SSS，返回：gameActionPush_SSS，时间："+(end-sta));
		}

	}

	/**
	 * 解散房间
	 * @param client
	 * @param data
	 */
	public void closeRoom(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		// 房间号
		String roomNo=client.get(Constant.ROOM_KEY_SSS);
		try {
			RoomManage.lock(roomNo);
			
			if(RoomManage.gameRoomMap.containsKey(roomNo)&&((SSSGameRoom) RoomManage.gameRoomMap.get(roomNo)).getPlayerPaiJu().get(client.get("userAccount"))!=null){
				
				JSONObject obj= JSONObject.fromObject(data);
				SSSGameRoom game = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
	
				/**
				 *if (game.getUuidList().size()<game.getPlayerCount()&&client.getSessionId()!=game.getFangzhu()&&obj.getInt("type")!=0) {
				if (game.getPlayerPaiJu().get(client.getSessionId()).getIsReady()==0&&game.getGameIndex()==0&&client.getSessionId()!=game.getFangzhu()&&obj.getInt("type")!=0) {
	>>>>>>> .r831
					exitRoom(client, data);
				}else if(client.getSessionId()==game.getFangzhu()&&game.getGameIndex()==0){
				*/ 
				if (game.getUserAcc().size()<=game.getPlayerCount()&&(game.getGameStatus()==0||game.getGameStatus()==5)&&game.getGameIndex()==0&&!game.getFangzhu().equals(client.get("userAccount").toString())&&obj.getInt("type")!=0) {
						exitRoom(client, data);
				}else if(game.getUserAcc().size()<game.getPlayerCount()&&game.getFangzhu().equals(client.get("userAccount").toString())&&game.getGameIndex()==0&&obj.getInt("type")!=0&&(game.getGameStatus()==0||game.getGameStatus()==5)){
				
					System.out.println("房主解散房间");
					JSONObject result = new JSONObject();
					//解散房间之前发动总结算
					if (game.getGameIndex()>0) {
					
						result.put("isSummary", 1);
					}else{
						result.put("isSummary", 0);
					}
					result.put("type", 1); //解散房间
					result.put("result", 1);//房主解散
					for(String uc:game.getUserAcc()){
						Playerinfo upi=game.getPlayerMap().get(uc);
						SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_SSS", result);
						}
					}
					if (game.getGameIndex()>0) {
						obj.element("room_no", roomNo);
						obj.element("jiesan", 1);
						obj.element("account", client.get("userAccount"));
						System.out.println("解散出结算");
						gameSummary(client, obj);
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
				}else{
						
					boolean isClose = false;
					if(obj.getInt("type")==1){//同意解散房间
						
						if(game.getCloseTime()==0||game.getCloseTime()==-1){
									System.out.println("解散时间塞入："+game.getSetting().getInt("jiesan"));
									game.setCloseTime(game.getSetting().getInt("jiesan"));
								}
						if (game.getExitThread()==null)
								{
									System.out.println("开启解散定时器");
									// 开启解散定时器，开始计时
									AutoExitThread m = new AutoExitThread(sssService, roomNo, 0);
									m.start();
									game.setExitThread(m);
								}
						
						
						game.getPlayerPaiJu().get(client.get("userAccount")).isCloseRoom=1; 
						//game.getPlayerPaiJu().get(client.getSessionId()).setIsReady(-1);
					}else if(obj.getInt("type")==0){//拒绝解散房间
						
						game.getPlayerPaiJu().get(client.get("userAccount")).isCloseRoom=-1;
						//game.getPlayerPaiJu().get(client.getSessionId()).setIsReady(-2);
						////LogUtil.print("【房间"+roomNo+"】有人拒绝退出，停止解散线程0");
						if(game.getExitThread()!=null){
							game.getExitThread().setExit(false);
							game.setExitThread(null);
						} 
						game.setCloseTime(-1);
						/*List<String> names = new ArrayList<String>();
						names.add(game.getPlayerMap().get(client.getSessionId()).getName());
						JSONObject result = new JSONObject();
						result.put("type", 1); //解散房间
						result.put("result", 0);
						result.put("user", names.toArray());
						for(UUID uuid:game.getUuidList()){
							// 重置准备状态
							game.getPlayerPaiJu().get(uuid).setIsReady(1);
							game.getPlayerPaiJu().get(uuid).isCloseRoom=0;
							SocketIOClient askclient=GameMain.server.getClient(uuid);
							if(askclient!=null){
								askclient.sendEvent("exitRoomPush_SSS", result);
							}
						}*/
						isClose=true;
					}else if(obj.getInt("type")==-1){ //倒计时后强制解散房间
						
						isClose=true;
					}
					
					JSONArray array = new JSONArray();
					int agree = 0; // 同意人数
					int refuse = 0;//拒绝人数
					List<String> names = new ArrayList<String>();
					for(String uuid:game.getUserAcc()){
						
						JSONObject result = new JSONObject();
						result.put("name", game.getPlayerMap().get(uuid).getName());
						result.put("index", game.getPlayerMap().get(uuid).getMyIndex());
						if(game.getPlayerPaiJu().get(uuid).isCloseRoom==1){
							result.put("result", 1);
							agree++;
						}else if(game.getPlayerPaiJu().get(uuid).isCloseRoom==-1){
							result.put("result", -1);
							refuse++;
							names.add(game.getPlayerMap().get(uuid).getName());
						}else{
							result.put("result", 0);
						}
						result.put("jiesanTime", game.getCloseTime());
						array.add(result);
					}
					
					if(agree+refuse==game.getUserAcc().size()){
						
						JSONObject result = new JSONObject();
						if(game.getPlayerCount()!=game.getReadyCount()&&game.getPlayerPaiJu().get(game.getFangzhu()).isCloseRoom==1
								&&game.getFangzhu().equals(client.get("userAccount"))){ //游戏未开始阶段，房主可以直接解散房间
							////LogUtil.print("【房间"+roomNo+"】解散1");
							//解散房间之前发动总结算
							if (game.getGameIndex()>0) {
								
								result.put("isSummary", 1);
							}else{
								result.put("isSummary", 0);
							}
							result.put("type", 1); //解散房间
							result.put("result", 1);//房主解散
							for(String uc:game.getUserAcc()){
								Playerinfo upi=game.getPlayerMap().get(uc);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_SSS", result);
								}
							}
							if (game.getGameIndex()>0) {
								obj.element("room_no", roomNo);
								obj.element("jiesan", 1);
								obj.element("account", client.get("userAccount"));
								System.out.println("解散出结算1");
								gameSummary(client, obj);
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
							Constant.sssGameMap.remove(roomNo);
							RoomManage.gameRoomMap.remove(roomNo);
						}else if(refuse==0){ //所有人都同意退出房间
							
							////LogUtil.print("【房间"+roomNo+"】解散2");
					
							//解散房间之前发动总结算
							if (game.getGameIndex()>0) {
								result.put("isSummary", 1);
							}else{
								result.put("isSummary", 0);
							}
							
							result.put("type", 1); //解散房间
							result.put("result", 1);
							for(String uc:game.getUserAcc()){
								Playerinfo upi=game.getPlayerMap().get(uc);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_SSS", result);
								}
							}
							
							if (game.getGameIndex()>0) {
								obj.element("room_no", roomNo);
								obj.element("jiesan", 1);
								obj.element("account", client.get("userAccount"));
								System.out.println("解散出结算2");
								gameSummary(client, obj);
							}
							
							// 更新房间信息
							JSONObject room = mjBiz.getRoomInfoByRno(roomNo);
							if(room!=null&&room.containsKey("id")){
								String sql = "update za_gamerooms set status=? where id=?";
								DBUtil.executeUpdateBySQL(sql, new Object[]{-1, room.getLong("id")});
								if(room.getInt("roomtype") == 2){
									//代开房间房间解散，新建房间
									GlobalService.insertGameRoom(roomNo);
								}
							}
							//设置一个无限大的数 确保房间线程终止
							game.setPlayerCount(999);
							
							// 清除房间缓存数据
							Constant.sssGameMap.remove(roomNo);
							RoomManage.gameRoomMap.remove(roomNo);
						}else{ // 有人拒绝退出
							////LogUtil.print("【房间"+roomNo+"】有人拒绝退出，停止解散线程0");
							if(game.getExitThread()!=null){
								game.getExitThread().setExit(false);
								game.setExitThread(null);
							} 
							game.setCloseTime(-1);
							result.put("type", 1); //解散房间
							result.put("result", 0);
							result.put("user", names.toArray());
							for(String uc:game.getUserAcc()){
								// 重置准备状态
								//game.getPlayerPaiJu().get(uuid).setIsReady(1);
								game.getPlayerPaiJu().get(uc).isCloseRoom=0;
								Playerinfo upi=game.getPlayerMap().get(uc);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_SSS", result);
								}
							}
						}
						
					}else{ 
						
						if(isClose&&refuse==0){
							//LogUtil.print("【房间"+roomNo+"】解散3");
							JSONObject result = new JSONObject();
							//解散房间之前发动总结算
							if (game.getGameIndex()>0) {
								
								result.put("isSummary", 1);
							}else{
								result.put("isSummary", 0);
							}
							result.put("type", 1); //解散房间
							result.put("result", 1);
							
							for(String uc:game.getUserAcc()){
								Playerinfo upi=game.getPlayerMap().get(uc);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_SSS", result);
								}
							}
							if (game.getGameIndex()>0) {
								obj.element("room_no", roomNo);
								obj.element("jiesan", 1);
								obj.element("account", client.get("userAccount"));
								System.out.println("解散出结算3");
								gameSummary(client, obj);
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
							Constant.sssGameMap.remove(roomNo);
							RoomManage.gameRoomMap.remove(roomNo);
							
						}else if(isClose&&refuse>0){
							//LogUtil.print("【房间"+roomNo+"】有人拒绝退出，停止解散线程1");
							if(game.getExitThread()!=null){
								game.getExitThread().setExit(false);
								game.setExitThread(null);
							} 
							game.setCloseTime(-1);
							JSONObject result = new JSONObject();
							result.put("type", 1); //解散房间
							result.put("result", 0);
							result.put("user", names.toArray());
							for(String uc:game.getUserAcc()){
								// 重置准备状态
								//game.getPlayerPaiJu().get(uuid).setIsReady(1);
								Playerinfo upi=game.getPlayerMap().get(uc);
								game.getPlayerPaiJu().get(uc).isCloseRoom=0;
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("exitRoomPush_SSS", result);
								}
							}
						}else{ //通知其他人退出申请
							
							//开启自动解散线程 
							//线程锁防止多开 不过感觉没什么必要 
							//爱删就删吧╮(╯_╰)╭
							/*synchronized (this) {
								if(game.getExitThread()==null){
									AutoExitThread at = new AutoExitThread(sssService, roomNo, 0);
									at.start();
									game.setExitThread(at);
								}					
							}*/
							
							for(String uc:game.getUserAcc()){
								Playerinfo upi=game.getPlayerMap().get(uc);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								if(askclient!=null){
									askclient.sendEvent("closeRoomPush_SSS", array);
								}
							}
						}
					}
				 }
			}
		
		 } catch (Exception e) {
				e.printStackTrace();
			}finally{
				RoomManage.unLock(roomNo);
			}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：closeRoom_SSS，返回：closeRoomPush_SSS、exitRoomPush_SSS，时间："+(end-sta));
		}

	}
	
	
	/**
	 * 退出房间
	 * @param client
	 * @param data
	 */
 	public void exitRoom(SocketIOClient client, Object data) {
 		long sta = System.currentTimeMillis();
		JSONObject objData=new JSONObject();
		if(client==null) 
			objData= JSONObject.fromObject(data);
		
		// 房间号
		//String roomNo=client==null?objData.getString("roomNo"):client.get(Constant.ROOM_KEY_SSS);
		//UUID sessionId=client==null?UUID.fromString(objData.getString("uuid")):client.getSessionId();
		//String account=client==null?objData.getString("account"):client.get("userAccount").toString();
		long userId;
		UUID sessionId;
		String roomNo;
		String account;
		if(client!=null){
			JSONObject userinfo = client.get("userinfo");
			userId = userinfo.getLong("id");
			account = userinfo.getString("account");
			sessionId=client.getSessionId();
			roomNo=client.get(Constant.ROOM_KEY_SSS);
		}else{
			sessionId=UUID.fromString(objData.getString("uuid"));
			account=objData.getString("account");
			userId=objData.getLong("id");
			roomNo=objData.getString("roomNo");
		}
		
		
		//System.out.println(Constant.sssGameMap);
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			SSSGameRoom game =(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if (game.getPlayerMap().get(account)!=null) {
				JSONObject result = new JSONObject();
				result.put("type", 2); //退出房间
				result.put("index", game.getPlayerIndex(account));
					//System.out.println("退出房间状态："+game.getGameStatus()+"，用户："+ account+"，用户状态："+ game.getPlayerPaiJu().get(account).getIsReady());
					//game.getPlayerPaiJu().size()<game.getPlayerCount()
				if ((game.getGameStatus()==0||game.getPlayerPaiJu().get(account).getIsReady()!=1)&&game.getGameStatus()!=5) {
						result.put("ISexit", 0);//同意退房
					}else{
						result.put("ISexit", 1);
					}
				int count = 0;
				for (String uid:game.getPlayerPaiJu().keySet()) {
					int ready = game.getPlayerPaiJu().get(uid).getIsReady();
					if(ready==1){
						count++;
					}
				}
				if (game.getPlayerPaiJu().get(account).getIsReady()==1&&count-1<game.getPlayerCount()) {
					result.put("readyTime", 0);
				}
				for(String uc:game.getUserAcc()){
					Playerinfo upi=game.getPlayerMap().get(uc);
					if (!game.getRobotList().contains(upi.getAccount())) {
						SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
						if(askclient!=null){
							askclient.sendEvent("exitRoomPush_SSS", result);
						}
					}
				}
			
			if ((game.getGameStatus()==0||game.getPlayerPaiJu().get(account).getIsReady()!=1)&&game.getGameStatus()!=5) {
			
				RoomManage roomManage = new RoomManage();
				roomManage.playerExit(account, userId, roomNo);
				
				// 清除房间用户数据
				game.getPlayerMap().remove(account);
				game.getPlayerPaiJu().remove(account);
				game.getUserAcc().remove(account);
				game.getUserSet().remove(userId);
				game.getUuidList().remove(sessionId);
				int count1 = 0;
				for (String uid:game.getPlayerPaiJu().keySet()) {
					if(game.getPlayerPaiJu().get(uid).getIsReady()==1){
						count1++;
					}
				}
				game.setReadyCount(count1);
		
				if (game.getGameStatus()==0) {
					if (count1<game.getPlayerCount()) {
						GameMain.singleTime.deleteTimer(roomNo);
					}else{
						if (count1 >= game.getPlayerCount()&&game.isAllReady()) {
							JSONObject postdata=new JSONObject();
							postdata.element("room_no", roomNo);
							postdata.element("thred", true);
							//定时器
							//GameMain.singleTime.deleteTimer(roomNo);
							System.out.println("进入");
							TimerMsgData tmd=new TimerMsgData();
							tmd.nTimeLimit=1;
							tmd.nType=10;
							tmd.roomid=roomNo;
							tmd.client=client;
							tmd.data=postdata;
							tmd.gid=4;
							tmd.gmd= new Messages(client, postdata, 4, 10);
							GameMain.singleTime.createTimer(tmd);
	
						}else{
							
							JSONObject postdata=new JSONObject();
							postdata.element("room_no", roomNo);
							//postdata.element("thred", true);
							//定时器
							//GameMain.singleTime.deleteTimer(roomNo);
							System.out.println("进入");
							TimerMsgData tmd=new TimerMsgData();
							tmd.nTimeLimit=game.getTimeLeft();
							tmd.nType=10;
							tmd.roomid=roomNo;
							tmd.client=client;
							tmd.data=postdata;
							tmd.gid=4;
							tmd.gmd= new Messages(client, postdata, 4, 10);
							if (!GameMain.singleTime.getM_map().containsKey(roomNo)) {
								GameMain.singleTime.createTimer(tmd);
							}
						}
					}
				}
					
					//全是机器人删除房间
					boolean deleteRoom = true;
					for (String ac : game.getPlayerMap().keySet()) {
						if (!game.getRobotList().contains(ac)) {
							deleteRoom = false;
							break;
						}
					}
					
						// 金币场没人的房间直接清除
						if((game.getRoomType()==1||game.getRoomType()==3)&&game.getPlayerMap().size()==0||deleteRoom){
							if (Constant.sssGameMap.get(roomNo).getThread()!=null) {
								Constant.sssGameMap.get(roomNo).getThread().setExit(false);
							}
							if (deleteRoom) {
								for (String uid1 : Constant.sssGameMap.get(roomNo).getRobotList()) {
									String sql = "update za_users set status=0 where account=?";
									DBUtil.executeUpdateBySQL(sql, new Object[]{uid1});
								}
							}
							Constant.sssGameMap.remove(roomNo);
							RoomManage.gameRoomMap.remove(roomNo);
							String sql = "update za_gamerooms set status=-2 where room_no=?";
							GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
							//DBUtil.executeUpdateBySQL(sql, new Object[]{roomNo});
							//LogUtil.print("金币场没人的房间直接清除："+roomNo);
							}
					}
			}
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：exitRoom_SSS，返回：exitRoomPush_SSS，时间："+(end-sta));
		}

	}
	

	public void reconnectGame(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		JSONObject obj= JSONObject.fromObject(data);
		// 房间号
		String roomNo = obj.getString("room_no");
		// 用户账号
		String account = obj.getString("account");
		//LogUtil.print("玩家开始重连-->"+account);
		
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			SSSGameRoom game = (SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
			
			System.out.println("当前房间状态："+game.getGameStatus());
			
			UUID oldUUID = null;
			
			Playerinfo player = null;
			for (UUID uuid : game.getUuidList()) {
				if(game.getPlayerMap().get(account)!=null&&game.getPlayerMap().get(account).getUuid().equals(uuid)){
					oldUUID = uuid;
					player = game.getPlayerMap().get(account);
					player.setStatus(Constant.ONLINE_STATUS_YES);
				}
			}
			if (player==null) {
				LogUtil.print("重连玩家"+account+"------当前房间内玩家"+game.getPlayerMap().keySet());
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
				client.set("userinfo", userinfo);
				client.set("userAccount", account);
				//LogUtil.print(account+":重连成功！");
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
						for(String uuid:game.getUserAcc()){
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
					
					if(game.getGameStatus()<3){ // 准备阶段
						
							//判断是否准备
							if(game.getPlayerPaiJu().get(account).getIsReady()==1){
								result.put("type", 0);
							}else{
								
								result.put("type", 0);
								int count1 = 0;
								for (String uid:game.getPlayerPaiJu().keySet()) {
									if(game.getPlayerPaiJu().get(uid).getIsReady()==1){
										count1++;
									}
								}
								if(((game.getRoomType()==1||game.getRoomType()==3) && game.isAllReady() && count1>1)|| count1 >= game.getPlayerCount()){
									// 重置准备
									//game.setReadyCount(0);
									
									//int[] pai = game.getPlayerPaiJu().get(client.getSessionId()).getMyPai();
									//result.put("myPai", pai);
									result.put("myIndex", game.getPlayerIndex(account));
									if (game.getGameType()==1) {
										result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
									}
									if (game.getReadyCount()>=game.getPlayerCount()&&game.getUserAcc().size()>=game.getPlayerCount()) {
										System.err.println("传出readyTime"+game.getReadyTime());
										//result1.put("readyTime", room.getReadyTime());
										//result.put("readyTime", game.getReadyTime());
										result.put("readyTime", game.getTimeLeft());
									}		
									result.put("game_index", game.getGameIndex()+1);
									//int sc= SSSSpecialCards.isSpecialCards(game.getPlayerPaiJu().get(client.getSessionId()).getPai());
									//result.put("myPaiType", sc);//牌
									result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());
								}else{
									if (game.getRoomType()==0&&game.getGameCount()<game.getGameIndex()+1) {
										result.put("type", 4);
									}
									result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());
									result.put("myIndex", game.getPlayerIndex(account));
									result.put("isReady", game.getReadyIndex());
									if (game.getReadyCount()>=game.getPlayerCount()&&game.getUserAcc().size()>=game.getPlayerCount()) {
										System.err.println("传出readyTime"+game.getReadyTime());
										//result1.put("readyTime", room.getReadyTime());
										//result.put("readyTime", game.getReadyTime());
										result.put("readyTime", game.getTimeLeft());
									}
									if (game.getGameType()==1) {
										result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
									}
								}
							}
						
						
						
						
					}else if(game.getGameStatus()==3||game.getGameStatus()==4){ // 发牌、配牌阶段
						
						result.put("type", 1);
						if (game.getGameType()==1) {
							result.put("zhuang", game.getPlayerIndex(game.getZhuang()));
						}
						//result.put("zhuang", game.getPlayerMap().get(game.getZhuang()).getMyIndex());
						if(game.getPlayerPaiJu().get(account).getStatus()==2){
							result.put("status", 1);
						}else{
							result.put("status", 0);
						}
						
						//result.put("peipaiTime", game.getPeipaiTime());
						result.put("peipaiTime", game.getTimeLeft());
						
						if(game.getPlayerPaiJu().get(account).getPai()!=null){
							result.put("myPaiType", SSSSpecialCards.isSpecialCards(game.getPlayerPaiJu().get(account).getPai(),game.getSetting()));
							result.put("myPai", game.getPlayerPaiJu().get(account).getMyPai());
						}
						
						result.put("myIndex",  game.getPlayerIndex(account));
						
						
						result.put("totalscore", game.getPlayerPaiJu().get(account).getTotalScore());
						
						
							JSONArray chu=new JSONArray();
						for (String uuid : game.getPlayerPaiJu().keySet()) {
							if(game.getPlayerPaiJu().get(uuid).getStatus()==2){
							   chu.add(game.getPlayerIndex(uuid));
							}
						}
						result.put("chupai", chu);
						result.put("gameIndex", game.getGameIndex()+1);
						result.put("maPai", ma);
					
						

					}else if(game.getGameStatus()==5){ // 结算阶段
						
						if(game.getPlayerPaiJu().get(account).getIsReady()==1){ // 玩家已经准备
							result.put("type", 0);
						}else{
							result.put("type", 3);
							Player uuu = game.getPlayerPaiJu().get(account);
							
							JSONArray data1=new JSONArray();
							int index=0;
							int index1=0;
							
							JSONArray us=new JSONArray();
							for (String uuid : game.getPlayerPaiJu().keySet()) {
								if(game.getPlayerPaiJu().get(uuid).getStatus()==2){ // 获取当前已配好牌的玩家
									us.add(uuid);
								}
							}
							for (int i = 0; i < us.size(); i++) {
							/*for (String uid : game.getPlayerPaiJu().keySet()) {*/
							
								Player u1 = game.getPlayerPaiJu().get(us.getString(i));
								int d=0;
								int dd=0;
								for (String uuid : game.getPlayerPaiJu().keySet()) {
								Player u2 = game.getPlayerPaiJu().get(uuid);
									
									if (!us.getString(i).equals(uuid)&&u1.getTotalScore()>u2.getTotalScore()) {
										d++;
									}
									if (!us.getString(i).equals(uuid)&&u1.getScore()>u2.getScore()) {
										dd++;
									}
								}
								if (d==game.getPlayerPaiJu().keySet().size()-1) {
									index=game.getPlayerIndex(us.getString(i));
								}
								if (dd==game.getPlayerPaiJu().keySet().size()-1) {
									index1=game.getPlayerIndex(us.getString(i));
									
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
							//game.getPlayerPaiJu().get(client.getSessionId()).setStatus(0);
					
						}
					}	
					
					JSONObject roomData=new JSONObject();
					roomData.put("room_no", roomNo);
					roomData.put("users",game.getAllPlayer());//告诉他原先加入的玩家
					roomData.put("myIndex",player.getMyIndex());
					roomData.put("isReady",game.getReadyIndex());
					roomData.put("gameStatus",game.getGameStatus());
					
					if (game.getReadyCount()>=game.getPlayerCount()&&game.getUserAcc().size()>=game.getPlayerCount()&&game.getGameStatus()<3) {
						System.err.println("传出readyTime"+game.getReadyTime());
						//result1.put("readyTime", room.getReadyTime());
						//result.put("readyTime", game.getReadyTime());
						result.put("readyTime", game.getTimeLeft());
					}	
					result.put("roomData", roomData);
					
					//LogUtil.print("断线重连返回="+result.toString());
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
				//LogUtil.print("游戏准备（888）："+JSONObject.fromObject(result));
			}
			
			//通知其他人用户重连
			for(String uuid : game.getUserAcc()){
				Playerinfo upi=game.getPlayerMap().get(uuid);
				if (!game.getRobotList().contains(upi.getAccount())) {
					SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
					if(askclient!=null){
						JSONObject cl = new JSONObject();
						cl.put("index", game.getPlayerIndex(account));
						askclient.sendEvent("userReconnectPush_SSS", cl);
						//LogUtil.print("断线重连通知其人："+cl);
					}
				}
			}
		}else{
			JSONObject result = new JSONObject();
			result.put("type", 999); //玩家还未创建房间就已经掉线
			client.sendEvent("reconnectGamePush_SSS", result);
			//LogUtil.print("创建房间（999）："+JSONObject.fromObject(result));
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			
			LogUtil.print("游戏：十三水，结束方法：reconnectGame_SSS，返回：reconnectGamePush_SSS，时间："+(end-sta));
		}

	}
	

	public void gameConnReset(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			JSONObject userinfo = client.get("userinfo");
			//LogUtil.print("玩家开始重连判断-->"+userinfo.getString("account"));
			if(!room.getUuidList().contains(client.getSessionId())){ //判断玩家是否是重新进入游戏
				// 重连恢复游戏
				JSONObject dataJson = new JSONObject();
				dataJson.put("room_no", roomNo);
				dataJson.put("account", userinfo.getString("account"));
				System.err.println(userinfo.toString());
				reconnectGame(client, dataJson);
			}else{
				//LogUtil.print("玩家不需要重连-->"+userinfo.getString("account"));
			}
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			
			LogUtil.print("游戏：十三水，结束方法：gameConnReset_SSS，返回：reconnectGamePush_SSS，时间："+(end-sta));
		}

	}
	
	
	public void gameSummary(SocketIOClient client, Object data) {
		long sta = System.currentTimeMillis();
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			if (room.getGameStatus()>2||room.getGameStatus()==0) {
				
				JSONArray us=new JSONArray();
				
				if (postdata.containsKey("jiesan")&&postdata.getInt("jiesan")==1) {
					for (String uuid : room.getPlayerPaiJu().keySet()) {
						us.add(uuid);
					}
					/*SaveLogsThreadSSS sl=new SaveLogsThreadSSS(room, us,true);
					sl.start();*/
				}else{
					for (String uuid : room.getPlayerPaiJu().keySet()) {
						if(room.getPlayerPaiJu().get(uuid).getStatus()==2){ // 获取当前已配好牌的玩家
							us.add(uuid);
						}
					}
				}
				System.out.println("几人发牌结算:"+us.size());
				if (room.getGameStatus()==5||room.getGameStatus()==0) {}
						JSONArray sum=new JSONArray();
						
						JSONArray jiesuan=new JSONArray();
						
						int index=-1;
						int index1=0;
						for (int i = 0; i < us.size(); i++) {
						//for (UUID id : room.getPlayerPaiJu().keySet()) {
							Player u1 = room.getPlayerPaiJu().get(us.getString(i));
							int d=0;
							int dd=0;
							for (int ii = 0; ii < us.size(); ii++) {
							//for (UUID ud : room.getPlayerPaiJu().keySet()) {
								Player u2 = room.getPlayerPaiJu().get(us.getString(ii));
								
								if (us.getString(i)!=us.getString(ii)&&u1.getTotalScore()>u2.getTotalScore()) {
									d++;
								}
								if (us.getString(i)!=us.getString(ii)&&u1.getScore()>u2.getScore()) {
									dd++;
								}
							}
							if (d==room.getPlayerPaiJu().keySet().size()-1) {
								index=room.getPlayerIndex(us.getString(i));
								
							}
							if (dd==room.getPlayerPaiJu().keySet().size()-1) {
								index1=room.getPlayerIndex(us.getString(i));
								
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
						//for (UUID uid : room.getPlayerPaiJu().keySet()) {
							JSONObject uglog = new JSONObject();
							JSONObject obj=new JSONObject();
							Player u = room.getPlayerPaiJu().get(us.getString(j));
							Playerinfo ui= room.getPlayerMap().get(us.getString(j));
							obj.element("gun", u.getGun());//打枪
							obj.element("swat", u.getSwat());//全垒打
							obj.element("special", u.getSpecial());//特殊牌
							obj.element("ordinary", u.getOrdinary());//普通牌
							/*if (room.getRoomType()==1) {
								obj.element("score", u.getScore());//单局分数
							}else{
								obj.element("score", u.getTotalScore());//总分
							}*/
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
							
							/*if (index==-1) {
								obj.put("isWinner", -1);//平分秋色
							}else*/
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
							System.err.println("总局："+room.getGameCount()+",当前："+room.getGameIndex());
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

						
							//SocketIOClient askclient=GameMain.server.getClient(client.getSessionId());
							//String msg="游戏ID：4,房间号："+roomNo+",第"+room.getGameIndex()+"局,用户："+client.get("userAccount").toString()+",结算牌："+Arrays.toString(room.getPlayerPaiJu().get(client.get("userAccount").toString()).getPai());
							//LogUtil.print(msg);
							if(client!=null){
								client.sendEvent("UserGameSummary_SSS", sum);
								if (Dto.stringIsNULL(client.get("userAccount").toString())) {
									room.getPlayerPaiJu().get(postdata.getString("account")).setIsReady(0);
								}else{
									room.getPlayerPaiJu().get(client.get("userAccount").toString()).setIsReady(0);
								}
							}else{
								
								SocketIOClient askclient=GameMain.server.getClient(room.getPlayerMap().get(postdata.getString("account")).getUuid());
								askclient.sendEvent("UserGameSummary_SSS", sum);
								room.getPlayerPaiJu().get(postdata.getString("account")).setIsReady(0);
							
							}
							//room.getPlayerPaiJu().get(client.getSessionId()).setIsReady(-1);//已出结算
						/*		if (room.getRoomType()==1||room.getRoomType()==3) {
							}*/
							//LogUtil.print("【房间"+roomNo+"】结算");
						}else{
							
							//通知其他人用户
							for(String uuid :room.getUserAcc()){
								Playerinfo upi=room.getPlayerMap().get(uuid);
								SocketIOClient askclient=GameMain.server.getClient(upi.getUuid());
								//String msg="游戏ID：4,房间号："+roomNo+",第"+room.getGameIndex()+"局,用户："+uuid+",结算牌："+Arrays.toString(room.getPlayerPaiJu().get(uuid).getPai());
								//LogUtil.print(msg);
								
								if(askclient!=null){
									askclient.sendEvent("UserGameSummary_SSS", sum);
								}
								//room.getPlayerPaiJu().get(uuid).setIsReady(-1);//已出结算
								
									// 重置玩家状态信息
										room.getPlayerPaiJu().get(uuid).setIsReady(0);
										room.getPlayerPaiJu().get(uuid).setStatus(Constant.ONLINE_STATUS_YES);
								/*		if (room.getRoomType()==1||room.getRoomType()==3) {
								}*/
							}
						}

						room.setGameStatus(0);
						room.setReadyCount(0);
						/*if (room.isRobot()) {
							new RobotThreadSSS(3, roomNo, sssService).start();
							
						}*/

						/*if(room.getRoomType()==1||room.getRoomType()==3){
						}*/

			}
		}
		long end = System.currentTimeMillis();
		if ((end-sta)>50) {
			LogUtil.print("游戏：十三水，结束方法：gameSummary_SSS，返回：UserGameSummary_SSS，时间："+(end-sta));
		}

	}
	public void playerExit(Object data) {
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);//获取房间
			if (room.getRoomType()==1||room.getRoomType()==3) {			
				JSONArray deluser=new JSONArray();
				for(String uid : room.getPlayerMap().keySet()){
					Playerinfo uinfo = room.getPlayerMap().get(uid);
					Player u = room.getPlayerPaiJu().get(uid);
					logger.info("踢人方法！用户："+uid+",状态："+u.getIsReady()+",倒计时："+room.getReadyTime()+",配牌："+u.getIsAuto());
					if ((u.getTotalScore()<room.getMinscore()&&room.getLevel()!=-1)||(u.getIsAuto()==1&&(room.getRoomType()==1||room.getRoomType()==3))||(room.getReadyTime()==1&&u.getIsReady()!=1&&(room.getRoomType()==1||room.getRoomType()==3))) {
						//不准备、离线、分数不够、自动比牌 踢出去
						JSONObject tmpObj=new JSONObject();
						tmpObj.element("roomNo", roomNo).element("uuid", uinfo.getUuid().toString())
						.element("id", uinfo.getId()).element("account", uinfo.getAccount());
						
						deluser.add(tmpObj);
					}
				}
				for (int i = 0; i < deluser.size(); i++) {
					exitRoom(null, deluser.getJSONObject(i));
				}
				
			}		
		}	
	}
}
