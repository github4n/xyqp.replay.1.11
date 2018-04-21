package com.zhuoan.biz.model.sss;

import com.zhuoan.biz.model.GameRoom;

/**
 * @author wqm
 * @DESCRIPTION 十三水房间实体类
 * @Date Created in 14:24 2018/4/21
 * @Modified By:
 **/
public class SSSGameRoomNew extends GameRoom{
    /**
     * 最低开始人数
     */
    private int minplayer=2;
    /**
     * 马牌类型
     */
    private int maPaiType;
    /**
     * 加色
     */
    private int color;
    /**
     * 马牌
     */
    private String maPai;
    /**
     * 定庄方式(霸王庄，互比)
     */
    private int bankerType;
    //private ConcurrentMap<String,UserPacket> userPacketMap;//玩家牌局信息
}
