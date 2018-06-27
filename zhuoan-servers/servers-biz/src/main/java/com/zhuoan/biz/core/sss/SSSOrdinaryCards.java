package com.zhuoan.biz.core.sss;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * 普通牌型的操作
 * @author hxp
 *
 */
public class SSSOrdinaryCards {
	
	/**
	 * 获取一副牌的最优组合
	 * @param player
	 * @return [3-1, 4-12, 4-3, 2-4, 2-7, 2-10, 2-11, 2-12, 2-5, 3-5, 4-5, 3-9, 4-9]
	 */
	public static String[] sort(String[] player) {
		String[] result=new String[13];
		ArrayList<String> five2=new ArrayList<String>();
		ArrayList<String> five1=new ArrayList<String>();
		ArrayList<String> three=new ArrayList<String>();
		
		ArrayList<String> playerTemp=new ArrayList<String>();
		for (String string:player) {
			playerTemp.add(string);
		}
//		playerTemp.addAll(player);
		//五同
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> wutong=both(playerTemp);
			if(wutong.size()>0){
				if(five2.size()>0){
					if("1".equals(wutong.get(0).get(0).split("-")[1])){
						five1.addAll(wutong.get(0));
					}else {
						five1.addAll(wutong.get(wutong.size()-1));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}else {
					if("1".equals(wutong.get(0).get(0).split("-")[1])){
						five2.addAll(wutong.get(0));
					}else {
						five2.addAll(wutong.get(wutong.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}
			}
		}
		//同花顺
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> tonghuashun=flushByFlower(playerTemp);
			if(tonghuashun.size()>0){
				if(five2.size()==0){
					if("1".equals(tonghuashun.get(0).get(0).split("-")[1])&&!"1".equals(tonghuashun.get(tonghuashun.size()-1).get(4).split("-")[1])){
						five2.addAll(tonghuashun.get(0));
					}else {
						five2.addAll(tonghuashun.get(tonghuashun.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					if("1".equals(tonghuashun.get(0).get(0).split("-")[1])&&!"1".equals(tonghuashun.get(tonghuashun.size()-1).get(4).split("-")[1])){
						five1.addAll(tonghuashun.get(0));
					}else {
						five1.addAll(tonghuashun.get(tonghuashun.size()-1));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}
			}
		}
		//铁支
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> tiezhi=bomb(playerTemp);
			if(tiezhi.size()>0){
                if (five2.size()==0) {
                    if("1".equals(tiezhi.get(0).get(0).split("-")[1])){
                        five2.addAll(tiezhi.get(0));
                    }else {
                        five2.addAll(tiezhi.get(tiezhi.size()-1));
                    }
                    for(String temp:five2){
                        playerTemp.remove(temp);
                    }
                }else if (five1.size()==0) {
                    if("1".equals(tiezhi.get(0).get(0).split("-")[1])){
                        five1.addAll(tiezhi.get(0));
                    }else {
                        five1.addAll(tiezhi.get(tiezhi.size()-1));
                    }
                    for(String temp:five1){
                        playerTemp.remove(temp);
                    }
                }
			}	
		}
		//葫芦
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> hulu=gourd(playerTemp);

			if(hulu.size()>0){
				int san=three(playerTemp).size();
				if(five2.size()==0){
					if ("1".equals(hulu.get(0).get(2).split("-")[1])) {
						five2.addAll(hulu.get(0));
					}else if(hulu.size()/san==1){
						int loop=0;
						for(int k=hulu.size()-1;k>=0;k--){
							ArrayList<String> huluTemp=new ArrayList<String>();
							huluTemp.addAll(playerTemp);
							for(String temp:hulu.get(k)){
								huluTemp.remove(temp);
							}
							if(sameFlower(huluTemp).size()>0||flush(huluTemp).size()>0){
								loop=k;
								break;
							}
						}
						if(loop>0){
							five2.addAll(hulu.get(loop));
						}else {
							five2.addAll(hulu.get(hulu.size()-1));
						}

					}else{
						
						if("1".equals(hulu.get(0).get(4).split("-")[1])){
							int loop=0;
							for(int k=(hulu.size()/san*(san-1))+1;k<hulu.size();k++){
								ArrayList<String> huluTemp=new ArrayList<String>();
								huluTemp.addAll(playerTemp);
								for(String temp:hulu.get(k)){
									huluTemp.remove(temp);
								}
								if(sameFlower(huluTemp).size()>0||flush(huluTemp).size()>0){
									loop=k;
									break;
								}
							}
							if(loop==0){
								five2.addAll(hulu.get((hulu.size()/san*(san-1))+1));
							}else {
								five2.addAll(hulu.get(loop));
							}
						}else {
							int loop=0;
							for(int k=hulu.size()-1;k>=0;k--){
								ArrayList<String> huluTemp=new ArrayList<String>();
								huluTemp.addAll(playerTemp);
								for(String temp:hulu.get(k)){
									huluTemp.remove(temp);
								}
								if(sameFlower(huluTemp).size()>0||flush(huluTemp).size()>0){
									loop=k;
									break;
								}
							}
							if(loop==0){
								five2.addAll(hulu.get(hulu.size()/san*(san-1)));
							}else {
								five2.addAll(hulu.get(loop));
							}										
						}
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					if(hulu.size()/san==1){
						five1.addAll(hulu.get(hulu.size()-1));
					}else if (san==1) {
						if("1".equals(hulu.get(0).get(4).split("-")[1])){
							five1.addAll(hulu.get(1));
						}else {
							five1.addAll(hulu.get(0));
						}
					}else{
						if("1".equals(hulu.get(0).get(4).split("-")[1])){
							five1.addAll(hulu.get(hulu.size()/san+1));
						}else {
							five1.addAll(hulu.get(hulu.size()/san));
						}
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}
			}
			
		}
		//同花
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> tonghua=sameFlower(playerTemp);
			if(tonghua.size()>0){
				if(five2.size()==0){
					ArrayList<String> tt=new ArrayList<String>();
					tt.addAll(playerTemp);
					ArrayList<ArrayList<String>> oneArrayList=one(tt);
					if(oneArrayList.size()>0){
						if("1".equals(oneArrayList.get(0).get(0).split("-")[1])){
							for(String temp:oneArrayList.get(0)){
								tt.remove(temp);
							}
							ArrayList<ArrayList<String>> fList=sameFlower(tt);
							if(fList.size()>0){
								ArrayList<ArrayList<String>> oneArrayList2=one(tt);
								if(oneArrayList2.size()>0){
									for(String temp:oneArrayList2.get(oneArrayList2.size()-1)){
										tt.remove(temp);
									}
									ArrayList<ArrayList<String>> fList2=sameFlower(tt);
									if(fList2.size()>0){
										ArrayList<ArrayList<String>> oneArrayList3=one(tt);
										if(oneArrayList3.size()>0){
											for(String temp:oneArrayList3.get(oneArrayList3.size()-1)){
												tt.remove(temp);
											}
											ArrayList<ArrayList<String>> fList3=sameFlower(tt);
											if(fList3.size()>0){
												five2.addAll(fList3.get(fList3.size()-1));
											}
										}
										if(five2.size()==0){
											five2.addAll(fList2.get(fList2.size()-1));
										}
									}
								}
								if(five2.size()==0){
									five2.addAll(fList.get(fList.size()-1));
								}
							}
						}else {
							for(String temp:oneArrayList.get(oneArrayList.size()-1)){
								tt.remove(temp);
							}
							ArrayList<ArrayList<String>> fList=sameFlower(tt);
							if(fList.size()>0){
								ArrayList<ArrayList<String>> oneArrayList2=one(tt);
								if(oneArrayList2.size()>0){
									for(String temp:oneArrayList2.get(oneArrayList2.size()-1)){
										tt.remove(temp);
									}
									ArrayList<ArrayList<String>> fList2=sameFlower(tt);
									if(fList2.size()>0){
										ArrayList<ArrayList<String>> oneArrayList3=one(tt);
										if(oneArrayList3.size()>0){
											for(String temp:oneArrayList3.get(oneArrayList3.size()-1)){
												tt.remove(temp);
											}
											ArrayList<ArrayList<String>> fList3=sameFlower(tt);
											if(fList3.size()>0){
												five2.addAll(fList3.get(fList3.size()-1));
											}
										}
										if(five2.size()==0){
											five2.addAll(fList2.get(fList2.size()-1));
										}
									}
								}
								if(five2.size()==0){
									five2.addAll(fList.get(fList.size()-1));
								}
							}
						}
					}
					if(five2.size()==0){
						five2.addAll(tonghua.get(tonghua.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					ArrayList<String> tt=new ArrayList<String>();
					tt.addAll(playerTemp);
					ArrayList<ArrayList<String>> oneArrayList=one(tt);
					if(oneArrayList.size()>0){
						if("1".equals(oneArrayList.get(0).get(0).split("-")[1])){
							for(String temp:oneArrayList.get(0)){
								tt.remove(temp);
							}
							ArrayList<ArrayList<String>> fList=sameFlower(tt);
							if(fList.size()>0){
								five1.addAll(fList.get(fList.size()-1));
							}
						}else {
							for(String temp:oneArrayList.get(oneArrayList.size()-1)){
								tt.remove(temp);
							}
							ArrayList<ArrayList<String>> fList=sameFlower(tt);
							if(fList.size()>0){
								five1.addAll(fList.get(fList.size()-1));
							}
						}
					}
					if(five1.size()==0){
						five1.addAll(tonghua.get(tonghua.size()-1));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}
				if(five1.size()>0&&five2.size()>0){
					if(i==1){
						boolean isFloewr=false;
						if(five1.get(0).split("-")[0].equals(five1.get(1).split("-")[0])
								&&five1.get(1).split("-")[0].equals(five1.get(2).split("-")[0])
								&&five1.get(2).split("-")[0].equals(five1.get(3).split("-")[0])
								&&five1.get(3).split("-")[0].equals(five1.get(4).split("-")[0])){
							isFloewr=true;
						}
						if(isFloewr){
							ArrayList<String> temp=new ArrayList<String>();
							if("1".equals(five1.get(0).split("-")[1])){
								if ("1".equals(five2.get(0).split("-")[1])) {
									if(Integer.parseInt(five1.get(4).split("-")[1])>Integer.parseInt(five2.get(4).split("-")[1])){
										temp.addAll(five1);
										five1.clear();
										five1.addAll(five2);
										five2.clear();
										five2.addAll(temp);
									}else if (Integer.parseInt(five1.get(4).split("-")[1])==Integer.parseInt(five2.get(4).split("-")[1])) {
										if(Integer.parseInt(five1.get(3).split("-")[1])>Integer.parseInt(five2.get(3).split("-")[1])){
											temp.addAll(five1);
											five1.clear();
											five1.addAll(five2);
											five2.clear();
											five2.addAll(temp);
										}else if (Integer.parseInt(five1.get(3).split("-")[1])==Integer.parseInt(five2.get(3).split("-")[1])) {
											if(Integer.parseInt(five1.get(2).split("-")[1])>Integer.parseInt(five2.get(2).split("-")[1])){
												temp.addAll(five1);
												five1.clear();
												five1.addAll(five2);
												five2.clear();
												five2.addAll(temp);
											}else if (Integer.parseInt(five1.get(2).split("-")[1])==Integer.parseInt(five2.get(2).split("-")[1])) {
												if(Integer.parseInt(five1.get(1).split("-")[1])>Integer.parseInt(five2.get(1).split("-")[1])){
													temp.addAll(five1);
													five1.clear();
													five1.addAll(five2);
													five2.clear();
													five2.addAll(temp);
												}
											}
										}
									}
								}else {
									temp.addAll(five1);
									five1.clear();
									five1.addAll(five2);
									five2.clear();
									five2.addAll(temp);
								}
							}else {
								if(Integer.parseInt(five1.get(4).split("-")[1])>Integer.parseInt(five2.get(4).split("-")[1])){
									temp.addAll(five1);
									five1.clear();
									five1.addAll(five2);
									five2.clear();
									five2.addAll(temp);
								}else if (Integer.parseInt(five1.get(4).split("-")[1])==Integer.parseInt(five2.get(4).split("-")[1])) {
									if(Integer.parseInt(five1.get(3).split("-")[1])>Integer.parseInt(five2.get(3).split("-")[1])){
										temp.addAll(five1);
										five1.clear();
										five1.addAll(five2);
										five2.clear();
										five2.addAll(temp);
									}else if (Integer.parseInt(five1.get(3).split("-")[1])==Integer.parseInt(five2.get(3).split("-")[1])) {
										if(Integer.parseInt(five1.get(2).split("-")[1])>Integer.parseInt(five2.get(2).split("-")[1])){
											temp.addAll(five1);
											five1.clear();
											five1.addAll(five2);
											five2.clear();
											five2.addAll(temp);
										}else if (Integer.parseInt(five1.get(2).split("-")[1])==Integer.parseInt(five2.get(2).split("-")[1])) {
											if(Integer.parseInt(five1.get(1).split("-")[1])>Integer.parseInt(five2.get(1).split("-")[1])){
												temp.addAll(five1);
												five1.clear();
												five1.addAll(five2);
												five2.clear();
												five2.addAll(temp);
											}else if (Integer.parseInt(five1.get(1).split("-")[1])==Integer.parseInt(five2.get(1).split("-")[1])) {
												if(Integer.parseInt(five1.get(0).split("-")[1])>Integer.parseInt(five2.get(0).split("-")[1])){
													temp.addAll(five1);
													five1.clear();
													five1.addAll(five2);
													five2.clear();
													five2.addAll(temp);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
		//顺子
		for(int i=0;i<2;i++){
			ArrayList<ArrayList<String>> shunzi=flush(playerTemp);

			if(shunzi.size()>0){
				if(five2.size()==0){
					if ("1".equals(shunzi.get(0).get(0).split("-")[1])&&!"1".equals(shunzi.get(shunzi.size()-1).get(4).split("-")[1])) {
						five2.addAll(shunzi.get(0));
					}else {
						five2.addAll(shunzi.get(shunzi.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					ArrayList<String> tt=new ArrayList<String>();
					tt.addAll(playerTemp);
					ArrayList<ArrayList<String>> oneArrayList=one(tt);
					if(oneArrayList.size()>0){
						int kk=0;
						if(!"1".equals(oneArrayList.get(0).get(0).split("-")[1])){
							kk=oneArrayList.size()-1;
						}
						for(String temp:oneArrayList.get(kk)){
							tt.remove(temp);
						}
						ArrayList<ArrayList<String>> fList=flush(tt);
						if(fList.size()>0){
							if("1".equals(shunzi.get(0).get(0).split("-")[1])&&!"1".equals(shunzi.get(shunzi.size()-1).get(4).split("-")[1])){
								five1.addAll(shunzi.get(0));
							}else {
								five1.addAll(fList.get(fList.size()-1));
							}
						}
					}
					if (five1.size()==0){
						if ("1".equals(shunzi.get(0).get(0).split("-")[1])&&!"1".equals(shunzi.get(shunzi.size()-1).get(4).split("-")[1])) {
							five1.addAll(shunzi.get(0));
						}else {
							five1.addAll(shunzi.get(shunzi.size()-1));
						}
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}
			}
		}
		//三条
		for(int i=0;i<3;i++){
			ArrayList<ArrayList<String>> santiao=three(playerTemp);

			if(santiao.size()>0){
				if(five2.size()==0){
					if("1".equals(santiao.get(0).get(0).split("-")[1])){
						five2.addAll(santiao.get(0));
					}else{
						five2.addAll(santiao.get(santiao.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					if("1".equals(santiao.get(0).get(0).split("-")[1])){
						five1.addAll(santiao.get(0));
					}else{
						five1.addAll(santiao.get(santiao.size()-1));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}else if (three.size()==0) {
					three.addAll(santiao.get(santiao.size()-1));
					for(String temp:three){
						playerTemp.remove(temp);
					}
				}

			}
		}
		//两对
		for(int i=0;i<3;i++){
			ArrayList<ArrayList<String>> liangdui=two(playerTemp);

			if(liangdui.size()>0){
				int loop=0;
				for(int k=0;k<liangdui.size();k++){
					if(!"1".equals(liangdui.get(k).get(0).split("-")[1])){
						loop=k;
						break;
					}
				}
				if(five2.size()==0){
					if(loop!=0){
						five2.addAll(liangdui.get(loop-1));
					}else {
						five2.addAll(liangdui.get(liangdui.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					if(loop!=0){
						five1.addAll(liangdui.get(loop));
					}else {
						five1.addAll(liangdui.get(0));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}

			}
		}
		//一对
		for(int i=0;i<3;i++){
			ArrayList<ArrayList<String>> yidui=one(playerTemp);

			if(yidui.size()>0){
				if(five2.size()==0){
					if("1".equals(yidui.get(0).get(0).split("-")[1])){
						five2.addAll(yidui.get(0));
					}else {
						five2.addAll(yidui.get(yidui.size()-1));
					}
					for(String temp:five2){
						playerTemp.remove(temp);
					}
				}else if (five1.size()==0) {
					if("1".equals(yidui.get(0).get(0).split("-")[1])){
						five1.addAll(yidui.get(0));
					}else {
						five1.addAll(yidui.get(yidui.size()-1));
					}
					for(String temp:five1){
						playerTemp.remove(temp);
					}
				}else if(three.size()==0) {
					if("1".equals(yidui.get(0).get(0).split("-")[1])){
						three.addAll(yidui.get(0));
					}else {
						three.addAll(yidui.get(yidui.size()-1));
					}
					for(String temp:three){
						playerTemp.remove(temp);
					}
				}

			}
		}
		if(three.size()<3){
			for(int k=three.size();k<3;k++){
				ArrayList<ArrayList<Integer>> num=SSSSpecialCards.getListByNum(playerTemp);
				if(num.get(0).size()>0&&five1.size()!=0&&five2.size()!=0){
					three.add(num.get(0).get(0)+"-"+1);
					playerTemp.remove(num.get(0).get(0)+"-"+1);
				}else {
					if(num.get(0).size()>0){
						if(five2.size()==0){
							five2.add(num.get(0).get(0)+"-"+1);
							playerTemp.remove(num.get(0).get(0)+"-"+1);
						}else if(five1.size()==0){
							five1.add(num.get(0).get(0)+"-"+1);
							playerTemp.remove(num.get(0).get(0)+"-"+1);
						}
					}
					int numTemp=0;
					for(int l=num.size()-1;l>=0;l--){
						if(num.get(l).size()>0){
							numTemp=l;
							break;
						}
					}
					if(five2.size()==0){
						five2.add(num.get(numTemp).get(0)+"-"+(numTemp+1));
						k--;
					}else if(five1.size()==0){
						five1.add(num.get(numTemp).get(0)+"-"+(numTemp+1));
						k--;
					}else {
						three.add(num.get(numTemp).get(0)+"-"+(numTemp+1));
					}
					playerTemp.remove(num.get(numTemp).get(0)+"-"+(numTemp+1));
				}
			}
		}
		if(five1.size()<5){
			for(int k=five1.size();k<5;k++){
				ArrayList<ArrayList<Integer>> num=SSSSpecialCards.getListByNum(playerTemp);
				if(num.get(0).size()>0){
					five1.add(num.get(0).get(0)+"-"+1);
					playerTemp.remove(num.get(0).get(0)+"-"+1);
				}else {
					int numTemp=0;
					for(int l=num.size()-1;l>=0;l--){
						if(num.get(l).size()>0){
							numTemp=l;
							break;
						}
					}
					five1.add(num.get(numTemp).get(0)+"-"+(numTemp+1));
					playerTemp.remove(num.get(numTemp).get(0)+"-"+(numTemp+1));
				}
			}
		}
		if(five2.size()<5){
			for(int k=five2.size();k<5;k++){
				ArrayList<ArrayList<Integer>> num=SSSSpecialCards.getListByNum(playerTemp);
				if(num.get(0).size()>0){
					five2.add(num.get(0).get(0)+"-"+1);
					playerTemp.remove(num.get(0).get(0)+"-"+1);
				}else {
					int numTemp=0;
					for(int l=num.size()-1;l>=0;l--){
						if(num.get(l).size()>0){
							numTemp=l;
							break;
						}
					}
					five2.add(num.get(numTemp).get(0)+"-"+(numTemp+1));
					playerTemp.remove(num.get(numTemp).get(0)+"-"+(numTemp+1));
				}
			}
		}
		
		if(three.size()==3&&five1.size()==5&&five2.size()==5){
			for(int i=0;i<result.length;i++){
				for(String string:three){
					result[i]=string;
					i++;
				}
				for(String string:five1){
					result[i]=string;
					i++;
				}
				for(String string:five2){
					result[i]=string;
					i++;
				}
			}
		}
		return result;
	}
	
	/**
	 * 获取牌中的五同
	 * @param player
	 * @return [[1-1, 2-1, 4-1, 2-1, 3-1],[1-10, 2-10, 3-10, 4-10, 2-10]]
	 */
	public static ArrayList<ArrayList<String>> both(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for (int index=0;index<set.size();index++) {
			ArrayList<Integer> list=set.get(index);
			if(list.size()==5){
				ArrayList<String> temp=new ArrayList<String>();
				temp.add(list.get(0)+"-"+(index+1));
				temp.add(list.get(1)+"-"+(index+1));
				temp.add(list.get(2)+"-"+(index+1));
				temp.add(list.get(3)+"-"+(index+1));
				temp.add(list.get(4)+"-"+(index+1));
				tempList.add(temp);
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的同花顺
	 * @param player
	 * @return [[1-1, 1-2, 1-3, 1-4, 1-5],[1-10, 1-11, 1-12, 1-13, 1-1]]
	 */
	public static ArrayList<ArrayList<String>> flushByFlower(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByFlower(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for (int index=0;index<set.size();index++) {
			ArrayList<Integer> list=set.get(index);
			if(list.size()>=5){
				if(list.get(0)!=1&&list.get(list.size()-1)!=13){
					for(int i=0;i<=list.size()-5;i++){
						ArrayList<String> temp=new ArrayList<String>();
						if(list.get(i)+1==list.get(i+1)&&list.get(i+1)+1==list.get(i+2)&&list.get(i+2)+1==list.get(i+3)&&list.get(i+3)+1==list.get(i+4)){
							temp.add(index+1+"-"+list.get(i));
							temp.add(index+1+"-"+list.get(i+1));
							temp.add(index+1+"-"+list.get(i+2));
							temp.add(index+1+"-"+list.get(i+3));
							temp.add(index+1+"-"+list.get(i+4));
							tempList.add(temp);
						}else {
							continue;
						}
					}
				}else {
					for(int i=0;i<=list.size()-5;i++){
						ArrayList<String> temp=new ArrayList<String>();
						if(list.get(i)+1==list.get(i+1)&&list.get(i+1)+1==list.get(i+2)&&list.get(i+2)+1==list.get(i+3)&&list.get(i+3)+1==list.get(i+4)){
							temp.add(index+1+"-"+list.get(i));
							temp.add(index+1+"-"+list.get(i+1));
							temp.add(index+1+"-"+list.get(i+2));
							temp.add(index+1+"-"+list.get(i+3));
							temp.add(index+1+"-"+list.get(i+4));
							tempList.add(temp);
						}else {
							continue;
						}
					}
					if(list.get(0)==1){
						list.add(list.get(0));
						list.remove(0);
						for(int i=list.size()-5;i<=list.size()-5;i++){
							ArrayList<String> temp=new ArrayList<String>();
							if(i==list.size()-5){
								if(list.get(i)+1==list.get(i+1)&&list.get(i+1)+1==list.get(i+2)&&list.get(i+2)+1==list.get(i+3)&&list.get(i+3)==13){
									temp.add(index+1+"-"+list.get(i));
									temp.add(index+1+"-"+list.get(i+1));
									temp.add(index+1+"-"+list.get(i+2));
									temp.add(index+1+"-"+list.get(i+3));
									temp.add(index+1+"-"+list.get(i+4));
									tempList.add(temp);
								}else {
									continue;
								}
							}
						}
					}
				}
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的铁支
	 * @param player
	 * @return [[1-12, 2-12, 3-12, 4-12],[1-13, 2-13, 4-13, 3-13]]
	 */
	public static ArrayList<ArrayList<String>> bomb(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for (int index=0;index<set.size();index++) {
			ArrayList<Integer> list=set.get(index);
			if(list.size()==4){
				ArrayList<String> temp=new ArrayList<String>();
				temp.add(list.get(0)+"-"+(index+1));
				temp.add(list.get(1)+"-"+(index+1));
				temp.add(list.get(2)+"-"+(index+1));
				temp.add(list.get(3)+"-"+(index+1));
				tempList.add(temp);
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的葫芦
	 * @param player
	 * @return [[1-13, 2-13, 4-13, 3-6, 4-6],[1-13, 2-13, 4-13, 1-8, 1-8]]
	 */
	public static ArrayList<ArrayList<String>> gourd(ArrayList<String> player){
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for (int index=0;index<13;index++) {
			ArrayList<ArrayList<Integer>> tempSet=SSSSpecialCards.getListByNum(player);
			ArrayList<Integer> list=tempSet.get(index);
			if(list.size()==3){
				ArrayList<String> temp=new ArrayList<String>();
				temp.add(list.get(0)+"-"+(index+1));
				temp.add(list.get(1)+"-"+(index+1));
				temp.add(list.get(2)+"-"+(index+1));
				tempSet.get(index).remove(0);
				tempSet.get(index).remove(0);
				tempSet.get(index).remove(0);
				for(int j=0;j<tempSet.size();j++){
					ArrayList<Integer> listj=tempSet.get(j);
					if(listj.size()==2){
						ArrayList<String> tempj=new ArrayList<String>();
						tempj.add(temp.get(0));
						tempj.add(temp.get(1));
						tempj.add(temp.get(2));
						tempj.add(listj.get(0)+"-"+(j+1));
						tempj.add(listj.get(1)+"-"+(j+1));
						tempList.add(tempj);
					}
				}
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的同花
	 * @param player
	 * @return [[1-1, 1-4, 1-6, 1-8, 1-10],[2-3, 2-5, 2-6, 2-8, 2-13]]
	 */
	public static ArrayList<ArrayList<String>> sameFlower(ArrayList<String> player){
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByFlower(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for(int index=0;index<set.size();index++){
			ArrayList<Integer> list=set.get(index);
			if(list.size()==5){
				ArrayList<String> temp=new ArrayList<String>();
				for(int i=0;i<list.size();i++){
					temp.add(index+1+"-"+list.get(i));
				}
				tempList.add(temp);
			}else if (list.size()>5) {
				for(int i=0;i<list.size();i++){
					if(list.size()==6){
						ArrayList<String> temp=new ArrayList<String>();
						for(int k=0;k<list.size();k++){
							if(i!=k){
								temp.add(index+1+"-"+list.get(k));
							}
						}
						if(temp.size()==5){
							tempList.add(temp);
						}
					}else if(list.size()==7){
						for(int j=i+1;j<list.size();j++){
							ArrayList<String> temp=new ArrayList<String>();
							for(int k=0;k<list.size();k++){
								if(i!=k&&j!=k){
									temp.add(index+1+"-"+list.get(k));
								}
							}
							if(temp.size()==5){
								tempList.add(temp);
							}
						}
					}else if(list.size()==8){
						for(int j=i+1;j<list.size();j++){
							for(int l=j+1;l<list.size();l++){
								ArrayList<String> temp=new ArrayList<String>();
								for(int k=0;k<list.size();k++){
									if(i!=k&&j!=k&&l!=k){
										temp.add(index+1+"-"+list.get(k));
									}
								}
								if(temp.size()==5){
									tempList.add(temp);
								}
							}
						}
					}else if (list.size()==9) {
						for(int j=i+1;j<list.size();j++){
							for(int l=j+1;l<list.size();l++){
								for(int m=l+1;m<list.size();m++){
									ArrayList<String> temp=new ArrayList<String>();
									for(int k=0;k<list.size();k++){
										if(i!=k&&j!=k&&l!=k&&m!=k){
											temp.add(index+1+"-"+list.get(k));
										}
									}
									if(temp.size()==5){
										tempList.add(temp);
									}
								}
							}
						}
					}else if (list.size()==10) {
						for(int j=i+1;j<list.size();j++){
							for(int l=j+1;l<list.size();l++){
								for(int m=l+1;m<list.size();m++){
									for(int n=m+1;n<list.size();n++){
										ArrayList<String> temp=new ArrayList<String>();
										for(int k=0;k<list.size();k++){
											if(i!=k&&j!=k&&l!=k&&m!=k&&n!=k){
												temp.add(index+1+"-"+list.get(k));
											}
										}
										if(temp.size()==5){
											tempList.add(temp);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		for(int i=0;i<tempList.size();i++){
			for(int j=0;j<tempList.size()-1;j++){
				ArrayList<String> temp=new ArrayList<String>();
				ArrayList<String> now=tempList.get(j);
				ArrayList<String> next=tempList.get(j+1);
				if("1".equals(now.get(0).split("-")[1])){
					if ("1".equals(next.get(0).split("-")[1])) {
						if(Integer.parseInt(now.get(4).split("-")[1])>Integer.parseInt(next.get(4).split("-")[1])){
							temp.addAll(now);
							tempList.set(j, next);
							tempList.set(j+1, temp);
						}else if (Integer.parseInt(now.get(4).split("-")[1])==Integer.parseInt(next.get(4).split("-")[1])) {
							if(Integer.parseInt(now.get(3).split("-")[1])>Integer.parseInt(next.get(3).split("-")[1])){
								temp.addAll(now);
								tempList.set(j, next);
								tempList.set(j+1, temp);
							}else if (Integer.parseInt(now.get(3).split("-")[1])==Integer.parseInt(next.get(3).split("-")[1])) {
								if(Integer.parseInt(now.get(2).split("-")[1])>Integer.parseInt(next.get(2).split("-")[1])){
									temp.addAll(now);
									tempList.set(j, next);
									tempList.set(j+1, temp);
								}else if (Integer.parseInt(now.get(2).split("-")[1])==Integer.parseInt(next.get(2).split("-")[1])) {
									if(Integer.parseInt(now.get(1).split("-")[1])>Integer.parseInt(next.get(1).split("-")[1])){
										temp.addAll(now);
										tempList.set(j, next);
										tempList.set(j+1, temp);
									}
								}
							}
						}
					}else {
						temp.addAll(now);
						tempList.set(j, next);
						tempList.set(j+1, temp);
					}
				}else {
					if(Integer.parseInt(now.get(4).split("-")[1])>Integer.parseInt(next.get(4).split("-")[1])){
						temp.addAll(now);
						tempList.set(j, next);
						tempList.set(j+1, temp);
					}else if (Integer.parseInt(now.get(4).split("-")[1])==Integer.parseInt(next.get(4).split("-")[1])) {
						if(Integer.parseInt(now.get(3).split("-")[1])>Integer.parseInt(next.get(3).split("-")[1])){
							temp.addAll(now);
							tempList.set(j, next);
							tempList.set(j+1, temp);
						}else if (Integer.parseInt(now.get(3).split("-")[1])==Integer.parseInt(next.get(3).split("-")[1])) {
							if(Integer.parseInt(now.get(2).split("-")[1])>Integer.parseInt(next.get(2).split("-")[1])){
								temp.addAll(now);
								tempList.set(j, next);
								tempList.set(j+1, temp);
							}else if (Integer.parseInt(now.get(2).split("-")[1])==Integer.parseInt(next.get(2).split("-")[1])) {
								if(Integer.parseInt(now.get(1).split("-")[1])>Integer.parseInt(next.get(1).split("-")[1])){
									temp.addAll(now);
									tempList.set(j, next);
									tempList.set(j+1, temp);
								}else if (Integer.parseInt(now.get(1).split("-")[1])==Integer.parseInt(next.get(1).split("-")[1])) {
									if(Integer.parseInt(now.get(0).split("-")[1])>Integer.parseInt(next.get(0).split("-")[1])){
										temp.addAll(now);
										tempList.set(j, next);
										tempList.set(j+1, temp);
									}
								}
							}
						}
					}
				}
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的顺子
	 * @param player
	 * @return [[3-1, 1-2, 4-3, 2-4, 1-5],[2-9, 2-10, 1-11, 3-12, 4-13]]
	 */
	public static ArrayList<ArrayList<String>> flush(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		TreeSet<Integer> allSet=new TreeSet<Integer>();
		ArrayList<Integer> allList=new ArrayList<Integer>();
		for(String list:player){
			allSet.add(Integer.parseInt(list.split("-")[1]));
		}
		allList.addAll(allSet);
		if(allList.size()>4){
			for(int i=0;i<allList.size()-4;i++){
				if(allList.get(i)+4==allList.get(i+4)){
					ArrayList<ArrayList<String>> listTemp=new ArrayList<ArrayList<String>>();
					ArrayList<String> list=new ArrayList<String>();
					for(int j=0;j<5;j++){
						if(set.get(allList.get(i+j)-1).size()==1){
							if(listTemp.size()==0){
								list.add(set.get(allList.get(i+j)-1).get(0)+"-"+allList.get(i+j));
								listTemp.add(list);
							}else {
								for(int k=0;k<listTemp.size();k++){
									ArrayList<String> li=listTemp.get(k);
									li.add(set.get(allList.get(i+j)-1).get(0)+"-"+allList.get(i+j));
								}
							}
						}else {
							if(listTemp.size()==0){
								for(int k=0;k<set.get(allList.get(i+j)-1).size();k++){
									ArrayList<String> litemp=new ArrayList<String>();
									litemp.add(set.get(allList.get(i+j)-1).get(k)+"-"+allList.get(i+j));
									listTemp.add(litemp);
								}
							}else {
								int si=listTemp.size();
								for(int k=0;k<si;k++){
									for(int l=0;l<set.get(allList.get(i+j)-1).size();l++){
										if(l==0){
											ArrayList<String> li=listTemp.get(k);
											li.add(set.get(allList.get(i+j)-1).get(l)+"-"+allList.get(i+j));
										}else {
											ArrayList<String> li=listTemp.get(k);
											ArrayList<String> litemp=new ArrayList<String>();
											for(int m=0;m<li.size()-1;m++){
												litemp.add(li.get(m));
											}
											litemp.add(set.get(allList.get(i+j)-1).get(l)+"-"+allList.get(i+j));
											listTemp.add(litemp);
										}
									}
								}
							}
						}
					}
					tempList.addAll(listTemp);
				}
				if(allList.get(0)==1&&allList.get(allList.size()-1)==13&&i==allList.size()-5){
					ArrayList<ArrayList<String>> listTemp=new ArrayList<ArrayList<String>>();
					if(allList.get(i+1)+3==allList.get(i+4)){
						ArrayList<String> list=new ArrayList<String>();
						for(int j=0;j<4;j++){
							if(set.get(allList.get(i+j+1)-1).size()==1){
								if(listTemp.size()==0){
									list.add(set.get(allList.get(i+j+1)-1).get(0)+"-"+allList.get(i+j+1));
									listTemp.add(list);
								}else {
									for(int k=0;k<listTemp.size();k++){
										ArrayList<String> li=listTemp.get(k);
										li.add(set.get(allList.get(i+j+1)-1).get(0)+"-"+allList.get(i+j+1));
									}
								}
							}else {
								if(listTemp.size()==0){
									for(int k=0;k<set.get(allList.get(i+j+1)-1).size();k++){
										ArrayList<String> litemp=new ArrayList<String>();
										litemp.add(set.get(allList.get(i+j+1)-1).get(k)+"-"+allList.get(i+j+1));
										listTemp.add(litemp);
									}
								}else {
									int si=listTemp.size();
									for(int k=0;k<si;k++){
										for(int l=0;l<set.get(allList.get(i+j+1)-1).size();l++){
											if(l==0){
												ArrayList<String> li=listTemp.get(k);
												li.add(set.get(allList.get(i+j+1)-1).get(l)+"-"+allList.get(i+j+1));
											}else {
												ArrayList<String> li=listTemp.get(k);
												ArrayList<String> litemp=new ArrayList<String>();
												for(int m=0;m<li.size()-1;m++){
													litemp.add(li.get(m));
												}
												litemp.add(set.get(allList.get(i+j+1)-1).get(l)+"-"+allList.get(i+j+1));
												listTemp.add(litemp);
											}
										}
									}
								}
							}
						}
						if(set.get(0).size()==1){
							for(int k=0;k<listTemp.size();k++){
								ArrayList<String> li=listTemp.get(k);
								li.add(set.get(0).get(0)+"-"+1);
							}
						}else if(set.get(0).size()>1){
							int si=listTemp.size();
							for(int k=0;k<si;k++){
								for(int l=0;l<set.get(0).size();l++){
									if(l==0){
										ArrayList<String> li=listTemp.get(k);
										li.add(set.get(0).get(l)+"-"+1);
									}else {
										ArrayList<String> li=listTemp.get(k);
										ArrayList<String> litemp=new ArrayList<String>();
										for(int m=0;m<li.size()-1;m++){
											litemp.add(li.get(m));
										}
										litemp.add(set.get(0).get(l)+"-"+1);
										listTemp.add(litemp);
									}
								}
							}
						}
						tempList.addAll(listTemp);
					}
				}
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的三条
	 * @param player
	 * @return [[3-1, 1-1, 4-1],[2-9, 3-9, 1-9]]
	 */
	public static ArrayList<ArrayList<String>> three(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for(int i=0;i<set.size();i++){
			if(set.get(i).size()==3){
				ArrayList<String> temp=new ArrayList<String>();
				for(int j=0;j<3;j++){
					temp.add(set.get(i).get(j)+"-"+(i+1));
				}
				tempList.add(temp);
			}else if(set.get(i).size()==4){
				for(int j=0;j<4;j++){
					ArrayList<String> temp=new ArrayList<String>();
					for(int k=0;k<4;k++){
						if(k!=j){
							temp.add(set.get(i).get(k)+"-"+(i+1));
						}
					}
					tempList.add(temp);
				}
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的两对
	 * @param player
	 * @return [[3-1, 1-1, 4-4, 2-4],[2-9, 3-9, 1-11, 3-11]]
	 */
	public static ArrayList<ArrayList<String>> two(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for(int i=0;i<set.size();i++){
			if(set.get(i).size()==2){
				ArrayList<String> temp=new ArrayList<String>();
				for(int j=0;j<2;j++){
					temp.add(set.get(i).get(j)+"-"+(i+1));
				}
				for(int j=i+1;j<set.size();j++){
					if(set.get(j).size()==2){
						ArrayList<String> ttemp=new ArrayList<String>();
						ttemp.add(temp.get(0));
						ttemp.add(temp.get(1));
						for(int k=0;k<2;k++){
							ttemp.add(set.get(j).get(k)+"-"+(j+1));
						}
						tempList.add(ttemp);
					}
				}
				
			}
		}
		return tempList;
	}
	
	/**
	 * 获取牌中的一对
	 * @param player
	 * @return [[3-1, 1-1],[2-9, 3-9]]
	 */
	public static ArrayList<ArrayList<String>> one(ArrayList<String> player) {
		ArrayList<ArrayList<Integer>> set=SSSSpecialCards.getListByNum(player);
		ArrayList<ArrayList<String>> tempList=new ArrayList<ArrayList<String>>();
		for(int i=0;i<set.size();i++){
			if(set.get(i).size()==2){
				ArrayList<String> temp=new ArrayList<String>();
				for(int j=0;j<2;j++){
					temp.add(set.get(i).get(j)+"-"+(i+1));
				}
				tempList.add(temp);
			}
		}
		return tempList;
	}
}
