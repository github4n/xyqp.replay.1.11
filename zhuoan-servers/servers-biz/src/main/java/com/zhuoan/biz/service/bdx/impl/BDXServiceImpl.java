package com.zhuoan.biz.service.bdx.impl;

import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import com.zhuoan.biz.service.bdx.BDXService;
import net.sf.json.JSONObject;

import java.util.*;

public class BDXServiceImpl implements BDXService {

	@Override
	public BDXGameRoom createGameRoom(JSONObject roomObj, UUID uuid,
                                      JSONObject objInfo, Playerinfo player) {
		
		BDXGameRoom room= (BDXGameRoom) RoomManage.gameRoomMap.get(roomObj.getString("room_no"));
		room.setFirstTime(1);
		room.setRoomNo(roomObj.getString("room_no"));//房间号，唯一值
		room.setPlayerCount(objInfo.getInt("player"));//玩家人数
		
		//room.setScore(100); //底分
		if (roomObj.getInt("roomtype")==2) {
			
			room.setRoomType(0);
		}else{
			room.setRoomType(roomObj.getInt("roomtype"));
		}
		room.setFangzhu(player.getAccount());
		room.setZhuang(player.getAccount());
		List<UUID> uuidList=new ArrayList<UUID>();
		uuidList.add(uuid);//房主加入房间
		room.setUuidList(uuidList);//用户的socketId
		Map<String,Playerinfo> users=new HashMap<String, Playerinfo>();
		users.put(player.getAccount(), player);
		room.setPlayerMap(users);
		
		Set<Long> userSet = new HashSet<Long>();
		userSet.add(player.getId());
		room.setUserSet(userSet);
		Set<String> acc=new HashSet<String>();
		acc.add(player.getAccount());
		room.setUserAcc(acc);
		//room.setGameType(objInfo.getInt("type"));//游戏模式
		room.setGameStatus(0);
		
		//将房间存入缓存
		//Constant.bdxGameMap.put(roomObj.getString("room_no"), room);
		RoomManage.gameRoomMap.put(roomObj.getString("room_no"), room);
		return room;
	}

	@Override
	public boolean joinGameRoom(String roomNo, UUID uuid, Playerinfo player,
			int roomType) {
		
		if(RoomManage.gameRoomMap.containsKey(roomNo)){
			BDXGameRoom room=(BDXGameRoom) RoomManage.gameRoomMap.get(roomNo);
			if(room!=null){
				if(!room.getUserSet().contains(player.getId())){ 
						//新加进来的玩家
						
						room.getUuidList().add(uuid);
						room.getPlayerMap().put(player.getAccount(), player);//用户的个人信息	
						room.getUserSet().add(player.getId());
						room.getUserAcc().add(player.getAccount());
						RoomManage.gameRoomMap.put(roomNo, room);
						return true;
					}else if(room.getGameStatus()>0){ // TODO 断线后进来的玩家
						return false;
					}
				
				
			}
		}
		return false;
	}

}
