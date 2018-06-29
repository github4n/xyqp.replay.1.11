package com.zhuoan.webapp.listener.event;

import com.zhuoan.queue.GameEventDeal;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Message;
import javax.jms.MessageListener;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 17:03 2018/6/27
 * @Modified By:
 **/
@Component("ddzQueueMessageListener")
public class DDZQueueMessageListener implements MessageListener {

    @Resource
    private GameEventDeal gameEventDeal;

    @Override
    public void onMessage(Message message) {
        gameEventDeal.eventsMQ(message);
    }
}
