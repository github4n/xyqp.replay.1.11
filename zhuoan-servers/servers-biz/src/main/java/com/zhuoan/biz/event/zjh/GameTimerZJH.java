package com.zhuoan.biz.event.zjh;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.zjh.ZJHGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.ZJHConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION 炸金花游戏定时器
 * @Date Created in 9:14 2018/4/26
 * @Modified By:
 **/
@Component
public class GameTimerZJH {
    @Resource
    private Destination zjhQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * 准备超时事件
     * @param roomNo
     * @param gameStatus
     */
    public void readyOverTime(String roomNo,int gameStatus){
        for (int i = ZJHConstant.ZJH_TIMER_READY; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                ZJHGameRoomNew room = (ZJHGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
                // 非当前游戏状态停止定时器
                if (room.getGameStatus()!=gameStatus) {
                    break;
                }
                // 准备状态需要检查当前准备人数是否大于最低开始人数
                if (gameStatus==ZJHConstant.ZJH_GAME_STATUS_READY&&room.getNowReadyCount()<ZJHConstant.ZJH_MIN_START_COUNT) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        // 除准备阶段以外不需要判断中途加入的玩家
                        if (gameStatus==ZJHConstant.ZJH_GAME_STATUS_READY) {
                            if (room.getUserPacketMap().get(account).getStatus()!=ZJHConstant.ZJH_USER_STATUS_READY) {
                                autoAccountList.add(account);
                            }
                        }
                    }
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 5));
                    }
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }else {
                break;
            }
        }
    }

    public void gameOverTime(String roomNo,int gameStatus,String account){
        for (int i = ZJHConstant.ZJH_TIMER_XZ; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                ZJHGameRoomNew room = (ZJHGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
                // 非当前游戏状态停止定时器
                if (room.getGameStatus()!=gameStatus) {
                    break;
                }
                // 当前非该玩家操作
                if (!room.getFocus().equals(account)) {
                    break;
                }
                if (i==ZJHConstant.ZJH_TIMER_XZ-1) {
                    // 跟到底
                    if (room.getUserPacketMap().get(account).isGenDaoDi) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        data.put("type",ZJHConstant.GAME_ACTION_TYPE_GZ);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 3));
                    }
                }
                // 设置倒计时
                room.setXzTimer(i);
                if (i==0) {
                    // 组织数据
                    JSONObject data = new JSONObject();
                    // 房间号
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                    // 账号
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                    data.put("type",ZJHConstant.GAME_ACTION_TYPE_GIVE_UP);
                    SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                    producerService.sendMessage(zjhQueueDestination, new Messages(client, data, 6, 3));
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {}
            }else {
                break;
            }
        }
    }
}
