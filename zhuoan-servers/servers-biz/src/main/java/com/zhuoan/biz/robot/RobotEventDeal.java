package com.zhuoan.biz.robot;

import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.Dto;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:29 2018/5/29
 * @Modified By:
 **/
@Component
public class RobotEventDeal {

    ConcurrentHashMap<String,RobotInfo> robots = new ConcurrentHashMap<String, RobotInfo>();

    @Resource
    private Destination baseQueueDestination;

    @Resource
    private Destination sssQueueDestination;

    @Resource
    private Destination nnQueueDestination;

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
        int robotCount = totalCount - playerCount - RandomUtils.nextInt(3);
        double minScore = RoomManage.gameRoomMap.get(roomNo).getEnterScore();
        List<String> robotLists = roomBiz.getRobotList(robotCount,minScore);
        for (int i = 0; i < robotCount; i++) {
            String robotAccount = robotLists.get(i);
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.DATA_KEY_ACCOUNT,robotAccount);
            obj.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
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
                default:
                    break;
            }
            robotInfo.setDelayTime(RandomUtils.nextInt(3)+2);
            robotInfo.setOutTimes(RandomUtils.nextInt(3)+2);
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
}
