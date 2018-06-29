package com.zhuoan.biz.core.ddz;

import com.zhuoan.constant.DdzConstant;

import java.util.*;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 11:22 2018/6/26
 * @Modified By:
 **/
public class DdzCore {

    /**
     * 一副完整的牌
     */
    private static List<String> ALL_CARD = Arrays.asList(
        "1-1","1-2","1-3","1-4","1-5","1-6","1-7","1-8","1-9","1-10","1-11","1-12","1-13",
        "2-1","2-2","2-3","2-4","2-5","2-6","2-7","2-8","2-9","2-10","2-11","2-12","2-13",
        "3-1","3-2","3-3","3-4","3-5","3-6","3-7","3-8","3-9","3-10","3-11","3-12","3-13",
        "4-1","4-2","4-3","4-4","4-5","4-6","4-7","4-8","4-9","4-10","4-11","4-12","4-13",
        "5-1","5-2"
    );

    /**
     * 洗牌发牌
     * @return [[玩家1],[玩家2],[玩家3],[地主牌]]
     */
    public static List<List<String>> shuffleAndDeal() {
        List<String> cards = ALL_CARD;
        // 打乱牌序
        Collections.shuffle(cards);
        List<List<String>> cardArray = new ArrayList<List<String>>();
        int cardIndex = 0;
        // 遍历所有玩家
        for (int i = 0; i < DdzConstant.DDZ_PLAYER_NUMBER; i++) {
            List<String> playerCard = new ArrayList<String>();
            // 玩家手牌
            for (int j = 0; j < DdzConstant.DDZ_INIT_CARD_NUMBER; j++) {
                playerCard.add(cards.get(cardIndex));
                cardIndex++;
            }
            sortCard(playerCard);
            cardArray.add(playerCard);
        }
        // 地主牌
        List<String> landlordCard = new ArrayList<String>();
        for (int i = cardIndex; i < cards.size(); i++) {
            landlordCard.add(cards.get(i));
        }
        // 添加地主牌
        cardArray.add(landlordCard);
        return cardArray;
    }

    /**
     * 手牌排序
     * @param cards
     */
    public static void sortCard(List<String> cards) {
        Collections.sort(cards, new Comparator<String>() {
            @Override
            public int compare(String card1, String card2) {
                // 点数相同按花色排序
                if (obtainCardValue(card1)==obtainCardValue(card2)) {
                    return obtainCardColor(card1)-obtainCardColor(card2);
                }
                // 按点数排序
                return obtainCardValue(card1)-obtainCardValue(card2);
            }
        });
    }

