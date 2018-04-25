package com.zhuoan.biz.event.nn;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 监听事件
 */
public class BRNNGameEvent {

    private final static Logger logger = LoggerFactory.getLogger(BRNNGameEvent.class);

	public static SocketIOServer server=null;
	
	public static void listenerNNGameEvent(SocketIOServer servers){
		
		server=servers;
		
		final BRNNGameEventDeal brnnService=new BRNNGameEventDeal();
		
		/**
		 * 加入房间，或创建房间事件
		 */
		server.addEventListener("enterRoom_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){
				
				try {
					brnnService.enterRoom(client, data);
				} catch (Exception e) {
                    logger.error("加入房间，或创建房间事件异常",e);
				}
			}
		});
		
		
		/**
		 * 准备方法
		 */
		server.addEventListener("gameReady_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.gameReady(client, data);
				} catch (Exception e) {
				    logger.error("准备方法发生异常",e);
				}
			}
		});
		

		/**
		 * 下注方法
		 */
		server.addEventListener("gameXiaZhu_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.gameXiaZhu(client, data);
				} catch (Exception e) {
				    logger.error("下注方法,发生异常",e);
				}
			}
		});
		
		
		/**
		 * 游戏事件
		 */
		server.addEventListener("gameEvent_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.gameEvent(client, data);
				} catch (Exception e) {
				    logger.error("游戏事件异常",e);
				}
			}
		});

		
		/**
		 * 退出房间事件
		 */
		server.addEventListener("exitRoom_BRNN", Object.class,new DataListener<Object>() {

			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.exitRoom(client, data);
				} catch (Exception e) {
				    logger.error("退出房间事件,发生异常",e);
				}
			}
		});
		
		
		/**
		 * 断线重连事件
		 */
		server.addEventListener("reconnectGame_BRNN", Object.class,new DataListener<Object>() {

			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.reconnectGame(client, data);
				} catch (Exception e) {
				    logger.error("断线重连事件,发生异常",e);
				}
			}
		});
		
		
		/**
		 * 判断玩家是否是重新连接
		 */
		server.addEventListener("gameConnReset_BRNN", Object.class,new DataListener<Object>() {

			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){

				try {
					brnnService.gameConnReset(client, data);
				} catch (Exception e) {
				    logger.error("判断玩家是否是重新连接,发生异常",e);
				}
			}
		});
		
		
		/**
		 * 闲家撤销下注
		 */
		server.addEventListener("revokeXiazhu_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){
				
				System.out.println("闲家撤销下注：data");
				try {
					brnnService.revokeXiazhu(client, data);
				} catch (Exception e) {
				    logger.error("闲家撤销下注,发生异常",e);
				}
				
			}
		});
		

		/**
		 * 闲家确认下注
		 */
		server.addEventListener("sureXiazhu_BRNN", Object.class,new DataListener<Object>() {
			
			@Override
			public void onData(SocketIOClient client, Object data, AckRequest ackSender){
				
				System.out.println("闲家确认下注：data");
				try {
					brnnService.sureXiazhu(client, data);
				} catch (Exception e) {
				    logger.error("闲家确认下注,发生异常",e);
				}
				
			}
		});

	}
}
