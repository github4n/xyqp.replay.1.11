package com.zhuoan.queue;

import com.corundumstudio.socketio.SocketIOClient;

import java.io.Serializable;

public class Messages implements Serializable{


    SocketIOClient client;
	Object dataObject;
	int gid;// 游戏id
	int sorts;// 事件顺序

    public Messages(SocketIOClient client, Object dataObject, int gid, int sorts) {
        super();
        this.client = client;
        this.dataObject = dataObject;
        this.gid = gid;
        this.sorts = sorts;
    }

    public SocketIOClient getClient() {
		return client;
	}
    public void setClient(SocketIOClient client) {
		this.client = client;
	}
    public Object getDataObject() {
		return dataObject;
	}
    public void setDataObject(Object dataObject) {
		this.dataObject = dataObject;
	}
    public int getGid() {
		return gid;
	}
    public void setGid(int gid) {
		this.gid = gid;
	}
    public int getSorts() {
		return sorts;
	}
    public void setSorts(int sorts) {
		this.sorts = sorts;
	}

	
	
}
