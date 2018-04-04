package com.zhuoan.biz.core.sss;

import com.zhuoan.biz.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;

public class SSSSpecialCardSort {

	/**
	 * 传入牌 及牌型 返回 特殊牌 排牌
	 * 
	 * @param car
	 * @param carType
	 * @return
	 */
	public static String[] CardSort(String[] car, int carType) {

		switch (carType) {
		case 0:
			/**
			 * 不是特殊牌
			 */
			return SSSOrdinaryCards.sort(car);
		case 1:
			/**
			 * 三同花
			 */
			return threeFlower(car);

		case 2:
			/**
			 * 三顺子
			 */
			return threeFlush(car);

		case 3:
			/**
			 * 六对半
			 */
			return sixPairs(car);

		case 4:
			/**
			 * 五对三条
			 */
			return fiveThree(car);

		case 5:
			/**
			 * 四套三条
			 */
			return fourThree(car);

		case 6:
			/**
			 * 双怪冲三
			 */
			return twoGourd(car);

		case 7:
			/**
			 * 凑一色
			 */
			return oneColor(car);

		case 8:
			/**
			 * 全小
			 */
			return allSmall(car);

		case 9:
			/**
			 * 全大
			 */
			return allBig(car);

		case 10:
			/**
			 * 三分天下
			 */
			return threeBomb(car);

		case 11:
			/**
			 * 三同花顺
			 */
			return threeFlushByFlower(car);

		case 12:
			/**
			 * 十二皇族
			 */
			return twelfth(car);

		case 13:
			/**
			 * 一条龙
			 */
			return thirteen(car);

		case 14:
			/**
			 * 至尊清龙
			 */
			return sameThirteen(car);
		case 15:
			/**
			 * 八仙过海
			 */
			return eightXian(car);
		case 16:
			/**
			 * 七星连珠
			 */
			return sevenStars(car);
		case 17:
			/**
			 *六六大顺
			 */
			return sixDaSun(car);
		case 18:
			/**
			 * 三皇五帝
			 */
			return ThreeEmFiveSo(car);

		default:
			break;
		}

		return SSSOrdinaryCards.sort(car);

	}

