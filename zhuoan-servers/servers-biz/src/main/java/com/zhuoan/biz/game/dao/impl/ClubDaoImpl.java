package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.ClubDao;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * ClubDaoImpl
 *
 * @author wqm
 * @Date Created in 15:40 2018/8/22
 **/
@Component
public class ClubDaoImpl implements ClubDao {

    @Override
    public JSONObject getUserClubByAccount(String account) {
        String sql = "select id,account,platform,clubIds,top_club from za_users where account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }

    @Override
    public JSONObject getClubByCode(String clubCode) {
        String sql = "select id,clubCode,platform,notice,setting,leaderId,clubName,quick_setting from club where clubCode=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{clubCode});
    }

    @Override
    public JSONObject getClubById(long id) {
        String sql = "select id,clubCode,clubName,platform,leaderId,quick_setting from club where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    @Override
    public JSONArray getClubMember(long clubId) {
        String sql = "select id,account,platform,clubIds,top_club,name,headimg from za_users where clubIds like ?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{"%"+clubId+"%"});
    }

    @Override
    public JSONObject getUserByAccountAndUuid(String account, String uuid) {
        String sql = "select id,clubIds from za_users where account=? and uuid=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, uuid});
    }

    @Override
    public void updateClubInfo(JSONObject clubInfo) {
        DBJsonUtil.update(clubInfo,"club");
    }

    @Override
    public void updateUserClubIds(long userId, String clubIds) {
        String sql = "update za_users set clubIds=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubIds, userId});
    }
}
