package com.zhuoan.biz.core.sss;

import com.zhuoan.biz.model.*;
import com.zhuoan.biz.model.sss.AutoExitThread;
import com.zhuoan.biz.model.sss.MutliThreadSSS1;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.model.sss.SaveLogsThreadSSS;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

public  class SSSGameRoom extends GameRoom {

	private String roomNo;//房间号
	private int roomType; //房间类型（0：房卡  1：金币 2：代开房间 3:元宝）
	private JSONObject roomobj;//房间信息
	private String fangzhu;//房主
	private String zhuang;//庄家
	private List<String> pai;
	private int playerCount;//玩家人数
	private int maxplayer=0;//运行最大玩家人数
	private int gameType;//游戏模式
	private int gameStatus;//游戏阶段       
	private double score;//一局的底分
	private int minscore;//一局的最低进入分
	private double fee;//抽水
	private int readyCount=0;//准备人数
	private int gameCount;//游戏总局数
	private int gameIndex;//当前第几局
	private int maPaiType;//马牌类型
	private int color;//加色
	private String maPai;//马牌
	private int readyTime=0;//准备时间
	private int peipaiTime=0;//配牌时间
	private int CloseTime=-1;//解散时间
	private Map<String,Playerinfo> playerMap;//玩家个人信息
	private Map<String,Player> playerPaiJu;//玩家牌局信息
	private ConcurrentSkipListSet<UUID> uuidList;//用户的uuid
	private ConcurrentSkipListSet<String> userAcc = new ConcurrentSkipListSet<String>();// 玩家account集合
	private ConcurrentSkipListSet<Long> userSet = new ConcurrentSkipListSet<Long>();// 玩家ID集合
	private MutliThreadSSS1 thread;//游戏定时线程
	private AutoExitThread exitThread;//游戏自动解散 线程
	private SaveLogsThreadSSS saveLogsThreadSSS;//游戏战绩保存线程
	private JSONObject setting;//游戏全局设置
	private int level;//金币场等级

	private static final int model1=0;//经典模式
	private static final int model2=1;//加一色
	//private static final int model3=2;//加两色
	
	private List<String> robotList = new ArrayList<String>();
	private boolean robot;
	
	public List<String> getRobotList() {
		return robotList;
	}

	public void setRobotList(List<String> robotList) {
		this.robotList = robotList;
	}

	public boolean isRobot() {
		return robot;
	}

	public void setRobot(boolean robot) {
		this.robot = robot;
	}

	public String getRoomNo() {
		return roomNo;
	}

	public AutoExitThread getExitThread() {
		return exitThread;
	}

	public void setExitThread(AutoExitThread exitThread) {
		this.exitThread = exitThread;
	}

	public MutliThreadSSS1 getThread() {
		return thread;
	}

	public void setThread(MutliThreadSSS1 thread) {
		this.thread = thread;
	}

	public void setRoomNo(String roomNo) {
		this.roomNo = roomNo;
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

	public Map<String, Playerinfo> getPlayerMap() {
		return playerMap;
	}

	public void setPlayerMap(Map<String, Playerinfo> playerMap) {
		this.playerMap = playerMap;
	}

	public List<String> getPai() {
		return pai;
	}

	public void setPai(List<String> pai) {
		this.pai = pai;
	}

	public Map<String, Player> getPlayerPaiJu() {
		return playerPaiJu;
	}

	public void setPlayerPaiJu(Map<String, Player> playerPaiJu) {
		this.playerPaiJu = playerPaiJu;
	}

	public ConcurrentSkipListSet<UUID> getUuidList() {
		return uuidList;
	}

	public void setUuidList(ConcurrentSkipListSet<UUID> uuidList) {
		this.uuidList = uuidList;
	}

	public ConcurrentSkipListSet<Long> getUserSet() {
		return userSet;
	}

	public void setUserSet(ConcurrentSkipListSet<Long> userSet) {
		this.userSet = userSet;
	}
	
	public int getGameType() {
		return gameType;
	}

	public void setGameType(int gameType) {
		this.gameType = gameType;
	}
	public int getRoomType() {
		return roomType;
	}

	public void setRoomType(int roomType) {
		this.roomType = roomType;
	}

	public int getPeipaiTime() {
		return peipaiTime;
	}

	public void setPeipaiTime(int peipaiTime) {
		this.peipaiTime = peipaiTime;
	}

	public int getReadyTime() {
		return readyTime;
	}

	public void setReadyTime(int readyTime) {
		this.readyTime = readyTime;
	}

	public int getMaPaiType() {
		return maPaiType;
	}

	public void setMaPaiType(int maPaiType) {
		this.maPaiType = maPaiType;
	}

	public String getMaPai() {
		return maPai;
	}

	public void setMaPai(String maPai) {
		this.maPai = maPai;
	}
	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}

