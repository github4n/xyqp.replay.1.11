package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * ClubBiz
 *
 * @author wqm
 * @Date Created in 15:39 2018/8/22
 **/
public interface ClubBiz {

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
     * @return JSONArray
     */
    JSONArray getClubMember(long clubId);

    /**
     * 获取用户信息
     *
     * @param account account
     * @param uuid    uuid
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
     * 俱乐部抽水
     * @param clubCode clubCode
     * @param sum sum
     * @param roomId roomId
     * @param roomNo roomNo
     * @param gid gid
     * @return
     */
    boolean clubPump(String clubCode, double sum, long roomId,String roomNo, int gid);

    /**
     * 更新玩家置顶俱乐部
     *
     * @param account
     * @param clubId
     */
    void updateUserTopClub(String account, long clubId);

    /**
     * 获取记录申请记录
     *
     * @param status status
     * @param clubId clubId
     * @return JSONArray
     */
    JSONArray getClubInviteRec(int status, long clubId);

    /**
     * 更新俱乐部邀请状态
     *
     * @param status status
     * @param clubInviteRecId clubInviteRecId
     */
    void updateClubInviteRecStatus(int status, long clubInviteRecId);

    /**
     * 更新玩家俱乐部
     *
     * @param userId
     * @param clubIds
     */
    void updateUserClub(long userId, String clubIds);

    /**
     * 添加邀请记录
     * @param userId
     * @param clubId
     * @param parId
     * @param memo
     * @param status
     * @return
     */
    void addClubInviteRec(long userId, long clubId, long parId, String memo, int status);
}
