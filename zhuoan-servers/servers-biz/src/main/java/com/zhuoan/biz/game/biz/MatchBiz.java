package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:15 2018/7/12
 * @Modified By:
 **/
public interface MatchBiz {

    /**
     * 获取比赛场配置
     *
     * @param type 1-满人开赛  2-定时开赛
     * @return 比赛场配置
     */
    JSONArray getMatchSettingByType(int type);

    /**
     * 获取比赛场信息
     *
     * @param matchId 场次id
     * @param gameId  游戏id
     * @return 比赛场信息
     */
    JSONObject getMatchSettingById(long matchId, long gameId);

    /**
     * 获取当前未开赛的场次
     *
     * @param matchId 场次id
     * @param isFull  是否满人
     * @param isEnd   是否结束
     * @return 当前未开赛的场次
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
     * @param matchNum
     * @param isFull
     */
    void updateMatchInfoByMatchNum(String matchNum, int isFull);

    /**
     * 获取机器人
     *
     * @param count 人数
     * @return 列表
     */
    JSONArray getRobotList(int count);

    /**
     * 更新玩家金币
     *
     * @param account
     * @param coins
     * @param score
     */
    void updateUserCoinsAndScoreByAccount(String account, int coins, int score);

    /**
     * 获取玩家获奖记录
     *
     * @param account
     * @param gameId
     * @return
     */
    JSONObject getUserWinningRecord(String account,int gameId);

    /**
     * 添加用户获奖记录
     * @param winningRecord
     */
    void addOrUpdateUserWinningRecord(JSONObject winningRecord);

}
