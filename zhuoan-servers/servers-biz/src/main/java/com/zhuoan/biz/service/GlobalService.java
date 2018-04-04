package com.zhuoan.biz.service;

import com.zhuoan.biz.dao.DBUtil;
import com.zhuoan.biz.util.DateUtils;
import com.zhuoan.biz.util.Dto;
import com.zhuoan.biz.util.TimeUtil;
import net.sf.json.JSONObject;

import java.util.Date;

public class GlobalService {

    /**
     * 判断该房间是否要重新创建好房间，游戏代开房间重新插入新数据
     * 进入代开房间重开方法
     *
     * @param roomNo the room no
     */
    public static void insertGameRoom(String roomNo) {
		
		try {
			System.out.println("进入代开房间重开方法");
			/**
			 * 去掉select *   wqm 2018/02/26
			 */
			String sql="select server_id,game_id,base_info,ip,port,game_count,paytype,stoptime,fangzhu from za_gamerooms where room_no=? order by id desc";
			JSONObject roomInfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
			System.out.println("进入代开房间重开方法roomInfo："+roomInfo);
			if(roomInfo.containsKey("stoptime") && !Dto.stringIsNULL(roomInfo.getString("stoptime"))
					&& roomInfo.getInt("roomtype")==2 && roomInfo.getInt("status")<0 ){
				
				TimeUtil.transTimeStamp(roomInfo, "yyyy-MM-dd HH:mm:ss", "stoptime");
				System.out.println("进入代开房间重开方法roomInfo2："+roomInfo);
				
				String stoptime = roomInfo.getString("stoptime");
				String newtime = DateUtils.getTimestamp().toString();
				System.out.println("进入代开房间重开方法stoptime："+stoptime+":newtime"+newtime);
				boolean result = TimeUtil.isLatter(newtime, stoptime);
				if(!result){
					sql = "insert into za_gamerooms(roomtype,server_id,game_id,room_no,base_info,createtime,ip,port,status,game_count,paytype,stoptime,fangzhu) "
							+ "values(?,?,?,?,?,?,?,?,?,?,?,?,?)";
					Object[] params = new Object[] { 2,roomInfo.getInt("server_id"), roomInfo.getInt("game_id"), roomNo,roomInfo.getString("base_info")
							, new Date(), roomInfo.getString("ip"), roomInfo.getInt("port"), 0, roomInfo.getInt("game_count")
							, roomInfo.getInt("paytype"),stoptime, roomInfo.getInt("fangzhu")};
					int n = DBUtil.executeUpdateBySQL(sql, params);
					System.out.println("是否插入成功n："+n);
				}
				
			}
		} catch (Exception e) {
		}
		
		
	}
	
}
