package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.zjh.ZJHGame;
import com.zhuoan.biz.service.zjh.ZhaJinHuaService;
import com.zhuoan.constant.Constant;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *	炸金花定时器
 */
public class MutliThreadZJH extends Thread{

    private final static Logger logger = LoggerFactory.getLogger(MutliThreadZJH.class);

	public static int READY = 0; // 准备
	public static int XIAZHU = 1; // 下注
	public static int GENDAODI = 2; //跟到底

	public static int xzTimer = 60; // 下注时间
	public static int gdzTimer = 2; //跟到底等待时间

	private ZhaJinHuaService zjhService;
	private String roomNo;
	private int type;
	private String nextUUID;

	public MutliThreadZJH (ZhaJinHuaService zjhService, String roomNo, int type){

		this.zjhService = zjhService;
		this.roomNo = roomNo;
		this.type = type;
	}

	public MutliThreadZJH (ZhaJinHuaService zjhService, String roomNo, String nextUUID, int type){

		this.zjhService = zjhService;
		this.roomNo = roomNo;
		this.nextUUID = nextUUID;
		this.type = type;
	}

	/**
	 * type: 0 准备  1 下注 2跟到底
	 */
	 public void run() {

		 switch (type) {
		 case 0:
			 ready();//准备倒计时
			 break;
		 case 1:
			 xiazhu();//下注倒计时
			 break;
		 case 2:
			 genDaoDi();//跟到底倒计时
			 break;
		 default:
			 break;
		 }
	 }


