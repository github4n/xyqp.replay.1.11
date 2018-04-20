package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;

public class NewConstant {

	// 客户端标识
	public final static String CLIENTTAG = "clienttag";
	
	// 房间号标识
	public final static String ROOMNO = "room_no";
	
	public final static int ROOM_CREATE = 0;
	public final static int ROOM_JOIN = 1;
	
	// 玩家状态
	public final static int USERSTATUS_SSS_HALFWAY = -1;
	public final static int USERSTATUS_SSS_READY = 1;
	public final static int USERSTATUS_SSS_FAPAI = 1;
	public final static int USERSTATUS_SSS_PEIPAI = 2;
	
	// 游戏状态
	public final static int GAMESTATUS_SSS_READY = 0;
	public final static int GAMESTATUS_SSS_DINGZHUANG = 1;
	public final static int GAMESTATUS_SSS_XIPAI = 2;
	public final static int GAMESTATUS_SSS_FAPAI = 3;
	public final static int GAMESTATUS_SSS_PEIPAI = 4;
	public final static int GAMESTATUS_SSS_JIESUAN = 5;
	
	// 定庄类型
	public final static int ZHUANGTYPE_SSS_FANGZHU = 0;
	public final static int ZHUANGTYPE_SSS_LUNZHUANG = 1;
	
	// 游戏类型
	public final static int GAMETYPE_SSS_HUBI = 0;
	public final static int GAMETYPE_SSS_BANGWANGZHUANG = 1;
	
	// 配牌类型
	public final static int PEIPAITYPE_SSS_AUTO = 1;
	public final static int PEIPAITYPE_SSS_COMMEN = 2;
	public final static int PEIPAITYPE_SSS_SPECIAL = 3;
	
	// 不需要检查游戏状态
	public final static int CHECK_GAMESTATUS_NO = -1;
	
	// 检查当前消息是否应该被忽略
	public static boolean checkStatus(SocketIOClient client, int gameStatus){
		// 客户端对象为空
		if (client==null) {
			return false;
		}

		// 客户端不包含房间号或用户账号
		if (!client.has(CLIENTTAG)||!client.has(ROOMNO)) {
			return false;
		}
		// 房间号
		String roomNo = client.get(ROOMNO).toString();
		// 账号
		String account = client.get(CLIENTTAG).toString();
		// 房间不存在或房间为空
		if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
			return false;
		}
		GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
		// 玩家不在房间内
		if (!gameRoom.getPlayerMap().containsKey(account)||gameRoom.getPlayerMap().get(account)==null) {
			return false;
		}
		// 当前非该游戏阶段
		if (gameStatus!=CHECK_GAMESTATUS_NO&&gameRoom.getGameStatus()!=gameStatus) {
			return false;
		}
		return true;
	}
}
