package com.zhuoan.biz.game.biz;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * @author huaping.li
 * @Description: 游戏大厅公共操作接口
 * @date 2018-04-17 15:09
 */
public interface PublicBiz {

    /**
     * 获取房间设置
     * @param gid
     * @param platform
     * @return
     */
    public JSONArray getRoomSetting(int gid, String platform);

    /**
     * 获取系统配置
     * @return
     */
    public JSONObject getSysBaseSet();

    /**
     * 获取游戏配置
     * @return
     */
    public JSONObject getAPPGameSetting();

    /**
     * 获取道具扣除记录
     * @param userId
     * @param doType
     * @param gid
     * @param roomid
     * @param roomNo
     * @return
     */
    public JSONArray getAppObjRec(Long userId, int doType, String gid,String roomid, String roomNo);

    /**
     * 添加道具扣除记录
     * @param object
     */
    public void addAppObjRec(JSONObject object);

    /**
     * 通过平台号获取滚动公告
     * @param platform
     * @return
     */
    public JSONObject getNoticeByPlatform(String platform);

    /**
     * 获取金币场设置
     * @param obj
     * @return
     */
    public JSONObject getGoldSetting(JSONObject obj);
}
