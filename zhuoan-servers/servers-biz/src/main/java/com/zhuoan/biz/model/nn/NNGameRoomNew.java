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
    public JSONArray qzTimes;
    /**
     * 配置特殊牌型（五花牛、葫芦牛、炸弹）
     */
    private List<Integer> specialType = new ArrayList<Integer>();
    /**
     * 是否允许中途加入（true：允许、false：不允许）
     */
    private boolean isHalfwayIn = false;
    /**
     * 准备超时（0：不处理 1：自动准备 2：踢出房间）
     */
    private int readyOvertime;
    /**
     * 无人抢庄(0:解散房间 1:随机庄家 2:重新开局)
     */
    private int qzNoBanker;
    /**
     * 是否加入机器人
     */
    private boolean robot;
    /**
     * 是否观战模式
     */
    private boolean visit;
    /**
     * 随机庄家
     */
    private int sjBanker;
    /**
     * 玩家个人信息
     */
    private ConcurrentMap<String,Playerinfo> playerMap = new ConcurrentHashMap<String, Playerinfo>();
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

    public boolean isHalfwayIn() {
        return isHalfwayIn;
    }

    public void setHalfwayIn(boolean halfwayIn) {
        isHalfwayIn = halfwayIn;
    }

    public int getReadyOvertime() {
        return readyOvertime;
    }

    public void setReadyOvertime(int readyOvertime) {
        this.readyOvertime = readyOvertime;
    }

    public int getQzNoBanker() {
        return qzNoBanker;
    }

    public void setQzNoBanker(int qzNoBanker) {
        this.qzNoBanker = qzNoBanker;
    }

    public boolean isRobot() {
        return robot;
    }

    public void setRobot(boolean robot) {
        this.robot = robot;
    }

    public boolean isVisit() {
        return visit;
    }

    public void setVisit(boolean visit) {
        this.visit = visit;
    }

    public ConcurrentMap<String, Playerinfo> getPlayerMap() {
        return playerMap;
    }

    public void setPlayerMap(ConcurrentMap<String, Playerinfo> playerMap) {
        this.playerMap = playerMap;
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
        getGameProcess().put("bankerType",bankerType);
    }

    /**
     * 获取当前房间内的所有玩家
     * @return
     */
    public JSONArray getAllPlayer(){
        JSONArray array = new JSONArray();

        for(String uuid : playerMap.keySet()){

            Playerinfo player = playerMap.get(uuid);
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
            if (userPacketMap.get(account).isCloseRoom!= CommonConstant.CLOSE_ROOM_AGREE) {
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
        int playerCount = playerMap.size();
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
            if(beiShu>=val){
                obj.put("isuse",1);
            }else{
                obj.put("isuse", 0);
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
            if(beiShu>=val){
                obj.put("isuse", 1);
            }else{
                obj.put("isuse", 0);
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
                obj.put("index", playerMap.get(uuid).getMyIndex());
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
                obj.put("index", playerMap.get(uuid).getMyIndex());
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
        //JSONArray array = new JSONArray();
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
            }else {// 其余模式下注阶段之前都没有牌
                pai = new int[0];
            }
            obj.put("pai",pai);
            data.put(playerMap.get(uuid).getMyIndex(),obj);
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
                    pai = userPacketMap.get(uuid).getMingPai();
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
            data.put(playerMap.get(uuid).getMyIndex(),obj);
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
                obj.put("scoreLeft",playerMap.get(uuid).getScore());
            }else {
                pai = new int[0];
            }
            obj.put("pai",pai);
            data.put(playerMap.get(uuid).getMyIndex(),obj);
        }
        return data;
    }

    /**
     * 获取解散数据
     * @return
     */
    public JSONArray getJieSanData(){
        JSONArray array = new JSONArray();
        for (String account : userPacketMap.keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("index",playerMap.get(account).getMyIndex());
            obj.put("name",playerMap.get(account).getName());
            obj.put("result",userPacketMap.get(account).isCloseRoom);
            obj.put("showTimer",1);
            obj.put("timer",getJieSanTime());
            array.add(obj);
        }
        return array;
    }

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
     * 获取当前房间内的所有人
     * @return
     */
    public List<UUID> getAllUUIDList(){
        List<UUID> uuidList = new ArrayList<UUID>();
        for (String account : playerMap.keySet()) {
            uuidList.add(playerMap.get(account).getUuid());
        }
        return uuidList;
    }

    /**
     * 获取当前房间内的所有人(不包括自己)
     * @param uuid
     * @return
     */
    public List<UUID> getAllUUIDList(String uuid){
        List<UUID> uuidList = new ArrayList<UUID>();
        for (String account : playerMap.keySet()) {
            if (!uuid.equals(account)) {
                uuidList.add(playerMap.get(account).getUuid());
            }
        }
        return uuidList;
    }

    /**
     * 获取更新数据类型
     * @return
     */
    public String getUpdateType(){
        switch (getRoomType()) {
            case CommonConstant.ROOM_TYPE_FK:
                return "roomcard";
            case CommonConstant.ROOM_TYPE_JB:
                return "coins";
            case CommonConstant.ROOM_TYPE_DK:
                return "roomcard";
            case CommonConstant.ROOM_TYPE_YB:
                return "yuanbao";
            default:
                return "";
        }
    }
}
