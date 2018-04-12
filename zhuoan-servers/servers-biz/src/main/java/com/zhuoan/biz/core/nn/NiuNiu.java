package com.zhuoan.biz.core.nn;

import org.apache.commons.lang.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 牛牛
 */
public class NiuNiu {
	
	/**
	 * 特殊牌型（从小到大）
	 */
	public static int SPECIALTYPE_SIHUANIU = 19; // 四花牛
	public static int SPECIALTYPE_WUHUANIU = 20; // 五花牛
	public static int SPECIALTYPE_HULUNIU = 30; // 葫芦牛
	public static int SPECIALTYPE_ZHADANNIU = 40; // 炸弹牛
	public static int SPECIALTYPE_WUXIAONIU = 50; // 五小牛
	
	/**
	 * 牌的下标
	 */
	private static int paiIndex = 0;
	
	/**
	 * 每局玩家人数
	 */
	public static int PLAYERCOUNT = 6;
	
	
	/**
	 * 牌局抢庄阶段
	 */
	public static int GAMESTATUS_QIANGZHUANG = 10;
	/**
	 * 牌局准备（初始）阶段
	 */
	public static int GAMESTATUS_READY = 0;
	/**
	 * 牌局下注阶段
	 */
	public static int GAMESTATUS_XIAZHU = 1;
	/**
	 * 牌局结算阶段
	 */
	public static int GAMESTATUS_JIESUAN = 2;
	/**
	 * 牌局亮牌阶段
	 */
	public static int GAMESTATUS_LIANGPAI = 3;
	
	/**
	 * 玩家状态（初始）
	 */
	public static int USERPACKER_STATUS_CHUSHI = 0;
	/**
	 * 玩家状态（发牌）
	 */
	public static int USERPACKER_STATUS_FAPAI = 1;
	/**
	 * 玩家状态（亮牌）
	 */
	public static int USERPACKER_STATUS_LIANGPAI = 2;
	
	/**
	 * 上局状态（是）
	 */
	public static int USERPACKER_LAST_YES = 1;
	
	/**
	 * 上局状态（否）
	 */
	public static int USERPACKER_LAST_NO = 0;
	
	/**
	 * 观战玩家下标
	 */
	public static int USERPACKER_STATUS_GUANZHAN = -1;
	
	
	/**
	 * 牌面
	 */
	private static Packer[] initPai(){
		
		Packer[] p=new Packer[52];
		// 黑桃
		p[0]=new Packer(Num.P_A,Color.HEITAO);
		p[1]=new Packer(Num.P_2,Color.HEITAO);
		p[2]=new Packer(Num.P_3,Color.HEITAO);
		p[3]=new Packer(Num.P_4,Color.HEITAO);
		p[4]=new Packer(Num.P_5,Color.HEITAO);
		p[5]=new Packer(Num.P_6,Color.HEITAO);
		p[6]=new Packer(Num.P_7,Color.HEITAO);
		p[7]=new Packer(Num.P_8,Color.HEITAO);
		p[8]=new Packer(Num.P_9,Color.HEITAO);
		p[9]=new Packer(Num.P_10,Color.HEITAO);
		p[10]=new Packer(Num.P_J,Color.HEITAO);
		p[11]=new Packer(Num.P_Q,Color.HEITAO);
		p[12]=new Packer(Num.P_K,Color.HEITAO);
		// 红桃
		p[13]=new Packer(Num.P_A,Color.HONGTAO);
		p[14]=new Packer(Num.P_2,Color.HONGTAO);
		p[15]=new Packer(Num.P_3,Color.HONGTAO);
		p[16]=new Packer(Num.P_4,Color.HONGTAO);
		p[17]=new Packer(Num.P_5,Color.HONGTAO);
		p[18]=new Packer(Num.P_6,Color.HONGTAO);
		p[19]=new Packer(Num.P_7,Color.HONGTAO);
		p[20]=new Packer(Num.P_8,Color.HONGTAO);
		p[21]=new Packer(Num.P_9,Color.HONGTAO);
		p[22]=new Packer(Num.P_10,Color.HONGTAO);
		p[23]=new Packer(Num.P_J,Color.HONGTAO);
		p[24]=new Packer(Num.P_Q,Color.HONGTAO);
		p[25]=new Packer(Num.P_K,Color.HONGTAO);
		// 梅花
		p[26]=new Packer(Num.P_A,Color.MEIHAU);
		p[27]=new Packer(Num.P_2,Color.MEIHAU);
		p[28]=new Packer(Num.P_3,Color.MEIHAU);
		p[29]=new Packer(Num.P_4,Color.MEIHAU);
		p[30]=new Packer(Num.P_5,Color.MEIHAU);
		p[31]=new Packer(Num.P_6,Color.MEIHAU);
		p[32]=new Packer(Num.P_7,Color.MEIHAU);
		p[33]=new Packer(Num.P_8,Color.MEIHAU);
		p[34]=new Packer(Num.P_9,Color.MEIHAU);
		p[35]=new Packer(Num.P_10,Color.MEIHAU);
		p[36]=new Packer(Num.P_J,Color.MEIHAU);
		p[37]=new Packer(Num.P_Q,Color.MEIHAU);
		p[38]=new Packer(Num.P_K,Color.MEIHAU);
		// 方块
		p[39]=new Packer(Num.P_A,Color.FANGKUAI);
		p[40]=new Packer(Num.P_2,Color.FANGKUAI);
		p[41]=new Packer(Num.P_3,Color.FANGKUAI);
		p[42]=new Packer(Num.P_4,Color.FANGKUAI);
		p[43]=new Packer(Num.P_5,Color.FANGKUAI);
		p[44]=new Packer(Num.P_6,Color.FANGKUAI);
		p[45]=new Packer(Num.P_7,Color.FANGKUAI);
		p[46]=new Packer(Num.P_8,Color.FANGKUAI);
		p[47]=new Packer(Num.P_9,Color.FANGKUAI);
		p[48]=new Packer(Num.P_10,Color.FANGKUAI);
		p[49]=new Packer(Num.P_J,Color.FANGKUAI);
		p[50]=new Packer(Num.P_Q,Color.FANGKUAI);
		p[51]=new Packer(Num.P_K,Color.FANGKUAI);
		
		return p;
	}
	
