package com.zhuoan.biz.model.sss;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.io.Serializable;


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
    /**
     * 解散房间申请  0:未确认 1:同意  -1:拒绝
     */
    private int isCloseRoom=0;
    /**
     * 牌型
     */
    private int paiType;
    /**
     * 牌型分数
     */
    private int paiScore;
    /**
     * 头道输赢结果
     */
    private JSONObject headResult = new JSONObject();
    /**
     * 中道输赢结果
     */
    private JSONObject midResult = new JSONObject();
    /**
     * 尾道输赢结果
     */
    private JSONObject footResult = new JSONObject();
    /**
     * 头道手牌
     */
    private int[] headPai;
    /**
     * 中道手牌
     */
    private int[] midPai;
    /**
     * 尾道手牌
     */
    private int[] footPai;

    public static long getSerialVersionUID() {
        return serialVersionUID;
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
    public int getSwat() {
        return swat;
    }

    public void setSwat(int swat) {
        this.swat = swat;
    }

    public int getGameNum() {
        return gameNum;
    }

    public void setGameNum(int gameNum) {
        this.gameNum = gameNum;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getIsCloseRoom() {
        return isCloseRoom;
    }

    public void setIsCloseRoom(int isCloseRoom) {
        this.isCloseRoom = isCloseRoom;
    }

    public int getPaiType() {
        return paiType;
    }

    public void setPaiType(int paiType) {
        this.paiType = paiType;
    }

    public int getPaiScore() {
        return paiScore;
    }

    public void setPaiScore(int paiScore) {
        this.paiScore = paiScore;
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

    /**
     * 初始化
     */
    public void initUserPacket(){
        score = 0;
        paiScore = 0;
        paiType = 0;
        swat = 0;
        headResult.clear();
        midResult.clear();
        footResult.clear();
    }
}
