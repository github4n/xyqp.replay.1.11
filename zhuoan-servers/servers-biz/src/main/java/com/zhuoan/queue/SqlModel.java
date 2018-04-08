package com.zhuoan.queue;

import net.sf.json.JSONArray;

public class SqlModel {
	
	public static final int GETOBJECTBYSQL = 1;
	public static final int GETOBJECTLISTBYSQL = 2;
	public static final int EXECUTEUPDATEBYSQL = 3;
	
	
	public String sql;// sql
	public Object[] params;// 参数
	public int type;// 1:查询单条数据   2:查询多条数据   3:更新
	
	public SqlModel(String sql, Object[] params, int type) {
		this.sql = sql;
		this.params = params;
		this.type = type;
	}
	
	JSONArray userIds;
	String roomNo;
	int gid;
	double fee;
	String type1;

	public SqlModel(int type, JSONArray userIds, String roomNo, int gid,
                    double fee, String type1) {
		this.type = type;
		this.userIds = userIds;
		this.roomNo = roomNo;
		this.gid = gid;
		this.fee = fee;
		this.type1 = type1;
	}
	
}
