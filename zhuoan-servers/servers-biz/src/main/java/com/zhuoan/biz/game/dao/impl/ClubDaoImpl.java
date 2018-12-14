package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.ClubDao;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.TimeUtil;
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
        String sql = "select id,clubCode,platform,notice,setting,leaderId,clubName,quick_setting,balance,balance_type,payType from club where clubCode=? and isUse=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{clubCode});
    }

    @Override
    public JSONObject getClubById(long id) {
        String sql = "select id,clubCode,platform,notice,setting,leaderId,clubName,quick_setting,balance,balance_type,payType from club where id=? and isUse=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    @Override
    public JSONArray getClubMember(long clubId) {
        String sql = "select id,account,platform,clubIds,top_club,name,headimg from za_users where clubIds like ?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{"%$" + clubId + "$%"});
    }

    @Override
    public JSONObject getUserByAccountAndUuid(String account, String uuid) {
        String sql = "select id,clubIds,roomcard,yuanbao from za_users where account=? and uuid=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account, uuid});
    }

    @Override
    public void updateClubInfo(JSONObject clubInfo) {
        DBJsonUtil.update(clubInfo, "club");
    }

    @Override
    public void updateUserClubIds(long userId, String clubIds) {
        String sql = "update za_users set clubIds=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubIds, userId});
    }

    @Override
    public void updateClubBalance(long clubId, double sum) {
        String sql = "update club set balance=balance-? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{sum, clubId});
    }

    @Override
    public void addClubPumpRec(long userId, long roomId, String roomNo, int gid, int type, double sum, String createTime,
                               String platform, double pocketNew, double pocketOld) {
        String sql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,doType,creataTime,memo," +
            "platform,pocketNew,pocketOld,pocketChange,operatorType) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{userId, roomId, roomNo, gid, type, sum, 2, createTime, "俱乐部抽水", platform, pocketNew, pocketOld,
            sum, CommonConstant.SCORE_CHANGE_TYPE_PUMP});
    }

    @Override
    public void updateUserTopClub(String account, long clubId) {
        String sql = "update za_users set top_club=? where account=?";
        DBUtil.executeUpdateBySQL(sql,new Object[]{clubId,account});
    }

    @Override
    public JSONArray getClubInviteRec(int status, long clubId) {
        String sql = "SELECT a.id,a.clubId,a.userId,b.account,b.`name`,b.headimg FROM `club_invite_rec` a " +
            "LEFT JOIN za_users b ON a.userId=b.id where a.status=? and a.clubId=? ORDER BY a.id DESC";
        return DBUtil.getObjectListBySQL(sql, new Object[]{status, clubId});
    }

    @Override
    public void updateClubInviteRecStatus(int status, long clubInviteRecId) {
        String sql = "update club_invite_rec set status=?,modifyTime=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{status, TimeUtil.getNowDate(), clubInviteRecId});
    }

    @Override
    public void updateUserClub(long userId, String clubIds) {
        String sql = "update za_users set clubIds=? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubIds, userId});
    }

    @Override
    public void addClubInviteRec(long userId, long clubId, long parId, String memo, int status) {
        String sql = "insert into club_invite_rec(clubId,userId,parId,description,status,createTime,modifyTime) values(?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql, new Object[]{clubId, userId , parId, memo, status, TimeUtil.getNowDate(), TimeUtil.getNowDate()});
    }
}
