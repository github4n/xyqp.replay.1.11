package com.zhuoan.constant;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:38 2018/6/26
 * @Modified By:
 **/
public class DdzConstant {
    /**
     * 初始手牌数
     */
    public static final int DDZ_INIT_CARD_NUMBER = 17;
    /**
     * 玩家数
     */
    public static final int DDZ_PLAYER_NUMBER = 3;
    /**
     * 牌点数 A-K
     */
    public static final int DDZ_CARD_NUM_ONE = 1;
    public static final int DDZ_CARD_NUM_TWO = 2;
    public static final int DDZ_CARD_NUM_THREE = 3;
    public static final int DDZ_CARD_NUM_FOUR = 4;
    public static final int DDZ_CARD_NUM_FIVE = 5;
    public static final int DDZ_CARD_NUM_SIX = 6;
    public static final int DDZ_CARD_NUM_SEVEN = 7;
    public static final int DDZ_CARD_NUM_EIGHT = 8;
    public static final int DDZ_CARD_NUM_NINE = 9;
    public static final int DDZ_CARD_NUM_TEN = 10;
    public static final int DDZ_CARD_NUM_ELEVEN = 11;
    public static final int DDZ_CARD_NUM_TWELVE = 12;
    public static final int DDZ_CARD_NUM_THIRTEEN = 13;
    /**
     * 花色 黑桃、红桃、梅花、方片、王
     */
    public static final int DDZ_CARD_COLOR_SPADES = 1;
    public static final int DDZ_CARD_COLOR_HEARTS = 2;
    public static final int DDZ_CARD_COLOR_PLUM  = 3;
    public static final int DDZ_CARD_COLOR_PIECE = 4;
    public static final int DDZ_CARD_COLOR_JOKER = 5;
    /**
     * 无法出牌
     */
    public static final int DDZ_CARD_TYPE_ILLEGAL = 0;
    /**
     * 单牌
     */
    public static final int DDZ_CARD_TYPE_SINGLE = 1;
    /**
     * 对子
     */
    public static final int DDZ_CARD_TYPE_PAIRS = 2;
    /**
     * 三不带
     */
    public static final int DDZ_CARD_TYPE_THREE = 3;
    /**
     * 三带一
     */
    public static final int DDZ_CARD_TYPE_THREE_WITH_SINGLE = 4;
    /**
     * 三带二
     */
    public static final int DDZ_CARD_TYPE_THREE_WITH_PARIS = 5;
    /**
     * 炸弹
     */
    public static final int DDZ_CARD_TYPE_BOMB = 6;
    /**
     * 四带二
     */
    public static final int DDZ_CARD_TYPE_BOMB_WITH_SINGLE = 7;
    /**
     * 四带两对
     */
    public static final int DDZ_CARD_TYPE_BOMB_WITH_PARIS = 8;
    /**
     * 顺子
     */
    public static final int DDZ_CARD_TYPE_STRAIGHT = 9;
    /**
     * 连对
     */
    public static final int DDZ_CARD_TYPE_DOUBLE_STRAIGHT = 10;
    /**
     * 飞机
     */
    public static final int DDZ_CARD_TYPE_PLANE = 11;
    /**
     * 飞机带单
     */
    public static final int DDZ_CARD_TYPE_PLANE_WITH_SINGLE = 12;
    /**
     * 飞机带对子
     */
    public static final int DDZ_CARD_TYPE_PLANE_WITH_DOUBLE = 13;


    public static final int DDZ_GAME_STATUS_INIT = 0;
    public static final int DDZ_GAME_STATUS_READY = 1;
    public static final int DDZ_GAME_STATUS_CHOICE_LANDLORD = 2;
    public static final int DDZ_GAME_STATUS_GAME_IN = 3;
    public static final int DDZ_GAME_STATUS_SUMMARY = 4;
    public static final int DDZ_GAME_STATUS_FINAL_SUMMARY = 5;

    public static final int DDZ_USER_STATUS_INIT = 0;
    public static final int DDZ_USER_STATUS_READY = 1;

    public static final int DDZ_BE_LANDLORD_TYPE_CALL = 1;
    public static final int DDZ_BE_LANDLORD_TYPE_ROB = 2;

    public static final int DDZ_GAME_EVENT_TYPE_YES = 1;
    public static final int DDZ_GAME_EVENT_TYPE_NO = 2;

    public static final int DDZ_GAME_EVENT_RESULT_YES = 1;
    public static final int DDZ_GAME_EVENT_RESULT_NO = -1;
    public static final int DDZ_GAME_EVENT_RESULT_ILLEGAL = 0;

    public static final int DDZ_GAME_EVENT_READY = 1;
    public static final int DDZ_GAME_EVENT_CALL_AND_ROB = 2;
    public static final int DDZ_GAME_EVENT_GAME_IN = 3;
    public static final int DDZ_GAME_EVENT_RECONNECT = 4;
    public static final int DDZ_GAME_EVENT_PROMPT = 5;
    public static final int DDZ_GAME_EVENT_CONTINUE = 6;
    public static final int DDZ_GAME_EVENT_EXIT_ROOM = 7;
    public static final int DDZ_GAME_EVENT_CLOSE_ROOM = 8;
    public static final int DDZ_GAME_EVENT_TRUSTEE = 9;
    public static final int DDZ_GAME_EVENT_AUTO_PLAY = 10;

    public static final int DDZ_RECONNECT_NODE_READY = 0;
    public static final int DDZ_RECONNECT_NODE_CALL = 1;
    public static final int DDZ_RECONNECT_NODE_ROB = 2;
    public static final int DDZ_RECONNECT_NODE_IN = 3;
    public static final int DDZ_RECONNECT_NODE_SUMMARY = 4;
    public static final int DDZ_RECONNECT_NODE_FINAL_SUMMARY = 5;


    public static final String DDZ_DATA_KEY_TYPE = "type";
    public static final String DDZ_DATA_KEY_IS_CHOICE = "isChoice";
    public static final String DDZ_DATA_KEY_PAI_LIST = "paiList";
    public static final String DDZ_DATA_KEY_AUTO = "auto";

}
