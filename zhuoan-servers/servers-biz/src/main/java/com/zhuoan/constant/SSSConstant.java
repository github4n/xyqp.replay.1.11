package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION 十三水常量类
 * @Date Created in 15:26 2018/4/21
 * @Modified By:
 **/
public class SSSConstant {
    /**
     * 十三水庄家类型-霸王庄
     */
    public static final int SSS_BANKER_TYPE_BWZ = 1;
    /**
     * 十三水庄家类型-互比
     */
    public static final int SSS_BANKER_TYPE_HB = 0;

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
     * 最低打枪人数
     */
    public static final int SSS_MIN_SWAT_COUNT = 3;

    public static final int SSS_TIMER_INIT = 0;
    public static final int SSS_TIMER_READY = 15;

    public static final int SSS_TIMER_GAME_EVENT = 30;

    public static final int SSS_GAME_ACTION_TYPE_AUTO = 1;

    public static final int SSS_GAME_ACTION_TYPE_COMMON = 2;

    public static final int SSS_GAME_ACTION_TYPE_SPECIAL = 3;

    public static final String SSS_DATA_KET_TYPE = "type";
    public static final String SSS_DATA_KET_MY_PAI = "myPai";
}
