package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NiuNiu;
import com.zhuoan.biz.core.nn.NiuNiuServer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.PackerCompare;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
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
 * 线程异步 
 */
public class MutliThreadBRNN extends Thread{
	
	public static int[] GLOBALTIMER = new int[]{5,15,20};
	
	private NiuNiuService nnService;
	private MaJiangBiz mjBiz=new MajiangBizImpl();
	private String roomNo;
	private int type;
	
    /**
     * @param nnService
     * @param roomNo
     * @param type  0、准备（开始游戏）倒计时，超过时间换庄（金币场）       1、下注倒计时      2、亮牌倒计时   
     */
    public MutliThreadBRNN (NiuNiuService nnService, String roomNo, int type){
    	this.nnService = nnService;
    	this.roomNo = roomNo;
    	this.type = type;
    }
    
    public void run() {
    	
    	switch (type) {
    	case 0:
    		ready();//准备（闲时倒计时）
    		break;
    	case 1:
    		xiazhu();//下注倒计时
    		break;
    	case 2:
    		jieSuan();//亮牌/结算倒计时
    		break;
    	default:
    		break;
    	}
    }

    
	/**
     * 准备（开始游戏）
     */
    public void ready() {
    	
    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		
    		boolean startGame = false;
    		if(Constant.niuNiuGameMap.get(roomNo)!=null 
    				&& (Constant.niuNiuGameMap.get(roomNo).getGameStatus()== NiuNiu.GAMESTATUS_QIANGZHUANG
    				||Constant.niuNiuGameMap.get(roomNo).getGameStatus()==NiuNiu.GAMESTATUS_READY)){
    			
    			//换庄
    			startGame = huanZhuang(roomNo);
    		}
    		
    		if(startGame){
    			
    			//定时器倒计时
    			for (int i = GLOBALTIMER[0]; i >= 0; i--) {
    				try {
    					Thread.sleep(1000);
    				} catch (InterruptedException e1) {
    					e1.printStackTrace();
    				}
    				if(Constant.niuNiuGameMap.get(roomNo)!=null){
    					
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    					NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    					if(room.getGameStatus()==NiuNiu.GAMESTATUS_READY){
    						
    						// 超过时间未开始游戏，执行换庄操作
    						if(i==0){
    							
    							// 当房间玩家人数大于1人时
    							if(room.getPlayerMap().size()>1){

    								// 游戏状态
    								Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_XIAZHU);
    								
    								// 重置准备
    								room.setReadyCount(0);
    								
    								// 初始化房间信息
    								room.initGame();
    								
    								Set<String> uuidList = room.getPlayerMap().keySet();
    								// 通知玩家
    								for (String uuid : uuidList) {
    									
    									JSONObject result = new JSONObject();
    									SocketIOClient clientother= GameMain.server.getClient(room.getUUIDByClientTag(uuid));
    									if(clientother!=null){
    										clientother.sendEvent("gameStartPush_BRNN", result);
    									}
    								}
    								
    								// 开启下注定时器，开始计时
    								new MutliThreadBRNN(null, roomNo, 1).start();
    							}else{
    								// 开启准备定时器
    								new MutliThreadBRNN(null, roomNo, 0).start();
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
    		}else{

        		try {
    				Thread.sleep(3000);
    				System.out.println("上庄");
    			} catch (InterruptedException e1) {
    				e1.printStackTrace();
    			}
        		
				// 开启准备定时器
				new MutliThreadBRNN(null, roomNo, 0).start();
			}
    	}
	}
    
    
	/**
	 * 换庄
	 * @param roomNo
	 * @return 
	 */
	private boolean huanZhuang(String roomNo) {

		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
		// 换庄前，移除掉线的玩家 
		if(nnService!=null){
			//nnService.cleanDisconnectPlayer(room);
		}
		
		// type：-1 没有庄；0 有庄但是没开始；1有庄且开始倒计时
		int type = -1;
		
		// 上庄列表不为空
		if(room.getShangzhuangList().size()>0 || room.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
			// 有庄
			type = 0;
			if(room.getShangzhuangList().size()>0 || (room.getZhuang()!=null && !room.getZhuang().equals("10000000"))){
				
				// 没人坐庄或是系统坐庄
				if(room.getZhuang()==null || room.getZhuang().equals("10000000")){
					
					// 设置新的庄
					String zhuang = room.getShangzhuangList().get(0).getAccount();
					Constant.niuNiuGameMap.get(roomNo).getPlayerMap().get(zhuang).setMyIndex(0);
					Constant.niuNiuGameMap.get(roomNo).setZhuang(zhuang);
					Constant.niuNiuGameMap.get(roomNo).setLianzhuang(0);
					
					room.getShangzhuangList().remove(0);
					
				}else if(room.getLianzhuang()==room.getMaxLianzhuang()){ // 达到最大连庄数换人
					
					if(room.getShangzhuangList().size()>0){
						
						// 移除旧的庄家
						String oldZhuang = room.getZhuang();
						long uid = room.getPlayerMap().get(oldZhuang).getId();
						Constant.niuNiuGameMap.get(roomNo).getPlayerMap().get(oldZhuang).setMyIndex((int)uid);
						
						// 设置新的庄
						String zhuang = room.getShangzhuangList().get(0).getAccount();
						Constant.niuNiuGameMap.get(roomNo).getPlayerMap().get(zhuang).setMyIndex(0);
						Constant.niuNiuGameMap.get(roomNo).setZhuang(zhuang);
						Constant.niuNiuGameMap.get(roomNo).setLianzhuang(0);
						
						room.getShangzhuangList().remove(0);
						
					}else{ // 上庄列表为空时，换成系统坐庄
						Constant.niuNiuGameMap.get(roomNo).setZhuang("10000000");
					}
				}
				// 有庄且玩家人数大于2人直接开始
				if(type==0&&room.getPlayerMap().size()>1 && !room.getZhuang().equals("10000000")){
					type = 1;
				}
			}
			
			if(room.getZhuang()!=null && !room.getZhuang().equals("10000000")){
				// 游戏准备
				Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_READY);
				for (String uid  : room.getUserPacketMap().keySet()) {
					
					JSONObject result = new JSONObject();
					result.put("myIndex", room.getPlayerMap().get(uid).getMyIndex());
					result.put("users", room.getBRNNAllPlayer(""));
					result.put("type", type);
					
					// 重置庄家信息
					SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
					if(clientother!=null){
						clientother.sendEvent("huanZhuangPush_BRNN", result);
					}
				}
			}
		}
		
		if(type==1){
			return true;
		}
		return false;
	}

	/**
     * 下注
     */
    public void xiazhu() {
    	
    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		//定时器倒计时
    		for (int i = GLOBALTIMER[1]; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			System.out.println("牛牛下注倒计时："+i);
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				
    				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				if(room.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
    					
    					//如果下注时间到，则执行比牌方法
    					if(i==0){

    						// 开启准备定时器，开始计时
    						new MutliThreadBRNN(null, roomNo, 2).start();
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
	}
    

	/**
	 * 结算
	 * @param roomNo
	 */
    public void jieSuan() {
		
		if(Constant.niuNiuGameMap.containsKey(roomNo)){
			
			// 洗牌
			NiuNiuServer.xiPai(roomNo);
			
			NNGameRoom room=Constant.niuNiuGameMap.get(roomNo);
			// 发牌
			List<UserPacket> userPacketList =NiuNiu.faPai(room.getPai(), 5, room.getSpecialType());
			
			// 配牌
			for (int i = 0; i < userPacketList.size(); i++) {
				
				UserPacket packet = userPacketList.get(i);
				if(i==0){ 
					UserPacket zhuang = new UserPacket(packet.getPs(), true, room.getSpecialType());
					packet.type = zhuang.type;
					packet.setWin(zhuang.isWin());
				}else{
					UserPacket zhuang = new UserPacket(userPacketList.get(0).getPs(), true, room.getSpecialType());
					UserPacket userpacket = new UserPacket(packet.getPs(), room.getSpecialType());
					PackerCompare.getWin(userpacket, zhuang);
					packet.type = userpacket.type;
					packet.setWin(userpacket.isWin());
				}
			}
			
			// 保存牌组
			room.setUserPacketList(userPacketList);
			
			// 积分结算
			room.getUserPacketMap().put(room.getZhuang(), userPacketList.get(0));
			UserPacket zhuangPacket = room.getUserPacketMap().get(room.getZhuang());
			
			for (int i = 1; i < userPacketList.size(); i++) {
				
				// 计算输赢
				UserPacket zhuang = new UserPacket(zhuangPacket.getPs(), true, room.getSpecialType());
				UserPacket userpacket = new UserPacket(userPacketList.get(i).getPs(), room.getSpecialType());
				UserPacket winner = PackerCompare.getWin(userpacket, zhuang);
				
				// 玩家投注赔付
				for (String xiazhuUUID : room.getUserPacketMap().keySet()) {
					
					if(!xiazhuUUID.equals(room.getZhuang())){
						
						// 获取玩家投注分数
						double score = room.getPlayerMoneyNum(Integer.valueOf(room.getPlayerAccount(xiazhuUUID)), i-1);
						if(score!=0){
							
							// 积分赔率
							double totalScore = score*winner.getRatio(room)*room.getScore();
							// 下注玩家信息
							UserPacket xiazhuPacket = room.getUserPacketMap().get(xiazhuUUID);
							// 闲家赢
							if(userpacket.isWin()){
								xiazhuPacket.setScore(xiazhuPacket.getScore() + totalScore);
								zhuangPacket.setScore(zhuangPacket.getScore() - totalScore);
							}else{ // 庄家赢
								xiazhuPacket.setScore(xiazhuPacket.getScore() - totalScore);
								zhuangPacket.setScore(zhuangPacket.getScore() + totalScore);
							}
						}
					}
				}
			}
			
			// 玩家数据
			JSONArray users = new JSONArray();
			for (String uuid : room.getUserPacketMap().keySet()) {

				UserPacket up = room.getUserPacketMap().get(uuid);
				if(up.getScore()!=0){
					
					JSONObject result = new JSONObject();
					result.put("id", room.getPlayerMap().get(uuid).getId());
					result.put("score", up.getScore());
					users.add(result);

					// 设置玩家总积分
					int totalScore = (int) (room.getPlayerMap().get(uuid).getScore() + up.getScore());
					room.getPlayerMap().get(uuid).setScore(totalScore);
				}
			}
			
			// 保存牌局胜负记录
			room.addQuShiList();
			
			// 结算数据返回
			JSONObject obj = new JSONObject();
			
			obj.put("zhuang", room.getPlayerMap().get(room.getZhuang()).getMyIndex());
			obj.put("zhuangScore", zhuangPacket.getScore());
			
			// 庄家区域
			JSONObject zhuangResult = new JSONObject();
			zhuangResult.put("pai", room.getUserPacketList().get(0).getMyPai());
			zhuangResult.put("result", room.getUserPacketList().get(0).type);
			obj.put("zhuangResult", zhuangResult);
			
			// 闲家下注区域信息
			JSONArray xianResult = new JSONArray();
			for (int j = 1; j <= 4; j++) {
				JSONObject result = new JSONObject();
				UserPacket up = room.getUserPacketList().get(j);
				result.put("pai", up.getMyPai());
				result.put("result", up.type);
				xianResult.add(result);
			}
			obj.put("xianResult", xianResult);
			
			for (String uuid : room.getPlayerMap().keySet()) {
				
				// 获取座位上的玩家数据
				String account = room.getPlayerMap().get(uuid).getAccount();
				obj.put("users", room.getBRNNAllPlayer(account));
				// 4个区域的下注
		    	JSONObject myXiazhu = room.getplaceArrayNums(Integer.valueOf(room.getPlayerAccount(uuid)));
				obj.put("myXiazhu", myXiazhu);
				int score = (int) room.getUserPacketMap().get(uuid).getScore();
				// 自己的总分
				obj.put("myScore", score);
				
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
				// -1/0/1 自己的输赢(总的,0是未下注)
				if(xiazhuScore>0){
					if(score>=0){
						obj.put("myResult", 1);
					}else{
						obj.put("myResult", -1);
					}
				}else{
					// 庄家
					if(uuid.equals(room.getZhuang())){
						if(zhuangPacket.getScore()>=0){
							obj.put("myResult", 1);
						}else{
							obj.put("myResult", -1);
						}
					}else{
						obj.put("myResult", 0);
					}
				}
				
				SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
				if(clientother!=null){
					clientother.sendEvent("gameJiesuanPush_BRNN", obj);
				}
				LogUtil.print("结算："+ JSONArray.fromObject(obj));
			}
			
			// 游戏状态
			Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_JIESUAN);
			// 局数记录
			Constant.niuNiuGameMap.get(roomNo).setGameIndex(room.getGameIndex()+1);
			// 连庄数
			Constant.niuNiuGameMap.get(roomNo).setLianzhuang(room.getLianzhuang()+1);
			
			JSONArray userIds = new JSONArray();
			// 金币结算
			for (int i = 0; i < users.size(); i++) {
				
				JSONObject user = users.getJSONObject(i);
				long uid = user.getLong("id");
				userIds.add(uid);
				int coins = user.getInt("score");
				if(coins<0){
					JSONObject userobj = mjBiz.getUserInfoByID(uid);
					if(obj!=null){
						int userCoins = userobj.getInt("coins");
						if(userCoins+coins<0){
							coins = -userCoins;
						}
					}
				}
				String sql = "update za_users set coins=coins+? where id=?";
				DBUtil.executeUpdateBySQL(sql, new Object[]{ coins, uid});
			}
			
			// 玩家扣水
			if(room.getFee()>0){
				mjBiz.dealGoldRoomFee(userIds, roomNo, 11, room.getFee(), "2");
			}
			
			// 在结算->发牌->亮牌->显示分数,之后才换庄,需要有20秒左右的间隔
			try {
				Thread.sleep(GLOBALTIMER[2]*1000);
				if(Constant.niuNiuGameMap.get(roomNo)!=null){
					Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_QIANGZHUANG);
				}
				// 开启换庄（准备）定时器
				new MutliThreadBRNN(null, roomNo, 0).start();
				
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
}
