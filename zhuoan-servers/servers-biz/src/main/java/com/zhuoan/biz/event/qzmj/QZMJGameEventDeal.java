package com.zhuoan.biz.event.qzmj;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.qzmj.MaJiangCore;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.biz.model.qzmj.DontMovePai;
import com.zhuoan.biz.model.qzmj.KaiJuModel;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.biz.model.qzmj.UserPacketQZMJ;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:44 2018/5/21
 * @Modified By:
 **/
@Component
public class QZMJGameEventDeal {

    public static int GAME_QZMJ = 1;

    /**
     * 出牌结果
     */
    private Map<String, Object[]> chuPaiJieGuo = new HashMap<String, Object[]>();

    @Resource
    private GameTimerQZMJ gameTimerQZMJ;

    @Resource
    private Destination daoQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RoomBiz roomBiz;

    /**
     * 创建房间
     * @param client
     * @param data
     */
    public void createRoom(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject roomData = obtainEnterData(roomNo, account);
        // 数据不为空
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("data", roomData);
            // 通知自己
            CommonConstant.sendMsgEventToSingle(client, result.toString(), "enterRoomPush");
        }
    }

    /**
     * 加入房间
     * @param client
     * @param data
     */
    public void joinRoom(SocketIOClient client,Object data) {
        // 进入房间通知自己
        createRoom(client, data);
        JSONObject joinData = JSONObject.fromObject(data);
        // 非重连通知其他玩家
        if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
            String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
            Playerinfo player = room.getPlayerMap().get(account);
            JSONObject obj = new JSONObject();
            obj.put("account", player.getAccount());
            obj.put("name", player.getName());
            obj.put("headimg", player.getRealHeadimg());
            obj.put("sex", player.getSex());
            obj.put("ip", player.getIp());
            obj.put("vip", player.getVip());
            obj.put("location", player.getLocation());
            obj.put("area", player.getArea());
            obj.put("score", player.getScore());
            obj.put("index", player.getMyIndex());
            obj.put("userOnlineStatus", player.getStatus());
            obj.put("ghName", player.getGhName());
            obj.put("introduction", player.getSignature());
            obj.put("userStatus", room.getUserPacketMap().get(account).getStatus());
            // 通知玩家
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), obj.toString(), "playerEnterPush");
        }
    }

    /**
     * 加载完成请求
     * @param client
     * @param data
     */
    public void loadFinish(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!postData.containsKey("type")) {
            return;
        }
        int type = postData.getInt("type");
        if (type == QZMJConstant.GAME_READY_TYPE_RECONNECT) {
            if (room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                reconnectGame(client,data);
            }else if (room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_INIT||room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_READY) {
                gameReady(client,data);
            }else {
                reconnectGame(client,data);
            }
        }else if (type == QZMJConstant.GAME_READY_TYPE_READY) {
            gameReady(client,data);
        }
    }

    /**
     * 准备
     * @param client
     * @param data
     */
    public void gameReady(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_INIT, client)&&
            !CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_READY, client)&&
            !CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (QZMJGameEventDeal.GAME_QZMJ==0) {
            postData.put("notSend",CommonConstant.GLOBAL_YES);
            exitRoom(client,postData);
            JSONObject result = new JSONObject();
            result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
            result.put(CommonConstant.RESULT_KEY_MSG,"即将停服");
            CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
            return;
        }
        // 元宝不足无法准备
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
            if (room.getPlayerMap().get(account).getScore() < room.getLeaveScore()) {
                // 清出房间
                postData.put("notSendToMe",CommonConstant.GLOBAL_YES);
                postData.put("notSend",CommonConstant.GLOBAL_YES);
                exitRoom(client,postData);
                JSONObject result = new JSONObject();
                result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                result.put(CommonConstant.RESULT_KEY_MSG,"余额不足");
                CommonConstant.sendMsgEventToSingle(client,result.toString(),"tipMsgPush");
                return;
            }
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(QZMJConstant.QZ_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != QZMJConstant.QZ_GAME_STATUS_READY) {
            room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_READY);
        }
        // 房间内所有玩家都已经完成准备且人数已满通知开始游戏,否则通知玩家准备
        if (room.isAllReady() && room.getPlayerMap().size() == room.getPlayerCount()) {
            startGame(roomNo);
        } else {
            JSONObject result = new JSONObject();
            JSONArray array = new JSONArray();
            for(String uuid:room.getUserPacketMap().keySet()){
                if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                    JSONObject obj = new JSONObject();
                    obj.put("index", room.getPlayerMap().get(uuid).getMyIndex());
                    obj.put("score", room.getPlayerMap().get(uuid).getScore());
                    if(room.getUserPacketMap().get(uuid).getStatus()==QZMJConstant.QZ_USER_STATUS_READY){
                        obj.put("ready", CommonConstant.GLOBAL_YES);
                    }else{
                        obj.put("ready", CommonConstant.GLOBAL_NO);
                    }
                    array.add(obj);
                }
            }
            result.put("data",array);
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "gameReadyPush");
        }
    }

    /**
     * 出牌
     * @param client
     * @param data
     */
    public void gameChuPai(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, QZMJConstant.QZ_GAME_STATUS_ING, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int paiSize = 3;
        int paiLeft = 2;
        if (room.getUserPacketMap().get(account).getMyPai().size()%paiSize != paiLeft) {
            return;
        }
        if (postData.containsKey("pai")) {
            //获取要出的牌
            int pai = postData.getInt("pai");
            //出牌
            //type： 0:无操作 ,1：抓 2：暗杠(抓杠)询问事件 3：自摸(抓糊)询问事件 4：吃 询问事件 5：碰 询问事件 6：杠 出牌 7：糊 询问事件  8：结算事件
            Object[] outResult = chuPai(roomNo, pai, account);
            //出牌返回
            detailDataByChuPai(outResult, roomNo);
            chuPaiJieGuo.put(roomNo, outResult);
            if(chuPaiJieGuo.containsKey(roomNo)&&!Dto.isNull(chuPaiJieGuo.get(roomNo))&&chuPaiJieGuo.get(roomNo)[0]!=null){
                String thisAskAccount = String.valueOf(chuPaiJieGuo.get(roomNo)[0]);
                if(!Dto.stringIsNULL(thisAskAccount)&&room.getPlayerMap().containsKey(thisAskAccount)&&room.getPlayerMap().get(thisAskAccount)!=null){
                    chuPaiJieGuo.get(roomNo)[0] = room.getPlayerMap().get(thisAskAccount).getAccount();
                }
            }
        }
    }

    /**
     * 游戏事件 胡杠碰吃过
     * @param client
     * @param data
     */
    public void gameEvent(SocketIOClient client,Object data) {
        JSONObject postData=JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if(postData.containsKey("type")){
            int type = -postData.getInt("type");

            switch (type) {
                case -1:
                    // 过
                    Object[] jieguo = guo(roomNo, account);
                    detailDataByGuo(jieguo, roomNo);
                    break;
                case -2:
                    // 暗杠
                    int[] gang=gang(roomNo, account, -type);

                    if(gang[0]==1){
                        // 抓杠
                        detailDataByChiGangPengHu(roomNo, -9, account, gang);
                    }else if(gang[0]==2){
                        // 明杠
                        detailDataByChiGangPengHu(roomNo, -6, account, gang);
                    }else if(gang[0]==3){
                        // 暗杠
                        detailDataByChiGangPengHu(roomNo, -2, account, gang);
                    }

                    break;
                case -3:
                    //自摸询问
                    JSONObject d = new JSONObject();
                    d.put("type", new int[]{3});
                    CommonConstant.sendMsgEventToSingle(client,String.valueOf(d),"gameActionPush");
                    break;
                case -4:
                    // 吃
                    if (postData.containsKey("chivalue")) {
                        JSONArray array = postData.getJSONArray("chivalue");
                        int[] chiValue = new int[array.size()];
                        for(int i = 0;i < array.size();i++){
                            chiValue[i] = array.getInt(i);
                        }
                        if (chi(roomNo, chiValue, account)) {
                            detailDataByChiGangPengHu(roomNo, -4, account, chiValue);
                        }
                    }
                    break;
                case -5:
                    // 碰
                    if (peng(roomNo, account)) {
                        detailDataByChiGangPengHu(roomNo, -5, account, null);
                    }
                    break;
                case -6:
                    //明杠
                    int[] mgang=gang(roomNo, account, -type);

                    if(mgang[0]==1){
                        // 抓杠
                        detailDataByChiGangPengHu(roomNo, -9, account, mgang);
                    }else if(mgang[0]==2){
                        // 明杠
                        detailDataByChiGangPengHu(roomNo, -6, account, mgang);
                    }else if(mgang[0]==3){
                        // 暗杠
                        detailDataByChiGangPengHu(roomNo, -2, account, mgang);
                    }
                    break;
                case -7:
                    // 胡
                    int hu=hu(roomNo, account, 7);
                    if(hu>0){
                        detailDataByChiGangPengHu(roomNo, -7, account, null);
                    }
                    break;
                case -8:
                    //结算事件，返回结算处理结果
                    sendSummaryData(roomNo);
                    break;
                case -9:
                    // 抓明杠
                    int[] bgang=gang(roomNo, account, -type);

                    if(bgang[0]==1){
                        //抓杠
                        detailDataByChiGangPengHu(roomNo, -9, account, bgang);
                    }else if(bgang[0]==2){
                        //明杠
                        detailDataByChiGangPengHu(roomNo, -6, account, bgang);
                    }else if(bgang[0]==3){
                        //暗杠
                        detailDataByChiGangPengHu(roomNo, -2, account, bgang);
                    }else if(bgang[0]==-1){
                        //有玩家可以抢杠胡
                        int myIndex = bgang[1];
                        for (String uuid : room.getPlayerMap().keySet()) {
                            if (room.getPlayerMap().containsKey(uuid)&&room.getPlayerMap().get(uuid)!=null) {
                                // 询问玩家是否抢杠
                                if(room.getPlayerMap().get(uuid).getMyIndex()==myIndex){
                                    JSONObject qgobj = new JSONObject();
                                    qgobj.put("type", new int[]{3});
                                    qgobj.put("huType",QZMJConstant.HU_TYPE_QGH);
                                    qgobj.put(QZMJConstant.value, new int[]{room.getLastMoPai()});
                                    CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(qgobj),"gameChupaiPush");
                                    break;
                                }
                            }
                        }
                    }
                    break;
                case -10:
                    // 补花
                    buHua(roomNo, account);
                    break;
                case -11:
                    //自摸
                    if (room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_ING) {
                        int zimo=hu(roomNo, account, 3);
                        if(zimo>0){
                            detailDataByChiGangPengHu(roomNo, -3, account, null);
                        }
                        break;
                    }
                default:
                    break;
            }
        }
    }

    /**
     * 杠出牌事件
     * @param client
     * @param data
     */
    public void gangChupaiEvent(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        // 房间号
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        //玩家抓牌
        JSONArray mjieguo = moPai(roomNo, account);
        detailDataByZhuaPai(mjieguo, room, null);
    }

    /**
     * 退出房间
     * @param client
     * @param data
     */
    public void exitRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (!Dto.stringIsNULL(account) && room.getUserPacketMap().containsKey(account) && room.getUserPacketMap().get(account) != null) {
            boolean canExit = false;
            // 金币场、元宝场
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_JB || room.getRoomType() == CommonConstant.ROOM_TYPE_YB) {
                // 未参与游戏可以自由退出
                if (room.getUserPacketMap().get(account).getStatus() == QZMJConstant.QZ_USER_STATUS_INIT) {
                    canExit = true;
                } else if (room.getGameStatus() != QZMJConstant.QZ_GAME_STATUS_ING) {
                    // 初始及准备阶段可以退出
                    canExit = true;
                }
            }else if (room.getRoomType() == CommonConstant.ROOM_TYPE_FK) {
                if (room.getUserPacketMap().get(account).getPlayTimes()==0) {
                    if (room.getPayType()==CommonConstant.PAY_TYPE_AA||!room.getOwner().equals(account)) {
                        canExit = true;
                    }
                }
                if (room.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
                    canExit = true;
                }
            }
            Playerinfo player = room.getPlayerMap().get(account);
            if (canExit) {
                List<UUID> allUUIDList = room.getAllUUIDList();
                // 更新数据库
                JSONObject roomInfo = new JSONObject();
                roomInfo.put("room_no", room.getRoomNo());
                if (room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                    roomInfo.put("user_id" + room.getPlayerMap().get(account).getMyIndex(), 0);
                }
                // 移除数据
                for (int i = 0; i < room.getUserIdList().size(); i++) {
                    if (room.getUserIdList().get(i) == room.getPlayerMap().get(account).getId()) {
                        room.getUserIdList().set(i, 0L);
                        break;
                    }
                }
                room.getPlayerMap().remove(account);
                room.getUserPacketMap().remove(account);
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                result.put("type", 1);
                result.put("index", player.getMyIndex());
                result.put("showTimer", CommonConstant.GLOBAL_NO);
                result.put("timer", room.getTimeLeft());
                if (!postData.containsKey("notSend")) {
                    CommonConstant.sendMsgEventToAll(allUUIDList, result.toString(), "exitRoomPush");
                }
                if (postData.containsKey("notSendToMe")) {
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "exitRoomPush");
                }
                // 房间内所有玩家都已经完成准备且人数大于两人通知开始游戏
                if (room.isAllReady() && room.getPlayerMap().size() == room.getPlayerCount()) {
                    startGame(roomNo);
                }
                // 所有人都退出清除房间数据
                if (room.getPlayerMap().size() == 0) {
                    roomInfo.put("status",room.getIsClose());
                    roomInfo.put("game_index",room.getGameIndex());
                    RoomManage.gameRoomMap.remove(room.getRoomNo());
                }
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
            } else {
                // 组织数据，通知玩家
                JSONObject result = new JSONObject();
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "游戏中无法退出");
                result.put("showTimer", CommonConstant.GLOBAL_NO);
                result.put("timer", room.getTimeLeft());
                result.put("type", 1);
                CommonConstant.sendMsgEventToSingle(client, result.toString(), "exitRoomPush");
            }
        }
    }

    /**
     * 解散房间
     * @param client
     * @param data
     */
    public void closeRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!CommonConstant.checkEvent(postData, CommonConstant.CHECK_GAME_STATUS_NO, client)) {
            return;
        }
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
            return;
        }
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (postData.containsKey("type")) {
            JSONObject result = new JSONObject();
            int type = postData.getInt("type");
            // 有人发起解散设置解散时间
            if (type == CommonConstant.CLOSE_ROOM_AGREE && room.getJieSanTime() == 0) {
                room.setJieSanTime(60);
                ThreadPoolHelper.executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        gameTimerQZMJ.closeRoomOverTime(roomNo,60);
                    }
                });
            }
            // 设置解散状态
            room.getUserPacketMap().get(account).setIsCloseRoom(type);
            // 有人拒绝解散
            if (type == CommonConstant.CLOSE_ROOM_DISAGREE) {
                // 重置解散
                room.setJieSanTime(0);
                // 设置玩家为未确认状态
                for (String uuid : room.getUserPacketMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        room.getUserPacketMap().get(uuid).setIsCloseRoom(CommonConstant.CLOSE_ROOM_UNSURE);
                    }
                }
                // 通知玩家
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                String[] names = {room.getPlayerMap().get(account).getName()};
                result.put("names", names);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush");
                return;
            }
            if (type == CommonConstant.CLOSE_ROOM_AGREE) {
                // 全部同意解散
                if (room.isAgreeClose()) {
                    // 未玩完一局不需要强制结算
                    if (room.getGameIndex()<=1&&Dto.isObjNull(room.getSummaryData())) {
                        // 所有玩家
                        List<UUID> uuidList = room.getAllUUIDList();
                        // 更新数据库
                        JSONObject roomInfo = new JSONObject();
                        roomInfo.put("room_no",room.getRoomNo());
                        roomInfo.put("status",room.getIsClose());
                        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_ROOM_INFO, roomInfo));
                        // 移除房间
                        RoomManage.gameRoomMap.remove(roomNo);
                        // 通知玩家
                        result.put("type",CommonConstant.SHOW_MSG_TYPE_BIG);
                        result.put(CommonConstant.RESULT_KEY_MSG,"成功解散房间");
                        CommonConstant.sendMsgEventToAll(uuidList,result.toString(),"tipMsgPush");
                        return;
                    }
                    room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY);
                    // 清除原有结算数据
                    room.getSummaryData().clear();
                    // 强制结算
                    JSONObject backObj = obtainSummaryObject(roomNo);
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(backObj),"gameJieSuanPush");
                } else {
                    // 刷新数据
                    room.getUserPacketMap().get(account).setIsCloseRoom(CommonConstant.CLOSE_ROOM_AGREE);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put("data", room.getCloseRoomData());
                    CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), result.toString(), "closeRoomPush");
                }
            }
        }
    }

    /**
     * 游戏中重连数据
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainIngReconnectData(String roomNo,String account) {
        QZMJGameRoom game = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Playerinfo player = game.getPlayerMap().get(account);
        UserPacketQZMJ userPacketQZMJ = game.getUserPacketMap().get(account);
        // 返回给玩家当前牌局信息（基础信息）
        JSONObject result=new JSONObject();
        //返回庄家的位置
        result.put(QZMJConstant.zhuang, game.getPlayerIndex(game.getBanker()));
        result.put("lianzhuang", game.getBankerTimes());
        //返回玩家的状态
        result.put(QZMJConstant.status, game.getUserPacketMap().get(account).getStatus());
        //返回游戏的底分
        result.put(QZMJConstant.soure, game.getScore());
        //返回金
        result.put(QZMJConstant.jin, game.getJin());
        result.put(QZMJConstant.myIndex, player.getMyIndex());
        result.put(QZMJConstant.myPai, game.getUserPacketMap().get(account).getMyPai().toArray());
        //返回骰子点数
        result.put("dice", game.getDice());
        result.put("jiesan", 0);
        int paishu=0;
        int shengyu=0;
        if(game.getPai()!=null){
            shengyu=game.getPai().length-game.getIndex();
            paishu=game.getPai().length;
        }
        //返回剩余牌数
        result.put(QZMJConstant.zpaishu, shengyu);
        //返回总牌数
        result.put(QZMJConstant.pai, paishu);

        // 当前游戏状态判断
        String thisUUID = game.getThisAccount();
        // 返回的type
        int[] backType = new int[]{0};
        int[] value = new int[]{};
        int focus = -1;
        int lastFoucs = -1;
        int lastPoint = -1;
        // 获取最后一次操作记录
        KaiJuModel kaijujl=game.getLastKaiJuValue(-1);

        int jltype=-1;
        if(kaijujl!=null){
            jltype = kaijujl.getType();

            if(game.getLastZhuoPaiValue()!=null){

                lastPoint = game.getLastZhuoPaiValue().getIndex();
            }
            focus = game.getPlayerIndex(thisUUID);
            lastFoucs = game.getLastFocus();

            // 三种情况：1.自己摸牌  2.别人出牌  3.自己操作完成后（吃碰杠）

            // 最后一次操作人
            if(kaijujl.getIndex()==game.getPlayerIndex(account)){

                if(jltype==2||jltype==4||jltype==5||jltype==6||jltype==9){

                    backType = new int[]{-jltype};
                    focus = player.getMyIndex();
                    lastFoucs = game.getLastFocus();

                }else if(game.getStartStatus()==-1&&(jltype==1||jltype==11)){
                    // 开局流程走完，且抓完牌（补花）->出牌

                    int pai = game.getLastMoPai();
                    // 执行摸牌处理方法，判断是否触发自摸，杠事件
                    JSONObject actData = moPaiDeal(roomNo, account, pai);
                    if(actData!=null){
                        JSONArray array = actData.getJSONArray("type");
                        backType = new int[array.size()];
                        for (int i = 0; i < array.size(); i++) {
                            backType[i] = array.getInt(i);
                        }
                        result.put("actData", actData);
                        if(actData.containsKey("tingTip")){

                            result.put("tingTip",actData.get("tingTip"));
                            // 去掉重复数据
                            actData.remove("tingTip");
                        }
                    }
                    if(pai>0){

                        value = new int[]{pai};
                        List<Integer> myPai = new ArrayList<Integer>(userPacketQZMJ.getMyPai());
                        myPai.remove((Integer)game.getLastMoPai());
                        result.put(QZMJConstant.myPai, myPai.toArray());
                        focus = player.getMyIndex();
                        lastFoucs = game.getLastFocus();
                    }
                }

            }else if(jltype==0){
                // 出牌
                if(player!=null&&chuPaiJieGuo.containsKey(roomNo)){
                    if(player.getAccount().equals(chuPaiJieGuo.get(roomNo)[0])){
                        JSONObject actData = chuPaiDeal(chuPaiJieGuo.get(roomNo), roomNo, account);
                        if(actData!=null){
                            focus = actData.getInt("focus");
                            JSONArray array = actData.getJSONArray("type");
                            backType = new int[array.size()];
                            for (int i = 0; i < array.size(); i++) {
                                backType[i] = array.getInt(i);
                            }
                        }
                        result.put("actData", actData);
                    }
                }

            }
        }

        //事件类型,当前正在进行的操作(1表示出牌 2~9表示事件询问请求,-2~-9表示事件返回结果后,等待出牌)
        result.put(QZMJConstant.type,backType);
        Playerinfo p = game.getPlayerMap().get(account);
        // type是1或者负数的时候出牌
        if(focus==p.getMyIndex()&&
            (backType==null||(backType!=null&&backType[0]<0))){

            // 牌局中剩余牌数（包含其他玩家手牌）
            List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(game.getPai(), game.getIndex()));
            for(String cliTag:game.getPlayerMap().keySet()) {
                if (game.getUserPacketMap().containsKey(cliTag)&&game.getUserPacketMap().get(cliTag)!=null) {
                    if(!account.equals(cliTag)){
                        shengyuList.addAll(game.getUserPacketMap().get(cliTag).getMyPai());
                    }
                }
            }

            // 出牌提示
            JSONArray tingTip = MaJiangCore.tingPaiTip(game.getUserPacketMap().get(account).getMyPai(), game.getJin(), shengyuList);
            result.put("tingTip",tingTip);
        }
        //当前正在操作的人
        result.put(QZMJConstant.foucs, focus);
        result.put(QZMJConstant.foucsIndex, game.getFocusIndex());
        //当type是2~9的询问事件时才需要
        result.put(QZMJConstant.lastFoucs, lastFoucs);
        //type为1或者-2 -6 -9时表示新摸的牌,如果是询问事件(2~9)就表示询问事件的牌
        result.put(QZMJConstant.value, value);
        //上一个出牌的人
        result.put(QZMJConstant.nowPoint, game.getNowPoint());
        //上上个出牌的人
        result.put(QZMJConstant.lastPoint, lastPoint);

        // 各个玩家信息
        JSONArray userInfos = new JSONArray();
        for(String cliTag:game.getPlayerMap().keySet()) {
            if (game.getUserPacketMap().containsKey(cliTag)&&game.getUserPacketMap().get(cliTag)!=null) {
                JSONObject user = new JSONObject();
                user.put("chupai", game.getChuPaiJiLu(game.getPlayerIndex(cliTag)).toString());
                user.put("hua", game.getUserPacketMap().get(cliTag).getHuaList().size());
                List<Integer> huaValue = game.getUserPacketMap().get(cliTag).getHuaList();
                user.put("huaValue", JSONArray.fromObject(huaValue));
                List<DontMovePai> dmPai = game.getUserPacketMap().get(cliTag).getHistoryPai();
                user.put("mingpai", JSONArray.fromObject(dmPai));
                user.put("paicount", game.getUserPacketMap().get(cliTag).getMyPai().size());
                user.put(QZMJConstant.myIndex, game.getPlayerMap().get(cliTag).getMyIndex());
                user.put("index", game.getPlayerMap().get(cliTag).getMyIndex());
                user.put("name", game.getPlayerMap().get(cliTag).getName());
                user.put("headimg",game.getPlayerMap().get(cliTag).getRealHeadimg());
                user.put("sex",game.getPlayerMap().get(cliTag).getSex());
                user.put("ip",game.getPlayerMap().get(cliTag).getIp());
                user.put("location",game.getPlayerMap().get(cliTag).getLocation());
                user.put("score",game.getPlayerMap().get(cliTag).getScore());
                user.put("status",game.getPlayerMap().get(cliTag).getStatus());

                int yjtype = game.getUserPacketMap().get(cliTag).getYouJinIng();
                // 光游时直接返回游金类型
                if(cliTag.equals(account) || game.isGuangYou){
                    user.put("youjin", yjtype);
                }else{
                    // 暗游时，只有双游以上才通知其他人
                    if(yjtype>1){
                        // 通知其他玩家正在双游、三游
                        user.put("youjin", yjtype);
                    }else{
                        user.put("youjin", 0);
                    }
                }

                userInfos.add(user);
            }
        }
        result.put("userInfos", userInfos);
        //返回开局状态
        result.put("kjtype", game.getStartStatus());
        result.put("game_index", game.getGameIndex());
        return result;
    }

    /**
     * 获取结算数据
     * @param roomNo
     * @return
     */
    public JSONObject obtainSummaryObject(String roomNo) {
        QZMJGameRoom gamePlay = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (!Dto.isObjNull(gamePlay.getSummaryData())) {
            return gamePlay.getSummaryData();
        }
        String winner = gamePlay.getWinner();
        JSONArray array = new JSONArray();
        // 结算时赢家排在第一个
        if(winner!=null&&gamePlay.getPlayerMap().containsKey(winner)){

            JSONObject data = new JSONObject();
            Playerinfo player = gamePlay.getPlayerMap().get(winner);
            UserPacketQZMJ userPacketQZMJ = gamePlay.getUserPacketMap().get(winner);
            // 确定赢家
            data.put("isWinner", 1);
            data.put("huType", gamePlay.getHuType());
            data.put("jin", gamePlay.getJin());
            data.put("huTimes", QZMJConstant.getHuTimes(gamePlay.getHuType(), gamePlay.getYouJinScore()));

            //获取玩家吃杠碰的牌
            List<Integer> paiList = userPacketQZMJ.getHistoryList();
            // 手牌排序
            int huPai = userPacketQZMJ.getMyPai().remove(userPacketQZMJ.getMyPai().size()-1);
            Collections.sort(userPacketQZMJ.getMyPai());
            userPacketQZMJ.getMyPai().add(huPai);
            paiList.addAll(userPacketQZMJ.getMyPai());
            data.put("myPai", paiList.toArray());
            data.put("fan", userPacketQZMJ.getFan());
            data.put("fanDetail", userPacketQZMJ.getFanDetail(userPacketQZMJ.getMyPai(), gamePlay,winner));
            data.put("score",userPacketQZMJ.getScore());
            data.put("player", player.getName());
            data.put("headimg", player.getRealHeadimg());
            data.put("hua", userPacketQZMJ.getHuaList().size());
            data.put("myIndex", player.getMyIndex());
            data.put("huaValue", JSONArray.fromObject(userPacketQZMJ.getHuaList()));
            data.put("gangValue", JSONArray.fromObject(userPacketQZMJ.getGangValue()));
            // 判断玩家是否是庄家
            if(winner.equals(gamePlay.getBanker())){
                data.put("zhuang", 1);
                // 庄家底分翻倍
                data.put("difen", gamePlay.getScore()*(2 + gamePlay.getBankerTimes()-1));
            }else{
                data.put("zhuang", 0);
                data.put("difen", gamePlay.getScore());
            }
            array.add(data);
        }

        // 标识牌局是否结束
        boolean isFinish = false;

        // 其他玩家结算
        for(String uuid:gamePlay.getPlayerMap().keySet()){
            if (gamePlay.getPlayerMap().containsKey(uuid)&&gamePlay.getPlayerMap().get(uuid)!=null) {
                Playerinfo player = gamePlay.getPlayerMap().get(uuid);
                UserPacketQZMJ userPacketQZMJ = gamePlay.getUserPacketMap().get(uuid);
                // 牌局为1课
                if(gamePlay.getGameCount()==999){
                    // 判断牌局是否结束（玩家积分是否小于等于0）
                    if(player.getScore()<=0){
                        isFinish = true;
                        if(!gamePlay.isCanOver){
                            player.setScore(0);
                            // 游戏结束
                            gamePlay.isGameOver=true;
                        }
                    }
                }

                if(!uuid.equals(winner)){
                    JSONObject data = new JSONObject();
                    data.put("isWinner", 0);
                    data.put("huType", gamePlay.getHuType());
                    data.put("jin", gamePlay.getJin());
                    data.put("huTimes", QZMJConstant.getHuTimes(gamePlay.getHuType(), gamePlay.getYouJinScore()));

                    //获取玩家吃杠碰的牌
                    List<Integer> paiList = userPacketQZMJ.getHistoryList();
                    // 手牌排序
                    Collections.sort(userPacketQZMJ.getMyPai());
                    paiList.addAll(userPacketQZMJ.getMyPai());
                    data.put("myPai", paiList.toArray());
                    data.put("fan", userPacketQZMJ.getFan());
                    data.put("fanDetail", userPacketQZMJ.getFanDetail(userPacketQZMJ.getMyPai(), gamePlay,uuid));
                    data.put("score",userPacketQZMJ.getScore());
                    data.put("player", player.getName());
                    data.put("headimg", player.getRealHeadimg());
                    data.put("hua", userPacketQZMJ.getHuaList().size());
                    data.put("myIndex", player.getMyIndex());
                    data.put("huaValue", JSONArray.fromObject(userPacketQZMJ.getHuaList()));
                    data.put("gangValue", JSONArray.fromObject(userPacketQZMJ.getGangValue()));
                    // 判断玩家是否是庄家
                    if(uuid.equals(gamePlay.getBanker())){
                        data.put("zhuang", 1);
                        // 庄家底分翻倍
                        data.put("difen", gamePlay.getScore()*(2 + gamePlay.getBankerTimes()-1));
                    }else{
                        data.put("zhuang", 0);
                        data.put("difen", gamePlay.getScore());
                    }
                    array.add(data);
                }
            }
        }
        // 返回数据
        JSONObject backObj = new JSONObject();
        // 结算类型, 0：正常结算  1：最后一局结算 2:解散结算
        backObj.put("type", 0);
        backObj.put("data", array);
        backObj.put("isLiuju", CommonConstant.GLOBAL_NO);
        if (gamePlay.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
            if(gamePlay.getGameCount()==gamePlay.getGameIndex() || isFinish){
                gamePlay.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);
                // 保存结算汇总数据
                JSONArray jiesuanArray = obtainFinalSummaryArray(gamePlay);
                backObj.put("type", 1);
                backObj.put("data1", jiesuanArray);
                gamePlay.setGameStatus(QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY);
            }else if (gamePlay.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
                // 保存结算汇总数据
                JSONArray jiesuanArray = obtainFinalSummaryArray(gamePlay);
                backObj.clear();
                backObj.put("type", 2);
                backObj.put("data1", jiesuanArray);
            }
        }
        gamePlay.setSummaryData(backObj);
        return backObj;
    }

    /**
     * 重连
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject result = new JSONObject();
        if (client == null) {
            return;
        }
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush");
            return;
        }
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        if (room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_INIT||room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_READY) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("gameStatus",room.getGameStatus());
            result.put("users",room.getAllPlayer());
        }else if (room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_ING) {
            result = obtainIngReconnectData(roomNo,account);
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("gameStatus",room.getGameStatus());
        }else if (room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_SUMMARY||room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
            result = obtainSummaryObject(roomNo);
            result.put("gameStatus",room.getGameStatus());
            result.put("users",room.getAllPlayer());
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        }
        if (room.getJieSanTime() > 0 && room.getGameStatus()!=QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY) {
            result.put("jiesan", CommonConstant.GLOBAL_YES);
            result.put("jiesanData", room.getCloseRoomData());
        }
        CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"reconnectGamePush");
    }

    public JSONObject moPaiDeal(String roomNo, String account, int pai){

        JSONArray array = new JSONArray();
        int backType=1;
        boolean back=false;
        QZMJGameRoom game = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 三游（或者光游时的双游）不能自摸，只能杠上摸
        if((game.getYjType()>2 && game.getUserPacketMap().get(account).getYouJinIng()<3)
            || (game.isGuangYou && game.hasYouJinType(2) && game.getUserPacketMap().get(account).getYouJinIng()<2)){

            game.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
        }else{

            //判断是否自摸
            back=checkIsHuByMySelf(roomNo, pai);
            if(back){
                backType=3;
            }
        }

        // 判断是否触发杠事件
        int[] gang=checkIsAnGang(roomNo, pai);
        if(gang[0]>0){
            // 有杠
            if(gang[0]==1){
                // 补杠
                backType=9;
            }else if(gang[0]==3){
                // 暗杠
                backType=2;
            }
            JSONObject obj = new JSONObject();
            obj.put("type", backType);
            obj.put("uuid", String.valueOf(account));
            obj.put("pai", pai);
            obj.put("gangValue", gang[1]);
            array.add(obj);
        }

        JSONObject obj = new JSONObject();
        obj.put("type", backType);
        obj.put("pai", pai);
        if(game.getLastMoAccount()!=null){
            // 开局补花后掉线，LastMoUUID为null
            obj.put("uuid", String.valueOf(game.getLastMoAccount()));
        }else{
            obj.put("uuid", "");
        }
        array.add(obj);

        return detailDataBymoPaiDeal(array, game, null);
    }

    public JSONObject detailDataBymoPaiDeal(JSONArray mjieguo,QZMJGameRoom game,JSONObject buhuaData){

        //解析结果数据
        String zhuaID=null;//抓牌的人
        int type=0;//抓牌人触发的事件
        int mopai=0;//摸的牌
        int gangType=0;
        int gangValue=0;

        if(mjieguo.size()>0){

            if(!mjieguo.getJSONObject(0).getString("uuid").equals("")){

                zhuaID = mjieguo.getJSONObject(0).getString("uuid");
                mopai = mjieguo.getJSONObject(0).getInt("pai");

                // 事件优先级：流局->补花->胡->杠
                if(mjieguo.size()==1){ // 没有杠事件

                    type = mjieguo.getJSONObject(0).getInt("type");

                }else if(mjieguo.getJSONObject(0).containsKey("gangValue")){ // 包含杠事件

                    // 杠事件
                    gangType = mjieguo.getJSONObject(0).getInt("type");
                    gangValue = mjieguo.getJSONObject(0).getInt("gangValue");
                    // 普通事件
                    type = mjieguo.getJSONObject(1).getInt("type");
                }
            }
        }

        int[] gangvalue=null;//杠
        if(gangType==2){
            gangvalue=new int[]{gangValue,gangValue,gangValue,gangValue};
        }
        if(gangType==9){
            gangvalue=new int[]{gangValue,gangValue,gangValue,gangValue};
        }
        Integer lastType=null;
        Object lastValue=null;
        Object lastAnValue=null;
        //获取之前事件
        KaiJuModel jilu=game.getLastValue();
        if(jilu!=null){
            if(jilu.getType()==2){//暗杠
                lastType=jilu.getType();
                lastAnValue=new int[]{jilu.getValues()[0]};
            }else if(jilu.getType()==9){//抓杠
                lastType=2;
                lastValue=new int[]{jilu.getValues()[0]};
            }else if(jilu.getType()==1){//出牌
                lastType=1;
                lastValue=jilu.getValues();
            }
        }

        JSONObject back=new JSONObject();

        if(gangType==9){

            //获取抓杠位置
            int zgindex = game.getUserPacketMap().get(zhuaID).buGangIndex(mopai);
            back.put(QZMJConstant.zgindex,zgindex);
        }
        back.put(QZMJConstant.foucs, game.getPlayerIndex(zhuaID));
        back.put(QZMJConstant.foucsIndex, game.getFocusIndex());
        back.put(QZMJConstant.nowPoint, game.getNowPoint());
        back.put(QZMJConstant.lastType,lastType);
        back.put(QZMJConstant.lastValue,lastValue);
        // 返回事件询问类型
        if(type==3&&gangType>0){

            back.put(QZMJConstant.type,new int[]{type, gangType});
        }else if(type==1&&gangType>0){
            back.put(QZMJConstant.type,new int[]{gangType});
        }else{
            back.put(QZMJConstant.type,new int[]{type});
        }
        if(!back.containsKey(QZMJConstant.lastValue)){
            back.put(QZMJConstant.lastValue,lastAnValue);
        }
        if(type==3){ // 胡牌类型
            Playerinfo player = game.getPlayerMap().get(zhuaID);
            UserPacketQZMJ userPacketQZMJ = game.getUserPacketMap().get(zhuaID);
            if(userPacketQZMJ.getMyPai().contains(game.getJin())){

                int huType=MaJiangCore.huPaiHasJin(userPacketQZMJ.getMyPai(), 0, game.getJin());
                // 满足双、三游条件
                int canYouJin = 0;
                // 当玩家有游金时判断是几游
                if(huType==QZMJConstant.HU_TYPE_YJ){

                    if(userPacketQZMJ.getYouJin()==1){
                        huType = QZMJConstant.HU_TYPE_YJ;
                        if(mopai==game.getJin() || MaJiangCore.shuangSanYouPanDing(userPacketQZMJ.getMyPai(), 0, game.getJin())){ // 可以开始双游
                            canYouJin = 2;
                        }
                    }else if(userPacketQZMJ.getYouJin()==2){
                        huType = QZMJConstant.HU_TYPE_SHY;
                        if(mopai==game.getJin() || MaJiangCore.shuangSanYouPanDing(userPacketQZMJ.getMyPai(), 0, game.getJin())){ // 可以开始三游
                            canYouJin = 3;
                        }
                    }else if(userPacketQZMJ.getYouJin()==3){
                        huType = QZMJConstant.HU_TYPE_SY;
                    }else{
                        // 三金倒
                        if(userPacketQZMJ.getPlayerJinCount(game.getJin())==3){
                            huType = QZMJConstant.HU_TYPE_SJD;
                        }else{
                            huType = QZMJConstant.HU_TYPE_ZM;
                        }
                    }
                }

                // 判断是否是天胡
                if(huType == QZMJConstant.HU_TYPE_ZM){

                    List<KaiJuModel> paiJuList = game.getKaiJuList();
                    int moPaiConut = 0;  // 摸牌次数
                    if(paiJuList!=null&&paiJuList.size()<10){

                        for (KaiJuModel kaiJu : paiJuList) {
                            if(kaiJu.getType()==1){
                                moPaiConut ++;
                            }
                        }
                        if(moPaiConut<=1){
                            huType = QZMJConstant.HU_TYPE_TH;
                        }
                    }
                }

                back.put("huType", huType);
                back.put("youjin", canYouJin);

            }else{
                back.put("huType",QZMJConstant.HU_TYPE_ZM);
                back.put("youjin", 0);
            }
        }

        if(type==1||type==3){ // 出牌或者胡

            // 牌局中剩余牌数（包含其他玩家手牌）
            List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(game.getPai(), game.getIndex()));
            for(String cliTag:game.getPlayerMap().keySet()) {
                if (game.getUserPacketMap().containsKey(cliTag)&&game.getUserPacketMap().get(cliTag)!=null) {
                    if(!zhuaID.equals(cliTag)){
                        shengyuList.addAll(game.getUserPacketMap().get(cliTag).getMyPai());
                    }
                }
            }

            // 出牌提示
            if(game.getPlayerMap().get(zhuaID)!=null){

                JSONArray tingTip = MaJiangCore.tingPaiTip(game.getUserPacketMap().get(zhuaID).getMyPai(), game.getJin(), shengyuList);
                back.put("tingTip",tingTip);
            }else{
                back.put("tingTip",new JSONArray());
            }
        }
        back.put(QZMJConstant.gangvalue,gangvalue);
        back.put(QZMJConstant.value, new int[]{mopai});

        return back;
    }

    /**
     * 出牌处理
     * @param jieguo
     * @param roomNo
     * @param thisAskAccount
     * @return
     */
    public JSONObject chuPaiDeal(Object[] jieguo,String roomNo, String thisAskAccount){

        QZMJGameRoom game= (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int type= Integer.valueOf(String.valueOf(jieguo[1]));
        //最后出的牌
        int pai=game.getLastPai();
        //上次出牌的人
        String chupai=game.getLastAccount();
        //获取总牌数
        int zpaishu=game.getPai().length-game.getIndex();
        //获取lastType
        int lastType=1;
        //获取lastValue
        int[] lastValue=new int[]{pai};
        int lastFoucs=game.getLastFocus();

        JSONObject result=new JSONObject();

        result.put(QZMJConstant.zpaishu, zpaishu);
        result.put(QZMJConstant.foucs, game.getPlayerIndex(thisAskAccount));
        result.put(QZMJConstant.foucsIndex, game.getPlayerIndex(chupai));
        result.put(QZMJConstant.nowPoint, game.getNowPoint());
        result.put(QZMJConstant.value, null);
        result.put(QZMJConstant.lastType, lastType);
        result.put(QZMJConstant.lastValue, lastValue);
        result.put(QZMJConstant.lastFoucs, lastFoucs);
        result.put(QZMJConstant.type, new int[]{type});

        if(type==7){
            //胡 询问事件
            result.put(QZMJConstant.huvalue, jieguo[2]);
        }else if(type==6){
            //杠  询问事件
            result.put(QZMJConstant.gangvalue, new Object[]{pai,pai,pai,pai});
        }else if(type==5){
            //碰  询问事件
            result.put(QZMJConstant.pengvalue, new int[]{pai,pai,pai});
        }else if(type==4){
            //吃  询问事件
            result.put(QZMJConstant.chivalue, jieguo[2]);
        }else if(type==10){
            //触发多事件询问
            JSONArray array = JSONArray.fromObject(jieguo[2]);
            int[] types = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                JSONObject obj = array.getJSONObject(i);
                if(obj.getInt("type")==7){
                    result.put(QZMJConstant.huvalue, pai);
                }else if(obj.getInt("type")==6){
                    result.put(QZMJConstant.gangvalue, new Object[]{pai,pai,pai,pai});
                }else if(obj.getInt("type")==5){
                    result.put(QZMJConstant.pengvalue, new int[]{pai,pai,pai});
                }else if(obj.getInt("type")==4){
                    result.put(QZMJConstant.chivalue, obj.get("value"));
                }
                types[i] = obj.getInt("type");
            }
            result.put(QZMJConstant.type, types);
        }
        return result;
    }

    /**
     * 结算通知
     * @param roomNo
     */
    public void sendSummaryData(String roomNo) {
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom gamePlay = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            // 游戏结束
            if(gamePlay.getGameStatus() == QZMJConstant.QZ_GAME_STATUS_SUMMARY){
                JSONObject backObj = obtainSummaryObject(roomNo);
                CommonConstant.sendMsgEventToAll(gamePlay.getAllUUIDList(),String.valueOf(backObj),"gameJieSuanPush");
                String winner = gamePlay.getWinner();
                // 判断是否连庄
                if(winner.equals(gamePlay.getBanker())){
                    gamePlay.addBankTimes();
                }else{
                    // 设置庄家，换下家当庄
                    gamePlay.setBanker(gamePlay.getNextPlayer(gamePlay.getBanker()));
                    gamePlay.setBankerTimes(1);
                }
                // 保存结算记录
                gamePlay.addKaijuList(-1, 8, new int[]{});
                updateUserScore(roomNo);
                if (gamePlay.getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
                    saveGameLog(roomNo);
                }
                if (gamePlay.getRoomType()==CommonConstant.ROOM_TYPE_YB) {
                    saveUserDeduction(roomNo);
                }
                if (gamePlay.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                    updateRoomCard(roomNo);
                }
            }
        }
    }

    /**
     * 开始游戏
     * @param roomNo
     */
    public void startGame(final String roomNo) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 确定庄家
        if (room.choiceBanker()) {
            room.initGame();
            RoomManage.gameRoomMap.get(roomNo).setGameStatus(QZMJConstant.QZ_GAME_STATUS_ING);
            // 摇骰子
            room.obtainDice();
            // 洗牌
            room.shufflePai();
            // 发牌
            room.faPai();
            // 开金
            room.choiceJin();
            if (room.getFee() > 0 && room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                JSONArray array = new JSONArray();
                for (String account : room.getPlayerMap().keySet()) {
                    if (room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
                        // 中途加入不抽水
                        if (room.getUserPacketMap().get(account).getStatus() > QZMJConstant.QZ_USER_STATUS_INIT) {
                            // 更新实体类数据
                            Playerinfo playerinfo = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account);
                            RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), room.getFee()));
                            // 负数清零
                            if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore() < 0) {
                                RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).setScore(0);
                            }
                            array.add(playerinfo.getId());
                        }
                    }
                }
                // 抽水
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getJsonObject(array)));
            }
            // 通知玩家
            for (String account : room.getPlayerMap().keySet()) {
                if (room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
                    room.getUserPacketMap().get(account).setStatus(QZMJConstant.QZ_USER_STATUS_GAME);
                    JSONObject result=new JSONObject();
                    //返回骰子点数
                    result.put("dice", room.getDice());
                    //返回庄家的位置
                    result.put(QZMJConstant.zhuang, room.getPlayerIndex(room.getBanker()));
                    //返回连庄数
                    result.put("lianzhuang", room.getBankerTimes());
                    //返回焦点的位置
                    result.put(QZMJConstant.foucs, room.getPlayerIndex(room.getBanker()));
                    result.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                    result.put(QZMJConstant.nowPoint, room.getNowPoint());
                    result.put(QZMJConstant.lastFoucs, -1);
                    //返回总牌数
                    result.put(QZMJConstant.pai, room.getPai().length);
                    result.put(QZMJConstant.zpaishu, QZMJConstant.PAI_COUNT);
                    //返回游戏的底分
                    result.put(QZMJConstant.soure, room.getScore());
                    //返回金
                    result.put(QZMJConstant.jin, room.getJin());
                    Object[] myPai = room.getUserPacketMap().get(account).getMyPai().toArray();
                    //返回发给我的牌
                    result.put(QZMJConstant.myPai, myPai);
                    //返回发给我的位置
                    result.put(QZMJConstant.myIndex, room.getPlayerMap().get(account).getMyIndex());
                    result.put("game_index", room.getGameIndex());
                    result.put("users", room.getAllPlayer());
                    CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(),String.valueOf(result),"gameStartPush");
                }
            }
            final int startStatus;
            if (room.getGameIndex()>1||room.getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                startStatus = 2;
            }else {
                startStatus = 1;
            }
            ThreadPoolHelper.executorService.submit(new Runnable() {
                @Override
                public void run() {
                    gameTimerQZMJ.gameStart(roomNo,startStatus);
                }
            });
        }
    }

    /**
     * 获取总结算数据
     * @param room
     * @return
     */
    public JSONArray obtainFinalSummaryArray(QZMJGameRoom room) {
        JSONArray fianlSummaryArray = new JSONArray();
        String gameId = room.getRoomNo();
        Set<String> uuids = room.getPlayerMap().keySet();
        // 大赢家分数
        int dayinjia = 0;
        for(String account : uuids){
            if (room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
                if(room.getPlayerMap().get(account).getScore()>=dayinjia){
                    dayinjia = (int) room.getPlayerMap().get(account).getScore();
                }
            }
        }
        // 设置玩家信息
        for(String account : uuids){
            Playerinfo player = room.getPlayerMap().get(account);
            JSONObject obj = new JSONObject();
            obj.put("name", player.getName());
            obj.put("account", player.getAccount());
            obj.put("headimg", player.getRealHeadimg());
            int score = (int) player.getScore();
            // 牌局为1课
            if(room.getGameCount()==999){
                // 一课的房间要先扣除掉之前的底分，计算真实赢得分数
                score = score - 100;
                String scoreStr = String.valueOf(score);
                if(score>0){
                    scoreStr = "+ "+score;
                }else if(score<0){
                    scoreStr = "- "+(-score);
                }
                obj.put("score", scoreStr);
            }else{
                obj.put("score", score);
            }

            if(account.equals(room.getOwner())){

                obj.put("isFangzhu", 1);
            }else{
                obj.put("isFangzhu", 0);
            }
            // 设置大赢家
            if(player.getScore() == dayinjia){
                obj.put("isWinner", 1);
            }else{
                obj.put("isWinner", 0);
            }

            // 获取胡牌类型次数
            JSONArray huTimes = getHuTypeTimes(gameId,account);

            obj.put("huTypeTimes", huTimes);

            fianlSummaryArray.add(obj);
        }
        return fianlSummaryArray;
    }

    /**
     * 获取胡牌次数
     * @param roomNo
     * @param account
     * @return
     */
    public JSONArray getHuTypeTimes(String roomNo,String account) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(account);
        // 统计牌局信息
        JSONArray array = new JSONArray();
        if(userPacketQZMJ.getPingHuTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "平胡次数        ");
            obj.put("val", userPacketQZMJ.getPingHuTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getZiMoTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "自摸次数        ");
            obj.put("val", userPacketQZMJ.getZiMoTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getSanJinDaoTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "三金倒次数    ");
            obj.put("val", userPacketQZMJ.getSanJinDaoTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getYouJinTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "游金次数        ");
            obj.put("val", userPacketQZMJ.getYouJinTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getShuangYouTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "双游次数        ");
            obj.put("val", userPacketQZMJ.getShuangYouTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getSanYouTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "三游次数        ");
            obj.put("val", userPacketQZMJ.getSanYouTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getTianHuTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "天胡次数        ");
            obj.put("val", userPacketQZMJ.getTianHuTimes());
            array.add(obj);
        }
        if(userPacketQZMJ.getQiangGangHuTimes()>0){
            JSONObject obj = new JSONObject();
            obj.put("name", "抢杠胡次数    ");
            obj.put("val", userPacketQZMJ.getQiangGangHuTimes());
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取加入房间数据
     * @param roomNo
     * @param account
     * @return
     */
    public JSONObject obtainEnterData(String roomNo, String account) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("room_no",roomNo);
        obj.put("game_count",room.getGameCount());
        obj.put("game_index",room.getGameIndex());
        obj.put("gameStatus",room.getGameStatus());
        obj.put("youjin",room.getYouJinScore());
        obj.put("users",room.getAllPlayer());
        obj.put("myIndex",room.getPlayerMap().get(account).getMyIndex());
        obj.put("gid",room.getGid());
        obj.put("roomType",room.getRoomType());
        StringBuffer roomInfo = new StringBuffer();
        roomInfo.append(room.getPlayerCount());
        roomInfo.append("人 ");
        if (room.getGameCount()==999) {
            roomInfo.append("1课");
        }else if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK){
            roomInfo.append(room.getGameCount());
            roomInfo.append("局");
        }
        if (room.getRoomType()==CommonConstant.ROOM_TYPE_JB||room.getRoomType()==CommonConstant.ROOM_TYPE_YB) {
            roomInfo.append("    底:");
            roomInfo.append((int) room.getScore());
            StringBuffer roomInfo2 = new StringBuffer();
            roomInfo2.append("入场:");
            roomInfo2.append((int) room.getEnterScore());
            roomInfo2.append("  离场:");
            roomInfo2.append((int) room.getLeaveScore());
            obj.put("roominfo2",String.valueOf(roomInfo2));
        }
        obj.put("roominfo",String.valueOf(roomInfo));
        return obj;
    }

    /**
     * 胡
     * @param roomNo
     * @param account
     * @param type
     * @return
     */
    public int hu(String roomNo, String account, int type){
        //胡牌类型
        int huType=0;
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom gamePlay = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            UserPacketQZMJ userPacketQZMJ = gamePlay.getUserPacketMap().get(account);
            // 设置赢家
            gamePlay.setWinner(account);
            //获取我的牌
            List<Integer> myPai = userPacketQZMJ.getMyPai();
            int pai = 0;
            //自摸
            if(type==3){
                //获取上次抓的牌
                pai=gamePlay.getLastMoPai();
                huType=MaJiangCore.huPaiType(myPai, pai, gamePlay.getJin(), 1);
            }else{
                //获取上次出的牌
                pai=gamePlay.getLastPai();
                huType=MaJiangCore.huPaiType(myPai, pai, gamePlay.getJin(), 2);
            }
            if(huType>0){
                // 胡类型
                if(huType==QZMJConstant.HU_TYPE_YJ){
                    if(userPacketQZMJ.getYouJin()==1){
                        gamePlay.setHuType(QZMJConstant.HU_TYPE_YJ);
                    }else if(userPacketQZMJ.getYouJin()==2){
                        gamePlay.setHuType(QZMJConstant.HU_TYPE_SHY);
                    }else if(userPacketQZMJ.getYouJin()==3){
                        gamePlay.setHuType(QZMJConstant.HU_TYPE_SY);
                    }else{
                        // 三金倒
                        if(userPacketQZMJ.getPlayerJinCount(gamePlay.getJin())==3){
                            gamePlay.setHuType(QZMJConstant.HU_TYPE_SJD);
                        }else{
                            gamePlay.setHuType(QZMJConstant.HU_TYPE_ZM);
                        }
                    }
                }else{
                    gamePlay.setHuType(huType);
                }
                // 判断是否是天胡
                if(huType == QZMJConstant.HU_TYPE_ZM){
                    List<KaiJuModel> paiJuList = gamePlay.getKaiJuList();
                    // 摸牌次数
                    int moPaiConut = 0;
                    for (KaiJuModel kaiJu : paiJuList) {
                        if(kaiJu.getType()==1){
                            moPaiConut ++;
                        }
                    }
                    if(moPaiConut<=1){
                        gamePlay.setHuType(QZMJConstant.HU_TYPE_TH);
                    }
                }
                if(myPai.size()%3==1){
                    // 完整的手牌
                    myPai.add(pai);
                }else{
                    // 胡的牌要放在最后一张
                    for (int i=myPai.size()-1; i>=0; i--) {
                        if(myPai.get(i).equals(pai)){
                            // 移除胡的牌
                            myPai.remove(i);
                            // 添加到末尾
                            myPai.add(pai);
                            break;
                        }
                    }
                }
                if (gamePlay.getGid()==CommonConstant.GAME_ID_QZMJ) {
                    summary(roomNo, account);
                }else if (gamePlay.getGid()==CommonConstant.GAME_ID_NAMJ) {
                    summaryNA(roomNo, account);
                }
                gamePlay.getUserPacketMap().get(account).addHuTimes(gamePlay.getHuType());
            }
        }
        return huType;
    }

    /**
     * 结算分数
     * @param roomNo
     * @param account
     */
    public void summary(String roomNo,String account) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 计算番
        for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(uuid);
                List<Integer> myPais =userPacketQZMJ.getMyPai();
                int fan = room.getUserPacketMap().get(uuid).getTotalFanShu(myPais, room,uuid);
                room.getUserPacketMap().get(uuid).setFan(fan);
            }
        }

        //其他未胡玩家结算
        for (String uuid : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                if(!uuid.equals(account)){
                    Playerinfo p = room.getPlayerMap().get(uuid);
                    UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(uuid);
                    for (String uid : room.getPlayerMap().keySet()) {
                        if (room.getUserPacketMap().containsKey(uid)&&room.getUserPacketMap().get(uid)!=null) {
                            if(!uid.equals(account)&&!uid.equals(uuid)){
                                Playerinfo p1 = room.getPlayerMap().get(uid);
                                UserPacketQZMJ userPacketQZMJ1 = room.getUserPacketMap().get(uid);
                                int score = userPacketQZMJ.getFan();

                                if(!room.isCanOver){

                                    // 牌局为1课或元宝场金币场
                                    if(room.getGameCount()==999||room.getGameCount()==9999){

                                        // 判断牌局是否结束（玩家积分是否小于等于0）
                                        if(p1.getScore()-score<=0){
                                            score = (int) p1.getScore();
                                        }
                                    }
                                }

                                // 减少玩家分数
                                p1.setScore(p1.getScore()-score);
                                userPacketQZMJ1.setScore(userPacketQZMJ1.getScore()-score);

                                // 增加玩家分数
                                p.setScore(p.getScore()+score);
                                userPacketQZMJ.setScore(userPacketQZMJ.getScore()+score);
                            }
                        }
                    }
                }
            }
        }

        // 赢家的总番数
        int winnerTotalFan = room.getUserPacketMap().get(account).getFan();

        // 赢家结算 account 为赢家的uuid
        for (String uuid : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                if(!uuid.equals(account)){

                    Playerinfo p = room.getPlayerMap().get(uuid);
                    UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(uuid);
                    int difen = (int) room.getScore();
                    if(account.equals(room.getBanker())){
                        // 庄赢（双倍底分）
                        difen = difen * (2 + room.getBankerTimes()-1);
                    }else if(uuid.equals(room.getBanker())){
                        // 连庄底分加倍
                        difen = difen * (2 + room.getBankerTimes()-1);
                    }

                    // 计分
                    int score = QZMJGameRoom.jiSuanScore(room.getHuType(), difen, winnerTotalFan, room.getYouJinScore());

                    if(!room.isCanOver){

                        // 牌局为1课或元宝场金币场
                        if(room.getGameCount()==999||room.getGameCount()==9999){

                            // 判断牌局是否结束（玩家积分是否小于等于0）
                            if(p.getScore()-score<=0){
                                score = (int) p.getScore();
                            }
                        }
                    }

                    // 更新未胡的玩家分数
                    p.setScore(p.getScore()-score);
                    userPacketQZMJ.setScore(userPacketQZMJ.getScore()-score);

                    // 更新胡的玩家分数
                    room.getPlayerMap().get(account).setScore(room.getPlayerMap().get(account).getScore()+score);
                    room.getUserPacketMap().get(account).setScore(room.getUserPacketMap().get(account).getScore()+score);
                }
            }
        }
        room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
    }

    public void summaryNA(String roomNo,String account) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 计算番
        for (String uuid : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                int fan = room.getUserPacketMap().get(uuid).getNanAnTotalFanShu();
                room.getUserPacketMap().get(uuid).setFan(fan);
            }
        }
        // 赢家的总番数
        int winnerTotalFan = room.getUserPacketMap().get(account).getFan();

        // 赢家结算 account 为赢家的uuid
        for (String uuid : room.getPlayerMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                if(!uuid.equals(account)){
                    Playerinfo p = room.getPlayerMap().get(uuid);
                    UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(uuid);
                    int difen = 0;
                    if (room.getHuType()==QZMJConstant.HU_TYPE_ZM) {
                        difen = QZMJConstant.SCORE_TYPE_NA_ZM;
                    }else if (room.getHuType()==QZMJConstant.HU_TYPE_YJ) {
                        difen = QZMJConstant.SCORE_TYPE_NA_YJ;
                    }else if (room.getHuType()==QZMJConstant.HU_TYPE_SHY) {
                        difen = QZMJConstant.SCORE_TYPE_NA_SHY;
                    }else if (room.getHuType()==QZMJConstant.HU_TYPE_SY) {
                        difen = QZMJConstant.SCORE_TYPE_NA_SY;
                    }
                    if(account.equals(room.getBanker())){
                        // 庄赢（双倍底分）
                        difen = difen * 2;
                    }else if(uuid.equals(room.getBanker())){
                        // 连庄底分加倍
                        difen = difen * 2;
                    }
                    // 计分
                    int score = difen + winnerTotalFan;
                    if(!room.isCanOver){
                        // 牌局为1课
                        if(room.getGameCount()==999){
                            // 判断牌局是否结束（玩家积分是否小于等于0）
                            if(p.getScore()-score<=0){
                                score = (int) p.getScore();
                            }
                        }
                    }
                    // 更新未胡的玩家分数
                    p.setScore(p.getScore()-score);
                    userPacketQZMJ.setScore(userPacketQZMJ.getScore()-score);
                    // 更新胡的玩家分数
                    room.getPlayerMap().get(account).setScore(room.getPlayerMap().get(account).getScore()+score);
                    room.getUserPacketMap().get(account).setScore(room.getUserPacketMap().get(account).getScore()+score);
                }
            }
        }
        room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
    }

    /**
     * 更新玩家积分
     * @param roomNo
     */
    public void updateUserScore(String roomNo) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 元宝输赢情况
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB||room.getRoomType()==CommonConstant.ROOM_TYPE_JB) {
                    double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                    double sum = room.getUserPacketMap().get(account).getScore();
                    long userId = room.getPlayerMap().get(account).getId();
                    array.add(obtainUserScoreData(total, sum, userId));
                }
            }
        }
        if (room.getId()==0) {
            JSONObject roomInfo = roomBiz.getRoomInfoByRno(room.getRoomNo());
            if (!Dto.isObjNull(roomInfo)) {
                room.setId(roomInfo.getLong("id"));
            }
        }
        if (array.size()>0) {
            // 更新玩家分数
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.UPDATE_SCORE, room.getPumpObject(array)));
        }
    }

    /**
     * 获取需要更新的数据
     * @param total
     * @param sum
     * @param id
     * @return
     */
    public JSONObject obtainUserScoreData(double total,double sum,long id) {
        JSONObject obj = new JSONObject();
        obj.put("total", total);
        obj.put("fen", sum);
        obj.put("id", id);
        return obj;
    }

    /**
     * 保存战绩
     * @param roomNo
     */
    public void saveGameLog(String roomNo) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray gameLogResults = new JSONArray();
        JSONArray gameResult = new JSONArray();
        JSONArray array = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                double total = RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore();
                double sum = room.getUserPacketMap().get(account).getScore();
                long userId = room.getPlayerMap().get(account).getId();
                array.add(obtainUserScoreData(total, sum, userId));
                // 战绩记录
                JSONObject gameLogResult = new JSONObject();
                gameLogResult.put("account", account);
                gameLogResult.put("name", room.getPlayerMap().get(account).getName());
                gameLogResult.put("headimg", room.getPlayerMap().get(account).getHeadimg());
                if (!Dto.stringIsNULL(room.getBanker())&&room.getPlayerMap().containsKey(room.getBanker())&&room.getPlayerMap().get(room.getBanker())!=null) {
                    gameLogResult.put("zhuang", room.getPlayerMap().get(room.getBanker()).getMyIndex());
                }else {
                    gameLogResult.put("zhuang", CommonConstant.NO_BANKER_INDEX);
                }
                gameLogResult.put("myIndex", room.getPlayerMap().get(account).getMyIndex());
                gameLogResult.put("score", room.getUserPacketMap().get(account).getScore());
                gameLogResult.put("totalScore", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                gameLogResult.put("win", CommonConstant.GLOBAL_YES);
                if (room.getUserPacketMap().get(account).getScore() < 0) {
                    gameLogResult.put("win", CommonConstant.GLOBAL_NO);
                }
                gameLogResults.add(gameLogResult);
                // 用户战绩
                JSONObject userResult = new JSONObject();
                userResult.put("zhuang", room.getBanker());
                userResult.put("isWinner", CommonConstant.GLOBAL_NO);
                if (room.getUserPacketMap().get(account).getScore() > 0) {
                    userResult.put("isWinner", CommonConstant.GLOBAL_YES);
                }
                userResult.put("score", room.getUserPacketMap().get(account).getScore());
                userResult.put("totalScore", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                userResult.put("player", room.getPlayerMap().get(account).getName());
                gameResult.add(userResult);
            }
        }
        // 战绩信息
        JSONObject gameLogObj = room.obtainGameLog(gameLogResults.toString(), JSONArray.fromObject(room.getKaiJuList()).toString());
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_GAME_LOG, gameLogObj));
        // 金币场不计战绩
        if (room.getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
            JSONArray userGameLogs = room.obtainUserGameLog(gameLogObj.getLong("id"), array, gameResult.toString());
            for (int i = 0; i < userGameLogs.size(); i++) {
                producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.INSERT_USER_GAME_LOG, userGameLogs.getJSONObject(i)));
            }
        }
    }

    public void updateRoomCard(String roomNo) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONArray array = new JSONArray();
        int roomCardCount = 0;
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 房主支付
                if (room.getPayType()==CommonConstant.PAY_TYPE_OWNER) {
                    if (account.equals(room.getOwner())) {
                        // 参与第一局需要扣房卡
                        if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                            array.add(room.getPlayerMap().get(account).getId());
                            roomCardCount = room.getPlayerCount()*room.getSinglePayNum();
                        }
                    }
                }
                // 房费AA
                if (room.getPayType()==CommonConstant.PAY_TYPE_AA) {
                    // 参与第一局需要扣房卡
                    if (room.getUserPacketMap().get(account).getPlayTimes()==1) {
                        array.add(room.getPlayerMap().get(account).getId());
                        roomCardCount = room.getSinglePayNum();
                    }
                }
            }
        }
        if (array.size()>0) {
            producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.PUMP, room.getRoomCardChangeObject(array,roomCardCount)));
        }
    }


    /**
     * 保存游戏记录
     * @param roomNo
     */
    public void saveUserDeduction(String roomNo) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room==null) {
            return;
        }
        JSONArray userDeductionData = new JSONArray();
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                // 用户游戏记录
                JSONObject object = new JSONObject();
                object.put("id", room.getPlayerMap().get(account).getId());
                object.put("roomNo", room.getRoomNo());
                object.put("gid", room.getGid());
                object.put("type", room.getRoomType());
                object.put("fen", room.getUserPacketMap().get(account).getScore());
                object.put("old", Dto.sub(RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore(),room.getUserPacketMap().get(account).getScore()));
                if (RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore()<0) {
                    object.put("new", 0);
                }else {
                    object.put("new", RoomManage.gameRoomMap.get(room.getRoomNo()).getPlayerMap().get(account).getScore());
                }
                userDeductionData.add(object);
            }
        }
        // 玩家输赢记录
        producerService.sendMessage(daoQueueDestination, new PumpDao(DaoTypeConstant.USER_DEDUCTION, new JSONObject().element("user", userDeductionData)));
    }

    /**
     * 杠
     * @param roomNo
     * @param account
     * @param gangType
     * @return
     */
    public int[] gang(String roomNo, String account, int gangType){

        int[] back=null;
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){

            QZMJGameRoom gamePlay = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            UserPacketQZMJ userPacketQZMJ = gamePlay.getUserPacketMap().get(account);
            //获取上次出的牌
            int pai=0;
            //获取我的牌
            List<Integer> myPai=userPacketQZMJ.getMyPai();
            //获取最后一次摸排的人
            if(gangType==2||gangType==9){
                //获取我碰过的排
                pai=gamePlay.getLastMoPai();
                List<DontMovePai> penghistory = userPacketQZMJ.getPengList();
                back=MaJiangCore.isGang(myPai, pai, 1, penghistory);
            }else if(gangType==6){
                //明杠
                pai=gamePlay.getLastPai();
                back=MaJiangCore.isGang(myPai, pai, 2, null);
            }
            int type=back[0];
            //杠
            if(type>0){

                pai = back[1];
                //处理杠事件
                if(type==1){
                    //抓，补杠
                    List<Integer> indexList = new ArrayList<Integer>();
                    // 有玩家可以抢杠
                    if(indexList.size()>0){

                        back = new int[indexList.size()+1];
                        back[0] = -1;
                        Collections.sort(indexList);
                        for (int i=0; i<indexList.size(); i++) {
                            back[i+1] = indexList.get(i);
                        }

                    }else{
                        //1.从手牌中减去1张牌
                        userPacketQZMJ.removeMyPai(pai);
                        //2.记录到不可动的排
                        //获取抓杠位置
                        int zgindex = userPacketQZMJ.buGangIndex(pai);
                        //将碰的牌组转化成杠牌组
                        for (DontMovePai dmpai : userPacketQZMJ.getPengList()) {
                            int focusPai = dmpai.getFoucsPai();
                            if(focusPai==pai){
                                dmpai.updateDontMovePai(5,pai, focusPai);
                            }
                        }
                        //3.记录到牌局记录
                        gamePlay.addKaijuList(gamePlay.getPlayerIndex(account), 9, new int[]{pai,pai,pai,pai,zgindex});
                    }

                }else if(type==3){
                    //暗杠
                    //1.从手牌中减去4张牌
                    userPacketQZMJ.removeMyPai(pai);
                    userPacketQZMJ.removeMyPai(pai);
                    userPacketQZMJ.removeMyPai(pai);
                    userPacketQZMJ.removeMyPai(pai);
                    //2.记录到不可动的排
                    userPacketQZMJ.addHistoryPai(3, new int[]{pai,pai,pai,pai}, pai);
                    //3.记录到牌局记录
                    gamePlay.addKaijuList(gamePlay.getPlayerIndex(account), 2, new int[]{pai,pai,pai,pai});
                }else if(type==2){
                    //明杠
                    //1.从手牌中减去3张牌
                    userPacketQZMJ.removeMyPai(pai);
                    userPacketQZMJ.removeMyPai(pai);
                    userPacketQZMJ.removeMyPai(pai);
                    //2.记录到不可动的排
                    userPacketQZMJ.addHistoryPai(4, new int[]{pai,pai,pai,pai}, pai);
                    //3.记录到牌局记录
                    gamePlay.addKaijuList(gamePlay.getPlayerIndex(account), 6, new int[]{pai,pai,pai,pai});
                    //4.给玩家计分
                }

                //4.定位ThisUUID
                gamePlay.setThisType(QZMJConstant.THIS_TYPE_ZUA);
                gamePlay.setThisAccount(account);
            }
        }
        return back;
    }

    /**
     * 碰
     * @param roomNo
     * @param account
     * @return
     */
    public boolean peng(String roomNo,String account){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            //获取上次出的牌
            int pai = room.getLastPai();
            UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(account);
            //获取我的牌
            List<Integer> myPai=userPacketQZMJ.getMyPai();
            int[] back=MaJiangCore.isPeng(myPai, pai);
            if(back[0]>0){
                //处理碰事件
                //1.从手牌中减去2张牌
                userPacketQZMJ.removeMyPai(pai);
                userPacketQZMJ.removeMyPai(pai);
                //2.记录到不可动的排
                userPacketQZMJ.addHistoryPai(2, new int[]{pai,pai,pai}, pai);
                //3.记录到牌局记录
                room.addKaijuList(room.getPlayerIndex(account), 5, new int[]{pai,pai,pai});
                //4.定位ThisUUID
                room.setThisType(QZMJConstant.THIS_TYPE_CHU);
                room.setThisAccount(account);
                return true;
            }
        }
        return false;
    }

    /**
     * 吃
     * @param roomNo
     * @param chiValue
     * @param account
     * @return
     */
    public boolean chi(String roomNo,int[] chiValue,String account){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            //获取上次出的牌
            int pai = room.getLastPai();
            Playerinfo player = room.getPlayerMap().get(account);
            UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(account);
            //获取我的牌
            List<Integer> myPai = userPacketQZMJ.getMyPai();
            List<int[]> back=MaJiangCore.isChi(myPai, pai, room.getJin());
            //
            boolean isChi=false;
            if(back!=null&&back.size()>0){
                for(int[] a:back){
                    if((a[0]==chiValue[0]&&a[1]==chiValue[1])||(a[0]==chiValue[1]&&a[1]==chiValue[0])){
                        isChi=true;
                        break;
                    }
                }
            }
            //处理吃事件
            if(isChi){
                //1.从手牌中减去2张牌
                userPacketQZMJ.removeMyPai(chiValue[0]);
                userPacketQZMJ.removeMyPai(chiValue[1]);
                //2.记录到不可动的排
                userPacketQZMJ.addHistoryPai(1, new int[]{chiValue[0],chiValue[1],pai}, pai);
                //3.记录到牌局记录
                room.addKaijuList(room.getPlayerIndex(account), 4, new int[]{chiValue[0],chiValue[1],pai});
                //4.定位ThisUUID
                room.setThisType(QZMJConstant.THIS_TYPE_CHU);
                room.setThisAccount(account);
                return isChi;
            }
        }
        return false;
    }

    /**
     * 过
     * @param roomNo
     * @param operateAccount
     * @return
     */
    public Object[] guo(String roomNo,String operateAccount){
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Object[] obj=null;
        //获取下次询问的事件
        int nextAskType = room.getNextAskType();
        //获取上次抓牌的人
        String lastMoAccount = room.getLastMoAccount();
        if((nextAskType==QZMJConstant.ASK_TYPE_HU_BY_MYSELF||nextAskType==QZMJConstant.ASK_TYPE_GANG_AN)&&lastMoAccount.equals(operateAccount)){
            //获取上次摸到的牌
            int newPai = room.getLastMoPai();
            //出牌
            obj = new Object[]{lastMoAccount,QZMJConstant.THIS_TYPE_ZUA,newPai};
        }else{
            //获取上次出的牌
            int pai = room.getLastPai();
            //获取上次出牌的玩家
            String chuPai = room.getLastAccount();
            // 出牌玩家的下家
            String xiajiaUUID = room.getNextPlayer(chuPai);
            String ask=null;
            JSONArray askArray = new JSONArray();
            //设置询问事件
            nextAskType = room.getNextAskType();
            //1.检查有无胡
            if(nextAskType == QZMJConstant.ASK_TYPE_HU_OTHER){
                //设置询问事件
                List<String> askHu=checkIsHuByOtherSelf(roomNo, pai);
                //有胡
                if(askHu.size()>0){
                    // 一家有胡
                    if(askHu.size()==1){
                        askArray.add(obtainAskResult(askHu.get(0).toString(),7,pai));
                    }else{
                        // 多人有胡,返回第一个玩家
                        room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                        // 下一个询问玩家
                        room.setNextAskAccount(room.getNextPlayer(askHu.get(0)));
                        return new Object[]{askHu.get(0),7,pai};
                    }
                }
            }
            nextAskType = room.getNextAskType();
            if(nextAskType == QZMJConstant.ASK_TYPE_GANG_MING || askArray.size()>0){
                //2.检查有无杠
                ask=checkIsMingGang(roomNo, pai);
                //有杠
                if(ask!=null){
                    // 同时出现胡和杠事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,6,pai));
                        }else{
                            // 其他玩家有杠
                            String uuid = askArray.getJSONObject(0).getString("uuid");
                            room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                            // 下一个询问玩家
                            room.setNextAskAccount(room.getNextPlayer(uuid));
                            return new Object[]{uuid,7,pai};
                        }
                    }else{
                        askArray.add(obtainAskResult(ask,6,pai));
                    }
                }
            }
            nextAskType = room.getNextAskType();
            if(nextAskType == QZMJConstant.ASK_TYPE_PENG || askArray.size()>0){
                //3.检查有无碰
                ask=checkIsPeng(roomNo, pai);
                //有碰
                if(ask!=null){
                    // 同时出现胡和碰事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,5,pai));
                        }else{
                            // 其他玩家有碰
                            String uuid = askArray.getJSONObject(0).getString("uuid");
                            room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                            // 下一个询问玩家
                            room.setNextAskAccount(room.getNextPlayer(uuid));
                            return new Object[]{uuid,7,pai};
                        }

                    }else{
                        askArray.add(obtainAskResult(ask,5,pai));
                    }
                }
            }

            nextAskType = room.getNextAskType();
            if(nextAskType == QZMJConstant.ASK_TYPE_CHI || (askArray.size()>0 && xiajiaUUID.equals(operateAccount) && nextAskType!= QZMJConstant.ASK_TYPE_FINISH)){
                //4.检查有无吃
                Object[] chiback=checkIsChi(roomNo, pai, xiajiaUUID);
                //有吃
                if(chiback!=null && chiback.length==2){
                    ask = (String) chiback[0];
                    // 同时出现胡和吃碰事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,4,JSONArray.fromObject(chiback[1])));
                        }else{ // 其他玩家有胡杠碰事件
                            room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
                            room.setNextAskAccount(ask);
                        }

                    }else{

                        return new Object[]{ask,4,chiback[1]};
                    }
                }
            }

            // 解决多事件询问问题
            if(askArray.size()>0){
                String uuid = askArray.getJSONObject(0).getString("uuid");
                return new Object[]{uuid,10,askArray};
            }
            //设置下一个焦点人
            room.setNextThisUUID();
            obj=new Object[]{null,QZMJConstant.THIS_TYPE_ZUA,null};
            return obj;
        }
        return obj;
    }

    public void buHua(String roomNo, String mPaier) {

        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){

            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            Playerinfo player = room.getPlayerMap().get(mPaier);
            UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(mPaier);
            // 1、获取花的数量
            List<Integer> paiList = userPacketQZMJ.getMyPai();
            List<Integer> huaList = new ArrayList<Integer>();
            for (Integer pai : paiList) {
                // 如果是花牌
                if(QZMJConstant.isHuaPai(pai)){
                    huaList.add(pai);
                }
            }
            int huaCount = huaList.size();
            if(huaCount>0){
                room.getUserPacketMap().get(mPaier).getHuaList().addAll(huaList);
                // 2、移除花牌
                for (Integer p : huaList) {
                    room.getUserPacketMap().get(mPaier).removeMyPai(p);
                }
                // 返回类型
                int backType=1;
                // 新补的牌
                int buPai=-1;
                // 3、开始摸牌（补花）
                int[] pais = new int[huaCount];
                for (int i = 0; i < huaCount; i++) {

                    int index=room.getIndex();
                    int[] pai=room.getPai();
                    int newpai=-1;
                    if(index+QZMJConstant.LEFT_PAI_COUNT<pai.length){
                        newpai=pai[index];
                        buPai = newpai;
                        pais[i] = newpai;
                        //重置牌的位数
                        room.setIndex(index+1);
                        //摸牌
                        room.getUserPacketMap().get(mPaier).addMyPai(newpai);
                        // 摸牌时补花，设置新摸的牌为最后一张摸的牌
                        if(room.getGameStatus()==QZMJConstant.QZ_GAME_STATUS_ING){
                            room.setLastMoPai(newpai);
                        }

                    }else{
                        // 流局
                        backType = 999;
                    }
                }
                if(pais.length>0){
                    // 记录补花记录
                    //0.出牌    1：抓   2.暗杠   3：自摸    4.吃    5.碰    6.明杠   7.胡   11补花
                    room.addKaijuList(room.getPlayerIndex(mPaier), 11, pais);
                }
                // 4、摸牌时补花，判断是否有触发事件
                boolean isChuPai = false;
                /**
                 * 20170822 lhp
                 * 玩家摸牌次数，以此判断玩家是否是开局补花
                 */
                // 摸牌次数
                int moPaiConut = room.getActionTimes(room.getPlayerIndex(mPaier), 1);
                //补花后需要出牌
                if(!QZMJConstant.hasHuaPai(pais) && moPaiConut>0){

                    isChuPai = true;

                    JSONObject buhuaData = new JSONObject();
                    buhuaData.put("huaCount", userPacketQZMJ.getHuaList().size());
                    buhuaData.put("huaValue", huaList.toArray());
                    buhuaData.put(QZMJConstant.lastValue, pais);
                    JSONArray array = new JSONArray();
                    //判断是否胡
                    boolean back=checkIsHuByMySelf(roomNo, buPai);
                    if(back){
                        backType=3;
                        int yjtype = room.getUserPacketMap().get(mPaier).getYouJinIng();
                        // 游金中
                        if(yjtype>0){
                            List<Integer> myPais = room.getUserPacketMap().get(mPaier).getMyPai();
                            if(myPais.contains(room.getJin())){

                                int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin());
                                if(result==QZMJConstant.HU_TYPE_YJ){
                                    // 游金成功
                                    room.getUserPacketMap().get(mPaier).setYouJin(yjtype);

                                }else{
                                    room.getUserPacketMap().get(mPaier).setYouJinIng(0);
                                    room.getUserPacketMap().get(mPaier).setYouJin(0);
                                }
                            }else{
                                room.getUserPacketMap().get(mPaier).setYouJinIng(0);
                                room.getUserPacketMap().get(mPaier).setYouJin(0);
                            }
                        }
                    }else{
                       room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
                    }

                    //判断是否有杠
                    int[] gang=checkIsAnGang(roomNo, buPai);

                    // 有杠
                    if(gang[0]>0){
                        int gangType = 0;
                        // 补杠
                        if(gang[0]==1){
                            gangType=9;
                        }else if(gang[0]==3){
                            // 暗杠
                            gangType=2;
                        }
                        JSONObject obj = new JSONObject();
                        obj.put("type", gangType);
                        obj.put("uuid", String.valueOf(room.getLastMoAccount()));
                        obj.put("pai", buPai);
                        obj.put("gangValue", gang[1]);
                        array.add(obj);
                    }

                    JSONObject obj = new JSONObject();
                    obj.put("type", backType);
                    obj.put("uuid", String.valueOf(room.getLastMoAccount()));
                    obj.put("pai", buPai);
                    array.add(obj);
                    detailDataByZhuaPai(array, room, buhuaData);
                }

                // 5、返回数据
                for(String uuid:room.getPlayerMap().keySet()){
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        // 当玩家是摸牌时补花，不返回补花事件（会与摸牌事件冲突）
                        if(uuid.equals(mPaier) && !isChuPai){
                            JSONObject data = new JSONObject();
                            data.put(QZMJConstant.zpaishu, room.getPai().length-room.getIndex());
                            if(QZMJConstant.hasHuaPai(pais)){
                                data.put(QZMJConstant.type,10);
                            }else{
                                data.put(QZMJConstant.type,1);
                            }
                            data.put(QZMJConstant.lastType,-10);
                            data.put(QZMJConstant.foucs,player.getMyIndex());
                            data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                            data.put(QZMJConstant.nowPoint, room.getNowPoint());
                            data.put("huaCount", userPacketQZMJ.getHuaList().size());
                            data.put("huaValue", huaList.toArray());
                            data.put(QZMJConstant.lastValue, pais);
                            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(data),"gameActReultPush");
                        }else if(!uuid.equals(mPaier)){
                            // 通知其他玩家
                            JSONObject data = new JSONObject();
                            data.put(QZMJConstant.zpaishu, room.getPai().length-room.getIndex());
                            data.put(QZMJConstant.type,1);
                            data.put(QZMJConstant.lastType,-10);
                            data.put(QZMJConstant.foucs,player.getMyIndex());
                            data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                            data.put(QZMJConstant.nowPoint, room.getNowPoint());
                            data.put("huaCount", userPacketQZMJ.getHuaList().size());
                            data.put("huaValue", huaList.toArray());
                            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(data),"gameActReultPush");
                        }
                    }
                }

            }else if(userPacketQZMJ.getHuaList().size()>0){
                // 最后一张补的花
                int hua = userPacketQZMJ.getHuaList().get(userPacketQZMJ.getHuaList().size()-1);
                // 最后一张摸到的牌
                int pai = userPacketQZMJ.getMyPai().get(userPacketQZMJ.getMyPai().size()-1);

                JSONObject data = new JSONObject();
                data.put(QZMJConstant.zpaishu, room.getPai().length-room.getIndex());
                data.put(QZMJConstant.type,1);
                data.put(QZMJConstant.lastType,-10);
                data.put(QZMJConstant.foucs,player.getMyIndex());
                data.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                data.put(QZMJConstant.nowPoint, room.getNowPoint());
                data.put(QZMJConstant.lastValue, new int[pai]);
                data.put("huaCount", userPacketQZMJ.getHuaList().size());
                data.put("huaValue", new int[hua]);
                CommonConstant.sendMsgEventToSingle(player.getUuid(),String.valueOf(data),"gameActReultPush");
            }

        }
    }

    public static JSONObject autoBuHua(String roomNo, String mPaier) {

        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(mPaier);

            // 1、获取花的数量
            List<Integer> paiList = userPacketQZMJ.getMyPai();
            List<Integer> huaList = new ArrayList<Integer>();
            for (Integer pai : paiList) {
                // 如果是花牌
                if(QZMJConstant.isHuaPai(pai)){
                    huaList.add(pai);
                }
            }
            int huaCount = huaList.size();
            if(huaCount>0){
                room.getUserPacketMap().get(mPaier).getHuaList().addAll(huaList);
                // 2、移除花牌
                for (Integer p : huaList) {
                    room.getUserPacketMap().get(mPaier).removeMyPai(p);
                }
                // 3、开始摸牌（补花）
                int[] pais = new int[huaCount];
                for (int i = 0; i < huaCount; i++) {
                    int index=room.getIndex();
                    int[] pai=room.getPai();
                    int newpai=-1;
                    if(index+QZMJConstant.LEFT_PAI_COUNT<pai.length){
                        newpai=pai[index];
                        pais[i] = newpai;
                        //重置牌的位数
                        room.setIndex(index+1);
                        //摸牌
                        room.getUserPacketMap().get(mPaier).addMyPai(newpai);
                        // 摸牌时补花，设置新摸的牌为最后一张摸的牌
                        if(room.getGameStatus() == room.getGameStatus()){
                            room.setLastMoPai(newpai);
                        }
                    }
                }
                if(pais.length>0){
                    // 记录补花记录
                    //0.出牌    1：抓   2.暗杠   3：自摸    4.吃    5.碰    6.明杠   7.胡   11补花
                    room.addKaijuList(room.getPlayerIndex(mPaier), 11, pais);
                }
                // 4、返回数据
                JSONObject data = new JSONObject();
                if(QZMJConstant.hasHuaPai(pais)){
                    data.put(QZMJConstant.type,10);
                }else{
                    data.put(QZMJConstant.type,1);
                }
                data.put("huaCount", userPacketQZMJ.getHuaList().size());
                data.put("huaValue", huaList.toArray());
                data.put(QZMJConstant.lastValue, pais);
                return data;
            }
        }
        return null;
    }

    public void buHuaByMoPai(String roomNo, String mPaier) {

        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            JSONArray huavals = new JSONArray();
            int buhuaType = 10;
            int buhuaCount = 0;
            // 需要补花
            while(buhuaType==10){
                JSONObject data = autoBuHua(roomNo, mPaier);
                if(data!=null){
                    buhuaType = data.getInt("type");
                    huavals.add(data);
                }
                buhuaCount ++;
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
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        JSONObject buhua = new JSONObject();
                        buhua.put("index", room.getPlayerMap().get(mPaier).getMyIndex());
                        buhua.put("zpaishu", room.getPai().length-room.getIndex());

                        if(mPaier.equals(uuid)){
                            buhua.put("huavals", huavals);
                        }else{
                            buhua.put("huavals", otherBuHua);
                        }
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(buhua),"gameBuHuaPush");
                    }
                }
            }

            // 补一次花延迟1500ms，让补花动画播完
            try {
                Thread.sleep(1500 * buhuaCount);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            // 判断是否触发事件
            JSONArray array = new JSONArray();
            // 返回类型
            int backType=1;
            int buPai = room.getLastMoPai();
            if(room.getIndex()+QZMJConstant.LEFT_PAI_COUNT<room.getPai().length){
                //判断是否胡
                boolean back=checkIsHuByMySelf(roomNo, buPai);
                if(back){
                    backType=3;

                    int yjtype = room.getUserPacketMap().get(mPaier).getYouJinIng();
                    // 游金中
                    if(yjtype>0){
                        List<Integer> myPais = room.getUserPacketMap().get(mPaier).getMyPai();
                        if(myPais.contains(room.getJin())){

                            int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin());
                            if(result==QZMJConstant.HU_TYPE_YJ){
                                // 游金成功
                                room.getUserPacketMap().get(mPaier).setYouJin(yjtype);

                            }else{
                                room.getUserPacketMap().get(mPaier).setYouJinIng(0);
                                room.getUserPacketMap().get(mPaier).setYouJin(0);
                            }
                        }else{
                            room.getUserPacketMap().get(mPaier).setYouJinIng(0);
                            room.getUserPacketMap().get(mPaier).setYouJin(0);
                        }
                    }
                }else{
                    room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
                }

                //判断是否有杠
                int[] gang=checkIsAnGang(roomNo, buPai);

                if(gang[0]>0){
                    // 有杠
                    int gangType = 0;
                    if(gang[0]==1){
                        // 补杠
                        gangType=9;
                    }else if(gang[0]==3){
                        // 暗杠
                        gangType=2;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("type", gangType);
                    obj.put("uuid", String.valueOf(room.getLastMoAccount()));
                    obj.put("pai", buPai);
                    obj.put("gangValue", gang[1]);
                    array.add(obj);
                }
            }else{
                backType = 999;
            }

            JSONObject obj = new JSONObject();
            obj.put("type", backType);
            obj.put("uuid", String.valueOf(room.getLastMoAccount()));
            obj.put("pai", buPai);
            array.add(obj);
            detailDataByZhuaPai(array, room, null);
        }
    }

    public JSONArray autoMoPai(String roomNo, String account) {
        JSONArray mjieguo=moPai(roomNo, account);
        detailDataByZhuaPai(mjieguo, (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo), null);
        return null;
    }

    /**
     * 摸牌
     * backType -1 错误，1：出牌  3：自摸  2：暗杠  9：补杠  10：补花  999：流局
     * @param roomNo
     * @param moPaiUUID
     * @return
     */
    public JSONArray moPai(String roomNo, String moPaiUUID) {
        JSONArray array = new JSONArray();
        int backType=-1;
        int newpai=-1;
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            newpai=mopai(roomNo, moPaiUUID);
            String uuid=room.getLastMoAccount();
            // 流局
            if(newpai < 0) {
                backType = 999;
            }else if(QZMJConstant.isHuaPai(newpai)) {
                //判断是否抓到花牌
                backType = 10;
            }else {
                backType=1;
                boolean back=false;
                int type = room.getNextAskType();

                if((room.getYjType()>2 && room.getUserPacketMap().get(uuid).getYouJinIng()<3)) {
                    // 三游不能自摸，只能杠上摸
                   room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
                }else if ((room.isGuangYou && room.hasYouJinType(2) && room.getUserPacketMap().get(uuid).getYouJinIng()<2)) {
                    // 光游时的双游不能自摸，只能杠上摸
                    room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
                }else{
                    //判断是否自摸
                    if(type == QZMJConstant.THIS_TYPE_ZUA){
                        back = checkIsHuByMySelf(roomNo, newpai);
                        if(back){
                            backType=3;
                            int yjtype = room.getUserPacketMap().get(uuid).getYouJinIng();
                            // 游金中
                            if(yjtype>0){
                                List<Integer> myPais = room.getUserPacketMap().get(uuid).getMyPai();
                                if(myPais.contains(room.getJin())){
                                    int result = MaJiangCore.huPaiHasJin(myPais, 0, room.getJin());
                                    if(result==QZMJConstant.HU_TYPE_YJ){
                                        // 游金成功
                                        room.getUserPacketMap().get(uuid).setYouJin(yjtype);
                                    }else{
                                        room.getUserPacketMap().get(uuid).setYouJinIng(0);
                                        room.getUserPacketMap().get(uuid).setYouJin(0);
                                    }
                                }else{
                                    room.getUserPacketMap().get(uuid).setYouJinIng(0);
                                    room.getUserPacketMap().get(uuid).setYouJin(0);
                                }
                            }
                        }
                    }
                }
                // 判断是否触发杠事件
                int[] gang = checkIsAnGang(roomNo, newpai);
                if(gang[0]>0) {
                    // 有杠
                    int gangType = 0;
                    if(gang[0] == 1) {
                        // 补杠
                        gangType = 9;
                    }else if(gang[0] == 3) {
                        // 暗杠
                        gangType = 2;
                    }
                    JSONObject obj = new JSONObject();
                    obj.put("type", gangType);
                    obj.put("uuid", uuid);
                    obj.put("pai", newpai);
                    obj.put("gangValue", gang[1]);
                    array.add(obj);
                }
            }
            JSONObject obj = new JSONObject();
            obj.put("type", backType);
            obj.put("uuid", String.valueOf(room.getLastMoAccount()));
            obj.put("pai", newpai);
            array.add(obj);
        }
        return array;
    }

    /**
     * 摸牌
     * @param roomNo
     * @param moPaiUUID
     * @return
     */
    public int mopai(String roomNo, String moPaiUUID) {

        int newpai=-1;
        //获取本次应该出牌的人
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            int index = room.getIndex();
            int[] pai = room.getPai();

            if(index < pai.length - QZMJConstant.LEFT_PAI_COUNT){

                //获取摸牌人
                String mopai = moPaiUUID;
                int paiCount = room.getUserPacketMap().get(mopai).getMyPai().size();
                if(paiCount <= room.getPaiCount()){

                    newpai=pai[index];
                    //重置牌的位数
                    room.setIndex(index+1);
                    //摸牌
                    room.getUserPacketMap().get(mopai).addMyPai(newpai);
                    //设置询问人
                    room.setNextAskAccount(mopai);
                    //设置询问事件
                    room.setNextAskType(QZMJConstant.THIS_TYPE_ZUA);
                    //设置最新被摸的牌
                    room.setLastMoPai(newpai);
                    //设置最新摸牌的人
                    room.setLastMoAccount(mopai);
                    //1.抓牌（出牌）  2.暗杠   3：自摸    4.吃    5.碰    6.明杠   7.胡
                    // 记录摸牌记录
                    room.addKaijuList(room.getPlayerIndex(mopai), 1,new int []{newpai});
                }else{
                    return room.getLastMoPai();
                }
            }
        }
        return newpai;
    }

    /**
     * 出牌
     * @param roomNo
     * @param pai
     * @param chupaiId
     * @return
     */
    public Object[] chuPai(String roomNo, int pai, String chupaiId) {
        Object[] obj=null;
        //出牌
        boolean back = chupai(roomNo,pai,chupaiId);
        //出牌成功
        if(back){
            // 通知前端出牌事件
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            // 当前游金状态
            int yjtype = room.getUserPacketMap().get(chupaiId).getYouJinIng();
            // 获取出牌玩家的下家uuid
            String xiajiaUUID = room.getNextPlayer(chupaiId);
            // 判断是否可以游金
            List<Integer> myPais = room.getUserPacketMap().get(chupaiId).getMyPai();
            //开始游金
            if(myPais.contains(room.getJin())){
                List<Integer> paiList = new ArrayList<Integer>(myPais);
                JSONObject result = MaJiangCore.youJinAndTingPaiPanDing(paiList, room.getJin(), null);
                if(result!=null && result.getInt("type")==1) {
                    //设置当前游金状态
                    if(yjtype==0){
                        room.getUserPacketMap().get(chupaiId).setYouJinIng(1);
                        if(room.getUserPacketMap().get(chupaiId).getYouJinIng() > room.getYjType()){
                            room.setYjType(room.getUserPacketMap().get(chupaiId).getYouJinIng());
                            room.setYjAccount(chupaiId);
                        }
                    }else if(yjtype>0){
                        // 打出金牌
                        if(room.getJin() == pai){
                            if(yjtype==1){
                                room.getUserPacketMap().get(chupaiId).setYouJinIng(2);
                            }else if(yjtype==2){
                                room.getUserPacketMap().get(chupaiId).setYouJinIng(3);
                            }

                            if(room.getUserPacketMap().get(chupaiId).getYouJinIng() > room.getYjType()){
                                room.setYjType(room.getUserPacketMap().get(chupaiId).getYouJinIng());
                                room.setYjAccount(chupaiId);
                            }
                        }else{ // 单游
                            room.getUserPacketMap().get(chupaiId).setYouJinIng(1);
                            room.getUserPacketMap().get(chupaiId).setYouJin(0);
                            if(room.getUserPacketMap().equals(chupaiId)){
                                room.setYjType(1);
                            }
                        }
                    }
                }else{
                    room.getUserPacketMap().get(chupaiId).setYouJinIng(0);
                    room.getUserPacketMap().get(chupaiId).setYouJin(0);
                }
            }else{
                room.getUserPacketMap().get(chupaiId).setYouJinIng(0);
                room.getUserPacketMap().get(chupaiId).setYouJin(0);
            }

            /// 通知前台展示玩家出的牌
            //总牌数
            int zpaishu = room.getPai().length-room.getIndex();
            //获取之前事件
            int foucs = room.getPlayerIndex(room.getLastAccount());
            // 出牌玩家当前游金状态
            yjtype = room.getUserPacketMap().get(chupaiId).getYouJinIng();
            for (String uuid : room.getPlayerMap().keySet()) {
                if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                    JSONObject backObj=new JSONObject();
                    backObj.put(QZMJConstant.zpaishu, zpaishu);
                    backObj.put(QZMJConstant.foucs,foucs);
                    backObj.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                    backObj.put(QZMJConstant.nowPoint, room.getNowPoint());
                    backObj.put(QZMJConstant.lastValue,pai);
                    // 出牌牌面展示
                    backObj.put(QZMJConstant.type,new int[]{11});
                    // 光游时直接返回游金类型
                    if(uuid.equals(chupaiId) || room.isGuangYou){
                        backObj.put("youjin", yjtype);
                    }else{
                        // 暗游时，只有双游以上才通知其他人
                        if(yjtype>1){
                            // 通知其他玩家正在双游、三游
                            backObj.put("youjin", yjtype);
                        }else{
                            backObj.put("youjin", 0);
                        }
                    }
                    CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(backObj),"gameChupaiPush");
                }
            }

            // 延迟1000ms播放出牌动画
            try {
                Thread.sleep(600);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            // 是否可以吃碰杠胡
            boolean isCanChiPengGangHu = true;

            if(room.getYjType()>=2 || (room.isGuangYou&&room.hasYouJinType(1))){

                isCanChiPengGangHu = false;
            }

            // 判断是否有玩家在双游，三游
            if(isCanChiPengGangHu){

                JSONArray askArray = new JSONArray();

                //1.检查有无胡
                String ask=null;
                List<String> askHu = checkIsHuByOtherSelf(roomNo, pai);
                //有胡
                if(askHu.size()>0){

                    // 下家当前游金状态
                    int youjining = room.getUserPacketMap().get(xiajiaUUID).getYouJinIng();
                    // 一家有胡
                    if(askHu.size()==1){
                        if(!askHu.get(0).equals(xiajiaUUID)||youjining<=0){
                            askArray.add(obtainAskResult(askHu.get(0),7,pai));
                        }
                    }else{
                        // 多人有胡,返回第一个玩家
                        room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                        // 当上家出的牌下家可以胡，且下家处于游金状态，则直接过。
                        if(askHu.get(0).equals(xiajiaUUID)&&youjining>0){
                            // 下一个询问玩家
                            room.setNextAskAccount(room.getNextPlayer(askHu.get(1)));
                            return new Object[]{askHu.get(1),7,pai};
                        }
                        // 下一个询问玩家
                        room.setNextAskAccount(room.getNextPlayer(askHu.get(0)));
                        return new Object[]{askHu.get(0),7,pai};
                    }
                }
                // 从下家开始询问
                room.setNextAskAccount(xiajiaUUID);
                //2.检查有无杠
                ask = checkIsMingGang(roomNo, pai);
                //有杠
                if(ask!=null){
                    // 同时出现胡和杠事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,6,pai));
                        }else{ // 其他玩家有杠
                            String uuid = askArray.getJSONObject(0).getString("uuid");
                            room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                            // 下一个询问玩家
                            room.setNextAskAccount(room.getNextPlayer(uuid));
                            return new Object[]{uuid,7,pai};
                        }

                    }else{
                        askArray.add(obtainAskResult(ask,6,pai));
                    }
                }

                // 从下家开始询问
                room.setNextAskAccount(xiajiaUUID);
                //3.检查有无碰
                ask=checkIsPeng(roomNo, pai);
                if(ask!=null){//有碰
                    // 同时出现胡和碰事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,5,pai));
                        }else{
                            // 其他玩家有碰
                            String uuid = askArray.getJSONObject(0).getString("uuid");
                            room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                            // 下一个询问玩家
                            room.setNextAskAccount(room.getNextPlayer(uuid));
                            return new Object[]{uuid,7,pai};
                        }
                    }else{
                        askArray.add(obtainAskResult(ask,5,pai));
                    }
                }

                // 从下家开始询问
                room.setNextAskAccount(xiajiaUUID);

                //4.检查有无吃
                Object[] chiback=checkIsChi(roomNo, pai, xiajiaUUID);
                //有吃
                if(chiback!=null&&chiback.length==2){
                    ask = (String) chiback[0];
                    // 同时出现胡和吃碰事件
                    if(askArray.size()>0){
                        if(askArray.getJSONObject(0).get("uuid").equals(ask.toString())){
                            askArray.add(obtainAskResult(ask,4,JSONArray.fromObject(chiback[1])));
                        }else{ // 其他玩家有胡杠碰事件
                            room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
                            room.setNextAskAccount(ask);
                        }
                    }else{
                        return new Object[]{ask,4,chiback[1]};
                    }
                }
                // 解决多事件询问问题
                if(askArray.size()>0){
                    String uuid = askArray.getJSONObject(0).getString("uuid");
                    return new Object[]{uuid,10,askArray};
                }
                //获取上次thisUUID，thisType
                if(room.getPlayerMap().get(room.getThisAccount())==null){
                    room.setThisAccount(chupaiId);
                }
                //设置下一个焦点人
                room.setNextThisUUID();
                obj=new Object[]{null,QZMJConstant.THIS_TYPE_ZUA,null};
                return obj;
            }else{
                //获取上次thisUUID，thisType
                if(room.getPlayerMap().get(room.getThisAccount())==null){
                    room.setThisAccount(chupaiId);
                }
                //设置下一个焦点人
                room.setNextThisUUID();
                obj=new Object[]{null,QZMJConstant.THIS_TYPE_ZUA,null};
                return obj;
            }
        }
        return obj;
    }

    /**
     * 出牌
     */
    public boolean chupai(String roomNo,int oldPai, String chupaiId){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            boolean back = room.getUserPacketMap().get(chupaiId).removeMyPai(oldPai);
            if(back){
                //下次需要判断的人
                String nextAsk = room.getNextPlayer(chupaiId);
                //设置下次询问的人
                room.setNextAskAccount(nextAsk);
                //设置下次询问的事件
                room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                //设置上次出的牌
                room.setLastPai(oldPai);
                //设置出牌人
                room.setLastAccount(chupaiId);
                //记录出牌记录
                room.addKaijuList(room.getPlayerIndex(chupaiId), 0,new int []{oldPai});
                return true;
            }
        }
        return false;
    }

    /**
     * 获取询问数据
     * @param askAccount
     * @param type
     * @param obj
     * @return
     */
    public JSONObject obtainAskResult(String askAccount,int type,Object obj) {
        JSONObject askResult = new JSONObject();
        askResult.put("uuid", String.valueOf(askAccount));
        askResult.put("type", type);
        askResult.put("value", obj);
        return askResult;
    }

    /**
     * 组织数据，抓牌，并返回数据
     * @param mjieguo
     * @param room
     * @param buhuaData
     * @return
     */
    public boolean detailDataByZhuaPai(JSONArray mjieguo, QZMJGameRoom room, JSONObject buhuaData){
        //抓牌的人
        String zhuaID=null;
        //抓牌人触发的事件
        int type=0;
        //摸的牌
        int mopai=0;
        int gangType=0;
        int gangValue=0;

        if(mjieguo.size()>0){
            zhuaID = mjieguo.getJSONObject(0).getString("uuid");
            mopai = mjieguo.getJSONObject(0).getInt("pai");
            // 事件优先级：流局->补花->胡->杠
            if(mjieguo.size()==1){
                // 没有杠事件
                type = mjieguo.getJSONObject(0).getInt("type");
            }else if(mjieguo.getJSONObject(0).containsKey("gangValue")){
                // 包含杠事件
                gangType = mjieguo.getJSONObject(0).getInt("type");
                gangValue = mjieguo.getJSONObject(0).getInt("gangValue");
                // 普通事件
                type = mjieguo.getJSONObject(1).getInt("type");
            }
        }
        // 流局，结束当局游戏
        if(type==999){
            liuJu(room.getRoomNo());
        }else{
            boolean hasHua = false;
            if(type==10){
                hasHua = true;
                gangType=0;
            }
            //本次暗杠
            int[] anvalue=null;
            //本次抓杠
            int[] zhuagangvalue=null;
            if(gangType==2){
                anvalue=new int[]{gangValue,gangValue,gangValue,gangValue};
            }
            if(gangType==9){
                zhuagangvalue=new int[]{gangValue,gangValue,gangValue,gangValue};
                anvalue=new int[]{gangValue,gangValue,gangValue,gangValue};
            }
            //总牌数
            int zpaishu= room.getPai().length- room.getIndex();
            //本次焦点人
            int foucs= room.getPlayerIndex(zhuaID);
            int lastFoucs= room.getPlayerIndex(room.getLastAccount());
            Integer lastType=null;
            Object lastValue=null;
            Object lastAnValue=null;
            // 获取之前事件
            KaiJuModel jilu = room.getLastValue();
            if(jilu!=null){
                if(jilu.getType()==2){
                    //暗杠
                    lastType=2;
                    lastAnValue=new int[]{jilu.getValues()[0]};
                }else if(jilu.getType()==9){
                    //抓杠
                    lastType=9;
                    lastValue=new int[]{jilu.getValues()[0]};
                }else if(jilu.getType()==6){
                    //明杠
                    lastType=6;
                    lastValue=new int[]{jilu.getValues()[0]};
                }else if(jilu.getType()==1){
                    //出牌
                    lastType=1;
                    lastValue=jilu.getValues();
                }
            }
            // 通知所有的玩家
            String next=zhuaID;
            do {
                SocketIOClient client= GameMain.server.getClient(room.getPlayerMap().get(next).getUuid());
                JSONObject back=new JSONObject();
                back.put(QZMJConstant.zpaishu, zpaishu);
                back.put(QZMJConstant.foucs,foucs);
                back.put(QZMJConstant.foucsIndex, room.getFocusIndex());
                back.put(QZMJConstant.nowPoint, room.getNowPoint());
                back.put(QZMJConstant.lastFoucs, lastFoucs);
                //获取抓杠位置
                int zgindex = room.getUserPacketMap().get(zhuaID).buGangIndex(mopai);
                back.put(QZMJConstant.zgindex,zgindex);
                back.put(QZMJConstant.lastType,lastType);
                back.put(QZMJConstant.lastValue,lastValue);
                if(client!=null){
                    if(next.equals(zhuaID)){
                        // 返回事件询问类型
                        if(type==3&&gangType>0){
                            back.put(QZMJConstant.type,new int[]{type, gangType});
                        }else if(type==1&&gangType>0){
                            back.put(QZMJConstant.type,new int[]{gangType});
                        }else{
                            back.put(QZMJConstant.type,new int[]{type});
                        }
                        if(!back.containsKey(QZMJConstant.lastValue)){
                            back.put(QZMJConstant.lastValue,lastAnValue);
                        }
                        // 胡牌类型
                        if(type==3){
                            Playerinfo player = room.getPlayerMap().get(zhuaID);
                            UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(zhuaID);
                            if(userPacketQZMJ.getMyPai().contains(room.getJin())){

                                int huType=MaJiangCore.huPaiHasJin(userPacketQZMJ.getMyPai(), 0, room.getJin());
                                // 满足双、三游条件
                                int canYouJin = 0;
                                // 当玩家有游金时判断是几游
                                if(huType==QZMJConstant.HU_TYPE_YJ){

                                    if(userPacketQZMJ.getYouJin()==1){
                                        huType = QZMJConstant.HU_TYPE_YJ;
                                        if(mopai== room.getJin() || MaJiangCore.shuangSanYouPanDing(userPacketQZMJ.getMyPai(), 0, room.getJin())){
                                            // 可以开始双游
                                            canYouJin = 2;
                                        }
                                    }else if(userPacketQZMJ.getYouJin()==2){
                                        huType = QZMJConstant.HU_TYPE_SHY;
                                        if(mopai== room.getJin() || MaJiangCore.shuangSanYouPanDing(userPacketQZMJ.getMyPai(), 0, room.getJin())){
                                            // 可以开始三游
                                            canYouJin = 3;
                                        }
                                    }else if(userPacketQZMJ.getYouJin()==3){
                                        huType = QZMJConstant.HU_TYPE_SY;
                                    }else{
                                        // 三金倒
                                        if(userPacketQZMJ.getPlayerJinCount(room.getJin())==3){
                                            huType = QZMJConstant.HU_TYPE_SJD;
                                        }else{
                                            huType = QZMJConstant.HU_TYPE_ZM;
                                        }
                                    }
                                }

                                // 判断是否是天胡
                                if(huType == QZMJConstant.HU_TYPE_ZM){

                                    List<KaiJuModel> paiJuList = room.getKaiJuList();
                                    // 摸牌次数
                                    int moPaiConut = 0;
                                    if(paiJuList!=null&&paiJuList.size()<10){
                                        for (KaiJuModel kaiJu : paiJuList) {
                                            if(kaiJu.getType()==1){
                                                moPaiConut ++;
                                            }
                                        }
                                        if(moPaiConut<=1){
                                            huType = QZMJConstant.HU_TYPE_TH;
                                        }
                                    }
                                }

                                back.put("huType", huType);
                                back.put("youjin", canYouJin);

                            }else{
                                back.put("huType",QZMJConstant.HU_TYPE_ZM);
                                back.put("youjin", 0);
                            }
                        }
                        // 出牌或者胡
                        if(type==1||type==3){
                            // 牌局中剩余牌数（包含其他玩家手牌）
                            List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
                            for(String uuid: room.getPlayerMap().keySet()) {
                                if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                                    if(!zhuaID.equals(uuid)){
                                        shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
                                    }
                                }
                            }

                            // 出牌提示
                            JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(zhuaID).getMyPai(), room.getJin(), shengyuList);
                            back.put("tingTip",tingTip);
                        }
                        back.put(QZMJConstant.gangvalue,anvalue);
                        back.put(QZMJConstant.value, new int[]{mopai});

                        // 补花触发事件
                        if(buhuaData!=null){
                            back.put("lastType", -10);
                            back.put("huaCount", buhuaData.get("huaCount"));
                            back.put("huaValue", buhuaData.get("huaValue"));
                            back.put("lastValue", buhuaData.get("lastValue"));
                        }
                        if(anvalue!=null&&anvalue.length>0){
                            CommonConstant.sendMsgEventToSingle(client,String.valueOf(back),"gameActionPush");
                        }else{
                            CommonConstant.sendMsgEventToSingle(client,String.valueOf(back),"gameChupaiPush");
                        }

                    }else{
                        back.put(QZMJConstant.gangvalue,zhuagangvalue);
                        back.put(QZMJConstant.type,new int[]{1});
                        back.put(QZMJConstant.value, null);
                        back.put(QZMJConstant.lastValue,lastValue);
                        CommonConstant.sendMsgEventToSingle(client,String.valueOf(back),"gameChupaiPush");
                    }
                }
                next= room.getNextPlayer(next);
            } while (!zhuaID.equals(next));

            // 摸牌时触发补花
            if(hasHua){
                buHuaByMoPai(room.getRoomNo(), zhuaID);
            }
        }
        return true;
    }

    /**
     * 组织数据，出牌，并返回数据
     * @param jieguo
     * @param roomNo
     * @return
     */
    public boolean detailDataByChuPai(Object[] jieguo,String roomNo){
        if (Dto.isNull(jieguo)) {
            return false;
        }
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String thisask = (String) jieguo[0];
        int type= Integer.valueOf(String.valueOf(jieguo[1]));
        //最后出的牌
        int pai = room.getLastPai();
        //上次出牌的人
        String chupai = room.getLastAccount();
        //获取总牌数
        int zpaishu=room.getPai().length-room.getIndex();
        //获取lastType
        int lastType=1;
        //获取lastValue
        int[] lastValue=new int[]{pai};
        int lastFoucs = room.getLastFocus();
        //下家抓牌事件，并通知所有人
        if(type==1){
            JSONArray mjieguo = moPai(roomNo, room.getThisAccount());
            detailDataByZhuaPai(mjieguo, room, null);
        }else{
            JSONObject result=new JSONObject();
            result.put(QZMJConstant.zpaishu, zpaishu);
            result.put(QZMJConstant.value, null);
            result.put(QZMJConstant.lastType, lastType);
            result.put(QZMJConstant.lastValue, lastValue);
            result.put(QZMJConstant.lastFoucs, lastFoucs);
            result.put(QZMJConstant.foucs, room.getPlayerIndex(thisask));
            result.put(QZMJConstant.foucsIndex, room.getPlayerIndex(chupai));
            result.put(QZMJConstant.nowPoint, room.getNowPoint());
            result.put(QZMJConstant.type, new int[]{type});

            if(type==7){
                //胡 询问事件
                result.put(QZMJConstant.huvalue, jieguo[2]);
            }else if(type==6){
                //杠  询问事件
                result.put(QZMJConstant.gangvalue, new Object[]{pai,pai,pai,pai});
            }else if(type==5){
                //碰  询问事件
                result.put(QZMJConstant.pengvalue, new int[]{pai,pai,pai});
            }else if(type==4){
                //吃  询问事件
                result.put(QZMJConstant.chivalue, jieguo[2]);
            }else if(type==10){
                //触发多事件询问
                JSONArray array = JSONArray.fromObject(jieguo[2]);
                int[] types = new int[array.size()];
                for (int i = 0; i < array.size(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    if(obj.getInt("type")==7){
                        result.put(QZMJConstant.huvalue, pai);
                    }else if(obj.getInt("type")==6){
                        result.put(QZMJConstant.gangvalue, new Object[]{pai,pai,pai,pai});
                    }else if(obj.getInt("type")==5){
                        result.put(QZMJConstant.pengvalue, new int[]{pai,pai,pai});
                    }else if(obj.getInt("type")==4){
                        result.put(QZMJConstant.chivalue, obj.get("value"));
                    }
                    types[i] = obj.getInt("type");
                }
                result.put(QZMJConstant.type, types);
            }
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(result),"gameActionPush");
        }
        return true;
    }

    /**
     * 过方法返回
     * Object[]{UUID,type,values};
     * @param jieguo
     * @param roomNo
     */
    public void detailDataByGuo(Object[] jieguo,String roomNo){

        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String thisask = (String) jieguo[0];
        //1.抓/出      2.暗杠     9.抓明杠     7.胡      6.杠      5.碰       4.吃
        int type=Integer.valueOf(String.valueOf(jieguo[1]));
        //最后出的牌
        int pai = room.getLastPai();
        //获取总牌数
        int zpaishu = room.getPai().length-room.getIndex();
        //获取lastType
        int lastType=1;
        //获取lastValue
        int[] lastValue=new int[]{pai};
        //组织数据
        JSONObject obj=new JSONObject();
        obj.put(QZMJConstant.zpaishu, zpaishu);
        obj.put(QZMJConstant.value, null);
        obj.put(QZMJConstant.lastType, lastType);
        obj.put(QZMJConstant.lastValue, lastValue);
        obj.put(QZMJConstant.lastFoucs, room.getPlayerIndex(room.getLastAccount()));
        obj.put(QZMJConstant.foucs, room.getPlayerIndex(thisask));
        obj.put(QZMJConstant.foucsIndex, room.getFocusIndex());
        obj.put(QZMJConstant.nowPoint, room.getNowPoint());
        obj.put(QZMJConstant.type, new int[]{type});
        if(type==7){
            //胡 询问事件
            JSONObject ishu=obj;
            ishu.put(QZMJConstant.huvalue, jieguo[2]);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(ishu),"gameActionPush");
        }else if(type==6){
            //杠  询问事件
            JSONObject isgang=obj;
            isgang.put(QZMJConstant.gangvalue,  new Object[]{jieguo[2],jieguo[2],jieguo[2],jieguo[2]});
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(isgang),"gameActionPush");
        }else if(type==5){
            //碰  询问事件
            JSONObject ispeng=obj;
            ispeng.put(QZMJConstant.pengvalue, new Object[]{jieguo[2],jieguo[2],jieguo[2]});
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(ispeng),"gameActionPush");
        }else if(type==4){
            //吃  询问事件
            JSONObject ischi=obj;
            ischi.put(QZMJConstant.chivalue, jieguo[2]);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(ischi),"gameActionPush");
        }else if(type==2){
            //询问暗杠
            JSONObject ischi=obj;
            ischi.put(QZMJConstant.gangvalue, new int[]{(Integer) jieguo[2],(Integer) jieguo[2],(Integer) jieguo[2],(Integer) jieguo[2]});
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(ischi),"gameActionPush");
        }else if(type==9){
            //询问抓杠
            JSONObject ischi=obj;
            ischi.put(QZMJConstant.gangvalue, new int[]{(Integer) jieguo[3]});
            //获取抓杠位置
            int zgindex = room.getUserPacketMap().get(thisask).buGangIndex((Integer)jieguo[2]);
            ischi.put(QZMJConstant.zgindex, zgindex);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(ischi),"gameActionPush");
        }else if(type==10){
            //触发多事件询问
            JSONObject result=obj;
            JSONArray array = JSONArray.fromObject(jieguo[2]);
            int[] types = new int[array.size()];
            for (int i = 0; i < array.size(); i++) {
                JSONObject data = array.getJSONObject(i);
                if(data.getInt("type")==7){
                    result.put(QZMJConstant.huvalue, pai);
                }else if(data.getInt("type")==6){
                    result.put(QZMJConstant.gangvalue, new Object[]{pai,pai,pai,pai});
                }else if(data.getInt("type")==5){
                    result.put(QZMJConstant.pengvalue, new int[]{pai,pai,pai});
                }else if(data.getInt("type")==4){
                    result.put(QZMJConstant.chivalue, data.get("value"));
                }
                types[i] = data.getInt("type");
            }
            result.put(QZMJConstant.type, types);
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(result),"gameActionPush");
        }else if(type==1){
            // 出牌或下家抓牌
            if(thisask!=null&&jieguo[2]!=null){
                //出牌
                JSONObject chupai=obj;

                // 牌局中剩余牌数（包含其他玩家手牌）
                List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
                for(String uuid:room.getPlayerMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        if(!thisask.equals(uuid)){
                            shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
                        }
                    }
                }

                // 出牌提示
                JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(thisask).getMyPai(), room.getJin(), shengyuList);
                chupai.put("tingTip",tingTip);
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(thisask).getUuid(),String.valueOf(chupai),"gameActionPush");
            }else{
                //下家抓牌事件，并通知所有人
                JSONArray mjieguo=moPai(roomNo, room.getThisAccount());
                detailDataByZhuaPai(mjieguo, room, null);
            }
        }
    }


    /**
     * 组织数据，杠，碰，胡，过，事件
     * @param roomNo
     * @param type
     * @param clientId
     * @param value
     */
    public void detailDataByChiGangPengHu(String roomNo,int type,String clientId,int[] value){
        // -2：暗杠结果事件      -3：自摸结果事件       -4：吃结果事件       -5：碰结果事件        -6：明杠结果事件      -7：胡  结果事件        -9：抓明杠
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Set<String> uuids = room.getPlayerMap().keySet();
        int lastpai = room.getLastPai();
        int lastmopai = room.getLastMoPai();
        JSONObject back=new JSONObject();
        back.put(QZMJConstant.foucs,room.getPlayerIndex(clientId));
        back.put(QZMJConstant.foucsIndex, room.getFocusIndex());
        back.put(QZMJConstant.nowPoint, room.getNowPoint());
        back.put(QZMJConstant.zpaishu, room.getPai().length-room.getIndex());
        int lastFoucs=room.getPlayerIndex(room.getLastAccount());
        back.put(QZMJConstant.lastFoucs,lastFoucs);
        switch (type) {
            case -2:
                // -2：暗杠结果事件
                JSONObject objAnGang=back;
                objAnGang.put(QZMJConstant.type,1);
                objAnGang.put(QZMJConstant.lastType,-2);
                objAnGang.put(QZMJConstant.lastGangvalue,new int[]{value[1],value[1],value[1],value[1]});
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(objAnGang),"gameActReultPush");
                break;
            case -3:
                // -3：自摸结果事件
                JSONObject objZiMo=new JSONObject();
                objZiMo.put(QZMJConstant.type,1);
                objZiMo.put(QZMJConstant.lastType,-3);
                objZiMo.put(QZMJConstant.lastValue,new int[]{lastmopai});
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(clientId).getUuid(),String.valueOf(objZiMo),"gameActReultPush");
                break;
            case -4:
                // -4：吃结果事件
                JSONObject objchi=back;
                objchi.put(QZMJConstant.type,1);
                objchi.put(QZMJConstant.lastType,-4);
                objchi.put(QZMJConstant.lastValue,new int[]{lastpai});
                objchi.put(QZMJConstant.lastChiValue,value);

                // 牌局中剩余牌数（包含其他玩家手牌）
                List<Integer> shengyuList = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
                for(String uuid:room.getPlayerMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        if(!clientId.equals(uuid)){
                            shengyuList.addAll(room.getUserPacketMap().get(uuid).getMyPai());
                        }
                    }
                }

                for(String uuid:room.getPlayerMap().keySet()){
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        // 听牌提示
                        if(uuid.equals(clientId)){
                            // 出牌提示
                            JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(clientId).getMyPai(), room.getJin(), shengyuList);
                            objchi.put("tingTip",tingTip);
                        }
                    }
                    CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(objchi),"gameActReultPush");
                }
                break;
            case -5:
                // -5：碰结果事件
                JSONObject objpeng=back;
                objpeng.put(QZMJConstant.type,1);
                objpeng.put(QZMJConstant.lastPengvalue,new int[]{lastpai,lastpai,lastpai});
                objpeng.put(QZMJConstant.lastType,-5);
                objpeng.put(QZMJConstant.lastValue,new int[]{lastpai});
                // 牌局中剩余牌数（包含其他玩家手牌）
                List<Integer> shengyuList1 = new ArrayList<Integer>(Dto.arrayToList(room.getPai(), room.getIndex()));
                for(String uuid:room.getPlayerMap().keySet()) {
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        if(!clientId.equals(uuid)){
                            shengyuList1.addAll(room.getUserPacketMap().get(uuid).getMyPai());
                        }
                    }
                }
                //通知所有玩家碰
                for(String uuid:uuids){
                    if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                        // 听牌提示
                        if(uuid.equals(clientId)){
                            // 出牌提示
                            JSONArray tingTip = MaJiangCore.tingPaiTip(room.getUserPacketMap().get(clientId).getMyPai(), room.getJin(), shengyuList1);
                            objpeng.put("tingTip",tingTip);
                        }
                        CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(uuid).getUuid(),String.valueOf(objpeng),"gameActReultPush");
                    }
                }
                break;
            case -6:
                // -6：明杠结果事件
                JSONObject objMingGang=back;
                objMingGang.put(QZMJConstant.type,1);
                objMingGang.put(QZMJConstant.lastType,-6);
                objMingGang.put(QZMJConstant.lastGangvalue,new int[]{value[1],value[1],value[1],value[1]});
                objMingGang.put(QZMJConstant.lastValue,new int[]{lastpai});
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(objMingGang),"gameActReultPush");
                break;
            case -7:
                // -7：胡  结果事件
                JSONObject objHu=new JSONObject();
                objHu.put(QZMJConstant.type,1);
                objHu.put(QZMJConstant.lastType,-7);
                objHu.put(QZMJConstant.lastValue,new int[]{lastpai});
                CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(clientId).getUuid(),String.valueOf(objHu),"gameActReultPush");
                break;
            case -9:
                // -9：抓明杠 结果事件
                JSONObject objZhuangGang=back;
                objZhuangGang.put(QZMJConstant.type,1);
                objZhuangGang.put(QZMJConstant.lastType,-9);
                objZhuangGang.put(QZMJConstant.lastGangvalue,new int[]{value[1],value[1],value[1],value[1]});
                //获取抓杠位置
                int zgindex = room.getUserPacketMap().get(clientId).buGangIndex(value[1]);
                objZhuangGang.put(QZMJConstant.lastzgindex,zgindex);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(objZhuangGang),"gameActReultPush");
                break;
            default:
                break;
        }
    }

    /**
     * 流局
     * @param roomNo
     */
    public void liuJu(String roomNo) {
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if(room.getIndex() >= room.getPai().length - QZMJConstant.LEFT_PAI_COUNT && room.getGameStatus() > 0){
                room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_SUMMARY);
                JSONArray array = new JSONArray();
                for(String account : room.getPlayerMap().keySet()){
                    if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null) {
                        JSONObject data = new JSONObject();
                        UserPacketQZMJ userPacketQZMJ = room.getUserPacketMap().get(account);
                        Playerinfo player = room.getPlayerMap().get(account);
                        //获取玩家吃杠碰的牌
                        List<Integer> paiList = userPacketQZMJ.getHistoryList();
                        // 手牌排序
                        Collections.sort(userPacketQZMJ.getMyPai());
                        paiList.addAll(userPacketQZMJ.getMyPai());
                        data.put("myPai", paiList.toArray());
                        data.put("isWinner", 0);
                        data.put("fan", 0);
                        data.put("fanDetail", userPacketQZMJ.getFanDetail(userPacketQZMJ.getMyPai(),room,account));
                        data.put("score", player.getScore());
                        data.put("player", player.getName());
                        data.put("headimg", player.getRealHeadimg());
                        data.put("hua", userPacketQZMJ.getHuaList().size());
                        data.put("myIndex", player.getMyIndex());
                        data.put("huaValue", JSONArray.fromObject(userPacketQZMJ.getHuaList()));
                        data.put("gangValue", JSONArray.fromObject(userPacketQZMJ.getGangValue()));
                        // 判断玩家是否是庄家
                        if(account.equals(room.getBanker())){
                            data.put("zhuang", 1);
                            // 庄家底分翻倍
                            data.put("difen", room.getScore()*(2 + room.getBankerTimes()-1));
                        }else{
                            data.put("zhuang", 0);
                            data.put("difen", room.getScore());
                        }
                        array.add(data);
                    }
                }
                // 返回数据
                JSONObject result = new JSONObject();
                result.put("type", 0);
                result.put("isLiuju", CommonConstant.GLOBAL_YES);
                result.put("data", array);
                // 流局算连庄
                room.addBankTimes();
                // 保存结算记录
                room.addKaijuList(-1, 999, new int[]{});
                // 保存结算汇总数据
                if(room.getGameCount() == room.getGameIndex()){
                    JSONArray jiesuanArray = obtainFinalSummaryArray(room);
                    result.put("data1", jiesuanArray);
                    result.put("type", 1);
                    room.setIsClose(CommonConstant.CLOSE_ROOM_TYPE_FINISH);
                    room.setGameStatus(QZMJConstant.QZ_GAME_STATUS_FINAL_SUMMARY);
                }
                room.setSummaryData(result);
                CommonConstant.sendMsgEventToAll(room.getAllUUIDList(),String.valueOf(result),"gameLiuJuPush");
                updateUserScore(roomNo);
                if (room.getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
                    saveGameLog(roomNo);
                }
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_YB) {
                    saveUserDeduction(roomNo);
                }
                if (room.getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                    updateRoomCard(roomNo);
                }
            }
        }
    }

    /**
     * 判断是否有自摸
     */
    public boolean checkIsHuByMySelf(String roomNo,int oldPai){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = room.getThisAccount();
            if(account!=null&&RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account)!=null){
                // 自摸
                room.setNextAskType(QZMJConstant.ASK_TYPE_HU_BY_MYSELF);
                room.setNextAskAccount(account);
                //获取我的手牌
                List<Integer> myPai = room.getUserPacketMap().get(account).getMyPai();
                return MaJiangCore.isHu(myPai, 0, room.getJin());
            }
        }

        return false;
    }

    /**
     * 检查是否是平胡
     * @param roomNo
     * @param oldPai
     * @return
     */
    public List<String> checkIsHuByOtherSelf(String roomNo,int oldPai){
        List<String> uuidList = new ArrayList<String>();
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (!room.isNotChiHu) {
                // 获取本次事件询问人
                String nextAsk=room.getNextAskAccount();

                // 轮到出牌的玩家，一轮询问结束，进入下一个事件询问（杠事件）
                if(nextAsk.equals(room.getLastAccount())){
                    // 跳过出牌的玩家
                    nextAsk = room.getNextPlayer(nextAsk);
                    //设置下一次询问的事件
                    room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
                    //设置下一次询问的人
                    room.setNextAskAccount(nextAsk);
                }

                int jinPai = room.getJin();

                while(room.getNextAskType()==QZMJConstant.ASK_TYPE_HU_OTHER){

                    //获取玩家的手牌
                    List<Integer> mypai=room.getUserPacketMap().get(nextAsk).getMyPai();
                    if(room.hasJinNoPingHu && mypai.contains(jinPai)){
                        // 游金不能平胡
                    }else{
                        // 玩家是否可以平胡
                        boolean isAllowHu = true;

                        int youjining = room.getUserPacketMap().get(nextAsk).getYouJinIng();
                        // 玩家在游金或金数大于2，不能平胡
                        if(youjining>0){
                            isAllowHu = false;
                        }else if (room.getUserPacketMap().get(nextAsk).getPlayerJinCount(jinPai)>=2) {
                            isAllowHu = false;
                        }

                        if(isAllowHu){

                            boolean back= MaJiangCore.isHu(mypai, oldPai, jinPai);
                            if(back){
                                uuidList.add(nextAsk);
                            }
                        }
                    }

                    // 设置下一个玩家
                    nextAsk = room.getNextPlayer(nextAsk);
                    // 一轮询问结束，进入下一个事件询问（杠事件）
                    // 轮到出牌的玩家
                    if(nextAsk.equals(room.getLastAccount())){
                        // 跳过出牌的玩家
                        nextAsk=room.getNextPlayer(nextAsk);
                        //设置下一次询问的事件
                        room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
                        //设置下一次询问的人
                        room.setNextAskAccount(nextAsk);
                    }else{
                        // 换下个人询问
                        room.setNextAskType(QZMJConstant.ASK_TYPE_HU_OTHER);
                        room.setNextAskAccount(nextAsk);
                    }

                }
            }
        }
        return uuidList;
    }

    /**
     * 判断是否有暗杠
     * @param roomNo
     * @param newPai
     * @return
     */
    public int[] checkIsAnGang(String roomNo,int newPai){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String account = room.getThisAccount();
            if(account!=null&&RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account)!=null){
                // 自摸
                room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_AN);
                room.setNextAskAccount(account);
                //获取我的手牌
                List<Integer> myPai = room.getUserPacketMap().get(account).getMyPai();
                List<DontMovePai> penghistory=room.getUserPacketMap().get(account).getPengList();
                return MaJiangCore.isGang(myPai, newPai,1, penghistory);
            }
        }
        return new int[]{0};
    }

    /**
     * 判断是否有明杠
     * @param roomNo
     * @param oldPai
     * @return
     */
    public String checkIsMingGang(String roomNo,int oldPai){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            //本次需要判断的人
            String askAccount = room.getNextAskAccount();
            //下次需要判断的人
            String nextAsk = room.getNextPlayer(askAccount);
            //判断下次询问的人是否是出牌人如果是出牌人进行下一次事件
            if(nextAsk.equals(room.getLastAccount())){
                nextAsk = room.getNextPlayer(nextAsk);
                //设置下一次询问的事件
                room.setNextAskType(QZMJConstant.ASK_TYPE_PENG);
                //设置下一次询问的人
                room.setNextAskAccount(nextAsk);
            }else{
                //设置下一次询问的事件
                room.setNextAskType(QZMJConstant.ASK_TYPE_GANG_MING);
                //设置下一次询问的人
                room.setNextAskAccount(nextAsk);
            }
            //获取我的手牌
            List<Integer> myPai=room.getUserPacketMap().get(askAccount).getMyPai();
            int[] back=MaJiangCore.isGang(myPai, oldPai, 2, null);
            if(back[0]>0){
                return askAccount;
            }else if(room.getNextAskType()==QZMJConstant.ASK_TYPE_GANG_MING){
                return checkIsMingGang(roomNo, oldPai);
            }
        }
        return null;
    }

    /**
     * 判断是否有碰
     * @param roomNo
     * @param oldPai
     * @return
     */
    public String checkIsPeng(String roomNo,int oldPai){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            String askAccount = room.getNextAskAccount();
            String nextAsk = room.getNextPlayer(askAccount);
            if(nextAsk.equals(room.getLastAccount())){
                nextAsk = room.getNextPlayer(nextAsk);
                //设置下一次询问的事件
                room.setNextAskType(QZMJConstant.ASK_TYPE_CHI);
                //设置下一次询问的人
                room.setNextAskAccount(nextAsk);
            }else{
                //设置下一次询问的事件
                room.setNextAskType(QZMJConstant.ASK_TYPE_PENG);
                //设置下一次询问的人
                room.setNextAskAccount(nextAsk);
            }
            //获取我的手牌
            List<Integer> myPai = room.getUserPacketMap().get(askAccount).getMyPai();
            int[] back=MaJiangCore.isPeng(myPai, oldPai);
            if(back[0]==1){
                return askAccount;
            }else if(room.getNextAskType() == QZMJConstant.ASK_TYPE_PENG){
                return checkIsPeng(roomNo, oldPai);
            }

        }

        return null;
    }

    /**
     * 判断是否有吃
     * @param roomNo
     * @param oldPai
     * @param nextAccount
     * @return
     */
    public Object[] checkIsChi(String roomNo,int oldPai, String nextAccount){
        if(RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo)!=null){
            QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if (!room.isNotChiHu) {
                String nextAsk = room.getNextAskAccount();
                // 只有出牌玩家的下家才可以吃
                if(nextAsk.equals(nextAccount)){
                    //获取我的手牌
                    List<Integer> myPai = room.getUserPacketMap().get(nextAsk).getMyPai();
                    //设置下一次询问 完成
                    room.setNextAskType(QZMJConstant.ASK_TYPE_FINISH);
                    List<int[]> back=MaJiangCore.isChi(myPai, oldPai, room.getJin());
                    if(back!=null&&back.size()>0){
                        return new Object[]{nextAsk,back};
                    }
                }
            }
        }
        return null;
    }

    /**
     * 设置下次询问
     * @param roomNo
     * @param nextAsk
     * @param thisAskType
     * @param nextAskType
     */
    public void setNextAsk(String roomNo,String nextAsk,int thisAskType,int nextAskType) {
        //判断下次询问的人是否是出牌人如果是出牌人进行下一次事件
        if(nextAsk.equals(((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).getLastAccount())){
            nextAsk = ((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).getNextPlayer(nextAsk);
            //设置下一次询问的事件
            ((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).setNextAskType(nextAskType);
            //设置下一次询问的人
            ((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).setNextAskAccount(nextAsk);
        }else{
            //设置下一次询问的事件
            ((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).setNextAskType(thisAskType);
            //设置下一次询问的人
            ((QZMJGameRoom)RoomManage.gameRoomMap.get(roomNo)).setNextAskAccount(nextAsk);
        }
    }
}
