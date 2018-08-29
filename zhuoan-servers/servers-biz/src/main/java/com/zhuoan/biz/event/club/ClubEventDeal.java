package com.zhuoan.biz.event.club;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.BaseEventDeal;
import com.zhuoan.biz.game.biz.ClubBiz;
import com.zhuoan.biz.game.biz.UserBiz;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.constant.ClubConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.Constant;
import com.zhuoan.util.Dto;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
    private BaseEventDeal baseEventDeal;

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
            List<JSONObject> clubList = new ArrayList<>();
            // 当前所有已加入的俱乐部
            String[] clubIds = userClub.getString("clubIds").substring(1, userClub.getString("clubIds").length() - 1).split("\\$");
            // 当前已经入该俱乐部
            for (int i = 0; i < clubIds.length; i++) {
                JSONObject clubInfo = clubBiz.getClubById(Long.valueOf(clubIds[i]));
                JSONObject obj = new JSONObject();
                obj.put("clubCode", clubInfo.getString("clubCode"));
                obj.put("clubName", clubInfo.getString("clubName"));
                // TODO: 2018/8/22 会长头像
                obj.put("imgUrl", "imgUrl");
                obj.put("isTop",userClub.containsKey("top_club") && Long.valueOf(clubIds[i]) == userClub.getLong("top_club") ? CommonConstant.GLOBAL_YES : CommonConstant.GLOBAL_NO);
                clubList.add(obj);
            }
            result.put("clubList", clubList);
        } else {
            result.put(CommonConstant.RESULT_KEY_CODE, CommonConstant.GLOBAL_NO);
            result.put(CommonConstant.RESULT_KEY_MSG, "未加入俱乐部");
        }
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getMyClubListPush");
    }

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

    public void getClubSetting(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (!postData.containsKey(ClubConstant.DATA_KEY_CLUB_CODE)) {
            return;
        }
        JSONObject result = new JSONObject();
        // 俱乐部编号
        String clubCode = postData.getString(ClubConstant.DATA_KEY_CLUB_CODE);
        // 俱乐部信息
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (!Dto.isObjNull(clubInfo)) {
            // TODO: 2018/8/22 会长头像、昵称
            result.put("leader_img", "todo_img");
            result.put("leader_name", "todo_name");
            result.put("clubName", clubInfo.getString("clubName"));
            result.put("clubCode", clubInfo.getString("clubCode"));
            JSONArray memberArray = clubBiz.getClubMember(clubInfo.getLong("id"));
            result.put("memberCount", memberArray == null ? 0 : memberArray.size());
            result.put("notice", clubInfo.containsKey("notice") ? clubInfo.getString("notice") : "");
            result.put("setting",clubInfo.containsKey("setting") ? clubInfo.getString("setting") : "");
            result.put("isLeader", CommonConstant.GLOBAL_YES);
            CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), "getClubSettingPush");
        }
    }

    public void changeClubSetting(SocketIOClient client, Object data) {
        /**
         * 1、是否会长本人发起修改
         * 2、验证数据格式是否正确
         * 3、修改数据库
         */
        JSONObject postData = JSONObject.fromObject(data);
        String clubCode = postData.getString("clubCode");
        String leader = postData.getString("leader");
        String uuid = postData.getString("uuid");
        String notice = postData.getString("notice");
        String setting = postData.getString("setting");
        String quickSetting = postData.getString("quick_setting");
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
        // TODO: 2018/8/29 数据验证
        // 更新数据库
        JSONObject newClubInfo = new JSONObject();
        newClubInfo.put("id", clubInfo.getLong("id"));
        newClubInfo.put("notice", notice);
        newClubInfo.put("setting", setting);
        newClubInfo.put("quick_setting", quickSetting);
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
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入俱乐部", eventName);
            return;
        }
        // 俱乐部是否存在
        JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
        if (Dto.isObjNull(clubInfo)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "俱乐部不存在", eventName);
            return;
        }
        // 当前用户所在的所有俱乐部
        String clubIds = userInfo.getString("clubIds");
        List<String> clubIdList = new ArrayList<>(Arrays.asList(clubIds.substring(1, clubIds.length()).split("\\$")));
        // 当前要退出的俱乐部id
        String clubId = clubInfo.getString("id");
        // 未加入当前俱乐部提示，已加入更新数据库
        if (!clubIdList.contains(clubId)) {
            sendPromptToSingle(client, CommonConstant.GLOBAL_NO, "未加入该俱乐部", eventName);
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

    }

    public void refreshClubInfo(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        // 俱乐部编号
        String clubCode = postData.getString("clubCode");
        // 玩家账号
        String account = postData.getString("account");
        // 游戏id
        long gid = postData.getLong("gid");
        JSONObject result = new JSONObject();
        // TODO: 2018/8/29 在线人数
        result.put("onlineNum", 0);
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

    public void sendPromptToSingle(SocketIOClient client, int code, String msg, String eventName) {
        JSONObject result = new JSONObject();
        result.put(CommonConstant.RESULT_KEY_CODE, code);
        result.put(CommonConstant.RESULT_KEY_MSG, msg);
        CommonConstant.sendMsgEventToSingle(client, String.valueOf(result), eventName);
    }

    public void quickJoinClubRoom(SocketIOClient client, Object data) {
        JSONObject postData = JSONObject.fromObject(data);
        if (postData.containsKey("base_info")) {
            baseEventDeal.createRoomBase(client,data);
            return;
        }
        String clubCode = postData.getString("clubCode");
        String account = postData.getString(CommonConstant.DATA_KEY_ACCOUNT);
        List<String> roomNoList = new ArrayList<String>();
        for (String roomNo : RoomManage.gameRoomMap.keySet()) {
            GameRoom room = RoomManage.gameRoomMap.get(roomNo);
            // 当前俱乐部的所有房间
            if (!Dto.stringIsNULL(clubCode) && clubCode.equals(room.getClubCode())
                && !room.getPlayerMap().containsKey(account) && room.getPlayerMap().size() < room.getPlayerCount()) {
                roomNoList.add(roomNo);
            }
        }
        if (roomNoList.size() == 0) {
            JSONObject clubInfo = clubBiz.getClubByCode(clubCode);
            if (!Dto.isObjNull(clubInfo)) {
                postData.put("base_info",clubInfo.getString("quick_setting"));
                // TODO: 2018/8/29 gid取值 
                postData.put("gid",4);
                baseEventDeal.createRoomBase(client,postData);
            }
        }else {
            // 随机加入
            Collections.shuffle(roomNoList);
            postData.put("room_no",roomNoList.get(0));
            baseEventDeal.joinRoomBase(client,postData);
        }
    }
}
