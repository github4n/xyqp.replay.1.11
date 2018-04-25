package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.protocol.Packet;
import com.zhuoan.biz.core.nn.NiuNiu;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.*;

public class AutoThreadNN extends Thread{
    
    private final static Logger logger = LoggerFactory.getLogger(AutoThreadNN.class);

	private NiuNiuService nnService;
	private String roomNo;
	private int type;
	NNGameEventDeal nnGameEventDeal = new NNGameEventDeal();
	MaJiangBiz mjBiz=new MajiangBizImpl();

	SocketIOClient client = new SocketIOClient() {

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

	public AutoThreadNN (NiuNiuService nnService, String roomNo, int type){
		this.nnService = nnService;
		this.roomNo = roomNo;
		this.type = type;
	}

	public void run() {

		switch (type) {
		case 0:
			joinGame();
			break;
		case 1:
			autoXiazhu();
			break;
		case 2:
			autoLiangpai();
			break;
		case 3:
			autoReady();
			break;
		case 4:
			autoExitRoom();
			break;
		case 5:
			autoQiangZhuang();
			break;
		default:
			break;
		}
	}

	public void joinGame() {
		// 如果房间存在
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			//定时器倒计时
			for (int i = 10; i >= 0; i--) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				if(Constant.niuNiuGameMap.get(roomNo)!=null){

					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
					NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
					if((room.getGameStatus()==0||room.getGameStatus()==3)&&room.getPlayerCount()>room.getAllPlayer().size()){
						List<String> list = new ArrayList<String>();
						for (int j = 0; j < room.getRobotList().size(); j++) {
							//取不在当前房间内的第一个机器人
							if (!room.getPlayerMap().keySet().contains(room.getRobotList().get(j))) {
								list.add(room.getRobotList().get(j));
							}
						}
						if(i%2==0&&list.size()>0){
							int roIndex = getMinIndex();
							String sql = "select id,name,headimg,score,account,uuid from za_users where account=?";
							JSONObject userinfo = DBUtil.getObjectBySQL(sql, new Object[] { list.get(0) });
							if (userinfo!=null) {
								sql = "update za_gamerooms set user_id"+roIndex+" =?, user_icon"+roIndex+" =?, user_name"+roIndex+" =?, user_score"+roIndex+" =? where room_no=?";
								Object[] params = new Object[] { userinfo.get("id"), userinfo.get("headimg"), userinfo.get("name"), userinfo.get("score"), roomNo };
								DBUtil.executeUpdateBySQL(sql, params);
								JSONObject obj = new JSONObject();
								obj.put("roomType", 1);
								obj.put("account", userinfo.getString("account"));
								obj.put("room_no", roomNo);
								obj.put("myIndex", roIndex);
								obj.put("uuid", userinfo.getString("uuid"));
								nnGameEventDeal.enterRoom(client, obj);
								try {
									Thread.sleep((new Random().nextInt(3)+1)*1000);
								} catch (InterruptedException e) {
									logger.error("",e);
								}
								autoReady(userinfo.getString("account"),obj);
							}
						}
					}else{
						System.out.println("加入游戏线程终止");
						break;
					}
				}else{
					System.out.println("房间不存在："+roomNo);
					break;
				}
			}
		}

	}

	public void autoReady(String clientTag, Object data) {
		JSONObject postdata = JSONObject.fromObject(data);
		// 房间号
		String roomNo = postdata.getString("room_no");

		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room=Constant.niuNiuGameMap.get(roomNo);//获取房间
			if (!(room.getGameStatus()== NiuNiu.GAMESTATUS_XIAZHU)) {
				// 准备
				NiuNiuServer.isReady(roomNo, clientTag);

				//通知用户
				for (String uuid : room.getPlayerMap().keySet()) {
					JSONObject result = new JSONObject();
					result.put("myIndex", room.getPlayerIndex(clientTag));
					result.put("isReady", room.getReadyIndex());
					if (!room.getRobotList().contains(uuid)) {
						SocketIOClient clientother= GameMain.server.getClient(room.getUUIDByClientTag(uuid));
						if(clientother!=null){
							clientother.sendEvent("playerReadyPush_NN", result);
						}
					}
				}

				// 两个人以上准备则开启准备倒计时
				if(!postdata.containsKey("auto") && room.getUserIDSet().size()>2 && room.getReadyCount()==2){

					// 开启准备定时器，开始计时
					MutliThreadNN m = new MutliThreadNN(null, roomNo, 6);
					m.start();
				}

				//准备人数等于房间现有人数开始游戏
				if (room.getReadyCount()==room.getPlayerMap().size()) {
					if(room.getZhuangType()!=2&&room.getZhuangType()!=3){
						nnGameEventDeal.startGame(room);
					}else {
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
										}
										
										SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
										if(clientother!=null){
											clientother.sendEvent("gameUISettingPush_NN", result);
										}
									}

								}
							}

							// 抢庄阶段
							Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_QIANGZHUANG);

							// 抢庄模式
							if(room.getGameCount()>room.getGameIndex()){ 

								MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 3);
								m.start();
							}

							// 明牌抢庄发牌
							if(room.getZhuangType()==3){ 

								// 洗牌
								NiuNiuServer.xiPai(roomNo);
								// 发牌
								NiuNiuServer.faPai(roomNo);
							}

							// 通知玩家
							for (String uuid : room.getPlayerMap().keySet()) {
								JSONObject result = new JSONObject();
								if(room.getZhuangType()==3){
									// 设置明牌抢庄的牌组
									Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uuid).saveMingPai();
									int[] mypai = Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uuid).getMingPai();
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
										clientother.sendEvent("qiangzhuangStartPush_NN", result);
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
						
						AutoThreadNN autoThreadNN = new AutoThreadNN(nnService, roomNo, 5);
						autoThreadNN.start();
					}
				}
			} else {
				System.out.println("游戏已经开始");
			}
		}
	}

	public void autoXiazhu() {
		// 如果房间存在
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			for(String uid : room.getPlayerMap().keySet()){
				try {
					Thread.sleep(new Random().nextInt(3)*1000);
				} catch (InterruptedException e) {
					logger.error("",e);
				}
				if (room.getRobotList().contains(uid) && !room.getZhuang().equals(uid)) {
					UserPacket up = room.getUserPacketMap().get(uid);
					int index = room.getPlayerMap().get(uid).getMyIndex();
					JSONObject obj = new JSONObject();
					obj.put("room_no", room.getRoomNo());
					obj.put("num", index);
					obj.put("place", index);
					obj.put("money", randomMoney(uid, index));
					obj.put("auto", 1);
					gameXiaZhu(uid, obj);
					up.setIsReady(2);
				}
			}
		}else{
			System.out.println("房间不存在："+roomNo);
		}
	}

	public void gameXiaZhu(String uuid, Object data) {

		JSONObject postdata = JSONObject.fromObject(data);
		String roomNo=postdata.getString("room_no");
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			int num = postdata.getInt("num");
			int place = postdata.getInt("place");
			int money = postdata.getInt("money");
			//添加下注记录
			room.addPlayerMoney(num, place, money);
			postdata.element("code", 1);
			postdata.element("msg", "下注成功");
			postdata.element("playerMoney", room.getMoneyPlace());
			//记录下下注信息
			room.getPlaceArray().add(postdata);
			// 房卡场下注完成
			room.getUserPacketMap().get(uuid).setIsReady(2);

			for(String uuid1:room.getPlayerMap().keySet()){
				if (!room.getRobotList().contains(uuid1)) {
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid1));
					if(clientother!=null){
						clientother.sendEvent("gameXiaZhuPush_NN", postdata);
					}
				}
			}
		}
	}

	public void autoLiangpai(){
		// 如果房间存在
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			if(Constant.niuNiuGameMap.get(roomNo)!=null){
				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
				for(String uid : room.getUserPacketMap().keySet()){
					if (room.getRobotList().contains(uid)) {
						try {
							Thread.sleep(new Random().nextInt(2)*1000);
						} catch (InterruptedException e) {
                            logger.error("", e);
						}
						// 重置玩家状态信息
						Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uid).setIsReady(0);
						Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
						nnService.showPai(roomNo, uid);
					}
				}
			}
		}
	}

	public void autoReady() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			logger.error("",e);
		}
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			if(Constant.niuNiuGameMap.get(roomNo)!=null){
				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);

				List<String> list = new ArrayList<String>();
				for(String uid : room.getPlayerMap().keySet()){
					if (room.getRobotList().contains(uid)) {
						list.add(uid);
					}
				}
				if (list.size()>0) {
					for (String string : list) {
						try {
							Thread.sleep(new Random().nextInt(3)*1000);
						} catch (InterruptedException e) {
							logger.error("",e);
						}
						JSONObject jsonObject = new JSONObject();
						jsonObject.element("room_no", roomNo);
						autoReady(string, jsonObject);
					}
				}
			}
		}
	}
	
	public void autoQiangZhuang(){
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			if(Constant.niuNiuGameMap.get(roomNo)!=null){
				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
				for(String uid : room.getUserPacketMap().keySet()){
					try {
						Thread.sleep(new Random().nextInt(3)*500);
					} catch (InterruptedException e) {
						logger.error("",e);
					}
					if (room.getRobotList().contains(uid)) {
						String qiang;
						int nextInt = new Random().nextInt(4);
						if (nextInt>0) {
							Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uid).qzTimes=nextInt;
							qiang = "1";
						}else {
							qiang = "-1";
						}
						nnService.qiangZhuang(roomNo, qiang, uid);
					}
				}
			}
		}	
	}

	public int randomMoney(String uuid,int index) {
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
			JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
			int moneyIndex = new Random().nextInt(3);
			int money = baseNum.getJSONObject(moneyIndex).getInt("val");
			boolean isCanXiaZhu = true;
			// 金币场时，判断玩家金币是否足够且庄家金币足够赔付

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

				// 玩家总积分
				double totalMoney = room.getPlayerMap().get(uuid).getScore();
				// 玩家可下注最大积分
				double maxScore = (xiazhuScore+money)*4*room.getScore();

				if(maxScore > totalMoney){

					isCanXiaZhu = false;

				}else{
					// 总下注筹码
					double playertotalMoney = room.getPlayerTotalMoney();
					// 庄家剩余筹码
					double zhuangtotalMoney = room.getPlayerMap().get(room.getZhuang()).getScore();

					if((playertotalMoney + money)*4*room.getScore() > zhuangtotalMoney){

						isCanXiaZhu = false;
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
			if (isCanXiaZhu) {
				return money;
			}else{
				return randomMoney(uuid, index);
			}
		}else {
			return 0;
		}
	}

	public void autoExitRoom(){
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		List<String> outList = new ArrayList<String>();
		List<Long> outIdList = new ArrayList<Long>();
		//取出金币少于最低金币的所有用户account和id
		for (String uuid : room.getPlayerMap().keySet()) {
			if (room.getRobotList().contains(uuid)) {
				String sql = "select id,yuanbao from za_users where account=?";
				JSONObject userinfo = DBUtil.getObjectBySQL(sql, new Object[]{ uuid });
				if (userinfo!=null&&userinfo.containsKey("yuanbao")&&userinfo.getDouble("yuanbao")<room.getGoldcoins()) {
					//金币少于1000退出房间
					outList.add(uuid);
					outIdList.add(userinfo.getLong("id"));
				}
			}
		}
		//获取新的机器人列表
		List<String> list = new ArrayList<String>();
		for (String string : room.getRobotList()) {
			if (!outList.contains(string)) {
				list.add(string);
			}
		}
		for (int i = 0; i < outList.size(); i++) {
			//退出房间
			nnGameEventDeal.exit(room, outList.get(i), outIdList.get(i));
			//加入新的机器人
			String sql = "select account from za_users where openid='0' and status=0 limit ?,1";
			JSONObject jsonObject = DBUtil.getObjectBySQL(sql, new Object[]{1});
			if (jsonObject!=null) {
				list.add(jsonObject.getString("account"));
				sql = "update za_users set status=1,yuanbao=? where account=?";
				DBUtil.executeUpdateBySQL(sql, new Object[]{25000+new Random().nextInt(50000),jsonObject.getString("account")});
			}
			//更改状态
			sql = "update za_users set status=0 where account=?";
			DBUtil.executeUpdateBySQL(sql, new Object[]{ outList.get(i) });
		}
		room.setRobotList(list);
		joinGame();
	}

	public int getMinIndex() {
		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		/**
		 * 去掉select *   wqm 2018/02/26
		 */
		String sql = "select user_id0,user_id1,user_id2,user_id3,user_id4,user_id5,user_id6,user_id7,user_id8,"
				+ "user_id9 from za_gamerooms where room_no=?";
		JSONObject roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
		int minIndex = 0;
		//获取当前房间的第一个空位
		for (int i = 0; i < room.getPlayerCount(); i++) {
			long user_id = roominfo.getLong("user_id" + Integer.toString(i));
			if (user_id == 0) {
				minIndex = i;
				break;
			}
		}
		return minIndex;
	}
}
