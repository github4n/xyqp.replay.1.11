package com.zhuoan.biz.service.majiang.impl;

import com.zhuoan.biz.service.GlobalService;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Service
public class MajiangBizImpl implements MaJiangBiz {
	
	@Override
	public JSONObject getUserInfoByID(long id){
		String sql="select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,"
				+ "logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,"
				+ "operatorMark,isManag,Losevalue,wholecost,sign,isown,platform from za_users where id=?";
		return DBUtil.getObjectBySQL(sql, new Object[]{id});
	}
	
	@Override
	public JSONObject getUserInfoByAccount(String account) {
		String sql="select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,"
				+ "logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,"
				+ "operatorMark,isManag,Losevalue,wholecost,sign,isown,platform from za_users where account=?";
		return DBUtil.getObjectBySQL(sql, new Object[]{account});
	}
	
	@Override
	public JSONObject checkUUID(long userID, String uuid) {
		
		String sql="select uuid from za_users where id=?";
		JSONObject resultJson = DBUtil.getObjectBySQL(sql, new Object[]{userID});
		JSONObject jsonObject = new JSONObject();
		if(resultJson==null){
			jsonObject.put("msg", "用户不存在");
			jsonObject.put("data", "");
			jsonObject.put("code", 0);
		} else if (uuid.equals(resultJson.get("uuid"))){
			jsonObject.put("msg", "");
			jsonObject.put("data", "");
			jsonObject.put("code", 1);
		} else{
			jsonObject.put("msg", "该帐号已在其他地方登录");
			jsonObject.put("data", "");
			jsonObject.put("code", 0);
		}
		return jsonObject;
	}
	
	@Override
	public JSONObject checkUUID(String account, String uuid) {

		String sql="select uuid from za_users where account=?";
		JSONObject resultJson = DBUtil.getObjectBySQL(sql, new Object[]{account});
		JSONObject jsonObject = new JSONObject();
		if(resultJson==null){
			jsonObject.put("msg", "用户不存在");
			jsonObject.put("data", "");
			jsonObject.put("code", 0);
		} else if (uuid.equals(resultJson.get("uuid"))){
			jsonObject.put("msg", "");
			jsonObject.put("data", "");
			jsonObject.put("code", 1);
		} else{
			jsonObject.put("msg", "该帐号已在其他地方登录");
			jsonObject.put("data", "");
			jsonObject.put("code", 0);
		}
		return jsonObject;
	}
	
	@Override
	public JSONObject getRoomInfoByRno(String roomNo) {

		String sql="select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,"
				+ "game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,"
				+ "user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,"
				+ "user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,"
				+ "user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,"
				+ "user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,"
				+ "user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open "
				+ "from za_gamerooms where status>=0 and room_no=? order by id desc";
		return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
	}
	@Override
	public JSONObject getRoomInfoByRno1(String roomNo) {
		
		String sql="select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,"
				+ "game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,"
				+ "user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,"
				+ "user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,"
				+ "user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,"
				+ "user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,"
				+ "user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open "
				+ "from za_gamerooms where status<0 and room_no=? order by id desc";
		return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
	}
	
	@Override
	public int updateRoomInfoByRid(JSONObject roominfo){
		return DBJsonUtil.update(roominfo, "za_gamerooms");
	}
	
	@Override
	public int addOrUpdateGameLog(JSONObject gamelog) {

		int back = DBJsonUtil.sava(gamelog, "za_gamelogs");
		int gamelog_id = 0;
		if(back>0){
			JSONObject result = DBUtil.getObjectBySQL("select id from za_gamelogs where gid=? and room_id=? and room_no=? and game_index=?",
					new Object[]{gamelog.get("gid"), gamelog.get("room_id"), gamelog.get("room_no"), gamelog.get("game_index")});
			
			if(result!=null){
				gamelog_id = result.getInt("id");
			}
		}
		return gamelog_id;
	}
	
	@Override
	public long getGameLogId(long room_id, int game_index) {
		String sql="select id from za_gamelogs where room_id=? and game_index=?";
		JSONObject result=DBUtil.getObjectBySQL(sql, new Object[]{room_id,game_index});
		if(result==null || !result.has("id")) return -1;
		return result.getLong("id");
	}
	
	@Override
	public int addUserGameLog(JSONObject usergamelog) {
		return DBJsonUtil.add(usergamelog, "za_usergamelogs");
	}
	
