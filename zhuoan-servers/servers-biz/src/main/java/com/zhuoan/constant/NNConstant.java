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
 * @Date Created in 9:11 2018/4/17
 * @Modified By:
 **/
public class NNConstant {

    /**
     * 游戏id-牛牛
     */
    public static final int GAME_ID_NN = 1;

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

    public static final String CLIENT_TAG_ACCOUNT = "account";
    public static final String CLIENT_TAG_ROOM_NO = "roomNo";
    public static final String CLIENT_TAG_USER_INFO = "userInfo";

    public static final String RESULT_KEY_CODE = "code";
    public static final String RESULT_KEY_MSG = "msg";

    /**
     * 抢庄类型-房主坐庄
     */
    public static final int NN_BANKER_TYPE_FZ = 0;
    /**
     * 抢庄类型-轮庄
     */
    public static final int NN_BANKER_TYPE_LZ = 1;
    /**
     * 抢庄类型-抢庄
     */
    public static final int NN_BANKER_TYPE_QZ = 2;
    /**
     * 抢庄类型-明牌抢庄
     */
    public static final int NN_BANKER_TYPE_MP = 3;
    /**
     * 抢庄类型-牛牛坐庄
     */
    public static final int NN_BANKER_TYPE_NN = 4;
    /**
     * 抢庄类型-通比
     */
    public static final int NN_BANKER_TYPE_TB = 5;

    /**
     * 无人抢庄-随机庄家
     */
    public static final int NN_QZ_NO_BANKER_SJ = 1;
    /**
     * 无人抢庄-解散房间
     */
    public static final int NN_QZ_NO_BANKER_JS = -1;
    /**
     * 无人抢庄-重开局
     */
    public static final int NN_QZ_NO_BANKER_CK = 2;

    /**
     * 准备超时-不处理
     */
    public static final int NN_READY_OVERTIME_NOTHING = 0;
    /**
     * 准备超时-自动准备
     */
    public static final int NN_READY_OVERTIME_AUTO = 1;
    /**
     * 准备超时-踢出房间
     */
    public static final int NN_READY_OVERTIME_OUT = 2;

    /**
     * 牛牛游戏状态-初始
     */
    public static final int NN_GAME_STATUS_INIT=0;
    /**
     * 牛牛游戏状态-准备
     */
    public static final int NN_GAME_STATUS_READY=1;
    /**
     * 牛牛游戏状态-抢庄
     */
    public static final int NN_GAME_STATUS_QZ=2;
    /**
     * 牛牛游戏状态-定庄
     */
    public static final int NN_GAME_STATUS_DZ=3;
    /**
     * 牛牛游戏状态-下注
     */
    public static final int NN_GAME_STATUS_XZ=4;
    /**
     * 牛牛游戏状态-亮牌
     */
    public static final int NN_GAME_STATUS_LP=5;
    /**
     * 牛牛游戏状态-结算
     */
    public static final int NN_GAME_STATUS_JS=6;
    /**
     * 牛牛游戏状态-总结算
     */
    public static final int NN_GAME_STATUS_ZJS=7;

    /**
     * 玩家状态-初始
     */
    public static final int NN_USER_STATUS_INIT=0;
    /**
     * 玩家状态-准备
     */
    public static final int NN_USER_STATUS_READY=1;
    /**
     * 玩家状态-抢庄
     */
    public static final int NN_USER_STATUS_QZ=2;
    /**
     * 玩家状态-定庄
     */
    public static final int NN_USER_STATUS_DZ=3;
    /**
     * 玩家状态-下注
     */
    public static final int NN_USER_STATUS_XZ=4;
    /**
     * 玩家状态-亮牌
     */
    public static final int NN_USER_STATUS_LP=5;
    /**
     * 玩家状态-结算
     */
    public static final int NN_USER_STATUS_JS=6;
    /**
     * 玩家状态-总结算
     */
    public static final int NN_USER_STATUS_ZJS=7;

    /**
     * 牛牛最少开始人数
     */
    public static final int NN_MIN_START_COUNT = 2;
    /**
     * 牛牛游戏倒计时-初始
     */
    public static final int NN_TIMER_INIT = 0;
    /**
     * 牛牛游戏倒计时-准备
     */
    public static final int NN_TIMER_READY = 10;
    /**
     * 牛牛游戏倒计时-抢庄
     */
    public static final int NN_TIMER_QZ = 15;
    /**
     * 牛牛游戏倒计时-下注
     */
    public static final int NN_TIMER_XZ = 10;
    /**
     * 牛牛游戏倒计时-亮牌
     */
    public static final int NN_TIMER_SHOW = 15;

    public static final String DATA_KEY_ROOM_NO = "room_no";
    public static final String DATA_KEY_ACCOUNT = "account";
    public static final String DATA_KEY_VALUE = "value";
    public static final String DATA_KEY_MONEY = "money";

    /**
     * 检查客户端数据是否正确，是否符合当前游戏状态
     * @param postData
     * @param gameStatus
     * @return
     */
    public static boolean checkEvent(JSONObject postData, int gameStatus){
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
