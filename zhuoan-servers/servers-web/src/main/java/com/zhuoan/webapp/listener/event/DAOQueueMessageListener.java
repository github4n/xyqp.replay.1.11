package com.zhuoan.webapp.listener.event;

import com.zhuoan.biz.game.biz.GameLogBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.dao.PumpDao;
import com.zhuoan.constant.DaoTypeConstant;
import com.zhuoan.exception.EventException;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * DAOQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-23 20:05
 */
@Component("daoQueueMessageListener")
public class DAOQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(DAOQueueMessageListener.class);


    @Resource
    private RoomBiz roomBiz;
    @Resource
    private UserBiz userBiz;
    @Resource
    private GameLogBiz gameLogBiz;


    @Override
    public void onMessage(Message message) {

        PumpDao pumpDao = (PumpDao) obtainMessageStr(message);
        JSONObject o = pumpDao.getObjectDao();

        switch (pumpDao.getDaoType()) {
            case DaoTypeConstant.PUMP:
                logger.info("抽水ING");
                roomBiz.pump(o.getJSONArray("array"), o.getString("roomNo"), o.getInt("gId"), o.getDouble("fee"), o.getString("updateType"));
                break;
            case DaoTypeConstant.UPDATE_SCORE:
                logger.info("更新元宝ING");
                userBiz.updateUserBalance(o.getJSONArray("array"), o.getString("updateType"));
                break;
            case DaoTypeConstant.USER_DEDUCTION:
                logger.info("更新元宝记录ING");
                userBiz.insertUserdeduction(o);
                break;
            case DaoTypeConstant.UPDATE_ROOM_INFO:
                logger.info("更新房间信息ING");
                roomBiz.updateGameRoom(o);
                break;
            case DaoTypeConstant.INSERT_GAME_ROOM:
                logger.info("插入房间信息ING");
                roomBiz.insertGameRoom(o);
                break;
            case DaoTypeConstant.INSERT_GAME_LOG:
                logger.info("插入战绩信息ING");
                gameLogBiz.addOrUpdateGameLog(o);
                break;
            case DaoTypeConstant.INSERT_USER_GAME_LOG:
                logger.info("插入玩家战绩信息ING");
                gameLogBiz.addUserGameLog(o);
                break;
            default:
                break;
        }

    }


    private Object obtainMessageStr(Message message) {
        if (message != null) {
            try {
                return ((ActiveMQObjectMessage) message).getObject();
            } catch (JMSException e) {
                throw new EventException("[" + this.getClass().getName() + "] 信息接收出现异常");
            }
        }
        return null;
    }
}
