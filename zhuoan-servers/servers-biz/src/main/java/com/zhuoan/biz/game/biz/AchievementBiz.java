package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION 成就
 * @Date Created in 10:11 2018/7/20
 * @Modified By:
 **/
public interface AchievementBiz {

    /**
     * 获取成就信息
     *
     * @param gameId   游戏id
     * @param platform 平台标识
     * @return JSONArray
     */
    JSONArray getAchievementInfoByGameId(int gameId, String platform);

    /**
     * 获取成就信息
     *
     * @param id id
     * @return JSONObject
     */
    JSONObject getAchievementInfoById(long id);

    /**
     * 获取用户成就信息
     *
     * @param account 玩家账号
     * @return JSONArray
     */
    JSONArray getUserAchievementByAccount(String account);

    /**
     * 获取用户成就信息
     *
     * @param account 玩家账号
     * @param gameId  游戏id
     * @return JSONArray
     */
    JSONObject getUserAchievementByAccountAndGameId(String account, int gameId);

    /**
     * 添加用户成就信息
     *
     * @param account          玩家账号
     * @param achievementScore 成就分数
     * @param gameId           游戏id
     * @return JSONObject
     */
    JSONObject addOrUpdateUserAchievement(String account, int gameId, int achievementScore);

    /**
     * 获取成就排行榜
     *
     * @param limit  获取条数
     * @param gameId 游戏id
     * @return JSONArray
     */
    JSONArray getAchievementRank(int limit, int gameId);

    /**
     * 更新用户成就信息
     *
     * @param userAchievement          奖励信息
     */
    void updateUserAchievement(JSONObject userAchievement);
}
