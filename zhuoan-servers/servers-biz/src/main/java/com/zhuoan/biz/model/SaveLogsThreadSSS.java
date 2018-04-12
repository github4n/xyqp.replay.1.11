package com.zhuoan.biz.model;

import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;
import net.sf.json.JSONArray;

public class SaveLogsThreadSSS extends Thread{

	private SSSGameRoom game;
	private SSSService sssService;
	private SSSGameEventDeal eventDel;
	private JSONArray us;
	private JSONArray uid;
	private boolean isExit=true;
	private boolean e=false;

	
	public SaveLogsThreadSSS(){}
    public SaveLogsThreadSSS(SSSGameRoom game, JSONArray us, JSONArray uid, boolean e){
    	this.game=game;
    	this.us=us;
    	this.uid=uid;
    	this.e=e;
    	sssService=new SSSServiceImpl();
    	eventDel=new SSSGameEventDeal();
    	
    }
    
    
  
    public void run() {
    	
    	sssService.gamelog(game, us,uid,e);
    	 
    	
    }
    
    
   
	public boolean isExit() {
		return isExit;
	}
	public void setExit(boolean isExit) {
		this.isExit = isExit;
	}
	
}
