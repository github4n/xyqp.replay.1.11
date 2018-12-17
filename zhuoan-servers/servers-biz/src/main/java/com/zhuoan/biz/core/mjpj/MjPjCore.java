package com.zhuoan.biz.core.mjpj;

import java.util.*;

/**
 * 麻将牌九
 *
 * @author wqm
 * @Date Created in 10:29 2018/12/7
 **/
public class MjPjCore {

    /**
     * 一副完整的牌
     * 花色-牌型-牌-点数
     * 花色：字 4、筒 3、条 2、万 1
     * 牌型：天 7、地 6、人 5、鹅 4、长牌 3、短牌 2、杂牌 1
     * 牌值：白板 17、二筒 16、八筒 15、四筒 14、六条 13、四条 12、北风 11、六筒 10、七筒 9、红中 8、一筒 7、九筒 6、八条 5、七条 4、五筒 3、六万 2、三万 1
     * 点数：10、9、8、7、6、5、4、3、2、1
     */
    private static final List<String> ALL_CARD = Arrays.asList(
        "4-7-17-2", "4-7-17-2", "4-7-17-2",
        "3-6-16-2", "3-6-16-2",
        "3-5-15-8", "3-5-15-8",
        "3-4-14-4", "3-4-14-4",
        "2-3-13-6", "2-3-13-6", "2-3-12-4", "2-3-12-4", "4-3-11-10", "4-3-11-10",
        "3-2-10-6", "3-2-10-6", "3-2-9-7", "3-2-9-7", "4-2-8-10", "4-2-8-10", "3-2-7-1", "3-2-7-1",
        "3-1-6-9", "3-1-6-9", "3-1-6-9", "2-1-5-8", "2-1-5-8", "2-1-4-7", "2-1-4-7", "3-1-3-5", "3-1-3-5",
        "1-1-2-6", "1-1-1-3"
    );

    /**
     * 长牌
     */
    private static final List<Integer> LONG_CARD_LIST = Arrays.asList(13, 12, 11);

    /**
     * 短牌
     */
    private static final List<Integer> SHORT_CARD_LIST = Arrays.asList(10, 9, 8, 7);

    /**
     * 牌型-皇帝 三万 + 六万
     */
    private static final int CARD_TYPE_EMPEROR = 28;

    /**
     * 牌型-天王九 白板 + 九筒
     */
    private static final int CARD_TYPE_KING_NINE = 27;

    /**
     * 牌型-天杠 白板 + 八筒、白板 + 八条
     */
    private static final int CARD_TYPE_HEAVEN = 11;

    /**
     * 牌型-地杠 二筒 + 八筒、二筒 + 八条
     */
    private static final int CARD_TYPE_EARTH = 10;

    /**
     * 牌型-闲十
     */
    private static final int CARD_TYPE_0 = 0;

    /**
     * 白板
     */
    private static final int CARD_VALUE_17 = 17;

    /**
     * 二筒
     */
    private static final int CARD_VALUE_16 = 16;

    /**
     * 八筒
     */
    private static final int CARD_VALUE_15 = 15;

    /**
     * 九筒
     */
    private static final int CARD_VALUE_6 = 6;

    /**
     * 八条
     */
    private static final int CARD_VALUE_5 = 5;

    /**
     * 六万
     */
    private static final int CARD_VALUE_2 = 2;

    /**
     * 七万
     */
    private static final int CARD_VALUE_1 = 1;

    /**
     * 牌型张数
     */
    private static final int CARD_SIZE = 2;

    /**
     * 牌型名称
     */
    private static Map<Integer, String> cardTypeNameMap = initCardTypeName();

    /**
     * 比牌结果-胜利
     */
    public static final int COMPARE_RESULT_WIN = 1;
    /**
     * 比牌结果-失败
     */
    public static final int COMPARE_RESULT_LOSE = -1;

    /**
     * 初始化牌型名称
     *
     * @return Map
     */
    private static Map<Integer, String> initCardTypeName() {
        Map<Integer, String> cardTypeName = new HashMap<>(29);
        cardTypeName.put(0, "闲  十");
        cardTypeName.put(1, "一  点");
        cardTypeName.put(2, "二  点");
        cardTypeName.put(3, "三  点");
        cardTypeName.put(4, "四  点");
        cardTypeName.put(5, "五  点");
        cardTypeName.put(6, "六  点");
        cardTypeName.put(7, "七  点");
        cardTypeName.put(8, "八  点");
        cardTypeName.put(9, "九  点");
        cardTypeName.put(10, "地  杠");
        cardTypeName.put(11, "天  杠");
        cardTypeName.put(12, "对五筒");
        cardTypeName.put(13, "对七条");
        cardTypeName.put(14, "对八条");
        cardTypeName.put(15, "对九筒");
        cardTypeName.put(16, "对一筒");
        cardTypeName.put(17, "对红中");
        cardTypeName.put(18, "对七筒");
        cardTypeName.put(19, "对六筒");
        cardTypeName.put(20, "对北风");
        cardTypeName.put(21, "对四条");
        cardTypeName.put(22, "对六条");
        cardTypeName.put(23, "对  鹅");
        cardTypeName.put(24, "对  人");
        cardTypeName.put(25, "对  地");
        cardTypeName.put(26, "对  天");
        cardTypeName.put(27, "天王九");
        cardTypeName.put(28, "皇  帝");
        return cardTypeName;
    }

