package com.zhuoan.biz.event;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.zhuoan.biz.constant.Constant;
import com.zhuoan.biz.dao.DBUtil;
import com.zhuoan.biz.event.sss.SSSGameEvent;
import com.zhuoan.biz.model.GameRoom;
import com.zhuoan.biz.model.RoomManage;
import com.zhuoan.biz.queue.MessageQueue;
import com.zhuoan.biz.queue.SqlQueue;
import com.zhuoan.biz.times.SingleTimer;
import com.zhuoan.biz.util.LogUtil;
import net.sf.json.JSONArray;

public class GameMain {

	/**
	 * SocketIOServer
	 */
	public static SocketIOServer server = null;
	public static MessageQueue messageQueue = null;
	public static GameRoom gameroom = null;
    public static SingleTimer singleTime=null;
    public static SqlQueue sqlQueue = null;
	
    public static void main(String[] args)  {
    	
    	startGameService();

    }
    
    /**
     * 开启游戏服务端
     */
    public static void startGameService() {
    	
        Configuration config = new Configuration();//链接对象
        config.setHostname(Constant.cfgProperties.getProperty("local_ip"));
        config.setPort(Integer.valueOf(Constant.cfgProperties.getProperty("local_port")));
        config.setWorkerThreads(2);
        config.setMaxFramePayloadLength(1024*1024);
        config.setMaxHttpContentLength(1024*1024);

        server = new SocketIOServer(config);//创建服务
        messageQueue=new MessageQueue(16);
        sqlQueue = new SqlQueue(1);
        gameroom=new GameRoom();
        singleTime=new SingleTimer();
        singleTime.start();
        //queue.execute();
        /**
         * 当client连接时触发此事件
         */
        server.addConnectListener(new ConnectListener(){
        	@Override
        	public void onConnect(SocketIOClient client) {
        		System.out.println(client.getSessionId()+"在线了！！！");
        	}
        });
        
        /**
         * 当client离线时触发此事件
         */
        server.addDisconnectListener( new DisconnectListener(){
        	@Override
        	public void onDisconnect(SocketIOClient client) {
        		System.out.println(client.getSessionId()+"离线了！！！");
        	}
        });
        
        /**
         * 心跳包
         */
        server.addEventListener("game_ping", Object.class, new DataListener<Object>(){
			@Override
			public void onData(SocketIOClient client, Object obj, AckRequest request){
				client.sendEvent("game_pong", obj);
			}
		});
        
        /**
         * 链接connection
         */
        server.addEventListener("connection", Object.class, new DataListener<Object>(){
			@Override
			public void onData(SocketIOClient client, Object obj, AckRequest request){
				System.out.println("链接成功");
				client.sendEvent("connect", request, "成功");
			}
		});
        


        /**
         * 监听十三水游戏事件
         */
        SSSGameEvent.listenerSSSGameEvent(server,messageQueue);
        

        
        
        /**
         * 服务开启
         */
        server.start();
        
        
        String sql = "select room_no from za_gamerooms where status>=0";
		JSONArray result = DBUtil.getObjectListBySQL(sql, new Object[]{});
		
		sql = "select id,game_id,opt_key,opt_name,opt_val,is_mul,is_use,createTime,memo,sort,is_open from za_gamesetting";
		RoomManage.result = DBUtil.getObjectListBySQL(sql, new Object[]{});
		System.err.println(RoomManage.result);
     	LogUtil.print("当前房间："+result);

    }

    /**
     * 停止游戏服务
     */
    public static void stopGameService() {
    	
    	server.stop();
    }
}