	@Override
	public JSONObject getRoomInfoSeting(int gameID, String optkey) {
		String sql="select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open"
				+ " from za_gamesetting where game_id=? and opt_key=?";
		return DBUtil.getObjectBySQL(sql, new Object[]{gameID,optkey});
	
	}

	@Override
	public boolean dealGoldRoomFee(JSONArray userIds, String roomNo, int gid, double fee, String type) {
//
//		try {
//			IService server = (IService) RegisterServer.registry.lookup("sysService");
//			JSONObject result = server.pump(userIds.toString(), roomNo, String.valueOf(gid), String.valueOf(fee), type);
//			LogUtil.print("玩家扣水:"+result);
//			if(result.containsKey("code")&&result.getInt("code")==1){
//				return true;
//			}
//		} catch (RemoteException e) {
//			Logger.getLogger(NiuNiuServiceImpl.class).error(e.getMessage(), e);
//			e.printStackTrace();
//		} catch (NotBoundException e) {
//			Logger.getLogger(NiuNiuServiceImpl.class).error(e.getMessage(), e);
//			e.printStackTrace();
//		}
		return false;
	}

	@Override
	public JSONObject getGameInfoByID(long id) {
		String sql="select id,name,logo,type,gameType,status,setting,isUse,clearTime from za_games where id=?";
		return DBUtil.getObjectBySQL(sql, new Object[]{id});
		
	}

