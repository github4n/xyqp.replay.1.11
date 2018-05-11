package com.zhuoan.biz.model.nn;

import com.zhuoan.biz.core.nn.Packer;
import com.zhuoan.biz.core.nn.UserPacket;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.NNConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 8:50 2018/4/17
 * @Modified By:
 **/
public class NNGameRoomNew extends GameRoom{
    /**
     * 定庄方式(霸王庄，轮庄，抢庄，明牌抢庄)
     */
    private int bankerType;
    /**
     * 倍率
     */
    public Map<Integer, Integer> ratio = initRatio();
    /**
     * 游戏筹码
     */
    private String baseNum;
    /**
     * 抢庄倍数
     */
    public JSONArray qzTimes = new JSONArray();
    /**
     * 配置特殊牌型（五花牛、葫芦牛、炸弹）
     */
    private List<Integer> specialType = new ArrayList<Integer>();
    /**
     * 无人抢庄(0:解散房间 1:随机庄家 2:重新开局)
     */
    private int qzNoBanker;
    /**
     * 随机庄家
     */
    private int sjBanker;
    /**
     * 玩家牌局信息
     */
    private ConcurrentMap<String,UserPacket> userPacketMap = new ConcurrentHashMap<String, UserPacket>();
    /**
     * 牌
     */
    private Packer[] pai;
    /**
     * 庄家通杀
     */
    private int tongSha = 0;

    public int getTongSha() {
        return tongSha;
    }

    public void setTongSha(int tongSha) {
        this.tongSha = tongSha;
    }

    public Packer[] getPai() {
        return pai;
    }

    public void setPai(Packer[] pai) {
        this.pai = pai;
    }

    public int getSjBanker() {
        return sjBanker;
    }

    public void setSjBanker(int sjBanker) {
        this.sjBanker = sjBanker;
    }

