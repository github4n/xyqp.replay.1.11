package com.zhuoan.biz.core.nn;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.nn.NNGameRoomNew;
import com.zhuoan.constant.NNConstant;
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
            // 房主坐庄
            case NNConstant.NN_BANKER_TYPE_FZ:
                return fangzhuZhuang(roomNo);
            // 轮庄
            case NNConstant.NN_BANKER_TYPE_LZ:
                return lunZhuang(roomNo);
            // 牛牛坐庄
            case NNConstant.NN_BANKER_TYPE_NN:
                return niuniuZhuang(roomNo);
            default:
                return null;
        }
	}
	
	/**
	 * 牛牛坐庄
	 * @param roomNo
	 * @return
	 */
	private static String niuniuZhuang(String roomNo) {
		
		NNGameRoomNew room=(NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		List<String> uuids = new ArrayList<String>();
		for (String uuid : room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                if(room.getUserPacketMap().get(uuid).getType()>=10){
                    uuids.add(uuid);
                }
            }
		}
		int count = uuids.size();
		if(count>0){
			
			String zhuang = uuids.get(RandomUtils.nextInt(count));
			RoomManage.gameRoomMap.get(roomNo).setBanker(zhuang);
			return zhuang;
		}else{
			return room.getBanker();
		}
	}

	/**
	 * 房主做庄
	 * @param roomNo
	 * @return 
	 */
	public static String fangzhuZhuang(String roomNo){

        NNGameRoomNew room=(NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
		if(room.getPlayerMap().get(room.getOwner())!=null){
            RoomManage.gameRoomMap.get(roomNo).setBanker(room.getOwner());
			return room.getBanker();
        }else{
            String zhuang = room.getNextPlayer(room.getBanker());
            RoomManage.gameRoomMap.get(roomNo).setBanker(zhuang);
			return zhuang;
		}
	}
	
	/**
	 * 轮流做庄
	 * @param roomNo
	 * @return 
	 */
	public static String lunZhuang(String roomNo){
		
		NNGameRoomNew room=((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo));
		String zhuang = room.getNextPlayer(room.getBanker());
		RoomManage.gameRoomMap.get(roomNo).setBanker(zhuang);
		return zhuang;
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
        // 返回玩家手牌
        List<UserPacket> userPackets =NiuNiu.faPai(room.getPai(), room.getPlayerCount(), room.getSpecialType());
		List<String> uuidList = new ArrayList<String>();
		for (String uuid:room.getUserPacketMap().keySet()) {
            if (room.getUserPacketMap().containsKey(uuid)&&room.getUserPacketMap().get(uuid)!=null) {
                if(room.getUserPacketMap().get(uuid).getStatus()> NNConstant.NN_USER_STATUS_INIT){
                    uuidList.add(uuid);
                }
            }
		}
        // 遍历玩家列表
        for (int i = 0; i < uuidList.size(); i++) {
			((NNGameRoomNew) RoomManage.gameRoomMap.get(roomNo)).getUserPacketMap().get(uuidList.get(i)).setPs(userPackets.get(i).getPs());
		}
	}
	
}
