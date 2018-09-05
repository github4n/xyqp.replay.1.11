package com.zhuoan.biz.game.dao.impl;

import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.dao.DBJsonUtil;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.DateUtils;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author huaping.li
 * @Description: 底层数据操作实现类
 * @date 2018-04-17 15:07
 */
@Component
public class GameDaoImpl implements GameDao {

    @Override
    public void insertGameRoom(JSONObject obj) {
        DBJsonUtil.saveOrUpdate(obj,"za_gamerooms");
    }

    /**
     * 根据用户ID获取用户信息
     *
     * @param id 用户ID
     * @return
     */
    @Override
    public JSONObject getUserByID(long id) {

        String sql="select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,"
            + "logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,"
            + "operatorMark,isManag,Losevalue,wholecost,sign,isown,platform,pumpVal from za_users where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    /**
     * 根据玩家账号获取用户信息
     *
     * @param account
     * @return
     */
    @Override
    public JSONObject getUserByAccount(String account) {

        String sql="select id,account,name,password,tel,sex,headimg,area,lv,roomcard,coins,score,createtime,ip,"
            + "logintime,openid,unionid,uuid,status,isAuthentication,memo,vip,safe,luck,safeprice,yuanbao,"
            + "operatorMark,isManag,Losevalue,wholecost,sign,isown,platform,gulidId,pumpVal,Identification from za_users where account=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{account});
    }


    /**
     * 更新用户 余额
     *
     * @param data  [{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}]
     * @param types 更新类型  元宝:yuanbao  金币：coins   房卡：roomcard  积分：score
     */
    @Override
    public boolean updateUserBalance(JSONArray data, String types) {

        // TODO: 2018-04-18  
        String sql = "update za_users SET "+types+" = CASE id  $ END WHERE id IN (/)";
        String z="";
        String d="";

        for (int i = 0; i < data.size(); i++) {

            JSONObject uuu = data.getJSONObject(i);
            if (uuu.getDouble("total")<=0) {
                z=z+" WHEN "+uuu.getLong("id")+" THEN 0";
            }else{
                z=z+" WHEN "+uuu.getLong("id")+" THEN "+types+"+"+uuu.getDouble("fen");
            }
            d=d+uuu.getLong("id")+",";
        }
        DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length() - 1)), new Object[]{});
        return false;
    }

    /**
     * 插入分润表数据
     *
     * @param obj {"user":[{"id":XXX,"fen":XXX},{"id":XXX,"fen":XXX}],"gid":x,"roomNo":xxx,"type":xxxx}
     */
    @Override
    public void insertUserdeduction(JSONObject obj) {

        // TODO: 2018-04-18
        StringBuffer sqlx=new StringBuffer();
        sqlx.append("insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime,pocketNew,pocketOld,pocketChange,operatorType,memo) values $");
        String ve="";
        String te=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        JSONArray jsoaArray =obj.getJSONArray("user");
        for (int i = 0; i < jsoaArray.size(); i++) {
            JSONObject uuu = jsoaArray.getJSONObject(i);
            ve=ve+"("+uuu.getLong("id")+","+uuu.getInt("gid")+",'"+uuu.getString("roomNo")+"',"+uuu.getInt("type")+","+
                uuu.getDouble("fen")+",'"+te+"',"+uuu.getDouble("new")+","+uuu.getDouble("old")+","+uuu.getDouble("fen")+
                ","+CommonConstant.SCORE_CHANGE_TYPE_GAME+",'游戏输赢'),";
        }
        DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{});
    }

    /**
     * 获取待更新到缓存的元宝
     *
     * @return JSONArray
     * @throws
     * @date 2018年3月28日
     */
    @Override
    public JSONArray getYbUpdateLog() {

        String sql = "select id,account,yuanbao,status from za_yb_update where status=0";
        JSONArray objectListBySQL = DBUtil.getObjectListBySQL(sql,new Object[]{});
        return objectListBySQL;
    }

    /**
     * 更新状态
     *
     * @param id@return void
     * @throws
     * @date 2018年3月28日
     */
    @Override
    public void delYbUpdateLog(long id) {

        String sql = "update za_yb_update set status=-1 where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{id});
    }

    /**
     * 获取工会信息
     *
     * @param userInfo
     * @return JSONObject
     * @throws
     * @date 2018年4月10日
     */
    @Override
    public JSONObject getGongHui(JSONObject userInfo) {
        String sql = "select id,code,name,isUse,platform from guild where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{userInfo.getLong("gulidId")});
    }

    /**
     * 根据游戏ID获取游戏信息
     *
     * @param id 游戏ID
     * @return
     */
    @Override
    public JSONObject getGameInfoByID(long id) {

        String sql="select id,name,logo,type,gameType,status,setting,isUse,clearTime from za_games where id=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{id});
    }

    /**
     * 根据房间号获取房间信息
     *
     * @param roomNo
     * @return
     */
    @Override
    public JSONObject getRoomInfoByRno(String roomNo) {

        String sql="select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,"
            + "game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,"
            + "user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,"
            + "user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,"
            + "user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,"
            + "user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,"
            + "user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open "
            + "from za_gamerooms where room_no=? and status>=0 order by id desc";
        return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
    }

    /**
     * 根据房间id更新房间信息(键名需和表中字段名相同)
     *
     * @param roominfo
     * @return
     */
    @Override
    public int updateRoomInfoByRid(JSONObject roominfo) {

        return DBJsonUtil.update(roominfo, "za_gamerooms");
    }

    /**
     * 根据游戏ID 获取对应设置
     *
     * @param gameID
     * @param optkey
     * @return
     */
    @Override
    public JSONObject getRoomInfoSeting(int gameID, String optkey) {

        String sql="select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open"
            + " from za_gamesetting where game_id=? and opt_key=?";
        return DBUtil.getObjectBySQL(sql, new Object[]{gameID,optkey});
    }


    /**
     * 将房间内userid由初始值置为-1
     *
     * @param roomNo
     * @return
     */
    @Override
    public boolean updateGameRoomUserId(String roomNo) {

        // 获取房间信息
        JSONObject roomInfo = getRoomInfoByRno(roomNo);
        if (!Dto.isObjNull(roomInfo)){
            StringBuffer sql = new StringBuffer("update za_gamerooms set ");
            List<Integer> paramList = new ArrayList<Integer>();
            int maxCount = 10;
            // 找出没人的空位
            for (int i = 0; i < maxCount; i++) {
                if(roomInfo.getInt("user_id"+i)==0){
                    sql.append("user_id"+i+"=? ");
                    if(i!=maxCount-1){
                        sql.append(",");
                    }
                    // 将没用的座位置为-1
                    paramList.add(-1);
                }
            }
            if(paramList.size()>0){

                sql.append("where id=?");
                paramList.add(roomInfo.getInt("id"));
                int i = DBUtil.executeUpdateBySQL(sql.toString(), paramList.toArray());
                if(i>0){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据房间号获取无用的房间信息（结束或解散 status<0）
     *
     * @param roomNo
     * @return
     */
    @Override
    public JSONObject getRoomInfoByRnoNotUse(String roomNo) {

        String sql="select id,server_id,game_id,room_no,roomtype,base_info,createtime,game_count,game_index,"
            + "game_score,game_coins,user_id0,user_icon0,user_name0,user_score0,user_id1,user_icon1,"
            + "user_name1,user_score1,user_id2,user_icon2,user_name2,user_score2,user_id3,user_icon3,"
            + "user_name3,user_score3,user_id4,user_icon4,user_name4,user_score4,user_id5,user_icon5,"
            + "user_name5,user_score5,ip,port,status,paytype,level,fangzhu,user_id6,user_icon6,user_name6,"
            + "user_score6,user_id7,user_icon7,user_name7,user_score7,user_id8,user_icon8,user_name8,"
            + "user_score8,user_id9,user_icon9,user_name9,user_score9,stoptime,open "
            + "from za_gamerooms where room_no=? and status<0 order by id desc";
        return DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
    }

    /**
     * 删除房间内玩家
     *
     * @param room
     * @param userId
     * @return
     */
    @Override
    public boolean delGameRoomUserByUid(JSONObject room, long userId) {

        String userIndex = null;
        int count = 10;
        for (int i = 0; i < count; i++){
            String uIndex = "user_id"+i;
            if(userId==room.getLong(uIndex)){
                userIndex = uIndex;
            }
        }

        if(userIndex!=null){

            String sql = "update za_gamerooms set "+userIndex+"=? where status=0 and id=?";
            //DBUtil.executeUpdateBySQL(sql, new Object[]{0, room.getLong("id")});
            GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{0, room.getLong("id")}, 3));
            return true;
        }

        return false;
    }

    /**
     * 获取游戏设置
     *
     * @return
     */
    @Override
    public JSONObject getGameSetting() {

        String sql="select id,isXipai,xipaiObj,xipaiLayer,xipaiCount,bangObj,bangData,pumpData,bangCount from app_game_setting";
        return DBUtil.getObjectBySQL(sql, new Object[]{});
    }

    @Override
    public JSONArray getRobotArray(int count,double minScore) {
        String sql = "select account,uuid from za_users where openid='0' and status=0 and coins>? limit ?,?";
        JSONArray robotArray = DBUtil.getObjectListBySQL(sql,new Object[]{minScore,0,count});
        for (int i = 0; i < robotArray.size(); i++) {
            sql = "update za_users set status=1 where account=?";
            DBUtil.executeUpdateBySQL(sql, new Object[]{robotArray.getJSONObject(i).getString("account")});
        }
        return robotArray;
    }

    /**
     * 金币场玩家抽水
     *
     * @param userIds 玩家id集合：[1,2]
     * @param roomNo
     * @param gid
     * @param fee     服务费
     * @param type    roomcard:房卡  coins:金币  yuanbao:元宝
     * @return
     */
    @Override
    public boolean pump(JSONArray userIds, String roomNo, int gid, double fee, String type) {

        String sql = "select id from za_gamerooms where room_no=?";
        long roomId;
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
            if (RoomManage.gameRoomMap.get(roomNo).getId()==0) {
                JSONObject roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
                roomId = roominfo.getLong("id");
                RoomManage.gameRoomMap.get(roomNo).setId(roomId);
            }else {
                roomId = RoomManage.gameRoomMap.get(roomNo).getId();
            }
        }else {
            JSONObject roominfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
            roomId = roominfo.getLong("id");
        }
        JSONArray users = JSONArray.fromObject(userIds);
        int userCount = users.size();
        Object[] params = new Object[userCount];
        sql = "select id,platform,roomcard,coins,yuanbao from za_users where id in(";
        for (int i = 0; i < userCount; i++) {
            params[i] = users.getString(i);
            sql += "?";
            if (i<userCount-1) {
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
        String sqlString2 = " END WHERE $ IN (";
        String addSql = "insert into za_userdeduction (userid,roomid,roomNo,gid,type,sum,doType,creataTime,memo,"
            + "platform,pocketNew,pocketOld,pocketChange,operatorType) values ";
        for (int i = 0; i < objectListBySQL.size(); i++) {
            JSONObject user = objectListBySQL.getJSONObject(i);
            long uid = user.getLong("id");
            if (user.containsKey("platform")&&!Dto.stringIsNULL(user.getString("platform"))) {
                platform=user.getString("platform");
            }
            double pocketOld = 0;
            double pocketNew = 0;
            // 数量不足
            if (type.equals("yuanbao")) {
                if (user.getDouble(type)<Double.parseDouble(String.valueOf(fee))) {
                    fee=user.getDouble("yuanbao");
                }
                pocketOld = user.getDouble("yuanbao");
                pocketNew = Dto.sub(pocketOld,fee);
            }else if (type.equals("roomcard")) {
                if (user.getInt(type)<Double.parseDouble(String.valueOf(fee))) {
                    fee=user.getInt("roomcard");
                }
                pocketOld = user.getDouble("roomcard");
                pocketNew = Dto.sub(pocketOld,fee);
            }else if (type.equals("coins")) {
                if (user.getDouble(type)<Double.parseDouble(String.valueOf(fee))) {
                    fee=user.getDouble("coins");
                }
                pocketOld = user.getDouble("coins");
                pocketNew = Dto.sub(pocketOld,fee);
            }
            String memo = "";
            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
                if (RoomManage.gameRoomMap.get(roomNo).getRoomType()==CommonConstant.ROOM_TYPE_FK) {
                    memo = "房卡场扣房卡";
                }else if (RoomManage.gameRoomMap.get(roomNo).getRoomType()==CommonConstant.ROOM_TYPE_YB) {
                    memo = "元宝场抽水";
                }
            }
            sql += " WHEN "+uid+" THEN "+fee;
            addSql += "("+uid+","+roomId+",'"+roomNo+"',"+gid+","+type1+","+(-fee)+","+
                2+",'"+nowTime+"','"+memo+"','"+platform+"',"+pocketNew+","+pocketOld+","+fee+","+
                CommonConstant.SCORE_CHANGE_TYPE_PUMP+")";
            if (i==objectListBySQL.size()-1) {
                sqlString2 += uid+")";
            }else {
                sqlString2 += uid+",";
                addSql += ",";
            }
        }
        sql += sqlString2.replace("$", "id");
        Object[] objects = new Object[]{};
        DBUtil.executeUpdateBySQL(sql, objects);
        if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null&&
            RoomManage.gameRoomMap.get(roomNo).getRoomType()!=CommonConstant.ROOM_TYPE_JB) {
            DBUtil.executeUpdateBySQL(addSql, objects);
        }
        return false;
    }

    /**
     * 更新玩家房卡
     *
     * @param roomCard
     * @param userId
     */
    @Override
    public void updateUserRoomCard(int roomCard, long userId) {

        String sql = "update za_users set roomcard=roomcard+? where id=?";
        DBUtil.executeUpdateBySQL(sql, new Object[]{roomCard, userId});
    }

    /**
     * 扣除房卡记录
     * @param data
     */
    @Override
    public void deductionRoomCardLog(Object[] data) {

        String sql1 = "insert into za_userdeduction(userid,roomid,gid,roomNo,type,sum,creataTime) values(?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql1, data);
    }

    /**
     * 保存游戏记录 (如果id<0或json中不包含id则新增一条记录,否则根据id更新记录)
     *
     * @param gamelog
     * @return
     */
    @Override
    public int addOrUpdateGameLog(JSONObject gamelog) {

        int gameLogId = 0;
        DBJsonUtil.add(gamelog,"za_gamelogs");
        return gameLogId;
    }

    /**
     * 根据room_id和game_index获取gamelog的id(未找到则返回-1)
     *
     * @param room_id
     * @param game_index
     * @return
     */
    @Override
    public long getGameLogId(long room_id, int game_index) {

        String sql="select id from za_gamelogs where room_id=? and game_index=?";
        JSONObject result=DBUtil.getObjectBySQL(sql, new Object[]{room_id,game_index});
        if(result==null || !result.has("id")){
            return -1;
        }
        return result.getLong("id");
    }

    /**
     * 保存玩家战斗记录
     *
     * @param usergamelog
     * @return
     */
    @Override
    public int addUserGameLog(JSONObject usergamelog) {

        return DBJsonUtil.add(usergamelog, "za_usergamelogs");
    }

    @Override
    public JSONObject getSysUser(String adminCode, String adminPass, String memo) {
        String sql = "select id from sys_admin where adminCode=? and adminPass=? and memo=?";
        JSONObject sysUserInfo = DBUtil.getObjectBySQL(sql, new Object[]{adminCode,adminPass,memo});
        return sysUserInfo;
    }

    @Override
    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType) {
        String sql = "SELECT id,room_no,createtime,result,gamelog_id FROM `za_usergamelogs` where user_id=? and gid=? " +
            "and room_type=? ORDER BY id DESC LIMIT 0,20";
        return TimeUtil.transTimestamp(DBUtil.getObjectListBySQL(sql,new Object[]{userId, gameId, roomType}),"createtime","yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public JSONObject getSysBaseSet() {
        String sql="SELECT appShareFrequency,appShareCircle,appShareFriend,cardname,coinsname,yuanbaoname,appShareObj FROM sys_base_set WHERE id=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{});
    }

    @Override
    public JSONObject getAPPGameSetting() {
        String sql="SELECT isXipai,xipaiLayer,xipaiCount,xipaiObj,bangData FROM app_game_setting WHERE id=1";
        return DBUtil.getObjectBySQL(sql, new Object[]{});
    }

    @Override
    public JSONArray getAppObjRec(Long userId, int doType, String gid, String roomid, String roomNo) {
        String sql="SELECT id FROM za_userdeduction WHERE userid=? AND gid=? AND roomid=? AND roomNo=? AND doType=?";
        return DBUtil.getObjectListBySQL(sql, new Object[]{userId,gid,roomid,roomNo,doType});
    }

    @Override
    public void addAppObjRec(JSONObject object) {
        String sql = "insert into za_userdeduction(userid,gid,roomNo,doType,roomid,creataTime,pocketNew,pocketOld,pocketChange,operatorType," +
            "memo,sum,platform,type) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        DBUtil.executeUpdateBySQL(sql,new Object[]{object.getLong("userId"),object.getInt("gameId"),object.getString("room_no"),
            1,object.getLong("room_id"),new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()),object.getDouble("new"),
            object.getDouble("old"),object.getDouble("change"),CommonConstant.SCORE_CHANGE_TYPE_SHUFFLE,"洗牌",
            object.getDouble("change"),object.getString("platform"),object.getInt("type")});
    }

    @Override
    public void updateUserPump(String account, String type, double sum) {
        StringBuffer sb = new StringBuffer();
        sb.append("update za_users set pumpVal=pumpVal+1,");
        sb.append(type);
        sb.append("=");
        sb.append(type);
        sb.append("+? where account=?");
        DBUtil.executeUpdateBySQL(String.valueOf(sb),new Object[]{sum,account});
    }

    /**
     * 更新战绩（房卡场解散）
     *
     * @param roomNo
     * @param gid
     * @param jsonArray
     * @return void
     * @throws
     * @date 2018年2月10日
     */
    @Override
    public void updateGamelogs(String roomNo, int gid, JSONArray jsonArray) {

        String sql = "select id from za_gamelogs where room_no=? and gid=? ORDER BY game_index DESC limit 1";
        JSONObject objectBySQL = DBUtil.getObjectBySQL(sql, new Object[]{roomNo,gid});
        if (!Dto.isObjNull(objectBySQL)) {
            sql = "update za_gamelogs set jiesuan=? where id=?";
            DBUtil.executeUpdateBySQL(sql, new Object[]{jsonArray.toString(),objectBySQL.getLong("id")});
        }
    }

    /**
     * 获取玩家战绩
     *
     * @param account
     * @param gid
     * @param num
     * @param createTime
     * @return JSONArray
     * @throws
     * @date 2018年3月19日
     */
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

    /**
     * 代开房间重开房间
     * @param roomNo
     * @return
     */
    @Override
    public boolean reDaikaiGameRoom(String roomNo) {

        try {
            String sql="select server_id,game_id,base_info,ip,port,game_count,paytype,stoptime,fangzhu from za_gamerooms where room_no=? order by id desc";
            JSONObject roomInfo = DBUtil.getObjectBySQL(sql, new Object[]{roomNo});
            if(roomInfo.containsKey("stoptime") && !Dto.stringIsNULL(roomInfo.getString("stoptime"))
                && roomInfo.getInt("roomtype")==2 && roomInfo.getInt("status")<0 ){

                TimeUtil.transTimeStamp(roomInfo, "yyyy-MM-dd HH:mm:ss", "stoptime");
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
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public JSONArray getRoomSetting(int gid, String platform, int flag) {
        String sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open" +
            " from za_gamesetting where is_use=? and is_open=0 and game_id=? and memo=? order by sort";
        JSONArray gameSetting = DBUtil.getObjectListBySQL(sql, new Object[]{flag, gid, platform});
        return gameSetting;
    }

    @Override
    public JSONObject getNoticeByPlatform(String platform){
        String sql="SELECT id,title,con,image,strcreateTime,endcreateTime,showType,createTime FROM za_message WHERE " +
            "`status`=1 AND type=2 AND platform=? ORDER BY createTime DESC LIMIT 1";
        return DBUtil.getObjectBySQL(sql,new Object[]{platform});
    }

    @Override
    public JSONArray getGoldSetting(JSONObject obj){
        String sql = "SELECT di,`option`,online,memo FROM za_gamegoldsetting WHERE game_id=? AND platform=?";
        return DBUtil.getObjectListBySQL(sql,new Object[]{obj.getInt("gameId"),obj.getString("platform")});
    }

    @Override
    public JSONObject getUserSignInfo(String platform, long userId) {
        String sql = "SELECT id,userID,singnum,createtime,platform from sys_sign where userID=? AND platform=?";
        JSONObject signInfo = DBUtil.getObjectBySQL(sql,new Object[]{userId,platform});
        if (Dto.isObjNull(signInfo)) {
            return signInfo;
        }
        return TimeUtil.transTimeStamp(signInfo, "yyyy-MM-dd HH:mm:ss", "createtime");
    }

    @Override
    public int addOrUpdateUserSign(JSONObject obj) {
        return DBJsonUtil.saveOrUpdate(obj,"sys_sign");
    }

    @Override
    public JSONObject getSignRewardInfoByPlatform(String platform) {
        String sql = "SELECT signin_base,signin_min,signin_max from operator_appsetting where platform=?";
        return DBUtil.getObjectBySQL(sql,new Object[]{platform});
    }

    @Override
    public void updateRobotStatus(String robotAccount,int status) {
        String sql = "update za_users set status=? where account=?";
        DBUtil.executeUpdateBySQL(sql,new Object[]{status,robotAccount});
    }

    @Override
    public JSONArray getArenaInfo() {
        String sql="select endTime,startTime,day,hour,isOpen,description,explanation,status,name,id,gid,memo from za_arena where status=1";
        return DBUtil.getObjectListBySQL(sql, new Object[]{});
    }

    @Override
    public JSONObject getUserCoinsRecById(long userId, int type, String startTime, String endTime) {
        String sql = "SELECT id,win,lose,coins FROM za_coins_rec WHERE user_id=? AND type=? AND createTime>=? AND createTime<=?";
        return DBUtil.getObjectBySQL(sql,new Object[]{userId,type,startTime,endTime});
    }

    @Override
    public int addOrUpdateUserCoinsRec(JSONObject obj) {
        return DBJsonUtil.saveOrUpdate(obj,"za_coins_rec");
    }

    @Override
    public JSONObject getUserGameInfo(String account) {
        String sql = "SELECT id,account,update_time,treasure_history,shuffle_times FROM za_user_game_info WHERE account=?";
        return DBUtil.getObjectBySQL(sql,new Object[]{account});
    }

    @Override
    public void addOrUpdateUserGameInfo(JSONObject obj) {
        DBJsonUtil.saveOrUpdate(obj,"za_user_game_info");
    }

    @Override
    public void addUserTicketRec(JSONObject ticketRec) {
        DBJsonUtil.add(ticketRec,"za_user_ticket_rec");
    }

    @Override
    public void addUserWelfareRec(JSONObject obj) {
        DBJsonUtil.add(obj,"za_userdeduction");
    }

    @Override
    public JSONArray getUserGameRoomByRoomType(long userId, int gameId, int roomType) {
        String sql = "SELECT id,room_no,game_count,createtime FROM za_gamerooms where game_id=? and roomtype=? and " +
            "(user_id0=? or user_id1=? or user_id2=? or user_id3=? or user_id4=? or user_id5=? or user_id6=? or user_id7=? or user_id8=? or user_id9=?) " +
            "order by id desc LIMIT 20";
        JSONArray roomList = DBUtil.getObjectListBySQL(sql, new Object[]{gameId,roomType,userId,userId,userId,userId,userId,userId,userId,userId,userId,userId});
        if (!Dto.isNull(roomList)) {
            return TimeUtil.transTimestamp(roomList,"createtime","yyyy-MM-dd HH:mm:ss");
        }
        return roomList;
    }

    @Override
    public JSONArray getUserGameLogsByUserId(long userId, int gameId, int roomType, List<String> roomList, String clubCode) {
        String sql = "SELECT id,room_no,result,gamelog_id,game_index FROM `za_usergamelogs` where user_id=? and gid=? " +
            "and room_type=?";
        if (roomList.size() > 0) {
            sql += " and room_no in(";
            for (int i = 0; i < roomList.size(); i++) {
                sql += roomList.get(i);
                if (i < roomList.size() - 1) {
                    sql += ",";
                }
            }
            sql += ")";
        }
        if (!Dto.stringIsNULL(clubCode)) {
            sql += " and club_code='" + clubCode + "'";
        }
        sql += " ORDER BY id DESC";
        return DBUtil.getObjectListBySQL(sql,new Object[]{userId, gameId, roomType});
    }
}

