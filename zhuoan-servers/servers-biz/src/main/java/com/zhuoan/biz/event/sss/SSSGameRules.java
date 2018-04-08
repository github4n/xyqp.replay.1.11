package com.zhuoan.biz.event.sss;

import com.zhuoan.constant.NewConstant;
import com.zhuoan.biz.core.sss.SSSComputeCards;
import com.zhuoan.biz.core.sss.SSSGameRoom;
import com.zhuoan.biz.model.Player;
import com.zhuoan.biz.model.RoomManage;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

// 游戏逻辑处理
public class SSSGameRules {

	// 定庄
	public void dingZhuang(SSSGameRoom room, int type){
		room.setGameStatus(NewConstant.GAMESTATUS_SSS_DINGZHUANG);
		switch (type) {
		case NewConstant.ZHUANGTYPE_SSS_FANGZHU:
			// 房主庄
			fangzhuZhuang(room.getRoomNo());
			break;
		case NewConstant.ZHUANGTYPE_SSS_LUNZHUANG:
			// 轮庄
			lunZhuang(room.getRoomNo());
			break;
		default:
			break;
		}
	}

	// 房主做庄
	public void fangzhuZhuang(String roomNo){
		SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		RoomManage.gameRoomMap.get(roomNo).setZhuang(room.getFangzhu());
	}
	
	// 轮庄
	public void lunZhuang(String roomNo){
		SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
		String zhuang = room.getNextPlayer(room.getZhuang());
		RoomManage.gameRoomMap.get(roomNo).setZhuang(zhuang);
	}
	
	// 洗牌
	public void xiPai(SSSGameRoom room){
		if (room.getGameStatus()<NewConstant.GAMESTATUS_SSS_XIPAI) {
			List<String> pai = room.xiPai1(room.getPlayerPaiJu().size(),room.getColor());
			room.setPai(pai);
			room.setGameStatus(NewConstant.GAMESTATUS_SSS_XIPAI);
		}
	}
	
	// 发牌
	public void faPai(SSSGameRoom room){
		//未发过牌 才能发牌
		if (room.getGameStatus()==NewConstant.GAMESTATUS_SSS_XIPAI) {
			List<String[]> myPai =room.faPai(room.getPai(), room.getPlayerPaiJu().size()); // 返回玩家手牌
			Set<String> userAcc=room.getPlayerPaiJu().keySet();
			int i=0;
			for (String acc : userAcc) { 
				String[] p=SSSGameRoom.daxiao(myPai.get(i));
				if (p!=null) {
					room.getPlayerPaiJu().get(acc).setPai(p);
				}else{
					room.getPlayerPaiJu().get(acc).setPai(myPai.get(i));
				}
				// 设置玩家已发牌状态
				room.getPlayerPaiJu().get(acc).setStatus(NewConstant.USERSTATUS_SSS_FAPAI);
				i++;
			}
			room.setGameStatus(NewConstant.GAMESTATUS_SSS_FAPAI);
		}
	}
	
