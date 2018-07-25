package com.zhuoan.biz.game.dao;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:03 2018/7/25
 * @Modified By:
 **/
public interface PropsDao {

    /**
     * 根据平台号获取道具
     * @param platform 平台号
     * @return JSONArray
     */
    JSONArray getPropsInfoByPlatform(String platform);

    /**
     * 根据id获取道具
     * @param propsId 道具id
     * @return JSONObject
     */
    JSONObject getPropsInfoById(long propsId);

    /**
     * 获取用户道具
     *
     * @param account 用户账号
     * @param propsType 道具类型
     * @return
     */
    JSONObject getUserPropsByType(String account, int propsType);

    /**
     * 更新用户道具
     *
     * @param userProps 用户道具信息
     */
    void addOrUpdateUserProps(JSONObject userProps);
}
