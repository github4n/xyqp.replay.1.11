package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.sss.SSSGameRoom;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Constant {
	
	
	// 客户端标识（用户唯一标识）
	public final static String CLIENTTAG = "clienttag";
	
	/**
	 * 获取用户标识
	 * @param client
	 * @return
	 */
	public static String getClientTag(SocketIOClient client){
		
		if(client!=null&&client.has(Constant.CLIENTTAG)){
			return client.get(Constant.CLIENTTAG);
		}
		return "";
	}
	
	// 玩家在线状态
	public final static int ONLINE_STATUS_YES=1;//在线
	public final static int ONLINE_STATUS_NO=0;//不在线
	
	// 加载配置文件信息
	public static Properties cfgProperties = new Properties();
	
	/**
	 * zagame项目名
	 */
	public static String DOMAIN = "/zagame";
	
	static{

		try {
			cfgProperties.load(Constant.class.getClassLoader().getResourceAsStream("config/common.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/////////////////// 麻将游戏 ////////////////////////
	
	/**
	 * 20180315
	 * 房间key
	 */
	public final static String ROOM_KEY_COMMEN="ROOM_KEY_COMMEN";
	
	/**
	 * 20180315
	 * 房间列表
	 */

	/**
	 * 麻将房间key
	 */
	public final static String ROOM_KEY_MJ="ROOM_KEY_MJ";
	
	/**
	 * 泉州麻将游戏房间列表
	 */

	/**
	 * 福州麻将游戏房间列表
	 */

	/**
	 * 漳浦麻将游戏房间列表
	 */

	/**
	 * 无锡麻将游戏房间列表
	 */

	/**
	 * 南安麻将游戏房间列表
	 */

	////////////////////////////////////////////////////
	
	/////////////////// 牛牛游戏 ////////////////////////
	
	/**
	 *  牛牛游戏房间Key
	 */
	public static String ROOM_KEY_NN = "ROOM_KEY_NN";
	
	////////////////////////////////////////////////////
	
	/////////////////// 斗地主游戏 ////////////////////////

	/**
	 *  斗地主游戏房间Key
	 */
	public static String ROOM_KEY_DDZ = "ROOM_KEY_DDZ";
	
	/**

	/////////////////// 十三水游戏 ////////////////////////
	
	/**
	 *  十三水游戏房间Key
	 */
	public static String ROOM_KEY_SSS = "ROOM_KEY_SSS";
	
	/**
	 * 十三水游戏房间列表
	 */

	////////////////////////////////////////////////////
	/////////////////// 比大小游戏 ////////////////////////
	
	/**
	 *  比大小游戏房间Key
	 */
	public static String ROOM_KEY_BDX = "ROOM_KEY_BDX";
	

	/**
	 *  骨牌牌九游戏房间Key
	 */
	public static String ROOM_KEY_PJXY = "ROOM_KEY_PJXY";
	
	/**
	 * 骨牌牌九游戏房间列表
	 */

	////////////////////////////////////////////////////
	
	//////////////////// 欢乐哈游戏 	////////////////////
	/**
	 * 欢乐哈游戏房间key
	 */
	public static String ROOM_KEY_HLH = "ROOM_KEY_HLH";
	
	/**
	 * 欢乐哈游戏房间列表
	 */
	///////////////////	 欢乐哈游戏		////////////////////

	/////////////////// 炸金花游戏 ////////////////////////

	/**
	 *  炸金花游戏房间Key
	 */
	public static String ROOM_KEY_ZJH = "ROOM_KEY_ZJH";
	
	/**
	 * 炸金花游戏房间列表
	 */

	////////////////////////////////////////////////////

	/////////////////// 百家乐游戏 ////////////////////////

	/**
	 *  百家乐游戏房间Key
	 */
	public static String ROOM_KEY_BJL = "ROOM_KEY_BJL";
	
	/**
	 *  百家乐游戏房间列表
	 */

	////////////////////////////////////////////////////

	/////////////////// 抢红包游戏 ////////////////////////

	/**
	 *  抢红包游戏房间Key
	 */
	public static String ROOM_KEY_HB = "ROOM_KEY_HB";

    /**
     * 十三水游戏房间列表
     */
    public static Map<String, SSSGameRoom> sssGameMap = new ConcurrentHashMap<String, SSSGameRoom>();
}