package com.zhuoan.biz.model;

import com.zhuoan.biz.model.qzmj.QZMJGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.util.Dto;
import com.zhuoan.util.TimeUtil;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
     * 房间机器人列表
     */
    private List<String> robotList = new ArrayList<String>();
    /**
     * 是否观战模式
     */
    private boolean visit;
    /**
     * 玩家人数
     */
	private int playerCount;
    /**
     * 一局的底分
     */
	private double score;
    /**
     * 是否开放
     */
	private boolean isOpen;
    /**
     * 房间支付类型
     */
	private int payType;
    /**
     * 玩家个人信息
     */
	private ConcurrentHashMap<String,Playerinfo> playerMap = new ConcurrentHashMap<String, Playerinfo>();
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
    /**
     * 每个玩家支付房卡数量
     */
    private int singlePayNum = 0;
    /**
     * 总结算数据
     */
    private JSONArray finalSummaryData = new JSONArray();
    /**
     * 金币场等级
     */
    private int level;
    /**
     * 最小坐庄分数
     */
    private double minBankerScore;
    /**
     *  结算数据
     */
    private JSONObject summaryData = new JSONObject();
    /**
     * 是否需要总结算
     */
    private boolean needFinalSummary = false;
    /**
     * 是否解散房间
     */
    private int isClose=-1;
    /**
     * 最后一个座位
     */
    private int lastIndex;
    /**
     * 是否是资金盘
     */
    private boolean isFund = false;
    /**
     * 比赛场编号
     */
    private String matchNum;
    /**
     * 比赛场总人数
     */
    private int totalNum;
    /**
     * 连胜信息
     */
    private JSONObject winStreakObj = new JSONObject();
    /**
     * 货币类型
     */
    private String currencyType;
    /**
     * 平台标识
     */
    private String platform;
    /**
     * 俱乐部编号
     */
    private String clubCode;

    public int getLastIndex() {
        return lastIndex;
    }

    public void setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
    }

    public int getIsClose() {
        return isClose;
    }

    public void setIsClose(int isClose) {
        this.isClose = isClose;
    }

    public boolean isNeedFinalSummary() {
        return needFinalSummary;
    }

    public void setNeedFinalSummary(boolean needFinalSummary) {
        this.needFinalSummary = needFinalSummary;
    }

    public JSONObject getSummaryData() {
        return summaryData;
    }

    public void setSummaryData(JSONObject summaryData) {
        this.summaryData = summaryData;
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

    public List<String> getRobotList() {
        return robotList;
    }

    public void setRobotList(List<String> robotList) {
        this.robotList = robotList;
    }

    public boolean isVisit() {
        return visit;
    }

    public void setVisit(boolean visit) {
        this.visit = visit;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
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

    public int getPayType() {
        return payType;
    }

    public void setPayType(int payType) {
        this.payType = payType;
    }

    public ConcurrentHashMap<String, Playerinfo> getPlayerMap() {
        return playerMap;
    }

    public void setPlayerMap(ConcurrentHashMap<String, Playerinfo> playerMap) {
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

    public int getSinglePayNum() {
        return singlePayNum;
    }

    public void setSinglePayNum(int singlePayNum) {
        this.singlePayNum = singlePayNum;
    }

    public JSONArray getFinalSummaryData() {
        return finalSummaryData;
    }

    public void setFinalSummaryData(JSONArray finalSummaryData) {
        this.finalSummaryData = finalSummaryData;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public double getMinBankerScore() {
        return minBankerScore;
    }

    public void setMinBankerScore(double minBankerScore) {
        this.minBankerScore = minBankerScore;
    }

    public boolean isFund() {
        return isFund;
    }

    public void setFund(boolean fund) {
        isFund = fund;
    }

    public String getMatchNum() {
        return matchNum;
    }

    public void setMatchNum(String matchNum) {
        this.matchNum = matchNum;
    }

    public int getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(int totalNum) {
        this.totalNum = totalNum;
    }

    public JSONObject getWinStreakObj() {
        return winStreakObj;
    }

    public void setWinStreakObj(JSONObject winStreakObj) {
        this.winStreakObj = winStreakObj;
    }

    public String getCurrencyType() {
        return currencyType;
    }

    public void setCurrencyType(String currencyType) {
        this.currencyType = currencyType;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getClubCode() {
        return clubCode;
    }

    public void setClubCode(String clubCode) {
        this.clubCode = clubCode;
    }

    /**
     * 获取当前房间内的所有人
     * @return
     */
    public List<UUID> getAllUUIDList(){
        List<UUID> uuidList = new ArrayList<UUID>();
        for (String account : getPlayerMap().keySet()) {
            if (getPlayerMap().containsKey(account)&&getPlayerMap().get(account)!=null) {
                uuidList.add(getPlayerMap().get(account).getUuid());
            }
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
            if (getPlayerMap().containsKey(account)&&getPlayerMap().get(account)!=null) {
                if (!uuid.equals(account)) {
                    uuidList.add(getPlayerMap().get(account).getUuid());
                }
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
            case CommonConstant.ROOM_TYPE_COMPETITIVE:
                return "roomcard";
            default:
                return "";
        }
    }

    public JSONObject getJsonObject(JSONArray array) {
        JSONObject objectDao = new JSONObject();
        objectDao.put("array",array);
        objectDao.put("roomNo",getRoomNo());
        objectDao.put("gId",getGid());
        objectDao.put("fee",getFee());
        objectDao.put("updateType",getCurrencyType());
        return objectDao;
    }

    public JSONObject getRoomCardChangeObject(JSONArray array,int roomCardCount) {
        JSONObject obj = new JSONObject();
        obj.put("array",array);
        obj.put("roomNo",getRoomNo());
        obj.put("gId",getGid());
        obj.put("fee",roomCardCount);
        obj.put("updateType",getCurrencyType());
        return obj;
    }

    public JSONObject getPumpObject(JSONArray array) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("array",array);
        jsonObject.put("updateType",getCurrencyType());
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
            userGameLog.put("game_index", getGameIndex());
            userGameLog.put("user_id", userId);
            userGameLog.put("gamelog_id", gameLogId);
            userGameLog.put("result", gameResult);
            userGameLog.put("createtime", TimeUtil.getNowDate());
            userGameLog.put("account", users.getJSONObject(i).getDouble("fen"));
            userGameLog.put("fee", getFee());
            userGameLog.put("room_type", getRoomType());
            if (getRoomType() == CommonConstant.ROOM_TYPE_CLUB) {
                userGameLog.put("club_code", getClubCode());
            }
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
        JSONObject baseInfo = getRoomInfo();
        gamelog.put("base_info", baseInfo);
        gamelog.put("result", result);
        if (getGid()==CommonConstant.GAME_ID_QZMJ||getGid()==CommonConstant.GAME_ID_NAMJ) {
            baseInfo.put("game_count", getGameCount());
            baseInfo.put("zhuang", ((QZMJGameRoom)this).getPlayerIndex(getBanker()));
            baseInfo.put("jin", ((QZMJGameRoom)this).getJin());
            baseInfo.put("users", ((QZMJGameRoom)this).getAllPlayer());
            gamelog.put("base_info",baseInfo);
            gamelog.put("result",((QZMJGameRoom)this).getSummaryData());
        }
        gamelog.put("action_records", gameProcess);
        String nowTime = TimeUtil.getNowDate();
        String visitCode = Dto.getEntNumCode(8);
        gamelog.put("visitcode", visitCode);
        gamelog.put("finishtime", nowTime);
        gamelog.put("createtime", nowTime);
        gamelog.put("status", 1);
        gamelog.put("roomtype", getRoomType());
        return gamelog;
    }
}
