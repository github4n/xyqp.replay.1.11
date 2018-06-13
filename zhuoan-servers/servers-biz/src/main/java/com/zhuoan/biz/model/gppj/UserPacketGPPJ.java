package com.zhuoan.biz.model.gppj;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:38 2018/6/9
 * @Modified By:
 **/
public class UserPacketGPPJ {

    /**
     * 状态
     */
    private int status;

    /**
     * 抢庄倍数
     */
    private int bankerTimes;

    /**
     * 下注倍数
     */
    private int xzTimes;
    /**
     * 当局输赢分数
     */
    private double score;
    /**
     * 牌型
     */
    private int paiType;
    /**
     * 牌
     */
    private String[] pai;
    /**
     * 是否同意解散房间
     */
    private int isCloseRoom = 0;
    /**
     * 游戏局数
     */
    private int playTimes = 0;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getBankerTimes() {
        return bankerTimes;
    }

    public void setBankerTimes(int bankerTimes) {
        this.bankerTimes = bankerTimes;
    }

    public int getXzTimes() {
        return xzTimes;
    }

    public void setXzTimes(int xzTimes) {
        this.xzTimes = xzTimes;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getPaiType() {
        return paiType;
    }

    public void setPaiType(int paiType) {
        this.paiType = paiType;
    }

    public String[] getPai() {
        return pai;
    }

    public void setPai(String[] pai) {
        this.pai = pai;
    }

    public int getIsCloseRoom() {
        return isCloseRoom;
    }

    public void setIsCloseRoom(int isCloseRoom) {
        this.isCloseRoom = isCloseRoom;
    }

    public int getPlayTimes() {
        return playTimes;
    }

    public void setPlayTimes(int playTimes) {
        this.playTimes = playTimes;
    }
}
