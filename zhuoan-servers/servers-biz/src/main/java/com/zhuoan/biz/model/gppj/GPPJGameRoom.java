package com.zhuoan.biz.model.gppj;

import com.zhuoan.biz.model.GameRoom;
import net.sf.json.JSONArray;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:37 2018/6/9
 * @Modified By:
 **/
public class GPPJGameRoom extends GameRoom {
    /**
     * 庄家类型
     */
    private int bankerType;
    /**
     * 游戏筹码
     */
    private JSONArray baseNum = new JSONArray();
    /**
     * 抢庄倍数
     */
    private JSONArray qzTimes = new JSONArray();
    /**
     * 玩家牌局信息
     */
    private ConcurrentMap<String,UserPacketGPPJ> userPacketMap = new ConcurrentHashMap<String, UserPacketGPPJ>();
    /**
     * 牌
     */
    private String[] pai;
    /**
     * 筛子
     */
    private JSONArray dice = new JSONArray();
    /**
     * 切牌玩家
     */
    private int cutIndex;
    /**
     * 切牌位置
     */
    private int cutPlace;
    /**
     * 剩余牌
     */
    private int[] leftPai;
    /**
     * 加倍类型
     */
    private int multiple = -1;

    public int getBankerType() {
        return bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public JSONArray getBaseNum() {
        return baseNum;
    }

    public void setBaseNum(JSONArray baseNum) {
        this.baseNum = baseNum;
    }

    public JSONArray getQzTimes() {
        return qzTimes;
    }

    public void setQzTimes(JSONArray qzTimes) {
        this.qzTimes = qzTimes;
    }

    public ConcurrentMap<String, UserPacketGPPJ> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, UserPacketGPPJ> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public String[] getPai() {
        return pai;
    }

    public void setPai(String[] pai) {
        this.pai = pai;
    }

    public JSONArray getDice() {
        return dice;
    }

    public void setDice(JSONArray dice) {
        this.dice = dice;
    }

    public int getCutIndex() {
        return cutIndex;
    }

    public void setCutIndex(int cutIndex) {
        this.cutIndex = cutIndex;
    }

    public int getCutPlace() {
        return cutPlace;
    }

    public void setCutPlace(int cutPlace) {
        this.cutPlace = cutPlace;
    }

    public int[] getLeftPai() {
        return leftPai;
    }

    public void setLeftPai(int[] leftPai) {
        this.leftPai = leftPai;
    }

    public int getMultiple() {
        return multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }
}