	// 互比结算
	public JSONArray jieSuan_HB(SSSGameRoom room){
		List<String> list = new ArrayList<String>();
		// 获取所有参与的玩家
		for (String string : room.getPlayerPaiJu().keySet()) {
			if (room.getPlayerPaiJu().get(string).getStatus()==NewConstant.USERSTATUS_SSS_PEIPAI) {
				list.add(string);
			}
		}
		Boolean qld=false;// 是否全垒打
		String qldacc="";// 全垒打账号
		JSONArray data=new JSONArray();
		// 参与结算的人大于等于全垒打要求人数才触发
		if (list.size()>=room.getSetting().getInt("qld")) {
			for (String account : list) {
				Player player = room.getPlayerPaiJu().get(account);
				int paiType = player.getPaiType();
				int gun=0;
				for (String account1 : list) {
					if (!account1.equals(account)) {
						Player player2 = room.getPlayerPaiJu().get(account1);
						int paiType2 = player2.getPaiType();
						if(paiType==0&&paiType2==0){
							JSONObject re= SSSComputeCards.compare(player.getPai(), player2.getPai());
							JSONArray re1= JSONArray.fromObject(re.getJSONArray("result").get(0));
							JSONArray re2= JSONArray.fromObject(re.getJSONArray("result").get(1));
							int d=0;
							for (int ii = 0; ii < re1.size(); ii++) {
								if (re1.getJSONObject(ii).getInt("score")>re2.getJSONObject(ii).getInt("score")) {
									d++;
								}
								if(d==3) {
									gun++;
								}
							}
						}
					}
				}
				if (list.size()-1==gun) {
					qld=true;
					qldacc=account;
				}
			}
		}
		
		
		for (String account1 : list) {
			int gun=0;// 打枪人数
			int playerScore = 0;// 当局输赢
			int isma1 = 0;// 马牌
			Player player1 = room.getPlayerPaiJu().get(account1);
			int paiType1 = player1.getPaiType();
			int paiScore1 = player1.getPaiScore();// 牌型倍数
			List<String> upai=Arrays.asList(player1.getPai());
			// 马牌
			if (upai.contains(room.getMaPai())) {
				isma1=1;
			} 
			JSONObject u1=new JSONObject();
			u1.element("index",  room.getPlayerIndex(account1));//用户下标
			u1.element("myPai", player1.getMyPai());//牌
			u1.element("myPaiType", paiType1);//牌型
			JSONArray dp=new JSONArray();//打枪
			JSONArray dp0=new JSONArray();//被打枪
			JSONArray t=new JSONArray();// 三道牌型及输赢分数
			JSONObject t1=new JSONObject();
			t1.element("score", 0);
			t1.element("type", 0);
			t.add(t1);
			t.add(t1);
			t.add(t1);
			for (String account2 : list) {
				if (!account2.equals(account1)) {
					Player player2 = room.getPlayerPaiJu().get(account2);
					int paiScore2 = player2.getPaiScore();
					int defen=0;
					JSONObject dp1=new JSONObject();//打枪
					JSONObject dp2=new JSONObject();//被打枪
					int isma2 = 0;// 马牌
					List<String> uupai=Arrays.asList(player2.getPai());
					if (uupai.contains(room.getMaPai())) {
						isma2=1;
					}
					// 都为特殊牌倍数高的赢
					if (paiScore1>0&&paiScore2>0) {
						if (paiScore1>paiScore2) {
							playerScore += paiScore1;
						}else if (paiScore2>paiScore1) {
							playerScore -= paiScore2;
						}
					}else if (paiScore1>0&&paiScore2==0) {// 只有一家为特殊牌，特殊牌赢想应的倍数
						playerScore += paiScore1;
					}else if (paiScore1==0&&paiScore2>0) {// 只有一家为特殊牌，特殊牌赢想应的倍数
						playerScore -= paiScore2;
					}else if (paiScore1==0&&paiScore2==0) {
						JSONObject re=SSSComputeCards.compare(player1.getPai(), player2.getPai());
						JSONArray re1= JSONArray.fromObject(re.getJSONArray("result").get(0));
						JSONArray re2= JSONArray.fromObject(re.getJSONArray("result").get(1));
						int daqiang = 0;
						int beidaqiang = 0;
						for (int i = 0; i < re1.size(); i++) {
							t.getJSONObject(i).element("score", t.getJSONObject(i).getInt("score")+re1.getJSONObject(i).getInt("score"));// 增加相应的分数
							t.getJSONObject(i).element("type",  re1.getJSONObject(i).getInt("type"));// 设置对应牌型
							if (re1.getJSONObject(i).getInt("score")>re2.getJSONObject(i).getInt("score")) {
								daqiang ++;
							}else if (re1.getJSONObject(i).getInt("score")<re2.getJSONObject(i).getInt("score")) {
								beidaqiang ++;
							}
						}
						//马牌
						if (isma1==1&&isma2==1) {
							defen=re.getInt("A")*4;
						}else if (isma1==0&&isma2==1) {
							defen=re.getInt("A")*2;
							
						}else if (isma1==1&&isma2==0) {
							defen=re.getInt("A")*2;
							
						}else{
							defen=re.getInt("A");
						}
						
						//打枪
						if(daqiang==3){
							//打枪 +分s
							dp1.element("index", room.getPlayerIndex(account2));
							dp1.element("code", -1);
							defen=defen*2;
							gun++;
						}else if(beidaqiang==3){
							dp2.element("index", room.getPlayerIndex(account2));
							dp2.element("code", -1);
							//被打枪(如果被打枪 分数肯定是负数)
							defen=defen*2;
						}
						if ("HHMJ".equals(room.getSetting().getString("platform"))) {
							
							//全垒打(只对被全垒打 的人 加倍)
							if (qld&&(qldacc.equals(account1)||qldacc.equals(account2))) {
								defen=defen*2;
							}
						}else{
							
							//全垒打(所有人加倍)
							if (qld) {
								defen=defen*2;
							}
						}
						
						playerScore+=defen;
						
						dp.add(dp1);
						dp0.add(dp2);
					}
				}
			}
			if (paiType1==0) {
				player1.setOrdinary(player1.getOrdinary()+1);// 才     
			}else{
				player1.setSpecial(player1.getSpecial()+1);
			}

			player1.setGun(gun+player1.getGun());
			//是否全垒打
			if (qld) {
				u1.element("qld", 1);//全垒打 
				player1.setSwat(player1.getSwat()+1);
			}else{
				u1.element("qld", 0);//
			}
			player1.setScore(playerScore*room.getScore());//存入比分分
			if (player1.getScore()+player1.getTotalScore()<0) {
				player1.setTotalScore(0);
			}else {
				player1.setTotalScore(player1.getScore()+player1.getTotalScore());
			}

			u1.element("score",player1.getScore());//当前牌局分
			u1.element("totalscore",player1.getTotalScore());//总分
			u1.element("result", t);//牌
			u1.element("dp", dp);//打枪
			u1.element("dp0", dp0);//被打枪
			u1.element("isma", isma1);//此人手牌是否存在马牌
			u1.element("account", account1);//账号
			data.add(u1);
		}
		return data;
	}
	
