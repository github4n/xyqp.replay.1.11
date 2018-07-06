package com.zhuoan.biz.game.dao;

import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 16:59 2018/7/5
 * @Modified By:
 **/
public interface FundDao {

    /**
     * 更新玩家元宝数
     * @param openId
     * @param score
     */
    public void updateUserScoreByOpenId(String openId, double score);

    /**
     * 获取第一个空闲的系统用户
     * @return
     */
    public JSONObject getSysUsers();

    /**
     * 更改系统用户状态
     * @param account
     * @param status
     */
    public void updateSysUserStatusByAccount(String account, int status);

    /**
     * 获取版本信息
     * @return
     */
    public JSONObject getVersionInfo();

    /**
     * 根据openId获取用户信息
     * @param openId
     * @return
     */
    public JSONObject getUserInfoByOpenId(String openId);

    /**
     * 根据account获取用户信息
     * @param account
     * @return
     */
    public JSONObject getUserInfoByAccount(String account);

    /**
     * 添加用户
     * @param account
     * @param obj
     * @param platform
     */
    public void insertUserInfo(String account, JSONObject obj, String platform);
}
