package com.zhuoan.biz.event.gppj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.gppj.GPPJGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.GPPJConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import net.sf.json.JSONArray;
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
 * @DESCRIPTION
 * @Date Created in 15:57 2018/6/11
 * @Modified By:
 **/
@Component
public class GameTimerGPPJ {

    private final static Logger logger = LoggerFactory.getLogger(GameTimerGPPJ.class);

    @Resource
    private Destination gppjQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private GPPJGameEventDeal gppjGameEventDeal;

    /**
     * 游戏超时事件
     * @param roomNo
     * @param gameStatus
     */
    public void gameOverTime(String roomNo,int gameStatus,int userStatus,int timeLeft,int sleepType){
        if (sleepType==GPPJConstant.SLEEP_TYPE_START_GAME) {
            try {
                Thread.sleep(GPPJConstant.SLEEP_TIME_START_GAME);
                // 切牌完成通知玩家
                gppjGameEventDeal.changeGameStatus(roomNo);
            } catch (Exception e) {
                logger.error("",e);
            }
        }
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
                // 非当前游戏状态停止定时器
                if (room.getGameStatus()!=gameStatus) {
                    break;
                }
                // 准备状态需要检查当前准备人数是否大于最低开始人数
                if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_READY&&gppjGameEventDeal.obtainNowReadyCount(roomNo)<GPPJConstant.GP_PJ_MIN_START_COUNT) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                // 通知前端
                JSONObject result = new JSONObject();
                result.put("time",i);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(result),"timePush_GPPJ");
                // 倒计时到了之后执行事件
                if (i==0) {
                    // 当前阶段所有未完成操作的玩家
                    List<String> autoAccountList = new ArrayList<String>();
                    for (String account : room.getUserPacketMap().keySet()) {
                        // 除准备阶段以外不需要判断中途加入的玩家
                        if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_READY||room.getUserPacketMap().get(account).getStatus()!=GPPJConstant.GP_PJ_USER_STATUS_INIT) {
                            if (room.getUserPacketMap().get(account).getStatus()!=userStatus) {
                                autoAccountList.add(account);
                            }
                        }
                    }
                    // 投递消息类型
                    int messageSort = 0;
                    for (String account : autoAccountList) {
                        // 组织数据
                        JSONObject data = new JSONObject();
                        // 房间号
                        data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                        // 账号
                        data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                        if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_CUT) {
                            messageSort = GPPJConstant.GP_PJ_GAME_EVENT_CUT;
                            // 切牌超时默认不切
                            data.put(GPPJConstant.DATA_KEY_CUT_PLACE,-1);
                        }
                        if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_QZ) {
                            messageSort = GPPJConstant.GP_PJ_GAME_EVENT_QZ;
                            // 抢庄超时默认不抢
                            data.put(GPPJConstant.DATA_KEY_QZ_TIMES,0);
                        }
                        if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_XZ) {
                            messageSort = GPPJConstant.GP_PJ_GAME_EVENT_XZ;
                            // 下注阶段默认下最小倍数
                            JSONArray baseNum = JSONArray.fromObject(room.getBaseNum());
                            data.put(GPPJConstant.DATA_KEY_XZ_TIMES,baseNum.getJSONObject(0).getInt("val"));
                        }
                        if (gameStatus==GPPJConstant.GP_PJ_GAME_STATUS_SHOW) {
                            messageSort = GPPJConstant.GP_PJ_GAME_EVENT_SHOW;
                        }
                        if (messageSort>0) {
                            SocketIOClient client = GameMain.server.getClient(room.getPlayerMap().get(account).getUuid());
                            producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, messageSort));
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
                GPPJGameRoom room = (GPPJGameRoom)RoomManage.gameRoomMap.get(roomNo);
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
                        producerService.sendMessage(gppjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_GP_PJ, GPPJConstant.GP_PJ_GAME_EVENT_CLOSE_ROOM));
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
