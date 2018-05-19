package com.zhuoan.biz.model.sss;

import com.zhuoan.biz.core.sss.SSSSpecialCards;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.Playerinfo;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.SSSConstant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wqm
 * @DESCRIPTION 十三水房间实体类
 * @Date Created in 14:24 2018/4/21
 * @Modified By:
 **/
public class SSSGameRoomNew extends GameRoom{
    /**
     * 最低开始人数
     */
    private int minPlayer=2;
    /**
     * 加色
     */
    private int color;
    /**
     * 马牌
     */
    private String maPai;
    /**
     * 定庄方式(霸王庄，互比)
     */
    private int bankerType;
    /**
     * 全垒打
     */
    private int swat = 0;
    /**
     * 玩家牌局信息
     */
    private ConcurrentHashMap<String,Player> userPacketMap = new ConcurrentHashMap<String, Player>();
    /**
     * 马牌类型 0表示不存在马牌
     */
    private int maPaiType = 0;
    /**
     * 牌
     */
    private List<String> pai;
    /**
     * 比牌时间
     */
    private int compareTimer = 0;
    /**
     * 打枪
     */
    private JSONArray dqArray = new JSONArray();
    /**
     * 游戏筹码
     */
    private String baseNum;

    public JSONArray getDqArray() {
        return dqArray;
    }

    public void setDqArray(JSONArray dqArray) {
        this.dqArray = dqArray;
    }

    public int getCompareTimer() {
        return compareTimer;
    }

    public void setCompareTimer(int compareTimer) {
        this.compareTimer = compareTimer;
    }

    public int getSwat() {
        return swat;
    }

    public void setSwat(int swat) {
        this.swat = swat;
    }

    public List<String> getPai() {
        return pai;
    }

    public void setPai(List<String> pai) {
        this.pai = pai;
    }

    public int getMinPlayer() {
        return minPlayer;
    }

