package com.zhuoan.webapp.controller.socketio.demo;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
/**
 * ClientDemo
 *
 * @author weixiang.wu
 * @date 2018-04-02 09:05
 **/
public class ClientDemo {
    public static void main(String[] args) {
        try{
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;
            final Socket socket = IO.socket("http://192.168.1.123:8084?deviceId=ZYLPC", options);

            socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("connect");
//                    socket.close();
                }
            }).on(Socket.EVENT_CONNECT_TIMEOUT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("connect timeout");
                }
            }).on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("connect error");
                }
            }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    System.out.println("disconnect");
                }
            }).on("advert_info", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    String data = (String)args[0];
                    System.out.println("服务端：************"+data.toString());
                    //给服务端发送信息
                    socket.emit("advert_info", "服务端你好，我是客户端，我有问题想咨询你！56565");
                }
            }).on("notice_info", new Emitter.Listener(){
                @Override
                public void call(Object... args){
                    String data = (String)args[0];
                }
            })
                .on("test",new Emitter.Listener(){
                    @Override
                    public void call(Object... args){
                        System.out.printf("12");
                    }
                });
            socket.open();
        }catch(Exception e){

        }
    }
}
