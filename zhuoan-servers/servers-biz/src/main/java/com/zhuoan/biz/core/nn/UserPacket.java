package com.zhuoan.biz.core.nn;

import com.zhuoan.biz.model.UserPacketCommen;
import com.zhuoan.biz.model.nn.NNGameRoom;
import org.apache.commons.lang.math.RandomUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
public class UserPacket extends UserPacketCommen implements Serializable{
	
	private Packer[] ps = new Packer[5];//手里的5张牌
	public int type;//牌的类型  0:无牛，1~9:牛一~牛9，10:牛牛
	private boolean win = false;//是否赢了
	private boolean isBanker = false;//是否是庄家
	private int isReady;// 玩家准备状态
	private int status = 0;// 玩家游戏状态
	private double score = 0;//分数
	public int isCloseRoom = 0;//解散房间申请  0:未确认 1:同意  -1:拒绝
	public int[] mingPai;//明牌抢庄
	public int qzTimes = 0;//抢庄倍数
	public int luck;// 幸运值
    private int xzTimes;// 下注倍数

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getQzTimes() {
        return qzTimes;
    }

    public void setQzTimes(int qzTimes) {
        this.qzTimes = qzTimes;
    }

    public int getXzTimes() {
        return xzTimes;
    }

    public void setXzTimes(int xzTimes) {
        this.xzTimes = xzTimes;
    }

    // ===============================闲家推注开始===============================
	public int isBankerLast=-1;// 上局是否是庄家  -1:刚加入房间,0:不是,1:是
	public int typeLast = -1;// 上局牌型 -1:刚加入房间,0:无牛,1~9:牛一~牛九,10:牛牛,100:特殊牌型
	public int winLast = -1;// 上局输赢 -1:刚加入房间,0:输,1:赢
	public boolean isTuiZhuLast = false;// 上局是否已经推注
	public double scoreLast;// 上局下注金额
	public boolean tzChouma = true;// 是否选择特殊筹码
	private String baseNumTuiZhu;// 闲家推注下注列表
	
	public int getLuck() {
		return luck;
	}

	public void setLuck(int luck) {
		this.luck = luck;
	}

	public String getBaseNumTuiZhu() {
		return baseNumTuiZhu;
	}

	public void setBaseNumTuiZhu(String baseNumTuiZhu) {
		this.baseNumTuiZhu = baseNumTuiZhu;
	}

	public boolean isTzChouma() {
		return tzChouma;
	}

	public void setTzChouma(boolean tzChouma) {
		this.tzChouma = tzChouma;
	}

	public double getScoreLast() {
		return scoreLast;
	}

	public void setScoreLast(double scoreLast) {
		this.scoreLast = scoreLast;
	}

	public boolean isTuiZhuLast() {
		return isTuiZhuLast;
	}

	public void setTuiZhuLast(boolean isTuiZhuLast) {
		this.isTuiZhuLast = isTuiZhuLast;
	}

	public int getIsBankerLast() {
		return isBankerLast;
	}

	public void setIsBankerLast(int isBankerLast) {
		this.isBankerLast = isBankerLast;
	}

	public int getTypeLast() {
		return typeLast;
	}

	public void setTypeLast(int typeLast) {
		this.typeLast = typeLast;
	}

	public int getWinLast() {
		return winLast;
	}

	public void setWinLast(int winLast) {
		this.winLast = winLast;
	}

	// ===============================闲家推注结束===============================

	// 牌局统计数据
	private int tongShaTimes;
	private int tongPeiTimes;
	private int niuNiuTimes;
	private int wuNiuTimes;
	private int winTimes;
	
	
	/**
	 * 保存明牌抢庄的牌组
	 */
	public void saveMingPai() {
		
		int[] mypai = getMyPai();
		Arrays.sort(mypai);
		int paiIndex = RandomUtils.nextInt(4);
		// 隐藏的牌放到最后一张
		if(paiIndex < 4){
			int temp = mypai[paiIndex];
			mypai[paiIndex] = mypai[4];
			mypai[4] = temp;
		}
		this.mingPai = mypai;
	}
	
	/**
	 * 初始化牌局信息
	 */
	public void initUserPacket(){
		
		win=false;
		isReady=0;
		isCloseRoom=0;
        score = 0;
        xzTimes = 0;
        qzTimes = 0;
    }
	
	public UserPacket() {
	}

	public boolean isWin() {
		return win;
	}

	public void setWin(boolean win) {
		this.win = win;
	}

	public int getIsReady() {
		return isReady;
	}