	/**
	 * 三皇五帝
	 */
	private static String[] ThreeEmFiveSo(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByNum(player);
		String[] cars=new String[13];
		int num=0;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 3) {
				cars[0]=list.get(0)+"-"+num;
				cars[1]=list.get(1)+"-"+num;
				cars[2]=list.get(2)+"-"+num;
			}else if (list.size() == 5){
				if (cars[3]==null) {
					cars[3]=list.get(0)+"-"+num;
					cars[4]=list.get(1)+"-"+num;
					cars[5]=list.get(2)+"-"+num;
					cars[6]=list.get(3)+"-"+num;
					cars[7]=list.get(4)+"-"+num;
				}else{
					cars[8]=list.get(0)+"-"+num;
					cars[9]=list.get(1)+"-"+num;
					cars[10]=list.get(2)+"-"+num;
					cars[11]=list.get(3)+"-"+num;
					cars[12]=list.get(4)+"-"+num;
				}
			}
		}
		
		for (String pai : cars) {
			if (pai==null) {
				LogUtil.print("三皇五帝排牌出错："+player);
				break;
			}
		}
		
		return cars;
	}

	/**
	 *六六大顺
	 */
	private static String[] sixDaSun(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByNum(player);
		
		String[] cars=new String[13];
		int num=0;
		int i=6;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 6) {
				cars[0]=list.get(0)+"-"+num;
				cars[1]=list.get(1)+"-"+num;
				cars[2]=list.get(2)+"-"+num;
				cars[3]=list.get(3)+"-"+num;
				cars[4]=list.get(4)+"-"+num;
				cars[5]=list.get(5)+"-"+num;
				
			}else if (list.size()!=0){
				for (int j = 0; j < list.size(); j++) {
					cars[i]=list.get(j)+"-"+num;
					i++;
				}
			}
		}
		
		for (String pai : cars) {
			if (pai==null) {
				LogUtil.print("六六大顺排牌出错："+player);
				break;
			}
		}
		
		return cars;
		
		
	}
	
	/**
	 * 七星连珠
	 */
	private static String[] sevenStars(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByNum(player);
		String[] cars=new String[13];
		int num=0;
		int i=7;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 7) {
				cars[0]=list.get(0)+"-"+num;
				cars[1]=list.get(1)+"-"+num;
				cars[2]=list.get(2)+"-"+num;
				cars[3]=list.get(3)+"-"+num;
				cars[4]=list.get(4)+"-"+num;
				cars[5]=list.get(5)+"-"+num;
				cars[6]=list.get(6)+"-"+num;
				
			}else if (list.size()!=0){
				for (int j = 0; j < list.size(); j++) {
					cars[i]=list.get(j)+"-"+num;
					i++;
				}
			}
		}
		
		for (String pai : cars) {
			if (pai==null) {
				LogUtil.print("七星连珠排牌出错："+player);
				break;
			}
		}
		return cars;
	}

	/**
	 * 八仙过海
	 */
	private static String[] eightXian(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByNum(player);
		String[] cars=new String[13];
		int num=0;
		int i=8;
		for (ArrayList<Integer> list : set) {
			num++;
			if (list.size() == 8) {
				cars[0]=list.get(0)+"-"+num;
				cars[1]=list.get(1)+"-"+num;
				cars[2]=list.get(2)+"-"+num;
				cars[3]=list.get(3)+"-"+num;
				cars[4]=list.get(4)+"-"+num;
				cars[5]=list.get(5)+"-"+num;
				cars[6]=list.get(6)+"-"+num;
				cars[7]=list.get(7)+"-"+num;
				
			}else if (list.size()!=0){
				for (int j = 0; j < list.size(); j++) {
					cars[i]=list.get(j)+"-"+num;
					i++;
				}
			}
		}
		
		for (String pai : cars) {
			if (pai==null) {
				LogUtil.print("八仙过海排牌出错："+player);
				break;
			}
		}
		return cars;
	}

	/**
	 * 至尊清龙
	 */
	public static String[] sameThirteen(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 一条龙
	 */
	public static String[] thirteen(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 十二皇族
	 */
	public static String[] twelfth(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {
			for (int j = i + 1; j < car.length; j++) {
				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 三同花顺
	 */
	public static String[] threeFlushByFlower(String[] car) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car);
	}

	/**
	 * 三分天下
	 */
	public static String[] threeBomb(String[] car) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car);
	}

	/**
	 * 全大
	 */
	public static String[] allBig(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {
			for (int j = i + 1; j < car.length; j++) {
				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 全小
	 */
	public static String[] allSmall(String[] car) {

		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 凑一色
	 */
	public static String[] oneColor(String[] car) {
		for (int i = 0; i < car.length - 1; i++) {

			for (int j = i + 1; j < car.length; j++) {

				if (SSSGameRoom.getValue(car[i]) > SSSGameRoom.getValue(car[j])) {
					String o = car[i];
					String o1 = car[j];
					car[i] = o1;
					car[j] = o;
				}
			}
		}
		return car;
	}

	/**
	 * 双怪冲三
	 */
	public static String[] twoGourd(String[] car) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car);
	}

	/**
	 * 四套三条
	 */
	public static String[] fourThree(String[] car) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car);
	}

	/**
	 * 五对三条
	 */
	public static String[] fiveThree(String[] car) {
		// TODO Auto-generated method stub
		return SSSOrdinaryCards.sort(car);
	}

	/**
	 * 六对半
	 */
	public static String[] sixPairs(String[] car) {
		
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByNum(player);
		//ArrayList<ArrayList<String>> set1 =SSSOrdinaryCards.one(player);
		
		String[] cars=new String[13];
		int num=0;
		int c=0;
		for (ArrayList<Integer> list : set) {
			num++;
			
			if (list.size() == 1||list.size() == 3||list.size() == 5||list.size() == 9) {
				for (int i = 0; i < list.size(); i++) {
					if (cars[c]==null) {
						cars[c]=list.get(i)+"-"+num;
					}
					c++;
				}
				
			}else if (list.size() == 2||list.size() == 4||list.size() == 6||list.size() == 8){
			
				for (int i = 0; i < list.size(); i++) {
					if (cars[c]==null) {
						cars[c]=list.get(i)+"-"+num;
					}
					c++;
				}
			}
		}
		/*int j=1;
		for (ArrayList<Integer> list : set) {
			if (list.size() == 1) {
				cars[0]=
			} else if (list.size() >= 2) {
				
				for (int i = 0; i < list.size(); i++) {
					
				}
				j++;
			}
		}*/
		Boolean is=false;
		for (String pai : cars) {
			if (pai==null) {
				is=true;
				LogUtil.print("六对半排牌出错："+player);
				break;
			}
		}
		
		if (is) {
			return SSSOrdinaryCards.sort(car);
		}else{
			return cars;
		}
		
		/*return SSSOrdinaryCards.sort(car);*/
	}

	/**
	 * 三顺子
	 */
	public static String[] threeFlush(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		
		String[] cars=new String[13];
		ArrayList<ArrayList<String>> flu=SSSOrdinaryCards.flush(player);
		
		for (int i = 0; i < flu.size(); i++) {
			/*ArrayList<String> player1=new ArrayList<String>(player);*/
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
						if (ps.get(0)+1==ps.get(1)&&ps.get(1)+1==ps.get(2)&&ps.get(2)-2==ps.get(0)) {
							if (SSSGameRoom.getValue(five1.get(4))>SSSGameRoom.getValue(five2.get(4))) {
								if ((SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))!=1)||(SSSGameRoom.getValue(five1.get(0))==1&&SSSGameRoom.getValue(five2.get(0))!=1)) {
									cars[12]=five1.get(4);
									cars[11]=five1.get(3);
									cars[10]=five1.get(2);
									cars[9]=five1.get(1);
									cars[8]=five1.get(0);
									cars[7]=five2.get(4);
									cars[6]=five2.get(3);
									cars[5]=five2.get(2);
									cars[4]=five2.get(1);
									cars[3]=five2.get(0);
									cars[2]=player1.get(2);
									cars[1]=player1.get(1);
									cars[0]=player1.get(0);
								}else if (SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))==1){
									cars[12]=five2.get(4);
									cars[11]=five2.get(3);
									cars[10]=five2.get(2);
									cars[9]=five2.get(1);
									cars[8]=five2.get(0);
									cars[7]=five1.get(4);
									cars[6]=five1.get(3);
									cars[5]=five1.get(2);
									cars[4]=five1.get(1);
									cars[3]=five1.get(0);
									cars[2]=player1.get(2);
									cars[1]=player1.get(1);
									cars[0]=player1.get(0);
								}
							}else{
								if ((SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))!=1)||(SSSGameRoom.getValue(five1.get(0))==1&&SSSGameRoom.getValue(five2.get(0))!=1)) {
									cars[12]=five2.get(4);
									cars[11]=five2.get(3);
									cars[10]=five2.get(2);
									cars[9]=five2.get(1);
									cars[8]=five2.get(0);
									cars[7]=five1.get(4);
									cars[6]=five1.get(3);
									cars[5]=five1.get(2);
									cars[4]=five1.get(1);
									cars[3]=five1.get(0);
									cars[2]=player1.get(2);
									cars[1]=player1.get(1);
									cars[0]=player1.get(0);
									
								}else if (SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))==1){
									cars[12]=five1.get(4);
									cars[11]=five1.get(3);
									cars[10]=five1.get(2);
									cars[9]=five1.get(1);
									cars[8]=five1.get(0);
									cars[7]=five2.get(4);
									cars[6]=five2.get(3);
									cars[5]=five2.get(2);
									cars[4]=five2.get(1);
									cars[3]=five2.get(0);
									cars[2]=player1.get(2);
									cars[1]=player1.get(1);
									cars[0]=player1.get(0);
								}
							}
						}else if(ps.get(0)==1&&ps.get(1)==12&&ps.get(2)==13){
							if ((SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))!=1)||(SSSGameRoom.getValue(five1.get(0))==1&&SSSGameRoom.getValue(five2.get(0))!=1)) {
								cars[12]=five1.get(4);
								cars[11]=five1.get(3);
								cars[10]=five1.get(2);
								cars[9]=five1.get(1);
								cars[8]=five1.get(0);
								cars[7]=five2.get(4);
								cars[6]=five2.get(3);
								cars[5]=five2.get(2);
								cars[4]=five2.get(1);
								cars[3]=five2.get(0);
								cars[2]=player1.get(2);
								cars[1]=player1.get(1);
								cars[0]=player1.get(0);
							}else if (SSSGameRoom.getValue(five1.get(0))!=1&&SSSGameRoom.getValue(five2.get(0))==1){
								cars[12]=five2.get(4);
								cars[11]=five2.get(3);
								cars[10]=five2.get(2);
								cars[9]=five2.get(1);
								cars[8]=five2.get(0);
								cars[7]=five1.get(4);
								cars[6]=five1.get(3);
								cars[5]=five1.get(2);
								cars[4]=five1.get(1);
								cars[3]=five1.get(0);
								cars[2]=player1.get(2);
								cars[1]=player1.get(1);
								cars[0]=player1.get(0);
							}
						}
					}
					
					
				}
				
			}
		}
		Boolean is=false;
		for (String pai : cars) {
			if (pai==null) {
				is=true;
				LogUtil.print("三顺子排牌出错："+player);
				break;
			}
		}
		if (is) {
			return SSSOrdinaryCards.sort(car);
		}else{
			return cars;
		}
	}

	/**
	 * 三同花
	 */
	public static String[] threeFlower(String[] car) {
		ArrayList<String> player = new ArrayList<String>();
		for (String string : car) {
			player.add(string);
		}
		
		ArrayList<ArrayList<Integer>> set =SSSSpecialCards.getListByFlower(player);
		
		String[] cars=new String[13];
		int flower=0;
		for (ArrayList<Integer> list : set) {
			flower++;
			if (list.size() == 3) {
				cars[0]=flower+"-"+list.get(0);
				cars[1]=flower+"-"+list.get(1);
				cars[2]=flower+"-"+list.get(2);
			}else if (list.size() == 5){
				if (cars[3]==null) {
					cars[3]=flower+"-"+list.get(0);
					cars[4]=flower+"-"+list.get(1);
					cars[5]=flower+"-"+list.get(2);
					cars[6]=flower+"-"+list.get(3);
					cars[7]=flower+"-"+list.get(4);
				}else{
					cars[8]=flower+"-"+list.get(0);
					cars[9]=flower+"-"+list.get(1);
					cars[10]=flower+"-"+list.get(2);
					cars[11]=flower+"-"+list.get(3);
					cars[12]=flower+"-"+list.get(4);
				}
			}else if (list.size() == 8){
				if (cars[3]==null&&cars[0]==null) {
					cars[0]=flower+"-"+list.get(0);
					cars[1]=flower+"-"+list.get(1);
					cars[2]=flower+"-"+list.get(2);
					cars[3]=flower+"-"+list.get(3);
					cars[4]=flower+"-"+list.get(4);
					cars[5]=flower+"-"+list.get(5);
					cars[6]=flower+"-"+list.get(6);
					cars[7]=flower+"-"+list.get(7);
				}else if(cars[0]==null&&cars[8]==null){
					cars[0]=flower+"-"+list.get(0);
					cars[1]=flower+"-"+list.get(1);
					cars[2]=flower+"-"+list.get(2);
					cars[8]=flower+"-"+list.get(3);
					cars[9]=flower+"-"+list.get(4);
					cars[10]=flower+"-"+list.get(5);
					cars[11]=flower+"-"+list.get(6);
					cars[12]=flower+"-"+list.get(7);
				}
			}else if (list.size() == 10){
				cars[3]=flower+"-"+list.get(0);
				cars[4]=flower+"-"+list.get(1);
				cars[5]=flower+"-"+list.get(2);
				cars[6]=flower+"-"+list.get(3);
				cars[7]=flower+"-"+list.get(4);
				cars[8]=flower+"-"+list.get(5);
				cars[9]=flower+"-"+list.get(6); 
				cars[10]=flower+"-"+list.get(7);
				cars[11]=flower+"-"+list.get(8);
				cars[12]=flower+"-"+list.get(9);
			}else if (list.size() == 13){
				cars[0]=flower+"-"+list.get(0);
				cars[1]=flower+"-"+list.get(1);
				cars[2]=flower+"-"+list.get(2);
				cars[3]=flower+"-"+list.get(3);
				cars[4]=flower+"-"+list.get(4);
				cars[5]=flower+"-"+list.get(5);
				cars[6]=flower+"-"+list.get(6);
				cars[7]=flower+"-"+list.get(7);
				cars[8]=flower+"-"+list.get(8);
				cars[9]=flower+"-"+list.get(9);
				cars[10]=flower+"-"+list.get(10);
				cars[11]=flower+"-"+list.get(11);
				cars[12]=flower+"-"+list.get(12);
			}
			
		}
		Boolean is=false;
		for (String pai : cars) {
			if (pai==null) {
				is=true;
				LogUtil.print("三同花排牌出错："+player);
				break;
			}
		}
		if (is) {
			return SSSOrdinaryCards.sort(car);
		}else{
			return cars;
		}
	}

}
