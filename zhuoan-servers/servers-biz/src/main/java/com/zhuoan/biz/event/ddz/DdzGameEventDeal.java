package com.zhuoan.biz.event.ddz;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.ddz.UserPacketDdz;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.DdzConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:47 2018/6/27
 * @Modified By:
 **/
@Component
public class DdzGameEventDeal {

    @Resource
    private UserBiz userBiz;

    /**
     * 创建房间通知自己
     * @param client
     */
    public void createRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        JSONObject roomData = obtainRoomData(roomNo, account);
        // 数据不为空
        if (!Dto.isObjNull(roomData)) {
            JSONObject result = new JSONObject();
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("data", roomData);
            // 通知自己
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "enterRoomPush_DDZ");
        }
    }

    /**
     * 加入房间通知
     * @param client
     * @param data
     */
    public void joinRoom(SocketIOClient client, Object data) {
        // 进入房间通知自己
        createRoom(client, data);
        JSONObject joinData = JSONObject.fromObject(data);
        // 非重连通知其他玩家
        if (joinData.containsKey("isReconnect") && joinData.getInt("isReconnect") == 0) {
            String account = joinData.getString(CommonConstant.DATA_KEY_ACCOUNT);
            DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(joinData.getString(CommonConstant.DATA_KEY_ROOM_NO));
            JSONObject obj = obtainPlayerInfo(room.getRoomNo(),account);
            // 通知玩家
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(account), String.valueOf(obj), "playerEnterPush_DDZ");
        }
    }

    /**
     * 准备
     * @param client
     * @param data
     */
    public void gameReady(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_INIT, client) &&
            !CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_READY, client) &&
            !CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_SUMMARY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus() == DdzConstant.DDZ_USER_STATUS_READY) {
            return;
        }
        // 设置玩家准备状态
        room.getUserPacketMap().get(account).setStatus(DdzConstant.DDZ_USER_STATUS_READY);
        // 设置房间准备状态
        if (room.getGameStatus() != DdzConstant.DDZ_GAME_STATUS_READY) {
            room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_READY);
        }
        // 房间内所有玩家都已经完成准备且人数为3通知开始游戏,否则通知玩家准备
        if (isAllReady(roomNo) && room.getPlayerMap().size() >= DdzConstant.DDZ_PLAYER_NUMBER) {
            // 初始化房间信息
            startGame(roomNo);
        } else {
            JSONObject result = new JSONObject();
            for (String player : obtainAllPlayerAccount(roomNo)) {
                int readyStatus = 0;
                if (room.getUserPacketMap().get(player).getStatus()==DdzConstant.DDZ_USER_STATUS_READY) {
                    readyStatus = 1;
                }
                result.put(room.getPlayerMap().get(player).getMyIndex(),readyStatus);
            }
            CommonConstant.sendMsgEventToAll(room.getAllUUIDList(), String.valueOf(result), "gameReadyPush_DDZ");
        }
    }

    /**
     * 叫、抢地主
     * @param client
     * @param data
     */
    public void gameBeLandlord(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus() != DdzConstant.DDZ_USER_STATUS_READY) {
            return;
        }
        // 当前不是该玩家操作
        if (room.getPlayerMap().get(account).getMyIndex()!=room.getFocusIndex()) {
            return;
        }
        // 设置下一个操作玩家
        room.setFocusIndex(obtainNextPlayerIndex(roomNo,account));
        // 是否抢地主
        int isChoice = postData.getInt(DdzConstant.DDZ_DATA_KEY_IS_CHOICE);
        // 1-叫地主  2-抢地主
        int type = postData.getInt(DdzConstant.DDZ_DATA_KEY_TYPE);
        // 抢地主翻倍
        if (type==DdzConstant.DDZ_BE_LANDLORD_TYPE_ROB&&isChoice==CommonConstant.GLOBAL_YES) {
            room.setMultiple(room.getMultiple()*2);
        }
        // 添加抢地主记录
        JSONObject record = new JSONObject();
        record.put("account",account);
        record.put("lastNote",type);
        record.put("isChoice",isChoice);
        room.getOperateRecord().add(record);
        // 设置下个操作玩家
        room.setFocusIndex(obtainNextDoLandlordIndex(roomNo,account));
        // 通知玩家
        JSONObject result = new JSONObject();
        if (isAllChoice(roomNo)) {
            // 确定地主
            determineLandlord(roomNo);
            // 状态改变
            result.put("card",obtainLandlordCard(roomNo));
            result.put("landlord",obtainLandlordIndex(roomNo));
        }
        result.put("type",type);
        result.put("num",room.getPlayerMap().get(account).getMyIndex());
        result.put("isChoice",isChoice);
        result.put("multiple",room.getMultiple());
        result.put("nextChoice",room.getFocusIndex());
        result.put("gameStatus",room.getGameStatus());
        if (type==DdzConstant.DDZ_BE_LANDLORD_TYPE_CALL&&isChoice==CommonConstant.GLOBAL_NO) {
            result.put("nextType",DdzConstant.DDZ_BE_LANDLORD_TYPE_CALL);
        }else {
            result.put("nextType",DdzConstant.DDZ_BE_LANDLORD_TYPE_ROB);
        }
        for (String player : obtainAllPlayerAccount(roomNo)) {
            result.put("myPai",room.getUserPacketMap().get(player).getMyPai());
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(player).getUuid(),String.valueOf(result),"gameLandlordPush_DDZ");
        }
    }

    /**
     * 出牌
     * @param client
     * @param data
     */
    public void gameEvent(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_GAME_IN, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        if (room.getUserPacketMap().get(account).getStatus() != DdzConstant.DDZ_USER_STATUS_READY) {
            return;
        }
        // 当前不是该玩家操作
        if (room.getPlayerMap().get(account).getMyIndex()!=room.getFocusIndex()) {
            return;
        }
        JSONObject result = new JSONObject();
        // 1-出牌 2-不出
        int type = postData.getInt(DdzConstant.DDZ_DATA_KEY_TYPE);
        // 出的牌
        List<String> paiList = postData.getJSONArray(DdzConstant.DDZ_DATA_KEY_PAI_LIST);
        // 牌型
        int paiType = DdzCore.obtainCardType(paiList);
        // 游戏是否结束
        boolean isGameOver = false;
        // 出牌
        if (type==DdzConstant.DDZ_GAME_EVENT_TYPE_YES) {
            List<String> oldList = room.getLastCard();
            // 上个出牌玩家是自己不需要检查牌的大小
            if (account.equals(room.getLastOperateAccount())) {
                oldList = new ArrayList<>();
            }
            // 选中的牌无法出
            if (!DdzCore.checkCard(oldList,paiList)) {
                result.put("check",DdzConstant.DDZ_GAME_EVENT_RESULT_ILLEGAL);
                result.put("msg","所选牌型不合规范");
                CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gameEventPush_DDZ");
                return;
            }
            // 炸弹翻倍
            if (paiType==DdzConstant.DDZ_CARD_TYPE_BOMB) {
                room.setMultiple(room.getMultiple()*2);
            }
            // 移除手牌
            room.getUserPacketMap().get(account).getMyPai().removeAll(paiList);
            // 设置上一次出牌
            room.setLastCard(paiList);
            // 设置上一个出牌人
            room.setLastOperateAccount(account);
        }
        // 设置下一个出牌玩家
        room.setFocusIndex(obtainNextPlayerIndex(roomNo,account));
        // 没有手牌游戏结束
        if (room.getUserPacketMap().get(account).getMyPai().size()==0) {
            isGameOver = true;
        }
        // 添加出牌记录
        JSONObject record = new JSONObject();
        record.put("account",account);
        record.put("lastNote",3);
        record.put("cardList",paiList);
        room.getOperateRecord().add(record);
        // 通知玩家
        if (type==DdzConstant.DDZ_GAME_EVENT_TYPE_YES) {
            result.put("check",DdzConstant.DDZ_GAME_EVENT_RESULT_YES);
            result.put("lastPai",room.getLastCard());
        }else {
            result.put("check",DdzConstant.DDZ_GAME_EVENT_RESULT_NO);
            result.put("lastPai",new ArrayList<String>());
        }
        result.put("paiType",paiType);
        result.put("cardNum",room.getUserPacketMap().get(account).getMyPai().size());
        result.put("num",room.getPlayerMap().get(account).getMyIndex());
        result.put("nextNum",obtainNextPlayerIndex(roomNo,account));
        result.put("multiple",room.getMultiple());
        result.put("isGameOver",CommonConstant.GLOBAL_NO);
        if (isGameOver) {
            // 结算
            summary(roomNo,account);
            result.put("isGameOver",CommonConstant.GLOBAL_YES);
            result.put("summaryData",obtainSummaryData(roomNo));
            // TODO: 2018/6/27 总结算
        }
        for (String player : obtainAllPlayerAccount(roomNo)) {
            result.put("myPai",room.getUserPacketMap().get(player).getMyPai());
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(player).getUuid(),String.valueOf(result),"gameEventPush_DDZ");
        }
    }

    public void gamePrompt(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_GAME_IN, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 当前不是该玩家操作
        if (room.getPlayerMap().get(account).getMyIndex()!=room.getFocusIndex()) {
            return;
        }
        JSONArray proList = new JSONArray();
        List<String> myPai = room.getUserPacketMap().get(account).getMyPai();
        // 还没出过牌或轮到自己出牌
        if (room.getLastCard().size()==0||account.equals(room.getLastOperateAccount())) {
            proList.addAll(changeFormat(DdzCore.obtainRepeatList(myPai,1,false)));
            proList.addAll(changeFormat(DdzCore.obtainRepeatList(myPai,2,false)));
            proList.addAll(changeFormat(DdzCore.obtainRepeatList(myPai,3,false)));
            proList.addAll(changeFormat(DdzCore.obtainRepeatList(myPai,4,false)));
        }else {
            proList.addAll(changeFormat(DdzCore.obtainAllCard(room.getLastCard(),myPai)));
        }
        if (proList.size()>0) {
            JSONObject result = new JSONObject();
            result.put("proList",proList);
            CommonConstant.sendMsgEventToSingle(client,String.valueOf(result),"gamePromptPush_DDZ");
        }else {
            postData.put("type",DdzConstant.DDZ_GAME_EVENT_TYPE_NO);
            postData.put("paiList",proList);
            gameEvent(client,postData);
        }

    }

    /**
     * 继续游戏
     * @param client
     * @param data
     */
    public void gameContinue(SocketIOClient client,Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 不满足准备条件直接忽略
        if (!CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_SUMMARY, client)&&
            !CommonConstant.checkEvent(postData, DdzConstant.DDZ_GAME_STATUS_READY, client)) {
            return;
        }
        // 房间号
        final String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 设置状态
        // 设置房间准备状态
        if (room.getGameStatus() != DdzConstant.DDZ_GAME_STATUS_READY) {
            room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_READY);
            for (String player : obtainAllPlayerAccount(roomNo)) {
                room.getUserPacketMap().get(player).setStatus(DdzConstant.DDZ_USER_STATUS_INIT);
            }
        }
        JSONObject result = new JSONObject();
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
    }

    /**
     * 重连
     * @param client
     * @param data
     */
    public void reconnectGame(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ROOM_NO)||
            !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)||
            !postData.containsKey("uuid")) {
            return;
        }
        String roomNo = postData.getString(CommonConstant.DATA_KEY_ROOM_NO);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userInfo = userBiz.getUserByAccount(account);
        // uuid不匹配
        if (!userInfo.containsKey("uuid")||Dto.stringIsNULL(userInfo.getString("uuid"))||
            !userInfo.getString("uuid").equals(postData.getString("uuid"))) {
            return;
        }
        JSONObject result = new JSONObject();
        if (client == null) {
            return;
        }
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo) || RoomManage.gameRoomMap.get(roomNo) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
            return;
        }
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不在当前房间内
        if (Dto.stringIsNULL(account) || !room.getPlayerMap().containsKey(account) || room.getPlayerMap().get(account) == null) {
            result.put("type", 0);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
            return;
        }
        // 刷新uuid
        room.getPlayerMap().get(account).setUuid(client.getSessionId());
        // 组织数据，通知玩家
        result.put("type", 1);
        result.put("data", obtainRoomData(roomNo, account));
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "reconnectGamePush_DDZ");
    }

    /**
     * 转换格式
     * @param list
     * @return
     */
    private List<String> changeFormat(List<List<String>> list) {
        List<String> newList = new ArrayList<>();
        for (List<String> stringList : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < stringList.size(); i++) {
                sb.append(stringList.get(i));
                if (i!=stringList.size()-1) {
                    sb.append(",");
                }
            }
            newList.add(String.valueOf(sb));
        }
        return newList;
    }

    /**
     * 开始游戏
     * @param roomNo
     */
    private void startGame(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 设置房间状态
        room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD);
        // 初始化房间信息
        initRoom(roomNo);
        // 设置手牌
        List<List<String>> cardList = DdzCore.shuffleAndDeal();
        int cardIndex = 0;
        List<String> accountList = obtainAllPlayerAccount(roomNo);
        for (String account : accountList) {
            room.getUserPacketMap().get(account).setMyPai(cardList.get(cardIndex));
            cardIndex++;
        }
        // 设置地主牌
        room.setLandlordCard(cardList.get(cardIndex));
        // 随机产生叫地主玩家
        room.setFocusIndex(RandomUtils.nextInt(DdzConstant.DDZ_PLAYER_NUMBER));
        // 通知玩家
        for (String account : accountList) {
            JSONObject result = new JSONObject();
            result.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            result.put("number",room.getFocusIndex());
            result.put("multiple",room.getMultiple());
            CommonConstant.sendMsgEventToSingle(room.getPlayerMap().get(account).getUuid(),String.valueOf(result),"gameStartPush_DDZ");
        }
    }

    /**
     * 初始化牌局信息
     * @param roomNo
     */
    private void initRoom(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 清空地主信息
        room.setLandlordAccount(null);
        // 清空上一次出牌
        room.getLastCard().clear();
        // 清空地主牌
        room.getLandlordCard().clear();
        // 清空叫、抢地主记录
        room.getOperateRecord().clear();
        // 初始化倍数
        room.setMultiple(1);
        // 初始化玩家
        for (String account : obtainAllPlayerAccount(roomNo)) {
            room.getUserPacketMap().get(account).setScore(0);
        }
    }

    /**
     * 结算
     * @param roomNo
     * @param winAccount
     */
    private void summary(String roomNo,String winAccount) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 农民输赢分数=倍数*底
        double farmerScore = Dto.mul(room.getMultiple(),room.getScore());
        if (winAccount.equals(room.getLandlordAccount())) {
            farmerScore = Dto.sub(0,farmerScore);
        }
        // 设置为玩家分数
        for (String account : obtainAllPlayerAccount(roomNo)) {
            if (account.equals(room.getLandlordAccount())) {
                room.getUserPacketMap().get(account).setScore(Dto.mul(-2,farmerScore));
            }else {
                room.getUserPacketMap().get(account).setScore(farmerScore);
            }
            double oldScore =  room.getPlayerMap().get(account).getScore();
            room.getPlayerMap().get(account).setScore(Dto.add(room.getUserPacketMap().get(account).getScore(),oldScore));
        }
        room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_SUMMARY);
    }

    /**
     * 确定地主
     * @param roomNo
     */
    private void determineLandlord(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String landlordAccount = null;
        // 获取最后一个选是的玩家
        for (int i = room.getOperateRecord().size() - 1; i >= 0; i--) {
            if (room.getOperateRecord().get(i).containsKey("isChoice")&&room.getOperateRecord().get(i).getInt("isChoice")==CommonConstant.GLOBAL_YES) {
                landlordAccount = room.getOperateRecord().get(i).getString("account");
                break;
            }
        }
        // 无人叫地主选默认
        if (!Dto.stringIsNULL(landlordAccount)) {
            room.setLandlordAccount(landlordAccount);
        }else {
            room.setLandlordAccount(room.getOperateRecord().get(0).getString("account"));
        }
        // 设置上个出牌玩家
        room.setLastOperateAccount(room.getLandlordAccount());
        // 设置焦点
        room.setFocusIndex(room.getPlayerMap().get(room.getLandlordAccount()).getMyIndex());
        // 添加地主牌
        List<String> landlordPai = room.getUserPacketMap().get(room.getLandlordAccount()).getMyPai();
        landlordPai.addAll(room.getLandlordCard());
        // 排序
        DdzCore.sortCard(landlordPai);
        room.getUserPacketMap().get(room.getLandlordAccount()).setMyPai(landlordPai);
        // 设置房间状态
        room.setGameStatus(DdzConstant.DDZ_GAME_STATUS_GAME_IN);
    }

    /**
     * 是否全部准备
     * @param roomNo
     * @return
     */
    private boolean isAllReady(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : obtainAllPlayerAccount(roomNo)) {
            if (room.getUserPacketMap().get(account).getStatus()!=DdzConstant.DDZ_USER_STATUS_READY) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否全部完成抢地主
     * @param roomNo
     * @return
     */
    private boolean isAllChoice(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<JSONObject> operateRecord = room.getOperateRecord();
        // 3条操作记录
        if (operateRecord.size()==DdzConstant.DDZ_PLAYER_NUMBER) {
            int choiceNum = 0;
            for (JSONObject record : operateRecord) {
                if (record.getInt("isChoice")==CommonConstant.GLOBAL_YES) {
                    choiceNum++;
                }
            }
            // 只有一个人叫或没人叫地主已经完成
            if (choiceNum<=1) {
                return true;
            }
        }
        // 4条操作记录
        if (operateRecord.size()==DdzConstant.DDZ_PLAYER_NUMBER+1) {
            return true;
        }
        return false;
    }

    /**
     * 获取结算数据
     * @param roomNo
     * @return
     */
    private JSONObject obtainSummaryData(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        // TODO: 2018/6/27 春天
        obj.put("isSpring",CommonConstant.GLOBAL_NO);
        obj.put("landlordWin",CommonConstant.GLOBAL_NO);
        if (!Dto.stringIsNULL(room.getWinner())&&room.getWinner().equals(room.getLandlordCard())) {
            obj.put("landlordWin",CommonConstant.GLOBAL_YES);
        }
        obj.put("gameIndex",room.getGameIndex());
        JSONObject array = new JSONObject();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            JSONObject object = new JSONObject();
            object.put("score",room.getUserPacketMap().get(account).getScore());
            object.put("name",room.getPlayerMap().get(account).getName());
            object.put("scoreLeft",room.getPlayerMap().get(account).getScore());
            object.put("isWin",CommonConstant.GLOBAL_NO);
            if (room.getUserPacketMap().get(account).getScore()>0) {
                object.put("isWin",CommonConstant.GLOBAL_YES);
            }
            object.put("isLandlord",CommonConstant.GLOBAL_NO);
            if (account.equals(room.getLandlordAccount())) {
                object.put("isLandlord",CommonConstant.GLOBAL_YES);
            }
            object.put("myPai",room.getUserPacketMap().get(account).getMyPai());
            array.put(room.getPlayerMap().get(account).getMyIndex(),object);
        }
        obj.put("array",array);
        return obj;
    }

    /**
     * 获取房间数据
     * @param roomNo
     * @param account
     */
    private JSONObject obtainRoomData(String roomNo,String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        obj.put("room_no",roomNo);
        obj.put("gameIndex",room.getGameIndex());
        obj.put("gameCount",room.getGameCount());
        obj.put("gameStatus",room.getGameStatus());
        obj.put("note",obtainReconnectNote(roomNo));
        obj.put("roomType",room.getRoomType());
        obj.put("myIndex",room.getPlayerMap().get(account).getMyIndex());
        obj.put("landlord",obtainLandlordIndex(roomNo));
        obj.put("time",room.getTimeLeft());
        obj.put("users",obtainAllPlayer(roomNo));
        obj.put("cardList",obtainLandlordCard(roomNo));
        obj.put("multiple",room.getMultiple());
        obj.put("myPai",room.getUserPacketMap().get(account).getMyPai());
        obj.put("focus",room.getFocusIndex());
        obj.put("map",obtainOperateRecord(roomNo));
        if (room.getGameStatus()==DdzConstant.DDZ_GAME_STATUS_SUMMARY) {
            obj.put("summaryData",obtainSummaryData(roomNo));
        }
        // TODO: 2018/6/27 总结算
        obj.put("isEnd",CommonConstant.GLOBAL_NO);
        obj.put("endData","");
        // TODO: 2018/6/27 解散
        obj.put("isClose",CommonConstant.GLOBAL_NO);
        obj.put("closeData","");
        return obj;
    }

    /**
     * 获取操作记录
     * @param roomNo
     * @return
     */
    private JSONObject obtainOperateRecord(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        JSONObject obj = new JSONObject();
        for (String account : obtainAllPlayerAccount(roomNo)) {
            obj.put(room.getPlayerMap().get(account).getMyIndex(),obtainLastOperateRecordByAccount(roomNo,account));
        }
        return obj;
    }

    /**
     * 获取玩家的最后一条操作记录
     * @param roomNo
     * @param account
     * @return
     */
    private JSONObject obtainLastOperateRecordByAccount(String roomNo,String account) {
        JSONObject obj = new JSONObject();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (int i = room.getOperateRecord().size() - 1; i >= 0; i--) {
            if (account.equals(room.getOperateRecord().get(i).getString("account"))) {
                return room.getOperateRecord().get(i);
            }
        }
        obj.put("lastNote",0);
        return obj;
    }

    /**
     * 获取所有玩家信息
     * @param roomNo
     * @return
     */
    private JSONArray obtainAllPlayer(String roomNo) {
        JSONArray array = new JSONArray();
        for(String account : obtainAllPlayerAccount(roomNo)){
            JSONObject playerInfo = obtainPlayerInfo(roomNo,account);
            if (!Dto.isObjNull(playerInfo)) {
                array.add(playerInfo);
            }
        }
        return array;
    }

    /**
     * 获取单个玩家信息
     * @param roomNo
     * @param account
     * @return
     */
    private JSONObject obtainPlayerInfo(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        Playerinfo player = room.getPlayerMap().get(account);
        JSONObject obj = new JSONObject();
        if(player!=null){
            UserPacketDdz up = room.getUserPacketMap().get(account);
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
            obj.put("userStatus", up.getStatus());
            obj.put("cardNum", up.getMyPai().size());
            obj.put("trusteeStatus", up.getIsTrustee());
        }
        return obj;
    }

    /**
     * 获取地主牌
     * @param roomNo
     * @return
     */
    private List<String> obtainLandlordCard(String roomNo) {
        List<String> landlordCard = new ArrayList<>();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()== DdzConstant.DDZ_GAME_STATUS_GAME_IN) {
            return room.getLandlordCard();
        }
        return landlordCard;
    }

    /**
     * 获取地主下标
     * @param roomNo
     * @return
     */
    private int obtainLandlordIndex(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getGameStatus()== DdzConstant.DDZ_GAME_STATUS_GAME_IN) {
            if (!Dto.stringIsNULL(room.getLandlordAccount())&&room.getPlayerMap().containsKey(room.getLandlordAccount())&&
                room.getPlayerMap().get(room.getLandlordAccount())!=null) {
                return room.getPlayerMap().get(room.getLandlordAccount()).getMyIndex();
            }
        }
        return CommonConstant.NO_BANKER_INDEX;
    }

    /**
     * 获取重连节点
     * @param roomNo
     * @return
     */
    private int obtainReconnectNote(String roomNo) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        switch (room.getGameStatus()) {
            case DdzConstant.DDZ_GAME_STATUS_INIT:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
            case DdzConstant.DDZ_GAME_STATUS_READY:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
            case DdzConstant.DDZ_GAME_STATUS_CHOICE_LANDLORD:
                // 有人叫地主之后返回抢地主
                for (JSONObject obj : room.getOperateRecord()) {
                    if (obj.getInt("isChoice")==CommonConstant.GLOBAL_YES) {
                        return DdzConstant.DDZ_RECONNECT_NODE_ROB;
                    }
                }
                return DdzConstant.DDZ_RECONNECT_NODE_CALL;
            case DdzConstant.DDZ_GAME_STATUS_GAME_IN:
                return DdzConstant.DDZ_RECONNECT_NODE_IN;
            case DdzConstant.DDZ_GAME_STATUS_SUMMARY:
                return DdzConstant.DDZ_RECONNECT_NODE_SUMMARY;
            case DdzConstant.DDZ_GAME_STATUS_FINAL_SUMMARY:
                return DdzConstant.DDZ_RECONNECT_NODE_FINAL_SUMMARY;
            default:
                return DdzConstant.DDZ_RECONNECT_NODE_READY;
        }
    }

    /**
     * 获取房间内的所有玩家
     * @param roomNo
     * @return
     */
    private List<String> obtainAllPlayerAccount(String roomNo) {
        List<String> accountList = new ArrayList<>();
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        for (String account : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(account)&&room.getUserPacketMap().get(account)!=null&&
                room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
                accountList.add(account);
            }
        }
        return accountList;
    }

    /**
     * 获取下个操作玩家下标
     * @param roomNo
     * @param account
     * @return
     */
    private int obtainNextPlayerIndex(String roomNo,String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        return playerIndex%DdzConstant.DDZ_PLAYER_NUMBER;
    }

    /**
     * 获取下个叫、抢地主玩家下标
     * @param roomNo
     * @param account
     * @return
     */
    private int obtainNextDoLandlordIndex(String roomNo,String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        int nextIndex = playerIndex%DdzConstant.DDZ_PLAYER_NUMBER;
        if (!isAllChoice(roomNo)) {
            // 多人抢地主才需要判断
            for (String player : obtainAllPlayerAccount(roomNo)) {
                // 获取下家账号
                if (room.getPlayerMap().get(player).getMyIndex()==nextIndex) {
                    // 最后一条操作记录
                    JSONObject lastRecord = obtainLastOperateRecordByAccount(roomNo,player);
                    // 下家不抢返回下下家
                    if (lastRecord.containsKey("account")&&player.equals(lastRecord.getString("account"))&&
                        lastRecord.containsKey("isChoice")&&lastRecord.getInt("isChoice")==CommonConstant.GLOBAL_NO) {
                        return obtainNextDoLandlordIndex(roomNo,player);
                    }
                }
            }
        }
        return nextIndex;
    }

}
