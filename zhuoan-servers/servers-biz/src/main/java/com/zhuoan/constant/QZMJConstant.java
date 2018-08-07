package com.zhuoan.constant;

import java.util.Arrays;
import java.util.List;

/**
 * @author lhp
 *
 */
public class QZMJConstant {
	

	/**
	 * 所有牌的种类
	 */
	public static int[] NO_REPEAT_PAI = new int[]{
		11,12,13,14,15,16,17,18,19,
		21,22,23,24,25,26,27,28,29,
		31,32,33,34,35,36,37,38,39,
		41,42,43,44,45,46,47,
		51,52,53,54,55,56,57,58};
	
	/**
	 * 可胡的牌
	 */
	public static int[] ALL_CAN_HU_PAI = new int[]{
		11,12,13,14,15,16,17,18,19,
		21,22,23,24,25,26,27,28,29,
		31,32,33,34,35,36,37,38,39,
		41,42,43,44,45,46,47};

	/**
	 * 一副完整的牌
	 */
	public static int[] ALL_PAI =new int[]{
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		11,12,13,14,15,16,17,18,19,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,38,39,41,42,43,44,45,46,47,
		51,52,53,54,55,56,57,58};

    /**
     * 筒
     */
	public static List<Integer> TONG_PAI = Arrays.asList(11,12,13,14,15,16,17,18,19);
    /**
     * 万
     */
	public static List<Integer> WANG_PAI = Arrays.asList(21,22,23,24,25,26,27,28,29);
    /**
     * 条
     */
	public static List<Integer> TIAO_PAI = Arrays.asList(31,32,33,34,35,36,37,38,39);
    /**
     * 字
     */
	public static List<Integer> ZI_PAI = Arrays.asList(41,42,43,44,45,46,47);
    /**
     * 花
     */
	public static List<Integer> HUA_PAI = Arrays.asList(51,52,53,54,55,56,57,58);

    /**
     * 总牌数
     */
	public static int PAI_COUNT=144;
    /**
     * 手牌数
     */
	public static int HAND_PAI_COUNT=16;
    /**
     * 玩家数
     */
	public static int PLAYER_COUNT = 4;
    /**
     * 牌堆剩余牌数
     */
	public static int LEFT_PAI_COUNT = 16;

    /**
     * 询问类型-出牌
     */
	public static final int ASK_TYPE_CHU = 0;
    /**
     * 询问类型-抓牌
     */
	public static final int ASK_TYPE_ZP = 1;
    /**
     * 询问类型-暗杠
     */
	public static final int ASK_TYPE_GANG_AN = 2;
    /**
     * 询问类型-自摸
     */
	public static final int ASK_TYPE_HU_BY_MYSELF = 3;
    /**
     * 询问类型-吃
     */
	public static final int ASK_TYPE_CHI = 4;
    /**
     * 询问类型-碰
     */
	public static final int ASK_TYPE_PENG = 5;
    /**
     * 询问类型-明杠
     */
	public static final int ASK_TYPE_GANG_MING = 6;
    /**
     * 询问类型-糊
     */
	public static final int ASK_TYPE_HU_OTHER = 7;
    /**
     * 询问类型-询问完成
     */
	public static final int ASK_TYPE_FINISH = 20;
    /**
     * 操作类别-抓牌
     */
	public static final int THIS_TYPE_ZUA=1;
    /**
     * 操作类别-出牌
     */
	public static final int THIS_TYPE_CHU=2;
    /**
     * 准备消息类型-重连准备
     */
	public static final int GAME_READY_TYPE_RECONNECT = 1;
    /**
     * 准备消息类型-准备
     */
	public static final int GAME_READY_TYPE_READY = 2;
    /**
     * 单局取消托管次数上限
     */
    public static final int MAX_CANCEL_TIME = 2;



    /**
     * 总牌数
     */
	public static final String pai="pai";
    /**
     * 剩余的牌
     */
	public static final String zpaishu="zpaishu";
    /**
     * 玩家状态
     */
	public static final String status="status";
    /**
     * 当前焦点指针
     */
	public static final String foucsIndex="focusIndex";
    /**
     * 操作玩家位置
     */
	public static final String foucs="focus";
	public static final String lastFoucs="lastFocus";
    /**
     * 分数
     */
	public static final String soure="soure";
    /**
     * 金
     */
	public static final String jin="jin";
	public static final String type="type";
	public static final String value="value";
	public static final String zhuang="zhuang";
    /**
     * 我的牌
     */
	public static final String myPai="myPai";
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


