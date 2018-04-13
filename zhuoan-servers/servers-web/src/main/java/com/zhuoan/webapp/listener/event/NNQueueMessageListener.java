package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventDeal;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * NNQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018-04-13 09:08
 **/
@Component("nnQueueMessageListener")
public class NNQueueMessageListener implements MessageListener {

    @Resource
    private GameEventDeal gameEventDeal;

    @Override
    public void onMessage(Message message) {
        gameEventDeal.eventsMQ(message);
    }
}

