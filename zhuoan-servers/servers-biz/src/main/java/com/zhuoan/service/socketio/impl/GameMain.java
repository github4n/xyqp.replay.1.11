package com.zhuoan.service.socketio.impl;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.za.game.remote.iservice.IService;
import com.zhuoan.biz.event.BaseGameEvent;
import com.zhuoan.biz.event.bdx.BDXGameEvent;
import com.zhuoan.biz.event.nn.NNGameEvent;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.event.zjh.ZJHGameEvent;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.model.UserInfoCache;
import com.zhuoan.constant.SocketListenerConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.enumtype.EnvKeyEnum;
import com.zhuoan.queue.SqlQueue;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.times.SingleTimer;
import com.zhuoan.util.LogUtil;
import com.zhuoan.util.thread.ThreadPoolHelper;
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

    /**
     * The constant server.
     */
    public static SocketIOServer server; // socketio服务
    /**
     * The constant sqlQueue.
     */

    @Resource
    private SqlQueue sqlQueue1;

    public static SqlQueue sqlQueue = null;
    /**
     * The constant singleTime.
     */
    public static SingleTimer singleTime = null;
    /**
     * The constant registry.
     */
    public static Registry registry = null;


    @Resource
    private Environment env;

    @Resource
    private BaseGameEvent baseGameEvent;

    @Resource
    private SSSGameEvent sssGameEvent;

    @Resource
    private ZJHGameEvent zjhGameEvent;

    @Resource
    private NNGameEvent nnGameEvent;

    @Resource
    private BDXGameEvent bdxGameEvent;

    @Override
    public void startServer() {
        /* 创建socketio服务 */
        server = new SocketIOServer(serverConfig());

        /*
         *  调用远程服务：  joinServerList + pingpong
         *  作用：远程服务注册中心调用此服务器 ip port 进行 socketio 事件通讯
         */
        asRemoteServer();

        /* 调度器处理：更新缓存+房间定时器 */
        scheduleDeal();

        /* 添加监听事件 */
        addEventListener(server);

        server.start();
        logger.info("SocketIO server is started successfully!!!!!!");
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

    private void asRemoteServer() {
        try {
            // 获取服务注册管理器
            Registry registry = LocateRegistry.getRegistry(env.getProperty(EnvKeyEnum.SERVER_IP.getKey()),
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
            logger.error("获取远程服务注册管理器中发生了异常",e.getMessage());
        }
    }

    private void scheduleDeal() {
//        messageQueue = new MessageQueue(16);


        sqlQueue = sqlQueue1;
        ThreadPoolHelper.executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        UserInfoCache.updateCache();
                        Thread.sleep(5000);
                    }
                } catch (Exception e) {
                    logger.error("数据库队列处理异常", e.getMessage());
                }
            }
        });

        singleTime = new SingleTimer();
        singleTime.start();

        /* 获取房间设置放入Json数组里面*/
        preSelectRoomSetting();
    }

    private void preSelectRoomSetting() {
        /* 查询上次服务器断开所有在线的房间*/
        String sql = "select room_no from za_gamerooms where status>=0";
        JSONArray result = DBUtil.getObjectListBySQL(sql, new Object[]{});
        LogUtil.print("查询上次服务器断开所有在线的房间：" + result);

        /* 获取房间设置*/
        sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting";
        RoomManage.result = DBUtil.getObjectListBySQL(sql, new Object[]{});
        LogUtil.print("获取房间设置：" + String.valueOf(RoomManage.result));
    }


    /**
     * The type Game task.
     */
    class GameTask extends TimerTask {

        public void run() {
            try {
                // 获取服务注册管理器
                 registry = LocateRegistry.getRegistry(env.getProperty(EnvKeyEnum.SERVER_IP.getKey()),
                    Integer.valueOf(env.getProperty(EnvKeyEnum.SERVER_PORT.getKey())));

                // 根据命名获取服务
                IService server = (IService) registry.lookup("sysService");

                // 心跳请求
                server.heartBeat(env.getProperty(EnvKeyEnum.LOCAL_NAME.getKey()));

            } catch (Exception e) {
               logger.error("获取服务注册管理器发生异常",e.getMessage());
            }
        }
    }

    /**
     * @param server 方便后续配置至不同服务器
     */
    private void addEventListener(SocketIOServer server) {

        /**
         * todo：  后续需在配置文件配置： (ip1：events标识1)|(ip2：events标识2)……并完善代码！  作用：event绑定对应的server，后续打包只要动配置
         */
//        String configHostIp = env.getProperty(EnvKeyEnum.SERVER_IP.getKey());
//        String currentHostIp = server.getConfiguration().getHostname();

        /* 公共事件监听 */
        baseGameEvent.listenerBaseGameEvent(server);



        /* 比大小 */
        bdxGameEvent.listenerBDXGameEvent(server);

        /* 牛牛 */
        nnGameEvent.listenerNNGameEvent(server);

        /* 十三水 */
        sssGameEvent.listenerSSSGameEvent(server);

        /* 炸金花 */
        zjhGameEvent.listenerZJHGameEvent(server);


    }
}
