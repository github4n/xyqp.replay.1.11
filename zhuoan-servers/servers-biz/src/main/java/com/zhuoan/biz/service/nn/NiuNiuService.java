package com.zhuoan.biz.service.nn;

import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.nn.NNGameRoom;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * The interface Niu niu service.
 */
public interface NiuNiuService {


    /**
     * 创建房间
     *
     * @param room   房间信息
     * @param uuid   用户会话标识
     * @param player 玩家信息
     * @return nn game room
     */
    public NNGameRoom createGameRoom(JSONObject room, String uuid, Playerinfo player);

    /**
     * 加入房间
     *
     * @param roomNo 房间号
     * @param uuid   用户标识（临时）
     * @param player 玩家信息
     * @param isNext the is next
     * @return boolean
     */
    public boolean joinGameRoom(String roomNo, String uuid, Playerinfo player, boolean isNext);

    /**
     * 亮牌
     *
     * @param roomNo the room no
     * @param uuid   the uuid
     */
    public void showPai(String roomNo, String uuid);

    /**
     * 结算
     *
     * @param roomNo the room no
     */
    public void jieSuan(String roomNo);

    /**
     * 清除离线的玩家
     *
     * @param room the room
     * @return list
     */
    public List<String> cleanPlayer(NNGameRoom room);

    /**
     * 抢庄
     *
     * @param roomNo    the room no
     * @param result    the result
     * @param sessionId the session id
     */
    public void qiangZhuang(String roomNo, String result, String sessionId);

    /**
     * 总结算
     *
     * @param room the room
     * @return json array
     */
    public JSONArray balance(NNGameRoom room);
	
}
