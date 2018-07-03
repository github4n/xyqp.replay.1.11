package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DdzConstant;
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
 * @DESCRIPTION
 * @Date Created in 17:01 2018/7/3
 * @Modified By:
 **/
@Component
public class GameTimerDdz {



    private final static Logger logger = LoggerFactory.getLogger(GameTimerDdz.class);

    @Resource
    private Destination ddzQueueDestination;

    @Resource
    private ProducerService producerService;

    /**
     * 出牌超时
     * @param roomNo
     * @param nextAccount
     * @param timeLeft
     */
    public void gameEventOverTime(String roomNo,String nextAccount,int timeLeft){
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 非操作
                if (room.getFocusIndex()!=room.getPlayerMap().get(nextAccount).getMyIndex()) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                // 托管状态自动出牌
                if (i==timeLeft-1&&room.getUserPacketMap().get(nextAccount).getIsTrustee()==CommonConstant.GLOBAL_YES) {
                    JSONObject data = new JSONObject();
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,nextAccount);
                    producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_AUTO_PLAY));
                    break;
                }
                // 倒计时到了之后执行事件
                if (i==0&&room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                    JSONObject data = new JSONObject();
                    data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                    data.put(CommonConstant.DATA_KEY_ACCOUNT,nextAccount);
                    data.put(DdzConstant.DDZ_DATA_KEY_TYPE,CommonConstant.GLOBAL_YES);
                    producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_TRUSTEE));
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
     * 抢地主超时
     * @param roomNo
     * @param focus
     * @param type
     * @param timeLeft
     */
    public void gameRobOverTime(String roomNo,int focus,int type,int timeLeft){
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 非操作
                if (room.getFocusIndex()!=focus) {
                    break;
                }
                // 设置倒计时
                room.setTimeLeft(i);
                // 倒计时到了之后执行事件
                if (i==0) {
                    for (String account : room.getPlayerMap().keySet()) {
                        if (room.getPlayerMap().get(account).getMyIndex()==focus) {
                            JSONObject data = new JSONObject();
                            data.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
                            data.put(CommonConstant.DATA_KEY_ACCOUNT,account);
                            data.put(DdzConstant.DDZ_DATA_KEY_TYPE,type);
                            data.put(DdzConstant.DDZ_DATA_KEY_IS_CHOICE,CommonConstant.GLOBAL_NO);
                            producerService.sendMessage(ddzQueueDestination, new Messages(null, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CALL_AND_ROB));
                            break;
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
                DdzGameRoom room = (DdzGameRoom)RoomManage.gameRoomMap.get(roomNo);
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
                        producerService.sendMessage(ddzQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_DDZ, DdzConstant.DDZ_GAME_EVENT_CLOSE_ROOM));
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
