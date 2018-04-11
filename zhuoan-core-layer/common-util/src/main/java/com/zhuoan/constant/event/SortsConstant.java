package com.zhuoan.constant.event;

/**
 * The type Sorts constant.
 */
public class SortsConstant {

    /**
     * The constant ENTER_ROOM.创建房间或加入房间
     */
    public static final int ENTER_ROOM = 1;
    /**
     * The constant GAME_READY.玩家准备
     */
    public static final int GAME_READY = 2;
    /**
     * The constant IN_GAME.游戏中
     */
    public static final int IN_GAME = 3;
    /**
     * The constant CLOSE_ROOM.解散房间
     */
    public static final int CLOSE_ROOM = 4;
    /**
     * The constant EXIT_ROOM.离开房间
     */
    public static final int EXIT_ROOM = 5;
    /**
     * The constant RECONNECT_GAME.断线重连
     */
    public static final int RECONNECT_GAME = 6;
    /**
     * The constant WHETHER_RECONNECT.判断玩家是否需要断线重连
     */
    public static final int WHETHER_RECONNECT = 7;
    /**
     * The constant GAME_SUMMARY.总结算
     */
    public static final int GAME_SUMMARY = 8;
    /**
     * The constant PLAYER_INFO.查询更新用户实时信息
     */
    public static final int PLAYER_INFO = 9;
    /**
     * The constant GAME_START.游戏开始-发牌（没有监听事件，只有推送事件）
     */
    public static final int GAME_START = 10;
    /**
     * The constant GAME_END.游戏结算-结算（没有监听事件，只有推送事件）
     */
    public static final int GAME_END = 11;
    /**
     * The constant READY_TIMEOUT.超时准备
     */
    public static final int READY_TIMEOUT = 12;
    /**
     * The constant PEIPAI_TIMEOUT.超时配牌
     */
    public static final int PEIPAI_TIMEOUT = 13;
}
