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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final static Logger logger = LoggerFactory.getLogger(GameTimerZJH.class);

    @Resource
    private Destination zjhQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * 准备超时事件
     * @param roomNo
     * @param gameStatus
     */
    public void readyOverTime(String roomNo,int gameStatus, int time){
        for (int i = time; i >= 0; i--) {
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
                        if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                            // 除准备阶段以外不需要判断中途加入的玩家
                            if (gameStatus==ZJHConstant.ZJH_GAME_STATUS_READY) {
                                if (room.getUserPacketMap().get(account).getStatus()!=ZJHConstant.ZJH_USER_STATUS_READY) {
                                    autoAccountList.add(account);
                                }
                            }
                        }
                    }
                    // 准备阶段超时踢出
                    if (room.getReadyOvertime()==CommonConstant.READY_OVERTIME_OUT) {
                        for (String account : autoAccountList) {
                            // 组织数据
                            JSONObject data = new JSONObject();
                            // 房间号
                            data.put(CommonConstant.DATA_KEY_ROOM_NO,room.getRoomNo());
                            // 账号
                            data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                            SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                            producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_EXIT));
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

    public void gameOverTime(String roomNo,int gameStatus,String account, int time){
        for (int i = time; i >= 0; i--) {
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
                if (i == time-1) {
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
                        producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_GAME));
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
                    producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_GAME));
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
                ZJHGameRoomNew room = (ZJHGameRoomNew)RoomManage.gameRoomMap.get(roomNo);
                if (room.getJieSanTime()==0) {
                    break;
                }
                // 设置倒计时
                room.setJieSanTime(i);
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                            if (room.getUserPacketMap().get(account).getIsCloseRoom() == CommonConstant.CLOSE_ROOM_UNSURE) {
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
                        // 同意解散
                        data.put("type",CommonConstant.CLOSE_ROOM_AGREE);
                        SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                        producerService.sendMessage(zjhQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_ZJH, ZJHConstant.ZJH_GAME_EVENT_CLOSE_ROOM));
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
