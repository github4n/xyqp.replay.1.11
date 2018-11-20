package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.service.cache.RedisService;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author huaping.li
 * @Description: 游戏记录实现类
 * @date 2018-04-18 18:32
 */
@Service
public class GameLogBizImpl implements GameLogBiz {

    /**
     * 游戏数据操作接口
     */
    @Resource
    private GameDao gameDao;

    @Resource
    private RedisService redisService;

    /**
     * 保存游戏记录 (如果id<0或json中不包含id则新增一条记录,否则根据id更新记录)
     *
     * @param gamelog
     * @return
     */
    @Override
    public int addOrUpdateGameLog(JSONObject gamelog) {

        return gameDao.addOrUpdateGameLog(gamelog);
    }

    /**
     * 根据room_id和game_index获取gamelog的id(未找到则返回-1)
     *
     * @param room_id
     * @param game_index
     * @return
     */
    @Override
    public long getGameLogId(long room_id, int game_index) {

        return gameDao.getGameLogId(room_id, game_index);
    }

    /**
     * 保存玩家战斗记录
     *
     * @param usergamelog
     * @return
     */
    @Override
    public int addUserGameLog(JSONObject usergamelog) {
        // 清除缓存
        redisService.hdel("user_game_log_list" + String.valueOf(usergamelog.getInt("gid")) + String.valueOf(usergamelog.getInt("room_type")),String.valueOf(usergamelog.getInt("user_id")) );
        return gameDao.addUserGameLog(usergamelog);
    }

    /**
     * 更新战绩（房卡场解散）
     *
     * @param roomNo
     * @param gid
     * @param jsonArray
     * @return void
     * @throws
     * @date 2018年2月10日
     */
    @Override
    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray) {

        gameDao.updateGamelogs(roomNo, gid, jsonArray);
    }

    /**
     * 获取玩家战绩
     *
     * @param account
     * @param gid
     * @param num
     * @param createTime
     * @return JSONArray
     * @throws
     * @date 2018年3月19日
     */
    @Override
    public JSONArray getUserGameLogList(String account, int gid, int num, String createTime) {

        return gameDao.getUserGameLogList(account, gid, num, createTime);
    }

    @Override
    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType) {
        Object userGameLogList = redisService.hget("user_game_log_list" + gameId + roomType, String.valueOf(userId));
        if (userGameLogList != null) {
            return JSONArray.fromObject(userGameLogList);
        }
        JSONArray userGameLogsByUserId = gameDao.getUserGameLogsByUserId(userId, gameId, roomType);
        redisService.hset("user_game_log_list" + gameId + roomType, String.valueOf(userId), String.valueOf(userGameLogsByUserId));
        return userGameLogsByUserId;
    }

    @Override
    public JSONArray getUserGameRoomByRoomType(long userId, int gameId, int roomType) {
        return gameDao.getUserGameRoomByRoomType(userId, gameId, roomType);
    }

    @Override
    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType, List<String> roomList, String clubCode) {
        return gameDao.getUserGameLogsByUserId(userId, gameId, roomType, roomList, clubCode);
    }
}