    /**
     * 加盘类别-金
     */
	public static final int SCORE_TYPE_JIN = 1;
    /**
     * 加盘类别-金
     */
	public static final int SCORE_TYPE_HUA = 1;
    /**
     * 加盘类别-四张颜色一样的花
     */
	public static final int SCORE_TYPE_TH = 2;
    /**
     * 加盘类别-刻
     */
	public static final int SCORE_TYPE_KE = 1;
    /**
     * 加盘类别-字
     */
	public static final int SCORE_TYPE_ZI = 1;
    /**
     * 加盘类别-补杠
     */
	public static final int SCORE_TYPE_GANG_BU = 1;
    /**
     * 加盘类别-明杠
     */
	public static final int SCORE_TYPE_GANG_MING = 2;
    /**
     * 加盘类别-暗杠
     */
	public static final int SCORE_TYPE_GANG_AN = 3;
    /**
     * 加盘类别-平胡
     */
	public static final int SCORE_TYPE_PH = 1;
    /**
     * 加盘类别-自摸
     */
	public static final int SCORE_TYPE_ZM = 2;
    /**
     * 加盘类别-三金倒
     */
	public static final int SCORE_TYPE_SJD = 3;
    /**
     * 加盘类别-八张花
     */
	public static final int SCORE_TYPE_BZH = 3;
    /**
     * 加盘类别-游金三倍
     */
	public static final int SCORE_TYPE_YJ_THREE = 3;
	public static final int SCORE_TYPE_SHY_THREE = 6;
	public static final int SCORE_TYPE_SY_THREE = 12;
    /**
     * 加盘类别-游金四倍
     */
	public static final int SCORE_TYPE_YJ_FOUR = 4;
	public static final int SCORE_TYPE_SHY_FOUR = 8;
	public static final int SCORE_TYPE_SY_FOUR = 16;
    /**
     * 加盘类别-南安麻将暗杠
     */
	public static final int SCORE_TYPE_NA_GANG_AN = 2;
    /**
     * 加盘类别-南安麻将补杠
     */
	public static final int SCORE_TYPE_NA_GANG_BU = 1;
    /**
     * 加盘类别-南安麻将明杠
     */
	public static final int SCORE_TYPE_NA_GANG_MING = 1;
    /**
     * 加盘类别-南安麻将自摸
     */
	public static final int SCORE_TYPE_NA_ZM = 1;
    /**
     * 加盘类别-南安麻将游金
     */
	public static final int SCORE_TYPE_NA_YJ = 2;
    /**
     * 加盘类别-南安麻将双游
     */
	public static final int SCORE_TYPE_NA_SHY = 4;
    /**
     * 加盘类别-南安麻将三游
     */
	public static final int SCORE_TYPE_NA_SY = 8;

    /**
     * 胡牌类型-平胡
     */
	public static final int HU_TYPE_PH = 1;
    /**
     * 胡牌类型-自摸
     */
	public static final int HU_TYPE_ZM = 2;
    /**
     * 胡牌类型-三金倒
     */
    public static final int HU_TYPE_SJD = 3;
    /**
     * 胡牌类型-游金
     */
    public static final int HU_TYPE_YJ = 4;
    /**
     * 胡牌类型-双游
     */
    public static final int HU_TYPE_SHY = 5;
    /**
     * 胡牌类型-三游
     */
    public static final int HU_TYPE_SY = 6;
    /**
     * 胡牌类型-八张花
     */
    public static final int HU_TYPE_BZH = 7;
    /**
     * 胡牌类型-天胡
     */
    public static final int HU_TYPE_TH = 8;
    /**
     * 胡牌类型-抢杠胡
     */
    public static final int HU_TYPE_QGH = 9;

    /**
     * 玩家状态-初始
     */
    public static final int QZ_USER_STATUS_INIT = 0;
    /**
     * 玩家状态-准备
     */
    public static final int QZ_USER_STATUS_READY = 1;
    /**
     * 玩家状态-游戏中
     */
    public static final int QZ_USER_STATUS_GAME = 2;
    /**
     * 玩家状态-结算
     */
    public static final int QZ_USER_STATUS_SUMMARY = 3;

    /**
     * 游戏状态-初始
     */
    public static final int QZ_GAME_STATUS_INIT = 0;
    /**
     * 游戏状态-准备
     */
    public static final int QZ_GAME_STATUS_READY = 1;
    /**
     * 游戏状态-游戏中
     */
    public static final int QZ_GAME_STATUS_ING = 2;
    /**
     * 游戏状态-结算
     */
    public static final int QZ_GAME_STATUS_SUMMARY = 3;
    /**
     * 游戏状态-总结算
     */
    public static final int QZ_GAME_STATUS_FINAL_SUMMARY = 4;

