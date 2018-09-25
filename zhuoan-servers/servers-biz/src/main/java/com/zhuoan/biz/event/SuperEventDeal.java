package com.zhuoan.biz.event;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.serializing.SerializingUtil;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author wqm
 * @Date Created in 21:30 2018/9/23
 **/
@Component
public class SuperEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(SuperEventDeal.class);

    protected static final String ROOM_KEY = "all_room_obj";

    @Resource
    private RedisService redisService;

    /**
     * 序列化
     *
     * @param room room
     */
    protected void roomSerializable(GameRoom room) {
        try {
            // null判断
            if (room != null) {
                String roomNo = room.getRoomNo();
                // 序列化
                String serStr = SerializingUtil.objectSerializable(room);
                // 存入缓存
                redisService.hset(ROOM_KEY, roomNo, serStr);
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    /**
     * 反序列化
     *
     * @param roomNo roomNo
     * @return GameRoom
     */
    protected GameRoom roomDeserializable(String roomNo) {
        try {
            // 序列化后的对象
            Object serObj = redisService.hget(ROOM_KEY, roomNo);
            if (serObj != null) {
                return (GameRoom) SerializingUtil.objectDeserialization(String.valueOf(serObj));
            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }

    /**
     * 移除房间
     *
     * @param roomNo roomNo
     */
    protected void removeRoom(String roomNo) {
        redisService.hdel(ROOM_KEY, roomNo);
    }

    /**
     * 检查客户端传递数据是否正确
     *
     * @param postData   postData
     * @param gameStatus gameStatus
     * @param client     client
     * @return boolean
     */
    protected boolean checkGameEvent(JSONObject postData, int gameStatus, SocketIOClient client) {
        // 所有游戏数据必须包含用户account和房间号
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO) || !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
            return false;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        // 账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 房间不存在或房间为空
        if (Dto.stringIsNULL(roomNo)) {
            return false;
        }
        GameRoom gameRoom = roomDeserializable(roomNo);
        // 房间为空
        if (gameRoom == null) {
            return false;
        }
        // 玩家不在房间内
        if (Dto.stringIsNULL(account) || !gameRoom.getPlayerMap().containsKey(account) || gameRoom.getPlayerMap().get(account) == null) {
            return false;
        }
        // client对象不为空验证是否为该玩家发的消息
        if (client != null) {
            if (!client.getSessionId().equals(gameRoom.getPlayerMap().get(account).getUuid())) {
                return false;
            }
        }
        // 当前非该游戏阶段
        return gameStatus == CommonConstant.CHECK_GAME_STATUS_NO || gameRoom.getGameStatus() == gameStatus;
    }

    /**
     * 发送失败提示信息
     *
     * @param client    client
     * @param code      code
     * @param msg       msg
     * @param eventName eventName
     */
    public void sendPromptToSinglePlayer(SocketIOClient client, int code, String msg, String eventName) {
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, code);
        result.put(CommonConstant.RESULT_KEY_MSG, msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

}
