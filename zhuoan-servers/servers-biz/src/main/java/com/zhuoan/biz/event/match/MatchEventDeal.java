package com.zhuoan.biz.event.match;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.game.biz.MatchBiz;
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
        // 场次配置
        JSONArray timeMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_TIME);
        // 更新实时场次配置
        JSONArray newTimeMatchSettings = new JSONArray();
        // 实时时间
        String nowTime = TimeUtil.getNowDate();
        for (Object object : timeMatchSettings) {
            JSONObject matchSetting = JSONObject.fromObject(object);
            String difference = TimeUtil.getDaysBetweenTwoTime(matchSetting.getString("create_time"), nowTime, 1000L);
            // 需要自动开赛
            if ("0".equals(difference)) {
                matchSetting.put("create_time", TimeUtil.addSecondBaseOnNowTime(nowTime, matchSetting.getInt("time_interval")));
                matchSetting.put("description", TimeUtil.addSecondBaseOnNowTime(nowTime, matchSetting.getInt("time_interval")) + "开赛");
                JSONObject unFullMatch = matchBiz.getMatchInfoByMatchId(matchSetting.getLong("id"), 0, 0);
                if (!Dto.isObjNull(unFullMatch)) {
                    if (matchSetting.getInt("must_full") != CommonConstant.GLOBAL_YES ||
                        matchSetting.getInt("player_count") <= unFullMatch.getInt("current_count")) {
                        startBeginTimer(matchSetting, unFullMatch.getString("match_num"));
                    }
                }
                matchBiz.updateMatchSettingById(matchSetting.getLong("id"), matchSetting.getString("create_time"));
            }
            matchSetting.put("online_num", matchSetting.getInt("online_num") + RandomUtils.nextInt(10));
            newTimeMatchSettings.add(matchSetting);
        }
        StringBuffer timeKey = new StringBuffer();
        timeKey.append("match_setting_");
        timeKey.append(MatchConstant.MATCH_TYPE_TIME);
        redisService.insertKey(String.valueOf(timeKey), String.valueOf(newTimeMatchSettings), null);
    }

    private void updateCountMatchSettings() {
        // 更新满人开赛场次信息
        JSONArray countMatchSettings = getMatchSettingByType(MatchConstant.MATCH_TYPE_COUNT);
        // 更新场次配置
        JSONArray newCountMatchSettings = new JSONArray();
        for (Object object : countMatchSettings) {
            JSONObject matchSetting = JSONObject.fromObject(object);
            matchSetting.put("online_num", matchSetting.getInt("online_num") + RandomUtils.nextInt(10));
            newCountMatchSettings.add(matchSetting);
        }
        StringBuffer countKey = new StringBuffer();
        countKey.append("match_setting_");
        countKey.append(MatchConstant.MATCH_TYPE_COUNT);
        redisService.insertKey(String.valueOf(countKey), String.valueOf(newCountMatchSettings), null);
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
        JSONArray matchSettings = getMatchSettingByType(type);
        JSONObject result = new JSONObject();
        // 数据是否存在
        if (matchSettings.size() > 0) {
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
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "获取失败");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMatchInfoPush");
    }

    /**
     * 获取场次信息配置，存入缓存
     *
     * @param type
     * @return
     */
    private JSONArray getMatchSettingByType(int type) {
        JSONArray matchSettings;
        try {
            StringBuffer key = new StringBuffer();
            key.append("match_setting_");
            key.append(type);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object != null) {
                matchSettings = JSONArray.fromObject(redisService.queryValueByKey(String.valueOf(key)));
            } else {
                matchSettings = matchBiz.getMatchSettingByType(type);
                redisService.insertKey(String.valueOf(key), String.valueOf(matchSettings), null);
            }
        } catch (Exception e) {
            logger.error("请启动REmote DIctionary Server");
            matchSettings = matchBiz.getMatchSettingByType(type);
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
            boolean isSignUp = redisService.sHasKey("match_sign_up_" + matchSetting.getInt("id"), account);
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
                            if ("coins".equals(type)) {
                                coins = -costFee;
                            }
                            if ("roomcard".equals(type)) {
                                roomCard = -costFee;
                            }
                            matchBiz.updateUserCoinsAndScoreByAccount(account, coins, 0, roomCard);
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
                result.put(CommonConstant.RESULT_KEY_MSG, "已报名该场次");
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
                    if ("coins".equals(playerObj.getString("type"))) {
                        coins = costFee;
                    }
                    if ("roomcard".equals(playerObj.getString("type"))) {
                        roomCard = costFee;
                    }
                    matchBiz.updateUserCoinsAndScoreByAccount(account, coins, 0, roomCard);
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
                    int time = RandomUtils.nextInt(5) + 5;
                    for (int i = 0; i < time; i++) {
                        // 更改人数
                        JSONObject matchInfo = getMatchInfoByNumFromRedis(matchNum);
                        // 场次不存在终止线程
                        if (Dto.isObjNull(matchInfo)) {
                            break;
                        }
                        // 增加人数
                        int addCount = RandomUtils.nextInt(matchInfo.getInt("total_count") - matchInfo.getInt("sign_count"));
                        int signCount = matchInfo.getInt("sign_count") + addCount;
                        // 超出最大人数按最大人数计算
                        if (signCount > matchInfo.getInt("total_count")) {
                            signCount = matchInfo.getInt("total_count");
                        }
                        matchInfo.put("sign_count", signCount);
                        // 更新缓存
                        redisService.insertKey("match_info_" + matchNum, String.valueOf(matchInfo), null);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                // 更改状态
                matchBiz.updateMatchInfoByMatchNum(matchNum, 1);
                // 初始化排行榜
                initRank(matchSetting, matchNum);
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
        if (matchSetting.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
            // 所有玩家
            Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
            int leftNum = matchSetting.getInt("player_count") - allPlayerInfo.size();
            JSONArray robotArray = matchBiz.getRobotList(leftNum);
            for (int i = 0; i < robotArray.size(); i++) {
                initRankList(robotArray.getJSONObject(i).getString("account"), matchNum);
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
        // 晋级人数
        JSONArray promotion = matchInfo.getJSONArray("promotion");
        // 当前轮数
        int curRound = matchInfo.getInt("cur_round");
        // 总轮数
        int totalRound = matchInfo.getInt("total_round");
        // 每桌人数
        int perCount = matchInfo.getInt("per_count");
        // 当前总人数
        int totalNum = promotion.getInt(curRound);
        Map<Object, Object> allPlayerInfo = redisService.hmget("player_info_" + matchNum);
        // 所有玩家
        List<String> allPlayerList = new ArrayList<>();
        for (Object o : allPlayerInfo.keySet()) {
            allPlayerList.add(String.valueOf(o));
        }
        // 机器人人数
        if (matchInfo.getInt("is_auto") == CommonConstant.GLOBAL_YES) {
            int robotNum = getRobotNum(allPlayerInfo.size(), curRound, totalRound, perCount, totalNum);
            for (int i = 0; i < robotNum; i++) {
                allPlayerList.add("0");
            }
        }
        // 打乱排序
        Collections.shuffle(allPlayerList);
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
                            // 清除缓存
                            redisService.setRemove("match_sign_up_" + matchInfo.getInt("match_id"), account);
                        }
                        // 移除缓存
                        redisService.deleteByKey("match_info_" + matchNum);
                        redisService.deleteByKey("robot_info_" + matchNum);
                        redisService.deleteByKey("player_info_" + matchNum);
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
                            // 移除缓存
                            redisService.deleteByKey("match_info_" + matchNum);
                            redisService.deleteByKey("robot_info_" + matchNum);
                        }
                    }
                }
            }
        });
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
        List<Map.Entry<Object, Object>> sortList = getSortedPlayers(matchNum);
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
                    for (Object rewardType : rewardTypes) {
                        JSONObject object = JSONObject.fromObject(rewardType);
                        if (object.getInt("type") == MatchConstant.MATCH_REWARD_TYPE_COINS) {
                            coins = object.getInt("value");
                        }
                        if (object.getInt("type") == MatchConstant.MATCH_REWARD_TYPE_SCORE) {
                            score = object.getInt("value");
                        }
                    }
                    // 更新数据库
                    matchBiz.updateUserCoinsAndScoreByAccount(account, coins, score, 0);
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
        //  出局玩家数
        int outPlayerCount = 0;
        // 根据分数排序
        Set<Map.Entry<Object, Object>> entry = allPlayer.entrySet();
        List<Map.Entry<Object, Object>> sortList = new ArrayList<>(entry);
        Collections.sort(sortList, new Comparator<Map.Entry<Object, Object>>() {
            @Override
            public int compare(Map.Entry<Object, Object> o1, Map.Entry<Object, Object> o2) {
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
                int time = RandomUtils.nextInt(10) + 5;
                for (int i = 0; i < time; i++) {
                    try {
                        // 取出所有本轮未操作的玩家
                        Map<Object, Object> allRobotInfo = redisService.hmget("robot_info_" + matchNum);
                        List<String> robotList = new ArrayList<>();
                        for (Map.Entry<Object, Object> obj : allRobotInfo.entrySet()) {
                            if (JSONObject.fromObject(obj.getValue()).getInt("round") == curRound) {
                                robotList.add(String.valueOf(obj.getKey()));
                            }
                        }
                        if (robotList.size() == 0) {
                            break;
                        }
                        int count = RandomUtils.nextInt(robotList.size() / perCount);
                        if (i == time - 1) {
                            count = robotList.size() / perCount;
                        }
                        for (int j = 0; j < count; j++) {
                            int landlordWin = RandomUtils.nextInt(2);
                            // 生成随机倍数
                            int multiple = RandomUtils.nextInt(5) + 1;
                            // 计算分数
                            int score = 1;
                            for (int k = 0; k < multiple; k++) {
                                score *= 2;
                            }
                            if (landlordWin == 1) {
                                // 玩家游戏详情
                                List<JSONObject> userDetails = new ArrayList<>();
                                userDetails.add(getUserDetail(robotList.get(j * perCount), score * 2, 1));
                                userDetails.add(getUserDetail(robotList.get(j * perCount + 1), -score, 1));
                                userDetails.add(getUserDetail(robotList.get(j * perCount + 2), -score, 1));
                                userFinish(matchNum, userDetails, new ArrayList<String>());
                            } else {
                                List<JSONObject> userDetails = new ArrayList<>();
                                userDetails.add(getUserDetail(robotList.get(j * perCount), -score * 2, 1));
                                userDetails.add(getUserDetail(robotList.get(j * perCount + 1), score, 1));
                                userDetails.add(getUserDetail(robotList.get(j * perCount + 2), score, 1));
                                userFinish(matchNum, userDetails, new ArrayList<String>());
                            }
                        }
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 获取用户详情
     *
     * @param account
     * @param score
     * @param round
     * @return
     */
    private JSONObject getUserDetail(String account, int score, int round) {
        JSONObject userDetail = new JSONObject();
        userDetail.put("account", account);
        userDetail.put("score", score);
        userDetail.put("round", round);
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
            List<String> realPlayerList = getRealPlayer(singleMate);
            // 有真实玩才创建房间
            if (realPlayerList.size() > 0) {
                int realSize = realPlayerList.size();
                // 机器人填满房间
                for (int i = realSize; i <= perCount - realSize; i++) {
                    String robotAccount = getRobotList(robotList);
                    realPlayerList.add(realSize, robotAccount);
                    changeRobotInfo(matchNum, robotAccount, 0, 1);
                }
                // 创建一个房间实体
                String roomNo = matchJoinDdz(matchNum, matchInfo, perCount);
                for (int i = 0; i < realPlayerList.size(); i++) {
                    // 加入房间
                    JSONObject obj = new JSONObject();
                    obj.put("room_no", roomNo);
                    obj.put("account", realPlayerList.get(i));
                    if (i < realSize) {
                        obj.put("my_rank", getUserRank(matchNum, realPlayerList.get(i)));
                        Object o = redisService.hget("player_info_" + matchNum, realPlayerList.get(i));
                        JSONObject playerInfo = JSONObject.fromObject(o);
                        obj.put("uuid", playerInfo.getString("uuid"));
                        SocketIOClient client = GameMain.server.getClient(UUID.fromString(playerInfo.getString("sessionId")));
                        producerService.sendMessage(baseQueueDestination, new Messages(client, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
                    } else {
                        producerService.sendMessage(baseQueueDestination, new Messages(null, obj, CommonConstant.GAME_BASE, CommonConstant.BASE_GAME_EVENT_JOIN_ROOM));
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
        room.setScore(1);
        room.setRobot(true);
        room.setSetting(new JSONObject());
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
            changeRobotInfo(matchNum, userDetail.getString("account"), userDetail.getInt("score"), userDetail.getInt("round"));
            changePlayerInfo(matchNum, null, null, userDetail.getString("account"), userDetail.getInt("score"), userDetail.getInt("round"));
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
    private void changeRobotInfo(String matchNum, String account, int score, int round) {
        Object o = redisService.hget("robot_info_" + matchNum, account);
        if (!Dto.isNull(o)) {
            JSONObject robotInfo = JSONObject.fromObject(o);
            robotInfo.put("score", robotInfo.getInt("score") + score);
            robotInfo.put("round", robotInfo.getInt("round") + round);
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
    public void changePlayerInfo(String matchNum, String sessionId, String uuid, String account, int score, int round) {
        Object o = redisService.hget("player_info_" + matchNum, account);
        if (!Dto.isNull(o)) {
            JSONObject playerInfo = JSONObject.fromObject(o);
            playerInfo.put("score", playerInfo.getInt("score") + score);
            playerInfo.put("round", playerInfo.getInt("round") + round);
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
    private List<String> getRealPlayer(List<String> playerList) {
        List<String> realPlayerList = new ArrayList<>();
        for (String player : playerList) {
            if (!"0".equals(player)) {
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
        cacheInfo.put("total_round", matchSetting.getInt("total_round"));
        cacheInfo.put("per_count", matchSetting.getInt("per_count"));
        cacheInfo.put("reward_detail", matchSetting.getJSONArray("reward_detail"));
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
            matchInfo.put("sign_count", matchInfo.getInt("sign_count") + 1);
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

}
