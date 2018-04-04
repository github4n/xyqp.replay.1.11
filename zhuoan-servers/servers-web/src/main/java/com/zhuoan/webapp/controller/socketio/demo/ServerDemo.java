package com.zhuoan.webapp.controller.socketio.demo;

import java.util.Map;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;

/**
 * ServerDemo
 *
 * @author weixiang.wu
 * @date 2018-04-02 09:05
 **/
public class ServerDemo {
    public static void main(String[] args) throws InterruptedException {
        Configuration config = new Configuration();
        //服务器主机ip，这里配置本机
        config.setHostname("localhost");
        //端口，任意
        config.setPort(9092);
        config.setMaxFramePayloadLength(1024 * 1024);
        config.setMaxHttpContentLength(1024 * 1024);
        SocketIOServer server = new SocketIOServer(config);
        //监听广告推送事件，advert_info为事件名称，自定义
        server.addEventListener("advert_info", String.class, new DataListener<String>(){
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) throws ClassNotFoundException {
                //客户端推送advert_info事件时，onData接受数据，这里是string类型的json数据，还可以为Byte[],object其他类型

                String sa = client.getRemoteAddress().toString();
                String clientIp = sa.substring(1,sa.indexOf(":"));//获取客户端连接的ip
                Map params = client.getHandshakeData().getUrlParams();//获取客户端url参数
                System.out.println(clientIp+"：客户端：************"+data);
            }
        });
        //监听通知事件
        server.addEventListener("notice_info", String.class, new DataListener<String>() {
            @Override
            public void onData(SocketIOClient client, String data, AckRequest ackRequest) {
                //同上
                client.sendEvent("test","");
            }
        });

        /**
         * 监听其他事件
         */

        //添加客户端连接事件
        server.addConnectListener(new ConnectListener() {
            @Override
            public void onConnect(SocketIOClient client) {
                // TODO Auto-generated method stub
                String sa = client.getRemoteAddress().toString();
                String clientIp = sa.substring(1,sa.indexOf(":"));//获取设备ip
                System.out.println(clientIp+"-------------------------"+"客户端已连接");
                Map params = client.getHandshakeData().getUrlParams();

                //给客户端发送消息
                client.sendEvent("advert_info",clientIp+"客户端你好，我是服务端，有什么能帮助你的？");
            }
        });
        //添加客户端断开连接事件
        server.addDisconnectListener(new DisconnectListener(){
            @Override
            public void onDisconnect(SocketIOClient client) {
                // TODO Auto-generated method stub
                String sa = client.getRemoteAddress().toString();
                String clientIp = sa.substring(1,sa.indexOf(":"));//获取设备ip
                System.out.println(clientIp+"-------------------------"+"客户端已断开连接");

                //给客户端发送消息
                client.sendEvent("advert_info",clientIp+"客户端你好，我是服务端，期待下次和你见面");
            }
        });
        server.start();

        Thread.sleep(Integer.MAX_VALUE);

        server.stop();
    }
}
