package com.zhuoan.service.socketio.impl;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.za.game.remote.iservice.IService;
import com.zhuoan.biz.event.BaseGameEvent;
import com.zhuoan.biz.event.bdx.BDXGameEvent;
import com.zhuoan.biz.event.gppj.GPPJGameEvent;
import com.zhuoan.biz.event.nn.NNGameEvent;
import com.zhuoan.biz.event.qzmj.QZMJGameEvent;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.event.sw.SwGameEvent;
import com.zhuoan.biz.event.zjh.ZJHGameEvent;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.robot.RobotEventDeal;
import com.zhuoan.constant.SocketConfigConstant;
import com.zhuoan.dao.DBUtil;
import com.zhuoan.enumtype.EnvKeyEnum;
import com.zhuoan.queue.SqlQueue;
import com.zhuoan.service.socketio.SocketIoManagerService;
import com.zhuoan.util.IpAddressUtil;
import com.zhuoan.util.LogUtil;
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
     * The constant registry.
     */
    private static Registry registry = null;
    /**
     * 游戏服务器名称
     */
    private static String hostname;

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

    @Resource
    private QZMJGameEvent qzmjGameEvent;

    @Resource
    private GPPJGameEvent gppjGameEvent;

    @Resource
    private SwGameEvent swGameEvent;

    @Resource
    private RobotEventDeal robotEventDeal;

    @Override
    public void startServer() {
        // ########################## 当前主机服务 HOSTNAME IP：PORT ##########################
        String localOuterNetIp = IpAddressUtil.getInnerNetIp();
        /* 当前环境 = online, 则走外网IP */

        if (StringUtils.endsWithIgnoreCase(env.getProperty(EnvKeyEnum.RUN_ENVIRONMENT.getKey()), SocketConfigConstant.RUN_ENV)) {
            // 本机外网IP
            localOuterNetIp = IpAddressUtil.getLocalOuterNetIp();
        }
        int localPort = Integer.valueOf(env.getProperty(EnvKeyEnum.LOCAL_PORT.getKey()));
        // 本地启用主机名字
        hostname = "游戏服务器名称 -> [" + localOuterNetIp + "_GAME_SERVER]";
        //##########################################################################

        // ########################## 远程主机服务 IP：PORT ##########################
        // 远程主机IP
        String remoteIp = env.getProperty(EnvKeyEnum.SERVER_IP.getKey());
        // 远程主机端口
        String remotePort = env.getProperty(EnvKeyEnum.SERVER_PORT.getKey());
        //###########################################################################

        logger.info("localOuterNetIp:"+localOuterNetIp+"localPort:"+localPort);
        logger.info("remoteIp:"+remoteIp+"remotePort:"+remotePort);

        /* 调用远程方法：告知 ### 当前主机服务 HOSTNAME IP：PORT ### */
        invokeRemoteMethod(remoteIp, remotePort, localOuterNetIp, localPort);

        /* 创建SocketIO服务 */
        server = new SocketIOServer(serverConfig(IpAddressUtil.getInnerNetIp(), localPort));

        /* 添加监听事件 */
        addEventListener(server);
        logger.info("============================== 紧接,SocketIO服务完成了监听事件的添加  ==============================");

        server.start();
        logger.info("==============================[ SOCKET-IO服务启用成功 ]==============================");

        /* 调度器处理：更新缓存 + 房间定时器 */
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
            logger.info("==============================[ SOCKET-IO服务[" + host + ":" + port + "]已关闭 ]==============================");
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


    private Configuration serverConfig(String innerNetIp, int localPort) {
        Configuration config = new Configuration();

        // 服务器主机IP
        config.setHostname(innerNetIp);
        // 端口
        config.setPort(localPort);

        logger.info("============================== 其次,SocketIO 启用本地服务 [" + innerNetIp + "(当前主机内网IP):" + localPort + "(必须开放此端口)] ==============================");

        config.setWorkerThreads(SocketConfigConstant.WORKER_THREADS);
        config.setMaxFramePayloadLength(SocketConfigConstant.MAX_FRAME_PAYLOAD_LENGTH);
        config.setMaxHttpContentLength(SocketConfigConstant.MAX_HTTP_CONTENT_LENGTH);
        return config;
    }

    private void invokeRemoteMethod(String remoteHostName, String remotePort, String localOuterNetIp, int localPort) {
        try {
            // 获取RMI注册管理器
            registry = LocateRegistry.getRegistry(remoteHostName, Integer.valueOf(remotePort));
            // 根据命名获取服务
            IService server = (IService) registry.lookup("sysService");

            /**
             *  调用远程方法: 告知SOCKET的IP和PORT
             */
            server.joinServer(
                //外网ip
                localOuterNetIp,
                //本地端口
                localPort,
                //socket主机名
                hostname
            );

            logger.info("============================== 首先,调用RMI成功：告知本地服务 [" + localOuterNetIp + "(当前主机外网IP):" + localPort + "(必须开放此端口)] ==============================");

            // 开启定时任务
            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("heartbeat-schedule-pool-%d").daemon(true).build());
            executor.scheduleWithFixedDelay(new GameTask(), 0, 1, TimeUnit.MINUTES);

        } catch (RemoteException | NotBoundException e) {
            logger.error("尝试重新连接Socket", e);
            startServer();
        } catch (Exception e) {
            logger.error("RMI异常", e);
            throw new RuntimeException(e);
        }
    }

    private void scheduleDeal() {
        sqlQueue = sqlQueue1;
        /* 获取房间设置放入Json数组里面*/
        preSelectRoomSetting();

        robotEventDeal.startRobot();
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
                // 根据命名获取服务
                IService server = (IService) registry.lookup("sysService");

                // 心跳请求
                server.heartBeat(hostname);

                logger.info("RMI调用成功：I am a RMI-heartbeat");

            } catch (RemoteException | NotBoundException e) {
                logger.info("尝试重新连接Socket");
                startServer();
            } catch (Exception e) {
                logger.error("RMI异常", e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @param server
     */
    private void addEventListener(SocketIOServer server) {
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

        /* 泉州麻将 */
        qzmjGameEvent.listenerQZMJGameEvent(server);

        /* 骨牌牌九 */
        gppjGameEvent.listenerGPPJGameEvent(server);

        /* 水蛙 */
        swGameEvent.listenerSwGameEvent(server);

    }
}
