package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventDeal;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 14:02 2018/5/23
 * @Modified By:
 **/
@Component("qzmjQueueMessageListener")
public class QZMJQueueMessageListener implements MessageListener {

    @Resource
    private GameEventDeal gameEventDeal;

    @Override
    public void onMessage(Message message) {
        gameEventDeal.eventsMQ(message);
    }
}
