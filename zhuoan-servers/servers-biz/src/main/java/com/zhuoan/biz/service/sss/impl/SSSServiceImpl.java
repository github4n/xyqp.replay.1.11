package com.zhuoan.biz.service.sss.impl;

import com.corundumstudio.socketio.SocketIOClient;
import com.za.game.remote.iservice.IService;
import com.zhuoan.biz.core.sss.*;
import com.zhuoan.biz.model.*;
import com.zhuoan.biz.model.sss.Player;
import com.zhuoan.biz.service.majiang.MaJiangBiz;
import com.zhuoan.biz.service.majiang.impl.MajiangBizImpl;
import com.zhuoan.biz.service.sss.SSSService;
import com.zhuoan.constant.Constant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.queue.SqlModel;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

// 业务逻辑处理
@Service
public class SSSServiceImpl implements SSSService{
    MaJiangBiz mjBiz=new MajiangBizImpl();

    @Override
    public SSSGameRoom createGameRoom(JSONObject roomObj, UUID uuid, JSONObject objInfo, Playerinfo player,SSSGameRoom room) {

        //SSSGameRoom room=new SSSGameRoom();
        room.setFirstTime(1);
        //room.setRoomNo(roomObj.getString("room_no"));//房间号，唯一值
        room.setPlayerCount(objInfo.getInt("player"));//玩家人数
		/*if (roomObj.getInt("roomtype")==3) {
			room.setGameCount(999);
		}else{
			room.setGameCount(objInfo.getJSONObject("turn").getInt("turn"));
		}*/
        if (room.getRoomType()==3) {
            room.setGameCount(999);
        }else{
            room.setGameCount(objInfo.getJSONObject("turn").getInt("turn"));
        }
        //room.setScore(100); //底分
        if (room.getRoomType()==2) {

            room.setRoomType(0);
        }

        //是否加入机器人
        if (objInfo.containsKey("robot")&&objInfo.getInt("robot")==1) {
            room.setRobot(true);
            List<String> list = mjBiz.getRobotList(room.getPlayerCount()-1);
            room.setRobotList(list);
        }else {
            room.setRobot(false);
        }
        //room.setFangzhu(player.getAccount());
        //room.setZhuang(player.getAccount());
        ConcurrentSkipListSet<UUID> uuidList=new ConcurrentSkipListSet<UUID>();
        uuidList.add(uuid);//房主加入房间
        room.setUuidList(uuidList);//用户的socketId
        Map<String,Playerinfo> users=new ConcurrentHashMap<String, Playerinfo>();
        users.put(player.getAccount(), player);
        room.setPlayerMap(users);
        if (objInfo.containsKey("color")) {
            room.setColor(objInfo.getInt("color"));//加色
        }else{
            room.setColor(0);//加色
        }
        Map<String, Player> user = new ConcurrentHashMap<String, Player>();
        Player pl=new Player();
        pl.setIsReady(0);
        pl.setStatus(0);
		/*if (roomObj.getInt("roomtype")==1) {
			pl.setTotalScore(player.getScore());
			pl.setGameNum(0);
			room.setScore(objInfo.getInt("goldcoins")); //底分 金币场
			room.setMinscore(objInfo.getInt("mingoldcoins"));
			room.setLevel(roomObj.getInt("level"));
		}else if(roomObj.getInt("roomtype")==3){
			System.out.println("元宝底分"+objInfo.getInt("yuanbao"));
			pl.setTotalScore(player.getScore());
			room.setScore(objInfo.getInt("yuanbao")); //底分 元宝场
			room.setMinscore(objInfo.getInt("leaveYB"));//离场分数
		}else{
			room.setScore(1); //底分 房卡场
			room.setMinscore(1);
		}*/
        if (room.getRoomType()==1) {
            pl.setTotalScore(player.getScore());
            pl.setGameNum(0);
            room.setScore(objInfo.getInt("goldcoins")); //底分 金币场
            room.setMinscore(objInfo.getInt("mingoldcoins"));
            room.setLevel(objInfo.getInt("level"));
        }else if(room.getRoomType()==3){
            System.out.println("元宝底分"+objInfo.getInt("yuanbao"));
            pl.setTotalScore(player.getScore());
            room.setScore(objInfo.getInt("yuanbao")); //底分 元宝场
            room.setMinscore(objInfo.getInt("leaveYB"));//离场分数
        }else{
            room.setScore(1); //底分 房卡场
            room.setMinscore(1);
        }
        user.put(player.getAccount(), pl);
        room.setPlayerPaiJu(user);
        ConcurrentSkipListSet<Long> userSet = new ConcurrentSkipListSet<Long>();
        userSet.add(player.getId());
        room.setUserSet(userSet);
        ConcurrentSkipListSet<String> acc=new ConcurrentSkipListSet<String>();
        acc.add(player.getAccount());
        room.setUserAcc(acc);
        room.setGameType(objInfo.getInt("type"));//游戏模式
        if (objInfo.containsKey("jiama")) {
            room.setMaPaiType(objInfo.getInt("jiama"));//马牌模式
        }else{
            room.setMaPaiType(0);//马牌模式
        }

        //在这里加入马牌 1黑桃，2红桃，3梅花，4方块
        String c="0-0";
        if (roomObj.getInt("roomtype")!=1) {
            if (room.getMaPaiType()==1) {
                //int p=objInfo.getInt("player")*13;
                int randomNumber = (int)Math.round(Math.random()*12+1);
						/*int randomColor = (int) Math.round(4);
						c=randomColor+"-"+randomNumber;
						if (randomNumber<20) {
							c="1-"+randomNumber;
						}else if(randomNumber>20&&randomNumber<40){
							c="2-"+(randomNumber-20);
						}else if(randomNumber>40&&randomNumber<60){
							c="3-"+(randomNumber-40);
						}else if(randomNumber>60){
							c="4-"+(randomNumber-60);
						}*/
                c="1-"+randomNumber;
            }else if (room.getMaPaiType()==2){
                c="1-1";
                //room.setMaPai(c);
            }else if (room.getMaPaiType()==3){
                c="1-5";
            }else if (room.getMaPaiType()==4){
                c="1-10";
            }
        }
        room.setMaPai(c);

        //将房间存入缓存
        RoomManage.gameRoomMap.put(room.getRoomNo(), room);
        Constant.sssGameMap.put(roomObj.getString("room_no"), room);
        return room;
    }

    @Override
    public boolean joinGameRoom(String roomNo, UUID uuid, Playerinfo player,int roomType) {

        if(RoomManage.gameRoomMap.containsKey(roomNo)){
            SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
            if(room!=null){
                System.err.println(1);
                if(room.getUserSet().size()<=room.getMaxplayer()){
                    System.err.println(2);
                    if(!room.getUserSet().contains(player.getId())){
                        //新加进来的玩家
                        System.err.println(3);
                        room.getUuidList().add(uuid);
                        room.getPlayerMap().put(player.getAccount(), player);//用户的个人信息
                        Player pl=new Player();
                        pl.setIsReady(0);
                        pl.setGameNum(0);
                        if (roomType==1||roomType==3) {
                            pl.setTotalScore(player.getScore());
                        }
                        room.getPlayerPaiJu().put(player.getAccount(), pl);
                        room.getUserSet().add(player.getId());
                        room.getUserAcc().add(player.getAccount());
                        RoomManage.gameRoomMap.put(roomNo, room);
                        return true;
                    }else if(room.getGameStatus()>0){ // TODO 断线后进来的玩家
                        return false;
                    }
                }

            }
        }
        return false;
    }


    /**
     * 准备就绪
     * @param roomNo
     * @param sessionId
     */
    @Override
    public void isReady(String roomNo, String uc) {

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        if (room.getPlayerPaiJu().containsKey(uc)&&room.getPlayerPaiJu().get(uc)!=null) {

            room.getPlayerPaiJu().get(uc).setIsReady(1);
            room.getPlayerPaiJu().get(uc).setIsAuto(0);
            //room.getPlayerPaiJu().get(uc).setStatus(0);

            int count = 0;
            for (String uid:room.getPlayerPaiJu().keySet()) {
                int ready = room.getPlayerPaiJu().get(uid).getIsReady();
                System.err.println("用户："+uid+",准备状态："+ready);
                if(ready!=0){
                    count++;
                }
                if (room.getGameType()==3&&room.getPlayerPaiJu().get(uid).getTotalScore()<room.getMinscore()) {
                    //元宝不足不能准备
                    count--;
                    room.getPlayerPaiJu().get(uc).setIsReady(0);
                }
            }

            room.setReadyCount(count);
        }
    }


    /**
     * 定庄
     * @param roomNo 房间号
     * @param type 定庄类型（房主、轮庄）
     */
    @Override
    public void dingZhuang(String roomNo, int type) {

        RoomManage.gameRoomMap.get(roomNo).setGameStatus(1);
        switch (type) {
            case 0:
                // 房主庄
                fangzhuZhuang(roomNo);
                break;
            case 1:
                // 轮庄
                lunZhuang(roomNo);
                break;
            default:
                break;
        }
    }

    /**
     * 房主做庄
     * @param roomNo
     */
    public void fangzhuZhuang(String roomNo){

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        RoomManage.gameRoomMap.get(roomNo).setZhuang(room.getFangzhu());

    }

