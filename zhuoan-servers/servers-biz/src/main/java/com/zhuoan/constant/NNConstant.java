package com.zhuoan.constant;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.service.socketio.impl.GameMain;

import java.util.List;
import java.util.UUID;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:11 2018/4/17
 * @Modified By:
 **/
public class NNConstant {

    public static final int GID_NN = 1;

    // 房间类型
    public static final int ROOM_TYPE_FK = 0;
    public static final int ROOM_TYPE_JB = 1;
    public static final int ROOM_TYPE_DK = 2;
    public static final int ROOM_TYPE_YB = 3;

    public static final int GLOBAL_YES = 1;
    public static final int GLOBAL_NO = 0;

    public static final int CLOSE_ROOM_AGREE = 1;// 同意解散
    public static final int CLOSE_ROOM_UNSURE = 0;// 未确认
    public static final int CLOSE_ROOM_DISAGREE = -1;// 拒绝解散

    // 客户端标识
    public static final String CLIENT_TAG_ACCOUNT = "account";
    public static final String CLIENT_TAG_ROOM_NO = "roomNo";
    public static final String CLIENT_TAG_USER_INFO = "userInfo";

    // 返回数据
    public static final String RESULT_KEY_CODE = "code";
    public static final String RESULT_KEY_MSG = "msg";

    // 抢庄类型
    public static final int NN_BANKER_TYPE_FZ = 0;
    public static final int NN_BANKER_TYPE_LZ = 1;
    public static final int NN_BANKER_TYPE_QZ = 2;
    public static final int NN_BANKER_TYPE_MP = 3;
    public static final int NN_BANKER_TYPE_NN = 4;
    public static final int NN_BANKER_TYPE_TB = 5;

    // 无人抢庄
    public static final int NN_QZ_NO_BANKER_SJ = 1;
    public static final int NN_QZ_NO_BANKER_JS = -1;
    public static final int NN_QZ_NO_BANKER_CK = 2;

    // 准备超时
    public static final int NN_READY_OVERTIME_NOTHING = 0;
    public static final int NN_READY_OVERTIME_AUTO = 1;
    public static final int NN_READY_OVERTIME_OUT = 2;

    // 游戏状态
    public static final int NN_GAME_STATUS_INIT=0;// 初始
    public static final int NN_GAME_STATUS_READY=1;// 准备
    public static final int NN_GAME_STATUS_QZ=2;// 抢庄
    public static final int NN_GAME_STATUS_DZ=3;// 定庄
    public static final int NN_GAME_STATUS_XZ=4;// 下注
    public static final int NN_GAME_STATUS_LP=5;// 亮牌
    public static final int NN_GAME_STATUS_JS=6;// 结算
    public static final int NN_GAME_STATUS_ZJS=7;// 总结算

    // 玩家状态
    public static final int NN_USER_STATUS_INIT=0;// 初始
    public static final int NN_USER_STATUS_READY=1;// 准备
    public static final int NN_USER_STATUS_QZ=2;// 抢庄
    public static final int NN_USER_STATUS_DZ=3;// 定庄
    public static final int NN_USER_STATUS_XZ=4;// 下注
    public static final int NN_USER_STATUS_LP=5;// 亮牌
    public static final int NN_USER_STATUS_JS=6;// 结算
    public static final int NN_USER_STATUS_ZJS=7;// 总结算
    
    public static final int CHECK_GAME_STATUS_NO = -1;// 不需要判断当前游戏状态

    public static boolean checkEvent(SocketIOClient client,int gameStatus){
        // 客户端不包含房间号或用户账号
        /*if (!client.has(CLIENT_TAG_ACCOUNT)||!client.has(CLIENT_TAG_ROOM_NO)) {
            return false;
        }
        if (Dto.isNull(client.get(CLIENT_TAG_ROOM_NO))||Dto.isNull(client.get(CLIENT_TAG_ACCOUNT))) {
            return false;
        }
        // 房间号
        String roomNo = client.get(CLIENT_TAG_ROOM_NO).toString();
        // 账号
        String account = client.get(CLIENT_TAG_ACCOUNT).toString();
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
        if (gameStatus!=CHECK_GAME_STATUS_NO&&gameRoom.getGameStatus()!=gameStatus) {
            return false;
        }*/
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
                client.sendEvent(eventName,data);
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
            client.sendEvent(eventName,data);
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
            client.sendEvent(eventName,data);
        }
    }

}
