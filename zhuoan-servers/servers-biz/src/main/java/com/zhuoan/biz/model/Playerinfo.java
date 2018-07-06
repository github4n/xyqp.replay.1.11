package com.zhuoan.biz.model;


import com.zhuoan.constant.Constant;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;


public class Playerinfo implements Serializable{


    private static final long serialVersionUID = -3467565324346126435L;
    /**
     * uuid
     */
    private UUID uuid;
    /**
     * 用户的userId
     */
    private long id;
    /**
     * 帐号
     */
    private String account;
    /**
     * 昵称
     */
    private String name;
    /**
     * 头像
     */
    private String headimg;
    /**
     * 玩家ip
     */
    private String ip;
    /**
     * 玩家坐标
     */
    private String location;
    /**
     * 地区
     */
    private String area;
    /**
     * 性别
     */
    private String sex;
    /**
     * 状态
     */
    private int status;
    /**
     * 房卡模式：玩家积分 ； 金币模式：玩家金币
     */
    private double score;
    /**
     * 座位号
     */
    private int myIndex;
    /**
     * 个性签名
     */
    private String signature;
    /**
     * VIP等级
     */
    private int vip;
    /**
     * 幸运值
     */
    private int luck=-1;
    /**
     * 是否提示消息
     */
    private boolean isTipMsg = true;
    /**
     * 工会
     */
    private String ghName = "该玩家未加入工会";
    /**
     * 房卡数
     */
    private int roomCardNum = 0;
    /**
     * openId
     */
    private String openId;
    /**
     * 冻结资金id
     */
    private long assetsId;

    public int getRoomCardNum() {
        return roomCardNum;
    }

    public void setRoomCardNum(int roomCardNum) {
        this.roomCardNum = roomCardNum;
    }

    public String getGhName() {
        return ghName;
    }
    public void setGhName(String ghName) {
        this.ghName = ghName;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {

        if(location!=null&&location.indexOf("失败")==-1&&!location.equals("null")){
            this.location = location;
        }else{
            this.location = "0,0";
        }
    }
    public String getArea() {
        return area;
    }
    public void setArea(String area) {
        this.area = area;
    }
    public UUID getUuid() {
        return uuid;
    }
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getAccount() {
        return account;
    }
    public void setAccount(String account) {
        this.account = account;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getHeadimg() {
        return headimg;
    }
    public void setHeadimg(String headimg) {
        this.headimg = headimg;
    }
    public String getSex() {
        return sex;
    }
    public void setSex(String sex) {
        this.sex = sex;
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
        this.score = new BigDecimal(score)
            .setScale(2, BigDecimal.ROUND_HALF_UP)
            .doubleValue();
    }
    public int getMyIndex() {
        return myIndex;
    }
    public void setMyIndex(int myIndex) {
        this.myIndex = myIndex;
    }

    public String getSignature() {
        return signature;
    }
    public void setSignature(String signature) {
        this.signature = signature;
    }
    public int getVip() {
        return vip;
    }
    public void setVip(int vip) {
        this.vip = vip;
    }
    public int getLuck() {
        return luck;
    }
    public void setLuck(int luck) {
        this.luck = luck;
    }
    public boolean isTipMsg() {
        return isTipMsg;
    }
    public void setTipMsg(boolean isTipMsg) {
        this.isTipMsg = isTipMsg;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public long getAssetsId() {
        return assetsId;
    }

    public void setAssetsId(long assetsId) {
        this.assetsId = assetsId;
    }

    /**
     * 获取玩家真实头像
     * @return
     */
    public String getRealHeadimg(){

        return Constant.cfgProperties.getProperty("server_domain") + getHeadimg();
    }
}
