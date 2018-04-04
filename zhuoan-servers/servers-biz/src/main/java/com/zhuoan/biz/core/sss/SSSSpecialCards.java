package com.zhuoan.biz.core.sss;

import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

/**
 * 特殊牌型的操作
 * @author hxp
 *
 */

/**
 * @author Administrator
【至尊清龙】清一色的从1（A）－－－13（k）
【八仙过海】13张牌出现8张相同的牌
【一条龙】从1（A）－－－13（k）
【四套三条】指的是4套相同的三张牌+任意一张杂牌
【三分天下】13张牌出现3副炸弹加一张杂牌（或称三套四梅、铁支、 三炸弹）
【三皇五帝】2个五同加一个三条
【七星连珠】13张牌出现7张相同的牌
【十二皇族】12张都是10以上的牌
【三同花顺】3 5 5都为同花顺
【双怪冲三】两个葫芦+一个对子 一张散牌
【六六大顺】13张牌出现6张相同的牌
【全大】十三张牌数字都为8-A
【全小】十三张牌数字都为2-8
【凑一色】十三牌都是方块、梅花或者黑桃、红心（指的在杂牌无任何特殊牌型出现的情况下）
【五对冲三】指的是5个对子+三张相同的牌型（三张牌冲头）
【六对半】指的是6个对子+任意一张杂牌
【三顺子】指三墩水都是顺子牌（也称杂顺）
【三同花】指三墩水都是同一种花色牌（也称杂花）
 */
public class SSSSpecialCards {

	/**
	 * 不是特殊牌型
	 */
	private static final int none=0;
	
	/**
	 * 三同花
	 */
	private static final int threeFlower=1;
	
	/**
	 * 三顺子
	 */
	private static final int threeFlush=2;
	
	/**
	 * 六对半
	 */
	private static final int sixPairs=3;
	
	/**
	 * 五对三条
	 */
	private static final int fiveThree=4;
	
	/**
	 * 四套三条
	 */
	private static final int fourThree=5;
	
	/**
	 * 双怪冲三
	 */
	private static final int twoGourd=6;
	
	/**
	 * 凑一色
	 */
	private static final int oneColor=7;
	
	/**
	 * 全小
	 */
	private static final int allSmall=8;
	
	/**
	 * 全大
	 */
	private static final int allBig=9;
	
	/**
	 * 三分天下
	 */
	private static final int threeBomb=10;
	
	/**
	 * 三同花顺
	 */
	private static final int threeFlushByFlower=11;
	
	/**
	 * 十二皇族
	 */
	private static final int twelfth=12;
	
	/**
	 * 一条龙
	 */
	private static final int thirteen=13;
	
	/**
	 * 至尊清龙
	 */
	private static final int sameThirteen=14;
	/**
	 *	八仙过海
	 */
	private static final int eightXian=15;
	/**
	 * 七星连珠
	 */
	private static final int sevenStars=16;
	/**
	 * 六六大顺
	 */
	private static final int sixDaSun=17;
	/**
	 * 三皇五帝
	 */
	private static final int ThreeEmFiveSo=18;
	
	/**
	 * 判断手牌是否是特殊牌型
	 * @param player
	 * @return 是特殊牌型返回相应数字，不是特殊牌型返回0
	 */
	public static int isSpecialCards(String[] cards,JSONObject setting){
		ArrayList<String> player=new ArrayList<String>();
		for(String string:cards){
			player.add(string);
		}
		if(setting.getInt("sameThirteen")!=0&&sameThirteen(player)){
			return sameThirteen;
		}else if (setting.getInt("eightXian")!=0&&eightXian(player)) {
			return eightXian;
		}else if (setting.getInt("thirteen")!=0&&thirteen(player)) {
			return thirteen;
		}else if (setting.getInt("fourThree")!=0&&fourThree(player)) {
			return fourThree;
		}else if (setting.getInt("threeBomb")!=0&&threeBomb(player)) {
			return threeBomb;
		}else if (setting.getInt("ThreeEmFiveSo")!=0&&ThreeEmFiveSo(player)) {
			return ThreeEmFiveSo;
		}else if (setting.getInt("sevenStars")!=0&&sevenStars(player)) {
			return sevenStars;
		}else  if (setting.getInt("twelfth")!=0&&twelfth(player)) {
			return twelfth;
		}else if (setting.getInt("threeFlushByFlower")!=0&&threeFlushByFlower(player)) {
			return threeFlushByFlower;
		}else  if (setting.getInt("sixDaSun")!=0&&sixDaSun(player)) {
			return sixDaSun;
		}else if (setting.getInt("allBig")!=0&&allBig(player)) {
			return allBig;
		}else if (setting.getInt("allSmall")!=0&&allSmall(player)) {
			return allSmall;
		}else if (setting.getInt("oneColor")!=0&&oneColor(player)) {
			return oneColor;
		}else if (setting.getInt("twoGourd")!=0&&twoGourd(player)) {
			return twoGourd;
		}else  if (setting.getInt("fiveThree")!=0&&fiveThree(player)) {
			return fiveThree;
		}else if (setting.getInt("sixPairs")!=0&&sixPairs(player)) {
			return sixPairs;
		}else if (setting.getInt("threeFlush")!=0&&threeFlush(player)) {
			return threeFlush;
		}else if (setting.getInt("threeFlower")!=0&&threeFlower(player)) {
			return threeFlower;
		}
		return 0;
	}
	
