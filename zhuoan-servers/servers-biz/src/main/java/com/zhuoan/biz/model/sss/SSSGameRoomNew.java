package com.zhuoan.biz.model.sss;

import com.zhuoan.biz.model.GameRoom;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION 十三水房间实体类
 * @Date Created in 14:24 2018/4/21
 * @Modified By:
 **/
public class SSSGameRoomNew extends GameRoom{
    /**
     * 最低开始人数
     */
    private int minPlayer=2;
    /**
     * 加色
     */
    private int color;
    /**
     * 马牌
     */
    private String maPai;
    /**
     * 定庄方式(霸王庄，互比)
     */
    private int bankerType;
    /**
     * 玩家牌局信息
     */
    private ConcurrentHashMap<String,Player> userPacketMap = new ConcurrentHashMap<String, Player>();
    /**
     * 马牌类型 0表示不存在马牌
     */
    private int maPaiType = 0;

    public int getMinPlayer() {
        return minPlayer;
    }

    public void setMinPlayer(int minPlayer) {
        this.minPlayer = minPlayer;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getMaPai() {
        return maPai;
    }

    public void setMaPai(String maPai) {
        this.maPai = maPai;
    }

    public int getBankerType() {
        return bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public ConcurrentHashMap<String, Player> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, Player> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public int getMaPaiType() {
        return maPaiType;
    }

    public void setMaPaiType(int maPaiType) {
        this.maPaiType = maPaiType;
    }
}
