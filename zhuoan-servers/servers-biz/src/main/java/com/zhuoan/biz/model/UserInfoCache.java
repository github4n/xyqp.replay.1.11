package com.zhuoan.biz.model;

import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;

public class UserInfoCache {

	public static ConcurrentHashMap<String, JSONObject> userInfoMap = new ConcurrentHashMap<String, JSONObject>();
	public static MaJiangBiz maJiangBiz = new MajiangBizImpl();
	
	/**
	 * 更新玩家元宝、金币
	 * type:3(元宝)   type:1(金币)
	 * @param @param account
	 * @param @param score
	 * @param @param type   
	 * @return void  
	 * @throws
	 * @date 2018年3月24日
	 */
	public static void updateUserScore(String account, double score, int type){
		if (userInfoMap.containsKey(account)&&userInfoMap.get(account)!=null) {
			JSONObject userInfo = userInfoMap.get(account);
			if (type==3&&userInfo.containsKey("yuanbao")) {
				double oldYB = userInfo.getDouble("yuanbao");
				BigDecimal b1 = new BigDecimal(oldYB);
				BigDecimal b2 = new BigDecimal(score);
				double newYB = b1.add(b2).doubleValue();
				// 元宝不能为负数
				if (newYB<0) {
					newYB = 0;
				}
				userInfo.put("yuanbao", newYB);
			}
		}
	}
	
	public static void updateCache(){
		JSONArray ybUpdate = maJiangBiz.getYbUpdate();
		if (!Dto.isNull(ybUpdate)) {
			for (int i = 0; i < ybUpdate.size(); i++) {
				JSONObject jsonObject = ybUpdate.getJSONObject(i);
				String account = jsonObject.getString("account");
				if (userInfoMap.containsKey(account)&&userInfoMap.get(account)!=null) {
					double yuanbao = jsonObject.getDouble("yuanbao");
					updateUserScore(account, yuanbao, 3);
					maJiangBiz.deleteYbStatus(jsonObject.getLong("id"));
				}
			}
		}
	}
}