    /**
     * 打断排序
     *
     * @return String[]
     */
    public static String[] ShuffleCard() {
        // 初始牌
        List<String> initCard = new ArrayList<>(ALL_CARD);
        // 打乱排序
        Collections.shuffle(initCard);
        String[] shuffleCard = new String[initCard.size()];
        for (int i = 0; i < initCard.size(); i++) {
            shuffleCard[i] = initCard.get(i);
        }
        return shuffleCard;
    }

    /**
     * 获取牌集合
     *
     * @param cards cards
     * @return List
     */
    private static List<Integer> getValueList(String[] cards) {
        List<Integer> valueList = new ArrayList<>();
        for (String card : cards) {
            valueList.add(getCardValue(card));
        }
        return valueList;
    }

    /**
     * 获取单张牌
     *
     * @param card card
     * @return int
     */
    public static int getCardValue(String card) {
        return Integer.parseInt(card.split("-")[2]);
    }

    /**
     * 获取牌值
     *
     * @param cards cards
     * @return int
     */
    public static int[] getCardValue(String[] cards) {
        int[] cardValues = new int[cards.length];
        for (int i = 0; i < cardValues.length; i++) {
            cardValues[i] = getCardValue(cards[i]);
        }
        return cardValues;
    }

    /**
     * 获取点数集合
     *
     * @param cards cards
     * @return List
     */
    private static List<Integer> getNumList(String[] cards) {
        List<Integer> numList = new ArrayList<>();
        for (String card : cards) {
            numList.add(Integer.parseInt(card.split("-")[3]));
        }
        return numList;
    }

    /**
     * 获取牌型
     *
     * @param cards cards
     * @return int
     */
    public static int getCardType(String[] cards) {
        List<Integer> valueList = getValueList(cards);
        // 皇帝  三万 + 六万
        if (valueList.contains(CARD_VALUE_1) && valueList.contains(CARD_VALUE_2)) {
            return CARD_TYPE_EMPEROR;
        }
        // 天王九  白板 + 九筒
        if (valueList.contains(CARD_VALUE_17) && valueList.contains(CARD_VALUE_6)) {
            return CARD_TYPE_KING_NINE;
        }
        // 对子
        if (valueList.get(0).equals(valueList.get(1))) {
            return valueList.get(0) + 9;
        }
        // 天杠
        if (valueList.contains(CARD_VALUE_17)) {
            if (valueList.contains(CARD_VALUE_15) || valueList.contains(CARD_VALUE_5)) {
                return CARD_TYPE_HEAVEN;
            }
        }
        // 地杠
        if (valueList.contains(CARD_VALUE_16)) {
            if (valueList.contains(CARD_VALUE_15) || valueList.contains(CARD_VALUE_5)) {
                return CARD_TYPE_EARTH;
            }
        }
        // 计算点数
        List<Integer> numList = getNumList(cards);
        return (numList.get(0) + numList.get(1)) % 10;
    }

    /**
     * 获取牌型用于比较
     *
     * @param cards cards
     * @return int
     */
    private static int getCardTypeForCompare(String[] cards) {
        List<Integer> valueList = getValueList(cards);
        if (valueList.get(0).equals(valueList.get(1))) {
            // 三个长牌对大小相等
            if (LONG_CARD_LIST.contains(valueList.get(0))) {
                return LONG_CARD_LIST.get(0) + 9;
            }
            // 四个短牌对大小相等
            if (SHORT_CARD_LIST.contains(valueList.get(0))) {
                return SHORT_CARD_LIST.get(0) + 9;
            }
        }
        return getCardType(cards);
    }

