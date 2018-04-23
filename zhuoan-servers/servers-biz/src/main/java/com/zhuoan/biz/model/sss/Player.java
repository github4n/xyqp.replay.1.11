package com.zhuoan.biz.model.sss;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 十三水玩家牌局信息
 * @author lhp
 *
 */
public class Player implements Serializable{


    private static final long serialVersionUID = -4837751151303196476L;
    /**
     * 牌
     */
    private String[] pai;
    /**
     * 玩家状态
     */
    private int status=0;
    /**
     * 普通牌型
     */
    private int ordinary;
    /**
     * 特殊牌型
     */
    private int special = 0;
    /**
     * 打枪
     */
    private int gun;
    /**
     * 全垒打
     */
    private int swat = 0;
    /**
     * 参与局数
     */
    private int gameNum = 0;
    /**
     * 单局分数
     */
    private double score;
    private double totalScore;//总分数
    private int isReady; // 准备状态
    private int isAuto;//是否自动配牌
    /**
     * 解散房间申请  0:未确认 1:同意  -1:拒绝
     */
    public int isCloseRoom=0;
    /**
     * 牌型
     */
    private int paiType;
    /**
     * 牌型分数
     */
    private int paiScore;
    private JSONObject headResult = new JSONObject();
    private JSONObject midResult = new JSONObject();
    private JSONObject footResult = new JSONObject();
    private int[] headPai;
    private int[] midPai;
    private int[] footPai;

    public int[] getHeadPai() {
        return headPai;
    }

    public void setHeadPai(int[] headPai) {
        this.headPai = headPai;
    }

    public int[] getMidPai() {
        return midPai;
    }

    public void setMidPai(int[] midPai) {
        this.midPai = midPai;
    }

    public int[] getFootPai() {
        return footPai;
    }

    public void setFootPai(int[] footPai) {
        this.footPai = footPai;
    }

    public JSONObject getHeadResult() {
        return headResult;
    }

    public void setHeadResult(JSONObject headResult) {
        this.headResult = headResult;
    }

    public JSONObject getMidResult() {
        return midResult;
    }

    public void setMidResult(JSONObject midResult) {
        this.midResult = midResult;
    }

    public JSONObject getFootResult() {
        return footResult;
    }

    public void setFootResult(JSONObject footResult) {
        this.footResult = footResult;
    }

    /**
     * 打枪下标
     */
    private List<Integer> allWinIndex = new ArrayList<Integer>();
    /**
     * 被打枪下标
     */
    private List<Integer> allLoseIndex = new ArrayList<Integer>();

    public List<Integer> getAllWinIndex() {
        return allWinIndex;
    }

    public void setAllWinIndex(List<Integer> allWinIndex) {
        this.allWinIndex = allWinIndex;
    }

    public List<Integer> getAllLoseIndex() {
        return allLoseIndex;
    }

    public void setAllLoseIndex(List<Integer> allLoseIndex) {
        this.allLoseIndex = allLoseIndex;
    }

    public int getPaiScore() {
        return paiScore;
    }
    public void setPaiScore(int paiScore) {
        this.paiScore = paiScore;
    }
    public int getPaiType() {
        return paiType;
    }
    public void setPaiType(int paiType) {
        this.paiType = paiType;
    }
    public String[] getPai() {
        return pai;
    }
    public void setPai(String[] pai) {
        this.pai = pai;
    }
    public int getStatus() {
        return status;
    }
    public void setStatus(int status) {
        this.status = status;
    }
    public double getScore() {
        return score;
    }
    public void setScore(double score) {
        this.score = score;
    }
    public double getTotalScore() {
        return totalScore;
    }
    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }
    public int getIsReady() {
        return isReady;
    }
    public void setIsReady(int isReady) {
        this.isReady = isReady;
    }

    /**
     * 获取玩家手牌
     * @return
     */
    public int[] getMyPai(){
        int p=0;
        if (pai!=null) {
            p=pai.length;
        }
        int[] pais = new int[p];
        for (int i = 0; i < pais.length; i++) {
            String[] val = pai[i].split("-");
            int num = 0;
            if(val[0].equals("2")){
                num = 20;
            }else if(val[0].equals("3")){
                num = 40;
            }else if(val[0].equals("4")){
                num = 60;
            }
            pais[i] = Integer.valueOf(val[1]) + num;
        }
        return pais;
    }

    /**
     * 获取玩家手牌
     * @return
     */
    public JSONArray togetMyPai(JSONArray p){

        JSONArray pais = new JSONArray();
        for (int i = 0; i < p.size(); i++) {

            if (p.getInt(i)<20) {
                String a="1-"+p.getString(i);
                pais.add(a);
            }else if(p.getInt(i)>20&&p.getInt(i)<40){
                String a="2-"+(p.getInt(i)-20);
                pais.add(a);
            }else if(p.getInt(i)>40&&p.getInt(i)<60){
                String a="3-"+(p.getInt(i)-40);
                pais.add(a);
            }else if(p.getInt(i)>60){
                String a="4-"+(p.getInt(i)-60);
                pais.add(a);
            }

        }
        return pais;
    }
    public int getOrdinary() {
        return ordinary;
    }
    public void setOrdinary(int ordinary) {
        this.ordinary = ordinary;
    }
    public int getSpecial() {
        return special;
    }
    public void setSpecial(int special) {
        this.special = special;
    }
    public int getGun() {
        return gun;
    }
    public void setGun(int gun) {
        this.gun = gun;
    }
    public int getSwat() {
        return swat;
    }
    public void setSwat(int swat) {
        this.swat = swat;
    }
    public int getIsAuto() {
        return isAuto;
    }
    public void setIsAuto(int isAuto) {
        this.isAuto = isAuto;
    }
    public int getGameNum() {
        return gameNum;
    }
    public void setGameNum(int gameNum) {
        this.gameNum = gameNum;
    }

    public void initUserPacket(){
        special = 0;
        score = 0;
        paiScore = 0;
        paiType = 0;
        swat = 0;
        headResult.clear();
        midResult.clear();
        footResult.clear();
    }
}
