package com.zhuoan.biz.core.nn;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 牌的比较
 */
public class PackerCompare {

    private final static Logger logger = LoggerFactory.getLogger(PackerCompare.class);
	
	//得到赢得人
	public static UserPacket getWin(UserPacket up1,UserPacket up2){
		if(!up1.isBanker()^up2.isBanker()){
			try {
				throw new Exception("两个用户,必须一个是庄家、一个是用户");
			} catch (Exception e) {
				logger.error("",e);
				return null;
			}
		}
		return CompareType(up1,up2);
	}
	
	
	//比较两个人的牌类型的大小，并计算输赢
	private static UserPacket CompareType(UserPacket up1,UserPacket up2){
		if(up1.getType()==up2.getType()){//当手牌类型相同时
			//其他类型的话就开始比较牌点
			compareNum(up1,up2);
			return up1.isWin()?up1:up2;
		}
		//两者都是特殊牌型
//		if(up1.type>10 && up2.type>10){
//			if(up1.type==NiuNiu.SPECIALTYPE_WUHUANIU || up2.type==NiuNiu.SPECIALTYPE_WUHUANIU){
//				if(up1.type==NiuNiu.SPECIALTYPE_WUHUANIU){
//					up1.setWin(true);
//					up2.setWin(false);
//				}else{
//					up1.setWin(false);
//					up2.setWin(true);
//				}
//				return up1.isWin()?up1:up2;
//			}else if(up1.type==NiuNiu.SPECIALTYPE_ZHADANNIU || up2.type==NiuNiu.SPECIALTYPE_ZHADANNIU){
//				if(up1.type==NiuNiu.SPECIALTYPE_ZHADANNIU){
//					up1.setWin(true);
//					up2.setWin(false);
//				}else{
//					up1.setWin(false);
//					up2.setWin(true);
//				}
//				return up1.isWin()?up1:up2;
//			}
//		}
		
		if(up1.getType()>up2.getType()){
			up1.setWin(true);
			up2.setWin(false);
		}else{
			up1.setWin(false);
			up2.setWin(true);
		}
		return up1.isWin()?up1:up2;
	}
	
	//比较牌点
	private static void compareNum(UserPacket up1,UserPacket up2){
		Packer[] newP1=Packer.sort(up1.getPs());
		Packer[] newP2=Packer.sort(up2.getPs());
		//比较最大牌的大小
		int result = newP1[4].compare(newP2[4]);
		if(result==0){
			try {
				throw new Exception("服务器异常，一副牌中出现大小花色完全一样的牌");
			} catch (Exception e) {
				logger.error("",e);
			}
			return;
		}
		if(result>0){
			up1.setWin(true);
			up2.setWin(false);
		}else{
			up1.setWin(false);
			up2.setWin(true);
		}
	}
}
