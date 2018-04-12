package com.zhuoan.biz.model;


import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.constant.Constant;

public class MutliThreadSSS1 extends Thread{

	private SSSGameRoom game;
	private boolean isExit=true;
	public boolean isExit() {
		return isExit;
	}
	public void setExit(boolean isExit) {
		this.isExit = isExit;
	}


	private boolean isStartClose=false;
	private MutliThreadSSS thread;
	
	public MutliThreadSSS1 (){}
    public MutliThreadSSS1 (String roomNo) throws InterruptedException{
    	if(Constant.sssGameMap.containsKey(roomNo)){
    		this.game=Constant.sssGameMap.get(roomNo);
    	}
    }
    
    
    /**
     * type:1、准备     2配牌    3结算
     */
    public void run() {
    	int flag=-1;
    	try {
	    	while(true){
	    		if(!isExit) break;
	    		int r=2;
	    		
	    		if (game.getRoomType()==0||game.getRoomType()==3) {
					r=game.getPlayerCount();
				}
	    		
	    		//if(this.game.getUuidList().size()<r)//当前牌局人数不够 终止线程
	    		System.err.println("最低人数："+this.game.getPlayerCount()+",准备人数："+this.game.getReadyCount());
	    		if(this.game.getReadyCount()<r)//当前牌局人数不够 终止线程&&this.game.getUuidList().size()<r
	    		{
	    			System.out.println("当前牌局准备人数不够 终止线程");
	    			game.setGameStatus(0);
	    			if (thread!=null) {
						thread.setExit(false);
					}	
	    			game.setThread(null);
	    			break;
	    		}else{
	    			
	    			if(game!=null){
	    				if(this.game.getGameStatus() != flag){
	    					if(thread!=null) thread.setExit(false);//已存在运行的线程 先终止
	    					Thread.sleep(1000);//等待1s让上个线程完全终止
	    					if (game.getRoomType()==0||game.getRoomType()==2) {
								game.setPeipaiTime(game.getSetting().getInt("fangkapeipai"));//重置配牌时间
							}else{
								game.setPeipaiTime(game.getSetting().getInt("goldpeipai"));
								
							}
	    					thread=new MutliThreadSSS(game);
	    					thread.start();
	    					System.out.println("当前状态："+this.game.getGameStatus());
	    					flag=this.game.getGameStatus();
	    				}
	    				/*System.err.println(game.getGameStatus()+","+flag);*/
	    			}
	    		}
	    		
	    		Thread.sleep(1000);
	    	}
    	} catch (InterruptedException e) {
    		e.printStackTrace();
    	}
    	
    }
}
