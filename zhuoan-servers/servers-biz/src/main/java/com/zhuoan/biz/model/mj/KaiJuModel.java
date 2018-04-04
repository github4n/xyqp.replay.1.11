package com.zhuoan.biz.model.mj;

public class KaiJuModel {
	
	private int index;//玩家下标
	/**
	 * 0.出牌    1：摸牌     2：暗杠   3：自摸   4：吃     5：碰      6：明杠      7：平胡     8.结算   9.补杠（抓杠） 10.发牌  11.补花
	 */
	private int type;
	private int[] values;//相关的牌
	private int showType;//是否显示在桌面（1不显示）
	
	public KaiJuModel(){
	}
	
	/**
	 * 初始化记录
	 * @param index
	 * @param type
	 * @param values
	 */
	public KaiJuModel(int index, int type, int[] values){
		this.index=index;
		this.type=type;
		this.values=values;
		this.showType=0;
	}

	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int[] getValues() {
		return values;
	}
	public void setValues(int[] values) {
		this.values = values;
	}
	public int getShowType() {
		return showType;
	}
	public void setShowType(int showType) {
		this.showType = showType;
	}
}
