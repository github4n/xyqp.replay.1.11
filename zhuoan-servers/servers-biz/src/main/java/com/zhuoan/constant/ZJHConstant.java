package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:32 2018/4/25
 * @Modified By:
 **/
public class ZJHConstant {
    /**
     * 炸金花游戏事件-准备
     */
    public static final int ZJH_GAME_EVENT_READY = 1;
    /**
     * 炸金花游戏事件-配牌
     */
    public static final int ZJH_GAME_EVENT_GAME = 2;
    /**
     * 炸金花游戏事件-退出
     */
    public static final int ZJH_GAME_EVENT_EXIT = 3;
    /**
     * 炸金花游戏事件-重连
     */
    public static final int ZJH_GAME_EVENT_RECONNECT = 4;
    /**
     * 炸金花游戏事件-解散房间
     */
    public static final int ZJH_GAME_EVENT_CLOSE_ROOM = 5;
    /**
     * 炸金花游戏状态-初始
     */
    public static final int ZJH_GAME_STATUS_INIT=0;
    /**
     * 炸金花游戏状态-准备
     */
    public static final int ZJH_GAME_STATUS_READY=1;
    /**
     * 炸金花游戏状态-游戏事件
     */
    public static final int ZJH_GAME_STATUS_GAME=2;
    /**
     * 炸金花游戏状态-结算
     */
    public static final int ZJH_GAME_STATUS_SUMMARY=3;
    /**
     * 炸金花游戏状态-总结算
     */
    public static final int ZJH_GAME_STATUS_FINAL_SUMMARY=4;

    /**
     * 玩家状态-初始
     */
    public static final int ZJH_USER_STATUS_INIT=0;
    /**
     * 玩家状态-准备
     */
    public static final int ZJH_USER_STATUS_READY=1;
    /**
     * 玩家状态-暗牌
     */
    public static final int ZJH_USER_STATUS_AP=2;
    /**
     * 玩家状态-看牌
     */
    public static final int ZJH_USER_STATUS_KP=3;
    /**
     * 玩家状态-弃牌
     */
    public static final int ZJH_USER_STATUS_QP=4;
    /**
     * 玩家状态-失败
     */
    public static final int ZJH_USER_STATUS_LOSE=5;
    /**
     * 玩家状态-胜利
     */
    public static final int ZJH_USER_STATUS_WIN=6;
    /**
     * 玩家状态-结算
     */
    public static final int ZJH_USER_STATUS_SUMMARY=7;
    /**
     * 玩家状态-总结算
     */
    public static final int ZJH_USER_STATUS_FINAL_SUMMARY=8;
    /**
     * 玩法-经典模式
     */
    public static final int ZJH_GAME_TYPE_CLASSIC = 0;
    /**
     * 玩法-必闷三圈
     */
    public static final int ZJH_GAME_TYPE_MEN = 1;
    /**
     * 玩法-激情模式
     */
    public static final int ZJH_GAME_TYPE_HIGH = 2;
    /**
     * 最低开始人数
     */
    public static final int ZJH_MIN_START_COUNT = 2;
    /**
     * 炸金花倒计时-初始
     */
    public static final int ZJH_TIMER_INIT = 0;
    /**
     * 炸金花倒计时-准备
     */
    public static final int ZJH_TIMER_READY = 15;
    /**
     * 炸金花倒计时-下注
     */
    public static final int ZJH_TIMER_XZ = 60;
    /**
     * 炸金花倒计时-解散
     */
    public static final int ZJH_TIMER_CLOSE = 60;
    /**
     * 炸金花游戏事件-跟到底
     */
    public static final int GAME_ACTION_TYPE_GDD = 1;
    /**
     * 炸金花游戏事件-弃牌
     */
    public static final int GAME_ACTION_TYPE_GIVE_UP = 2;
    /**
     * 炸金花游戏事件-比牌
     */
    public static final int GAME_ACTION_TYPE_COMPARE = 3;
    /**
     * 炸金花游戏事件-看牌
     */
    public static final int GAME_ACTION_TYPE_LOOK = 4;
    /**
     * 炸金花游戏事件-跟注
     */
    public static final int GAME_ACTION_TYPE_GZ = 5;
    /**
     * 炸金花游戏事件-加注
     */
    public static final int GAME_ACTION_TYPE_JZ = 6;
    /**
     * 炸金花游戏事件-总结算
     */
    public static final int GAME_ACTION_TYPE_FINAL_SUMMARY = 8;
    /**
     * 炸金花游戏事件-比牌结束
     */
    public static final int GAME_ACTION_TYPE_COMPARE_FINISH = 9;
}
