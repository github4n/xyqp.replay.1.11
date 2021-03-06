package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 10:20 2018/7/20
 * @Modified By:
 **/
public interface AchievementDao {

    /**
     * 获取成就信息
     *
     * @param gameId 游戏id
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
     * @param userAchievement 用户成就信息
     */
    void addOrUpdateUserAchievement(JSONObject userAchievement);

    /**
     * 获取成就排行榜
     *
     * @param limit  获取条数
     * @param gameId 游戏id
     * @return JSONArray
     */
    JSONArray getAchievementRank(int limit, int gameId);
}
