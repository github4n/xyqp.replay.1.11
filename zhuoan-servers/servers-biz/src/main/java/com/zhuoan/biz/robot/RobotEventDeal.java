package com.zhuoan.biz.robot;

import com.zhuoan.biz.core.ddz.DdzCore;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.constant.*;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:29 2018/5/29
 * @Modified By:
 **/
@Component
public class RobotEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(RobotEventDeal.class);

    public static ConcurrentHashMap<String,RobotInfo> robots = new ConcurrentHashMap<String, RobotInfo>();

    @Resource
    private Destination baseQueueDestination;

    @Resource
    private Destination sssQueueDestination;

    @Resource
    private Destination ddzQueueDestination;

    @Resource
    private Destination nnQueueDestination;

    @Resource
    private Destination qzmjQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private RoomBiz roomBiz;

    public void startRobot() {
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (robots.size()>0) {
                            for (String robotAccount : robots.keySet()) {
                                robots.get(robotAccount).subDelayTime();
                                if (robots.get(robotAccount).getDelayTime()==0) {
                                    switch (robots.get(robotAccount).getPlayGameId()) {
                                        case CommonConstant.GAME_ID_NN:
                                            playNN(robotAccount);
                                            break;
                                        case CommonConstant.GAME_ID_SSS:
                                            playSSS(robotAccount);
                                            break;
                                        case CommonConstant.GAME_ID_DDZ:
                                            playDdz(robotAccount);
                                            break;
                                        case CommonConstant.GAME_ID_QZMJ:
                                            playQzMj(robotAccount);
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                        }
                        Thread.sleep(1000);
                    } catch (Exception e) {

                    }
                }
            }
        });
    }

    /**
     * 机器人加入房间
     * @param roomNo
     */
    public void robotJoin(String roomNo) {
        // 房间不存在
        if (!RoomManage.gameRoomMap.containsKey(roomNo)||RoomManage.gameRoomMap.get(roomNo)==null) {
            return;
        }
        // 房间人总数
        int totalCount = RoomManage.gameRoomMap.get(roomNo).getPlayerCount();
        // 当前人数
        int playerCount = RoomManage.gameRoomMap.get(roomNo).getPlayerMap().size();
        // 机器人数(随机留出1-3个空座)
        int robotCount = totalCount - playerCount ;
        double minScore = RoomManage.gameRoomMap.get(roomNo).getEnterScore();
        JSONArray robotArray = roomBiz.getRobotArray(robotCount,minScore);
        for (int i = 0; i < robotArray.size(); i++) {
            String robotAccount = robotArray.getJSONObject(i).getString("account");
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
            obj.put("uuid",robotArray.getJSONObject(i).getString("uuid"));
            // 加入房间
            producerService.sendMessage(baseQueueDestination, new Messages(null, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
            // 添加机器人列表
            RoomManage.gameRoomMap.get(roomNo).getRobotList().add(robotAccount);
            // 设置机器人信息
            RobotInfo robotInfo = new RobotInfo();
            robotInfo.setRobotAccount(robotAccount);
            robotInfo.setPlayRoomNo(roomNo);
            robotInfo.setPlayGameId(RoomManage.gameRoomMap.get(roomNo).getGid());
            // 准备
            switch (RoomManage.gameRoomMap.get(roomNo).getGid()) {
                case CommonConstant.GAME_ID_NN:
                    robotInfo.setActionType(NNConstant.NN_GAME_EVENT_READY);
                    break;
                case CommonConstant.GAME_ID_SSS:
                    robotInfo.setActionType(SSSConstant.SSS_GAME_EVENT_READY);
                    break;
                case CommonConstant.GAME_ID_DDZ:
                    robotInfo.setActionType(DdzConstant.DDZ_GAME_EVENT_READY);
                    break;
                case CommonConstant.GAME_ID_QZMJ:
                    robotInfo.setActionType(QZMJConstant.QZMJ_GAME_EVENT_READY);
                    break;
                default:
                    break;
            }
            robotInfo.setDelayTime(RandomUtils.nextInt(3)+2);
            robotInfo.setOutTimes(RandomUtils.nextInt(6)+6);
            robots.put(robotAccount,robotInfo);
        }
    }

    public void robotExit(String robotAccount) {
        if (checkRobotAccount(robotAccount)) {
            roomBiz.updateRobotStatus(robotAccount,0);
            robots.remove(robotAccount);
        }
    }

    /**
     * 改变机器人状态
     * @param robotAccount
     * @param nextActionType
     * @param delayTime
     */
    public void changeRobotActionDetail(String robotAccount, int nextActionType, int delayTime) {
        if (checkRobotAccount(robotAccount)) {
            // 设置下一条消息类型
            robots.get(robotAccount).setActionType(nextActionType);
            // 设置延迟时间
            robots.get(robotAccount).setDelayTime(delayTime);
            // 增加对应的游戏次数
            if (robots.get(robotAccount).getPlayGameId()==CommonConstant.GAME_ID_NN&&nextActionType==NNConstant.NN_GAME_EVENT_READY) {
                robots.get(robotAccount).subOutTimes();
            }
            if (robots.get(robotAccount).getPlayGameId()==CommonConstant.GAME_ID_SSS&&nextActionType==SSSConstant.SSS_GAME_EVENT_READY) {
                robots.get(robotAccount).subOutTimes();
            }
            if (robots.get(robotAccount).getPlayGameId()==CommonConstant.GAME_ID_DDZ&&nextActionType==DdzConstant.DDZ_GAME_EVENT_READY) {
                robots.get(robotAccount).subOutTimes();
            }
        }
    }

    /**
     * 牛牛
     * @param robotAccount
     */
    public void playNN(String robotAccount) {
        if (checkRobotAccount(robotAccount)) {
            RobotInfo robotInfo = robots.get(robotAccount);
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,robotInfo.getPlayRoomNo());
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            if (robotInfo.getActionType() == NNConstant.NN_GAME_EVENT_QZ) {
                obj.put(NNConstant.DATA_KEY_VALUE,getRobotQZTimes(robotInfo.getPlayRoomNo(),robotAccount));
            }
            if (robotInfo.getActionType() == NNConstant.NN_GAME_EVENT_XZ) {
                obj.put(NNConstant.DATA_KEY_MONEY,getRobotXZTimes(robotInfo.getPlayRoomNo(),robotAccount));
            }
            if (robotInfo.getOutTimes()<0&&robotInfo.getActionType()==NNConstant.NN_GAME_EVENT_READY) {
                robotInfo.setActionType(NNConstant.NN_GAME_EVENT_EXIT);
            }
            producerService.sendMessage(nnQueueDestination, new Messages(null, obj, CommonConstant.GAME_ID_NN, robotInfo.getActionType()));
        }
    }

    /**
     * 获取机器人下注倍数
     * @param roomNo
     * @param robotAccount
     * @return
     */
    public int getRobotXZTimes(String roomNo,String robotAccount) {
        int xzTimes = 1;
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            int maxTimes = getMaxTimes(roomNo,robotAccount,2);
            NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
            UserPacket robot = new UserPacket(room.getUserPacketMap().get(robotAccount).getPs(), room.getSpecialType());
            if (robot.getType()>6) {
                xzTimes = maxTimes;
            }else if (robot.getType()>0) {
                xzTimes += RandomUtils.nextInt(maxTimes);
            }
        }
        return xzTimes;
    }

    /**
     * 获取机器人抢庄倍数
     * @param roomNo
     * @param robotAccount
     * @return
     */
    public int getRobotQZTimes(String roomNo,String robotAccount) {
        int qzTimes = 0;
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            int maxTimes = getMaxTimes(roomNo,robotAccount,1);
            NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
            UserPacket robot = new UserPacket(room.getUserPacketMap().get(robotAccount).getPs(), room.getSpecialType());
            if (robot.getType()>6) {
                qzTimes = maxTimes;
            }else if (robot.getType()>0) {
                qzTimes += RandomUtils.nextInt(maxTimes);
            }
        }
        return qzTimes;
    }

    /**
     * 获取可选最大倍数
     * @param roomNo
     * @param robotAccount
     * @param type
     * @return
     */
    public int getMaxTimes(String roomNo,String robotAccount,int type) {
        int maxTimes = 0;
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            NNGameRoomNew room = (NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
            JSONArray array = room.getQzTimes(room.getPlayerMap().get(robotAccount).getScore());
            if (type==2) {
                array = room.getBaseNumTimes(room.getPlayerMap().get(robotAccount).getScore());
            }
            if (array.size()>0) {
                for (int i = 0; i < array.size(); i++) {
                    JSONObject baseNum = array.getJSONObject(i);
                    if (baseNum.getInt("isuse")==CommonConstant.GLOBAL_YES&&baseNum.getInt("val")>maxTimes) {
                        maxTimes = baseNum.getInt("val");
                    }
                }
            }
        }
        return maxTimes;
    }

    /**
     * 十三水
     * @param robotAccount
     */
    public void playSSS(String robotAccount) {
        if (checkRobotAccount(robotAccount)) {
            RobotInfo robotInfo = robots.get(robotAccount);
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,robotInfo.getPlayRoomNo());
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            if (robotInfo.getActionType() == SSSConstant.SSS_GAME_EVENT_EVENT) {
                obj.put("type",1);
            }
            if (robotInfo.getOutTimes()<0&&robotInfo.getActionType()==SSSConstant.SSS_GAME_EVENT_READY) {
                robotInfo.setActionType(SSSConstant.SSS_GAME_EVENT_EXIT);
            }
            producerService.sendMessage(sssQueueDestination, new Messages(null, obj, CommonConstant.GAME_ID_SSS, robotInfo.getActionType()));
        }
    }

    /**
     * 斗地主
     * @param robotAccount
     */
    public void playDdz(String robotAccount) {
        if (checkRobotAccount(robotAccount)) {
            RobotInfo robotInfo = robots.get(robotAccount);
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,robotInfo.getPlayRoomNo());
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            // 叫、抢地主
            if (robotInfo.getActionType() == DdzConstant.DDZ_GAME_EVENT_ROBOT_CALL || robotInfo.getActionType() == DdzConstant.DDZ_GAME_EVENT_ROBOT_ROB) {
                if (robotInfo.getActionType() == DdzConstant.DDZ_GAME_EVENT_ROBOT_CALL) {
                    obj.put(DdzConstant.DDZ_DATA_KEY_TYPE,DdzConstant.DDZ_BE_LANDLORD_TYPE_CALL);
                }else {
                    obj.put(DdzConstant.DDZ_DATA_KEY_TYPE,DdzConstant.DDZ_BE_LANDLORD_TYPE_ROB);
                }
                obj.put(DdzConstant.DDZ_DATA_KEY_IS_CHOICE,obtainRobOrNot(robotInfo.getPlayRoomNo(),robotAccount));
                robotInfo.setActionType(DdzConstant.DDZ_GAME_EVENT_CALL_AND_ROB);
            }
            // 出牌
            if (robotInfo.getActionType() == DdzConstant.DDZ_GAME_EVENT_GAME_IN) {
                List<String> allCard = obtainRobotCardDdz(robotInfo.getPlayRoomNo(),robotAccount);
                obj.put(DdzConstant.DDZ_DATA_KEY_PAI_LIST,allCard);
                if (allCard.size()>0) {
                    obj.put(DdzConstant.DDZ_DATA_KEY_TYPE,DdzConstant.DDZ_GAME_EVENT_TYPE_YES);
                }else {
                    obj.put(DdzConstant.DDZ_DATA_KEY_TYPE,DdzConstant.DDZ_GAME_EVENT_TYPE_NO);
                }
            }
            if (robotInfo.getOutTimes()<0&&robotInfo.getActionType()== DdzConstant.DDZ_GAME_EVENT_READY) {
                robotInfo.setActionType(DdzConstant.DDZ_GAME_EVENT_EXIT_ROOM);
            }
            producerService.sendMessage(ddzQueueDestination, new Messages(null, obj, CommonConstant.GAME_ID_DDZ, robotInfo.getActionType()));
        }
    }

    /**
     * 是否抢地主
     * @param roomNo
     * @param robotAccount
     * @return
     */
    private int obtainRobOrNot(String roomNo, String robotAccount) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<String> robotPai = room.getUserPacketMap().get(robotAccount).getMyPai();
        DdzCore.sortCard(robotPai);
        if (DdzCore.obtainCardValue(robotPai.get(robotPai.size()-4))>DdzConstant.DDZ_CARD_NUM_THIRTEEN) {
            return CommonConstant.GLOBAL_YES;
        }
        List<List<String>> bombList = DdzCore.obtainRepeatList(robotPai, 4, false);
        if (bombList.size()>0) {
            return CommonConstant.GLOBAL_YES;
        }
        return CommonConstant.GLOBAL_NO;
    }

    /**
     * 获取机器人的牌
     * @param roomNo
     * @param robotAccount
     * @return
     */
    private List<String> obtainRobotCardDdz(String roomNo, String robotAccount) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        List<String> lastCard = room.getLastCard();
        if (room.getLastCard().size()==0||robotAccount.equals(room.getLastOperateAccount())) {
            lastCard.clear();
        }
        List<String> robotPai = room.getUserPacketMap().get(robotAccount).getMyPai();
        // 手牌能一次出完
        if (DdzCore.checkCard(lastCard,robotPai)) {
            // 不是4带2 或 4带2两对
            if (DdzCore.obtainCardType(robotPai)!=DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE&&DdzCore.obtainCardType(robotPai)!=DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS) {
                return robotPai;
            }
        }
        List<List<String>> allCard = DdzCore.obtainRobotCard(lastCard, robotPai);
        // 下家不是队友且只有一张牌
        if (!isTeammateWithNext(roomNo,robotAccount)) {
            String nextAccount = getNextAccount(roomNo,robotAccount);
            if (allCard.size() > 0 && room.getUserPacketMap().get(nextAccount).getMyPai().size() == 1) {
                List<List<String>> notSingleList = getNotSingleList(allCard);
                return notSingleList.size() > 0 ? notSingleList.get(0) : new ArrayList<String>();
            }
        }
        if (isTeammateWithLast(roomNo,room.getLastOperateAccount(),robotAccount)) {
            // 单牌和对子 垫牌  队友能直接走完不垫牌
            if (DdzCore.obtainCardType(lastCard) != DdzConstant.DDZ_CARD_TYPE_SINGLE && DdzCore.obtainCardType(lastCard) != DdzConstant.DDZ_CARD_TYPE_PAIRS) {
                return new ArrayList<String>();
            }else if (lastCard.size()>0 && DdzCore.obtainCardValue(lastCard.get(0)) > DdzConstant.DDZ_CARD_NUM_TWELVE) {
                return new ArrayList<String>();
            }else if (DdzCore.checkCard(lastCard,room.getUserPacketMap().get(room.getLastOperateAccount()).getMyPai())) {
                return new ArrayList<String>();
            }
        }

        // 手牌多的时候不出炸
        if (lastCard.size()>0&&allCard.size()>0&&DdzCore.obtainCardType(allCard.get(0))==DdzConstant.DDZ_CARD_TYPE_BOMB) {
            if (room.getUserPacketMap().get(room.getLastOperateAccount()).getMyPai().size()>10) {
                return new ArrayList<>();
            }
        }
        if (allCard.size() > 0) {
            if (DdzCore.obtainCardType(allCard.get(0)) == DdzConstant.DDZ_CARD_TYPE_STRAIGHT) {
                return allCard.get(0);
            }else {
                return obtainMinRobotCardDdz(allCard, lastCard);
            }
        }
        return new ArrayList<String>();
    }

    /**
     * 下家单牌处理
     * @param allCard
     * @return
     */
    private List<List<String>> getNotSingleList(List<List<String>> allCard) {
        List<List<String>> notSingleList = new ArrayList<>();
        List<List<String>> singleList = new ArrayList<>();
        // 优先取出所有非单牌
        for (List<String> cardList : allCard) {
            if (DdzCore.obtainCardType(cardList) != DdzConstant.DDZ_CARD_TYPE_SINGLE) {
                notSingleList.add(cardList);
            }else {
                singleList.add(cardList);
            }
        }
        // 单牌从大小出
        Collections.sort(singleList, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> o1, List<String> o2) {
                return getMaxValue(o2) - getMaxValue(o1);
            }
        });
        notSingleList.addAll(singleList);
        return notSingleList;
    }

    private List<String> obtainMinRobotCardDdz(List<List<String>> allCard,List<String> lastCard) {
        List<String> bestList = new ArrayList<>();
        for (int i = 0; i < allCard.size(); i++) {
            if (getMinValue(allCard.get(i)) < getMinValue(bestList)) {
                if (lastCard.size() == 0 || DdzCore.obtainCardType(lastCard) == DdzCore.obtainCardType(allCard.get(i))) {
                    bestList = allCard.get(i);
                }
            }
        }
        return bestList;
    }

    private int getMinValue(List<String> cardList) {
        int minValue = 18;
        int cardType = DdzCore.obtainCardType(cardList);
        if (cardType == DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE || cardType == DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS) {
            cardList = DdzCore.obtainRepeatList(cardList,3,false).get(0);
        }
        for (String card : cardList) {
            int cardValue = DdzCore.obtainCardValue(card);
            if (cardValue < minValue) {
                minValue = cardValue;
            }
        }
        return minValue;
    }

    private int getMaxValue(List<String> cardList) {
        int maxValue = 0;
        int cardType = DdzCore.obtainCardType(cardList);
        if (cardType == DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE || cardType == DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS) {
            cardList = DdzCore.obtainRepeatList(cardList,3,false).get(0);
        }
        for (String card : cardList) {
            int cardValue = DdzCore.obtainCardValue(card);
            if (cardValue > maxValue) {
                maxValue = cardValue;
            }
        }
        return maxValue;
    }

    /**
     * 上一个出牌玩家和自己是不是队友
     * @param roomNo
     * @param lastAccount
     * @param account
     * @return
     */
    private boolean isTeammateWithLast(String roomNo, String lastAccount, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不是自己
        if (!Dto.stringIsNULL(lastAccount)&&!Dto.stringIsNULL(account)&&!lastAccount.equals(account)) {
            // 都不是地主
            if (!lastAccount.equals(room.getLandlordAccount())&&!account.equals(room.getLandlordAccount())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 下家和自己是不是队友
     * @param roomNo
     * @param account
     * @return
     */
    private boolean isTeammateWithNext(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 自己不是地主
        if (account.equals(room.getLandlordAccount())) {
            return false;
        }
        // 下家不是地主
        if (room.getLandlordAccount().equals(getNextAccount(roomNo,account))) {
            return false;
        }
        return true;
    }

    /**
     * 下家和自己是不是队友
     * @param roomNo
     * @param account
     * @return
     */
    private String getNextAccount(String roomNo, String account) {
        DdzGameRoom room = (DdzGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 不是地主判断下家是不是地主
        int playerIndex = room.getPlayerMap().get(account).getMyIndex();
        playerIndex++;
        for (String next : room.getPlayerMap().keySet()) {
            if (room.getPlayerMap().get(next).getMyIndex() == playerIndex%DdzConstant.DDZ_PLAYER_NUMBER) {
                return next;
            }
        }
        return null;
    }

    /**
     * 泉州麻将
     * @param robotAccount
     */
    public void playQzMj(String robotAccount) {
        if (checkRobotAccount(robotAccount)) {
            RobotInfo robotInfo = robots.get(robotAccount);
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,robotInfo.getPlayRoomNo());
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            // 准备
            if (robotInfo.getActionType() == QZMJConstant.QZMJ_GAME_EVENT_READY) {
                if (robotInfo.getOutTimes() < 0) {
                    robotInfo.setActionType(QZMJConstant.QZMJ_GAME_EVENT_EXIT_ROOM);
                }else {
                    obj.put("type",QZMJConstant.GAME_READY_TYPE_READY);
                }
            }
            // 过
            if (robotInfo.getActionType() == QZMJConstant.QZMJ_GAME_EVENT_ROBOT_GUO) {
                obj.put("type",1);
                robotInfo.setActionType(QZMJConstant.QZMJ_GAME_EVENT_IN);
            }else if (robotInfo.getActionType() == QZMJConstant.QZMJ_GAME_EVENT_ROBOT_ZM) {
                obj.put("type",11);
                robotInfo.setActionType(QZMJConstant.QZMJ_GAME_EVENT_IN);
            }else if (robotInfo.getActionType() == QZMJConstant.QZMJ_GAME_EVENT_ROBOT_HU) {
                obj.put("type",7);
                robotInfo.setActionType(QZMJConstant.QZMJ_GAME_EVENT_IN);
            }
            // 出牌
            if (robotInfo.getActionType() == QZMJConstant.QZMJ_GAME_EVENT_CP) {
                obj.put("pai",obtainRobotChuQzMj(robotInfo.getPlayRoomNo(),robotAccount));
            }
            producerService.sendMessage(qzmjQueueDestination, new Messages(null, obj, CommonConstant.GAME_ID_QZMJ, robotInfo.getActionType()));
        }
    }

    private int obtainRobotChuQzMj(String roomNo, String robotAccount) {
        QZMJGameRoom room = (QZMJGameRoom) RoomManage.gameRoomMap.get(roomNo);
        try {
            JSONObject special = new JSONObject();
            special.put("mj_count", 34);
            List<Integer> indexList = MaJiangAI.getMaJiangIndex(room.getUserPacketMap().get(robotAccount).getMyPai(), special);
            int[] paiIndex = new int[indexList.size()];
            for (int i = 0; i < indexList.size(); i++) {
                paiIndex[i] = indexList.get(i);
            }
            int jin = -1;
            for (int i = 0; i < QZMJConstant.ALL_CAN_HU_PAI.length; i++) {
                if (room.getJin() == QZMJConstant.ALL_CAN_HU_PAI[i]) {
                    jin = i;
                    break;
                }
            }
            int index = MaJiangAI.getRobotChupai(paiIndex, special, jin);
            return QZMJConstant.ALL_CAN_HU_PAI[index];
        }catch (Exception e) {
            logger.error("",e);
            return room.getUserPacketMap().get(robotAccount).getMyPai().get(0);
        }
    }

    /**
     * 判断机器人账号是否正确
     * @param robotAccount
     * @return
     */
    public boolean checkRobotAccount(String robotAccount) {
        if (!Dto.stringIsNULL(robotAccount)&&robots.containsKey(robotAccount)&&robots.get(robotAccount)!=null) {
            return true;
        }
        return false;
    }

    /**
     * 添加机器人
     * @param account
     * @param roomNo
     * @param gameId
     */
    public void addRobotInfo(String account, String roomNo, int gameId) {
        // 设置机器人信息
        RobotInfo robotInfo = new RobotInfo();
        robotInfo.setRobotAccount(account);
        robotInfo.setPlayRoomNo(roomNo);
        robotInfo.setPlayGameId(gameId);
        robotInfo.setOutTimes(10);
        robots.put(account,robotInfo);
    }
}