	/**
	 * 获取房间人员的信息
	 * @return
	 */
	public JSONArray getAllPlayer(){
		
		JSONArray array = new JSONArray();
		
		for(String uc : playerMap.keySet()){
			
			Playerinfo player = playerMap.get(uc);
			Player player1 = playerPaiJu.get(uc);
			if(player!=null){
				
				JSONObject obj = new JSONObject();
				obj.put("name", player.getName());
				obj.put("headimg", player.getRealHeadimg());
				obj.put("score", player1.getTotalScore());
				obj.put("index", player.getMyIndex());
				obj.put("status", player.getStatus());
				obj.put("ip", player.getIp());
				obj.put("location", player.getLocation());
				obj.put("id", player.getAccount());
				obj.put("ghName", player.getGhName());
				array.add(obj);
			}
		}
		return array;
	}
	
	/**
	 * 获取玩家的准备状态
	 * @return
	 */
	public int[] getPlayerIsReady(){
		
		int[] isReady = new int[playerPaiJu.size()];
		int i = 0;
		for (String uc :playerPaiJu.keySet()) {
			isReady[i] = playerPaiJu.get(uc).getIsReady();
			i++;
		}
		return isReady;
	}

	/**
	 * 获取玩家的位置
	 * @return
	 */
	public int getPlayerIndex(String uc){
		if(this.playerMap.get(uc)!=null){
			return this.playerMap.get(uc).getMyIndex();
		}else{
			return 0;
		}
	}
	public int getPlayerIndex1(UUID uc){
		if(this.playerMap.get(uc)!=null){
			return this.playerMap.get(uc).getMyIndex();
		}else{
			return 0;
		}
	}
	
