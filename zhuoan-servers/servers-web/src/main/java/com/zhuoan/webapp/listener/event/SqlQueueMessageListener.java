package com.zhuoan.webapp.listener.event;

import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.exception.EventException;
import com.zhuoan.queue.SqlModel;
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
 * SqlQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-11 21:43
 */
@Component("sqlQueueMessageListener")
public class SqlQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(SqlQueueMessageListener.class);

    @Resource
    private MaJiangBiz mjBiz;


    @Override
    public void onMessage(Message message) {
        SqlModel sqlModel;
        try {
            sqlModel = (SqlModel) ((ActiveMQObjectMessage) message).getObject();
            logger.info("[" + this.getClass().getName() + "] 接收了消息 = [" + JSONObject.fromObject(sqlModel) + "]");
        } catch (JMSException e) {
            throw new EventException("sqlModel对象接收异常", e.getMessage());
        }
        switch (sqlModel.type) {
            case SqlModel.EXECUTEUPDATEBYSQL:// 更新
                DBUtil.executeUpdateBySQL(sqlModel.sql, sqlModel.params);
                break;
            case 4:// 抽水
                mjBiz.pump(sqlModel.getUserIds(), sqlModel.getRoomNo(), sqlModel.getGid(), sqlModel.getFee(), sqlModel.getType1());
                break;
            default:
                break;
        }
    }
}