    /**
     * 轮流做庄
     * @param roomNo
     */
    public void lunZhuang(String roomNo){

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        String zhuang = room.getNextPlayer(room.getZhuang());
        RoomManage.gameRoomMap.get(roomNo).setZhuang(zhuang);

    }


    /**
     * 洗牌
     * @param roomNo
     */
    @Override
    public void xiPai(String roomNo) {

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        //List<String> pai = room.xiPai();
        //未洗过牌
        if (room.getGameStatus()<2) {
            List<String> pai = room.xiPai1(room.getUserAcc().size(),room.getColor());
            room.setPai(pai);
            RoomManage.gameRoomMap.get(roomNo).setGameStatus(2);
        }
    }
    /**
     * 胜率控制器
     * @param Paizu
     */
    public int govern(List<String[]> Paizu,JSONObject obj) {
        Map<Integer, Integer> fen= new ConcurrentHashMap<Integer, Integer>();
        ArrayList<Integer> bi=new ArrayList<Integer>();
        for (int i = 0; i <Paizu.size(); i++) {
            int scA=0;
            int sc=SSSSpecialCards.isSpecialCards(Paizu.get(i), obj);
            String[] a=SSSOrdinaryCards.sort(Paizu.get(i));
            for (int j = 0; j < Paizu.size(); j++) {
                if (Paizu.get(j)!=Paizu.get(i)) {
                    int sc1=SSSSpecialCards.isSpecialCards(Paizu.get(j), obj);
                    String[] b=SSSOrdinaryCards.sort(Paizu.get(j));
                    if (sc>0&&sc1>0) {
                        if ((sc==14&&sc1<14)||(sc==13&&sc1<13)) {
                            scA=scA+sc;
                        }else if((sc<14&&sc1==14)||(sc<13&&sc1==13)){
                            scA=scA-sc1;
                        }else{
                            scA=scA+sc-sc1;
                        }
                    }else if(sc>0&&sc1==0){
                        scA=scA+sc;
                    }else if(sc==0&&sc1>0){
                        scA=scA-sc1;
                    }else if(sc==0&&sc1==0){

                        JSONObject re=SSSComputeCards.compare(a, b);
                        scA=scA+re.getInt("A");//A对比得分
                        JSONArray re1=JSONArray.fromObject(re.getJSONArray("result").get(0));
                        JSONArray re2=JSONArray.fromObject(re.getJSONArray("result").get(1));
                        int d=0;
                        int p=0;
                        for (int ii = 0; ii < re1.size(); ii++) {
                            if (re1.getJSONObject(ii).getInt("score")>re2.getJSONObject(ii).getInt("score")) {
                                d++;
                            }else if(re1.getJSONObject(ii).getInt("score")<re2.getJSONObject(ii).getInt("score")){
                                p++;
                            }
                        }
                        if(d==3){
                            //打枪 +分
                            scA+=re.getInt("A");//
                        }
                        if(p==3){
                            //被打枪(如果被打枪 分数肯定是负数)
                            scA+=re.getInt("A");
                        }
                    }
                }
            }
            fen.put(i, scA);
            bi.add(scA);
        }

        //排序
        Collections.sort(bi);
        int p = 0;
        for (int k = 0; k <fen.size()-1; k++) {
            System.out.println("牌组:"+Arrays.toString(Paizu.get(k))+"分数："+fen.get(k));
            if (fen.get(k)==bi.get(bi.size()-1)) {
                p=k;
            }
        }
        System.out.println("牌组分:"+bi);



        return p;

    }

    /**
     * 发牌
     * @param roomNo
     */
    @Override
    public void faPai(String roomNo) {

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        //未发过牌 才能发牌
        if (room.getGameStatus()==2) {
            List<String[]> myPai =room.faPai(room.getPai(), room.getUserAcc().size()); // 返回玩家手牌
            //ConcurrentSkipListSet<UUID> uuidList = room.getUuidList();
            Set<String> userAcc=room.getUserAcc();
            //清空旧牌
			/*for (String acc : userAcc) {
				room.getPlayerPaiJu().get(acc).setPai(null);
			}*/
			/*for (int i=0; i<userAcc.size(); i++) {
				room.getPlayerPaiJu().get(userAcc.).setPai(null);
			}*/
            //提前算分
			/*int pp= govern(myPai,room.getSetting());
			JSONArray fapai=new JSONArray();
			for (int i = 0; i < myPai.size(); i++) {
				if (i!=pp) {
					fapai.add(i);
				}
			}
			int fapai1=0;
			//这局有资格获取最大牌的UUID list
			JSONArray ul=new JSONArray();
			 for (int i=0; i<uuidList.size(); i++) {
				 Playerinfo pinfo=room.getPlayerMap().get(uuidList.get(i));
				 Random random = new Random();
				 int zb = random.nextInt(100);
				 if (zb<=pinfo.getLuck()) {
					 ul.add(uuidList.get(i));
				 }
			}
			 System.out.println("paizu:"+myPai.toString()+"max:"+pp);*/
            int i=0;
            for (String acc : userAcc) {
                // 遍历玩家列表

                String des="";
                //Playerinfo pinfo=room.getPlayerMap().get(uuidList.get(i));
                //资格集合判断
				/* if (ul.size()>0) {
					 //当前UUID 是否存在集合内
					if (ul.contains(uuidList.get(i))&&pp!=-1) {
							//塞入 最大牌  （先进房间有 优势）

							String[] p=SSSGameRoom.daxiao(myPai.get(pp));
							room.getPlayerPaiJu().get(uuidList.get(i)).setPai(p);

							des="【发牌阶段】胜用户："+room.getPlayerMap().get(uuidList.get(i)).getName()+"，用户手牌："+Arrays.toString(myPai.get(pp));
							pp=-1;

					}else{
						String[] p=SSSGameRoom.daxiao(myPai.get(fapai.getInt(fapai1)));
						if (p!=null) {
							room.getPlayerPaiJu().get(uuidList.get(i)).setPai(p);
						}else{
							room.getPlayerPaiJu().get(uuidList.get(i)).setPai(myPai.get(fapai.getInt(fapai1)));
						}
						 des="【发牌阶段】普用户："+room.getPlayerMap().get(uuidList.get(i)).getName()+"，用户手牌："+Arrays.toString(myPai.get(fapai.getInt(fapai1)));
						 fapai1++;
					}


				}else{if (room.getPlayerPaiJu().get(uuidList.get(i)).getPai()==null) {}}*/
                //无资格随机发牌
                String[] p=SSSGameRoom.daxiao(myPai.get(i));
                if (p!=null) {
                    room.getPlayerPaiJu().get(acc).setPai(p);
                }else{
                    room.getPlayerPaiJu().get(acc).setPai(myPai.get(i));
                }
                des="【发牌阶段】随用户："+room.getPlayerMap().get(acc).getName()+"，用户手牌："+Arrays.toString(myPai.get(i));





                // 设置玩家已发牌状态
                room.getPlayerPaiJu().get(acc).setStatus(1);

				/*JSONObject obj=new JSONObject();
				obj.element("des", des);
				obj.element("userid", room.getPlayerMap().get(acc).getId());
				obj.element("roomType", room.getRoomType());
				obj.element("roomNo", roomNo);
				obj.element("GameIndex", room.getGameIndex()+1);
				LogUtil.addZaGameRecord(obj);*/
                i++;

            }
            room.setGameStatus(3);
        }

    }


