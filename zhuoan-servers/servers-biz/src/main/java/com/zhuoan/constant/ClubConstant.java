package com.zhuoan.constant;

/**
 * ClubConstant
 *
 * @author wqm
 * @Date Created in 13:41 2018/8/22
 **/
public class ClubConstant {
    /**
     * 加入俱乐部
     */
    public static final int CLUB_EVENT_JOIN_CLUB = 1;
    /**
     * 获取玩家俱乐部列表
     */
    public static final int CLUB_EVENT_GET_MY_CLUB_LIST = 2;
    /**
     * 获取俱乐部成员
     */
    public static final int CLUB_EVENT_GET_CLUB_MEMBERS = 3;
    /**
     * 获取俱乐部设置
     */
    public static final int CLUB_EVENT_GET_CLUB_SETTING = 4;
    /**
     * 更改俱乐部设置
     */
    public static final int CLUB_EVENT_CHANGE_CLUB_SETTING = 5;
    /**
     * 退出俱乐部
     */
    public static final int CLUB_EVENT_EXIT_CLUB = 6;
    /**
     * 置顶/取消置顶
     */
    public static final int CLUB_EVENT_TO_TOP = 7;
    /**
     * 刷新俱乐部信息
     */
    public static final int CLUB_EVENT_REFRESH_CLUB_INFO = 8;
    /**
     * 俱乐部快速加入
     */
    public static final int CLUB_EVENT_QUICK_JOIN_CLUB_ROOM = 9;


    public static final String DATA_KEY_CLUB_CODE = "clubCode";
    public static final String DATA_KEY_PLATFORM = "platform";
}