	/**
	 * 获取当前玩家的下家的UUID
	 * @return
	 */
	public String getNextPlayer(String useruuid){
		int index=playerMap.get(useruuid).getMyIndex();
		int next=index+1;
		Playerinfo player = null;
		while (player==null&&index!=next) {
			if(next>=this.playerMap.size()){
				next=0;
			}
			for (String uc : playerMap.keySet()) {
				if(next==playerMap.get(uc).getMyIndex()){
					return uc;
				}
			}
			next++;
		}
		return zhuang;
	}
	
	
	/**
	 * 获取已准备的玩家下标
	 * @return
	 */
	public Integer[] getReadyIndex() {
		
		List<Integer> indexList = new ArrayList<Integer>();
		for (String uc :playerPaiJu.keySet()) {
			int ready = playerPaiJu.get(uc).getIsReady();
			if(ready>0){
				indexList.add(getPlayerMap().get(uc).getMyIndex());
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
		for (String uc :playerPaiJu.keySet()) {
			int status = playerPaiJu.get(uc).getStatus();
			if(status==2){
				indexList.add(getPlayerMap().get(uc).getMyIndex());
			}
		}
		return indexList.toArray(new Integer[indexList.size()]);
	}

	
	/**
	 * 洗牌
	 * @return
	 */
	public List<String> xiPai() {
		
		// 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
		String[] colors = { "1-", "2-", "3-", "4-" };
		// 定义一个点数数组
		String[] numbers = { "2","3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
				"13", "1" };

		List<String> pai=new ArrayList<String>();
		for (String number : numbers) {
			for (String color : colors) {
				String poker = color.concat(number);
				pai.add(poker);
			}
		}
		// 洗牌
		Collections.shuffle(pai);
		return pai;
	}

	/**
	 * 洗牌
	 * @param perNum 玩家数量
	 * @param mode 游戏模式 0：经典模式，1：加一色，2：加两色
	 * @return
	 */
	public  List<String> xiPai1(int perNum,int mode) {
		
		// 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
		String[] colors = { "1-", "2-", "3-", "4-" };
		// 定义一个点数数组
		String[] numbers = { "2","3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
				"13", "1" };

		List<String> pai=new ArrayList<String>();
		List<String> oneFlower=new ArrayList<String>();//黑桃
		List<String> twoFlower=new ArrayList<String>();//红心
		List<String> threeFlower=new ArrayList<String>();//梅花
		List<String> FourFlower=new ArrayList<String>();//方块
		for (String number : numbers) {
			for (String color : colors) {
				String poker = color.concat(number);
				pai.add(poker);
				if("1-".equals(color)){
					oneFlower.add(poker);
				}else if ("2-".equals(color)) {
					twoFlower.add(poker);
				}else if ("3-".equals(color)) {
					threeFlower.add(poker);
				}else if ("4-".equals(color)) {
					FourFlower.add(poker);
				}
			}
		}
		//根据人数判断使用几副牌
		/*if(perNum>4){
			//pai.addAll(pai);
			System.out.println("两副牌"); 
		}*/
		System.out.println("人数："+perNum+"；模式："+mode);
		if (perNum<5&&mode>0) {
			
				if (mode==1) {
					pai.addAll(oneFlower);
					System.out.println("模式多一色");
				}else if(mode==2){
					pai.addAll(oneFlower);
					pai.addAll(twoFlower);
					System.out.println("模式多两色");
				}
			
		}else if (perNum==5) {
			pai.addAll(oneFlower);
			System.out.println("5人加一色");
			if (mode==1) {
				pai.addAll(twoFlower);
				System.out.println("5人加一色");
			}else if(mode==2){
				pai.addAll(twoFlower);
				pai.addAll(threeFlower);
				System.out.println("5人加两色");
			}
		}else if(perNum==6){
			pai.addAll(oneFlower);
			pai.addAll(twoFlower);
			System.out.println("6人加两色");
			if (mode==1) {
				pai.addAll(threeFlower);
				System.out.println("6人加一色");
			}else if(mode==2){
				pai.addAll(threeFlower);
				pai.addAll(FourFlower);
				System.out.println("6人加两色");
			} 
			 
		}else if(perNum==7){
			pai.addAll(oneFlower);
			pai.addAll(twoFlower);
			pai.addAll(threeFlower);
			System.out.println("7人加三色");
			
		}else if(perNum==8){
			pai.addAll(oneFlower);
			pai.addAll(twoFlower);
			pai.addAll(threeFlower);
			pai.addAll(FourFlower);
			System.out.println("8人加四色");
			
		}
		/*if (perNum<5) {
			if (mode==1) {
				pai.addAll(oneFlower);
				System.out.println("模式多一色");
			}else if(mode==2){
				pai.addAll(oneFlower);
				pai.addAll(twoFlower);
				System.out.println("模式多两色");
			}
		}else if (perNum==5) {
			pai.addAll(oneFlower);
			if (mode==1) {
				pai.addAll(twoFlower);
				System.out.println("5人加一色");
			}else if(mode==2){
				pai.addAll(twoFlower);
				pai.addAll(threeFlower);
				System.out.println("5人加两色");
			}
		}else if(perNum==6){
			pai.addAll(oneFlower);
			pai.addAll(twoFlower);
			if (mode==1) {
				pai.addAll(threeFlower);
				System.out.println("6人加一色");
			}else if(mode==2){
				pai.addAll(threeFlower);
				pai.addAll(FourFlower);
				System.out.println("6人加两色");
			}
		}*/
		
		// 洗牌
		Collections.shuffle(pai);
		System.out.println(pai);
		return pai;
	}
	
	
	/**
	 * 发牌
	 * @param paiList
	 * @param playerCount
	 * @return
	 */
	public  List<String[]> faPai(List<String> paiList, int playerCount) {
		
		List<String[]> player=new ArrayList<String[]>();
		/*[4-4, 2-9, 1-9, 3-1, 2-8, 4-12, 2-11, 1-3, 2-4, 4-8, 4-2, 3-2, 1-7]
		 * [4-3, 1-1, 1-4, 2-13, 1-6, 2-6, 3-13, 3-8, 4-6, 2-2, 1-10, 3-4, 1-2]
		 * [3-3, 3-7, 2-10, 4-1, 4-10, 2-7, 1-12, 3-9, 4-9, 2-3, 4-11, 3-12, 4-13]
		 * [3-10, 3-5, 1-13, 1-5, 4-7, 1-8, 2-5, 3-6, 3-11, 2-12, 2-1, 4-5, 1-11]*/
		/*String[] a1={"2-13", "3-13", "1-12", "2-10", "3-10", "4-8", "2-6", "1-6", "2-5", "3-4", "1-3", "2-2", "4-2"};
		String[] b1={"4-1", "4-13", "1-12", "1-2", "2-3", "1-4", "2-5", "3-6", "2-8", "1-9", "1-10", "4-11", "3-12"};
		String[] c1={"3-13", "1-13", "3-1", "2-9", "2-9", "4-6", "3-6", "3-7", "2-12", "4-12", "4-5", "1-5", "2-3"};
		String[] d1={"2-11", "1-11", "2-1", "2-13", "1-13", "4-9", "2-6", "4-4", "4-7", "3-7", "1-7", "4-10", "3-10"};
		String[] e1={"2-1", "2-4", "2-7", "1-1", "1-4", "1-8", "1-9", "1-10", "3-1", "3-2", "3-3", "3-4", "3-11"};
		String[] f1={"1-1", "3-9", "1-3", "2-11", "2-10", "2-8", "2-7", "2-4", "3-8", "3-8", "3-5", "3-5", "3-2"};
		String[] g1={"2-12", "3-12", "3-9", "3-11", "1-11", "3-3", "4-3","2-2", "1-8", "1-7", "1-6", "1-5", "1-2"};
		player.add(a1);
		player.add(b1);
		player.add(c1);
		player.add(d1);
		player.add(e1);
		player.add(f1);
		player.add(g1);*/
		int paiIndex = 0;
		for (int i = 0; i < playerCount; i++) {
			String[] pai=new String[13];
			for (int j = 0; j < 13; j++) {
				pai[j] = paiList.get(paiIndex);
				paiIndex = paiIndex + 1;
			}
			player.add(pai);
		}
		return player;
	}
	
	/**
	 * 小到大
	 * @param card
	 * @return
	 */
	public static String[] daxiao(String[] list) {
		
		for (int i = 0; i < list.length; i++) {
			if (getValue(list[i])==1) {
				list[i]=list[i].replace("-1", "-14");
			}
		}
		
		
		for (int i = 0; i < list.length-1; i++) {
			
			for (int j = i+1; j < list.length; j++) {
				
				if (getValue(list[i])>getValue(list[j])) {
					String o=list[i];
					String o1=list[j];
					list[i]=o1;
					list[j]=o;
				}
			}
		}
		
		for (int i = 0; i < list.length; i++) {
			if (getValue(list[i])==14) {
				list[i]=list[i].replace("-14","-1");
			}
		}
		String[] dd=new String[13];
		int p=0;
		//倒序
		for (int i =list.length-1; i>-1 ; i--) {
			dd[p]=list[i];
			p++;
		}
		//System.out.println(list);
		return dd;
	}
	
	/**
	 * 传入牌组 已经要排序的 起始位置  结束位置
	 * @param list
	 * @param i1
	 * @param j1
	 * @return
	 */
	public static String[] AppointSort(String[] list,int i1,int j1) {
		String[] a=new String[j1-i1+1]; 
		int h=i1;
		for (int i = 0; i < a.length; i++) {
			if (i1<=j1) {
				a[i]=list[h];
				h++;
			}
		}
		
		JSONArray arr=new JSONArray();
		
    	for ( int i = 0; i < a.length; i++) {
    		arr.add(getValue(a[i]));
    	}
		JSONArray array=new JSONArray();
		JSONArray array1=new JSONArray();
		JSONArray array2=new JSONArray();
		JSONArray array3=new JSONArray();
		JSONArray array4=new JSONArray();
    	
    	for(int i=1;i<14;i++){
    		int count = 0;
    		for(int t = 0; t < arr.size(); t++){
    			if(arr.getInt(t)==i){
    				count++;
    			}
    		}
    		if(count==1){array1.add(i);}
    		if(count==2){array2.add(i);}
    		if(count==3){array3.add(i);}
    		if(count==4){array4.add(i);}
    	}
    	array.add(array1);
    	array.add(array2);
    	array.add(array3);
    	array.add(array4);
	
    	JSONArray array22=new JSONArray();
		for( int i = array.size()-1; i>=0; i--){
			JSONArray jsona=array.getJSONArray(i);
			for (int ii=0;ii<jsona.size()-1;ii++){
	    	    for (int jj=ii+1;jj<jsona.size();jj++)  
	    		{  
	    			int a1 =jsona.getInt(ii)==1?14:jsona.getInt(ii);
	    			int a2 =jsona.getInt(jj)==1?14:jsona.getInt(jj);
	    			if (a1>a2) // 这里是从大到小排序，如果是从小到大排序，只需将“<”换成“>”  
	    			{  
	    				int temp=jsona.getInt(ii);  
	    				jsona.element(ii, jsona.getInt(jj));
	    				jsona.element(jj,temp);
	    				
	    			};
	    		};
	    	};
			
			for (int j =jsona.size()-1; j >=0 ; j--) {
				for (int j2 = 0; j2 < a.length; j2++) {
					if(jsona.getInt(j)==getValue(a[j2])){
						array22.add(a[j2]);
						
					}
				}
			}
		}
		
		for (int i = 0; i <array22.size(); i++) {
			//list[i1]=array22.getString(i);
			if (i1<=j1) {
				list[i1]=array22.getString(i);
				i1++;
			}
			
		}
		
		return list;
		
	}
	/**
	 * 获得牌数值
	 * @param card
	 * @return
	 */
	public static int getValue(String card){
		int i= Integer.parseInt(card.substring(2,card.length()));
		return i;
	}
	
	
	
	
	public static void main(String[] args) {
		
		/*
		{"3-1","3-7","3-8","4-11","2-13","2-4","2-1","2-10","4-9","4-3","4-2","4-1","3-9"}
		{"3-9","3-4","4-3","3-1","4-6","4-1","2-6","1-8","3-7","2-7","3-2","2-10","1-1"}
		{"4-2","4-3","4-9","2-7","2-1","4-8","4-6","2-12","1-1","3-2","3-1","2-2","2-5"}
		{"1-12","3-7","1-9","1-1","2-11","3-1","1-10","2-9","2-2","4-4","2-1","3-3","4-8"}
		{"3-1", "3-7", "3-8", "4-11", "2-13", "2-4", "2-1", "2-10", "4-9", "4-3", "4-2", "4-1", "3-9"}
		{"3-9", "3-4", "4-3", "3-1", "4-6", "4-1", "2-6", "1-8", "3-7", "2-7", "3-2", "2-10", "1-1"}
		{"4-2", "4-3", "4-9", "2-7", "2-1", "4-8", "4-6", "2-12", "1-1", "3-2", "3-1", "2-2", "2-5"}
		{"1-12", "3-7", "1-9", "1-1", "2-11", "3-1", "1-10", "2-9", "2-2", "4-4", "2-1", "3-3", "4-8"}
		【发牌阶段】随用户：哎喲喂，用户手牌：[1-2, 1-2, 4-3, 3-3, 3-4, 2-4, 4-5, 1-5, 2-11, 2-12, 1-13, 3-1, 1-1]
		*/
		JSONObject obj= JSONObject.fromObject("{\"platform\":\"SDTQP\",\"maxplayer\":6,\"fangkapeipai\":180,\"goldpeipai\":70,\"goldready\":30,\"jiesan\":180,\"sameThirteen\":104,\"eightXian\":80,\"sevenStars\":40,\"sixDaSun\":20,\"ThreeEmFiveSo\":52,\"thirteen\":52,\"twelfth\":26,\"threeFlushByFlower\":26,\"threeBomb\":52,\"allBig\":6,\"allSmall\":6,\"oneColor\":6,\"twoGourd\":0,\"fourThree\":52,\"fiveThree\":6,\"sixPairs\":6,\"threeFlush\":6,\"threeFlower\":6}");
		String[] a1={"1-2", "1-2", "4-3", "3-3", "3-4", "2-4", "4-5","1-5", "2-11", "2-12", "1-13","3-1", "1-1"};
		String[] a2={"1-6", "3-6", "1-6", "1-6", "4-6", "3-1", "1-5", "4-3", "2-7", "2-7", "2-6", "2-11", "2-5"};
		String[] a3={"1-8", "3-8", "1-11", "1-8", "4-8", "3-1", "1-5", "4-8", "2-8", "2-8", "2-11", "2-11", "2-5"};
		String[] a4={"1-5", "3-5", "2-5", "1-8", "4-8", "4-5", "1-5", "4-3", "2-7", "2-7", "2-5", "3-5", "4-5"};
		String[] a5={"1-5", "3-4", "2-1", "1-2", "4-3", "4-6", "1-8", "4-9", "2-10", "2-7", "2-11", "3-12", "4-13"};
		String[] a6={"2-13", "3-13", "1-12", "2-10", "3-10", "4-8", "2-6", "1-6", "2-5", "3-4", "1-3", "2-2", "4-2"};//提示特殊牌 不是特殊牌
		/*String[] a3= daxiao(a1);*/
		//System.out.println(Arrays.toString(a3));
		int sc=SSSSpecialCards.isSpecialCards(a6,obj);
		String[] ss= SSSSpecialCardSort.CardSort(a6, sc);
		String s= SSSSpecialCards.getName(sc);
		int f= SSSSpecialCards.score(sc, obj);
		System.out.println(Arrays.toString(ss)+",ts:"+sc+",name:"+s+",score:"+f);
	/*	int sc2=SSSSpecialCards.isSpecialCards(a2,obj);
		String[] ss2= SSSSpecialCardSort.CardSort(a2, sc2);
		String s2= SSSSpecialCards.getName(sc2);
		int f2= SSSSpecialCards.score(sc2, obj);
		System.out.println(Arrays.toString(ss2)+",ts:"+sc2+",name:"+s2+",score:"+f2);
		int sc3=SSSSpecialCards.isSpecialCards(a3,obj);
		String[] ss3= SSSSpecialCardSort.CardSort(a3, sc3);
		String s3= SSSSpecialCards.getName(sc3);
		int f3= SSSSpecialCards.score(sc3, obj);
		System.out.println(Arrays.toString(ss3)+",ts:"+sc3+",name:"+s3+",score:"+f3);
		int sc4=SSSSpecialCards.isSpecialCards(a4,obj);
		String[] ss4= SSSSpecialCardSort.CardSort(a4, sc4);
		String s4= SSSSpecialCards.getName(sc4);
		int f4= SSSSpecialCards.score(sc4, obj);
		System.out.println(Arrays.toString(ss4)+",ts:"+sc4+",name:"+s4+",score:"+f4);
		int sc5=SSSSpecialCards.isSpecialCards(a5,obj);
		String[] ss5= SSSSpecialCardSort.CardSort(a5, sc5);
		String s5= SSSSpecialCards.getName(sc5);
		int f5= SSSSpecialCards.score(sc5, obj);
		System.out.println(Arrays.toString(ss5)+",ts:"+sc5+",name:"+s5+",score:"+f5);*/
		//JSONObject obj1=SSSComputeCards.compare(a1, a2);
		//System.out.println(obj1.toString());
	}

	public int getCloseTime() {
		return CloseTime;
	}

	public void setCloseTime(int closeTime) {
		CloseTime = closeTime;
	}

	public double getMinscore() {
		return minscore;
	}

	public void setMinscore(int minscore) {
		this.minscore = minscore;
	}

	public JSONObject getSetting() {
		return setting;
	}

	public void setSetting(JSONObject setting) {
		this.setting = setting;
	}

	public ConcurrentSkipListSet<String> getUserAcc() {
		return userAcc;
	}

	public void setUserAcc(ConcurrentSkipListSet<String> userAcc) {
		this.userAcc = userAcc;
	}

	public int getMaxplayer() {
		return maxplayer;
	}

	public void setMaxplayer(int maxplayer) {
		this.maxplayer = maxplayer;
	}

	public double getFee() {
		return fee;
	}

	public void setFee(double fee) {
		this.fee = fee;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public SaveLogsThreadSSS getSaveLogsThreadSSS() {
		return saveLogsThreadSSS;
	}

	public void setSaveLogsThreadSSS(SaveLogsThreadSSS saveLogsThreadSSS) {
		this.saveLogsThreadSSS = saveLogsThreadSSS;
	}

	public JSONObject getRoomobj() {
		return roomobj;
	}

	public void setRoomobj(JSONObject roomobj) {
		this.roomobj = roomobj;
	}


	public boolean isAllReady(){
		for (String uid:getPlayerPaiJu().keySet()) {
			int ready = getPlayerPaiJu().get(uid).getIsReady();
			if(ready!=1){
				return false;
			}
		}	
		return true;
	}



}
