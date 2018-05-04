package com.zhuoan.webapp.listener.event;

import com.zhuoan.dao.DBUtil;
import com.zhuoan.exception.EventException;
import com.zhuoan.queue.SqlModel;
import net.sf.json.JSONObject;
import org.apache.activemq.command.ActiveMQObjectMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * SqlQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-11 21:43
 */
@Component
public class SqlQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(SqlQueueMessageListener.class);

    @Override
    public void onMessage(Message message) {
        SqlModel sqlModel;
        try {
            sqlModel = (SqlModel) ((ActiveMQObjectMessage) message).getObject();
            logger.info("[" + this.getClass().getName() + "] 接收 = [" + JSONObject.fromObject(sqlModel) + "]");
        } catch (JMSException e) {
            throw new EventException("sqlModel对象接收异常", e.getMessage());
        }
        try {
            switch (sqlModel.type) {
                case SqlModel.EXECUTEUPDATEBYSQL:
                    // 更新
                    DBUtil.executeUpdateBySQL(sqlModel.sql, sqlModel.params);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            throw new EventException("*********************异常sql****************"+sqlModel.type, e.getMessage());
        }
    }
}
