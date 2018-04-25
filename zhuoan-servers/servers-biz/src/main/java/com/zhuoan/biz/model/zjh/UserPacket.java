package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Arrays;


/**
 * 玩家当前牌局
 */
public class UserPacket {
	
	private Integer[] pai;//手里的牌
	public int type;//牌的类型 
	private boolean win=false;//是否赢了
	private boolean isBanker=false;//是否是庄家
	private int isReady;// 玩家准备状态
	private int status=-1;// 玩家游戏状态
	private double score;//分数
	public int isCloseRoom=0;//解散房间申请  0:未确认 1:同意  -1:拒绝
	public boolean isGenDaoDi=false;//是否跟到底
	public boolean isShow=false;//是否看牌
	private JSONArray bipaiList = new JSONArray();//比牌记录
	private int luck;//幸运值
	
	// 牌局统计数据
	private int winTimes;
	
	
	public Integer[] getPai() {
		return pai;
	}
	public void setPai(Integer[] pai) {
		this.pai = pai;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public boolean isWin() {
		return win;
	}
	public void setWin(boolean win) {
		this.win = win;
	}
	public boolean isBanker() {
		return isBanker;
	}
	public void setBanker(boolean isBanker) {
		this.isBanker = isBanker;
	}
	public int getIsReady() {
		return isReady;
	}
	public void setIsReady(int isReady) {
		this.isReady = isReady;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public double getScore() {
		return score;
	}
	public void setScore(double score) {
		this.score = score;
	}
	public int getIsCloseRoom() {
		return isCloseRoom;
	}
	public void setIsCloseRoom(int isCloseRoom) {
		this.isCloseRoom = isCloseRoom;
	}
	public JSONArray getBipaiList() {
		return bipaiList;
	}
	public void setBipaiList(JSONArray bipaiList) {
		this.bipaiList = bipaiList;
	}
	public int getLuck() {
		return luck;
	}
	public void setLuck(int luck) {
		this.luck = luck;
	}
	public int getWinTimes() {
		return winTimes;
	}
	public void setWinTimes(int winTimes) {
		this.winTimes = winTimes;
	}
	
	/**
	 * 初始牌局信息
	 */
	public void initUserPacket(){
        //牌的类型
        type=0;
        //是否赢了
        win=false;
        //分数
        score=0;
        //解散房间申请  0:未确认 1:同意  -1:拒绝
        isCloseRoom=0;
        //是否跟到底
        isGenDaoDi=false;
        //是否看牌
        isShow=false;
		bipaiList = new JSONArray();
		//setStatus(ZhaJinHuaCore.USERPACKER_STATUS_CHUSHI);
		//setIsReady(ZhaJinHuaCore.USERPACKER_STATUS_CHUSHI);
	}
	

	/**
	 * 添加比牌记录
	 * @param index
	 * @param bipai
	 */
	public void addBiPaiList(int index, Integer[] bipai) {
		
		JSONObject obj = new JSONObject();
		obj.put("index", index);
		obj.put("pai", bipai);
		obj.put("paiType", ZhaJinHuaCore.getPaiType(Arrays.asList(bipai)));
		this.bipaiList.add(obj);
	}
	
}
