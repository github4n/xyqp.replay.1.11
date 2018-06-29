package com.zhuoan.biz.model.ddz;

import com.zhuoan.biz.model.GameRoom;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:33 2018/6/27
 * @Modified By:
 **/
public class DdzGameRoom extends GameRoom {
    /**
     * 倍数
     */
    private int multiple = 1;
    /**
     * 地主牌
     */
    private List<String> landlordCard = new ArrayList<String>();
    /**
     * 上一次出牌
     */
    private List<String> lastCard = new ArrayList<String>();
    /**
     * 上一个操作玩家
     */
    private String lastOperateAccount = null;
    /**
     * 地主玩家
     */
    private String landlordAccount;
    /**
     * 当前操作玩家
     */
    private int focusIndex;

    /**
     * 玩家牌局信息
     */
    private ConcurrentMap<String,UserPacketDdz> userPacketMap = new ConcurrentHashMap<String, UserPacketDdz>();
    /**
     * 操作记录记录
     */
    private List<JSONObject> operateRecord = new ArrayList<>();
    /**
     * 胜利玩家
     */
    private String winner;

    public int getMultiple() {
        return multiple;
    }

    public void setMultiple(int multiple) {
        this.multiple = multiple;
    }

    public List<String> getLandlordCard() {
        return landlordCard;
    }

    public void setLandlordCard(List<String> landlordCard) {
        this.landlordCard = landlordCard;
    }

    public List<String> getLastCard() {
        return lastCard;
    }

    public void setLastCard(List<String> lastCard) {
        this.lastCard = lastCard;
    }

    public String getLandlordAccount() {
        return landlordAccount;
    }

    public void setLandlordAccount(String landlordAccount) {
        this.landlordAccount = landlordAccount;
    }

    public int getFocusIndex() {
        return focusIndex;
    }

    public void setFocusIndex(int focusIndex) {
        this.focusIndex = focusIndex;
    }

    public ConcurrentMap<String, UserPacketDdz> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, UserPacketDdz> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public String getLastOperateAccount() {
        return lastOperateAccount;
    }

    public void setLastOperateAccount(String lastOperateAccount) {
        this.lastOperateAccount = lastOperateAccount;
    }

    public String getWinner() {
        return winner;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }

    public List<JSONObject> getOperateRecord() {
        return operateRecord;
    }

    public void setOperateRecord(List<JSONObject> operateRecord) {
        this.operateRecord = operateRecord;
    }
}
