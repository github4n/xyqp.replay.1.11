package com.zhuoan.biz.event.nn.deal;

import com.zhuoan.dao.DBUtil;
import com.zhuoan.biz.model.nn.NNGameRoom;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveLogsThreadNN extends Thread {

	private MaJiangBiz mjBiz;
	private NNGameRoom room;
	JSONObject gamelog;
	JSONArray uglogs;
	int type;
	JSONArray array;


	public SaveLogsThreadNN(MaJiangBiz mjBiz, NNGameRoom room,
                            JSONObject gamelog, JSONArray uglogs, int type, JSONArray array) {
		this.mjBiz = mjBiz;
		this.room = room;
		this.gamelog = gamelog;
		this.uglogs = uglogs;
		this.type = type;
		this.array = array;
	}


	public void run() {
		// 房间号
		String roomNo = room.getRoomNo();
		//更新房间信息
		JSONObject roomInfo = mjBiz.getRoomInfoByRno(roomNo);
		if(roomInfo!=null&&roomInfo.containsKey("id")){
			int game_index = roomInfo.getInt("game_index")+1;
			String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
			long gamelog_id = mjBiz.addOrUpdateGameLog(gamelog);
			if (type==1) {// 房卡场
				// 保存玩家战绩
				String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
						+ "gamelog_id,result,createtime) VALUES";
				Object[] params = new Object[]{};
				int temp = 0;
				for(Long uid:room.getUserIDSet()){
					gamelogSql += "("+1+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+",'"+
							uglogs.toString()+"','"+nowTime+"')";
					if (temp < room.getUserIDSet().size()-1) {
						gamelogSql += ",";
					}
					temp++;
				}
				DBUtil.executeUpdateBySQL(gamelogSql, params);
			}else if (type==2) {// 元宝、金币场
				String gamelogSql = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,"
						+ "gamelog_id,result,createtime,account,fee) VALUES";
				Object[] params = new Object[array.size()];
				for (int i = 0; i < array.size(); i++) {
					JSONObject user = array.getJSONObject(i);
					long uid = user.getLong("uid");
					gamelogSql += "("+1+","+roomInfo.getLong("id")+",'"+roomNo+"',"+game_index+","+uid+","+gamelog_id+","+
							"?"+",'"+nowTime+"',"+user.get("score")+","+room.getFee()+")";
					if (i!=array.size()-1) {
						gamelogSql += ",";
					}
					params[i] = uglogs.toString();
				}
				DBUtil.executeUpdateBySQL(gamelogSql, params);
			}
		}
	}
}
