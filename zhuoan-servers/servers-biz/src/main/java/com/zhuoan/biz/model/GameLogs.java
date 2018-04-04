package com.zhuoan.biz.model;

import net.sf.json.JSONObject;

import java.util.Map;

/**
 * 战绩实体类
 * ClassName: GameLogs
 * @author wqm
 * @date 2018年3月19日
 */
public class GameLogs {

	public String roomNo;// 房间号
	public Map<String, JSONObject> playerMap;// 玩家账号-玩家输赢分数
	public String createTime;// 游戏时间
	
	public GameLogs(String roomNo, Map<String, JSONObject> playerMap,
                    String createTime) {
		this.roomNo = roomNo;
		this.playerMap = playerMap;
		this.createTime = createTime;
	}
	
}
