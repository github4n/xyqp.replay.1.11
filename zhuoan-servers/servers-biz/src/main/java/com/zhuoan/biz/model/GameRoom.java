package com.zhuoan.biz.model;

import com.zhuoan.constant.CommonConstant;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author wqm
 * @DESCRIPTION 房间实体类父类
 * @Date Created in 14:24 2018/4/21
 * @Modified By:
 **/
public class GameRoom {
    /**
     * 房间号
     */
    private String roomNo;
    /**
     * 房间类型(0：房卡  1：金币 3:元宝)
     */
    private int roomType;
    /**
     * 金币、元宝扣除的服务费
     */
    private double fee;
    /**
     * 准入分数
     */
    private double enterScore;
    /**
     * 离场分数
     */
    private double leaveScore;
    /**
     * 房间信息
     */
	private JSONObject roomInfo;
    /**
     * 游戏类型
     */
	private int gid;
    /**
     * 游戏状态
     */
	private int gameStatus;
    /**
     * 当前局数
     */
	private int gameIndex;
    /**
     * 游戏总局数
     */
	private int gameCount;
    /**
     * 游戏最大人数
     */
	private int maxPlayer;
    /**
     * 是否允许中途加入（true：允许、false：不允许）
     */
    private boolean isHalfwayIn = false;
    /**
     * 准备超时（0：不处理 1：自动准备 2：踢出房间）
     */
    private int readyOvertime;
    /**
     * 是否加入机器人
     */
    private boolean robot;
    /**
     * 是否观战模式
     */
    private boolean visit;
	private String fangzhu;// 房主
	private String zhuang;// 庄家
    /**
     * 玩家人数
     */
	private int playerCount;
	private int readyCount;// 准备人数
    /**
     * 一局的底分
     */
	private double score;
    /**
     * 是否开放
     */
	private boolean isOpen;
	private int level;//房间等级
    /**
     * 房间支付类型
     */
	private int payType;
    /**
     * 玩家个人信息
     */
	private Map<String,Playerinfo> playerMap = new HashMap<String, Playerinfo>();
    /**
     * 游戏全局设置
     */
	private JSONObject setting;
    /**
     * 游戏信息
     */
	private String wfType;
    /**
     * 创建时间
     */
	private String createTime;
    /**
     * 游戏流程
     */
    private JSONObject gameProcess = new JSONObject();
    /**
     * ip
     */
	private String ip;
    /**
     * 端口
     */
	private int port;
    /**
     * 房间倒计时
     */
	private int timeLeft;
    /**
     * 房间倒计时
     */
	private int firstTime=0;
    /**
     * 玩家座位号
     */
	private List<Long> userIdList;
    private List<String> userIconList;// 玩家图标
    private List<String> userNameList;// 玩家昵称
    private List<Integer> userScoreList;// 玩家积分
    /**
     * 房主
     */
    private String owner;
    /**
     * 庄家
     */
    private String banker;
    /**
     * 解散时间
     */
    private int jieSanTime = 0;
    /**
     * 房间id
     */
    private long id = 0;

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

    public JSONObject getGameProcess() {
        return gameProcess;
    }

    public void setGameProcess(JSONObject gameProcess) {
        this.gameProcess = gameProcess;
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

    public int getJieSanTime() {
        return jieSanTime;
    }

    public void setJieSanTime(int jieSanTime) {
        this.jieSanTime = jieSanTime;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * 获取当前房间内的所有人
     * @return
     */
    public List<UUID> getAllUUIDList(){
        List<UUID> uuidList = new ArrayList<UUID>();
        for (String account : getPlayerMap().keySet()) {
            uuidList.add(getPlayerMap().get(account).getUuid());
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
        for (String account : getPlayerMap().keySet()) {
            if (!uuid.equals(account)) {
                uuidList.add(getPlayerMap().get(account).getUuid());
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


    //==================================组织数据开始======================================

    public JSONObject getJsonObject(JSONArray array) {
        JSONObject objectDao = new JSONObject();
        objectDao.put("array",array);
        objectDao.put("roomNo",getRoomNo());
        objectDao.put("gId",getGid());
        objectDao.put("fee",getFee());
        objectDao.put("updateType",getUpdateType());
        return objectDao;
    }

    public JSONObject getPumpObject(JSONArray array) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("array",array);
        jsonObject.put("updateType",getUpdateType());
        return jsonObject;
    }

    /**
     * 获取玩家战绩数据
     * @param gameLogId
     * @param users
     * @param gameResult
     * @return
     */
    public JSONArray obtainUserGameLog( long gameLogId, JSONArray users, String gameResult) {
        JSONArray userGameLogs = new JSONArray();
        for (int i = 0; i < users.size(); i++) {
            long userId = users.getJSONObject(i).getLong("id");
            JSONObject userGameLog = new JSONObject();
            userGameLog.put("gid", getGid());
            userGameLog.put("room_id", getId());
            userGameLog.put("room_no", getRoomNo());
            userGameLog.put("user_id", userId);
            userGameLog.put("gamelog_id", gameLogId);
            userGameLog.put("result", gameResult);
            userGameLog.put("createtime", TimeUtil.getNowDate());
            userGameLog.put("account", users.getJSONObject(i).getDouble("fen"));
            userGameLog.put("fee", getFee());
            userGameLogs.add(userGameLog);
        }
        return userGameLogs;
    }

    /**
     * 获取战绩数据
     * @param result
     * @return
     */
    public JSONObject obtainGameLog(String result, String gameProcess) {
        JSONObject gamelog = new JSONObject();
        StringBuffer id = new StringBuffer();
        id.append(System.currentTimeMillis());
        id.append(getRoomNo());
        gamelog.put("id",Long.valueOf(id.toString()));
        gamelog.put("gid", getGid());
        gamelog.put("room_no", getRoomNo());
        gamelog.put("game_index", getGameIndex());
        gamelog.put("base_info", getRoomInfo());
        gamelog.put("result", result);
        gamelog.put("action_records", gameProcess);
        String nowTime = TimeUtil.getNowDate();
        gamelog.put("finishtime", nowTime);
        gamelog.put("createtime", nowTime);
        gamelog.put("status", 1);
        gamelog.put("roomtype", getRoomType());
        return gamelog;
    }
    //==================================组织数据结束======================================

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
