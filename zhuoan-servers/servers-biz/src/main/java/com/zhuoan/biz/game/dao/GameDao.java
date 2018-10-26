package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * @author huaping.li
 * @Description: 底层数据操作接口
 * @date 2018-04-17 15:07
 */
public interface GameDao {

    public void insertGameRoom(JSONObject obj);

    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return
     */
    public JSONObject getUserByID(long id);

    /**
     * 根据玩家账号获取用户信息
     * @param account
     * @return
     */
    public JSONObject getUserByAccount(String account);



    /**
     * 更新用户 余额
     *
     * @param data [{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}]
     * @param types 更新类型  元宝:yuanbao  金币：coins   房卡：roomcard  积分：score
     */
    public boolean updateUserBalance(JSONArray data, String types);


    /**
     * 插入分润表数据
     *
     * @param obj {"user":[{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}],"gid":x,"roomNo":xxx,"type":xxxx}
     */
    public void insertUserdeduction(JSONObject obj);


    /**
     * 获取待更新到缓存的元宝
     * @param @return
     * @return JSONArray
     * @throws
     * @date 2018年3月28日
     */
    public JSONArray getYbUpdateLog();

    /**
     * 更新状态
     * @param @param id
     * @return void
     * @throws
     * @date 2018年3月28日
     */
    public void delYbUpdateLog(long id);

    /**
     * 获取工会信息
     * @param userInfo
     * @return
     */
    public JSONObject getGongHui(JSONObject userInfo);


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
     * 根据房间id更新房间信息(键名需和表中字段名相同)
     * @param roominfo
     * @return
     */
    public int updateRoomInfoByRid(JSONObject roominfo);


    /**
     * 根据游戏ID 获取对应设置
     * @param gameID
     * @param optkey
     * @return
     */
    public JSONObject getRoomInfoSeting(int gameID, String optkey);


    /**
     * 将房间内userid由初始值置为-1
     * @param roomNo
     * @return
     */
    public boolean updateGameRoomUserId(String roomNo);


    /**
     * 根据房间号获取无用的房间信息（结束或解散 status<0）
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
     * 更新玩家房卡
     * @param roomCard
     * @param userId
     */
    public void updateUserRoomCard(int roomCard, long userId);

    /**
     * 保存扣房卡记录
     * @param data
     */
    public void deductionRoomCardLog(Object[] data);

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
    public long getGameLogId(long room_id, int game_index);

    /**
     * 保存玩家战斗记录
     * @param usergamelog
     * @return
     */
    public int addUserGameLog(JSONObject usergamelog);


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
     * 获取玩家战绩
     * @param @param account
     * @param @param gid
     * @param @param num
     * @param @return
     * @return JSONArray
     * @throws
     * @date 2018年3月19日
     */
    public JSONArray getUserGameLogList(String account, int gid, int num, String createTime);


    /**
     * 代开房间重开房间
     * @param roomNo
     * @return
     */
    public boolean reDaikaiGameRoom(String roomNo);

    /**
     * 获取房间设置
     * @param gid
     * @param platform
     * @return
     */
    public JSONArray getRoomSetting(int gid, String platform, int flag);

    /**
     * 获取管理员用户信息
     * @param adminCode
     * @param adminPass
     * @param memo
     * @return
     */
    public JSONObject getSysUser(String adminCode, String adminPass, String memo);


    /**
     * 根据用户id获取玩家战绩
     * @param userId
     * @param gameId
     * @param roomType
     * @return
     */
    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType);

    /**
     * 获取系统配置
     * @return
     */
    public JSONObject getSysBaseSet();

    /**
     * 获取游戏配置
     * @return
     */
    public JSONObject getAPPGameSetting();

    /**
     * 获取道具扣除记录
     * @param userId
     * @param doType
     * @param gid
     * @param roomid
     * @param roomNo
     * @return
     */
    public JSONArray getAppObjRec(Long userId, int doType, String gid,String roomid, String roomNo);

    /**
     * 添加道具扣除记录
     * @param object
     */
    public void addAppObjRec(JSONObject object);

    /**
     * 更新玩家洗牌次数
     * @param account
     */
    public void updateUserPump(String account, String type, double sum);

    /**
     * 通过平台号获取滚动公告
     * @param platform
     * @return
     */
    public JSONObject getNoticeByPlatform(String platform);

    /**
     * 获取金币场设置
     * @param obj
     * @return
     */
    public JSONArray getGoldSetting(JSONObject obj);

    /**
     * 获取用户签到信息
     * @return
     */
    public JSONObject getUserSignInfo(String platform,long userId);

    /**
     * 用户签到记录
     * @param obj
     * @return
     */
    public int addOrUpdateUserSign(JSONObject obj);

    /**
     * 根据平台号获取签到奖励
     * @param platform
     */
    public JSONObject getSignRewardInfoByPlatform(String platform);

    /**
     * 更改机器人状态
     * @param robotAccount
     * @param status
     */
    public void updateRobotStatus(String robotAccount,int status);

    /**
     * 获取竞技场信息
     * @return
     */
    public JSONArray getArenaInfo();

    /**
     * 获取用户积分记录
     * @param userId
     * @param type
     * @param startTime
     * @param endTime
     * @return
     */
    public JSONObject getUserCoinsRecById(long userId, int type, String startTime, String endTime);

    /**
     * 增加、更新用户积分记录
     * @param obj
     * @return
     */
    public int addOrUpdateUserCoinsRec(JSONObject obj);

    /**
     * 获取玩家游戏信息
     * @param account
     * @return
     */
    public JSONObject getUserGameInfo(String account);

    /**
     * 更改玩家游戏信息
     * @param obj
     */
    public void addOrUpdateUserGameInfo(JSONObject obj);

    /**
     * 添加券操作记录
     * @param ticketRec
     */
    void addUserTicketRec(JSONObject ticketRec);

    /**
     * 添加福利记录
     * @param obj
     */
    void addUserWelfareRec(JSONObject obj);

    /**
     * 获取用户游戏房间
     * @param userId
     * @param gameId
     * @param roomType
     * @return
     */
    JSONArray getUserGameRoomByRoomType(long userId, int gameId, int roomType);

    /**
     * 获取用户战绩(带房间号)
     * @param userId
     * @param gameId
     * @param roomType
     * @param roomList
     * @return
     */
    JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType, List<String> roomList, String clubCode);

    /**
     * 增加游戏下标
     *
     * @param roomNo roomNo
     */
    void increaseRoomIndexByRoomNo(String roomNo);
}
