package com.zhuoan.biz.model.qzmj;

import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class QZMJGameRoom extends GameRoom{

	/**
	 * 游金倍数
	 */
	private int youJinScore;
	/**
	 * 手牌数量
	 */
	private int paiCount;
	/**
	 * 庄家下标
	 */
	private int bankerIndex;
	/**
	 * 金
	 */
	private int jin;
	/**
	 * 连庄局数
	 */
	private int bankerTimes = 1;
	/**
	 * 游金类型
	 */
	private int yjType;
	/**
	 * 游金玩家
	 */
	private String yjAccount;
	/**
	 * 牌
	 */
	private int pai[];
	/**
	 * 下一张牌
	 */
	private int index;
	/**
	 * 上一张摸的牌
	 */
	private int lastMoPai;
	/**
	 * 上一个摸牌玩家
	 */
	private String lastMoAccount;
	/**
	 * 上一张牌
	 */
	private int lastPai;
	/**
	 * 上一个出牌玩家
	 */
	private String lastAccount;
	/**
	 * 本次操作的玩家
	 */
	private String thisAccount;
	/**
	 * 本次操作的类别
	 */
	private int thisType;
	/**
	 * 下个询问的玩家
	 */
	private String nextAskAccount;
	/**
	 *下次询问的类别
	 */
	private int nextAskType;
	/**
	 * 赢家
	 */
	private String winner;
	/**
	 * 玩家牌局信息
	 */
	private ConcurrentHashMap<String, UserPacketQZMJ> userPacketMap = new ConcurrentHashMap<String, UserPacketQZMJ>();
	/**
	 * 开局记录
	 */
	private List<KaiJuModel> kaiJuList = new ArrayList<KaiJuModel>();
	/**
	 * 骰子
	 */
	private int[] dice;
    /**
     * 胡牌类型
     */
    private int huType;
	/**
	 * 是否光游
	 */
	public boolean isGuangYou;
	/**
	 * 有金不平胡
	 */
	public boolean hasJinNoPingHu;
	/**
	 * 一课牌局积分是否可以超出（负数）
	 */
	public boolean isCanOver;
	/**
	 * 是否没有吃、胡
	 */
	public boolean isNotChiHu;
	/**
	 * 游戏是否结束
	 */
	public boolean isGameOver = false;
    /**
     * 开局状态
     */
	private int startStatus;

    public int getStartStatus() {
        return startStatus;
    }

    public void setStartStatus(int startStatus) {
        this.startStatus = startStatus;
    }

    public int getYouJinScore() {
		return youJinScore;
	}
	public int getPaiCount() {
		return paiCount;
	}
	public int getBankerIndex() {
		return bankerIndex;
	}
	public int getJin() {
		return jin;
	}
	public int getBankerTimes() {
		return bankerTimes;
	}
	public int getYjType() {
		return yjType;
	}
	public String getYjAccount() {
		return yjAccount;
	}
	public int[] getPai() {
		return pai;
	}
	public int getIndex() {
		return index;
	}
	public int getLastMoPai() {
		return lastMoPai;
	}
	public String getLastMoAccount() {
		return lastMoAccount;
	}
	public int getLastPai() {
		return lastPai;
	}
	public String getLastAccount() {
		return lastAccount;
	}
	public String getThisAccount() {
		return thisAccount;
	}
	public int getThisType() {
		return thisType;
	}
	public String getNextAskAccount() {
		return nextAskAccount;
	}
	public int getNextAskType() {
		return nextAskType;
	}
	public String getWinner() {
		return winner;
	}
	public ConcurrentHashMap<String, UserPacketQZMJ> getUserPacketMap() {
		return userPacketMap;
	}
	public List<KaiJuModel> getKaiJuList() {
		return kaiJuList;
	}
	public int[] getDice() {
		return dice;
	}
	public void setYouJinScore(int youJinScore) {
		this.youJinScore = youJinScore;
	}
	public void setPaiCount(int paiCount) {
		this.paiCount = paiCount;
	}
	public void setBankerIndex(int bankerIndex) {
		this.bankerIndex = bankerIndex;
	}
	public void setJin(int jin) {
		this.jin = jin;
	}
	public void setBankerTimes(int bankerTimes) {
		this.bankerTimes = bankerTimes;
	}
	public void setYjType(int yjType) {
		this.yjType = yjType;
	}
	public void setYjAccount(String yjAccount) {
		this.yjAccount = yjAccount;
	}
	public void setPai(int[] pai) {
		this.pai = pai;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public void setLastMoPai(int lastMoPai) {
		this.lastMoPai = lastMoPai;
	}
	public void setLastMoAccount(String lastMoAccount) {
		this.lastMoAccount = lastMoAccount;
	}
	public void setLastPai(int lastPai) {
		this.lastPai = lastPai;
	}
	public void setLastAccount(String lastAccount) {
		this.lastAccount = lastAccount;
	}
	public void setThisAccount(String thisAccount) {
		this.thisAccount = thisAccount;
	}
	public void setThisType(int thisType) {
		this.thisType = thisType;
	}
	public void setNextAskAccount(String nextAskAccount) {
		this.nextAskAccount = nextAskAccount;
	}
	public void setNextAskType(int nextAskType) {
		this.nextAskType = nextAskType;
	}
	public void setWinner(String winner) {
		this.winner = winner;
	}
	public void setUserPacketMap(
			ConcurrentHashMap<String, UserPacketQZMJ> userPackerMap) {
		this.userPacketMap = userPackerMap;
	}
	public void setKaiJuList(List<KaiJuModel> kaiJuList) {
		this.kaiJuList = kaiJuList;
	}
	public void setDice(int[] dice) {
		this.dice = dice;
	}

    public int getHuType() {
        return huType;
    }

    public void setHuType(int huType) {
        this.huType = huType;
    }

    public void initGame() {
        //游金类型  1：单游 2：双游 3：三游
        this.yjType = 0;
        //游金玩家
        this.yjAccount = null;
        // 胡牌类型
        this.huType = 0;
        this.startStatus = 0;
        this.kaiJuList = new ArrayList<KaiJuModel>();
        this.lastMoPai = 0;
        getSummaryData().clear();
        setGameIndex(getGameIndex()+1);
        for (String uuid : getUserPacketMap().keySet()) {
            if (getUserPacketMap().containsKey(uuid)&&getUserPacketMap().get(uuid)!=null) {
                getUserPacketMap().get(uuid).initUserPacket();
            }
        }
    }

    /**
     * 确定庄家
     * @return
     */
	public boolean choiceBanker() {
		/*
		 * 1.庄家在房间内取庄家
		 * 2.房主在房间内取房主
		 * 3.庄家房主均不在随机庄家
		 * 4.设置开局询问
		 */
		String bankerAccount = null;
		if (checkAccount(getBanker())) {
			bankerAccount = getBanker();
		}else if (checkAccount(getOwner())) {
			bankerAccount = getOwner();
		}else if (getPlayerMap().size()>0) {
			for (String account : getPlayerMap().keySet()) {
			    if (getPlayerMap().containsKey(account)&&getPlayerMap().get(account)!=null) {
                    bankerAccount = account;
                    break;
                }
			}
		}
		if (!Dto.stringIsNULL(bankerAccount)) {
            setBanker(bankerAccount);
            thisType = 1;
			thisAccount = bankerAccount;
			return true;
		}
		return false;
	}

    /**
     * 摇筛子
     */
	public void obtainDice() {
        int[] dices = new int[2];
        dices[0] = RandomUtils.nextInt(6)+1;
        dices[1] = RandomUtils.nextInt(6)+1;
        setDice(dices);
    }


    /**
     * 洗牌
     */
	public void shufflePai() {
		/*
		 * 1.打乱牌下标,设置牌组
		 * 2.设置下一张牌的下标
		 */
		int[] indexs = randomPai(QZMJConstant.ALL_PAI.length);
		int[] newPais = new int[QZMJConstant.ALL_PAI.length];

		for (int i = 0; i < indexs.length; i++) {
			
			newPais[i] = QZMJConstant.ALL_PAI[indexs[i]];
		}
		setPai(newPais);
		this.index = 0;
	}

    /**
     * 开金
     */
	public void choiceJin() {
		/*
		 * 1.从牌堆随机一张金牌
		 * 2.设置金牌
		 */
		Random rd = new Random();
		while(true){
			// 开金选择在不可动的牌堆里开（最后16张）
			int index = QZMJConstant.ALL_PAI.length - rd.nextInt(QZMJConstant.LEFT_PAI_COUNT) - 1;
			// 如果为花牌重新随机
			if(!QZMJConstant.isHuaPai(this.pai[index])){
				// 设置金牌
				setJin(this.pai[index]);
				break;
			}
		}
	}

    /**
     * 发牌
     */
	public void faPai() {
	    /*
		 * 1.设置玩家手牌
		 * 2.添加游戏记录
		 */
		for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().containsKey(account)&&getUserPacketMap().get(account)!=null) {
                int lastIndex = index+paiCount;
                int[] myPai=Arrays.copyOfRange(pai, index, lastIndex);
                index = lastIndex;
                getUserPacketMap().get(account).setMyPai(myPai);
                //记录发牌记录
                addKaijuList(getPlayerIndex(account), 10,myPai);
            }
		}
	}
	
	/**
	 * 打乱牌的下标
	 * @param @param paiCount
	 * @param @return   
	 * @return int[]  
	 * @throws
	 * @date 2018年5月21日
	 */
	private int[] randomPai(int paiCount){
		int[] nums = new int[paiCount];
		Random rd = new Random();
		for (int i = 0; i < nums.length; i++) {
			while(true){
				int num = rd.nextInt(paiCount);
				if(!ArrayUtils.contains(nums,num)){
					nums[i] = num;
					break;
				}else if(num==0){ //若是0，判断之前是否已存在
					if(ArrayUtils.indexOf(nums, num) == i){
						break;
					}
				}
			}
		}
		return nums;
	}
	
	/**
	 * 根据账号判断玩家是否在房间内
	 * @param @param account
	 * @param @return   
	 * @return boolean  
	 * @throws
	 * @date 2018年5月21日
	 */
	public boolean checkAccount(String account) {
		if (Dto.stringIsNULL(account)) {
			return false;
		}
		if (!getPlayerMap().containsKey(account)) {
			return false;
		}
		if (getPlayerMap().get(account)==null) {
			return false;
		}
		return true;
	}

    /**
     * 获取最后一张桌面的牌
     * @return
     */
    public KaiJuModel getLastZhuoPaiValue() {

        if(kaiJuList!=null&&kaiJuList.size()>0){
            for(int i=kaiJuList.size()-1;i>=0;i--){
                KaiJuModel jl=kaiJuList.get(i);
                if(jl.getType()==0&&jl.getShowType()==0){
                    return jl;
                }
            }
        }
        return null;
    }

    /**
     * 获取玩家的出牌记录
     * @return
     */
    public JSONArray getChuPaiJiLu(int index){

        List<Integer> jlList = new ArrayList<Integer>();
        for (KaiJuModel jl : this.kaiJuList) {
            if(jl.getType()==0 && jl.getIndex()==index && jl.getShowType()==0){
                jlList.add(jl.getValues()[0]);
            }
        }
        return JSONArray.fromObject(jlList);
    }

	/**
	 * 获取玩家的位置
	 * @return
	 */
	public int getPlayerIndex(String account){
		if(!Dto.stringIsNULL(account) && getPlayerMap().get(account)!=null){
			return getPlayerMap().get(account).getMyIndex();
		}else{
			return -1;
		}
	}

    /**
     * 自动设置下一个焦点人（抓牌，或者出牌）
     * @return
     */
    public void setNextThisUUID(){
        this.thisAccount = getNextPlayer(this.thisAccount);
    }

    /**
     * 当前玩家游金类型
     * @param youJinType
     * @return
     */
    public boolean hasYouJinType(int youJinType) {
        for(String uuid:getUserPacketMap().keySet()) {
            if (getUserPacketMap().containsKey(uuid)&&getUserPacketMap().get(uuid)!=null) {
                UserPacketQZMJ up = getUserPacketMap().get(uuid);
                if(up!=null&&up.getYouJinIng()>=youJinType){
                    return true;
                }
            }
        }
        return false;
    }

	/**
	 * 添加出牌纪录
	 * @return
	 */
	public void addKaijuList(int index,int type,int[] values){
		// 常量定义-todo
        // 本次操作是吃杠碰
		if(type==4||type==5||type==6){
			if(this.kaiJuList.size()>0){
				KaiJuModel jilu = this.kaiJuList.get(this.kaiJuList.size()-1);
                // 上次操作是出牌事件
				if(jilu.getType()==0){
				    // 隐藏吃碰杠后的牌
					jilu.setShowType(1);
				}
			}
		}
		KaiJuModel kj = new KaiJuModel(index, type, values);
		this.kaiJuList.add(kj);
	}

    /**
     * 最后一次杠操作记录
     * @return
     */
    public KaiJuModel getLastValue() {

        if(kaiJuList!=null&&kaiJuList.size()>0){
            for(int i=kaiJuList.size()-1;i>=0;i--){
                KaiJuModel jl=kaiJuList.get(i);
                if(jl.getType()==2){
                    //暗杠
                    return jl;
                }else if(jl.getType()==9){
                    //抓杠
                    return jl;
                }else if(jl.getType()==6){
                    //明杠
                    return jl;
                }else if(jl.getType()==0){
                    //出牌
                    return new KaiJuModel(jl.getIndex(), 1, jl.getValues());
                }
            }
        }
        return null;
    }

    /**
     * 获取上一次操作的index
     * @return
     */
    public int getLastFocus() {
        if(kaiJuList!=null&&kaiJuList.size()-1>0){
            for(int i=kaiJuList.size()-1;i>=0;i--){
                KaiJuModel jl=kaiJuList.get(i);
                if(jl.getType()!=1){
                    //暗杠
                    return jl.getIndex();
                }
            }
        }
        return getPlayerIndex(getBanker());
    }

	/**
	 * 获取当前游戏焦点指针
	 * @return
	 */
	public int getFocusIndex(){
		
		// 获取最后一次操作记录
		KaiJuModel kaijujl=getLastKaiJuValue(-1);
		if(kaijujl!=null){
			int jltype = kaijujl.getType();
			// 需要出牌-todo 常量定义
			if(jltype==0||jltype==1||jltype==2||jltype==4||jltype==5||jltype==6||jltype==9){ 
				return kaijujl.getIndex();
			}else if(jltype==11){
                // 摸牌次数
                int moPaiConut = getActionTimes(kaijujl.getIndex(), 1);
				if(moPaiConut>0){
					return kaijujl.getIndex();
				}
			}
		}
		return getPlayerIndex(getBanker());
	}
	
	/**
	 * 获取玩家操作次数（摸牌、出牌、吃、杠、碰、补花等）
	 * @param userIndex 玩家下标
	 * @param actType 操作类型
	 * @return
	 */
	public int getActionTimes(int userIndex, int actType){
		
		// 操作次数
		int actionTimes = 0; 
		for (KaiJuModel kaiJu : getKaiJuList()) {
			
			if(userIndex!=-1 && actType!=-1){
				
				if(kaiJu.getType()==actType && kaiJu.getIndex()==userIndex){
					actionTimes ++;
				}
			}else if(userIndex!=-1){
				
				if(kaiJu.getIndex()==userIndex){
					actionTimes ++;
				}
			}else if(actType!=-1){
				
				if(kaiJu.getType()==actType){
					actionTimes ++;
				}
			}
		}
		return actionTimes;
	}
	
	/**
	 * 获取最后一次操作记录
	 * @param type 操作类型（-1表示直接取最后一条记录）
	 * @return
	 */
	public KaiJuModel getLastKaiJuValue(int type) {
		
		if(kaiJuList !=null&& kaiJuList.size()>0){
			for(int i = kaiJuList.size()-1; i>=0; i--){
				KaiJuModel jl= kaiJuList.get(i);
				if(type==-1){
					return jl;
				}else if(jl.getType()==type){
					return jl;
				}
			}
		}
		return null;
	}
	
	/**
	 * 获取玩家最后一张出的牌的位置
	 * @return
	 */
	public int getNowPoint(){
		
		for (KaiJuModel kaijujl : getKaiJuList()) {
			if(kaijujl!=null){
				int jltype = kaijujl.getType();
				// 出牌记录
				if(jltype==0){
					if(kaijujl.getShowType()!=1){
						return kaijujl.getIndex();
					}else{
						return -1;
					}
				}
			}
		}
		return -1;
	}

    /**
     * 获取当前玩家的下家的UUID
     * @return
     */
    public String getNextPlayer(String account){

        int index=0;
        if(getPlayerMap().get(account)!=null){
            index = getPlayerMap().get(account).getMyIndex();
        }
        int next = 0;
        // 两人局，坐对面
        if(getPlayerCount()==2){
            next=index+2;
            if(next>=getPlayerMap().size()*2){
                next=next-getPlayerMap().size()*2;
            }
        }else{
            next=index+1;
            if(next>=getPlayerMap().size()){
                next=next-getPlayerMap().size();
            }
        }
        for (String uuid : getPlayerMap().keySet()) {
            if (getPlayerMap().containsKey(account)&&getPlayerMap().get(account)!=null) {
                if(next==getPlayerMap().get(uuid).getMyIndex()){
                    return uuid;
                }
            }
        }
        return getBanker();
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
                UserPacketQZMJ up = getUserPacketMap().get(uuid);
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
                obj.put("isTrustee", up.getIsTrustee());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 获取解散数据
     * @return
     */
    public JSONArray getCloseRoomData(){
        JSONArray array = new JSONArray();
        for (String account : userPacketMap.keySet()) {
            if (userPacketMap.containsKey(account)&&userPacketMap.get(account)!=null) {
                JSONObject obj = new JSONObject();
                obj.put("index",getPlayerMap().get(account).getMyIndex());
                obj.put("result",userPacketMap.get(account).getIsCloseRoom());
                obj.put("name",getPlayerMap().get(account).getName());
                obj.put("jiesanTimer",getJieSanTime());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 计算分数
     * @param huType 胡的类型
     * @param difen 底分
     * @param fan 总番数
     * @param youjin 游金倍数（3倍或者4倍）
     * @return
     */
    public static int jiSuanScore(int huType, int difen, int fan, int youjin){

        int score = 0;
        /**
         *  点炮和：(底＋盘数)
         自摸：[(底＋盘数)×2]
         游金 ：[(底＋盘数)×3]
         三金倒 ：[(底＋盘数)×3]
         双游：[(底＋盘数)×6]
         三游：[(底＋盘数)×12]
         */
        if(huType == QZMJConstant.HU_TYPE_PH){

            score = (difen + fan)*QZMJConstant.SCORE_TYPE_PH;

        }else if(huType == QZMJConstant.HU_TYPE_ZM){

            score = (difen + fan)*QZMJConstant.SCORE_TYPE_ZM;

        }else if(huType == QZMJConstant.HU_TYPE_YJ){

            if(youjin==3){
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_YJ_THREE;
            }else{
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_YJ_FOUR;
            }

        }else if(huType == QZMJConstant.HU_TYPE_SJD){

            score = (difen + fan)*QZMJConstant.SCORE_TYPE_SJD;

        }else if(huType == QZMJConstant.HU_TYPE_SHY){

            if(youjin==3){
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_SHY_THREE;
            }else{
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_SHY_FOUR;
            }

        }else if(huType == QZMJConstant.HU_TYPE_SY){

            if(youjin==3){
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_SY_THREE;
            }else{
                score = (difen + fan)*QZMJConstant.SCORE_TYPE_SY_FOUR;
            }

        }
        return score;
    }

    /**
     * 增加连庄数
     */
    public void addBankTimes() {
        bankerTimes++;
    }

    public boolean isAllReady() {
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().containsKey(account)&&getUserPacketMap().get(account)!=null) {
                if (getUserPacketMap().get(account).getStatus()!=QZMJConstant.QZ_USER_STATUS_READY) {
                    return false;
                }
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
            if (getUserPacketMap().containsKey(account)&&getUserPacketMap().get(account)!=null) {
                if (userPacketMap.get(account).getIsCloseRoom()!= CommonConstant.CLOSE_ROOM_AGREE) {
                    return false;
                }
            }
        }
        return true;
    }
}
