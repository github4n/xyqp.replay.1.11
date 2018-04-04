package com.zhuoan.webapp.listener.mq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.*;

/**
 * QueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:01
 */
public class QueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(QueueMessageListener.class);

    /**
     * 当收到消息时，自动调用该方法。
     *
     * @param message
     */
    @Override
    public void onMessage(Message message) {
        if (message instanceof TextMessage) {
            TextMessage tm = (TextMessage) message;
            try {
                logger.info("QueueMessageListener 监听到" +
                    "队列[" + String.valueOf(message.getJMSDestination()) + "] 消息：" + tm.getText());
            } catch (JMSException e) {
                logger.error("队列监听发生异常", e.getMessage());
            }
        }
        if (message instanceof ObjectMessage) {
            ObjectMessage objectMessage = (ObjectMessage) message;
            logger.info("当前队列监听到对象,待有相关处理...");
//            TODO something ...
        }


    }
}
