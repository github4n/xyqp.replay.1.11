package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.math.BigDecimal;
import java.util.*;

public class ZJHGame extends GameRoom {

	private String roomNo;//房间号
	private int roomType; //房间类型（0：房卡  1：金币）
	private int gameType; // 游戏类型（0：投注 1：倍率）
	private String fangzhu;//房主
	private String zhuang;//庄家
	private String focus;//当前操作玩家
	private int zhuangType;//定庄方式
	private int[] pai;
	private int paiIndex;
	private int playerCount;//玩家人数
	private int gameStatus;//游戏阶段 
	private double score;//底分
	private double totalScore;//总分
	private double singleMaxScore;//单注最大限制
	private double currentScore;//当前分数（加注要大于当前分数）
	private double enterScore;//准入积分
	private double leaveScore;//离场积分
	private int readyCount=0;//准备人数
	private int gameCount;//游戏总局数
	private int gameIndex;//当前第几局
	private int gameNum;//当前第几轮
	private int totalGameNum;//轮数上限
	private Map<String,Playerinfo> playerMap;//玩家个人信息
	private Map<String,UserPacket> userPacketMap;//玩家牌局信息
	Set<Long> userIDSet = new HashSet<Long>();
	private String baseNum;//加注["10","20","30","40","50","60"]
	private List<Integer> yixiazhu = new ArrayList<Integer>();// 已下注的玩家
	private JSONArray xiazhuList = new JSONArray();
	private String jiesuanTime;
	private int timer;//定时器
	private double fee;//金币、元宝扣除的服务费
	private int readyovertime;//准备超时（0：不处理 1：自动准备 2：踢出房间）
	private int wanfa;//玩法（0：普通模式  1：必闷三圈 2：激情模式）
	
	
	public String getRoomNo() {
		return roomNo;
	}

	public void setRoomNo(String roomNo) {
		this.roomNo = roomNo;
	}

