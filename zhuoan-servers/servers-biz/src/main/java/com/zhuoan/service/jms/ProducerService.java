package com.zhuoan.service.jms;

import com.zhuoan.queue.Messages;

import javax.jms.Destination;

/**
 * ProducerService
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:33
 */
public interface ProducerService {

    /**
     * Send message.向指定队列发送消息
     *
     * @param destination the destination
     * @param msg         the msg
     */
    void sendMessage(Destination destination, final String msg);

    /**
     * Send message.向默认队列发送消息
     *
     * @param msg the msg
     */
    void sendMessage(final String msg);

    /**
     * Send message.向指定队列发送 自定义消息
     *
     * @param destination the destination
     * @param msg         the msg
     */
    void sendMessage(Destination destination, final Object msg);
    void sendMessage(Destination destination, final Messages msg);

}