	@Override
	public boolean stopJoin(String roomNo) {
		
		// 获取房间信息
		String sql = "select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,"
				+ "game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,"
				+ "user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,"
				+ "user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,"
				+ "user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,"
				+ "user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,"
				+ "user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open"
				+ " from za_gamerooms where room_no=? order by id desc";
		Object[] params = new Object[]{roomNo};
		JSONObject roominfo = DBUtil.getObjectBySQL(sql, params);
		
		StringBuffer sqlsb = new StringBuffer("update za_gamerooms set ");
		List<Integer> paramList = new ArrayList<Integer>();
		int maxCount = 10;
		// 找出没人的空位
		for (int i = 0; i < maxCount; i++) {
			if(roominfo.getInt("user_id"+i)==0){
				sqlsb.append("user_id"+i+"=? ");
				if(i!=maxCount-1){
					sqlsb.append(",");
				}
				// 将没用的座位置为-1
				paramList.add(-1);
			}
		}
		if(paramList.size()>0){
			
			sqlsb.append("where id=?");
			paramList.add(roominfo.getInt("id"));
			int i = DBUtil.executeUpdateBySQL(sqlsb.toString(), paramList.toArray());
			if(i>0){
				return true;
			}
		}
		return false;
	}

	
	@Override
	public boolean delGameRoomUserByUid(JSONObject room, long userId) {
		
		String userIndex = null;
		
		if(userId==room.getLong("user_id0")){
			userIndex = "user_id0";
		}
		if(userId==room.getLong("user_id1")){
			userIndex = "user_id1";
		}
		if(userId==room.getLong("user_id2")){
			userIndex = "user_id2";
		}
		if(userId==room.getLong("user_id3")){
			userIndex = "user_id3";
		}
		if(userId==room.getLong("user_id4")){
			userIndex = "user_id4";
		}
		if(userId==room.getLong("user_id5")){
			userIndex = "user_id5";
		}
		if(userId==room.getLong("user_id6")){
			userIndex = "user_id6";
		}
		if(userId==room.getLong("user_id7")){
			userIndex = "user_id7";
		}
		if(userId==room.getLong("user_id8")){
			userIndex = "user_id8";
		}
		if(userId==room.getLong("user_id9")){
			userIndex = "user_id9";
		}
		
		if(userIndex!=null){
			
			String sql = "update za_gamerooms set "+userIndex+"=? where status=0 and id=?";
			//DBUtil.executeUpdateBySQL(sql, new Object[]{0, room.getLong("id")});
			GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{0, room.getLong("id")}, 3));
			return true;
		}
		
		return false;
	}

	@Override
	public JSONObject getGameSetting() {
		
		String sql="select id,isXipai,xipaiObj,xipaiLayer,xipaiCount,bangObj,bangData,pumpData,bangCount from app_game_setting";
		return DBUtil.getObjectBySQL(sql, new Object[]{});
	}

	@Override
	public List<String> getRobotList(int count) {
		
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < count; i++) {
			String sql = "select account from za_users where openid='0' and status=0 limit ?,1";
			JSONObject jsonObject = DBUtil.getObjectBySQL(sql, new Object[]{i});
			if (jsonObject!=null) {
				list.add(jsonObject.getString("account"));
				sql = "update za_users set status=1,yuanbao=? where account=?";
				DBUtil.executeUpdateBySQL(sql, new Object[]{25000+new Random().nextInt(50000),jsonObject.getString("account")});
//				DBUtil.executeUpdateBySQL(sql, new Object[]{1000,jsonObject.getString("account")});
			}
		}
		return list;
	}

	@Override
	public boolean pump(JSONArray userIds, String roomNo, int gid, double fee,
                        String type) {
		String sql = "select id from za_gamerooms where room_no=?";
		JSONObject roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
		JSONArray users = JSONArray.fromObject(userIds);
		Object[] params = new Object[users.size()];
		sql = "select id,platform,roomcard,coins,yuanbao from za_users where id in(";
		for (int i = 0; i < users.size(); i++) {
			params[i] = users.getString(i);
			sql += "?";
			if (i<users.size()-1) {
				sql += ",";
			}else {
				sql += ")";
			}
		}
		// 获取玩家信息
		JSONArray objectListBySQL = DBUtil.getObjectListBySQL(sql, params);
		// 平台号
		String platform="";
		// 扣费类型
		int type1 = 0;
		// 当前时间
		String nowTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		if (type.equals("roomcard")) {
			type1 = 0;
		}else if (type.equals("coins")) {
			type1 = 1;
		}else if (type.equals("yuanbao")) {
			type1 = 3;
		}
		
		// 更新元宝、房卡、金币
		sql = "UPDATE za_users SET "+type+"="+type+"- CASE id";
//		String sql2="update za_users za,base_users ba set ba."+type+"=ba."+type+"- CASE za.id";
		String sqlString2 = " END WHERE $ IN (";
		String addSql = "insert into za_userdeduction (userid,roomid,roomNO,gid,type,sum,doType,creataTime,memo,"
				+ "platform) values ";
		for (int i = 0; i < objectListBySQL.size(); i++) {
			JSONObject user = objectListBySQL.getJSONObject(i);
			long uid = user.getLong("id");
			if (user.containsKey("platform")&&!Dto.stringIsNULL(user.getString("platform"))) {
				platform=user.getString("platform");
			}
			// 数量不足
			if (type.equals("yuanbao")) {
				if (user.getDouble(type)<Double.parseDouble(String.valueOf(fee))) {	
					fee=user.getDouble("yuanbao");
				}
			}else if (type.equals("roomcard")) {
				if (user.getInt(type)<Integer.parseInt(String.valueOf(fee))) {	
					fee=user.getInt("roomcard");
				}
			}else if (type.equals("coins")) {
				if (user.getDouble(type)<Double.parseDouble(String.valueOf(fee))) {	
					fee=user.getDouble("coins");
				}
			}
			sql += " WHEN "+uid+" THEN "+fee;
//			sql2 += " WHEN "+uid+" THEN "+fee;
			addSql += "("+uid+","+roominfo.getLong("id")+",'"+roomNo+"',"+gid+","+type1+","+(-fee)+","+
					2+",'"+nowTime+"','','"+platform+"')";
			if (i==objectListBySQL.size()-1) {
				sqlString2 += uid+")";
			}else {
				sqlString2 += uid+",";
				addSql += ",";
			}
		}
		sql += sqlString2.replace("$", "id");
//		sql2 += (sqlString2.replace("$", "za.id")+" and za.unionid=ba.unionID");
		Object[] objects = new Object[]{};
		DBUtil.executeUpdateBySQL(sql, objects);
//		DBUtil.executeUpdateBySQL(sql2, objects);
		DBUtil.executeUpdateBySQL(addSql, objects);
		return false;
	}
	
	public void settlementRoomNo(String roomNo){
		//扣除数据库房间游戏房卡
		String sql2 = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
		DBUtil.executeUpdateBySQL(sql2, new Object[]{roomNo});
		
		JSONObject roomInfo = getRoomInfoByRno(roomNo);
		
		
		//判断是否是房卡模式roomtype：  0房卡，1金币
		if(!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype")==0){

			if(roomInfo.getInt("game_index")==1){ // 第一局结束需要扣除房卡

				// AA制
				if(roomInfo.containsKey("paytype") && roomInfo.getInt("paytype")==1){

					// 获取所有参与玩家
					List<Long> idList = new ArrayList<Long>();

					if(roomInfo.getLong("user_id0")>0){

						idList.add(roomInfo.getLong("user_id0"));
					}
					if(roomInfo.getLong("user_id1")>0){

						idList.add(roomInfo.getLong("user_id1"));
					}
					if(roomInfo.getLong("user_id2")>0){

						idList.add(roomInfo.getLong("user_id2"));
					}
					if(roomInfo.getLong("user_id3")>0){

						idList.add(roomInfo.getLong("user_id3"));
					}
					if(roomInfo.getLong("user_id4")>0){

						idList.add(roomInfo.getLong("user_id4"));
					}
					if(roomInfo.getLong("user_id5")>0){

						idList.add(roomInfo.getLong("user_id5"));
					}
					if(roomInfo.getLong("user_id6")>0){

						idList.add(roomInfo.getLong("user_id6"));
					}
					if(roomInfo.getLong("user_id7")>0){

						idList.add(roomInfo.getLong("user_id7"));
					}
					if(roomInfo.getLong("user_id8")>0){

						idList.add(roomInfo.getLong("user_id8"));
					}
					if(roomInfo.getLong("user_id9")>0){

						idList.add(roomInfo.getLong("user_id9"));
					}

					JSONObject base_info = roomInfo.getJSONObject("base_info");

					String sql4 = "UPDATE za_users SET roomcard=roomcard- CASE id";
					String sqlString2 = " END WHERE id IN (";
					String addSql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,creataTime) values ";
					int temp = 0;
					for (Long userid : idList) {

						sql4 += " WHEN "+userid+" THEN "+base_info.getJSONObject("turn").getInt("AANum");
						addSql += "("+userid+","+roomInfo.getLong("id")+",'"+roomNo+"',"+roomInfo.getInt("game_id")+","+0+","
								+(-base_info.getJSONObject("turn").getInt("AANum"))+",'"
								+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+"')";
						if (temp==idList.size()-1) {
							sqlString2 += userid+")";
						}else {
							sqlString2 += userid+",";
							addSql += ",";
						}
						temp++;
					}
					sql4 += sqlString2;
					DBUtil.executeUpdateBySQL(sql4, new Object[]{});
					DBUtil.executeUpdateBySQL(addSql, new Object[]{});

				}else{ // 支付类型为房主支付

					JSONObject base_info = roomInfo.getJSONObject("base_info");

					// 固定房费
					int roomcard = base_info.getJSONObject("turn").getInt("roomcard");

					if(!base_info.getJSONObject("turn").containsKey("noAANum")){
						// 另一种计算房卡的方式，即玩家人数*单价
						if(base_info.containsKey("player")&&base_info.getJSONObject("turn").containsKey("AANum")){
							roomcard = base_info.getJSONObject("turn").getInt("AANum") * base_info.getInt("player");
						}
					}

					//扣除数据库用户游戏房卡数--(房卡扣除规则：用户下完完整一局后才扣除房间总房卡数)
					String sql4 = "update za_users set roomcard=roomcard-? where id=?";
					DBUtil.executeUpdateBySQL(sql4, new Object[]{roomcard, roomInfo.getLong("user_id0")});

					//扣除房卡记录
					String sql1 = "insert into za_userdeduction(userid,roomid,gid,roomNo,type,sum,creataTime) values(?,?,?,?,?,?,?)";
					DBUtil.executeUpdateBySQL(sql1, new Object[]{roomInfo.getLong("user_id0"),roomInfo.getInt("id"),roomInfo.getInt("game_id"), roomNo,0,-roomcard, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())});
				}
			}

			if(roomInfo.getInt("game_count")<=roomInfo.getInt("game_index")){
				//房卡已用完，解散房间
				String sql3 = "update za_gamerooms set status=? where room_no=? order by id desc";
				DBUtil.executeUpdateBySQL(sql3, new Object[]{-2, roomNo});
			}

		}else if(!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype")==2){
			System.out.println("进入代开房间扣房卡方法base_info："+roomInfo.getJSONObject("base_info"));
			if(roomInfo.getInt("game_index")==1){ // 第一局结束需要扣除房卡
			}

			if(roomInfo.getInt("game_count")<=roomInfo.getInt("game_index")){
				//房卡已用完，解散房间
				String sql3 = "update za_gamerooms set status=? where room_no=? order by id desc";
				DBUtil.executeUpdateBySQL(sql3, new Object[]{-2, roomNo});

				//进入代开房间重开方法
				GlobalService.insertGameRoom(roomNo);

			}
		}

	}

	@Override
	public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray) {
		String sql = "select id from za_gamelogs where room_no=? and gid=? ORDER BY game_index DESC limit 1";
		JSONObject objectBySQL = DBUtil.getObjectBySQL(sql, new Object[]{roomNo,gid});
		System.err.println(objectBySQL);
		if (!Dto.isObjNull(objectBySQL)) {
			sql = "update za_gamelogs set jiesuan=? where id=?";
			DBUtil.executeUpdateBySQL(sql, new Object[]{jsonArray.toString(),objectBySQL.getLong("id")});
		}
	}

	@Override
	public void updateUser(JSONArray jsonArray, String types) {
		
		
		String sql = "update za_users SET "+types+" = CASE id  $ END WHERE id IN (/)";
		String z="";
		String d="";
		
		for (int i = 0; i < jsonArray.size(); i++) {
			
			JSONObject uuu = jsonArray.getJSONObject(i);
			if (uuu.getDouble("total")<=0) {
				z=z+" WHEN "+uuu.getLong("id")+" THEN 0";
			}else{
				
				z=z+" WHEN "+uuu.getLong("id")+" THEN "+types+"+"+uuu.getDouble("fen");
			}
			
			d=d+uuu.getLong("id")+",";
		}
		//DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{});
		GameMain.sqlQueue.addSqlTask(new SqlModel(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
	}
	
	@Override
	public void insertUserdeduction(JSONObject obj){
		
		StringBuffer sqlx=new StringBuffer();
		sqlx.append("insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values $");
		String ve="";
		String te=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
		
		JSONArray jsoaArray =obj.getJSONArray("user");
		for (int i = 0; i < jsoaArray.size(); i++) {
			JSONObject uuu = jsoaArray.getJSONObject(i);
			ve=ve+"("+uuu.getLong("id")+","+obj.getInt("gid")+",'"+obj.getString("roomNo")+"',"+obj.getInt("type")+","+uuu.getDouble("fen")+",'"+te+"'),";
		}
		//DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{});
		GameMain.sqlQueue.addSqlTask(new SqlModel(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{}, SqlModel.EXECUTEUPDATEBYSQL));
	}

	@Override
	public JSONArray getUserGameLogList(String account, int gid, int num, String createTime) {
		String sql = "select id from za_users where account=?";
		JSONObject objectBySQL = DBUtil.getObjectBySQL(sql, new Object[]{account});
		if (!Dto.isObjNull(objectBySQL)) {
			if (Dto.stringIsNULL(createTime)) {
				sql=" select id,gid,user_id,gamelog_id,result,createtime,room_no from za_usergamelogs where gid=? and user_id=? GROUP BY gamelog_id  order by id desc LIMIT ?";
				return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{gid,objectBySQL.getLong("id"),num}), "createtime", "yyyy-MM-dd hh:mm:ss");
			}else {
				sql=" select id,gid,user_id,gamelog_id,result,createtime,room_no from za_usergamelogs where gid=? and user_id=? and createtime<? GROUP BY gamelog_id  order by id desc LIMIT ?";
				return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql, new Object[]{gid,objectBySQL.getLong("id"),createTime,num}), "createtime", "yyyy-MM-dd hh:mm:ss");
			}
		}
		return null;
	}

	@Override
	public JSONArray getYbUpdate() {
		String sql = "select id,account,yuanbao,status from za_yb_update";
		JSONArray objectListBySQL = DBUtil.getObjectListBySQL(sql,new Object[]{});
		return objectListBySQL;
	}

	@Override
	public void deleteYbStatus(long id) {
		String sql = "delete from za_yb_update where id=?";
		DBUtil.executeUpdateBySQL(sql, new Object[]{id});
 	}
}

