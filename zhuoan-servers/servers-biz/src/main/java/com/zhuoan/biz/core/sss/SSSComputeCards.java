package com.zhuoan.biz.core.sss;


import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;

/**
 * 比对算分和牌型计算
 * @author hxp
 *
 */
public class SSSComputeCards {
	
	/**
	 * 牌型不符合时
	 */
	private static final int none=-1;
	
	/**
	 * 牌型为散牌（乌龙）时
	 */
	private static final int zero=0;
	
	/**
	 * 牌型为一对时
	 */
	private static final int one=1;
	
	/**
	 * 牌型为两对时
	 */
	private static final int two=2;
	
	/**
	 * 牌型为三条时
	 */
	private static final int three=3;
	
	/**
	 * 牌型为顺子时
	 */
	private static final int flush=4;
	
	/**
	 * 牌型为同花时
	 */
	private static final int sameFlower=5;

	/**
	 * 牌型为葫芦时
	 */
	private static final int gourd=6;
	
	/**
	 * 牌型为铁支时
	 */
	private static final int bomb=7;
	
	/**
	 * 牌型为同花顺时
	 */
	private static final int flushByFlower=8;
	
	/**
	 * 牌型为五同时
	 */
	private static final int both=9;
	/**
	 * 牌型为冲三时
	 */
	private static final int dashThree=11;
	/**
	 * 牌型为中墩葫芦时
	 */
	private static final int mideGourd=10;
	
