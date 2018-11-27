package com.zhuoan.biz.event.club;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.biz.RoomBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.ClubConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.Constant;
import com.zhuoan.service.cache.RedisService;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;

/**
 * ClubEventDeal
 *
 * @author wqm
 * @Date Created in 12:00 2018/8/22
 **/
@Component
public class ClubEventDeal {

    @Resource
    private ClubBiz clubBiz;

    @Resource
    private UserBiz userBiz;

    @Resource
    private RedisService redisService;

    @Resource
    private RoomBiz roomBiz;

    @Resource
    private BaseEventDeal baseEventDeal;

    private String clubName = "亲友团";

    /**
     * 获取玩家俱乐部列表
     * @param client
     * @param data
     */
    public void getMyClubList(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
            return;
        }
        JSONObject result = new JSONObject();
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        JSONObject userClub = clubBiz.getUserClubByAccount(account);
        if (!Dto.isObjNull(userClub) && userClub.containsKey("clubIds") && !Dto.stringIsNULL(userClub.getString("clubIds"))) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            List<JSONObject> clubList = getUserClubListInfo(userClub);
            result.put("clubList", clubList);
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "未加入" + clubName);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMyClubListPush");
    }

    /**
     * 获取玩家俱乐部详情
     * @param userClub
     * @return
     */
    private List<JSONObject> getUserClubListInfo(JSONObject userClub) {
        List<JSONObject> clubList = new ArrayList<>();
        // 当前所有已加入的俱乐部
        String[] clubIds = userClub.getString("clubIds").substring(1, userClub.getString("clubIds").length() - 1).split("\\$");
        // 当前已经入该俱乐部
        for (int i = 0; i < clubIds.length; i++) {
            JSONObject clubInfo = clubBiz.getClubById(Long.valueOf(clubIds[i]));
            if (!Dto.isObjNull(clubInfo)) {
                JSONObject leaderInfo = userBiz.getUserByID(clubInfo.getLong("leaderId"));
                JSONObject obj = new JSONObject();
                obj.put("clubId", clubInfo.getLong("id"));
                obj.put("clubCode", clubInfo.getString("clubCode"));
                obj.put("clubName", clubInfo.getString("clubName"));
                obj.put("imgUrl", Constant.cfgProperties.getProperty("server_domain") + leaderInfo.getString("headimg"));
                obj.put("isTop",userClub.containsKey("top_club") && Long.valueOf(clubIds[i]) == userClub.getLong("top_club") ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
                clubList.add(obj);
            }
        }
        // 排序
        Collections.sort(clubList, new Comparator<JSONObject>() {
            @Override
            public int compare(JSONObject o1, JSONObject o2) {
                return o2.getInt("isTop") - o1.getInt("isTop");
            }
        });
        return clubList;
    }

    /**
     * 获取俱乐部成员
     * @param client
     * @param data
     */
    public void getClubMembers(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(ClubConstant.DATA_KEY_CLUB_CODE)) {
            return;
        }
        JSONObject result = new JSONObject();
        // 俱乐部编号
        String clubCode = postData.getString(ClubConstant.DATA_KEY_CLUB_CODE);
        // 俱乐部信息
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        // 成员列表
        List<JSONObject> members = new ArrayList<>();
        if (!Dto.isObjNull(clubInfo)) {
            JSONArray memberArray = clubBiz.getClubMember(clubInfo.getLong("id"));
            for (int i = 0; i < memberArray.size(); i++) {
                JSONObject memberObj = memberArray.getJSONObject(i);
                JSONObject member = new JSONObject();
                member.put("account", memberObj.getString("account"));
                member.put("name", memberObj.getString("name"));
                member.put("img", Constant.cfgProperties.getProperty("server_domain") + memberObj.getString("headimg"));
                // 是否是会长
                member.put("isLeader", clubInfo.getLong("leaderId") == memberObj.getLong("id") ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
                members.add(member);
            }
        }
        result.put("members", members);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubMembersPush");
    }

    /**
     * 获取俱乐部设置
     * @param client
     * @param data
     */
    public void getClubSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(ClubConstant.DATA_KEY_CLUB_CODE) || !postData.containsKey(CommonConstant.DATA_KEY_ACCOUNT)) {
            return;
        }
        JSONObject result = new JSONObject();
        // 俱乐部编号
        String clubCode = postData.getString(ClubConstant.DATA_KEY_CLUB_CODE);
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        // 俱乐部信息
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (!Dto.isObjNull(clubInfo)) {
            JSONObject leaderInfo = userBiz.getUserByID(clubInfo.getLong("leaderId"));
            if (!Dto.isObjNull(leaderInfo)) {
                result.put("leader_img", leaderInfo.getString("headimg"));
                result.put("leader_name", leaderInfo.getString("name"));
                result.put("clubName", clubInfo.getString("clubName"));
                result.put("clubCode", clubInfo.getString("clubCode"));
                JSONArray memberArray = clubBiz.getClubMember(clubInfo.getLong("id"));
                result.put("memberCount", memberArray == null ? 0 : memberArray.size());
                result.put("notice", clubInfo.containsKey("notice") ? clubInfo.getString("notice") : "");
                String setting = "";
                if (clubInfo.containsKey("setting")) {
                    JSONObject clubSetting = JSONObject.fromObject(clubInfo.getString("setting"));
                    for (Object obj : clubSetting.keySet()) {
                        String gameName = getGameNameById(Integer.parseInt(String.valueOf(obj)));
                        if (!Dto.stringIsNULL(gameName)) {
                            setting += gameName;
                            setting += " ： ";
                            setting += clubSetting.get(obj);
                            setting += "\n";
                        }
                    }
                }
                result.put("setting", setting);
                result.put("isLeader", leaderInfo.getString("account").equals(account) ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
                CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubSettingPush");
            }
        }
    }

    /**
     * 更改俱乐部设置
     * @param client
     * @param data
     */
    public void changeClubSetting(SocketIOClient client, Object data) {
        /**
         * 1、是否会长本人发起修改
         * 2、验证数据格式是否正确
         * 3、修改数据库
         */
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.containsKey("clubCode") ? postData.getString("clubCode") : "";
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String notice = postData.containsKey("notice") ? postData.getString("notice") : "";
        String setting = postData.containsKey("setting") ? postData.getString("setting") : "";
        String quickSetting = postData.containsKey("quick_setting") ? postData.getString("quick_setting") : "";
        int gameId = postData.containsKey("gid") ? postData.getInt("gid") : 0;
        String eventName = "changeClubSettingPush";
        JSONObject leaderInfo = clubBiz.getUserByAccountAndUuid(leader, uuid);
        // 验证用户信息是否合法
        if (Dto.isObjNull(leaderInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "账号已在其他地方登录", eventName);
            return;
        }
        // 俱乐部不存在或非会长发起
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo) || clubInfo.getLong("leaderId") != leaderInfo.getLong("id")) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "无修改权限", eventName);
            return;
        }
        // 更新数据库
        JSONObject newClubInfo = new JSONObject();
        newClubInfo.put("id", clubInfo.getLong("id"));
        if (!Dto.stringIsNULL(notice)) {
            newClubInfo.put("notice", notice);
        }
        if (!Dto.stringIsNULL(setting) && !Dto.stringIsNULL(quickSetting)) {
            newClubInfo.put("setting", clubInfo.getJSONObject("setting").element(String.valueOf(gameId),setting));
            newClubInfo.put("quick_setting", clubInfo.getJSONObject("quick_setting").element(String.valueOf(gameId),quickSetting));
        }
        clubBiz.updateClubInfo(newClubInfo);
        // 通知玩家
        sendPromptToSingle(client, CommonConstant.GLOBAL_YES, "修改成功", eventName);
    }

    /**
     * 玩家退出俱乐部
     * 1、是否本人发起修改
     * 2、俱乐部信息是否正确
     * 3、修改数据库
     *
     * @param client
     * @param data
     */
    public void exitClub(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 俱乐部编号
        String clubCode = postData.getString("clubCode");
        // 玩家账号
        String account = postData.getString("account");
        // 玩家uuid
        String uuid = postData.getString("uuid");
        // 通知事件名称
        String eventName = "exitClubPush";
        // 用户信息
        JSONObject userInfo = clubBiz.getUserByAccountAndUuid(account, uuid);
        // 验证用户信息是否合法
        if (Dto.isObjNull(userInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "账号已在其他地方登录", eventName);
            return;
        }
        // 是加入俱乐部
        if (!userInfo.containsKey("clubIds") || Dto.stringIsNULL(userInfo.getString("clubIds"))) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入" + clubName, eventName);
            return;
        }
        // 俱乐部是否存在
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, clubName + "不存在", eventName);
            return;
        }
        // 当前用户所在的所有俱乐部
        List<String> clubIdList = getUserClubList(userInfo);
        // 当前要退出的俱乐部id
        String clubId = clubInfo.getString("id");
        // 未加入当前俱乐部提示，已加入更新数据库
        if (!clubIdList.contains(clubId)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入该" + clubName, eventName);
            return;
        } else {
            // 移除当前俱乐部
            clubIdList.remove(clubId);
            // 如果未加入其它俱乐部重置为空，否则只移除当前俱乐部
            StringBuffer newClubIds = new StringBuffer();
            if (clubIdList.size() > 0) {
                // 拼接成 $X$X$X$ 格式
                newClubIds.append("$");
                for (int i = 0; i < clubIdList.size(); i++) {
                    newClubIds.append(clubIdList.get(i));
                    newClubIds.append("$");
                }
            }
            // 更新俱乐部
            clubBiz.updateUserClubIds(userInfo.getLong("id"), String.valueOf(newClubIds));
            // 通知成功
            sendPromptToSingle(client, CommonConstant.GLOBAL_YES, "退出成功", eventName);
        }
    }

    public void toTop(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 俱乐部编号
        String clubCode = postData.getString("clubCode");
        // 玩家账号
        String account = postData.getString("account");
        // 玩家uuid
        String uuid = postData.getString("uuid");
        // 是否置顶
        int top = postData.getInt("top");
        // 通知事件名称
        String eventName = "toTopPush";
        // 用户信息
        JSONObject userInfo = clubBiz.getUserByAccountAndUuid(account, uuid);
        // 验证用户信息是否合法
        if (Dto.isObjNull(userInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "账号已在其他地方登录", eventName);
            return;
        }
        // 是加入俱乐部
        if (!userInfo.containsKey("clubIds") || Dto.stringIsNULL(userInfo.getString("clubIds"))) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入" + clubName, eventName);
            return;
        }
        // 俱乐部是否存在
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, clubName + "不存在", eventName);
            return;
        }
        // 更新数据库 置顶取当前俱乐部id，取消置顶取0
        long topClubId = top == CommonConstant.GLOBAL_YES ? clubInfo.getLong("id") : 0L;
        clubBiz.updateUserTopClub(account, topClubId);
        JSONObject userClub = clubBiz.getUserClubByAccount(account);
        // 通知玩家
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
        result.put(CommonConstant.RESULT_KEY_MSG, "修改成功");
        if (!Dto.isObjNull(userClub) && userClub.containsKey("clubIds") && !Dto.stringIsNULL(userClub.getString("clubIds"))) {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_YES);
            List<JSONObject> clubList = getUserClubListInfo(userClub);
            result.put("clubList", clubList);
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

    /**
     * 刷新俱乐部信息
     * @param client
     * @param data
     */
    public void refreshClubInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 俱乐部编号
        String clubCode = postData.getString("clubCode");
        // 游戏id
        long gid = postData.getLong("gid");
        JSONObject result = new JSONObject();
        int onlineNum = 1;
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            if (clubCode.equals(RoomManage.gameRoomMap.get(roomNo).getClubCode())) {
                onlineNum += RoomManage.gameRoomMap.get(roomNo).getPlayerMap().size();
            }
        }
        result.put("onlineNum", onlineNum);
        // 游戏id
        result.put("gid", gid);
        // 俱乐部编号
        result.put("clubCode", clubCode);
        List<JSONObject> roomList = new ArrayList<>();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            // 当前俱乐部的所有房间
            if (!Dto.stringIsNULL(clubCode) && clubCode.equals(room.getClubCode())) {
                // gid为0刷新全部，否则只刷新当前房间
                if (gid == 0 || room.getGid() == gid) {
                    JSONObject roomObj = new JSONObject();
                    // 房间号
                    roomObj.put("room_no", roomNo);
                    // 游戏id
                    roomObj.put("game", room.getGid());
                    // 底注
                    roomObj.put("di", room.getScore());
                    // 房间详情
                    roomObj.put("detail", room.getWfType());
                    // 是否满人
                    roomObj.put("isFull", CommonConstant.GLOBAL_NO);
                    if (room.getPlayerMap().size() >= room.getPlayerCount()) {
                        roomObj.put("isFull", CommonConstant.GLOBAL_YES);
                    }
                    // 当前人数
                    roomObj.put("curCount", room.getPlayerMap().size());
                    // 总人数
                    roomObj.put("totalCount", room.getPlayerCount());
                    // 当前局数
                    roomObj.put("curIndex", room.getGameIndex());
                    // 总局数
                    roomObj.put("totalIndex", room.getGameCount());
                    // 玩家详情
                    List<JSONObject> users = new ArrayList<>();
                    for (String player : room.getPlayerMap().keySet()) {
                        users.add(new JSONObject().element("headimg", room.getPlayerMap().get(player).getRealHeadimg()));
                    }
                    roomObj.put("users", users);
                    roomList.add(roomObj);
                }
            }
        }
        result.put("roomList", roomList);
        // 通知玩家
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "refreshClubInfoPush");
    }

    /**
     * 快速加入
     * @param client
     * @param data
     */
    public void quickJoinClubRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 通知事件名称
        String eventName = "quickJoinClubRoomPush";
        String clubCode = postData.getString("clubCode");
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        int gameId = postData.getInt("gid");
        // 俱乐部不存在
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, clubName + "不存在", eventName);
            return;
        }
        // 用户信息不存在
        JSONObject userInfo = clubBiz.getUserByAccountAndUuid(account, postData.getString("uuid"));;
        if (Dto.isObjNull(userInfo) || !getUserClubList(userInfo).contains(clubInfo.getString("id"))) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入该" + clubName, eventName);
            return;
        }
        if (postData.containsKey("base_info")) {
            // 判断余额是否足够
            // 当前俱乐部总余额必须大于已开房未扣费的房间需要消耗的总和+当前房间需要消耗的
            int cost = getRoomCostByBaseInfo(postData.getJSONObject("base_info"));
            if (cost == -1 || cost + getClubCost(clubCode) > clubInfo.getDouble("balance")) {
                sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "余额不足，请联系会长充值", eventName);
                return;
            }
            baseEventDeal.createRoomBase(client,data);
            return;
        }
        List<String> roomNoList = new ArrayList<String>();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            // 当前俱乐部的所有房间
            if (!Dto.stringIsNULL(clubCode) && clubCode.equals(room.getClubCode()) && room.getGid() == gameId
                && !room.getPlayerMap().containsKey(account) && room.getPlayerMap().size() < room.getPlayerCount()) {
                roomNoList.add(roomNo);
            }
        }
        // 快速加入有房间加入房间 没有房间创建房间
        if (roomNoList.size() == 0) {
            JSONObject quickSetting = !Dto.isObjNull(clubInfo.getJSONObject("quick_setting")) ?
                clubInfo.getJSONObject("quick_setting").getJSONObject(String.valueOf(gameId)) : null;
            if (!Dto.isObjNull(quickSetting)) {
                // 判断余额是否足够
                int cost = getRoomCostByBaseInfo(quickSetting);
                if (cost == -1 || cost + getClubCost(clubCode) > clubInfo.getDouble("balance")) {
                    sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "余额不足，请联系会长充值", eventName);
                    return;
                }
                postData.put("base_info", quickSetting);
                baseEventDeal.createRoomBase(client,postData);
            } else {
                sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "参数不正确,请联系会长修改", eventName);
            }
        }else {
            // 随机加入
            Collections.shuffle(roomNoList);
            postData.put("room_no",roomNoList.get(0));
            postData.put("clubId",clubInfo.getLong("id"));
            baseEventDeal.joinRoomBase(client,postData);
        }
    }

    /**
     * 计算当前房间所需要消耗的房卡
     * @param baseInfo
     * @return
     */
    private int getRoomCostByBaseInfo(JSONObject baseInfo) {
        try {
            if (baseInfo.containsKey("player") && baseInfo.containsKey("turn")) {
                int player = baseInfo.getInt("player");
                JSONObject turn = baseInfo.getJSONObject("turn");
                if (turn.containsKey("AANum")) {
                    int aaNum = turn.getInt("AANum");
                    if (turn.containsKey("increase")) {
                        aaNum += player  > 4 ? player * turn.getInt("increase") : 4 * turn.getInt("increase");
                    }
                    return player * aaNum;
                }
            }
        } catch (Exception e) {

        }
        return -1;
    }

    /**
     * 当前俱乐部已消耗
     * @param clubCode
     * @return
     */
    private int getClubCost(String clubCode) {
        int cost = 0;
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            if (room.getRoomType() == CommonConstant.ROOM_TYPE_CLUB && clubCode.equals(room.getClubCode()) && !room.isCost()) {
                cost += room.getSinglePayNum() * room.getPlayerCount();
            }
        }
        return cost;
    }

    /**
     * 发送失败提示信息
     * @param client
     * @param code
     * @param msg
     * @param eventName
     */
    private void sendPromptToSingle(SocketIOClient client, int code, String msg, String eventName) {
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, code);
        result.put(CommonConstant.RESULT_KEY_MSG, msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

    /**
     * 获取玩家俱乐部集合
     * @param userInfo
     * @return
     */
    private List<String> getUserClubList(JSONObject userInfo) {
        String clubIds = userInfo.getString("clubIds");
        return new ArrayList<>(Arrays.asList(clubIds.substring(1, clubIds.length()).split("\\$")));
    }

    /**
     * 根据游戏id获取游戏名称
     * @param gameId
     * @return
     */
    private String getGameNameById(int gameId) {
        JSONObject gameInfo;
        try {
            StringBuffer key = new StringBuffer();
            key.append("game_on_or_off_");
            key.append(gameId);
            Object object = redisService.queryValueByKey(String.valueOf(key));
            if (object!=null) {
                gameInfo = JSONObject.fromObject(object);
                return gameInfo.getString("name");
            }else {
                gameInfo = roomBiz.getGameInfoByID(gameId);
                if (!Dto.isObjNull(gameInfo)) {
                    redisService.insertKey(String.valueOf(key), String.valueOf(gameInfo), null);
                    return gameInfo.getString("name");
                }
            }
        } catch (Exception e) {
            gameInfo = roomBiz.getGameInfoByID(gameId);
            if (!Dto.isObjNull(gameInfo)) {
                return gameInfo.getString("name");
            }
        }
        return null;
    }
}
