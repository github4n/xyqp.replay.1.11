package com.zhuoan.biz.event.sw;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sw.SwGameRoom;
import com.zhuoan.constant.SwConstant;
import com.zhuoan.util.Dto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 10:22 2018/6/19
 * @Modified By:
 **/
@Component
public class GameTimerSw {
    private final static Logger logger = LoggerFactory.getLogger(GameTimerSw.class);

    @Resource
    private SwGameEventDeal swGameEventDeal;

    /**
     * 游戏超时事件
     * @param roomNo
     * @param timeLeft
     */
    public void gameOverTime(String roomNo,int timeLeft,int gameStatus){
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                SwGameRoom room = (SwGameRoom)RoomManage.gameRoomMap.get(roomNo);
                // 设置倒计时
                room.setTimeLeft(i);
                if (room.getGameStatus()!=gameStatus) {
                    break;
                }
                // 倒计时到了之后执行事件
                if (i==0) {
                    if (gameStatus == SwConstant.SW_GAME_STATUS_BET) {
                        swGameEventDeal.betFinish(roomNo);
                    }else if (gameStatus == SwConstant.SW_GAME_STATUS_SHOW) {
                        swGameEventDeal.summary(roomNo);
                    }else if (gameStatus == SwConstant.SW_GAME_STATUS_SUMMARY) {
                        swGameEventDeal.choiceBanker(roomNo);
                    }else if (gameStatus == SwConstant.SW_GAME_STATUS_HIDE_TREASURE) {
                        swGameEventDeal.hideOverTime(roomNo);
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
     * 开始游戏超时事件
     * @param roomNo
     * @param account
     * @param timeLeft
     */
    public void startOverTime(String roomNo, String account, int timeLeft) {
        for (int i = timeLeft; i >= 0; i--) {
            // 房间存在
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                SwGameRoom room = (SwGameRoom) RoomManage.gameRoomMap.get(roomNo);
                // 设置倒计时
                room.setTimeLeft(i);
                // 游戏已经开始
                if (room.getGameStatus() == SwConstant.SW_GAME_STATUS_BET) {
                    room.setTimeLeft(0);
                    break;
                }
                // 庄家不在或者换人
                if (Dto.stringIsNULL(room.getBanker()) || !account.equals(room.getBanker())) {
                    room.setTimeLeft(0);
                    break;
                }
                // 倒计时到了之后执行事件
                if (i == 0) {
                    swGameEventDeal.choiceBanker(roomNo);
                }
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    logger.error("", e);
                }
            } else {
                break;
            }
        }
    }
}
