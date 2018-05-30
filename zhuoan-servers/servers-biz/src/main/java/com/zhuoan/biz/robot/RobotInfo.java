package com.zhuoan.biz.robot;

import org.springframework.stereotype.Component;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:32 2018/5/29
 * @Modified By:
 **/
@Component
public class RobotInfo {
    /**
     * 机器人账号
     */
    private String robotAccount;
    /**
     * 机器人所在房间号
     */
    private String playRoomNo;
    /**
     * 机器人游戏id
     */
    private int playGameId;
    /**
     * 机器人事件类型
     */
    private int actionType;
    /**
     * 消息发送延迟时间
     */
    private int delayTime;
    /**
     * 退出局数
     */
    private int outTimes;
    /**
     * 最大退出分数
     */
    private double maxOutScore;
    /**
     * 最小退出分数
     */
    private double minOutScore;

    public String getRobotAccount() {
        return robotAccount;
    }

    public void setRobotAccount(String robotAccount) {
        this.robotAccount = robotAccount;
    }

    public String getPlayRoomNo() {
        return playRoomNo;
    }

    public void setPlayRoomNo(String playRoomNo) {
        this.playRoomNo = playRoomNo;
    }

    public int getPlayGameId() {
        return playGameId;
    }

    public void setPlayGameId(int playGameId) {
        this.playGameId = playGameId;
    }

    public int getActionType() {
        return actionType;
    }

    public void setActionType(int actionType) {
        this.actionType = actionType;
    }

    public int getDelayTime() {
        return delayTime;
    }

    public void setDelayTime(int delayTime) {
        this.delayTime = delayTime;
    }

    public int getOutTimes() {
        return outTimes;
    }

    public void setOutTimes(int outTimes) {
        this.outTimes = outTimes;
    }

    public double getMaxOutScore() {
        return maxOutScore;
    }

    public void setMaxOutScore(double maxOutScore) {
        this.maxOutScore = maxOutScore;
    }

    public double getMinOutScore() {
        return minOutScore;
    }

    public void setMinOutScore(double minOutScore) {
        this.minOutScore = minOutScore;
    }

    public void subDelayTime() {
        this.delayTime--;
    }

    public void subOutTimes() {
        this.outTimes--;
    }
}