    public void setMinPlayer(int minPlayer) {
        this.minPlayer = minPlayer;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getMaPai() {
        return maPai;
    }

    public void setMaPai(String maPai) {
        this.maPai = maPai;
    }

    public int getBankerType() {
        return bankerType;
    }

    public void setBankerType(int bankerType) {
        this.bankerType = bankerType;
    }

    public ConcurrentHashMap<String, Player> getUserPacketMap() {
        return userPacketMap;
    }

    public void setUserPacketMap(ConcurrentHashMap<String, Player> userPacketMap) {
        this.userPacketMap = userPacketMap;
    }

    public int getMaPaiType() {
        return maPaiType;
    }

    public void setMaPaiType(int maPaiType) {
        this.maPaiType = maPaiType;
    }

    public String getBaseNum() {
        return baseNum;
    }

    public void setBaseNum(String baseNum) {
        this.baseNum = baseNum;
    }

    /**
     * 获取当前房间内的所有玩家
     * @return
     */
    public JSONArray getAllPlayer(){
        JSONArray array = new JSONArray();

        for(String uuid : getPlayerMap().keySet()){

            Playerinfo player = getPlayerMap().get(uuid);
            if(player!=null){
                JSONObject obj = new JSONObject();
                obj.put("account", player.getAccount());
                obj.put("name", player.getName());
                obj.put("headimg", player.getRealHeadimg());
                obj.put("sex", player.getSex());
                obj.put("ip", player.getIp());
                obj.put("vip", player.getVip());
                obj.put("location", player.getLocation());
                obj.put("area", player.getArea());
                if (getRoomType()!= CommonConstant.ROOM_TYPE_FK) {
                    if (player.getScore()<0) {
                        obj.put("score", 0);
                    }else {
                        obj.put("score", player.getScore());
                    }
                }else {
                    obj.put("score",player.getScore());
                }
                obj.put("index", player.getMyIndex());
                obj.put("userOnlineStatus", player.getStatus());
                obj.put("ghName", player.getGhName());
                obj.put("introduction", player.getSignature());
                obj.put("userStatus", userPacketMap.get(uuid).getStatus());
                array.add(obj);
            }
        }
        return array;
    }

    /**
     * 获取游戏数据
     * @return
     */
    public JSONObject obtainGameData(){
        JSONObject gameData = new JSONObject();
        gameData.put("showIndex",obtainShowIndex());
        gameData.put("dq",getDqArray());
        gameData.put("data",obtainData());
        return gameData;
    }

    /**
     * 获取游戏结果
     * @return
     */
    public JSONArray obtainData(){
        JSONArray data = new JSONArray();
        if (getGameStatus()>SSSConstant.SSS_GAME_STATUS_GAME_EVENT) {
            // 获取所有参与玩家得分情况
            for (String account : getUserPacketMap().keySet()) {
                if (getUserPacketMap().get(account).getStatus() != SSSConstant.SSS_USER_STATUS_INIT) {
                    JSONObject userData = new JSONObject();
                    userData.put("index",getPlayerMap().get(account).getMyIndex());
                    userData.put("paiType",getUserPacketMap().get(account).getPaiType());
                    userData.put("havema",0);
                    JSONArray userResult = new JSONArray();
                    userResult.add(getUserPacketMap().get(account).getHeadResult());
                    userResult.add(getUserPacketMap().get(account).getMidResult());
                    userResult.add(getUserPacketMap().get(account).getFootResult());
                    userData.put("result",userResult);
                    userData.put("sum",getUserPacketMap().get(account).getScore());
                    userData.put("account",account);
                    if (getGameStatus()==SSSConstant.SSS_GAME_STATUS_COMPARE&&getRoomType()!=CommonConstant.ROOM_TYPE_FK) {
                        double scoreLeft = Dto.add(getPlayerMap().get(account).getScore(),getUserPacketMap().get(account).getScore());
                        if (scoreLeft<0) {
                            scoreLeft = 0;
                        }
                        userData.put("scoreLeft",scoreLeft);
                    }else {
                        userData.put("scoreLeft",getPlayerMap().get(account).getScore());
                    }
                    userData.put("isQld",getUserPacketMap().get(account).getSwat());
                    data.add(userData);
                }
            }
        }
        return data;
    }

    /**
     * 获取动画播放顺序
     * @return
     */
    public JSONArray obtainShowIndex(){
        JSONArray showIndex = new JSONArray();
        if (getGameStatus()>SSSConstant.SSS_GAME_STATUS_GAME_EVENT&&getGameStatus()!=SSSConstant.SSS_GAME_STATUS_FINAL_SUMMARY) {
            Map<String,Double> headMap = new HashMap<String, Double>();
            Map<String,Double> midMap = new HashMap<String, Double>();
            Map<String,Double> footMap = new HashMap<String, Double>();
            // 获取所有参与玩家得分情况
            for (String account : getUserPacketMap().keySet()) {
                if (getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                    if (getUserPacketMap().get(account).getPaiType()==0) {
                        headMap.put(account,getUserPacketMap().get(account).getHeadResult().getDouble("score"));
                        midMap.put(account,getUserPacketMap().get(account).getMidResult().getDouble("score"));
                        footMap.put(account,getUserPacketMap().get(account).getFootResult().getDouble("score"));
                    }
                }
            }
            showIndex.add(sortUserScore(headMap));
            showIndex.add(sortUserScore(midMap));
            showIndex.add(sortUserScore(footMap));
        }
        return showIndex;
    }

    /**
     * 对map根据值进行排序
     * @param map
     * @return 排序之后的玩家下标
     */
    public JSONArray sortUserScore(Map<String,Double> map){
        Set<Map.Entry<String, Double>> entry = map.entrySet();
        LinkedList<Map.Entry<String, Double>> list = new LinkedList<Map.Entry<String, Double>>(entry);
        Collections.sort(list, new Comparator<Entry<String,Double>>() {
            @Override
            public int compare(Entry<String, Double> o1,
                               Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue()) ;
            }
        });
        JSONArray array = new JSONArray();
        for (Entry<String, Double> e : list) {
            array.add(getPlayerMap().get(e.getKey()).getMyIndex());
        }

        return array;
    }

