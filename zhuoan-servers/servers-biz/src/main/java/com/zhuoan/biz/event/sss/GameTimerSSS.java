package com.zhuoan.biz.event.sss;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION 十三水游戏定时器
 * @Date Created in 20:08 2018/4/23
 * @Modified By:
 **/
@Component
public class GameTimerSSS {

    private final static Logger logger = LoggerFactory.getLogger(GameTimerSSS.class);

    @Resource
    private Destination sssQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * 游戏超时事件
     * @param roomNo
     * @param gameStatus
     */
    public void gameOverTime(String roomNo,int gameStatus,int timeLeft){
        // 玩家状态
        int userStatus = SSSConstant.SSS_USER_STATUS_INIT;
        switch (gameStatus) {
            case SSSConstant.SSS_GAME_STATUS_READY:
                userStatus = SSSConstant.SSS_USER_STATUS_READY;
                break;
            case SSSConstant.SSS_GAME_STATUS_GAME_EVENT:
                userStatus = SSSConstant.SSS_USER_STATUS_GAME_EVENT;
                break;
            case SSSConstant.SSS_GAME_STATUS_XZ:
                userStatus = SSSConstant.SSS_USER_STATUS_XZ;
                break;
            default:
                break;
        }
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                SSSGameRoomNew room = (SSSGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
                // 非当前游戏状态停止定时器
                if (room.getGameStatus()!=gameStatus) {
                    break;
                }
                // 准备状态需要检查当前准备人数是否大于最低开始人数
                if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY&&room.getNowReadyCount()<room.getMinPlayer()) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        // 除准备阶段以外不需要判断中途加入的玩家
                        if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY||room.getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                            if (room.getUserPacketMap().get(account).getStatus()!=userStatus) {
                                autoAccountList.add(account);
                            }
                        }
                        if (room.getBankerType() == SSSConstant.SSS_BANKER_TYPE_ZZ&&gameStatus==SSSConstant.SSS_GAME_STATUS_READY) {
                            if (autoAccountList.contains(account)&&account.equals(room.getBanker())) {
                                autoAccountList.remove(account);
                            }
                        }
                    }
                    // 投递消息类型
                    int messageSort = 0;
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        if (gameStatus==SSSConstant.SSS_GAME_STATUS_READY) {
                            // 准备阶段超时踢出
                            if (room.getReadyOvertime()==CommonConstant.READY_OVERTIME_OUT) {
                                messageSort = SSSConstant.SSS_GAME_EVENT_EXIT;
                            }
                        }
                        if (gameStatus==SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
                            messageSort = SSSConstant.SSS_GAME_EVENT_EVENT;
                            // 自动配牌
                            data.put("type",1);
                        }
                        if (gameStatus==SSSConstant.SSS_GAME_STATUS_XZ) {
                            messageSort = SSSConstant.SSS_GAME_EVENT_XZ;
                            // 自动下注
                            data.put("money",1);
                        }
                        if (messageSort>0) {
                            SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                            producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, messageSort));
                        }
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("",e);
                }
            }else {
                break;
            }
        }
    }

    /**
     * 解散超时
     * @param roomNo
     * @param timeLeft
     */
    public void closeRoomOverTime(String roomNo,int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                SSSGameRoomNew room = (SSSGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
                if (room.getJieSanTime()==0) {
                    break;
                }
                // 设置倒计时
                room.setJieSanTime(i);
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().get(account).getIsCloseRoom() == CommonConstant.CLOSE_ROOM_UNSURE) {
                            autoAccountList.add(account);
                        }
                    }
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        // 同意解散
                        data.put("type",CommonConstant.CLOSE_ROOM_AGREE);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(sssQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_SSS, SSSConstant.SSS_GAME_EVENT_CLOSE_ROOM));
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("",e);
                }
            }else {
                break;
            }
        }
    }
}
