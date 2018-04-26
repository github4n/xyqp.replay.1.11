package com.zhuoan.biz.model.zjh;

import com.zhuoan.biz.core.zjh.ZhaJinHuaCore;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.ZJHConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 10:19 2018/4/25
 * @Modified By:
 **/
@Component
public class ZJHGameRoomNew extends GameRoom{

    private int xzTimer;
    private JSONArray baseNum;
    private int[] pai;
    private int paiIndex;
    private double currentScore;
    private double totalScore;
    private double maxScore;
    private String focus;
    private int gameNum;
    private int totalGameNum;
    private Map<String, UserPacket> userPacketMap = new ConcurrentHashMap<String, UserPacket>();
    private List<Integer> yiXiaZhu = new ArrayList<Integer>();
    private JSONArray xiaZhuList = new JSONArray();
    private int gameType;

    public int getGameType() {
        return gameType;
    }

    public void setGameType(int gameType) {
        this.gameType = gameType;
    }

    public int getXzTimer() {
        return xzTimer;
    }

    public void setXzTimer(int xzTimer) {
        this.xzTimer = xzTimer;
    }

    public JSONArray getBaseNum() {
        return baseNum;
    }

    public void setBaseNum(JSONArray baseNum) {
        this.baseNum = baseNum;
    }

    public int[] getPai() {
        return pai;
    }

    public void setPai(int[] pai) {
        this.pai = pai;
    }

    public int getPaiIndex() {
        return paiIndex;
    }

    public void setPaiIndex(int paiIndex) {
        this.paiIndex = paiIndex;
    }