    /* (non-Javadoc)
     * 配牌
     * @see com.za.gameservers.sss.service.SSSService#peiPai(java.lang.String, java.util.UUID, int, net.sf.json.JSONObject)
     */
    @Override
    public void peiPai(String roomNo, String uuid, int type, JSONObject data) {

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        // 完成配牌操作
        room.getPlayerPaiJu().get(uuid).setStatus(2);

        int isReady = 0;
        JSONArray us=new JSONArray();
        for (String uid : room.getPlayerPaiJu().keySet()) {
            if(room.getPlayerPaiJu().get(uid).getStatus()==2){ // 获取当前已配好牌的玩家
                isReady++;
                us.add(uid);
            }
        }
        int count = 0;
        for (String uid:room.getPlayerPaiJu().keySet()) {
            int ready = room.getPlayerPaiJu().get(uid).getIsReady();
            if(ready!=0){
                count++;
            }
			/*if (room.getGameType()==3&&room.getPlayerPaiJu().get(uid).getTotalScore()<room.getMinscore()) {
				//元宝不足不能准备
				count--;
				room.getPlayerPaiJu().get(uc).setIsReady(0);
			}*/
        }
        System.err.println("<-----------"+count+"----------->");

        if (isReady == count) {
            // 游戏状态
            RoomManage.gameRoomMap.get(roomNo).setGameStatus(4);
        }
        Player player = room.getPlayerPaiJu().get(uuid);
        String des="";
        if(type==1){ // 系统自动配牌
            //最优排牌型
            String[] you=SSSOrdinaryCards.sort(player.getPai());
            //LogUtil.print("【配牌阶段】自动配牌，原牌："+Arrays.toString(player.getPai())+"，优牌："+Arrays.toString(you));
            player.setPai(you);
            player.setIsAuto(1);

            //存入用户牌记录

            des="【配牌阶段-自动】用户："+room.getPlayerMap().get(uuid).getName()+"，用户手牌："+Arrays.toString(you);
			/*JSONObject obj=new JSONObject();
			obj.element("des", des);
			obj.element("userid", room.getPlayerMap().get(uuid).getId());
			obj.element("roomType", room.getRoomType());
			obj.element("roomNo", roomNo);
			obj.element("GameIndex", room.getGameIndex()+1);
			LogUtil.addZaGameRecord(obj);*/

        }else if(type==2){ // 玩家手动配牌
            JSONArray p=data.getJSONArray("myPai");
            System.out.println("手动："+p.toString());
            int[] p1=player.getMyPai();
            boolean is=false;
            for (int i = 0; i < p1.length; i++) {
                is=p.contains(p1[i]);
                if (!is) {
                    break;
                }
            }
            if (is) {
                System.out.println("转化之后："+player.togetMyPai(p).toString());

                JSONArray t= SSSComputeCards.judge(player.togetMyPai(p));
                if ("倒水".equals(t.get(0))) {

                    String[] you=SSSOrdinaryCards.sort(player.getPai());
                    player.setPai(you);
                    des="【配牌阶段-倒水】用户："+room.getPlayerMap().get(uuid).getName()+"，用户手牌："+Arrays.toString(you);
					/*JSONObject obj=new JSONObject();
					obj.element("des", des);
					obj.element("userid", room.getPlayerMap().get(uuid).getId());
					obj.element("roomType", room.getRoomType());
					obj.element("roomNo", roomNo);
					obj.element("GameIndex", room.getGameIndex()+1);
					LogUtil.addZaGameRecord(obj);*/
                }else{
					/*for (int ii = 3; ii < 7; ii++) {

						for (int jj = ii+1; jj < 7; jj++) {

							if (p.getInt(ii)<p.getInt(jj)) {
								int o=p.getInt(ii);
								p.element(ii, p.getInt(jj));
								p.element(jj, o);

							}
						}
					}*/
                    //System.out.println("配牌排序之后："+p.toString());
                    //LogUtil.print("前端配牌之后："+p.toString());
                    String[] str=new String[13];
                    for (int i = 0; i < p.size(); i++) {
                        if (p.getInt(i)<20) {
                            String a="1-"+p.getString(i);
                            str[i]=a;
                        }else if(p.getInt(i)>20&&p.getInt(i)<40){
                            String a="2-"+(p.getInt(i)-20);
                            str[i]=a;
                        }else if(p.getInt(i)>40&&p.getInt(i)<60){
                            String a="3-"+(p.getInt(i)-40);
                            str[i]=a;
                        }else if(p.getInt(i)>60){
                            String a="4-"+(p.getInt(i)-60);
                            str[i]=a;
                        }
                        //str[i]=p.getString(i);
                    }
                    //System.out.println("存入："+str.toString());

                    //
                    str=SSSGameRoom.AppointSort(str, 0, 2);

                    str=SSSGameRoom.AppointSort(str, 3, 7);

                    str=SSSGameRoom.AppointSort(str, 8, 12);
                    //LogUtil.print("前端配牌转化之后："+str.toString());
                    player.setPai(str);
                    player.setIsAuto(2);

                    //存入用户牌记录

                    des="【配牌阶段-手动】用户："+room.getPlayerMap().get(uuid).getName()+"，用户手牌："+Arrays.toString(str);
					/*JSONObject obj=new JSONObject();
					obj.element("des", des);
					obj.element("userid", room.getPlayerMap().get(uuid).getId());
					obj.element("roomType", room.getRoomType());
					obj.element("roomNo", roomNo);
					obj.element("GameIndex", room.getGameIndex()+1);
					LogUtil.addZaGameRecord(obj);*/

                }
            }else{

                //LogUtil.print("牌不匹配.传入："+p.toString()+" 原来："+player.getMyPai());
            }
        }else{
            int sc=SSSSpecialCards.isSpecialCards(player.getPai(),room.getSetting());
            String[] you=SSSSpecialCardSort.CardSort(player.getPai(),sc);
            player.setPai(you);
            player.setIsAuto(3);


            des="【配牌阶段-特殊】用户："+room.getPlayerMap().get(uuid).getName()+"，用户手牌："+Arrays.toString(you);

            System.out.println("特殊牌");

        }

		/*JSONObject obj=new JSONObject();
		obj.element("des", des);
		obj.element("userid", room.getPlayerMap().get(uuid).getId());
		obj.element("roomType", room.getRoomType());
		obj.element("roomNo", roomNo);
		obj.element("GameIndex", room.getGameIndex()+1);
		LogUtil.addZaGameRecord(obj);*/

        // 通知玩家
        JSONObject result = new JSONObject();
        result.put("type", 1);
        result.put("myIndex", room.getPlayerIndex(uuid));

        for (String uid  : room.getPlayerPaiJu().keySet()) {
            if (!room.getRobotList().contains(uid)) {
                Playerinfo pi=room.getPlayerMap().get(uid);
                SocketIOClient clientother=GameMain.server.getClient(pi.getUuid());
                if(clientother!=null){

				/*if(uuid.equals(uid)){
					result.put("myPai",room.getPlayerPaiJu().get(uuid).getMyPai());
				}*/

                    //String msg="游戏ID：4,房间号："+roomNo+",第"+(room.getGameIndex()+1)+"局,用户："+uuid+",配牌："+Arrays.toString(room.getPlayerPaiJu().get(uuid).getPai());
                    //LogUtil.print(msg);

                    clientother.sendEvent("gameActionPush_SSS", result);
                }
            }
        }
    }


    /* (non-Javadoc)
     * 结算 庄闲比对 霸王庄
     * @see com.za.gameservers.sss.service.SSSService#jieSuan(java.lang.String, java.util.UUID)
     */
    @Override
    public void jieSuan(String roomNo) {

        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);
        //int isReady = 0;

        JSONArray us=new JSONArray();
        JSONArray uid=new JSONArray();
        for (String uuid : room.getPlayerPaiJu().keySet()) {

            if(room.getPlayerPaiJu().get(uuid).getStatus()==2){ // 获取当前已配好牌的玩家
                //isReady++;
                us.add(uuid);
                uid.add(room.getPlayerMap().get(uuid).getId());
            }
        }

        boolean flag = true;
        for (String uuid : room.getPlayerPaiJu().keySet()) {
            if (room.getPlayerPaiJu().get(uuid).getMyPai().length!=0) {
                if (room.getPlayerPaiJu().get(uuid).getStatus()!=2) {
                    flag = false;
                    break;
                }
            }
        }