	/**
	 * 旧版备份
	 * @param cards
	 * @return
	 */
	public static int isSpecialCards1(String[] cards){
		ArrayList<String> player=new ArrayList<String>();
		for(String string:cards){
			player.add(string);
		}
		if(sameThirteen(player)){
			return sameThirteen;
		}else if (thirteen(player)) {
			return thirteen;
		}else if (twelfth(player)) {
			return twelfth;
		}else if (threeFlushByFlower(player)) {
			return threeFlushByFlower;
		}else if (threeBomb(player)) {
			return threeBomb;
		}else if (allBig(player)) {
			return allBig;
		}else if (allSmall(player)) {
			return allSmall;
		}else if (oneColor(player)) {
			return oneColor;
		}else if (twoGourd(player)) {
			return twoGourd;
		}else if (fourThree(player)) {
			return fourThree;
		}else if (fiveThree(player)) {
			return fiveThree;
		}else if (sixPairs(player)) {
			return sixPairs;
		}else if (threeFlush(player)) {
			return threeFlush;
		}else if (threeFlower(player)) {
			return threeFlower;
		}
		return 0;
	}
	/**
	 * 特殊牌型分数获取
	 * @param spe
	 * @return 返回相应特殊牌型的分数
	 */
	public static int score(int spe,JSONObject setting){
		switch (spe) {
		case threeFlower:
			return setting.getInt("threeFlower");
			
		case threeFlush:
			return setting.getInt("threeFlush");
			
		case sixPairs:
			return setting.getInt("sixPairs");
			
		case fiveThree:
			return setting.getInt("fiveThree");
			
		case fourThree:
			return setting.getInt("fourThree");
			
		case twoGourd:
			return setting.getInt("twoGourd");
			
		case oneColor:
			return setting.getInt("oneColor");
			
		case allSmall:
			return setting.getInt("allSmall");
			
		case allBig:
			return setting.getInt("allBig");
			
		case threeBomb:
			return setting.getInt("threeBomb");
			
		case threeFlushByFlower:
			return setting.getInt("threeFlushByFlower");
			
		case twelfth:
			return setting.getInt("twelfth");
			
		case thirteen:
			return setting.getInt("thirteen");
			
		case sameThirteen:
			return setting.getInt("sameThirteen");
		
		case eightXian:
			return setting.getInt("eightXian");
		
		case sevenStars:
			return setting.getInt("sevenStars");
		
		case sixDaSun:
			return setting.getInt("sixDaSun");

		case ThreeEmFiveSo:
			return setting.getInt("ThreeEmFiveSo");
			
		default:
			break;
		}
		return none;
	}
	/**
	 * 旧版备份
	 * 特殊牌型分数获取
	 * @param spe
	 * @return 返回相应特殊牌型的分数
	 */
	public static int score1(int spe){
		switch (spe) {
		case threeFlower:
			return 4;
			
		case threeFlush:
			return 4;
			
		case sixPairs:
			return 4;
			
		case fiveThree:
			return 5;
			
		case fourThree:
			return 6;
			
		case twoGourd:
			return 7;
			
		case oneColor:
			return 10;
			
		case allSmall:
			return 10;
			
		case allBig:
			return 10;
			
		case threeBomb:
			return 20;
			
		case threeFlushByFlower:
			return 20;
			
		case twelfth:
			return 24;
			
		case thirteen:
			return 36;
			
		case sameThirteen:
			return 108;
			
		default:
			break;
		}
		return none;
	}
	
	/**
	 * 特殊牌型名称获取
	 * @param spe
	 * @return 返回相应特殊牌型的名称
	 */
	public static String getName(int spe){
		switch (spe) {
		case threeFlower:
			return "三同花";
			
		case threeFlush:
			return "三顺子";
			
		case sixPairs:
			return "六对半";
			
		case fiveThree:
			return "五对三条";
			
		case fourThree:
			return "四套三条";
			
		case twoGourd:
			return "双怪冲三";
			
		case oneColor:
			return "凑一色";
			
		case allSmall:
			return "全小";
			
		case allBig:
			return "全大";
			
		case threeBomb:
			return "三分天下";
			
		case threeFlushByFlower:
			return "三同花顺";
			
		case twelfth:
			return "十二皇族";
			
		case thirteen:
			return "一条龙";
			
		case sameThirteen:
			return "至尊清龙";
			
		case eightXian:
			return "八仙过海";
			
		case sevenStars:
			return "七星连珠";
			
		case sixDaSun:
			return "六六大顺";
			
		case ThreeEmFiveSo:
			return "三皇五帝";
			
		default:
			break;
		}
		return null;
	}
	
	/**
	 * 以花色分类手牌(一副牌)
	 * @param player
	 * @return [[6, 10], [13], [4, 5, 6, 8, 9], [2, 3, 6, 7, 10]]
	 */
	/*public static ArrayList<ArrayList<Integer>> getListByFlower(TreeSet<String> player) {
		
		ArrayList<ArrayList<Integer>> set=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1=new ArrayList<Integer>();
		ArrayList<Integer> set2=new ArrayList<Integer>();
		ArrayList<Integer> set3=new ArrayList<Integer>();
		ArrayList<Integer> set4=new ArrayList<Integer>();
		
		for (String string : player) {
			if("1".equals(string.split("-")[0])){
				set1.add(Integer.parseInt(string.split("-")[1]));
			}else if("2".equals(string.split("-")[0])){
				set2.add(Integer.parseInt(string.split("-")[1]));
			}else if("3".equals(string.split("-")[0])){
				set3.add(Integer.parseInt(string.split("-")[1]));
			}else if("4".equals(string.split("-")[0])){
				set4.add(Integer.parseInt(string.split("-")[1]));
			}
		}
		Collections.sort(set1);
		Collections.sort(set2);
		Collections.sort(set3);
		Collections.sort(set4);
		
		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		return set;
	}*/
	
