package com.zhuoan.biz.model.bdx;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

public class BDXGameRoom extends GameRoom {

	private String roomNo;//房间号
	private int roomType; //房间类型（0：房卡  1：金币 2：代开房间）
	private String fangzhu;//房主
	private String zhuang;//庄家
	private List<String> pai;
	private int playerCount;//玩家人数
	private int maxplayer=0;//运行最大玩家人数
	private int gameType;//游戏模式
	private int gameStatus;//游戏阶段       
	private int readyCount=0;//准备人数
	private int gameIndex;//当前第几局
	private Map<String,Playerinfo> playerMap;//玩家个人信息
	private List<UUID> uuidList;//用户的uuid
	private Set<String> userAcc = new HashSet<String>();// 玩家account集合
	private Set<Long> userSet = new HashSet<Long>();// 玩家ID集合
	
	private JSONObject setting;//游戏全局设置

	
	
	public String getRoomNo() {
		return roomNo;
	}
	public void setRoomNo(String roomNo) {
		this.roomNo = roomNo;
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



	public int getReadyCount() {
		return readyCount;
	}

	public void setReadyCount(int readyCount) {
		this.readyCount = readyCount;
	}
	public int getGameIndex() {
		return gameIndex;
	}

	public void setGameIndex(int gameIndex) {
		this.gameIndex = gameIndex;
	}

	public Map<String, Playerinfo> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<String, Playerinfo> playerMap) {
		this.playerMap = playerMap;
	}

	public List<String> getPai() {
		return pai;
	}

	public void setPai(List<String> pai) {
		this.pai = pai;
	}
	public List<UUID> getUuidList() {
		return uuidList;
	}

	public void setUuidList(List<UUID> uuidList) {
		this.uuidList = uuidList;
	}

	public Set<Long> getUserSet() {
		return userSet;
	}

	public void setUserSet(Set<Long> userSet) {
		this.userSet = userSet;
	}
	
	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
	}
	public int getRoomType() {
		return roomType;
	}

	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}
	
	/**
	 * 获取已准备的玩家下标
	 * @return
	 */
	public Integer[] getReadyIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uc :playerMap.keySet()) {
			int ready = playerMap.get(uc).getStatus();
			if(ready>0){
				indexList.add(getPlayerMap().get(uc).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}
	
	/**
	 * 获取房间人员的信息
	 * @return
	 */
	public JSONArray getAllPlayer(){
		
		JSONArray array = new JSONArray();
		
		for(String uc : playerMap.keySet()){
			
			Playerinfo player = playerMap.get(uc);
		
			if(player!=null){
				
				JSONObject obj = new JSONObject();
				obj.put("name", player.getName());
				obj.put("headimg", player.getRealHeadimg());
				obj.put("score",player.getScore());
				obj.put("index", player.getMyIndex());
				obj.put("status", player.getStatus());
				obj.put("ip", player.getIp());
				obj.put("location", player.getLocation());
				obj.put("id", player.getAccount());
				obj.put("zhu", player.getLuck());
				array.add(obj);
			}
		}
		return array;
	}
	/**
	 * 获取当前玩家的下家的UUID
	 * @return
	 */
	public String getNextPlayer(String useruuid){
		int index=playerMap.get(useruuid).getMyIndex();
		int next=index+1;
		Playerinfo player = null;
		while (player==null&&index!=next) {
			if(next>=this.playerMap.size()){
				next=0;
			}
			for (String uc : playerMap.keySet()) {
				if(next==playerMap.get(uc).getMyIndex()){
					return uc;
				}
			}
			next++;
		}
		return zhuang;
	}
	
	/**
	 * 获取玩家的位置
	 * @return
	 */
	public int getPlayerIndex(String uc){
		if(this.playerMap.get(uc)!=null){
			return this.playerMap.get(uc).getMyIndex();
		}else{
			return 0;
		}
	}
	public JSONObject getSetting() {
		return setting;
	}

	public void setSetting(JSONObject setting) {
		this.setting = setting;
	}

	public Set<String> getUserAcc() {
		return userAcc;
	}

	public void setUserAcc(Set<String> userAcc) {
		this.userAcc = userAcc;
	}

	public int getMaxplayer() {
		return maxplayer;
	}

	public void setMaxplayer(int maxplayer) {
		this.maxplayer = maxplayer;
	}





}
