package com.zhuoan.biz.service.nn.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NiuNiu;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.Packer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.event.nn.AutoThreadNN;
import com.zhuoan.biz.event.nn.NNGameEventDeal;
import com.zhuoan.biz.model.*;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.Messages;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.times.TimerMsgData;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class NiuNiuServiceImpl implements NiuNiuService {

    private final static Logger logger = LoggerFactory.getLogger(NiuNiuServiceImpl.class);

	MaJiangBiz mjBiz=new MajiangBizImpl();
	public static NNGameEventDeal nnGameEventDeal = new NNGameEventDeal();

	@Override
	public NNGameRoom createGameRoom(JSONObject roomObj, String uuid, Playerinfo player) {


		// 房间属性信息
		String base_info = roomObj.getString("base_info");
		JSONObject objInfo = JSONObject.fromObject(base_info);
		// 房间号
		String roomNo = roomObj.getString("room_no");

		NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
		room.setFirstTime(1);
		room.setRoomNo(roomNo);//房间名，唯一值
		room.setPlayerCount(objInfo.getInt("player"));//玩家人数
		room.setZhuangType(objInfo.getInt("type"));//定庄类型（房主霸庄、轮庄）
		if (objInfo.containsKey("robot")&&objInfo.getInt("robot")==1) {//是否加入机器人
			// 是否允许陌生人加入
			if (objInfo.containsKey("open")&&objInfo.getInt("open")==0) {
				room.setRobot(false);
			}else {
				room.setRobot(true);
				List<String> list = mjBiz.getRobotList(room.getPlayerCount()-1);
				room.setRobotList(list);
			}
		}else {
			room.setRobot(false);
		}
		if (objInfo.containsKey("tuizhu")&&objInfo.getInt("tuizhu")==1) {//是否闲家推注
			room.setTuizhu(true);
		}else {
			room.setTuizhu(false);
		}
		if (objInfo.containsKey("guanzhan")&&objInfo.getInt("guanzhan")==1) {//是否观战模式
			room.setGuanzhan(true);
		}else {
			room.setGuanzhan(false);
		}
		room.setFangzhu(uuid);
		room.setZhuang(uuid);
		//底分设置
		if(objInfo.containsKey("di")){
			//room.setScore(objInfo.getInt("di"));
			/**
			 * 金皇冠筹码小数     wqm  2018/02/27
			 */
			room.setScore(objInfo.getDouble("di"));
		}else{
			room.setScore(1);
		}
		if(objInfo.containsKey("yuanbao")&&objInfo.getDouble("yuanbao")>0){ // 元宝模式
			room.setScore(objInfo.getDouble("yuanbao")); //底分
		}
		if(objInfo.containsKey("leaveYB")&&objInfo.getDouble("leaveYB")>0){ // 元宝模式
			room.setGoldcoins(objInfo.getDouble("leaveYB"));
		}
		if(objInfo.containsKey("goldcoins")&&roomObj.getInt("roomtype")==1){
			room.setGoldcoins(objInfo.getInt("goldcoins"));//设置金币场准入金币
		}
		if(objInfo.containsKey("gametype")&&objInfo.getInt("gametype")==1){
			room.setGameType(1);
		}else{
			room.setGameType(0);
		}

		// 设置基本牌型倍数
		if(objInfo.containsKey("niuniuNum")){
			JSONArray nnNums = objInfo.getJSONArray("niuniuNum");
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

		// 设置特殊牌型玩法和倍数
		if(objInfo.containsKey("special")){
			List<Integer> specialType = new ArrayList<Integer>();
			JSONArray types = objInfo.getJSONArray("special");
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
		if(objInfo.containsKey("qzTimes")){
			room.qzTimes = objInfo.getJSONArray("qzTimes");
		}
		// 抢庄是否是随机庄（随机、最高倍数为庄）
		if(objInfo.getJSONObject("turn").containsKey("qzsjzhuang")){
			room.qzsjzhuang = objInfo.getJSONObject("turn").getInt("qzsjzhuang");
		}
		// 没人抢庄
		if (objInfo.containsKey("qznozhuang")&&objInfo.getInt("qznozhuang")==2) {
			room.qznozhuang = 2; // 无人抢庄，重新发牌
		}else if(roomObj.getInt("roomtype")==3){
			room.qznozhuang = -1; // 无人抢庄，房间自动解散
		}else{
			room.qznozhuang = 1; // 无人抢庄，随机庄
		}
		// 是否允许玩家中途加入
		if(objInfo.containsKey("halfwayin")&&objInfo.getInt("halfwayin")==1){
			room.setHalfwayin(true);
		}
		//准备超时（0：不处理 1：自动准备 2：踢出房间）
		if(objInfo.containsKey("readyovertime")){
			if(objInfo.getInt("readyovertime")==0){
				room.setReadyovertime(0);
			}else if(objInfo.getInt("readyovertime")==1){
				room.setReadyovertime(1);
			}else if(objInfo.getInt("readyovertime")==2){
				room.setReadyovertime(2);
			}
		}else if(roomObj.getInt("roomtype")==3){
			room.setReadyovertime(2);
		}else{
			room.setReadyovertime(0);
		}
		// 金币、元宝扣服务费
		if((objInfo.containsKey("fee")&&objInfo.getInt("fee")==1)||roomObj.getInt("roomtype")==3){
			//RoomManage.unLock(roomNo);
			JSONObject gameSetting = mjBiz.getGameSetting();
			//RoomManage.lock(roomNo);
			JSONObject roomFee = gameSetting.getJSONObject("pumpData");
			double fee = roomFee.getDouble("proportion")*room.getScore();
			// 服务费：费率x底注
			if(objInfo.containsKey("custFee")){ // 自定义费率
				fee = objInfo.getDouble("custFee")*room.getScore();
			}else{ // 统一费率
				fee = roomFee.getDouble("proportion")*room.getScore();
			}
			double maxFee = roomFee.getDouble("max");
			double minFee = roomFee.getDouble("min");
			if(fee>maxFee){
				fee = maxFee;
			}else if(fee<minFee){
				fee = minFee;
			}
			fee = new BigDecimal(fee).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
			room.setFee(fee);
		}
		if(objInfo.containsKey("baseNum")){
			room.setBaseNum(objInfo.getJSONArray("baseNum").toString());//设置基础倍率
		}
		room.setRoomType(roomObj.getInt("roomtype"));
		int gameCount = roomObj.getInt("game_count");
		if(gameCount>0){
			room.setGameCount(gameCount);
		}else{
			room.setGameCount(10000000);
		}

		if(roomObj.getInt("game_id")==11){ // 百人牛牛
			room.setFangzhu("10000000");
			room.setZhuang("10000000");// 设置系统庄
			room.setMaxLianzhuang(10);
		}
		ConcurrentMap<String,Playerinfo> users=new ConcurrentHashMap<String,Playerinfo>();
		users.put(uuid, player);
		room.setPlayerMap(users);
		ConcurrentMap<String, UserPacket> userPacketMap = new ConcurrentHashMap<String, UserPacket>();
		userPacketMap.put(uuid, new UserPacket());
		userPacketMap.get(uuid).setLuck(player.getLuck());
		room.setUserPacketMap(userPacketMap);
		CopyOnWriteArraySet<Long> userIDSet = new CopyOnWriteArraySet<Long>();
		userIDSet.add(player.getId());
		room.setUserIDSet(userIDSet);
		//将房间存入缓存
		RoomManage.gameRoomMap.put(roomNo, room);
		// 游戏状态
		((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_READY); // 房间初始状态
		return room;
	}


	@Override
	public boolean joinGameRoom(String roomNo, String uuid, Playerinfo player, boolean isNext) {

		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
			if(room!=null){

				if(room.getUserIDSet().size()<=room.getPlayerCount()){
					List<Long> ids = new ArrayList<Long>();
					for (String uid:room.getPlayerMap().keySet()) {
						if(room.getPlayerMap().get(uid)!=null){

							ids.add(room.getPlayerMap().get(uid).getId());
						}
					}
					if (room.isGuanzhan()) {
						String sql = "update za_gamerooms set user_id"+player.getMyIndex()+"=? where room_no=?";
						//DBUtil.executeUpdateBySQL(sql, new Object[]{0, roomNo});
						GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{0, roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
						// 观战玩家myIndex为-1
						player.setMyIndex(NiuNiu.USERPACKER_STATUS_GUANZHAN);
						room.getGzPlayerMap().put(uuid, player);
						
					}else if(room.getUserIDSet().size()<room.getPlayerCount()&&!ids.contains(player.getId())){ //新加进来的玩家
						room.getPlayerMap().put(uuid, player);//用户的个人信息
						room.getUserPacketMap().put(uuid, new UserPacket());
						room.getUserPacketMap().get(uuid).setLuck(player.getLuck());;
						room.getUserIDSet().add(player.getId());
						RoomManage.gameRoomMap.put(roomNo, room);
						return true;
					}else if(room.getGameStatus()>NiuNiu.GAMESTATUS_READY){ //断线后进来的玩家
						return false;
					}
				}

			}
		}
		return false;
	}


	@Override
	public void showPai(String roomNo, String uuid) {


		// 玩家已经亮牌
		((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).setStatus(NiuNiu.USERPACKER_STATUS_LIANGPAI);

		NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
		int isReady = 0;
		int hasPaiCount = 0;
		for (String uid : room.getUserPacketMap().keySet()) {
			if(room.getUserPacketMap().get(uid).getMyPai().length>0&&room.getUserPacketMap().get(uid).getStatus()!=-1){ // 获取当前已发牌的玩家
				hasPaiCount++;
			}
			if(room.getUserPacketMap().get(uid).getStatus()==NiuNiu.USERPACKER_STATUS_LIANGPAI){ // 获取当前亮牌的玩家
				isReady++;
			}
		}

		if(isReady == hasPaiCount){ // 所有玩家都亮牌完毕

			// 游戏状态
			((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_LIANGPAI);

			// 所有玩家都亮牌完毕，展示结算数据
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
				room.getUserPacketMap().get(uid).setIsReady(0);
				room.getUserPacketMap().get(uid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
			}
			
			// 通知观战玩家
			if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
				for (String uid : room.getGzPlayerMap().keySet()) {
					SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(uid).getUuid());
					if(clientother!=null){
						JSONObject result = new JSONObject();
						result.put("type", 1);
						clientother.sendEvent("showPaiPush_NN", result);
					}
				}
			}
			
			if (room.isRobot()&&room.getRobotList().size()>0) {

				AutoThreadNN a1 = new AutoThreadNN(this, roomNo, 4);
				a1.start();

				AutoThreadNN a = new AutoThreadNN(this, roomNo, 3);
				a.start();
			}

			// 金币场
			//			if(room.getRoomType()==1){
			//
			//				// 开启准备定时器，开始计时
			//				MutliThreadNN m = new MutliThreadNN(this, roomNo, 0);
			//				m.start();
			//				
			//			}

		}else{
			UserPacket packet = room.getUserPacketMap().get(uuid);
			// 通知玩家
			for (String uid  : room.getUserPacketMap().keySet()) {
				if (!room.getRobotList().contains(uid)) {
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
					if(clientother!=null){
						JSONObject result = new JSONObject();
						result.put("type", 0);
						result.put("index", room.getPlayerIndex(uuid));
						result.put("result", packet.type);
						result.put("ratio", packet.getRatio(room)); // 倍率
						LogUtil.print(roomNo+"玩家亮牌:"+result);
						clientother.sendEvent("showPaiPush_NN", result);
					}
				}
			}
			
			if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
				for (String uid : room.getGzPlayerMap().keySet()) {
					SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(uid).getUuid());
					if (clientother!=null) {
						JSONObject result = new JSONObject();
						result.put("type", 0);
						result.put("index", room.getPlayerIndex(uuid));
						result.put("result", packet.type);
						result.put("ratio", packet.getRatio(room)); // 倍率
						clientother.sendEvent("showPaiPush_NN", result);
					}
				}
			}
		}
	}


	/**
	 * 配牛
	 * @param roomNo
	 * @param uuid
	 */
	public void peiNiu(String roomNo, String uuid) {

		NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
		UserPacket packet = room.getUserPacketMap().get(uuid);
		if(uuid.equals(room.getZhuang())){ 
			UserPacket zhuang = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), true, room.getSpecialType());
			packet.type = zhuang.type;
			packet.setWin(zhuang.isWin());
		}else{
			Packer[] ups = room.getUserPacketMap().get(uuid).getPs();
			// 有发牌的玩家
			if(ups!=null&&ups.length>0&&ups[0]!=null){
				UserPacket zhuang = new UserPacket(room.getUserPacketMap().get(room.getZhuang()).getPs(), true, room.getSpecialType());
				UserPacket userpacket = new UserPacket(ups, room.getSpecialType());
				PackerCompare.getWin(userpacket, zhuang);
				packet.type = userpacket.type;
				packet.setWin(userpacket.isWin());
			}
		}
	}

	
	/**
	 * 牛牛结算同步锁
	 * @param room
	 * @return
	 */
	public boolean jieSuanLock(NNGameRoom room) {
		
		if(room.getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
			// 游戏状态
			((NNGameRoom) RoomManage.gameRoomMap.get(room.getRoomNo())).setGameStatus(NiuNiu.GAMESTATUS_JIESUAN);
			return true;
		}
		return false;
	}
	

	@Override
	public void jieSuan(String roomNo) {

		if(RoomManage.gameRoomMap.containsKey(roomNo)){

			NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
			
			if(jieSuanLock(room)){
				
				// 明牌抢庄已发牌
				if(room.getZhuangType()!=3){ 
					
					// 洗牌
					NiuNiuServer.xiPai(roomNo);
					// 发牌
					NiuNiuServer.faPai(roomNo);
					
					NNGameEventDeal.luckyTurning(room);
				}
				
				// 庄家通杀
				boolean tongSha = true;
				// 庄家通赔
				boolean tongPei = true;
				
				// 配牌、积分结算
				if(room.getZhuangType()==5){ // 通比模式
					
					// 获取最终赢家
					String win = niuNiuTongBi(room);
					UserPacket winner = room.getUserPacketMap().get(win);
					
					// 积分结算
					for (String uuid : room.getUserPacketMap().keySet()) {
						
						if(!uuid.equals(win)&&room.getUserPacketMap().get(uuid).getPs()[0]!=null){
							
							// 玩家投注赔付
							for (String xiazhuUUID : room.getUserPacketMap().keySet()) {
								
								if(!xiazhuUUID.equals(win)){
									
									// 获取玩家投注分数
									double score = room.getPlayerMoneyNum(room.getPlayerIndex(xiazhuUUID), room.getPlayerIndex(uuid));
									// 下注玩家信息
									UserPacket xiazhuPacket = room.getUserPacketMap().get(xiazhuUUID);
									// 积分赔率【计算公式：投注分数x牌型倍数（赢家）x牌局底分x(1+抢庄加倍)】
									int qztimes = winner.qzTimes; // 按庄的倍数算
									if(qztimes<=0){
										qztimes = 1;
									}
									double totalScore = score*winner.getRatio(room)*room.getScore()*qztimes;
									xiazhuPacket.setScore(xiazhuPacket.getScore() - totalScore);
									winner.setScore(winner.getScore() + totalScore);
								}
							}
						}
					}
					
				}else{ // 庄闲模式
					
					for (String uuid : room.getUserPacketMap().keySet()) {
						if (room.isTuizhu()) {// 闲家推注
							if (uuid.equals(room.getZhuang())) {// 上局是庄家
								room.getUserPacketMap().get(uuid).setIsBankerLast(1);
							}else {// 上局不是庄家
								room.getUserPacketMap().get(uuid).setIsBankerLast(0);
							}
						}
						// 配牛
						peiNiu(roomNo, uuid);
					}
					
					UserPacket zhuangPacket = room.getUserPacketMap().get(room.getZhuang());
					
					for (String uuid : room.getUserPacketMap().keySet()) {
						
						if(!uuid.equals(room.getZhuang())&&room.getUserPacketMap().get(uuid).getPs()[0]!=null){
							
							// 计算输赢
							UserPacket zhuang = new UserPacket(room.getUserPacketMap().get(room.getZhuang()).getPs(), true, room.getSpecialType());
							UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), room.getSpecialType());
							UserPacket winner = PackerCompare.getWin(userpacket, zhuang);
							
							// 玩家投注赔付
							for (String xiazhuUUID : room.getUserPacketMap().keySet()) {
								
								if(!xiazhuUUID.equals(room.getZhuang())){
									
									// 获取玩家投注分数
									double score = room.getPlayerMoneyNum(room.getPlayerIndex(xiazhuUUID), room.getPlayerIndex(uuid));
									// 下注玩家信息
									UserPacket xiazhuPacket = room.getUserPacketMap().get(xiazhuUUID);
									// 积分赔率【计算公式：投注分数x牌型倍数（赢家）x牌局底分x(1+抢庄加倍)】
									int qztimes = zhuangPacket.qzTimes; // 按庄的倍数算
									//								int qztimes = userpacket.qzTimes; // 按闲家的倍数算
									if(qztimes<=0){
										qztimes = 1;
									}
									double totalScore = score*winner.getRatio(room)*room.getScore()*qztimes;
									// 闲家推注模式判断是否翻倍
									if (room.isTuizhu()) {
										if (room.getUserPacketMap().get(xiazhuUUID).isTzChouma()) {
											totalScore = score;
										}
									}
									// 闲家赢
									if(userpacket.isWin()){
										if (room.isTuizhu()&&uuid.equals(xiazhuUUID)) {
											xiazhuPacket.setWinLast(NiuNiu.USERPACKER_LAST_YES);// 上局赢了
										}
										xiazhuPacket.setScore(xiazhuPacket.getScore() + totalScore);
										zhuangPacket.setScore(zhuangPacket.getScore() - totalScore);
									}else{ // 庄家赢
										if (room.isTuizhu()&&uuid.equals(xiazhuUUID)) {
											xiazhuPacket.setWinLast(NiuNiu.USERPACKER_LAST_NO);// 上局输了
										}
										xiazhuPacket.setScore(xiazhuPacket.getScore() - totalScore);
										zhuangPacket.setScore(zhuangPacket.getScore() + totalScore);
									}
									if (room.isTuizhu()&&uuid.equals(xiazhuUUID)) {
										xiazhuPacket.setTypeLast(xiazhuPacket.type);// 上局牌型
										xiazhuPacket.setIsBankerLast(NiuNiu.USERPACKER_LAST_NO);// 上局不是庄家
										xiazhuPacket.setScoreLast(score);// 上局下注金额
										zhuangPacket.setTypeLast(zhuangPacket.type);// 上局牌型
										zhuangPacket.setIsBankerLast(NiuNiu.USERPACKER_LAST_YES);// 上局是庄家
									}
									
								}
							}
						}
					}
					
					for (String uuid : room.getUserPacketMap().keySet()) {
						
						UserPacket up = room.getUserPacketMap().get(uuid);
						
						// 根据闲家的输赢判断庄家是否通杀或是通赔
						if(!uuid.equals(room.getZhuang())){
							if(up.isWin()){
								tongSha = false;
							}else{
								tongPei = false;
							}
						}
					}
				}
				
				
				// 保存玩家战绩
				JSONArray uglogs = new JSONArray();
				// 保存玩家结算数据
				JSONArray array = new JSONArray();
				// 通知玩家
				for (String uuid : room.getUserPacketMap().keySet()) {
					
					UserPacket up = room.getUserPacketMap().get(uuid);
					if(up.getMyPai()[0]>0){
						// 统计牌局数据
						if(up.type==0){
							room.getUserPacketMap().get(uuid).setWuNiuTimes(up.getWuNiuTimes()+1);
						}else if(up.type>=10){
							room.getUserPacketMap().get(uuid).setNiuNiuTimes(up.getNiuNiuTimes()+1);
						}
						
						JSONObject result = new JSONObject();
						result.put("account", room.getPlayerMap().get(uuid).getAccount());
						result.put("uid", room.getPlayerMap().get(uuid).getId());
						result.put("name", room.getPlayerMap().get(uuid).getName());
						result.put("headimg", room.getPlayerMap().get(uuid).getRealHeadimg());
						if(uuid.equals(room.getZhuang())){
							result.put("zhuang", 1);
						}else{
							result.put("zhuang", 0);
						}
						result.put("myIndex", room.getPlayerIndex(uuid));
						result.put("myPai", up.getSortPai());
						result.put("mingPai", up.getMingPai());
						result.put("result", up.type);
						result.put("ratio", up.getRatio(room)); // 倍率
						result.put("score", up.getScore());
						result.put("myMoney", room.getplaceArrayNums(room.getPlayerIndex(uuid)));
						int win = 0;
						if(room.getZhuangType()==5){ // 通比模式
							
							if(up.getScore()>=0){
								win=1;
							}
						}else{ // 庄闲模式
							
							if(!uuid.equals(room.getZhuang())){
								
								if(up.isWin()){
									win=1;
								}
							}else{
								// 判断庄家输赢
								if(up.getScore()>=0){
									win=1;
								}
							}
						}
						result.put("win", win);
						// 庄通杀、通赔
						int zhuangTongsha = 0;
						if(tongSha){
							zhuangTongsha = 1;
						}else if(tongPei){
							zhuangTongsha = -1;
						}
						result.put("zhuangTongsha", zhuangTongsha);
						
						// 设置玩家总积分
						double score = room.getUserPacketMap().get(uuid).getScore();
						double totalScore = room.getPlayerMap().get(uuid).getScore() + score;
						// 金币、元宝场积分不能为负
						if(room.getRoomType()!=0 && room.getRoomType()!=2){
							if(totalScore<0){
								totalScore = 0;
							}
						}
						room.getPlayerMap().get(uuid).setScore(totalScore);
						result.put("totalScore", room.getPlayerMap().get(uuid).getScore());
						
						array.add(result);
						
						// 玩家战绩
						Playerinfo player = room.getPlayerMap().get(uuid);
						JSONObject uglog = new JSONObject();
						uglog.put("isWinner", result.get("win"));
						uglog.put("score", room.getUserPacketMap().get(uuid).getScore());
						uglog.put("player", player.getName());
						uglog.put("zhuang", room.getPlayerIndex(room.getZhuang()));
						uglogs.add(uglog);
					}
				}
				
				if(room.getZhuangType()!=5){// 庄闲模式
					
					// 设置庄家通杀、通赔次数
					if(tongSha){
						room.getUserPacketMap().get(room.getZhuang()).setTongShaTimes(room.getUserPacketMap().get(room.getZhuang()).getTongShaTimes()+1);
					}else if(tongPei){
						room.getUserPacketMap().get(room.getZhuang()).setTongPeiTimes(room.getUserPacketMap().get(room.getZhuang()).getTongPeiTimes()+1);
					}
				}
				
				// 结算数据返回
				JSONObject obj = new JSONObject();
				
				obj.put("type", 0);
				obj.put("users", array);
				obj.element("playerMoney", room.getMoneyPlace());
				
				//更新房间信息
				/*RoomManage.unLock();
				JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
				RoomManage.lock();*/
				
				// 保存结算汇总数据
				JSONArray jiesuanArray = new JSONArray();
				
				if(room!=null){
					// 房卡模式
					if(room.getRoomType()==0 || room.getRoomType()==2){
						
						// 最后一局结算
						if(room.getGameCount()==room.getGameIndex()+1){ // 房间局数已用完
							
							obj.put("type", 1); // gameActionPush_NN 返回类型： 0普通结算  1 最后一局结算
							obj.put("game_count", room.getGameIndex()+1);
							
							// 获取总结算数据
							jiesuanArray = balance(room);
						}
					}
				}
				
				String zhuang = room.getZhuang();
				// 抢庄模式下不定庄
				if(room.getZhuangType()!=2&&room.getZhuangType()!=3){
					
					// 定庄
					zhuang = NiuNiuServer.dingZhuang(roomNo, room.getZhuangType());
				}
				
				// 非通比模式
				if(room.getZhuangType()!=5){
					obj.put("zhuang", room.getPlayerMap().get(zhuang).getMyIndex());
				}
				
				for (String uuid : room.getUserPacketMap().keySet()) {
					if (!room.getRobotList().contains(uuid)) {
						
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							if(obj.getInt("type")==1){ // 最后一局结算
								obj.put("fangzhu", room.getPlayerMap().get(room.getFangzhu()).getMyIndex());
								obj.put("jiesuanData", jiesuanArray);
								LogUtil.print("【总结算结果：】"+ JSONArray.fromObject(jiesuanArray));
							}
							
							clientother.sendEvent("gameActionPush_NN", obj);
							if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
								for (String string : room.getGzPlayerMap().keySet()) {
									SocketIOClient clientother1=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
									if (clientother1!=null) {
										clientother1.sendEvent("gameActionPush_NN", obj);
									}
								}
							}
						}
						LogUtil.print("结算："+ JSONArray.fromObject(obj));
					}
				}
				
				// 开启搓牌定时器，开始计时
				/*MutliThreadNN m = new MutliThreadNN(this, roomNo, 2);
				m.start();*/
				//定时器
				TimerMsgData tmd=new TimerMsgData();
				tmd.nTimeLimit=NNGameEventDeal.GLOBALTIMER[1];
				tmd.nType=20;
				tmd.roomid=roomNo;
				tmd.client=null;
				tmd.data=new JSONObject().element("room_no", roomNo);
				tmd.gid=1;
				tmd.gmd= new Messages(null, new JSONObject().element("room_no", roomNo), 1, 20);
				GameMain.singleTime.createTimer(tmd);
				
				//更新房间信息
				//JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
				if(room!=null){
					
					int game_index = room.getGameIndex()+1;
					
					// 局数记录
					if(game_index<room.getGameCount()){
						((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setGameIndex(room.getGameIndex()+1);
					}
					
					// 保存游戏记录
					JSONObject gamelog = new JSONObject();
					gamelog.put("gid", 1);
					gamelog.put("room_no", roomNo);
					gamelog.put("game_index", game_index);
					gamelog.put("base_info", room.getRoomInfo());
					gamelog.put("result", obj.toString());
					String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
					gamelog.put("finishtime", nowTime);
					gamelog.put("createtime", nowTime);
					gamelog.put("status", 1);
					if(room.getRoomType()==0 || room.getRoomType()==2){
						gamelog.put("roomtype", 0);
					}else{
						gamelog.put("roomtype", 1);
					}
					
					// 房卡模式
					if(room.getRoomType()==0 || room.getRoomType()==2){
						
						// 最后一局结算
						if(room.getGameCount()==room.getGameIndex()+1){ // 房间局数已用完
							obj.put("type", 1); // gameActionPush_NN 返回类型： 0普通结算  1 最后一局结算
							obj.put("game_count", room.getGameIndex()+1);
							
							// 获取总结算数据
							jiesuanArray = balance(room);
						}
						
						if(jiesuanArray.size()>0){
							
							JSONArray jsArray = JSONArray.fromObject(jiesuanArray);
							for (int i = 0; i < jsArray.size(); i++) {
								JSONObject jsobj = jsArray.getJSONObject(i);
								
								JSONArray datas = new JSONArray();
								// 通杀次数
								if(jsobj.getInt("tongShaTimes")>0){
									JSONObject ts = new JSONObject();
									ts.put("name", "通杀次数");
									ts.put("val", jsobj.getInt("tongShaTimes"));
									datas.add(ts);
								}
								// 通赔次数
								if(jsobj.getInt("tongPeiTimes")>0){
									JSONObject tp = new JSONObject();
									tp.put("name", "通赔次数");
									tp.put("val", jsobj.getInt("tongPeiTimes"));
									datas.add(tp);
								}
								// 牛牛次数
								if(jsobj.getInt("niuNiuTimes")>0){
									JSONObject nn = new JSONObject();
									nn.put("name", "牛牛次数");
									nn.put("val", jsobj.getInt("niuNiuTimes"));
									datas.add(nn);
								}
								// 无牛次数
								if(jsobj.getInt("wuNiuTimes")>0){
									JSONObject wn = new JSONObject();
									wn.put("name", "无牛次数");
									wn.put("val", jsobj.getInt("wuNiuTimes"));
									datas.add(wn);
								}
								// 胜利次数
								if(jsobj.getInt("winTimes")>0){
									JSONObject wt = new JSONObject();
									wt.put("name", "胜利次数");
									wt.put("val", jsobj.getInt("winTimes"));
									datas.add(wt);
								}
								
								jsobj.put("data", datas);
								jsobj.remove("tongShaTimes");
								jsobj.remove("tongPeiTimes");
								jsobj.remove("niuNiuTimes");
								jsobj.remove("wuNiuTimes");
								jsobj.remove("winTimes");
							}
							
							gamelog.put("jiesuan", jsArray.toString());
						}
						
//						long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);
//						
//						// 保存玩家战绩
//						for(Long uid:room.getUserIDSet()){
//							
//							JSONObject usergamelog = new JSONObject();
//							usergamelog.put("gid", 1);
//							usergamelog.put("room_id", roomInfo.getLong("id"));
//							usergamelog.put("room_no", roomNo);
//							usergamelog.put("game_index", game_index);
//							usergamelog.put("user_id", uid);
//							usergamelog.put("gamelog_id", gamelog_id);
//							usergamelog.put("result", uglogs.toString());
//							usergamelog.put("createtime", nowTime);
//							
//							mjBiz.addUserGameLog(usergamelog);
//						}
						//SaveLogsThreadNN saveLogsThreadNN = new SaveLogsThreadNN(mjBiz, room, gamelog,uglogs,1,array);
						//saveLogsThreadNN.start();
						try {
							// 扣除房卡，更改房间局数
//							IService server = (IService) RegisterServer.registry.lookup("sysService");
//							server.settlementRoomNo(roomNo);
							mjBiz.settlementRoomNo(roomNo);
						} catch (Exception e) {
							LogUtil.print("扣除房卡，更改房间局数方法异常："+e.getMessage());
							logger.error("",e);
						}
					}else{
						for (int i = 0; i < array.size(); i++) {
							JSONObject user = array.getJSONObject(i);
							String account = user.getString("account");
							Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();
							for (int j = 0; j < array.size(); j++) {
								playerMap.put(array.getJSONObject(j).getString("account"), new JSONObject().element("score", array.getJSONObject(j).getInt("score")).element("name", array.getJSONObject(j).getString("name")));
							}
							GameLogsCache.addGameLogs(account, 1, new GameLogs(roomNo, playerMap, nowTime));
						}
						/*GameLogsCache.updateGameLogsList(array.getJSONObject(0).getString("account"), 1);
						List<GameLogs> list = GameLogsCache.gameLogsMap.get(array.getJSONObject(0).getString("account")).get(1);
						for (GameLogs gameLogs : list) {
							System.err.println(gameLogs.playerMap);
						}*/
						//GameLogsCache.getGameLogsList(array.getJSONObject(0).getString("account"), 1);
						// 更新房间局数序号
						String sql2 = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
						//DBUtil.executeUpdateBySQL(sql2, new Object[]{roomNo});
						GameMain.sqlQueue.addSqlTask(new SqlModel(sql2, new Object[]{roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
//						long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);
//						
//						// 保存玩家战绩
//						for (int i = 0; i < array.size(); i++) {
//							
//							JSONObject user = array.getJSONObject(i);
//							long uid = roomInfo.getLong("user_id"+user.getInt("myIndex"));
//							
//							JSONObject usergamelog = new JSONObject();
//							usergamelog.put("gid", 1);
//							usergamelog.put("room_id", roomInfo.getLong("id"));
//							usergamelog.put("room_no", roomNo);
//							usergamelog.put("game_index", game_index);
//							usergamelog.put("user_id", uid);
//							usergamelog.put("gamelog_id", gamelog_id);
//							usergamelog.put("result", uglogs.toString());
//							usergamelog.put("createtime", nowTime);
//							usergamelog.put("account", user.get("score"));
//							
//							mjBiz.addUserGameLog(usergamelog);
//						}
						
						//SaveLogsThreadNN saveLogsThreadNN = new SaveLogsThreadNN(mjBiz, room, gamelog,uglogs,2,array);
						//saveLogsThreadNN.start();
						GameMain.sqlQueue.addSqlTask(new SqlModel(SqlModel.SAVELOGS_NN, room, gamelog, uglogs, 2, array));
						
						if(room.getRoomType()==1){ // 金币模式
							String sqlString = "UPDATE za_users SET coins = coins+ CASE id";
							String sqlString2 = " END WHERE id IN (";
							// 金币结算
							for (int i = 0; i < array.size(); i++) {
								
								JSONObject user = array.getJSONObject(i);
								long uid = user.getLong("uid");
								double coins = user.getDouble("score");
								if(coins<0){
									JSONObject userobj = mjBiz.getUserInfoByID(uid);
									if(obj!=null){
										double userCoins = userobj.getDouble("coins");
										if(userCoins+coins<0){
											coins = -userCoins;
										}
									}
								}
								sqlString += " WHEN "+uid+" THEN "+coins;
								if (i==array.size()-1) {
									sqlString2 += uid+")";
								}else {
									sqlString2 += uid+",";
								}
							}
							sqlString += sqlString2;
							DBUtil.executeUpdateBySQL(sqlString, new Object[]{});
						}else if(room.getRoomType()==3){ // 元宝模式
							long start = System.currentTimeMillis();
							//RoomManage.unLock();
							String sqlString = "UPDATE za_users SET yuanbao = yuanbao+ CASE id";
							String sqlString2 = " END WHERE id IN (";
							String addSql = "insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values ";
							//String addSql2 = "insert into za_userdeduction (userid,roomid,roomNO,gid,type,sum,doType,creataTime,memo,platform) values";
							for (int i = 0; i < array.size(); i++) {
								
								JSONObject user = array.getJSONObject(i);
								long uid = user.getLong("uid");
								double yuanbao = user.getDouble("score");
								JSONObject userobj = new JSONObject();
								if (UserInfoCache.userInfoMap.containsKey(user.getString("account"))) {
									userobj = UserInfoCache.userInfoMap.get(user.getString("account"));
								}else {
									userobj = mjBiz.getUserInfoByID(uid);
									UserInfoCache.userInfoMap.put(user.getString("account"), userobj);
								}
								if(yuanbao<0){
									if(userobj!=null){
										double userCoins = userobj.getDouble("yuanbao");
										if(userCoins+yuanbao<0){
											yuanbao = -userCoins;
										}
									}
								}
								addSql +="("+uid+","+1+",'"+roomNo+"',"+3+","+yuanbao+",'"+nowTime+"')";
								/*addSql2 += "("+uid+","+1+",'"+roomNo+"',"+1+","+3+","+(-room.getFee())+","+
										2+",'"+nowTime+"','','"+userobj.getString("platform")+"')";*/
								/*if (room.getFee()>0) {
									yuanbao = Dto.sub(yuanbao, room.getFee());
								}*/
								UserInfoCache.updateUserScore(user.getString("account"), yuanbao-room.getFee(), 3);
								sqlString += " WHEN "+uid+" THEN "+yuanbao;
								if (i==array.size()-1) {
									sqlString2 += uid+")";
								}else {
									sqlString2 += uid+",";
									addSql += ",";
									//addSql2 += ",";
								}
							}
							sqlString += sqlString2;
							//RoomManage.unLock();
							//DBUtil.executeUpdateBySQL(sqlString, new Object[]{});
							//DBUtil.executeUpdateBySQL(addSql, new Object[]{});
							//DBUtil.executeUpdateBySQL(addSql2, new Object[]{});
							Object[] params = new Object[]{};
							GameMain.sqlQueue.addSqlTask(new SqlModel(sqlString, params, SqlModel.EXECUTEUPDATEBYSQL));
							GameMain.sqlQueue.addSqlTask(new SqlModel(addSql, params, SqlModel.EXECUTEUPDATEBYSQL));
							//GameMain.sqlQueue.addSqlTask(new SqlModel(addSql2, params, SqlModel.EXECUTEUPDATEBYSQL));
							long end = System.currentTimeMillis();
							//RoomManage.lock();
							LogUtil.print("牛牛结算耗时:"+(end-start));
							
						}
					}
				}
			}else{
				LogUtil.print("##################结算出错，当前游戏状态："+room.getGameStatus());
			}
		}
	}


	/**
	 * 通比牛牛计算
	 * @param room
	 * @return 
	 */
	private String niuNiuTongBi(NNGameRoom room) {

		String winUUID = null;
		for (String win : room.getUserPacketMap().keySet()) {

			UserPacket winPacket = room.getUserPacketMap().get(win);
			if(winPacket.getMyPai()!=null&&winPacket.getMyPai()[0]>0){
				
				UserPacket winner = new UserPacket(winPacket.getPs(), true, room.getSpecialType());
				// 设置牌型
				winPacket.type=winner.type;
				
				boolean isWin = true;
				for (String uuid : room.getUserPacketMap().keySet()) {
					if(!uuid.equals(win)&&room.getUserPacketMap().get(uuid).getPs()[0]!=null){
						
						UserPacket userpacket = new UserPacket(room.getUserPacketMap().get(uuid).getPs(), room.getSpecialType());
						// 计算玩家输赢
						PackerCompare.getWin(winner, userpacket);
						// 输给其他玩家
						if(!winner.isWin()){
							isWin = false;
						}
					}
				}
				// 通杀
				if(isWin){
					winUUID = win;
					winPacket.setWin(true);
				}else{
					winPacket.setWin(false);
				}
			}
		}
		return winUUID;
	}


	/**
	 * 清除离线或金币不足的玩家
	 * @param room
	 */
	@Override
	public List<String> cleanPlayer(NNGameRoom room){

		List<String> lxList = new ArrayList<String>();
		try {
			Set<String> uuids = room.getPlayerMap().keySet();
			for (String uuid:uuids) {
				Playerinfo player = room.getPlayerMap().get(uuid);
				// 离线玩家
				//				if(player!=null&&player.getStatus()==Constant.ONLINE_STATUS_NO){
				//					lxList.add(uuid);
				//				}
				// 金币、元宝不足玩家
				if(player!=null&&player.getScore()<room.getGoldcoins()){
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
					if(clientother!=null){
						JSONObject obj = new JSONObject();
						// type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
						obj.put("type", 1);
						obj.put("msg", "您的余额不足："+room.getGoldcoins()+"，请先充值。");
						clientother.sendEvent("tipMsgPush_NN", obj);
//						try {
//							Thread.sleep(2000);
//						} catch (InterruptedException e1) {
//							e1.printStackTrace();
//						}
					}
					lxList.add(uuid);
				}
			}

			for (String uuid : lxList) {

				if(room.getPlayerMap().get(uuid)!=null){

					nnGameEventDeal.exitRoom(uuid, room.getRoomNo(), room.getPlayerMap().get(uuid).getId());
				}
			}
		} catch (Exception e) {
			logger.error("",e);
		}
		return lxList;
	}


	/**
	 * 玩家抢庄
	 * @param roomNo
	 * @param result
	 * @param clientId
	 */
	@Override
	public void qiangZhuang(String roomNo, String result, String clientId) {

		NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
		int isReady = 0;
		int qzCount = 0;

		if(Integer.valueOf(result)==1){ // 抢庄
			room.getUserPacketMap().get(clientId).setIsReady(10);
		}else{
			room.getUserPacketMap().get(clientId).setIsReady(Integer.valueOf(result));
		}

		for (String uuid : room.getPlayerMap().keySet()) {
			UserPacket userPacket = room.getUserPacketMap().get(uuid);
			if(userPacket.getIsReady()==10||userPacket.getIsReady()==-1){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
				isReady++;
			}
		}

		boolean isFinish = false;
		// 明牌抢庄（抢庄时）有人中途加入，需要等待下一局
		if(room.getZhuangType()==3){
			int hasPai=0;
			for (String uuid : room.getPlayerMap().keySet()) {
				UserPacket userPacket = room.getUserPacketMap().get(uuid);
				// 获取有发牌的玩家
				if(userPacket.getMingPai()!=null&&userPacket.getMingPai()[0]>0){
					hasPai++;
				}
			}
			// 已发牌的玩家都已准备
			if(isReady == hasPai){
				isFinish = true;
			}
		}

		JSONObject obj = new JSONObject();

		if(isReady >= room.getGameIngIndex().length || isFinish){ // 所有玩家都完成选择

			List<String> uuids = new ArrayList<String>();
			List<String> totalUuids = new ArrayList<String>();

			if(room.qzsjzhuang==1){ // 抢庄随机庄
				for (String uuid : room.getPlayerMap().keySet()) {
					UserPacket up = room.getUserPacketMap().get(uuid);
					if(up.getIsReady()==10){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
						uuids.add(uuid);
					}

					// 获取参与抢庄的玩家
					if(up.getIsReady()==10 || up.getIsReady()==-1){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
						totalUuids.add(uuid);
					}
				}
			}else{

				// 按抢庄加倍大小确定庄家
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

					// 获取参与抢庄的玩家
					if(up.getIsReady()==10 || up.getIsReady()==-1){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
						totalUuids.add(uuid);
					}
				}
			}

			qzCount = uuids.size();
			int uuidIndex = 0;
			if(qzCount==1){
				((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(uuids.get(uuidIndex));
			}else if(qzCount>1){
				uuidIndex = RandomUtils.nextInt(qzCount);
				((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(uuids.get(uuidIndex));
			}else { // 没人抢庄

				if(room.qznozhuang==1){ // 抢庄随机庄

					// 没人抢庄，随机庄家
					uuidIndex = RandomUtils.nextInt(totalUuids.size());
					((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(totalUuids.get(uuidIndex));
					System.out.println("没人抢庄！！！随机庄家");

				}else if(room.qznozhuang==-1){ // 无人抢庄，房间自动解散

					List<String> lxList = new ArrayList<String>();
					for (String uuid:room.getPlayerMap().keySet()) {
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							JSONObject data = new JSONObject();
							// type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
							data.put("type", 1);
							data.put("msg", "无人抢庄，房间自动解散！");
							clientother.sendEvent("tipMsgPush_NN", data);
						}
						lxList.add(uuid);
					}
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					for (String uuid : lxList) {

						if(room.getPlayerMap().get(uuid)!=null){
							nnGameEventDeal.exit(room, uuid, room.getPlayerMap().get(uuid).getId());
						}
					}
					return;
				}else if (room.qznozhuang==2) {
					
					// 重开次数
					room.setRestartTime(room.getRestartTime()+1);
					
					for (String string : room.getUserPacketMap().keySet()) {
						// 通知玩家
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(string));
						if(clientother!=null){
							JSONObject data = new JSONObject();
							// type 0 普通提示 1 弹窗提示  2 弹窗提示+确定退出
							data.put("type", 0);
							data.put("msg", "无人抢庄重新开局");
							clientother.sendEvent("tipMsgPush_NN", data);
						}
					}
					
					// 无人抢庄重新开局
					for (String uid  : room.getUserPacketMap().keySet()) {
						if (!room.getRobotList().contains(uid)) {

							SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
							if(clientother!=null){
								JSONObject result1 = new JSONObject();
								result1.put("type", 2);
								clientother.sendEvent("showPaiPush_NN", result1);
							}
						}
						// 重置玩家状态信息
						room.getUserPacketMap().get(uid).setIsReady(0);
						room.getUserPacketMap().get(uid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
					}
					
					// 游戏状态
					((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(NiuNiu.GAMESTATUS_READY);
					
					return;
				}
			}

			obj.put("type", 1);
			obj.put("zhuang", room.getPlayerIndex(room.getZhuang()));
			obj.put("index", room.getPlayerMap().get(clientId).getMyIndex());
			obj.put("result", result);
			// 抢庄加倍
			if(Integer.valueOf(result)==1){ // 抢庄
				obj.put("value", room.getUserPacketMap().get(clientId).qzTimes);
			}

			// 将确定下来的倍数回传给玩家
			int qzScore = room.getUserPacketMap().get(room.getZhuang()).qzTimes;
			if(qzScore<1){
				qzScore = 1;
			}
			obj.put("qzScore", qzScore);
			obj.put("qzhuangtimes", room.getPlayerQzResult());

			// 开启开始游戏定时器
			/*MutliThreadNN m = new MutliThreadNN(this, roomNo, 5);
			m.start();*/
			//定时器
			TimerMsgData tmd=new TimerMsgData();
			tmd.nTimeLimit=0;
			tmd.nType=21;
			tmd.roomid=roomNo;
			tmd.client=null;
			tmd.data=new JSONObject().element("room_no", roomNo);
			tmd.gid=1;
			tmd.gmd= new Messages(null, new JSONObject().element("room_no", roomNo), 1, 21);
			GameMain.singleTime.createTimer(tmd);

		}else{

			obj.put("type", 0);
			obj.put("index", room.getPlayerMap().get(clientId).getMyIndex());
			obj.put("result", result);
			// 抢庄加倍
			if(Integer.valueOf(result)==1){ // 抢庄
				obj.put("value", room.getUserPacketMap().get(clientId).qzTimes);
			}
		}
		// 通知其他玩家
		for (String uid  : room.getUserPacketMap().keySet()) {
			if (!room.getRobotList().contains(uid)) {
				SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
				if(clientother!=null){
					clientother.sendEvent("qiangZhuangPush_NN", obj);
				}
			}
		}
		
		if (room.isGuanzhan()&&room.getGzPlayerMap().size()>0) {
			for (String string : room.getGzPlayerMap().keySet()) {
				SocketIOClient clientother=GameMain.server.getClient(room.getGzPlayerMap().get(string).getUuid());
				if(clientother!=null){
					clientother.sendEvent("qiangZhuangPush_NN", obj);
				}
			}
		}
	}

	@Override
	public JSONArray balance(NNGameRoom room) {

		JSONArray jiesuanArray = new JSONArray();
		Set<String> uuids = room.getPlayerMap().keySet();
		// 大赢家分数
		double dayinjia = 0;
		for(String uuid:uuids){
			if(room.getPlayerMap().get(uuid).getScore()>=dayinjia){
				dayinjia = room.getPlayerMap().get(uuid).getScore();
			}
		}

		// 设置玩家信息
		for(String uuid:uuids){
			Playerinfo player = room.getPlayerMap().get(uuid);
			JSONObject user = new JSONObject();
			user.put("name", player.getName());
			user.put("account", player.getAccount());
			user.put("headimg", player.getRealHeadimg());
			user.put("score", player.getScore());

			if(room.getFangzhu().equals(uuid)){

				user.put("isFangzhu", 1);
			}else{
				user.put("isFangzhu", 0);
			}

			if(player.getScore() == dayinjia){

				user.put("isWinner", 1);
			}else{
				user.put("isWinner", 0);
			}

			UserPacket up = room.getUserPacketMap().get(uuid);
			user.put("tongShaTimes", up.getTongShaTimes());
			user.put("tongPeiTimes", up.getTongPeiTimes());
			user.put("niuNiuTimes", up.getNiuNiuTimes());
			user.put("wuNiuTimes", up.getWuNiuTimes());
			user.put("winTimes", up.getWinTimes());

			jiesuanArray.add(user);
		}
		return jiesuanArray;
	}

}