	public int getRoomType() {
		return roomType;
	}

	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}

	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
	}

	public String getFangzhu() {
		return fangzhu;
	}

	public void setFangzhu(String fangzhu) {
		this.fangzhu = fangzhu;
	}

	public String getZhuang() {
		return zhuang;
	}

	public void setZhuang(String zhuang) {
		this.zhuang = zhuang;
	}

	public int getZhuangType() {
		return zhuangType;
	}

	public void setZhuangType(int zhuangType) {
		this.zhuangType = zhuangType;
	}

	public int[] getPai() {
		return pai;
	}

	public void setPai(int[] pai) {
		this.pai = pai;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}

	public int getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(int gameStatus) {
		this.gameStatus = gameStatus;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getTotalScore() {
		return totalScore;
	}

	public void setTotalScore(double totalScore) {
		this.totalScore = totalScore;
	}

	public double getSingleMaxScore() {
		return singleMaxScore;
	}

	public void setSingleMaxScore(double singleMaxScore) {
		this.singleMaxScore = singleMaxScore;
	}

	public double getCurrentScore() {
		return currentScore;
	}

	public void setCurrentScore(double currentScore) {
		this.currentScore = currentScore;
	}

	public double getEnterScore() {
		return enterScore;
	}

	public void setEnterScore(double enterScore) {
		this.enterScore = enterScore;
	}

	public double getLeaveScore() {
		return leaveScore;
	}

	public void setLeaveScore(double leaveScore) {
		this.leaveScore = leaveScore;
	}

	public int getReadyCount() {
		return readyCount;
	}

	public void setReadyCount(int readyCount) {
		this.readyCount = readyCount;
	}

	public int getGameCount() {
		return gameCount;
	}

	public void setGameCount(int gameCount) {
		this.gameCount = gameCount;
	}

	public int getGameIndex() {
		return gameIndex;
	}

	public void setGameIndex(int gameIndex) {
		this.gameIndex = gameIndex;
	}

	public int getGameNum() {
		return gameNum;
	}

	public void setGameNum(int gameNum) {
		this.gameNum = gameNum;
	}

	public int getTotalGameNum() {
		return totalGameNum;
	}

	public void setTotalGameNum(int totalGameNum) {
		this.totalGameNum = totalGameNum;
	}

	public Map<String, Playerinfo> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<String, Playerinfo> playerMap) {
		this.playerMap = playerMap;
	}

	public Map<String, UserPacket> getUserPacketMap() {
		return userPacketMap;
	}

	public void setUserPacketMap(Map<String, UserPacket> userPacketMap) {
		this.userPacketMap = userPacketMap;
	}

	public Set<Long> getUserIDSet() {
		return userIDSet;
	}

	public void setUserIDSet(Set<Long> userIDSet) {
		this.userIDSet = userIDSet;
	}

	public String getFocus() {
		return focus;
	}

	public void setFocus(String focus) {
		this.focus = focus;
	}

	public String getBaseNum() {
		return baseNum;
	}

	public void setBaseNum(String baseNum) {
		this.baseNum = baseNum;
	}


	public List<Integer> getYixiazhu() {
		return yixiazhu;
	}

	public void setYixiazhu(List<Integer> yixiazhu) {
		this.yixiazhu = yixiazhu;
	}

	public JSONArray getXiazhuList() {
		return xiazhuList;
	}

	public void setXiazhuList(JSONArray xiazhuList) {
		this.xiazhuList = xiazhuList;
	}
	
	
	/**
	 * 添加下注记录
	 * @param index 玩家下标
	 * @param score 下注积分
	 */
	public void addXiazhuList(int index, double score) {
		
		JSONObject obj = new JSONObject();
		double count = score/currentScore;
		if(count>1){ // 筹码加倍记录多条
			for (int i = 0; i < count; i++) {
				obj.put("index", index);
				obj.put("score", currentScore);
				this.xiazhuList.add(obj);
			}
		}else{
			obj.put("index", index);
			obj.put("score", score);
			this.xiazhuList.add(obj);
		}
	}
	
	/**
	 * 获取玩家下注积分
	 * @param index
	 * @return
	 */
	public double getXiazhuScore(int index) {
		
		double score = 0;
		for (int i = 0; i < this.xiazhuList.size(); i++) {
			JSONObject obj = this.xiazhuList.getJSONObject(i);
			if(!Dto.isObjNull(obj)&&obj.getInt("index") == index){
//				score += obj.getDouble("score");
				/**
				 * 金皇冠筹码小数  2018/02/11 wqm
				 */
				BigDecimal b1 = new BigDecimal(Double.toString(score));
			    BigDecimal b2 = new BigDecimal(Double.toString(obj.getDouble("score")));
				score = b1.add(b2).doubleValue();
			}
		}
		return score;
	}

	public String getJiesuanTime() {
		return jiesuanTime;
	}

	public void setJiesuanTime(String jiesuanTime) {
		this.jiesuanTime = jiesuanTime;
	}

	public int getTimer() {
		return timer;
	}

	public void setTimer(int timer) {
		this.timer = timer;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public int getReadyovertime() {
		return readyovertime;
	}

	public void setReadyovertime(int readyovertime) {
		this.readyovertime = readyovertime;
	}

	public int getWanfa() {
		return wanfa;
	}

	public void setWanfa(int wanfa) {
		this.wanfa = wanfa;
	}

	/**
	 * 获取房间人员的信息
	 * @return
	 */
	public JSONArray getAllPlayer(){
		
		JSONArray array = new JSONArray();
		
		for(String uuid : playerMap.keySet()){
			
			Playerinfo player = playerMap.get(uuid);
			if(player!=null){
				
				JSONObject obj = new JSONObject();
				obj.put("account", player.getAccount());
				obj.put("name", player.getName());
				obj.put("headimg", player.getRealHeadimg());
				obj.put("sex", player.getSex());
				obj.put("ip", player.getIp());
				obj.put("vip", player.getVip());
				obj.put("location", player.getLocation());
				obj.put("area", player.getArea());
				obj.put("score", player.getScore());
				obj.put("index", player.getMyIndex());
				obj.put("status", player.getStatus());
				obj.put("introduction", player.getSignature());
				array.add(obj);
			}
		}
		return array;
	}
	
	/**
	 * 获取玩家的准备状态
	 * @return
	 */
	public JSONArray getPlayerIsReady(){
		
		JSONArray array = new JSONArray();
		for (String uuid :userPacketMap.keySet()) {
			JSONObject obj = new JSONObject();
			obj.put("index", playerMap.get(uuid).getMyIndex());
			int ready = getUserPacketMap().get(uuid).getIsReady();
			if(ready== ZhaJinHuaCore.USERPACKER_STATUS_READY){
				obj.put("result", ready);
			}else{
				obj.put("result", 0);
			}
			array.add(obj);
		}
		return array;
	}
	
	/**
	 * 获取当前玩家的状态
	 * @return
	 */
	public JSONArray getPlayerStatus(){
		
		JSONArray array = new JSONArray();
		for (String uuid :userPacketMap.keySet()) {
			JSONObject obj = new JSONObject();
			obj.put("index", playerMap.get(uuid).getMyIndex());
			obj.put("result", userPacketMap.get(uuid).getStatus());
			array.add(obj);
		}
		return array;
	}

	/**
	 * 获取玩家的位置
	 * @return
	 */
	public int getPlayerIndex(String uuid){
		if(this.playerMap.get(uuid)!=null){
			return this.playerMap.get(uuid).getMyIndex();
		}else{
			return 0;
		}
	}
	
	/**
	 * 获取当前玩家的下家的UUID
	 * @return
	 */
	public String getNextPlayer(String useruuid){
		
		if(playerMap.get(useruuid)!=null){
			
			int playerCount = 7;
			int index=playerMap.get(useruuid).getMyIndex();
			int next=index+1;
			Playerinfo player = null;
			while (player==null&&index!=next) {
				if(next>=playerCount){
					next=0;
				}
				for (String uuid : playerMap.keySet()) {
					if(next==playerMap.get(uuid).getMyIndex()){
						return uuid;
					}
				}
				next++;
			}
		}
		return zhuang;
	}
	

	/**
	 * 获取当前游戏局数（第几局）
	 * @return
	 */
	public int getCurrentGameIndex() {
		
		if(gameStatus==ZhaJinHuaCore.GAMESTATUS_JIESUAN){
			return gameIndex;
		}
		return gameIndex + 1;
	}

	/**
	 * 获取已准备的玩家下标
	 * @return
	 */
	public Integer[] getReadyIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			int ready = getUserPacketMap().get(uuid).getIsReady();
			if(ready==ZhaJinHuaCore.USERPACKER_STATUS_READY){
				indexList.add(getPlayerMap().get(uuid).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}
	
	/**
	 * 获取正在游戏中的玩家下标
	 * @return
	 */
	public Integer[] getGameIngIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			int status = getUserPacketMap().get(uuid).getStatus();
			if(status>ZhaJinHuaCore.USERPACKER_STATUS_READY){
				indexList.add(getPlayerMap().get(uuid).getMyIndex());
			}
		}
		Collections.sort(indexList);
		return indexList.toArray(new Integer[indexList.size()]);
	}

	/**
	 * 获取还未比牌的玩家下标
	 * @return
	 */
	public Integer[] getProgressIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			int status = getUserPacketMap().get(uuid).getStatus();
			if(status==ZhaJinHuaCore.USERPACKER_STATUS_ANPAI || status==ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){ // 暗牌或是明牌
				indexList.add(getPlayerMap().get(uuid).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}
	
	
	/**
	 * 初始化游戏房间
	 */
	public void initGame(){
		
		// 重置牌的下标
		paiIndex = 0;
		
		// 重置投注信息
		totalScore = 0;
		gameNum = 1;
		currentScore = score;
		yixiazhu.clear();
		xiazhuList = new JSONArray();
		
		// 重置玩家信息
		for (String uuid : getUserPacketMap().keySet()) {
			if(userPacketMap.containsKey(uuid)){
				userPacketMap.get(uuid).initUserPacket();
			}
		}
	}
	
	
	/**
	 * 洗牌
	 * @return 
	 */
	public void xiPai(){
		
		int[] pais = ZhaJinHuaCore.PAIS;
		//玩法（0：普通模式  1：必闷三圈 2：激情模式）
		if(wanfa==2){
			pais = ZhaJinHuaCore.PAIS_JQ;
		}
		int[] indexs = randomPai(pais.length);
		pai = new int[pais.length];
		for (int i = 0; i < indexs.length; i++) {
			pai[i] = pais[indexs[i]];
		}
	}
	
	
	/**
	 * 打乱牌的下标
	 * @param paiCount 牌数量
	 * @return
	 */
	private int[] randomPai(int paiCount){
		
		int[] nums = new int[paiCount];
		for (int i = 0; i < nums.length; i++) {
			while(true){
				int num = RandomUtils.nextInt(paiCount);
				if(!ArrayUtils.contains(nums,num)){
					nums[i] = num;
					break;
				}else if(num==0){ //若是0，判断之前是否已存在
					if(ArrayUtils.indexOf(nums, num) == i){
						break;
					}
				}
			}
		}
		return nums;
	}
	
	
	/**
	 * 发牌
	 * @return
	 */
	public Integer[] faPai(){
		
		Integer[] myPai = new Integer[3];
		for (int i = 0; i < 3; i++) {
			myPai[i] = pai[paiIndex];
			paiIndex = paiIndex + 1;
		}
		return myPai;
	}
	
	/**
	 * 增加总下注积分
	 * @param score
	 */
	public void addTotalScore(double score) {
//		this.totalScore += score;
		/**
		 * 金皇冠筹码小数  2018/02/11 wqm
		 */
		BigDecimal b1 = new BigDecimal(Double.toString(this.totalScore));
		BigDecimal b2 = new BigDecimal(Double.toString(score));
		this.totalScore = b1.add(b2).doubleValue();
	}

	
	/**
	 * 所有玩家都是准备状态
	 * @return
	 */
	public boolean isAllReady() {
		
		for (String uuid : getUserPacketMap().keySet()) {
			int ready = getUserPacketMap().get(uuid).getStatus();
			if(ready!=ZhaJinHuaCore.USERPACKER_STATUS_READY){
				return false;
			}
		}
		return true;
	}

	/**
	 * 清除玩家下注信息
	 * @param index
	 */
	public void clearXiaZhuList(int index) {
		
		for (int i = 0; i < xiazhuList.size(); i++) {
			JSONObject obj = xiazhuList.getJSONObject(i);
			if(obj.getInt("index")==index){
				xiazhuList.getJSONObject(i).put("score", 0);
			}
		}
	}

	/**
	 * 获取下个可操作的玩家
	 * @param room
	 * @param uuid
	 * @return
	 */
	public String getNextOperationPlayer(ZJHGame room, String uuid){
		
		uuid = room.getNextPlayer(uuid);
		int count = room.getUserPacketMap().size();
		// 若玩家已经开完牌，则换下一个
		while(room.getUserPacketMap().get(uuid).getStatus()!=ZhaJinHuaCore.USERPACKER_STATUS_ANPAI
				&& room.getUserPacketMap().get(uuid).getStatus()!=ZhaJinHuaCore.USERPACKER_STATUS_KANPAI){
			
			uuid = room.getNextPlayer(uuid);
			count--;
			if(count<=1){
				break;
			}
		}
		((ZJHGame) RoomManage.gameRoomMap.get(room.getRoomNo())).setFocus(uuid);
		return uuid;
	}

	/**
	 * 获取玩家uuid
	 * @param clientTag
	 * @return
	 */
	public UUID getUUIDByClientTag(String clientTag){
		
		if(playerMap.containsKey(clientTag)){
			return playerMap.get(clientTag).getUuid();
		}
		return null;
	}

}
