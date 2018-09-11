package com.zhuoan.biz.game.dao;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * ClubDao
 *
 * @author wqm
 * @Date Created in 15:40 2018/8/22
 **/
public interface ClubDao {

    /**
     * 获取用户俱乐部信息
     *
     * @param account account
     * @return JSONObject
     */
    JSONObject getUserClubByAccount(String account);

    /**
     * 根据编号获取俱乐部信息
     *
     * @param clubCode clubCode
     * @return JSONObject
     */
    JSONObject getClubByCode(String clubCode);

    /**
     * 根据id获取俱乐部信息
     *
     * @param id id
     * @return JSONObject
     */
    JSONObject getClubById(long id);

    /**
     * 获取俱乐部成员
     *
     * @param clubId clubId
     * @return
     */
    JSONArray getClubMember(long clubId);

    /**
     * 获取用户信息
     *
     * @param account account
     * @param uuid uuid
     * @return JSONObject
     */
    JSONObject getUserByAccountAndUuid(String account, String uuid);

    /**
     * 更新俱乐部信息
     *
     * @param clubInfo clubInfo
     */
    void updateClubInfo(JSONObject clubInfo);

    /**
     * 更新用户俱乐部信息
     *
     * @param userId  userId
     * @param clubIds clubIds
     */
    void updateUserClubIds(long userId, String clubIds);

    /**
     * 更新俱乐部余额
     * @param clubId clubId
     * @param sum sum
     */
    void updateClubBalance(long clubId, double sum);

    /**
     * 添加俱乐部抽水记录
     * @param userId userId
     * @param roomId roomId
     * @param roomNo roomNo
     * @param gid gid
     * @param type type
     * @param sum sum
     * @param createTime createTime
     * @param platform platform
     * @param pocketNew pocketNew
     * @param pocketOld pocketOld
     */
    void addClubPumpRec(long userId, long roomId, String roomNo, int gid, int type, double sum,String createTime, String platform, double pocketNew, double pocketOld);

    /**
     * 更新玩家置顶俱乐部
     *
     * @param account
     * @param clubId
     */
    void updateUserTopClub(String account, long clubId);
}
