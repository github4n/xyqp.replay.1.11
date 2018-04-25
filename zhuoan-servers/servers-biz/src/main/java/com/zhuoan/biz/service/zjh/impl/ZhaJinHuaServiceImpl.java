package com.zhuoan.biz.service.zjh.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.event.zjh.ZJHGameEventDeal;
import com.zhuoan.biz.model.*;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.biz.model.zjh.ZJHGame;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.zjh.ZhaJinHuaService;
import com.zhuoan.constant.Constant;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.LogUtil;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class ZhaJinHuaServiceImpl implements ZhaJinHuaService {

    private final static Logger logger = LoggerFactory.getLogger(ZhaJinHuaServiceImpl.class);

	MaJiangBiz mjBiz=new MajiangBizImpl();
	public static ZJHGameEventDeal zjhGameEventDeal = new ZJHGameEventDeal();
	
	@Override
	public ZJHGame createGameRoom(JSONObject roomObj, String uuid, Playerinfo player) {
		
		// 房间属性信息
		String base_info = roomObj.getString("base_info");
		JSONObject objInfo = JSONObject.fromObject(base_info);
		// 房间号
		String roomNo = roomObj.getString("room_no");
				
		ZJHGame room=(ZJHGame) RoomManage.gameRoomMap.get(roomNo);
		room.setFirstTime(1);
		room.setRoomNo(roomNo);//房间名，唯一值
		room.setRoomType(roomObj.getInt("roomtype"));
		if(room.getRoomType()==3){
			room.setScore(objInfo.getDouble("yuanbao")); //底分
			room.setEnterScore(objInfo.getDouble("enterYB"));
			room.setLeaveScore(objInfo.getDouble("leaveYB"));
		}else{
//			room.setScore(objInfo.getInt("di")); //底分
			/**
			 * 金皇冠筹码小数  2018/02/11 wqm
			 */
			room.setScore(objInfo.getDouble("di")); //底分
		}
		room.setPlayerCount(objInfo.getInt("player"));//玩家人数
		// 玩法
		if(objInfo.containsKey("type")){
			room.setWanfa(objInfo.getInt("type"));
		}
		// 下注上限
		if(objInfo.containsKey("maxcoins")){
			room.setSingleMaxScore(objInfo.getInt("maxcoins"));
		}else{
			room.setSingleMaxScore(100000000);
		}
		// 下注轮数上限
		if(objInfo.containsKey("gameNum")){
			room.setTotalGameNum(objInfo.getInt("gameNum"));
		}else{
			room.setTotalGameNum(15);
		}
		room.setCurrentScore(room.getScore());
		if(objInfo.containsKey("gametype")&&objInfo.getInt("gametype")==1){
			room.setGameType(1);
		}else{
			room.setGameType(0);
		}
		// 局数
		if(objInfo.containsKey("turn")){
			room.setGameCount(objInfo.getJSONObject("turn").getInt("turn"));
		}
		// 倍数
		if(objInfo.containsKey("baseNum")){
			room.setBaseNum(objInfo.getJSONArray("baseNum").toString());//设置基础倍率
		}else{
			JSONArray baseNum = new JSONArray();
			for (int i = 1; i <= 5; i++) {
				if (room.getScore()%1==0) {
					baseNum.add(String.valueOf((int)room.getScore()*i));
				}else {
					baseNum.add(String.valueOf(room.getScore()*i));
				}
			}
			room.setBaseNum(baseNum.toString());
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
		}
		room.setFangzhu(uuid);
		room.setZhuang(uuid);
		Map<String,Playerinfo> users=new HashMap<String, Playerinfo>();
		users.put(uuid, player);
		room.setPlayerMap(users);
		Map<String, UserPacket> userPacketMap = new HashMap<String, UserPacket>();
		userPacketMap.put(uuid, new UserPacket());
		userPacketMap.get(uuid).setLuck(player.getLuck());
		room.setUserPacketMap(userPacketMap);
		Set<Long> userIDSet = new HashSet<Long>();
		userIDSet.add(player.getId());
		room.setUserIDSet(userIDSet);
		room.setGameStatus(ZhaJinHuaCore.GAMESTATUS_READY); // 房间初始状态
		//将房间存入缓存
		RoomManage.gameRoomMap.put(roomNo, room);
		return room;
	}

	@Override
	public boolean joinGameRoom(String roomNo, String uuid, Playerinfo player, boolean isNext) {
		
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
			if(room!=null){
				
				if(room.getUserIDSet().size()<=room.getPlayerCount()){
					List<Long> ids = new ArrayList<Long>();
					for (String uid:room.getUserPacketMap().keySet()) {
						if(room.getPlayerMap().get(uid)!=null){
							
							ids.add(room.getPlayerMap().get(uid).getId());
						}
					}
					if(room.getUserIDSet().size()<room.getPlayerCount()&&!ids.contains(player.getId())){ //新加进来的玩家
						room.getPlayerMap().put(uuid, player);//用户的个人信息
						room.getUserPacketMap().put(uuid, new UserPacket());
						room.getUserPacketMap().get(uuid).setLuck(player.getLuck());
						room.getUserIDSet().add(player.getId());
						RoomManage.gameRoomMap.put(roomNo, room);
						return true;
					}else if(room.getGameStatus()>ZhaJinHuaCore.GAMESTATUS_READY){ //断线后进来的玩家
						return false;
					}
				}
				
			}
		}
		return false;
	}
	

	/**
	 * 结算
	 * @param roomNo
	 * @return
	 */
	private JSONArray jieSuan(String roomNo) {
		
		// 结算数据
		JSONArray array = new JSONArray();
		
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

			// 通知玩家
			for (String uuid : room.getUserPacketMap().keySet()) {
				
				Playerinfo player = room.getPlayerMap().get(uuid);
				// 参与游戏的玩家才能参与结算
				if(room.getUserPacketMap().get(uuid).getStatus()>ZhaJinHuaCore.USERPACKER_STATUS_READY){
					
					JSONObject result = new JSONObject();
					int index = room.getPlayerIndex(uuid);
					UserPacket userPacket = room.getUserPacketMap().get(uuid);
					result.put("index", index);
					result.put("uid", player.getId());
					result.put("player", player.getName());
					result.put("headimg", player.getRealHeadimg());
					result.put("id", player.getId());
					result.put("account", player.getAccount());
					result.put("name", player.getName());
					double myScore = - room.getXiazhuScore(index);
					
					if(userPacket.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_VICTORY){
						// 设置赢家为庄
						((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setZhuang(player.getAccount());
						result.put("win", 1);
//						myScore = room.getTotalScore() + myScore;
						/**
						 * 金皇冠筹码小数  2018/02/11 wqm
						 */
						BigDecimal b1 = new BigDecimal(Double.toString(room.getTotalScore()));
						BigDecimal b2 = new BigDecimal(Double.toString(myScore));
						myScore = b1.add(b2).doubleValue();
					}else{
						result.put("win", 0);
					}
					result.put("score", myScore);
					double totalScore = myScore + player.getScore();
					player.setScore(totalScore);
					result.put("totalScore", player.getScore());
					
					array.add(result);
				}
			}
			
			// 游戏状态
			((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(ZhaJinHuaCore.GAMESTATUS_JIESUAN);
			// 局数记录
			((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setGameIndex(room.getGameIndex()+1);
			// 结算时间
			((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setJiesuanTime(TimeUtil.getNowDate());
			
			//更新房间信息
			//JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
			
			//if(roomInfo!=null&&roomInfo.containsKey("id")){
				
				/*if(room.getRoomType()==1){ // 金币模式
					// 金币结算
					for (int i = 0; i < array.size(); i++) {
						JSONObject user = array.getJSONObject(i);
						//int coins = user.getInt("score");
						double coins = user.getDouble("score");
						if(coins!=0){
							long uid = roomInfo.getLong("user_id"+user.getInt("index"));
							String sql = "update za_users set coins=coins+? where id=?";
							DBUtil.executeUpdateBySQL(sql, new Object[]{coins, uid});
						}
					}
				}*/
			if (room!=null) {
				if(room.getRoomType()==3){ // 元宝模式
					// 元宝结算
					long start = System.currentTimeMillis();
					String sqlString = "UPDATE za_users SET yuanbao = yuanbao+ CASE id";
					String sqlString2 = " END WHERE id IN (";
					String addSql = "insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values ";
					String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
					for (int i = 0; i < array.size(); i++) {
						JSONObject user = array.getJSONObject(i);
						String account = user.getString("account");
						Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();
						for (int j = 0; j < array.size(); j++) {
							playerMap.put(array.getJSONObject(j).getString("account"), new JSONObject().element("score", array.getJSONObject(j).getInt("score")).element("name", array.getJSONObject(j).getString("name")));
						}
						GameLogsCache.addGameLogs(account, 6, new GameLogs(roomNo, playerMap, nowTime));
					}
					for (int i = 0; i < array.size(); i++) {
						JSONObject user = array.getJSONObject(i);
						double yuanbao = user.getDouble("score");
						if(yuanbao!=0){
							long uid = user.getLong("uid");;
							sqlString += " WHEN "+uid+" THEN "+yuanbao;
							addSql +="("+uid+","+6+",'"+roomNo+"',"+3+","+yuanbao+",'"+nowTime+"')";
							if (i==array.size()-1) {
								sqlString2 += uid+")";
							}else {
								sqlString2 += uid+",";
								addSql += ",";
							}
						}
						UserInfoCache.updateUserScore(user.getString("account"), yuanbao, 3);
					}
					sqlString += sqlString2;
					//DBUtil.executeUpdateBySQL(sqlString, new Object[]{});
					//DBUtil.executeUpdateBySQL(addSql, new Object[]{});
					GameMain.sqlQueue.addSqlTask(new SqlModel(sqlString, new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
					GameMain.sqlQueue.addSqlTask(new SqlModel(addSql, new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
					long end = System.currentTimeMillis();
					LogUtil.print("炸金花结算耗时:"+(end-start));
				}
			}
		}
		
		return array;
	}
	
	
	/**
	 * 清除离线的玩家
	 * @param room
	 */
	@Override
	public void cleanDisconnectPlayer(ZJHGame room){
		
		try {
			Set<String> uuidList = room.getUserPacketMap().keySet();
			List<String> lxList = new ArrayList<String>();
			for (String uuid : uuidList) {
				Playerinfo player = room.getPlayerMap().get(uuid);
				if(player!=null&&(player.getStatus()== Constant.ONLINE_STATUS_NO)){
					lxList.add(uuid);
				}else if(room.getRoomType()==3 && player.getScore()<room.getLeaveScore()){
					lxList.add(uuid);
				}
			}
			for (String uuid : lxList) {
				
				if(room.getPlayerMap().get(uuid)!=null){
					zjhGameEventDeal.exitRoom(uuid, room.getRoomNo(), room.getPlayerMap().get(uuid).getId());
				}
			}
		} catch (Exception e) {
			logger.error("",e);
		}
	}


	/**
	 * 准备就绪
	 * @param roomNo
	 * @param sessionId
	 */
	@Override
	public void isReady(String roomNo, String uuid) {
		
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		if(room.getUserPacketMap().get(uuid)!=null){
			
			room.getUserPacketMap().get(uuid).setIsReady(ZhaJinHuaCore.USERPACKER_STATUS_READY);
			room.getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_READY);
			int count = 0;
			for (String uid:room.getUserPacketMap().keySet()) {
				int ready = room.getUserPacketMap().get(uid).getIsReady();
				if(ready!=0){
					count++;
				}
			}
			room.setReadyCount(count);
		}
	}
	

	@Override
	public void xiPai(String roomNo) {
		
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			
			((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).xiPai();
		}
	}
	

	@Override
	public void faPai(String roomNo) {
		
		((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).setGameStatus(ZhaJinHuaCore.GAMESTATUS_FAPAI);
		
		ZJHGame game = ((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		for(String uuid:game.getUserPacketMap().keySet()){
			
			UserPacket userPacket = game.getUserPacketMap().get(uuid);
			if(userPacket!=null){
				
				Integer[] myPai = game.faPai();
				userPacket.setPai(myPai);
				userPacket.setType(ZhaJinHuaCore.getPaiType(Arrays.asList(myPai)));
				userPacket.setStatus(ZhaJinHuaCore.USERPACKER_STATUS_ANPAI);
			}
		}
	}

	@Override
	public boolean xiazhu(String uuid, String roomNo, double score, int type) {
		int type1 = type;
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		UserPacket user = room.getUserPacketMap().get(uuid);
		Playerinfo player = room.getPlayerMap().get(uuid);

		if(type==ZJHGameEventDeal.JIAZHU){
			room.setCurrentScore(score);
		}
		
		// 判断玩家是否需要加倍
		if(user.getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
			score = score*2;
		}
		
		// 获取玩家下注总分数
//		double totalScore = room.getXiazhuScore(player.getMyIndex()) + score;
		/**
		 * 金皇冠筹码小数  2018/02/11 wqm
		 */
		BigDecimal b1 = new BigDecimal(Double.toString(room.getXiazhuScore(player.getMyIndex())));
		BigDecimal b2 = new BigDecimal(Double.toString(score));
		double totalScore = b1.add(b2).doubleValue();
		
		boolean canXiazhu = false;
		if (room.getRoomType()==0||room.getRoomType()==2) {
			canXiazhu = true;
		}else if (player.getScore()>=totalScore) {
			canXiazhu = true;
		}
		
		// 判断玩家金币是否足够
		if(canXiazhu){
			
			// 添加下注记录
			room.addXiazhuList(player.getMyIndex(), score);
			
			// 更新总下注记录
			room.addTotalScore(score);
			
			// 确定下次操作的玩家
			String nextPlayer = room.getNextOperationPlayer(room, uuid);
			int nextNum = room.getPlayerIndex(nextPlayer);
			int gameNum = room.getGameNum();
			if(room.getYixiazhu().contains(nextNum)){
				gameNum = gameNum+1;
			}
			
			// 比牌时跟注
			if(type==ZJHGameEventDeal.BIPAI){
				nextNum = -1;
				type = ZJHGameEventDeal.GENZHU;
			}
			
			// 更新下注轮数
			if(room.getYixiazhu().contains(player.getMyIndex())){
				room.getYixiazhu().clear();
				room.setGameNum(room.getGameNum()+1);
			}
			room.getYixiazhu().add(player.getMyIndex());
			
			if (type1!=ZJHGameEventDeal.BIPAI) {
				
				// 通知玩家
				for (String uid  : room.getUserPacketMap().keySet()) {
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
					if(clientother!=null){
						JSONObject result = new JSONObject();
						result.put("code", 1);
						result.put("index", room.getPlayerMap().get(uuid).getMyIndex());
						result.put("nextNum", nextNum);
						result.put("gameNum", gameNum);
						result.put("currentScore", room.getCurrentScore());
						result.put("totalScore", room.getTotalScore());
						result.put("myScore", totalScore);
						result.put("score", score);
						result.put("realScore", room.getPlayerMap().get(uuid).getScore()-totalScore);
						result.put("type", type);
						clientother.sendEvent("gameActionPush_ZJH", result);
					}
				}
			}
			
			// 超过单局下注上限，强制结算
			if(room.getSingleMaxScore()>0 && room.getTotalScore()>=room.getSingleMaxScore()){
				
				compelBiPai(roomNo, uuid);
				return false;
			}
			
			// 达到下注轮数上限（最后一轮所有人都已下注），强制结算
			if(room.getTotalGameNum()>0 
					&& room.getGameNum()>=room.getTotalGameNum() 
					&& room.getProgressIndex().length==room.getYixiazhu().size()){
				
				compelBiPai(roomNo, uuid);
				return false;
			}
			
			return true;
			
		}else{
			
			SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
			if(clientother!=null){
				JSONObject result = new JSONObject();
				result.put("code", 0);
				result.put("type", type);
				result.put("index", room.getPlayerMap().get(uuid).getMyIndex());
				result.put("msg", "您的金币不足，无法下注");
				clientother.sendEvent("gameActionPush_ZJH", result);
			}
			
			return false;
		}

	}

	@Override
	public void genzhu(String uuid, String roomNo) {
		
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		double score = room.getCurrentScore();
		xiazhu(uuid, roomNo, score, ZJHGameEventDeal.GENZHU);
	}

	@Override
	public void gendaodi(String uuid, String roomNo, JSONObject obj) {
		
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
		if(clientother!=null){
		
			int value = obj.getInt("value");
			
			if(value==1){
				((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).isGenDaoDi = true;
			}else{
				((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).isGenDaoDi = false;
			}
			
			JSONObject result = new JSONObject();
			result.put("code", 1);
			result.put("type", ZJHGameEventDeal.GENDAODI);
			result.put("value", value);
			result.put("index", room.getPlayerIndex(uuid));
			clientother.sendEvent("gameActionPush_ZJH", result);
		}
	}

	@Override
	public void qipai(String uuid, String roomNo) {

		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		if(room.getUserPacketMap().get(uuid)!=null){
			// 玩家弃牌
			((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_QIPAI);
		}
		int myIndex = room.getPlayerMap().get(uuid).getMyIndex();

		// 游戏是否结束
		boolean isGameover = false;
		JSONArray jiesuanArray = new JSONArray();
		
		// 当剩下一个玩家还未开牌时，游戏结束
		if(room.getProgressIndex().length<=1){
			
			isGameover = true;
			
			// 剩下的玩家直接赢
			for (String winner :room.getUserPacketMap().keySet()) {
				int status = room.getUserPacketMap().get(winner).getStatus();
				if(status==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI || status==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){ // 暗牌或是明牌
					room.getUserPacketMap().get(winner).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_VICTORY);
					break;
				}
			}
			
			// 进入结算
			jiesuanArray = jieSuan(roomNo);
		}

		// 确定下次操作的玩家
		String nextPlayer = room.getNextOperationPlayer(room, uuid);
		int nextNum = room.getPlayerIndex(nextPlayer);
		int gameNum = room.getGameNum();
		if(!isGameover && room.getYixiazhu().contains(nextNum)){
			gameNum = gameNum+1;
		}

		// 进入总结算
		JSONArray jiesuanData = getJieSuanData(room, jiesuanArray, isGameover);
		
		double myScore = room.getXiazhuScore(myIndex);
		// 通知玩家
		for (String uid  : room.getUserPacketMap().keySet()) {
			SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
			if(clientother!=null){
				JSONObject result = new JSONObject();
				result.put("code", 1);
				result.put("index", room.getPlayerMap().get(uuid).getMyIndex());
				result.put("nextNum", nextNum);
				result.put("gameNum", gameNum);
				result.put("currentScore", room.getCurrentScore());
				result.put("totalScore", room.getTotalScore());
				result.put("myScore", myScore);
				result.put("type", ZJHGameEventDeal.QIPAI);
				if(isGameover){
					result.put("jiesuan", jiesuanArray);
					result.put("showPai", room.getUserPacketMap().get(uid).getBipaiList());
					result.put("isGameover", 1);
					if(jiesuanData.size()>0){
						result.put("jiesuanData", jiesuanData);
						result.put("jiesuanTime", room.getJiesuanTime());
					}
					
				}else{
					result.put("isGameover", 0);
				}
				clientother.sendEvent("gameActionPush_ZJH", result);
			}
		}
		
	}

	@Override
	public void kanpai(String uuid, String roomNo) {

		// 玩家已经看牌
		((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_KANPAI);
		((ZJHGame) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuid).isShow = true;
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));
		// 通知玩家
		for (String uid  : room.getUserPacketMap().keySet()) {
			SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
			if(clientother!=null){
				JSONObject result = new JSONObject();
				result.put("code", 1);
				result.put("index", room.getPlayerMap().get(uuid).getMyIndex());
				result.put("type", ZJHGameEventDeal.KANPAI);
				if(uuid.equals(uid)){
					result.put("mypai", room.getUserPacketMap().get(uuid).getPai());
					result.put("paiType", room.getUserPacketMap().get(uuid).getType());
				}
				clientother.sendEvent("gameActionPush_ZJH", result);
			}
		}
	}

	@Override
	public void bipai(String uuid, String roomNo, JSONObject data) {

		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

		// 被比牌的玩家下标
		int index = data.getInt("index");
		for(String bipaiuuid:room.getUserPacketMap().keySet()){
			
			Playerinfo player = room.getPlayerMap().get(bipaiuuid);
			if(player!=null && player.getMyIndex() == index){
				
				// 比牌下注分数
				double score = room.getCurrentScore();
				if(room.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
					score = score*2;
				}
				// 比牌需要下注
				if(xiazhu(uuid, roomNo, room.getCurrentScore(), ZJHGameEventDeal.BIPAI)){
					
					Integer[] mypai = room.getUserPacketMap().get(uuid).getPai();
					Integer[] bipai = room.getUserPacketMap().get(bipaiuuid).getPai();
					
					int backresult = ZhaJinHuaCore.compare(Arrays.asList(mypai), Arrays.asList(bipai));
					
					// 添加比牌记录
					room.getUserPacketMap().get(uuid).addBiPaiList(index, bipai);
					room.getUserPacketMap().get(uuid).addBiPaiList(room.getPlayerMap().get(uuid).getMyIndex(), mypai);
					
					room.getUserPacketMap().get(bipaiuuid).addBiPaiList(index, bipai);
					room.getUserPacketMap().get(bipaiuuid).addBiPaiList(room.getPlayerMap().get(uuid).getMyIndex(), mypai);
					
					JSONArray compareResult = new JSONArray();
					
					JSONObject obj = new JSONObject();
					obj.put("index", room.getPlayerMap().get(uuid).getMyIndex());
					
					JSONObject obj1 = new JSONObject();
					obj1.put("index", player.getMyIndex());
					
					if(backresult>0){
						
						obj.put("result", 1);
						obj1.put("result", 0);
						compareResult.add(obj);
						compareResult.add(obj1);
						
						// 失败
						room.getUserPacketMap().get(bipaiuuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_SHIBAI);
						
					}else{ // 相等比牌的人输
						
						obj.put("result", 0);
						obj1.put("result", 1);
						compareResult.add(obj);
						compareResult.add(obj1);
						
						// 失败
						room.getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_SHIBAI);
					}
					
					// 游戏结束
					boolean isGameover = false;
					JSONArray jiesuanArray = new JSONArray();
					
					// 当剩下一个玩家还未开牌时，游戏结束
					if(room.getProgressIndex().length<=1){
						
						isGameover = true;
						if(backresult>0){ // 最后一次对决，确定胜利玩家
							room.getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_VICTORY);
						}else{
							room.getUserPacketMap().get(bipaiuuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_VICTORY);
						}
						// 进入结算
						jiesuanArray = jieSuan(roomNo);
					}

					// 确定下次操作的玩家
					String nextPlayer = room.getNextOperationPlayer(room, uuid);
					int nextNum = room.getPlayerIndex(nextPlayer);
					int gameNum = room.getGameNum();
					if(!isGameover && room.getYixiazhu().contains(nextNum)){
						gameNum = gameNum+1;
					}

					// 进入总结算
					JSONArray jiesuanData = getJieSuanData(room, jiesuanArray, isGameover);
					
					// 发起比牌玩家下标
					int myIndex = room.getPlayerMap().get(uuid).getMyIndex();
					double myScore = room.getXiazhuScore(myIndex);
					// 通知玩家
					for (String uid  : room.getUserPacketMap().keySet()) {
						SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
						if(clientother!=null){
							JSONObject result = new JSONObject();
							result.put("code", 1);
							result.put("index", myIndex);
							result.put("nextNum", nextNum);
							result.put("gameNum", gameNum);
							result.put("currentScore", room.getCurrentScore());
							result.put("totalScore", room.getTotalScore());
							result.put("myScore", myScore);
							result.put("score", score);
							result.put("type", ZJHGameEventDeal.BIPAI);
							result.put("result", compareResult);
							if(isGameover){
								result.put("jiesuan", jiesuanArray);
								result.put("showPai", room.getUserPacketMap().get(uid).getBipaiList());
								result.put("isGameover", 1);
								if(jiesuanData.size()>0){
									result.put("jiesuanData", jiesuanData);
									result.put("jiesuanTime", room.getJiesuanTime());
								}
							}else{
								result.put("isGameover", 0);
							}
							clientother.sendEvent("gameActionPush_ZJH", result);
						}
					}
				}
				
			}
		}
		
	}

	
	/**
	 * 获取总结算数据
	 * @param room
	 * @param jiesuanArray
	 * @param isGameover
	 * @return 
	 */
	private JSONArray getJieSuanData(ZJHGame room, JSONArray jiesuanArray, boolean isGameover) {

		String roomNo = room.getRoomNo();
		JSONArray jiesuanData = new JSONArray();
		if(isGameover){
			
			// 总结算数据
			if(room.getGameCount()==room.getGameIndex()){

				Set<String> uuids = room.getUserPacketMap().keySet();
				// 大赢家分数
				double dayinjia = 0;
				for(String uid:uuids){
					if(room.getPlayerMap().get(uid).getScore()>=dayinjia){
						dayinjia = room.getPlayerMap().get(uid).getScore();
					}
				}
				
				// 设置玩家信息
				for(String uid:uuids){
					Playerinfo p = room.getPlayerMap().get(uid);
					JSONObject user = new JSONObject();
					user.put("player", p.getName());
					user.put("account", p.getAccount());
					user.put("headimg", p.getRealHeadimg());
					user.put("score", p.getScore());

					if(room.getFangzhu().equals(uid)){
						
						user.put("isFangzhu", 1);
					}else{
						user.put("isFangzhu", 0);
					}
					
					if(p.getScore() == dayinjia){
						
						user.put("isWinner", 1);
					}else{
						user.put("isWinner", 0);
					}
					
					jiesuanData.add(user);
				}
			}

			/*
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
				for(Long uid:room.getUserIDSet()){
					
					JSONObject usergamelog = new JSONObject();
					usergamelog.put("gid", 6);
					usergamelog.put("room_id", roomInfo.getLong("id"));
					usergamelog.put("room_no", roomNo);
					usergamelog.put("game_index", game_index);
					usergamelog.put("user_id", uid);
					usergamelog.put("gamelog_id", gamelog_id);
					usergamelog.put("result", jiesuanArray.toString());
					usergamelog.put("createtime", nowTime);
					
					mjBiz.addUserGameLog(usergamelog);
				}

				// 更新房间局数序号
				String sql = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
				DBUtil.executeUpdateBySQL(sql, new Object[]{roomNo});
			}*/
		
			//SaveLogsThreadZJH saveLogsThreadZJH = new SaveLogsThreadZJH(roomNo, jiesuanData, jiesuanArray);
			//saveLogsThreadZJH.start();
			GameMain.sqlQueue.addSqlTask(new SqlModel(SqlModel.SAVELOGS_ZJH, roomNo, jiesuanData, jiesuanArray));
			
			try {
				// 扣除房卡，更改房间局数
				//IService server = (IService) RegisterServer.registry.lookup("sysService");
				//server.settlementRoomNo(roomNo);
				mjBiz.settlementRoomNo(roomNo);
			} catch (Exception e) {
				LogUtil.print("扣除房卡，更改房间局数方法异常："+e.getMessage());
				logger.error("",e);
			}

		}
		return jiesuanData;
	}

	@Override
	public void compelBiPai(String roomNo, String opuuid) {
		
		ZJHGame room=((ZJHGame) RoomManage.gameRoomMap.get(roomNo));

		// 获取还在游戏中的玩家
		List<String> uuidList = new ArrayList<String>();
		for (String uuid : room.getUserPacketMap().keySet()) {
			if(room.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
					|| room.getUserPacketMap().get(uuid).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
				
				uuidList.add(uuid);
			}
		}
		
		// 比较牌的大小，牌最大者获胜
		if(uuidList.size()>1){

			// 添加比牌记录
			for (String uuid : uuidList) {
				for (String uuid1 : uuidList) {
					room.getUserPacketMap().get(uuid).addBiPaiList(room.getPlayerMap().get(uuid1).getMyIndex(), 
							room.getUserPacketMap().get(uuid1).getPai());
				}
			}
			
			// 弃牌玩家可以看到强制结算玩家的牌
			for (String string : room.getUserPacketMap().keySet()) {
				if (!uuidList.contains(string)) {
					for (String uuid : uuidList) {
						room.getUserPacketMap().get(string).addBiPaiList(room.getPlayerMap().get(uuid).getMyIndex(), 
								room.getUserPacketMap().get(uuid).getPai());
					}
				}
			}
			
			String maxUUID = uuidList.get(0);
			for (int i = 1; i < uuidList.size(); i++) {

				String uuid = uuidList.get(i);
				Integer[] paiA = room.getUserPacketMap().get(maxUUID).getPai();
				Integer[] paiB = room.getUserPacketMap().get(uuid).getPai();
				
				int bipairesult = ZhaJinHuaCore.compare(Arrays.asList(paiA), Arrays.asList(paiB));
				
				if(bipairesult>0){
					// 失败
					room.getUserPacketMap().get(uuid).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_SHIBAI);
					
				}else{ // 相等比牌的人输
					
					// 失败
					room.getUserPacketMap().get(maxUUID).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_SHIBAI);
					// 重置最大牌
					maxUUID = uuid;
				}
			}
			

			// 游戏结束
			boolean isGameover = false;
			JSONArray jiesuanArray = new JSONArray();
			
			// 当剩下一个玩家还未开牌时，游戏结束
			if(room.getProgressIndex().length<=1){
				
				isGameover = true;
				room.getUserPacketMap().get(maxUUID).setStatus(ZhaJinHuaCore.USERPACKER_STATUS_VICTORY);
				// 进入结算
				jiesuanArray = jieSuan(roomNo);
			}

			// 确定下次操作的玩家
			String nextPlayer = room.getNextOperationPlayer(room, opuuid);
			int nextNum = room.getPlayerIndex(nextPlayer);
			int gameNum = room.getGameNum();
			if(!isGameover && room.getYixiazhu().contains(nextNum)){
				gameNum = gameNum+1;
			}

			// 进入总结算
			JSONArray jiesuanData = getJieSuanData(room, jiesuanArray, isGameover);
			
			int myScore = 0;
			// 通知玩家
			for (String uid  : room.getUserPacketMap().keySet()) {
				SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
				if(clientother!=null){
					JSONObject result = new JSONObject();
					result.put("code", 1);
					result.put("index", room.getPlayerMap().get(opuuid).getMyIndex()+1);
					result.put("nextNum", nextNum);
					result.put("gameNum", gameNum);
					result.put("currentScore", room.getCurrentScore());
					result.put("totalScore", room.getTotalScore());
					result.put("myScore", myScore);
					result.put("score", 0);
					result.put("type", ZJHGameEventDeal.JIESUAN);
					if(isGameover){
						result.put("jiesuan", jiesuanArray);
						result.put("showPai", room.getUserPacketMap().get(uid).getBipaiList());
						result.put("isGameover", 1);
						if(jiesuanData.size()>0){
							result.put("jiesuanData", jiesuanData);
							result.put("jiesuanTime", room.getJiesuanTime());
						}
					}else{
						result.put("isGameover", 0);
					}
					clientother.sendEvent("gameActionPush_ZJH", result);
				}
			}
		}
	}

}
