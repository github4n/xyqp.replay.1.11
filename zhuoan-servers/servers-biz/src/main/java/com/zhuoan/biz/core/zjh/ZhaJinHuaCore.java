package com.zhuoan.biz.core.zjh;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 炸金花
 * @author lhp
 *规则：（豹子》同花顺》金花》顺子》对子》散牌）
 */
public class ZhaJinHuaCore {

    /**
     * 1方块  21梅花 41红心  61黑桃
     */
	public static int[] PAIS = new int[]{1,2,3,4,5,6,7,8,9,10,11,12,13,
						   21,22,23,24,25,26,27,28,29,30,31,32,33,
						   41,42,43,44,45,46,47,48,49,50,51,52,53,
						   61,62,63,64,65,66,67,68,69,70,71,72,73};

    /**
     * 1方块  21梅花 41红心  61黑桃（激情模式）
     */
	public static int[] PAIS_JQ = new int[]{1,9,10,11,12,13,
							   			   21,29,30,31,32,33,
							   			   41,49,50,51,52,53,
							   			   61,69,70,71,72,73};

    /**
     * 豹子
     */
	public static int TYPE_BAOZI = 106;
    /**
     * 同花顺
     */
	public static int TYPE_TONGHUASHUN = 105;
    /**
     * 金花
     */
	public static int TYPE_JINHUA = 104;
    /**
     * 顺子
     */
	public static int TYPE_SHUNZI = 103;
    /**
     * 对子
     */
	public static int TYPE_DUIZI = 102;
    /**
     * 散牌
     */
	public static int TYPE_SANPAI = 101;

