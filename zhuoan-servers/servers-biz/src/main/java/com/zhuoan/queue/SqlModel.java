package com.zhuoan.queue;

import net.sf.json.JSONArray;

import java.io.Serializable;

public class SqlModel implements Serializable{
	
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

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public Object[] getParams() {
        return params;
    }

    public void setParams(Object[] params) {
        this.params = params;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public JSONArray getUserIds() {
        return userIds;
    }

    public void setUserIds(JSONArray userIds) {
        this.userIds = userIds;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public String getType1() {
        return type1;
    }

    public void setType1(String type1) {
        this.type1 = type1;
    }
}
