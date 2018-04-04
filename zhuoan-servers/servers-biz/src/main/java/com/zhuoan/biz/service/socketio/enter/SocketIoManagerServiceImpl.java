package com.zhuoan.biz.service.socketio.enter;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.zhuoan.biz.dao.DBUtil;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.queue.MessageQueue;
import com.zhuoan.biz.queue.SqlQueue;
import com.zhuoan.biz.times.SingleTimer;
import com.zhuoan.biz.util.LogUtil;
import com.zhuoan.constant.SocketListenerConstant;
import com.zhuoan.enumtype.EnvKeyEnum;
import net.sf.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * SocketIoManagerService
 *
 * @author weixiang.wu
 * @date 2018 -04-02 09:15
 */
@Service
public class SocketIoManagerServiceImpl implements SocketIoManagerService {

    private final static Logger logger = LoggerFactory.getLogger(SocketIoManagerServiceImpl.class);

    @Resource
    private Environment env;

    /**
     * The Server.
     */
    private static SocketIOServer server;

    @Override
    public void startServer() {
        // 创建服务
        server = new SocketIOServer(serverConfig());



        MessageQueue messageQueue = new MessageQueue(16);
        new SqlQueue(1);
        SingleTimer singleTime = new SingleTimer();
        singleTime.start();




        logger.info("SocketIO 添加监听事件INg");

        /**
         * 监听十三水游戏事件
         */
        SSSGameEvent.listenerSSSGameEvent(server, messageQueue);


        // 监听广告推送事件，advert_info为事件名称，自定义
       /* server.addEventListener("advert_info", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) throws ClassNotFoundException {
                //客户端推送advert_info事件时，onData接受数据，这里是string类型的json数据，还可以为Byte[],object其他类型
                String sa = String.valueOf(client.getRemoteAddress());
                String clientIp = sa.substring(1, sa.indexOf(":"));//获取客户端连接的ip
                Map params = client.getHandshakeData().getUrlParams();//获取客户端url参数

                logger.info(clientIp + "：客户端：************" + data);
            }
        });*/


        /**
         * 当client连接时触发此事件
         */
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                logger.info(obtainClientIp(client) + "在线了！！！");
            }
        });

        /**
         * 当client离线时触发此事件
         */
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                logger.info(obtainClientIp(client) + "离线了！！！");
            }
        });

        server.start();

        logger.info("SocketIO server is started successfully!!!!!!");

        String sql = "select room_no from za_gamerooms where status>=0";
        JSONArray result = DBUtil.getObjectListBySQL(sql, new Object[]{});

        sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting";
        RoomManage.result = DBUtil.getObjectListBySQL(sql, new Object[]{});
        System.err.println(RoomManage.result);
        LogUtil.print("当前房间：" + result);
    }

    /**
     * 停止服务
     */
    @Override
    public void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }


    /**
     * 给所有连接客户端推送消息
     *
     * @param eventType 推送的事件类型
     * @param message   推送的内容
     */
    @Override
    public void sendMessageToAllClient(String eventType, String message) {
        Collection<SocketIOClient> clients = server.getAllClients();
        for (SocketIOClient client : clients) {
            client.sendEvent(eventType, message);
        }
    }

    /**
     * 给具体的客户端推送消息
     *
     * @param uuid      the uuid
     * @param eventType the event type
     * @param message   推送的消息内容
     */
    @Override
    public void sendMessageToOneClient(String uuid, String eventType, String message) {
        if (StringUtils.isNotBlank(uuid)) {
            Map<String, SocketIOClient> clientsMap = new HashMap<String, SocketIOClient>();
            SocketIOClient client = clientsMap.get(uuid);
            if (client != null) {
                client.sendEvent(eventType, message);
            }
        }
    }

    @Override
    public SocketIOServer getServer() {
        return server;
    }


    private Configuration serverConfig() {
        Configuration config = new Configuration();
        // 服务器主机IP
        config.setHostname(env.getProperty(EnvKeyEnum.LOCAL_IP.getKey()));
        // 端口
        config.setPort(Integer.valueOf(env.getProperty(EnvKeyEnum.LOCAL_PORT.getKey())));
        config.setWorkerThreads(SocketListenerConstant.WORKER_THREADS);
        config.setMaxFramePayloadLength(SocketListenerConstant.MAX_FRAME_PAYLOAD_LENGTH);
        config.setMaxHttpContentLength(SocketListenerConstant.MAX_HTTP_CONTENT_LENGTH);
        return config;
    }

    /**
     * 获取设备ip
     *
     * @param client
     * @return
     */
    private String obtainClientIp(SocketIOClient client) {
        String sa = String.valueOf(client.getRemoteAddress());
        return sa.substring(1, sa.indexOf(":"));
    }

}
