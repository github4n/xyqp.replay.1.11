package com.zhuoan.biz.service.service;

import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.nn.NNGameRoom;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.List;

public interface NiuNiuService {


	/**
	 * 创建房间
	 * @param room 房间信息
	 * @param uuid 用户会话标识
	 * @param player 玩家信息
	 * @return
	 */
	public NNGameRoom createGameRoom(JSONObject room, String uuid, Playerinfo player);

	/**
	 * 加入房间
	 * @param roomNo 房间号
	 * @param uuid  用户标识（临时）
	 * @param player 玩家信息
	 * @param isNext 
	 * @return 
	 */
	public boolean joinGameRoom(String roomNo, String uuid, Playerinfo player, boolean isNext);

	/**
	 * 亮牌
	 * @param roomNo
	 * @param uuid
	 */
	public void showPai(String roomNo, String uuid);

	/**
	 * 结算
	 * @param roomNo
	 * @param uuid 
	 */
	public void jieSuan(String roomNo);

	/**
	 * 清除离线的玩家
	 * @param room
	 * @return 
	 */
	public List<String> cleanPlayer(NNGameRoom room);

	/**
	 * 抢庄
	 * @param roomNo
	 * @param result
	 * @param sessionId
	 */
	public void qiangZhuang(String roomNo, String result, String sessionId);

	/**
	 * 总结算
	 * @param room
	 * @return
	 */
	public JSONArray balance(NNGameRoom room);
	
}
