package com.zhuoan.biz.model.ddz;

import java.util.ArrayList;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:38 2018/6/27
 * @Modified By:
 **/
public class UserPacketDdz {
    /**
     * 游戏状态
     */
    private int status;
    /**
     * 分数
     */
    private double score;
    /**
     * 手牌
     */
    private List<String> myPai = new ArrayList<>();
    /**
     * 叫地主次数
     */
    private int callNum;
    /**
     * 胜利次数
     */
    private int winNum;
    /**
     * 地主次数
     */
    private int landlordNum;
    /**
     * 是否托管
     */
    private int isTrustee;
    /**
     * 参与局数
     */
    private int playTimes;
    /**
     * 是否同意解散房间
     */
    private int isCloseRoom;
    /**
     * 记牌器结束时间
     */
    private String jpqEndTime;

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

    public List<String> getMyPai() {
        return myPai;
    }

    public void setMyPai(List<String> myPai) {
        this.myPai = myPai;
    }

    public int getCallNum() {
        return callNum;
    }

    public void setCallNum(int callNum) {
        this.callNum = callNum;
    }

    public int getWinNum() {
        return winNum;
    }

    public void setWinNum(int winNum) {
        this.winNum = winNum;
    }

    public int getLandlordNum() {
        return landlordNum;
    }

    public void setLandlordNum(int landlordNum) {
        this.landlordNum = landlordNum;
    }

    public int getIsTrustee() {
        return isTrustee;
    }

    public void setIsTrustee(int isTrustee) {
        this.isTrustee = isTrustee;
    }

    public int getPlayTimes() {
        return playTimes;
    }

    public void setPlayTimes(int playTimes) {
        this.playTimes = playTimes;
    }

    public int getIsCloseRoom() {
        return isCloseRoom;
    }

    public void setIsCloseRoom(int isCloseRoom) {
        this.isCloseRoom = isCloseRoom;
    }

    public String getJpqEndTime() {
        return jpqEndTime;
    }

    public void setJpqEndTime(String jpqEndTime) {
        this.jpqEndTime = jpqEndTime;
    }
}
