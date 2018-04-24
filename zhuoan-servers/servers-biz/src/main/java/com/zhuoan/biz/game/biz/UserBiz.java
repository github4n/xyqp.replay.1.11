package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author huaping.li
 * @Description 玩家操作接口
 * @date 2018-04-17 15:07
 */
public interface UserBiz {


    /**
     * 根据用户ID获取用户信息
     * @param id 用户ID
     * @return
     */
    public JSONObject getUserByID(long id);

    /**
     * 根据玩家账号获取用户信息
     * @param account
     * @return
     */
    public JSONObject getUserByAccount(String account);

    /**
     * 检查uuid是否合法
     * @param account
     * @param uuid
     * @return
     */
    public JSONObject checkUUID(String account, String uuid);


    /**
     * 更新用户 余额
     *
     * @param data [{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}]
     * @param types 更新类型  元宝:yuanbao  金币：coins   房卡：roomcard  积分：score
     */
    public boolean updateUserBalance(JSONArray data, String types);


    /**
     * 插入分润表数据
     *
     * @param obj {"user":[{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}],"gid":x,"roomNo":xxx,"type":xxxx}
     */
    public void insertUserdeduction(JSONObject obj);

    /**
     * 获取玩家充值元宝（待更新到缓存的元宝）
     * @param
     * @return JSONArray
     * @throws
     * @date 2018年3月28日
     */
    public JSONArray refreshUserBalance();

    /**
     * 获取工会信息
     * @param id
     * @param
     * @return JSONObject
     * @throws
     * @date 2018年4月10日
     */
    public JSONObject getGongHui(long id);

    /**
     * 获取系统管理员信息
     * @param adminCode
     * @param adminPass
     * @param memo
     * @return
     */
    public JSONObject getSysUser(String adminCode, String adminPass, String memo);

}