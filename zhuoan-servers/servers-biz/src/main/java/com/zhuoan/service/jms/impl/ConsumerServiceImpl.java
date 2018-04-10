package com.zhuoan.service.jms.impl;

import com.zhuoan.service.jms.ConsumerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.TextMessage;

/**
 * ConsumerServiceImpl
 *
 * @author weixiang.wu
 * @date 2018-04-03 10:48
 **/
@Deprecated
@Service
public class ConsumerServiceImpl implements ConsumerService {

    private final static Logger logger = LoggerFactory.getLogger(ConsumerServiceImpl.class);

    @Resource
    private JmsTemplate jmsTemplate;

    @Override
    public TextMessage receive(Destination destination) {
        TextMessage textMessage = (TextMessage) jmsTemplate.receive(destination);
        try {

            if (StringUtils.isNotBlank((CharSequence) textMessage)){
                logger.info("从队列" + String.valueOf(destination) + "收到了消息：\t"
                    + textMessage.getText());
            }else {
                logger.info(((ActiveMQQueue) destination).getPhysicalName()+":收到的信息为空");
            }
        } catch (JMSException e) {
            e.printStackTrace();
        }
        return textMessage;
    }

}
