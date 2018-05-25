package com.zhuoan.biz.model.qzmj;

import java.util.ArrayList;
import java.util.List;

public class DontMovePai {

    /**
     * 1.吃，2.碰，3.暗杠，4.明杠  5.抓明杠
     */
    private int type;
    /**
     * 底牌
     */
    private List<Integer> pai;
    /**
     * 操作的牌
     */
	private int foucsPai;
	
	public DontMovePai(){
		
	}
	
	public DontMovePai(int type,int[] pai,int foucsPai){
		this.type=type;
		this.pai=new ArrayList<Integer>();
		for(int i=0;i<pai.length;i++){
			this.pai.add(pai[i]);
		}
		this.foucsPai=foucsPai;
	}
	
	/**
	 * 修改,碰--》杠
	 * @return
	 */
	public void updateDontMovePai(int type,int pai,int foucsPai){
		this.type=type;
		this.pai.add(pai);
		this.foucsPai=foucsPai;
	}
	
	
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int[] getPai() {
		int[] newpai=new int[this.pai.size()];
		for(int i=0;i<this.pai.size();i++){
			newpai[i]=this.pai.get(i);
		}
		return newpai;
	}
	public void setPai(int[] pai) {
		this.pai=new ArrayList<Integer>();
		for(int i=0;i<pai.length;i++){
			this.pai.add(pai[i]);
		}
	}
	public int getFoucsPai() {
		return foucsPai;
	}
	public void setFoucsPai(int foucsPai) {
		this.foucsPai = foucsPai;
	}
	
}
