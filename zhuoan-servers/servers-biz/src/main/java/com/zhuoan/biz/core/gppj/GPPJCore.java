package com.zhuoan.biz.core.gppj;

import com.zhuoan.constant.GPPJConstant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 15:42 2018/6/9
 * @Modified By:
 **/
public class GPPJCore {

    /**
     * 上点-下点-牌权重-点数
     */
    public static List<String> ALL_PAi = Arrays.asList(
        //天、地、人、鹅、梅、长三、板凳
        "6-6-21-12","1-1-20-2","4-4-19-8","1-3-18-4","5-5-17-10","3-3-16-6","2-2-15-4",
        "6-6-21-12","1-1-20-2","4-4-19-8","1-3-18-4","5-5-17-10","3-3-16-6","2-2-15-4",
        //斧头、红头十、高脚七、铜锤六
        "5-6-14-11","4-6-13-10","1-6-12-7","1-5-11-6",
        "5-6-14-11","4-6-13-10","1-6-12-7","1-5-11-6",
        //红九、白九、弯八、平八、红七、白七、红五、白五、大头六、丁三
        "4-5-10-9","3-6-9-9","3-5-8-8","2-6-7-8","3-4-6-7","2-5-5-7","1-4-4-5","2-3-3-5","2-4-2-6","1-2-1-3"
    );

    /**
     * 打乱牌顺序
     * @return
     */
    public static String[] ShufflePai() {
        // 初始牌
        List<String> initPai = ALL_PAi;
        // 打乱排序
        Collections.shuffle(initPai);
        String[] shufflePai = new String[initPai.size()];
        for (int i = 0; i < initPai.size(); i++) {
            shufflePai[i] = initPai.get(i);
        }
        return shufflePai;
    }

    /**
     * 获取牌数值
     * @param pai
     * @return
     */
    public static int getPaiValue(String pai) {
        String[] values = pai.split("-");
        return Integer.valueOf(values[2]);
    }

    /**
     * 获取牌值
     * @param pai
     * @return
     */
    public static int[] getPaiValue(String[] pai) {
        int[] newPai = new int[pai.length];
        for (int i = 0; i < newPai.length; i++) {
            newPai[i] = getPaiValue(pai[i]);
        }
        return newPai;
    }

    /**
     * 获取牌点数
     * @param pai
     * @return
     */
    public static int getPaiNum(String pai) {
        String[] values = pai.split("-");
        return Integer.valueOf(values[3]);
    }

    /**
     * 获取牌型
     * @param myPai
     * @return
     */
    public static int getPaiType(String[] myPai) {
        if (myPai.length == 2) {
            String pai1 = myPai[0];
            String pai2 = myPai[1];
            int value1 = getPaiValue(pai1);
            int value2 = getPaiValue(pai2);
            int num1 = getPaiNum(pai1);
            int num2 = getPaiNum(pai2);
            if(value1+value2==3){
                // 至尊
                return 38;
            }else if(value1+value2 == 42){
                // 双天
                return 37;
            }else if(value1 == 20 && value2 == 20){
                // 双地
                return 36;
            }else if(value1 == 19 && value2 == 19){
                // 双人
                return 35;
            }else if(value1 == 18 && value2 == 18){
                // 双鹅
                return 34;
            }else if(value1 == 17 && value2 == 17){
                // 双梅
                return 33;
            }else if(value1 == 16 && value2 == 16){
                // 双长三
                return 32;
            }else if(value1 == 15 && value2 == 15){
                // 双板凳
                return 31;
            }else if(value1 == 14 && value2 == 14){
                // 双斧头
                return 30;
            }else if(value1 == 13 && value2 == 13){
                // 双红头
                return 29;
            }else if(value1 == 12 && value2 == 12){
                // 双高脚
                return 28;
            }else if(value1 == 11 && value2 == 11){
                // 双铜锤
                return 27;
            }else if(value1 == 10 && value2 == 9){
                // 杂九
                return 26;
            }else if(value1 == 8 && value2 == 7){
                // 杂八
                return 25;
            }else if(value1 == 6 && value2 == 5){
                // 杂七
                return 24;
            }else if(value1 == 4 && value2 == 3){
                // 杂五
                return 23;
            }else if(value1 == 21 && value2 == 10){
                // 天王
                return 22;
            }else if(value1 == 21 && value2 == 9){
                // 天王
                return 21;
            }else if(value1 == 20 && value2 == 10){
                // 地王
                return 20;
            }else if(value1 == 20 && value2 == 9){
                // 地王
                return 19;
            }else if(value1 == 21 && value2 == 19){
                // 天杠
                return 18;
            }else if(value1 == 21 && value2 == 8){
                // 天杠
                return 17;
            }else if(value1 == 21 && value2 == 7){
                // 天杠
                return 16;
            }else if(value1 == 20 && value2 == 19){
                // 地杠
                return 15;
            }else if(value1 == 20 && value2 == 8){
                // 地杠
                return 14;
            }else if(value1 == 20 && value2 == 7){
                // 地杠
                return 13;
            }else if(value1 == 21 && value2 == 5){
                // 天高九
                return 12;
            }else if(value1 == 20 && value2 == 12){
                // 天高九
                return 11;
            }else{
                //算点数
                return (num1 + num2) % 10;
            }
        }
        return -1;
    }

    /**
     * 比较两副牌的大小
     * @param myPai
     * @param otherPai
     * @return 1:myPai较大  0:相等  1:otherPai较大
     */
    public static int comparePai(String[] myPai, String[] otherPai) {
        int myPaiType = GPPJCore.getPaiType(myPai);
        int otherPaiType = GPPJCore.getPaiType(otherPai);
        if (myPaiType==17&&otherPaiType==16) {
            // 同为天杠
            return GPPJConstant.COMPARE_RESULT_EQUALS;
        }else if (myPaiType==16&&otherPaiType==17) {
            // 同为天杠
            return GPPJConstant.COMPARE_RESULT_EQUALS;
        }else if (myPaiType==14&&otherPaiType==13) {
            // 同为地杠
            return GPPJConstant.COMPARE_RESULT_EQUALS;
        }else if (myPaiType==13&&otherPaiType==14) {
            // 同为地杠
            return GPPJConstant.COMPARE_RESULT_EQUALS;
        }else if (myPaiType>otherPaiType) {
            // 牌型较大返回胜利
            return GPPJConstant.COMPARE_RESULT_WIN;
        }else if (myPaiType<otherPaiType) {
            // 牌型较小返回失败
            return GPPJConstant.COMPARE_RESULT_LOSE;
        }else if (myPaiType==otherPaiType&&myPaiType<11) {
            // 非特殊牌型比较最大的一张牌
            if (getMaxValue(myPai)>getMaxValue(otherPai)) {
                return GPPJConstant.COMPARE_RESULT_WIN;
            }else if (getMaxValue(myPai)<getMaxValue(otherPai)) {
                return GPPJConstant.COMPARE_RESULT_LOSE;
            }else {
                return GPPJConstant.COMPARE_RESULT_EQUALS;
            }
        }else {
            // 特殊牌型返回相等
            return GPPJConstant.COMPARE_RESULT_EQUALS;
        }
    }

    /**
     * 获取一副牌中的最大权值
     * @param myPai
     * @return
     */
    public static int getMaxValue(String[] myPai) {
        int maxValue = 0;
        for (int i = 0; i < myPai.length; i++) {
            if (getPaiValue(myPai[i])>maxValue) {
                maxValue = getPaiValue(myPai[i]);
            }
        }
        return maxValue;
    }
}