	/**
	 * 以花色分类手牌(两副牌)
	 * @param player
	 * @return [[6, 10], [13], [4, 5, 6, 8, 9], [2, 3, 6, 7, 10]]
	 */
	public static ArrayList<ArrayList<Integer>> getListByFlower(ArrayList<String> player) {
		
		ArrayList<ArrayList<Integer>> set=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1=new ArrayList<Integer>();
		ArrayList<Integer> set2=new ArrayList<Integer>();
		ArrayList<Integer> set3=new ArrayList<Integer>();
		ArrayList<Integer> set4=new ArrayList<Integer>();
		
		for (String string : player) {
			if("1".equals(string.split("-")[0])){
				set1.add(Integer.parseInt(string.split("-")[1]));
			}else if("2".equals(string.split("-")[0])){
				set2.add(Integer.parseInt(string.split("-")[1]));
			}else if("3".equals(string.split("-")[0])){
				set3.add(Integer.parseInt(string.split("-")[1]));
			}else if("4".equals(string.split("-")[0])){
				set4.add(Integer.parseInt(string.split("-")[1]));
			}
		}
		Collections.sort(set1);
		Collections.sort(set2);
		Collections.sort(set3);
		Collections.sort(set4);
		
		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		return set;
	}
	
	/**
	 * 以数字分类手牌(一副牌)
	 * @param player
	 * @return [[3, 4], [], [], [3], [2], [4], [1, 3, 4], [4], [1, 2], [2], [], [4], []]
	 */
	/*public static ArrayList<ArrayList<Integer>> getListByNum(TreeSet<String> player) {
		
		ArrayList<ArrayList<Integer>> set=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1=new ArrayList<Integer>();
		ArrayList<Integer> set2=new ArrayList<Integer>();
		ArrayList<Integer> set3=new ArrayList<Integer>();
		ArrayList<Integer> set4=new ArrayList<Integer>();
		ArrayList<Integer> set5=new ArrayList<Integer>();
		ArrayList<Integer> set6=new ArrayList<Integer>();
		ArrayList<Integer> set7=new ArrayList<Integer>();
		ArrayList<Integer> set8=new ArrayList<Integer>();
		ArrayList<Integer> set9=new ArrayList<Integer>();
		ArrayList<Integer> set10=new ArrayList<Integer>();
		ArrayList<Integer> set11=new ArrayList<Integer>();
		ArrayList<Integer> set12=new ArrayList<Integer>();
		ArrayList<Integer> set13=new ArrayList<Integer>();
		
		for (String string : player) {
			if("1".equals(string.split("-")[1])){
				set1.add(Integer.parseInt(string.split("-")[0]));
			}else if("2".equals(string.split("-")[1])){
				set2.add(Integer.parseInt(string.split("-")[0]));
			}else if("3".equals(string.split("-")[1])){
				set3.add(Integer.parseInt(string.split("-")[0]));
			}else if("4".equals(string.split("-")[1])){
				set4.add(Integer.parseInt(string.split("-")[0]));
			}else if("5".equals(string.split("-")[1])){
				set5.add(Integer.parseInt(string.split("-")[0]));
			}else if("6".equals(string.split("-")[1])){
				set6.add(Integer.parseInt(string.split("-")[0]));
			}else if("7".equals(string.split("-")[1])){
				set7.add(Integer.parseInt(string.split("-")[0]));
			}else if("8".equals(string.split("-")[1])){
				set8.add(Integer.parseInt(string.split("-")[0]));
			}else if("9".equals(string.split("-")[1])){
				set9.add(Integer.parseInt(string.split("-")[0]));
			}else if("10".equals(string.split("-")[1])){
				set10.add(Integer.parseInt(string.split("-")[0]));
			}else if("11".equals(string.split("-")[1])){
				set11.add(Integer.parseInt(string.split("-")[0]));
			}else if("12".equals(string.split("-")[1])){
				set12.add(Integer.parseInt(string.split("-")[0]));
			}else if("13".equals(string.split("-")[1])){
				set13.add(Integer.parseInt(string.split("-")[0]));
			}
		}
		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		set.add(set5);
		set.add(set6);
		set.add(set7);
		set.add(set8);
		set.add(set9);
		set.add(set10);
		set.add(set11);
		set.add(set12);
		set.add(set13);
		return set;
	}*/
	
