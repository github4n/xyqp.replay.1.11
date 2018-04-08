package com.zhuoan.biz.model.nn;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.biz.model.zjh.UserPacket;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class NNGameRoom extends GameRoom {

	private String roomNo;//房间号
	private int roomType; //房间类型（0：房卡  1：金币）
	private int gameType; // 游戏类型（0：投注 1：倍率）
	private String fangzhu;//房主
	private String zhuang;//庄家
	private int zhuangType;//定庄方式
	private Packer[] pai;
	private int playerCount;//玩家人数
	private int gameStatus;//游戏阶段 
	private double score;//一局的底分
	private double goldcoins;//准入金币（金币场玩家金币需要大于准入金币才能进入）
	private int readyCount=0;//准备人数
	private int gameCount;//游戏总局数
	private int gameIndex;//当前第几局
	private ConcurrentMap<String,Playerinfo> playerMap;//玩家个人信息
	private ConcurrentMap<String,UserPacket> userPacketMap;//玩家牌局信息
	CopyOnWriteArraySet<Long> userIDSet = new CopyOnWriteArraySet<Long>();
	private int closeTime;//申请解散房间倒计时
	private double fee;//金币、元宝扣除的服务费
	public Map<Integer, Integer> ratio = initRatio();
	
	private int xiazhuTime = 1;//下注时间
	private JSONObject playerMoney;//用户下注金额记录
	private JSONArray placeArray;
	private String baseNum;//游戏基数["10","20","30","40","50","60"]
	private JSONObject opensure = new JSONObject();//确认下注人数{"1":1,"2":0}    位置/是否
	private List<Integer> specialType = new ArrayList<Integer>();//配置特殊牌型（五花牛、葫芦牛、炸弹）
	private boolean isHalfwayin = false;//是否允许中途加入（true：允许、false：不允许）
	private int readyovertime;//准备超时（0：不处理 1：自动准备 2：踢出房间）
	public JSONArray qzTimes;//抢庄倍数
	public int qzsjzhuang;//抢庄随机庄
	public int qznozhuang;//没人抢庄
	
	public int restartTime=0;//明牌抢庄重发牌次数
	
	// 百人场牛牛
	private int lianzhuang;//连庄数
	private int maxLianzhuang;//最大连庄数
	private List<UserPacket> userPacketList;//牌组信息（下注区域：天地玄黄）
	private List<Playerinfo> shangzhuangList = new ArrayList<Playerinfo>();//上庄列表
	private JSONArray qushiArray = new JSONArray();//走势列表
	
	private boolean robot;//是否加入机器人
	private List<String> robotList = new ArrayList<String>();//机器人列表
	
	private boolean tuizhu;//是否闲家推注
	
	private boolean guanzhan;//是否观战模式
	private Map<String,Playerinfo> gzPlayerMap=new HashMap<String, Playerinfo>();;//观战玩家个人信息
	
	public int getRestartTime() {
		return restartTime;
	}

	public void setRestartTime(int restartTime) {
		this.restartTime = restartTime;
	}

	public boolean isGuanzhan() {
		return guanzhan;
	}

	public void setGuanzhan(boolean guanzhan) {
		this.guanzhan = guanzhan;
	}

	public Map<String, Playerinfo> getGzPlayerMap() {
		return gzPlayerMap;
	}

	public void setGzPlayerMap(Map<String, Playerinfo> gzPlayerMap) {
		this.gzPlayerMap = gzPlayerMap;
	}

	public boolean isTuizhu() {
		return tuizhu;
	}

	public void setTuizhu(boolean tuizhu) {
		this.tuizhu = tuizhu;
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
	 * 添加游戏下注金币
	 * @return,  
	 */
	public void addPlayerMoney(Integer num,Integer place,Integer money) {
		JSONObject data = this.getPlayerMoney();
		if(money>0){
			JSONObject numdata = data.getJSONObject(String.valueOf(num));
			double total = 0;
			if(!Dto.isObjNull(numdata)){
				if(numdata.containsKey(String.valueOf(place))){
					
					total = numdata.getDouble(String.valueOf(place));
					numdata.element(String.valueOf(place), total+money);
					data.element(String.valueOf(num), numdata);
				}else{
					numdata.element(String.valueOf(place), money);
					data.element(String.valueOf(num), numdata);
				}
			}else{
				numdata = new JSONObject();
				numdata.element(String.valueOf(place), money);
				data.element(String.valueOf(num), numdata);
			}
		}
		this.setPlayerMoney(data);
	}
	
	/**
	 * 获取方位的的金额总数
	 * @return
	 */
	public JSONObject getMoneyPlace() {
		
		JSONObject obj = new JSONObject();
		JSONObject data = this.getPlayerMoney();
		
		for (int i = 0; i < 10; i++) {
			
			String index = String.valueOf(i);
			JSONObject data1 = data.getJSONObject(index);
			if(!Dto.isObjNull(data1)){
				
				double money = data1.getDouble(index);
				if(obj.containsKey(index)){
					money += obj.getDouble(index);
				}
				obj.element(index, money);
			}
		}
		
		return obj;
	}
	
	/**
	 * 获取方位的的金额总数
	 * @param map 
	 * @return
	 */
	public JSONObject getMoneyPlaceByBR(Map<String, Playerinfo> map) {
		
		JSONObject obj = new JSONObject();
		JSONObject data = this.getPlayerMoney();
		double price = 0;
		double price1 = 0;
		double price2 = 0;
		double price3 = 0;
		
		for (String key:map.keySet()) {
			JSONObject data1 = data.getJSONObject(key);
			if(!Dto.isObjNull(data1)){
				if(data1.containsKey("0")){
					
					price+=data1.getDouble("0");
				}
				if(data1.containsKey("1")){
					
					price1+=data1.getDouble("1");
				}

				if(data1.containsKey("2")){
					
					price2+=data1.getDouble("2");
				}

				if(data1.containsKey("3")){
					
					price3+=data1.getDouble("3");
				}
			}
		}
		obj.element("0", price);
		obj.element("1", price1);
		obj.element("2", price2);
		obj.element("3", price3);
		return obj;
	}
	
	/**
	 * 获取位置下注的金额记录
	 * @param num 投注玩家
	 * @param place 投注区域（玩家下标）
	 * @return
	 */
	public double getPlayerMoneyNum(Integer num,Integer place) {
		
		JSONObject data = this.getPlayerMoney();
		if(!Dto.isObjNull(data)){
			JSONObject data2 = data.getJSONObject(String.valueOf(num));
			if(!Dto.isObjNull(data2)&&data2.containsKey(String.valueOf(place))){
				return data2.getDouble(String.valueOf(place));
			}
		}
		return 0;
	}
	
	/**
	 * 获取当局游戏下注总分
	 * @return
	 */
	public double getPlayerTotalMoney() {
		
		JSONObject data = this.getPlayerMoney();
		double totalMoney = 0;
		
		for (int i = 0; i < 10; i++) {
			String index = String.valueOf(i);
			JSONObject data1 = data.getJSONObject(index);
			if(!Dto.isObjNull(data1)){
				
				totalMoney += data1.getDouble(index);
			}
		}
		
		return totalMoney;
	}

	/**
	 * 获取当局游戏下注总分
	 * @return
	 */
	public double getPlayerTotalMoneyByBR(Map<String, Playerinfo> map) {
		
		JSONObject data = this.getPlayerMoney();
		double price = 0;
		double price1 = 0;
		double price2 = 0;
		double price3 = 0;
		
		for (String key:map.keySet()) {
			JSONObject data1 = data.getJSONObject(key);
			if(!Dto.isObjNull(data1)){
				
				if(data1.containsKey("0")){
					
					price+=data1.getDouble("0");
				}
				if(data1.containsKey("1")){
					
					price1+=data1.getDouble("1");
				}
				
				if(data1.containsKey("2")){
					
					price2+=data1.getDouble("2");
				}
				
				if(data1.containsKey("3")){
					
					price3+=data1.getDouble("3");
				}
			}
		}
		
		return price1+price2+price3+price;
	}
	
	/**
	 * 获取该玩家下注分数
	 * @return num
	 */
	public JSONObject getplaceArrayNums(int num) {
		
		JSONObject myMoney = new JSONObject();
		for (int i = 0; i < 10; i++) {
			myMoney.element(String.valueOf(i), 0);
		}
		
		JSONArray placeArray = this.placeArray;
		
		for (int i = 0, len = placeArray.size(); i < len; i++) {
			
			JSONObject postdata = placeArray.getJSONObject(i);
			int postnum = postdata.getInt("num");
			if(postnum==num){
				int place = postdata.getInt("place");
				double money = postdata.getDouble("money");
				myMoney.element(String.valueOf(place), myMoney.getDouble(String.valueOf(place))+money);
			}
		}
		return myMoney;
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

	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
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

	public int getZhuangType() {
		return zhuangType;
	}

	public void setZhuangType(int zhuangType) {
		this.zhuangType = zhuangType;
	}

	public Packer[] getPai() {
		return pai;
	}

	public void setPai(Packer[] pai) {
		this.pai = pai;
	}

	public int getPlayerCount() {
		return playerCount;
	}

	public void setPlayerCount(int playerCount) {
		this.playerCount = playerCount;
	}

	public int getGameStatus() {
		return gameStatus;
	}

	public void setGameStatus(int gameStatus) {
		this.gameStatus = gameStatus;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getGoldcoins() {
		return goldcoins;
	}

	public void setGoldcoins(double goldcoins) {
		this.goldcoins = goldcoins;
	}

	public int getReadyCount() {
		return readyCount;
	}

	public void setReadyCount(int readyCount) {
		this.readyCount = readyCount;
	}

	public int getGameCount() {
		return gameCount;
	}

	public void setGameCount(int gameCount) {
		this.gameCount = gameCount;
	}

	public int getGameIndex() {
		return gameIndex;
	}

	public void setGameIndex(int gameIndex) {
		this.gameIndex = gameIndex;
	}


	public int getCloseTime() {
		return closeTime;
	}

	public void setCloseTime(int closeTime) {
		this.closeTime = closeTime;
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

	public CopyOnWriteArraySet<Long> getUserIDSet() {
		return userIDSet;
	}

	public void setUserIDSet(CopyOnWriteArraySet<Long> userIDSet) {
		this.userIDSet = userIDSet;
	}

	public int getXiazhuTime() {
		return xiazhuTime;
	}

	public void setXiazhuTime(int xiazhuTime) {
		this.xiazhuTime = xiazhuTime;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public JSONObject getPlayerMoney() {
		return playerMoney;
	}

	public void setPlayerMoney(JSONObject playerMoney) {
		this.playerMoney = playerMoney;
	}

	public JSONArray getPlaceArray() {
		return placeArray;
	}

	public void setPlaceArray(JSONArray placeArray) {
		this.placeArray = placeArray;
	}

	public String getBaseNum() {
		if(zhuangType==5){ // 通比模式
			JSONArray array = new JSONArray();
			for (int i = 0; i < 3; i++) {
				JSONObject obj = new JSONObject();
				obj.put("name", (int)score * i);
				obj.put("val", i);
				array.add(obj);
			}
			return array.toString();
		}else{ // 庄闲模式
			return baseNum;
		}
	}

	public void setBaseNum(String baseNum) {
		this.baseNum = baseNum;
	}

	public JSONObject getOpensure() {
		return opensure;
	}

	public void setOpensure(JSONObject opensure) {
		this.opensure = opensure;
	}

	public List<Integer> getSpecialType() {
		return specialType;
	}

	public void setSpecialType(List<Integer> specialType) {
		this.specialType = specialType;
	}

	public boolean isHalfwayin() {
		return isHalfwayin;
	}

	public void setHalfwayin(boolean isHalfwayin) {
		this.isHalfwayin = isHalfwayin;
	}

	public int getReadyovertime() {
		return readyovertime;
	}

	public void setReadyovertime(int readyovertime) {
		this.readyovertime = readyovertime;
	}

	public int getLianzhuang() {
		return lianzhuang;
	}

	public void setLianzhuang(int lianzhuang) {
		this.lianzhuang = lianzhuang;
	}

	public int getMaxLianzhuang() {
		return maxLianzhuang;
	}

	public void setMaxLianzhuang(int maxLianzhuang) {
		this.maxLianzhuang = maxLianzhuang;
	}

	public List<UserPacket> getUserPacketList() {
		return userPacketList;
	}

	public void setUserPacketList(List<UserPacket> userPacketList) {
		this.userPacketList = userPacketList;
	}

	public List<Playerinfo> getShangzhuangList() {
		return shangzhuangList;
	}

	public void setShangzhuangList(List<Playerinfo> shangzhuangList) {
		this.shangzhuangList = shangzhuangList;
	}

	public JSONArray getQushiArray() {
		return qushiArray;
	}

	public void setQushiArray(JSONArray qushiArray) {
		this.qushiArray = qushiArray;
	}

	/**
	 * 获取房间人员的信息
	 * @return
	 */
	public JSONArray getAllPlayer(){
		
		JSONArray array = new JSONArray();
		
		for(String uuid : playerMap.keySet()){
			
			Playerinfo player = playerMap.get(uuid);
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
				obj.put("status", player.getStatus());
				obj.put("introduction", player.getSignature());
				UserPacket up = getUserPacketMap().get(uuid);
				if(up.getStatus()==-1 || (up.getMyPai().length>0&&up.getMyPai()[0]==0)){ // 判断玩家是否是中途加入
					obj.put("readyStatus", -1);
				}else{
					obj.put("readyStatus", up.getStatus());
				}
				array.add(obj);
			}
		}
		return array;
	}
	
	/**
	 * 获取百人牛牛有座位的玩家
	 * @return
	 */
	public JSONArray getBRNNAllPlayer(String account){
		
		JSONArray array = new JSONArray();
		
		for(String uuid : playerMap.keySet()){
			
			Playerinfo player = playerMap.get(uuid);
			if(player!=null&&((player.getMyIndex()>=0&&player.getMyIndex()<=6)||player.getAccount().equals(account))){
				
				JSONObject obj = new JSONObject();
				obj.put("account", player.getAccount());
				obj.put("name", player.getName());
				obj.put("headimg", player.getRealHeadimg());
				obj.put("sex", player.getSex());
				obj.put("ip", player.getIp());
				obj.put("location", player.getLocation());
				obj.put("score", player.getScore());
				obj.put("index", player.getMyIndex());
				obj.put("status", player.getStatus());
				array.add(obj);
			}
		}
		return array;
	}
	
	/**
	 * 获取玩家的准备状态
	 * @return
	 */
	public JSONArray getPlayerIsReady(){
		
		JSONArray array = new JSONArray();
		for (String uuid :userPacketMap.keySet()) {
			JSONObject obj = new JSONObject();
			obj.put("index", playerMap.get(uuid).getMyIndex());
			obj.put("result", userPacketMap.get(uuid).getIsReady());
			obj.put("value", userPacketMap.get(uuid).qzTimes);
			array.add(obj);
		}
		return array;
	}

	/**
	 * 获取玩家账号
	 * @return
	 */
	public String getPlayerAccount(String uuid){
		if(this.playerMap.get(uuid)!=null){
			return this.playerMap.get(uuid).getAccount();
		}else{
			return "10000000";
		}
	}
	
	/**
	 * 获取玩家的位置
	 * @return
	 */
	public int getPlayerIndex(String uuid){
		if(this.playerMap.get(uuid)!=null){
			return this.playerMap.get(uuid).getMyIndex();
		}else{
			return 0;
		}
	}
	
	/**
	 * 获取当前玩家的下家的UUID
	 * @return
	 */
	public String getNextPlayer(String useruuid){
		
		if(playerMap.get(useruuid)!=null){
			
			int playerCount = 6;
			int index=playerMap.get(useruuid).getMyIndex();
			int next=index+1;
			Playerinfo player = null;
			while (player==null&&index!=next) {
				if(next>=playerCount){
					next=0;
				}
				for (String uuid : playerMap.keySet()) {
					if(next==playerMap.get(uuid).getMyIndex()){
						return uuid;
					}
				}
				next++;
			}
		}
		return zhuang;
	}
	
	
	/**
	 * 获取已准备的玩家下标
	 * @return
	 */
	public Integer[] getReadyIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			int ready = getUserPacketMap().get(uuid).getIsReady();
			if(ready==1){
				indexList.add(getPlayerMap().get(uuid).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}
	
	/**
	 * 获取已出牌的玩家下标
	 * @return
	 */
	public Integer[] getChuPaiIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			int status = getUserPacketMap().get(uuid).getStatus();
			if(status==NiuNiu.USERPACKER_STATUS_LIANGPAI){
				indexList.add(getPlayerMap().get(uuid).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}
	
	
	/**
	 * 初始化游戏房间
	 */
	public void initGame(){
		
		// 重置投注信息
		setPlaceArray(new JSONArray());
		setPlayerMoney(new JSONObject());
		
		// 重置玩家信息
		for (String uuid : getUserPacketMap().keySet()) {
			if(userPacketMap.containsKey(uuid)){
				userPacketMap.get(uuid).initUserPacket();
			}
		}
	}
	

	/**
	 * 撤销游戏下注金币
	 * @return
	 */
	public void delPlayerMoney(Integer num) {
		JSONObject data = this.getPlayerMoney();
		
		JSONObject numdata = data.getJSONObject(num+"");
		
		for(Object key:numdata.keySet()){
			numdata.element((String)key, "0");
		}
		this.setPlayerMoney(data);
	}
	
	
	/**
	 * 获取确认下注人数
	 * @return 
	 */
	public int getsureNum() {
		
		JSONObject data = this.opensure;
		int num = 0;
		for(Object key:data.keySet()){
			if(data.getInt(key+"")==1){
				num++;
			}
		}
		return num;
	}
	
	/**
	 * 获取玩家uuid
	 * @param clientTag
	 * @return
	 */
	public UUID getUUIDByClientTag(String clientTag){
		
		if(playerMap.containsKey(clientTag)){
			return playerMap.get(clientTag).getUuid();
		}
		return null;
	}

	/**
	 * 保存胜负记录（百人牛牛）
	 */
	public void addQuShiList() {
		
		JSONArray array = new JSONArray();
		for (int i = 0; i < userPacketList.size(); i++) {
			int win = 0;
			if(i==0){
				if(userPacketList.get(i).getScore()>0){
					win=1;
				}else if(userPacketList.get(i).getScore()==0){
					if(userPacketList.get(1).isWin()
							&&userPacketList.get(2).isWin()
							&&userPacketList.get(3).isWin()
							&&userPacketList.get(4).isWin()){
						win = 0;
					}else{
						win = 1;
					}
				}
			}else{
				if(userPacketList.get(i).isWin()){
					win=1;
				}
			}
			array.add(win);
		}
		qushiArray.add(array);
	}
	
	/**
	 * 获取胜负记录（百人牛牛）
	 * @return
	 */
	public JSONArray getQuShiList() {
		
		// 截取胜负记录
		JSONArray array = new JSONArray();
		int listSize = qushiArray.size();
		if(listSize>11){
			array = JSONArray.fromObject(qushiArray.subList(listSize-11, listSize-1));
		}else{
			array = qushiArray;
		}
		
		// 组织数据
		JSONArray qushiArray = new JSONArray();
		for (int i = 0; i < 5; i++) {
			JSONArray a = new JSONArray();
			qushiArray.add(a);
		}
		for (int i = 0; i < array.size(); i++) {
			JSONArray arr = array.getJSONArray(i);
			for (int j = 0; j < arr.size(); j++) {
				qushiArray.getJSONArray(j).add(arr.getInt(j));
			}
		}
		
		return qushiArray;
	}
	

	/**
	 * 获取玩家抢庄倍数
	 * @return
	 */
	public JSONArray getPlayerQzResult(){
		
		JSONArray array = new JSONArray();
		for (String uuid :userPacketMap.keySet()) {
			UserPacket up = userPacketMap.get(uuid);
			if(up.getIsReady()==10||up.getIsReady()==-1){ // 获取当前已选择的玩家，10：抢庄 -1：不抢
				JSONObject obj = new JSONObject();
				obj.put("index", playerMap.get(uuid).getMyIndex());
				int qztimes = up.qzTimes;
				if(qztimes<=0){
					qztimes = 1;
				}
				obj.put("value", qztimes);
				array.add(obj);
			}
		}
		return array;
	}
	
	
	/**
	 * 获取抢庄倍数
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
		int beishu = (int) (yuanbao/(baseNum*score*(playerCount-1)*maxVal));
		JSONArray qzts = new JSONArray();
		for (int i = 0; i < qzTimes.size(); i++) {
			JSONObject obj = new JSONObject();
			int val = qzTimes.getInt(i);
			obj.put("name", String.valueOf(val));
			if(beishu>=val){
				obj.put("val", 1);
			}else{
				obj.put("val", 0);
			}
			qzts.add(obj);
		}
		return qzts;
	}

	
	/**
	 * 获取玩家下注倍数
	 * @param yuanbao
	 * @return
	 */
	public JSONArray getBaseNumTimes(double yuanbao){
		
		// 基数
		int baseNum = 5;		
		// 底注
		double di = score;
		// 庄家抢庄倍数
		int qzTimes = 1;
		if((zhuangType==2 || zhuangType==3)&&!Dto.isNull(userPacketMap)&&!Dto.isNull(userPacketMap.get(zhuang))){ // 抢庄
			qzTimes = userPacketMap.get(zhuang).qzTimes;
		}
		// 最大下注倍数
		int beishu = (int) (yuanbao/(baseNum*di*qzTimes));
		JSONArray baseNums = new JSONArray();
		JSONArray array = JSONArray.fromObject(getBaseNum());
		for (int i = 0; i < array.size(); i++) {
			int val = array.getJSONObject(i).getInt("val");
			JSONObject obj = new JSONObject();
			obj.put("name", String.valueOf(val));
			if(beishu>=val){
				obj.put("val", 1);
			}else{
				obj.put("val", 0);
			}
			baseNums.add(obj);
		}
		return baseNums;
	}

	/**
	 * 获取正在游戏中的玩家下标
	 * @return
	 */
	public Integer[] getGameIngIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uuid :userPacketMap.keySet()) {
			UserPacket up = getUserPacketMap().get(uuid);
			if(up.getStatus()>=NiuNiu.USERPACKER_STATUS_CHUSHI){
				if(up.getStatus()==NiuNiu.USERPACKER_STATUS_LIANGPAI){
					if(up.getMyPai()[0]>0){
						indexList.add(getPlayerMap().get(uuid).getMyIndex());
					}
				}else{
					indexList.add(getPlayerMap().get(uuid).getMyIndex());
				}
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}

}
