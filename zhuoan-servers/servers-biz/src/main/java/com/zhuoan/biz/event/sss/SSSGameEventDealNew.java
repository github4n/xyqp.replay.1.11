package com.zhuoan.biz.event.sss;

import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.sss.SSSGameRoomNew;
import net.sf.json.JSONObject;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 15:10 2018/4/21
 * @Modified By:
 **/
public class SSSGameEventDealNew {


    public JSONObject obtainRoomData(String roomNo, String account){
        SSSGameRoomNew room = (SSSGameRoomNew) RoomManage.gameRoomMap.get(roomNo);
        JSONObject roomData = new JSONObject();
        if (room!=null) {
//            roominfo:房间信息
//            roominfo2: 房间信息2
//            zhuang:庄家下标
//            game_index:当前游戏局数(房卡场)
//            showTimer:是否展示定时器(或者timer=0直接隐藏)
//            timer:倒计时
//            myIndex:玩家下标(配牌阶段)
//            users:
//            myPai:
//            myPaiType: 0 自己的牌型
//            gameData:
//            jiesan:// 0不是解散阶段 1解散阶段
//            jiesanData:
            roomData.put("gameStatus",room.getGameStatus());
            roomData.put("room_no",room.getRoomNo());
            roomData.put("roomType",room.getRoomType());
            roomData.put("game_count",room.getGameCount());
            roomData.put("di",room.getScore());
            roomData.put("isma",room.getMaPaiType());
            roomData.put("mapai",room.getMaPai());
        }
        return roomData;
    }
}
