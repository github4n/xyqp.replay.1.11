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
     * 公共游戏事件-获取玩家信息
     */
    public static final int BASE_GAME_GET_USER_INFO = 1;
    /**
     * 公共游戏事件-检查是否在房间内
     */
    public static final int BASE_GAME_EVENT_CHECK_USER = 2;
    /**
     * 公共游戏事件-获取游戏设置
     */
    public static final int BASE_GAME_EVENT_GET_GAME_SETTING = 3;
    /**
     * 公共游戏事件-获取房间列表
     */
    public static final int BASE_GAME_EVENT_GET_ALL_ROOM_LIST = 4;
    /**
     * 公共游戏事件-创建房间
     */
    public static final int BASE_GAME_EVENT_CREATE_ROOM = 5;
    /**
     * 公共游戏事件-加入房间
     */
    public static final int BASE_GAME_EVENT_JOIN_ROOM = 6;
    /**
     * 公共游戏事件-获取洗牌信息
     */
    public static final int BASE_GAME_EVENT_GET_SHUFFLE_INFO = 7;
    /**
     * 公共游戏事件-洗牌
     */
    public static final int BASE_GAME_EVENT_DO_SHUFFLE = 8;
    /**
     * 公共游戏事件-发送聊天信息
     */
    public static final int BASE_GAME_EVENT_SEND_MESSAGE = 9;
    /**
     * 公共游戏事件-发送语音
     */
    public static final int BASE_GAME_EVENT_SEND_VOICE = 10;
    /**
     * 公共游戏事件-获取玩家战绩
     */
    public static final int BASE_GAME_EVENT_GET_USER_GAME_LOGS = 11;
    /**
     * 公共游戏事件-解散房间
     */
    public static final int BASE_GAME_EVENT_DISSOLVE_ROOM = 12;
    /**
     * 公共游戏事件-开关游戏
     */
    public static final int BASE_GAME_EVENT_ON_OR_OFF_GAME = 13;
    /**
     * 公共游戏事件-发送滚动公告
     */
    public static final int BASE_GAME_EVENT_SEND_NOTICE = 14;
    /**
     * 公共游戏事件-获取滚动公告
     */
    public static final int BASE_GAME_EVENT_GET_NOTICE = 15;
    /**
     * 公共游戏事件-获取房间数、玩家数
     */
    public static final int BASE_GAME_EVENT_GET_ROOM_AND_PLAYER_COUNT = 16;
    /**
     * 公共游戏事件-测试接口
     */
    public static final int BASE_GAME_EVENT_TEST = 17;
    /**
     * 公共游戏事件-子游戏接口
     */
    public static final int BASE_GAME_EVENT_SON_GAME  = 18;
    /**
     * 公共游戏事件-金币场加入房间
     */
    public static final int BASE_GAME_EVENT_JOIN_COIN_ROOM  = 19;
    /**
     * 公共游戏事件-金币场加入房间
     */
    public static final int BASE_GAME_EVENT_GET_ROOM_CARD_PAY_INFO  = 20;
    /**
     * 游戏公共部分
     */
    public static final int GAME_BASE = 0;
    /**
     * 游戏id-牛牛
     */
    public static final int GAME_ID_NN = 1;
    /**
     * 游戏id-十三水
     */
    public static final int GAME_ID_SSS = 4;
    /**
     * 游戏id-炸金花
     */
    public static final int GAME_ID_ZJH = 6;
    /**
     * 游戏id-比大小
     */
    public static final int GAME_ID_BDX = 10;
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
     * 支付类型-房主支付
     */
    public static final int PAY_TYPE_OWNER = 0;
    /**
     * 支付类型-AA
     */
    public static final int PAY_TYPE_AA = 1;

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
    /**
     * 公告类型-大厅
     */
    public static final int NOTICE_TYPE_MALL = 1;
    /**
     * 公告类型-游戏内
     */
    public static final int NOTICE_TYPE_GAME = 2;
    /**
     * 提示消息类型-普通提示
     */
    public static final int SHOW_MSG_TYPE_NORMAL = 0;
    /**
     * 提示消息类型-小弹窗
     */
    public static final int SHOW_MSG_TYPE_SMALL = 1;
    /**
     * 提示消息类型-大弹窗
     */
    public static final int SHOW_MSG_TYPE_BIG = 2;
    /**
     * 积分变动类型-抽水
     */
    public static final int SCORE_CHANGE_TYPE_PUMP = 10;
    /**
     * 积分变动类型-游戏输赢
     */
    public static final int SCORE_CHANGE_TYPE_GAME = 20;
    /**
     * 积分变动类型-洗牌
     */
    public static final int SCORE_CHANGE_TYPE_SHUFFLE = 30;
    /**
     * 无庄家
     */
    public static final int NO_BANKER_INDEX = -1;

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
