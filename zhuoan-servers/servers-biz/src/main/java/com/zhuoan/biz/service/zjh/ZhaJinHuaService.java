package com.zhuoan.biz.service.zjh;

import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.zjh.ZJHGame;
import net.sf.json.JSONObject;

public interface ZhaJinHuaService {


	/**
	 * 创建房间
	 * @param room 房间信息
	 * @param uuid 用户会话标识
	 * @param player 玩家信息
	 * @return
	 */
	public ZJHGame createGameRoom(JSONObject room, String uuid, Playerinfo player);

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
	 * 清除离线的玩家
	 * @param room
	 */
	public void cleanDisconnectPlayer(ZJHGame room);

	/**
	 * 玩家准备
	 * @param roomNo
	 * @param uuid
	 */
	public void isReady(String roomNo, String uuid);

	/**
	 * 洗牌
	 * @param roomNo
	 */
	public void xiPai(String roomNo);

	/**
	 * 发牌
	 * @param roomNo
	 */
	public void faPai(String roomNo);

	/**
	 * 下注
	 * @param uuid
	 * @param roomNo
	 * @param score
	 * @return 
	 */
	public boolean xiazhu(String uuid, String roomNo, double score, int type);

	/**
	 * 跟注
	 * @param uuid
	 * @param roomNo
	 */
	public void genzhu(String uuid, String roomNo);

	/**
	 * 跟到底
	 * @param uuid
	 * @param roomNo
	 * @param obj 
	 */
	public void gendaodi(String uuid, String roomNo, JSONObject obj);

	/**
	 * 弃牌
	 * @param uuid
	 * @param roomNo
	 */
	public void qipai(String uuid, String roomNo);

	/**
	 * 看牌
	 * @param uuid
	 * @param roomNo
	 */
	public void kanpai(String uuid, String roomNo);

	/**
	 * 比牌
	 * @param uuid
	 * @param roomNo
	 * @param data
	 */
	public void bipai(String uuid, String roomNo, JSONObject data);

	/**
	 * 强制比牌
	 * @param roomNo
	 */
	public void compelBiPai(String roomNo, String opuuid);
	
}
