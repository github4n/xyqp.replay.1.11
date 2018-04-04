package com.zhuoan.service.jms.impl;

import com.zhuoan.service.jms.ProducerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import java.io.Serializable;

/**
 * ProducerServiceImpl
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:35
 */
@Service
public class ProducerServiceImpl implements ProducerService {

    private final static Logger logger = LoggerFactory.getLogger(ProducerServiceImpl.class);

    @Resource
    private JmsTemplate jmsTemplate;

    /**
     * 向指定队列发送消息
     */
    @Override
    public void sendMessage(Destination destination, final String msg) {
        logger.info("向队列" + String.valueOf(destination) + "发送了消息------------" + msg);
        jmsTemplate.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msg);
            }
        });
    }

    /**
     * 向默认队列发送消息
     */
    @Override
    public void sendMessage(final String msg) {
        Destination destination = jmsTemplate.getDefaultDestination();
        logger.info("向队列" + String.valueOf(destination) + "发送了消息------------" + msg);
        jmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(msg);
            }
        });

    }

    /**
     * 向指定队列发送自定义消息
     */
    @Override
    public void sendMessage(Destination destination, final Object msg) {
        logger.info("向队列" + String.valueOf(destination) + "发送了消息------------" + msg);
        jmsTemplate.send(destination, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createObjectMessage((Serializable) msg);
                //return session.createTextMessage(ObjectConverter.class.);
            }
        });
    }
}
