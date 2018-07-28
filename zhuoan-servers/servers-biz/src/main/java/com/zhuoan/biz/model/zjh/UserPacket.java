package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.Arrays;


/**
 * 玩家当前牌局
 */
public class UserPacket {

    /**
     * 手里的牌
     */
    private Integer[] pai;
    /**
     * 牌的类型
     */
	public int type;
    /**
     * 是否赢了
     */
	private boolean win=false;
    /**
     * 是否是庄家
     */
	private boolean isBanker=false;
    /**
     * 玩家游戏状态
     */
	private int status=0;
    /**
     * 分数
     */
	private double score;
    /**
     * 解散房间申请  0:未确认 1:同意  -1:拒绝
     */
	public int isCloseRoom=0;
    /**
     * 是否跟到底
     */
	public boolean isGenDaoDi=false;
    /**
     * 是否看牌
     */
	public boolean isShow=false;
    /**
     * 比牌记录
     */
	private JSONArray bipaiList = new JSONArray();
    /**
     * 牌局统计数据
     */
	private int winTimes;
    /**
     * 参与游戏局数
     */
    private int playTimes = 0;

    private int luck;
	
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
	public int getWinTimes() {
		return winTimes;
	}
	public void setWinTimes(int winTimes) {
		this.winTimes = winTimes;
	}

    public int getPlayTimes() {
        return playTimes;
    }

    public void setPlayTimes(int playTimes) {
        this.playTimes = playTimes;
    }

    public int getLuck() {
        return luck;
    }

    public void setLuck(int luck) {
        this.luck = luck;
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
        // 游戏局数+1
        playTimes++;
		bipaiList = new JSONArray();
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
