package com.zhuoan.socketio.impl;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.za.game.remote.iservice.IService;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.queue.MessageQueue;
import com.zhuoan.queue.SqlQueue;
import com.zhuoan.socketio.SocketIoManagerService;
import com.zhuoan.times.SingleTimer;
import com.zhuoan.util.LogUtil;
import com.zhuoan.constant.SocketListenerConstant;
import com.zhuoan.enumtype.EnvKeyEnum;
import net.sf.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SocketIoManagerService
 *
 * @author weixiang.wu
 * @date 2018 -04-02 09:15
 */
@Service
public class GameMain implements SocketIoManagerService {

    private final static Logger logger = LoggerFactory.getLogger(GameMain.class);

    @Resource
    private Environment env;

    /**
     * The Server.
     */
    public static SocketIOServer server;

    // 注册管理器
    public static Registry registry = null;

    public static SqlQueue sqlQueue = null;
    public static MessageQueue messageQueue = null;
    public static SingleTimer singleTime = null;

    @Override
    public void startServer() {
//        注册服务
        registerService();

        // 创建服务
        server = new SocketIOServer(serverConfig());

        messageQueue = new MessageQueue(16);
        sqlQueue = new SqlQueue(1);
        singleTime = new SingleTimer();
        singleTime.start();

        /**
         * 心跳包
         */
        server.addEventListener("game_ping", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                client.sendEvent("game_pong", obj);
            }
        });

        /**
         * 链接connection
         */
        server.addEventListener("connection", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object obj, AckRequest request) {
                logger.info("链接成功");
                client.sendEvent("connect", request, "成功");
            }
        });

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
                logger.info("Client: {} with sessionId: {} 在线了！！！", obtainClientIp(client), client.getSessionId());
            }
        });

        /**
         * 当client离线时触发此事件
         */
        server.addDisconnectListener(new DisconnectListener() {
            @Override
            public void onDisconnect(SocketIOClient client) {
                logger.info("Client: {} with sessionId: {} 离线了！！！", obtainClientIp(client), client.getSessionId());
            }
        });

        server.start();

        logger.info("SocketIO server is started successfully!!!!!!");

        String sql = "select room_no from za_gamerooms where status>=0";
        JSONArray result = DBUtil.getObjectListBySQL(sql, new Object[]{});

        sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting";
        RoomManage.result = DBUtil.getObjectListBySQL(sql, new Object[]{});
        logger.error(String.valueOf(RoomManage.result));
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


    private void registerService() {
        try {

            // 获取服务注册管理器
            registry = LocateRegistry.getRegistry(env.getProperty(EnvKeyEnum.SERVER_IP.getKey()),
                Integer.valueOf(env.getProperty(EnvKeyEnum.SERVER_PORT.getKey())));

            // 列出所有注册的服务
            //String[] list = registry.list();

            // 根据命名获取服务
            IService server = (IService) registry.lookup("sysService");

            // 调用远程方法
            server.joinServer(env.getProperty(EnvKeyEnum.LOCAL_REMOTE_IP.getKey()),
                Integer.valueOf(env.getProperty(EnvKeyEnum.LOCAL_PORT.getKey())),
                env.getProperty(EnvKeyEnum.LOCAL_NAME.getKey()));

            // 开启定时任务
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleWithFixedDelay(new GameTask(), 0, 1, TimeUnit.MINUTES);

        } catch (NotBoundException | RemoteException e) {
            e.printStackTrace();
        }
    }

    class GameTask extends TimerTask {

        public void run() {

            try {
                // 根据命名获取服务
                IService server = (IService) registry.lookup("sysService");

                // 心跳请求
                server.heartBeat(env.getProperty(EnvKeyEnum.LOCAL_NAME.getKey()));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
