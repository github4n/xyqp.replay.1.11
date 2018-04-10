package com.zhuoan.webapp.listener.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * BaseQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018-04-09 20:14
 **/
@Component
public class BaseQueueMessageListener implements MessageListener {

    private final static Logger logger = LoggerFactory.getLogger(BaseQueueMessageListener.class);

    @Override
    public void onMessage(Message message) {
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
        logger.info("BaseQueueMessageListener监听开始"+message);
    }
    //guava


}