	/**
	 * 比对算分
	 * @param card1
	 * @param card2
	 * @return {"result":[[{"score":-1,"type":0},{"score":2,"type":6},{"score":5,"type":8}],[{"score":1,"type":0},{"score":-2,"type":0},{"score":-5,"type":0}]],"A":6,"B":-6}
	 */
	public static JSONObject compare(String[] card1, String[] card2){
		
		JSONArray p1=new JSONArray();
		JSONArray p2=new JSONArray();
		JSONArray threeP1=new JSONArray();
		JSONArray threeP2=new JSONArray();
		JSONArray five1P1=new JSONArray();
		JSONArray five1P2=new JSONArray();
		JSONArray five2P1=new JSONArray();
		JSONArray five2P2=new JSONArray();
		
		for(int i=0;i<card1.length;i++){
			if(i<3){
				threeP1.add(card1[i]);
				threeP2.add(card2[i]);
			}else if (i<8) {
				five1P1.add(card1[i]);
				five1P2.add(card2[i]);
			}else {
				five2P1.add(card1[i]);
				five2P2.add(card2[i]);
			}
		}
		p1.add(threeP1);
		p1.add(five1P1);
		p1.add(five2P1);
		p2.add(threeP2);
		p2.add(five1P2);
		p2.add(five2P2);
		
		JSONObject result=new JSONObject();
		JSONArray resultSum=new JSONArray();
		JSONArray resultA=new JSONArray();
		JSONArray resultB=new JSONArray();
		
		int sumA=0;
		int sumB=0;
		
		for (int k=0;k<p1.size();k++) {
			JSONArray player1=(JSONArray) p1.get(k);
			JSONArray player2=(JSONArray) p2.get(k);

			int type1=none;
			int type2=none;
			
			int bothPlayer1=isBoth(player1);
			int flushByflowerPlayer1=none;
			int bombPlayer1=none;
			int gourdPlayer1=none;
			int sameFlowerPlayer1=none;
			int flushPlayer1=none;
			int threePlayer1=none;
			int twoPlayer1=none;
			int onePlayer1=none;
			if(bothPlayer1==none){
				flushByflowerPlayer1=isFlushByFlower(player1);
				if(flushByflowerPlayer1==none){
					bombPlayer1=isBomb(player1);
					if(bombPlayer1==none){
						gourdPlayer1=isGourd(player1);
						if(gourdPlayer1==none){
							sameFlowerPlayer1=isSameFlower(player1);
							if(sameFlowerPlayer1==none){
								flushPlayer1=isFlush(player1);
								if(flushPlayer1==none){
									threePlayer1=isThree(player1);
									if(threePlayer1==none){
										twoPlayer1=isTwo(player1);
										if(twoPlayer1==none){
											onePlayer1=isOne(player1);
											if(onePlayer1==zero){
												type1=zero;
											}else {
												type1=onePlayer1;
											}
										}else {
											type1=twoPlayer1;
										}
									}else {
										type1=threePlayer1;
										if (k==0) {
											type1=dashThree;
										}
									}
								}else {
									type1=flushPlayer1;
								}
							}else {
								type1=sameFlowerPlayer1;
							}
						}else {
							type1=gourdPlayer1;
							if (k==1) {
								type1=mideGourd;
							}
						}
					}else {
						type1=bombPlayer1;
					}
				}else {
					type1=flushByflowerPlayer1;
				}
			}else {
				type1=bothPlayer1;
			}
			
			
			
			int bothPlayer2=isBoth(player2);
			int flushByflowerPlayer2=none;
			int bombPlayer2=none;
			int gourdPlayer2=none;
			int sameFlowerPlayer2=none;
			int flushPlayer2=none;
			int threePlayer2=none;
			int twoPlayer2=none;
			int onePlayer2=none;
			if(bothPlayer2==none){
				flushByflowerPlayer2=isFlushByFlower(player2);
				if(flushByflowerPlayer2==none){
					bombPlayer2=isBomb(player2);
					if(bombPlayer2==none){
						gourdPlayer2=isGourd(player2);
						if(gourdPlayer2==none){
							sameFlowerPlayer2=isSameFlower(player2);
							if(sameFlowerPlayer2==none){
								flushPlayer2=isFlush(player2);
								if(flushPlayer2==none){
									threePlayer2=isThree(player2);
									if(threePlayer2==none){
										twoPlayer2=isTwo(player2);
										if(twoPlayer2==none){
											onePlayer2=isOne(player2);
											if(onePlayer2==zero){
												type2=zero;
											}else {
												type2=onePlayer2;
											}
										}else {
											type2=twoPlayer2;
										}
									}else {
										type2=threePlayer2;
										if (k==0) {
											type2=dashThree;
										}
									}
								}else {
									type2=flushPlayer2;
								}
							}else {
								type2=sameFlowerPlayer2;
							}
						}else {
							type2=gourdPlayer2;
							if (k==1) {
								type2=mideGourd;
							}
						}
					}else {
						type2=bombPlayer2;
					}
				}else {
					type2=flushByflowerPlayer2;
				}
			}else {
				type2=bothPlayer2;
			}
			
			
			JSONObject temp=new JSONObject();
			int score=0;
			
			//五同
			if(bothPlayer1>bothPlayer2){
				score=1;
				if(k==1){
					score+=19;
				}else if (k==2) {
					score+=9;
				}
			}else if (bothPlayer1==bothPlayer2) {
				if(bothPlayer1>0){
					int i=compareBoth(player1,player2);
					if(i==1){
						score+=1;
						if(k==1){
							score+=19;
						}else if (k==2) {
							score+=9;
						}
					}else if(i==-1){
						score+=-1;
						if(k==1){
							score+=-19;
						}else if (k==2) {
							score+=-9;
						}
					}
				}else {
					//同花顺
					if(flushByflowerPlayer1>flushByflowerPlayer2){
						score=1;
						if(k==1){
							score+=9;
						}else if (k==2) {
							score+=4;
						}
					}else if (flushByflowerPlayer1==flushByflowerPlayer2) {
						if(flushByflowerPlayer1>0){
							int i=compareFlushByflower(player1,player2);
							if(i==1){
								score+=1;
								if(k==1){
									score+=9;
								}else if (k==2) {
									score+=4;
								}
							}else if(i==-1){
								score+=-1;
								if(k==1){
									score+=-9;
								}else if (k==2) {
									score+=-4;
								}
							}
						}else {
							//铁支
							if(bombPlayer1>bombPlayer2){
								score+=1;
								if(k==1){
									score+=7;
								}else if (k==2) {
									score+=3;
								}
							}else if (bombPlayer1==bombPlayer2) {
								if(bombPlayer1>0){
									int i=compareBomb(player1,player2);
									if(i==1){
										score+=1;
										if(k==1){
											score+=7;
										}else if (k==2) {
											score+=3;
										}
									}else if(i==-1){
										score+=-1;
										if(k==1){
											score+=-7;
										}else if (k==2) {
											score+=-3;
										}
									}
								}else {
									//葫芦
									if(gourdPlayer1>gourdPlayer2){
										score+=1;
										if(k==1){
											score+=1;
										}
									}else if (gourdPlayer1==gourdPlayer2) {
										if(gourdPlayer1>0){
											int i=compareGourd(player1,player2);
											if(i==1){
												score+=1;
												if(k==1){
													score+=1;
												}
											}else if(i==-1){
												score+=-1;
												if(k==1){
													score+=-1;
												}
											}
										}else {
											//同花
											if(sameFlowerPlayer1>sameFlowerPlayer2){
												score+=1;
											}else if (sameFlowerPlayer1==sameFlowerPlayer2) {
												if(sameFlowerPlayer1>0){
													int i=compareSameFlower(player1,player2);
													if(i==1){
														score+=1;
													}else if(i==-1){
														score+=-1;
													}
												}else {
													//顺子
													if(flushPlayer1>flushPlayer2){
														score+=1;
													}else if (flushPlayer1==flushPlayer2) {
														if(flushPlayer1>0){
															int i=compareFlush(player1,player2);
															if(i==1){
																score+=1;
															}else if(i==-1){
																score+=-1;
															}
														}else {
															//三条
															if(threePlayer1>threePlayer2){
																score+=1;
																if(k==0){
																	score+=2;
																}
															}else if (threePlayer1==threePlayer2) {
																if(threePlayer1>0){
																	int i=compareThree(player1,player2);
																	if(i==1){
																		score+=1;
																		if(k==0){
																			score+=2;
																		}
																	}else if(i==-1){
																		score+=-1;
																		if(k==0){
																			score+=-2;
																		}
																	}
																}else {
																	//两对
																	if(twoPlayer1>twoPlayer2){
																		score+=1;
																	}else if (twoPlayer1==twoPlayer2) {
																		if(twoPlayer1>0){
																			int i=compareTwo(player1,player2);
																			if(i==1){
																				score+=1;
																			}else if(i==-1){
																				score+=-1;
																			}
																		}else {
																			//一对
																			if(onePlayer1>onePlayer2){
																				score+=1;
																			}else if (onePlayer1==onePlayer2) {
																				if(onePlayer1>0){
																					int i=compareOne(player1,player2);
																					if(i==1){
																						score+=1;
																					}else if(i==-1){
																						score+=-1;
																					}
																				}else {
																					//乌龙
																					int i=compareZero(player1,player2);
																					if(i==1){
																						score+=1;
																					}else if(i==-1){
																						score+=-1;
																					}
																				}
																			}else if(onePlayer1<onePlayer2){
																				score+=-1;
																			}
																		}
																	}else if(twoPlayer1<twoPlayer2){
																		score+=-1;
																	}
																}
															}else if(threePlayer1<threePlayer2){
																score+=-1;
																if(k==0){
																	score+=-2;
																}
															}
														}
													}else if(flushPlayer1<flushPlayer2){
														score+=-1;
													}
												}
											}else if(sameFlowerPlayer1<sameFlowerPlayer2){
												score+=-1;
											}
										}
									}else if(gourdPlayer1<gourdPlayer2){
										score+=-1;
										if(k==1){
											score+=-1;
										}
									}
								}
							}else if(bombPlayer1<bombPlayer2){
								score+=-1;
								if(k==1){
									score+=-7;
								}else if (k==2) {
									score+=-3;
								}
							}
						}
					}else if(flushByflowerPlayer1<flushByflowerPlayer2){
						score+=-1;
						if(k==1){
							score+=-9;
						}else if (k==2) {
							score+=-4;
						}
					}
				}
			}else if (bothPlayer1<bothPlayer2) {
				score+=-1;
				if(k==1){
					score+=-19;
				}else if (k==2) {
					score+=-9;
				}
			}
			
			
			JSONObject tempResult1=new JSONObject();
			JSONObject tempResult2=new JSONObject();
			
			tempResult1.put("score", score);
			tempResult1.put("type", type1);
			tempResult2.put("score", -score);
			tempResult2.put("type", type2);
			
			resultA.add(tempResult1);
			resultB.add(tempResult2);
			sumA+=score;
			sumB+=-score;
		}
		
		resultSum.add(resultA);
		resultSum.add(resultB);
		
		result.put("result", resultSum);
		result.put("A", sumA);
		result.put("B", sumB);
		
		return result;
	}
	
	
	/**
	 * 牌型判断
	 * @param player
	 * @return ["乌龙","同花","同花"]
	 */
	public static JSONArray judge(JSONArray player){
		JSONArray result=new JSONArray();
		JSONArray num=new JSONArray();
		
		ListIterator<Object> it = (ListIterator<Object>) player.iterator();
		JSONArray temp=new JSONArray();
		JSONArray temp1=new JSONArray();
		JSONArray temp2=new JSONArray();
		JSONArray temp3=new JSONArray();
		while (it.hasNext()) {
			String string=(String) it.next();
			if(temp1.size()<3){
				temp1.add(string);
			}else if (temp2.size()<5) {
				temp2.add(string);
			}else if (temp3.size()<5) {
				temp3.add(string);
			}
		}
		temp.add(temp1);
		temp.add(temp2);
		temp.add(temp3);
		ListIterator<Object> itTemp = (ListIterator<Object>) temp.iterator();
		while (itTemp.hasNext()) {
			JSONArray playerJsonArray=(JSONArray) itTemp.next();
			int bothPlayer=isBoth(playerJsonArray);
			int flushByflowerPlayer=none;
			int bombPlayer=none;
			int gourdPlayer=none;
			int sameFlowerPlayer=none;
			int flushPlayer=none;
			int threePlayer=none;
			int twoPlayer=none;
			int onePlayer=none;
			if(bothPlayer==none){
				flushByflowerPlayer=isFlushByFlower(playerJsonArray);
				if(flushByflowerPlayer==none){
					bombPlayer=isBomb(playerJsonArray);
					if(bombPlayer==none){
						gourdPlayer=isGourd(playerJsonArray);
						if(gourdPlayer==none){
							sameFlowerPlayer=isSameFlower(playerJsonArray);
							if(sameFlowerPlayer==none){
								flushPlayer=isFlush(playerJsonArray);
								if(flushPlayer==none){
									threePlayer=isThree(playerJsonArray);
									if(threePlayer==none){
										twoPlayer=isTwo(playerJsonArray);
										if(twoPlayer==none){
											onePlayer=isOne(playerJsonArray);
											if(onePlayer==zero){
												result.add("乌龙");
												num.add(zero);
											}else {
												result.add("一对");
												num.add(onePlayer);
											}
										}else {
											result.add("两对");
											num.add(twoPlayer);
										}
									}else {
										if(playerJsonArray.size()==3){
											result.add("冲三");
										}else {
											result.add("三条");
										}
										num.add(threePlayer);
									}
								}else {
									result.add("顺子");
									num.add(flushPlayer);
								}
							}else {
								result.add("同花");
								num.add(sameFlowerPlayer);
							}
						}else {
							if(it.hasPrevious()&&it.hasNext()){
								result.add("中墩葫芦");
							}else {
								result.add("葫芦");
							}
							num.add(gourdPlayer);
						}
					}else {
						result.add("铁支");
						num.add(bombPlayer);
					}
				}else {
					result.add("同花顺");
					num.add(flushByflowerPlayer);
				}
			}else {
				result.add("五同");
				num.add(bothPlayer);
			}
		}

		if(num.getInt(0)>num.getInt(1)||num.getInt(1)>num.getInt(2)){
			result.clear();
			result.add("倒水");
		}else {
			if(num.getInt(0)==num.getInt(1)){
				if(num.getInt(0)==zero){
					int i=compareZero(temp.getJSONArray(0), temp.getJSONArray(1));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(0)==one) {
					int i=compareOne(temp.getJSONArray(0), temp.getJSONArray(1));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}
			}
			if(num.getInt(1)==num.getInt(2)){
				if(num.getInt(1)==zero){
					int i=compareZero(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==one) {
					int i=compareOne(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==two) {
					int i=compareTwo(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==three) {
					int i=compareThree(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==flush) {
					int i=compareFlush(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==sameFlower) {
					int i=compareSameFlower(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==gourd) {
					int i=compareGourd(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==bomb) {
					if(compareBomb(temp.getJSONArray(1), temp.getJSONArray(2))==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==flushByFlower) {
					int i=compareFlushByflower(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}else if (num.getInt(1)==both) {
					int i=compareBoth(temp.getJSONArray(1), temp.getJSONArray(2));
					if(i==1){
						result.clear();
						result.add("倒水");
					}
				}
			}
		}
		return result;
	}
	
	
	/**
	 * 以花色分类传入的牌
	 * @param player
	 * @return [[6, 8, 10, 13], [11], [], []]
	 */
	public static ArrayList<ArrayList<Integer>> getListByFlower(JSONArray player) {
		
		ArrayList<ArrayList<Integer>> set=new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> set1=new ArrayList<Integer>();
		ArrayList<Integer> set2=new ArrayList<Integer>();
		ArrayList<Integer> set3=new ArrayList<Integer>();
		ArrayList<Integer> set4=new ArrayList<Integer>();
		
		Iterator<Object> it = player.iterator();
		
		while (it.hasNext()) {
			String string=(String) it.next();
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
	 * 以数字分类传入的牌
	 * @param player
	 * @return [[], [], [], [], [], [1], [], [4], [3], [], [4], [], [1]]
	 */
	public static ArrayList<ArrayList<Integer>> getListByNum(JSONArray player) {
		
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
		
		Iterator<Object> it = player.iterator();
		
		while (it.hasNext()) {
			String string=(String) it.next();
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
	 * 按数字从小到大排序
	 * @param player
	 * @return [6, 8, 10, 11, 13]
	 */
	public static TreeSet<Integer> sortByNum(JSONArray player){
		TreeSet<Integer> p=new TreeSet<Integer>();
		Iterator<Object> it = player.iterator();
		while (it.hasNext()) {
			String string=(String) it.next();
			p.add(Integer.parseInt(string.split("-")[1]));
		}
		return p;
	}
	
	/**
	 * 判断是否只有一对
	 * @param player
	 * @return 有的话返回1，没有的话返回0
	 */
	public static int isOne(JSONArray player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==2){
				i++;
			}
		}
		if(i==1){
			return one;
		}else {
			return zero;
		}
	}
	
	/**
	 * 判断是否只有两对
	 * @param player
	 * @return 有的话返回2，没有的话返回-1
	 */
	public static int isTwo(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==2){
				i++;
			}
		}
		if(i==2){
			return two;
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否有三条
	 * @param player
	 * @return 有的话返回3，没有的话返回-1
	 */
	public static int isThree(JSONArray player){
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==3){
				i++;
			}
		}
		if(i==1){
			return three;
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否是顺子
	 * @param player
	 * @return 是的话返回4，不是的话返回-1
	 */
	public static int isFlush(JSONArray player){
		if(player.size()==3){
			return none;
		}
		TreeSet<Integer> treeSet=new TreeSet<Integer>();
		Iterator<Object> it = player.iterator();
		
		while (it.hasNext()) {
			String string=(String) it.next();
			treeSet.add(Integer.parseInt(string.split("-")[1]));
		}
		if(treeSet.size()==5){
			if(treeSet.contains(1)&&treeSet.contains(13)){
				if(treeSet.contains(10)&&treeSet.contains(11)&&treeSet.contains(12)){
					return flush;
				}else {
					return none;
				}
			}else {
				if(treeSet.first()+4==treeSet.last()){
					return flush;
				}else {
					return none;
				}
			}
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否是同花
	 * @param player
	 * @return 是的话返回5，不是的话返回-1
	 */
	public static int isSameFlower(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByFlower(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==5){
				i++;
			}
		}
		if(i==1){
			return sameFlower;
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否是葫芦
	 * @param player
	 * @return 是的话返回6，不是的话返回-1
	 */
	public static int isGourd(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==2){
				i++;
			}
			if(temp.size()==3){
				i+=2;
			}
		}
		if(i==3){
			return gourd;
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否是铁支
	 * @param player
	 * @return 是的话返回7，不是的话返回-1
	 */
	public static int isBomb(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==4){
				i++;
			}
		}
		if(i==1){
			return bomb;
		}else {
			return none;
		}
	}
	
	/**
	 * 判断是否是同花顺
	 * @param player
	 * @return 是的话返回8，不是的话返回-1
	 */
	public static int isFlushByFlower(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByFlower(player);
		for(ArrayList<Integer> temp:set){
			if(temp.size()==5){
				if(temp.get(0)==1&&temp.get(4)==13){
					if(temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
						return flushByFlower;
					}
				}else {
					if(temp.get(0)+1==temp.get(1)&&temp.get(1)+1==temp.get(2)&&temp.get(2)+1==temp.get(3)&&temp.get(3)+1==temp.get(4)){
						return flushByFlower;
					}
				}
			}
		}
		return none;
	}
	
	/**
	 * 判断是否是五同
	 * @param player
	 * @return 是的话返回9，不是的话返回-1
	 */
	public static int isBoth(JSONArray player){
		if(player.size()==3){
			return none;
		}
		ArrayList<ArrayList<Integer>> set=getListByNum(player);
		int i=0;
		for(ArrayList<Integer> temp:set){
			if(temp.size()==5){
				i++;
			}
		}
		if(i==1){
			return both;
		}else {
			return none;
		}
	}
	
	/**
	 * 五同之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回true,player1<player2 返回false
	 */
	public static int compareBoth(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		int t1=0;
		int t2=0;
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==5) {
				t1=i+1;
				break;
			}
		}
		for(int i=0;i<p2.size();i++){
			if (p2.get(i).size()==5) {
				t2=i+1;
				break;
			}
		}
		if(t1>t2){
			if(t2==1){
				return -1;
			}
			return 1;
		}else if(t1<t2){
			if(t1==1){
				return 1;
			}
			return -1;
		}else {
			return 0;
		}
	}
	
	/**
	 * 同花顺之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareFlushByflower(JSONArray player1, JSONArray player2){
		TreeSet<Integer> p1=sortByNum(player1);
		TreeSet<Integer> p2=sortByNum(player2);
		
		if(p1.contains(1)){
			if(p2.contains(1)){
				if(p1.last()>p2.last()){
					return 1;
				}else if(p1.last()<p2.last()){
					return -1;
				}
			}else {
				return 1;
			}
		}else {
			if(p2.contains(1)){
				return -1;
			}else {
				if(p1.last()>p2.last()){
					return 1;
				}else if(p1.last()<p2.last()){
					return -1;
				}
			}
		}
		return 0;
	}

	/**
	 * 铁支之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareBomb(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		int t1=0;
		int t2=0;
		int s1=0;
		int s2=0;
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==4) {
				t1=i+1;
				
			}
			if (p1.get(i).size()==1) {
				s1=i+1;
				
			}
		}
		for(int i=0;i<p2.size();i++){
			if (p2.get(i).size()==4) {
				t2=i+1;
				
			}
			if (p2.get(i).size()==1) {
				s2=i+1;
				
			}
		}
		if(t1>t2){
			if(t2==1){
				return -1;
			}else {
				return 1;
			}
			
		}else if(t1<t2){
			if(t1==1){
				return 1;
			}else {
				return -1;
			}
		}else {
			if(s1>s2){
				return 1;
			}else if (s1==s2) {
				return 0;
			}else {
				return -1;
			}
		}
	}
	
	/**
	 * 葫芦之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareGourd(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		int t1=0;
		int t2=0;
		int s1=0;
		int s2=0;
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==3) {
				t1=i+1;
			}
			if (p1.get(i).size()==2) {
				s1=i+1;
			}
			if(t1>0&&s1>0){
				break;
			}
		}
		for(int i=0;i<p2.size();i++){
			if (p2.get(i).size()==3) {
				t2=i+1;
			}
			if (p2.get(i).size()==2) {
				s2=i+1;
			}
			if(t2>0&&s2>0){
				break;
			}
		}
		if(t1>t2){
			if(t2==1){
				return -1;
			}
			return 1;
		}else if(t1<t2){
			if(t1==1){
				return 1;
			}
			return -1;
		}else {
			if(s1>s2){
				if(s2==1){
					return -1;
				}
				return 1;
			}else if(s1<s2){
				if(s1==1){
					return 1;
				}
				return -1;
			}else {
				return 0;
			}
		}
	}
	
	/**
	 * 同花之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareSameFlower(JSONArray player1, JSONArray player2){
		TreeSet<Integer> p1=sortByNum(player1);
		TreeSet<Integer> p2=sortByNum(player2);
		if (p1.size()<p2.size()) {
			return 1;
		}else if(p1.size()>p2.size()){
			return -1;
		}else{
			if (p1.size()==3&&p2.size()==3) {
				return	compareTwo(player1, player2);
			}else if(p1.size()==4&&p2.size()==4){
				return	compareOne(player1, player2);
			}else{
				if(p1.contains(1)){
					if(p2.contains(1)){
						/*for(int i=0;i<p1.size();i++){
							if(p1.last()>p2.last()){
								return 1;
							}else if(p1.last()<p2.last()){
								return -1;
							}else {
								p1.remove(p1.last());
								p2.remove(p2.last());
							}
						}*/
						for (int i = p1.size()-1; i >-1; i--) {
							if(p1.last()>p2.last()){
								return 1;
							}else if(p1.last()<p2.last()){
								return -1;
							}else {
								p1.remove(p1.last());
								p2.remove(p2.last());
							}
						}
					}else {
						return 1;
					}
				}else {
					if(p2.contains(1)){
						return -1;
					}else {
						for(int i=0;i<p1.size();i++){
							if(p1.last()>p2.last()){
								return 1;
							}else if(p1.last()<p2.last()){
								return -1;
							}else {
								p1.remove(p1.last());
								p2.remove(p2.last());
								i--;
							}
						}
					}
				}
			}
			
			
		}
		return 0;
	}
	
	/**
	 * 顺子之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareFlush(JSONArray player1, JSONArray player2){
		TreeSet<Integer> p1=sortByNum(player1);
		TreeSet<Integer> p2=sortByNum(player2);
		
		if(p1.contains(1)){
			if(p2.contains(1)){
				if(p1.last()>p2.last()){
					return 1;
				}else if(p1.last()<p2.last()){
					return -1;
				}
			}else {
				return 1;
			}
		}else {
			if(p2.contains(1)){
				return -1;
			}else {
				if(p1.last()>p2.last()){
					return 1;
				}else if(p1.last()<p2.last()){
					return -1;
				}
			}
		}
		return 0;
	}
	
	/**
	 * 三条之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1==player2 返回0,player1<player2 返回-1
	 */
	public static int compareThree(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		int t1=0;
		int t2=0;
		ArrayList<Integer> s1=new ArrayList<Integer>();
		ArrayList<Integer> s2=new ArrayList<Integer>();
		
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==3) {
				t1=i+1;
				if(player1.size()==3){
					break;
				}
			}
			if (p1.get(i).size()==1) {
				s1.add(i+1);
			}
			if(t1>0&&s1.size()==2){
				break;
			}
		}
		for(int i=0;i<p2.size();i++){
			if (p2.get(i).size()==3) {
				t2=i+1;
				if(player2.size()==3){
					break;
				}
			}
			if (p2.get(i).size()==1) {
				s2.add(i+1);
			}
			if(t2>0&&s2.size()==2){
				break;
			}
		}
		if(t1>t2){
			if(t2==1){
				return -1;
			}
			return 1;
		}else if(t1==t2){
			if(s1.size()==0&&s2.size()==0){
				return 0;
			}
			if(s1.contains(1)&&!s2.contains(1)){
				return 1;
			}else if (!s1.contains(1)&&s2.contains(1)) {
				return -1;
			}else {
				if(s1.get(1)>s2.get(1)){
					return 1;
				}else if (s1.get(1)<s2.get(1)) {
					return -1;
				}else {
					if(s1.get(0)>s2.get(0)){
						return 1;
					}else if (s1.get(0)<s2.get(0)) {
						return -1;
					}else {
						return 0;
					}
				}
			}
		}else {
			if(t1==1){
				return 1;
			}
			return -1;
		}
	}
	
	/**
	 * 两对之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareTwo(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		
		ArrayList<Integer> t1=new ArrayList<Integer>();
		ArrayList<Integer> t2=new ArrayList<Integer>();
		int s1=0;
		int s2=0;
		
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==2) {
				t1.add(i+1);
			}
			if (p1.get(i).size()==1) {
				s1=i+1;
			}
		}
		for(int i=0;i<p1.size();i++){
			if (p2.get(i).size()==2) {
				t2.add(i+1);
			}
			if (p2.get(i).size()==1) {
				s2=i+1;
			}
		}
	
		if(t1.get(0)==1){
			if(t2.get(0)==1){
				if(t1.get(1)>t2.get(1)){
					return 1;
				}else if (t1.get(1)<t2.get(1)) {
					return -1;
				}else if (t1.get(1)==t2.get(1)) {
					if(s1==s2){
						return 0;
					}else {
						if(s1==1){
							return 1;
						}else if(s2==1){
							return -1;
						}else {
							if(s1>s2){
								return 1;
							}else if (s1<s2) {
								return -1;
							}
						}
					}
				}
			}else {
				return 1;
			}
		}else {
			if(t2.get(0)==1){
				return -1;
			}else {
				if(t1.get(1)>t2.get(1)){
					return 1;
				}else if (t1.get(1)<t2.get(1)) {
					return -1;
				}else if (t1.get(1)==t2.get(1)) {
					if (t1.get(0)>t2.get(0)) {
						return 1;
					}else if(t1.get(0)<t2.get(0)){
						return -1;
					}else{
						if(s1==s2){
							return 0;
						}else {
							if(s1==1){
								return 1;
							}else if(s2==1){
								return -1;
							}else {
								if(s1>s2){
									return 1;
								}else if (s1<s2) {
									return -1;
								}
							}
						}
					}				
				}
			}
		}
		return 0;
	}
	
	/**
	 * 一对之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareOne(JSONArray player1, JSONArray player2){
		ArrayList<ArrayList<Integer>> p1=getListByNum(player1);
		ArrayList<ArrayList<Integer>> p2=getListByNum(player2);
		
		ArrayList<Integer> t1=new ArrayList<Integer>();
		ArrayList<Integer> t2=new ArrayList<Integer>();
		int s1=0;
		int s2=0;
		
		for(int i=0;i<p1.size();i++){
			if (p1.get(i).size()==2) {
				s1=i+1;
			}
			if (p1.get(i).size()==1) {
				t1.add(i+1);
			}
		}
		for(int i=0;i<p1.size();i++){
			if (p2.get(i).size()==2) {
				s2=i+1;
			}
			if (p2.get(i).size()==1) {
				t2.add(i+1);
			}
		}
		if(s1==s2){
			if(t1.get(0)==1&&t2.get(0)!=1){
				return 1;
			}else if(t1.get(0)!=1&&t2.get(0)==1){
				return -1;
			}else {
				for(int i=0;i<t1.size();i++){
					if(t1.get(t1.size()-1)>t2.get(t2.size()-1)){
						return 1;
					}else if (t1.get(t1.size()-1)<t2.get(t2.size()-1)) {
						return -1;
					}else if (t1.get(t1.size()-1)==t2.get(t2.size()-1)) {
						t1.remove(t1.size()-1);
						t2.remove(t2.size()-1);
						i--;
					}
				}
			}
			
		}else {
			if(s1==1){
				return 1;
			}else if (s2==1) {
				return -1;
			}else {
				if(s1>s2){
					return 1;
				}else {
					return -1;
				}
			}
		}
		return 0;
	}

	/**
	 * 乌龙之间的对比
	 * @param player1
	 * @param player2
	 * @return player1>player2 返回1,player1<player2 返回-1,player1=player2 返回0
	 */
	public static int compareZero(JSONArray player1, JSONArray player2){
		TreeSet<Integer> p1=sortByNum(player1);
		TreeSet<Integer> p2=sortByNum(player2);
		
		if(p1.first()==1&&p2.first()!=1){
			return 1;
		}else if (p1.first()!=1&&p2.first()==1) {
			return -1;
		}else {
			for(int i=0;i<p1.size();i++){
				if(p1.last()>p2.last()){
					return 1;
				}else if (p1.last()<p2.last()) {
					return -1;
				}else if (p1.last()==p2.last()) {
					p1.remove(p1.last());
					p2.remove(p2.last());
					i--;
				}
			}
		}
		return 0;
	}
	
}