    /**
     * 初始话房间信息
     */
    public void initGame(){
        setGameIndex(getGameIndex()+1);
        // 清空游戏记录
        getGameProcess().clear();
        // 清空比牌时间
        compareTimer = 0;
        // 清空打枪记录
        getDqArray().clear();
        // 全垒打清零
        swat = 0;
        // 霸王庄换庄
        if (getBankerType()==SSSConstant.SSS_BANKER_TYPE_BWZ) {
            if (!userPacketMap.containsKey(getBanker())||userPacketMap.get(getBanker())==null) {
                // 换庄
                for (String newBanker : getUserPacketMap().keySet()) {
                    setBanker(newBanker);
                }
            }
        }
        // 初始化用户信息
        for (String uuid : getUserPacketMap().keySet()) {
            if(userPacketMap.containsKey(uuid)){
                userPacketMap.get(uuid).initUserPacket();
            }
        }
    }

    /**
     * 获取当前房间内所有不是特殊牌的玩家数量
     * @return
     */
    public int obtainNotSpecialCount(){
        int notSpecialCount = 0;
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                if (getUserPacketMap().get(account).getPaiType()==0) {
                    notSpecialCount ++;
                }
            }
        }
        return notSpecialCount;
    }

    /**
     * 洗牌
     * @param perNum
     * @param mode
     * @return
     */
    public void shufflePai(int perNum, int mode) {

        // 定义一个花色数组 1黑桃，2红桃，3梅花，4方块
        String[] colors = { "1-", "2-", "3-", "4-" };
        // 定义一个点数数组
        String[] numbers = { "2","3", "4", "5", "6", "7", "8", "9", "10", "11", "12",
            "13", "1" };

        List<String> pai=new ArrayList<String>();
        // 黑桃
        List<String> oneFlower=new ArrayList<String>();
        // 红心
        List<String> twoFlower=new ArrayList<String>();
        //梅花
        List<String> threeFlower=new ArrayList<String>();
        //方块
        List<String> FourFlower=new ArrayList<String>();
        for (String number : numbers) {
            for (String color : colors) {
                String poker = color.concat(number);
                pai.add(poker);
                if("1-".equals(color)){
                    oneFlower.add(poker);
                }else if ("2-".equals(color)) {
                    twoFlower.add(poker);
                }else if ("3-".equals(color)) {
                    threeFlower.add(poker);
                }else if ("4-".equals(color)) {
                    FourFlower.add(poker);
                }
            }
        }
        if (perNum<5&&mode>0) {
            if (mode==1) {
                pai.addAll(oneFlower);
            }else if(mode==2){
                pai.addAll(oneFlower);
                pai.addAll(twoFlower);
            }
        }else if (perNum==5) {
            pai.addAll(oneFlower);
            if (mode==1) {
                pai.addAll(twoFlower);
            }else if(mode==2){
                pai.addAll(twoFlower);
                pai.addAll(threeFlower);
            }
        }else if(perNum==6){
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            if (mode==1) {
                pai.addAll(threeFlower);
            }else if(mode==2){
                pai.addAll(threeFlower);
                pai.addAll(FourFlower);
            }
        }else if(perNum==7){
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            pai.addAll(threeFlower);
        }else if(perNum==8){
            pai.addAll(oneFlower);
            pai.addAll(twoFlower);
            pai.addAll(threeFlower);
            pai.addAll(FourFlower);
        }
        // 洗牌
        Collections.shuffle(pai);
        setPai(pai);
    }

    /**
     * 发牌
     */
    public void faPai() {
        int paiIndex = 0;
        for (String account : getUserPacketMap().keySet()) {
            String[] userPai = new String[13];
            for (int i = 0; i < userPai.length; i++) {
                userPai[i] = getPai().get(paiIndex);
                paiIndex++;
            }
            // 设置玩家手牌
            getUserPacketMap().get(account).setPai(sortPaiDesc(userPai));
            // 设置玩家牌型
            getUserPacketMap().get(account).setPaiType(SSSSpecialCards.isSpecialCards(userPai,getSetting()));
        }
    }

    /**
     * 传入牌组 已经要排序的 起始位置  结束位置
     * @param list
     * @param i1
     * @param j1
     * @return
     */
    public static String[] AppointSort(String[] list,int i1,int j1) {
        String[] a=new String[j1-i1+1];
        int h=i1;
        for (int i = 0; i < a.length; i++) {
            if (i1<=j1) {
                a[i]=list[h];
                h++;
            }
        }
        JSONArray arr=new JSONArray();
        for ( int i = 0; i < a.length; i++) {
            arr.add(getValue(a[i]));
        }
        JSONArray array=new JSONArray();
        JSONArray array1=new JSONArray();
        JSONArray array2=new JSONArray();
        JSONArray array3=new JSONArray();
        JSONArray array4=new JSONArray();

        for(int i=1;i<14;i++){
            int count = 0;
            for(int t = 0; t < arr.size(); t++){
                if(arr.getInt(t)==i){
                    count++;
                }
            }
            if(count==1){array1.add(i);}
            if(count==2){array2.add(i);}
            if(count==3){array3.add(i);}
            if(count==4){array4.add(i);}
        }
        array.add(array1);
        array.add(array2);
        array.add(array3);
        array.add(array4);
        JSONArray array22=new JSONArray();
        for( int i = array.size()-1; i>=0; i--){
            JSONArray jsona=array.getJSONArray(i);
            for (int ii=0;ii<jsona.size()-1;ii++){
                for (int jj=ii+1;jj<jsona.size();jj++)
                {
                    int a1 =jsona.getInt(ii)==1?14:jsona.getInt(ii);
                    int a2 =jsona.getInt(jj)==1?14:jsona.getInt(jj);
                    // 这里是从大到小排序，如果是从小到大排序，只需将“<”换成“>”
                    if (a1>a2) {
                        int temp=jsona.getInt(ii);
                        jsona.element(ii, jsona.getInt(jj));
                        jsona.element(jj,temp);

                    };
                };
            };
            for (int j =jsona.size()-1; j >=0 ; j--) {
                for (int j2 = 0; j2 < a.length; j2++) {
                    if(jsona.getInt(j)==getValue(a[j2])){
                        array22.add(a[j2]);

                    }
                }
            }
        }
        for (int i = 0; i <array22.size(); i++) {
            if (i1<=j1) {
                list[i1]=array22.getString(i);
                i1++;
            }

        }
        return list;
    }

    /**
     * 获得牌数值
     * @param card
     * @return
     */
    public static int getValue(String card){
        int i= Integer.parseInt(card.substring(2,card.length()));
        return i;
    }

    /**
     * 获取牌数值(带花色)
     * @param card
     * @return
     */
    public int getValueWithColor(String card){
        String[] cards = card.split("-");
        int value = Integer.valueOf(cards[1]);
        if(cards[0].equals("2")){
            return value+20;
        }else if(cards[0].equals("3")){
            return value+40;
        }else if(cards[0].equals("4")){
            return value+60;
        }
        return value;
    }

    /**
     * 根据牌型设置玩家头中尾手牌
     * @param myPai
     * @param account
     */
    public void changePlayerPai(String[] myPai,String account){
        int[] headPai = new int[3];
        for (int i = 0; i < 3; i++) {
            headPai[i] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setHeadPai(headPai);
        int[] midPai = new int[5];
        for (int i = 3; i < 8; i++) {
            midPai[i-3] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setMidPai(midPai);
        int[] footPai = new int[5];
        for (int i = 8; i < 13; i++) {
            footPai[i-8] = getValueWithColor(myPai[i]);
        }
        getUserPacketMap().get(account).setFootPai(footPai);
    }

    /**
     * 获取实时准备人数
     * @return
     */
    public int getNowReadyCount(){
        int readyCount = 0;
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()==SSSConstant.SSS_USER_STATUS_READY) {
                readyCount++;
            }
        }
        return readyCount;
    }

    /**
     * 判断房间内所有玩家是否全部准备
     * @return
     */
    public boolean isAllReady(){
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_READY) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断房间内所有玩家是否全部开始
     * @return
     */
    public boolean isAllFinish(){
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_INIT) {
                if (getUserPacketMap().get(account).getStatus()!=SSSConstant.SSS_USER_STATUS_GAME_EVENT) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 小到大
     * @param list
     * @return
     */
    public static String[] sortPaiDesc(String[] list) {

        for (int i = 0; i < list.length; i++) {
            if (getValue(list[i])==1) {
                list[i]=list[i].replace("-1", "-14");
            }
        }


        for (int i = 0; i < list.length-1; i++) {

            for (int j = i+1; j < list.length; j++) {

                if (getValue(list[i])>getValue(list[j])) {
                    String o=list[i];
                    String o1=list[j];
                    list[i]=o1;
                    list[j]=o;
                }
            }
        }

        for (int i = 0; i < list.length; i++) {
            if (getValue(list[i])==14) {
                list[i]=list[i].replace("-14","-1");
            }
        }
        String[] dd=new String[13];
        int p=0;
        //倒序
        for (int i =list.length-1; i>-1 ; i--) {
            dd[p]=list[i];
            p++;
        }
        return dd;
    }

    /**
     * 获取总结算数据
     * @return
     */
    public JSONArray obtainFinalSummaryData(){
        if (getFinalSummaryData().size()>0) {
            return getFinalSummaryData();
        }
        JSONArray array = new JSONArray();
        for (String account : getUserPacketMap().keySet()) {
            if (getUserPacketMap().get(account).getStatus()>SSSConstant.SSS_USER_STATUS_INIT) {
                JSONObject obj = new JSONObject();
                obj.put("name",getPlayerMap().get(account).getName());
                obj.put("account",account);
                obj.put("headimg",getPlayerMap().get(account).getRealHeadimg());
                obj.put("score",getPlayerMap().get(account).getScore());
                obj.put("isFangzhu",CommonConstant.GLOBAL_NO);
                if (account.equals(getOwner())) {
                    obj.put("isFangzhu",CommonConstant.GLOBAL_YES);
                }
                obj.put("isWinner",CommonConstant.GLOBAL_NO);
                if (getPlayerMap().get(account).getScore()>0) {
                    obj.put("isWinner",CommonConstant.GLOBAL_YES);
                }
                obj.put("winTimes",getUserPacketMap().get(account).getWinTimes());
                obj.put("dqTimes",getUserPacketMap().get(account).getDqTimes());
                obj.put("bdqTimes",getUserPacketMap().get(account).getBdqTimes());
                obj.put("qldTimes",getUserPacketMap().get(account).getSwatTimes());
                obj.put("specialTimes",getUserPacketMap().get(account).getSpecialTimes());
                obj.put("ordinaryTimes",getUserPacketMap().get(account).getOrdinaryTimes());
                array.add(obj);
            }
        }
        setFinalSummaryData(array);
        return array;
    }

    /**
     * 是否全部同意解散
     * @return
     */
    public boolean isAgreeClose(){
        for (String account : userPacketMap.keySet()){
            if (userPacketMap.get(account).getIsCloseRoom()!= CommonConstant.CLOSE_ROOM_AGREE) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取解散数据
     * @return
     */
    public JSONArray getJieSanData(){
        JSONArray array = new JSONArray();
        for (String account : getUserPacketMap().keySet()) {
            JSONObject obj = new JSONObject();
            obj.put("index",getPlayerMap().get(account).getMyIndex());
            obj.put("name",getPlayerMap().get(account).getName());
            obj.put("result",getUserPacketMap().get(account).getIsCloseRoom());
            obj.put("jiesanTimer",getJieSanTime());
            array.add(obj);
        }
        return array;
    }

    /**
     * 获取可选玩家下注倍数
     * @param yuanbao
     * @return
     */
    public JSONArray getBaseNumTimes(double yuanbao){
        // 底注
        double di = getScore();
        // 最大下注倍数
        JSONArray baseNums = new JSONArray();
        JSONArray array = JSONArray.fromObject(getBaseNum());
        for (int i = 0; i < array.size(); i++) {
            int val = array.getJSONObject(i).getInt("val");
            JSONObject obj = new JSONObject();
            obj.put("name", new StringBuffer().append(String.valueOf(val)).append("倍").toString());
            obj.put("val", val);
            if(yuanbao>=val*SSSConstant.SSS_XZ_BASE_NUM||getRoomType()==CommonConstant.ROOM_TYPE_FK){
                obj.put("isuse", CommonConstant.GLOBAL_YES);
            }else{
                obj.put("isuse", CommonConstant.GLOBAL_NO);
            }
            baseNums.add(obj);
        }

        return JSONArray.fromObject(baseNums);
    }
}
