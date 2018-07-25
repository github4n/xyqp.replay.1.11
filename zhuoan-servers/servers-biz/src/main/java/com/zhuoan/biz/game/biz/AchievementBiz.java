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
     * @param account 玩家账号
     * @param achievementScore 成就分数
     * @param gameId 游戏id
     * @return JSONObject
     */
    JSONObject addOrUpdateUserAchievement(String account, int gameId, int achievementScore);
}
