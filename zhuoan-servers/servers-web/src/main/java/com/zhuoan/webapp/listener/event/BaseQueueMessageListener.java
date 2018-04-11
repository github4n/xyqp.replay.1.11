package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventsImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * BaseQueueMessageListener
 *
 * @author weixiang.wu
 * @date 2018-04-09 20:14
 **/
@Service("baseQueueMessageListener")
public class BaseQueueMessageListener implements MessageListener {

    @Resource
    private GameEventsImpl gameEventsMQ;

    @Override
    public void onMessage(Message message) {
        gameEventsMQ.EventsMQDeal(message);
    }
}
