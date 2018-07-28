package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.AchievementBiz;
import com.zhuoan.biz.game.dao.AchievementDao;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 10:26 2018/7/20
 * @Modified By:
 **/
@Service
public class AchievementBizImpl implements AchievementBiz {

    @Resource
    private RedisService redisService;

    @Resource
    private AchievementDao achievementDao;

    @Resource
    private GameDao gameDao;

    @Override
    public JSONArray getAchievementInfoByGameId(int gameId) {
        StringBuffer key = new StringBuffer("achievement_info_");
        key.append(gameId);
        JSONArray achievementInfo;
        try {
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                achievementInfo = JSONArray.fromObject(object);
            } else {
                achievementInfo = achievementDao.getAchievementInfoByGameId(gameId);
                redisService.insertKey(String.valueOf(key), String.valueOf(achievementInfo), null);
            }
        } catch (Exception e) {
            achievementInfo = achievementDao.getAchievementInfoByGameId(gameId);
        }
        return achievementInfo;
    }

    @Override
    public JSONArray getUserAchievementByAccount(String account) {
        return achievementDao.getUserAchievementByAccount(account);
    }

    @Override
    public JSONObject getUserAchievementByAccountAndGameId(String account, int gameId) {
        return achievementDao.getUserAchievementByAccountAndGameId(account, gameId);
    }

    @Override
    public JSONObject addOrUpdateUserAchievement(String account, int gameId, int achievementScore) {
        JSONObject levelUp = new JSONObject();
        JSONArray achievementInfo = getAchievementInfoByGameId(gameId);
        // 是否存在记录
        JSONObject userAchievement = achievementDao.getUserAchievementByAccountAndGameId(account, gameId);
        JSONObject obj = new JSONObject();
        if (!Dto.isObjNull(userAchievement)) {
            obj.put("id", userAchievement.getLong("id"));
            achievementScore += userAchievement.getLong("achievement_score");
        }
        JSONObject userInfo = gameDao.getUserByAccount(account);
        if (!Dto.isObjNull(userInfo)) {
            if (userInfo.containsKey("name")) {
                obj.put("user_name", userInfo.getString("name"));
            }
            if (userInfo.containsKey("headimg")) {
                obj.put("user_img", userInfo.getString("headimg"));
            }
            if (userInfo.containsKey("sign")) {
                obj.put("user_sign", userInfo.getString("sign"));
            }
        }
        obj.put("user_account", account);
        obj.put("game_id", gameId);
        obj.put("achievement_score", achievementScore);
        // 取得对应的成就信息
        for (int i = 0; i < achievementInfo.size(); i++) {
            JSONObject achievement = achievementInfo.getJSONObject(i);
            if (achievement.getInt("min_score") == achievementScore) {
                obj.put("achievement_id", achievement.getLong("id"));
                obj.put("achievement_name", achievement.getString("achievement_name"));
                levelUp = achievement;
                break;
            }
        }
        achievementDao.addOrUpdateUserAchievement(obj);
        return levelUp;
    }

    @Override
    public JSONArray getAchievementRank(int limit, int gameId) {
        return achievementDao.getAchievementRank(limit, gameId);
    }
}
