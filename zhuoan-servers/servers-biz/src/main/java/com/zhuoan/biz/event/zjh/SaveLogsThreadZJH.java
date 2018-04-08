package com.zhuoan.biz.event.zjh;

import com.zhuoan.dao.DBUtil;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.zjh.ZJHGame;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class SaveLogsThreadZJH extends Thread{
	
	MaJiangBiz mjBiz=new MajiangBizImpl();
	private String roomNo;
	private JSONArray jiesuanData;
	private JSONArray jiesuanArray;

	public SaveLogsThreadZJH(String roomNo, JSONArray jiesuanData,
                             JSONArray jiesuanArray) {
		this.roomNo = roomNo;
		this.jiesuanData = jiesuanData;
		this.jiesuanArray = jiesuanArray;
	}


	public void run(){
		ZJHGame room = (ZJHGame) RoomManage.gameRoomMap.get(roomNo);
		//房间信息
		JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
		if(roomInfo!=null){
			int game_index = roomInfo.getInt("game_index");
			// 保存游戏记录
			JSONObject gamelog = new JSONObject();
			gamelog.put("gid", 6);
			gamelog.put("room_id", roomInfo.getLong("id"));
			gamelog.put("room_no", roomNo);
			gamelog.put("game_index", game_index);
			gamelog.put("base_info", roomInfo.getString("base_info"));
			gamelog.put("result", jiesuanArray.toString());
			String nowTime = TimeUtil.getNowDate();
			gamelog.put("finishtime", nowTime);
			gamelog.put("createtime", nowTime);
			gamelog.put("status", 1);
			gamelog.put("roomtype", 0);
			if(jiesuanData.size()>0){
				gamelog.put("jiesuan", jiesuanData.toString());
			}
			
			long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);
			
			// 保存玩家战绩
			String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
					+ "gamelog_id,result,createtime,account,fee) VALUES";
			Object[] params = new Object[room.getUserIDSet().size()];
			int temp = 0;
			for(Long uid:room.getUserIDSet()){
				double score = 0;
				for (Object jsonObject : jiesuanArray) {
					JSONObject fromObject = JSONObject.fromObject(jsonObject);
					if (fromObject.getLong("id")==uid) {
						score = fromObject.getDouble("score");
					}
				}
				gamelogSql += "("+6+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+","+
						"?"+",'"+nowTime+"',"+score+","+room.getFee()+")";
				if (temp<room.getUserIDSet().size()-1) {
					gamelogSql += ",";
				}
				params[temp] = jiesuanArray.toString();
				temp ++;
//				JSONObject usergamelog = new JSONObject();
//				usergamelog.put("gid", 6);
//				usergamelog.put("room_id", roomInfo.getLong("id"));
//				usergamelog.put("room_no", roomNo);
//				usergamelog.put("game_index", game_index);
//				usergamelog.put("user_id", uid);
//				usergamelog.put("gamelog_id", gamelog_id);
//				usergamelog.put("result", jiesuanArray.toString());
//				usergamelog.put("createtime", nowTime);
//				usergamelog.put("account", score);
//				usergamelog.put("fee", room.getFee());
				
//				mjBiz.addUserGameLog(usergamelog);
			}
			DBUtil.executeUpdateBySQL(gamelogSql, params);

			// 更新房间局数序号
			String sql = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
			DBUtil.executeUpdateBySQL(sql, new Object[]{roomNo});
		}
	}
}
