package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.MatchDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:18 2018/7/12
 * @Modified By:
 **/
@Component
public class MatchDaoImpl implements MatchDao {

    @Override
    public JSONArray getMatchSettingByType(int type, String createTime) {
        String sql = "select id,type,game_id,match_name,per_count,player_count,total_round,is_auto,robot_level,must_full,description,time_interval," +
            "online_num,match_cost,cost_type,reward_info,match_info,rule,promotion,is_use,create_time,platform,memo from za_match_setting where type=?";
        if (!Dto.stringIsNULL(createTime)) {
            sql += " and create_time>'" + createTime + "'";
        }
        return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{type}), "create_time", "yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public JSONObject getMatchSettingById(long matchId, long gameId) {
        String sql = "select id,type,game_id,match_name,per_count,player_count,total_round,is_auto,robot_level,must_full,description,online_num,match_cost," +
            "cost_type,reward_info,match_info,rule,promotion,is_use,create_time,platform,memo,reward_detail from za_match_setting where id=? and game_id=?";
        return TimeUtil.transTimeStamp(DBUtil.getObjectBySQL(sql, new Object[]{matchId, gameId}), "yyyy-MM-dd HH:mm:ss", "create_time");
    }

    @Override
    public void updateMatchSettingById(long matchId, String createTime) {
        String sql = "update za_match_setting set create_time=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{createTime, matchId});
    }

    @Override
    public JSONObject getMatchInfoByMatchId(long matchId, int isFull, int isEnd) {
        String sql = "select id,match_num,match_id,type,create_time,player_array,robot_array,current_count,current_round," +
            "total_round,is_full,is_end from za_match_info where match_id=? and is_full=? and is_end=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{matchId, isFull, isEnd});
    }

    @Override
    public void addOrUpdateMatchInfo(JSONObject obj) {
        DBJsonUtil.saveOrUpdate(obj, "za_match_info");
    }

    @Override
    public void updateMatchInfoByMatchNum(String matchNum, int isFull) {
        String sql = "update za_match_info set is_full=? where match_num=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{isFull, matchNum});
    }

    @Override
    public JSONArray getRobotList(int count) {
        String sql = "select account from za_users where status=? and openid=? limit ?,?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{0, 0, 0, count});
    }

    @Override
    public void updateUserCoinsAndScoreByAccount(String account, int coins, int score, int roomCard) {
        String sql = "update za_users set coins=coins+?,score=score+?,roomcard=roomcard+? where account=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{coins, score, roomCard, account});
    }

    @Override
    public JSONObject getUserWinningRecord(String account, int gameId) {
        String sql = "select id,user_account,game_id,winning_record,win_coins,win_score from za_match_winning_record where user_account=? and game_id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, gameId});
    }

    @Override
    public void addOrUpdateUserWinningRecord(JSONObject winningRecord) {
        DBJsonUtil.saveOrUpdate(winningRecord, "za_match_winning_record");
    }

    @Override
    public void updateRobotStatus(String account, int status) {
        String sql = "update za_users set status=? where account=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{status, account});
    }

}