        if(flag&&room.getGameStatus()==4){

            JSONObject obj=new JSONObject();
            obj.put("type", 2);
            obj.put("gameIndex",room.getGameIndex()+1);
            String a=room.getMaPai();
            String[] val = a.split("-");
            int num = 0;
            if(val[0].equals("2")){
                num = 20;
            }else if(val[0].equals("3")){
                num = 40;
            }else if(val[0].equals("4")){
                num = 60;
            }
            int ma = Integer.valueOf(val[1]) + num;
            obj.put("mapai", ma);
            JSONArray data=new JSONArray();
            int isma=0;
            int ismaz=0;
            Player zhuang = room.getPlayerPaiJu().get(room.getZhuang());
            int sc0= SSSSpecialCards.isSpecialCards(zhuang.getPai(),room.getSetting());
            if (zhuang.getIsAuto()==2) {sc0=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
            int score=SSSSpecialCards.score(sc0,room.getSetting());

            if (sc0==0) {
                zhuang.setOrdinary(zhuang.getOrdinary()+1);//
            }else{
                zhuang.setSpecial(zhuang.getSpecial()+1);
            }
            //room.getPlayerPaiJu().get(uid).setGun(gun+room.getPlayerPaiJu().get(uid).getGun());
            JSONArray tt=new JSONArray();
            JSONObject tt1=new JSONObject();
            tt1.element("score", 0);
            tt1.element("type", 0);
            tt.add(tt1);
            JSONObject tt2=new JSONObject();
            tt2.element("score", 0);
            tt2.element("type", 0);
            tt.add(tt2);
            JSONObject tt3=new JSONObject();
            tt3.element("score", 0);
            tt3.element("type", 0);
            tt.add(tt3);
            JSONArray dpzz=new JSONArray();//被打枪

            List<String> upai=Arrays.asList(zhuang.getPai());
            if (upai.contains(room.getMaPai())) {
                isma=1;
                ismaz=1;
            }
            zhuang.setScore(0);
            //int gun=0;
            int l=0;
            for (int i = 0; i < us.size(); i++) {

                if (!room.getZhuang().equals(us.getString(i))) {
                    Player u = room.getPlayerPaiJu().get(us.getString(i));
                    List<String> upai1=Arrays.asList(u.getPai());

                    int isma1=0;
                    if (upai1.contains(room.getMaPai())) {
                        isma=1;
                        isma1=1;
                    }

                    int sc1= SSSSpecialCards.isSpecialCards(u.getPai(),room.getSetting());
                    if (u.getIsAuto()==2) {sc1=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
                    int scA=0;//得分
                    int scB=0;//得分
                    int gun=0;
                    JSONObject u1=new JSONObject();
                    u1.element("index", room.getPlayerIndex(us.getString(i)));//用户下标
                    u1.element("myPai", u.getMyPai());//牌
                    u1.element("myPaiType", sc1);//牌

                    JSONArray t=new JSONArray();

                    JSONArray dp=new JSONArray();//被打枪

                    int score1=SSSSpecialCards.score(sc1,room.getSetting());


                    if (score>0&&score1>0) {
							/*scA=score1-score;//玩家
							scB=score-score1;//庄
							*/
                        if (score>score1) {
                            scA=scA-score;//玩家
                            scB=scB+score;//庄
                        }else if (score<score1){
                            scA=scA+score1;
                            scB=scB-score1;//庄
                        }else{
                            scA=scA+score-score1;
                            scB=scB+score-score1;//庄
                        }

							/*if (score>score1) {
								//room.getPlayerPaiJu().get(uid).setScore(u.getScore()-score);//存入比分分
							}else if(score<score1){
								scA=score1-score;
								scB=score-score1;
							}else{
								scA=score1;
								scB=score;
							}*/
                    }else if(score>0&&score1==0){
                        scA=-score;
                        scB=scB+score;
                    }else if(score==0&&score1>0){
                        scA=score1;
                        scB=scB-score1;
                    }
                    //else if(sc0==0&&sc1==0){
                    JSONObject t1=new JSONObject();
                    t1.element("score", 0);
                    t1.element("type", 0);
                    t.add(t1);
                    JSONObject t2=new JSONObject();
                    t2.element("score", 0);
                    t2.element("type", 0);
                    t.add(t2);
                    JSONObject t3=new JSONObject();
                    t3.element("score", 0);
                    t3.element("type", 0);
                    t.add(t3);

                    JSONObject dp1=new JSONObject();//被打枪
                    JSONObject dpz=new JSONObject();//被打枪

                    JSONObject re=SSSComputeCards.compare(u.getPai(),zhuang.getPai());

                    JSONArray re1=JSONArray.fromObject(re.getJSONArray("result").get(0));

                    JSONArray re2=JSONArray.fromObject(re.getJSONArray("result").get(1));

                    int d=0;
                    int p=0;

                    if(sc0==0&&sc1==0){
                        scA+=re.getInt("A");//A对比得分
                        scB+=re.getInt("B");//A对比得分
                    }

                    for (int j = 0; j < re1.size(); j++) {

                        t.getJSONObject(j).element("score",t.getJSONObject(j).getInt("score")+re1.getJSONObject(j).getInt("score"));
                        t.getJSONObject(j).element("type",  re1.getJSONObject(j).getInt("type"));
                        tt.getJSONObject(j).element("score",tt.getJSONObject(j).getInt("score")+re2.getJSONObject(j).getInt("score"));
                        tt.getJSONObject(j).element("type",  re2.getJSONObject(j).getInt("type"));

                        if (re1.getJSONObject(j).getInt("score")>re2.getJSONObject(j).getInt("score")) {
                            d++;

                        }else if(re1.getJSONObject(j).getInt("score")<re2.getJSONObject(j).getInt("score")){
                            p++;
                        }
                    }

                    if(d==3){
                        scA+=re.getInt("A");//
                        scB+=re.getInt("B");//A对比得分
                        dp1.element("index",room.getPlayerIndex(room.getZhuang()));
                        dp1.element("code", -1);


                        gun++;
                    }
                    if(p==3){
								/*dp1.element("index",room.getPlayerIndex(room.getZhuang()));
									dp1.element("code", -1);*/
                        l++;//被庄打枪
                        //被打枪(如果被打枪 分数肯定是负数)
                        scA+=re.getInt("A");
                        scB+=re.getInt("B");//A对比得分
                        dpz.element("index", room.getPlayerIndex(us.getString(i)));
                        dpz.element("code", -1);
                    }
                    dpzz.add(dpz);
                    dp.add(dp1);
                    //}
                    if (sc1==0) {
                        u.setOrdinary(u.getOrdinary()+1);//
                    }else{
                        u.setSpecial(u.getSpecial()+1);
                    }
                    u.setGun(gun+u.getGun());

                    u.setScore(scA);//存入比分分
                    u.setTotalScore(u.getScore()+u.getTotalScore());

                    u1.element("qld", 0);//

                    zhuang.setScore(zhuang.getScore()+scB);//存入比分分
                    zhuang.setTotalScore(scB+zhuang.getTotalScore());
                    u1.element("isma", isma1);//此人手牌是否存在马牌
                    u1.element("score",u.getScore());//当前牌局分
                    u1.element("totalscore",u.getTotalScore());//总分
                    u1.element("result", t);//牌
                    u1.element("dp", dp);//打枪
                    data.add(u1);
                }

            }

            JSONObject uz=new JSONObject();
            uz.element("index", room.getPlayerIndex(room.getZhuang()));//用户下标
            uz.element("myPai", zhuang.getMyPai());//牌
            uz.element("myPaiType", sc0);//牌

            uz.element("qld", 0);//全垒打
            zhuang.setGun(l+zhuang.getGun());

            uz.element("isma", ismaz);//此人手牌是否存在马牌
            uz.element("score",zhuang.getScore());//当前牌局分
            uz.element("totalscore",zhuang.getTotalScore());//总分
            uz.element("result", tt);//牌
            uz.element("dp", dpzz);//打枪
            data.add(uz);

            obj.put("data",data);
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


            //System.out.println("排序后："+data.toString());

            JSONArray showIndex=new JSONArray();

            showIndex.add(one);
            showIndex.add(two);
            showIndex.add(three);


            obj.put("showIndex",showIndex);
            obj.put("isma",isma);//此局是否存在马牌
            obj.put("jiesuan",1);//结算类型 1 小结  2 大结



            for (String uuuid : room.getPlayerPaiJu().keySet()) {
                Player uuu = room.getPlayerPaiJu().get(uuuid);
                Playerinfo upi = room.getPlayerMap().get(uuuid);

                JSONObject users=new JSONObject();
                users.element("score",uuu.getScore() );//当前牌局分
                users.element("totalscore",uuu.getTotalScore());//总分
                users.element("myIndex", room.getPlayerIndex(uuuid));

                obj.put("users",users);

                System.out.println("uid："+uuuid+"数据："+obj.toString());

                SocketIOClient clientother=GameMain.server.getClient(upi.getUuid());
                if(clientother!=null){
                    clientother.sendEvent("gameActionPush_SSS", obj);
                }


                // 重置玩家状态信息
                uuu.setIsReady(0);

                //存入用户牌记录

					/*String des="【结算阶段】用户："+room.getPlayerMap().get(uuuid).getName()+"，用户手牌："+Arrays.toString(uuu.getPai());
					JSONObject obj1=new JSONObject();
					obj1.element("des", des);
					obj1.element("userid", room.getPlayerMap().get(uuuid).getId());
					obj1.element("roomType", room.getRoomType());
					obj1.element("roomNo", roomNo);
					obj1.element("GameIndex", room.getGameIndex()+1);
					LogUtil.addZaGameRecord(obj1);*/
                //room.getPlayerPaiJu().get(uuuid).setStatus(0);
            }

            // 游戏状态
            RoomManage.gameRoomMap.get(roomNo).setGameStatus(5);
            // 局数记录
            RoomManage.gameRoomMap.get(roomNo).setGameIndex(room.getGameIndex()+1);

            if(room.getRoomType()==3){
                //元宝结算
                for (String uuuid : room.getPlayerPaiJu().keySet()) {
                    Player uuu = room.getPlayerPaiJu().get(uuuid);

                    Playerinfo uinfo = room.getPlayerMap().get(uuuid);

                    if (uuu.getTotalScore()<=0) {
                        //负数清零
                        String sql = "update za_users set yuanbao=yuanbao-yuanbao where id=?";
                        GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{uinfo.getId()}, SqlModel.EXECUTEUPDATEBYSQL));
                        //int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ uinfo.getId() });
                        //System.out.println("更新用户金币："+i);
                    }else{

                        String sql = "update za_users set yuanbao=yuanbao+? where id=?";
                        GameMain.sqlQueue.addSqlTask(new SqlModel(sql, new Object[]{ uuu.getScore(),uinfo.getId() }, SqlModel.EXECUTEUPDATEBYSQL));
                        //int i=DBUtil.executeUpdateBySQL(sql, new Object[]{ uuu.getScore(),uinfo.getId() });
                        //System.out.println("更新用户金币："+i);
                    }

                    //扣除房卡记录
                    String sql1 = "insert into za_userdeduction(userid,roomid,gid,roomNo,type,sum,creataTime) values(?,?,?,?,?,?,?)";
                    //DBUtil.executeUpdateBySQL(sql1, new Object[]{uinfo.getId(),4, roomNo,3,uuu.getScore(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())});
                    Object[] params = new Object[]{uinfo.getId(),4, roomNo,3,uuu.getScore(), new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())};
                    GameMain.sqlQueue.addSqlTask(new SqlModel(sql1, params, SqlModel.EXECUTEUPDATEBYSQL));
                }
            }
            //SaveLogsThreadSSS sl=new SaveLogsThreadSSS(room, us,uid,false);
            //gamelog(room,us);
            //sl.start();
            GameMain.sqlQueue.addSqlTask(new SqlModel(SqlModel.SAVELOGS_SSS, room, us, uid, false));
            try {
                // 扣除房卡，更改房间局数
                //IService server = (IService) RegisterServer.registry.lookup("sysService");
                //server.settlementRoomNo(roomNo);
                mjBiz.settlementRoomNo(roomNo);
            } catch (Exception e) {
                //LogUtil.print("扣除房卡，更改房间局数方法异常："+e.getMessage());
                e.printStackTrace();
            }






        }
    }


    /* (non-Javadoc)
     * 结算  牌型两两比对
     * @see com.za.gameservers.sss.service.SSSService#jieSuan(java.lang.String, java.util.UUID)
     */
    @Override
    public void jieSuan1(String roomNo) {


        SSSGameRoom room=(SSSGameRoom) RoomManage.gameRoomMap.get(roomNo);

        //Player te = room.getPlayerPaiJu().get(clientID);

        //int isReady = 0;
        JSONArray us=new JSONArray();
        JSONArray uid=new JSONArray();
        for (String uuid : room.getPlayerPaiJu().keySet()) {
            if(room.getPlayerPaiJu().get(uuid).getStatus()==2){ // 获取当前已配好牌的玩家
                //isReady++;
                us.add(uuid);
                uid.add(room.getPlayerMap().get(uuid).getId());
            }
        }

        boolean flag = true;
        for (String uuid : room.getPlayerPaiJu().keySet()) {
            if (room.getPlayerPaiJu().get(uuid).getMyPai().length!=0) {
                if (room.getPlayerPaiJu().get(uuid).getStatus()!=2) {
                    flag = false;
                    break;
                }
            }
        }

        if(flag&&room.getGameStatus()==4){
            GameMain.singleTime.deleteTimer(roomNo);
            JSONObject obj = new JSONObject();

            obj.put("type", 2);
            String a=room.getMaPai();
            String[] val = a.split("-");
            int num = 0;
            if(val[0].equals("2")){
                num = 20;
            }else if(val[0].equals("3")){
                num = 40;
            }else if(val[0].equals("4")){
                num = 60;
            }
            int ma = Integer.valueOf(val[1]) + num;
            obj.put("mapai", ma);

            int isma=0;
            int qldsrco=0;
            Boolean qld=false;
            String qldacc="";
            JSONArray data=new JSONArray();
            for (int i = 0; i < us.size(); i++) {
                Player u = room.getPlayerPaiJu().get(us.getString(i));
                int sc= SSSSpecialCards.isSpecialCards(u.getPai(),room.getSetting());
                if (u.getIsAuto()==2) {sc=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
                int gun=0;
                for (int j = 0; j < us.size(); j++) {
                    if (!us.getString(j).equals(us.getString(i))) {
                        Player uu = room.getPlayerPaiJu().get(us.getString(j));
                        int sc1= SSSSpecialCards.isSpecialCards(uu.getPai(),room.getSetting());
                        if (uu.getIsAuto()==2) {sc1=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
                        if(sc==0&&sc1==0){
                            JSONObject re=SSSComputeCards.compare(u.getPai(), uu.getPai());

                            JSONArray re1=JSONArray.fromObject(re.getJSONArray("result").get(0));
                            JSONArray re2=JSONArray.fromObject(re.getJSONArray("result").get(1));
                            int d=0;

                            for (int ii = 0; ii < re1.size(); ii++) {

                                if (re1.getJSONObject(ii).getInt("score")>re2.getJSONObject(ii).getInt("score")) {
                                    d++;
                                }

                                if(d==3){
                                    gun++;
                                }
                            }
                        }
                    }
                }
                if (us.size()-1==gun&&room.getUserSet().size()>=room.getSetting().getInt("qld")&&us.size()>=room.getSetting().getInt("qld")) {
                    qld=true;
                    qldacc=us.getString(i);
                }
            }
            for (int i = 0; i < us.size(); i++) {

                /*for (UUID uuid : room.getPlayerPaiJu().keySet()) {*/
                int gun=0;
                int scA=0;
                int isma1=0;
                Player u = room.getPlayerPaiJu().get(us.getString(i));

                List<String> upai=Arrays.asList(u.getPai());
                if (upai.contains(room.getMaPai())) {
                    isma=1;
                    isma1=1;
                }

                JSONObject u1=new JSONObject();
                u1.element("index",  room.getPlayerIndex(us.getString(i)));//用户下标
                u1.element("myPai", u.getMyPai());//牌

                int sc= SSSSpecialCards.isSpecialCards(u.getPai(),room.getSetting());
                if (u.getIsAuto()==2) {sc=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
                int score=SSSSpecialCards.score(sc,room.getSetting());

                u1.element("myPaiType", sc);//牌

                //JSONArray result=new JSONArray();
                JSONArray dp=new JSONArray();//打枪
                JSONArray dp0=new JSONArray();//被打枪

                JSONArray t=new JSONArray();
                JSONObject t1=new JSONObject();
                t1.element("score", 0);
                t1.element("type", 0);
                t.add(t1);
                JSONObject t2=new JSONObject();
                t2.element("score", 0);
                t2.element("type", 0);
                t.add(t1);
                JSONObject t3=new JSONObject();
                t3.element("score", 0);
                t3.element("type", 0);
                t.add(t1);

                // JSONObject dp2=new JSONObject();//打枪
                //for (UUID uid : room.getPlayerPaiJu().keySet()) {
                for (int j = 0; j < us.size(); j++) {
                    if (!us.getString(j).equals(us.getString(i))) {
                        int defen=0;
                        JSONObject dp1=new JSONObject();//被打枪
                        JSONObject dp2=new JSONObject();//打枪
                        Player uu = room.getPlayerPaiJu().get(us.getString(j));
                        int isma2=0;
                        List<String> uupai=Arrays.asList(uu.getPai());
                        if (uupai.contains(room.getMaPai())) {
                            isma2=1;
                        }
                        int sc1= SSSSpecialCards.isSpecialCards(uu.getPai(),room.getSetting());
                        if (uu.getIsAuto()==2) {sc1=0;}//如果手动配牌，就是有特殊牌型，也按照配牌算
                        int scor1=SSSSpecialCards.score(sc1,room.getSetting());

                        System.out.println("特殊牌型："+sc+","+sc1+";分数："+score+","+scor1);

                        if (score>0&&scor1>0) {
                            if (score>scor1) {
                                scA=scA+score;
                                //defen=score;
                            }else if (score<scor1){
                                scA=scA-scor1;
                                //defen=-scor1;
                            }else{
                                scA=scA+score-scor1;
                                //defen=score-scor1;
                            }
											/*
											 *  if ((sc==14&&sc1<14)||(sc==13&&sc1<13)) {
												scA=scA+score;
											}else if((sc<14&&sc1==14)||(sc<13&&sc1==13)){
												scA=scA-scor1;
											}else{
												scA=scA+score-scor1;
											}*/

											/*if (score>scor1) {
												//room.getPlayerPaiJu().get(uid).setScore(u.getScore()-score);//存入比分分
											}else if(score<scor1){
												scA=scA+(scor1-score);
											}else{
												//scA=score;
											}*/

                        }else if(score>0&&scor1==0){
                            scA=scA+score;
                            //defen=score;
                            //room.getPlayerPaiJu().get(uid).setScore(u.getScore()-score);//存入比分分

                        }else if(score==0&&scor1>0){
                            scA=scA-scor1;
                            //defen=-scor1;
                            //room.getPlayerPaiJu().get(uid).setScore(u.getScore()+score);//存入比分分

                        }else if(score==0&&scor1==0){

                            JSONObject re=SSSComputeCards.compare(u.getPai(), uu.getPai());

                            JSONArray re1=JSONArray.fromObject(re.getJSONArray("result").get(0));
                            JSONArray re2=JSONArray.fromObject(re.getJSONArray("result").get(1));
                            int d=0;
                            int p=0;
                            for (int ii = 0; ii < re1.size(); ii++) {

                                t.getJSONObject(ii).element("score",t.getJSONObject(ii).getInt("score")+re1.getJSONObject(ii).getInt("score"));
												/*if (isma1==1) {
													t.getJSONObject(ii).element("score",t.getJSONObject(ii).getInt("score")+(re1.getJSONObject(ii).getInt("score")*2));
												}*/
                                t.getJSONObject(ii).element("type",  re1.getJSONObject(ii).getInt("type"));
                                if (re1.getJSONObject(ii).getInt("score")>re2.getJSONObject(ii).getInt("score")) {
                                    d++;

                                }else if(re1.getJSONObject(ii).getInt("score")<re2.getJSONObject(ii).getInt("score")){
                                    p++;
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
                            if(d==3){
                                //dp2.element("index", room.getPlayerIndex(uid));
                                //dp0.add(dp2);
                                //打枪 +分
                                dp1.element("index", room.getPlayerIndex(us.getString(j)));
                                dp1.element("code", -1);
                                defen=defen*2;
                                //scA+=re.getInt("A")*2;//
                                gun++;
                            }else if(p==3){
                                dp2.element("index", room.getPlayerIndex(us.getString(j)));
                                dp2.element("code", -1);
                                //被打枪(如果被打枪 分数肯定是负数)
                                //scA+=re.getInt("A")*2;
                                defen=defen*2;
                            }
                            if ("HHMJ".equals(room.getSetting().getString("platform"))) {

                                //全垒打(只对被全垒打 的人 加倍)
                                if (qld&&(qldacc.equals(us.getString(i))||qldacc.equals(us.getString(j)))) {
                                    defen=defen*2;
                                }
                            }else{

                                //全垒打(所有人 加倍)
                                if (qld) {
                                    defen=defen*2;
                                }
                            }


                            scA+=defen;

                            dp.add(dp1);
                            dp0.add(dp2);
                        }

                    }
                }

                if (sc==0) {
                    u.setOrdinary(u.getOrdinary()+1);// 才
                }else{
                    u.setSpecial(u.getSpecial()+1);
                }

                u.setGun(gun+u.getGun());

                //是否全垒打&&room.getReadyCount()>2
                if (us.size()-1==gun&&room.getUserSet().size()>=room.getSetting().getInt("qld")&&us.size()>=room.getSetting().getInt("qld")) {
                    u1.element("qld", 1);//全垒打
                    u.setSwat(u.getSwat()+1);
                    //T淘  全垒打 3人可触发
								/*for (int j = 0; j < us.size(); j++) {
										if (!us.getString(j).equals(us.getString(i))) {
												Player uu = room.getPlayerPaiJu().get(us.getString(j));
												JSONObject re=SSSComputeCards.compare(u.getPai(), uu.getPai());
												//int uus=uu.getScore()-(re.getInt("A")*2);
												//uu.setScore(uus);

												if (qldsrco==0) {
													qldsrco=re.getInt("B")*2;
													if (isma1==1) {
														qldsrco=re.getInt("B")*4;
													}
												}
											}
										}*/
							/*	for (UUID id : room.getPlayerPaiJu().keySet()) {
									if (id!=UUID.fromString(us.getString(i))) {
										room.getPlayerPaiJu().get(id).setScore(room.getPlayerPaiJu().get(id).getScore()*2);//存入比分分
										room.getPlayerPaiJu().get(id).setTotalScore(room.getPlayerPaiJu().get(id).getTotalScore()-scA);
									}
								}
								scA=scA*2;*/
                }else{
                    u1.element("qld", 0);//
                }
                System.out.println("用户分数："+scA*room.getScore());
                u.setScore(scA*room.getScore());//存入比分分
                double ta=u.getScore()+u.getTotalScore();
                if (ta<0) {
                    ta=0;
                }
                u.setTotalScore(ta);

                u1.element("score",u.getScore());//当前牌局分
                u1.element("totalscore",u.getTotalScore());//总分
                u1.element("result", t);//牌
                u1.element("dp", dp);//打枪
                u1.element("dp0", dp0);//被打枪
                u1.element("isma", isma1);//此人手牌是否存在马牌
                u1.element("account", us.getString(i));//账号
                data.add(u1);
            }
            System.out.println("全垒打分数："+qldsrco);
            //全垒打 当局分数*2
					/*for (int i = 0; i < data.size(); i++) {
						if (data.getJSONObject(i).getInt("qld")==1) {
							System.out.println("全垒打");
							for (int j = 0; j <data.size(); j++) {
								  if (j!=i) {
									  System.out.println("全垒打输1");
									  data.getJSONObject(j).element("totalscore", data.getJSONObject(j).getInt("totalscore")+qldsrco*room.getScore());
									  data.getJSONObject(j).element("score", data.getJSONObject(j).getInt("score")+qldsrco*room.getScore());
								  }else{
									  System.out.println("全垒打主1");
									  data.getJSONObject(j).element("totalscore", data.getJSONObject(j).getInt("totalscore")-(qldsrco*(data.size()-1)*room.getScore()));
									  data.getJSONObject(j).element("score", data.getJSONObject(j).getInt("score")-(qldsrco*(data.size()-1)*room.getScore()));
								  }
								}
							for (String id : room.getPlayerPaiJu().keySet()) {
									if (data.getJSONObject(i).getInt("index")!=room.getPlayerIndex(id)) {
										 System.out.println("全垒打输2");
										room.getPlayerPaiJu().get(id).setTotalScore(room.getPlayerPaiJu().get(id).getTotalScore()+qldsrco*room.getScore());
										room.getPlayerPaiJu().get(id).setScore(room.getPlayerPaiJu().get(id).getScore()+qldsrco*room.getScore());//存入比分分
									}else{ System.out.println("全垒打主2");
										room.getPlayerPaiJu().get(id).setTotalScore(room.getPlayerPaiJu().get(id).getTotalScore()-(qldsrco*(data.size()-1)*room.getScore()));
										room.getPlayerPaiJu().get(id).setScore(room.getPlayerPaiJu().get(id).getScore()-(qldsrco*(data.size()-1)*room.getScore()));//存入比分分
									}
							}
						}
					}*/

            System.out.println("排序前："+data.toString());
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

            //System.out.println("排序后："+data.toString());

            JSONArray showIndex=new JSONArray();
            showIndex.add(one);
            showIndex.add(two);
            showIndex.add(three);

            System.out.println("展示顺序："+showIndex);
            //System.out.println("马牌之前："+data);


            //马牌 各自输赢分数*2
					/*if (isma==1&&room.getMaPaiType()>0) {
						System.out.println("马牌");
						for (int i = 0; i < data.size(); i++) {
							if (data.getJSONObject(i).getInt("isma")==1) {

								for (int j = 0; j <data.size(); j++) {
									  if (j!=i) {

										  data.getJSONObject(j).element("totalscore", data.getJSONObject(j).getInt("totalscore")+qldsrco*room.getScore());
										  data.getJSONObject(j).element("score", data.getJSONObject(j).getInt("score")+qldsrco*room.getScore());
									  }else{
										  System.out.println("全垒打主1");
										  data.getJSONObject(j).element("totalscore", data.getJSONObject(j).getInt("totalscore")-(qldsrco*(data.size()-1)*room.getScore()));
										  data.getJSONObject(j).element("score", data.getJSONObject(j).getInt("score")-(qldsrco*(data.size()-1)*room.getScore()));
									  }
									}
								for (String id : room.getPlayerPaiJu().keySet()) {
										if (data.getJSONObject(i).getInt("index")!=room.getPlayerIndex(id)) {

											room.getPlayerPaiJu().get(id).setTotalScore(room.getPlayerPaiJu().get(id).getTotalScore()+qldsrco*room.getScore());
											room.getPlayerPaiJu().get(id).setScore(room.getPlayerPaiJu().get(id).getScore()+qldsrco*room.getScore());//存入比分分
										}else{
											room.getPlayerPaiJu().get(id).setTotalScore(room.getPlayerPaiJu().get(id).getTotalScore()-(qldsrco*(data.size()-1)*room.getScore()));
											room.getPlayerPaiJu().get(id).setScore(room.getPlayerPaiJu().get(id).getScore()-(qldsrco*(data.size()-1)*room.getScore()));//存入比分分
										}
								}
							}
						}
					}*/

            for (String uuuid : room.getPlayerPaiJu().keySet()) {
                Player uuu = room.getPlayerPaiJu().get(uuuid);
                Playerinfo upi=room.getPlayerMap().get(uuuid);
                JSONObject users=new JSONObject();
                users.element("score",uuu.getScore() );//当前牌局分
                users.element("totalscore",uuu.getTotalScore());//总分
                users.element("myIndex", room.getPlayerIndex(uuuid));

                //users.element("dp",dp);//打枪
                obj.put("data",data);
                obj.put("jiesuan",1);//结算类型 1 小结  2 大结
                obj.put("isma",isma);//此局是否存在马牌
                obj.put("showIndex",showIndex);
                obj.put("gameIndex",room.getGameIndex()+1);

                System.out.println("uid："+uuuid+"数据："+obj.toString());
                if (!room.getRobotList().contains(upi.getAccount())) {
                    SocketIOClient clientother=GameMain.server.getClient(upi.getUuid());
                    if(clientother!=null){
                        clientother.sendEvent("gameActionPush_SSS", obj);
                    }
                }
                room.getPlayerPaiJu().get(uuuid).setIsReady(0);
                //room.getPlayerPaiJu().get(uuuid).setStatus(0);

                //存入用户牌记录

						/*String des="【结算阶段】用户："+room.getPlayerMap().get(uuuid).getName()+"，用户手牌："+Arrays.toString(uuu.getPai());
						JSONObject obj1=new JSONObject();
						obj1.element("des", des);
						obj1.element("userid", room.getPlayerMap().get(uuuid).getId());
						obj1.element("roomType", room.getRoomType());
						obj1.element("roomNo", roomNo);
						obj1.element("GameIndex", room.getGameIndex()+1);
						LogUtil.addZaGameRecord(obj1);*/
            }


            // 游戏状态
            RoomManage.gameRoomMap.get(roomNo).setGameStatus(5);

            System.out.println("当前局数："+room.getGameIndex()+"下一局："+(room.getGameIndex()+1));
            // 局数记录
            RoomManage.gameRoomMap.get(roomNo).setGameIndex(room.getGameIndex()+1);

            if (room.getRoomType()==1) {

                if (room.getLevel()==-1) {
                    // 竞技场积分结算

                    String sql = "update za_users SET score = CASE id  $ END WHERE id IN (/)";
                    String z="";
                    String d="";

                    StringBuffer sqlx=new StringBuffer();
                    sqlx.append("insert into za_userdeduction(userid,roomid,gid,roomNo,type,sum,creataTime) values $");
                    String ve="";
                    String te=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    // 房间信息
                    JSONObject roomInfo = mjBiz.getRoomInfoByRno(room.getRoomNo());
                    if (roomInfo == null) {
                        roomInfo = mjBiz.getRoomInfoByRno1(room.getRoomNo());
                    }
                    String sqlj="select memo from za_arena where gid=? ";
                    Object[] params = new Object[] {4};
                    JSONObject jjc= DBUtil.getObjectBySQL(sqlj,params);

                    //扣除数据库用户游戏房卡数--(房卡扣除规则：用户下完完整一局后才扣除房间总房卡数)
                    String sql4 = "update za_users set roomcard=roomcard-? where id in ($)";
                    String dd="";

                    for (String uuuid : room.getPlayerPaiJu().keySet()) {
                        Player uuu = room.getPlayerPaiJu().get(uuuid);

                        Playerinfo uinfo = room.getPlayerMap().get(uuuid);
                        if (uuu.getTotalScore()<=0) {
                            z=z+" WHEN "+uinfo.getId()+" THEN 0";
                        }else{
                            z=z+" WHEN "+uinfo.getId()+" THEN score+"+uuu.getScore();
                        }

                        d=d+uinfo.getId()+",";

                        if (uuu.getGameNum()==0) {
                            ve=ve+"("+uinfo.getId()+","+roomInfo.getLong("id")+","+4+","+roomNo+","+0+","+jjc.getInt("memo")+","+te+"),";
                            dd=dd+uinfo.getId()+",";
                        }
                        uuu.setGameNum(uuu.getGameNum()+1);
                        try {
                            IService server = (IService) GameMain.registry.lookup("sysService");
                            //更改玩家积分记录
                            server.updateZaCoinsRec(uinfo.getId(),(int)uuu.getScore(),2);
                        } catch (Exception e) {

                        }

                    }
                    DBUtil.executeUpdateBySQL(sql4.replace("$", dd.substring(0, dd.length()-1)), new Object[]{jjc.getInt("memo")});

                    DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{});

                    int i=DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{});
                    System.out.println("更新用户金币："+i);
                }else{

                    JSONArray ar=new JSONArray();
                    for (int i = 0; i < us.size(); i++) {
                        JSONObject uobj=new JSONObject();
                        Player uu = room.getPlayerPaiJu().get(us.getString(i));
                        uobj.element("id", uid.get(i));
                        uobj.element("fen", uu.getScore());
                        uobj.element("total", uu.getTotalScore());
                        ar.add(uobj);
                    }
                    mjBiz.updateUser(ar, "coins");

                    // 金币结算
						/*String sql = "update za_users SET coins = CASE id  $ END WHERE id IN (/)";
						String z="";
						String d="";

						for (String uuuid : room.getPlayerPaiJu().keySet()) {

							Player uuu = room.getPlayerPaiJu().get(uuuid);

							Playerinfo uinfo = room.getPlayerMap().get(uuuid);

							if (uuu.getTotalScore()<=0) {
								z=z+" WHEN "+uinfo.getId()+" THEN 0";
							}else{
								z=z+" WHEN "+uinfo.getId()+" THEN coins+"+uuu.getScore();
							}

							d=d+uinfo.getId()+",";
						}
						int i=DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{});
						System.out.println("更新用户金币："+i);*/
                }
            }else if(room.getRoomType()==3){
                //元宝结算
                JSONArray ar=new JSONArray();
                for (int i = 0; i < us.size(); i++) {
                    JSONObject uobj=new JSONObject();
                    Player uu = room.getPlayerPaiJu().get(us.getString(i));
                    uobj.element("id", uid.get(i));
                    uobj.element("fen", uu.getScore());
                    uobj.element("total", uu.getTotalScore());
                    ar.add(uobj);
                    UserInfoCache.updateUserScore(room.getPlayerMap().get(us.getString(i)).getAccount(), uu.getScore(), 3);
                    UserInfoCache.updateUserScore(room.getPlayerMap().get(us.getString(i)).getAccount(), -room.getFee(), 3);
                }
                mjBiz.updateUser(ar, "yuanbao");

                JSONObject duction=new JSONObject();
                duction.element("user", ar);
                duction.element("gid", 4);
                duction.element("type", 3);
                duction.element("roomNo", roomNo);
                mjBiz.insertUserdeduction(duction);


                // 玩家扣服务费
                if(room.getFee()>0){
                    System.err.println("抽水后台记录");
                    if(room.getRoomType()==1){ // 金币模式

                        mjBiz.dealGoldRoomFee(uid, roomNo, 4, room.getFee(), "2");

                    }else if(room.getRoomType()==3){ // 元宝模式
                        //+mjBiz.dealGoldRoomFee(userIds, roomNo, 4, room.getFee(), "3");
                        //mjBiz.pump(uid, roomNo, 4, room.getFee(), "yuanbao");
                        GameMain.sqlQueue.addSqlTask(new SqlModel(4, uid, roomNo, 4, room.getFee(), "yuanbao"));
                    }
                }

					/*StringBuffer sqlx=new StringBuffer();
					sqlx.append("insert into za_userdeduction(userid,gid,roomNo,type,sum,creataTime) values $");
					String ve="";
					String te=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
					String sql = "update za_users SET yuanbao = CASE id  $ END WHERE id IN (/)";
					String z="";
					String d="";
					for (String uuuid : room.getPlayerPaiJu().keySet()) {
						Player uuu = room.getPlayerPaiJu().get(uuuid);
						Playerinfo uinfo = room.getPlayerMap().get(uuuid);

						ve=ve+"("+uinfo.getId()+","+4+",'"+roomNo+"',"+3+","+uuu.getScore()+",'"+te+"'),";

						if (uuu.getTotalScore()<=0) {
							z=z+" WHEN "+uinfo.getId()+" THEN 0";
						}else{
							z=z+" WHEN "+uinfo.getId()+" THEN yuanbao+"+uuu.getScore();
						}

						d=d+uinfo.getId()+",";
					}
					int i=DBUtil.executeUpdateBySQL(sql.replace("$", z).replace("/", d.substring(0, d.length()-1)), new Object[]{});
					//扣除元宝记录
					DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), new Object[]{});

					*/

                SSSGameRoom roomlog=new SSSGameRoom();
                roomlog=room;
                //SaveLogsThreadSSS sl=new SaveLogsThreadSSS(roomlog, us,uid,false);
                //sl.start();
                //((SSSGameRoom) RoomManage.gameRoomMap.get(roomNo)).setSaveLogsThreadSSS(sl);
                Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();

                for (int j = 0; j < us.size(); j++) {
                    Playerinfo ui = room.getPlayerMap().get(us.getString(j));
                    for (int k = 0; k < us.size(); k++) {
                        Player uu = room.getPlayerPaiJu().get(us.getString(k));
                        Playerinfo uii = room.getPlayerMap().get(us.getString(k));
                        playerMap.put(uii.getAccount(), new JSONObject().element("score", uu.getScore()).element("name", uii.getName()));
                    }

                    GameLogsCache.addGameLogs(ui.getAccount(), 4, new GameLogs(room.getRoomNo(), playerMap, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
                }

                GameMain.sqlQueue.addSqlTask(new SqlModel(SqlModel.SAVELOGS_SSS, roomlog, us, uid, false));

                try {
                    // 扣除房卡，更改房间局数
                    //IService server = (IService) RegisterServer.registry.lookup("sysService");
                    //server.settlementRoomNo(roomNo);
                    //mjBiz.settlementRoomNo(roomNo);
                    String sql2 = "update za_gamerooms set game_index=game_index+1 where room_no=? order by id desc";
                    GameMain.sqlQueue.addSqlTask(new SqlModel(sql2, new Object[]{roomNo}, SqlModel.EXECUTEUPDATEBYSQL));
                } catch (Exception e) {
                    //LogUtil.print("扣除房卡，更改房间局数方法异常："+e.getMessage());
                    e.printStackTrace();
                }
            }else{
                //gamelog(room,us);
					/*SaveLogsThreadSSS sl=new SaveLogsThreadSSS(room, us,uid,false);
					sl.start();
					((SSSGameRoom) RoomManage.gameRoomMap.get(roomNo)).setSaveLogsThreadSSS(sl);
					if (((SSSGameRoom) RoomManage.gameRoomMap.get(roomNo)).getSaveLogsThreadSSS()==null)
					{
						System.out.println("zhanji");
					}*/
                GameMain.sqlQueue.addSqlTask(new SqlModel(SqlModel.SAVELOGS_SSS, room, us, uid, false));
                try {
                    mjBiz.settlementRoomNo(roomNo);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }



        }
    }

    /**
     * 战绩记录
     * @param room
     * @param us
     */
    public void gamelog(SSSGameRoom room, JSONArray us,JSONArray uid,boolean e) {
        JSONArray sum = new JSONArray();

        JSONArray jiesuan = new JSONArray();

        int index = -1;
        int index1 = 0;
        //for (int i = 0; i < us.size(); i++) {}




        JSONArray ugloga = new JSONArray();
        JSONObject uoc=new JSONObject();
        for (int j = 0; j < us.size(); j++) {

            Player u1 = room.getPlayerPaiJu().get(us.getString(j));
            int d = 0;
            int dd = 0;
            for (int ii = 0; ii < us.size(); ii++) {
                // for (UUID ud : room.getPlayerPaiJu().keySet()) {
                Player u2 = room.getPlayerPaiJu().get(
                    us.getString(ii));

                if (!us.getString(j) .equals(us.getString(ii))
                    && u1.getTotalScore() > u2.getTotalScore()) {
                    d++;
                }
                if (!us.getString(j) .equals(us.getString(ii))  && u1.getScore() > u2.getScore()) {
                    dd++;
                }
            }
            if (d == room.getPlayerPaiJu().keySet().size() - 1) {
                index = room.getPlayerIndex(us.getString(j));

            }
            if (dd == room.getPlayerPaiJu().keySet().size() - 1) {
                index1 = room.getPlayerIndex(us.getString(j));

            }


            // for (UUID uid : room.getPlayerPaiJu().keySet()) {
            JSONObject uglog = new JSONObject();
            JSONObject objt = new JSONObject();
            Player u = room.getPlayerPaiJu().get(us.getString(j));
            Playerinfo ui = room.getPlayerMap().get(us.getString(j));

            //战绩存缓存
			/*Map<String, JSONObject> playerMap = new HashMap<String, JSONObject>();

			for (int k = 0; k < us.size(); k++) {
				Player uu = room.getPlayerPaiJu().get(us.getString(k));
				Playerinfo uii = room.getPlayerMap().get(us.getString(k));
				playerMap.put(uii.getAccount(), new JSONObject().element("score", uu.getScore()).element("name", uii.getName()));
			}

			GameLogsCache.addGameLogs(ui.getAccount(), 4, new GameLogs(room.getRoomNo(), playerMap, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));*/




            uoc.element(String.valueOf(ui.getId()), u.getScore());

            objt.element("gun", u.getGun());// 打枪
            objt.element("swat", u.getSwat());// 全垒打
            objt.element("special", u.getSpecial());// 特殊牌
            objt.element("ordinary", u.getOrdinary());// 普通牌
            /*
             * if (room.getRoomType()==1) { obj.element("score",
             * u.getScore());//单局分数 }else{ obj.element("score",
             * u.getTotalScore());//总分 }
             */
            objt.element("score", u.getScore());// 单局分数
            objt.element("totalscore", u.getTotalScore());// 总分
            objt.element("myIndex", room.getPlayerIndex(us.getString(j)));
            objt.element("win", index);// 谁赢
            objt.element("myPai", u.getMyPai());
            objt.element("jiesuan", 2);// 结算类型 1 小结 2 大结
            objt.element("zhuang", room.getPlayerIndex(room.getZhuang()));
			/*objt.element(
					"headimg",
					"http://"
							+ Constant.cfgProperties
									.getProperty("local_remote_ip")+ Constant.DOMAIN + ui.getHeadimg());
			*/objt.element("name", ui.getName());
            objt.element("account", ui.getAccount());

            if (u.getScore() == 0) {
                objt.put("isdan", -1);// 平分秋色
            } else if (u.getScore() > 0) {
                objt.put("isdan", 1);// 赢
            } else {
                objt.put("isdan", 0);// 输
            }

            /*
             * if (index==-1) { obj.put("isWinner", -1);//平分秋色 }else
             */
            if (room.getPlayerIndex(us.getString(j)) == index) {
                uglog.put("isWinner", 1);
            } else {
                uglog.put("isWinner", 0);
            }

            uglog.put("score", u.getScore());
            uglog.put("TotalScore", u.getTotalScore());
            uglog.put("player", room.getPlayerMap().get(us.getString(j))
                .getName());
            uglog.put("zhuang", room.getPlayerIndex(room.getZhuang()));
			/*uglog.put(
					"headimg",
					"http://"
							+ Constant.cfgProperties
									.getProperty("local_remote_ip")+ Constant.DOMAIN + ui.getHeadimg());
			*/uglog.put("name", ui.getName());
            uglog.put("account", ui.getAccount());

            ugloga.add(uglog);

            sum.add(objt);
            System.err.println("总局：" + room.getGameCount() + ",当前："
                + room.getGameIndex());
            if (room.getGameCount() == room.getGameIndex()||e) {
                JSONObject ju = new JSONObject();
                //ju.element("score", u.getScore());// 单局分数
                ju.element("score", u.getTotalScore());// 总分
                ju.element("myPai", u.getMyPai());
				/*ju.element(
						"headimg",
						"http://"
								+ Constant.cfgProperties
										.getProperty("local_remote_ip")+ Constant.DOMAIN + ui.getHeadimg());
				*/ju.element("player", ui.getName());
                ju.element("account", ui.getAccount());
                if (room.getPlayerIndex(us.getString(j)) == index) {
                    ju.element("isWinner", 1);
                } else {
                    ju.element("isWinner", 0);
                }
                if (room.getPlayerIndex(us.getString(j)) == room
                    .getPlayerIndex(room.getZhuang())) {
                    ju.element("isFangzhu", 1);
                } else {
                    ju.element("isFangzhu", 0);
                }
                JSONArray array = new JSONArray();

                JSONObject gm = new JSONObject();
                gm.element("name", "打枪");
                gm.element("val", u.getGun());
                array.add(gm);
                gm.element("name", "全垒打");
                gm.element("val", u.getSwat());
                array.add(gm);
                gm.element("name", "特殊牌");
                gm.element("val", u.getSpecial());
                array.add(gm);
                gm.element("name", "普通牌");
                gm.element("val", u.getOrdinary());
                array.add(gm);

                ju.element("data", array);

                jiesuan.add(ju);
            }
        }

        // 房间信息
        JSONObject roomInfo = mjBiz.getRoomInfoByRno(room.getRoomNo());
        if (roomInfo == null) {
            roomInfo = mjBiz.getRoomInfoByRno1(room.getRoomNo());
        }
        // 查询总战绩
        String sql3 = "select id from za_gamelogs where room_no=?  and game_index=? and gid=? and room_id=?";
        JSONArray arr1 = DBUtil.getObjectListBySQL(sql3, new Object[] { room.getRoomNo(),
            room.getGameIndex(), 4, roomInfo.getLong("id") });
        long gamelog_id = 0;
        if (arr1.size() == 0) {
            // （已完结）保存游戏记录
            String sql = "insert into za_gamelogs(gid,room_id,room_no,game_index,base_info,result,jiesuan,finishtime,status) values(?,?,?,?,?,?,?,?,?)";
            DBUtil.executeUpdateBySQL(
                sql,
                new Object[] {
                    4,
                    roomInfo.getLong("id"),
                    room.getRoomNo(),
                    room.getGameIndex(),
                    roomInfo.getString("base_info"),
                    sum.toString(),
                    jiesuan.toString(),
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        .format(new Date()), 1 });
        }
        // 查询战绩
        JSONObject result = DBUtil
            .getObjectBySQL(
                "select id from za_gamelogs where gid=? and room_id=? and room_no=? and game_index=?",
                new Object[] { 4, roomInfo.getLong("id"), room.getRoomNo(),
                    room.getGameIndex() });
        if (!Dto.isObjNull(result)) {
            gamelog_id = result.getLong("id");
        }

        StringBuffer  sqlx =new StringBuffer();
        Object[] params = new Object[us.size()];
        sqlx.append("insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,gamelog_id,result,fee,account,createtime) values $");
        String ve="";

		/*StringBuffer  sqly =new StringBuffer();
		sqly.append("select id from za_usergamelogs where room_id=? and room_no=? and game_index=? and user_id=$");
		String ve1="";*/
        String te =new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        for (int j = 0; j < us.size(); j++) {
            //Playerinfo ui = room.getPlayerMap().get(us.getString(j));
            ve=ve+"("+4+","+roomInfo.getLong("id")+",'"+room.getRoomNo()+"',"+
                room.getGameIndex()+","+
                uid.getLong(j)+","+
                gamelog_id+","+
                "?"+","+
                room.getFee()+","+
                uoc.getInt(uid.getString(j))+",'"+te +"'),";
            params[j] = ugloga.toString();
        }

        //JSONArray arr = DBUtil.getObjectListBySQL(sql2, new Object[] {roomInfo.getLong("id"), room.getRoomNo(), room.getGameIndex()});


        DBUtil.executeUpdateBySQL(sqlx.toString().replace("$", ve.substring(0, ve.length()-1)), params);

        // 保存玩家战绩
		/*for (Long uid : room.getUserSet()) {
			String sql2 = "select id from za_usergamelogs where room_id=? and room_no=? and user_id=? and game_index=?";
			JSONArray arr = DBUtil.getObjectListBySQL(sql2, new Object[] {
					roomInfo.getLong("id"), room.getRoomNo(), uid, room.getGameIndex()});
			if (arr.size() == 0) {
				String sql1 = "insert into za_usergamelogs(gid,room_id,room_no,game_index,user_id,gamelog_id,result,fee,account,createtime) values(?,?,?,?,?,?,?,?,?,?)";
				DBUtil.executeUpdateBySQL(
						sql1,
						new Object[] {
								4,
								roomInfo.getLong("id"),
								room.getRoomNo(),
								room.getGameIndex(),
								uid,
								gamelog_id,
								ugloga.toString(),
								room.getFee(),
								uoc.getInt(String.valueOf(uid)),
								new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
										.format(new Date()) });
			}
		}*/

    }

}
