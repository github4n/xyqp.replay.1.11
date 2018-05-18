package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:11 2018/4/17
 * @Modified By:
 **/
public class NNConstant {

    /**
     * 牛牛游戏事件-准备
     */
    public static final int NN_GAME_EVENT_READY = 1;
    /**
     * 牛牛游戏事件-抢庄
     */
    public static final int NN_GAME_EVENT_QZ = 2;
    /**
     * 牛牛游戏事件-下注
     */
    public static final int NN_GAME_EVENT_XZ = 3;
    /**
     * 牛牛游戏事件-亮牌
     */
    public static final int NN_GAME_EVENT_LP = 4;
    /**
     * 牛牛游戏事件-退出
     */
    public static final int NN_GAME_EVENT_EXIT = 5;
    /**
     * 牛牛游戏事件-重连
     */
    public static final int NN_GAME_EVENT_RECONNECT = 6;
    /**
     * 牛牛游戏事件-解散房间
     */
    public static final int NN_GAME_EVENT_CLOSE_ROOM = 7;
    /**
     * 牛牛游戏事件-上庄
     */
    public static final int NN_GAME_EVENT_BE_BANKER = 8;
    /**
     * 庄家类型-房主坐庄
     */
    public static final int NN_BANKER_TYPE_FZ = 0;
    /**
     * 庄家类型-轮庄
     */
    public static final int NN_BANKER_TYPE_LZ = 1;
    /**
     * 庄家类型-抢庄
     */
    public static final int NN_BANKER_TYPE_QZ = 2;
    /**
     * 庄家类型-明牌抢庄
     */
    public static final int NN_BANKER_TYPE_MP = 3;
    /**
     * 庄家类型-牛牛坐庄
     */
    public static final int NN_BANKER_TYPE_NN = 4;
    /**
     * 庄家类型-通比
     */
    public static final int NN_BANKER_TYPE_TB = 5;
    /**
     * 庄家类型-坐庄模式
     */
    public static final int NN_BANKER_TYPE_ZZ = 6;

    /**
     * 无人抢庄-随机庄家
     */
    public static final int NN_QZ_NO_BANKER_SJ = 1;
    /**
     * 无人抢庄-解散房间
     */
    public static final int NN_QZ_NO_BANKER_JS = -1;
    /**
     * 无人抢庄-重开局
     */
    public static final int NN_QZ_NO_BANKER_CK = 2;

    /**
     * 牛牛游戏状态-初始
     */
    public static final int NN_GAME_STATUS_INIT=0;
    /**
     * 牛牛游戏状态-准备
     */
    public static final int NN_GAME_STATUS_READY=1;
    /**
     * 牛牛游戏状态-抢庄
     */
    public static final int NN_GAME_STATUS_QZ=2;
    /**
     * 牛牛游戏状态-定庄
     */
    public static final int NN_GAME_STATUS_DZ=3;
    /**
     * 牛牛游戏状态-下注
     */
    public static final int NN_GAME_STATUS_XZ=4;
    /**
     * 牛牛游戏状态-亮牌
     */
    public static final int NN_GAME_STATUS_LP=5;
    /**
     * 牛牛游戏状态-结算
     */
    public static final int NN_GAME_STATUS_JS=6;
    /**
     * 牛牛游戏状态-总结算
     */
    public static final int NN_GAME_STATUS_ZJS=7;
    /**
     * 牛牛游戏状态-上庄
     */
    public static final int NN_GAME_STATUS_TO_BE_BANKER=8;

    /**
     * 玩家状态-初始
     */
    public static final int NN_USER_STATUS_INIT=0;
    /**
     * 玩家状态-准备
     */
    public static final int NN_USER_STATUS_READY=1;
    /**
     * 玩家状态-抢庄
     */
    public static final int NN_USER_STATUS_QZ=2;
    /**
     * 玩家状态-定庄
     */
    public static final int NN_USER_STATUS_DZ=3;
    /**
     * 玩家状态-下注
     */
    public static final int NN_USER_STATUS_XZ=4;
    /**
     * 玩家状态-亮牌
     */
    public static final int NN_USER_STATUS_LP=5;
    /**
     * 玩家状态-结算
     */
    public static final int NN_USER_STATUS_JS=6;
    /**
     * 玩家状态-总结算
     */
    public static final int NN_USER_STATUS_ZJS=7;

    /**
     * 牛牛最少开始人数
     */
    public static final int NN_MIN_START_COUNT = 2;
    /**
     * 牛牛游戏倒计时-初始
     */
    public static final int NN_TIMER_INIT = 0;
    /**
     * 牛牛游戏倒计时-准备
     */
    public static final int NN_TIMER_READY = 10;
    /**
     * 牛牛游戏倒计时-抢庄
     */
    public static final int NN_TIMER_QZ = 10;
    /**
     * 牛牛游戏倒计时-下注
     */
    public static final int NN_TIMER_XZ = 10;
    /**
     * 牛牛游戏倒计时-亮牌
     */
    public static final int NN_TIMER_SHOW = 15;
    /**
     * 抢庄类型-明牌抢庄
     */
    public static final int NN_QZ_TYPE = 1;
    /**
     * 玩法-斗公牛
     */
    public static final int NN_GAME_TYPE = 1;

    public static final String DATA_KEY_VALUE = "value";
    public static final String DATA_KEY_MONEY = "money";


}
