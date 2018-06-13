package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION 骨牌牌九常量类
 * @Date Created in 11:27 2018/6/8
 * @Modified By:
 **/
public class GPPJConstant {
    /**
     * 庄家类型-房主坐庄
     */
    public static final int BANKER_TYPE_OWNER = 0;
    /**
     * 庄家类型-看牌抢庄
     */
    public static final int BANKER_TYPE_LOOK = 1;
    /**
     * 庄家类型-互比
     */
    public static final int BANKER_TYPE_COMPARE = 2;
    /**
     * 游戏状态-初始
     */
    public static final int GP_PJ_GAME_STATUS_INIT = 0;
    /**
     * 游戏状态-准备
     */
    public static final int GP_PJ_GAME_STATUS_READY = 1;
    /**
     * 游戏状态-切牌
     */
    public static final int GP_PJ_GAME_STATUS_CUT = 2;
    /**
     * 游戏状态-抢庄
     */
    public static final int GP_PJ_GAME_STATUS_QZ = 3;
    /**
     * 游戏状态-下注
     */
    public static final int GP_PJ_GAME_STATUS_XZ = 4;
    /**
     * 游戏状态-咪牌
     */
    public static final int GP_PJ_GAME_STATUS_SHOW = 5;
    /**
     * 游戏状态-结算
     */
    public static final int GP_PJ_GAME_STATUS_SUMMARY = 6;
    /**
     * 游戏状态-总结算
     */
    public static final int GP_PJ_GAME_STATUS_FINAL_SUMMARY = 7;
    /**
     * 玩家状态-初始
     */
    public static final int GP_PJ_USER_STATUS_INIT = 0;
    /**
     * 玩家状态-准备
     */
    public static final int GP_PJ_USER_STATUS_READY = 1;
    /**
     * 玩家状态-切牌
     */
    public static final int GP_PJ_USER_STATUS_CUT = 2;
    /**
     * 玩家状态-抢庄
     */
    public static final int GP_PJ_USER_STATUS_QZ = 3;
    /**
     * 玩家状态-下注
     */
    public static final int GP_PJ_USER_STATUS_XZ = 4;
    /**
     * 玩家状态-咪牌
     */
    public static final int GP_PJ_USER_STATUS_SHOW = 5;
    /**
     * 游戏倒计时-初始
     */
    public static final int GP_PJ_TIMER_INIT = 0;
    /**
     * 按钮类别-不显示
     */
    public static final int GP_PJ_BTN_TYPE_NONE = 0;
    /**
     * 按钮类别-准备
     */
    public static final int GP_PJ_BTN_TYPE_READY = 1;
    /**
     * 按钮类别-开始
     */
    public static final int GP_PJ_BTN_TYPE_START = 2;
    /**
     * 按钮类别-查看总成绩
     */
    public static final int GP_PJ_BTN_TYPE_SHOW = 3;
    /**
     * 最低开始人数
     */
    public static final int GP_PJ_MIN_START_COUNT = 2;
    /**
     * 游戏事件-准备
     */
    public static final int GP_PJ_GAME_EVENT_READY = 1;
    /**
     * 游戏事件-开始
     */
    public static final int GP_PJ_GAME_EVENT_START = 2;
    /**
     * 游戏事件-切牌
     */
    public static final int GP_PJ_GAME_EVENT_CUT = 3;
    /**
     * 游戏事件-抢庄
     */
    public static final int GP_PJ_GAME_EVENT_QZ = 4;
    /**
     * 游戏事件-下注
     */
    public static final int GP_PJ_GAME_EVENT_XZ = 5;
    /**
     * 游戏事件-咪牌
     */
    public static final int GP_PJ_GAME_EVENT_SHOW = 6;
    /**
     * 游戏事件-重连
     */
    public static final int GP_PJ_GAME_EVENT_RECONNECT = 7;
    /**
     * 游戏事件-退出
     */
    public static final int GP_PJ_GAME_EVENT_EXIT = 8;
    /**
     * 游戏事件-解散
     */
    public static final int GP_PJ_GAME_EVENT_CLOSE_ROOM = 9;
    /**
     * 比牌结果-赢
     */
    public static final int COMPARE_RESULT_WIN = 1;
    /**
     * 比牌结果-相等
     */
    public static final int COMPARE_RESULT_EQUALS = 0;
    /**
     * 比牌结果-输
     */
    public static final int COMPARE_RESULT_LOSE = -1;
    /**
     * 加倍类型-至尊*4 对子*2
     */
    public static final int MULTIPLE_TYPE_ZZ_DOUBLE = 0;
    /**
     * 开始游戏类别-人数未满未确认
     */
    public static final int START_GAME_TYPE_UNSURE = 1;
    /**
     * 开始游戏类别-人数未满已确认
     */
    public static final int START_GAME_TYPE_SURE = 2;
    /**
     * 休眠类型-无休眠
     */
    public static final int SLEEP_TYPE_NONE = 0;
    /**
     * 休眠类型-开始游戏动画
     */
    public static final int SLEEP_TYPE_START_GAME = 1;
    /**
     * 倒计时-切牌
     */
    public static final int GP_PJ_TIME_CUT = 10;
    /**
     * 倒计时-抢庄
     */
    public static final int GP_PJ_TIME_QZ = 10;
    /**
     * 倒计时-下注
     */
    public static final int GP_PJ_TIME_XZ = 10;
    /**
     * 倒计时-咪牌
     */
    public static final int GP_PJ_TIME_SHOW = 10;
    /**
     * 休眠时间-开始游戏
     */
    public static final int SLEEP_TIME_START_GAME = 2500;

    public static final String DATA_KEY_QZ_TIMES = "qzTimes";
    public static final String DATA_KEY_XZ_TIMES = "xzTimes";
    public static final String DATA_KEY_CUT_PLACE = "cutPlace";
    public static final String DATA_KEY_TYPE = "type";
}
