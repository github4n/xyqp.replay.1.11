package com.zhuoan.biz.model.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;

public class MutliThreadSSS extends Thread{

	private SSSGameRoom game;
	private SSSService sssService;
	private SSSGameEventDeal eventDel;
	private boolean isExit=true;
	
	public MutliThreadSSS (){}
    public MutliThreadSSS (SSSGameRoom game){
    	this.game=game;
    	sssService=new SSSServiceImpl();
    	eventDel=new SSSGameEventDeal();
    }
    
    
    /**
     * type:1、准备     2配牌    3结算
     */
    public void run() {
    	switch (this.game.getGameStatus()) {
		case 0: //准备阶段
			ready();
			break;
		case 3:	
			peipai();//配牌阶段
			break;
		case 4:	
			peipai();//有人配牌
			break;
		case 5:
			summary();
			break;
		default:
			break;
		}
    	
    }
    
    
   
	public boolean isExit() {
		return isExit;
	}
	public void setExit(boolean isExit) {
		this.isExit = isExit;
	}
	/**
     * 准备
     */
    public void ready() {
    	//eventDel.playerExit( new JSONObject().element("room_no", game.getRoomNo()));
    	//game.setPeipaiTime((game.getRoomType()==0)?game.getSetting().getInt("fangkapeipai"):game.getSetting().getInt("goldpeipai"));//重置配牌时间
    	if (game.getRoomType()==0||game.getRoomType()==2) {
			game.setPeipaiTime(game.getSetting().getInt("fangkapeipai"));//重置配牌时间
		}else{
			game.setPeipaiTime(game.getSetting().getInt("goldpeipai"));
			
		}
    	if (game.getRoomType()==0) return;//房卡场不检查准备状态
    	
    	int time=game.getSetting().getInt("goldready");
    	if (game.getReadyTime()!=0) {
    		time=game.getReadyTime();
    	}
    	for (int i = time; i >= 0; i--) {
    		if(!isExit){
    			game.setReadyTime(time);
    			break;
    		}     			
    		
    		try {
    			Thread.sleep(1000);

    			System.out.println("准备倒计时定时："+i);

    			
    			if((i==0||(game.getReadyCount() >= game.getPlayerCount()&&game.getReadyCount() == game.getUserAcc().size()))&&game.getGameStatus()==0){
    				eventDel.playerExit( new JSONObject().element("room_no", game.getRoomNo()));
    				game.setReadyTime(time);

    				try {
    					SocketIOClient clientother=null;
    					
    					//0.时间到后所有人状态设为准备
    					for (String uid:game.getUserAcc()) {
    						Playerinfo pi=game.getPlayerMap().get(uid);
							//sssService.isReady(game.getRoomNo(), uid);
							if(clientother==null)
								clientother= GameMain.server.getClient(pi.getUuid());
						}
    					
    					//执行发牌操作 只执行一次
    					if(clientother!=null){
    						SSSGameEventDeal event=new SSSGameEventDeal();
							event.gameReady(clientother,new JSONObject().element("room_no", game.getRoomNo()).element("thred", true));
							break;
						}
    					
    					//开始滞留判断
    					
    					summary();
    					
    				}catch (Exception e) {
						e.printStackTrace();
					}
    			}else{
    				game.setReadyTime(i);    				
    			}
    		} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
    	}
	}
    
    /**
     * 配牌
     */
    public void peipai() {
    	// 房间号
    	JSONObject postdata=new JSONObject();
    	postdata.element("type", 1);//自动配牌

    	int time=0;
    	if (game.getRoomType()==0||game.getRoomType()==2) {
    		time=game.getSetting().getInt("fangkapeipai");//重置配牌时间
		}else{
			 time=game.getSetting().getInt("goldpeipai");
			
		}
    	if (game.getPeipaiTime()!=0) {
    		time=game.getPeipaiTime();
    	}
    	
    	for (int i = time; i >= 0; i--) {
    		//线程终止时不进行循环
    		if(!isExit){
    			game.setPeipaiTime(i);
    			break;
    		} 
    		
    		try {
    			Thread.sleep(1000);
    			
//    			System.out.println("配牌倒计时："+time);
//				System.out.println("配牌倒计时定时："+i);

				if(i==0){
					//如果配牌时间到未配牌的人执行强制配牌
					for (String uuid : game.getPlayerPaiJu().keySet()) {
						if(game.getPlayerPaiJu().get(uuid).getStatus()==1)
							sssService.peiPai(game.getRoomNo(), uuid, 1, postdata);
					}
					synchronized (this) {
						//发起结算
						if (game.getGameType()==1) {
							sssService.jieSuan(game.getRoomNo());
						}else{
							sssService.jieSuan1(game.getRoomNo());
						}						
					}
					game.setPeipaiTime(0);
				}else
					game.setPeipaiTime(i);
    		}catch (InterruptedException e1) {
				e1.printStackTrace();
			}
    	}
	}
    
    /**
     * 结算滞留检查
     */
    public void summary(){
    	
    	/*if (game.getRoomType()!=0) {
    		//eventDel.playerExit( new JSONObject().element("room_no", game.getRoomNo()));
    		for (int i = 40; i >= 0; i--) {
	    		if(!isExit) break; 
	    		try {
	    			Thread.sleep(1000);
					System.out.println("结算滞留倒计时："+i);
					if(i==0){
						eventDel.playerExit( new JSONObject().element("room_no", game.getRoomNo()));
					}
	    		}catch (InterruptedException e1) {
					e1.printStackTrace();
				}
	    	}
    	}else{
    		game.setPeipaiTime((game.getRoomType()==0)?game.getSetting().getInt("fangkapeipai"):game.getSetting().getInt("goldpeipai"));//房卡场在这里重置时间
    		System.out.println("房卡场不进行结算滞留检查");
    	}*/
    }
}
