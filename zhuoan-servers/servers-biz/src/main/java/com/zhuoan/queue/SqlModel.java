package com.zhuoan.queue;

import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.model.bdx.BDXGameRoom;
import com.zhuoan.biz.model.nn.NNGameRoom;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.Arrays;

public class SqlModel implements Serializable{
    private static final long serialVersionUID = -5236848137376023025L;
    public static final int GETOBJECTBYSQL = 1;
    public static final int GETOBJECTLISTBYSQL = 2;
    public static final int EXECUTEUPDATEBYSQL = 3;
    public static final int PUMP = 4;
    public static final int SAVELOGS_NN = 5;
    public static final int SAVELOGS_SSS = 6;
    public static final int SAVELOGS_ZJH = 7;
    public static final int SAVELOGS_BDX = 8;

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

    // 抽水
    public SqlModel(int type, JSONArray userIds, String roomNo, int gid,
                    double fee, String type1) {
        this.type = type;
        this.userIds = userIds;
        this.roomNo = roomNo;
        this.gid = gid;
        this.fee = fee;
        this.type1 = type1;
    }

    // 牛牛战绩
    NNGameRoom room_nn;
    JSONObject gamelog_nn;
    JSONArray uglogs_nn;
    int type_nn;
    JSONArray array_nn;

    public SqlModel(int type, NNGameRoom room_nn, JSONObject gamelog_nn,
                    JSONArray uglogs_nn, int type_nn, JSONArray array_nn) {
        this.type = type;
        this.room_nn = room_nn;
        this.gamelog_nn = gamelog_nn;
        this.uglogs_nn = uglogs_nn;
        this.type_nn = type_nn;
        this.array_nn = array_nn;
    }

    // 炸金花战绩
    String roomNo_zjh;
    JSONArray jiesuanData_zjh;
    JSONArray jiesuanArray_zjh;
    public SqlModel(int type, String roomNo_zjh, JSONArray jiesuanData_zjh,
                    JSONArray jiesuanArray_zjh) {
        this.type = type;
        this.roomNo_zjh = roomNo_zjh;
        this.jiesuanData_zjh = jiesuanData_zjh;
        this.jiesuanArray_zjh = jiesuanArray_zjh;
    }

    // 十三水战绩
    SSSGameRoom room_sss;
    JSONArray us_sss;
    JSONArray uid_sss;
    boolean e_sss;
    public SqlModel(int type, SSSGameRoom room_sss, JSONArray us_sss,
                    JSONArray uid_sss, boolean e_sss) {
        this.type = type;
        this.room_sss = room_sss;
        this.us_sss = us_sss;
        this.uid_sss = uid_sss;
        this.e_sss = e_sss;
    }

    // 比大小战绩
    BDXGameRoom room_bdx;
    JSONArray us_bdx;
    public SqlModel(int type, BDXGameRoom room_bdx, JSONArray us_bdx) {
        this.type = type;
        this.room_bdx = room_bdx;
        this.us_bdx = us_bdx;
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

    public NNGameRoom getRoom_nn() {
        return room_nn;
    }

    public void setRoom_nn(NNGameRoom room_nn) {
        this.room_nn = room_nn;
    }

    public JSONObject getGamelog_nn() {
        return gamelog_nn;
    }

    public void setGamelog_nn(JSONObject gamelog_nn) {
        this.gamelog_nn = gamelog_nn;
    }

    public JSONArray getUglogs_nn() {
        return uglogs_nn;
    }

    public void setUglogs_nn(JSONArray uglogs_nn) {
        this.uglogs_nn = uglogs_nn;
    }

    public int getType_nn() {
        return type_nn;
    }

    public void setType_nn(int type_nn) {
        this.type_nn = type_nn;
    }

    public JSONArray getArray_nn() {
        return array_nn;
    }

    public void setArray_nn(JSONArray array_nn) {
        this.array_nn = array_nn;
    }

    public String getRoomNo_zjh() {
        return roomNo_zjh;
    }

    public void setRoomNo_zjh(String roomNo_zjh) {
        this.roomNo_zjh = roomNo_zjh;
    }

    public JSONArray getJiesuanData_zjh() {
        return jiesuanData_zjh;
    }

    public void setJiesuanData_zjh(JSONArray jiesuanData_zjh) {
        this.jiesuanData_zjh = jiesuanData_zjh;
    }

    public JSONArray getJiesuanArray_zjh() {
        return jiesuanArray_zjh;
    }

    public void setJiesuanArray_zjh(JSONArray jiesuanArray_zjh) {
        this.jiesuanArray_zjh = jiesuanArray_zjh;
    }

    public SSSGameRoom getRoom_sss() {
        return room_sss;
    }

    public void setRoom_sss(SSSGameRoom room_sss) {
        this.room_sss = room_sss;
    }

    public JSONArray getUs_sss() {
        return us_sss;
    }

    public void setUs_sss(JSONArray us_sss) {
        this.us_sss = us_sss;
    }

    public JSONArray getUid_sss() {
        return uid_sss;
    }

    public void setUid_sss(JSONArray uid_sss) {
        this.uid_sss = uid_sss;
    }

    public boolean isE_sss() {
        return e_sss;
    }

    public void setE_sss(boolean e_sss) {
        this.e_sss = e_sss;
    }

    public BDXGameRoom getRoom_bdx() {
        return room_bdx;
    }

    public void setRoom_bdx(BDXGameRoom room_bdx) {
        this.room_bdx = room_bdx;
    }

    public JSONArray getUs_bdx() {
        return us_bdx;
    }

    public void setUs_bdx(JSONArray us_bdx) {
        this.us_bdx = us_bdx;
    }

    @Override
    public String toString() {
        return "SqlModel{" +
            "sql='" + sql + '\'' +
            ", params=" + Arrays.toString(params) +
            ", type=" + type +
            ", userIds=" + userIds +
            ", roomNo='" + roomNo + '\'' +
            ", gid=" + gid +
            ", fee=" + fee +
            ", type1='" + type1 + '\'' +
            ", room_nn=" + room_nn +
            ", gamelog_nn=" + gamelog_nn +
            ", uglogs_nn=" + uglogs_nn +
            ", type_nn=" + type_nn +
            ", array_nn=" + array_nn +
            ", roomNo_zjh='" + roomNo_zjh + '\'' +
            ", jiesuanData_zjh=" + jiesuanData_zjh +
            ", jiesuanArray_zjh=" + jiesuanArray_zjh +
            ", room_sss=" + room_sss +
            ", us_sss=" + us_sss +
            ", uid_sss=" + uid_sss +
            ", e_sss=" + e_sss +
            ", room_bdx=" + room_bdx +
            ", us_bdx=" + us_bdx +
            '}';
    }
}
