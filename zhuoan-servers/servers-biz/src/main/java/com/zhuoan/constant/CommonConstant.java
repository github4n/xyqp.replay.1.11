package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;

import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 14:01 2018/4/21
 * @Modified By:
 **/
public class CommonConstant {
    /**
     * 游戏id-牛牛
     */
    public static final int GAME_ID_NN = 1;
    /**
     * 游戏id-十三水
     */
    public static final int GAME_ID_SSS = 4;

    /**
     * 客户端标识-账号
      */
    public static final String CLIENT_TAG_ACCOUNT = "account";
    /**
     * 客户端标识-房间号
     */
    public static final String CLIENT_TAG_ROOM_NO = "roomNo";
    /**
     * 客户端标识-用户信息
     */
    public static final String CLIENT_TAG_USER_INFO = "userInfo";

    /**
     * 房间类型-房卡
     */
    public static final int ROOM_TYPE_FK = 0;
    /**
     * 房间类型-金币
     */
    public static final int ROOM_TYPE_JB = 1;
    /**
     * 房间类型-代开房间
     */
    public static final int ROOM_TYPE_DK = 2;
    /**
     * 房间类型-元宝
     */
    public static final int ROOM_TYPE_YB = 3;

    /**
     * 全局-是
     */
    public static final int GLOBAL_YES = 1;
    /**
     * 全局否
     */
    public static final int GLOBAL_NO = 0;

    /**
     * 准备超时-不处理
     */
    public static final int READY_OVERTIME_NOTHING = 0;
    /**
     * 准备超时-自动准备
     */
    public static final int READY_OVERTIME_AUTO = 1;
    /**
     * 准备超时-踢出房间
     */
    public static final int READY_OVERTIME_OUT = 2;

    /**
     * 解散房间-同意
     */
    public static final int CLOSE_ROOM_AGREE = 1;
    /**
     * 解散房间-未确认
     */
    public static final int CLOSE_ROOM_UNSURE = 0;
    /**
     * 解散房间-拒绝
     */
    public static final int CLOSE_ROOM_DISAGREE = -1;
    /**
     * 是否检查游戏状态
     */
    public static final int CHECK_GAME_STATUS_NO = -1;

    public static final int SHOW_MSG_TYPE_SMALL = 1;
    public static final int SHOW_MSG_TYPE_BIG = 2;

    public static final String DATA_KEY_ROOM_NO = "room_no";
    public static final String DATA_KEY_ACCOUNT = "account";

    public static final String RESULT_KEY_CODE = "code";
    public static final String RESULT_KEY_MSG = "msg";

    /**
     * 检查客户端数据是否正确，是否符合当前游戏状态
     * @param postData
     * @param gameStatus
     * @return
     */
    public static boolean checkEvent(JSONObject postData, int gameStatus, SocketIOClient client){
        if (!postData.containsKey(DATA_KEY_ROOM_NO)||!postData.containsKey(DATA_KEY_ACCOUNT)) {
            return false;
        }
        // 房间号
        String roomNo = postData.getString(DATA_KEY_ROOM_NO);
        // 账号
        String account = postData.getString(DATA_KEY_ACCOUNT);
        // 房间不存在或房间为空
        if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
            return false;
        }
        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
        // 玩家不在房间内
        if (!gameRoom.getPlayerMap().containsKey(account)||gameRoom.getPlayerMap().get(account)==null) {
            return false;
        }
        // client对象不为空验证是否为该玩家发的消息
        if (client!=null) {
            if (!client.getSessionId().equals(gameRoom.getPlayerMap().get(account).getUuid())) {
                return false;
            }
        }
        // 当前非该游戏阶段
        if (gameStatus!=CHECK_GAME_STATUS_NO&&gameRoom.getGameStatus()!=gameStatus) {
            return false;
        }
        return true;
    }

    /**
     * 通知所有玩家
     * @param uuidList
     * @param data
     * @param eventName
     */
    public static void sendMsgEventToAll(List<UUID> uuidList, String data, String eventName){
        for (UUID uuid : uuidList) {
            SocketIOClient client = GameMain.server.getClient(uuid);
            if (client!=null) {
                client.sendEvent(eventName,JSONObject.fromObject(data));
            }
        }
    }

    /**
     * 通知单个玩家
     * @param uuid
     * @param data
     * @param eventName
     */
    public static void sendMsgEventToSingle(UUID uuid,String data,String eventName){
        SocketIOClient client = GameMain.server.getClient(uuid);
        if (client!=null) {
            client.sendEvent(eventName,JSONObject.fromObject(data));
        }
    }

    /**
     * 通知单个玩家
     * @param client
     * @param data
     * @param eventName
     */
    public static void sendMsgEventToSingle(SocketIOClient client,String data,String eventName){
        if (client!=null) {
            client.sendEvent(eventName,JSONObject.fromObject(data));
        }
    }
}
