package com.zhuoan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtil {

    private final static Logger logger = LoggerFactory.getLogger(LogUtil.class);

    /**
     * 消息打印
     *
     * @param msg the msg
     */
    public static void print(String msg){

        logger.info(msg);
	}
	
	/**
	 * 普通消息
	 * @param clazz
	 * @param msg
	 */
	public static void info(Class clazz, String msg){

        logger.info(msg);
	}
	
	/**
	 * 错误消息
	 * @param clazz
	 * @param msg
	 */
	public static void error(Class clazz, String msg){

        logger.error(msg);
	}
	
	/**
	 * 警告消息
	 * @param clazz
	 * @param msg
	 */
	public static void warn(Class clazz, String msg){

        logger.warn(msg);
	}
	
	/**
	 * 调试消息
	 * @param clazz
	 * @param msg
	 */
	public static void debug(Class clazz, String msg){

        logger.debug(msg);
	}
	
	/**
	 * 用户出牌记录
	 * @param obj
	 */
//	public static void addZaGameRecord(JSONObject obj) {
//		String sql = "insert into za_game_record(gameID,userID,roomType,roomNO,gameIndex,des,createTime) values(?,?,?,?,?,?,?)";
//		/*String des="【发牌阶段】用户："+obj.getString("name")+"，用户手牌："+obj.getString("pai");*/
//		DBUtil.executeUpdateBySQL(sql, new Object[]{4,obj.getInt("userid") ,obj.getInt("roomType"),obj.getInt("roomNo"), obj.getInt("GameIndex"),obj.getString("des"), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())});
//	}
}