	/**
	 * 洗牌
	 * @return 
	 */
	public static Packer[] xiPai(){
		
		Packer[] pais = initPai();
		int[] indexs = randomPai();
		Packer[] newPais = new Packer[pais.length];
		for (int i = 0; i < indexs.length; i++) {
			
			newPais[i] = pais[indexs[i]];
		}
		return newPais;
	}
	
	
	/**
	 * 打乱牌的下标
	 * @return
	 */
	private static int[] randomPai(){
		
		int[] nums = new int[52];
		Random rd = new Random();
		for (int i = 0; i < nums.length; i++) {
			while(true){
				int num = rd.nextInt(52);
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
	 * 发牌
	 * @param pais
	 * @param userCount
	 * @return
	 */
	public static List<UserPacket> faPai(Packer[] pais, int userCount, List<Integer> types){
		
		List<UserPacket> userPacketList = new ArrayList<UserPacket>();
		paiIndex = 0;
		for (int i = 0; i < userCount; i++) {
			
			Packer[] pai=new Packer[5];
			for (int j = 0; j < 5; j++) {
				pai[j] = pais[paiIndex];
				paiIndex = paiIndex + 1;
			}
			UserPacket userPacket = new UserPacket(pai,types);
			userPacketList.add(userPacket);
		}
		return userPacketList;
	}
	
	
	public static void main(String[] args) {
		
		List<Integer> types =  new ArrayList<Integer>();
		types.add(NiuNiu.SPECIALTYPE_HULUNIU);
		types.add(NiuNiu.SPECIALTYPE_SIHUANIU);
		types.add(NiuNiu.SPECIALTYPE_WUHUANIU);
		types.add(NiuNiu.SPECIALTYPE_WUXIAONIU);
		types.add(NiuNiu.SPECIALTYPE_ZHADANNIU);
		
		Packer[] p=new Packer[5];
		p[0]=new Packer(Num.P_6,Color.HONGTAO);
		p[1]=new Packer(Num.P_K,Color.FANGKUAI);
		p[2]=new Packer(Num.P_K,Color.HONGTAO);
		p[3]=new Packer(Num.P_J,Color.HEITAO);
		p[4]=new Packer(Num.P_4,Color.FANGKUAI);
		
		UserPacket up=new UserPacket(p,true,types);
		
		Packer[] p1=new Packer[5];
		p1[0]=new Packer(Num.P_10,Color.FANGKUAI);
		p1[1]=new Packer(Num.P_6,Color.MEIHAU);
		p1[2]=new Packer(Num.P_K,Color.MEIHAU);
		p1[3]=new Packer(Num.P_4,Color.HEITAO);
		p1[4]=new Packer(Num.P_10,Color.MEIHAU);
		
		UserPacket up1=new UserPacket(p1,types);
		
		UserPacket u = PackerCompare.getWin(up1, up);
		
		System.out.println(up.isWin());
		
		System.out.println(u.type);
		
	}
	
}
