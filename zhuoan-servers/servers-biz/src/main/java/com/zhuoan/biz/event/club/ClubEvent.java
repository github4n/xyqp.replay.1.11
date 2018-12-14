package com.zhuoan.biz.event.club;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.ClubConstant;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 *
 */
@Service
public class ClubEvent {

    @Resource
    private Destination clubQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerClubEvent(SocketIOServer server) {

        /**
         * 加入俱乐部
         */
        server.addEventListener("joinClub", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_JOIN_CLUB));
            }
        });

        /**
         * 获取玩家俱乐部列表
         */
        server.addEventListener("getMyClubList", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_GET_MY_CLUB_LIST));
            }
        });


        /**
         * 获取俱乐部成员
         */
        server.addEventListener("getClubMembers", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_GET_CLUB_MEMBERS));
            }
        });

        /**
         * 获取俱乐部设置
         */
        server.addEventListener("getClubSetting", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_GET_CLUB_SETTING));
            }
        });

        /**
         * 更改俱乐部设置
         */
        server.addEventListener("changeClubSetting", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_CHANGE_CLUB_SETTING));
            }
        });

        /**
         * 退出俱乐部
         */
        server.addEventListener("exitClub", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_EXIT_CLUB));
            }
        });

        /**
         * 置顶/取消置顶
         */
        server.addEventListener("toTop", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_TO_TOP));
            }
        });

        /**
         * 刷新俱乐部信息
         */
        server.addEventListener("refreshClubInfo", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_REFRESH_CLUB_INFO));
            }
        });

        /**
         * 俱乐部快速加入房间
         */
        server.addEventListener("quickJoinClubRoom", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_QUICK_JOIN_CLUB_ROOM));
            }
        });

        /**
         * 获取俱乐部会长审批列表
         */
        server.addEventListener("getClubApplyList", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_GET_CLUB_APPLY_LIST));
            }
        });

        /**
         * 俱乐部会长审批
         */
        server.addEventListener("clubApplyReview", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_CLUB_APPLY_REVIEW));
            }
        });

        /**
         * 俱乐部会长邀请
         */
        server.addEventListener("clubLeaderInvite", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(clubQueueDestination, new Messages(client, data, CommonConstant.GAME_CLUB, ClubConstant.CLUB_EVENT_CLUB_LEADER_INVITE));
            }
        });

    }
}
