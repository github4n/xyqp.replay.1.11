package com.zhuoan.biz.model.bdx;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 14:58 2018/4/26
 * @Modified By:
 **/
public class BDXGameRoomNew extends GameRoom{
    private ConcurrentHashMap<String,UserPackerBDX> userPacketMap = new ConcurrentHashMap<String, UserPackerBDX>();

    public ConcurrentHashMap<String, UserPackerBDX> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, UserPackerBDX> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public void initGame(){

    }

    public JSONArray getAllPlayer(){
        JSONArray array = new JSONArray();

        for(String uuid : getPlayerMap().keySet()){

            Playerinfo player = getPlayerMap().get(uuid);
            if(player!=null){
                UserPackerBDX up = userPacketMap.get(uuid);
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                obj.put("score", player.getScore());
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", up.getStatus());
                obj.put("value", up.getValue());
                obj.put("sum", up.getScore());
                array.add(obj);
            }
        }
        return array;
    }
}