    /**
     * 当前牌的下标
     */
	public static int paiIndex = 0;
	
	
	/**
	 * 获取花色
	 * @param pai
	 * @return
	 */
	public static int getColor(int pai){
		
		return pai/20;
	}
	
	
	/**
	 * 获取数值
	 * @param pai
	 * @return
	 */
	public static int getNumber(int pai){
		
		return pai%20;
	}
	
	
	/**
	 * 判断是否是豹子
	 * @return
	 */
	public static boolean isBaoZi(List<Integer> paiList){
		
		if(paiList.size()>0 && paiList.size()==3){
			
			int p = getNumber(paiList.get(0));
			int p1 = getNumber(paiList.get(1));
			int p2 = getNumber(paiList.get(2));
			if(p==p1 && p==p2){
				
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断是否是同花顺
	 * @return
	 */
	public static boolean isTongHuaSun(List<Integer> paiList){
		
		if(isJinHua(paiList)&&isSunZi(paiList)){
			
			return true;
		}
		return false;
	}
	
	/**
	 * 判断是否是金花（同花）
	 * @return
	 */
	public static boolean isJinHua(List<Integer> paiList){
		
		if(paiList.size()>0 && paiList.size()==3){
			
			Collections.sort(paiList);
			int p = getColor(paiList.get(0));
			int p1 = getColor(paiList.get(1));
			int p2 = getColor(paiList.get(2));
			if(p == p1 && p == p2){
				
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断是否是顺子
	 * @return
	 */
	public static boolean isSunZi(List<Integer> paiList){
		
		if(paiList.size()>0 && paiList.size()==3){
			
			List<Integer> pais = new ArrayList<Integer>();
			for (Integer pai : paiList) {
				pais.add(getNumber(pai));
			}
			Collections.sort(pais);
			int p = pais.get(0);
			int p1 = pais.get(1) - 1;
			int p2 = pais.get(2) - 2;
			if(p == p1 && p == p2){
				
				return true;
				
			}else if(p==1 && p==p1-10 && p==p2-10){ // 特殊情况 QKA
				
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 判断是否是对子
	 * @return
	 */
	public static boolean isDuiZi(List<Integer> paiList){
		
		if(paiList.size()>0 && paiList.size()==3){
			
			List<Integer> pais = new ArrayList<Integer>();
			for (Integer pai : paiList) {
				pais.add(getNumber(pai));
			}
			Collections.sort(pais);
			int p = pais.get(0);
			int p1 = pais.get(1);
			int p2 = pais.get(2);
			if((p == p1&&p!=p2) || (p1 == p2&&p!=p2)){
				
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * 获取牌最大值（1~13）
	 * @return
	 */
	public static int getMaxNum(List<Integer> paiList){
		
		if(paiList.size()>0 && paiList.size()==3){

			List<Integer> pais = new ArrayList<Integer>();
			for (Integer pai : paiList) {
				pais.add(getNumber(pai));
			}
			Collections.sort(pais);
			if(pais.get(0)==1){ // A最大
				return pais.get(0);
			}else{
				return pais.get(2);
			}
		}
		return 0;
	}
	
	
	/**
	 * 获取最大牌（带花色）
	 * @return
	 */
	public static int getMaxPai(List<Integer> paiList){
		
		int maxNum = getMaxNum(paiList);
		int maxPai = 0;
		for (int i = 0; i < paiList.size(); i++) {
			int num = getNumber(paiList.get(i));
			if(num==maxNum && paiList.get(i)>maxPai){
				maxPai = paiList.get(i);
			}
		}
		return maxPai;
	}
	
	
	/**
	 * 获取牌型
	 * @param paiList
	 * @return
	 */
	public static int getPaiType(List<Integer> paiList){
		
		if(isBaoZi(paiList)){
			
			return TYPE_BAOZI;
			
		}else if(isTongHuaSun(paiList)){
			
			return TYPE_TONGHUASHUN;
			
		}else if(isJinHua(paiList)){
			
			return TYPE_JINHUA;
			
		}else if(isSunZi(paiList)){
			
			return TYPE_SHUNZI;
			
		}else if(isDuiZi(paiList)){
			
			return TYPE_DUIZI;
			
		}else{
			
			return TYPE_SANPAI;
		}
	}
	
	
	/**
	 * 比牌
	 * @param paiA
	 * @param paiB
	 * @return A大返回1，A小返回-1，相等返回0
	 */
	public static int compare(List<Integer> paiA, List<Integer> paiB){
		
		int aType = getPaiType(paiA);
		int bType = getPaiType(paiB);
		
		if(aType>bType){
		
			return 1;
			
		}else if(aType<bType){
			
			return -1;
			
		}else if(aType==bType){
			// 对子
			if(aType==ZhaJinHuaCore.TYPE_DUIZI){

				// 比较牌的大小
				int duiziA = 0;
				int duiziB = 0;
				
				for (int i = 0; i < paiA.size()-1; i++) {
					int duizi = 0;
					for (int j = i+1; j < paiA.size(); j++) {
						if(getNumber(paiA.get(i))==getNumber(paiA.get(j))){
							duizi ++;
						}
					}
					if(duizi!=0){
						duiziA = paiA.get(i);
						break;
					}
				}
				for (int i = 0; i < paiB.size()-1; i++) {
					int duizi = 0;
					for (int j = i+1; j < paiB.size(); j++) {
						if(getNumber(paiB.get(i))==getNumber(paiB.get(j))){
							duizi ++;
						}
					}
					if(duizi!=0){
						duiziB = paiB.get(i);
						break;
					}
				}
				
				// 比较牌的大小
				if(compareNum(getNumber(duiziA), getNumber(duiziB))>0){
					return 1;
				}else if(compareNum(getNumber(duiziA), getNumber(duiziB))<0){
					return -1;
				}else{ // 比较最大的牌的花色

					return compareDaXiao(paiA, paiB);
				}
			}
			return compareDaXiao(paiA, paiB);
		}
		
		return 0;
	}
	
	/**
	 * 比较牌的大小，最大的相同比较第二大，第二大也相同比较最后一张
	 * A大返回1，A小返回-1，相同返回0
	 * @param @param paiA
	 * @param @param paiB
	 * @param @return   
	 * @return int  
	 * @throws
	 * @date 2018年1月12日
	 */
	public static int compareDaXiao(List<Integer> paiA, List<Integer> paiB){
		List<Integer> list1 = new ArrayList<Integer>();
		List<Integer> list2 = new ArrayList<Integer>();
		for (Integer integer : paiA) {
			if (getNumber(integer)==1) {
				list1.add(14);
			}else {
				list1.add(getNumber(integer));
			}
		}
		for (Integer integer : paiB) {
			if (getNumber(integer)==1) {
				list2.add(14);
			}else {
				list2.add(getNumber(integer));
			}
		}
		Collections.sort(list1);
		Collections.sort(list2);
		if (list1.get(2)>list2.get(2)) {
			return 1;
		}else if (list1.get(2)<list2.get(2)) {
			return -1;
		}else {
			if (list1.get(1)>list2.get(1)) {
				return 1;
			}else if (list1.get(1)<list2.get(1)) {
				return -1;
			}else {
				if (list1.get(0)>list2.get(0)) {
					return 1;
				}else if (list1.get(0)<list2.get(0)) {
					return -1;
				}else {
					return 0;
				}
			}
		}
	}

	
	/**
	 * 比较牌面大小
	 * @param paiA
	 * @param paiB
	 * @return
	 */
	public static int compareNum(int paiA, int paiB){
		
		// 相等
		if(paiA==paiB){
			return 0;
		}
		
		// 含有A，A最大
		if(paiA==1 || paiB==1){
			
			if(paiA==1){
				
				return 1;
			}else{
				return -1;
			}
		}else{
			
			if(paiA>paiB){
				
				return 1;
			}else{
				
				return -1;
			}
		}
	}
	
	
	/**
	 * 洗牌
	 * @return 
	 */
	public static List<Integer> xiPai(){
		
		paiIndex = 0;
		int[] indexs = randomPai();
		List<Integer> pais = new ArrayList<Integer>();
		for (int i = 0; i < indexs.length; i++) {
			
			pais.add(PAIS[indexs[i]]);
		}
		return pais;
	}
	
	
	/**
	 * 打乱牌的下标
	 * @return
	 */
	private static int[] randomPai(){
		
		int[] nums = new int[52];
		for (int i = 0; i < nums.length; i++) {
			while(true){
				int num = RandomUtils.nextInt(52);
				if(!ArrayUtils.contains(nums,num)){
					nums[i] = num;
					break;
				}else if(num==0){
				    //若是0，判断之前是否已存在
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
	 * @return
	 */
	public static List<Integer> faPai(List<Integer> pais){
		
		List<Integer> paiList = new ArrayList<Integer>();
		for (int j = 0; j < 3; j++) {
			paiList.add(pais.get(paiIndex));
			paiIndex = paiIndex + 1;
		}
		return paiList;
	}
	
	
	public static void main(String[] args) {
		
		/*for (int i=0;i<1;i++) {

			List<Integer> paiList = xiPai();

			List<Integer> mypai = faPai(paiList);
			System.out.println(JSONArray.fromObject(mypai));
			System.out.println(getPaiType(mypai));

			List<Integer> mypai1 = faPai(paiList);
			System.out.println(JSONArray.fromObject(mypai1));
			System.out.println(getPaiType(mypai1));

			List<Integer> mypai2 = faPai(paiList);
			System.out.println(JSONArray.fromObject(mypai2));
			System.out.println(getPaiType(mypai2));

			System.out.println(compare(mypai, mypai1));
			System.out.println(compare(mypai, mypai2));
			System.out.println(compare(mypai1, mypai2));

			System.out.println("----------------------");
			{1,2,3,4,5,6,7,8,9,10,11,12,13,
			21,22,23,24,25,26,27,28,29,30,31,32,33,
			41,42,43,44,45,46,47,48,49,50,51,52,53,
			61,62,63,64,65,66,67,68,69,70,71,72,73};
		}*/
		List<Integer> a =Arrays.asList(6,21,62);
		List<Integer> b =Arrays.asList(41,70,72);
		System.out.println(getPaiType(a));
		System.out.println(getPaiType(b));
		System.out.println(compare(a, b));
	}
}
