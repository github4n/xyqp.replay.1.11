package com.zhuoan.service.jms;

import javax.jms.Destination;
import javax.jms.TextMessage;

/**
 * ConsumerService
 *
 * @author weixiang.wu
 * @date 2018 -04-03 10:47
 */
@Deprecated
public interface ConsumerService {

    /**
     * Receive text message.接受消息
     *
     * @param destination the destination
     * @return the text message
     */
    TextMessage receive(Destination destination);

}
