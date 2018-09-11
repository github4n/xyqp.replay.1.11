package com.zhuoan.biz.event.match;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.biz.MatchBiz;
import com.zhuoan.biz.game.biz.PublicBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.ddz.DdzGameRoom;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.MatchConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.service.jms.ProducerService;
import com.zhuoan.service.socketio.impl.GameMain;
import com.zhuoan.util.Dto;
import com.zhuoan.util.MathDelUtil;
import com.zhuoan.util.TimeUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;
import java.util.*;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 9:06 2018/7/12
 * @Modified By:
 **/
@Component
public class MatchEventDeal {

    private final static Logger logger = LoggerFactory.getLogger(MatchEventDeal.class);

    @Resource
    private RedisService redisService;

    @Resource
    private MatchBiz matchBiz;

    @Resource
    private UserBiz userBiz;

    @Resource
    private PublicBiz publicBiz;

    @Resource
    private Destination baseQueueDestination;

    @Resource
    private ProducerService producerService;

    @Scheduled(cron = "0/10 * * * * ?")
    public void startTimeMatch() {
        // 更新满人开赛配置
        updateCountMatchSettings();
        // 更新实时红包赛配置
        updateTimeMatchSettings();
    }

    private void updateTimeMatchSettings() {
        // 更新实时场次配置
        JSONArray newTimeMatchSettings = new JSONArray();
        // 实时时间
        String nowTime = TimeUtil.getNowDate();
        // 场次配置
        JSONArray timeMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_TIME, nowTime);
        for (Object object : timeMatchSettings) {
            JSONObject matchSetting = JSONObject.fromObject(object);
            String difference = TimeUtil.getDaysBetweenTwoTime(matchSetting.getString("create_time"), nowTime, 1000L);
            // 需要自动开赛
            if ("0".equals(difference)) {
                String nextTime = TimeUtil.addSecondBaseOnNowTime(nowTime, matchSetting.getInt("time_interval"));
                matchSetting.put("create_time", nextTime);
                matchSetting.put("description", nextTime + "开赛");
                JSONArray matchInfoArr = matchSetting.getJSONArray("match_info");
                for (int i = 0; i < matchInfoArr.size(); i++) {
                    if (matchInfoArr.getJSONObject(i).getInt("type") == MatchConstant.MATCH_INFO_TYPE_BEGIN_CONDITION) {
                        matchInfoArr.getJSONObject(i).put("value", nextTime);
                        continue;
                    }
                }
                matchSetting.put("match_info",matchInfoArr);
                JSONObject unFullMatch = matchBiz.getMatchInfoByMatchId(matchSetting.getLong("id"), 0, 0);
                if (!Dto.isObjNull(unFullMatch)) {
                    if (matchSetting.getInt("must_full") != CommonConstant.GLOBAL_YES ||
                        matchSetting.getInt("player_count") <= unFullMatch.getInt("current_count")) {
                        startBeginTimer(matchSetting, unFullMatch.getString("match_num"));
                    }
                }
                matchBiz.updateMatchSettingById(matchSetting);
            }
            int onlineNum = matchSetting.getInt("online_num") + RandomUtils.nextInt(10) - RandomUtils.nextInt(10);
            matchSetting.put("online_num", onlineNum > 0 ? onlineNum : 100);
            newTimeMatchSettings.add(matchSetting);
        }
        StringBuffer timeKey = new StringBuffer();
        timeKey.append("match_setting_");
        timeKey.append(MatchConstant.MATCH_TYPE_TIME);
        redisService.insertKey(String.valueOf(timeKey), String.valueOf(newTimeMatchSettings), null);
    }

    private void updateCountMatchSettings() {
        // 更新满人开赛场次信息
        JSONArray countMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_COUNT, null);
        // 更新场次配置
        JSONArray newCountMatchSettings = new JSONArray();
        for (Object object : countMatchSettings) {
            JSONObject matchSetting = JSONObject.fromObject(object);
            int onlineNum = matchSetting.getInt("online_num") + RandomUtils.nextInt(10) - RandomUtils.nextInt(10);
            matchSetting.put("online_num", onlineNum > 0 ? onlineNum : 100);
            newCountMatchSettings.add(matchSetting);

            int flag = RandomUtils.nextInt(10);
            if (flag % 3 == 0) {
                sendWinnerInfoToAll(String.valueOf(RandomUtils.nextInt(99999999)),1,matchSetting.getJSONArray("reward_detail"),matchSetting.getString("match_name"));
            }
        }
        StringBuffer countKey = new StringBuffer();
        countKey.append("match_setting_");
        countKey.append(MatchConstant.MATCH_TYPE_COUNT);
        redisService.insertKey(String.valueOf(countKey), String.valueOf(newCountMatchSettings), null);
    }

    /**
     * 获取报名信息
     * @param client
     * @param data
     */
    public void getSignUpInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 玩家账号
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 当前所有未开始比赛场信息
        JSONArray unFullMatchInfo = matchBiz.getUnFullMatchInfo();
        JSONObject curMatchInfo = new JSONObject();
        // 该玩家当前所在场次
        for (int i = 0; i < unFullMatchInfo.size(); i++) {
            JSONArray playerArray = unFullMatchInfo.getJSONObject(i).getJSONArray("player_array");
            for (int j = 0; j < playerArray.size(); j++) {
                if (playerArray.getJSONObject(j).getString("account").equals(account)) {
                    curMatchInfo = unFullMatchInfo.getJSONObject(i);
                    break;
                }
            }
        }
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
        if (!Dto.isObjNull(curMatchInfo)) {
            // 场次信息
            JSONObject matchInfo = getMatchInfoByNumFromRedis(curMatchInfo.getString("match_num"));
            if (!Dto.isObjNull(matchInfo)) {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                int type = curMatchInfo.getInt("type");
                result.put("type", type);
                result.put("match_name", matchInfo.getString("match_name"));
                result.put("matchNum", curMatchInfo.getString("match_num"));
                result.put("signCount", matchInfo.getInt("sign_count"));
                // 满人开赛传当前人数 定时赛传总时间及剩余时间
                if (type == MatchConstant.MATCH_TYPE_COUNT) {
                    result.put("totalCount", matchInfo.getInt("total_count"));
                } else if (type == MatchConstant.MATCH_TYPE_TIME) {
                    JSONObject matchSetting = matchBiz.getMatchSettingById(matchInfo.getLong("match_id"), matchInfo.getLong("game_id"));
                    if (!Dto.isObjNull(matchSetting)) {
                        JSONArray matchInfoArr = matchSetting.getJSONArray("match_info");
                        for (int i = 0; i < matchInfoArr.size(); i++) {
                            if (matchInfoArr.getJSONObject(i).getInt("type") == MatchConstant.MATCH_INFO_TYPE_TIME) {
                                result.put("totalTime", matchInfoArr.getJSONObject(i).getString("value"));
                                break;
                            }
                        }
                        String difference = TimeUtil.getDaysBetweenTwoTime(matchSetting.getString("create_time"), TimeUtil.getNowDate(), 1000L);
                        result.put("timeLeft", difference);
                    }
                }
            }
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getSignUpInfoPush");
    }

    /**
     * 获取比赛场信息
     *
     * @param client
     * @param data
     */
    public void obtainMatchInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 玩家账号
        String account = postData.getString("account");
        int type = postData.getInt("type");
        String createTime = null;
        if (type == MatchConstant.MATCH_TYPE_TIME) {
            createTime = TimeUtil.getNowDate();
        }
        JSONArray matchSettings = getMatchSettingByType(type, createTime);
        JSONObject result = new JSONObject();
        // 数据是否存在
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        // 判断是否报名，是否需要倒计时
        for (int i = 0; i < matchSettings.size(); i++) {
            JSONObject matchSetting = matchSettings.getJSONObject(i);
            // 是否报名
            boolean isSignUp = redisService.sHasKey("match_sign_up_" + matchSetting.getInt("id"), account);
            if (isSignUp) {
                matchSetting.put("is_sign", CommonConstant.GLOBAL_YES);
            } else {
                matchSetting.put("is_sign", CommonConstant.GLOBAL_NO);
            }
            // 是否需要倒计时
            if (matchSetting.getInt("type") == MatchConstant.MATCH_TYPE_TIME) {
                String difference = TimeUtil.getDaysBetweenTwoTime(matchSetting.getString("create_time"), TimeUtil.getNowDate(), 1000L);
                matchSetting.put("timeLeft", difference);
            }
            matchSetting.remove("reward_detail");
        }
        result.put("data", matchSettings);

        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMatchInfoPush");
    }

    /**
     * 获取场次信息配置，存入缓存
     *
     * @param type
     * @param createTime
     * @return
     */
    private JSONArray getMatchSettingByType(int type, String createTime) {
        JSONArray matchSettings;
        try {
            StringBuffer key = new StringBuffer();
            key.append("match_setting_");
            key.append(type);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                matchSettings = JSONArray.fromObject(redisService.queryValueByKey(String.valueOf(key)));
            } else {
                matchSettings = matchBiz.getMatchSettingByType(type, createTime);
                redisService.insertKey(String.valueOf(key), String.valueOf(matchSettings), null);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
            matchSettings = matchBiz.getMatchSettingByType(type, createTime);
        }
        return matchSettings;
    }

    /**
     * 报名
     *
     * @param client
     * @param data
     */
    public void matchSignUp(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 玩家账号
        String account = postData.getString("account");
        // 游戏id
        int gameId = postData.getInt("gid");
        // 场次id
        int matchId = postData.getInt("match_id");
        // 消耗类型
        String type = postData.getString("type");
        // 获取场次信息
        JSONObject matchSetting = matchBiz.getMatchSettingById(matchId, gameId);
        JSONObject result = new JSONObject();
        Map<String, JSONObject> beginMap = new HashMap<>();
        if (!Dto.isObjNull(matchSetting)) {
            // 是否已报名该场次
            boolean isSignUp = checkUserSign(account);
            if (!isSignUp) {
                // 消耗类型
                int costFee = -1;
                JSONArray costType = matchSetting.getJSONArray("cost_type");
                for (Object costObj : costType) {
                    if (type.equals(JSONObject.fromObject(costObj).getString("type"))) {
                        costFee = JSONObject.fromObject(costObj).getInt("value");
                        break;
                    }
                }
                if (costFee > 0) {
                    JSONObject userInfo = userBiz.getUserByAccount(account);
                    if (!Dto.isObjNull(userInfo) && userInfo.containsKey(type) && userInfo.getInt(type) > costFee) {
                        // 所有未满场次
                        JSONObject unFullMatch = matchBiz.getMatchInfoByMatchId(matchId, 0, 0);
                        // 没有未满场次创建，有未满场次加入
                        if (client != null) {
                            String matchNum;
                            if (Dto.isObjNull(unFullMatch)) {
                                // 场次编号
                                matchNum = randomMatchNum();
                                // 创建场次
                                createMatch(userInfo.getString("uuid"), client, account, matchId, matchSetting, matchNum, type);
                                // 开始倒计时
                                if (matchSetting.getInt("type") == MatchConstant.MATCH_TYPE_COUNT &&
                                    matchSetting.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
                                    startBeginTimer(matchSetting, matchNum);
                                }
                            } else {
                                // 加入
                                joinMatch(userInfo.getString("uuid"), client, account, matchSetting, unFullMatch, type);
                                // 场次编号
                                matchNum = unFullMatch.getString("match_num");
                                // 最后一人报名开始游戏
                                if (matchSetting.getInt("type") == MatchConstant.MATCH_TYPE_COUNT && !Dto.isObjNull(unFullMatch) &&
                                    unFullMatch.getInt("current_count") + 1 >= matchSetting.getInt("player_count")) {
                                    beginMap.put(matchNum, matchSetting);
                                }
                            }
                            // 存入缓存
                            redisService.sSet("match_sign_up_" + matchSetting.getInt("id"), account);
                            // 扣除金币
                            int coins = 0;
                            int roomCard = 0;
                            int score = 0;
                            double yb = 0;
                            if ("coins".equals(type)) {
                                coins = -costFee;
                            }
                            if ("roomcard".equals(type)) {
                                roomCard = -costFee;
                            }
                            if ("score".equals(type)) {
                                score = -costFee;
                            }
                            if ("yuanbao".equals(type)) {
                                yb = -costFee;
                            }
                            matchBiz.updateUserCoinsAndScoreByAccount(account, coins, score, roomCard, yb);
                            JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
                            // 通知前端
                            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                            result.put(CommonConstant.RESULT_KEY_MSG, "报名成功");
                            result.put("matchNum", matchNum);
                            result.put("signCount", matchInfo.getInt("sign_count"));
                            result.put("totalCount", matchSetting.getInt("player_count"));
                            result.put("type", matchSetting.getInt("type"));
                            result.put("match_id", matchSetting.getLong("id"));
                            result.put("match_name", matchSetting.getString("match_name"));
                            if (matchSetting.getInt("type") == MatchConstant.MATCH_TYPE_TIME) {
                                JSONArray matchInfoArr = matchSetting.getJSONArray("match_info");
                                for (int i = 0; i < matchInfoArr.size(); i++) {
                                    if (matchInfoArr.getJSONObject(i).getInt("type") == MatchConstant.MATCH_INFO_TYPE_TIME) {
                                        result.put("totalTime", matchInfoArr.getJSONObject(i).getString("value"));
                                        break;
                                    }
                                }
                                String difference = TimeUtil.getDaysBetweenTwoTime(matchSetting.getString("create_time"), TimeUtil.getNowDate(), 1000L);
                                result.put("timeLeft", difference);
                            }
                        }
                    } else {
                        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                        result.put(CommonConstant.RESULT_KEY_MSG, "余额不足");
                    }
                } else {
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                    result.put(CommonConstant.RESULT_KEY_MSG, "支付类型错误");
                }
            } else {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "已报名比赛场");
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "场次信息不正确");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "matchSignUpPush");
        // 开始游戏
        for (String matchNum : beginMap.keySet()) {
            initRank(beginMap.get(matchNum), matchNum);
        }
    }

    private boolean checkUserSign(String account) {
        JSONArray timeMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_TIME, TimeUtil.getNowDate());
        JSONArray countMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_COUNT, null);
        JSONArray matchSettings = new JSONArray();
        matchSettings.addAll(timeMatchSettings);
        matchSettings.addAll(countMatchSettings);
        for (int i = 0; i < matchSettings.size(); i++) {
            long matchId = matchSettings.getJSONObject(i).getLong("id");
            if (redisService.sHasKey("match_sign_up_" + matchId, account)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 取消报名
     *
     * @param client
     * @param data
     */
    public void matchCancelSign(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 玩家账号
        String account = postData.getString("account");
        // 游戏id
        int gameId = postData.getInt("gid");
        // 场次id
        int matchId = postData.getInt("match_id");
        JSONObject result = new JSONObject();
        JSONObject matchSetting = matchBiz.getMatchSettingById(matchId, gameId);
        if (!Dto.isObjNull(matchSetting)) {
            // 是否已报名该场次
            boolean isSignUp = redisService.sHasKey("match_sign_up_" + matchId, account);
            if (isSignUp) {
                // 所有未满场次
                JSONObject unFullMatch = matchBiz.getMatchInfoByMatchId(matchId, 0, 0);
                if (!Dto.isObjNull(unFullMatch)) {
                    // 场次编号
                    String matchNum = unFullMatch.getString("match_num");
                    JSONObject obj = new JSONObject();
                    // 清除缓存数据
                    playerOutMatch(matchNum, account);
                    Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
                    if (allPlayerInfo == null || allPlayerInfo.size() == 0) {
                        redisService.deleteByKey("match_info_" + matchNum);
                        obj.put("is_full", 1);
                    } else {
                        // 更改缓存
                        JSONObject matchInfo = getMatchInfoByNumFromRedis(unFullMatch.getString("match_num"));
                        if (!Dto.isObjNull(matchInfo)) {
                            matchInfo.put("sign_count", matchInfo.getInt("sign_count") - 1);
                            addMatchInfoIntoRedis(unFullMatch.getString("match_num"), matchInfo);
                        }
                    }
                    // 玩家信息
                    JSONArray playerArray = unFullMatch.getJSONArray("player_array");
                    List<String> playerList = new ArrayList<>();
                    JSONObject playerObj = new JSONObject();
                    for (Object player : playerArray) {
                        if (!account.equals(JSONObject.fromObject(player).getString("account"))) {
                            playerList.add(String.valueOf(player));
                        } else {
                            playerObj = JSONObject.fromObject(player);
                        }
                    }
                    obj.put("id", unFullMatch.getLong("id"));
                    obj.put("player_array", String.valueOf(playerList));
                    obj.put("current_count", unFullMatch.getInt("current_count") - 1);
                    // 更新数据库
                    matchBiz.addOrUpdateMatchInfo(obj);
                    // 返还金币
                    int costFee = 0;
                    JSONArray costType = matchSetting.getJSONArray("cost_type");
                    for (Object costObj : costType) {
                        if (playerObj.getString("type").equals(JSONObject.fromObject(costObj).getString("type"))) {
                            costFee = JSONObject.fromObject(costObj).getInt("value");
                            break;
                        }
                    }
                    int coins = 0;
                    int roomCard = 0;
                    int score = 0;
                    double yb = 0;
                    if ("coins".equals(playerObj.getString("type"))) {
                        coins = costFee;
                    }
                    if ("roomcard".equals(playerObj.getString("type"))) {
                        roomCard = costFee;
                    }
                    if ("score".equals(playerObj.getString("type"))) {
                        score = costFee;
                    }
                    if ("yuanbao".equals(playerObj.getString("type"))) {
                        yb = costFee;
                    }
                    matchBiz.updateUserCoinsAndScoreByAccount(account, coins, score, roomCard, yb);
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
                    result.put(CommonConstant.RESULT_KEY_MSG, "退赛成功");
                    result.put("type", matchSetting.getInt("type"));
                } else {
                    result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                    result.put(CommonConstant.RESULT_KEY_MSG, "当前无法退赛");
                }
            } else {
                result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
                result.put(CommonConstant.RESULT_KEY_MSG, "当前未报名该场次");
            }
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "场次信息不正确");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "matchCancelSignPush");
    }

    /**
     * 获取战绩
     *
     * @param client
     * @param data
     */
    public void getWinningRecord(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        int gameId = postData.getInt("gid");
        String account = postData.getString("account");
        JSONObject winningRecord = matchBiz.getUserWinningRecord(account, gameId);
        JSONObject result = new JSONObject();
        if (!Dto.isObjNull(winningRecord)) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            result.put("gid", gameId);
            result.put("data", winningRecord.getJSONArray("winning_record"));
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "无战绩记录");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getWinningRecordPush");

    }


    /**
     * 更新实时报名人数
     *
     * @param client
     * @param data
     */
    public void updateMatchCount(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("matchNum")) {
            String matchNum = postData.getString("matchNum");
            JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
            if (!Dto.isObjNull(matchInfo)) {
                JSONObject result = new JSONObject();
                result.put("signCount", matchInfo.getInt("sign_count"));
                result.put("totalCount", matchInfo.getInt("total_count"));
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "updateMatchCountPush");
            }
        }
    }

    /**
     * 开始倒计时
     *
     * @param matchSetting
     * @param matchNum
     */
    private void startBeginTimer(final JSONObject matchSetting, final String matchNum) {
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (matchSetting.getInt("type") == MatchConstant.MATCH_TYPE_COUNT) {
                    // 满人开始的需要改变人数
                    int time = RandomUtils.nextInt(5) + 30;
                    for (int i = 0; i < time; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        // 更改人数
                        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
                        // 场次不存在终止线程
                        if (Dto.isObjNull(matchInfo)) {
                            return;
                        }
                        // 增加人数
                        int addCount = RandomUtils.nextInt(matchInfo.getInt("total_count") - matchInfo.getInt("sign_count"));
                        int signCount = matchInfo.getInt("sign_count") + addCount;
                        // 超出最大人数按最大人数计算
                        if (signCount >= matchInfo.getInt("total_count")) {
                            signCount = matchInfo.getInt("total_count") - 1;
                        }
                        matchInfo.put("sign_count", signCount);
                        // 更新缓存
                        redisService.insertKey("match_info_" + matchNum, String.valueOf(matchInfo), null);
                    }
                }
                JSONObject unFullMatch = matchBiz.getMatchInfoByMatchId(matchSetting.getLong("id"), 0, 0);
                // 当前场次未开始进行开始游戏(防止最后一个玩家报名与线程冲突开始多次)
                if (!Dto.isObjNull(unFullMatch) && unFullMatch.getString("match_num").equals(matchNum)) {
                    // 玩家信息
                    JSONArray playerArray = unFullMatch.getJSONArray("player_array");
                    // 消耗类型
                    int costFee = -1;
                    JSONArray costType = matchSetting.getJSONArray("cost_type");
                    for (Object costObj : costType) {
                        if ("roomcard".equals(JSONObject.fromObject(costObj).getString("type"))) {
                            costFee = JSONObject.fromObject(costObj).getInt("value");
                            break;
                        }
                    }
                    // 添加记录
                    for (Object player : playerArray) {
                        if ("roomcard".equals(JSONObject.fromObject(player).getString("type"))) {
                            publicBiz.addUserWelfareRec(JSONObject.fromObject(player).getString("account"), -costFee,
                                CommonConstant.CURRENCY_TYPE_ROOM_CARD - 1, matchSetting.getInt("game_id"));
                        } else if ("yuanbao".equals(JSONObject.fromObject(player).getString("type"))) {
                            publicBiz.addUserWelfareRec(JSONObject.fromObject(player).getString("account"), -costFee,
                                CommonConstant.CURRENCY_TYPE_YB- 1, matchSetting.getInt("game_id"));
                        } else if ("score".equals(JSONObject.fromObject(player).getString("type"))) {
                            publicBiz.addUserWelfareRec(JSONObject.fromObject(player).getString("account"), -costFee,
                                CommonConstant.CURRENCY_TYPE_SCORE - 1, matchSetting.getInt("game_id"));
                        }
                    }
                    // 初始化排行榜
                    initRank(matchSetting, matchNum);
                    // 更改状态
                    matchBiz.updateMatchInfoByMatchNum(matchNum, 1);
                }
            }
        });
    }

    /**
     * 初始化排行榜
     *
     * @param matchSetting
     * @param matchNum
     */
    private void initRank(JSONObject matchSetting, String matchNum) {
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        if (matchSetting.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
            // 所有玩家
            Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
            // 已经在房间内的玩家退赛
            Set<Object> players = allPlayerInfo.keySet();
            for (Object player : players) {
                if (allPlayerInfo.containsKey(player)) {
                    for (String roomNo : RoomManage.gameRoomMap.keySet()) {
                        if (RoomManage.gameRoomMap.get(roomNo).getPlayerMap().containsKey(player)) {
                            JSONObject data = new JSONObject();
                            data.put("account", String.valueOf(player));
                            data.put("gid", matchInfo.getLong("game_id"));
                            data.put("match_id", matchInfo.getLong("match_id"));
                            matchCancelSign(null, data);
                        }
                    }
                }
            }
            allPlayerInfo = redisService.hmget("player_info_" + matchNum);
            matchInfo = getMatchInfoByNumFromRedis(matchNum);
            if (allPlayerInfo.size() == 0 || Dto.isObjNull(matchInfo)) {
                return;
            }
            int totalCount = matchSetting.getInt("player_count");
            if (matchSetting.getJSONArray("promotion").getInt(0) > totalCount) {
                totalCount = matchSetting.getJSONArray("promotion").getInt(0);
            }
            int leftNum = totalCount - allPlayerInfo.size();
            JSONArray robotArray = matchBiz.getRobotList(leftNum);
            if (robotArray.size() < leftNum) {
                return;
            }
            for (int i = 0; i < robotArray.size(); i++) {
                initRankList(robotArray.getJSONObject(i).getString("account"), matchNum);
                matchBiz.updateRobotStatus(robotArray.getJSONObject(i).getString("account"), 1);
            }
        }
        // 开始匹配
        startMatch(matchNum);
    }

    /**
     * 比赛场开始
     *
     * @param matchNum
     */
    private void startMatch(String matchNum) {
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        // 当前轮数
        int curRound = matchInfo.getInt("cur_round");
        // 每桌人数
        int perCount = matchInfo.getInt("per_count");
        // 所有玩家
        List<String> allPlayerList = new ArrayList<>();
        List<Map.Entry<Object, Object>> sortedPlayers = getSortedPlayers(matchNum);
        for (int i = 0; i < sortedPlayers.size(); i++) {
            allPlayerList.add(String.valueOf(sortedPlayers.get(i).getKey()));
        }
        // 匹配结果
        List<List<String>> mateResult = new ArrayList<>();
        for (int i = 0; i < allPlayerList.size(); i = i + perCount) {
            List<String> singleResult = new ArrayList<>();
            singleResult.addAll(allPlayerList.subList(i, i + perCount));
            mateResult.add(singleResult);
        }
        // 根据匹配结果加入房间
        matchJoinRoom(matchNum, matchInfo, perCount, mateResult);
        // 开始改变玩家分数
        if (matchInfo.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
            startChangeUserScore(matchNum, curRound, perCount);
        }
    }

    /**
     * 完成一轮
     *
     * @param matchNum
     */
    public void allFinishDeal(final String matchNum) {
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
                if (!Dto.isObjNull(matchInfo)) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // 清除上一轮数据
                    clearLastRoundRoom(matchNum);
                    // 增加游戏轮数
                    matchInfo.put("cur_round", matchInfo.getInt("cur_round") + 1);
                    redisService.insertKey("match_info_" + matchNum, String.valueOf(matchInfo), null);
                    // 晋级人数
                    JSONArray promotion = matchInfo.getJSONArray("promotion");
                    // 当前轮数
                    int curRound = matchInfo.getInt("cur_round");
                    if (curRound >= promotion.size() - 1) {
                        Map<String, UUID> allPlayerUUID = getAllPlayerUUID(matchNum);
                        for (String account : allPlayerUUID.keySet()) {
                            // 当前排名 淘汰下标+未淘汰人数+1
                            int rank = getUserRank(matchNum, account);
                            // 更新玩家奖励,返回奖励详情
                            String rewardInfo = updateUserReward(matchNum, rank, account);
                            JSONObject result = new JSONObject();
                            result.put("type", MatchConstant.MATCH_PROMOTION_TYPE_FINISH);
                            result.put("myRank", rank);
                            result.put("rewardInfo", rewardInfo);
                            CommonConstant.sendMsgEventToSingle(allPlayerUUID.get(account), String.valueOf(result), "matchSummaryResultPush");
                            // 发送冠军通知
                            if (rank == 1) {
                                sendWinnerInfoToAll(matchNum, account, rank);
                            }
                            // 清除缓存
                            redisService.setRemove("match_sign_up_" + matchInfo.getInt("match_id"), account);
                        }
                        // 移除缓存
                        redisService.deleteByKey("match_info_" + matchNum);
                        redisService.deleteByKey("robot_info_" + matchNum);
                        redisService.deleteByKey("player_info_" + matchNum);
                        redisService.deleteByKey("double_info_" + matchNum);
                        // 更新机器人状态
                        updateAllRobotStatus(matchNum, 0);
                    } else {
                        // 当前总人数
                        int totalNum = promotion.getInt(curRound);
                        // 淘汰玩家
                        boolean isContinue = userPromotion(matchNum, totalNum);
                        if (isContinue) {
                            // 通知晋级玩家
                            sendPromotionToUser(matchNum, new ArrayList<String>(), curRound - 1, promotion, promotion.getInt(curRound - 1), 1);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Map<String, UUID> allPlayerUUID = getAllPlayerUUID(matchNum);
                            for (String account : allPlayerUUID.keySet()) {
                                JSONObject result = new JSONObject();
                                result.put("type", MatchConstant.MATCH_PROMOTION_TYPE_CONTINUE);
                                result.put("myRank", getUserRank(matchNum, account));
                                CommonConstant.sendMsgEventToSingle(allPlayerUUID.get(account), String.valueOf(result), "matchSummaryResultPush");
                            }
                            // 继续下一轮
                            startMatch(matchNum);
                        } else {
                            // 取出所有本轮未操作的玩家
                            Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
                            for (Object robot : allRobotInfo.keySet()) {
                                // 发送冠军通知
                                sendWinnerInfoToAll(matchNum, String.valueOf(robot), 1);
                                break;
                            }
                            // 更新机器人状态
                            updateAllRobotStatus(matchNum, 0);
                            // 移除缓存
                            redisService.deleteByKey("match_info_" + matchNum);
                            redisService.deleteByKey("robot_info_" + matchNum);
                            redisService.deleteByKey("double_info_" + matchNum);
                        }
                    }
                }
            }
        });
    }

    /**
     * 赢家奖励通知
     *
     * @param matchNum
     * @param account
     * @param rank
     */
    private void sendWinnerInfoToAll(String matchNum, String account, int rank) {
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        if (!Dto.isObjNull(matchInfo)) {
            JSONArray rewardDetails = matchInfo.getJSONArray("reward_detail");
            String matchName = matchInfo.getString("match_name");
            sendWinnerInfoToAll(account, rank, rewardDetails, matchName);

        }
    }

    private void sendWinnerInfoToAll(String account, int rank, JSONArray rewardDetails, String matchName) {
        JSONObject rewardInfo = new JSONObject();
        // 获取第一名奖励
        for (Object rewardDetail : rewardDetails) {
            JSONObject obj = JSONObject.fromObject(rewardDetail);
            JSONArray rankArray = obj.getJSONArray("name");
            // 排名落在指定区间
            if (rank > rankArray.getInt(0) && rank <= rankArray.getInt(1)) {
                // 奖励详情
                rewardInfo = obj.getJSONObject("value");
            }
        }
        if (!Dto.isObjNull(rewardInfo)) {
            JSONObject obj = new JSONObject();
            obj.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            obj.put("type", CommonConstant.NOTICE_TYPE_ALL);
            obj.put("content", "恭喜" + account + "在" + matchName + "中获得第" + rank + "名,奖励" + rewardInfo.getString("name"));
            if (GameMain.server != null) {
                for (SocketIOClient client : GameMain.server.getAllClients()) {
                    CommonConstant.sendMsgEventToSingle(client, String.valueOf(obj), "getMessagePush");
                }
            }
        }
    }

    /**
     * 更改机器人状态
     *
     * @param matchNum
     * @param status
     */
    private void updateAllRobotStatus(String matchNum, int status) {
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        for (Object robot : allRobotInfo.keySet()) {
            matchBiz.updateRobotStatus(String.valueOf(robot), status);
        }
    }

    /**
     * 清空上轮房间
     *
     * @param matchNum
     */
    private void clearLastRoundRoom(String matchNum) {
        Set<String> roomSet = RoomManage.gameRoomMap.keySet();
        for (String roomNo : roomSet) {
            if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
                GameRoom room = RoomManage.gameRoomMap.get(roomNo);
                // matchNum是否匹配
                if (room.getRoomType() == CommonConstant.ROOM_TYPE_MATCH && matchNum.equals(room.getMatchNum())) {
                    // 移除所有机器人
                    for (String account : room.getRobotList()) {
                        redisService.setRemove("game_in_robot_list", account);
                    }
                    redisService.deleteByKey("summaryTimes_ddz_" + roomNo);
                    redisService.deleteByKey("startTimes_ddz_" + roomNo);
                    RoomManage.gameRoomMap.remove(roomNo);
                }
            }
        }
    }

    /**
     * 玩家晋级
     *
     * @param matchNum
     * @param totalNum
     * @return
     */
    private boolean userPromotion(String matchNum, int totalNum) {
        // 获取排序之后的玩家account集合
        List<Map.Entry<Object, Object>> sortList = getSortedPlayersByNum(matchNum);
        //  出局玩家数
        int outPlayerCount = 0;
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        // 所有淘汰玩家
        List<Map.Entry<Object, Object>> outList = sortList.subList(totalNum, sortList.size());
        for (int i = 0; i < outList.size(); i++) {
            if (allRobotInfo.containsKey(outList.get(i).getKey())) {
                matchBiz.updateRobotStatus(String.valueOf(outList.get(i).getKey()), 0);
                redisService.hdel("robot_info_" + matchNum, String.valueOf(outList.get(i).getKey()));
            } else if (allPlayerInfo.containsKey(outList.get(i).getKey())) {
                // 当前排名 淘汰下标+未淘汰人数+1
                int rank = i + totalNum + 1;
                // 更新玩家奖励,返回奖励详情
                String rewardInfo = updateUserReward(matchNum, rank, String.valueOf(outList.get(i).getKey()));
                // 获取client对象
                JSONObject o = JSONObject.fromObject(allPlayerInfo.get(outList.get(i).getKey()));
                SocketIOClient client = GameMain.server.getClient(UUID.fromString(o.getString("sessionId")));
                // 通知玩家
                JSONObject result = new JSONObject();
                result.put("type", MatchConstant.MATCH_PROMOTION_TYPE_FINISH);
                result.put("myRank", rank);
                result.put("rewardInfo", rewardInfo);
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "matchSummaryResultPush");
                outPlayerCount++;
                // 清除玩家
                playerOutMatch(matchNum, String.valueOf(outList.get(i).getKey()));
            }
        }
        // 比赛场积分规则调整  wqm  20180903
        Map<Object, Object> newAllPlayerInfo = redisService.hmget("player_info_" + matchNum);
        if (newAllPlayerInfo != null && newAllPlayerInfo.size() > 0) {
            for (Object o : newAllPlayerInfo.keySet()) {
                JSONObject playerObj = JSONObject.fromObject(newAllPlayerInfo.get(o));
                playerObj.put("score", (int) (playerObj.getInt("score") * 0.1) + 1000);
                redisService.hset("player_info_" + matchNum, String.valueOf(o), String.valueOf(playerObj));
            }
            Map<Object, Object> newAllRobotInfo = redisService.hmget("robot_info_" + matchNum);
            if (newAllRobotInfo != null && newAllRobotInfo.size() > 0) {
                for (Object o : newAllRobotInfo.keySet()) {
                    JSONObject robotObj = JSONObject.fromObject(newAllRobotInfo.get(o));
                    robotObj.put("score", (int) (robotObj.getInt("score") * 0.1) + 1000);
                    redisService.hset("robot_info_" + matchNum, String.valueOf(o), String.valueOf(robotObj));
                }
            }
        }
        return outPlayerCount < allPlayerInfo.size();
    }

    /**
     * 更新玩家奖励
     *
     * @param matchNum
     * @param rank
     * @param account
     * @return
     */
    private String updateUserReward(String matchNum, int rank, String account) {
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        if (!Dto.isObjNull(matchInfo)) {
            JSONArray rewardDetails = matchInfo.getJSONArray("reward_detail");
            for (Object rewardDetail : rewardDetails) {
                JSONObject obj = JSONObject.fromObject(rewardDetail);
                JSONArray rankArray = obj.getJSONArray("name");
                // 排名落在指定区间
                if (rank > rankArray.getInt(0) && rank <= rankArray.getInt(1)) {
                    // 奖励详情
                    JSONObject rewardInfo = obj.getJSONObject("value");
                    JSONArray rewardTypes = rewardInfo.getJSONArray("value");
                    // 奖励的金币
                    int coins = 0;
                    // 奖励的积分
                    int score = 0;
                    int roomCard = 0;
                    double yb = 0;
                    for (Object rewardType : rewardTypes) {
                        JSONObject object = JSONObject.fromObject(rewardType);
                        if (object.getInt("type") == MatchConstant.MATCH_REWARD_TYPE_COINS) {
                            coins = object.getInt("value");
                        }else if (object.getInt("type") == MatchConstant.MATCH_REWARD_TYPE_SCORE) {
                            score = object.getInt("value");
                        }
                    }
                    // 更新数据库
                    matchBiz.updateUserCoinsAndScoreByAccount(account, coins, score, roomCard, yb);
                    // 添加获奖记录
                    JSONObject winningRecord = matchBiz.getUserWinningRecord(account, matchInfo.getInt("game_id"));
                    JSONObject object = new JSONObject();
                    if (Dto.isObjNull(winningRecord)) {
                        object.put("user_account", account);
                        object.put("game_id", matchInfo.getInt("game_id"));
                        List<JSONObject> records = new ArrayList<>();
                        JSONObject record = getUserWinningRecord(rank, matchInfo, rewardInfo.getString("name"));
                        records.add(record);
                        object.put("winning_record", String.valueOf(records));
                        object.put("win_coins", coins);
                        object.put("win_score", score);
                    } else {
                        object.put("id", winningRecord.getLong("id"));
                        List<JSONObject> records = winningRecord.getJSONArray("winning_record");
                        JSONObject record = getUserWinningRecord(rank, matchInfo, rewardInfo.getString("name"));
                        records.add(0, record);
                        if (records.size() > MatchConstant.MATCH_WINNING_RECORD_SIZE) {
                            records = records.subList(0, MatchConstant.MATCH_WINNING_RECORD_SIZE);
                        }
                        object.put("winning_record", String.valueOf(records));
                        object.put("win_coins", winningRecord.getLong("win_coins") + coins);
                        object.put("win_score", winningRecord.getLong("win_score") + score);
                    }
                    matchBiz.addOrUpdateUserWinningRecord(object);
                    if (score > 0) {
                        // 添加记录
                        publicBiz.addUserWelfareRec(account, score, CommonConstant.TICKET_TYPE_THING, matchInfo.getInt("game_id"));
                    }

                    return rewardInfo.getString("name");
                }
            }
        }
        return "本次参赛未获奖,请继续努力";
    }

    /**
     * 用户获奖记录
     *
     * @param rank
     * @param matchInfo
     * @param reward
     * @return
     */
    private JSONObject getUserWinningRecord(int rank, JSONObject matchInfo, String reward) {
        JSONObject record = new JSONObject();
        record.put("name", matchInfo.getString("match_name"));
        record.put("myRank", rank);
        record.put("totalCount", matchInfo.getInt("total_count"));
        record.put("createTime", TimeUtil.getNowDate());
        record.put("reward", reward);
        return record;
    }
    /**
     * 玩家晋级排名(每桌第一名一定晋级)
     *
     * @param matchNum
     * @return
     */
    private List<Map.Entry<Object, Object>> getSortedPlayersByNum(String matchNum) {
        Map<Object, Object> allPlayer = new HashMap<>();
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        allPlayer.putAll(allRobotInfo);
        allPlayer.putAll(allPlayerInfo);
        // 根据分数排序
        Set<Map.Entry<Object, Object>> entry = allPlayer.entrySet();
        List<Map.Entry<Object, Object>> sortList = new ArrayList<>(entry);
        Collections.sort(sortList, new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
                if (JSONObject.fromObject(o2.getValue()).getInt("win") == JSONObject.fromObject(o1.getValue()).getInt("win")) {
                    if (JSONObject.fromObject(o2.getValue()).getInt("score") == JSONObject.fromObject(o1.getValue()).getInt("score")) {
                        return JSONObject.fromObject(o1.getValue()).getInt("card") - JSONObject.fromObject(o2.getValue()).getInt("card");
                    }
                    return JSONObject.fromObject(o2.getValue()).getInt("score") - JSONObject.fromObject(o1.getValue()).getInt("score");
                }
                return JSONObject.fromObject(o2.getValue()).getInt("win") - JSONObject.fromObject(o1.getValue()).getInt("win");
            }
        });
        return sortList;
    }


    /**
     * 根据玩家积分排序
     *
     * @param matchNum
     * @return
     */
    private List<Map.Entry<Object, Object>> getSortedPlayers(String matchNum) {
        Map<Object, Object> allPlayer = new HashMap<>();
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        allPlayer.putAll(allRobotInfo);
        allPlayer.putAll(allPlayerInfo);
        // 根据分数排序
        Set<Map.Entry<Object, Object>> entry = allPlayer.entrySet();
        List<Map.Entry<Object, Object>> sortList = new ArrayList<>(entry);
        Collections.sort(sortList, new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
                if (JSONObject.fromObject(o2.getValue()).getInt("score") == JSONObject.fromObject(o1.getValue()).getInt("score")) {
                    return JSONObject.fromObject(o1.getValue()).getInt("card") - JSONObject.fromObject(o2.getValue()).getInt("card");
                }
                return JSONObject.fromObject(o2.getValue()).getInt("score") - JSONObject.fromObject(o1.getValue()).getInt("score");
            }
        });
        return sortList;
    }

    /**
     * 玩家退出
     *
     * @param matchNum
     * @param account
     */
    private void playerOutMatch(String matchNum, String account) {
        // 移除玩家信息
        redisService.hdel("player_info_" + matchNum, account);
        // 移除报名信息
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        redisService.setRemove("match_sign_up_" + matchInfo.getInt("match_id"), account);
    }


    /**
     * 改变分数
     *
     * @param matchNum
     * @param curRound
     * @param perCount
     */
    private void startChangeUserScore(final String matchNum, final int curRound, final int perCount) {
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                List<String> curRoundRobotList = getCurRoundRobotList(matchNum, curRound);
                while (curRoundRobotList.size() > 0) {
                    // 桌数
                    int tableNum = curRoundRobotList.size() / perCount;
                    if (tableNum > 0) {
                        // 本次完成桌数
                        int changeTableNum = RandomUtils.nextInt(tableNum) + 1;
                        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
                        for (int j = 0; j < changeTableNum; j++) {
                            int landlordWin = RandomUtils.nextInt(2);
                            // 最大倍数
                            int maxMultiple = matchInfo.getInt("robot_level");
                            if (maxMultiple < 1) {
                                maxMultiple = 1;
                            }
                            // 生成随机倍数
                            int multiple = RandomUtils.nextInt(maxMultiple) + 1;
                            // 计算分数
                            int score = 30;
                            try {
                                Object object = redisService.queryValueByKey(String.valueOf("match_room_score"));
                                if (object != null) {
                                    score = Integer.parseInt(String.valueOf(object));
                                }
                            } catch (Exception e) {}
                            for (int k = 0; k < multiple; k++) {
                                score *= 2;
                            }
                            if (landlordWin == 1) {
                                // 玩家游戏详情
                                List<JSONObject> userDetails = new ArrayList<>();
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount), score * 2, curRound + 1,0));
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount + 1), -score, curRound + 1, RandomUtils.nextInt(17) + 1));
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount + 2), -score, curRound + 1, RandomUtils.nextInt(17) + 1));
                                userFinish(matchNum, userDetails, new ArrayList<String>());
                            } else {
                                List<JSONObject> userDetails = new ArrayList<>();
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount), -score * 2, curRound + 1, RandomUtils.nextInt(17) + 1));
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount + 1), score, curRound + 1, RandomUtils.nextInt(17) + 1));
                                userDetails.add(getUserDetail(curRoundRobotList.get(j * perCount + 2), score, curRound + 1, 0));
                                userFinish(matchNum, userDetails, new ArrayList<String>());
                            }
                        }
                        curRoundRobotList = getCurRoundRobotList(matchNum, curRound);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (curRoundRobotList.size() > 0) {
                        /**
                         *  如果有玩家轮数异常，强制刷新
                         *  wqm  2018/09/07
                         */
                        List<JSONObject> userDetails = new ArrayList<>();
                        for (String account : curRoundRobotList) {
                            userDetails.add(getUserDetail(account, 0, curRound + 1, RandomUtils.nextInt(17) + 1));
                        }
                        userFinish(matchNum, userDetails, new ArrayList<String>());
                        curRoundRobotList = getCurRoundRobotList(matchNum, curRound);
                    }
                }
            }
        });
    }

    private List<String> getCurRoundRobotList(String matchNum, int curRound) {
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        List<String> robotList = new ArrayList<>();
        for (Map.Entry<Object, Object> obj : allRobotInfo.entrySet()) {
            if (JSONObject.fromObject(obj.getValue()).getInt("round") != curRound + 1) {
                robotList.add(String.valueOf(obj.getKey()));
            }
        }
        return robotList;
    }

    /**
     * 获取用户详情
     *
     * @param account
     * @param score
     * @param round
     * @return
     */
    private JSONObject getUserDetail(String account, int score, int round, int card) {
        JSONObject userDetail = new JSONObject();
        userDetail.put("account", account);
        userDetail.put("score", score);
        userDetail.put("round", round);
        userDetail.put("card", card);
        userDetail.put("win", 1);
        return userDetail;
    }

    /**
     * 比赛场加入房间
     *
     * @param matchNum
     * @param matchInfo
     * @param perCount
     * @param mateResult
     */
    private void matchJoinRoom(String matchNum, JSONObject matchInfo, int perCount, List<List<String>> mateResult) {
        // 当前场次所有机器人
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        List<Object> robotList = new ArrayList<>(allRobotInfo.keySet());
        Collections.shuffle(robotList);
        // 遍历配桌结果
        for (List<String> singleMate : mateResult) {
            // 取出所有真实玩家
            List<String> realPlayerList = getRealPlayer(singleMate, robotList);
            // 有真实玩才创建房间
            if (realPlayerList.size() > 0) {
                // 创建一个房间实体
                String roomNo = matchJoinDdz(matchNum, matchInfo, perCount);
                for (int i = 0; i < singleMate.size(); i++) {
                    // 加入房间
                    JSONObject obj = new JSONObject();
                    obj.put("room_no", roomNo);
                    obj.put("account", singleMate.get(i));
                    if (!robotList.contains(singleMate.get(i))) {
                        obj.put("my_rank", getUserRank(matchNum, singleMate.get(i)));
                        Object o = redisService.hget("player_info_" + matchNum, singleMate.get(i));
                        JSONObject playerInfo = JSONObject.fromObject(o);
                        obj.put("uuid", playerInfo.getString("uuid"));
                        SocketIOClient client = GameMain.server.getClient(UUID.fromString(playerInfo.getString("sessionId")));
                        producerService.sendMessage(baseQueueDestination, new Messages(client, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
                    } else {
                        changeRobotInfo(matchNum, singleMate.get(i), 0, matchInfo.getInt("cur_round") + 1, 17,0);
                        producerService.sendMessage(baseQueueDestination, new Messages(null, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 获取第一个空闲的机器人
     *
     * @param robotList
     * @return
     */
    private String getRobotList(List<Object> robotList) {
        if (robotList.size() == 0) {
            return null;
        }
        String account = String.valueOf(robotList.get(0));
        // 是否已经在游戏中
        boolean isGameIn = redisService.sHasKey("game_in_robot_list", account);
        if (!isGameIn) {
            // 存入缓存
            redisService.sSet("game_in_robot_list", account);
            return account;
        }
        // 移除第一个
        robotList.remove(0);
        return getRobotList(robotList);
    }

    /**
     * 创建斗地主房间
     *
     * @param matchNum
     * @param matchInfo
     * @param perCount
     * @return
     */
    private String matchJoinDdz(String matchNum, JSONObject matchInfo, int perCount) {
        String roomNo = randomRoomNo();
        DdzGameRoom room = new DdzGameRoom();
        room.setMatchNum(matchNum);
        room.setGid(matchInfo.getInt("game_id"));
        room.setRoomType(CommonConstant.ROOM_TYPE_MATCH);
        room.setRoomNo(roomNo);
        room.setPlayerCount(perCount);
        room.setGameCount(1);
        room.setGameIndex(0);
        int score = 30;
        try {
            Object object = redisService.queryValueByKey(String.valueOf("match_room_score"));
            if (object != null) {
                score = Integer.parseInt(String.valueOf(object));
            }
        } catch (Exception e) {}
        room.setScore(score);
        room.setRobot(true);
        JSONObject setting = new JSONObject();
        // 托管直接过
        setting.put("trustee_pass", CommonConstant.GLOBAL_YES);
        // 托管包赔
        setting.put("trustee_lose", CommonConstant.GLOBAL_YES);
        // 无人叫地主重新发牌
        setting.put("re_shuffle", CommonConstant.GLOBAL_YES);
        // 允许加倍
        setting.put("is_double", CommonConstant.GLOBAL_YES);
        // 成就奖励
        setting.put("isAchievement", CommonConstant.GLOBAL_YES);
        room.setSetting(setting);
        List<Long> idList = new ArrayList<Long>();
        for (int j = 0; j < perCount; j++) {
            idList.add(0L);
        }
        room.setUserIdList(idList);
        // 当前轮数
        int curRound = matchInfo.getInt("cur_round");
        // 晋级人数
        JSONArray promotion = matchInfo.getJSONArray("promotion");
        // 当前总人数
        int totalNum = promotion.getInt(curRound);
        // 定时赛第一轮打两局
        if (curRound == 0 && matchInfo.getInt("type") == MatchConstant.MATCH_TYPE_TIME) {
            room.setGameCount(3);
        }
        // 冠亚季军打两局
        if (totalNum == promotion.getInt(promotion.size() - 2)) {
            room.setGameCount(3);
        }
        room.setTotalNum(totalNum);
        RoomManage.gameRoomMap.put(roomNo, room);
        return roomNo;
    }

    /**
     * 积分变更
     *
     * @param matchNum
     * @param userDetails
     */
    public void userFinish(String matchNum, List<JSONObject> userDetails, List<String> realPlayers) {
        // 更改分数
        for (JSONObject userDetail : userDetails) {
            changeRobotInfo(matchNum, userDetail.getString("account"), userDetail.getInt("score"), userDetail.getInt("round"),
                userDetail.getInt("card"), userDetail.getInt("win"));
            changePlayerInfo(matchNum, null, null, userDetail.getString("account"), userDetail.getInt("score"),
                userDetail.getInt("round"), userDetail.getInt("card"), userDetail.getInt("win"));
        }
        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
        if (!Dto.isObjNull(matchInfo)) {
            // 当前轮数
            int curRound = matchInfo.getInt("cur_round");
            // 晋级人数
            JSONArray promotion = matchInfo.getJSONArray("promotion");
            // 当前总人数
            int totalNum = promotion.getInt(curRound);
            // 通知玩家晋级结果
            sendPromotionToUser(matchNum, realPlayers, curRound, promotion, totalNum, 0);
            // 本轮全部完成
            if (checkIsAllFinish(matchNum, curRound)) {
                allFinishDeal(matchNum);
            }
        }
    }

    /**
     * 晋级结果通知
     *
     * @param matchNum
     * @param realPlayers
     * @param curRound
     * @param promotion
     * @param totalNum
     * @param isPromotion
     */
    private void sendPromotionToUser(String matchNum, List<String> realPlayers, int curRound, JSONArray promotion, int totalNum, int isPromotion) {
        // 通知玩家
        Map<String, UUID> allPlayerUUID = getAllPlayerUUID(matchNum);
        for (String account : allPlayerUUID.keySet()) {
            JSONObject result = new JSONObject();
            result.put("type", 0);
            if (realPlayers.contains(account)) {
                result.put("type", 1);
            }
            result.put("myRank", getUserRank(matchNum, account));
            result.put("totalPlayer", totalNum);
            result.put("rankArray", promotion);
            result.put("rankIndex", curRound);
            result.put("isPromotion", isPromotion);
            CommonConstant.sendMsgEventToSingle(allPlayerUUID.get(account), String.valueOf(result), "matchWaittingResultPush");
        }
    }

    /**
     * 获取玩家排名
     *
     * @param matchNum
     * @param account
     * @return
     */
    public int getUserRank(String matchNum, String account) {
        List<Map.Entry<Object, Object>> sortList = getSortedPlayers(matchNum);
        for (int i = 0; i < sortList.size(); i++) {
            if (String.valueOf(sortList.get(i).getKey()).equals(account)) {
                return i + 1;
            }
        }
        return -1;
    }

    private Map<String, UUID> getAllPlayerUUID(String matchNum) {
        // 取出所有玩家
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        Map<String, UUID> allPlayerUUID = new HashMap<>();
        for (Object player : allPlayerInfo.keySet()) {
            JSONObject playerInfo = JSONObject.fromObject(allPlayerInfo.get(player));
            allPlayerUUID.put(String.valueOf(player), UUID.fromString(playerInfo.getString("sessionId")));
        }
        return allPlayerUUID;
    }

    /**
     * 是否全部完成
     *
     * @param matchNum
     * @param curRound
     * @return
     */
    private boolean checkIsAllFinish(String matchNum, int curRound) {
        Map<Object, Object> allPlayer = new HashMap<>();
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
        // 取出所有本轮未操作的玩家
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        allPlayer.putAll(allRobotInfo);
        allPlayer.putAll(allPlayerInfo);
        for (Object o : allPlayer.keySet()) {
            JSONObject playerInfo = JSONObject.fromObject(allPlayer.get(o));
            if (playerInfo.containsKey("round") && playerInfo.getInt("round") == curRound) {
                return false;
            }
        }
        return true;
    }

    /**
     * 更新机器人信息
     *
     * @param matchNum
     * @param account
     * @param score
     * @param round
     */
    public void changeRobotInfo(String matchNum, String account, int score, int round, int card, int win) {
        Object o = redisService.hget("robot_info_" + matchNum, account);
        if (!Dto.isNull(o)) {
            JSONObject robotInfo = JSONObject.fromObject(o);
            robotInfo.put("score", robotInfo.getInt("score") + score);
            if (round != 0) {
                robotInfo.put("round", round);
            }
            robotInfo.put("card", card);
            robotInfo.put("win", win);
            redisService.hset("robot_info_" + matchNum, account, String.valueOf(robotInfo));
        }
    }

    /**
     * 更新玩家信息
     *
     * @param matchNum
     * @param account
     * @param score
     * @param round
     */
    public void changePlayerInfo(String matchNum, String sessionId, String uuid, String account, int score, int round, int card, int win) {
        Object o = redisService.hget("player_info_" + matchNum, account);
        if (!Dto.isNull(o)) {
            JSONObject playerInfo = JSONObject.fromObject(o);
            playerInfo.put("score", playerInfo.getInt("score") + score);
            playerInfo.put("round", playerInfo.getInt("round") + round);
            playerInfo.put("card", card);
            if (win != 0) {
                playerInfo.put("win", win);
            }
            if (!Dto.stringIsNULL(sessionId)) {
                playerInfo.put("sessionId", sessionId);
            }
            if (!Dto.stringIsNULL(uuid)) {
                playerInfo.put("uuid", uuid);
            }
            redisService.hset("player_info_" + matchNum, account, String.valueOf(playerInfo));
        }
    }

    /**
     * 取所有玩家
     *
     * @param playerList
     * @return
     */
    private List<String> getRealPlayer(List<String> playerList, List<Object> robotList) {
        List<String> realPlayerList = new ArrayList<>();
        for (String player : playerList) {
            if (!robotList.contains(player)) {
                realPlayerList.add(player);
            }
        }
        return realPlayerList;
    }

    /**
     * 获取当前需要的机器人人数
     *
     * @param playNum
     * @param curRound
     * @param totalRound
     * @param perCount
     * @param totalNum
     * @return
     */
    private int getRobotNum(int playNum, int curRound, int totalRound, int perCount, int totalNum) {
        // 按照轮次每轮增加对应的机器人
        int maxNum = (perCount * (totalRound - curRound) - 1) * playNum;
        // 超除总人数返回总人数-实际人数
        return maxNum + playNum > totalNum ? (totalNum - playNum) : maxNum;
    }


    /**
     * 创建比赛场
     *
     * @param client
     * @param account
     * @param matchId
     * @param matchSetting
     * @param matchNum
     * @param type
     */
    private void createMatch(String uuid, SocketIOClient client, String account, int matchId, JSONObject matchSetting, String matchNum, String type) {
        // 添加缓存
        JSONObject cacheInfo = new JSONObject();
        cacheInfo.put("match_id", matchId);
        cacheInfo.put("match_name", matchSetting.getString("match_name"));
        cacheInfo.put("game_id", matchSetting.getInt("game_id"));
        if (matchSetting.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
            cacheInfo.put("sign_count", RandomUtils.nextInt(matchSetting.getInt("player_count") / matchSetting.getInt("per_count")));
        } else {
            cacheInfo.put("sign_count", 1);
        }
        cacheInfo.put("total_count", matchSetting.getInt("player_count"));
        cacheInfo.put("promotion", matchSetting.getJSONArray("promotion"));
        cacheInfo.put("cur_round", 0);
        cacheInfo.put("is_auto", matchSetting.getInt("is_auto"));
        cacheInfo.put("robot_level", matchSetting.getInt("robot_level"));
        cacheInfo.put("total_round", matchSetting.getInt("total_round"));
        cacheInfo.put("per_count", matchSetting.getInt("per_count"));
        cacheInfo.put("reward_detail", matchSetting.getJSONArray("reward_detail"));
        cacheInfo.put("type", matchSetting.getInt("type"));
        cacheInfo.put("free_double_time", matchSetting.getInt("free_double_time"));
        cacheInfo.put("pay_double_time", matchSetting.getInt("pay_double_time"));
        addMatchInfoIntoRedis(matchNum, cacheInfo);
        // 添加玩家信息
        addPlayerInfo(account, matchNum, uuid, String.valueOf(client.getSessionId()), 1000, 0);
        // 添加数据库信息
        JSONObject obj = new JSONObject();
        obj.put("match_num", matchNum);
        obj.put("match_id", matchId);
        obj.put("type", matchSetting.getInt("type"));
        obj.put("create_time", TimeUtil.getNowDate());
        List<JSONObject> playerList = new ArrayList<>();
        JSONObject playerObj = getSignPlayerObj(account, type);
        playerList.add(playerObj);
        obj.put("player_array", String.valueOf(playerList));
        obj.put("total_round", matchSetting.getInt("total_round"));
        matchBiz.addOrUpdateMatchInfo(obj);
    }

    /**
     * 获取报名用户信息
     *
     * @param account
     * @param type
     * @return
     */
    private JSONObject getSignPlayerObj(String account, String type) {
        JSONObject playerObj = new JSONObject();
        playerObj.put("account", account);
        playerObj.put("type", type);
        return playerObj;
    }

    /**
     * 添加玩家信息
     *
     * @param account
     * @param matchNum
     * @param uuid
     * @param sessionId
     * @param score
     * @param round
     */
    private void addPlayerInfo(String account, String matchNum, String uuid, String sessionId, int score, int round) {
        JSONObject playerInfo = new JSONObject();
        playerInfo.put("uuid", uuid);
        playerInfo.put("sessionId", sessionId);
        playerInfo.put("score", score);
        playerInfo.put("round", round);
        playerInfo.put("card", 17);
        playerInfo.put("win", 1);
        redisService.hset("player_info_" + matchNum, account, String.valueOf(playerInfo));
    }

    /**
     * 加入比赛场
     *
     * @param uuid
     * @param client
     * @param account
     * @param matchSetting
     * @param unFullMatch
     * @param type
     */
    private void joinMatch(String uuid, SocketIOClient client, String account, JSONObject matchSetting, JSONObject unFullMatch, String type) {
        // 添加数据库
        List<JSONObject> playerList = new ArrayList<>();
        playerList.addAll(unFullMatch.getJSONArray("player_array"));
        JSONObject playerObj = getSignPlayerObj(account, type);
        playerList.add(playerObj);
        int isFull = 0;
        if (unFullMatch.getInt("type") == MatchConstant.MATCH_TYPE_COUNT && unFullMatch.getInt("current_count") + 1 >= matchSetting.getInt("player_count")) {
            isFull = 1;
        }
        JSONObject obj = new JSONObject();
        obj.put("id", unFullMatch.getLong("id"));
        obj.put("player_array", String.valueOf(playerList));
        obj.put("current_count", unFullMatch.getInt("current_count") + 1);
        obj.put("is_full", isFull);
        matchBiz.addOrUpdateMatchInfo(obj);
        // 更改缓存
        JSONObject matchInfo = getMatchInfoByNumFromRedis(unFullMatch.getString("match_num"));
        if (!Dto.isObjNull(matchInfo)) {
            // 添加玩家信息
            addPlayerInfo(account, unFullMatch.getString("match_num"), uuid, String.valueOf(client.getSessionId()), 1000, 0);
            int signCount = matchInfo.getInt("sign_count") + 1;
            // 超出最大人数按最大人数计算
            if (signCount >= matchInfo.getInt("total_count")) {
                signCount = matchInfo.getInt("total_count") - 1;
            }
            matchInfo.put("sign_count", signCount);
            addMatchInfoIntoRedis(unFullMatch.getString("match_num"), matchInfo);
        }
    }

    /**
     * 初始化排行榜
     *
     * @param account
     * @param matchNum
     */
    private void initRankList(String account, String matchNum) {
        JSONObject robotInfo = new JSONObject();
        robotInfo.put("score", 1000);
        robotInfo.put("round", 0);
        robotInfo.put("card", 17);
        robotInfo.put("win", 1);
        redisService.hset("robot_info_" + matchNum, account, String.valueOf(robotInfo));
    }

    /**
     * 添加缓存
     *
     * @param matchNum
     * @param matchInfo
     */
    private void addMatchInfoIntoRedis(String matchNum, JSONObject matchInfo) {
        String key = "match_info_" + matchNum;
        try {
            redisService.insertKey(key, String.valueOf(matchInfo), null);
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
        }
    }

    /**
     * 获取缓存
     *
     * @param matchNum
     * @return
     */
    private JSONObject getMatchInfoByNumFromRedis(String matchNum) {
        String key = "match_info_" + matchNum;
        try {
            Object object = redisService.queryValueByKey(key);
            if (object != null) {
                return JSONObject.fromObject(object);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
        }
        return null;
    }

    /**
     * 生成不重复场次编号
     *
     * @return matchNum
     */
    private String randomMatchNum() {
        String matchNum = MathDelUtil.getRandomStr(8);
        if (!Dto.isObjNull(getMatchInfoByNumFromRedis(matchNum))) {
            return randomMatchNum();
        }
        return matchNum;
    }

    /**
     * 生成随机房间号
     *
     * @return
     */
    private String randomRoomNo() {
        String roomNo = MathDelUtil.getRandomStr(6);
        if (RoomManage.gameRoomMap.containsKey(roomNo)) {
            return randomRoomNo();
        }
        return roomNo;
    }

    /**
     * 刷新用户排名
     *
     * @param roomNo
     */
    public void refreshUserRank(String roomNo, String account) {
        // 房间是否存在
        if (RoomManage.gameRoomMap.containsKey(roomNo) && RoomManage.gameRoomMap.get(roomNo) != null) {
            if (RoomManage.gameRoomMap.get(roomNo).getPlayerMap().containsKey(account)
                && RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account) != null) {
                // 获取玩家排名
                int userRank = getUserRank(RoomManage.gameRoomMap.get(roomNo).getMatchNum(), account);
                // 更新玩家排名
                if (userRank != -1) {
                    RoomManage.gameRoomMap.get(roomNo).getPlayerMap().get(account).setMyRank(userRank);
                }
            }
        }
    }
}
