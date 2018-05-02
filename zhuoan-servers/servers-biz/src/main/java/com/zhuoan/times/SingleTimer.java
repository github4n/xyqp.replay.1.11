package com.zhuoan.times;


import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * The type Single timer.
 */
@Component
public class SingleTimer extends Thread {

    @Resource
    private Destination nnQueueDestination;

    @Resource
    private ProducerService producerService;

    private final static Logger logger = LoggerFactory.getLogger(SingleTimer.class);

    /**
     * 可重入锁(默认：非公平锁):线程A把此锁全部释放了，状态值减到0了，其他线程才有机会获取锁
     */
    private Lock m_locker = new ReentrantLock();


    private Map<String, Messages> m_map = new HashMap<String, Messages>();

    /**
     * Gets m map.
     *
     * @return the m map
     */
    public Map<String, Messages> getM_map() {
        return m_map;
    }

    /**
     * Sets m map.
     *
     * @param m_map the m map
     */
    public void setM_map(Map<String, Messages> m_map) {
        this.m_map = m_map;
    }


    /**
     * Has key boolean.
     *
     * @param roomNo the room no
     * @return the boolean
     */
    public boolean hasKey(String roomNo) {
        boolean flag = false;
        m_locker.lock();
        if (m_map.containsKey(roomNo)) {
            flag = true;
        }
        m_locker.unlock();
        return flag;
    }


    /**
     *
     */
    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);

                m_locker.lock();
                Iterator<Map.Entry<String, Messages>> it = m_map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, Messages> entry = it.next();
                    Object data = entry.getValue().getDataObject();
                    JSONObject obj = JSONObject.fromObject(data);
                    String roomNo = obj.getString(CommonConstant.DATA_KEY_ROOM_NO);
                    if (RoomManage.gameRoomMap.get(roomNo) != null) {
                        GameRoom gameRoom = RoomManage.gameRoomMap.get(roomNo);
                        gameRoom.setTimeLeft(gameRoom.getTimeLeft()-1);
                        if (gameRoom.getTimeLeft()==0) {
                            producerService.sendMessage(nnQueueDestination, entry.getValue());
                            it.remove();
                        }
                    }else {
                        it.remove();
                    }
                }
            } catch (Exception e) {
                // TODO Auto-generated catch block
                logger.error("",e);
            } finally {
                m_locker.unlock();
            }
        }
    }

    /**
     * 创建定时器
     * @param roomNo
     * @param messages
     */
    public void createTimer(String roomNo,Messages messages) {
        try {
            m_locker.lock();
            m_map.put(roomNo, messages);

        } finally {
            m_locker.unlock();
        }
    }

    /**
     * Delete timer.
     *
     * @param roomid the roomid
     */
    public void deleteTimer(String roomid) {
        try {
            m_locker.lock();
            m_map.remove(roomid);
        } finally {
            m_locker.unlock();
        }
    }

}