    public double getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(double currentScore) {
        this.currentScore = currentScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public String getFocus() {
        return focus;
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public int getGameNum() {
        return gameNum;
    }

    public void setGameNum(int gameNum) {
        this.gameNum = gameNum;
    }

    public int getTotalGameNum() {
        return totalGameNum;
    }

    public void setTotalGameNum(int totalGameNum) {
        this.totalGameNum = totalGameNum;
    }

    public Map<String, UserPacket> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(Map<String, UserPacket> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public List<Integer> getYiXiaZhu() {
        return yiXiaZhu;
    }

    public void setYiXiaZhu(List<Integer> yiXiaZhu) {
        this.yiXiaZhu = yiXiaZhu;
    }

    public JSONArray getXiaZhuList() {
        return xiaZhuList;
    }

    public void setXiaZhuList(JSONArray xiaZhuList) {
        this.xiaZhuList = xiaZhuList;
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
                obj.put("userStatus", userPacketMap.get(uuid).getStatus());
                array.add(obj);
            }
        }
        return array;
    }

    public void initGame() {
        // 当局总分
        totalScore = 0;
        paiIndex = 0;
        // 当前分数
        currentScore = getScore();
        // 轮数
        gameNum = 1;
        // 初始化下注时间
        setTimeLeft(ZJHConstant.ZJH_TIMER_INIT);
        yiXiaZhu.clear();
        xiaZhuList.clear();
        for (String account : getUserPacketMap().keySet()) {
            getUserPacketMap().get(account).initUserPacket();
        }
    }

    /**
     * 洗牌
     * @return
     */
    public void xiPai(){

        int[] pais = ZhaJinHuaCore.PAIS;
        //玩法（0：普通模式  1：必闷三圈 2：激情模式）
        if(getGameType()== ZJHConstant.ZJH_GAME_TYPE_HIGH){
            pais = ZhaJinHuaCore.PAIS_JQ;
        }
        int[] indexs = randomPai(pais.length);
        pai = new int[pais.length];
        for (int i = 0; i < indexs.length; i++) {
            pai[i] = pais[indexs[i]];
        }
    }

    /**
     * 打乱牌的下标
     * @param paiCount 牌数量
     * @return
     */
    private int[] randomPai(int paiCount){

        int[] nums = new int[paiCount];
        for (int i = 0; i < nums.length; i++) {
            while(true){
                int num = RandomUtils.nextInt(paiCount);
                if(!ArrayUtils.contains(nums,num)){
                    nums[i] = num;
                    break;
                }else if(num==0){
                    if(ArrayUtils.indexOf(nums, num) == i){
                        break;
                    }
                }
            }
        }
        return nums;
    }

    /**
     * 发牌
     * @return
     */
    public void faPai(){
        for(String account : getUserPacketMap().keySet()){
            Integer[] myPai = new Integer[3];
            for (int i = 0; i < 3; i++) {
                myPai[i] = pai[paiIndex];
                paiIndex = paiIndex + 1;
            }
            getUserPacketMap().get(account).setPai(myPai);
            getUserPacketMap().get(account).setType(ZhaJinHuaCore.getPaiType(Arrays.asList(myPai)));
            getUserPacketMap().get(account).setStatus(ZJHConstant.ZJH_USER_STATUS_AP);
        }
    }

    /**
     * 根据玩家账号获取下标
     * @param account
     * @return
     */
    public int getPlayerIndex(String account) {
        if (getPlayerMap().containsKey(account)&&getPlayerMap().get(account)!=null) {
            return getPlayerMap().get(account).getMyIndex();
        }
        return -1;
    }

    /**
     * 添加下注记录
     * @param myIndex
     * @param score
     */
    public void addXiazhuList(int myIndex, double score){
        JSONObject obj = new JSONObject();
        obj.put("index",myIndex);
        obj.put("score",score);
        getXiaZhuList().add(obj);
    }

    /**
     * 添加下注记录
     * @param score
     */
    public void addScoreChange(String account, double score){
        // 增加总下注数
        totalScore = Dto.add(score,totalScore);
        // 增加用户下注筹码
        double oldScore = getUserPacketMap().get(account).getScore();
        getUserPacketMap().get(account).setScore(Dto.add(oldScore,score));
        // 更新实体类数据
        Playerinfo playerinfo = getPlayerMap().get(account);
        getPlayerMap().get(account).setScore(Dto.sub(playerinfo.getScore(), score));
        // 负数清零
        if (getPlayerMap().get(account).getScore() < 0) {
            getPlayerMap().get(account).setScore(0);
        }
    }

    /**
     * 获取还未比牌的玩家下标
     * @return
     */
    public Integer[] getProgressIndex() {

        List<Integer> indexList = new ArrayList<Integer>();
        for (String uuid :userPacketMap.keySet()) {
            int status = getUserPacketMap().get(uuid).getStatus();
            // 暗牌或是明牌
            if(status==ZJHConstant.ZJH_USER_STATUS_AP || status==ZJHConstant.ZJH_USER_STATUS_KP){
                indexList.add(getPlayerMap().get(uuid).getMyIndex());
            }
        }
        return indexList.toArray(new Integer[indexList.size()]);
    }

    /**
     * 添加已下注用户
     * @param myIndex
     */
    public void addXzPlayer(int myIndex){
        getYiXiaZhu().add(myIndex);
        // 更新下注轮数
        if(getYiXiaZhu().size()==getProgressIndex().length){
            getYiXiaZhu().clear();
            setGameNum(getGameNum()+1);
        }
    }

    public String getNextPlayer(String account){

        if(getPlayerMap().get(account)!=null){

            int playerCount = 7;
            int index=getPlayerMap().get(account).getMyIndex();
            int next=index+1;
            Playerinfo player = null;
            while (player==null&&index!=next) {
                if(next>=playerCount){
                    next=0;
                }
                for (String uuid : getPlayerMap().keySet()) {
                    if(next==getPlayerMap().get(uuid).getMyIndex()){
                        return uuid;
                    }
                }
                next++;
            }
        }
        return getBanker();
    }

    /**
     * 获取下个可操作的玩家
     * @param uuid
     * @return
     */
    public String getNextOperationPlayer(String uuid){

        uuid = getNextPlayer(uuid);
        int count = getUserPacketMap().size();
        // 若玩家已经开完牌，则换下一个
        while(getUserPacketMap().get(uuid).getStatus()!=ZJHConstant.ZJH_USER_STATUS_AP
            && getUserPacketMap().get(uuid).getStatus()!=ZJHConstant.ZJH_USER_STATUS_KP){

            uuid = getNextPlayer(uuid);
            count--;
            if(count<=1){
                break;
            }
        }
        setFocus(uuid);
        return uuid;
    }

    public boolean isAllReady() {
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()!=ZJHConstant.ZJH_USER_STATUS_READY) {
                return false;
            }
        }
        return true;
    }

    public int getNowReadyCount(){
        int readyCount = 0;
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()==ZJHConstant.ZJH_USER_STATUS_READY) {
                readyCount ++;
            }
        }
        return readyCount;
    }

    public JSONArray getPlayerScore(){
        JSONArray array = new JSONArray();
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()>ZJHConstant.ZJH_USER_STATUS_INIT) {
                JSONObject obj = new JSONObject();
                obj.put("index",getPlayerIndex(account));
                obj.put("myScore",getUserPacketMap().get(account).getScore());
                array.add(obj);
            }
        }
        return array;
    }
}