    /**
     * 比较手牌
     *
     * @param myCard    myCard
     * @param bankCard bankCard
     * @return int
     */
    public static int compareCard(String[] myCard, String[] bankCard) {
        int myCardType = getCardTypeForCompare(myCard);
        int otherCardType = getCardTypeForCompare(bankCard);
        if (myCardType == otherCardType && myCardType < CARD_TYPE_EARTH) {
            if (myCardType == CARD_TYPE_0) {
                return -1;
            }
            List<Integer> myValueList = getValueListForCompare(myCard);
            List<Integer> otherValueList = getValueListForCompare(bankCard);
            if (myValueList.size() == CARD_SIZE && otherValueList.size() == CARD_SIZE) {
                if (myValueList.get(0).intValue() == otherValueList.get(0).intValue()) {
                    return Integer.compare(myValueList.get(1), otherValueList.get(1));
                }
                return Integer.compare(myValueList.get(0), otherValueList.get(0));
            }
        }
        return Integer.compare(myCardType, otherCardType);
    }

    /**
     * 获取牌集合
     *
     * @param cards cards
     * @return List
     */
    private static List<Integer> getValueListForCompare(String[] cards) {
        List<Integer> valueList = new ArrayList<>();
        for (String card : cards) {
            int cardValue = getCardValue(card);
            if (LONG_CARD_LIST.contains(cardValue)) {
                cardValue = LONG_CARD_LIST.get(0);
            }
            if (SHORT_CARD_LIST.contains(cardValue)) {
                cardValue = SHORT_CARD_LIST.get(0);
            }
            valueList.add(cardValue);
        }
        Collections.sort(valueList);
        return valueList;
    }

    /**
     * 获取手牌（麻将牌）
     *
     * @param cards cards
     * @return List
     */
    private static List<String> getCardNameList(String[] cards) {
        List<String> nameList = new ArrayList<>();
        for (String card : cards) {
            if ("4-7-17-2".equals(card)) {
                nameList.add("白板");
            }
            if ("3-6-16-2".equals(card)) {
                nameList.add("二筒");
            }
            if ("3-5-15-8".equals(card)) {
                nameList.add("八筒");
            }
            if ("3-4-14-4".equals(card)) {
                nameList.add("四筒");
            }
            if ("2-3-13-6".equals(card)) {
                nameList.add("六条");
            }
            if ("2-3-12-4".equals(card)) {
                nameList.add("四条");
            }
            if ("4-3-11-10".equals(card)) {
                nameList.add("北风");
            }
            if ("3-2-10-6".equals(card)) {
                nameList.add("六筒");
            }
            if ("3-2-9-7".equals(card)) {
                nameList.add("七筒");
            }
            if ("4-2-8-10".equals(card)) {
                nameList.add("红中");
            }
            if ("3-2-7-1".equals(card)) {
                nameList.add("一筒");
            }
            if ("3-1-6-9".equals(card)) {
                nameList.add("九筒");
            }
            if ("2-1-5-8".equals(card)) {
                nameList.add("八条");
            }
            if ("2-1-4-7".equals(card)) {
                nameList.add("七条");
            }
            if ("3-1-3-5".equals(card)) {
                nameList.add("五筒");
            }
            if ("1-1-2-6".equals(card)) {
                nameList.add("六万");
            }
            if ("1-1-1-3".equals(card)) {
                nameList.add("三万");
            }
        }
        return nameList;
    }

    public static void main(String[] args) {
        ArrayList<String> allCard = new ArrayList<>(ALL_CARD);
        ArrayList<String[]> allCards = new ArrayList<>();
        ArrayList<String> temp0 = new ArrayList<>();
        for (int i = 0; i < allCard.size(); i++) {
            if (!temp0.contains(allCard.get(i))) {
                ArrayList<String> temp1 = new ArrayList<>();
                for (int j = i + 1; j < allCard.size(); j++) {
                    if (!temp1.contains(allCard.get(j))) {
                        String[] cards = new String[2];
                        cards[0] = allCard.get(i);
                        cards[1] = allCard.get(j);
                        allCards.add(cards);
                        temp1.add(allCard.get(j));
                    }
                }
                temp0.add(allCard.get(i));
            }
        }
        for (int i = 0; i < allCards.size(); i++) {
            for (int j = i + 1; j < allCards.size(); j++) {
                int compareCard = compareCard(allCards.get(i), allCards.get(j));
                System.out.println("庄家手牌:"+getCardNameList(allCards.get(i)) + "(" + cardTypeNameMap.get(getCardType(allCards.get(i))) + ")   与   闲家手牌:" +
                    getCardNameList(allCards.get(j)) + "(" + cardTypeNameMap.get(getCardType((allCards.get(j)))) + ")   比牌结果为:" +
                    (compareCard == 1 ? "胜" : compareCard == 0 ? "平" : "负"));
            }
        }
    }


}
