package com.zhuoan.biz.game.biz;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;


/**
 * @author huaping.li
 * @Description: 游戏日志操作接口
 * @date 2018-04-17 15:16
 */
public interface GameLogBiz {


    /**
     * 保存游戏记录 (如果id<0或json中不包含id则新增一条记录,否则根据id更新记录)
     * @param gamelog
     * @return
     */
    public int addOrUpdateGameLog(JSONObject gamelog);

    /**
     * 根据room_id和game_index获取gamelog的id(未找到则返回-1)
     * @param room_id
     * @param game_index
     * @return
     */
    public long getGameLogId(long room_id,int game_index);

    /**
     * 保存玩家战斗记录
     * @param usergamelog
     * @return
     */
    public int addUserGameLog(JSONObject usergamelog);


    /**
     * 更新战绩（房卡场解散）
     * @param @param roomNo
     * @param @param gid
     * @param @param jsonArray
     * @return void
     * @throws
     * @date 2018年2月10日
     */
    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray);

    /**
     * 获取玩家战绩
     * @param @param account
     * @param @param gid
     * @param @param num
     * @param @return
     * @return JSONArray
     * @throws
     * @date 2018年3月19日
     */
    public JSONArray getUserGameLogList(String account, int gid, int num,String createTime);

    /**
     * 根据用户id获取玩家战绩
     * @param userId
     * @param gameId
     * @return
     */
    public JSONArray getUserGameLogsByUserId(long userId, int gameId);


}
