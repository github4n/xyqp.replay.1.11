package com.zhuoan.biz.model.mj;

import java.util.Arrays;
import java.util.List;

/**
 * @author lhp
 *
 */
public class QuanZhouMJ {
	

	/**
	 * 所有牌的种类
	 */
	public static int[] PAITYPE=new int[]{
		11,12,13,14,15,16,17,18,19,
		21,22,23,24,25,26,27,28,29,
		31,32,33,34,35,36,37,38,39,
		41,42,43,44,45,46,47,
		51,52,53,54,55,56,57,58};
	
	/**
	 * 可胡的牌
	 */
	public static int[] HUPAITYPE=new int[]{
		11,12,13,14,15,16,17,18,19,
		21,22,23,24,25,26,27,28,29,
		31,32,33,34,35,36,37,38,39,
		41,42,43,44,45,46,47};

	/**
	 * 一副完整的牌
	 */
	public static int[] PAIS=new int[]{
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		51,52,53,54,55,56,57,58};
	
	//筒
	public static List<Integer> TONGPAI=Arrays.asList(11,12,13,14,15,16,17,18,19);
	//万
	public static List<Integer> WANGPAI=Arrays.asList(21,22,23,24,25,26,27,28,29);
	//条
	public static List<Integer> TIAOPAI=Arrays.asList(31,32,33,34,35,36,37,38,39);
	//字
	public static List<Integer> ZIPAI=Arrays.asList(41,42,43,44,45,46,47);
	//花
	public static List<Integer> HUAPAI=Arrays.asList(51,52,53,54,55,56,57,58);
	
	//总牌数
	public static int PAICOUNT=144;
	//手牌数
	public static int SHOUPAICOUNT=16;
	//玩家数
	public static int PLAYERCOUNT=4;
	//牌堆剩余牌数
	public static int SHENGPAI=16;
	
	
	//0.出牌    1.抓牌     2.暗杠    3.自摸     4.吃     5.碰     6.明杠    7.糊   20.询问完成    ASKTYPE    
	public static final int ASKTYPE_CHU=0;//0.出牌
	public static final int ASKTYPE_ZUA=1;//1.抓牌  
	public static final int ASKTYPE_GANG_AN=2;//2.暗杠
	public static final int ASKTYPE_HU_BYMYSELF=3;//3.自摸
	public static final int ASKTYPE_CHI=4;//4.吃 
	public static final int ASKTYPE_PENG=5;//5.碰
	public static final int ASKTYPE_GANG_MING=6;//6.明杠
	public static final int ASKTYPE_HU_OTHERSELF=7;// 7.糊 
	public static final int ASKTYPE_WANG=20;//询问完成
	//private int thisType;//本次应该操作的类别,  1.抓牌    2.出牌      THISTYPE
	public static final int THISTYPE_ZUA=1;//抓牌
	public static final int THISTYPE_CHU=2;//出牌
	
	public static final String pai="pai";//总牌数
	public static final String zpaishu="zpaishu";//剩余的牌
	public static final String status="status";//玩家状态
	public static final String foucsIndex="focusIndex";//当前焦点指针
	public static final String foucs="focus";//操作玩家位置
	public static final String lastFoucs="lastFocus";
	public static final String soure="soure";//分数
	public static final String jin="jin";//金
	public static final String type="type";//
	public static final String value="value";
	public static final String zhuang="zhuang";
	public static final String myPai="myPai";//我的牌
	public static final String myIndex="myIndex";
	public static final String zgindex="zgindex";
	public static final String lastType="lastType";
	public static final String lastValue="lastValue";
	public static final String lastzgindex="lastzgindex";
	public static final String huvalue="huvalue";
	public static final String gangvalue="gangvalue";
	public static final String pengvalue="pengvalue";
	public static final String chivalue="chivalue";
	public static final String lastChiValue="lastChiValue";
	public static final String lastPengvalue="lastPengValue";
	public static final String lastGangvalue="lastGangValue";
	public static final String nowPoint="nowPoint";
	public static final String lastPoint="lastPoint";
	