    /**
     * 开局状态-ip检测
     */
    public static final int QZ_START_STATUS_CHECK_IP = 1;
    /**
     * 开局状态-摇骰子
     */
    public static final int QZ_START_STATUS_DICE = 2;
    /**
     * 开局状态-发牌
     */
    public static final int QZ_START_STATUS_FP = 3;
    /**
     * 开局状态-补花
     */
    public static final int QZ_START_STATUS_BH = 4;
    /**
     * 开局状态-开金
     */
    public static final int QZ_START_STATUS_KJ = 5;
    /**
     * 游戏事件-准备
     */
    public static final int QZMJ_GAME_EVENT_READY = 1;
    /**
     * 游戏事件-出牌
     */
    public static final int QZMJ_GAME_EVENT_CP = 2;
    /**
     * 游戏事件-胡碰杠吃
     */
    public static final int QZMJ_GAME_EVENT_IN = 3;
    /**
     * 游戏事件-出牌
     */
    public static final int QZMJ_GAME_EVENT_GANG_CP = 4;
    /**
     * 游戏事件-解散
     */
    public static final int QZMJ_GAME_EVENT_CLOSE_ROOM = 5;
    /**
     * 游戏事件-退出
     */
    public static final int QZMJ_GAME_EVENT_EXIT_ROOM = 6;
    /**
     * 游戏事件-重连
     */
    public static final int QZMJ_GAME_EVENT_RECONNECT = 7;

    public static final int QZMJ_GAME_EVENT_ROBOT_HU = 8;
    public static final int QZMJ_GAME_EVENT_ROBOT_GANG = 9;
    public static final int QZMJ_GAME_EVENT_ROBOT_PENG = 10;
    public static final int QZMJ_GAME_EVENT_ROBOT_CHI = 11;
    public static final int QZMJ_GAME_EVENT_ROBOT_GUO = 12;
    public static final int QZMJ_GAME_EVENT_ROBOT_SUMMARY = 13;
    public static final int QZMJ_GAME_EVENT_ROBOT_ZM = 14;
    /**
     * 游戏事件-重连
     */
    public static final int QZMJ_GAME_EVENT_TRUSTEE = 14;


    public static final int QZMJ_RECONNECT_TYPE_READY = 888;
    public static final int QZMJ_RECONNECT_TYPE_FINAL_SUMMARY = 666;

    /**
     * 托管类别
     */
    public static final int QZ_MJ_TIMER_TYPE_EVENT = 1;
    public static final int QZ_MJ_TIMER_TYPE_CP = 2;

    /**
     * 已出牌阈值
     */
    public static final int OUT_CARD_THRESHOLD = 2;


	/**
	 * 获取胡牌的倍数
	 * @param huType 胡的类型
	 * @param youjin 游金倍数（3倍或者4倍）
	 * @return
	 */
	public static int getHuTimes(int huType, int youjin){
		
		switch (huType) {
		case HU_TYPE_PH:
			return HU_TYPE_PH;
		case HU_TYPE_ZM:
			return SCORE_TYPE_ZM;
		case HU_TYPE_SJD:
			return SCORE_TYPE_SJD;
		case HU_TYPE_YJ:
			if(youjin==3){
				return SCORE_TYPE_YJ_THREE;
			}else{
				return SCORE_TYPE_YJ_FOUR;
			}
		case HU_TYPE_SHY:
			if(youjin==3){
				return SCORE_TYPE_SHY_THREE;
			}else{
				return SCORE_TYPE_SHY_FOUR;
			}
		case HU_TYPE_SY:
			if(youjin==3){
				return SCORE_TYPE_SY_THREE;
			}else{
				return SCORE_TYPE_SY_FOUR;
			}
		case HU_TYPE_BZH:
			return SCORE_TYPE_BZH;
		case HU_TYPE_TH:
			return SCORE_TYPE_ZM;
		case HU_TYPE_QGH:
			return SCORE_TYPE_ZM;
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
			if(HUA_PAI.contains(pai)){
				return true;
			}
		}
		return false;
	}
	
	/**
	 * 判断是否是花牌
	 * @param pai
	 * @return
	 */
	public static boolean isHuaPai(int pai){
		
		if(HUA_PAI.contains(pai)){
			
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
			if(HUA_PAI.contains(pai)){
				return true;
			}
		}
		return false;
	}
	
}
