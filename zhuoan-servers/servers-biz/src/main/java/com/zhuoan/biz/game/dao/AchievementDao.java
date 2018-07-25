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
     * @return JSONArray
     */
    JSONArray getAchievementInfoByGameId(int gameId);

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
     * @param gameId 游戏id
     * @return JSONArray
     */
    JSONObject getUserAchievementByAccountAndGameId(String account, int gameId);

    /**
     * 添加用户成就信息
     *
     * @param userAchievement 用户成就信息
     */
    void addOrUpdateUserAchievement(JSONObject userAchievement);
}
