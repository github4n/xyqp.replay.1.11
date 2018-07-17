package com.zhuoan.biz.event.match;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.constant.CommonConstant;
import com.zhuoan.constant.MatchConstant;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * @author wqm
 * @DESCRIPTION
 * @Date Created in 13:42 2018/7/12
 * @Modified By:
 **/
@Component
public class MatchEvent {
    @Resource
    private Destination matchQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerMatchGameEvent(SocketIOServer server) {

        /**
         * 获取比赛场配置信息
         */
        server.addEventListener("getMatchInfo", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(matchQueueDestination, new Messages(client, data, CommonConstant.GAME_MATCH, MatchConstant.MATCH_EVENT_GET_MATCH_SETTING));
            }
        });

        /**
         * 比赛场报名
         */
        server.addEventListener("matchSignUp", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(matchQueueDestination, new Messages(client, data, CommonConstant.GAME_MATCH, MatchConstant.MATCH_EVENT_SIGN_UP));
            }
        });

        /**
         * 更新人数
         */
        server.addEventListener("updateMatchCount", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(matchQueueDestination, new Messages(client, data, CommonConstant.GAME_MATCH, MatchConstant.MATCH_EVENT_UPDATE_MATCH_COUNT));
            }
        });

        /**
         * 退赛
         */
        server.addEventListener("matchCancelSign", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(matchQueueDestination, new Messages(client, data, CommonConstant.GAME_MATCH, MatchConstant.MATCH_EVENT_CANCEL_SIGN));
            }
        });

        /**
         * 获奖记录
         */
        server.addEventListener("getWinningRecord", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(matchQueueDestination, new Messages(client, data, CommonConstant.GAME_MATCH, MatchConstant.MATCH_EVENT_GET_WINNING_RECORD));
            }
        });
    }
}