	/**
	 * 以数字分类手牌(两副牌)
	 * @param player
	 * @return [[3, 4], [], [], [3], [2], [4], [1, 3, 4], [4], [1, 2], [2], [], [4], []]
	 */
	public static ArrayList<ArrayList<Integer>> getListByNum(ArrayList<String> player) {
		
		ArrayList<ArrayList<Integer>> set=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1=new ArrayList<Integer>();
		ArrayList<Integer> set2=new ArrayList<Integer>();
		ArrayList<Integer> set3=new ArrayList<Integer>();
		ArrayList<Integer> set4=new ArrayList<Integer>();
		ArrayList<Integer> set5=new ArrayList<Integer>();
		ArrayList<Integer> set6=new ArrayList<Integer>();
		ArrayList<Integer> set7=new ArrayList<Integer>();
		ArrayList<Integer> set8=new ArrayList<Integer>();
		ArrayList<Integer> set9=new ArrayList<Integer>();
		ArrayList<Integer> set10=new ArrayList<Integer>();
		ArrayList<Integer> set11=new ArrayList<Integer>();
		ArrayList<Integer> set12=new ArrayList<Integer>();
		ArrayList<Integer> set13=new ArrayList<Integer>();
		
		for (String string : player) {
			if("1".equals(string.split("-")[1])){
				set1.add(Integer.parseInt(string.split("-")[0]));
			}else if("2".equals(string.split("-")[1])){
				set2.add(Integer.parseInt(string.split("-")[0]));
			}else if("3".equals(string.split("-")[1])){
				set3.add(Integer.parseInt(string.split("-")[0]));
			}else if("4".equals(string.split("-")[1])){
				set4.add(Integer.parseInt(string.split("-")[0]));
			}else if("5".equals(string.split("-")[1])){
				set5.add(Integer.parseInt(string.split("-")[0]));
			}else if("6".equals(string.split("-")[1])){
				set6.add(Integer.parseInt(string.split("-")[0]));
			}else if("7".equals(string.split("-")[1])){
				set7.add(Integer.parseInt(string.split("-")[0]));
			}else if("8".equals(string.split("-")[1])){
				set8.add(Integer.parseInt(string.split("-")[0]));
			}else if("9".equals(string.split("-")[1])){
				set9.add(Integer.parseInt(string.split("-")[0]));
			}else if("10".equals(string.split("-")[1])){
				set10.add(Integer.parseInt(string.split("-")[0]));
			}else if("11".equals(string.split("-")[1])){
				set11.add(Integer.parseInt(string.split("-")[0]));
			}else if("12".equals(string.split("-")[1])){
				set12.add(Integer.parseInt(string.split("-")[0]));
			}else if("13".equals(string.split("-")[1])){
				set13.add(Integer.parseInt(string.split("-")[0]));
			}
		}
		set.add(set1);
		set.add(set2);
		set.add(set3);
		set.add(set4);
		set.add(set5);
		set.add(set6);
		set.add(set7);
		set.add(set8);
		set.add(set9);
		set.add(set10);
		set.add(set11);
		set.add(set12);
		set.add(set13);
		return set;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 八仙过海 13张牌出现8张相同的牌 
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean eightXian(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		for (ArrayList<Integer> list : set) {
			if(list.size()==8){
				return true;
			}
		}
		return false;
		
	}
	/**
	 * 特殊牌型的判断:
	 * 七星连珠  13张牌出现7张相同的牌 
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean sevenStars(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		for (ArrayList<Integer> list : set) {
			if(list.size()==7){
				return true;
			}
		}
		
		return false;
		
	}
	/**
	 * 特殊牌型的判断:
	 * 六六大顺 13张牌出现6张相同的牌 
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean sixDaSun(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		for (ArrayList<Integer> list : set) {
			if(list.size()==6){
				return true;
			}
		}
		return false;
		
	}
	/**
	 * 特殊牌型的判断:
	 * 三皇五帝，2个五同  一个冲三
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean ThreeEmFiveSo(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int three=0;
		int five=0;
		
		for (ArrayList<Integer> list : set) {
			if(list.size()==5){
				five++;
			}
			if(list.size()==3){
				three++;
			}
		}
		
		if (three==1&&five==2) {
			return true;
		}
		
		return false;
		
	}
	
	/**
	 * 特殊牌型的判断:
	 * 至尊清龙（同花十三水），同样花色从1到13一条龙
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean sameThirteen(ArrayList<String> player){
		String flower=player.get(0).split("-")[0];
		TreeSet<String> set=new TreeSet<String>();
		for (String string : player) {
			if(flower.equals(string.split("-")[0])){
				set.add(string.split("-")[1]);
			}
		}
		if(set.size()==13){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 十三水，从1到13一条龙
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean thirteen(ArrayList<String> player){
		TreeSet<String> set=new TreeSet<String>();
		for (String string : player) {
			set.add(string.split("-")[1]);
		}
		if(set.size()==13){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 十二皇族，12张都是10以上的牌
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean twelfth(ArrayList<String> player){
		int i=0;
		for (String string : player) {
			if(Integer.parseInt(string.split("-")[1])>10||Integer.parseInt(string.split("-")[1])==1){
				i++;
			}
		}
		if(i==13){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 三同花顺，3组都是同花顺
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean threeFlushByFlower(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByFlower(player);
		
		int i=0;
		for (ArrayList<Integer> list : set) {
			if(list.size()==3||list.size()==5||list.size()==8||list.size()==10){
				if(list.size()==3){
					if(list.get(0)==1&&list.get(2)==13){
						if(list.get(1)+1==list.get(2)){
							i++;
						}
					}else {
						if(list.get(0)+1==list.get(1)&&list.get(1)+1==list.get(2)){
							i++;
						}
					}
				}else if (list.size()==5) {
					if(list.get(0)==1&&list.get(4)==13){
						if(list.get(1)+1==list.get(2)&&list.get(2)+1==list.get(3)&&list.get(3)+1==list.get(4)){
							i+=2;
						}
					}else {
						if(list.get(0)+1==list.get(1)&&list.get(1)+1==list.get(2)&&list.get(2)+1==list.get(3)&&list.get(3)+1==list.get(4)){
							i+=2;
						}
					}
				}else if (list.size()==8) {
					boolean isTrue1=false;
					boolean isTrue2=false;
					for(int k=0;k<list.size()-3;k++){
						ArrayList<Integer> temp=new ArrayList<Integer>();
						temp.addAll(list);
						if(list.contains(temp.get(k)+1)&&list.contains(temp.get(k)+2)){
							isTrue1=true;
							i++;
							temp.remove(temp.indexOf(temp.get(k)+2));
							temp.remove(temp.indexOf(temp.get(k)+1));
							temp.remove(k);
						}else {
							isTrue1=false;
						}
						if(isTrue1){
							if(temp.get(0)==1&&temp.get(temp.size()-1)==13){
								if(temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
									i+=2;
									isTrue2=true;
								}else {
									isTrue2=false;
								}
							}else {
								if(temp.get(0)+1==temp.get(1)&&temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
									i+=2;
									isTrue2=true;
								}else {
									isTrue2=false;
								}
							}
							if(isTrue2){
								break;
							}else {
								i=0;
							}
						}else{
							i=0;
						}
					}

					if(!isTrue2){
						for(int k=0;k<list.size()-5;k++){
							ArrayList<Integer> temp=new ArrayList<Integer>();
							temp.addAll(list);
							if(list.contains(temp.get(k)+1)&&list.contains(temp.get(k)+2)&&list.contains(temp.get(k)+3)&&list.contains(temp.get(k)+4)){
								isTrue1=true;
								i+=2;
								temp.remove(temp.indexOf(temp.get(k)+4));
								temp.remove(temp.indexOf(temp.get(k)+3));
								temp.remove(temp.indexOf(temp.get(k)+2));
								temp.remove(temp.indexOf(temp.get(k)+1));
								temp.remove(k);
							}else {
								isTrue1=false;
							}
							if(isTrue1){
								if(temp.get(0)==1&&temp.get(temp.size()-1)==13){
									if(temp.get(1)+1==temp.get(2)){
										i++;
										isTrue2=true;
									}else {
										isTrue2=false;
									}
								}else {
									if(temp.get(0)+1==temp.get(1)&&temp.get(1)+1==temp.get(2)){
										i++;
										isTrue2=true;
									}else {
										isTrue2=false;
									}
								}
								if(isTrue2){
									break;
								}else {
									i=0;
								}
							}else {
								i=0;
							}
						}
					}
					
				}else if (list.size()==10) {
					boolean isTrue1=false;
					boolean isTrue2=false;

					for(int k=0;k<list.size()-5;k++){
						ArrayList<Integer> temp=new ArrayList<Integer>();
						temp.addAll(list);
						if(list.contains(temp.get(k)+1)&&list.contains(temp.get(k)+2)&&list.contains(temp.get(k)+3)&&list.contains(temp.get(k)+4)){
							isTrue1=true;
							i+=2;
							temp.remove(temp.indexOf(temp.get(k)+4));
							temp.remove(temp.indexOf(temp.get(k)+3));
							temp.remove(temp.indexOf(temp.get(k)+2));
							temp.remove(temp.indexOf(temp.get(k)+1));
							temp.remove(k);
						}else {
							isTrue1=false;
						}
						if(isTrue1){
							if(temp.get(0)==1&&temp.get(temp.size()-1)==13){
								if(temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
									i+=2;
									isTrue2=true;
								}else {
									isTrue2=false;
								}
							}else {
								if(temp.get(0)+1==temp.get(1)&&temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
									i+=2;
									isTrue2=true;
								}else {
									isTrue2=false;
								}
							}
							if(isTrue2){
								break;
							}else {
								i=0;
							}
						}else {
							i=0;
						}
					}
				}
			}
		}
		
		if(i==5){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 三分天下，3个炸弹加一张杂牌
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean threeBomb(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int i=0;
		
		for(int k=0;k<set.size();k++){
			if(set.get(k).size()==4){
				i++;
			}
		}
		
		if(i==3){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 全大，十三张牌数字都为8—A
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean allBig(ArrayList<String> player){
		int i=0;
		for (String string : player) {
			if(Integer.parseInt(string.split("-")[1])>=8||Integer.parseInt(string.split("-")[1])==1){
				i++;
			}
		}
		if(i==13){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 全小，十三张牌数字都为2—8
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean allSmall(ArrayList<String> player){
		int i=0;
		for (String string : player) {
			if(Integer.parseInt(string.split("-")[1])<=8&&Integer.parseInt(string.split("-")[1])>=2){
				i++;
			}
		}
		if(i==13){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 凑一色，十三张花色为红色或者黑色
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean oneColor(ArrayList<String> player){
		int black=0;
		int red=0;
		for (String string : player) {
			if("1".equals(string.split("-")[0])||"3".equals(string.split("-")[0])){
				black++;
			}else if("2".equals(string.split("-")[0])||"4".equals(string.split("-")[0])){
				red++;
			}
		}
		if(black==0||red==0){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 双怪冲三，2对葫芦+1个对子+任意一张杂牌
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean twoGourd(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int i=0;//单牌
		int j=0;//对子
		int k=0;//三张
		int l=0;//炸
		
		for (ArrayList<Integer> list : set) {
			if(list.size()==1){
				i++;
			}else if(list.size()==2){
				j++;
			}else if(list.size()==3){
				k++;
			}else if(list.size()==4){
				l++;
			}
		}
		if((i==1&&j==3&&k==2&&l==0)
				||(i==1&&j==1&&k==2&&l==1)
				||(i==0&&j==2&&k==3&&l==0)
				||(i==0&&j==0&&k==3&&l==1)
				||(i==0&&j==3&&k==1&&l==1)
				||(i==0&&j==1&&k==2&&l==1)){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 四套三条，4套相同的三张牌+任意一张杂牌
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean fourThree(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int i=0;//单牌
		int j=0;//对子
		int k=0;//三张
		int l=0;//炸
		for (ArrayList<Integer> list : set) {
			if(list.size()==1){
				i++;
			}else if(list.size()==2){
				j++;
			}else if(list.size()==3){
				k++;
			}else if(list.size()==4){
				l++;
			}
		}
		if((i==1&&j==0&&k==4&&l==0)
				||(i==0&&j==0&&k==3&&l==1)){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 五对三条，5个对子+三张相同的牌型（三张牌冲头）
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean fiveThree(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int i=0;//单牌
		int j=0;//对子
		int k=0;//三张
		int l=0;//炸
		for (ArrayList<Integer> list : set) {
			if(list.size()==1){
				i++;
			}else if(list.size()==2){
				j++;
			}else if(list.size()==3){
				k++;
			}else if(list.size()==4){
				l++;
			}
		}
		if(i==0&&j==5&&k==1&&l==0
				||i==0&&j==3&&k==1&&l==1
				||i==0&&j==1&&k==1&&l==2){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 六对半，6个对子+任意一张杂牌
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean sixPairs(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		
		int i=0;//单牌
		int j=0;//对子
		int k=0;//三张
		int l=0;//炸
		for (ArrayList<Integer> list : set) {
			if(list.size()==1){
				i++;
			}else if(list.size()==2){
				j++;
			}else if(list.size()==3){
				k++;
			}else if(list.size()==4){
				l++;
			}
		}
		if((i==1&&j==6&&k==0&&l==0)
				||(i==0&&j==5&&k==1&&l==0)
				||(i==1&&j==4&&k==0&&l==1)
				||(i==0&&j==3&&k==1&&l==1)
				||(i==0&&j==1&&k==1&&l==2)
				||(i==1&&j==2&&k==0&&l==2)
				||(i==1&&j==0&&k==0&&l==3)){
			return true;
		}
		return false;
	}
	
	/**
	 * 特殊牌型的判断:
	 * 三顺子，三敦水都是顺子牌（也成杂顺）
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean threeFlush(ArrayList<String> player){
		ArrayList<ArrayList<String>> flu= SSSOrdinaryCards.flush(player);
		int s=0;
		for (int i = 0; i < flu.size(); i++) {
			ArrayList<String> five1=flu.get(i);
			for (int j = 1; j < flu.size(); j++) {
				ArrayList<String> player1=new ArrayList<String>(player);
				ArrayList<String> five2=flu.get(j);
				 if (five1!=five2) {
					for (int k = 0; k < five1.size(); k++) {
						player1.remove(five1.get(k));
						player1.remove(five2.get(k));
					}
					if (player1.size()==3) {
						int one=SSSGameRoom.getValue(player1.get(0));
						int two=SSSGameRoom.getValue(player1.get(1));
						int three=SSSGameRoom.getValue(player1.get(2));
						ArrayList<Integer> ps=new ArrayList<Integer>();
						ps.add(one);
						ps.add(two);
						ps.add(three);
						Collections.sort(ps);
						if ((ps.get(0)+1==ps.get(1)&&ps.get(1)+1==ps.get(2)&&ps.get(2)-2==ps.get(0))||(ps.get(0)==1&&ps.get(1)==12&&ps.get(2)==13)) {
							return true;
						}
					}
				 }
			}
		}
		return false;
	}
	
/*	public static boolean threeFlush(ArrayList<String> player){
		ArrayList<Integer> temp=new ArrayList<Integer>();
		ArrayList<Integer> temp2=new ArrayList<Integer>();
		ArrayList<Integer> set=new ArrayList<Integer>();
		ArrayList<Integer> three=new ArrayList<Integer>();
		ArrayList<Integer> five1=new ArrayList<Integer>();
		ArrayList<Integer> five2=new ArrayList<Integer>();
		for (String string : player) {
			set.add(Integer.parseInt(string.split("-")[1]));
			temp.add(Integer.parseInt(string.split("-")[1]));
		}
		Collections.sort(set);
		Collections.sort(temp);
		
		//以3,5,5形式进行判断是否含有顺子
		if(set.get(set.size()-1)!=13&&set.get(0)!=1){
			three.add(set.get(0));
			set.remove(0);
			for(int i=0;i<2;i++){
				if(set.contains(three.get(i)+1)){
					three.add(set.get(set.indexOf(three.get(i)+1)));
					set.remove(set.indexOf(three.get(i)+1));
				}else {
					break;
				}
			}
			if(three.size()==3){
				five1.add(set.get(0));
				set.remove(0);
				for(int i=0;i<4;i++){
					if(set.contains(five1.get(i)+1)){
						five1.add(set.get(set.indexOf(five1.get(i)+1)));
						set.remove(set.indexOf(five1.get(i)+1));
					}else {
						break;
					}
				}
				if(five1.size()==5){
					five2.add(set.get(0));
					set.remove(0);
					for(int i=0;i<4;i++){
						if(set.contains(five2.get(i)+1)){
							five2.add(set.get(set.indexOf(five2.get(i)+1)));
							set.remove(set.indexOf(five2.get(i)+1));
						}else {
							break;
						}
					}
				}
				if(three.size()==3&&five1.size()==5&&five2.size()==5){
					return true;
				}else {
					//以5,3,5的形式获取顺子
					set.clear();
					set.addAll(temp);
					three.clear();
					five1.clear();
					five2.clear();
					five1.add(set.get(0));
					set.remove(0);
					for(int i=0;i<4;i++){
						if(set.contains(five1.get(i)+1)){
							five1.add(set.get(set.indexOf(five1.get(i)+1)));
							set.remove(set.indexOf(five1.get(i)+1));
						}else {
							break;
						}
					}
					if(five1.size()==5){
						temp2.addAll(set);
						three.add(set.get(0));
						set.remove(0);
						for(int i=0;i<2;i++){
							if(set.contains(three.get(i)+1)){
								three.add(set.get(set.indexOf(three.get(i)+1)));
								set.remove(set.indexOf(three.get(i)+1));
							}else {
								break;
							}
						}
						if(three.size()==3){
							five2.add(set.get(0));
							set.remove(0);
							for(int i=0;i<4;i++){
								if(set.contains(five2.get(i)+1)){
									five2.add(set.get(set.indexOf(five2.get(i)+1)));
									set.remove(set.indexOf(five2.get(i)+1));
								}else {
									break;
								}
							}
						}
						
						if(three.size()==3&&five1.size()==5&&five2.size()==5){
							return true;
						}else {
							//以5,5,3形式获取顺子
							three.clear();
							five2.clear();
							set.clear();
							set.addAll(temp2);
							five2.add(set.get(0));
							set.remove(0);
							for(int i=0;i<4;i++){
								if(set.contains(five2.get(i)+1)){
									five2.add(set.get(set.indexOf(five2.get(i)+1)));
									set.remove(set.indexOf(five2.get(i)+1));
								}else {
									break;
								}
							}
							if(five2.size()==5){
								three.add(set.get(0));
								set.remove(0);
								for(int i=0;i<2;i++){
									if(set.contains(three.get(i)+1)){
										three.add(set.get(set.indexOf(three.get(i)+1)));
										set.remove(set.indexOf(three.get(i)+1));
									}else {
										break;
									}
								}
							}
							
							
							if(three.size()==3&&five1.size()==5&&five2.size()==5){
								return true;
							}
						}
						
					}
				}
			}
		}else {
			three.add(set.get(0));
			set.remove(0);
			for(int i=0;i<2;i++){
				if(set.contains(three.get(i)+1)){
					three.add(set.get(set.indexOf(three.get(i)+1)));
					set.remove(set.indexOf(three.get(i)+1));
				}else {
					break;
				}
			}
			if(three.size()==3){
				five1.add(set.get(0));
				set.remove(0);
				for(int i=0;i<4;i++){
					if(set.contains(five1.get(i)+1)){
						five1.add(set.get(set.indexOf(five1.get(i)+1)));
						set.remove(set.indexOf(five1.get(i)+1));
					}else {
						break;
					}
				}
				if(five1.size()==5){
					five2.add(set.get(0));
					set.remove(0);
					for(int i=0;i<4;i++){
						if(set.contains(five2.get(i)+1)){
							five2.add(set.get(set.indexOf(five2.get(i)+1)));
							set.remove(set.indexOf(five2.get(i)+1));
						}else {
							break;
						}
					}
				}
				if(three.size()==3&&five1.size()==5&&five2.size()==5){
					return true;
				}else {
					
					//以5,3,5的形式获取顺子
					set.clear();
					set.addAll(temp);
					three.clear();
					five1.clear();
					five2.clear();
					five1.add(set.get(0));
					set.remove(0);
					for(int i=0;i<4;i++){
						if(set.contains(five1.get(i)+1)){
							five1.add(set.get(set.indexOf(five1.get(i)+1)));
							set.remove(set.indexOf(five1.get(i)+1));
						}else {
							break;
						}
					}
					if(five1.size()==5){
						temp2.clear();
						temp2.addAll(set);
						three.add(set.get(0));
						set.remove(0);
						for(int i=0;i<2;i++){
							if(set.contains(three.get(i)+1)){
								three.add(set.get(set.indexOf(three.get(i)+1)));
								set.remove(set.indexOf(three.get(i)+1));
							}else {
								break;
							}
						}
						if(three.size()==3){
							five2.add(set.get(0));
							set.remove(0);
							for(int i=0;i<4;i++){
								if(set.contains(five2.get(i)+1)){
									five2.add(set.get(set.indexOf(five2.get(i)+1)));
									set.remove(set.indexOf(five2.get(i)+1));
								}else {
									break;
								}
							}
						}
						
						if(three.size()==3&&five1.size()==5&&five2.size()==5){
							return true;
						}else {
							//以5,5,3形式获取顺子
							three.clear();
							five1.clear();
							five2.clear();
							set.clear();
							set.addAll(temp);
							five2.add(set.get(0));
							set.remove(0);
							for(int i=0;i<4;i++){
								if(set.contains(five2.get(i)+1)){
									five2.add(set.get(set.indexOf(five2.get(i)+1)));
									set.remove(set.indexOf(five2.get(i)+1));
								}else {
									break;
								}
							}
							if(five2.size()==5){
								three.add(set.get(0));
								set.remove(0);
								for(int i=0;i<2;i++){
									if(set.contains(three.get(i)+1)){
										three.add(set.get(set.indexOf(three.get(i)+1)));
										set.remove(set.indexOf(three.get(i)+1));
									}else {
										break;
									}
								}
							}
							
							if(three.size()==3&&five1.size()==5&&five2.size()==5){
								return true;
							}
						}
						
					}
				}
			}
			
			//将1当14来判断
			three.clear();
			five1.clear();
			five2.clear();
			set.clear();
			set.addAll(temp);
			while(set.size()==13&&set.get(0)==1&&set.get(12)==13){
				set.clear();
				set.addAll(temp);
				set.remove(0);
				set.add(14);
				temp.clear();
				temp.addAll(set);
				three.add(set.get(0));
				set.remove(0);
				for(int i=0;i<2;i++){
					if(set.contains(three.get(i)+1)){
						three.add(set.get(set.indexOf(three.get(i)+1)));
						set.remove(set.indexOf(three.get(i)+1));
					}else {
						break;
					}
				}
				if(three.size()==3){
					five1.add(set.get(0));
					set.remove(0);
					for(int i=0;i<4;i++){
						if(set.contains(five1.get(i)+1)){
							five1.add(set.get(set.indexOf(five1.get(i)+1)));
							set.remove(set.indexOf(five1.get(i)+1));
						}else {
							break;
						}
					}
					if(five1.size()==5){
						five2.add(set.get(0));
						set.remove(0);
						for(int i=0;i<4;i++){
							if(set.contains(five2.get(i)+1)){
								five2.add(set.get(set.indexOf(five2.get(i)+1)));
								set.remove(set.indexOf(five2.get(i)+1));
							}else {
								break;
							}
						}
					}
					if(three.size()==3&&five1.size()==5&&five2.size()==5){
						return true;
					}else {
						
						//以5,3,5的形式获取顺子
						set.clear();
						set.addAll(temp);
						three.clear();
						five1.clear();
						five2.clear();
						five1.add(set.get(0));
						set.remove(0);
						for(int i=0;i<4;i++){
							if(set.contains(five1.get(i)+1)){
								five1.add(set.get(set.indexOf(five1.get(i)+1)));
								set.remove(set.indexOf(five1.get(i)+1));
							}else {
								break;
							}
						}
						if(five1.size()==5){
							temp2.clear();
							temp2.addAll(set);
							three.add(set.get(0));
							set.remove(0);
							for(int i=0;i<2;i++){
								if(set.contains(three.get(i)+1)){
									three.add(set.get(set.indexOf(three.get(i)+1)));
									set.remove(set.indexOf(three.get(i)+1));
								}else {
									break;
								}
							}
							if(three.size()==3){
								five2.add(set.get(0));
								set.remove(0);
								for(int i=0;i<4;i++){
									if(set.contains(five2.get(i)+1)){
										five2.add(set.get(set.indexOf(five2.get(i)+1)));
										set.remove(set.indexOf(five2.get(i)+1));
									}else {
										break;
									}
								}
							}
							
							if(three.size()==3&&five1.size()==5&&five2.size()==5){
								return true;
							}else {
								//以5,5,3形式获取顺子
								three.clear();
								five2.clear();
								set.clear();
								set.addAll(temp2);
								five2.add(set.get(0));
								set.remove(0);
								for(int i=0;i<4;i++){
									if(set.contains(five2.get(i)+1)){
										five2.add(set.get(set.indexOf(five2.get(i)+1)));
										set.remove(set.indexOf(five2.get(i)+1));
									}else {
										break;
									}
								}
								if(five2.size()==5){
									three.add(set.get(0));
									set.remove(0);
									for(int i=0;i<2;i++){
										if(set.contains(three.get(i)+1)){
											three.add(set.get(set.indexOf(three.get(i)+1)));
											set.remove(set.indexOf(three.get(i)+1));
										}else {
											break;
										}
									}
								}
								
								
								if(three.size()==3&&five1.size()==5&&five2.size()==5){
									return true;
								}
							}
							
						}
					}
				}
			}
		}
		
		
		return false;
	}
	
*/	/**
	 * 特殊牌型的判断:
	 * 三同花，三敦水都是同一种花色牌（也成杂花）
	 * @param player
	 * @return 是的话返回true,不是的话返回false
	 */
	public static boolean threeFlower(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=getListByFlower(player);
		
		int i=0;
		
		int zero=0;
		for(ArrayList<Integer> list : set){
			if(list.size()==0){
				zero++;
			}
		}
		if(zero==1){
			for (ArrayList<Integer> list : set) {
				if(list.size()==3||list.size()==5){
					i++;
				}
			}
		}else if (zero==2) {
			for (ArrayList<Integer> list : set) {
				if(list.size()==3||list.size()==5){
					i++;
				}else if (list.size()==8||list.size()==10) {
					i+=2;
				}
			}
		}
		if(i==3){
			return true;
		}
		return false;
	}
	
}
