package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author huaping.li
 * @Description: 游戏房间操作接口
 * @date 2018-04-17 15:08
 */
public interface RoomBiz {

    /**
     * 插入房间
     * @param obj
     */
    public void insertGameRoom(JSONObject obj);

    /**
     * 根据游戏ID获取游戏信息
     * @param id 游戏ID
     * @return
     */
    public JSONObject getGameInfoByID(long id);

    /**
     * 根据房间号获取房间信息
     * @param roomNo
     * @return
     */
    public JSONObject getRoomInfoByRno(String roomNo);

    /**
     * 根据房间号解散房间
     * @param room
     * @return
     */
    public boolean updateGameRoom(JSONObject room);

    /**
     * 根据房间号关闭房间
     * @param roomNo
     * @return
     */
    public boolean closeGameRoom(String roomNo);


    /**
     * 根据游戏ID 获取对应设置
     * @param gameID
     * @param optkey
     * @return
     */
    public JSONObject getRoomInfoSeting(int gameID, String optkey);


    /**
     * 游戏开始后，禁止玩家进入房间
     * @param roomNo
     * @return
     */
    public boolean stopJoin(String roomNo);


    /**
     * 根据房间号 stutas《0 房间信息
     * @param roomNo
     * @return
     */
    public JSONObject getRoomInfoByRnoNotUse(String roomNo);

    /**
     * 删除房间内玩家
     * @param room
     * @param userId
     * @return
     */
    public boolean delGameRoomUserByUid(JSONObject room, long userId);


    /**
     * 获取游戏设置
     * @return
     */
    public JSONObject getGameSetting();


    /**
     * 获取机器人列表
     * @param count
     * @param minScore
     * @param maxScore
     * @return
     */
    public JSONArray getRobotArray(int count,double minScore, double maxScore);

    /**
     * 金币场玩家抽水
     * @param userIds 玩家id集合：[1,2]
     * @param roomNo
     * @param gid
     * @param fee 服务费
     * @param type roomcard:房卡  coins:金币  yuanbao:元宝
     * @return
     */
    public boolean pump(JSONArray userIds, String roomNo, int gid, double fee, String type);

    /**
     * 扣房卡
     * @param @param roomNo
     * @return void
     * @throws
     * @date 2018年2月7日
     */
    public void settlementRoomNo(String roomNo);

    /**
     * 更改机器人状态
     * @param robotAccount
     * @param status
     */
    public void updateRobotStatus(String robotAccount,int status);

    /**
     * 增加游戏下标
     *
     * @param roomNo roomNo
     */
    void increaseRoomIndexByRoomNo(String roomNo);

}
