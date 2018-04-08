package com.zhuoan.times;

import com.corundumstudio.socketio.SocketIOClient;
import com.zhuoan.queue.Messages;
import net.sf.json.JSONObject;

public class TimerMsgData {
	public int nType;
	public int nTimeLimit;
	public Messages gmd;
	public String roomid;
	public SocketIOClient client;
	public JSONObject data;
	public int gid;

}