	public void setIsReady(int isReady) {
		this.isReady = isReady;
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

	public boolean isBanker() {
		return isBanker;
	}

	public void setBanker(boolean isBanker) {
		this.isBanker = isBanker;
	}

	public Packer[] getPs() {
		return ps;
	}

	public void setPs(Packer[] ps) {
		this.ps = ps;
	}

	
	public int getTongShaTimes() {
		return tongShaTimes;
	}

	public void setTongShaTimes(int tongShaTimes) {
		this.tongShaTimes = tongShaTimes;
	}

	public int getTongPeiTimes() {
		return tongPeiTimes;
	}

	public void setTongPeiTimes(int tongPeiTimes) {
		this.tongPeiTimes = tongPeiTimes;
	}

	public int getNiuNiuTimes() {
		return niuNiuTimes;
	}

	public void setNiuNiuTimes(int niuNiuTimes) {
		this.niuNiuTimes = niuNiuTimes;
	}

	public int getWuNiuTimes() {
		return wuNiuTimes;
	}

	public void setWuNiuTimes(int wuNiuTimes) {
		this.wuNiuTimes = wuNiuTimes;
	}

	public int getWinTimes() {
		return winTimes;
	}

	public void setWinTimes(int winTimes) {
		this.winTimes = winTimes;
	}

	public int[] getMingPai() {
		return mingPai;
	}

	/**
	 * 判断是否是四炸 
	 * 炸弹：5张牌有4张是一样的
	 * @return
	 */
	public boolean isSiZha() {
		Packer[] newPs= Packer.sort(this.ps);
		//数组第二个值
		int max2=newPs[1].getNum().getNum();
		//数组倒数第二个
		int min3=newPs[3].getNum().getNum();
		//如果数组第二个值和数组最后一个值一样，或者数组倒数第二个值个第一个一样那么是4炸
		if(max2==newPs[4].getNum().getNum()||min3==newPs[0].getNum().getNum()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是否是葫芦牛
	 * @return
	 */
	public boolean isHuLuNiu() {
		
		Packer[] newPs=Packer.sort(this.ps);
		//如果数组第一个和第三个或者第二个和第四个或者第三个和第五个一样，那么就是葫芦牛
		if(newPs[0].getNum().getNum()==newPs[2].getNum().getNum()
				||newPs[1].getNum().getNum()==newPs[3].getNum().getNum()
				||newPs[2].getNum().getNum()==newPs[4].getNum().getNum()){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是否是五花牛
	 * 五花牛：5张牌都是10以上（不包含10）的牌
	 * @return
	 */
	public boolean isWuHuaNiu(){
		Packer[] newPs=Packer.sort(this.ps);
		//如果数组最小值是大于10，那么就是五花
		int min=newPs[0].getNum().getNum();
		if(min>10){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是否是四花牛
	 * 四花牛：5张牌中有4张牌大于10，另一张等于10
	 * @return
	 */
	public boolean isSiHuaNiu(){
		Packer[] newPs=Packer.sort(this.ps);
		//如果数组最小值等于10，其他大于10，那么就是四花
		int min=newPs[0].getNum().getNum();
		int secondMin=newPs[1].getNum().getNum();
		if(min==10 && secondMin>10){
			return true;
		}else{
			return false;
		}
	}

	/**
	 * 判断是否是五小牛
	 * 五小牛：5张牌点数加起来小于等于10
	 * @return
	 */
	public boolean isWuXiaoNiu(){
		
		int sum = 0;
		for (Packer packer : ps) {
			sum += packer.getNum().getNum();
		}
		if(sum <= 10){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * 判断是牛几
	 * @return
	 */
	public int isNiuNum(){
		int [] n=new int[5];
		for(int i=0;i<5;i++){
			if(ps[i].getNum().getNum()>10){
				n[i]=10;
			}else{
				n[i]=ps[i].getNum().getNum();
			}
		}
		Map<String, Boolean> map=isHasNiu(n);
		if(map.get("isNiuNiu")){
			return 10;
		}
		if(map.get("isNiuNum")){
			int num=0;
			for(int i:n){
				num+=i;
			}
			return num%10;
		}else{
			return 0;
		}
	}
	
	/**
	 * 判断是否有牛
	 * @param i
	 * @return
	 */
	private Map<String,Boolean> isHasNiu(int [] i){
		
		// 是否有牛
		boolean isNiuNum=false;
		// 是否是牛牛
		boolean isNiuNiu=false;
		for(int m=0;m<=2;m++){
			for(int n=m+1;n<=3;n++){
				for(int z=n+1;z<=4;z++){
					if((i[m]+i[n]+i[z])%10==0){
						isNiuNum=true;
						int num=0;
						for(int x=0;x<=4;x++){
							if(x!=m&&x!=n&&x!=z){
								num+=i[x];
							}
						}
						if(num%10==0){
							isNiuNiu=true;
						}
					}
				}
			}
		}
		Map<String,Boolean> result=new HashMap<String, Boolean>();
		result.put("isNiuNum", isNiuNum);
		result.put("isNiuNiu", isNiuNiu);
		return result;
	}
	
	public UserPacket(Packer [] ps, List<Integer> types){
		this(ps,false,types);
	}
	
	/**
	 * 构造方法
	 * @param ps 牌
	 * @param isBanker 是否是庄
	 */
	public UserPacket(Packer [] ps,boolean isBanker, List<Integer> types){
		
		this.ps=ps;
		this.isBanker=isBanker;
		
		if(types.contains(NiuNiu.SPECIALTYPE_ZHADANNIU) && isSiZha()){ // 炸弹
			
			this.type=NiuNiu.SPECIALTYPE_ZHADANNIU;
			
		}else if(types.contains(NiuNiu.SPECIALTYPE_WUHUANIU) && isWuHuaNiu()){ // 五花牛
			
			this.type=NiuNiu.SPECIALTYPE_WUHUANIU;
			
		}else if(types.contains(NiuNiu.SPECIALTYPE_SIHUANIU) && isSiHuaNiu()){ // 四花牛
			
			this.type=NiuNiu.SPECIALTYPE_SIHUANIU;
			
		}else if(types.contains(NiuNiu.SPECIALTYPE_WUXIAONIU) && isWuXiaoNiu()){ // 五小牛
			
			this.type=NiuNiu.SPECIALTYPE_WUXIAONIU;
			
		}else{ // 普通牌型
			
			this.type=isNiuNum();
			
			if(this.type==10){
                // 葫芦牛
				if(types.contains(NiuNiu.SPECIALTYPE_HULUNIU) && isHuLuNiu()){
					this.type=NiuNiu.SPECIALTYPE_HULUNIU;
				}
			}
		}
	}


	/**
	 * 倍率计算
	 * @return
	 */
	public int getRatio(NNGameRoom room){
		return room.ratio.get(this.type);
	}
	
	
	/**
	 * 获取玩家手牌
	 * @return
	 */
	public int[] getMyPai(){
		
		int[] pais = new int[ps.length];
		if(ps[0]!=null){
			
			for (int i = 0; i < pais.length; i++) {
				int num = 0;
				if(ps[i].getColor().equals(Color.HONGTAO)){
					num = 20;
				}else if(ps[i].getColor().equals(Color.MEIHAU)){
					num = 40;
				}else if(ps[i].getColor().equals(Color.FANGKUAI)){
					num = 60;
				}
				pais[i] = ps[i].getNum().getNum() + num;
			}
		}
		return pais;
	}
	
	
	/**
	 * 经过排序整理过的牌
	 * @return
	 */
	public int[] getSortPai(){
		
		int[] pais = getMyPai();
		// 取得牌的数值
		for (int i = 0; i < pais.length; i++) {
			int number = pais[i]%20;
			if(number>10){
				pais[i] = 10;
			}else{
				pais[i] = number;
			}
		}
		int[] pindex = new int[pais.length];
		boolean isSort = false;
		for(int m=0;m<=2;m++){
			for(int n=m+1;n<=3;n++){
				for(int z=n+1;z<=4;z++){
					// 判断是否有牛
					if((pais[m]+pais[n]+pais[z])%10==0){

						pindex[0] = m;
						pindex[1] = n;
						pindex[2] = z;
						
						pais[m] = 0;
						pais[n] = 0;
						pais[z] = 0;
						
						int paiindex = 3;
						for (int i = 0; i < pais.length; i++) {
							if(pais[i]>0){
								pindex[paiindex] = i;
								paiindex++;
							}
						}
						
						// 排序完成，跳出循环
						m=n=z=5;
						isSort = true;
					}
				}
			}
		}
		
		int[] myPai = getMyPai();
		if(isSort){
			int[] newPais = new int[myPai.length];
			for (int i = 0; i < pindex.length; i++) {
				int ii = pindex[i];
				newPais[i] = myPai[ii];
			}
			return newPais;
		}
		return myPai;
	}

	
	/**
	 * 玩家手动配牛，判断是否有牛
	 * @param pais
	 * @return 
	 */
	public boolean peiNiu(int[] pais) {
		
		if(pais.length>0){
			
			int sum = 0;
			for (int p : pais) {
				int val = p%20;
				if(val>10){
					val = 10;
				}
				sum+=val;
			}
			
			if(sum%10==0){
				
				return true;
			}
		}
		return false;
	}
	
}
