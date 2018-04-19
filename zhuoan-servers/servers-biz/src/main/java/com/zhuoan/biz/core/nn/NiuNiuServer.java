package com.zhuoan.biz.core.nn;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.List;

public class NiuNiuServer {


	/**
	 * 定庄
	 * @param roomNo 房间号
	 * @param type 定庄类型（0：房主坐庄、1：轮庄、2：抢庄、3：明牌抢庄、4：牛牛坐庄）
	 * @return 
	 */
	public static String dingZhuang(String roomNo, int type) {
		
		switch (type) {
		case 0:
			// 房主坐庄
			return fangzhuZhuang(roomNo);
		case 1:
			// 轮庄
			return lunZhuang(roomNo);
		case 4:
			// 牛牛坐庄
			return niuniuZhuang(roomNo);
		}
		return null;
	}
	
	/**
	 * 牛牛坐庄
	 * @param roomNo
	 * @return
	 */
	private static String niuniuZhuang(String roomNo) {
		
		NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
		List<String> uuids = new ArrayList<String>();
		for (String uuid : room.getUserPacketMap().keySet()) {
			if(room.getUserPacketMap().get(uuid).type>=10){
				uuids.add(uuid);
			}
		}
		int count = uuids.size();
		if(count>0){
			
			String zhuang = uuids.get(RandomUtils.nextInt(count));
			((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
			return zhuang;
		}else{
			return room.getZhuang();
		}
	}

	/**
	 * 房主做庄
	 * @param roomNo
	 * @return 
	 */
	public static String fangzhuZhuang(String roomNo){
		
		NNGameRoom room=(NNGameRoom) RoomManage.gameRoomMap.get(roomNo);
		if(room.getPlayerMap().get(room.getFangzhu())!=null){
			
			((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(room.getFangzhu());
			return room.getFangzhu();
		}else{
			
			String zhuang = room.getNextPlayer(room.getZhuang());
			((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
			return zhuang;
		}
	}
	
	/**
	 * 轮流做庄
	 * @param roomNo
	 * @return 
	 */
	public static String lunZhuang(String roomNo){
		
		NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
		String zhuang = room.getNextPlayer(room.getZhuang());
		((NNGameRoom) RoomManage.gameRoomMap.get(roomNo)).setZhuang(zhuang);
		return zhuang;
	}
	
	/**
	 * 准备就绪
	 * @param roomNo
	 * @param uuid
	 */
	public static void isReady(String roomNo, String uuid) {
		
		NNGameRoom room=((NNGameRoom) RoomManage.gameRoomMap.get(roomNo));
		if(room.getPlayerMap().containsKey(uuid)){
			
			room.getUserPacketMap().get(uuid).setIsReady(1);
			room.getUserPacketMap().get(uuid).setStatus(NiuNiu.USERPACKER_STATUS_CHUSHI);
			int count = 0;
			for (String uid:room.getUserPacketMap().keySet()) {
				int ready = room.getUserPacketMap().get(uid).getIsReady();
				if(ready!=0&&ready!=10){
					count++;
				}
			}
			room.setReadyCount(count);
		}
	}
	

	/**
	 * 洗牌
	 * @param roomNo
	 */
	public static void xiPai(String roomNo) {

        NNGameRoomNew room=((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
		Packer[] pai = NiuNiu.xiPai();
		room.setPai(pai);
	}

	/**
	 * 发牌
	 * @param roomNo
	 */
	public static void faPai(String roomNo) {

        NNGameRoomNew room=((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
		List<UserPacket> userPackets =NiuNiu.faPai(room.getPai(), room.getPlayerCount(), room.getSpecialType()); // 返回玩家手牌
		List<String> uuidList = new ArrayList<String>(); 
		for (String uuid:room.getUserPacketMap().keySet()) {
			if(room.getUserPacketMap().get(uuid).getStatus()>=NiuNiu.USERPACKER_STATUS_CHUSHI){
				uuidList.add(uuid);
			}
		}
		for (int i = 0; i < uuidList.size(); i++) { // 遍历玩家列表
			((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuidList.get(i)).setPs(userPackets.get(i).getPs());
			// 设置玩家已发牌状态
			//((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuidList.get(i)).setStatus(NiuNiu.USERPACKER_STATUS_FAPAI);
		}
	}
	
}
