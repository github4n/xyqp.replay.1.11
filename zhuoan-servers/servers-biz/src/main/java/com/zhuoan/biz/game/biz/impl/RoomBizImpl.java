package com.zhuoan.biz.game.biz.impl;

import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.dao.GameDao;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author huaping.li
 * @Description:
 * @date 2018-04-18 11:04
 */
@Service
public class RoomBizImpl implements RoomBiz{

    /**
     * 游戏数据操作接口
     */
    @Resource
    private GameDao gameDao;


    /**
     * 根据游戏ID获取游戏信息
     *
     * @param id 游戏ID
     * @return
     */
    @Override
    public JSONObject getGameInfoByID(long id) {

        return gameDao.getGameInfoByID(id);
    }

    /**
     * 根据房间号获取房间信息
     *
     * @param roomNo
     * @return
     */
    @Override
    public JSONObject getRoomInfoByRno(String roomNo) {

        return gameDao.getRoomInfoByRno(roomNo);
    }

    /**
     * 根据房间号解散房间
     *
     * @param roomNo
     * @return
     */
    @Override
    public boolean jieSanGameRoom(String roomNo) {

        JSONObject roomInfo = gameDao.getRoomInfoByRno(roomNo);
        JSONObject room = new JSONObject();
        room.put("id", roomInfo.getLong("id"));
        room.put("status", -1);
        int i = gameDao.updateRoomInfoByRid(room);
        if(i>0){
            return true;
        }
        return false;
    }

    /**
     * 根据房间号关闭房间
     *
     * @param roomNo
     * @return
     */
    @Override
    public boolean closeGameRoom(String roomNo) {

        JSONObject roomInfo = gameDao.getRoomInfoByRno(roomNo);
        JSONObject room = new JSONObject();
        room.put("id", roomInfo.getLong("id"));
        room.put("status", -2);
        int i = gameDao.updateRoomInfoByRid(room);
        if(i>0){
            return true;
        }
        return false;
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

        return gameDao.getRoomInfoSeting(gameID, optkey);
    }


    /**
     * 游戏开始后，禁止玩家进入房间
     *
     * @param roomNo
     * @return
     */
    @Override
    public boolean stopJoin(String roomNo) {

        return gameDao.updateGameRoomUserId(roomNo);
    }

    /**
     * 根据房间号获取无用的房间信息（结束或解散 status<0）
     *
     * @param roomNo
     * @return
     */
    @Override
    public JSONObject getRoomInfoByRnoNotUse(String roomNo) {

        return gameDao.getRoomInfoByRnoNotUse(roomNo);
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

        return gameDao.delGameRoomUserByUid(room, userId);
    }

    /**
     * 获取游戏设置
     *
     * @return
     */
    @Override
    public JSONObject getGameSetting() {

        return gameDao.getGameSetting();
    }

    /**
     * 获取机器人列表
     *
     * @param count
     * @return
     */
    @Override
    public List<String> getRobotList(int count) {

        return gameDao.getRobotList(count);
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

        return gameDao.pump(userIds, roomNo, gid, fee, type);
    }

    /**
     * 扣房卡
     *
     * @param roomNo@return void
     * @throws
     * @date 2018年2月7日
     */
    @Override
    public void settlementRoomNo(String roomNo) {


        JSONObject roomInfo = getRoomInfoByRno(roomNo);

        //判断是否是房卡模式roomtype：  0房卡，1金币
        if(!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype")==0){

            if(roomInfo.getInt("game_index")==1){ // 第一局结束需要扣除房卡

                // AA制
                if(roomInfo.containsKey("paytype") && roomInfo.getInt("paytype")==1){

                    // 存放所有参与玩家
                    List<Long> idList = new ArrayList<Long>();
                    int count = 10;
                    for (int i=0; i<count; i++){
                        String uIndex = "user_id"+i;
                        if(roomInfo.getLong(uIndex)>0){
                            idList.add(roomInfo.getLong(uIndex));
                        }
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
                    gameDao.updateUserRoomCard( - roomcard, roomInfo.getLong("user_id0"));

                    //保存扣房卡记录
                    gameDao.deductionRoomCardLog(new Object[]{roomInfo.getLong("user_id0"),roomInfo.getInt("id"),roomInfo.getInt("game_id"),
                        roomNo,0,roomcard, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())});
                }
            }

        }else if(!Dto.isObjNull(roomInfo) && roomInfo.getInt("roomtype")==2){

            if(roomInfo.getInt("game_count")<=roomInfo.getInt("game_index")){

                //进入代开房间重开方法
                gameDao.reDaikaiGameRoom(roomNo);
            }
        }
    }
}
