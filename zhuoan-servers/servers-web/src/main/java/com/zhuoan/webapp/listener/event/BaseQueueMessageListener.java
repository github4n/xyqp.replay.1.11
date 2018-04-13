package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventDeal;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    @Resource
    private GameEventDeal gameEventDeal;

    @Override
    public void onMessage(Message message) {
        gameEventDeal.eventsMQ(message);
    }
}
