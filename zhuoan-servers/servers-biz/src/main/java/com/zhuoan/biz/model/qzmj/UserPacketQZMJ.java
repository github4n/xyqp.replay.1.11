package com.zhuoan.biz.model.qzmj;

import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.QZMJConstant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserPacketQZMJ {
	/**
	 * 牌
	 */
	private List<Integer> myPai = new ArrayList<Integer>();
	/**
	 * 花牌
	 */
	private List<Integer> huaList = new ArrayList<Integer>();
	/**
	 * 不可出的牌
	 */
	private List<DontMovePai> historyPai = new ArrayList<DontMovePai>();
	/**
	 * 状态
	 */
	private int status;
	/**
	 * 单局分数
	 */
	private int score;
	/**
	 * 番数
	 */
	private int fan;
	/**
	 * 是否同意解散房间
	 */
	private int isCloseRoom = 0;
    /**
     * 游金
     */
    private int youJin;
	/**
	 * 游金中 （1单游；2双游；3三游）
	 */
	private int youJinIng;
	/**
	 * 胡牌类型
	 */
	private int huType;
	/**
	 * 平胡次数
	 */
	private int pingHuTimes;
	/**
	 * 自摸次数
	 */
	private int ziMoTimes;
	/**
	 * 三金倒次数
	 */
	private int sanJinDaoTimes;
	/**
	 * 游金次数
	 */
	private int youJinTimes;
	/**
	 * 双游次数
	 */
	private int shuangYouTimes;
	/**
	 * 三金次数
	 */
	private int sanYouTimes;
	/**
	 * 天胡次数
	 */
	private int tianHuTimes;
	/**
	 * 抢杠胡次数
	 */
	private int qiangGangHuTimes;
	/**
	 * 参与游戏局数
	 */
	private int playTimes;
    /**
     * 是否托管
     */
    private int isTrustee;

    public List<Integer> getMyPai() {
        return myPai;
    }

    public void setMyPai(List<Integer> myPai) {
        this.myPai = myPai;
    }

    public List<Integer> getHuaList() {
        return huaList;
    }

    public void setHuaList(List<Integer> huaList) {
        this.huaList = huaList;
    }

    public List<DontMovePai> getHistoryPai() {
        return historyPai;
    }

    public void setHistoryPai(List<DontMovePai> historyPai) {
        this.historyPai = historyPai;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getFan() {
        return fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }

    public int getIsCloseRoom() {
        return isCloseRoom;
    }

    public void setIsCloseRoom(int isCloseRoom) {
        this.isCloseRoom = isCloseRoom;
    }

    public int getYouJin() {
        return youJin;
    }

    public void setYouJin(int youJin) {
        this.youJin = youJin;
    }

    public int getYouJinIng() {
        return youJinIng;
    }

    public void setYouJinIng(int youJinIng) {
        this.youJinIng = youJinIng;
    }

    public int getHuType() {
        return huType;
    }

    public void setHuType(int huType) {
        this.huType = huType;
    }

    public int getPingHuTimes() {
        return pingHuTimes;
    }

    public void setPingHuTimes(int pingHuTimes) {
        this.pingHuTimes = pingHuTimes;
    }

    public int getZiMoTimes() {
        return ziMoTimes;
    }

    public void setZiMoTimes(int ziMoTimes) {
        this.ziMoTimes = ziMoTimes;
    }

    public int getSanJinDaoTimes() {
        return sanJinDaoTimes;
    }

    public void setSanJinDaoTimes(int sanJinDaoTimes) {
        this.sanJinDaoTimes = sanJinDaoTimes;
    }

    public int getYouJinTimes() {
        return youJinTimes;
    }

    public void setYouJinTimes(int youJinTimes) {
        this.youJinTimes = youJinTimes;
    }

    public int getShuangYouTimes() {
        return shuangYouTimes;
    }

    public void setShuangYouTimes(int shuangYouTimes) {
        this.shuangYouTimes = shuangYouTimes;
    }

    public int getSanYouTimes() {
        return sanYouTimes;
    }

    public void setSanYouTimes(int sanYouTimes) {
        this.sanYouTimes = sanYouTimes;
    }

    public int getTianHuTimes() {
        return tianHuTimes;
    }

    public void setTianHuTimes(int tianHuTimes) {
        this.tianHuTimes = tianHuTimes;
    }

    public int getQiangGangHuTimes() {
        return qiangGangHuTimes;
    }

    public void setQiangGangHuTimes(int qiangGangHuTimes) {
        this.qiangGangHuTimes = qiangGangHuTimes;
    }

    public int getPlayTimes() {
        return playTimes;
    }

    public void setPlayTimes(int playTimes) {
        this.playTimes = playTimes;
    }

    public int getIsTrustee() {
        return isTrustee;
    }

    public void setIsTrustee(int isTrustee) {
        this.isTrustee = isTrustee;
    }

    public void initUserPacket(){
        this.playTimes ++;
        // 番数
        this.fan=0;
        // 游金
        this.youJin=0;
        this.youJinIng=0;
        this.score=0;
        //花牌
        this.huaList = new ArrayList<Integer>();
        //不可出的牌，可用来杠
        this.historyPai = new ArrayList<DontMovePai>();
        this.myPai.clear();
        this.isTrustee = 0;
	}
	
	/**
	 * 初始化手牌
	 * @param pais
	 */
	public void setMyPai(int[] pais) {
		// 清空当前数据
		if(this.myPai!=null&&this.myPai.size()>0){
			this.myPai.clear();
		}
		for (int i = 0; i < pais.length; i++) {
			this.myPai.add(pais[i]);
		}
	}

    /**
     * 获取玩家手牌中金的数量
     * @param jinPai
     * @return
     */
	public int getPlayerJinCount(int jinPai) {
        int jinCount = 0;
        for (Integer p : myPai) {
            if(p==jinPai){
                jinCount++;
            }
        }
        return jinCount;
    }

    /**
     * 移除一张手牌
     * @param oldPai
     * @return
     */
    public boolean removeMyPai(Integer oldPai) {
        return myPai.remove(oldPai);
    }

    /**
     * 摸牌
     * @param newpai
     * @return
     */
    public boolean addMyPai(int newpai) {

        return myPai.add(newpai);
    }

    /**
     * 获取桌面补杠的牌堆下标
     * @param pai
     * @return
     */
    public int buGangIndex(int pai) {

        List<DontMovePai> history=new ArrayList<DontMovePai>();
        if(this.historyPai!=null&&this.historyPai.size()>0){
            for(DontMovePai dontMovePai:this.historyPai){
                // 获取碰杠的牌
                if(dontMovePai.getType()==2 || dontMovePai.getType()==5){
                    history.add(dontMovePai);
                }
            }
        }

        for(int i=0;i<history.size();i++){

            if(history.get(i).getFoucsPai()==pai){
                return i;
            }
        }
        return 0;
    }

    /**
     * 添加桌面牌堆
     * @param type
     * @param pai
     * @param foucsPai
     */
    public void addHistoryPai(int type, int[] pai, int foucsPai) {

        this.historyPai.add(new DontMovePai(type, pai, foucsPai));
    }

    /**
     * 获取 peng的牌
     * @return
     */
    public List<DontMovePai> getPengList(){
        return dontMovePaiList(2);
    }

    /**
     * 获取 暗杆的牌
     * @return
     */
    public List<DontMovePai> getGangAnList(){
        return dontMovePaiList(3);
    }

    /**
     * 获取 明杆的牌
     * @return
     */
    public List<DontMovePai> getGangMingList(){
        return dontMovePaiList(4);
    }

    /**
     * 获取 补杆的牌
     * @return
     */
    public List<DontMovePai> getGangBuList(){
        return dontMovePaiList(5);
    }

    /**
     * 根据类别获取 历史碰对杠的 牌
     * @return
     * //1.吃，2.碰，3.暗杠，4.明杠  5.补杠
     */
    public List<DontMovePai> dontMovePaiList(int type){

        List<DontMovePai> back=new ArrayList<DontMovePai>();
        if(this.historyPai!=null&&this.historyPai.size()>0){
            for(DontMovePai dontMovePai:this.historyPai){
                if(dontMovePai.getType()==type){
                    back.add(dontMovePai);
                }
            }
        }
        return back;
    }

    /**
     * 获取吃杠碰的牌
     * @return
     */
    public List<Integer> getHistoryList(){
        List<Integer> paiList = new ArrayList<Integer>();
        for (int i = 1; i <= 5; i++) {
            List<DontMovePai> pais = dontMovePaiList(i);
            if(pais!=null){
                for (int j = 0; j < pais.size(); j++) {
                    int[] p = pais.get(j).getPai();
                    for (int k = 0; k < p.length; k++) {
                        paiList.add(p[k]);
                    }
                }
            }
        }
        return paiList;
    }

    /**
     * 获取杠的牌
     * @return
     */
    public List<Integer> getGangValue(){

        List<Integer> back=new ArrayList<Integer>();
        if(this.historyPai!=null&&this.historyPai.size()>0){
            for(DontMovePai dontMovePai:this.historyPai){
                if(dontMovePai.getType()==3||dontMovePai.getType()==4||dontMovePai.getType()==5){
                    back.add(dontMovePai.getFoucsPai());
                }
            }
        }
        return back;
    }

    /**
     * 获取番的详情
     * @param pais
     * @param room
     * @param account
     * @return
     */
    public String getFanDetail(List<Integer> pais, QZMJGameRoom room, String account) {

        if (room.getGid()== CommonConstant.GAME_ID_NAMJ) {
            return getFanDetailNA();
        }

        StringBuffer fanDetail = new StringBuffer();

        // 计算金牌番数
        int jinPai = getFanShuOnJinPai(room.getJin());
        if(jinPai>0){
            fanDetail.append("金牌 ");
            fanDetail.append(jinPai);
            fanDetail.append("番   ");
        }

        // 计算花牌番数
        int huaPai = getFanShuOnHuaPai();
        if(huaPai>0){
            fanDetail.append("花牌 ");
            fanDetail.append(huaPai);
            fanDetail.append("番   ");
        }

        // 计算桌牌番数
        int mingGang = 0;
        int anGang = 0;
        int keZi = 0;

        List<DontMovePai> pengList = getPengList();
        List<DontMovePai> anGList = getGangAnList();
        List<DontMovePai> mGList = getGangMingList();
        mGList.addAll(getGangBuList());

        // 暗杠
        for (DontMovePai dontMovePai : anGList) {
            anGang += QZMJConstant.SCORE_TYPE_GANG_AN;
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                anGang += QZMJConstant.SCORE_TYPE_ZI;
            }
        }
        if(anGang>0){
            fanDetail.append("暗杠 ");
            fanDetail.append(anGang);
            fanDetail.append("番   ");
        }

        // 明杠
        for (DontMovePai dontMovePai : mGList) {
            mingGang += QZMJConstant.SCORE_TYPE_GANG_MING;
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                mingGang += QZMJConstant.SCORE_TYPE_ZI;
            }
        }
        if(mingGang>0){
            fanDetail.append("明杠 ");
            fanDetail.append(mingGang);
            fanDetail.append("番   ");
        }

        for (DontMovePai dontMovePai : pengList) {
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                keZi += QZMJConstant.SCORE_TYPE_ZI;
            }
        }

        // 加上手牌的刻子数
        keZi += getFanShuOnHandPai(pais, room, account);

        if(keZi>0){
            fanDetail.append("刻子 ");
            fanDetail.append(keZi);
            fanDetail.append("番   ");
        }

        return fanDetail.toString();
    }

    public String getFanDetailNA() {
        StringBuffer fanDetail = new StringBuffer();
        int gang = getNanAnFanShuOnGang();
        if (gang>0) {
            fanDetail.append("杠 ");
            fanDetail.append(gang);
            fanDetail.append("番   ");
        }
        int hua = getNanAnFanShuOnHuaPai();
        if (hua>0) {
            fanDetail.append("花牌 ");
            fanDetail.append(hua);
            fanDetail.append("番   ");
        }
        return String.valueOf(fanDetail);
    }

    /**
     * 获取金牌的番数
     * @param jin
     * @return
     */
    private int getFanShuOnJinPai(int jin){
        return getPlayerJinCount(jin) * QZMJConstant.SCORE_TYPE_JIN;
    }

    /**
     * 获取花的番数
     * @return
     */
    private int getFanShuOnHuaPai(){
        if(huaList.size() >= 4){

            int flower = 0;
            int season = 0;
            for (int hua : huaList) {
                if(hua<55){
                    season++;
                }else{
                    flower++;
                }
            }
            // 4张颜色一样的花加倍
            if(flower==4&&season==4){
                return huaList.size() * QZMJConstant.SCORE_TYPE_HUA * 2;
            }else if(flower==4){
                return huaList.size() * QZMJConstant.SCORE_TYPE_HUA + 4 * QZMJConstant.SCORE_TYPE_HUA;
            }else if(season==4){
                return huaList.size() * QZMJConstant.SCORE_TYPE_HUA + 4 * QZMJConstant.SCORE_TYPE_HUA;
            }
        }
        return huaList.size() * QZMJConstant.SCORE_TYPE_HUA;
    }

    /**
     * 获取手牌的番数
     * @param myPai
     * @param room
     * @param account
     * @return
     */
    private int getFanShuOnHandPai(List<Integer> myPai, QZMJGameRoom room,String account){
        //刻子
        int ke = 0;
        //字牌
        int zi = 0;
        List<Integer> pais = new ArrayList<Integer>(myPai);
        List<Integer> paiList = new ArrayList<Integer>(myPai);
        //排序
        Collections.sort(paiList);
        List<Integer> fs = new ArrayList<Integer>();
        for (Integer i = 0; i < pais.size(); i++){
            for (Integer j = 0; j < paiList.size(); j++){
                if(pais.get(i).equals(paiList.get(j))){
                    fs.add(pais.get(i));
                }
            }
            //刻子
            if (fs.size() == 3) {
                paiList.remove(pais.get(i));
                paiList.remove(pais.get(i));
                paiList.remove(pais.get(i));
                //玩家胡的牌刚好组成刻子（平胡不能算，自摸才算）
                if(account.equals(room.getWinner())){
                    if(room.getHuType()!=QZMJConstant.HU_TYPE_PH || !pais.get(i).equals(room.getLastPai())){
                        ke += 1;
                    }
                }else{
                    ke += 1;
                }
                //字牌
                if(QZMJConstant.ZI_PAI.contains(pais.get(i))){
                    zi += 1;
                }
            }
            fs.clear();
        }
        // 番数
        int fan = ke * QZMJConstant.SCORE_TYPE_KE + zi * QZMJConstant.SCORE_TYPE_ZI;
        return fan;
    }

    /**
     * 获取桌牌的番数
     * @return
     */
    private int getFanShuOnZhuoPai(){

        //暗杠
        int angang = 0;
        //明杠
        int minggang = 0;
        //字牌
        int zi = 0;

        List<DontMovePai> pengList = getPengList();
        List<DontMovePai> anGList = getGangAnList();
        List<DontMovePai> mGList = getGangMingList();
        mGList.addAll(getGangBuList());

        for (DontMovePai dontMovePai : mGList) {
            minggang++;
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                zi++;
            }
        }
        for (DontMovePai dontMovePai : anGList) {
            angang++;
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                zi++;
            }
        }
        for (DontMovePai dontMovePai : pengList) {
            if(QZMJConstant.ZI_PAI.contains(dontMovePai.getFoucsPai())){
                zi++;
            }
        }
        return angang * QZMJConstant.SCORE_TYPE_GANG_AN
            + minggang * QZMJConstant.SCORE_TYPE_GANG_MING
            + zi * QZMJConstant.SCORE_TYPE_ZI;
    }

    /**
     * 获取总番数
     * @param pais
     * @return
     */
    public int getTotalFanShu(List<Integer> pais, QZMJGameRoom game, String account){

        return getFanShuOnHandPai(pais, game,account) + getFanShuOnZhuoPai() + getFanShuOnHuaPai() + getFanShuOnJinPai(game.getJin());
    }

    public void addHuTimes(int huType) {
        switch (huType) {
            case QZMJConstant.HU_TYPE_PH:
                this.pingHuTimes++;
                break;
            case QZMJConstant.HU_TYPE_ZM:
                this.ziMoTimes++;
                break;
            case QZMJConstant.HU_TYPE_SJD:
                this.sanJinDaoTimes++;
                break;
            case QZMJConstant.HU_TYPE_YJ:
                this.youJinTimes++;
                break;
            case QZMJConstant.HU_TYPE_SHY:
                this.shuangYouTimes++;
                break;
            case QZMJConstant.HU_TYPE_SY:
                this.sanYouTimes++;
                break;
            case QZMJConstant.HU_TYPE_TH:
                this.tianHuTimes++;
                break;
            case QZMJConstant.HU_TYPE_QGH:
                this.qiangGangHuTimes++;
                break;
            default:
                break;
        }
    }

    /**
     * 获取南安麻将总番数
     * @param @param pais
     * @param @param game
     * @param @return
     * @return int
     * @throws
     * @date 2018年3月7日
     */
    public int getNanAnTotalFanShu(){
        return getNanAnFanShuOnGang()+getNanAnFanShuOnHuaPai();
    }

    /**
     * 获取南安麻将杠积分
     * @return
     */
    private int getNanAnFanShuOnGang(){
        //暗杠
        List<DontMovePai> anGList = getGangAnList();
        //明杠
        List<DontMovePai> mGList = getGangMingList();
        //补杠
        List<DontMovePai> bGList = getGangBuList();
        return anGList.size() * QZMJConstant.SCORE_TYPE_NA_GANG_AN
            + mGList.size() * QZMJConstant.SCORE_TYPE_NA_GANG_MING
            + bGList.size() * QZMJConstant.SCORE_TYPE_NA_GANG_BU;
    }

    /**
     * 获取南安麻将花的番数
     * @return
     */
    private int getNanAnFanShuOnHuaPai(){
        if (getHuaList().size()==8) {
            return 2;
        }else if (getHuaList().size()>=4&&getHuaList().size()<8) {
            return 1;
        }else {
            return 0;
        }
    }

}
