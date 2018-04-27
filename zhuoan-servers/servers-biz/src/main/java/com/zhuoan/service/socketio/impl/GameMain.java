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
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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
     * The constant server.SocketIO服务
     */
    public static SocketIOServer server;
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
        //本地Server ip：port
        String localHostName = env.getProperty(EnvKeyEnum.LOCAL_IP.getKey());
        String localPort = env.getProperty(EnvKeyEnum.LOCAL_PORT.getKey());
        //远程Server ip：port
        String remoteHostName = env.getProperty(EnvKeyEnum.SERVER_IP.getKey());
        String remotePort = env.getProperty(EnvKeyEnum.SERVER_PORT.getKey());

        /* 调用远程方法：告知本地服务的ip:port*/
        invokeRemoteMethod(remoteHostName, remotePort, localHostName, localPort);

        /* 创建SocketIO服务 */
        server = new SocketIOServer(serverConfig(localHostName, localPort));

        /* 添加监听事件 */
        addEventListener(server);
        logger.info("==============================[ 紧接,SocketIO服务完成了监听事件的添加  ]==============================");

        server.start();
        logger.info("==============================[ 最终,SOCKET-IO服务开启成功!  ]==============================");

        /* 调度器处理：更新缓存+房间定时器 */
        scheduleDeal();
    }

    /**
     * 停止服务
     */
    @Override
    public void stopServer() {
        if (server != null) {
            server.stop();
            Configuration configuration = server.getConfiguration();
            int port = configuration.getPort();
            String host = configuration.getHostname();
            server = null;
            logger.info("==============================[ SOCKET-IO服务[" + host + ":" + port + " ]已关闭!  ]==============================");
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


    private Configuration serverConfig(String localHostName, String localPort) {
        Configuration config = new Configuration();

        // 服务器主机IP
        config.setHostname(localHostName);
        // 端口
        config.setPort(Integer.valueOf(localPort));

        logger.info("============================== 其次,SocketIO 启用本地服务 [" + localHostName + ":" + localPort + "] ==============================");

        config.setWorkerThreads(SocketListenerConstant.WORKER_THREADS);
        config.setMaxFramePayloadLength(SocketListenerConstant.MAX_FRAME_PAYLOAD_LENGTH);
        config.setMaxHttpContentLength(SocketListenerConstant.MAX_HTTP_CONTENT_LENGTH);
        return config;
    }

    private void invokeRemoteMethod(String remoteHostName, String remotePort, String localHostName, String localPort) {
        try {
            // 获取RMI注册管理器
            Registry registry = LocateRegistry.getRegistry(remoteHostName, Integer.valueOf(remotePort));
            // 根据命名获取服务
            IService server = (IService) registry.lookup("sysService");

            // 调用远程方法: 告知SOCKETIO的IP和PORT
            server.joinServer(localHostName, Integer.valueOf(localPort),
                env.getProperty(EnvKeyEnum.LOCAL_NAME.getKey()));
            logger.info("============================== 首先,调用远程方法：告知本地服务 [" + localHostName + ":" + localPort + "] ==============================");

            // 开启定时任务
            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("userInfoCache-schedule-pool-%d").daemon(true).build());
            executor.scheduleWithFixedDelay(new GameTask(), 0, 1, TimeUnit.MINUTES);

        } catch (Exception e) {
            logger.error("RMI异常", e);
            throw new RuntimeException(e);
        }
    }

    private void scheduleDeal() {
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
                    logger.error("数据库队列处理异常", e);
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
    }


    /**
     * The type Game task.
     */
    class GameTask extends TimerTask {

        @Override
        public void run() {
            try {
                // 获取服务注册管理器
                registry = LocateRegistry.getRegistry(env.getProperty(EnvKeyEnum.SERVER_IP.getKey()),
                    Integer.valueOf(env.getProperty(EnvKeyEnum.SERVER_PORT.getKey())));

                // 根据命名获取服务
                IService server = (IService) registry.lookup("sysService");

                // 心跳请求
                server.heartBeat(env.getProperty(EnvKeyEnum.LOCAL_NAME.getKey()));

                logger.info("I am a RMI-heartbeat");

            } catch (RemoteException | NotBoundException e) {
                logger.info("尝试重新连接");
                startServer();
            } catch (Exception e) {
                logger.error("RMI异常", e);
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
