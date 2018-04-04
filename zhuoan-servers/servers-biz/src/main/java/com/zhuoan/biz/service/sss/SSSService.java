package com.zhuoan.biz.service.sss;

import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.model.Playerinfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.UUID;

/**
 * 十三水游戏业务接口
 */
public interface SSSService {

    /**
     * 创建房间
     *
     * @param roomObj the room obj
     * @param uuid    用户标识（临时）
     * @param objInfo 房间属性信息
     * @param player  玩家信息
     * @param room    the room
     * @return sss game room
     */
    public SSSGameRoom createGameRoom(JSONObject roomObj, UUID uuid, JSONObject objInfo, Playerinfo player, SSSGameRoom room);

    /**
     * 加入房间
     *
     * @param roomNo   房间号
     * @param uuid     用户标识（临时）
     * @param player   玩家信息
     * @param roomType the room type
     * @return boolean
     */
    public boolean joinGameRoom(String roomNo, UUID uuid, Playerinfo player, int roomType);

    /**
     * 庄闲比对
     *
     * @param roomNo the room no
     */
    public void jieSuan(String roomNo);

    /**
     * Pei pai.
     *
     * @param roomNo the room no
     * @param uc     the uc
     * @param type   the type
     * @param data   the data
     */
    public void peiPai(String roomNo, String uc, int type, JSONObject data);

    /**
     * Is ready.
     *
     * @param roomNo the room no
     * @param uc     the uc
     */
    public void isReady(String roomNo, String uc);

    /**
     * Ding zhuang.
     *
     * @param roomNo the room no
     * @param i      the
     */
    public void dingZhuang(String roomNo, int i);

    /**
     * Xi pai.
     *
     * @param roomNo the room no
     */
    public void xiPai(String roomNo);

    /**
     * Fa pai.
     *
     * @param roomNo the room no
     */
    public void faPai(String roomNo);

    /**
     * 两两比对
     *
     * @param roomNo the room no
     */
    public void jieSuan1(String roomNo);


    /**
     * Gamelog.
     *
     * @param room the room
     * @param us   the us
     * @param uid  the uid
     * @param e    the e
     */
    public void gamelog(SSSGameRoom room, JSONArray us, JSONArray uid, boolean e);
	

}
