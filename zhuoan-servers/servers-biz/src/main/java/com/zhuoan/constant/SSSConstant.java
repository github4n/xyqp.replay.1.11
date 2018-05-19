package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION 十三水常量类
 * @Date Created in 15:26 2018/4/21
 * @Modified By:
 **/
public class SSSConstant {
    /**
     * 十三水游戏事件-准备
     */
    public static final int SSS_GAME_EVENT_READY = 1;
    /**
     * 十三水游戏事件-配牌
     */
    public static final int SSS_GAME_EVENT_EVENT = 2;
    /**
     * 十三水游戏事件-退出
     */
    public static final int SSS_GAME_EVENT_EXIT = 3;
    /**
     * 十三水游戏事件-重连
     */
    public static final int SSS_GAME_EVENT_RECONNECT = 4;
    /**
     * 十三水游戏事件-解散房间
     */
    public static final int SSS_GAME_EVENT_CLOSE_ROOM = 5;
    /**
     * 十三水游戏事件-上庄
     */
    public static final int SSS_GAME_EVENT_BE_BANKER = 6;
    /**
     * 十三水游戏事件-下注
     */
    public static final int SSS_GAME_EVENT_XZ = 7;
    /**
     * 十三水庄家类型-互比
     */
    public static final int SSS_BANKER_TYPE_HB = 0;
    /**
     * 十三水庄家类型-霸王庄
     */
    public static final int SSS_BANKER_TYPE_BWZ = 1;
    /**
     * 十三水庄家类型-坐庄
     */
    public static final int SSS_BANKER_TYPE_ZZ = 2;

    /**
     * 十三水游戏状态-初始
     */
    public static final int SSS_GAME_STATUS_INIT = 0;
    /**
     * 十三水游戏状态-准备
     */
    public static final int SSS_GAME_STATUS_READY = 1;
    /**
     * 十三水游戏状态-配牌
     */
    public static final int SSS_GAME_STATUS_GAME_EVENT = 2;
    /**
     * 十三水游戏状态-比牌
     */
    public static final int SSS_GAME_STATUS_COMPARE = 3;
    /**
     * 十三水游戏状态-结算
     */
    public static final int SSS_GAME_STATUS_SUMMARY = 4;
    /**
     * 十三水游戏状态-总结算
     */
    public static final int SSS_GAME_STATUS_FINAL_SUMMARY = 5;
    /**
     * 十三水游戏状态-下注
     */
    public static final int SSS_GAME_STATUS_XZ = 6;
    /**
     * 十三水游戏状态-上庄
     */
    public static final int SSS_GAME_STATUS_TO_BE_BANKER = 8;

    /**
     * 十三水玩家状态-初始
     */
    public static final int SSS_USER_STATUS_INIT = 0;
    /**
     * 十三水玩家状态-准备
     */
    public static final int SSS_USER_STATUS_READY = 1;
    /**
     * 十三水玩家状态-配牌
     */
    public static final int SSS_USER_STATUS_GAME_EVENT = 2;
    /**
     * 十三水玩家状态-比牌
     */
    public static final int SSS_USER_STATUS_COMPARE = 3;
    /**
     * 十三水玩家状态-结算
     */
    public static final int SSS_USER_STATUS_SUMMARY = 4;
    /**
     * 十三水玩家状态-总结算
     */
    public static final int SSS_USER_STATUS_FINAL_SUMMARY = 5;
    /**
     * 十三水玩家状态-下注
     */
    public static final int SSS_USER_STATUS_XZ = 6;
    /**
     * 最低打枪人数
     */
    public static final int SSS_MIN_SWAT_COUNT = 4;
    /**
     * 初始倒计时
     */
    public static final int SSS_TIMER_INIT = 0;
    /**
     * 准备倒计时
     */
    public static final int SSS_TIMER_READY = 15;
    /**
     * 配牌倒计时
     */
    public static final int SSS_TIMER_GAME_EVENT = 120;
    /**
     * 下注倒计时
     */
    public static final int SSS_TIMER_GAME_XZ = 10;
    /**
     * 配牌类型-自动
     */
    public static final int SSS_GAME_ACTION_TYPE_AUTO = 1;
    /**
     * 配牌类型-手动
     */
    public static final int SSS_GAME_ACTION_TYPE_COMMON = 2;
    /**
     * 配牌类型-特殊牌
     */
    public static final int SSS_GAME_ACTION_TYPE_SPECIAL = 3;
    /**
     * 比牌动画时间-基础时间
     */
    public static final int SSS_COMPARE_TIME_BASE = 15;
    /**
     * 比牌动画时间-翻牌时间
     */
    public static final int SSS_COMPARE_TIME_SHOW = 7;
    /**
     * 比牌动画时间-打枪时间
     */
    public static final int SSS_COMPARE_TIME_DQ = 11;
    /**
     * 比牌动画时间-全垒打时间
     */
    public static final int SSS_COMPARE_TIME_SWAT = 38;
    /**
     * 下注基数
     */
    public static final int SSS_XZ_BASE_NUM = 75;

    public static final String SSS_DATA_KET_TYPE = "type";
    public static final String SSS_DATA_KET_MY_PAI = "myPai";
}
