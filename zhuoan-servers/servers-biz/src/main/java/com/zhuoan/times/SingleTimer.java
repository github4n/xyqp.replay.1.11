package com.zhuoan.times;


import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.event.GameMain;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.util.LogUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/*
 * ȫ�ֶ�ʱ��
 * ��TimerMsgData��nLimit�趨�������, Ͷ��CGameMessageData����Ϸ��Ϣ����
 */
public class SingleTimer extends Thread {
	private ReentrantLock m_locker = new ReentrantLock();//������
	private Map<String, TimerMsgData> m_map = new HashMap<String, TimerMsgData>(); 

	public Map<String, TimerMsgData> getM_map() {
		return m_map;
	}
	public void setM_map(Map<String, TimerMsgData> m_map) {
		this.m_map = m_map;
	}
	
	public boolean hasKey(String roomNo){
		boolean flag = false;
		m_locker.lock();
		if (m_map.containsKey(roomNo)) {
			flag =  true;
		}
		m_locker.unlock();
		return flag;
	}
	
	public void run() {
		while(true)
		{
			try{
				Thread.sleep(1000);//ÿ��һ��
				
				m_locker.lock();//����,�������
				//���������map
				Iterator<Map.Entry<String, TimerMsgData>> it = m_map.entrySet().iterator(); 
				while (it.hasNext()) { 
					Map.Entry<String, TimerMsgData>  entry = it.next();
					//LogUtil.print("定时器："+entry.getValue().nTimeLimit+",游戏："+entry.getValue().gid+",方法："+entry.getValue().nType);
					if (RoomManage.gameRoomMap.get(entry.getValue().roomid)!=null) {
						if (entry.getValue().gid==4&&entry.getValue().nType==10) {
							((SSSGameRoom) RoomManage.gameRoomMap.get(entry.getValue().roomid)).setReadyTime(entry.getValue().nTimeLimit);
						}else if (entry.getValue().gid==4&&entry.getValue().nType==11){
							((SSSGameRoom) RoomManage.gameRoomMap.get(entry.getValue().roomid)).setPeipaiTime(entry.getValue().nTimeLimit);
						}
						
						RoomManage.gameRoomMap.get(entry.getValue().roomid).setTimeLeft(entry.getValue().nTimeLimit);
					}
					// 炸金花跟到底特殊处理
//					if (entry.getValue().gid==6&&entry.getValue().nType==10&&entry.getValue().nTimeLimit== MutliThreadZJH.xzTimer) {
//						GameMain.messageQueue.addQueue(new Messages(null, entry.getValue().gmd.getDataObject(), 6, 11));
//					}
					if(entry.getValue().nTimeLimit<=1)
					{
						LogUtil.print("-----------定时器投递事件-----------");
						GameMain.messageQueue.addQueue(entry.getValue().gmd);

						// GameMain.messageQueue.addQueue(new Messages(entry.getValue().client, entry.getValue().data, entry.getValue().gid, entry.getValue().nType));
						it.remove();
					}
					entry.getValue().nTimeLimit--;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally{
				m_locker.unlock();
			}
		}
	}
	public void createTimer(TimerMsgData tmd)
	{
		try
		{
			m_locker.lock();//����,�������
			//������TMD ����new �����ľ���Ҫ������new һ�����ƺ�ֵ;
			m_map.put(tmd.roomid, tmd);

		}finally
		{
			m_locker.unlock();
		}
	}
	public void deleteTimer(String roomid)
	{
		try
		{
			m_locker.lock();//����,�������
			m_map.remove(roomid);

		}finally
		{
			m_locker.unlock();
		}
	}

}
