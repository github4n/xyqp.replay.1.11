package com.zhuoan.biz.event;

import com.zhuoan.biz.game.biz.FundBiz;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.util.MathDelUtil;
import net.sf.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.Map;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 16:59 2018/7/4
 * @Modified By:
 **/
@Component
public class FundEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(FundEventDeal.class);

    @Resource
    private RedisService redisService;

    @Resource
    private Destination baseQueueDestination;

    @Resource
    private ProducerService producerService;

    @Resource
    private FundBiz fundBiz;

    /**
     * 获取并更新用户余额
     * @param chainAdd
     */
    public void getAndUpdateUserMoney(String chainAdd) {
//        double userMoney = getUserMoneyFromZob(chainAdd);
//        if (userMoney>=0) {
//            fundBiz.updateUserScoreByOpenId(chainAdd,userMoney);
//        }
    }

    /**
     * 从zob获取用户实时余额
     * @param chainAdd
     * @return
     */
    private double getUserMoneyFromZob(String chainAdd) {
//        Map<String,String> map=new HashMap<String,String>();
//        map.put("chain_add", chainAdd);
//        map.put("path", "profitmodel");
//        map.put("add", "canusebal");
//        try {
//            String backMsg = HttpReqUtil.doPost("http://127.0.0.1:80/usermodel/game/interface.html", map, null, "utf-8");
//            JSONObject backResult = JSONObject.fromObject(backMsg);
//            if (backResult.getInt("code")==1) {
//                if (backResult.containsKey("data")&&backResult.getJSONObject("data").containsKey("can_use_bal")) {
//                    double userMoney = backResult.getJSONObject("data").getDouble("can_use_bal");
//                    return userMoney;
//                }
//            }
//        } catch (Exception e) {
//            logger.error("",e);
//        }
        return -1;
    }

    /**
     * 改变用户资金状态
     * @param chainAdd
     * @param status
     * @param assetsId
     * @param roomNo
     * @param account
     */
    public void changeUserBalStatus(String chainAdd,int status,long assetsId,String roomNo,String account) {
//        Map<String,String> map=new HashMap<String,String>();
//        map.put("chain_add", chainAdd);
//        map.put("sta", String.valueOf(status));
//        map.put("path", "profitmodel");
//        map.put("add", "updateusermoneysta");
//        if (status==1) {
//            map.put("assets_id", String.valueOf(assetsId));
//        }
//        try {
//            String backMsg = HttpReqUtil.doPost("http://127.0.0.1:80/usermodel/game/interface.html", map, null, "utf-8");
//            System.out.println(backMsg);
//            // TODO: 2018/7/5 code 0处理
//            JSONObject backResult = JSONObject.fromObject(backMsg);
//            // 设置冻结资金id
//            if (status==0&&backResult.getInt("code")==1) {
//                if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
//                    GameRoom room = RoomManage.gameRoomMap.get(roomNo);
//                    if (!Dto.stringIsNULL(account)&&room.getPlayerMap().containsKey(account)&&room.getPlayerMap().get(account)!=null) {
//                        room.getPlayerMap().get(account).setAssetsId(backResult.getJSONObject("data").getLong("assets_id"));
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.error("",e);
//        }
    }

    /**
     * 资金变动记录
     * @param userScoreMap
     * @param des
     */
    public void addBalChangeRecord(Map<String,Double> userScoreMap, String des) {
//        String createTime = TimeUtil.getNowDate();
//        JSONArray array = new JSONArray();
//        for (String chainAdd : userScoreMap.keySet()) {
//            JSONObject param = new JSONObject();
//            // 用户区块地址+金额+时间戳+密钥
//            String sign = MD5.MD5Encode(chainAdd+userScoreMap.get(chainAdd)+createTime+ CommonConstant.SECRET_KEY_ZOB);
//            param.put("chain_add",chainAdd);
//            param.put("money", String.valueOf(userScoreMap.get(chainAdd)));
//            StringBuffer sb = new StringBuffer();
//            sb.append("用户:");
//            sb.append(chainAdd);
//            sb.append(des);
//            sb.append(userScoreMap.get(chainAdd));
//            param.put("des", String.valueOf(sb));
//            JSONObject extra = new JSONObject();
//            extra.put("time",createTime);
//            extra.put("sign",sign);
//            param.put("extra",String.valueOf(extra));
//            array.add(param);
//        }
//        if (array.size()>0) {
//            Map<String,String> params = new HashMap<String,String>();
//            params.put("path","profitmodel");
//            params.put("add","addtreadbalrec");
//            params.put("array",String.valueOf(array));
//            try {
//                String backMsg = HttpReqUtil.doPost("http://127.0.0.1:80/usermodel/game/interface.html", params, null, "utf-8");
//                JSONObject backResult = JSONObject.fromObject(backMsg);
//                if (backResult.getInt("code")==0) {
//                    logger.info("资金变动未成功，数据为:"+String.valueOf(array)+",msg:"+backResult.getString("msg"));
//                }
//            } catch (Exception e) {
//                logger.error("",e);
//            }
//        }
    }

    /**
     * 添加下分订单
     * @param array
     */
    public void addGameOrder(JSONArray array) {
//        if (array.size()!=2) {
//            return;
//        }
//        JSONObject user1 = array.getJSONObject(0);
//        JSONObject user2 = array.getJSONObject(1);
//        String buyChain = null;
//        String saleChain = null;
//        double money = 0;
//        if (user1.getDouble("sum")>0&&user2.getDouble("sum")<0) {
//            money = user1.getDouble("sum");
//            buyChain = user1.getString("openId");
//            saleChain = user2.getString("openId");
//        }
//        if (user2.getDouble("sum")>0&&user1.getDouble("sum")<0) {
//            money = user1.getDouble("sum");
//            buyChain = user2.getString("openId");
//            saleChain = user1.getString("openId");
//        }
//        if (!Dto.stringIsNULL(buyChain)&&!Dto.stringIsNULL(saleChain)&&money>0) {
//            Map<String,String> params = new HashMap<String,String>();
//            params.put("buy_chain",buyChain);
//            params.put("sale_chain",saleChain);
//            params.put("count",String.valueOf(money));
//            params.put("path","cashmodel");
//            params.put("add","addGameOrder");
//            try {
//                String backMsg = HttpReqUtil.doPost("http://127.0.0.1:80/usermodel/game/interface.html", params, null, "utf-8");
//                JSONObject backResult = JSONObject.fromObject(backMsg);
//                if (backResult.getInt("code")==0) {
//                    logger.info("添加下分订单未成功，数据为:"+String.valueOf(params)+",msg:"+backResult.getString("msg"));
//                }
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//        }
    }

    /**
     * 系统用户加入
     * @param roomNo
     */
    public void joinSysUser(String roomNo) {
//        initSysUser();
//        JSONObject sysUser = fundBiz.getSysUsers();
//        if (!Dto.isObjNull(sysUser)) {
//            // 更新余额
//            getAndUpdateUserMoney(sysUser.getString("openid"));
//            JSONObject obj = new JSONObject();
//            obj.put(CommonConstant.DATA_KEY_ACCOUNT,sysUser.getString("account"));
//            obj.put(CommonConstant.DATA_KEY_ROOM_NO,roomNo);
//            obj.put("uuid",sysUser.getString("uuid"));
//            // 加入房间
//            producerService.sendMessage(baseQueueDestination, new Messages(null, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
//            // 更改状态
//            fundBiz.updateSysUserStatusByAccount(sysUser.getString("account"),1);
//            // 添加机器人列表
//            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
//                RoomManage.gameRoomMap.get(roomNo).getRobotList().add(sysUser.getString("account"));
//            }
//        }
    }

    public void sysUserExit(String account) {
//        fundBiz.updateSysUserStatusByAccount(account,0);
//        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
//            if (RoomManage.gameRoomMap.containsKey(roomNo)&&RoomManage.gameRoomMap.get(roomNo)!=null) {
//                GameRoom room = RoomManage.gameRoomMap.get(roomNo);
//                if (room.getGid()==CommonConstant.GAME_ID_BDX&&room.getPlayerMap().size()==1&&room.isFund()) {
//                    joinSysUser(roomNo);
//                    break;
//                }
//            }
//        }
    }


    /**
     * 初始化系统账号
     */
    private void initSysUser() {
//        long summaryTimes = redisService.incr("init_time_fund",1);
//        if (summaryTimes>1) {
//            return;
//        }
//        // 查询平台号
//        JSONObject versionInfo = fundBiz.getVersionInfo();
//        if (!Dto.isObjNull(versionInfo)) {
//            // 是否为资金盘
//            if (CommonConstant.fundPlatformList.contains(versionInfo.getString("platform"))) {
//                Map<String,String> params = new HashMap<String,String>();
//                try {
//                    String backMsg = HttpReqUtil.doPost("http://127.0.0.1:80/usermodel/game/getsysusers.html", params, null, "utf-8");
//                    JSONObject backResult = JSONObject.fromObject(backMsg);
//                    if (backResult.getInt("code")==1) {
//                        // 获取所有的系统用户、如果数据库中没有，则添加到数据库
//                        JSONArray sysUsers = backResult.getJSONArray("data");
//                        if (sysUsers.size()>0) {
//                            JSONObject sysUser = fundBiz.getUserInfoByOpenId(sysUsers.getJSONObject(0).getString("chain_add"));
//                            if (Dto.isObjNull(sysUser)) {
//                                for (Object user : sysUsers) {
//                                    JSONObject userInfo = JSONObject.fromObject(user);
//                                    String account = getUsefulAccount();
//                                    fundBiz.insertUserInfo(account,userInfo,versionInfo.getString("platform"));
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }

    /**
     * 取得不重复的玩家账号
     * @return
     */
    private String getUsefulAccount() {
        String account = MathDelUtil.getRandomStr(8);
//        JSONObject user = fundBiz.getUserInfoByAccount(account);
//        if (!Dto.isObjNull(user)) {
//            return getUsefulAccount();
//        }
        return account;
    }


}
