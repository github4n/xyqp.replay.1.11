package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.zhuoan.queue.Messages;
import com.zhuoan.service.jms.ProducerService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.jms.Destination;

/**
 * 监听事件
 */
@Service
public class NNGameEvent {

    @Resource
    private Destination nnQueueDestination;

    @Resource
    private ProducerService producerService;

    public void listenerNNGameEvent(SocketIOServer server) {
        /**
         * 加入房间，或创建房间事件
         */
        server.addEventListener("enterRoom_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 1));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 1));
//					//nnService.enterRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });
        server.addEventListener("getUserInfo", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 1));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 23));
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });
        server.addEventListener("joinRoomNN", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 1));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 16));
//					//nnService.enterRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });
        server.addEventListener("createRoomNN", Object.class, new DataListener<Object>() {

            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 1));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 15));
//					//nnService.enterRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 准备方法
         */
        server.addEventListener("gameReady_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 2));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 2));
//					//nnService.gameReady(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 下注方法
         */
        server.addEventListener("gameXiaZhu_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 3));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 3));
//					//nnService.gameXiaZhu(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 游戏事件
         */
        server.addEventListener("gameEvent_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 4));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 4));
//					//nnService.gameEvent(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 抢庄事件
         */
        server.addEventListener("qiangZhuang_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 5));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 5));
//					//nnService.qiangZhuang(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 申请解散房间事件
         */
        server.addEventListener("closeRoom_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 6));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 6));
//					//nnService.closeRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 退出房间事件
         */
        server.addEventListener("exitRoom_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 7));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 7));
//					//nnService.exitRoom(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 断线重连事件
         */
        server.addEventListener("reconnectGame_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 8));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 8));
//					//nnService.reconnectGame(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 判断玩家是否是重新连接
         */
        server.addEventListener("gameConnReset_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 9));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 9));
//					//nnService.gameConnReset(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(NNGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}
            }
        });


        /**
         * 闲家撤销下注
         */
        server.addEventListener("revokeXiazhu_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 10));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 10));
//					//nnService.revokeXiazhu(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(MJGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}

            }
        });


        /**
         * 闲家确认下注
         */
        server.addEventListener("sureXiazhu_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 11));

//				try {
//					queue.addQueue(new Messages(client, data, 1, 11));
//					//nnService.sureXiazhu(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(MJGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}

            }
        });


        /**
         * 观战玩家坐下
         */
        server.addEventListener("gameRuzuo_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 12));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 12));
//					//nnService.gameRuzuo(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(MJGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}

            }
        });

        /**
         * 观战玩家站起
         */
        server.addEventListener("gameZhanQi_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 13));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 13));
//					//nnService.gameZhanQi(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(MJGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}

            }
        });

        /**
         * 玩家换桌
         */
        server.addEventListener("getChangeTable_NN", Object.class, new DataListener<Object>() {
            @Override
            public void onData(SocketIOClient client, Object data, AckRequest ackSender) {
                producerService.sendMessage(nnQueueDestination, new Messages(client, data, 1, 14));
//				try {
//					queue.addQueue(new Messages(client, data, 1, 14));
//					//nnService.changeTable(client, data);
//				} catch (Exception e) {
//					Logger.getLogger(MJGameEvent.class).error(e.getMessage(), e);
//					e.printStackTrace();
//				}

            }
        });

    }
}
