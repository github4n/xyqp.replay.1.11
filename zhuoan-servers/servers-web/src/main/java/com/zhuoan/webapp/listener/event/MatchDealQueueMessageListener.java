package com.zhuoan.webapp.listener.event;

import com.zhuoan.biz.event.ddz.GameTimerDdz;
import com.zhuoan.biz.event.match.MatchEventDeal;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.MatchDealConstant;
import com.zhuoan.exception.EventException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.UUID;

/**
 * MatchDealQueueMessageListener
 *
 * @author wqm
 * @date 2018-09-19 21:14
 **/
@Component("matchDealQueueMessageListener")
public class MatchDealQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(MatchDealQueueMessageListener.class);

    @Resource
    private MatchEventDeal matchEventDeal;

    @Resource
    private GameTimerDdz gameTimerDdz;

    @Override
    public void onMessage(Message message) {

        JSONObject object = JSONObject.fromObject(obtainMessageStr(message));
        switch (object.getInt("deal_type")) {
            case MatchDealConstant.MATCH_DEAL_TYPE_SEND:
                CommonConstant.sendMsgEventToSingle(UUID.fromString(object.getString("uuid")), String.valueOf(object.getString("result")), object.getString("eventName"));
                break;
            case MatchDealConstant.MATCH_DEAL_TYPE_JOIN:
                String matchNum = object.getString("matchNum");
                JSONObject matchInfo = object.getJSONObject("matchInfo");
                int perCount = object.getInt("perCount");
                JSONArray robotList = object.getJSONArray("robotList");
                JSONArray singleMate = object.getJSONArray("singleMate");
                JSONObject rankObj = object.getJSONObject("rankObj");
                matchEventDeal.singleJoin(matchNum, matchInfo, perCount, robotList, singleMate, rankObj);
                break;
            case MatchDealConstant.MATCH_DEAL_TYPE_TIME:
                gameTimerDdz.doOverTimeDeal(object.getString("roomNo"), object.getJSONObject("roomInfo"));
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
