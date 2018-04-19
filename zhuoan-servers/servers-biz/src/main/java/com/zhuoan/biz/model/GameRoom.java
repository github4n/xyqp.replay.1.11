package com.zhuoan.biz.model;

import net.sf.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class GameRoom {
    private String roomNo;// 房间号
    private int roomType; // 房间类型(0：房卡  1：金币 3:元宝)
    private double fee;// 金币、元宝扣除的服务费
    private double enterScore;// 准入分数
    private double leaveScore;// 离场分数
	private JSONObject roomInfo;// 房间信息
	private int gid;// 游戏类型
	private int gameStatus;// 游戏状态
	private int gameIndex;// 当前局数
	private int gameCount;// 游戏总局数
	private int maxPlayer;//游戏最大人数
	private String fangzhu;// 房主
	private String zhuang;// 庄家
	private int playerCount;// 玩家人数
	private int readyCount;// 准备人数
	private double score;//一局的底分
	private boolean isOpen;//是否开放
	private int level;//房间等级
	private int payType;//房间支付类型
	private Map<String,Playerinfo> playerMap = new HashMap<String, Playerinfo>();// 玩家个人信息
	private JSONObject setting;//游戏全局设置
	private String wfType;// 游戏信息
	private String createTime;// 创建时间
	private String ip;
	private int port;
	private int timeLeft;
	private int firstTime=0;
	private List<Long> userIdList;// 玩家座位号
    private List<String> userIconList;// 玩家图标
    private List<String> userNameList;// 玩家昵称
    private List<Integer> userScoreList;// 玩家积分
    private String owner;// 房主
    private String banker;// 庄家
    private int jieSanTime = 0;// 解散时间

    public int getJieSanTime() {
        return jieSanTime;
    }

    public void setJieSanTime(int jieSanTime) {
        this.jieSanTime = jieSanTime;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public int getRoomType() {
        return roomType;
    }

    public void setRoomType(int roomType) {
        this.roomType = roomType;
    }

    public double getFee() {
        return fee;
    }

    public void setFee(double fee) {
        this.fee = fee;
    }

    public double getEnterScore() {
        return enterScore;
    }

    public void setEnterScore(double enterScore) {
        this.enterScore = enterScore;
    }

    public double getLeaveScore() {
        return leaveScore;
    }

    public void setLeaveScore(double leaveScore) {
        this.leaveScore = leaveScore;
    }

    public JSONObject getRoomInfo() {
        return roomInfo;
    }

    public void setRoomInfo(JSONObject roomInfo) {
        this.roomInfo = roomInfo;
    }

    public int getGid() {
        return gid;
    }

    public void setGid(int gid) {
        this.gid = gid;
    }

    public int getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(int gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getGameIndex() {
        return gameIndex;
    }

    public void setGameIndex(int gameIndex) {
        this.gameIndex = gameIndex;
    }

    public int getGameCount() {
        return gameCount;
    }

    public void setGameCount(int gameCount) {
        this.gameCount = gameCount;
    }

    public int getMaxPlayer() {
        return maxPlayer;
    }

    public void setMaxPlayer(int maxPlayer) {
        this.maxPlayer = maxPlayer;
    }

    public String getFangzhu() {
        return fangzhu;
    }

    public void setFangzhu(String fangzhu) {
        this.fangzhu = fangzhu;
    }

    public String getZhuang() {
        return zhuang;
    }

    public void setZhuang(String zhuang) {
        this.zhuang = zhuang;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getReadyCount() {
        return readyCount;
    }

    public void setReadyCount(int readyCount) {
        this.readyCount = readyCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public void setOpen(boolean open) {
        isOpen = open;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPayType() {
        return payType;
    }

    public void setPayType(int payType) {
        this.payType = payType;
    }

    public Map<String, Playerinfo> getPlayerMap() {
        return playerMap;
    }

    public void setPlayerMap(Map<String, Playerinfo> playerMap) {
        this.playerMap = playerMap;
    }

    public JSONObject getSetting() {
        return setting;
    }

    public void setSetting(JSONObject setting) {
        this.setting = setting;
    }

    public String getWfType() {
        return wfType;
    }

    public void setWfType(String wfType) {
        this.wfType = wfType;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public int getFirstTime() {
        return firstTime;
    }

    public void setFirstTime(int firstTime) {
        this.firstTime = firstTime;
    }

    public List<Long> getUserIdList() {
        return userIdList;
    }

    public void setUserIdList(List<Long> userIdList) {
        this.userIdList = userIdList;
    }

    public List<String> getUserIconList() {
        return userIconList;
    }

    public void setUserIconList(List<String> userIconList) {
        this.userIconList = userIconList;
    }

    public List<String> getUserNameList() {
        return userNameList;
    }

    public void setUserNameList(List<String> userNameList) {
        this.userNameList = userNameList;
    }

    public List<Integer> getUserScoreList() {
        return userScoreList;
    }

    public void setUserScoreList(List<Integer> userScoreList) {
        this.userScoreList = userScoreList;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getBanker() {
        return banker;
    }

    public void setBanker(String banker) {
        this.banker = banker;
    }

    public ReentrantLock getM_locker() {
        return m_locker;
    }

    public void setM_locker(ReentrantLock m_locker) {
        this.m_locker = m_locker;
    }

    private ReentrantLock m_locker = new ReentrantLock(true);
    public void lock(){
        m_locker.lock();
    }

    public void unlock(){
        m_locker.unlock();
    }
}
