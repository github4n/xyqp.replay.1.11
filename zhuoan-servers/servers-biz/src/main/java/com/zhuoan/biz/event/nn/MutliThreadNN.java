package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.nn.NiuNiu;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.nn.NiuNiuService;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.LogUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 线程异步 
 *
 */
public class MutliThreadNN extends Thread{
	
	private NiuNiuService nnService;
	private String roomNo;
	private int type;
	
    /**
     * @param nnService
     * @param roomNo
     * @param type  0、准备（开始游戏）倒计时，超过时间换庄（金币场）       1、下注倒计时      2、亮牌倒计时   3、抢庄   4、解散倒计时
     */
    public MutliThreadNN (NiuNiuService nnService, String roomNo, int type){
    	this.nnService = nnService;
    	this.roomNo = roomNo;
    	this.type = type;
    }
    
    public void run() {
    	
    	switch (type) {
    	case 0:
//    		ready();//准备
    		break;
    	case 1:
    		xiazhu(NNGameEventDeal.GLOBALTIMER[0]);//下注
    		break;
    	case 2:
    		showPai(NNGameEventDeal.GLOBALTIMER[1]);//亮牌
    		break;
    	case 3:
    		qiangZhuang(NNGameEventDeal.GLOBALTIMER[2]);//抢庄
    		break;
    	case 4:
    		jiesan();//申请解散
    		break;
    	case 5:
    		startGame();//开始游戏（抢庄）
    		break;
    	case 6:
    		autoReady(NNGameEventDeal.GLOBALTIMER[3]);//超时开始游戏
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
    		//定时器倒计时
    		for (int i = 25; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			////System.out.println("牛牛准备倒计时："+i);
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				if(room.getGameStatus()== NiuNiu.GAMESTATUS_READY || room.getGameStatus()== NiuNiu.GAMESTATUS_LIANGPAI){
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    					
    					// 5秒后自动切换到准备状态
    					if(i==5 && room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI){
    						Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_READY);
    					}
    					
    					// 超过时间未开始游戏，执行换庄操作
    					if(i==0){
    						
    						// 换庄前，移除掉线的玩家 
    						if(nnService!=null){
    							nnService.cleanPlayer(room);
    						}
    						
    						String oldZhuang = room.getZhuang();
    						String zhuang = room.getNextPlayer(oldZhuang);
    						Constant.niuNiuGameMap.get(roomNo).setZhuang(zhuang);
    						JSONObject result = new JSONObject();
    						result.put("zhuang", room.getPlayerIndex(zhuang));
        					for (String uid  : room.getUserPacketMap().keySet()) {
        						// 重置庄家信息
        						SocketIOClient clientother= GameMain.server.getClient(room.getUUIDByClientTag(uid));
        						if(clientother!=null){
        							clientother.sendEvent("huanZhuangPush_NN", result);
        						}
        					}
        					
        					// 当房间玩家人数大于1人时
        					if(room.getUserIDSet().size()>1){
        						
        						// 开启准备定时器，开始计时
        						MutliThreadNN m = new MutliThreadNN(null, roomNo, 0);
        						m.start();
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
    	}
	}
    
    
	/**
     * 下注
	 * @param time
     */
    public void xiazhu(int time) {
    	
    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		if (Constant.niuNiuGameMap.get(roomNo)!=null) {
				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(time);
			}
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
//    			System.out.println("牛牛下注倒计时："+i);
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				
    				// 金币场或房卡场且局数还没用完
    				if(room.getRoomType()==1 || room.getGameCount()>room.getGameIndex()){ 
    					if(room.getGameStatus()==NiuNiu.GAMESTATUS_XIAZHU){
    						Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间

    						boolean isReady = true;
    						for(String uid : room.getPlayerMap().keySet()){
    							UserPacket up = room.getUserPacketMap().get(uid);
    							if(!room.getZhuang().equals(uid)&&up.getIsReady()!=2&&up.getStatus()!=-1){ // 判断是否存在玩家未下注
    								if(room.getZhuangType()==3){ // 明牌抢庄
    									if(up.getPs()!=null&&up.getPs()[0]!=null){
    										isReady = false;
    									}
    								}else{
    									isReady = false;
    								}
    							}
    						}
    						
    						//如果下注时间到，则执行比牌方法
    						if(i==0 || isReady){
    							
    							try {
    								if(!isReady){
    									JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
    									// 选择最低筹码
    									int money = baseNum.getJSONObject(0).getInt("val");
    									for(String uuid:room.getPlayerMap().keySet()){
    										UserPacket up = room.getUserPacketMap().get(uuid);
    		    							if(!room.getZhuang().equals(uuid)&&up.getIsReady()!=2&&up.getStatus()!=-1){ // 判断是否存在玩家未下注
    		    								// 下注倒计时结束，自动下注（最小筹码）
    		    								if((room.getZhuangType()!=2 && room.getZhuangType()!=3)||(up.getPs()!=null&&up.getPs()[0]!=null)){
    		    									
    		    									int index = room.getPlayerMap().get(uuid).getMyIndex();
    		    									JSONObject obj = new JSONObject();
    		    									obj.put("room_no", room.getRoomNo());
    		    									obj.put("num", index);
    		    									obj.put("place", index);
    		    									obj.put("money", money);
    		    									obj.put("auto", 1);
    		    									obj.put("uuid", uuid);
    		    									SocketIOClient client=GameMain.server.getClient(room.getUUIDByClientTag(uuid));
    		    									new NNGameEventDeal().gameXiaZhu(client, obj);
    		    								}
    		    							}
    									}
    								}
    								
    								// 进入结算
    								if(Constant.niuNiuGameMap.containsKey(roomNo)
    										&&Constant.niuNiuGameMap.get(roomNo).getGameStatus()!=NiuNiu.GAMESTATUS_JIESUAN){
    									LogUtil.print("下注倒计时结束，进入结算，当前游戏状态："+room.getGameStatus());
    									
    									nnService.jieSuan(roomNo);
    								}
    								
    							} catch (Exception e) {
    								e.printStackTrace();
    							}
    						}
    					}else{
    						break;
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
     * 亮牌
     * @param time 
     */
    private void showPai(int time) {

    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		if (Constant.niuNiuGameMap.get(roomNo)!=null) {
				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(time);
			}
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			//System.out.println("牛牛亮牌倒计时："+i);
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				if(room.getGameStatus()==NiuNiu.GAMESTATUS_JIESUAN){
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
						if (i==time-5 && room.isRobot()) {
							AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 2);
							a.start();
						}
    					if(i==0){
        					// 游戏状态
        					Constant.niuNiuGameMap.get(roomNo).setGameStatus(NiuNiu.GAMESTATUS_LIANGPAI);
        					
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
        						Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uid).setIsReady(0);
        						Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
        					}
        					
        					// 金币场
        					if(room.getRoomType()==1){

        						// 1、游戏结束后，开始下一局游戏时，移除掉线的玩家 
        						//nnService.cleanDisconnectPlayer(room);
        						
        						// 2、超过时间没有开始游戏，需要换庄
        						// 开启准备定时器，开始计时
        						MutliThreadNN m = new MutliThreadNN(null, roomNo, 0);
        						m.start();
        						
        						if (room.isRobot()) {
									AutoThreadNN a = new AutoThreadNN(nnService, roomNo, 3);
									a.start();
									
									AutoThreadNN a1 = new AutoThreadNN(nnService, roomNo, 4);
									a1.start();
								}
        						
        					}

        					// 抢庄模式
//        					if(room.getZhuangType()==2 || room.getZhuangType()==3){ 
//        						
//        						MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 3);
//        						m.start();
//        					}
        					
        				}
    				}else{
    					//System.out.println("牛牛亮牌倒计时：线程终止");
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
	 * 抢庄
	 * @param time
	 */
	private void qiangZhuang(int time) {
		
		int restartTime = 0;

    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		if (Constant.niuNiuGameMap.get(roomNo)!=null) {
				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(time);
				restartTime = Constant.niuNiuGameMap.get(roomNo).getRestartTime();
			}
    		
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				
    				// 重新发牌终止上一轮线程
    				if (restartTime!=room.getRestartTime()) {
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(0);
						break;
					}
    				
    				if(room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI
    						|| room.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    					//如果抢庄时间到，则默认不抢
    					if(i==0){
    						try {
    							for (String uuid : room.getUserPacketMap().keySet()) {
    								UserPacket up = room.getUserPacketMap().get(uuid);
    								if(!(up.getIsReady()==10||up.getIsReady()==-1)){ // 获取当前未抢庄的玩家
    									if(up.getPs()!=null&&up.getPs()[0]!=null){
    										Constant.niuNiuGameMap.get(roomNo).getUserPacketMap().get(uuid).qzTimes=-1;
    										nnService.qiangZhuang(roomNo, "-1", uuid);
    									}
    								}
    							}
    							
    							// 开启开始游戏定时器
//    							MutliThreadNN m = new MutliThreadNN(nnService, roomNo, 5);
//    							m.start();
    							
    						} catch (Exception e) {
    							e.printStackTrace();
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
    	}
	}


	/**
	 * 申请解散
	 */
	private void jiesan() {

    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		int time = Constant.niuNiuGameMap.get(roomNo).getCloseTime();
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			if(Constant.niuNiuGameMap.get(roomNo)!=null&&Constant.niuNiuGameMap.get(roomNo).getCloseTime()>0){
    				
    				Constant.niuNiuGameMap.get(roomNo).setCloseTime(i);
    				
    				if (i==0) {
						try {
							Thread.sleep(5000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (Constant.niuNiuGameMap.get(roomNo)!=null) {
							MaJiangBiz mjBiz=new MajiangBizImpl();
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
							Constant.niuNiuGameMap.remove(roomNo);
						}
					}
    			}else{
    				System.out.println("房间不存在："+roomNo);
    				break;
    			}
    		}
    	}
	}
	

	/**
	 * 开始游戏
	 */
	private void startGame() {
		
    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		
    		NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    		List<String> uuids = new ArrayList<String>();
    		// 抢庄最大倍数
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
			}
    		int time = uuids.size();
    		if(time==0){
    			time = 1;
    		}
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				
    				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    				if(room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI
    						|| room.getGameStatus()==NiuNiu.GAMESTATUS_QIANGZHUANG){
    					// 开始游戏
    					if(i==0){
    						try {
    							
    							// 开始游戏
    							new NNGameEventDeal().startGame(room);
    							
    						} catch (Exception e) {
    							e.printStackTrace();
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
    	}
	}
	
	
	/**
	 * 倒计时结束后自动开始游戏
	 * @param time 
	 */
	private void autoReady(int time) {

    	// 如果房间存在
    	if(Constant.niuNiuGameMap.containsKey(roomNo)){
    		
    		if (Constant.niuNiuGameMap.get(roomNo)!=null) {
				Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(time);
			}

    		NNGameRoom game = Constant.niuNiuGameMap.get(roomNo);
    		JSONObject result = new JSONObject();
    		result.put("timer", time);
    		for (String uuid  : game.getUserPacketMap().keySet()) {
    			if (!game.getRobotList().contains(uuid)) {
					
    				SocketIOClient askclient=GameMain.server.getClient(game.getUUIDByClientTag(uuid));
    				if(askclient!=null){
    					askclient.sendEvent("nnTimerPush_NN", result);
    				}
    			}
			}
    		
    		// 通知观战玩家
    		if (game.isGuanzhan()&&game.getGzPlayerMap().size()>0) {
				for (String string : game.getGzPlayerMap().keySet()) {
					SocketIOClient askclient=GameMain.server.getClient(game.getGzPlayerMap().get(string).getUuid());
    				if(askclient!=null){
    					askclient.sendEvent("nnTimerPush_NN", result);
    				}
				}
			}
    		
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			//System.out.println("牛牛准备倒计时："+i);
    			if(Constant.niuNiuGameMap.get(roomNo)!=null){
    				
    				NNGameRoom room = Constant.niuNiuGameMap.get(roomNo);
    				
    				// 准备人数多于2人才踢人
    				int count = 0;
    				for (String uid:room.getUserPacketMap().keySet()) {
    					int ready = room.getUserPacketMap().get(uid).getIsReady();
    					if(ready!=0&&ready!=10){
    						count++;
    					}
    				}
    				if((room.getGameStatus()==NiuNiu.GAMESTATUS_READY || room.getGameStatus()==NiuNiu.GAMESTATUS_LIANGPAI)&&count>1){
    					Constant.niuNiuGameMap.get(roomNo).setXiazhuTime(i);//保存倒计时时间
    					
    					// 超过时间未准备，自动准备
    					if(i==0){
    						
    						int readyType = room.getReadyovertime();
    						if(readyType==1){ // 自动准备
    							
    							for (String uid  : room.getUserPacketMap().keySet()) {
            						int ready = room.getUserPacketMap().get(uid).getIsReady();
            						if((ready==0 || ready==10)&&!room.getRobotList().contains(uid)){
            							
            							SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
            							if(clientother!=null){
            								JSONObject data = new JSONObject();
            								data.put("room_no", roomNo);
            								data.put("auto", 1);
            								new NNGameEventDeal().gameReady(clientother, data);
            							}
            						}
            					}
    						}else if(readyType==2){ // 踢出房间

    							NNGameEventDeal deal = new NNGameEventDeal();
        						// 超过时间未准备，清出房间
            					try {
            						Set<String> uuidList = room.getUserPacketMap().keySet();
            						List<String> lxList = new ArrayList<String>();
            						for (String uuid : uuidList) {
            							// 还没准备
            							if(room.getUserPacketMap().get(uuid).getIsReady() != 1){
            								lxList.add(uuid);
            							}
            						}
            						for (String uuid : lxList) {
            							
            							if(room.getPlayerMap().get(uuid)!=null){
            								deal.exitRoom(uuid, room.getRoomNo(), room.getPlayerMap().get(uuid).getId());
            							}
            						}
            					} catch (Exception e) {
            						e.printStackTrace();
            					}
            					
            					if(room.getZhuangType()==2||room.getZhuangType()==3){
            						// 触发抢庄方法
            						room = Constant.niuNiuGameMap.get(roomNo);
            						if (room!=null) {
            							for (String uid  : room.getUserPacketMap().keySet()) {
            								int ready = room.getUserPacketMap().get(uid).getIsReady();
            								if(ready==1){
            									
            									SocketIOClient clientother=GameMain.server.getClient(room.getUUIDByClientTag(uid));
            									if(clientother!=null){
            										JSONObject data = new JSONObject();
            										data.put("room_no", roomNo);
            										data.put("auto", 1);
            										deal.gameReady(clientother, data);
            										break;
            									}
            								}
            							}
									}
            					}else{
            						// 多于一人才开始游戏
            						if(room.getUserPacketMap().size()>1){
            							// 触发开始游戏
            							deal.startGame(room);
            						}
            					}
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
    	}
	}

}