	// 霸王庄结算
	public void jieSuan_BWZ(){
		
	}

	// 获取展示顺序
	public JSONArray getShowIndex(JSONArray data){
		JSONArray one= new JSONArray();
		JSONArray two= new JSONArray();
		JSONArray three= new JSONArray();

		for (int i = 0; i < data.size(); i++) {
			for (int j = i; j <data.size(); j++) {
				if (data.getJSONObject(i).getJSONArray("result").getJSONObject(0).getInt("score")>data.getJSONObject(j).getJSONArray("result").getJSONObject(0).getInt("score")) {
					JSONObject s=data.getJSONObject(i);
					data.element(i, data.getJSONObject(j));
					data.element(j,s);
				}	
			}
		}

		for (int i = 0; i <data.size(); i++) {
			one.add(data.getJSONObject(i).getInt("index"));
		}
		for (int i = 0; i < data.size(); i++) {
			for (int j = i; j <data.size(); j++) {
				if (data.getJSONObject(i).getJSONArray("result").getJSONObject(1).getInt("score")>data.getJSONObject(j).getJSONArray("result").getJSONObject(1).getInt("score")) {
					JSONObject s=data.getJSONObject(i);
					data.element(i, data.getJSONObject(j));
					data.element(j,s);
				}	
			}
		}
		for (int i = 0; i <data.size(); i++) {
			two.add(data.getJSONObject(i).getInt("index"));
		}
		for (int i = 0; i < data.size(); i++) {
			for (int j = i; j <data.size(); j++) {
				if (data.getJSONObject(i).getJSONArray("result").getJSONObject(2).getInt("score")>data.getJSONObject(j).getJSONArray("result").getJSONObject(2).getInt("score")) {
					JSONObject s=data.getJSONObject(i);
					data.element(i, data.getJSONObject(j));
					data.element(j,s);
				}	
			}
		}
		for (int i = 0; i <data.size(); i++) {
			three.add(data.getJSONObject(i).getInt("index"));
		}
		JSONArray showIndex=new JSONArray();
		showIndex.add(one);
		showIndex.add(two);
		showIndex.add(three);
		return showIndex;
	}
}
