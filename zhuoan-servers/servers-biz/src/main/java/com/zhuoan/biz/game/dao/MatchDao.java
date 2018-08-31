package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:18 2018/7/12
 * @Modified By:
 **/
public interface MatchDao {

    /**
     * 获取比赛场配置
     *
     * @param type       1-满人开赛  2-定时开赛
     * @param createTime 创建时间
     * @return JSONArray
     */
    JSONArray getMatchSettingByType(int type, String createTime);

    /**
     * 获取比赛场信息
     *
     * @param matchId 场次id
     * @param gameId  游戏id
     * @return JSONObject
     */
    JSONObject getMatchSettingById(long matchId, long gameId);

    /**
     * 更新场次信息
     *
     * @param matchSetting    场次信息
     */
    void updateMatchSettingById(JSONObject matchSetting);

    /**
     * 获取当前未开赛的场次
     *
     * @param matchId 场次id
     * @param isFull  是否满人
     * @param isEnd   是否结束
     * @return JSONObject
     */
    JSONObject getMatchInfoByMatchId(long matchId, int isFull, int isEnd);

    /**
     * 更新场次信息
     *
     * @param obj obj
     */
    void addOrUpdateMatchInfo(JSONObject obj);

    /**
     * 更改状态
     *
     * @param matchNum 场次编号
     * @param isFull   是否满人
     */
    void updateMatchInfoByMatchNum(String matchNum, int isFull);

    /**
     * 获取机器人
     *
     * @param count 人数
     * @return JSONArray
     */
    JSONArray getRobotList(int count);

    /**
     * 更新玩家金币
     *
     * @param account  玩家账号
     * @param coins    金币
     * @param score    积分
     * @param roomCard 房卡
     */
    void updateUserCoinsAndScoreByAccount(String account, int coins, int score, int roomCard);

    /**
     * 获取玩家获奖记录
     *
     * @param account 玩家账号
     * @param gameId  游戏id
     * @return JSONObject
     */
    JSONObject getUserWinningRecord(String account, int gameId);

    /**
     * 添加用户获奖记录
     *
     * @param winningRecord 获奖记录
     */
    void addOrUpdateUserWinningRecord(JSONObject winningRecord);

    /**
     * 更新机器人状态
     *
     * @param account 机器人账号
     * @param status  状态
     */
    void updateRobotStatus(String account, int status);

    /**
     * 获取所有未开赛比赛场
     *
     * @return JSONArray
     */
    JSONArray getUnFullMatchInfo();

}
