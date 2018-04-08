package com.zhuoan.socketio;

import com.corundumstudio.socketio.SocketIOServer;

/**
 * SocketIoManagerService
 *
 * @author weixiang.wu
 * @date 2018-04-03 17:19
 **/
public interface SocketIoManagerService {
    /**
     * Start server.
     */
    void startServer() ;

    /**
     * Stop server.
     */
    void stopServer();

    /**
     * Gets server.
     *
     * @return the server
     */
    SocketIOServer getServer();

    /**
     * Send message to all client.
     *
     * @param eventType the event type
     * @param message   the message
     */
    void sendMessageToAllClient(String eventType, String message);

    /**
     * Send message to one client.
     *
     * @param uuid      the uuid
     * @param eventType the event type
     * @param message   the message
     */
    void sendMessageToOneClient(String uuid, String eventType, String message);
}
