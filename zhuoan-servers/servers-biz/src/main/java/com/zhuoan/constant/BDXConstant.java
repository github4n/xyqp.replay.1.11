package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION 比大小常量类
 * @Date Created in 15:14 2018/4/26
 * @Modified By:
 **/
public class BDXConstant {
    /**
     * 比大小游戏事件-下注
     */
    public static final int BDX_GAME_EVENT_XZ = 1;
    /**
     * 比大小游戏事件-弃牌
     */
        public static final int BDX_GAME_EVENT_GIVE_UP = 2;
    /**
     * 比大小游戏事件-退出
     */
    public static final int BDX_GAME_EVENT_EXIT = 3;
    /**
     * 比大小游戏事件-重连
     */
    public static final int BDX_GAME_EVENT_RECONNECT = 4;
    /**
     * 比大小游戏状态-初始
     */
    public static final int BDX_GAME_STATUS_INIT=0;
    /**
     * 比大小游戏状态-准备
     */
    public static final int BDX_GAME_STATUS_READY=1;
    /**
     * 比大小游戏状态-下注
     */
    public static final int BDX_GAME_STATUS_GAME_EVENT=2;
    /**
     * 比大小游戏状态-结算
     */
    public static final int BDX_GAME_STATUS_SUMMARY=3;

    /**
     * 玩家状态-初始
     */
    public static final int BDX_USER_STATUS_INIT=0;
    /**
     * 玩家状态-准备
     */
    public static final int BDX_USER_STATUS_READY=1;
    /**
     * 玩家状态-下注
     */
    public static final int BDX_USER_STATUS_GAME_EVENT=2;
    /**
     * 玩家状态-结算
     */
    public static final int BDX_USER_STATUS_SUMMARY=3;
}
