package com.zhuoan.biz.model;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;

import java.util.UUID;

/**
 * 自动解散房间线程
 * @author nanoha
 *
 */
public class AutoExitThread extends Thread {
	
	private MaJiangBiz mjBiz;
	private SSSService sssService;
	private SSSGameEventDeal eventDel;
	private int type;
	private String roomNo;
	private boolean isExit=true;
	
	public AutoExitThread(SSSService sssService, String roomNo, int type){
    	this.sssService = sssService;
    	this.roomNo = roomNo;
    	this.type = type;
    	mjBiz=new MajiangBizImpl();
    	eventDel=new SSSGameEventDeal();
	}
	
	public boolean isExit() {
		return isExit;
	}

	public void setExit(boolean isExit) {
		this.isExit = isExit;
	}

	public void run(){
		switch (type) {
    	case 0:
    		jiesan();//申请解散
    		break;
    	default:
    		
    		break;
    	}
	}
		
	private void jiesan() {

    	// 如果房间存在
    	if(Constant.sssGameMap.containsKey(roomNo)){
    		int time = Constant.sssGameMap.get(roomNo).getCloseTime();
    		SSSGameRoom game= Constant.sssGameMap.get(roomNo);
    		//定时器倒计时
    		for (int i = time; i >= 0; i--) {
    			if(Constant.sssGameMap.get(roomNo)==null||Constant.sssGameMap.get(roomNo).getCloseTime() == -1) break;
    			System.out.println("房间解散倒计时1："+i);
    			try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
    			if(Constant.sssGameMap.get(roomNo)!=null&&Constant.sssGameMap.get(roomNo).getCloseTime()>1){
    				System.out.println("房间解散倒计时2："+i);
    				Constant.sssGameMap.get(roomNo).setCloseTime(i);
    				
    			}else if(Constant.sssGameMap.get(roomNo)!=null&&Constant.sssGameMap.get(roomNo).getCloseTime()==1){
    				System.out.println("房间解散倒计时3："+i);
    				System.out.println("进入自动解散");
    				if(Constant.sssGameMap.containsKey(roomNo)){
    					
    						JSONObject result = new JSONObject();
    	    				//解散房间之前发动总结算
    	    				if (game.getGameIndex()>0) {
    	    				
    	    					result.put("isSummary", 1);
    	    				}else{
    	    					result.put("isSummary", 0);
    	    				}
    	    				result.put("type", 1); //解散房间
    	    				result.put("result", 1);//房主解散
    	    				SocketIOClient askclient=null;
    	    				Constant.sssGameMap.get(roomNo).setCloseTime(0);
    	    				for(UUID uuid:game.getUuidList()){
    	    					
    	    						askclient= GameMain.server.getClient(uuid);
    	    					
    	    					if(askclient!=null){
    	    						askclient.sendEvent("exitRoomPush_SSS", result);
    	    					}
    	    					if (game.getGameIndex()>0) {
    	    						JSONObject obj=new JSONObject();
    	    						obj.element("room_no", roomNo);
    	    						obj.element("jiesan", 1);
    	    						System.out.println("线程解散出结算");
    	    						eventDel.gameSummary(askclient, obj);
    	    					}
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
    	    				System.out.println("自动解散房间："+roomNo);
    					
    					
	    				
    				}else{
    					
    					System.out.println("房间不存在："+roomNo);
    				}
    				break;
    			}
    		}
    	}
	}
}
