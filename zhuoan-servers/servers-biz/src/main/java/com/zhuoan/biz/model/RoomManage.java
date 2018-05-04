package com.zhuoan.biz.model;

import net.sf.json.JSONArray;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManage {
	/**
	 * 20180315
	 * 房间key
	 */
	public final static String ROOM_KEY_COMMEN="ROOM_KEY_COMMEN";

	/**
	 * 20180315
	 * 房间列表
	 */
	public static Map<String, GameRoom> gameRoomMap = new ConcurrentHashMap<String,GameRoom>();
	
	public static JSONArray result;
}
