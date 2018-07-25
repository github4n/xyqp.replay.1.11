package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.AchievementDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:07 2018/7/20
 * @Modified By:
 **/
@Component
public class AchievementDaoImpl implements AchievementDao{
    @Override
    public JSONArray getAchievementInfoByGameId(int gameId) {
        String sql = "select id,game_id,achievement_name,achievement_level,min_score,reward,reward_type from " +
            "za_achievement_info where game_id=? order by achievement_level";
        return DBUtil.getObjectListBySQL(sql, new Object[]{gameId});
    }

    @Override
    public JSONArray getUserAchievementByAccount(String account) {
        String sql = "select id,user_account,game_id,achievement_score,reward_level,achievement_id," +
            "achievement_name from za_user_achievement where user_account=?";
        return DBUtil.getObjectListBySQL(sql,new Object[]{account});
    }

    @Override
    public JSONObject getUserAchievementByAccountAndGameId(String account, int gameId) {
        String sql = "select id,user_account,game_id,achievement_score,reward_level,achievement_id," +
            "achievement_name from za_user_achievement where user_account=? and game_id=?";
        return DBUtil.getObjectBySQL(sql,new Object[]{account,gameId});
    }

    @Override
    public void addOrUpdateUserAchievement(JSONObject userAchievement) {
        DBJsonUtil.saveOrUpdate(userAchievement,"za_user_achievement");
    }
}
