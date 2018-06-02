package com.zhuoan.biz.event.qzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.UserPacketQZMJ;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;
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
 * @Date Created in 14:11 2018/5/23
 * @Modified By:
 **/
@Component
public class GameTimerQZMJ {

    private final static Logger logger = LoggerFactory.getLogger(GameTimerQZMJ.class);

    @Resource
    private Destination qzmjQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private QZMJGameEventDeal qzmjGameEventDeal;

    public void gameStart(String roomNo, int firstStatus) {
        if (firstStatus==QZMJConstant.QZ_START_STATUS_DICE) {
            doDice(roomNo);
        }else if (firstStatus==QZMJConstant.QZ_START_STATUS_CHECK_IP) {
            checkIp(roomNo);
        }
    }


    private void changeStartStatus(String roomNo,int startStatus) {
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
            if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_ING) {
                room.setStartStatus(startStatus);
                if (startStatus!=-1) {
                    sendChangeData(roomNo,startStatus);
                }
            }
        }
    }

    private void sendChangeData(String roomNo,int startStatus) {
        QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
        if(startStatus==4){
            List<String> cliTagList = new ArrayList<String>();
            int playerCount = 0;
            String next = room.getBanker();
            while(playerCount < room.getPlayerCount()){
                UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(next);
                // 需要补花
                if(QZMJConstant.hasHuaPai(userPacketQZMJ.getMyPai().toArray())){
                    cliTagList.add(next);
                }
                next = room.getNextPlayer(next);
                playerCount++;
            }

            // 按顺序补花
            for (String clientTag : cliTagList) {
                // 玩家间补花时间间隔1000ms
                if(!clientTag.equals(cliTagList.get(0))){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                }

                JSONArray huavals = new JSONArray();
                int buhuaType = 10;
                // 需要补花
                while(buhuaType==10){

                    JSONObject data = QZMJGameEventDeal.autoBuHua(roomNo, clientTag);
                    if(data!=null){
                        buhuaType = data.getInt("type");
                        huavals.add(data);
                    }
                }

                // 通知玩家补花结果
                if(buhuaType!=10){
                    // 通知其他玩家的补花结果
                    JSONArray otherBuHua = new JSONArray();
                    for (int i = 0; i < huavals.size(); i++) {
                        JSONObject obj = huavals.getJSONObject(i);
                        JSONObject obuhua = new JSONObject();
                        obuhua.put("huaValue", obj.get("huaValue"));
                        otherBuHua.add(obuhua);
                    }
                    for (String uuid : room.getPlayerMap().keySet()) {

                        JSONObject buhua = new JSONObject();
                        buhua.put("index", room.getPlayerMap().get(clientTag).getMyIndex());
                        buhua.put("zpaishu", room.getPai().length-room.getIndex());

                        if(clientTag.equals(uuid)){
                            buhua.put("huavals", huavals);
                        }else{
                            buhua.put("huavals", otherBuHua);
                        }

                        JSONObject buhuaData = new JSONObject();
                        buhuaData.put("type", startStatus);
                        buhuaData.put("buhua", buhua);
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(buhuaData),"gameKaiJuPush");
                    }
                }
            }
        }else{ // （除补花外）其他开局流程

            for (String uuid : room.getPlayerMap().keySet()) {

                UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(uuid);
                JSONObject result = new JSONObject();
                result.put("type", startStatus);
                if(startStatus==3){
                    //发牌流程返回总牌数
                    result.put("zpaishu", room.getPai().length-room.getIndex());
                }else if(startStatus==5){
                    //返回当前最新的牌
                    result.put(QZMJConstant.myPai, userPacketQZMJ.getMyPai().toArray());
                    result.put("huaValue", userPacketQZMJ.getHuaList().toArray());
                }
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(result),"gameKaiJuPush");
            }
        }
    }

    private void checkIp(String roomNo) {
        changeStartStatus(roomNo,QZMJConstant.QZ_START_STATUS_CHECK_IP);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        doDice(roomNo);
    }

    private void doDice(String roomNo) {
        changeStartStatus(roomNo,QZMJConstant.QZ_START_STATUS_DICE);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        faPai(roomNo);
    }

    private void faPai(String roomNo) {
        changeStartStatus(roomNo,QZMJConstant.QZ_START_STATUS_FP);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_QZMJ) {
            buHua(roomNo);
        }else if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_NAMJ) {
            dingJin(roomNo);
        }
    }

    private void buHua(String roomNo) {
        changeStartStatus(roomNo,QZMJConstant.QZ_START_STATUS_BH);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_QZMJ) {
            dingJin(roomNo);
        }else if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_NAMJ) {
            moPai(roomNo);
        }
    }

    private void dingJin(String roomNo) {
        changeStartStatus(roomNo,QZMJConstant.QZ_START_STATUS_KJ);
        try {
            Thread.sleep(1250);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_QZMJ) {
            moPai(roomNo);
        }else if (RoomManage.gameRoomMap.get(roomNo).getGid()==CommonConstant.GAME_ID_NAMJ) {
            buHua(roomNo);
        }
    }

    private void moPai(String roomNo) {
        changeStartStatus(roomNo,-1);
        qzmjGameEventDeal.autoMoPai(roomNo, RoomManage.gameRoomMap.get(roomNo).getBanker());
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
                QZMJGameRoom room = (QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo);
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
                        producerService.sendMessage(qzmjQueueDestination, new Messages(client, data, CommonConstant.GAME_ID_QZMJ, QZMJConstant.QZMJ_GAME_EVENT_CLOSE_ROOM));
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
