package com.zhuoan.biz.service.bdx.service;

import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import net.sf.json.JSONObject;

import java.util.UUID;

public interface BDXService {

	/**
	 * 创建房间
	 * @param roomNo 房间号
	 * @param uuid  用户标识（临时）
	 * @param objInfo  房间属性信息
	 * @param player 玩家信息
	 * @return
	 */
	public BDXGameRoom createGameRoom(JSONObject roomObj, UUID uuid, JSONObject objInfo, Playerinfo player);

	/**
	 * 加入房间
	 * @param roomNo 房间号
	 * @param uuid  用户标识（临时）
	 * @param player 玩家信息
	 * @param isNext 
	 * @return 
	 */
	public boolean joinGameRoom(String roomNo, UUID uuid, Playerinfo player, int roomType);

}