    public int getBankerType() {
        return bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public Map<Integer, Integer> getRatio() {
        return ratio;
    }

    public void setRatio(Map<Integer, Integer> ratio) {
        this.ratio = ratio;
    }

    public String getBaseNum() {
        return baseNum;
    }

    public void setBaseNum(String baseNum) {
        this.baseNum = baseNum;
    }

    public void setQzTimes(JSONArray qzTimes) {
        this.qzTimes = qzTimes;
    }

    public List<Integer> getSpecialType() {
        return specialType;
    }

    public void setSpecialType(List<Integer> specialType) {
        this.specialType = specialType;
    }

    public int getQzNoBanker() {
        return qzNoBanker;
    }

    public void setQzNoBanker(int qzNoBanker) {
        this.qzNoBanker = qzNoBanker;
    }

    public ConcurrentMap<String, UserPacket> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentMap<String, UserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    /**
     * 初始化倍率信息
     * @return
     */
    private Map<Integer, Integer> initRatio() {
        Map<Integer, Integer> ratio = new HashMap<Integer, Integer>();
        ratio.put(0, 1);
        ratio.put(1, 1);
        ratio.put(2, 1);
        ratio.put(3, 1);
        ratio.put(4, 1);
        ratio.put(5, 1);
        ratio.put(6, 1);
        ratio.put(7, 2);
        ratio.put(8, 2);
        ratio.put(9, 3);
        ratio.put(10, 4);
        return ratio;
    }

    /**
     * 初始化房间
     */
    public void initGame(){
        getGameProcess().clear();
        tongSha = 0;
        // 重置玩家信息
        for (String uuid : getUserPacketMap().keySet()) {
            if(userPacketMap.containsKey(uuid)){
                userPacketMap.get(uuid).initUserPacket();
            }
        }
        setGameIndex(getGameIndex()+1);
        // 霸王庄庄家退出重置庄家
        if (bankerType==NNConstant.NN_BANKER_TYPE_FZ) {
            if (!getUserPacketMap().containsKey(getBanker())||getUserPacketMap().get(getBanker())==null) {
                for (String account : getUserPacketMap().keySet()) {
                    setBanker(account);
                    break;
                }
            }
        }
        getGameProcess().put("bankerType",bankerType);
    }

    /**
     * 获取当前房间内的所有玩家
     * @return
     */
    public JSONArray getAllPlayer(){
        JSONArray array = new JSONArray();

        for(String uuid : getPlayerMap().keySet()){

            Playerinfo player = getPlayerMap().get(uuid);
            if(player!=null){
                UserPacket up = userPacketMap.get(uuid);
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
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 检查是否全部完成准备
     * @return
     */
    public boolean isAllReady(){
        for (String account : userPacketMap.keySet()){
            if (userPacketMap.get(account).getStatus()!= NNConstant.NN_USER_STATUS_READY) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否全部完成抢庄
     * @return
     */
    public boolean isAllQZ(){
        for (String account : userPacketMap.keySet()){
            if (userPacketMap.get(account).getStatus()!=NNConstant.NN_USER_STATUS_INIT&&
                userPacketMap.get(account).getStatus()!= NNConstant.NN_USER_STATUS_QZ) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检查是否全部完成下注
     * @return
     */
    public boolean isAllXiaZhu(){
        for (String account : userPacketMap.keySet()){
            if (!account.equals(getBanker())) {
                if (userPacketMap.get(account).getStatus()!=NNConstant.NN_USER_STATUS_INIT&&
                    userPacketMap.get(account).getStatus()!= NNConstant.NN_USER_STATUS_XZ) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查是否全部完成亮牌
     * @return
     */
    public boolean isAllShowPai(){
        for (String account : userPacketMap.keySet()){
            if (userPacketMap.get(account).getStatus()!=NNConstant.NN_USER_STATUS_INIT&&
                userPacketMap.get(account).getStatus()!= NNConstant.NN_USER_STATUS_LP) {
                return false;
            }
        }
        return true;
    }

    /**
     * 是否全部同意解散
     * @return
     */
    public boolean isAgreeClose(){
        for (String account : userPacketMap.keySet()){
            if (userPacketMap.get(account).getIsCloseRoom()!= CommonConstant.CLOSE_ROOM_AGREE) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取可选抢庄倍数
     * @param yuanbao
     * @return
     */
    public JSONArray getQzTimes(double yuanbao){
        // 基数
        int baseNum = 3;
        // 玩家人数
        int playerCount = getUserPacketMap().size();
        // 最大下注倍数
        int maxVal = 0;
        JSONArray array = JSONArray.fromObject(getBaseNum());
        for (int i = 0; i < array.size(); i++) {
            int val = array.getJSONObject(i).getInt("val");
            if(val>maxVal){
                maxVal = val;
            }
        }
        // 抢庄最大倍数
        int beiShu = (int) (yuanbao/(baseNum*getScore()*(playerCount-1)*maxVal));
        JSONArray qzts = new JSONArray();
        for (int i = 0; i < qzTimes.size(); i++) {
            JSONObject obj = new JSONObject();
            int val = qzTimes.getInt(i);
            obj.put("name", new StringBuffer().append(String.valueOf(val)).append("倍").toString());
            obj.put("val", val);
            if(beiShu>=val||getRoomType()==CommonConstant.ROOM_TYPE_FK){
                obj.put("isuse",CommonConstant.GLOBAL_YES);
            }else{
                obj.put("isuse", CommonConstant.GLOBAL_NO);
            }
            qzts.add(obj);
        }
        return qzts;
    }

    /**
     * 获取可选玩家下注倍数
     * @param yuanbao
     * @return
     */
    public JSONArray getBaseNumTimes(double yuanbao){
        // 基数
        int baseNum = 5;
        // 底注
        double di = getScore();
        // 庄家抢庄倍数
        int qzTimes = 1;
        // 抢庄
        if(bankerType==NNConstant.NN_BANKER_TYPE_QZ || bankerType==NNConstant.NN_BANKER_TYPE_MP) {
            if (!Dto.isNull(userPacketMap)&&userPacketMap.get(getBanker())!=null) {
                qzTimes = userPacketMap.get(getBanker()).getQzTimes();
            }
        }
        // 最大下注倍数
        int beiShu = (int) (yuanbao/(baseNum*di*qzTimes));
        JSONArray baseNums = new JSONArray();
        JSONArray array = JSONArray.fromObject(getBaseNum());
        for (int i = 0; i < array.size(); i++) {
            int val = array.getJSONObject(i).getInt("val");
            JSONObject obj = new JSONObject();
            obj.put("name", new StringBuffer().append(String.valueOf(val)).append("倍").toString());
            obj.put("val", val);
            if(beiShu>=val||getRoomType()==CommonConstant.ROOM_TYPE_FK){
                obj.put("isuse", CommonConstant.GLOBAL_YES);
            }else{
                obj.put("isuse", CommonConstant.GLOBAL_NO);
            }
            baseNums.add(obj);
        }

        return JSONArray.fromObject(baseNums);
    }

    /**
     * 获取抢庄结果
     * @return
     */
    public JSONArray getQZResult(){
        JSONArray array = new JSONArray();
        for (String uuid :userPacketMap.keySet()) {
            UserPacket up = userPacketMap.get(uuid);
            if(up.getStatus()==NNConstant.NN_USER_STATUS_QZ){
                JSONObject obj = new JSONObject();
                obj.put("index", getPlayerMap().get(uuid).getMyIndex());
                obj.put("value", up.getQzTimes());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 获取下注结果
     * @return
     */
    public JSONArray getXiaZhuResult(){
        JSONArray array = new JSONArray();
        for (String uuid :userPacketMap.keySet()) {
            UserPacket up = userPacketMap.get(uuid);
            if(up.getStatus()==NNConstant.NN_USER_STATUS_XZ){
                JSONObject obj = new JSONObject();
                obj.put("index", getPlayerMap().get(uuid).getMyIndex());
                obj.put("value", up.getXzTimes());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 获取gameData
     * @param account
     * @return
     */
    public JSONObject getGameData(String account){
        // 抢庄下注阶段
        if (getGameStatus()==NNConstant.NN_GAME_STATUS_QZ||getGameStatus()==NNConstant.NN_GAME_STATUS_DZ||getGameStatus()==NNConstant.NN_GAME_STATUS_XZ) {
            return getGameDataQzOrXz(account);
        } else if (getGameStatus()==NNConstant.NN_GAME_STATUS_LP) {
            return getGameDataLP(account);
        } else if (getGameStatus()==NNConstant.NN_GAME_STATUS_JS) {
            return getGameDataJS(account);
        }
        return null;
    }

    /**
     * 抢庄、下注阶段进入
     * @param account
     * @return
     */
    public JSONObject getGameDataQzOrXz(String account){
        JSONObject data = new JSONObject();
        for (String uuid : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            int[] pai;
            // 明牌抢庄提前发牌
            if (bankerType==NNConstant.NN_BANKER_TYPE_MP){
                // 有参与的玩家
                if (userPacketMap.get(uuid).getStatus()>NNConstant.NN_USER_STATUS_INIT) {
                    // 自己的牌可以看到
                    if (uuid.equals(account)) {
                        int[] myPai = userPacketMap.get(uuid).getMingPai();
                        // 抢庄或定庄阶段四张牌
                        pai = new int[myPai.length-1];
                        for (int i = 0; i < pai.length; i++) {
                            pai[i] = myPai[i];
                        }
                        obj.put("paiType",userPacketMap.get(uuid).getType());
                    }else {// 其他人的牌传[0,0,0,0]
                        pai = new int[]{0,0,0,0};
                    }
                } else {// 中途加入玩家传[]
                    pai = new int[0];
                }
            }else {
                // 其余模式下注阶段之前都没有牌
                pai = new int[0];
            }
            obj.put("pai",pai);
            data.put(getPlayerMap().get(uuid).getMyIndex(),obj);
        }
        return data;
    }

    /**
     * 亮牌阶段加入
     * @param account
     * @return
     */
    public JSONObject getGameDataLP(String account){
        JSONObject data = new JSONObject();
        for (String uuid : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            int[] pai;
            // 有参与的玩家
            if (userPacketMap.get(uuid).getStatus()>NNConstant.NN_USER_STATUS_INIT) {
                // 自己或者已经亮牌的玩家
                if (uuid.equals(account)||userPacketMap.get(uuid).getStatus()==NNConstant.NN_USER_STATUS_LP) {
                    if (getBankerType()==NNConstant.NN_BANKER_TYPE_MP) {
                        pai = userPacketMap.get(uuid).getMingPai();
                    } else {
                        pai = userPacketMap.get(uuid).getMyPai();
                    }
                    // 已亮牌展示牌型
                    if (userPacketMap.get(uuid).getStatus()==NNConstant.NN_USER_STATUS_LP){
                        pai = userPacketMap.get(uuid).getSortPai();
                        obj.put("paiType",userPacketMap.get(uuid).getType());
                    }
                }else {
                    pai = new int[]{0,0,0,0,0};
                }
            }else {
                pai = new int[0];
            }
            obj.put("pai",pai);
            data.put(getPlayerMap().get(uuid).getMyIndex(),obj);
        }
        return data;
    }

    /**
     * 结算阶段加入
     * @param account
     * @return
     */
    public JSONObject getGameDataJS(String account){
        JSONObject data = new JSONObject();
        for (String uuid : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("tongsha",tongSha);
            int[] pai;
            if (userPacketMap.get(uuid).getStatus()>NNConstant.NN_USER_STATUS_INIT) {
                pai = userPacketMap.get(uuid).getSortPai();
                obj.put("paiType",userPacketMap.get(uuid).getType());
                obj.put("sum",userPacketMap.get(uuid).getScore());
                obj.put("scoreLeft",getPlayerMap().get(uuid).getScore());
            }else {
                pai = new int[0];
            }
            obj.put("pai",pai);
            data.put(getPlayerMap().get(uuid).getMyIndex(),obj);
        }
        return data;
    }

    /**
     * 获取总结算数据
     * @return
     */
    public JSONArray getFinalSummary() {
        JSONArray array = new JSONArray();
        for (String account : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("name",getPlayerMap().get(account).getName());
            obj.put("account",account);
            obj.put("headimg",getPlayerMap().get(account).getRealHeadimg());
            obj.put("score",getPlayerMap().get(account).getScore());
            obj.put("isFangzhu",CommonConstant.GLOBAL_NO);
            if (account.equals(getOwner())) {
                obj.put("isFangzhu",CommonConstant.GLOBAL_YES);
            }
            obj.put("isWinner",CommonConstant.GLOBAL_NO);
            if (getPlayerMap().get(account).getScore()>0) {
                obj.put("isWinner",CommonConstant.GLOBAL_YES);
            }
            obj.put("tongShaTimes",userPacketMap.get(account).getTongShaTimes());
            obj.put("tongPeiTimes",userPacketMap.get(account).getTongPeiTimes());
            obj.put("niuNiuTimes",userPacketMap.get(account).getNiuNiuTimes());
            obj.put("wuNiuTimes",userPacketMap.get(account).getWuNiuTimes());
            obj.put("winTimes",userPacketMap.get(account).getWinTimes());
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取解散数据
     * @return
     */
    public JSONArray getJieSanData(){
        JSONArray array = new JSONArray();
        for (String account : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("index",getPlayerMap().get(account).getMyIndex());
            obj.put("name",getPlayerMap().get(account).getName());
            obj.put("result",userPacketMap.get(account).getIsCloseRoom());
            obj.put("jiesanTimer",getJieSanTime());
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取实时准备人数
     * @return
     */
    public int getNowReadyCount(){
        int readyCount = 0;
        for (String uuid : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(uuid).getStatus()==NNConstant.NN_USER_STATUS_READY) {
                readyCount ++;
            }
        }
        return readyCount;
    }

    /**
     * 获取下个玩家
     * @param account
     * @return
     */
    public String getNextPlayer(String account){
        return account;
    }
}