    /**
     * 判断所选的牌是否可出
     * @param oldCards
     * @param newCards
     * @return
     */
    public static boolean checkCard(List<String> oldCards,List<String> newCards) {
        // 牌型不符合条件无法出牌
        if (obtainCardType(newCards)==DdzConstant.DDZ_CARD_TYPE_ILLEGAL) {
            return false;
        }
        // 未出过牌可以直接出牌
        if (oldCards.size()==0) {
            return true;
        }
        // 上次是炸弹此次不是炸弹
        if (obtainCardType(oldCards)!=DdzConstant.DDZ_CARD_TYPE_BOMB&&obtainCardType(newCards)==DdzConstant.DDZ_CARD_TYPE_BOMB) {
            return true;
        }
        // 王炸可以出
        if (newCards.size()==2&&obtainCardType(newCards)==DdzConstant.DDZ_CARD_TYPE_BOMB) {
            return true;
        }
        // 牌序
        sortCard(oldCards);
        sortCard(newCards);
        // 牌型相同且牌数相同
        if (obtainCardType(oldCards)==obtainCardType(newCards)&&oldCards.size()==newCards.size()) {
            int cardType = obtainCardType(oldCards);
            // 单牌、对子、三带、炸弹、顺子、连对、飞机只比较最大的牌
            if (cardType==DdzConstant.DDZ_CARD_TYPE_SINGLE||cardType==DdzConstant.DDZ_CARD_TYPE_PAIRS||
                cardType==DdzConstant.DDZ_CARD_TYPE_THREE||cardType==DdzConstant.DDZ_CARD_TYPE_BOMB||
                cardType==DdzConstant.DDZ_CARD_TYPE_STRAIGHT||cardType==DdzConstant.DDZ_CARD_TYPE_DOUBLE_STRAIGHT||
                cardType==DdzConstant.DDZ_CARD_TYPE_PLANE) {
                if (obtainCardValue(newCards.get(newCards.size()-1))>obtainCardValue(oldCards.get(oldCards.size()-1))) {
                    return true;
                }
            }
            // 三带一、三带二、飞机带单牌、飞机带对子只比较最大的三带
            if (cardType==DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE||cardType==DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS||
                cardType==DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_SINGLE||cardType==DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_DOUBLE) {
                List<List<String>> oldThree = obtainRepeatCardList(oldCards,3);
                List<List<String>> newThree = obtainRepeatCardList(newCards,3);
                if (obtainCardValue(newThree.get(newThree.size()-1).get(0))>obtainCardValue(oldThree.get(oldThree.size()-1).get(0))) {
                    return true;
                }
            }
            // 四带二、四带两对只比较最大的炸弹
            if (cardType==DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE||cardType==DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS) {
                List<List<String>> oldThree = obtainRepeatCardList(oldCards,4);
                List<List<String>> newThree = obtainRepeatCardList(newCards,4);
                if (obtainCardValue(newThree.get(newThree.size()-1).get(0))>obtainCardValue(oldThree.get(oldThree.size()-1).get(0))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 获取牌型
     * @param cards
     * @return
     */
    public static int obtainCardType(List<String> cards) {
        sortCard(cards);
        Map<Integer,Integer> cardNum = obtainCardNum(cards);
        switch (cards.size()) {
            case 0:
                return DdzConstant.DDZ_CARD_TYPE_ILLEGAL;
            case 1:
                return DdzConstant.DDZ_CARD_TYPE_SINGLE;
            case 2:
                // 两张一样-对子
                if (isAllTheSame(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_PAIRS;
                }
                // 两张王-炸弹
                if (isDoubleJoker(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_BOMB;
                }
                return DdzConstant.DDZ_CARD_TYPE_ILLEGAL;
            case 3:
                // 三张一样-三不带
                if (isAllTheSame(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_THREE;
                }
                return DdzConstant.DDZ_CARD_TYPE_ILLEGAL;
            case 4:
                // 四张一样-炸弹
                if (isAllTheSame(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_BOMB;
                }
                // 一张单牌+一个三带-三带一
                if (cardNum.get(1)==1&&cardNum.get(3)==1) {
                    return DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE;
                }
                return DdzConstant.DDZ_CARD_TYPE_ILLEGAL;
            default:
                // 一个对子+一个三带-三带一对
                if (cards.size()==5&&cardNum.get(2)==1&&cardNum.get(3)==1) {
                    return DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS;
                }
                // 两个单牌/一个对子+一个炸弹-四带二
                if (cards.size()==6&&cardNum.get(4)==1) {
                    return DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE;
                }
                // 两个对子+一个炸弹-四带两对
                if (cards.size()==8&&cardNum.get(2)==2&&cardNum.get(4)==1) {
                    return DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS;
                }
                // 顺子
                if (isStraight(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_STRAIGHT;
                }
                // 双顺
                if (isDoubleStraight(cards)) {
                    return DdzConstant.DDZ_CARD_TYPE_DOUBLE_STRAIGHT;
                }
                // 飞机
                if (isPlaneWithN(cards,0)) {
                    return DdzConstant.DDZ_CARD_TYPE_PLANE;
                }
                // 飞机带单牌
                if (isPlaneWithN(cards,1)) {
                    return DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_SINGLE;
                }
                // 飞机带对子
                if (isPlaneWithN(cards,2)) {
                    return DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_DOUBLE;
                }
                return DdzConstant.DDZ_CARD_TYPE_ILLEGAL;
        }
    }

    /**
     * 获取单牌、对子、三带、炸弹数量
     * @param cards
     * @return
     */
    private static Map<Integer,Integer> obtainCardNum(List<String> cards) {
        // 每张牌更有多少张
        Map<Integer,Integer> cardNum = new HashMap<Integer,Integer>();
        for (String card : cards) {
            int cardValue = obtainCardValue(card);
            if (!cardNum.containsKey(cardValue)) {
                cardNum.put(cardValue,1);
            }else {
                cardNum.put(cardValue,cardNum.get(cardValue)+1);
            }
        }
        // 单牌、对子、三带、炸弹更有多少个
        Map<Integer,Integer> map = new HashMap<Integer,Integer>();
        map.put(1,0);
        map.put(2,0);
        map.put(3,0);
        map.put(4,0);
        for (Integer num : cardNum.values()) {
            map.put(num,map.get(num)+1);
        }
        return map;
    }

    /**
     * 是否是飞机、飞机带单牌、飞机不带
     * @param cards
     * @param n
     * @return
     */
    private static boolean isPlaneWithN(List<String> cards,int n) {
        if (cards.size()%(n+3)!=0) {
            return false;
        }
        int threeNum = cards.size()/(n+3);
        List<List<String>> three = obtainRepeatCardList(cards,3);
        if (three.size()!=threeNum) {
            return false;
        }
        // A以上不算飞机
        if (obtainCardValue(three.get(threeNum-1).get(0))>14) {
            return false;
        }
        // 三带二需要判断对子个数
        if (n==2) {
            Map<Integer,Integer> cardNum = obtainCardNum(cards);
            if (cardNum.get(2)!=threeNum) {
                return false;
            }
        }
        int min = obtainCardValue(three.get(0).get(0));
        int max = obtainCardValue(three.get(threeNum-1).get(0));
        if (max-min!=threeNum-1) {
            return false;
        }
        return true;
    }

    /**
     * 判断牌组是否是同一张牌
     * @param cards
     * @return
     */
    private static boolean isAllTheSame(List<String> cards) {
        if (cards.size()<1) {
            return false;
        }else {
            String cardTemp = cards.get(0);
            for (String card : cards) {
                if (obtainCardValue(card)!=obtainCardValue(cardTemp)) {
                    return  false;
                }
            }
            return true;
        }
    }

    /**
     * 判断是否是顺子
     * @param cards
     * @return
     */
    private static boolean isStraight(List<String> cards) {
        // 5张以上
        if (cards.size()>4) {
            // 最大的牌不超过A
            if (obtainCardValue(cards.get(cards.size()-1))<15) {
                Map<Integer, Integer> cardNum = obtainCardNum(cards);
                // 全是单牌
                if (cardNum.get(1)==cards.size()) {
                    int min = obtainCardValue(cards.get(0));
                    int max = obtainCardValue(cards.get(cards.size()-1));
                    // 最大与最小相差长度-1
                    if (max-min==cards.size()-1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断是否是连对
     * @param cards
     * @return
     */
    private static boolean isDoubleStraight(List<String> cards) {
        // 6张以上且是双数
        if (cards.size()>5&&cards.size()%2==0) {
            // 最大的牌不超过A
            if (obtainCardValue(cards.get(cards.size()-1))<15) {
                Map<Integer, Integer> cardNum = obtainCardNum(cards);
                // 全是对子
                if (cardNum.get(2)==cards.size()/2) {
                    int min = obtainCardValue(cards.get(0));
                    int max = obtainCardValue(cards.get(cards.size()-1));
                    // 最大与最小相差长度-1
                    if (max-min==cards.size()/2-1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 判断是否是双王
     * @param cards
     * @return
     */
    private static boolean isDoubleJoker(List<String> cards) {
        if (cards.size()!=2) {
            return false;
        }else {
            for (String card : cards) {
                if (obtainCardColor(card)!=DdzConstant.DDZ_CARD_COLOR_JOKER) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * 获取牌的点数
     * @param card
     * @return 点数
     */
    private static int obtainCardValue(String card) {
        int cardValue = Integer.valueOf(card.split("-")[1]);
        // A或2
        if (cardValue==DdzConstant.DDZ_CARD_NUM_ONE||cardValue==DdzConstant.DDZ_CARD_NUM_TWO) {
            cardValue += 13;
        }
        // 大小王
        if (obtainCardColor(card)==DdzConstant.DDZ_CARD_COLOR_JOKER) {
            cardValue += 2;
        }
        return cardValue;
    }

    /**
     * 获取牌的花色
     * @param card
     * @return 花色
     */
    private static int obtainCardColor(String card) {
        return Integer.valueOf(card.split("-")[0]);
    }

    /**
     * 获取重复数为num的所有手牌
     * @param cards
     * @param num
     * @return
     */
    private static List<List<String>> obtainRepeatCardList(List<String> cards,int num) {
        sortCard(cards);
        List<List<String>> list = new ArrayList<>();
        int index = 0;
        do {
            if (obtainCardValue(cards.get(index))==obtainCardValue(cards.get(index+num-1))) {
                List<String> cardArray = new ArrayList<>();
                for (int i = index; i <= index + num - 1; i++) {
                    cardArray.add(cards.get(i));
                }
                list.add(cardArray);
                index = index+num;
            }else {
                index++;
            }
        }while (index+num-1<cards.size());
        return list;
    }

    /**
     * 获取牌中所有的顺子、连对、飞机不带
     * @param cards
     * @param num 1-顺子  2-连对  3-飞机不带
     * @return
     */
    private static List<List<String>> obtainStraightList(List<String> cards,int num) {
        List<List<String>> cardList = new ArrayList<List<String>>();
        List<String> distinctCards = obtainCardsWithNum(cards,num,true);
        int minLength = 0;
        int beginIndex = 0;
        // 顺子最小长度为5 从第5位开始遍历
        if (num==1) {
            minLength = 5;
            beginIndex = 4;
        }
        // 连对最小长度为6 从第5位开始遍历
        if (num==2) {
            minLength = 6;
            beginIndex = 4;
        }
        // 飞机不带最小长度为6 从第4位开始遍历
        if (num==3) {
            minLength = 6;
            beginIndex = 3;
        }
        if (beginIndex>0&&minLength>0&&distinctCards.size()>minLength) {
            // 取顺子间隔1 取连对间隔2 取飞机不带间隔3
            for (int i = 0; i < distinctCards.size()-minLength; i=i+num) {
                for (int j = i+beginIndex; j < distinctCards.size()-num+1; j=j+num) {
                    List<String> list = distinctCards.subList(i, j+num);
                    if (obtainCardValue(distinctCards.get(j))<15&&obtainCardValue(distinctCards.get(j))-obtainCardValue(distinctCards.get(i))==(j-i)/num) {
                        cardList.add(list);
                    }else {
                        continue;
                    }
                }
            }
        }
        // 长度刚好为最小长度判断该牌是否符合条件
        if (beginIndex>0&&minLength>0&&distinctCards.size()==minLength) {
            if (num==1&&isStraight(distinctCards)) {
                cardList.add(distinctCards);
            }
            if (num==2&&isDoubleStraight(distinctCards)) {
                cardList.add(distinctCards);
            }
            if (num==3&&isPlaneWithN(distinctCards,0)) {
                cardList.add(distinctCards);
            }
        }
        return cardList;
    }

    /**
     * 获取牌中所有的单牌、对子、三带、炸弹
     * @param cards
     * @param num
     * @param isMore true包含大于的，false不包含
     * @return
     */
    public static List<List<String>> obtainRepeatList(List<String> cards,int num,boolean isMore) {
        List<List<String>> list = new ArrayList<>();
        List<String> cardList = obtainCardsWithNum(cards,num,isMore);
        for (int i = 0; i < cardList.size(); i=i+num) {
            List<String> cardArray = new ArrayList<>();
            for (int j = i; j < i+num; j++) {
                cardArray.add(cardList.get(j));
            }
            list.add(cardArray);
        }
        return list;
    }

    /**
     * 获取张数为num的所有牌
     * @param cards
     * @param num
     * @param isMore true包含大于的，false不包含
     * @return
     */
    private static List<String> obtainCardsWithNum(List<String> cards,int num,boolean isMore) {
        List<String> cardList = new ArrayList<>();
        Map<Integer,List<String>> cardMap = new HashMap<Integer,List<String>>();
        // 将牌根据点数分类
        for (String card : cards) {
            int cardValue = obtainCardValue(card);
            List<String> list = new ArrayList<>();
            list.add(card);
            // 有存在添加之前所有牌
            if (cardMap.containsKey(cardValue)) {
                list.addAll(cardMap.get(cardValue));
            }
            cardMap.put(cardValue,list);
        }
        for (Integer cardValue : cardMap.keySet()) {
            // 多出部分不取
            if (cardMap.get(cardValue).size()==num) {
                cardList.addAll(cardMap.get(cardValue).subList(0,num));
            }else if (isMore&&cardMap.get(cardValue).size()>num) {
                cardList.addAll(cardMap.get(cardValue).subList(0,num));
            }
        }
        // 排序
        sortCard(cardList);
        return cardList;
    }

    public static List<List<String>> obtainAllCard(List<String> lastCard, List<String> myPai) {
        List<List<String>> list = new ArrayList<>();
        int lastType = DdzCore.obtainCardType(lastCard);
        switch (lastType) {
            case DdzConstant.DDZ_CARD_TYPE_SINGLE:
                list.addAll(obtainRepeatList(myPai,1,false));
                break;
            case DdzConstant.DDZ_CARD_TYPE_PAIRS:
                list.addAll(obtainRepeatList(myPai,2,false));
                break;
            case DdzConstant.DDZ_CARD_TYPE_THREE:
                list.addAll(obtainRepeatList(myPai,3,false));
                break;
            case DdzConstant.DDZ_CARD_TYPE_THREE_WITH_SINGLE:
                list.addAll(joinCardList(obtainRepeatList(myPai,3,false),myPai,1,3));
                break;
            case DdzConstant.DDZ_CARD_TYPE_THREE_WITH_PARIS:
                list.addAll(joinCardList(obtainRepeatList(myPai,3,false),myPai,2,3));
                break;
            case DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_SINGLE:
                list.addAll(joinCardList(obtainRepeatList(myPai,4,false),myPai,1,4));
                break;
            case DdzConstant.DDZ_CARD_TYPE_BOMB_WITH_PARIS:
                list.addAll(joinCardList(obtainRepeatList(myPai,4,false),myPai,2,4));
                break;
            case DdzConstant.DDZ_CARD_TYPE_STRAIGHT:
                list.addAll(obtainStraightList(myPai,1));
                break;
            case DdzConstant.DDZ_CARD_TYPE_DOUBLE_STRAIGHT:
                list.addAll(obtainStraightList(myPai,2));
                break;
            case DdzConstant.DDZ_CARD_TYPE_PLANE:
                list.addAll(obtainStraightList(myPai,3));
                break;
            case DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_SINGLE:
                list.addAll(joinCardList(obtainStraightList(myPai,3),myPai,1,3));
                break;
            case DdzConstant.DDZ_CARD_TYPE_PLANE_WITH_DOUBLE:
                list.addAll(joinCardList(obtainStraightList(myPai,3),myPai,2,3));
                break;
            default:
                break;
        }
        list.addAll(obtainRepeatList(myPai,4,false));
        List<List<String>> list1 = new ArrayList<>();
        for (List<String> strings : list) {
            if (checkCard(lastCard,strings)) {
                list1.add(strings);
            }
        }
        return list1;
    }

    private static List<List<String>> joinCardList(List<List<String>> allChoice,List<String> myPai,int num,int type) {
        List<List<String>> list = new ArrayList<>();
        // 带的牌
        List<String> bandChoice = new ArrayList<>();
        // 取出所有单牌或对子
        bandChoice.addAll(obtainCardsWithNum(myPai, num, false));
        // 带单牌添加所有的对子
        if (num==1) {
            bandChoice.addAll(obtainCardsWithNum(myPai, 2, false));
        }
        for (List<String> choice : allChoice) {
            // 足够带
            if (bandChoice.size() >= (type-2) * choice.size() / type) {
                choice.addAll(bandChoice.subList(0, (type-2) * choice.size() / type));
                list.add(choice);
            }
        }
        return list;
    }


    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        List<List<String>> list = shuffleAndDeal();
        for (List<String> strings : list) {
            System.out.println(strings);
        }
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }
}
