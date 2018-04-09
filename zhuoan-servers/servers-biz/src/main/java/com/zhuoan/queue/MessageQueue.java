package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.biz.event.sss.SSSGameEventDeal;
import com.zhuoan.biz.model.GameLogsCache;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.service.sss.impl.SSSServiceImpl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The type Message queue.
 */
public class MessageQueue {
    // 队列大小
    //private final int QUEUE_LENGTH = 10000 * 10;
    // 基于内存的阻塞队列
    private BlockingQueue<Messages> queue = new LinkedBlockingQueue<Messages>();
    // 创建计划任务执行器
    //private ScheduledExecutorService es = Executors.newScheduledThreadPool(4);

    private final static int NN = 1;// 牛牛
    private final static int SSS = 4;// 十三水
    private final static int ZJH = 6;// 炸金花
    private final static int BDX = 10;// 比大小
    private final static int COMMEN = 0;// 通用

    //	public final NNGameEventDeal nnService=new NNGameEventDeal();
    public final SSSGameEventDeal sssService = new SSSGameEventDeal();
    //	public final BDXGameEventDeal bdxService=new BDXGameEventDeal();
    public final RoomManage roomManage = new RoomManage();
    public final GameLogsCache gameLogsCache = new GameLogsCache();

    /**
     * 构造函数，执行execute方法
     */
    public MessageQueue(int works) {
        for (int i = 0; i < works; i++) {
            execute();
        }

    }

    /**
     * 添加信息至队列中
     *
     * @param messages the messages
     */
    public void addQueue(Messages messages) {
        queue.add(messages);
    }

    /**
     * 初始化执行
     */
    public void execute() {

        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    while (true) {
                        Messages messages = queue.take();
                        SocketIOClient client = messages.getClient();
                        Object data = messages.getDataObject();
                        // 处理队列中的信息。。。。。
                        switch (messages.getGid()) {
                            case COMMEN:
                                switch (messages.getSorts()) {
                                    case 1:
                                        roomManage.getGameSetting(client, data);
                                        break;
                                    case 2:
                                        roomManage.getAllRoomList(client, data);
                                        break;
                                    case 3:
                                        roomManage.checkUser(client, data);
                                        break;
                                    case 4:
                                        gameLogsCache.getGameLogsList(client, data);
                                        break;
                                    default:
                                        break;
                                }
                                break;
                            case NN:
                                switch (messages.getSorts()) {

                                    case 15:
                                        //new com.za.gameservers.sss.test.SSSGameEventDeal().createRoom(client, data);
                                        roomManage.createRoomNN(client, data);
                                        break;
                                    case 16:
                                        //new com.za.gameservers.sss.test.SSSGameEventDeal().joinRoom(client, data);
                                        roomManage.joinRoomNN(client, data);
                                        break;
                                }
                            case SSS:
                                // 十三水
                                switch (messages.getSorts()) {
                                    case 1:
                                        // 创建房间或加入房间
                                        sssService.enterRoom(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 2:
                                        // 玩家准备
                                        sssService.gameReady(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 3:
                                        // 游戏中
                                        sssService.gameEvent(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 4:
                                        // 解散房间
//								sssService.closeRoom(messages.getClient(),
//										messages.getDataObject());
                                        break;
                                    case 5:
                                        // 离开房间
                                        sssService.exitRoom(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 6:
                                        // 断线重连
                                        sssService.reconnectGame(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 7:
                                        // 判断玩家是否需要断线重连
                                        sssService.gameConnReset(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 8:
                                        // 总结算
                                        sssService.gameSummary(messages.getClient(),
                                            messages.getDataObject());
                                        break;
                                    case 9:
                                        // 查询更新用户实时信息
//								sssService.playerInfo(messages.getClient(),
//										messages.getDataObject());
                                        break;
                                    case 10:
                                        // 游戏开始-发牌（没有监听事件，只有推送事件）
//								sssService.gameStart(messages.getDataObject());
                                        break;
                                    case 11:
                                        // 游戏结算-结算（没有监听事件，只有推送事件）
//								sssService.gameEnd(messages.getDataObject());
                                        break;
                                    case 12:
                                        // 超时准备
                                        new SSSServiceImpl().readyOvertime(messages.getDataObject());
                                        break;
                                    case 13:
                                        // 超时配牌
                                        new SSSServiceImpl().peipaiOvertime(messages.getDataObject());
                                        break;
                                    default:
                                        break;
                                }
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