	// 加盘规则
	public static final int SCORETYPE_JIN = 1;
	public static final int SCORETYPE_HUA = 1;
	public static final int SCORETYPE_TONGHUA = 2; // 四张颜色一样的花
	public static final int SCORETYPE_KE = 1;
	public static final int SCORETYPE_ZI = 1;
	public static final int SCORETYPE_GANG_BU = 1;
	public static final int SCORETYPE_GANG_MING = 2;
	public static final int SCORETYPE_GANG_AN = 3;
	public static final int SCORETYPE_PINGHU = 1;
	public static final int SCORETYPE_ZIMO = 2;
	public static final int SCORETYPE_SANJINDAO = 3;
	public static final int SCORETYPE_BAHUA = 3;
	// 游金3倍
	public static final int SCORETYPE_YOUJIN_y3 = 3;
	public static final int SCORETYPE_SHUANGYOU_y3 = 6;
	public static final int SCORETYPE_SANYOU_y3 = 12;
	// 游金4倍
	public static final int SCORETYPE_YOUJIN_y4 = 4;
	public static final int SCORETYPE_SHUANGYOU_y4 = 8;
	public static final int SCORETYPE_SANYOU_y4 = 16;
	

	// 牌计分规则
	public static final int HUTYPE_PINGHU = 1; //平胡
	public static final int HUTYPE_ZIMO = 2; //自摸
	public static final int HUTYPE_SANJINDAO = 3; //三金倒
	public static final int HUTYPE_YOUJIN = 4; //游金
	public static final int HUTYPE_SHUANGYOU = 5; //双游
	public static final int HUTYPE_SANYOU = 6; //三游
	public static final int HUTYPE_BAHUA = 7; //八张花
	public static final int HUTYPE_TIANHU = 8; //天胡
	public static final int HUTYPE_QIANGGANGHU = 9; //抢杠胡
	
	
	/**
	 * 获取胡牌的倍数
	 * @param huType 胡的类型
	 * @param youjin 游金倍数（3倍或者4倍）
	 * @return
	 */
	public static int getHuTimes(int huType, int youjin){
		
		switch (huType) {
		case HUTYPE_PINGHU:
			return HUTYPE_PINGHU;
		case HUTYPE_ZIMO:
			return SCORETYPE_ZIMO;
		case HUTYPE_SANJINDAO:
			return SCORETYPE_SANJINDAO;
		case HUTYPE_YOUJIN:
			
			if(youjin==3){
				return SCORETYPE_YOUJIN_y3;
			}else{
				return SCORETYPE_YOUJIN_y4;
			}
		case HUTYPE_SHUANGYOU:
			
			if(youjin==3){
				return SCORETYPE_SHUANGYOU_y3;
			}else{
				return SCORETYPE_SHUANGYOU_y4;
			}
		case HUTYPE_SANYOU:
			
			if(youjin==3){
				return SCORETYPE_SANYOU_y3;
			}else{
				return SCORETYPE_SANYOU_y4;
			}
		case HUTYPE_BAHUA:
			return SCORETYPE_BAHUA;
		case HUTYPE_TIANHU:
			return SCORETYPE_ZIMO;
		case HUTYPE_QIANGGANGHU:
			return SCORETYPE_ZIMO;
		default:
			return 0;
		}
	}
	
	/**
	 * 是否存在花牌
	 * @param pais
	 * @return
	 */
	public static boolean hasHuaPai(int[] pais){
		
		for (int pai : pais) {
			if(HUAPAI.contains(pai)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断是否是花牌
	 * @param pais
	 * @return
	 */
	public static boolean isHuaPai(int pai){
		
		if(HUAPAI.contains(pai)){
			
			return true;
		}
		return false;
	}

	/**
	 * 是否存在花牌
	 * @param pais
	 * @return
	 */
	public static boolean hasHuaPai(Object[] pais) {
		
		for (Object pai : pais) {
			if(HUAPAI.contains(pai)){
				return true;
			}
		}
		return false;
	}
	
}
