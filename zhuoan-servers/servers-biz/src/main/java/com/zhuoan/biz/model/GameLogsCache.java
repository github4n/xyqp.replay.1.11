package com.zhuoan.biz.model;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 战绩缓存类
 * ClassName: GameLogsCache
 * @author wqm
 * @date 2018年3月19日
 */
public class GameLogsCache {

	// <玩家账号,<游戏id,战绩>>
	public static Map<String, Map<Integer, List<GameLogs>>> gameLogsMap = new ConcurrentHashMap<String, Map<Integer, List<GameLogs>>>();
	// <玩家账号,是否需要查询数据库补全>
	public static Map<String, Map<Integer, Boolean>> selectOrNotMap = new ConcurrentHashMap<String, Map<Integer, Boolean>>();
	// 缓存条数
	public static int cacheNumber = 20;
	
	public static MaJiangBiz maJiangBiz = new MajiangBizImpl();
	
	/**
	 * 获取战绩
	 * @param @param account
	 * @param @param gid
	 * @param @param gameLogs   
	 * @return void  
	 * @throws
	 * @date 2018年3月19日
	 */
	public static void addGameLogs(String account, int gid, GameLogs gameLogs){
		Map<Integer, List<GameLogs>> map;
		List<GameLogs> list;
		// 已存在更新
		if (gameLogsMap.containsKey(account)) {
			map = gameLogsMap.get(account);
			if (map.containsKey(gid)) {// 已存在更新
				list = map.get(gid);
				// 添加战绩记录，放在第一条；
				list.add(0, gameLogs);
				// 超过20条移除旧数据
				if (list.size()>cacheNumber) {
					list.remove(cacheNumber);
				}
			}else {// 未存在加入新的数据
				list = new ArrayList<GameLogs>();
				list.add(gameLogs);
			}
		}else {// 未存在加入新的数据
			map = new HashMap<Integer, List<GameLogs>>();
			list = new ArrayList<GameLogs>();
			list.add(gameLogs);
		}
		// 存入缓存
		map.put(gid, list);
		gameLogsMap.put(account, map);
		// 增加该玩家是否需要查询数据库判断
		if (!selectOrNotMap.containsKey(account)) {
			HashMap<Integer, Boolean> map2 = new HashMap<Integer, Boolean>();
			map2.put(gid, true);
			selectOrNotMap.put(account, map2);
		}
	}
	
	public static JSONObject updateGameLogsList(String account, int gid){
		boolean flag = true;
		// 没有该玩家的缓存或缓存中条数不够需要查询数据库(只查询一次)
		if (!gameLogsMap.containsKey(account)||(gameLogsMap.get(account).get(gid)!=null&&gameLogsMap.get(account).get(gid).size()<cacheNumber)) {
			flag = false;
		}
		// 不需要从数据库获取或该玩家已经从数据库获取过一次
		if (!(flag||(selectOrNotMap.containsKey(account)&&
				selectOrNotMap.get(account).containsKey(gid)&&!selectOrNotMap.get(account).get(gid)))) {
			Map<Integer,List<GameLogs>> hashMap = new HashMap<Integer, List<GameLogs>>();
			List<GameLogs> list = new ArrayList<GameLogs>();
			// 缓存有战绩不够条数在现有基础上加
			String temp = null;
			if (gameLogsMap.containsKey(account)&&gameLogsMap.get(account).containsKey(gid)
					&&gameLogsMap.get(account).get(gid).size()>0) {
				hashMap = gameLogsMap.get(account);
 				list = gameLogsMap.get(account).get(gid);
 				// 只查最后一条战绩之前的
 				temp = list.get(list.size()-1).createTime;
			}
			// 获取战绩
			JSONArray userGameLogList = maJiangBiz.getUserGameLogList(account, gid, cacheNumber,temp);
			// 数据库有数据组织数据放入缓存
			if (!Dto.isNull(userGameLogList)) {
				for (Object object : userGameLogList) {
					// 各个玩家输赢
					Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();
					JSONArray jsonArray = JSONObject.fromObject(object).getJSONArray("result");
					// 获取当局游戏的所有玩家及对应的输赢分数
					for (int i = 0; i < jsonArray.size(); i++) {
						String player = jsonArray.getJSONObject(i).getString("player");
						int score = jsonArray.getJSONObject(i).getInt("score");
						String name = jsonArray.getJSONObject(i).getString("player");
						playerMap.put(player, new JSONObject().element("score", score).element("name", name));
					}
					// 房间号
					String roomNo = JSONObject.fromObject(object).getString("room_no");
					// 游戏时间
					String createTime = JSONObject.fromObject(object).getString("createtime");
					// 添加实体对象
					list.add(new GameLogs(roomNo, playerMap, createTime));
					// 达到缓存条数停止循环
					if (list.size()==cacheNumber) {
						break;
					}
				}
			}
			hashMap.put(gid, list);
			gameLogsMap.put(account, hashMap);
			// 设置为已经查询过数据库状态
			Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();
			map.put(gid, false);
			selectOrNotMap.put(account, map);
		}
		JSONObject jsonObject = new JSONObject();
		if (gameLogsMap.get(account).get(gid)==null||gameLogsMap.get(account).get(gid).size()==0) {
			jsonObject.element("code", 0);
		}else {
			jsonObject.element("code", 1);
			jsonObject.element("gid", gid);
			List<GameLogs> list = gameLogsMap.get(account).get(gid);
			JSONArray jsonArray = new JSONArray();
			System.err.println(list.size());
			for (int i = 0; i < list.size(); i++) {
				JSONObject jsonObject2 = new JSONObject();
				jsonObject2.element("room_no", list.get(i).roomNo);
				jsonObject2.element("createTime", list.get(i).createTime);
				JSONArray jsonArray2 = new JSONArray();
				for (String string : list.get(i).playerMap.keySet()) {
					JSONObject jsonObject3 = new JSONObject();
					jsonObject3.element("player", list.get(i).playerMap.get(string).getString("name"));
					jsonObject3.element("score", list.get(i).playerMap.get(string).getInt("score"));
					jsonArray2.add(jsonObject3);
				}
				jsonObject2.element("playermap", jsonArray2);
				jsonArray.add(jsonObject2);
			}
			jsonObject.element("data", jsonArray);
		}
		return jsonObject;
	}
	
	public void getGameLogsList(SocketIOClient client, Object data){
		JSONObject fromObject = JSONObject.fromObject(data);
		int gid = fromObject.getInt("gid");
		String account = fromObject.getString("account");
		JSONObject updateGameLogsList = updateGameLogsList(account, gid);
		client.sendEvent("getGameLogsListPush", updateGameLogsList);
	}
	
	
	public static void main(String[] args) {
		Map<String, Integer> playerMap = new HashMap<String, Integer>();
		playerMap.put("444", 1);
		playerMap.put("555", 2);
		playerMap.put("666", 3);
		for (int i = 1; i <= 10; i++) {
			long start = System.currentTimeMillis();
			updateGameLogsList("12765555", 1);
			long end = System.currentTimeMillis();
			System.err.println("第"+i+"次获取战绩耗时"+(end-start)+"<-->战绩条数为:"+gameLogsMap.get("12765555").get(1).size());
		}
	}
}
