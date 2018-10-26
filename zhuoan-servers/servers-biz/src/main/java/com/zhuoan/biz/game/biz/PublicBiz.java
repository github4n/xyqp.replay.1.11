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
    public JSONArray getRoomSetting(int gid, String platform, int flag);

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
    public JSONArray getGoldSetting(JSONObject obj);

    /**
     * 获取用户签到信息
     * @return
     */
    public JSONObject getUserSignInfo(String platform,long userId);

    /**
     * 用户签到
     * @param obj
     */
    public int addOrUpdateUserSign(JSONObject obj);

    /**
     * 根据平台号获取签到奖励
     * @param platform
     */
    public JSONObject getSignRewardInfoByPlatform(String platform);

    /**
     * 获取竞技场信息
     * @return
     */
    public JSONArray getArenaInfo();

    /**
     * 更新玩家积分
     * @param userId
     * @param score
     * @param type
     */
    public void addOrUpdateUserCoinsRec(long userId, int score,int type);

    /**
     * 获取玩家游戏信息
     * @param account
     * @return
     */
    public JSONObject getUserGameInfo(String account);

    /**
     * 更改玩家游戏信息
     * @param obj
     */
    public void addOrUpdateUserGameInfo(JSONObject obj);

    /**
     * 添加玩家福利信息
     * @param account
     * @param sum
     * @param type
     * @param gameId
     */
    void addUserWelfareRec(String account, double sum, int type, int gameId);

    /**
     * 获取app配置
     *
     * @param platform platform
     * @return JSONObject
     */
    JSONObject getAppSettingInfo(String platform);
}