	 /**
	  * 准备倒计时
	  * 倒计时结束后，没有准备的玩家自动清出房间
	  */
	 private void ready() {

		 // 如果房间存在
		 if(Constant.zjhGameMap.containsKey(roomNo)){

			 int timer = 15;
			 ZJHGame game = Constant.zjhGameMap.get(roomNo);
			 JSONObject result = new JSONObject();
			 result.put("type", READY);
			 result.put("timer", timer);
			 for (String uuid  : game.getUserPacketMap().keySet()) {
				 SocketIOClient askclient= GameMain.server.getClient(game.getUUIDByClientTag(uuid));
				 if(askclient!=null){
					 askclient.sendEvent("zjhTimerPush_ZJH", result);
				 }
			 }

			 for (int i = timer; i >= 0; i--) {
				 try {
					 if (Constant.zjhGameMap.get(roomNo)!=null) {
						 Constant.zjhGameMap.get(roomNo).setTimer(i);
					 }
					 Thread.sleep(1000);
				 } catch (InterruptedException e1) {
					 logger.error("",e1);
				 }
				 if(Constant.zjhGameMap.get(roomNo)!=null){

					 //    				Constant.zjhGameMap.get(roomNo).setTimer(i);
					 ZJHGame room = Constant.zjhGameMap.get(roomNo);
					 
					 int count = 0;
					 for (String uid:Constant.zjhGameMap.get(roomNo).getUserPacketMap().keySet()) {
						 int ready = Constant.zjhGameMap.get(roomNo).getUserPacketMap().get(uid).getIsReady();
						 if(ready!=0){
							 count++;
						 }
					 }

					 if((room.getGameStatus()== ZhaJinHuaCore.GAMESTATUS_READY || room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_JIESUAN)&&count>1){

						 if(i==0){

							 // 超过时间未准备，清出房间
							 try {
								 Set<String> uuidList = room.getUserPacketMap().keySet();
								 List<String> lxList = new ArrayList<String>();
								 for (String uuid : uuidList) {

									 if(room.getUserPacketMap().get(uuid).getStatus()!=ZhaJinHuaCore.USERPACKER_STATUS_READY){
										 lxList.add(uuid);
									 }
								 }
								 ZJHGameEventDeal deal = new ZJHGameEventDeal();
								 for (String uuid : lxList) {

									 if(room.getPlayerMap().get(uuid)!=null){
										 deal.exitRoom(uuid, room.getRoomNo(), room.getPlayerMap().get(uuid).getId());
									 }
								 }
							 } catch (Exception e) {
								 logger.error("",e);
							 }
							 
							 if(room.getReadyCount()>1 && room.getUserPacketMap().size()>1 && room.isAllReady()){
								 // 触发开始游戏
								 new ZJHGameEventDeal().startGame(room);
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
	  * 下注倒计时
	  * 倒计时结束后未下注的玩家自动弃牌
	  */
	 private void xiazhu() {

		 // 如果房间存在
		 if(Constant.zjhGameMap.containsKey(roomNo)){

			 String focus = Constant.zjhGameMap.get(roomNo).getFocus();

			 for (int i = xzTimer; i >= 0; i--) {
				 try {
					 if (Constant.zjhGameMap.get(roomNo)!=null) {
						 Constant.zjhGameMap.get(roomNo).setTimer(i);
					 }
					 Thread.sleep(1000);
				 } catch (InterruptedException e1) {
					 logger.error("",e1);
				 }
				 if(Constant.zjhGameMap.get(roomNo)!=null){

					 //    				Constant.zjhGameMap.get(roomNo).setTimer(i);
					 ZJHGame room = Constant.zjhGameMap.get(roomNo);
					 if(room.getGameStatus()==ZhaJinHuaCore.GAMESTATUS_XIAZHU && room.getFocus().equals(focus)){
						 // 超过时间未下注，自动弃牌
						 if(i==0){

							 zjhService.qipai(focus, roomNo);
							 
							 // 下一个操作的玩家
							 String nextUUID = room.getNextOperationPlayer(room, focus);
							 // 跟到底定时器
							 new MutliThreadZJH(zjhService, roomNo, nextUUID, MutliThreadZJH.GENDAODI).start();
							 // 下注倒计时
 							 new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();

							 /*
    						if(Constant.zjhGameMap.get(roomNo).getGameStatus()==ZhaJinHuaCore.GAMESTATUS_XIAZHU){
    							// 确定下次操作的玩家
    							String nextUUID = room.getNextOperationPlayer(room, focus);
    							if(!nextUUID.equals(focus)&&room.getUserPacketMap().get(nextUUID).isGenDaoDi){
    								if(room.getUserPacketMap().get(nextUUID).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
    										|| room.getUserPacketMap().get(nextUUID).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
    									// 自动跟注
    									JSONObject gzData = new JSONObject();
    									gzData.put("room_no", room.getRoomNo());
    									gzData.put("uuid", nextUUID);
    									gzData.put("type", ZJHGameEventDeal.GENZHU);
    									gzData.put("auto", 1);
    									SocketIOClient gzClient=GameMain.server.getClient(room.getUUIDByClientTag(nextUUID));
    									new ZJHGameEventDeal().gameEvent(gzClient, gzData);
    								}
    							}
    							// 下注倒计时
    							new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
    						}*/
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
	  * 判断下一个操作玩家是否跟到底，是则自动下注
	  * @param    
	  * @return void  
	  * @throws
	  * @date 2018年1月12日
	  */
	 private void genDaoDi() {
		 boolean gen = false;
		 if (Constant.zjhGameMap.containsKey(roomNo)) {
			 if (Constant.zjhGameMap.get(roomNo)!=null) {
				 if (checkXiazhu(Constant.zjhGameMap.get(roomNo))) {
					 // 下一个操作玩家是否为跟到底
					 gen = Constant.zjhGameMap.get(roomNo).getUserPacketMap().get(nextUUID).isGenDaoDi;
				 }
			 }
			 for (int i = gdzTimer; i >= 0; i--) {
				 try {
					 if (Constant.zjhGameMap.get(roomNo)!=null) {
						 // 无法下注不进行休眠
						 if (checkXiazhu(Constant.zjhGameMap.get(roomNo))) {
							 Thread.sleep(250);
						 }
					 }
				 } catch (InterruptedException e) {
					 logger.error("",e);
				 }
				 if (Constant.zjhGameMap.get(roomNo)!=null) {
					 ZJHGame room = Constant.zjhGameMap.get(roomNo);
					 if (i==0) {
						 if (checkXiazhu(room)) {
							 // 3秒前后都是跟到底状态才进行跟到底操作
							 if(gen&&room.getUserPacketMap().get(nextUUID).isGenDaoDi){
								 if(room.getUserPacketMap().get(nextUUID).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
										 || room.getUserPacketMap().get(nextUUID).getStatus()==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
									 double score = room.getCurrentScore();
									 
									 // 跟到底是否成功
									 boolean xiazhu2 = zjhService.xiazhu(nextUUID, roomNo, score, ZJHGameEventDeal.GENZHU);
									 if (xiazhu2) {
										 // 下一个操作的玩家
										 String nextUUID1 = room.getNextOperationPlayer(room, nextUUID);
										 // 跟到底定时器
										 new MutliThreadZJH(zjhService, roomNo, nextUUID1, MutliThreadZJH.GENDAODI).start();
										 // 下注定时器
										 new MutliThreadZJH(zjhService, roomNo, MutliThreadZJH.XIAZHU).start();
									 }
								 }
							 }
						 }else {
							 System.out.println("超过单局下注上限或已达到下注轮数上限");
							 //强制结算
							 zjhService.compelBiPai(roomNo, nextUUID);
							 break;
						 }
					 }
				 }else {
					 System.out.println("房间不存在："+roomNo);
					 break;
				 }
			 }
		 }
	 }
	 
	 /**
	  * 是否强制结算
	  * @param @param room
	  * @param @return   
	  * @return boolean  
	  * @throws
	  * @date 2018年1月12日
	  */
	 public boolean checkXiazhu(ZJHGame room) {
		 // 超过单局下注上限，强制结算
		 if(room.getSingleMaxScore()>0 && room.getTotalScore()>=room.getSingleMaxScore()){

			 return false;
		 }

		 // 达到下注轮数上限（最后一轮所有人都已下注），强制结算
		 if(room.getTotalGameNum()>0 
				 && room.getGameNum()>=room.getTotalGameNum() 
				 && room.getProgressIndex().length==room.getYixiazhu().size()){

			 return false;
		 }

		 return true;
	 }

}
