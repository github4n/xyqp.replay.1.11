package com.zhuoan.biz.service.majiang;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;

public interface MaJiangBiz {


    // 获取玩家信息
    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return
     */
    public JSONObject getUserInfoByID(long id);


    /**
     * 根据游戏ID获取游戏信息
     * @param id 游戏ID
     * @return
     */
    public JSONObject getGameInfoByID(long id);

    /**
     * 根据玩家账号获取用户信息
     * @param uuid
     * @return
     */
    public JSONObject getUserInfoByAccount(String uuid);

    /**
     * 检查uuid是否合法
     * @param userID
     * @param uuid
     * @return
     */
    public JSONObject checkUUID(long userID,String uuid);

    /**
     * 检查uuid是否合法
     * @param account
     * @param uuid
     * @return
     */
    public JSONObject checkUUID(String account,String uuid);

    // 获取房间信息
    /**
     * 根据房间号获取房间信息
     * @param roomNo
     * @return
     */
    public JSONObject getRoomInfoByRno(String roomNo);

    // 更新房间信息
    /**
     * 根据房间id更新房间信息(键名需和表中字段名相同)
     * @param roominfo
     * @return
     */
    public int updateRoomInfoByRid(JSONObject roominfo);

    // 保存游戏记录
    /**
     * 保存游戏记录 (如果id<0或json中不包含id则新增一条记录,否则根据id更新记录)
     * @param gamelog
     * @return
     */
    public int addOrUpdateGameLog(JSONObject gamelog);

    /**
     * 根据room_id和game_index获取gamelog的id(未找到则返回-1)
     * @param room_id
     * @param game_index
     * @return
     */
    public long getGameLogId(long room_id,int game_index);

    // 保存玩家战斗记录
    /**
     * 保存玩家战斗记录
     * @param usergamelog
     * @return
     */
    public int addUserGameLog(JSONObject usergamelog);

    /**
     * 根据游戏ID 获取对应设置
     * @param gameID
     * @param optkey
     * @return
     */
    public JSONObject getRoomInfoSeting(int gameID, String optkey);

    /**
     * 金币场玩家抽水
     * @param userIds 玩家id集合：[1,2]
     * @param roomNo
     * @param gid
     * @param fee 服务费
     * @param type 1 房卡  2 金币  3元宝
     * @return
     */
    public boolean dealGoldRoomFee(JSONArray userIds, String roomNo, int gid, double fee, String type);


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
    public JSONObject getRoomInfoByRno1(String roomNo);

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
     * @return
     */
    public List<String> getRobotList(int count);

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
     * 更新战绩（房卡场解散）
     * @param @param roomNo
     * @param @param gid
     * @param @param jsonArray
     * @return void
     * @throws
     * @date 2018年2月10日
     */
    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray);

    /**
     * 更新用户 余额
     *
     * @param jsonArray [{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}]
     * @param types 更新类型  元宝:yuanbao  金币：coins   房卡：roomcard  积分：score
     */
    public void updateUser(JSONArray jsonArray,String types);


    /**
     * 插入分润表数据
     *
     * @param obj {"user":[{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}],"gid":x,"roomNo":xxx,"type":xxxx}
     */
    public void insertUserdeduction(JSONObject obj);


    /**
     * 获取玩家战绩
     * @param @param account
     * @param @param gid
     * @param @param num
     * @param @return
     * @return JSONArray
     * @throws
     * @date 2018年3月19日
     */
    public JSONArray getUserGameLogList(String account, int gid, int num,String createTime);

    /**
     * 获取待更新到缓存的元宝
     * @param @return
     * @return JSONArray
     * @throws
     * @date 2018年3月28日
     */
    public JSONArray getYbUpdate();

    /**
     * 更新状态
     * @param @param id
     * @return void
     * @throws
     * @date 2018年3月28日
     */
    public void deleteYbStatus(long id);

    /**
     * 获取工会名称
     * @param @param id
     * @param @return
     * @return JSONObject
     * @throws
     * @date 2018年4月10日
     */
    public JSONObject getGHName(long id);
}
