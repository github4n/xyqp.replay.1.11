package com.zhuoan.biz.core.nn;

/**
 * 牌的花色
 */
public enum Color {
	HEITAO(4),
	HONGTAO(3),
	MEIHAU(2),
	FANGKUAI(1)
	;
	
	
	private int color;

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}
	
	Color(int i){
		this.color=i;
	}
	
}
