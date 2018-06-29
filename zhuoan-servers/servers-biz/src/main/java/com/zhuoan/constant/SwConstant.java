package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:51 2018/6/15
 * @Modified By:
 **/
public class SwConstant {
    /**
     * 車
     */
    public static final int TREASURE_BLACK_ROOK = 1;
    /**
     * 馬
     */
    public static final int TREASURE_BLACK_KNIGHT = 2;
    /**
     * 砲
     */
    public static final int TREASURE_BLACK_CANNON = 3;
    /**
     * 象
     */
    public static final int TREASURE_BLACK_ELEPHANT = 4;
    /**
     * 士
     */
    public static final int TREASURE_BLACK_MANDARIN = 5;
    /**
     * 將
     */
    public static final int TREASURE_BLACK_KING = 6;
    /**
     * 俥
     */
    public static final int TREASURE_RED_ROOK = 7;
    /**
     * 傌
     */
    public static final int TREASURE_RED_KNIGHT = 8;
    /**
     * 炮
     */
    public static final int TREASURE_RED_CANNON = 9;
    /**
     * 相
     */
    public static final int TREASURE_RED_ELEPHANT = 10;
    /**
     * 仕
     */
    public static final int TREASURE_RED_MANDARIN = 11;
    /**
     * 帥
     */
    public static final int TREASURE_RED_KING = 12;

    /**
     * 游戏状态-初始
     */
    public static final int SW_GAME_STATUS_INIT = 0;
    /**
     * 游戏状态-准备
     */
    public static final int SW_GAME_STATUS_READY = 1;
    /**
     * 游戏状态-下注
     */
    public static final int SW_GAME_STATUS_BET = 2;
    /**
     * 游戏状态-展示押宝
     */
    public static final int SW_GAME_STATUS_SHOW = 3;
    /**
     * 游戏状态-结算
     */
    public static final int SW_GAME_STATUS_SUMMARY = 4;
    /**
     * 游戏状态-上庄
     */
    public static final int SW_GAME_STATUS_CHOICE_BANKER = 5;
    /**
     * 游戏状态-押宝
     */
    public static final int SW_GAME_STATUS_HIDE_TREASURE = 6;

    /**
     * 结算结果-胜利
     */
    public static final int SUMMARY_RESULT_WIN = 1;
    /**
     * 结算结果-未参与
     */
    public static final int SUMMARY_RESULT_NO_IN = 0;
    /**
     * 结算结果-失败
     */
    public static final int SUMMARY_RESULT_LOSE = -1;

    /**
     * 游戏事件-开始游戏
     */
    public static final int SW_GAME_EVENT_START_GAME = 1;
    /**
     * 游戏事件-下注
     */
    public static final int SW_GAME_EVENT_BET = 2;
    /**
     * 游戏事件-上庄
     */
    public static final int SW_GAME_EVENT_BE_BANKER = 3;
    /**
     * 游戏事件-撤销下注
     */
    public static final int SW_GAME_EVENT_UNDO = 4;
    /**
     * 游戏事件-退出房间
     */
    public static final int SW_GAME_EVENT_EXIT_ROOM = 5;
    /**
     * 游戏事件-换坐
     */
    public static final int SW_GAME_EVENT_CHANGE_SEAT = 6;
    /**
     * 游戏事件-重连
     */
    public static final int SW_GAME_EVENT_RECONNECT = 7;
    /**
     * 游戏事件-获取走势图
     */
    public static final int SW_GAME_EVENT_GET_HISTORY = 8;
    /**
     * 游戏事件-获取玩家列表
     */
    public static final int SW_GAME_EVENT_GET_ALL_USER = 9;
    /**
     * 游戏事件-开始游戏押宝
     */
    public static final int SW_GAME_EVENT_HIDE_TREASURE = 10;
    /**
     * 游戏事件-开始游戏押宝
     */
    public static final int SW_GAME_EVENT_GET_UNDO_INFO = 11;

    /**
     * 最小座位号
     */
    public static final int SW_MIN_SEAT_NUM = 0;
    /**
     * 最大座位号
     */
    public static final int SW_MAX_SEAT_NUM = 18;
    /**
     * 走势图长度
     */
    public static final int SW_HISTORY_TREASURE_SIZE = 30;
    /**
     * 1赔10
     */
    public static final int SW_TYPE_TEN = 0;
    /**
     * 1赔10.5
     */
    public static final int SW_TYPE_TEN_POINT_FIVE = 1;
    /**
     * 1赔11
     */
    public static final int SW_TYPE_TEN_ELEVEN = 2;
    /**
     * 押宝倒计时
     */
    public static final int SW_TIME_HIDE_TREASURE = 60;
    /**
     * 下注倒计时
     */
    public static final int SW_TIME_BET = 70;
    /**
     * 展示押宝倒计时
     */
    public static final int SW_TIME_SHOW = 10;
    /**
     * 结算动画倒计时
     */
    public static final int SW_TIME_SUMMARY_ANIMATION = 4;
    /**
     * 玩家列表每页条数
     */
    public static final int SW_USER_SIZE_PER_PAGE = 30;

    public static final String SW_DATA_KEY_TREASURE = "treasure";
    public static final String SW_DATA_KEY_PLACE = "place";
    public static final String SW_DATA_KEY_VALUE = "value";
    public static final String SW_DATA_KEY_INDEX = "index";
}
